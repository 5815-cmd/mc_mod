package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.domain.DomainData;
import com.hongmeng.xiuxian.domain.DomainEffectTable;
import com.hongmeng.xiuxian.domain.DomainField;
import com.hongmeng.xiuxian.network.ClientCultivationCache;
import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

/**
 * 本命领域的<b>客户端特效</b>。
 *
 * <p>规格 §五 的硬约束：<b>不下载任何网上素材</b>，特效全部用原版粒子拼。
 * 所以这里一个贴图都没有，只有 {@link ParticleTypes} 里现成的东西 + 玩家元素色
 * 的 {@link DustParticleOptions}（红色石粉粒子可以带任意 RGB，正好拿来当"灵气"）。</p>
 *
 * <h2>为什么特效在客户端自己算</h2>
 *
 * <p>领域数据（哪座道宫、什么元素、开没开）已经跟着 {@code SyncCultivationPacket}
 * 同步到客户端了，所以客户端不需要任何新包就能画出正确的特效：每台客户端自己画
 * 自己看到的。要发一个"玩家在某处放了领域"的包反而会出现"别人看不到你领域"的问题。</p>
 *
 * <h2>为什么只有粒子、没有自定义渲染</h2>
 *
 * <p>水是"涨潮"，但<b>一个方块都不改</b>（规格 §3.3：不破坏物品、不影响火把与作物）。
 * 所以"水位上升"只能靠粒子表现：脚下铺一层水花、头顶落雨幕。</p>
 *
 * <h2>密度</h2>
 *
 * <p>领域有 96×96 格（{@code domainChunks} 可调），逐格铺满粒子会卡死。
 * 这里只在玩家周围 {@value #NEAR_RADIUS} 格内密集画，另外按很低概率在领域边界
 * 附近点几颗——"看得见边界"和"跑得动"之间的折中。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class DomainClientVfx {

    /** 密集绘制半径（格）。R2：20 → 13（玩家 2026-11-07「领域特效也是」再降一档）。 */
    private static final double NEAR_RADIUS = 13.0D;

    /**
     * 灵气层周期（tick）。
     *
     * <p>R2 下调：玩家 2026-11-07「法术特效还是下调，领域特效也是」。
     * 从每 tick 一层（1）放宽到每 4 tick 一层，粒子量约降到 1/4；
     * 仍然每 4 tick 都有一层灵气，观感是"持续浓郁"而不是"断流"。</p>
     */
    private static final int AURA_PERIOD = 4;

    /** R2：每 tick 额外补的元素粒子数。8 → 2（只保留"脚下有气"的最小观感）。 */
    private static final int DENSE_PER_TICK = 2;

    private final RandomSource random = RandomSource.create();

    /** 上一 tick 领域是否开着（捕捉"刚开启"那一瞬）。 */
    private boolean domainWasActive;

    /**
     * 领域**刚开启**时铺三层巨大法阵（玩家要的"开领域像开了一个空间"）。
     *
     * <p>客户端本来就知道自己领域开没开，所以不需要新增网络包；
     * 只在"关 → 开"的那个边沿生成一次。</p>
     */
    private void tickDomainCircles() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            // 用项目既有的缓存读领域状态（与下面 onClientTick 里的读法一致）
            com.hongmeng.xiuxian.domain.DomainData domains =
                    com.hongmeng.xiuxian.network.ClientCultivationCache.get().getDomainData();
            boolean active = domains.isDomainActive() && domains.domainCount() > 0;
            if (active && !domainWasActive) {
                com.hongmeng.xiuxian.spirit.Element el = domains.domains().isEmpty()
                        ? com.hongmeng.xiuxian.spirit.Element.NONE
                        : domains.domains().get(0).element();
                com.hongmeng.xiuxian.client.MagicCircles.domain(mc.player.position(), el);
            }
            domainWasActive = active;
        } catch (Throwable ignored) {
            // 表现层绝不影响游戏
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickDomainCircles();
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.isPaused()) {
            return;
        }
        CultivationData data = ClientCultivationCache.get();
        DomainData domains = data.getDomainData();
        if (!domains.isDomainActive() || domains.domainCount() == 0) {
            return;
        }
        for (DomainData.Domain domain : domains.domains()) {
            Element element = domain.element();
            if (!DomainEffectTable.hasEffect(element)) {
                continue;
            }
            emit(level, player, element);
        }
    }

    /** 按元素画一帧。 */
    private void emit(ClientLevel level, LocalPlayer player, Element element) {
        int tick = player.tickCount;
        // 灵气：所有元素共有的一层"元素色"微粒（道宫品质越高越亮）
        if (tick % AURA_PERIOD == 0) {
            aura(level, player, element);
        }

        // ---- 浓郁化层：每 tick 在玩家周围铺一圈元素粒子 ----
        //
        // 玩家反馈「特效稀薄」「要浓郁一点要看得出来」：原来每种元素每 2~7 tick
        // 才发 1~2 个粒子，远远看不出"领域开着"。这里补一层**每 tick 都有**的
        // 元素色粒子，铺在 NEAR_RADIUS 范围内、贴地不同高度上，
        // 让玩家一眼就能看出脚下这片地"属于自己"。
        //
        // R2（2026-11-07「领域特效也是」）：这一层从 8 颗/tick 降到 2 颗/tick。
        //
        // 全部走客户端本地粒子（level.addParticle），**不占服务端带宽**，
        // 也受 vfx.particleDensity 那个总旋钮控制——觉得卡就往下调。
        for (int i = 0; i < DENSE_PER_TICK; i++) {
            double[] q = ring(level, player, NEAR_RADIUS, 3.0D);
            double gy = ground(level, q[0], q[2]);
            level.addParticle(edgeParticle(element),
                    q[0], gy + 0.25D + (i % 4) * 0.55D, q[2],
                    0.0D, 0.015D, 0.0D);
        }

        switch (element) {
            case FIRE -> {
                if (tick % 4 == 0) {
                    // 天降火雨：头顶落火（R2：2 颗/次 → 1 颗，只留可辨识的火雨）
                    double[] p = ring(level, player, NEAR_RADIUS, 6.0D);
                    level.addParticle(ParticleTypes.FLAME, p[0], p[1] + 6.0D, p[2],
                            0.0D, -0.35D, 0.0D);
                }
                if (tick % 10 == 0) {
                    double[] p = ring(level, player, NEAR_RADIUS, 2.0D);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, p[0],
                            ground(level, p[0], p[2]) + 0.2D, p[2], 0.0D, 0.03D, 0.0D);
                }
            }
            case WOOD -> {
                if (tick % 6 == 0) {
                    // 藤蔓破土：贴地冒绿光（R2：一次 2 颗 → 1 颗）
                    double[] p = ring(level, player, NEAR_RADIUS, 1.0D);
                    double y = ground(level, p[0], p[2]);
                    level.addParticle(ParticleTypes.COMPOSTER, p[0], y + 0.1D, p[2],
                            0.0D, 0.05D, 0.0D);
                }
                if (tick % 14 == 0) {
                    // 偶尔一根木刺窜出来
                    double[] p = ring(level, player, NEAR_RADIUS * 0.6D, 0.5D);
                    level.addParticle(ParticleTypes.CRIT, p[0],
                            ground(level, p[0], p[2]) + 0.3D, p[2], 0.0D, 0.25D, 0.0D);
                }
            }
            case WATER -> {
                // 涨潮 + 雨幕：纯粒子，绝不碰方块
                if (tick % 4 == 0) {
                    double[] p = ring(level, player, NEAR_RADIUS, 5.0D);
                    level.addParticle(ParticleTypes.SPLASH, p[0],
                            ground(level, p[0], p[2]) + 0.1D, p[2], 0.0D, 0.0D, 0.0D);
                    // 雨幕（玩家要求「开启水的领域要下雨」）：R2 从每 2 tick 6 滴
                    // 降到每 4 tick 2 滴，仍是"一直在下雨"的最小观感。
                    for (int r = 0; r < 2; r++) {
                        double[] rp = ring(level, player, NEAR_RADIUS, 12.0D);
                        level.addParticle(ParticleTypes.FALLING_WATER,
                                rp[0], rp[1] + 10.0D, rp[2], 0.0D, -0.8D, 0.0D);
                    }
                }
                if (tick % 12 == 0) {
                    double[] p = ring(level, player, NEAR_RADIUS, 1.0D);
                    level.addParticle(ParticleTypes.BUBBLE, p[0],
                            ground(level, p[0], p[2]) + 0.2D, p[2], 0.0D, 0.02D, 0.0D);
                }
            }
            case METAL -> {
                if (tick % 4 == 0) {
                    // 万千细刃虚影：绕着玩家的切割线
                    double[] p = ring(level, player, NEAR_RADIUS * 0.8D, 1.0D);
                    level.addParticle(ParticleTypes.SWEEP_ATTACK, p[0], p[1], p[2],
                            0.0D, 0.0D, 0.0D);
                }
                if (tick % 8 == 0) {
                    double[] p = ring(level, player, NEAR_RADIUS, 1.0D);
                    level.addParticle(ParticleTypes.ENCHANTED_HIT, p[0], p[1] + 0.5D, p[2],
                            0.0D, 0.0D, 0.0D);
                }
            }
            case EARTH -> {
                if (tick % 6 == 0) {
                    // 沙尘（R2：沙尘 + 烟 2 颗/次 → 1 颗，只留可辨识的灰）
                    double[] p = ring(level, player, NEAR_RADIUS, 0.5D);
                    double y = ground(level, p[0], p[2]);
                    level.addParticle(ParticleTypes.ASH, p[0], y + 0.1D, p[2],
                            0.0D, 0.03D, 0.0D);
                }
            }
            case ICE -> {
                if (tick % 4 == 0) {
                    // 霜花蔓延 + 冰晶落下（R2：2 颗/次 → 1 颗）
                    double[] p = ring(level, player, NEAR_RADIUS, 0.5D);
                    level.addParticle(ParticleTypes.SNOWFLAKE, p[0], p[1] + 3.0D, p[2],
                            0.0D, -0.08D, 0.0D);
                }
            }
            case THUNDER -> {
                if (tick % 8 == 0) {
                    // 雷云 + 电弧：头顶一颗火花，偶尔一记闪
                    double[] p = ring(level, player, NEAR_RADIUS, 10.0D);
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, p[0], p[1] + 4.0D, p[2],
                            0.0D, -0.1D, 0.0D);
                }
                if (tick % 40 == 0) {
                    double[] p = ring(level, player, NEAR_RADIUS, 12.0D);
                    level.addParticle(ParticleTypes.END_ROD, p[0], p[1] + 5.0D, p[2],
                            0.0D, 0.0D, 0.0D);
                }
            }
            case WIND -> {
                if (tick % 4 == 0) {
                    // 风刃轨迹（R2：风刃 + 云 2 颗/次 → 1 颗，只留可辨识的风刃）
                    double[] p = ring(level, player, NEAR_RADIUS, 1.0D);
                    level.addParticle(ParticleTypes.SWEEP_ATTACK, p[0], p[1], p[2],
                            0.0D, 0.0D, 0.0D);
                }
                if (tick % 10 == 0) {
                    double[] p = ring(level, player, NEAR_RADIUS, 0.5D);
                    level.addParticle(ParticleTypes.CRIT, p[0], p[1], p[2],
                            0.05D, 0.0D, 0.05D);
                }
            }
            default -> {
                // 无属性道宫没有领域特效
            }
        }
    }

    /** 元素色的灵气微粒（红石粉粒子可带任意颜色）。 */
    private void aura(ClientLevel level, LocalPlayer player, Element element) {
        int rgb = element.color();
        Vector3f color = new Vector3f(
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F);
        DustParticleOptions dust = new DustParticleOptions(color, 1.0F);
        double[] p = ring(level, player, NEAR_RADIUS * 0.75D, 0.0D);
        level.addParticle(dust, p[0], p[1] + random.nextDouble() * 1.5D, p[2],
                0.0D, 0.01D, 0.0D);

        // 领域边界：很低概率点一颗，让玩家隐约看出范围有多大
        // R2：1/6 → 1/12，边界提示再减半。
        if (random.nextInt(12) == 0) {
            double edge = DomainField.halfBlocks() * (0.85D + random.nextDouble() * 0.15D);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double x = player.getX() + Math.cos(angle) * edge;
            double z = player.getZ() + Math.sin(angle) * edge;
            level.addParticle(edgeParticle(element), x, player.getY() + 1.0D, z,
                    0.0D, 0.01D, 0.0D);
        }
    }

    private static ParticleOptions edgeParticle(Element element) {
        return switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case WOOD -> ParticleTypes.HAPPY_VILLAGER;
            case WATER -> ParticleTypes.SPLASH;
            case METAL -> ParticleTypes.END_ROD;
            case EARTH -> ParticleTypes.ASH;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
            case WIND -> ParticleTypes.CLOUD;
            default -> ParticleTypes.END_ROD;
        };
    }

    /** 玩家周围随机一点（附带一点高度抖动），返回 {@code {x, y, z}}。 */
    private double[] ring(ClientLevel level, LocalPlayer player, double radius, double height) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = Math.sqrt(random.nextDouble()) * radius;
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        double y = player.getY() + (height > 0.0D
                ? random.nextDouble() * height : random.nextDouble() * 2.0D);
        return new double[]{x, y, z};
    }

    /** 该水平位置的地面高度（找不到就退回玩家脚下）。 */
    private static double ground(ClientLevel level, double x, double z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(z));
    }
}
