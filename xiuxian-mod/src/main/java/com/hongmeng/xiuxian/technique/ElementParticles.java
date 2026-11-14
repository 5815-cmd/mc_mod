package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * <b>元素粒子层</b>：主粒子（材质）+ 辅粒子（点缀）。
 *
 * <h2>为什么要两层</h2>
 * <p>单一粒子看起来很"平"：火只有 {@code FLAME}、木只有 {@code HAPPY_VILLAGER}，
 * 缺少层次。好看的特效都是两种以上叠加 ——
 * <b>主粒子给材质，辅粒子给细节</b>。</p>
 *
 * <h2>为什么辅粒子必须同色系</h2>
 * <p>元素的辨识度来自<b>颜色</b>（金=金、风=青、雷=电光白）。
 * 辅粒子只加"层次"不改"色相"，否则玩家会分不清这是哪个元素的法术。</p>
 *
 * <h2>为什么用 ParticleBatch</h2>
 * <p>一次施法动辄几十颗粒子。逐颗 {@code sendParticles} 会让包数爆炸
 * （项目里清掉过 30 处这种写法）。这里每 24 颗合一个包。</p>
 */
public final class ElementParticles {

    private ElementParticles() {
    }

    /** 主粒子（沿用 {@link SpellVfx#particleOf} 的既定配色）。 */
    public static ParticleOptions primary(Element element) {
        return SpellVfx.particleOf(element);
    }

    /**
     * 辅粒子：同色系内的第二种，用来加层次。
     */
    public static ParticleOptions accent(Element element) {
        if (element == null) {
            return ParticleTypes.ENCHANT;
        }
        return switch (element) {
            case FIRE -> ParticleTypes.LAVA;              // 熔岩滴：比火焰更重
            case WATER -> ParticleTypes.BUBBLE;           // 气泡：水里的空气
            case ICE -> ParticleTypes.END_ROD;            // 冷光：冰的锐利反光
            case THUNDER -> ParticleTypes.END_ROD;        // 电光白
            case WIND -> ParticleTypes.SWEEP_ATTACK;      // 风刃划痕
            case EARTH -> ParticleTypes.ASH;              // 尘
            case WOOD -> ParticleTypes.SPORE_BLOSSOM_AIR; // 花粉
            case METAL -> ParticleTypes.CRIT;             // 金属火星
            default -> ParticleTypes.ENCHANT;
        };
    }

    /**
     * 在 {@code center} 打一圈主粒子 + 内圈辅粒子（**批量发包**）。
     *
     * @param radius 主环半径
     * @param count  主环颗数（辅环取 1/3）
     */
    public static void burst(ServerLevel level, Element element, Vec3 center,
                             double radius, int count) {
        if (level == null || center == null || count <= 0) {
            return;
        }
        ParticleBatch pb = new ParticleBatch(level);
        ParticleOptions main = primary(element);
        ParticleOptions acc = accent(element);
        int n = Math.min(count, 96);
        for (int i = 0; i < n; i++) {
            double a = 2.0D * Math.PI * i / n;
            pb.add(main, center.x + Math.cos(a) * radius,
                    center.y + 0.25D, center.z + Math.sin(a) * radius);
        }
        int m = Math.max(4, n / 3);
        for (int i = 0; i < m; i++) {
            double a = 2.0D * Math.PI * i / m + Math.PI / m;   // 错开半格相位
            pb.add(acc, center.x + Math.cos(a) * radius * 0.62D,
                    center.y + 0.55D, center.z + Math.sin(a) * radius * 0.62D);
        }
        pb.flush();
    }

    /**
     * 升级版：双层反向环 + 中心柱。
     *
     * <p>比 {@link #burst} 更"贵"，用于灭世级。</p>
     */
    public static void grandBurst(ServerLevel level, Element element, Vec3 center,
                                  double radius, int count) {
        if (level == null || center == null || count <= 0) {
            return;
        }
        ParticleBatch pb = new ParticleBatch(level);
        ParticleOptions main = primary(element);
        ParticleOptions acc = accent(element);
        int n = Math.min(count, 96);
        for (int i = 0; i < n; i++) {
            double a = 2.0D * Math.PI * i / n;
            pb.add(main, center.x + Math.cos(a) * radius,
                    center.y + 0.2D, center.z + Math.sin(a) * radius);
            // 外圈更高、半径更大（第二层）
            // 减量：每 2 颗主粒子才配 1 颗辅粒子（原来 1:1，总颗粒减约 1/4）
            if (i % 2 == 0) {
                pb.add(acc, center.x + Math.cos(-a) * radius * 1.45D,
                        center.y + 1.1D, center.z + Math.sin(-a) * radius * 1.45D);
            }
        }
        // 中心光柱（减量：k < 8 的循环保留，只加偶数下标 → 8 颗降为 4 颗）
        for (int k = 0; k < 8; k++) {
            if (k % 2 == 0) {
                pb.add(main, center.x, center.y + 0.5D + k * 0.55D, center.z);
            }
        }
        pb.flush();
    }
}
