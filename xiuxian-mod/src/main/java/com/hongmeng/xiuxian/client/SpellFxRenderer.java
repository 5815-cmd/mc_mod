package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.network.SpellFxPacket;
import com.hongmeng.xiuxian.spirit.Element;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * <b>法术贴图特效渲染器（立体版）</b>。
 *
 * <p>玩家要求：「陨石要立体的，那种释放的特效都要立体的，比如还有召唤的巨剑、
 * 黑洞什么的，<b>你不能只贴图贴一个面</b>，还要配合特效」。</p>
 *
 * <h2>相比上一版改了什么</h2>
 * <p>上一版每个形状都是<b>一张朝向相机的面片</b>（billboard）—— 陨石转到侧面就是薄纸。
 * 现在全部走 {@link SpellFxMesh} 的<b>真几何</b>：</p>
 * <table border="1">
 *   <caption>形状 → 几何</caption>
 *   <tr><th>形状</th><th>几何构成</th></tr>
 *   <tr><td>陨石</td><td>8 棱不规则<b>棱柱</b>岩块 + 尾部<b>圆锥</b>火焰 + 内核<b>球</b></td></tr>
 *   <tr><td>金色巨剑</td><td><b>扁棱柱</b>剑身（有厚度）+ 护手 + 剑柄 + 剑尖<b>圆锥</b></td></tr>
 *   <tr><td>光柱</td><td>多层同心<b>圆柱筒</b>，越里越亮</td></tr>
 *   <tr><td>冲击环</td><td><b>立体星芒</b>（八角三棱锥）+ 贴地环</td></tr>
 *   <tr><td>爆闪</td><td>内核<b>球</b> + 立体星芒</td></tr>
 *   <tr><td>冲击穹顶</td><td><b>球壳</b></td></tr>
 *   <tr><td>虚空裂缝</td><td>两片正交锯齿面（从任何角度都有体积）</td></tr>
 *   <tr><td>花瓣旋涡</td><td>三圈不同高度的花瓣面片绕轴旋转</td></tr>
 * </table>
 *
 * <p>粒子层保持不动 —— 玩家要的是「贴图 + 粒子配合」，不是二选一。</p>
 */
public final class SpellFxRenderer {

    /**
     * 玩家 2026-11-02：除 Boss 召唤/登场与自己挨打外，其他镜头反馈全关。
     * 法术贴图/粒子保留，只关掉每个特效包首次出现时的镜头震动/FOV。
     */
    private static final boolean FX_CAMERA_ENABLED = false;

    /**
     * PERF(C): 当前正在渲染的特效（给"贴地特效的地面高度"缓存用）。
     *
     * <p>渲染线程单线程顺序执行，不存在并发访问；每次 {@link #render} 的循环里
     * 逐条赋值，地面高度按特效生命周期缓存（见 {@link ClientSpellFx.Fx#groundY}）。</p>
     */
    private static ClientSpellFx.Fx currentFx;

    /** PERF(C): colorOf 的复用缓冲（同一次绘制内同步用掉，不跨帧保存）。 */
    private static final float[] COLOR_SCRATCH = new float[3];

    /** PERF(C): flashPass 的复用缓冲（同上，只在本帧绘制内使用）。 */
    private static final float[] FLASH_SCRATCH = new float[3];

    /** PERF(C): Element.values() 每次调用都会克隆数组，这里缓存一份。 */
    private static final Element[] ELEMENTS = Element.values();

    private SpellFxRenderer() {
    }

