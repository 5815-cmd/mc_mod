package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.network.SpellFxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Boss 出场：虚空裂缝 + 缓慢降落</b>（玩家要求）。
 *
 * <p>反射入口：{@code ClientBossEntrance.begin(Vec3, int)}（由
 * {@code BossEntrancePacket} 调用）—— 用反射是为了**专用服务器不链接客户端类**。</p>
 *
 * <p>玩家原话：「最终 boss 战场地然后有个祭坛，右击互动召唤 boss，boss 召唤特效什么的
 * 要对标法术特效，最后特效蓄力到最大可以是那种<b>改天换日、晴天霹雳</b>的效果带有压迫感，
 * boss 从<b>天空的虚空裂缝</b>，<b>缓慢出场降落</b>」。</p>
 *
 * <h2>出场序列（约 7 秒，分三段）</h2>
 * <ol>
 *   <li><b>0.0~1.5s 改天换日</b>：天空压暗（客户端做）、地面符文环亮起、
 *       雷声滚过（{@code CombatSounds.bossRoar} 已在服务端播）；</li>
 *   <li><b>1.5~3.0s 晴天霹雳</b>：天顶劈下一道<b>能量光柱</b>，
 *       顶上撕开一道<b>虚空裂缝</b>（用贴图，不是粒子堆的）；</li>
 *   <li><b>3.0~7.0s 缓慢降落</b>：裂缝里垂下一道引导光柱，boss 本体沿它**缓缓降下**，
 *       落地时一圈<b>冲击环 + 半球气浪 + 爆闪</b>。</li>
 * </ol>
 *
 * <p><b>为什么要做成"记录 + 每帧算位置"</b>：boss 的实体位置由服务端控制，
 * 但"从天上落下来"这个过程如果靠服务端每 tick 改坐标，会与寻路/碰撞打架。
 * 所以这里只在**客户端**做一段纯视觉的降落（把模型"抬"上去再放下来），
 * 服务端只负责在结束时把 boss 放到地面 —— 两边都干净。</p>
 */
public final class ClientBossEntrance {

    /** 一位正在出场的 boss。 */
    public static final class Entry {
        public final Vec3 at;
        public final int duration;
        public int age;

        Entry(Vec3 at, int duration) {
            this.at = at;
            this.duration = Math.max(20, duration);
        }

        public double t() {
            return Math.min(1.0D, (double) age / duration);
        }
    }

    private static final List<Entry> ACTIVE = new ArrayList<>();

    private ClientBossEntrance() {
    }

    /** 收到"boss 出场"时调用（由 BossEntrance 走 SpellFxPacket 之外的通道触发）。 */
    public static void begin(Vec3 at, int durationTicks) {
        if (at == null) {
            return;
        }
        ACTIVE.add(new Entry(at, durationTicks));
        if (ACTIVE.size() > 4) {
            ACTIVE.remove(0);
        }
    }

    /**
     * 每客户端 tick：推进进度，并按阶段派发贴图特效。
     *
     * <p>贴图特效是通过 {@link ClientSpellFx} 本地注入的 —— 这样
     * 不用为"出场"再多写一套渲染器，直接复用已经写好的 12 种形状。</p>
     */
    public static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Entry e = ACTIVE.get(i);
            e.age++;
            double t = e.t();
            Vec3 at = e.at;
            int el = com.hongmeng.xiuxian.spirit.Element.NONE.ordinal();

            // ---- 阶段①：地面符文环亮起（"改天换日"的起手） ----
            if (e.age == 2) {
                inject(SpellFxPacket.RUNE_RING, at, at, 26.0F, 0.0F, 70, el, 1.2F);
            }
            // ---- 阶段②：天顶劈下光柱 + 撕开虚空裂缝（"晴天霹雳"） ----
            if (e.age == 26) {
                Vec3 sky = at.add(0.0D, 56.0D, 0.0D);
                inject(SpellFxPacket.BEAM_COLUMN, sky, at, 7.0F, 0.0F, 60, el, 1.8F);
                inject(SpellFxPacket.CRACK_VOID, sky, sky, 16.0F, 0.0F, 120, el, 1.6F);
                inject(SpellFxPacket.IMPACT_FLARE, sky, sky, 14.0F, 0.0F, 40, el, 1.8F);
            }
            // ---- 阶段③：引导光柱（boss 沿它缓缓降下） ----
            if (e.age == 60) {
                Vec3 sky = at.add(0.0D, 44.0D, 0.0D);
                inject(SpellFxPacket.PILLAR_DROP, sky, at, 5.0F, 0.0F, 80, el, 1.3F);
            }
            // ---- 阶段④：落地：冲击环 + 穹顶 + 爆闪 ----
            if (e.age == e.duration - 12) {
                inject(SpellFxPacket.SHOCK_RING, at, at, 30.0F, 0.0F, 70, el, 1.6F);
                inject(SpellFxPacket.SHOCK_DOME, at, at, 22.0F, 0.0F, 60, el, 1.4F);
                inject(SpellFxPacket.IMPACT_FLARE, at, at, 16.0F, 0.0F, 45, el, 2.0F);
            }
            // ---- 全程：裂缝里往下飘的暗尘（粒子做点缀，不靠粒子撑场面） ----
            if (SpellFxQuality.particles() && e.age < e.duration && e.age % 3 == 0) {
                for (int k = 0; k < 3; k++) {
                    double y = at.y + 6.0D + (e.duration - e.age) * 0.35D + k * 2.0D;
                    mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            at.x + (mc.level.random.nextDouble() - 0.5D) * 5.0D,
                            y, at.z + (mc.level.random.nextDouble() - 0.5D) * 5.0D,
                            0.0D, -0.09D, 0.0D);
                }
            }
            if (e.age >= e.duration) {
                ACTIVE.remove(i);
            }
        }
    }

    /** 本地注入一条贴图特效（复用法术那套渲染器）。 */
    private static void inject(byte shape, Vec3 from, Vec3 to, float size, float length,
                               int life, int element, float power) {
        ClientSpellFx.accept(new SpellFxPacket(shape, from, to, size, length, life,
                element, power));
    }

    /**
     * <b>降落位移</b>：把 boss 的渲染位置抬高一段，做出"缓缓降下"。
     *
     * @param at       boss 的落地位置
     * @return 当前应抬高的格数（0 = 已经落地）
     */
    public static double liftFor(Vec3 at) {
        for (Entry e : ACTIVE) {
            if (e.at.distanceToSqr(at) < 36.0D) {
                double t = e.t();
                if (t < 0.45D) {
                    return 46.0D;                       // 裂缝未开，还在天上
                }
                if (t < 0.90D) {
                    // 45% → 90% 是降落段：从 46 格缓降到 0（用平滑曲线，不是匀速）
                    double k = (t - 0.45D) / 0.45D;
                    double smooth = k * k * (3.0D - 2.0D * k);   // smoothstep
                    return 46.0D * (1.0D - smooth);
                }
                return 0.0D;
            }
        }
        return 0.0D;
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
