package com.hongmeng.xiuxian.realworld;


/**
 * <b>「真实世界」开启时的过场演出</b>。
 *
 * <p>复用 BOSS 出场那套镜头系统（{@code Cinematic}）——
 * 玩家要求「选择开启后这可以做个过场动画」，不需要为它另做一套相机。</p>
 *
 * <h2>这段运镜与 BOSS 出场的区别</h2>
 * <p>BOSS 出场是<b>绕着 boss 转</b>（交代形体）；
 * 这里是<b>从贴地缓缓拉开、最后俯视</b>——
 * 表达"世界在你眼前展开、你看到了它的全貌"。</p>
 *
 * <p>⚠ 服务端不能直接引用客户端类（专服会 NoClassDefFound），
 * 所以这里的调用必须走 {@code DistExecutor} 或只发网络包。
 * 本类<b>只发网络包</b>，不 import 任何客户端类。</p>
 */
public final class RealWorldCinematic {

    /** 过场时长（tick）。 */
    public static final int TICKS = 200;

    private RealWorldCinematic() {
    }

    /** 播一段"世界展开"的过场。 */
    public static void play(net.minecraft.server.level.ServerLevel level,
                            net.minecraft.server.level.ServerPlayer player) {
        if (level == null || player == null) {
            return;
        }
        try {
            var anchor = player.position();
            float[] shots = buildShots();
            com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(player,
                    com.hongmeng.xiuxian.network.CinematicPacket.play(anchor, shots, TICKS,
                            "\u00a74\u00a7l天 地 动 荡", "\u00a78真实世界 · 万物显形"));
            // ⚠ 不能只挂不摘：走 SpellCinematic 的登记，由服务端到点自动摘掉
            com.hongmeng.xiuxian.combat.SpellCinematic.armInvulnerablePublic(player, 200);
            playDreadFx(level, player);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[realworld] 过场启动失败：{}", t.toString());
        }
    }

    /**
     * 压迫感的"声与光"：连续闷雷 + 红光脉冲 + 一句低语。
     *
     * <p>镜头只是骨架 —— 真正让人心里发紧的是<b>声音</b>与<b>闪光</b>。
     * 这里叠三样：</p>
     * <ul>
     *   <li>三次闷雷（间隔逐渐变短，像越逼越近）；</li>
     *   <li>两次红光脉冲（复用受伤闪屏的通道，观感上是"天在渗血"）；</li>
     *   <li>一句低语。</li>
     * </ul>
     */
    private static void playDreadFx(net.minecraft.server.level.ServerLevel level,
                                    net.minecraft.server.level.ServerPlayer player) {
        try {
            var pos = player.position();
            // 三次闷雷：0 / 60 / 100 tick（越来越急）
            for (int delay : new int[]{0, 60, 100}) {
                com.hongmeng.xiuxian.realworld.RealWorldScheduler
                        .after(level, delay, () -> level.playSound(null,
                                pos.x, pos.y, pos.z,
                                net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                                net.minecraft.sounds.SoundSource.WEATHER,
                                1.0F, 0.42F));
            }
            // 两次红光脉冲
            for (int delay : new int[]{10, 70}) {
                com.hongmeng.xiuxian.realworld.RealWorldScheduler
                        .after(level, delay, () -> {
                            com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(player,
                                    new com.hongmeng.xiuxian.network.PainFlashPacket(
                                            12, 0.55F));
                            // 走批量器（项目硬线：A 类 speed=0 的粒子必须走 ParticleBatch）
                            var flash = new com.hongmeng.xiuxian.technique.ParticleBatch(level);
                            flash.add(net.minecraft.core.particles.ParticleTypes.FLASH,
                                    pos.x, pos.y + 3.0D, pos.z);
                            flash.flush();
                        });
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a78\u00a7o……天塌了一角。"), false);
        } catch (Throwable ignored) {
            // 演出失败不影响真正的世界切换
        }
    }

    /**
     * 关键帧：<b>天地动荡</b>（玩家要求"一定要有压迫感"）。
     *
     * <p>原版镜头是"贴地仰视 → 缓缓拉开 → 俯瞰"，情绪是<b>壮阔</b>。
     * 但玩家要的是压迫感 —— 于是重排成：</p>
     * <ol>
     *   <li><b>低机位贴地</b>：视野被地面压住，天在头顶很近（仰角大）；</li>
     *   <li><b>天空压下来</b>：镜头不下移，靠<b>快速拉远</b>让天地同时出现，
     *       人缩小成一个小点；</li>
     *   <li><b>猛的升到极高空</b>俯视（人如蝼蚁）—— 这是压迫感的顶点；</li>
     *   <li><b>硬切回近景</b>：从高空直接砸回玩家肩膀高度，落差制造冲击；</li>
     *   <li>落定。</li>
     * </ol>
     *
     * <p>每 5 个浮点一帧：{@code (距离, 水平角, 俯仰角, 看向高度, 视场角)}。
     * 视场角从 78 一路收到 46 再放开 —— 收窄的那几帧就是"天地向你合拢"。</p>
     */
    private static float[] buildShots() {
        float[][] shots = {
                {2.2F, 0.0F, -22.0F, 3.4F, 78.0F},     // 贴地仰视：天很近
                {2.6F, 8.0F, -26.0F, 3.8F, 76.0F},     // 再压低，天更"沉"
                {7.0F, 20.0F, -18.0F, 3.0F, 66.0F},    // 猛拉远（天地同框）
                {16.0F, 40.0F, 2.0F, 2.4F, 56.0F},     // 升起来
                {34.0F, 70.0F, 30.0F, 1.6F, 48.0F},    // 高空俯视：人如蝼蚁
                {52.0F, 120.0F, 48.0F, 1.0F, 46.0F},   // 更高更小
                {30.0F, 175.0F, 40.0F, 1.2F, 50.0F},   // 缓慢回落
                {6.0F, 210.0F, 6.0F, 1.6F, 62.0F},     // 硬切回近景
                {3.2F, 250.0F, 1.0F, 1.5F, 70.0F},     // 落在肩膀高度
        };
        float[] packed = new float[shots.length * 5];
        for (int k = 0; k < shots.length; k++) {
            System.arraycopy(shots[k], 0, packed, k * 5, 5);
        }
        return packed;
    }
}
