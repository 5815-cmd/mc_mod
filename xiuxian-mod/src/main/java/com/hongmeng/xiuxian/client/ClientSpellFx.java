package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.network.SpellFxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>客户端法术贴图特效运行时</b>：收包 → 排队 → 每帧交给渲染器画。
 *
 * <p>玩家要求：「法术特效不仅仅粒子效果还有贴图表现……贴图配合粒子，
 * 不是纯贴图化。你像轨道炮就有一个激光柱那种的光波，四溢的能量粒子等，
 * 其他法术也要这种」。</p>
 *
 * <h2>分工</h2>
 * <ul>
 *   <li><b>贴图</b>（这里）：光柱、冲击环、穹顶、爆闪、巨剑、陨石、裂缝、
 *       雪花、花瓣旋涡、符文环 —— 提供"形状与体积"，1 个面片 1 个 draw call；</li>
 *   <li><b>粒子</b>（{@code SpellVfx} / {@code MagicCircles}）：做边缘的碎光、
 *       火星、尘埃 —— 提供"动态与随机感"。</li>
 * </ul>
 * 两者叠加才是玩家要的效果；只用其中一种都不够。
 */
public final class ClientSpellFx {

    /** 一条正在播放的贴图特效。 */
    public static final class Fx {
        public final SpellFxPacket packet;
        public int age;
        public final int life;
        /**
         * 是否已经给过镜头冲击。
         *
         * <p>冲击只在特效<b>刚出现的那一帧</b>给一次 ——
         * 持续抖会晕，而且会把伤害反馈的震动盖掉。</p>
         */
        public boolean shook;

        /**
         * PERF(C): 施法点正下方地面高度的缓存（NaN = 还没算过）。
         *
         * <p>地面裂纹/法阵/冰牢/血符这几类贴地特效，原来<b>每帧</b>都要
         * {@code level.clip(...)} 从原点向下打一次 8 格的射线来重新找地面。
         * 一条特效的寿命只有 2~5 秒、地面在这期间不会变，所以整个生命周期
         * 只需要算一次。缓存在渲染线程写、渲染线程读，没有并发问题。</p>
         */
        public double groundY = Double.NaN;

        Fx(SpellFxPacket packet) {
            this.packet = packet;
            this.life = Math.max(1, packet.life());
        }

        /** 0→1 的进度。 */
        public double progress(float partial) {
            return Math.min(1.0D, (age + partial) / life);
        }

        public boolean dead() {
            return age >= life;
        }
    }

    /** 上限：防止极端情况下堆积（超量时丢最旧的）。 */
    private static final int MAX_FX = 48;

    private static final List<Fx> ACTIVE = new ArrayList<>();

    private ClientSpellFx() {
    }

    /** 收包入口（由 {@code SpellFxPacket.handle} 反射调用）。 */
    public static void accept(SpellFxPacket p) {
        if (p == null || Minecraft.getInstance().level == null) {
            return;
        }
        // 画质档位：最低档直接不显示贴图特效（只留粒子）
        if (SpellFxQuality.level() <= 0) {
            return;
        }
        ACTIVE.add(new Fx(p));
        // 大尺寸特效顺手把视野拉远（玩家反馈"全在特效里看不见"）
        noteForCamera(p.size());
        while (ACTIVE.size() > MAX_FX) {
            ACTIVE.remove(0);
        }
    }

    /** 每客户端 tick 推进寿命。 */
    public static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Fx f = ACTIVE.get(i);
            f.age++;
            if (f.dead()) {
                ACTIVE.remove(i);
            }
        }
    }

    /** 供渲染器遍历。 */
    /**
     * 收到一条特效时调用：尺寸够大就顺手把视野拉远一点。
     *
     * <p>这样玩家不会"整个人埋在特效里"。</p>
     */
    public static void noteForCamera(float size) {
        if (size >= 2.0F) {
            CastZoom.begin(true);      // 灭世级：拉得很远
        } else if (size >= 1.0F) {
            CastZoom.begin(false);
        }
    }

    public static List<Fx> active() {
        return ACTIVE;
    }

    /** 玩家与特效的最大可见距离（超过就不画，省性能）。 */
    public static boolean visible(Vec3 at, double maxDist) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        // PERF(C): 直接用本地玩家的坐标算，不再 mc.player.position()（每次 new Vec3）
        double dx = mc.player.getX() - at.x;
        double dy = mc.player.getY() - at.y;
        double dz = mc.player.getZ() - at.z;
        return dx * dx + dy * dy + dz * dz <= maxDist * maxDist;
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
