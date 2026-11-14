package com.hongmeng.xiuxian.sword;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.network.SpellFxPacket;
import com.hongmeng.xiuxian.technique.SpellVfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>飞剑剑阵 · 服务端运行层</b>。
 *
 * <h2>参考</h2>
 * <p>学自 <b>御剑工艺（Yujian Craft）</b>的六器剑阵：
 * 六把飞剑环绕玩家、<b>每把独立冷却</b>、<b>自动索敌</b>、
 * 目标是<b>准心附近的敌人</b>（"灵剑指引"）、锁定持续到目标死亡。</p>
 *
 * <h2>视觉怎么做的（为什么不新建渲染器）</h2>
 * <p>项目里已经有一套成熟的"服务端发形状包 → 客户端画三维几何"的管线
 * （{@code SpellFxPacket} + {@code ClientSpellFx}），而且里头
 * <b>已经有现成的立体剑身</b>（{@code BLADE_GOLD} —— 带厚度的金色巨剑）。</p>
 *
 * <p>所以飞剑直接<b>复用这个形状</b>，只是把尺寸调小、寿命调长 ——
 * 于是"环绕的飞剑"不需要任何新的渲染代码。这是我先查过管线才做的决定。</p>
 *
 * <h2>剑阵的观感关键：错开，而不是齐射</h2>
 * <p>六把剑如果共用冷却会<b>同时</b>飞出去，看起来像一次性爆发而不是剑阵。
 * 所以 {@link SwordFormation#ATTACK_COOLDOWN} 是<b>每把剑自己的</b>，
 * 并且初始相位错开 —— 结果是"此起彼伏"的连续穿刺。</p>
 */
public final class SwordFormationRuntime {

    /** 每个玩家的剑阵状态。 */
    private static final class State {
        boolean active;
        SwordFormation.Pattern pattern = SwordFormation.Pattern.RING;
        /** 每把剑上次攻击的刻（Long.MIN_VALUE = 从未）。 */
        final long[] lastAttack = new long[SwordFormation.MAX_SWORDS];
        /** 锁定目标。 */
        UUID target;
        /**
         * 下次允许再做索敌扫描的刻。
         *
         * <p>没有目标时每 tick 扫一遍是白扫（20 次/秒）。扫空之后
         * 5 tick 内不再扫 —— 0.25 秒，玩家感觉不到"剑慢半拍"，
         * 但开销从 20 次/秒降到 4 次/秒。</p>
         */
        long nextScanTick;
        /** 每把剑当前这次的穿刺进度（-1 = 没在穿刺）。 */
        final int[] pierceStart = new int[SwordFormation.MAX_SWORDS];
        final Vec3[] pierceFrom = new Vec3[SwordFormation.MAX_SWORDS];
        final Vec3[] pierceTo = new Vec3[SwordFormation.MAX_SWORDS];

        State() {
            java.util.Arrays.fill(this.lastAttack, Long.MIN_VALUE);
            java.util.Arrays.fill(this.pierceStart, -1);
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private SwordFormationRuntime() {
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), k -> new State());
    }

    // ------------------------------------------------------------------
    // 开关 / 阵型
    // ------------------------------------------------------------------

    /** 展开或收回剑阵。 */
    public static void toggle(ServerPlayer player) {
        if (player == null) {
            return;
        }
        State st = state(player);
        // 第一道门槛：**有没有学会御剑术**（玩家要求给剑阵一个获取方式）
        CultivationData sd = CultivationCapability.get(player);
        if (!com.hongmeng.xiuxian.talent.SwordArt.unlocked(sd)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7c尚未参透御剑术。\u00a77先寻得《\u00a7b御剑术秘籍\u00a77》研读。"), true);
            return;
        }
        // 第二道门槛：境界（第 2 境起才有飞剑）
        int tier = realmTier(player);
        if (!SwordFormation.canUse(tier)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a77御剑需 \u00a7e筑基\u00a77 以上。你目前是 \u00a7f"
                            + realmName(player) + "\u00a77。"), true);
            return;
        }
        st.active = !st.active;
        int n = SwordFormation.swordCountFor(tier);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                st.active
                        ? "\u00a7b\u00a7l剑阵展开 \u00a77· \u00a7f" + n + " \u00a77柄 · "
                                + st.pattern.cn() + "\u00a78（Ctrl+R 换阵）"
                        : "\u00a78剑阵收回。"), true);
        if (st.active) {
            // 展开时错开初始冷却，避免六把同时出剑
            long now = player.level().getGameTime();
            for (int i = 0; i < n; i++) {
                st.lastAttack[i] = now - SwordFormation.ATTACK_COOLDOWN
                        + (long) i * (SwordFormation.ATTACK_COOLDOWN / Math.max(1, n));
            }
        }
    }

    /** 切换阵型。 */
    public static void cyclePattern(ServerPlayer player) {
        if (player == null) {
            return;
        }
        State st = state(player);
        st.pattern = st.pattern.next();
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a7b剑阵归位 \u00a77· \u00a7f" + st.pattern.cn()), true);
    }

    /** 手动锁定准心方向上的目标（"灵剑指引"）。 */
    public static void lockAhead(ServerPlayer player) {
        if (player == null) {
            return;
        }
        State st = state(player);
        LivingEntity found = findTarget(player);
        if (found == null) {
            st.target = null;
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a78前方无可锁之敌。"), true);
            return;
        }
        st.target = found.getUUID();
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a7b已锁定 \u00a7f" + found.getName().getString()
                        + "\u00a78（持续到目标死亡）"), true);
    }

    public static SwordFormation.Pattern patternOf(ServerPlayer player) {
        return state(player).pattern;
    }

    public static boolean isActive(ServerPlayer player) {
        return state(player).active;
    }

    public static void forget(UUID uuid) {
        STATES.remove(uuid);
    }

    // ------------------------------------------------------------------
    // 每 tick
    // ------------------------------------------------------------------

    /** 由 {@code ModEvents.onPlayerTick} 调。 */
    public static void tick(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        State st = state(player);
        if (!st.active) {
            return;
        }
        // 玩家 2026-11-08：剑阵视觉/攻击只在主手拿着武器时生效；
        // 空手时保留展开状态，换回武器会自动恢复。
        if (!holdingWeapon(player)) {
            return;
        }
        int tier = realmTier(player);
        int n = SwordFormation.swordCountFor(tier);
        if (n <= 0) {
            st.active = false;
            return;
        }
        long now = level.getGameTime();
        Vec3 look = player.getLookAngle();
        Vec3 center = player.position();

        // ---- 锁定目标：优先已锁的（持续到死亡/离开范围），否则自动索敌 ----
        LivingEntity target = resolveTarget(level, st, player);
        if (target == null) {
            // 扫空之后 5 tick 内不再扫：没有目标时每 tick 扫一遍纯属浪费
            // （20 次/秒 -> 4 次/秒）。找到目标就恢复正常节奏。
            if (now >= st.nextScanTick) {
                target = findTarget(player);
                st.target = target == null ? null : target.getUUID();
                if (target == null) {
                    st.nextScanTick = now + 5L;
                }
            }
        }

        for (int i = 0; i < n; i++) {
            Vec3 orbit = SwordFormation.orbitPos(center, look, i, n, st.pattern, now);

            // 正在穿刺：只画飞行段，不重新决策
            if (st.pierceStart[i] >= 0) {
                int el = (int) (now - st.pierceStart[i]);
                if (el >= SwordFormation.PIERCE_TICKS) {
                    st.pierceStart[i] = -1;
                } else {
                    Vec3 at = SwordFormation.piercePos(st.pierceFrom[i], st.pierceTo[i],
                            (double) el / SwordFormation.PIERCE_TICKS);
                    bladeFx(level, at, 1.15F, 5);
                    continue;
                }
            }

            // 环绕中的剑：画一把小剑
            //
            // PERF(Lead): 原来这里每 tick 每把剑都发一个 SpellFxPacket
            // （6 把剑 = 120 包/s，NEAR 半径 192 格逐玩家序列化），而剑的
            // 寿命是 4 tick、绕圈角速度只有 0.035 rad/tick（2 tick 移动约
            // 0.15 格，肉眼几乎看不出来）。改成每 2 tick 发一次：包量减半，
            // 视觉上由两个生命周期重叠的剑身形成极轻微拖影。
            // 穿刺段（上面 PIERCE_TICKS 分支）仍然逐 tick 发，保证飞行顺滑。
            if ((now & 1L) == 0L) {
                bladeFx(level, orbit, 1.0F, 4);
            }

            // 冷却好了 + 有目标 → 出击
            if (target != null && SwordFormation.ready(now, st.lastAttack[i])) {
                st.lastAttack[i] = now;
                st.pierceStart[i] = (int) now;
                st.pierceFrom[i] = orbit;
                Vec3 hit = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                        target.getZ());
                st.pierceTo[i] = hit;
                strike(player, level, target, orbit, hit, i, n);
            }
        }
    }

    /** 一次穿刺：伤害 + 火花 + 剑光。 */
    private static void strike(ServerPlayer player, ServerLevel level, LivingEntity target,
                               Vec3 from, Vec3 to, int index, int count) {
        try {
            // 伤害：用玩家自己的攻击力（跟装备与境界走），稍微打折 ——
            // 剑阵是"持续压制"，不该比玩家亲手一剑还狠。
            float dmg = (float) Math.max(2.0D,
                    player.getAttributeValue(net.minecraft.world.entity.ai.attributes
                            .Attributes.ATTACK_DAMAGE) * 0.55D);
            target.hurt(level.damageSources().playerAttack(player), dmg);
            // 剑光贯穿：从环绕位到目标画一道
            SpellVfx.trail(level, from, to, ParticleTypes.END_ROD, 12, 0.06D, 0.0D);
            SpellVfx.burst(level, to, ParticleTypes.CRIT, 8, 0.25D, 0.12D);
        } catch (Throwable t) {
            HongmengLog(t);
        }
    }

    /** 目标解析：已锁的必须还活着且在范围内，否则失效。 */
    private static LivingEntity resolveTarget(ServerLevel level, State st,
                                              ServerPlayer player) {
        if (st.target == null) {
            return null;
        }
        var e = level.getEntity(st.target);
        if (e instanceof LivingEntity le && le.isAlive()
                && le.distanceTo(player) <= SwordFormation.LOCK_RANGE * 1.6D) {
            return le;
        }
        st.target = null;
        return null;
    }

    /**
     * 自动索敌：<b>优先准心方向的敌人</b>（"灵剑指引"），没有就取最近的。
     *
     * <p>为什么优先准心：玩家拿剑阵打谁，应该由他看哪决定 ——
     * 纯粹取最近会导致"我想打前面那个，剑全飞向旁边的小怪"。</p>
     */
    private static LivingEntity findTarget(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(SwordFormation.LOCK_RANGE);
        List<LivingEntity> cands = new ArrayList<>();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == player || !e.isAlive() || e.isSpectator()) {
                continue;
            }
            if (e instanceof ServerPlayer) {
                continue;                       // 不打玩家
            }
            if (e instanceof Mob m && m.isAlliedTo(player)) {
                continue;
            }
            cands.add(e);
        }
        if (cands.isEmpty()) {
            return null;
        }
        Vec3 look = player.getLookAngle();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : cands) {
            Vec3 dir = e.position().subtract(player.position());
            double dist = dir.length();
            if (dist < 1.0E-3D) {
                continue;
            }
            // 夹角越小越好（点积越大）；距离作为次要因素
            double dot = dir.normalize().dot(look);
            double score = (1.0D - dot) * 30.0D + dist;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** 飞剑的客户端视觉（复用现成的立体剑身形状包）。 */
    private static void bladeFx(ServerLevel level, Vec3 at, float size, int life) {
        try {
            com.hongmeng.xiuxian.network.ModNetwork.sendToAllAround(level, at,
                    new SpellFxPacket(SpellFxPacket.BLADE_GOLD, at, at, size,
                            0.0F, life, 0, 1.0F));
        } catch (Throwable ignored) {
            // 视觉失败不影响战斗
        }
    }

    private static void HongmengLog(Throwable t) {
        HongmengMod.LOGGER.debug("[sword] 剑阵结算异常：{}", t.toString());
    }

    private static int realmTier(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        return data == null || data.realm() == null ? 1 : data.realm().tier();
    }

    private static String realmName(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        return data == null || data.realm() == null ? "凡人" : data.realm().fullCn();
    }

    /** 主手是否拿着武器；剑阵只在持械时显示和攻击。 */
    private static boolean holdingWeapon(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof net.minecraft.world.item.SwordItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.CareerWeaponItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.ModPathWeapon.PathWeaponItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.RealmWeaponItem) {
            return true;
        }
        return com.hongmeng.xiuxian.item.gear.WeaponAttributeHandler.attackOf(stack) > 0.0D;
    }
}