    /** 每帧调用（AFTER_PARTICLES）。 */
    public static void render(PoseStack pose, Vec3 cam, float partial) {
        if (ClientSpellFx.active().isEmpty() || SpellFxQuality.layers() <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        // **自己的缓冲区**：绝不用关卡共享的（提前 endBatch 会让 getShader() 停在 null，
        // 紧接着 renderChunkLayer 读它就 NPE 崩客户端 —— 项目里踩过一次）
        MultiBufferSource.BufferSource own = MultiBufferSource.immediate(
                com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder());
        double maxDist = SpellFxQuality.viewDistance();

        // PERF(C): 相机/视角这些每帧只有一个值，提到循环外算一次。
        // 原来每条特效都算一遍，而且 getViewVector() 每次都会 new Vec3。
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        Vec3 camLook = firstPerson && mc.player != null
                ? mc.player.getViewVector(partial) : null;

        for (ClientSpellFx.Fx fx : ClientSpellFx.active()) {
            SpellFxPacket p = fx.packet;
            if (!ClientSpellFx.visible(p.from(), maxDist)
                    && !ClientSpellFx.visible(p.to(), maxDist)) {
                continue;
            }
            // ---- 第一人称：不画"贴在自己身上的光环" ----
            //
            // 玩家反复反馈"特效一出现就极其遮挡视野"。血池/魔气的
            // TORUS / HELIX 就铺在脚边与身上，第三人称很好看，
            // 但第一人称等于把镜头糊住 —— 所以**只在第一人称跳过它们**。
            //
            // 判定：形状是环/螺旋，且距本地玩家 2.2 格以内。
            // 别人身上的光环、远处的环都不受影响。
            if (firstPerson && isPersonalAura(p, cam)) {
                continue;
            }

            double t = fx.progress(partial);
            float[] rgb = colorOf(p.element(), t);
            float alpha = alphaOf(p, t);
            if (alpha <= 0.02F) {
                continue;
            }
            // ---- 2026-11-01 近距遮挡保护；2026-11-07 再收紧一档 ----
            //
            // 第一人称最容易被"贴在视野正前方"的特效糊住：距离相机越近、
            // 越靠近视线中心，透明度越低（smoothstep 衰减）；1.2 格以内直接跳过，
            // 4.5 格以内按距离压暗（上一版是 0.9 / 3.4，玩家仍反馈"还是多"）。
            // 只对"在视线前方"的部分生效 —— 脚下法阵、身边光环不衰减。
            // PERF(C): 用 dx/dy/dz + 点乘代替 Vec3.subtract()/length()/scale()/dot()，
            //          每帧每条特效少 3 个临时 Vec3。
            Vec3 at = p.from();
            double dx = at.x - cam.x;
            double dy = at.y - cam.y;
            double dz = at.z - cam.z;
            double nearD = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (firstPerson) {
                double facing = nearD > 1.0E-4D && camLook != null
                        ? (dx * camLook.x + dy * camLook.y + dz * camLook.z) / nearD
                        : 0.0D;
                if (facing > 0.20D) {
                    if (nearD < 1.2D) {
                        continue;          // 贴脸：整条跳过
                    }
                    if (nearD < 4.5D) {
                        double k = (nearD - 1.2D) / 3.3D;
                        double smooth = k * k * (3.0D - 2.0D * k);
                        alpha *= (float) (0.06D + 0.94D * smooth);
                    }
                }
            } else if (nearD < 1.5D) {
                alpha *= 0.45F;
            }
            // ---- 2026-11-07 R3：地面平片环（截图里糊住下半屏的"大圆环"）----
            //
            // SHOCK_RING / RUNE_RING / MANDALA / BLOOD_SIGIL / GROUND_CRACK 都是
            // **贴地大面片**：半径 10+ 格、铺在身前 6 格处，第一人称直接盖住半个屏幕。
            // 现在：<12 格压暗到 30%，<7 格整条跳过（远处仍能看到"地上有阵"）。
            int shapeNow = p.shape();
            boolean groundPlane = shapeNow == SpellFxPacket.SHOCK_RING
                    || shapeNow == SpellFxPacket.RUNE_RING
                    || shapeNow == SpellFxPacket.MANDALA
                    || shapeNow == SpellFxPacket.BLOOD_SIGIL
                    || shapeNow == SpellFxPacket.GROUND_CRACK
                    || shapeNow == SpellFxPacket.CREEPING_CRACK;
            if (firstPerson && groundPlane && nearD < 12.0D) {
                alpha *= 0.30F;
                if (nearD < 7.0D) {
                    continue;
                }
            }
            // ---- 镜头冲击：重型法术命中/爆发的那一瞬抖屏 ----
            //
            // 玩家一直说特效"观感差"。光加几何不够 ——
            // 影视里让爆炸"有份量"的一半功劳在**镜头**：爆炸的瞬间画面要震。
            // 这里复用项目已有的 CombatShake（伤害反馈也用它），
            // 所以不需要新的渲染管线。
            //
            // 只在**效果刚出现的那几帧**抖：持续抖会晕，而且会盖过伤害反馈。
            if (!fx.shook) {
                fx.shook = true;
                // Boss 转阶段的五层爆发包 noCamera=true：只画特效，不推镜头。
                if (FX_CAMERA_ENABLED && !p.noCamera()) {
                    punchForShape(p);
                }
            }
            try {
                // PERF(C): 供贴地特效复用"地面高度"缓存（每条特效一生只打一次射线）
                currentFx = fx;
                draw(pose, own, p, cam, t, rgb, alpha, mc);
            } catch (Throwable ignored) {
                // 单条特效画失败不能拖垮整帧
            }
        }
        currentFx = null;
        own.endBatch();
    }

    /**
     * 按形状给一次镜头冲击。
     *
     * <p>分级刻意拉开：陨石/灭世级是重击，光柱/斩击是中等，
     * 环/穹顶这类"扩散型"只有轻推 —— 否则满屏都在抖，玩家会晕。</p>
     */
    /**
     * 这是不是「贴在本地玩家身上」的资源光环。
     *
     * @param p   特效包
     * @param cam 当前相机位置
     * @return 第一人称会挡住视野的那种就返回 true
     */
    private static boolean isPersonalAura(SpellFxPacket p,
                                          net.minecraft.world.phys.Vec3 cam) {
        int shape = p.shape();
        boolean ringLike = shape == SpellFxPacket.TORUS
                || shape == SpellFxPacket.HELIX
                || shape == SpellFxPacket.RUNE_RING
                // 批次一补的三类"贴脚环"：以前第一人称会把镜头糊住
                || shape == SpellFxPacket.RING_TOWER
                || shape == SpellFxPacket.MANDALA
                || shape == SpellFxPacket.SHOCK_RING
                // 2026-11-01 第三轮：蓄力 3D 法阵/血符/穹顶也是"贴在脚下"的，
                // 第一人称直接跳过（它们的信息量在第三人称才读得出）
                || shape == SpellFxPacket.BLOOD_SIGIL
                || shape == SpellFxPacket.SHOCK_DOME
                || shape == SpellFxPacket.GROUND_CRACK
                || shape == SpellFxPacket.CREEPING_CRACK
                || shape == SpellFxPacket.IMPACT_FLARE
                || shape == SpellFxPacket.PETAL_SWIRL;
        if (!ringLike) {
            return false;
        }
        // 只处理"就在脚下/身上"的那种（≤2.2 格）
        return cam.distanceToSqr(p.from()) <= 2.2D * 2.2D;
    }

    private static void punchForShape(SpellFxPacket p) {
        try {
            float s = Mth.clamp(p.size() / 6.0F, 0.35F, 1.8F);
            switch (p.shape()) {
                case SpellFxPacket.METEOR_ROCK -> {
                    CombatShake.punch(com.hongmeng.xiuxian.combat.CombatFeel.Shake.HEAVY, 1.15D * s);
                    CombatShake.punchFov(0.10F * s);
                }
                case SpellFxPacket.BLADE_GOLD -> {
                    CombatShake.punch(com.hongmeng.xiuxian.combat.CombatFeel.Shake.HEAVY, 0.85D * s);
                    CombatShake.punchFov(0.07F * s);
                }
                case SpellFxPacket.CRACK_VOID, SpellFxPacket.RING_TOWER -> {
                    CombatShake.punch(com.hongmeng.xiuxian.combat.CombatFeel.Shake.HEAVY, 0.95D * s);
                    CombatShake.punchFov(0.08F * s);
                }
                case SpellFxPacket.BEAM_COLUMN, SpellFxPacket.PILLAR_DROP -> {
                    CombatShake.punch(com.hongmeng.xiuxian.combat.CombatFeel.Shake.MEDIUM, 0.9D * s);
                    CombatShake.punchFov(0.05F * s);
                }
                case SpellFxPacket.SHOCK_RING, SpellFxPacket.SHOCK_DOME,
                     SpellFxPacket.TORUS -> {
                    CombatShake.punch(com.hongmeng.xiuxian.combat.CombatFeel.Shake.LIGHT, 0.9D * s);
                }
                case SpellFxPacket.FROST_CAGE -> {
                    // 冰牢是"合拢"的重控制，比扩散型重一档，但没有爆炸那么晃
                    CombatShake.punch(com.hongmeng.xiuxian.combat.CombatFeel.Shake.MEDIUM, 0.85D * s);
                    CombatShake.punchFov(0.045F * s);
                }
                case SpellFxPacket.BLOOD_SIGIL -> {
                    // 血符是贴地法阵：只有地面在"闷"一下，不推镜头（否则第一人称会晕）
                    CombatShake.punch(com.hongmeng.xiuxian.combat.CombatFeel.Shake.LIGHT, 0.75D * s);
                }
                default -> {
                    // 爆闪 / 雪花 / 花瓣 / 符文环 / 螺旋 / 晶格：只轻推一下
                    CombatShake.punch(com.hongmeng.xiuxian.combat.CombatFeel.Shake.LIGHT, 0.55D * s);
                }
            }
        } catch (Throwable ignored) {
            // 抖屏失败不影响画面
        }
    }

    // ------------------------------------------------------------------
    // 各形状（全部立体）
    // ------------------------------------------------------------------

    /**
     * <b>三层叠画</b>：本体 + 中层辉光 + 外层辉光。
     *
     * <p>为什么这样做（而不是换加色 RenderType）：{@code RenderStateShard} 的
     * {@code ADDITIVE_TRANSPARENCY} / {@code NO_CULL} / {@code COLOR_WRITE}
     * 全是 <b>protected</b>，模组侧造不出加色类型；项目也不用 Access Transformer。</p>
     *
     * <p>所以用"缩放叠画"来造光晕：以特效中心为原点放大 1.16 / 1.34 倍，
     * 透明度递减 —— 视觉上就是"核心实、外围虚"，在 alpha 混合下同样成立，
     * 而且比换混合模式更可控（辉光厚度可调）。</p>
     */
    private static void draw(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                             Vec3 cam, double t, float[] rgb, float alpha, Minecraft mc) {
        // 本体
        drawOnce(pose, buf, p, cam, t, rgb, alpha, mc);
        // 辉光层数跟随画质档（0 档不画，省性能）
        int layers = SpellFxQuality.layers();
        if (layers <= 0) {
            return;
        }
        // 第一人称：辉光只留 1 层 —— 三层叠画会糊成一片奶白，既挡视线又费性能
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            layers = 1;
        }
        Vec3 c = haloCenter(p);
        // 中层：1.16 倍，26% 透明度（2026-11-07：42% → 26%，再压一档"糊屏感"）
        halo(pose, buf, p, cam, t, rgb, alpha * 0.26F, c, 1.16F, mc);
        // 外层：1.34 倍，6% 透明度（只在中高档画；2026-11-07：16% → 6%）
        if (layers >= 2) {
            halo(pose, buf, p, cam, t, rgb, alpha * 0.06F, c, 1.34F, mc);
        }
    }

    /** 辉光以哪个点为原点缩放：有落点就用落点，否则用起点。 */
    private static Vec3 haloCenter(SpellFxPacket p) {
        try {
            return p.to() == null ? p.from() : p.to();
        } catch (Throwable ignored) {
            return Vec3.ZERO;
        }
    }

    /** 画一层"以 c 为中心放大 k 倍"的辉光。 */
    private static void halo(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                             Vec3 cam, double t, float[] rgb, float alpha,
                             Vec3 c, float k, Minecraft mc) {
        if (alpha <= 0.02F) {
            return;
        }
        pose.pushPose();
        // 世界坐标 -> 相机坐标，再以中心为原点缩放，最后撤回
        double dx = c.x - cam.x;
        double dy = c.y - cam.y;
        double dz = c.z - cam.z;
        pose.translate(dx, dy, dz);
        pose.scale(k, k, k);
        pose.translate(-dx, -dy, -dz);
        try {
            drawOnce(pose, buf, p, cam, t, rgb, alpha, mc);
        } catch (Throwable ignored) {
            // 单层失败不影响本体
        }
        pose.popPose();
    }

