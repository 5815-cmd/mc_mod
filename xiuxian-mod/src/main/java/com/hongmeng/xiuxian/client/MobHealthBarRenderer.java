package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

/**
 * 怪物头顶血条 + 品阶外观（光环 / 护甲片 / 光晕）。
 *
 * <h2>为什么用 {@code RenderNameTagEvent}（而不是自定义 RenderLayer）</h2>
 *
 * <p>看过原版字节码之后再选的，理由有三条，每一条都能验证：</p>
 *
 * <ol>
 *   <li><b>位置天生就是对的。</b>Forge 把 {@code RenderNameTagEvent}
 *       打在 {@code EntityRenderer.render} 的<b>最开头</b>
 *       （字节码里事件构造在第 0 条指令），此时 {@link PoseStack} 停在
 *       "实体脚底、未旋转"的世界坐标上——而原版画名牌时用的锚点是
 *       {@code Entity#getNameTagOffsetY()}（= {@code bbHeight + 0.5}，
 *       见 {@code Entity} 的字节码）。所以我只要照抄同样的
 *       {@code translate → cameraOrientation → scale(-0.025,-0.025,0.025)}
 *       就落在名牌的同一套局部坐标里，<b>血条与名牌永远不会错位</b>。</li>
 *   <li><b>一个钩子管住所有怪。</b>自定义 RenderLayer 必须在每个渲染器的构造器里
 *       {@code addLayer}（本模组有 10 个怪渲染器）。事件只需要注册一次，
 *       以后谁再加一只妖兽都自动带上血条——不会再出现"新怪忘了加层"。</li>
 *   <li><b>能省掉一次模型重画。</b>事件带了 {@code getMultiBufferSource()} 与
 *       {@code getPackedLight()}，所以光环、护甲片、光晕这类"世界坐标里就能画完"
 *       的东西不必再叠一层 RenderLayer。真正需要模型空间的只有"发光眼睛"
 *       （见 {@link RankGlowLayer}），那才用 RenderLayer。</li>
 * </ol>
 *
 * <p>副作用是好的：事件<b>无论 {@code shouldShowName()} 是真是假都会触发</b>
 * （看字节码：先 post 事件，再判断 {@code Result.DENY} 与 {@code shouldShowName}），
 * 所以普通怪"没被看着时不显示名牌"的既有表现不变，血条却照样画得出来。</p>
 *
 * <h2>血条</h2>
 *
 * <ul>
 *   <li>颜色取自 {@link MobRank#auraColor()}（与 BOSS 条、光晕同源，只有一份表）；</li>
 *   <li>精英及以上额外写数字 {@code 当前 / 上限}（{@link MobRank#showsHealthNumbers()}），
 *       大数字沿用 {@link HealthBarOverlay#formatHp(float)} 的"万/亿"口径；</li>
 *   <li>血量本来就由原版同步，<b>不需要任何新包</b>。</li>
 * </ul>
 *
 * <h2>品阶外观（可验证的机制，不是形容词）</h2>
 *
 * <pre>
 *   普通  体型 ×1.00                          什么都不加
 *   精英  体型 ×1.05  发光眼睛（附加发光贴图）   + 灵气粒子
 *   队长  体型 ×1.12  ＋肩甲片（4 面）           + 粒子 + 足下光环
 *   头目  体型 ×1.20  ＋腰带甲                   + 更多粒子 + 体表光晕
 *   首领  体型 ×1.32  ＋头冠甲（三重甲）         + 最强粒子 + 最大光晕
 * </pre>
 *
 * <p>数值全部来自 {@link MobRank#look()}，本类<b>一个写死的倍率都没有</b>。</p>
 *
 * <p>人形怪（{@link CultivationHumanoidRenderer}）另有真实模型部件——护肩与披风，
 * 由品阶强制打开（见 {@code CultivationHumanoidRenderer.Variant}），
 * 所以它们<b>不再叠画四边形护甲片</b>，免得"两层护甲"糊在一起。</p>
 */
@OnlyIn(Dist.CLIENT)
public class MobHealthBarRenderer {

    /**
     * 纯白 16×16 贴图。
     *
     * <p>血条、护甲片、光环、光晕全都用它 + 顶点色：<b>一张贴图办完所有事</b>。
     * 贴图是纯白的，所以 UV 指到哪都一样，不存在"UV 越界糊色"的问题。</p>
     */
    public static final ResourceLocation PLATE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/rank_plate.png");

