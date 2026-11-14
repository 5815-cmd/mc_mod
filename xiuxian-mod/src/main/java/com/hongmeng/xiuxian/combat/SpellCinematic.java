package com.hongmeng.xiuxian.combat;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.network.CinematicPacket;
import com.hongmeng.xiuxian.network.ModNetwork;
import com.hongmeng.xiuxian.technique.Technique;
import com.hongmeng.xiuxian.technique.TechniqueKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>灭世级法术的专属运镜（自动切第三视角、拉远观看）</b>。
 *
 * <h2>玩家原话</h2>
 * <blockquote>
 * 「释放灭世级别的法术的时候自动切换第三视角拉远观看特效，要影视级别的运镜」
 * </blockquote>
 *
 * <h2>为什么这是三段而不是一段</h2>
 * <p>BOSS 出场只有一个节拍（出现），所以一段环绕就够了。
 * 但法术有<b>蓄力 → 释放 → 余波</b>三个节拍，运镜必须跟着走，
 * 否则"拉远"会发生在错误的时刻（例如法术还没放出去镜头就飞了）。</p>
 *
 * <table border="1">
 *   <caption>三段运镜</caption>
 *   <tr><th>段</th><th>时机</th><th>镜头</th><th>想表达</th></tr>
 *   <tr><td>① 起势</td><td>蓄力开始 ~40%</td>
 *       <td>从<b>近景</b>（贴脸）缓缓<b>拉远升高</b></td>
 *       <td>让玩家先看到自己在凝聚力量</td></tr>
 *   <tr><td>② 蓄势</td><td>蓄力 40% ~ 100%</td>
 *       <td><b>绕人半圈</b> + 继续拉远</td>
 *       <td>力量在积累，画面逐渐壮阔</td></tr>
 *   <tr><td>③ 释放</td><td>结算后约 3 秒</td>
 *       <td>切到<b>炸点</b>：贴地仰拍 → 大幅拉远俯视</td>
 *       <td>看清破坏的全貌（这是玩家最想看的）</td></tr>
 * </table>
 *
 * <h2>为什么释放段要"切到炸点"</h2>
 * <p>灭世级法术的看点<b>不在施法者身上，而在被摧毁的地方</b>。
 * 所以释放瞬间必须把镜头"剪"过去（电影里的 cross-cut），
 * 而不是继续围着玩家转 —— 后者会让玩家看不到自己造成了什么。</p>
 *
 * <h2>与 BOSS 出场共用同一套相机基建</h2>
 * <p>都用 {@link CinematicPacket} + 客户端的 {@code Cinematic}：
 * 关键帧服务端算、客户端只插值。所以不存在"两端不同步"，
 * 也不需要 mixin。</p>
 *
 * <h2>⚠ 移动会不会打断施法</h2>
 * <p>不会。项目的 {@code SpellCastHandler.interruptReason} 已经<b>刻意去掉</b>了
 * "移动打断"这条判据（玩家要求"移动不会打断施法"），只留身死与换维度。
 * 所以镜头移动玩家是安全的 —— 这一点我先查过才敢做。</p>
 */
public final class SpellCinematic {

    /**
     * 2026-11-01 玩家要求：<b>法术运镜全部取消</b>。
     *
     * <p>改成 {@code true} 即恢复原来的「蓄力拉远 + 释放切炸点」三段运镜。
     * 关掉时只影响镜头与演出无敌；{@link #armInvulnerablePublic} 仍可供其它演出复用。</p>
     */
    public static final boolean CAMERA_ENABLED = false;

    /** 蓄力段的镜头 tick 数上限（按实际蓄力时长缩放）。 */
    private static final int CHARGE_TICKS = 90;
    /** 释放段的镜头 tick 数（炸点特写）。 */
    private static final int BLAST_TICKS = 70;

