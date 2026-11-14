package com.hongmeng.xiuxian.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <b>攻击动作</b>：把"谁在挥什么武器"变成能看见的招式。
 *
 * <h2>参考了什么</h2>
 * <p>参考动作类 mod（Better Combat / Epic Fight）的<u>设计思路</u>，但<b>不引入任何
 * 外部动画库或素材</b> —— 全部用代码在 {@code PoseStack} 上做变换：</p>
 * <ul>
 *   <li><b>三段连击</b>：同一把武器连打三下换三种招式，不是同一动作重复播；</li>
 *   <li><b>武器分型</b>：剑横扫 / 杵重砸 / 刃突刺 / 双刀左右交替 / 轮刃旋身；</li>
 *   <li><b>蓄发两段</b>：重武器先"抬"再"砸"，有前摇后摇，不是匀速转；</li>
 *   <li><b>缓动</b>：用 easeOut / easeIn，不用线性插值 —— 线性看起来像机械臂。</li>
 * </ul>
 *
 * <h2>为什么改 PoseStack 而不是改模型骨骼</h2>
 * <p>原版 {@code PlayerRenderer} 在 {@code RenderPlayerEvent.Pre} 之后才调
 * {@code model.setupAnim(...)}，所以 Pre 里改手臂旋转会被<b>覆盖</b>；
 * 而改骨骼的正确钩子是 mixin（本仓库不用）。{@code PoseStack} 的变换则会
 * 一路带进后续渲染，所以这里做<b>整体姿态</b>（前冲 / 旋身 / 下沉 / 后仰），
 * 配合第一人称的手部变换，观感上已经能读出"招式"。</p>
 *
 * <h2>两类视角</h2>
 * <ul>
 *   <li><b>别人看你</b>（第三人称）：{@link #applyBody} 整体姿态；</li>
 *   <li><b>你自己看</b>（第一人称）：{@link #applyHand} 只动手里那把武器。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class CombatAnimClient {

    /** 一次攻击动作持续多少 tick（8 tick = 0.4 秒，接近原版挥砍节奏）。 */
    public static final int TICKS = 8;

    /** 武器分型（与服务端 {@code CombatAnim.kindOf} 一一对应）。 */
    public static final int KIND_SWORD = 0;   // 剑：横扫
    public static final int KIND_HEAVY = 1;   // 杵：举高重砸
    public static final int KIND_STAB = 2;    // 刃：直刺
    public static final int KIND_TWIN = 3;    // 双刀：左右交替
    public static final int KIND_RING = 4;    // 轮刃：旋身

    /** 一次动作的状态。 */
    private static final class Anim {
        final int kind;
        final int combo;
        final long startTick;

        Anim(int kind, int combo, long startTick) {
            this.kind = kind;
            this.combo = combo;
            this.startTick = startTick;
        }
    }

    private static final Map<UUID, Anim> ANIMS = new HashMap<>();

    /** 自己这次攻击的"主观"进度（第一人称用；不依赖服务端回包，手感 0 延迟）。 */
    private static long localStart = -1L;
    private static int localKind = KIND_SWORD;
    private static int localCombo = 0;

    private CombatAnimClient() {
    }

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------

    /** 服务端广播到达：记录某玩家的动作起点。 */
    public static void onAttack(UUID player, int kind, int combo) {
        if (player == null) {
            return;
        }
        ANIMS.put(player, new Anim(kind, combo, now()));
        // 剑气拖尾：把 UUID 换回实体（别人挥砍也要看到）
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null) {
                var p = mc.level.getPlayerByUUID(player);
                if (p != null) {
                    SlashTrailRenderer.start(p, kind, combo);
                }
            }
        } catch (Throwable ignored) {
            // 拖尾是纯表现，失败不能影响动作
        }
    }

    /** 自己发起攻击：立刻开始本地动作（第一人称不等回包，避免延迟感）。 */
    public static void onLocalAttack(int kind, int combo) {
        localKind = kind;
        localCombo = combo;
        localStart = now();
        // 自己这一下也立刻出拖尾（不等服务端回包）
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            SlashTrailRenderer.start(mc.player, kind, combo);
        } catch (Throwable ignored) {
            // 同上
        }
    }

    /** 当前 tick（拿不到世界时退回系统时间 / 50ms）。 */
    private static long now() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.level == null ? System.currentTimeMillis() / 50L
                : mc.level.getGameTime();
    }

    /** 动作进度 0~1；不在动作中返回 -1。 */
    private static float progressOf(long startTick, float partial) {
        long t = now();
        float done = (t - startTick) + partial;
        if (done < 0.0F || done > TICKS) {
            return -1.0F;
        }
        return done / (float) TICKS;
    }

    // ------------------------------------------------------------------
    // 缓动（线性插值看起来像机械臂，必须用缓动）
    // ------------------------------------------------------------------

    /** 快起慢收：适合"发力后惯性"。 */
    private static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    /** 慢起快收：适合"蓄力后爆发"。 */
    private static float easeIn(float t) {
        return t * t;
    }

    /** 先慢后快再收尾：重武器砸下去那一下。 */
    private static float easeInOut(float t) {
        return t < 0.5F ? 2.0F * t * t : 1.0F - 2.0F * (1.0F - t) * (1.0F - t);
    }

    // ------------------------------------------------------------------
    // 第三人称：整体姿态
    // ------------------------------------------------------------------

    /**
     * 在 {@code RenderPlayerEvent.Pre} 里调用：按动作阶段给整具身体加姿态。
     *
     * <p>调用方必须在 {@code Post} 里 {@code poseStack.popPose()}。
     * 返回 false 表示"这一帧没有动作"，调用方不要 push。</p>
     */
    public static boolean applyBody(Player player, PoseStack pose, float partial) {
        if (player == null || pose == null) {
            return false;
        }
        Anim a = ANIMS.get(player.getUUID());
        if (a == null) {
            return false;
        }
        float p = progressOf(a.startTick, partial);
        if (p < 0.0F) {
            ANIMS.remove(player.getUUID());
            return false;
        }
        pose.pushPose();
        switch (a.kind) {
            case KIND_HEAVY -> {
                // 举高 -> 砸下：前半段把身体拉高后仰，后半段猛地压下去前倾
                if (p < 0.42F) {
                    float k = easeOut(p / 0.42F);
                    pose.translate(0.0D, 0.10D * k, -0.06D * k);
                    pose.mulPose(com.mojang.math.Axis.XN.rotationDegrees(9.0F * k));
                } else {
                    float k = easeInOut((p - 0.42F) / 0.58F);
                    pose.translate(0.0D, -0.13D * k, 0.34D * k);
                    pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(17.0F * k));
                }
            }
            case KIND_STAB -> {
                // 直刺：先微后仰（蓄），再一路前冲
                float k = p < 0.25F ? -0.35F * easeOut(p / 0.25F)
                        : easeInOut((p - 0.25F) / 0.75F);
                pose.translate(0.0D, 0.0D, 0.52D * k);
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(11.0F * Math.max(0.0F, k)));
            }
            case KIND_SWORD -> {
                // 横扫：以腰为轴转体 + 轻微前倾
                float k = easeOut(p);
                pose.mulPose(com.mojang.math.Axis.YN.rotationDegrees(10.0F * k));
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(7.0F * k));
                pose.translate(-0.10D * k, 0.0D, 0.16D * k);
            }
            case KIND_TWIN -> {
                // 双刀：按连击序号左右交替，第三段双手同时收束
                float k = easeOut(p);
                int side = (a.combo % 2 == 0) ? 1 : -1;
                if (a.combo >= 2) {
                    // 这里原本叠了 Z 轴侧倾 12° —— 玩家反馈"打怪时身子会歪"，已去掉
                    pose.translate(0.0D, -0.05D * k, 0.22D * k);
                } else {
                    pose.mulPose(com.mojang.math.Axis.YN.rotationDegrees(24.0F * side * k));
                    pose.translate(0.12D * side * k, 0.0D, 0.20D * k);
                }
            }
            case KIND_RING -> {
                // 轮刃：整身旋一圈（分两段是为了不晕，转 180° 再回正）
                float k = easeInOut(p);
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F * k));
                pose.translate(0.0D, 0.04D * Mth.sin(p * Mth.PI), 0.10D * k);
            }
            default -> {
                float k = easeOut(p);
                pose.translate(0.0D, 0.0D, 0.12D * k);
            }
        }
        // 段位变体：让"三连击"看得出是三下不同的招
        // （双刀自己已经有左右交替，再叠变体也不会打架 —— 两者是不同轴）
        applyVariant(pose, a.combo, easeOut(p), 1.0F);
        return true;
    }

    // ------------------------------------------------------------------
    // 连招段位的招式变体
    // ------------------------------------------------------------------

    /**
     * 按连招段位给姿态叠加"这一下从哪来"。
     *
     * <p>为什么需要：在此之前只有双刀用段位做左右交替，
     * 其它武器<b>三段连招是同一个动作播三遍</b> —— 玩家看不出这是一套连击。</p>
     *
     * <p>变体只叠加<b>方向性</b>（横扫/竖劈/斜斩/突刺/回旋），
     * 不推翻各武器已有的基础动作，避免把手感改坏。</p>
     *
     * @param variant 连招段位（0 起）
     * @param k       0~1 的动作进度（已缓动）
     * @param scale   整体幅度（第一人称比第三人称小一点，否则会晃）
     */
    private static void applyVariant(PoseStack pose, int variant, float k,
                                     float scale) {
        if (k <= 0.0F) {
            return;
        }
        switch (Math.floorMod(variant, 5)) {
            case 0 -> {
                // 横扫：从右往左
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(26.0F * scale * k));
                pose.translate(-0.06D * scale * k, 0.0D, 0.10D * scale * k);
            }
            case 1 -> {
                // 竖劈：从上往下压
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(30.0F * scale * k));
                pose.translate(0.0D, -0.10D * scale * k, 0.08D * scale * k);
            }
            case 2 -> {
                // 斜斩：只做 Y + X 两轴（Z 侧倾会让身子看起来是歪的，已去掉）
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(20.0F * scale * k));
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(16.0F * scale * k));
                pose.translate(0.05D * scale * k, -0.04D * scale * k, 0.12D * scale * k);
            }
            case 3 -> {
                // 突刺：一路向前（不转太多，不然像挥砍）
                pose.translate(0.0D, 0.0D, 0.20D * scale * k);
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-8.0F * scale * k));
            }
            default -> {
                // 大回旋：整圈（终结技）
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(150.0F * scale * k));
                pose.translate(0.0D, 0.03D * scale * k, 0.06D * scale * k);
            }
        }
    }

    // ------------------------------------------------------------------
    // 第一人称：手里那把武器
    // ------------------------------------------------------------------

    /**
     * 在 {@code RenderHandEvent}（主手）里调用：把武器沿一条弧线甩出去再收回。
     *
     * <p>这段是<b>玩家自己最有感的</b> —— 第三人称的姿态别人看得到，
     * 而挥砍的手感全在这里。</p>
     */
    public static boolean applyHand(PoseStack pose, float partial) {
        if (localStart < 0L || pose == null) {
            return false;
        }
        float p = progressOf(localStart, partial);
        if (p < 0.0F) {
            localStart = -1L;
            return false;
        }
        pose.pushPose();
        float swing = easeOut(p);
        // 共同：前伸 + 收尾回位
        pose.translate(0.0D, 0.0D, -0.16D * Mth.sin(swing * Mth.PI));
        switch (localKind) {
            case KIND_HEAVY -> {
                // 重砸：先抬后落，落点在下方
                if (p < 0.42F) {
                    pose.translate(0.0D, 0.16D * easeOut(p / 0.42F), 0.0D);
                    pose.mulPose(com.mojang.math.Axis.XN.rotationDegrees(34.0F
                            * easeOut(p / 0.42F)));
                } else {
                    float k = easeIn((p - 0.42F) / 0.58F);
                    pose.translate(0.0D, -0.20D * k, 0.0D);
                    pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(56.0F * k));
                }
            }
            case KIND_STAB -> {
                // 直刺：沿 -Z 推出去
                pose.translate(0.0D, 0.02D, -0.34D * easeInOut(p));
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-6.0F * swing));
            }
            case KIND_SWORD -> {
                // 横扫：从左上到右下的一条弧
                pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(48.0F * swing));
                pose.mulPose(com.mojang.math.Axis.YN.rotationDegrees(30.0F * swing));
                pose.translate(-0.10D * swing, 0.04D * Mth.sin(p * Mth.PI), 0.0D);
            }
            case KIND_TWIN -> {
                // 双刀：左右交替甩
                int side = (localCombo % 2 == 0) ? 1 : -1;
                pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(40.0F * side * swing));
                pose.translate(0.12D * side * swing, 0.0D, 0.0D);
            }
            case KIND_RING -> {
                // 轮刃：绕自身转半圈
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(140.0F * easeInOut(p)));
            }
            default -> {
            }
        }
        // 段位变体（幅度收小：第一人称晃太狠会晕）
        applyVariant(pose, localCombo, swing, 0.62F);
        return true;
    }

    /** 掉线/切换世界时清账。 */
    public static void clear(UUID player) {
        if (player == null) {
            ANIMS.clear();
            SlashTrailRenderer.clear();
        } else {
            ANIMS.remove(player);
        }
    }
}