    /** 原来的单层绘制（几何不变，只是换个名字）。 */
    private static void drawOnce(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                                 Vec3 cam, double t, float[] rgb, float alpha,
                                 Minecraft mc) {
        // ① 伪泛光：所有形状的外围光晕（只在最高画质档开，省性能）
        if (SpellFxQuality.level() >= 2) {
            try {
                glowPass(pose, buf, p, cam, t, rgb, alpha);
            } catch (Throwable ignored) {
                // 泛光失败不影响本体
            }
        }
        // ② 核心闪爆：法术出现的前 12% 时间
        try {
            flashPass(pose, buf, p, cam, t, rgb, alpha);
        } catch (Throwable ignored) {
            // 同上
        }
        switch (p.shape()) {
            case SpellFxPacket.METEOR_ROCK -> meteor(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.BLADE_GOLD -> blade(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.BEAM_COLUMN -> beam(pose, buf, p, cam, t, rgb, alpha,
                    "beam_column", 1.0F, 8);
            case SpellFxPacket.BEAM_SOFT -> beam(pose, buf, p, cam, t, rgb, alpha,
                    "beam_soft", 2.1F, 6);
            case SpellFxPacket.PILLAR_DROP -> beam(pose, buf, p, cam, t, rgb, alpha,
                    "pillar_drop", 1.5F, 8);
            case SpellFxPacket.CRACK_VOID -> crack(pose, buf, p, cam, t, rgb, alpha, mc);
            case SpellFxPacket.SHOCK_RING -> shockRing(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.SHOCK_DOME -> dome(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.IMPACT_FLARE -> flare(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.RUNE_RING -> runeRing(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.SNOW_FLAKE -> snow(pose, buf, p, cam, t, rgb, alpha, mc);
            case SpellFxPacket.PETAL_SWIRL -> petal(pose, buf, p, cam, t, rgb, alpha, mc);
            // ---- 第 12~15 种形状（真三维几何，见 SpellFxMesh2）----
            case SpellFxPacket.SHARD_BURST ->
                    shardFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.GROUND_CRACK ->
                    groundCrackFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.FILAMENTS ->
                    filamentFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.METEOR_ARC ->
                    meteorArcFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.SPIRAL_ASCENT ->
                    spiralFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.ORBIT -> orbitFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.CREEPING_CRACK ->
                    creepingFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.BEAM_SWEEP ->
                    beamSweepFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.MANDALA -> mandalaFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.TORUS -> torusFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.LATTICE -> latticeFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.HELIX -> helixFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.RING_TOWER -> ringTowerFx(pose, buf, p, cam, t, rgb, alpha);
            // ---- 飞剑系列（玩家反馈旧模型不好看，单独做的四件套几何）----
            case SpellFxPacket.FLYING_SWORD -> flyingSwordFx(pose, buf, p, cam, t,
                    rgb, alpha, FlyingSwordMesh.Kind.SWORD);
            case SpellFxPacket.FLYING_SWORD_HEAVY -> flyingSwordFx(pose, buf, p, cam, t,
                    rgb, alpha, FlyingSwordMesh.Kind.HEAVY);
            case SpellFxPacket.FLYING_DAGGER -> flyingSwordFx(pose, buf, p, cam, t,
                    rgb, alpha, FlyingSwordMesh.Kind.DAGGER);
            case SpellFxPacket.FLYING_BONEBLADE -> flyingSwordFx(pose, buf, p, cam, t,
                    rgb, alpha, FlyingSwordMesh.Kind.BONEBLADE);
            // ---- 批次一：冰牢 / 血符（专用几何 + 专用贴图，见下方 frostCageFx / bloodSigilFx）----
            case SpellFxPacket.FROST_CAGE ->
                    frostCageFx(pose, buf, p, cam, t, rgb, alpha);
            case SpellFxPacket.BLOOD_SIGIL ->
                    bloodSigilFx(pose, buf, p, cam, t, rgb, alpha);
            default -> flare(pose, buf, p, cam, t, rgb, alpha);
        }
    }

    /**
     * <b>圆环体</b>（有厚度的甜甜圈）：缓慢自转 + 沿 Y 轻微起伏。
     *
     * <p>用于护体金光 / 领域边界 / 传送法阵 —— 从任何角度看都是立体的环。</p>
     */
    /**
     * <b>碎片群</b>：向外飞散的多面体碎片。
     *
     * <p>碎片本身<b>不是</b>在原地缩放 —— 位置随 {@code t} 沿各自方向外扩，
     * 并受重力下坠。这是"运动"而不是"变形"。</p>
     */
    private static void shardFx(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                                Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        float s = p.size();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + s * 0.5D, at.z - cam.z);
        int count = 10 + (int) (s * 3.0F);
        SpellFxMesh2.shards(pose, buf, "blade_gold", Math.min(28, count),
                s * 0.16F, s * 1.5F, (float) t, rgb, alpha * 0.9F);
        pose.popPose();
    }

    /**
     * <b>地面裂纹贴花</b>：贴在地面上的放射裂纹。
     *
     * <p>y 从法术原点<b>向下扫描</b>找地面（最多 8 格），找不到就用原点下方 1 格。
     * 不这样做的话，空中施法会把裂纹画在半空。</p>
     */
    private static void groundCrackFx(PoseStack pose, MultiBufferSource buf,
                                      SpellFxPacket p, Vec3 cam, double t,
                                      float[] rgb, float alpha) {
        Vec3 at = p.from();
        // PERF(C): 地面高度按特效生命周期缓存（原来每帧打一次 8 格射线）
        double groundY = groundYFor(p);
        // 前 35% 时间淡入，后 65% 慢慢淡出
        float fade = (float) (t < 0.15D ? t / 0.15D : 1.0D - (t - 0.15D) / 0.85D);
        float a = alpha * Math.max(0.0F, Math.min(1.0F, fade)) * 0.9F;
        if (a <= 0.01F) {
            return;
        }
        pose.pushPose();
        pose.translate(at.x - cam.x, groundY - cam.y + 0.02D, at.z - cam.z);
        SpellFxMesh2.groundCrack(pose, buf, "ground_crack", p.size() * 1.6F,
                rgb, a);
        pose.popPose();
    }

    /** <b>能量丝</b>：多股细长光带缠绕上升。 */
    private static void filamentFx(PoseStack pose, MultiBufferSource buf,
                                   SpellFxPacket p, Vec3 cam, double t,
                                   float[] rgb, float alpha) {
        Vec3 at = p.from();
        float s = p.size();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        // 随时间略微上升（不是静止的柱）
        pose.translate(0.0D, s * 0.3D * t, 0.0D);
        SpellFxMesh2.filaments(pose, buf, "beam_soft", 5,
                s * 2.4F, s * 0.55F, s * 0.06F, rgb, alpha * 0.85F);
        pose.popPose();
    }

    /**
     * <b>陨石拖尾</b>：沿抛物线从 from 飞到 to。
     *
     * <p>这条是"运动轨迹"的核心示例：旧的形状都在原地缩放，
     * 这个形状的位置由 {@code p.from()} 和 {@code p.to()} 插值得到。</p>
     */
    private static void meteorArcFx(PoseStack pose, MultiBufferSource buf,
                                    SpellFxPacket p, Vec3 cam, double t,
                                    float[] rgb, float alpha) {
        Vec3 a = p.from();
        Vec3 b = p.to();
        if (b == null || (b.x == a.x && b.y == a.y && b.z == a.z)) {
            // 没给终点：退化成"原地往下砸"
            b = a.add(0.0D, -6.0D, 0.0D);
        }
        pose.pushPose();
        SpellFxMesh2.meteorArc(pose, buf, "impact_flare",
                (float) (a.x - cam.x), (float) (a.y - cam.y), (float) (a.z - cam.z),
                (float) (b.x - cam.x), (float) (b.y - cam.y), (float) (b.z - cam.z),
                Math.max(0.35F, p.size() * 0.55F), (float) t, rgb, alpha);
        pose.popPose();
    }

    /**
     * <b>光柱横扫</b>：竖直光柱绕原点扫过一个角度。
     *
     * <p>扫向由 {@code from → to} 的方向决定；扫过的角度随 {@code t} 从 -60° 到 +60°。</p>
     */
    private static void beamSweepFx(PoseStack pose, MultiBufferSource buf,
                                    SpellFxPacket p, Vec3 cam, double t,
                                    float[] rgb, float alpha) {
        Vec3 at = p.from();
        float s = p.size();
        // 基础朝向：from -> to 的水平方向
        float baseDeg = 0.0F;
        try {
            Vec3 b = p.to();
            if (b != null) {
                Vec3 d = b.subtract(at);
                if (d.x * d.x + d.z * d.z > 1.0E-4D) {
                    baseDeg = (float) Math.toDegrees(Math.atan2(d.z, d.x));
                }
            }
        } catch (Throwable ignored) {
            // 默认朝 +X
        }
        float sweep = -60.0F + 120.0F * (float) t;
        // 两端淡出（扫入扫出时不要硬边）
        float fade = (float) (t < 0.12D ? t / 0.12D : 1.0D);
        float a = alpha * fade * 0.95F;
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        SpellFxMesh2.beamSweep(pose, buf, "beam_column", s * 4.5F, s * 0.22F,
                baseDeg + sweep, rgb, a);
        pose.popPose();
    }

    /** <b>螺旋上升</b>：光球沿螺旋被吸上去。 */
    private static void spiralFx(PoseStack pose, MultiBufferSource buf,
                                 SpellFxPacket p, Vec3 cam, double t,
                                 float[] rgb, float alpha) {
        Vec3 at = p.from();
        float s = p.size();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        SpellFxMesh2.spiralAscent(pose, buf, "impact_flare", 7,
                s * 3.0F, s * 0.9F, Math.max(0.12F, s * 0.12F), (float) t,
                rgb, alpha * 0.9F);
        pose.popPose();
    }

    /** <b>环绕</b>：光球绕施法点转。 */
    private static void orbitFx(PoseStack pose, MultiBufferSource buf,
                                SpellFxPacket p, Vec3 cam, double t,
                                float[] rgb, float alpha) {
        Vec3 at = p.from();
        float s = p.size();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + s * 1.1D, at.z - cam.z);
        SpellFxMesh2.orbit(pose, buf, "impact_flare", 4,
                s * 1.1F, 0.0F, Math.max(0.10F, s * 0.10F), (float) t,
                rgb, alpha * 0.85F);
        pose.popPose();
    }

    /** <b>爬行裂纹</b>：贴地生长 + 前缘亮环。 */
    private static void creepingFx(PoseStack pose, MultiBufferSource buf,
                                   SpellFxPacket p, Vec3 cam, double t,
                                   float[] rgb, float alpha) {
        Vec3 at = p.from();
        // PERF(C): 地面高度按特效生命周期缓存（原来每帧打一次 8 格射线）
        double groundY = groundYFor(p);
        float fade = (float) (t < 0.1D ? t / 0.1D : 1.0D - (t - 0.1D) / 0.9D);
        float a = alpha * Math.max(0.0F, Math.min(1.0F, fade));
        if (a <= 0.01F) {
            return;
        }
        pose.pushPose();
        pose.translate(at.x - cam.x, groundY - cam.y + 0.02D, at.z - cam.z);
        SpellFxMesh2.creepingCrack(pose, buf, "ground_crack", p.size() * 2.0F,
                (float) t, rgb, a);
        pose.popPose();
    }

    /**
     * <b>伪泛光</b>：在锚点画一层更大更暗的柔光。
     *
     * <p>真泛光要后期处理，代价太大。这里用"再画一层放大的柔光面片"代替 ——
     * 所有形状都能受益，且只多几张面片。</p>
     *
     * <p>alpha 随 {@code t} 走：爆发瞬间最亮，之后慢慢收。 </p>
     */
    private static void glowPass(PoseStack pose, MultiBufferSource buf,
                                 SpellFxPacket p, Vec3 cam, double t,
                                 float[] rgb, float alpha) {
        Vec3 at = p.from();
        float s = p.size();
        if (s <= 0.01F || alpha <= 0.02F) {
            return;
        }
        float r = s * (1.9F + 0.5F * (float) t);
        float a = alpha * 0.16F * (1.0F - 0.55F * (float) t);
        if (a <= 0.01F) {
            return;
        }
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + s * 0.35D, at.z - cam.z);
        // 三层不同朝向的柔光面片 —— 任何视角都看得到"光晕"
        SpellFxMesh2.shards(pose, buf, "impact_flare", 3, r * 0.55F, r * 0.1F,
                0.35F, rgb, a);
        pose.popPose();
    }

    /**
     * <b>核心闪爆</b>：法术出现后前 12% 时间的一圈亮核。
     *
     * <p>没有这一下的话，很多法术是"直接出现"的，缺少爆发的重量。</p>
     */
    private static void flashPass(PoseStack pose, MultiBufferSource buf,
                                  SpellFxPacket p, Vec3 cam, double t,
                                  float[] rgb, float alpha) {
        if (t > 0.12D) {
            return;
        }
        Vec3 at = p.from();
        float s = p.size();
        float k = (float) (1.0D - t / 0.12D);      // 1 → 0
        float r = Math.max(0.35F, s * (0.5F + 1.6F * (float) t));
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + s * 0.3D, at.z - cam.z);
        // PERF(C): 复用缓冲，不再每帧 new float[]{...}
        FLASH_SCRATCH[0] = Math.min(1.0F, rgb[0] * 1.3F + 0.25F);
        FLASH_SCRATCH[1] = Math.min(1.0F, rgb[1] * 1.3F + 0.25F);
        FLASH_SCRATCH[2] = Math.min(1.0F, rgb[2] * 1.3F + 0.25F);
        SpellFxMesh.sphere(buf, pose, "impact_flare", 6, 10, r * 0.6F,
                FLASH_SCRATCH, alpha * k * 0.85F, 1.5F, 0.0F, 0.0F, 0.0F);
        pose.popPose();
    }

    /** <b>阵法</b>：三重环 + 辐条 + 中心符文（贴地）。 */
    private static void mandalaFx(PoseStack pose, MultiBufferSource buf,
                                  SpellFxPacket p, Vec3 cam, double t,
                                  float[] rgb, float alpha) {
        Vec3 at = p.from();
        // PERF(C): 地面高度按特效生命周期缓存（原来每帧打一次 8 格射线）
        double groundY = groundYFor(p);
        float fade = (float) (t < 0.12D ? t / 0.12D : 1.0D - (t - 0.12D) / 0.88D);
        float a = alpha * Math.max(0.0F, Math.min(1.0F, fade)) * 0.55F;
        if (a <= 0.01F) {
            return;
        }
        pose.pushPose();
        pose.translate(at.x - cam.x, groundY - cam.y + 0.03D, at.z - cam.z);
        // 2026-11-07 R3：阵法贴图缩小（2.2 倍半径 → 1.4 倍），不再铺满下半屏
        SpellFxMesh2.mandala(pose, buf, "torus_soft", p.size() * 1.4F,
                (float) t * 2.2F, rgb, a);
        pose.popPose();
    }

    private static void torusFx(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                                Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        float s = p.size();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + s * 0.6D, at.z - cam.z);
        pose.mulPose(Axis.YP.rotationDegrees((float) (t * 220.0D)));
        // 展开阶段：从小到大（法术"成型"的感觉）
        float grow = (float) Math.min(1.0D, t / 0.30D);
        pose.scale(grow, grow, grow);
        // 主体环：用**管面专用贴图**（横向平滑高光）。
        //
        // ⚠ 原来用 rune_ring_wide —— 那是平面符文环，包到圆环体的管面上
        //   符文会被拉成一圈条纹，看起来像糊了图案的纸圈。
        //   torus_soft 是按管截面画的，配合逐顶点明暗才有管子感。
        SpellFxMesh2.torus(pose, buf, "torus_soft", 36, 12,
                s * 1.5F, s * 0.28F, rgb, alpha);
        // 内圈细环（双层感）
        pose.pushPose();
        pose.scale(0.62F, 1.6F, 0.62F);
        SpellFxMesh2.torus(pose, buf, "shock_ring", 30, 10,
                s * 1.5F, s * 0.14F, rgb, alpha * 0.7F);
        pose.popPose();
        pose.popPose();
    }

    /**
     * <b>飞剑</b>：立体四件套（剑身 / 剑格 / 剑柄 / 剑首）。
     *
     * <p>玩家反馈「法剑的模型也不好看」。原来我复用的是一个金色<b>巨剑</b>形状
     * （给"斩星"用的，又宽又大）—— 环绕时看起来笨重。
     * 现在换成 {@link FlyingSwordMesh}：剑身长宽比 ≈ 9:1，细长灵巧。</p>
     *
     * <p>剑的朝向：<b>指向它的运动方向</b>。穿刺时指向目标；
     * 环绕时没有运动方向可依，就缓慢自转。这一步是"活"与"死"的分界 ——
     * 一把始终朝同一个方向的剑，看起来就是贴图在转。</p>
     */
    private static void flyingSwordFx(PoseStack pose, MultiBufferSource buf,
                                      SpellFxPacket p, Vec3 cam, double t,
                                      float[] rgb, float alpha,
                                      FlyingSwordMesh.Kind kind) {
        Vec3 at = p.from();
        Vec3 to = p.to();
        // 微微上下浮动（静止时也"活"着）
        float bob = Mth.sin((float) (t * 6.0D)) * 0.03F;
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + bob, at.z - cam.z);

        Vec3 dir = to.subtract(at);
        boolean moving = dir.lengthSqr() > 1.0E-4D;
        if (moving) {
            dir = dir.normalize();
            // 把 +Y（剑身方向）转到 dir
            float yaw = (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
            float pitch = (float) -Math.toDegrees(Math.asin(
                    Mth.clamp(dir.y, -1.0D, 1.0D)));
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
        } else {
            pose.mulPose(Axis.YP.rotationDegrees((float) (t * 260.0D)));
        }

        // 尺寸：包里的 size（环绕 1.0 / 穿刺 1.15）乘一个"不要太大"的系数。
        // 乘 0.62 是刻意的：飞剑要细巧，按原尺寸画会像大剑。
        float scale = p.size() * 0.62F;
        String tex = SpellFxPacket.textureOf(p.shape());
        if (moving) {
            FlyingSwordMesh.swordWithTrail(pose, buf, tex, kind, scale, rgb, alpha, 2.2F);
        } else {
            FlyingSwordMesh.sword(pose, buf, tex, kind, scale, rgb, alpha);
        }
        pose.popPose();
    }

    /**
     * <b>立体晶格</b>：三轴交叉的棒条笼，持续自转 + 缓慢收缩（"收紧"的禁锢感）。
     */
    private static void latticeFx(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                                  Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        float s = p.size();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + s * 0.9D, at.z - cam.z);
        pose.mulPose(Axis.YP.rotationDegrees((float) (t * 150.0D)));
        pose.mulPose(Axis.XP.rotationDegrees((float) (t * 60.0D)));
        // 后期收紧
        float shrink = 1.0F - (float) Math.min(0.35D, t * 0.35D);
        pose.scale(shrink, shrink, shrink);
        SpellFxMesh2.lattice(pose, buf, "blade_gold", s * 1.4F, 4, s * 0.16F, rgb, alpha);
        pose.popPose();
    }

