package com.hongmeng.xiuxian.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 旧渲染器名（兼容壳）。
 *
 * <p>叛徒执事的渲染器已经换成 {@link TraitorDiscipleRenderer}（自建人形骨架
 * {@link TraitorDiscipleModel} + 三派真贴图）；本类只是为了让
 * {@code ClientSetup} 里那行旧注册
 * （{@code ...GUILTY_SECT_DISCIPLE.get(), GuiltySectDiscipleRenderer::new}）
 * <b>不改也能正常跑</b>——它现在就是新渲染器的一个子类。</p>
 *
 * <p>新代码请直接用 {@link TraitorDiscipleRenderer}；本类不再承载任何逻辑。</p>
 */
@OnlyIn(Dist.CLIENT)
public class GuiltySectDiscipleRenderer extends TraitorDiscipleRenderer {

    public GuiltySectDiscipleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
