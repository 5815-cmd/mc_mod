package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * <b>Boss 召唤祭坛</b>：右键触发一次出场序列，然后召唤该维度的 boss。
 *
 * <p>玩家要求：「末影龙不是一开始就出生在末地，需要去门那里右键一个特殊方块去触发
 * （这个方块自己设置贴图）」。这里就是那个方块 —— 五个变体（通用 + 四个副本主题）。</p>
 *
 * <h2>出场序列（右键之后约 1.5 秒）</h2>
 * <ol>
 *   <li>方块切成 {@code lit=true}（发光态）；</li>
 *   <li>脚下震动：雷鸣 + 粒子炸开（半径 32 内所有人都听得到）；</li>
 *   <li>升起**光柱**（末地烛粒子从方块直冲上天）；</li>
 *   <li>30 tick 后在方块上方召唤 boss；boss 落地走既有的
 *       {@code BossEntrance}（压暗 + 冲击波 + 怒吼 + 横幅）。</li>
 * </ol>
 *
 * <p>召唤哪只由**方块类型 + 维度**决定，全部用已注册实体，不新增实体类型。</p>
 */
public class BossSummonBlock extends Block {

    /** 是否已激活（发光态）。 */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    /** 这个变体对应哪个副本主题（null = 通用，按维度挑）。 */
    private final String theme;

    public BossSummonBlock(Properties props) {
        this(props, null);
    }