    /**
     * <b>双螺旋</b>：两条缠绕的螺旋带一边上升一边旋转。
     */
    private static void helixFx(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                                Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        float s = p.size();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(Axis.YP.rotationDegrees((float) (t * 400.0D)));
        float grow = (float) Math.min(1.0D, t / 0.25D);
        SpellFxMesh2.helix(pose, buf, "beam_soft", 3, (int) (s * 5.0F),
                s * 1.1F, s * 0.5F, 2, rgb, alpha * grow);
        // 顶部一颗亮球收口
        pose.pushPose();
        pose.translate(0.0D, s * 5.0D, 0.0D);
        SpellFxMesh.sphere(buf, pose, "impact_flare", 4, 8, s * 0.5F, rgb,
                alpha * 0.9F * grow);
        pose.popPose();
        pose.popPose();
    }

    /**
     * <b>层叠环塔</b>：多圈半径递减的环叠成塔，整体缓慢自转 + 上升。
     */
    private static void ringTowerFx(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                                    Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        float s = p.size();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(Axis.YP.rotationDegrees((float) (t * 120.0D)));
        int rings = 5;
        for (int i = 0; i < rings; i++) {
            double f = (double) i / rings;
            pose.pushPose();
            // 越往上越小、越往上越晚出现（依次"升"起来）
            double appear = Math.max(0.0D, Math.min(1.0D, (t - f * 0.35D) / 0.25D));
            float rr = (float) (s * (1.6D - f * 0.95D)) * (float) appear;
            if (rr < 0.02F) {
                pose.popPose();
                continue;
            }
            pose.translate(0.0D, s * (0.4D + f * 2.4D), 0.0D);
            SpellFxMesh2.torus(pose, buf, "rune_ring_wide", 30, 10,
                    rr, s * 0.13F, rgb, (float) (alpha * (1.0D - f * 0.55D) * appear));
            pose.popPose();
        }
        pose.popPose();
    }

