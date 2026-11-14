package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 人形野怪的渲染器：血魔修士／傀儡队长／魔道统领／虚空道主，
 * 以及第四批的魔道执事／血魔长老／虚空行者／剑冢守灵。
 *
 * <p><b>不再借用 {@code ModelLayers.ZOMBIE}。</b>模型是本模组自建的
 * {@link CultivationHumanoidModel}（64×64 骨架，六个人形部件 + 12 个自建部件
 * + 9 个变体的 37 个身份件），所有走这条线的人形怪共用这一套骨架，
 * 靠 {@link Variant} 开关部件与调体型来区分；身份件按 {@code Variant.id()} 传给模型显隐。</p>
 *
 * <p><b>贴图不再由渲染器写死</b>：每个 {@code Variant} 的 {@code id} 就是实体注册名，
 * 渲染时向实体要 {@code textureLocation()}（见
 * {@code CultivationMonsterEntity.textureName()}）。这样一套渲染器可以服务
 * <b>任意多</b>只人形怪——加一只怪只要加一个 {@code Variant} 常量 + 一张贴图，
 * 不必再写渲染器，也不必动 {@code ClientSetup} 里的贴图路径。</p>
 *
 * <p>贴图路径仍是 {@code hongmeng:textures/entity/<实体注册名>.png}，
 * 全部 64×64，UV 与 {@link CultivationHumanoidModel} 的布局一一对应：
 * 人形六个盒子走原版人形 UV，自建部件全部落在原版人形贴图 v≥32 的空白区。</p>
 */