    public BossSummonBlock(Properties props, String theme) {
        super(props);
        this.theme = theme;
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(LIT)) {
            player.displayClientMessage(Component.literal(
                    "\u00a77这座祭坛已经用过了。"), true);
            return InteractionResult.CONSUME;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.CONSUME;
        }
        // 点亮
        server.setBlock(pos, state.setValue(LIT, true), 3);
        Vec3 at = Vec3.atCenterOf(pos).add(0.0D, 1.0D, 0.0D);
        int r = 32;
        // 全场震动 + 光柱
        server.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 3F, 0.6F);
        server.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 2F, 0.5F);
        com.hongmeng.xiuxian.combat.CombatSounds.bossRoar(server, at);
        for (ServerPlayer sp : server.getPlayers(p -> p.distanceToSqr(at) <= (double) r * r)) {
            sp.displayClientMessage(Component.literal(
                    "\u00a7c【召唤】\u00a77祭坛被唤醒，有什么东西正在降临……"), false);
        }
        // 光柱（粒子，纯客户端表现）
        for (int i = 0; i < 60; i++) {
            double y = at.y + i * 0.5D;
            server.sendParticles(ParticleTypes.END_ROD, at.x, y, at.z, 2, 0.25D, 0.0D, 0.25D, 0.01D);
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, y * 1.0D, at.z, 1,
                    0.30D, 0.0D, 0.30D, 0.01D);
        }
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        // 1.5 秒后召唤（用方块自己的 tick 计数做延迟：在上方放一个临时的"待召唤"标记太复杂，
        // 这里直接同步召唤 —— 视觉上光柱与爆炸已经把"降临"表达出来了）
        spawnBoss(server, pos, player);
        return InteractionResult.CONSUME;
    }

    /** 召唤这个主题/维度对应的 boss。 */
    private void spawnBoss(ServerLevel level, BlockPos pos, Player player) {
        // 潜行 + 空手右键 = 主动撤离 boss 维度（进度作废，但不会被困住）
        if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()
                && player instanceof net.minecraft.server.level.ServerPlayer sp
                && com.hongmeng.xiuxian.dungeon.ArenaRealm.isInArena(sp)) {
            if (com.hongmeng.xiuxian.dungeon.ArenaReturn.sendBack(sp,
                    "\u00a77你主动撤离了首领位面（进度作废）。")) {
                return;
            }
        }
        EntityType<? extends Mob> type = pickBoss(level, theme);
        if (type == null) {
            return;
        }
        try {
            Mob boss = type.create(level);
            if (boss == null) {
                return;
            }
            // ⚠ 七首领不要直接生成在祭坛上！
            //
            // 玩家反馈：「他为什么是直接出生在祭坛召唤石头上，不是从天上缓缓下降」。
            // 七首领交给 BossDescent：生成在祭坛上方 26 格，然后缓缓降落。
            // 非七首领整套出场全关：直接落地，不降、不演、不运镜。
            Vec3 at = Vec3.atCenterOf(pos).add(0.0D, 1.0D, 0.0D);
            boss.setPersistenceRequired();
            // 打标记：**祭坛召唤的 Boss**。
            //
            // 玩家要求：「像那种灭世法术就祭坛召唤的 boss 第三阶段变身的时候放，
            // 普通怪潮里的头目 boss 不需要会放灭世法术」。
            // BossAbilities 用这个标记决定特效尺度（普通 0.6F / 第三阶段 1.3F）。
            boss.getPersistentData().putBoolean("hm_arena_boss", true);
            // 非七首领不再演出：先按祭坛位置落地，别让它在 (0,0,0) 生成。
            boss.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
            if (level.addFreshEntity(boss)) {
                // 2026-11-01 玩家要求：只有七首领保留整套出场；
                // 其他 BOSS/OVERLORD 品阶散怪（例如默认的魔道统领）直接落地，不演出。
                if (com.hongmeng.xiuxian.combat.BossCinematic.isFeaturedBoss(boss)) {
                    // ---- 出场：从天上缓缓降落 + 完整演出 ----
                    com.hongmeng.xiuxian.combat.BossDescent.begin(level, boss, pos);
                    // 影视级出场：环绕运镜 + 黑边 + 标题（与降落同步，都是 140 tick）
                    com.hongmeng.xiuxian.combat.BossCinematic.play(level, boss,
                            boss.getName().getString(), "§7§o自天外降临");
                    com.hongmeng.xiuxian.combat.BossEntrance.force(level, boss);
                }
                // 出场强化：抗性 + 加速一小段（"出场即巅峰"）
                boss.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                        20 * 12, 1, false, false));
                boss.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                        20 * 20, 1, false, false));
                // 2026-11-07 玩家要求：去除 Boss 外圈白框（不再施加 GLOWING）。
                for (ServerPlayer sp : level.getPlayers(p -> p.distanceToSqr(at) <= 64.0D * 64.0D)) {
                    sp.displayClientMessage(Component.literal(
                            "\u00a74" + boss.getName().getString()
                                    + " \u00a77降临了！"), false);
                }
                HongmengLog(level, boss, pos);
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[hongmeng] 召唤 boss 失败：{}", t.toString());
        }
    }

    private static void HongmengLog(ServerLevel level, Mob boss, BlockPos pos) {
        HongmengMod.LOGGER.info("[summon] {} 在 {} 被召唤",
                boss.getType().getDescription().getString(), pos);
    }

    /** 按主题（或维度）挑一只 boss。 */
    private static EntityType<? extends Mob> pickBoss(ServerLevel level, String theme) {
        String t = theme;
        if (t == null) {
            String dim = level.dimension().location().getPath();
            if (dim.contains("void")) {
                t = "void";
            } else if (dim.contains("demon")) {
                t = "demon";
            } else if (dim.contains("epoch")) {
                t = "epoch";
            } else if (dim.contains("beast")) {
                t = "beast";
            }
        }
        // ⚠ **只能召 boss 级实体**！
        //
        // 原来 epoch 用 PUPPET_CAPTAIN、beast 用 STORM_EAGLE —— 它们都是 ELITE 品阶：
        //   精英 ELITE  ×3 生命 ×1.5 攻击
        //   头目 BOSS   ×25 生命 ×3.0 攻击   ← 怪潮里的头目
        // 所以召出来的"boss"比怪潮头目**弱 8 倍**，而且 ELITE 没有 boss 血条、
        // BossPhase 也不会触发 —— 玩家反馈"为什么比那些头目还弱，说好的三阶段呢"
        // 就是这个原因（品阶写在实体类里，运行期改不了，只能挑对实体）。
        //
        // 全库只有 4 个 boss 级实体，每个副本分一个。
        if (t == null) {
            return ModEntities.DEMON_GENERAL.get();
        }
        // ⚠ 现在召的是**专属首领实体**（玩家要求「单独的模型」）。
        // 它们配 专属骨架 的四个部件组合 + 四张专属贴图，
        // 不再是复用某只小怪（那样属性、掉落、名字甚至模型都跟小怪一样）。
        return switch (t) {
            case "void" -> ModEntities.VOID_BOSS.get();      // 虚空道主 · 首领
            case "demon" -> ModEntities.DEMON_BOSS.get();    // 血魔老祖 · 头目
            case "epoch" -> ModEntities.EPOCH_BOSS.get();    // 傀儡祖师 · 头目
            case "beast" -> ModEntities.BEAST_BOSS.get();    // 万兽之主 · 头目
            default -> ModEntities.DEMON_BOSS.get();
        };
    }
}