    /**
     * <b>冰牢</b>（FROST_CAGE，第 29 种形状）：一圈冰晶柱从地面升起并向内合拢，
     * 顶部扣一道冰环把笼子锁上，中心悬一颗内亮的霜核。
     *
     * <p>为什么不是"一个大冰球"：玩家要的是<b>牢</b>。牢的读法是
     * 「围合 + 闭合」—— 所以柱子必须从地面往上长、柱顶朝中心收，
     * 最后用顶环收口；再加一圈地霜把"根"钉在地面上。</p>
     *
     * <p>全部用现成原语：{@link SpellFxMesh#prism}（冰柱）、
     * {@link SpellFxMesh2#torus}（顶环）、{@link SpellFxMesh#cone}（柱顶冰锥/冠刺）、
     * {@link SpellFxMesh#sphereShells}（霜核）、{@link SpellFxMesh#starBurst}（地霜）、
     * {@link SpellFxMesh2#groundCrack}（贴地晶纹）。贴图只传裸名。</p>
     */
    private static void frostCageFx(PoseStack pose, MultiBufferSource buf,
                                    SpellFxPacket p, Vec3 cam, double t,
                                    float[] rgb, float alpha) {
        Vec3 at = p.from();
        // PERF(C): 地面高度按特效生命周期缓存（原来每帧打一次 8 格射线）
        double groundY = groundYFor(p);
        float s = Math.max(0.35F, p.size());
        int layers = Math.max(1, SpellFxQuality.layers());
        // 成长曲线：前 38% 冰柱才从地里长出来（smoothstep，起止都不突兀）
        float k = (float) Math.min(1.0D, t / 0.38D);
        float grow = k * k * (3.0F - 2.0F * k);
        // 局部淡入淡出：叠在全局 alpha 上，避免整座牢"啪"地出现 / 消失
        float fade = (float) (t < 0.12D ? t / 0.12D
                : (t > 0.80D ? 1.0D - (t - 0.80D) / 0.20D : 1.0D));
        float a = alpha * Math.max(0.0F, Math.min(1.0F, fade));
        // ⚠ 所有提前返回都在第一次 pushPose 之前 —— push/pop 绝不会漏
        if (grow <= 0.01F || a <= 0.01F) {
            return;
        }
        float spin = (float) (t * 55.0D);                    // 整座牢缓慢自转
        float cageR = s * (1.05F - 0.10F * grow);             // 收紧：半径略缩
        float cageH = s * (1.90F + 0.50F * grow);             // 高度
        float tilt = 5.0F + 14.0F * grow;                     // 柱顶向心倾角（合拢）
        float avgH = cageH * 0.89F * grow;
        float topR = Math.max(s * 0.18F,
                cageR - (float) Math.sin(Math.toRadians(tilt)) * avgH);

        pose.pushPose();
        pose.translate(at.x - cam.x, groundY - cam.y, at.z - cam.z);

        // ---- ① 地霜：一片贴地的冰晶纹（反着转，和笼子不是一个整体）----
        pose.pushPose();
        pose.translate(0.0D, 0.02D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(-spin * 1.7F));
        SpellFxMesh2.groundCrack(pose, buf, "frost_crystal",
                s * (1.20F + 0.45F * grow), rgb, a * 0.42F);
        pose.popPose();

        // ---- ② 地面放射冰刺：给"牢"一圈向外扎的根（剪影在暗处也认得出）----
        pose.pushPose();
        pose.translate(0.0D, 0.025D, 0.0D);
        SpellFxMesh.starBurst(buf, pose, "frost_crystal", 8,
                s * (0.85F + 0.60F * grow), s * 0.40F * grow, s * 0.045F,
                rgb, a * 0.55F);
        pose.popPose();

        // ---- ③ 冰柱：6 根从地面升起，柱顶朝中心收（"围合感"全靠这一条）----
        for (int i = 0; i < 6; i++) {
            float ang = spin + i * 60.0F;
            // 每根高矮不同（天然结晶不是复制粘贴；同 i 每帧一致，不会抖）
            float hMul = 0.74F + 0.26F * (float) ((i * 37) % 5) / 4.0F;
            float h = cageH * hMul * grow;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(ang));
            pose.translate(cageR, 0.0D, 0.0D);
            // 局部 +Y 向 -X 倾 => 柱顶朝笼心倒（正角度绕 Z 就是往 -X）
            pose.mulPose(Axis.ZP.rotationDegrees(tilt));
            // 外壳：冰晶贴图（棱柱 5 面 + 不规则扰动 —— 是结晶不是水管）
            pose.pushPose();
            pose.translate(0.0D, h * 0.5D, 0.0D);
            SpellFxMesh.prism(buf, pose, "frost_crystal", 5, h, s * 0.17F,
                    i * 23.0F, 0.22F, rgb, a * 0.95F);
            pose.popPose();
            // 内芯：管面贴图给"实心冰"的柱体（只有晶纹的话柱子会是空心的）
            pose.pushPose();
            pose.translate(0.0D, h * 0.5D, 0.0D);
            SpellFxMesh.prism(buf, pose, "torus_soft", 5, h * 0.96F, s * 0.10F,
                    i * 23.0F + 18.0F, 0.10F, rgb, a * 0.55F);
            pose.popPose();
            // 柱顶冰锥（继续沿柱子方向收成尖端）
            pose.pushPose();
            pose.translate(0.0D, h, 0.0D);
            SpellFxMesh.cone(buf, pose, "frost_crystal", 4, h * 0.42F,
                    s * 0.09F, rgb, a * 0.85F);
            pose.popPose();
            pose.popPose();
        }

