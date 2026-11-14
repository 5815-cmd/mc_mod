package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 品阶发光层：精英及以上的怪，眼睛会亮。
 *
 * <p><b>这是"精英与普通看得出来不一样"的第一眼线索。</b>机制照抄原版蜘蛛／末影人的
 * 眼睛层（{@code EyesLayer}）：把<b>同一个模型</b>再用
 * {@link RenderType#eyes(ResourceLocation)} 画一遍——那个渲染类型是
 * 加法混合 + 只写颜色不写深度 + 不吃光照，于是只有发光贴图里不透明的地方
 * 会"叠一层光"在模型上，其余像素因为 alpha=0 被丢弃。</p>
 *
 * <p>发光贴图由 {@code tools/gen_rank_beast_textures.py} 生成：
 * 只保留眼睛那几个像素（每个怪的坐标都是从原始贴图里反查出来并逐个校验过的），
 * 并染成品阶色。等级越高，"体表微光"那一层也会出现：</p>
 *
 * <pre>
 *   发光等级 1（精英）  只有眼睛
 *   发光等级 2（头目）  眼睛 + 很淡的体表轮廓
 *   发光等级 3（首领）  眼睛 + 更亮的体表轮廓（整只妖在暗处也发红光）
 * </pre>
 *
 * <p>为什么这一层必须是 {@code RenderLayer} 而不是像血条那样走
 * {@code RenderNameTagEvent}：画模型需要<b>模型空间</b>的 PoseStack
 * （原版在里面先做了一次 {@code scale(-1,-1,1)} 翻转与朝向旋转，事件触发时那次
 * 变换已经出栈了）。RenderLayer 拿到的正是模型空间，所以只要把父模型重画一遍，
 * 位置天生就对——本类里<b>没有任何坐标计算</b>。</p>
 */
@OnlyIn(Dist.CLIENT)
public class RankGlowLayer<T extends CultivationMonsterEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {

    public RankGlowLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    /**
     * 发光渲染类型的缓存。
     *
     * <p><b>必须缓存，不能每帧新建。</b>原版 {@code EyesLayer}（蜘蛛／末影人的眼睛层）
     * 就是把 {@code RenderType.eyes(texture)} 存在 {@code static final} 里只建一次。
     * 每帧新建的后果不是"慢一点"，而是：{@code BufferSource.getBuffer()} 对不在
     * {@code fixedBuffers} 里的 RenderType 会**新分配一个 ByteBufferBuilder**，
     * 于是每帧成百上千个缓冲区被创建又丢弃，渲染资源被 churn 坏，
     * 表现是 {@code LevelRenderer.renderChunkLayer} 读到 null 着色器而崩客户端
     * （玩家 15:19 / 15:25 两次崩溃就是这个签名）。</p>
     */
    private static final java.util.Map<ResourceLocation, RenderType> GLOW_TYPES =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * PERF(C): 发光贴图 ResourceLocation 的缓存。
     *
     * <p>{@link #glowTexture} 每帧每只发光怪都要调一次，原来每次都做字符串拼接
     * + {@code new ResourceLocation}（还会触发路径校验）。贴图是固定的，
     * 按原贴图缓存一次即可。</p>
     */
    private static final java.util.Map<ResourceLocation, ResourceLocation> GLOW_TEX =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 发光贴图命名规则：{@code <原贴图>_glow.png}（同目录）。 */
    public static ResourceLocation glowTexture(ResourceLocation base) {
        return GLOW_TEX.computeIfAbsent(base, b -> {
            String path = b.getPath();
            if (path.endsWith(".png")) {
                path = path.substring(0, path.length() - 4);
            }
            return new ResourceLocation(b.getNamespace(), path + "_glow.png");
        });
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        MobRank rank = entity.rank();
        // 普通品阶默认不发光；entity.forceGlow() 给"普通但依赖发光"的怪开绿灯
        // （泽魅青磷灯 / 晶蛛灵晶 / 虚空水母虚空眼 —— 它们的 _glow.png 已按契约生成）
        if ((!rank.glowingEyes() && !entity.forceGlow()) || entity.isInvisible()) {
            return;
        }
        ResourceLocation glow = glowTexture(this.getTextureLocation(entity));
        // 缓存查找（computeIfAbsent 只会在第一次为该贴图建一个 RenderType）
        RenderType type = GLOW_TYPES.computeIfAbsent(glow, RenderType::eyes);
        VertexConsumer consumer = buffer.getBuffer(type);
        // 高阶怪的眼光会"呼吸"一下，静止时也看得出是活的
        float pulse = rank.glowLevel() >= 2
                ? 0.88F + 0.12F * Mth.cos(ageInTicks * 0.16F)
                : 1.0F;
        this.getParentModel().renderToBuffer(poseStack, consumer,
                0xF000F0, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1.0F);
    }
}
