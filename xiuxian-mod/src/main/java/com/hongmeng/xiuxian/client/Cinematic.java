package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>演出镜头（过场动画）—— 客户端状态与插值</b>。
 *
 * <h2>参考了哪些做法</h2>
 * <p>玩家要求「BOSS 出场也要做出场动画，影视级别的运镜和特效」，
 * 并让我参考灾变（Cataclysm）那套过场。查到的关键事实：</p>
 * <ul>
 *   <li>灾变的过场模组（<b>Cinematic Cataclysm</b>）把过场做成<b>实体</b>
 *       （{@code cinematiccataclysm:scylla_cutscene} 之类），
 *       集成包必须把它们加进实体 tick 剔除白名单，否则
 *       <b>「服务器与客户端之间出现严重不同步，过场动画无法正常运行」</b>；
 *       —— 说明它是<b>服务端驱动、按 tick 同步给客户端</b>的，不是纯客户端演出。</li>
 *   <li>过场期间玩家<b>无敌且无法被选中</b>；对附近所有玩家触发，
 *       但<b>只有生存模式</b>才被传送。</li>
 *   <li>{@code ImmersiveCinematics} 的技术要点是
 *       <b>关键帧相机 + 贝塞尔曲线</b>插值。</li>
 * </ul>
 *
 * <p>所以这里照同样的路子做：<b>服务端算好整段镜头的关键帧</b>，
 * 一次发给客户端；客户端只做<b>插值</b>与呈现。
 * 这样不会出现"客户端自己演、服务端不知道"的失同步，
 * 也不需要 mixin —— 只需挂 Forge 的 {@code ViewportEvent} 钩子。</p>
 *
 * <h2>镜头怎么算</h2>
 * <p>每个关键帧描述的是<b>相机相对于"锚点"（一般是 boss）的位置</b>：
 * 球坐标 {@code (距离, 水平角, 俯仰角)} + 看向的点 + 视场角。
 * 这样即使 boss 在动，镜头也会跟着它（自动跟随的运镜）。</p>
 */
public final class Cinematic {

    private Cinematic() {
    }

    /**
     * 一个镜头关键帧。
     *
     * @param dist   相机到锚点的距离（格）
     * @param yawDeg 水平角（度，0 = +Z 方向）
     * @param pitchDeg 俯仰角（度，正数 = 更靠下往上拍）
     * @param lookY  看向锚点的哪个高度（相对锚点脚的偏移）
     * @param fov    视场角（度；比默认 70 小 = 长焦压缩感）
     */
    public record Shot(float dist, float yawDeg, float pitchDeg, float lookY, float fov) {
    }

    /** 正在播放的演出。 */
    private static final class Playing {
        final Vec3 anchor;
        final List<Shot> shots;
        final long startTick;
        final int totalTicks;
        final String title;
        final String subtitle;

        Playing(Vec3 anchor, List<Shot> shots, long startTick, int totalTicks,
                String title, String subtitle) {
            this.anchor = anchor;
            this.shots = shots;
            this.startTick = startTick;
            this.totalTicks = totalTicks;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private static Playing current;
    /** 服务器发来的"现在还在演"的心跳（防止客户端演出比服务端长）。 */
    private static long lastServerTick;

    // ------------------------------------------------------------------
    // 服务端 → 客户端
    // ------------------------------------------------------------------

    public static void begin(Vec3 anchor, float[] packed, int totalTicks,
                             String title, String subtitle) {
        List<Shot> shots = new ArrayList<>();
        for (int i = 0; i + 4 < packed.length; i += 5) {
            shots.add(new Shot(packed[i], packed[i + 1], packed[i + 2],
                    packed[i + 3], packed[i + 4]));
        }
        if (shots.size() < 2) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        current = new Playing(anchor, shots, now, totalTicks, title, subtitle);
        lastServerTick = now;
        // 演出期间**隐藏 HUD**（等同按 F1）—— 玩家要求：
        // 「移动镜头自动开启 f1 的那个界面，移动完关闭」。
        // 记住原值，演出结束严格还原（玩家本来就是 F1 状态的话不会被改坏）。
        if (mc.options != null) {
            hudWasHidden = mc.options.hideGui;
            mc.options.hideGui = true;
        }
        HongmengMod.LOGGER.debug("[cinematic] 开始演出：{}（{} 个关键帧 / {} tick）",
                title, shots.size(), totalTicks);
    }

    /**
     * 演出结束时把镜头还给玩家。
     *
     * <p>⚠ 我们移动的是<b>客户端</b>的玩家坐标（为了运镜），
     * 所以结束时要把它放回"服务端认定的位置" —— 否则镜头一收，
     * 玩家会突然"弹"回原处，或者停在半空。</p>
     *
     * <p>做法：直接让客户端以服务端最后同步的位置为准。
     * Minecraft 每 tick 会从服务端收位置包，所以最稳的收场是
     * <b>把玩家坐标设回演出开始前记下的那个位置</b>。</p>
     */
    public static void restorePlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || current == null) {
            return;
        }
        Vec3 back = current.anchor;
        mc.player.setPos(back.x, back.y, back.z);
        mc.player.setDeltaMovement(Vec3.ZERO);
    }

