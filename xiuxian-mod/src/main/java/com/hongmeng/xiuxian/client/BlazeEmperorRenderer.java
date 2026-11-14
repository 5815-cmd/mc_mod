package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 焚天炎帝（熔岩泰坦）渲染器 —— 骨架见 {@link BlazeEmperorModel}。
 *
 * <p>第六轮 Boss 重制：模型换成高细节熔岩泰坦后，补上<b>品阶发光层</b>
 * {@link RankGlowLayer}（发光贴图 = {@code blaze_emperor_glow.png}，由渲染层自动按
 * {@code <贴图>_glow.png} 规则找到）。它负责：胸核熔炉 / 散热口 / 眼睛 / 火焰鬃毛 /
 * 关节裂缝 / 排气管口在暗处的自发光。</p>
 *
 * <p>发光层只在品阶 {@code glowingEyes()}（精英及以上）或实体 {@code forceGlow()} 时绘制；
 * 焚天炎帝是 {@code MobRank.BOSS}（头目，glowLevel=2），常亮。</p>
 *
 * <p><b>不改</b>：LAYER id（{@code blaze_emperor}）、贴图路径、ClientSetup 里的注册方式。</p>
 */
public class BlazeEmperorRenderer extends MobRenderer<CultivationMonsterEntity, BlazeEmperorModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(
                    new ResourceLocation(HongmengMod.MOD_ID, "blaze_emperor"), "main");

    private static final ResourceLocation TEX =
            new ResourceLocation(HongmengMod.MOD_ID, "textures/entity/blaze_emperor.png");

    public BlazeEmperorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BlazeEmperorModel(ctx.bakeLayer(LAYER)), 2.2F);
        // 第六轮：熔炉胸核/眼/鬃毛/裂缝/排气管的发光层（_glow.png 由命名规则自动定位）
        this.addLayer(new RankGlowLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationMonsterEntity entity) {
        return TEX;
    }
}