@OnlyIn(Dist.CLIENT)
public class CultivationHumanoidRenderer
        extends HumanoidMobRenderer<CultivationMonsterEntity, CultivationHumanoidModel> {

    // ------------------------------------------------------------------
    // 人形怪的外观配置
    // ------------------------------------------------------------------

    /** 血魔修士（精英）：瘦一圈的血红道袍，只有发髻，没有护肩／披风／剑。 */
    public static final Variant BLOOD_DEMON_CULTIVATOR =
            new Variant("blood_demon_cultivator", MobRank.ELITE,
                    0.90F, 0.98F, false, false, false, false);

    /** 劫灰修士（精英）：灰白枯瘦，无护肩无披风，背后一件残破灰袍。 */
    public static final Variant TRIBULATION_ASH_CULTIVATOR =
            new Variant("tribulation_ash_cultivator", MobRank.ELITE,
                    0.88F, 1.02F, false, true, false, false);

    /** 傀儡队长（队长）：壮实，青铜护肩，背后背剑，肢体带金属分节环。 */
    public static final Variant PUPPET_CAPTAIN =
            new Variant("puppet_captain", MobRank.CAPTAIN,
                    1.14F, 1.06F, true, false, true, false);

    /** 魔道统领（头目）：高瘦，深紫披风 + 长剑，伸手扑击的架势。 */
    public static final Variant DEMON_GENERAL =
            new Variant("demon_general", MobRank.BOSS,
                    0.98F, 1.04F, false, true, true, true);

    /** 虚空道主（首领）：最高最壮，部件全开（护肩 + 披风 + 剑），金线滚边。 */
    public static final Variant VOID_OVERLORD =
            new Variant("void_overlord", MobRank.OVERLORD,
                    1.18F, 1.12F, true, true, true, true);

    // ---- 第四批：新增的四只人形怪 ----

    /**
     * 魔道执事（精英）：人形系里最瘦最小的一只，只有发髻、没有甲。
     *
     * <p>刻意与血魔修士（同为精英）拉开体型差：0.94 对 0.90 只是横向上的一点点，
     * 真正的辨识度在贴图上——执事是<b>灰紫官袍 + 腰牌</b>，血魔是<b>血红道袍</b>。</p>
     */
    public static final Variant DEMON_STEWARD =
            new Variant("demon_steward", MobRank.ELITE,
                    0.94F, 1.00F, false, false, false, false);

    /** 血魔长老（队长）：比血魔修士壮一圈，多一件披风——队长档位自动点亮护肩。 */
    public static final Variant BLOOD_DEMON_ELDER =
            new Variant("blood_demon_elder", MobRank.CAPTAIN,
                    1.08F, 1.04F, false, true, false, false);

    /** 虚空行者（头目）：部件全开（护肩 + 披风 + 剑）+ 伸手扑击的架势。 */
    public static final Variant VOID_WALKER =
            new Variant("void_walker", MobRank.BOSS,
                    1.02F, 1.06F, true, true, true, true);

    /** 剑冢守灵（队长）：瘦高、背后一柄剑，站姿笔直（不摆扑击架势）。 */
    public static final Variant SWORD_TOMB_GUARDIAN =
            new Variant("sword_tomb_guardian", MobRank.CAPTAIN,
                    0.96F, 1.08F, true, false, true, false);

    /** 纵向身高倍率，只在 {@link #scale} 里用一次。 */
    private final float heightScale;

    public CultivationHumanoidRenderer(EntityRendererProvider.Context context, Variant variant) {
        super(context, new CultivationHumanoidModel(
                        context.bakeLayer(CultivationHumanoidModel.LAYER),
                        variant.bulk(), variant.effectivePauldrons(), variant.effectiveCape(),
                        variant.blade(), variant.id())
                        .setReaching(variant.reaching()),
                // 影子跟着体型一起长，否则高阶怪脚下一小圈影子很出戏
                variant.shadowRadius() * variant.rank().scaleXZ());

        this.heightScale = variant.height();

        // 品阶发光层：精英及以上眼睛会亮（见 RankGlowLayer）
        this.addLayer(new RankGlowLayer<>(this));
    }

    /**
     * 贴图向<b>实体</b>要，而不是渲染器自己绑一张。
     *
     * <p>这一句是"新增人形怪不必写渲染器"的关键：同一套骨架服务任意多只怪，
     * 贴图名就是实体注册名（{@code CultivationMonsterEntity.textureName()}）。</p>
     */
    @Override
    public ResourceLocation getTextureLocation(CultivationMonsterEntity entity) {
        return entity.textureLocation();
    }

    /** 队长及以上的怪，隔着老远也要看得见名牌。 */
    @Override
    protected boolean shouldShowName(CultivationMonsterEntity entity) {
        return super.shouldShowName(entity) || entity.rank().isRare();
    }

    /**
     * 体型 = <b>变体倍率 × 品阶倍率</b>。
     *
     * <p>两个来源刻意分开：{@code Variant} 描述"这只怪长什么样"（瘦高／壮实），
     * {@link MobRank} 描述"这一档该有多大"。所以同一只怪如果以后被改成别的品阶，
     * 体型会自动跟着走，不需要再手改数字。</p>
     */
    @Override
    protected void scale(CultivationMonsterEntity entity, PoseStack poseStack, float partialTick) {
        MobRank rank = entity.rank();
        float horizontal = rank.scaleXZ();
        float vertical = this.heightScale * rank.scaleY();
        if (horizontal != 1.0F || vertical != 1.0F) {
            poseStack.scale(horizontal, vertical, horizontal);
        }
    }

    /**
     * 一只人形怪的完整外观参数。
     *
     * @param id        实体注册名，同时是贴图文件名（不含 {@code .png}）
     * @param rank      品阶——决定体型下限与"该有的部件"
     * @param bulk      横向体型倍率（压躯干与四肢的 x/z，头不动）
     * @param height    纵向身高倍率
     * @param pauldrons 显示护肩 + 尖钉
     * @param cape      显示披风
     * @param blade     显示背后剑
     * @param reaching  摆"伸手扑击"的架势
     */
    public record Variant(String id, MobRank rank, float bulk, float height, boolean pauldrons,
                          boolean cape, boolean blade, boolean reaching) {

        /** 人形怪的影子半径：体型越壮影子越大。 */
        public float shadowRadius() {
            return 0.42F + (this.bulk - 0.9F) * 0.30F;
        }

        /**
         * 实际要不要画护肩。
         *
         * <p>队长及以上<b>一律要有护甲件</b>——这是玩家要的"队长比普通怪多一件甲"。
         * 变体自己的配置只可能"多要"，不可能"少给"。</p>
         */
        public boolean effectivePauldrons() {
            return this.pauldrons || this.rank.armorLevel() >= 1;
        }

        /** 实际要不要画披风：头目及以上（玩家要的"头目加披风"）。 */
        public boolean effectiveCape() {
            return this.cape || this.rank.armorLevel() >= 2;
        }
    }
}
