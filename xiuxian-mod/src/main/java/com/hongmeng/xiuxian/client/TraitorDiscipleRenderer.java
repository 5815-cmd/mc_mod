package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.GuiltySectDiscipleEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 叛徒执事（{@code guilty_sect_disciple}）的渲染器。
 *
 * <p><b>原来的实现是原版僵尸模型占位</b>：{@code ModelLayers.ZOMBIE} + 三张根本不存在的
 * {@code sect_disciple_*.png}，进游戏就是一块 missing texture 的紫黑方块。
 * 现在换成自建骨架 {@link TraitorDiscipleModel}（原版人形 UV + 鬼面罩/教徽/锁链腕环），
 * 并真正画出三派贴图（64×64，同骨架同 UV，只有配色不同）。</p>
 *
 * <h3>三派贴图</h3>
 * <ul>
 *   <li><b>千机</b> · 青钢：青绿道袍 + 青钢鬼面</li>
 *   <li><b>神霄</b> · 赤铁：赤红道袍 + 赤铁鬼面</li>
 *   <li><b>魔渊</b> · 紫黑：紫黑道袍 + 黑曜紫铁鬼面</li>
 * </ul>
 *
 * <p>贴图路径沿用旧代码里已经写好的三条（{@code sect_disciple_<门派>.png}），
 * {@code textureFor()} 的语义（门派名 → 贴图，查不到走兜底）保持不变；
 * 只把兜底从「原版僵尸」换成「魔渊」——叛门者多堕入魔渊，兜底也不会再是僵尸脸。</p>
 */
@OnlyIn(Dist.CLIENT)
public class TraitorDiscipleRenderer
        extends HumanoidMobRenderer<GuiltySectDiscipleEntity, TraitorDiscipleModel> {

    /** 模型层：LayerDefinition 由 {@link TraitorDiscipleModel#createBodyLayer()} 提供（需在 ClientSetup 注册）。 */
    public static final ModelLayerLocation LAYER = TraitorDiscipleModel.LAYER;

    /** 未知门派时的兜底贴图（魔渊）。 */
    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/sect_disciple_moyuan.png");

    /** 三个门派各一份贴图（骨架/UV 完全相同，只有配色不同）。 */
    private static final Map<String, ResourceLocation> SECT_TEXTURES = new ConcurrentHashMap<>();

    static {
        SECT_TEXTURES.put("千机", new ResourceLocation(HongmengMod.MOD_ID,
                "textures/entity/sect_disciple_qianji.png"));
        SECT_TEXTURES.put("神霄", new ResourceLocation(HongmengMod.MOD_ID,
                "textures/entity/sect_disciple_shenxiao.png"));
        SECT_TEXTURES.put("魔渊", new ResourceLocation(HongmengMod.MOD_ID,
                "textures/entity/sect_disciple_moyuan.png"));
    }

    public TraitorDiscipleRenderer(EntityRendererProvider.Context context) {
        super(context, new TraitorDiscipleModel(context.bakeLayer(LAYER)), 0.5F);
        // 执事手里常年握着铁剑（实体 tick 里会补回来），把手中物品层挂上，剑才看得见
        this.addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(
                this, context.getItemInHandRenderer()));
        // 执事本身不穿护甲（实体只拿一把铁剑），但玩家可以用指令给它套甲——
        // 保留人形护甲层，套上去会正常画在这套骨架上（不是僵尸骨架）。
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(GuiltySectDiscipleEntity entity) {
        return textureFor(entity.sectName());
    }

    /** 门派名 → 贴图；空/未知门派走 {@link #DEFAULT_TEXTURE}（语义与旧实现一致）。 */
    private static ResourceLocation textureFor(String sectName) {
        if (sectName != null) {
            ResourceLocation mapped = SECT_TEXTURES.get(sectName);
            if (mapped != null) {
                return mapped;
            }
        }
        return DEFAULT_TEXTURE;
    }
}