    /**
     * 血条底板 / 护甲片 / 光环 / 光晕共用的渲染类型：**只建一次**。
     *
     * <p><b>为什么必须缓存：</b>每帧新建 RenderType 会让
     * {@code BufferSource.getBuffer()} 对不在 {@code fixedBuffers} 里的类型
     * 反复分配 {@code ByteBufferBuilder}，把渲染资源 churn 坏 ——
     * 表现是 {@code LevelRenderer.renderChunkLayer} 读到 null 着色器而崩客户端
     * （玩家 15:19 / 15:25 两次崩溃就是这个签名）。</p>
     *
     * <p>不吃光照、正反面都画（NO_CULL），夜里也看得清。</p>
     */
    private static final RenderType PLATE_TYPE =
            RenderType.entityTranslucentEmissive(PLATE);

    /** 不吃光照的半透明渲染类型：夜里也看得清，且正反面都画（NO_CULL）。 */
    public static RenderType plateType() {
        // 缓存！理由同 RankGlowLayer：每帧新建 RenderType 会让 BufferSource
        // 反复分配 ByteBufferBuilder，把渲染资源 churn 坏。
        return PLATE_TYPE;
    }

    // ---- 血条尺寸（单位是名牌局部坐标：1 单位 = 0.025 格）----
    private static final float BAR_HALF_WIDTH = 30.0F;
    private static final float BAR_TOP = 11.0F;
    private static final float BAR_BOTTOM = 22.0F;
    /** 数字相对条内文字整体缩放：8px 的字塞进 9 单位高的条里。 */
    private static final float TEXT_SCALE = 0.75F;

    /**
     * 血条沿视线方向的前移量（名牌局部单位，1 单位 = 0.025 格）。
     *
     * <p>原版名牌只前移 0.03 单位——因为它假设"名牌永远在模型头顶之上"。
     * 本模组这个前提不成立：品阶体型是在<b>渲染层放大</b>的，模型可以比命中箱还高
     * （石傀模型 3.08 格 / 命中箱 2.80 格），于是"贴着 bbHeight+0.5 画"的血条
     * 会有一截落进头里、被深度测试切掉，看起来像断成两半。</p>
     *
     * <p>这里把血条整体沿<b>视线方向</b>前移 0.25 格。billboard 平面上的纯深度位移
     * <b>不改变屏幕上的位置</b>，只保证它永远画在模型前面。</p>
     */
    private static final float BAR_DEPTH = 10.0F;

    /** 超过这个距离就不画血条了（普通的怪）。 */
    private static final double MAX_DISTANCE = 40.0D;
    /** 队长及以上看得更远一点。 */
    private static final double MAX_DISTANCE_RARE = 56.0D;