        // ---- ④ 顶环：把六根柱子"锁"在一起（层数跟画质档走）----
        float ringY = avgH;
        for (int i = 0; i < layers; i++) {
            float rr = Math.max(s * 0.16F, topR * (1.0F - 0.10F * i));
            pose.pushPose();
            pose.translate(0.0D, ringY * (1.0F - 0.10F * i), 0.0D);
            pose.mulPose(Axis.YP.rotationDegrees(-spin * 0.8F));
            SpellFxMesh2.torus(pose, buf, "torus_soft", 20, 6,
                    rr, s * (0.055F - 0.008F * i), rgb, a * (0.90F - 0.22F * i));
            pose.popPose();
        }

        // ---- ⑤ 冰冠：顶环上一圈向外张的冰刺（牢顶的"牙"）----
        for (int i = 0; i < 8; i++) {
            float ang = -spin + i * 45.0F;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(ang));
            pose.translate(topR, ringY, 0.0D);
            pose.mulPose(Axis.ZP.rotationDegrees(-26.0F));   // 负角度绕 Z => 向外张
            SpellFxMesh.cone(buf, pose, "frost_crystal", 4, s * 0.36F * grow,
                    s * 0.06F, rgb, a * 0.75F);
            pose.popPose();
        }

        // ---- ⑥ 霜核：笼心一颗内亮的结晶（球壳层数跟画质档）----
        float pulse = 0.86F + 0.14F * (float) Math.sin(t * Math.PI * 6.0D);
        pose.pushPose();
        pose.translate(0.0D, cageH * grow * 0.52D, 0.0D);
        SpellFxMesh.sphereShells(buf, pose, "impact_flare", 4 + layers,
                10 + layers * 2, s * 0.26F, layers, 0.16F, rgb, a * 0.90F * pulse);
        pose.popPose();

        pose.popPose();
    }

    /**
     * <b>血符</b>（BLOOD_SIGIL，第 30 种形状）：贴地的暗红符文法阵 +
     * 逐层抬升的血环 + 一圈獠牙 + 中心血核。
     *
     * <p>风格线是 <b>F-3 灾变暗黑</b>，只给血修/魔修用：颜色压暗、发光面积克制，
     * 亮的东西只留"血核 + 少量符文交点"。法阵贴图本身已经画好
     * 同心环 / 辐条 / 缺口，几何只负责让它"立起来、动起来、咬起来"。</p>
     *
     * <p>几何：{@link SpellFxMesh2#groundCrack}（法阵贴花）、
     * {@link SpellFxMesh#starBurst}（外圈血齿）、{@link SpellFxMesh2#torus}（上升血环）、
     * {@link SpellFxMesh#cone}（獠牙）、{@link SpellFxMesh#sphereShells}（血核）、
     * {@link SpellFxMesh2#filaments}（上升血丝）。贴图只传裸名。</p>
     */
    private static void bloodSigilFx(PoseStack pose, MultiBufferSource buf,
                                     SpellFxPacket p, Vec3 cam, double t,
                                     float[] rgb, float alpha) {
        Vec3 at = p.from();
        // PERF(C): 地面高度按特效生命周期缓存（原来每帧打一次 8 格射线）
        double groundY = groundYFor(p);
        float s = Math.max(0.35F, p.size());
        int layers = Math.max(1, SpellFxQuality.layers());
        // 铺开：前 22% 法阵从中心"印"到最大（smoothstep）
        float k = (float) Math.min(1.0D, t / 0.22D);
        float open = k * k * (3.0F - 2.0F * k);
        float fade = (float) (t < 0.10D ? t / 0.10D
                : (t > 0.82D ? 1.0D - (t - 0.82D) / 0.18D : 1.0D));
        float a = alpha * Math.max(0.0F, Math.min(1.0F, fade));
        // ⚠ 所有提前返回都在第一次 pushPose 之前 —— push/pop 绝不会漏
        if (open <= 0.01F || a <= 0.01F) {
            return;
        }
        float spin = (float) (t * 42.0D);
        float radius = s * (1.15F + 0.55F * open);

        pose.pushPose();
        pose.translate(at.x - cam.x, groundY - cam.y, at.z - cam.z);

        // ---- ① 法阵贴花：暗红符文圆盘（正转；同心环/辐条/缺口都在贴图里）----
        pose.pushPose();
        pose.translate(0.0D, 0.02D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        SpellFxMesh2.groundCrack(pose, buf, "blood_sigil", radius, rgb, a * 0.95F);
        pose.popPose();

        // ---- ② 外圈血齿：贴地放射星芒 + 逆时针转（和法阵反向 => 机关在咬合）----
        pose.pushPose();
        pose.translate(0.0D, 0.035D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(-spin * 1.6F));
        SpellFxMesh.starBurst(buf, pose, "blood_sigil", 12,
                radius * 0.94F, radius * 0.64F, s * 0.040F, rgb, a * 0.55F);
        pose.popPose();

        // ---- ③ 上升血环：几道细环从法阵里"浮起来"（层数跟画质档）----
        for (int i = 0; i < layers; i++) {
            float f = layers <= 1 ? 0.0F : (float) i / (layers - 1);
            float ry = s * (0.06F + 0.30F * (float) t + 0.30F * f);
            float rr = Math.max(s * 0.20F, radius * (0.92F - 0.26F * f));
            pose.pushPose();
            pose.translate(0.0D, ry, 0.0D);
            pose.mulPose(Axis.YP.rotationDegrees(spin * (0.6F + 0.5F * f)));
            SpellFxMesh2.torus(pose, buf, "torus_soft", 20, 6,
                    rr, s * (0.045F - 0.006F * i), rgb, a * (0.80F - 0.20F * f));
            pose.popPose();
        }

        // ---- ④ 一圈獠牙：向外斜张的暗红尖锥（F-3 的"牙"）----
        for (int i = 0; i < 6; i++) {
            float ang = -spin + i * 60.0F;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(ang));
            pose.translate(radius * 0.74F, 0.03D, 0.0D);
            pose.mulPose(Axis.ZP.rotationDegrees(-30.0F));    // 负角度绕 Z => 向外张
            SpellFxMesh.cone(buf, pose, "blood_sigil", 4,
                    s * 0.58F * open, s * 0.075F, rgb, a * 0.85F);
            // 内芯给牙"实体感"（只有带缺口的贴图会显得发虚）
            pose.pushPose();
            pose.scale(0.55F, 0.92F, 0.55F);
            SpellFxMesh.cone(buf, pose, "torus_soft", 4,
                    s * 0.58F * open, s * 0.075F, rgb, a * 0.45F);
            pose.popPose();
            pose.popPose();
        }

        // ---- ⑤ 上升血丝：细带沿螺旋往上收（"血在往上走"）----
        pose.pushPose();
        pose.translate(0.0D, 0.02D, 0.0D);
        SpellFxMesh2.filaments(pose, buf, "blood_sigil", 4, s * 0.95F * open,
                s * 0.36F, s * 0.045F, rgb, a * 0.42F);
        pose.popPose();

        // ---- ⑥ 血核：法阵中心一颗脉动的球（发光只给这里，面积克制）----
        float pulse = 0.84F + 0.16F * (float) Math.sin(t * Math.PI * 6.0D);
        pose.pushPose();
        pose.translate(0.0D, s * (0.26F + 0.18F * (float) t), 0.0D);
        SpellFxMesh.sphereShells(buf, pose, "impact_flare", 4 + layers,
                10 + layers * 2, s * 0.20F, layers, 0.16F, rgb, a * 0.88F * pulse);
        pose.popPose();

        pose.popPose();
    }

    /** <b>陨石（立体）</b>：从<b>天外</b>斜着砸下来 —— 前 78% 时间都在下落。 */
    private static void meteor(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                              Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        // 玩家要求：「陨石要从天上掉下来」。
        // 所以下落段拉长到 78%：起始高度 90 格、水平偏移 45 格（从斜上方天外飞来），
        // 用二次缓动（越接近地面越快）做出"加速坠落"的观感。
        double k = Math.min(1.0D, t / 0.78D);
        double ease = k * k;                        // 加速下落
        double dy = 90.0D * (1.0D - ease);
        double dx = 45.0D * (1.0D - ease);
        double dz = 34.0D * (1.0D - ease);
        float r = (float) (p.size() * (3.4D - 1.7D * ease));

        pose.pushPose();
        pose.translate(at.x - cam.x + dx, at.y - cam.y + dy, at.z - cam.z + dz);
        // 自转：让"立体"看得出来
        pose.mulPose(Axis.YP.rotationDegrees((float) (t * 520.0D)));
        pose.mulPose(Axis.XP.rotationDegrees((float) (t * 380.0D)));
        // 岩块：8 棱不规则棱柱（带扰动 → 像石头）
        SpellFxMesh.prism(buf, pose, "meteor_rock", 8, r * 1.5F, r, 0.0F, 0.30F,
                rgb, alpha);
        // 尾部火焰锥（朝上来向）
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        SpellFxMesh.cone(buf, pose, "beam_soft", 7, r * 2.6F, r * 0.85F, rgb, alpha * 0.75F);
        pose.popPose();
        // 内核亮球
        SpellFxMesh.sphere(buf, pose, "impact_flare", 4, 8, r * 0.55F, rgb, alpha * 0.9F);
        pose.popPose();
    }

    /**
     * <b>金色巨剑（立体）</b>：从<b>天顶</b>劈下来 —— 前 62% 时间都在下落。
     *
     * <p>玩家要求：「巨剑也要从天上斩下」。所以起始高度拉到 80 格、
     * 用加速曲线落下，落地那一刻最亮（{@code drop == 0}）。</p>
     */
    private static void blade(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                             Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        // 前 62% 下落（加速），之后停在地面附近做余韵
        double k = Math.min(1.0D, t / 0.62D);
        double ease = k * k;
        double drop = 80.0D * (1.0D - ease);
        // 落地后略微放大（斩击的"炸开"感）
        float s = p.size() * (t < 0.62D
                ? 0.55F + 0.75F * (float) k
                : 1.30F + 0.20F * (float) Math.min(1.0D, (t - 0.62D) / 0.38D));

        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + drop, at.z - cam.z);
        pose.mulPose(Axis.ZP.rotationDegrees(6.0F));
        // 剑身：扁棱柱（x 宽、z 薄 —— 这就是"有厚度的剑身"）
        pose.pushPose();
        pose.scale(0.42F, 1.0F, 0.10F);
        SpellFxMesh.prism(buf, pose, "blade_gold", 4, s * 3.4F, s * 0.55F, 45.0F, 0.0F,
                rgb, alpha);
        pose.popPose();
        // 护手
        pose.pushPose();
        pose.translate(0.0D, s * 1.75D, 0.0D);
        pose.scale(1.0F, 0.16F, 0.16F);
        SpellFxMesh.prism(buf, pose, "blade_gold", 4, s * 2.3F, s * 0.55F, 45.0F, 0.0F,
                rgb, alpha * 0.95F);
        pose.popPose();
        // 剑柄
        pose.pushPose();
        pose.translate(0.0D, s * 2.3D, 0.0D);
        pose.scale(0.24F, 1.0F, 0.24F);
        SpellFxMesh.prism(buf, pose, "blade_gold", 6, s * 1.4F, s * 0.5F, 0.0F, 0.0F,
                rgb, alpha * 0.9F);
        pose.popPose();
        // 剑尖（圆锥，朝下）
        pose.pushPose();
        pose.translate(0.0D, -s * 1.7D, 0.0D);
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.scale(0.42F, 1.0F, 0.42F);
        SpellFxMesh.cone(buf, pose, "blade_gold", 4, s * 0.9F, s * 0.55F, rgb, alpha);
        pose.popPose();
        pose.popPose();
    }

    /** <b>光柱（立体圆柱筒）</b>：多层同心，越里越亮。 */
    private static void beam(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                            Vec3 cam, double t, float[] rgb, float alpha,
                            String tex, float widthMul, int sides) {
        Vec3 a = p.from();
        Vec3 b = p.to();
        Vec3 mid = a.add(b).scale(0.5D);
        double len = a.distanceTo(b);
        if (len < 0.05D) {
            return;
        }
        double grow = t < 0.18D ? t / 0.18D : (t > 0.72D ? (1.0D - t) / 0.28D : 1.0D);
        float w = (float) (p.size() * widthMul * Mth.clamp(grow, 0.0D, 1.0D));

        pose.pushPose();
        pose.translate(mid.x - cam.x, mid.y - cam.y, mid.z - cam.z);
        Vec3 dir = b.subtract(a).normalize();
        double yaw = Math.toDegrees(Math.atan2(dir.x, dir.z));
        double pitch = Math.toDegrees(Math.asin(Mth.clamp(dir.y, -1.0D, 1.0D)));
        pose.mulPose(Axis.YP.rotationDegrees((float) -yaw));
        pose.mulPose(Axis.XP.rotationDegrees((float) (pitch - 90.0D)));

        int layers = Math.max(1, SpellFxQuality.layers());
        for (int i = 0; i < layers; i++) {
            float r = w * (1.0F - i * 0.22F);
            SpellFxMesh.cylinder(buf, pose, i == 0 ? tex : "beam_soft", sides,
                    (float) len, r, rgb, alpha * (i == 0 ? 1.0F : 0.5F));
        }
        pose.popPose();
    }

    /** <b>虚空裂缝（立体）</b>：两片正交锯齿面 —— 从任何角度看都有体积。 */
    private static void crack(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                             Vec3 cam, double t, float[] rgb, float alpha, Minecraft mc) {
        Vec3 at = p.from();
        float s = p.size() * (t < 0.25D ? (float) (t / 0.25D) : 1.0F);
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees((float) (t * 40.0D)));
        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 90.0F));
            quad(buf, pose, "crack_void", s * 2.0F, s * 0.9F, rgb, alpha);
            pose.popPose();
        }
        pose.popPose();
    }

    /** <b>冲击环（立体）</b>：贴地环 + 管状圆环 + 立体星芒（八角三棱锥）。 */
    private static void shockRing(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                                 Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        // 2026-11-07 R3：扩张幅度收敛（0.25+1.35t → 0.20+0.85t），贴地大环不再盖住半屏
        float r = (float) (p.size() * (0.20D + 0.85D * t));
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + 0.08D, at.z - cam.z);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        quad(buf, pose, "shock_ring", r * 2.0F, r * 2.0F, rgb, alpha * 0.70F);
        pose.popPose();
        // 2026-11-01 3D 化：再加一圈**管状圆环**（有厚度、有高光）。
        // 原来只有"平面贴图 + 星芒"，侧看像一张纸；管环从任何角度看都是实体。
        SpellFxMesh2.torus(pose, buf, "torus_soft", 28, 8,
                r * 0.96F, Math.max(0.04F, r * 0.07F), rgb, alpha * 0.75F);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        SpellFxMesh.starBurst(buf, pose, "impact_flare", 8, r * 1.15F, r * 0.7F,
                r * 0.10F, rgb, alpha * 0.85F);
        pose.popPose();
    }

    /** <b>冲击穹顶（立体）</b>：球壳。 */
    private static void dome(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                            Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        float r = (float) (p.size() * (0.35D + 0.9D * t));
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        // 双层穹顶：外壳淡而大（气浪）、内壳亮而小（本体）
        int shells = Math.max(1, Math.min(3, SpellFxQuality.layers()));
        SpellFxMesh.sphereShells(buf, pose, "shock_dome", 5 + shells, 12 + shells * 2, r,
                shells, 0.42F, rgb, alpha * 0.75F);
        pose.popPose();
    }

    /** <b>爆闪（立体）</b>：内核球 + 立体星芒。 */
    private static void flare(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                             Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        double sc = t < 0.16D ? t / 0.16D : (1.0D - 0.45D * t);
        float r = (float) (p.size() * Math.max(0.05D, sc));
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        // 多层同心壳：内亮外暗 —— 单层球从侧面看就是一条线，
        // 多层才会读成「一个内部在发光的球」（玩家说"贴图贴上就完事"的正是这个）
        // 层数**跟随画质档位**（低配 1 层、高配 3 层）——
        // 写死层数会绕过玩家自己的性能设置。
        int shells = Math.max(1, Math.min(3, SpellFxQuality.layers()));
        SpellFxMesh.sphereShells(buf, pose, "impact_flare", 4 + shells, 10 + shells * 2,
                r * 0.75F, shells, 0.16F, rgb, alpha);
        SpellFxMesh.starBurst(buf, pose, "impact_flare", 8, r * 1.5F, r * 0.95F,
                r * 0.10F, rgb, alpha * 0.9F);
        pose.popPose();
    }

    /** <b>符文环（立体化）</b>：主环 + 两圈上浮小环 + 细管环（贴地法阵有层次）。 */
    private static void runeRing(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                                Vec3 cam, double t, float[] rgb, float alpha) {
        Vec3 at = p.from();
        // 2026-11-07 R3：与冲击环同步收敛（0.25+1.35t → 0.20+0.85t）
        float r = (float) (p.size() * (0.20D + 0.85D * t));
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + 0.07D, at.z - cam.z);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (t * 220.0D)));
        quad(buf, pose, "rune_ring_wide", r * 2.0F, r * 2.0F, rgb, alpha * 0.70F);
        pose.popPose();
        // 2026-11-01 3D 化：主环上方叠 2 圈更小、反向旋转的符文环 ——
        // 平面法阵变成"有层次的立体法阵"，侧面不再是一张纸
        for (int i = 1; i <= 2; i++) {
            pose.pushPose();
            pose.translate(at.x - cam.x, at.y - cam.y + 0.07D + i * 0.16D,
                    at.z - cam.z);
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees((float) (t * (220.0D - i * 80.0D))));
            float k = 1.0F - i * 0.24F;
            quad(buf, pose, "rune_ring_wide", r * 2.0F * k, r * 2.0F * k,
                    rgb, alpha * (0.35F - i * 0.10F));
            pose.popPose();
        }
        // 一圈细管环：给法阵边缘一个实体高光（任何角度都能看出"有厚度"）
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + 0.10D, at.z - cam.z);
        SpellFxMesh2.torus(pose, buf, "torus_soft", 26, 6,
                r * 0.92F, Math.max(0.03F, r * 0.045F), rgb, alpha * 0.6F);
        pose.popPose();
    }

    /** <b>雪花</b>：三片正交面片（立体，不是单张纸）。 */
    private static void snow(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                            Vec3 cam, double t, float[] rgb, float alpha, Minecraft mc) {
        Vec3 at = p.from();
        float r = (float) (p.size() * 0.5D * (0.35D + 0.9D * t));
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y + 0.06D, at.z - cam.z);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        for (int i = 0; i < 3; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 60.0F));
            pose.mulPose(Axis.ZP.rotationDegrees((float) (t * 160.0D)));
            quad(buf, pose, "snow_flake", r * 2.0F, r * 2.0F, rgb, alpha * 0.8F);
            pose.popPose();
        }
        pose.popPose();
    }

    /** <b>花瓣旋涡（立体）</b>：多圈不同高度的花瓣绕轴旋转。 */
    private static void petal(PoseStack pose, MultiBufferSource buf, SpellFxPacket p,
                             Vec3 cam, double t, float[] rgb, float alpha, Minecraft mc) {
        Vec3 at = p.from();
        float r = (float) (p.size() * (t < 0.16D ? t / 0.16D : (1.0D - 0.45D * t)));
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        int layers = Math.max(1, SpellFxQuality.layers());
        for (int i = 0; i < layers; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees((float) (t * 260.0D) + i * 40.0F));
            pose.translate(0.0D, (i - 1) * r * 0.35D, 0.0D);
            quad(buf, pose, "petal_swirl", r * 1.8F, r * 1.8F, rgb,
                    alpha * (i == 0 ? 1.0F : 0.6F));
            pose.popPose();
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------
    // 基础
    // ------------------------------------------------------------------

    private static void quad(MultiBufferSource buf, PoseStack pose, String tex,
                             float w, float h, float[] rgb, float alpha) {
        VertexConsumer vc = buf.getBuffer(SpellFxMesh.type(tex));
        Matrix4f m = pose.last().pose();
        float hw = w * 0.5F;
        float hh = h * 0.5F;
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) & 0xFF;
        vertex(vc, m, -hw, -hh, 0.0F, rgb, a, 0.0F, 1.0F);
        vertex(vc, m, hw, -hh, 0.0F, rgb, a, 1.0F, 1.0F);
        vertex(vc, m, hw, hh, 0.0F, rgb, a, 1.0F, 0.0F);
        vertex(vc, m, -hw, hh, 0.0F, rgb, a, 0.0F, 0.0F);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z,
                               float[] rgb, int a, float u, float v) {
        vc.vertex(m, x, y, z)
                .color(rgb[0], rgb[1], rgb[2], a / 255.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    /** 元素颜色 + 随时间的光色漂移（与粒子特效同一套配色）。 */
    private static float[] colorOf(int elementOrdinal, double t) {
        // PERF(C): Element.values() 每次都会克隆数组；颜色写进复用缓冲，
        // 不再每帧每条特效 new 一个 float[3]。
        Element el = ELEMENTS[Math.floorMod(elementOrdinal, ELEMENTS.length)];
        int c = el.color();
        float r = ((c >> 16) & 0xFF) / 255.0F;
        float g = ((c >> 8) & 0xFF) / 255.0F;
        float b = (c & 0xFF) / 255.0F;
        float k = (float) (0.10D * Math.sin(t * Math.PI * 2.0D));
        COLOR_SCRATCH[0] = Mth.clamp(r + k, 0.0F, 1.0F);
        COLOR_SCRATCH[1] = Mth.clamp(g, 0.0F, 1.0F);
        COLOR_SCRATCH[2] = Mth.clamp(b - k * 0.6F, 0.0F, 1.0F);
        return COLOR_SCRATCH;
    }

    /**
     * PERF(C): 贴地特效的"地面高度"。
     *
     * <p>原来 5 种贴地形状（裂纹/法阵/冰牢/血符）<b>每帧</b>都要
     * {@code level.clip(...)} 从原点向下打 8 格射线重新找地面，而且重复画的
     * 本体 + 辉光层会各算一次。地面在一条特效 2~5 秒的寿命里不会变，
     * 所以缓存在 {@link ClientSpellFx.Fx#groundY} 里，整个生命周期只算一次。</p>
     */
    private static double groundYFor(SpellFxPacket p) {
        ClientSpellFx.Fx fx = currentFx;
        if (fx != null) {
            if (!Double.isNaN(fx.groundY)) {
                return fx.groundY;
            }
            double y = computeGroundY(p);
            fx.groundY = y;
            return y;
        }
        return computeGroundY(p);
    }

    /** 从特效原点向下最多 8 格找地面；找不到就用原点下方 1 格。 */
    private static double computeGroundY(SpellFxPacket p) {
        Vec3 at = p.from();
        Minecraft mc = Minecraft.getInstance();
        double groundY = at.y - 1.0D;
        if (mc.level != null) {
            try {
                var from = new net.minecraft.world.phys.Vec3(at.x, at.y + 0.5D, at.z);
                var to = from.add(0.0D, -8.0D, 0.0D);
                var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                        from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                    groundY = hit.getLocation().y;
                }
            } catch (Throwable ignored) {
                // 取不到就用默认值
            }
        }
        return groundY;
    }

    /** 透明度包络：进场快、保持久、退场缓（玩家要"释放后缓慢消失"）。 */
    private static float alphaOf(SpellFxPacket p, double t) {
        float power = Mth.clamp(p.power(), 0.2F, 2.0F);
        double in = t < 0.10D ? t / 0.10D : 1.0D;
        double out = t > 0.55D ? 1.0D - (t - 0.55D) / 0.45D * 0.92D : 1.0D;
        // 2026-11-07：整体峰值 0.85 → 0.70（玩家第二轮「还是下调」）
        return (float) (Mth.clamp(in * out, 0.0D, 1.0D) * 0.70D * power);
    }
}