    public static void end() {
        current = null;
        // 演出结束：把 HUD 还原成演出前的状态。
        //
        // ⚠ 必须还原成**原来的值**而不是一律 false：
        //   玩家自己按过 F1 的话，演出结束不该把 HUD 强行打开。
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            mc.options.hideGui = hudWasHidden;
        }
    }

    /** 演出开始前的 {@code hideGui} 状态（演出结束要严格还原）。 */
    private static boolean hudWasHidden = false;

    public static boolean active() {
        return current != null;
    }

    /** 演出进度 0~1（给 HUD 与其它系统用）。 */
    public static float progress() {
        if (current == null) {
            return 0.0F;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        return Mth.clamp((float) (now - current.startTick) / current.totalTicks, 0.0F, 1.0F);
    }

    public static String title() {
        return current == null ? "" : current.title;
    }

    public static String subtitle() {
        return current == null ? "" : current.subtitle;
    }

    // ------------------------------------------------------------------
    // 每 tick：超时就自己结束（服务端还会再发一次 end 兜底）
    // ------------------------------------------------------------------

    public static void tick() {
        if (current == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        if (now - current.startTick > current.totalTicks + 20L) {
            // ⚠ 必须走 end()，不能只把 current 置空 ——
            // end() 负责把 HUD 还原成演出前的状态（player.hideGui）。
            // 曾经这里直接 `current = null`，导致运镜结束后 HUD 永远是隐藏的。
            end();
            restorePlayer();
        }
    }

    // ------------------------------------------------------------------
    // 相机求解
    // ------------------------------------------------------------------

    /**
     * <b>相机世界坐标</b>（相对<b>调用方给的锚点</b>算）。
     *
     * <h2>⚠ 为什么需要这个方法（修的是一个真 bug）</h2>
     * <p>我上一版是在 {@code ComputeCameraAngles} 里直接
     * {@code player.setPos(镜头位)} —— 那等于<b>把玩家本人搬到镜头位置</b>。</p>
     *
     * <p>玩家一眼看出来了：「那个法术的运镜如果你移动玩家的话，
     * 那么岂不是特效位置也会发生变化」—— <b>完全正确</b>，后果三条：</p>
     * <ol>
     *   <li>以<b>玩家为锚点</b>的特效（蓄力阵、剑阵、领域）会跟着镜头飘走；</li>
     *   <li>破坏类法术的落点是按玩家视线算的，玩家一动<b>射线起点就变了</b>；</li>
     *   <li>玩家被搬到几十格外 → <b>区块加载/卸载</b>跟着抖。</li>
     * </ol>
     *
     * <p>所以现在把镜头计算抽成纯函数：<b>锚点由调用方传进来</b>
     * （一般就是玩家当前位置），相机算出来之后交给原版 {@code Camera#setPosition} ——
     * <b>玩家实体始终不动</b>，而镜头看起来仍然在环绕、在拉远。</p>
     *
     * @param anchor  锚点（一般是玩家当前位置；"以玩家为中心环绕"就是这个意思）
     * @param partial 帧内插值系数（0~1）
     */
    public static Vec3 cameraWorld(Vec3 anchor, float partial) {
        if (current == null) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        double t = Mth.clamp((now - current.startTick + partial)
                / (double) current.totalTicks, 0.0D, 1.0D);
        return shotAt(anchor, t, true);
    }

    /** 相机看向的世界坐标（相对给定锚点）。 */
    public static Vec3 lookWorld(Vec3 anchor, float partial) {
        if (current == null) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        double t = Mth.clamp((now - current.startTick + partial)
                / (double) current.totalTicks, 0.0D, 1.0D);
        return shotAt(anchor, t, false);
    }

    /** 演出开始时记下的锚点（给需要固定锚点的运镜用，例如BOSS出场）。 */
    public static Vec3 anchor() {
        return current == null ? null : current.anchor;
    }

    /**
     * 取这一帧的镜头参数。
     *
     * @param isPos {@code true} 返回相机位；{@code false} 返回看向点
     */
    private static Vec3 shotAt(Vec3 anchor, double t, boolean isPos) {
        int n = current.shots.size() - 1;
        double seg = t * n;
        int i = Mth.clamp((int) Math.floor(seg), 0, n - 1);
        double f = smoothstep(seg - i);
        Shot a = current.shots.get(i);
        Shot b = current.shots.get(i + 1);
        if (!isPos) {
            float lookY = Mth.lerp((float) f, a.lookY(), b.lookY());
            return new Vec3(anchor.x, anchor.y + lookY, anchor.z);
        }
        float dist = Mth.lerp((float) f, a.dist(), b.dist());
        float yaw = Mth.lerp((float) f, a.yawDeg(), b.yawDeg());
        float pitch = Mth.lerp((float) f, a.pitchDeg(), b.pitchDeg());
        double ya = Math.toRadians(yaw);
        double pa = Math.toRadians(pitch);
        double horiz = Math.cos(pa) * dist;
        return new Vec3(
                anchor.x + Math.sin(ya) * horiz,
                anchor.y + Math.sin(pa) * dist,
                anchor.z + Math.cos(ya) * horiz);
    }

    /**
     * 当前这一帧的相机位置（世界坐标）。没有演出时返回 {@code null}。
     *
     * <p>插值用 <b>smoothstep</b>（两端缓入缓出）而不是线性 ——
     * 线性运镜看起来"很硬"，缓入缓出才有"镜头在移动"的感觉。</p>
     *
     * @param partial 帧内插值系数（0~1）
     */
    public static Vec3 cameraPos(float partial) {
        if (current == null) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        double t = (now - current.startTick + partial) / (double) current.totalTicks;
        t = Mth.clamp(t, 0.0D, 1.0D);

        int n = current.shots.size() - 1;
        double seg = t * n;
        int i = Mth.clamp((int) Math.floor(seg), 0, n - 1);
        double f = smoothstep(seg - i);

        Shot a = current.shots.get(i);
        Shot b = current.shots.get(i + 1);
        float dist = Mth.lerp((float) f, a.dist(), b.dist());
        float yaw = Mth.lerp((float) f, a.yawDeg(), b.yawDeg());
        float pitch = Mth.lerp((float) f, a.pitchDeg(), b.pitchDeg());

        double ya = Math.toRadians(yaw);
        double pa = Math.toRadians(pitch);
        double horiz = Math.cos(pa) * dist;
        return new Vec3(
                current.anchor.x + Math.sin(ya) * horiz,
                current.anchor.y + Math.sin(pa) * dist,
                current.anchor.z + Math.cos(ya) * horiz);
    }

    /** 当前这一帧相机看向的点。 */
    public static Vec3 lookTarget(float partial) {
        if (current == null) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        double t = (now - current.startTick + partial) / (double) current.totalTicks;
        t = Mth.clamp(t, 0.0D, 1.0D);
        int n = current.shots.size() - 1;
        double seg = t * n;
        int i = Mth.clamp((int) Math.floor(seg), 0, n - 1);
        double f = smoothstep(seg - i);
        Shot a = current.shots.get(i);
        Shot b = current.shots.get(i + 1);
        float lookY = Mth.lerp((float) f, a.lookY(), b.lookY());
        return new Vec3(current.anchor.x, current.anchor.y + lookY, current.anchor.z);
    }

    /** 当前这一帧的视场角（度）。 */
    public static float fov(float partial) {
        if (current == null) {
            return 70.0F;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        double t = (now - current.startTick + partial) / (double) current.totalTicks;
        t = Mth.clamp(t, 0.0D, 1.0D);
        int n = current.shots.size() - 1;
        double seg = t * n;
        int i = Mth.clamp((int) Math.floor(seg), 0, n - 1);
        double f = smoothstep(seg - i);
        return Mth.lerp((float) f, current.shots.get(i).fov(), current.shots.get(i + 1).fov());
    }

    /** 两端缓入缓出（smoothstep）。 */
    private static double smoothstep(double x) {
        double c = Mth.clamp(x, 0.0D, 1.0D);
        return c * c * (3.0D - 2.0D * c);
    }
}
