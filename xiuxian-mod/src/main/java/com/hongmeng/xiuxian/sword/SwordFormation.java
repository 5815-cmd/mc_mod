package com.hongmeng.xiuxian.sword;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * <b>飞剑剑阵</b> —— 数据层（纯逻辑，可在服务端断言）。
 *
 * <h2>学自哪里</h2>
 * <p>玩家发了一个修仙 mod 的视频，说「看看人家的飞剑可以环绕自己，学习一下人家的玩法」。
 * 查到该玩法出自 <b>御剑工艺（Yujian Craft）</b>，其设计要点（MC百科词条原文提炼）：</p>
 * <ul>
 *   <li><b>六器剑阵</b>：最多同时装配 6 把飞剑，<b>每把独立冷却</b>，
 *       剑阵<b>随玩家移动</b>并<b>根据目标调整位置</b>；</li>
 *   <li><b>阵型可切</b>（前向扇阵 / 环形阵），战斗中切换；</li>
 *   <li><b>索敌</b>：准心锁定后标记<b>持续到目标死亡</b>，也可自动索敌；</li>
 *   <li><b>术式</b>：穿刺 / 环斩 / 剑阵 / 护阵 等；</li>
 *   <li><b>灵剑指引</b>：能在原版近战范围之外操纵整套剑阵。</li>
 * </ul>
 *
 * <h2>这个类做什么</h2>
 * <p>只做<b>纯计算</b>（不碰 Minecraft 世界与网络），所以它可以被离线验证器直接断言：</p>
 * <ul>
 *   <li>{@link #phaseOf(int, long)} —— 每把剑的环绕相位（含错开，免得六把叠在一起）；</li>
 *   <li>{@link #orbitPos} —— 环绕位置（环阵 / 扇阵 / 散阵三种排法）；</li>
 *   <li>{@link #swordCountFor(int)} —— 按境界解锁剑数（学它的"逐把解锁"节奏）；</li>
 *   <li>{@link #ATTACK_COOLDOWN} —— 每把剑自己的攻击冷却。</li>
 * </ul>
 *
 * <h2>为什么"每把剑独立冷却"很重要</h2>
 * <p>如果六把剑共用冷却，它们会<b>同时</b>飞出去 —— 看起来像一次性爆发，
 * 而不是"剑阵持续压制"。独立冷却之后是<b>此起彼伏</b>的连续穿刺，
 * 这才是"剑阵"的手感。这一点是照抄它的设计。</p>
 */
public final class SwordFormation {

    private SwordFormation() {
    }

    /** 最多几把飞剑。 */
    public static final int MAX_SWORDS = 6;

    /** 每把剑自己的攻击冷却（tick）。 */
    public static final int ATTACK_COOLDOWN = 26;

    /** 一次穿刺的飞行时间（tick）——这段时间剑"离开"环绕位去扎目标。 */
    public static final int PIERCE_TICKS = 8;

    /** 环形阵的环绕半径（格）。 */
    public static final double RING_RADIUS = 2.2D;

    /** 剑阵展开的高度（相对玩家脚）。 */
    public static final double ORBIT_HEIGHT = 1.25D;

    /** 索敌半径（格）——"远距离索敌"是它的卖点之一，所以给得比近战远得多。 */
    public static final double LOCK_RANGE = 22.0D;

    /** 阵型。 */
    public enum Pattern {
        /** <b>环形阵</b>：均匀绕一圈（默认，最适合"环绕自己"的观感）。 */
        RING("环形阵"),
        /** <b>前向扇阵</b>：集中在玩家正前方一个扇面（利于集火）。 */
        FAN("前向扇阵"),
        /** <b>天罡阵</b>：上下两层错开（做出"立体剑阵"的感觉）。 */
        HEAVEN("天罡阵");

        private final String cn;

        Pattern(String cn) {
            this.cn = cn;
        }

        public String cn() {
            return this.cn;
        }

        /** 下一个阵型（战斗中切换用）。 */
        public Pattern next() {
            Pattern[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    /**
     * 按境界解锁剑数。
     *
     * <p>学它的"逐把解锁"节奏：一开始只有一两把，随着修为增长逐步到六把。
     * 这样"剑阵变强"是**肉眼可见**的成长反馈，而不是一次性给满。</p>
     *
     * <table border="1">
     *   <caption>境界 → 剑数</caption>
     *   <tr><th>境界</th><th>剑数</th></tr>
     *   <tr><td>筑基(2) 以下</td><td>0（还没资格御剑）</td></tr>
     *   <tr><td>筑基(2)</td><td>2</td></tr>
     *   <tr><td>金丹(3)</td><td>3</td></tr>
     *   <tr><td>元婴(4)</td><td>4</td></tr>
     *   <tr><td>化神(5)</td><td>5</td></tr>
     *   <tr><td>道印(6) 及以上</td><td>6</td></tr>
     * </table>
     */
    public static int swordCountFor(int realmTier) {
        if (realmTier < 2) {
            return 0;
        }
        return Math.min(MAX_SWORDS, realmTier);
    }

    /** 能否御剑（筑基起）。 */
    public static boolean canUse(int realmTier) {
        return swordCountFor(realmTier) > 0;
    }

    /**
     * 第 {@code index} 把剑的环绕相位（弧度）。
     *
     * <p>两种阵型的相位不同 —— 这就是"阵型切换"的实现核心：</p>
     * <ul>
     *   <li>{@code RING}：均匀分布整圈；</li>
     *   <li>{@code FAN}：压缩到玩家正前方 ±55 度；</li>
     *   <li>{@code HEAVEN}：均匀分布，但奇数把抬高一层（见 {@link #heightOf}）。</li>
     * </ul>
     *
     * @param index  第几把（0 起）
     * @param count  总数
     * @param pattern 阵型
     * @param time   游戏刻（用于整体缓慢旋转）
     */
    public static float phaseOf(int index, int count, Pattern pattern, long time) {
        int n = Math.max(1, count);
        // 整体缓慢自转：让剑阵"活"起来（否则六把剑像钉死的）
        float spin = time * 0.035F;
        if (pattern == Pattern.FAN) {
            // 前向扇阵：±55 度内均匀铺开（0 弧度 = 玩家正前方 +Z）
            float half = (float) Math.toRadians(55.0D);
            float t = n == 1 ? 0.0F : (float) index / (n - 1) * 2.0F - 1.0F;
            return t * half + spin * 0.35F;
        }
        return (float) (Math.PI * 2.0D * index / n) + spin;
    }

    /** 第 {@code index} 把剑的高度偏移（天罡阵分两层）。 */
    public static double heightOf(int index, Pattern pattern) {
        if (pattern != Pattern.HEAVEN) {
            return 0.0D;
        }
        return (index % 2 == 0) ? 0.0D : 0.85D;
    }

    /**
     * 第 {@code index} 把剑的环绕世界坐标。
     *
     * @param center  玩家位置（脚）
     * @param look    玩家朝向（用来让扇阵跟着视线转）
     */
    public static Vec3 orbitPos(Vec3 center, Vec3 look, int index, int count,
                                Pattern pattern, long time) {
        float phase = phaseOf(index, count, pattern, time);
        // 扇阵要跟着"玩家看哪"转；环阵不需要
        double baseYaw = (pattern == Pattern.FAN) ? Math.atan2(look.z, look.x) - Math.PI / 2.0D
                : 0.0D;
        double a = phase + baseYaw;
        double radius = RING_RADIUS + ((index % 2 == 0) ? 0.0D : 0.22D);
        return new Vec3(
                center.x + Math.cos(a) * radius,
                center.y + ORBIT_HEIGHT + heightOf(index, pattern),
                center.z + Math.sin(a) * radius);
    }

    /**
     * 这次能不能打（每把剑独立冷却）。
     *
     * @param lastAttackTick 该剑上次攻击的刻；从未攻击过传 {@code Long.MIN_VALUE}
     */
    public static boolean ready(long now, long lastAttackTick) {
        return lastAttackTick == Long.MIN_VALUE
                || now - lastAttackTick >= ATTACK_COOLDOWN;
    }

    /**
     * 穿刺的飞行插值位置（0 → 1：从环绕位飞到目标）。
     *
     * <p>用 smoothstep 而不是线性 —— 线性看起来像"匀速平移"，
     * 缓入缓出才有"剑刺出去"的爆发感。</p>
     *
     * @param from  环绕位
     * @param to    目标
     * @param t     0~1 进度
     */
    public static Vec3 piercePos(Vec3 from, Vec3 to, double t) {
        double c = Mth.clamp(t, 0.0D, 1.0D);
        double e = c * c * (3.0D - 2.0D * c);      // smoothstep
        return from.add(to.subtract(from).scale(e));
    }

    /**
     * 六剑齐备时的"太虚剑阵"是否可发动。
     *
     * <p>学它的规则：「当六把飞剑全部冷却完成后……即可发动」。</p>
     */
    public static boolean ultimateReady(int count, long now, long[] lastAttack) {
        if (count < MAX_SWORDS || lastAttack == null || lastAttack.length < MAX_SWORDS) {
            return false;
        }
        for (int i = 0; i < MAX_SWORDS; i++) {
            if (!ready(now, lastAttack[i])) {
                return false;
            }
        }
        return true;
    }
}
