package com.hongmeng.xiuxian.epoch;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.CultivationSource;
import com.hongmeng.xiuxian.item.ModItems;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 纪元秘境：原文里的"纪元任务"是赛季制的大型副本。
 *
 * <p>这里把它做成一个真正可进入的维度（{@code hongmeng:epoch_realm}）：
 * 在门内点火入场，连过三波敌潮即为通关，奖励纪元令与大量修为，
 * 每通关 5 次推进一个赛季。</p>
 */
public final class EpochRealm {

    /** 维度键。 */
    public static final ResourceKey<Level> EPOCH_REALM =
            ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    new ResourceLocation("hongmeng", "epoch_realm"));

    /** 秘境内的固定挑战坐标。 */
    private static final int ARENA_X = 0;
    private static final int ARENA_Z = 0;

    /** 波次怪物配置：第 1 波、第 2 波、第 3 波（首领波）。 */
    private static final int[] WAVE_COUNTS = {4, 6, 3};

    /** 每波之间的间隔 tick。 */
    private static final int WAVE_GAP_TICKS = 20 * 8;

    /** 通关冷却（tick）：5 分钟，避免刷奖励。 */
    private static final long CLEAR_COOLDOWN = 20L * 60L * 5L;

    private static final UUID EPOCH_DAMAGE_ID =
            UUID.nameUUIDFromBytes("hongmeng_epoch_mob_damage".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final UUID EPOCH_HEALTH_ID =
            UUID.nameUUIDFromBytes("hongmeng_epoch_mob_health".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    /** 每个玩家当前的挑战状态。 */
    private record Challenge(int wave, Set<UUID> mobs, long lastWaveTick, long startedAt) {
    }

    private static final Map<UUID, Challenge> CHALLENGES = new HashMap<>();
    private static final Map<UUID, Vec3> RETURN_POINTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_CLEAR = new HashMap<>();

    private EpochRealm() {
    }

    // ------------------------------------------------------------------
    // 进出秘境
    // ------------------------------------------------------------------

    public static ServerLevel level(net.minecraft.server.MinecraftServer server) {
        return server.getLevel(EPOCH_REALM);
    }

    /**
     * 纪元秘境已并入通用副本流程（怪潮 → arena_epoch → 傀儡祖师）。
     *
     * <p>为什么改：旧流程是独立一套“3 波直接通关”，没有 boss 决战维度，
     * 与另三座副本不一致（玩家观感“纪元没有 boss 决战”，审计 A-3）；
     * 而且两套系统各自维护返回点，容易把玩家送回很远的位置（A-6/A-7）。
     * 现在这里只做转发，入口（旧纪元门/命令）都不变。</p>
     */
    public static void enter(ServerPlayer player, CultivationData data) {
        com.hongmeng.xiuxian.dungeon.DungeonRealm.enter(
                player, com.hongmeng.xiuxian.dungeon.Dungeon.EPOCH, data);
    }

    /** 纪元已并入通用流程：退出走 {@code DungeonRealm.leave}（带返回点/安全落点/传送冷却）。 */
    public static void leave(ServerPlayer player, CultivationData data) {
        com.hongmeng.xiuxian.dungeon.DungeonRealm.leave(
                player, com.hongmeng.xiuxian.dungeon.Dungeon.EPOCH);
    }

    /** 强制加载以 center 为中心的 5×5 区块，保证挑战场地是活的。 */
    /**
     * <b>先把秘境中心区块立起来</b>（在算落点之前调用）。
     *
     * <p>为什么必须提前：{@link #findLanding} 第一步就是
     * {@code level.getHeight(MOTION_BLOCKING, x, z)} ——
     * 区块没加载时这个查询会返回<b>海平面高度(63)</b>而不是真实地形高度，
     * 于是落点算错（玩家反馈"进入传送门的出生地还是错的"）。</p>
     */
    private static void forceLoadAreaRaw(ServerLevel level) {
        int cx = ARENA_X >> 4;
        int cz = ARENA_Z >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz,
                        net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
            }
        }
    }

    private static void forceLoadArea(ServerLevel level, BlockPos center) {
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz,
                        net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
            }
        }
    }

    /** 找一块能站的地面。 */
    private static BlockPos findLanding(ServerLevel level, int x, int z) {
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos candidate = new BlockPos(x, top, z);
        if (level.isEmptyBlock(candidate) && !level.isEmptyBlock(candidate.below())) {
            return candidate;
        }
        // 兜底：往上找两格
        for (int dy = 0; dy <= 4; dy++) {
            BlockPos p = candidate.above(dy);
            if (level.isEmptyBlock(p) && level.isEmptyBlock(p.above())
                    && !level.isEmptyBlock(p.below())) {
                return p;
            }
        }
        return candidate.above();
    }

    // ------------------------------------------------------------------
    // 挑战流程
    // ------------------------------------------------------------------

    public static void startChallenge(ServerPlayer player, CultivationData data) {
        cleanupChallenge(player);
        long now = player.level().getGameTime();
        CHALLENGES.put(player.getUUID(), new Challenge(0, new HashSet<>(), now, now));
        showBossBar(player, 1, WAVE_COUNTS[0], "第 1 波");
        spawnWave(player, data, 0);
    }

    /** 由 ModEvents 每 tick 驱动。 */
    public static void tick(ServerPlayer player, CultivationData data) {
        Challenge ch = CHALLENGES.get(player.getUUID());
        if (ch == null) {
            return;
        }
        if (!player.level().dimension().equals(EPOCH_REALM)) {
            // 跑到别处去了，作废
            cleanupChallenge(player);
            return;
        }
        // 清理已死的记录
        ch.mobs().removeIf(id -> {
            Entity e = ((ServerLevel) player.level()).getEntity(id);
            return e == null || !e.isAlive();
        });

        if (!ch.mobs().isEmpty()) {
            updateBossBar(player, ch.wave() + 1, ch.mobs().size());
            return;
        }

        long now = player.level().getGameTime();
        int nextWave = ch.wave() + 1;
        if (nextWave >= WAVE_COUNTS.length) {
            complete(player, data);
            return;
        }
        if (now - ch.lastWaveTick() < WAVE_GAP_TICKS) {
            long remain = (WAVE_GAP_TICKS - (now - ch.lastWaveTick())) / 20L;
            player.displayClientMessage(Component.literal(
                    "\u00a77下一波还有 " + remain + " 秒…"), true);
            return;
        }
        CHALLENGES.put(player.getUUID(),
                new Challenge(nextWave, new HashSet<>(), now, ch.startedAt()));
        showBossBar(player, nextWave + 1, WAVE_COUNTS[nextWave], "第 " + (nextWave + 1) + " 波");
        spawnWave(player, data, nextWave);
    }

    private static void spawnWave(ServerPlayer player, CultivationData data, int wave) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Challenge ch = CHALLENGES.get(player.getUUID());
        if (ch == null) {
            return;
        }
        int count = WAVE_COUNTS[wave];
        int tier = data.realm().tier();
        boolean boss = wave == WAVE_COUNTS.length - 1;

        List<Mob> spawned = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Mob mob = createWaveMob(level, wave, boss && i == 0);
            if (mob == null) {
                continue;
            }
            Vec3 pos = ringPosition(player, 8.0D + i * 1.5D, i, count);
            mob.moveTo(pos.x, pos.y, pos.z, player.getYRot() + 180.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)),
                    MobSpawnType.EVENT, null, null);
            enhance(mob, tier, wave);
            if (level.addFreshEntity(mob)) {
                spawned.add(mob);
            }
        }

        // 只登记真正生成成功的怪，否则会留下永远清不掉的幽灵 UUID
        ch.mobs().clear();
        for (Mob mob : spawned) {
            ch.mobs().add(mob.getUUID());
        }

        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.HOSTILE, 0.8F, 1.2F);
        player.sendSystemMessage(Component.literal(
                "\u00a7c【第 " + (wave + 1) + " 波】\u00a7f"
                        + (boss ? "执纪元者现身——" : "敌潮涌来——") + count + " 个。"));
        updateBossBar(player, wave + 1, ch.mobs().size());
    }

    private static Mob createWaveMob(ServerLevel level, int wave, boolean leader) {
        EntityType<? extends Mob> type;
        if (wave == 0) {
            type = level.random.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
        } else if (wave == 1) {
            type = level.random.nextBoolean() ? EntityType.VINDICATOR : EntityType.PILLAGER;
        } else {
            type = leader ? EntityType.EVOKER : EntityType.WITCH;
        }
        Mob mob = type.create(level);
        if (mob == null) {
            return null;
        }
        if (mob instanceof Zombie || mob instanceof Skeleton) {
            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        }
        mob.setPersistenceRequired();
        // 2026-11-07 玩家要求：去除怪物外圈白框（不再施加 GLOWING）。
        return mob;
    }

    /** 按玩家境界强化副本怪。 */
    private static void enhance(Mob mob, int tier, int wave) {
        double dmgMul = 0.35D + tier * 0.30D + wave * 0.25D;
        double hpMul = 0.50D + tier * 0.45D + wave * 0.35D;

        // ⚠ `addPermanentModifier` 在**同 UUID 修饰符已存在时会抛**
        //   IllegalArgumentException("Modifier is already applied on this attribute!")。
        //   同一只怪可能被强化不止一次（波次重算 / 区块重载），所以必须**先摘同名的再挂**。
        //   症状：日志刷 `Modifier is already applied on this attribute!`，
        //   而且**这一波怪的强化根本没生效**。
        AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            if (dmg.getModifier(EPOCH_DAMAGE_ID) != null) {
                dmg.removeModifier(EPOCH_DAMAGE_ID);
            }
            dmg.addPermanentModifier(new AttributeModifier(
                    EPOCH_DAMAGE_ID, "hongmeng_epoch_damage", dmgMul,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            if (hp.getModifier(EPOCH_HEALTH_ID) != null) {
                hp.removeModifier(EPOCH_HEALTH_ID);
            }
            hp.addPermanentModifier(new AttributeModifier(
                    EPOCH_HEALTH_ID, "hongmeng_epoch_health", hpMul,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            mob.setHealth(mob.getMaxHealth());
        }
        if (mob instanceof Evoker) {
            mob.setCustomName(Component.literal("\u00a7c执纪元者"));
            mob.setCustomNameVisible(true);
        }
    }

    private static Vec3 ringPosition(ServerPlayer player, double radius, int index, int total) {
        double angle = (Math.PI * 2 / Math.max(1, total)) * index;
        double x = player.getX() + Math.cos(angle) * radius;
        double z = player.getZ() + Math.sin(angle) * radius;
        ServerLevel level = (ServerLevel) player.level();
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(x), (int) Math.floor(z));
        return new Vec3(x, top, z);
    }

    // ------------------------------------------------------------------
    // 通关与结束
    // ------------------------------------------------------------------

    private static void complete(ServerPlayer player, CultivationData data) {
        cleanupChallenge(player);
        long now = player.level().getGameTime();
        Long last = LAST_CLEAR.get(player.getUUID());
        if (last != null && now - last < CLEAR_COOLDOWN) {
            long remain = (CLEAR_COOLDOWN - (now - last)) / 20L;
            player.sendSystemMessage(Component.literal(
                    "\u00a77秘境余韵未散，还需 " + remain + " 秒才能再次结算。"));
            return;
        }
        LAST_CLEAR.put(player.getUUID(), now);

        int tier = data.realm().tier();
        double reward = 400.0D + tier * 500.0D;
        CultivationSource.DUNGEON.grant(player, reward, false);
        data.addDungeonClear();

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    120, 1.0D, 1.0D, 1.0D, 0.4D);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                        "\u00a7b\u00a7l═══ 纪 元 任 务 完 成 ═══")
                .withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal(
                "\u00a7f三波敌潮尽数伏诛。你站在秘境中央，喘着粗气。"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f累计通关：\u00a7e" + data.getDungeonClears() + " \u00a7f次"));

        give(player, new ItemStack(ModItems.JIDAN.get(), 1), "纪元令 ×1");
        if (player.getRandom().nextInt(100) < 35) {
            give(player, new ItemStack(ModItems.PILL_POJING.get(), 1), "破境丹 ×1");
        }
        if (player.getRandom().nextInt(100) < 20) {
            give(player, new ItemStack(ModItems.DAOYIN.get(), 1), "道印 ×1");
        }
        player.sendSystemMessage(Component.literal(""));

        if (data.getDungeonClears() % 5 == 0) {
            data.setEpochSeason(data.getEpochSeason() + 1);
            player.sendSystemMessage(Component.literal(
                    "\u00a76\u00a7l纪元推进——新赛季 S" + data.getEpochSeason() + " 开启。"));
            player.sendSystemMessage(Component.literal(
                    "\u00a77下一轮秘境的敌人会更强。"));
        }
        ModNetworkBridge.sync(player);
    }

    private static void give(ServerPlayer player, ItemStack stack, String label) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.sendSystemMessage(Component.literal("\u00a7a获得：" + label));
    }

    private static void cleanupChallenge(ServerPlayer player) {
        Challenge ch = CHALLENGES.remove(player.getUUID());
        if (ch != null && player.level() instanceof ServerLevel level) {
            for (UUID id : ch.mobs()) {
                Entity e = level.getEntity(id);
                if (e != null) {
                    e.discard();
                }
            }
        }
        clearBossBar(player);
    }

    /**
     * 清掉<b>旧纪元流程</b>的挑战状态。
     *
     * <p>⚠ 这里**不能**再转发 {@code DungeonRealm.abort}：{@code ModEvents} 的
     * “换维度时挑战作废”旧逻辑会在**每一次换维度**调用本方法；
     * 而新流程的“怪潮 → arena_epoch → 决战”本身就要换维度，
     * 转发会把刚进决战阶段的挑战当场清掉（等于把本轮修复打回去）。
     * 新流程的作废由 {@code DungeonRealmEvents.onChangedDimension →
     * DungeonRealm.abortIfOutside} 按阶段判定，死亡由 DungeonRealmEvents 处理。</p>
     */
    public static void abort(ServerPlayer player) {
        cleanupChallenge(player);
    }

    // ------------------------------------------------------------------
    // Boss 条
    // ------------------------------------------------------------------

    private static final Map<UUID, net.minecraft.server.level.ServerBossEvent> BARS = new HashMap<>();

    private static void showBossBar(ServerPlayer player, int wave, int count, String label) {
        net.minecraft.server.level.ServerBossEvent bar = BARS.get(player.getUUID());
        if (bar == null) {
            bar = new net.minecraft.server.level.ServerBossEvent(
                    Component.literal("\u00a7c纪元秘境 · " + label),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            BARS.put(player.getUUID(), bar);
            bar.addPlayer(player);
        }
        bar.setName(Component.literal("\u00a7c纪元秘境 · " + label + " \u00a7f剩余 " + count));
        bar.setProgress(Math.min(1.0F, count / 8.0F));
        bar.setVisible(true);
    }

    private static void updateBossBar(ServerPlayer player, int wave, int count) {
        net.minecraft.server.level.ServerBossEvent bar = BARS.get(player.getUUID());
        if (bar == null) {
            return;
        }
        bar.setName(Component.literal("\u00a7c纪元秘境 · 第 " + wave + " 波 \u00a7f剩余 " + count));
        bar.setProgress(Math.min(1.0F, count / 8.0F));
    }

    private static void clearBossBar(ServerPlayer player) {
        net.minecraft.server.level.ServerBossEvent bar = BARS.remove(player.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
            bar.setVisible(false);
        }
    }

    /** 是否正在挑战中（新流程统一问 DungeonRealm）。 */
    public static boolean inChallenge(ServerPlayer player) {
        return com.hongmeng.xiuxian.dungeon.DungeonRealm.inChallenge(player);
    }

    /** 给外部（比如调试命令）查询用。 */
    public static int remainingMobs(ServerPlayer player) {
        return com.hongmeng.xiuxian.dungeon.DungeonRealm.remainingMobs(player);
    }

    /** 调试用：直接清掉当前这一波（新流程统一走 DungeonRealm）。 */
    public static int debugKillWave(ServerPlayer player) {
        return com.hongmeng.xiuxian.dungeon.DungeonRealm.debugKillWave(
                player, com.hongmeng.xiuxian.dungeon.Dungeon.EPOCH);
    }

    static void clearAll() {
        CHALLENGES.clear();
        RETURN_POINTS.clear();
        LAST_CLEAR.clear();
        BARS.clear();
    }
}