    /**
     * 玩家 -> 演出应该结束的 gameTime。
     *
     * <p>⚠ 存在的理由：{@code setInvulnerable(true)} 挂上去之后，
     * 原代码<b>没有任何一处设回 false</b> —— 放一次灭世法术就<b>永久无敌</b>，
     * 而且 {@code Invulnerable} 会写进 NBT，重登还在。</p>
     *
     * <p>现在由服务端每 tick 检查这个表，到点自动摘掉无敌。</p>
     */
    private static final java.util.Map<java.util.UUID, Long> CAM_UNTIL =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 演出无敌的最长兜底时长（tick）—— 即使包丢了也不会永久无敌。 */
    private static final int MAX_CAM_TICKS = 20 * 20;


    private SpellCinematic() {
    }

    /** 这个法术是否值得上运镜（只有灭世级 / 引力级）。 */
    public static boolean deserves(Technique technique) {
        if (!CAMERA_ENABLED) {
            return false;
        }
        if (technique == null) {
            return false;
        }
        TechniqueKind k = technique.kind();
        return k == TechniqueKind.ULTIMATE || k == TechniqueKind.GRAVITY;
    }

    /**
     * <b>第一 + 二段</b>：蓄力开始就切第三视角并拉远。
     *
     * @param player 施法者
     * @param totalTicks 这个法术的<b>实际蓄力总时长</b>（不同法术 4.5~9 秒）
     * @param title 标题（一般是法术名）
     */
    public static void beginCharge(ServerPlayer player, int totalTicks, String title) {
        if (!CAMERA_ENABLED) {
            return;
        }
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        try {
            int ticks = Math.max(40, Math.min(CHARGE_TICKS, totalTicks));
            float[] shots = chargeShots();
            ModNetwork.sendToPlayer(player, CinematicPacket.play(
                    player.position(), shots, ticks,
                    "\u00a7d" + title,
                    "\u00a77\u00a7o天地为之色变"));
            armInvulnerable(player, ticks);
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[spell-cam] 蓄力运镜启动失败：{}", t.toString());
        }
    }

    // ------------------------------------------------------------------
    // 演出无敌：**挂上就必须能摘掉**
    // ------------------------------------------------------------------

    /** 供其它演出系统复用（RealWorldCinematic 等），避免"只挂不摘"。 */
    public static void armInvulnerablePublic(ServerPlayer player, int ticks) {
        armInvulnerable(player, ticks);
    }

