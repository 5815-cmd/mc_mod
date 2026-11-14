package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 所有 NPC 共用的渲染器：一套骨架，靠 {@link Variant} 换体型、换随身件、换贴图。
 *
 * <p>贴图路径 {@code hongmeng:textures/entity/<NPC 注册名>.png}，全部 64×64，
 * UV 与 {@link CultivationNpcModel} 的布局一一对应（原版人形六件套 + v32..47 的自建件）。</p>
 *
 * <p>新增一个 NPC 的成本因此是：<b>一个 Entity 类 + 一个 Variant 常量 + 一张 64×64 贴图</b>，
 * 不必再写渲染器（第四批的丹药商人／炼器师／秘境向导／悬赏牌看板就是这么加的）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class CultivationNpcRenderer
        extends HumanoidMobRenderer<CultivationNpcEntity, CultivationNpcModel> {

    /** 实体注册名 → 贴图；贴图不可变，缓存起来省掉每帧的哈希。 */
    private static final Map<String, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();

    /** 门派长老：清瘦、白须垂胸，一身素色道袍。 */
    public static final Variant SECT_ELDER =
            new Variant("sect_elder", 0.98F, 1.00F, false, CultivationNpcModel.POUCH_BEARD);

    /** 炼丹师：微胖、腰间挂丹葫芦，短了一头。 */
    public static final Variant ALCHEMY_MASTER =
            new Variant("alchemy_master", 1.10F, 0.96F, false, CultivationNpcModel.POUCH_HIP);

    /** 云游商人：背着褡裢、拄着拐杖，风尘仆仆。 */
    public static final Variant WANDERING_MERCHANT =
            new Variant("wandering_merchant", 1.02F, 1.02F, true, CultivationNpcModel.POUCH_BACK);

    // ------------------------------------------------------------------
    // 第四批 NPC（丹药商人 / 炼器师 / 秘境向导 / 悬赏牌看板）
    // ------------------------------------------------------------------

    /** 丹药商人：矮胖、腰间挂丹葫芦（与炼丹师同挂点，靠贴图与身高区分）。 */
    public static final Variant PILL_MERCHANT =
            new Variant("pill_merchant", 1.06F, 0.98F, false, CultivationNpcModel.POUCH_HIP);

    /** 炼器师：壮实（1.16 是全部 NPC 里最壮的），背后斜插着新打的剑胚。 */
    public static final Variant ARTIFACT_REFINER =
            new Variant("artifact_refiner", 1.16F, 1.02F, false, CultivationNpcModel.POUCH_BACK);

    /** 秘境向导：清瘦、拄着一根探路的竹杖，袖口沾着风沙。 */
    public static final Variant REALM_GUIDE =
            new Variant("realm_guide", 1.00F, 1.02F, true, CultivationNpcModel.POUCH_BACK);

    /** 悬赏牌看板：木牌也有身子，这样能直接复用整套 NPC 骨架与交互。 */
    public static final Variant BOUNTY_BOARD =
            new Variant("bounty_board", 1.20F, 0.94F, true, CultivationNpcModel.POUCH_BACK);

    private final ResourceLocation texture;
    private final float heightScale;

    public CultivationNpcRenderer(EntityRendererProvider.Context context, Variant variant) {
        super(context, new CultivationNpcModel(
                        context.bakeLayer(CultivationNpcModel.LAYER),
                        variant.bulk(), variant.staff(), variant.pouch()),
                variant.shadowRadius());

        this.texture = TEXTURES.computeIfAbsent(variant.id(), id -> new ResourceLocation(
                HongmengMod.MOD_ID, "textures/entity/" + id + ".png"));
        this.heightScale = variant.height();
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationNpcEntity entity) {
        return this.texture;
    }

    /** NPC 的名牌一直挂着：玩家得知道这位能说话。 */
    @Override
    protected boolean shouldShowName(CultivationNpcEntity entity) {
        return true;
    }

    /** 只做纵向拉伸（炼丹师矮一点、商人高一点），不整体放大。 */
    @Override
    protected void scale(CultivationNpcEntity entity, PoseStack poseStack, float partialTick) {
        if (this.heightScale != 1.0F) {
            poseStack.scale(1.0F, this.heightScale, 1.0F);
        }
    }

    /**
     * 一个 NPC 的外观参数。
     *
     * @param id      实体注册名，同时是贴图文件名（不含 {@code .png}）
     * @param bulk    横向体型倍率
     * @param height  纵向身高倍率
     * @param staff   是否拄拐杖
     * @param pouch   随身件的挂点（决定那块几何是白须、丹葫芦还是褡裢）
     */
    public record Variant(String id, float bulk, float height, boolean staff, PartPose pouch) {

        public float shadowRadius() {
            return 0.34F + (this.bulk - 0.9F) * 0.30F;
        }
    }
}