    @SubscribeEvent
    public void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof CultivationMonsterEntity mob)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!Minecraft.renderNames() || mob.isInvisible()) {
            return;
        }
        MobRank rank = mob.rank();
        double maxDistance = rank.isRare() ? MAX_DISTANCE_RARE : MAX_DISTANCE;
        double distanceSq = mc.getEntityRenderDispatcher().distanceToSqr(mob);
        if (distanceSq > maxDistance * maxDistance) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();

        // ---- 世界坐标部分：足下光环 / 护甲片 / 光晕 ----
        pose.pushPose();
        rankDecoration(pose, buffer, light, mob, rank, event.getEntityRenderer());
        pose.popPose();

        // ---- 名牌局部坐标部分：血条 ----
        pose.pushPose();
        pose.translate(0.0F, mob.getNameTagOffsetY(), 0.0F);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);
        healthBar(pose, buffer, light, mob, rank);
        pose.popPose();
    }

    // ------------------------------------------------------------------
    // 血条
    // ------------------------------------------------------------------

    private void healthBar(PoseStack pose, MultiBufferSource buffer, int light,
                           CultivationMonsterEntity mob, MobRank rank) {
        float max = Math.max(1.0F, mob.getMaxHealth());
        float current = Mth.clamp(mob.getHealth(), 0.0F, max);
        float ratio = current / max;

        int rgb = rank.auraColor();
        int fill = 0xFF000000 | rgb;
        int track = 0xC0000000 | darken(rgb, 0.35F);
        int border = 0xFF000000 | darken(rgb, 0.55F);

        VertexConsumer vc = buffer.getBuffer(plateType());
        PoseStack.Pose last = pose.last();

        // 外框 → 底槽 → 填充 → 顶部高光（全部前移 BAR_DEPTH，见该常量的说明）
        quad(last, vc, -BAR_HALF_WIDTH - 1.0F, BAR_TOP - 1.0F,
                BAR_HALF_WIDTH + 1.0F, BAR_BOTTOM + 1.0F, border, 255, BAR_DEPTH);
        quad(last, vc, -BAR_HALF_WIDTH, BAR_TOP, BAR_HALF_WIDTH, BAR_BOTTOM, track, 230,
                BAR_DEPTH);
        float filled = -BAR_HALF_WIDTH + (BAR_HALF_WIDTH * 2.0F) * ratio;
        if (filled > -BAR_HALF_WIDTH + 0.01F) {
            quad(last, vc, -BAR_HALF_WIDTH, BAR_TOP, filled, BAR_BOTTOM, fill, 245,
                    BAR_DEPTH);
            quad(last, vc, -BAR_HALF_WIDTH, BAR_TOP, filled, BAR_TOP + 2.0F,
                    0xFF000000 | lighten(rgb, 0.45F), 110, BAR_DEPTH);
        }

        if (!rank.showsHealthNumbers()) {
            return;
        }
        // 数字：精英及以上才写。整体缩到 0.75 才塞得进 9 单位高的条里。
        String text = HealthBarOverlay.formatHp(current) + " / " + HealthBarOverlay.formatHp(max);
        Font font = Minecraft.getInstance().font;
        pose.pushPose();
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        float textX = -font.width(text) / 2.0F;
        float textY = (BAR_TOP + 1.0F + (BAR_BOTTOM - 1.0F - (BAR_TOP + 1.0F)) / 2.0F)
                / TEXT_SCALE - font.lineHeight / 2.0F;
        Matrix4f matrix = pose.last().pose();
        font.drawInBatch(text, textX, textY, 0xFFFFFFFF, true, matrix, buffer,
                Font.DisplayMode.NORMAL, 0, light);
        pose.popPose();
    }

    // ------------------------------------------------------------------
    // 品阶外观
    // ------------------------------------------------------------------

    /**
     * 足下光环 / 护甲片 / 体表光晕。
     *
     * <p>全部画在<b>实体脚底、未旋转</b>的世界坐标里，所以：</p>
     * <ul>
     *   <li>水平光环与朝向无关，任何角度看都对；</li>
     *   <li>护甲片做成<b>四面环绕</b>（不是单独一片），于是"怪转身了甲片还在肩上"；</li>
     *   <li>体表光晕用相机朝向做 billboard，永远正对玩家。</li>
     * </ul>
     */
    private void rankDecoration(PoseStack pose, MultiBufferSource buffer, int light,
                                CultivationMonsterEntity mob, MobRank rank,
                                Object renderer) {
        int aura = rank.auraLevel();
        int armor = rank.armorLevel();
        if (aura <= 0 && armor <= 0) {
            return;
        }
        float w = Math.max(0.25F, mob.getBbWidth());
        float h = Math.max(0.35F, mob.getBbHeight());
        int rgb = rank.auraColor();
        VertexConsumer vc = buffer.getBuffer(plateType());

        // ---- 足下光环：贴地一圈，品阶越高越大越亮 ----
        if (aura >= 2) {
            float r = w * 0.85F + 0.10F * aura;
            pose.pushPose();
            pose.translate(0.0F, 0.03F, 0.0F);
            flatRing(pose.last(), vc, r, rgb, 55 + 22 * aura);
            pose.popPose();
        }

        // ---- 护甲片：**已移除**（玩家反馈「白框是怪物四周的白色框，呈长方形」）----
        //
        // 原来这里用 armourBand() 在怪身体四周画四片矩形平面（肩甲 / 腰带甲 / 头冠甲），
        // 颜色是 mix(rgb, 0xFFFFFF, 0.32~0.55)（往白里调）、不透明度 205~220 ——
        // 实际观感就是**套在怪身上的一圈白色长方框**，而不是"护甲"。
        // 玩家明确要求去掉，所以整段删除；品阶的可读性交给下面的足下光环与体表光晕。
        boolean modelArmor = renderer instanceof CultivationHumanoidRenderer;
        if (false && modelArmor) {
            // 故意留成永假分支：让 `armor`/`modelArmor` 仍有引用，避免局部变量告警。
        }

        // ---- 体表光晕：头目及以上，billboard，永远正对玩家 ----
        if (aura >= 3) {
            float haloW = w * (1.9F + 0.15F * (aura - 3));
            float haloH = h * (1.45F + 0.10F * (aura - 3));
            pose.pushPose();
            pose.translate(0.0F, h * 0.52F, 0.0F);
            pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher()
                    .cameraOrientation());
            int alpha = aura >= 4 ? 60 : 42;
            quad(pose.last(), vc, -haloW, -haloH, haloW, haloH,
                    0xFF000000 | rgb, alpha, 0.0F);
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------
    // 顶点工具
    // ------------------------------------------------------------------

    /**
     * 一个面向相机的四边形（局部坐标，+z 朝相机）。
     *
     * @param z 沿视线方向的深度偏移（billboard 平面内的纯深度位移，不改变屏幕位置）
     */
    private static void quad(PoseStack.Pose pose, VertexConsumer vc,
                             float x0, float y0, float x1, float y1,
                             int rgb, int alpha, float z) {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float a = alpha / 255.0F;
        vertex(pose, vc, x0, y1, z, r, g, b, a, 0.0F, 1.0F);
        vertex(pose, vc, x1, y1, z, r, g, b, a, 1.0F, 1.0F);
        vertex(pose, vc, x1, y0, z, r, g, b, a, 1.0F, 0.0F);
        vertex(pose, vc, x0, y0, z, r, g, b, a, 0.0F, 0.0F);
    }

    /** 贴地的水平光环（世界坐标，+y 向上）。 */
    private static void flatRing(PoseStack.Pose pose, VertexConsumer vc,
                                 float radius, int rgb, int alpha) {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float a = alpha / 255.0F;
        vertex(pose, vc, -radius, 0.0F, radius, r, g, b, a, 0.0F, 1.0F);
        vertex(pose, vc, radius, 0.0F, radius, r, g, b, a, 1.0F, 1.0F);
        vertex(pose, vc, radius, 0.0F, -radius, r, g, b, a, 1.0F, 0.0F);
        vertex(pose, vc, -radius, 0.0F, -radius, r, g, b, a, 0.0F, 0.0F);
    }

    /** 轴对齐长方体：六个面各两个三角形。 */
    private static void box(PoseStack.Pose pose, VertexConsumer vc,
                            float x0, float y0, float z0,
                            float x1, float y1, float z1, int rgb, int alpha) {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float a = alpha / 255.0F;
        // 上下
        face(pose, vc, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
        face(pose, vc, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, r, g, b, a);
        // 前后
        face(pose, vc, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        face(pose, vc, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
        // 左右
        face(pose, vc, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
        face(pose, vc, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, r, g, b, a);
    }

    private static void face(PoseStack.Pose pose, VertexConsumer vc,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float r, float g, float b, float a) {
        vertex(pose, vc, ax, ay, az, r, g, b, a, 0.0F, 1.0F);
        vertex(pose, vc, bx, by, bz, r, g, b, a, 1.0F, 1.0F);
        vertex(pose, vc, cx, cy, cz, r, g, b, a, 1.0F, 0.0F);
        vertex(pose, vc, dx, dy, dz, r, g, b, a, 0.0F, 0.0F);
    }

    /** 顶点顺序必须与 {@code DefaultVertexFormat.NEW_ENTITY} 一致。 */
    private static void vertex(PoseStack.Pose pose, VertexConsumer vc,
                               float x, float y, float z,
                               float r, float g, float b, float a, float u, float v) {
        vc.vertex(pose.pose(), x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    // ------------------------------------------------------------------
    // 颜色
    // ------------------------------------------------------------------

    /** 按比例压暗一个 0xRRGGBB。 */
    private static int darken(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    /** 按比例提亮一个 0xRRGGBB。 */
    private static int lighten(int rgb, float factor) {
        return mix(rgb, 0xFFFFFF, factor);
    }

    /** 两个 0xRRGGBB 之间线性插值。 */
    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