    /** 挂上演出无敌，并登记"到点自动摘"。 */
    private static void armInvulnerable(ServerPlayer player, int ticks) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int t = Math.max(20, Math.min(MAX_CAM_TICKS, ticks + 20));
        CAM_UNTIL.put(player.getUUID(), level.getGameTime() + t);
        player.setInvulnerable(true);
    }

    /**
     * 每 tick 调用：到点就把演出无敌摘掉。
     *
     * <p>这是"只挂不摘"的兜底 —— 即使客户端包丢了、玩家中途退出，
     * 也一定会在最长 {@link #MAX_CAM_TICKS} 内恢复。</p>
     */
    public static void tickCamera(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Long until = CAM_UNTIL.get(player.getUUID());
        if (until == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now >= until) {
            CAM_UNTIL.remove(player.getUUID());
            player.setInvulnerable(false);
        }
    }

    /** 立刻结束演出并摘掉无敌（下线 / 死亡 / 手动收场）。 */
    public static void endCam(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CAM_UNTIL.remove(player.getUUID());
        player.setInvulnerable(false);
    }

    /**
     * <b>第三段</b>：释放瞬间切到炸点。
     *
     * @param player 施法者（用来在结尾把镜头拉回他）
     * @param impact 炸点（一般就是玩家准心命中的位置）
     */
    public static void showBlast(ServerPlayer player, Vec3 impact, String title) {
        if (!CAMERA_ENABLED) {
            return;
        }
        if (player == null || impact == null
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        try {
            float[] shots = blastShots(impact, player.position());
            ModNetwork.sendToPlayer(player, CinematicPacket.play(impact, shots,
                    BLAST_TICKS,
                    "\u00a7c" + title + " \u00a77\u00a7o· 落",
                    ""));
            armInvulnerable(player, BLAST_TICKS);
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[spell-cam] 释放运镜启动失败：{}", t.toString());
        }
    }

    /**
     * 蓄力段关键帧：近景 → 拉远 → 绕人半圈。
     *
     * <p>每 5 个浮点一帧：{@code (距离, 水平角, 俯仰角, 看向高度, 视场角)}。
     * 起始距离刻意<b>很近（2.6 格）</b>——"贴脸"是电影里的 standard opening，
     * 从近到远才有"世界展开"的张力；一上来就远景会很平。</p>
     */
    private static float[] chargeShots() {
        List<float[]> shots = new ArrayList<>();
        shots.add(new float[]{2.6F, -30.0F, -4.0F, 1.5F, 66.0F});   // 贴脸近景
        shots.add(new float[]{5.0F, 10.0F, 8.0F, 1.6F, 62.0F});      // 开始拉远
        shots.add(new float[]{9.0F, 70.0F, 17.0F, 1.7F, 56.0F});     // 拉开 + 升高
        shots.add(new float[]{14.0F, 140.0F, 24.0F, 1.8F, 52.0F});   // 绕人半圈
        shots.add(new float[]{17.0F, 200.0F, 20.0F, 1.6F, 50.0F});   // 到位（等释放）
        return flatten(shots);
    }

    /**
     * 释放段关键帧：炸点贴地仰拍 → 大幅拉远 → 回望施法者。
     *
     * <p>锚点是<b>炸点</b>（由调用方传入），所以最后几帧的"水平角"要朝施法者的方向
     * —— 由 {@link #blastShots} 按<b>炸点指向施法者</b>的实际方位算出来，
     * 于是结尾自然地把视线带回玩家，而不是生硬地跳回去。</p>
     *
     * @param impact 炸点（锚点）
     * @param caster 施法者位置（用来算"回望"的方位）
     */
    private static float[] blastShots(Vec3 impact, Vec3 caster) {
        // 炸点 -> 施法者的方位角（度）。0 = +Z，与 Cinematic 的球坐标一致。
        //
        // ⚠ 这里原来写错过：我写成 `dx = caster.x`（漏了减炸点），
        //   等于把炸点当成原点 —— 炸点一远离原点，镜头朝向就会偏掉。
        double dx = caster.x - impact.x;
        double dz = caster.z - impact.z;
        float towardCaster = (float) Math.toDegrees(Math.atan2(dx, dz));
        List<float[]> shots = new ArrayList<>();
        // 起手贴地仰拍：先给"压迫感"
        shots.add(new float[]{4.5F, towardCaster + 150.0F, -10.0F, 1.0F, 70.0F});
        // 拉远 + 升高，看清全貌
        shots.add(new float[]{14.0F, towardCaster + 110.0F, 26.0F, 1.5F, 60.0F});
        // 最高最远：交代破坏范围（这是玩家最想看的一帧）
        shots.add(new float[]{26.0F, towardCaster + 60.0F, 44.0F, 0.5F, 52.0F});
        // 缓慢收回，转向施法者
        shots.add(new float[]{18.0F, towardCaster + 20.0F, 30.0F, 1.2F, 58.0F});
        // 收束：朝施法者，准备把镜头还给他
        shots.add(new float[]{9.0F, towardCaster, 12.0F, 1.5F, 66.0F});
        return flatten(shots);
    }

    private static float[] flatten(List<float[]> shots) {
        float[] flat = new float[shots.size() * 5];
        int i = 0;
        for (float[] s : shots) {
            for (float v : s) {
                flat[i++] = v;
            }
        }
        return flat;
    }
}
