package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 修仙界人形怪的自建模型。
 *
 * <p><b>不使用 {@code ModelLayers.ZOMBIE}，骨架也不再是僵尸那一套。</b>
 * 原版僵尸是"伸着两只手、连袍子都没有"的贴图盒，四个出身完全不同的怪
 * 套上去只能靠换颜色区分，玩家看到的永远是四只染了色的僵尸。这里从头搭一套
 * 有修仙味的骨架：发髻、护肩、披风、背后剑，再加上画在贴图上的道袍下摆。</p>
 *
 * <p>继承 {@link HumanoidModel} 只是为了白拿它已经写好的通用动画
 * （下蹲、举物、攻击挥手、左右手姿势）；<b>骨架本身是本文件从零搭的</b>：
 * 六个人形部件之外还有 5 个自建部件，全部挂在头／躯干／双臂下面，
 * 跟着父部件一起动（护肩跟着手臂摆、披风跟着躯干走）。</p>
 *
 * <h2>UV 布局</h2>
 *
 * <p>贴图固定 {@code 64×64}。<b>头／身／双臂／双腿完全沿用原版人形 UV 坐标</b>
 * （{@code HumanoidModel.createMesh} 那一套），所以任何标准 64×64 人形皮肤
 * 贴上来都不会花脸：</p>
 *
 * <pre>
 *   头部   texOffs( 0,  0)              8×8×8
 *   身体   texOffs(16, 16)              8×12×4
 *   右臂   texOffs(40, 16)              4×12×4
 *   左臂   texOffs(40, 16).mirror()     4×12×4
 *   右腿   texOffs( 0, 16)              4×12×4
 *   左腿   texOffs( 0, 16).mirror()     4×12×4
 * </pre>
 *
 * <p>左右臂、左右腿都是<b>同一个 texOffs + mirror()</b>，这正是原版
 * {@code HumanoidModel.createMesh} 的做法（用 javap 反编译确认过：左臂左腿的
 * {@code texOffs} 与右侧相同，只是多调了一次 {@code mirror()}）——镜像件复用
 * 右半边的像素，所以标准皮肤的"左臂／左腿"那两块留空也不影响。</p>
 *
 * <p><b>UV 校验依据：</b>从 {@code client-extra.jar} 解出的
 * {@code minecraft:textures/entity/zombie/zombie.png} 与
 * {@code minecraft:textures/entity/player/wide/steve.png} 都是
 * <b>u 的 0..55、v 的 0..31 有像素，v≥32 的整个下半区是空白</b>
 * （左边 u0..31 的下半区被左臂／左腿的镜像件和躯干下缘占掉，
 * 真正能用的只有 u32..63 / v32..63 这 32×32 一块）。
 * 自建部件全部塞在这一块里：</p>
 *
 * <pre>
 *   自建部件        texOffs    尺寸        占据 UV（tools/preview_entity_model.py 会逐项交叉校验）
 *   发髻             32, 32    4×5×4       u32..47  v32..45
 *   披风             40, 48    6×6×1       u40..49  v48..54
 *   剑身             48, 32    2×6×1       u48..55  v32..39
 *   剑柄             48, 40    3×2×2       u48..55  v42..47
 *   右护肩           32, 56    4×2×3       u32..45  v56..63
 *   左护肩           32, 56*   4×2×3       u32..45  v56..63   （*镜像盒，与右护肩共用同一块贴图）
 * </pre>
 *
 * <p>自建件之间<b>零相交</b>（由 {@code tools/gen_entity_textures.py} 的
 * {@code check_layouts()} 强制）。唯一一处与原版骨架共用像素的是剑身的背面
 * （u52..53／v33..38）落在左臂底面那条横带里——那个面朝里贴着躯干，看不见。</p>
 *
 * <p><b>道袍不打额外盒子：</b>躯干贴图 v36 以下那几行（对应世界坐标
 * y=8..12 的腰胯）直接画成袍摆，两条腿就"收"进袍子里了。在只剩 32×32 空地
 * 的前提下，"少加盒子、多在贴图上做文章"比硬塞十几个小盒子更划算——
 * 这件道袍的效果是画出来的，不是堆出来的。</p>
 *
 * <h2>9 个变体的身份件（round2）</h2>
 *
 * <p>为了让 9 只共用骨架的人形怪不再"只差颜色"，本类追加了 <b>37 个身份件</b>
 * （22 个 UV slot，同样全部排在 v≥32 自由区），每个变体只显示自己那组：</p>
 *
 * <ul>
 *   <li>傀儡队长：背后提线横梁 + 2 条垂线；魔道统领：双角（2 段）+ 肩骨刺；</li>
 *   <li>虚空霸主：背后 4 片虚空晶翼 + 独眼面具；虚空行者：兜帽 + 反手短刃；</li>
 *   <li>血魔长老：血袍长摆 + 血珠项链；血魔修士：半脸血纹面罩 + 腕部血刃；</li>
 *   <li>劫灰修士：灰烬破布 3 片 + 披肩；魔道执事：账册 + 笔；</li>
 *   <li>剑冢守灵：背后剑匣 + 3 剑柄 + 剑冢纹面罩。</li>
 * </ul>
 *
 * <p>显隐由构造函数最后一个参数 {@code String identity}（渲染器传 {@code Variant.id()}）
 * 交给 {@link #applyIdentity(String)} 的 switch 开关；身份件全部挂在 head / body / 双臂上，
 * 跟着原版人形动画走（尺寸相同的重复件共用同一块 UV）。UV 由
 * {@code 工程/角色设计/round2/identity-humanoid/uv_plan.json} 程序化装箱得到
 * （0 越界 / 0 跨足迹重叠），贴图只在新增矩形上作画、不改人形原像素。</p>
 *
 * @see CultivationHumanoidRenderer
 */
@OnlyIn(Dist.CLIENT)
public class CultivationHumanoidModel extends HumanoidModel<CultivationMonsterEntity> {

    /** 模型层标识：四个怪共用一套骨架，靠构造参数开关部件。 */
    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER =
            new net.minecraft.client.model.geom.ModelLayerLocation(
                    new net.minecraft.resources.ResourceLocation(
                            com.hongmeng.xiuxian.HongmengMod.MOD_ID, "cultivation_humanoid"), "main");

    // ------------------------------------------------------------------
    // 自建部件
    // ------------------------------------------------------------------

    /** 发髻：头顶那坨束起来的头发。 */
    private final ModelPart hairBun;

    /** 护肩（左右各一，镜像件，同一块贴图）。 */
    private final ModelPart rightPauldron;
    private final ModelPart leftPauldron;

    /** 披风。 */
    private final ModelPart cape;

    /** 背后剑：剑身 + 剑柄。 */
    private final ModelPart blade;
    private final ModelPart bladeHilt;

    // ------------------------------------------------------------------
    // 身份件（9 个变体各一组，渲染器按 Variant.id() 开关）
    // ------------------------------------------------------------------

    private final ModelPart puppetBar;
    private final ModelPart puppetStringL;
    private final ModelPart puppetStringR;
    private final ModelPart dgHornBaseL;
    private final ModelPart dgHornBaseR;
    private final ModelPart dgHornTipL;
    private final ModelPart dgHornTipR;
    private final ModelPart dgSpikeL;
    private final ModelPart dgSpikeR;
    private final ModelPart voWingInnerL;
    private final ModelPart voWingInnerR;
    private final ModelPart voWingOuterL;
    private final ModelPart voWingOuterR;
    private final ModelPart voMask;
    private final ModelPart vwHoodTop;
    private final ModelPart vwHoodBack;
    private final ModelPart vwBlade;
    private final ModelPart vwGuard;
    private final ModelPart beRobeL;
    private final ModelPart beRobeR;
    private final ModelPart beBead1;
    private final ModelPart beBead2;
    private final ModelPart beBead3;
    private final ModelPart bcMask;
    private final ModelPart bcWristL;
    private final ModelPart bcWristR;
    private final ModelPart ashTatter1;
    private final ModelPart ashTatter2;
    private final ModelPart ashTatter3;
    private final ModelPart ashShawl;
    private final ModelPart dsBook;
    private final ModelPart dsPen;
    private final ModelPart stCasket;
    private final ModelPart stHilt1;
    private final ModelPart stHilt2;
    private final ModelPart stHilt3;
    private final ModelPart stMask;

    // ------------------------------------------------------------------
    // 配置
    // ------------------------------------------------------------------

    private final float bulk;

    /** body 的基准横向缩放（= bulk）；每帧呼吸要用它做基准，
     *  不能直接读 xScale（那会把上一帧的呼吸值当基准累乘）。 */
    private float bodyBaseScale = 1.0F;
    private final boolean pauldrons;
    private final boolean capeVisible;
    private final boolean bladeVisible;

    /** 是否摆出"伸手扑击"的架势。 */
    private boolean reaching;

    /**
     * @param root      {@link #createBodyLayer()} 烘出来的根部件
     * @param bulk      横向体型倍率：1.0 标准，0.90 偏瘦，1.18 壮硕
     * @param pauldrons 是否显示护肩
     * @param cape      是否显示披风
     * @param blade     是否显示背后剑
     * @param identity  变体 id（{@code Variant.id()}）：决定打开哪一组身份件
     */
    public CultivationHumanoidModel(ModelPart root, float bulk,
                                    boolean pauldrons, boolean cape, boolean blade,
                                    String identity) {
        super(root);
        this.bulk = bulk;
        this.pauldrons = pauldrons;
        this.capeVisible = cape;
        this.bladeVisible = blade;

        this.hairBun = child(root, "head", "hair_bun");

        this.rightPauldron = child(root, "right_arm", "right_pauldron");
        this.leftPauldron = child(root, "left_arm", "left_pauldron");

        this.cape = child(root, "body", "cape");
        this.blade = child(root, "body", "blade");
        this.bladeHilt = child(root, "body", "blade_hilt");
        // 身份件（37 个；只挂不开关，显隐交给 applyIdentity）
        this.puppetBar = child(root, "body", "puppet_bar");
        this.puppetStringL = child(root, "body", "puppet_string_l");
        this.puppetStringR = child(root, "body", "puppet_string_r");
        this.dgHornBaseL = child(root, "head", "dg_horn_base_l");
        this.dgHornBaseR = child(root, "head", "dg_horn_base_r");
        this.dgHornTipL = child(root, "head", "dg_horn_tip_l");
        this.dgHornTipR = child(root, "head", "dg_horn_tip_r");
        this.dgSpikeL = child(root, "body", "dg_spike_l");
        this.dgSpikeR = child(root, "body", "dg_spike_r");
        this.voWingInnerL = child(root, "body", "vo_wing_inner_l");
        this.voWingInnerR = child(root, "body", "vo_wing_inner_r");
        this.voWingOuterL = child(root, "body", "vo_wing_outer_l");
        this.voWingOuterR = child(root, "body", "vo_wing_outer_r");
        this.voMask = child(root, "head", "vo_mask");
        this.vwHoodTop = child(root, "head", "vw_hood_top");
        this.vwHoodBack = child(root, "head", "vw_hood_back");
        this.vwBlade = child(root, "right_arm", "vw_blade");
        this.vwGuard = child(root, "right_arm", "vw_guard");
        this.beRobeL = child(root, "body", "be_robe_l");
        this.beRobeR = child(root, "body", "be_robe_r");
        this.beBead1 = child(root, "body", "be_bead_1");
        this.beBead2 = child(root, "body", "be_bead_2");
        this.beBead3 = child(root, "body", "be_bead_3");
        this.bcMask = child(root, "head", "bc_mask");
        this.bcWristL = child(root, "left_arm", "bc_wrist_l");
        this.bcWristR = child(root, "right_arm", "bc_wrist_r");
        this.ashTatter1 = child(root, "body", "ash_tatter_1");
        this.ashTatter2 = child(root, "body", "ash_tatter_2");
        this.ashTatter3 = child(root, "body", "ash_tatter_3");
        this.ashShawl = child(root, "body", "ash_shawl");
        this.dsBook = child(root, "left_arm", "ds_book");
        this.dsPen = child(root, "right_arm", "ds_pen");
        this.stCasket = child(root, "body", "st_casket");
        this.stHilt1 = child(root, "body", "st_hilt_1");
        this.stHilt2 = child(root, "body", "st_hilt_2");
        this.stHilt3 = child(root, "body", "st_hilt_3");
        this.stMask = child(root, "head", "st_mask");

        applyVariant();
        applyIdentity(identity);
    }

    /** 取子部件；部件树是本文件自己搭的，缺了就是写错了，直接抛。 */
    private static ModelPart child(ModelPart root, String parent, String name) {
        ModelPart p = root.getChild(parent);
        if (!p.hasChild(name)) {
            throw new IllegalStateException("CultivationHumanoidModel 缺少部件: "
                    + parent + "/" + name);
        }
        return p.getChild(name);
    }

    /** 按构造参数开关部件、压体型。 */
    private void applyVariant() {
        this.rightPauldron.visible = this.pauldrons;
        this.leftPauldron.visible = this.pauldrons;

        this.cape.visible = this.capeVisible;
        this.blade.visible = this.bladeVisible;
        this.bladeHilt.visible = this.bladeVisible;

        // 体型只压横向：头不动（否则会变成大头娃娃），腿只在 x 上压。
        this.bodyBaseScale = this.bulk;
        this.body.xScale = this.bulk;
        this.body.zScale = this.bulk;
        this.rightArm.xScale = this.bulk;
        this.rightArm.zScale = this.bulk;
        this.leftArm.xScale = this.bulk;
        this.leftArm.zScale = this.bulk;
        this.rightLeg.xScale = this.bulk;
        this.leftLeg.xScale = this.bulk;

        // 站在那不动时双臂自然下垂，攻击时才抬起来
        this.rightArmPose = ArmPose.EMPTY;
        this.leftArmPose = ArmPose.EMPTY;
    }


    /**
     * 按变体 id 开关「身份件」。渲染器把 {@link CultivationHumanoidRenderer.Variant#id()}
     * 传进构造函数，由这里逐组点亮；默认全关（未知 id 不会显示任何身份件）。
     *
     * <p>身份件的 UV 全部排在 64×64 贴图 v≥32 的自由区（见 parts_table.py / uv_plan.json），
     * 每个变体只显示自己那组，所以 9 只怪不再"只差颜色"。</p>
     */
    private void applyIdentity(String id) {
        this.puppetBar.visible = false;
        this.puppetStringL.visible = false;
        this.puppetStringR.visible = false;
        this.dgHornBaseL.visible = false;
        this.dgHornBaseR.visible = false;
        this.dgHornTipL.visible = false;
        this.dgHornTipR.visible = false;
        this.dgSpikeL.visible = false;
        this.dgSpikeR.visible = false;
        this.voWingInnerL.visible = false;
        this.voWingInnerR.visible = false;
        this.voWingOuterL.visible = false;
        this.voWingOuterR.visible = false;
        this.voMask.visible = false;
        this.vwHoodTop.visible = false;
        this.vwHoodBack.visible = false;
        this.vwBlade.visible = false;
        this.vwGuard.visible = false;
        this.beRobeL.visible = false;
        this.beRobeR.visible = false;
        this.beBead1.visible = false;
        this.beBead2.visible = false;
        this.beBead3.visible = false;
        this.bcMask.visible = false;
        this.bcWristL.visible = false;
        this.bcWristR.visible = false;
        this.ashTatter1.visible = false;
        this.ashTatter2.visible = false;
        this.ashTatter3.visible = false;
        this.ashShawl.visible = false;
        this.dsBook.visible = false;
        this.dsPen.visible = false;
        this.stCasket.visible = false;
        this.stHilt1.visible = false;
        this.stHilt2.visible = false;
        this.stHilt3.visible = false;
        this.stMask.visible = false;
        if (id == null) {
            return;
        }
        switch (id) {
            case "puppet_captain":
                this.puppetBar.visible = true;
                this.puppetStringL.visible = true;
                this.puppetStringR.visible = true;
                break;
            case "demon_general":
                this.dgHornBaseL.visible = true;
                this.dgHornBaseR.visible = true;
                this.dgHornTipL.visible = true;
                this.dgHornTipR.visible = true;
                this.dgSpikeL.visible = true;
                this.dgSpikeR.visible = true;
                break;
            case "void_overlord":
                this.voWingInnerL.visible = true;
                this.voWingInnerR.visible = true;
                this.voWingOuterL.visible = true;
                this.voWingOuterR.visible = true;
                this.voMask.visible = true;
                break;
            case "void_walker":
                this.vwHoodTop.visible = true;
                this.vwHoodBack.visible = true;
                this.vwBlade.visible = true;
                this.vwGuard.visible = true;
                break;
            case "blood_demon_elder":
                this.beRobeL.visible = true;
                this.beRobeR.visible = true;
                this.beBead1.visible = true;
                this.beBead2.visible = true;
                this.beBead3.visible = true;
                break;
            case "blood_demon_cultivator":
                this.bcMask.visible = true;
                this.bcWristL.visible = true;
                this.bcWristR.visible = true;
                break;
            case "tribulation_ash_cultivator":
                this.ashTatter1.visible = true;
                this.ashTatter2.visible = true;
                this.ashTatter3.visible = true;
                this.ashShawl.visible = true;
                break;
            case "demon_steward":
                this.dsBook.visible = true;
                this.dsPen.visible = true;
                break;
            case "sword_tomb_guardian":
                this.stCasket.visible = true;
                this.stHilt1.visible = true;
                this.stHilt2.visible = true;
                this.stHilt3.visible = true;
                this.stMask.visible = true;
                break;
            default:
                break;
        }
    }


    /**
     * 9 个变体的身份件（共 37 个盒子）。<b>每个变体只显示自己那组</b>（见 applyIdentity），
     * 悬挂点全部落在 head / body / 双臂上，跟着原版人形动画一起动。
     *
     * <p>UV 用 64×64 贴图 v≥32 的自由区（原版人形 UV 与现有 5 个自建件全部避开），
     * 尺寸相同的左右/重复件共用同一块 UV + mirror()（同 texOffs + 同尺寸 = 同 footprint）。</p>
     */
    private static void addIdentityParts(PartDefinition head, PartDefinition body,
                                         PartDefinition rightArm, PartDefinition leftArm) {

        // ===== 傀儡队长：身份件 =====
        body.addOrReplaceChild("puppet_bar", CubeListBuilder.create()
                .texOffs(6, 45).addBox(-7.0F, -0.5F, -0.5F, 14.0F, 1.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -2.5F, 3.0F));
        body.addOrReplaceChild("puppet_string_l", CubeListBuilder.create()
                .texOffs(54, 44).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(5.5F, -2.0F, 3.0F));
        body.addOrReplaceChild("puppet_string_r", CubeListBuilder.create()
                .texOffs(54, 44).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(-5.5F, -2.0F, 3.0F));

        // ===== 魔道统领：身份件 =====
        head.addOrReplaceChild("dg_horn_base_l", CubeListBuilder.create()
                .texOffs(28, 32).addBox(-0.5F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offset(3.5F, -6.0F, 0.0F));
        head.addOrReplaceChild("dg_horn_base_r", CubeListBuilder.create()
                .texOffs(28, 32).mirror().addBox(-1.5F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offset(-3.5F, -6.0F, 0.0F));
        head.addOrReplaceChild("dg_horn_tip_l", CubeListBuilder.create()
                .texOffs(8, 32).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(5.0F, -6.5F, 1.6F));
        head.addOrReplaceChild("dg_horn_tip_r", CubeListBuilder.create()
                .texOffs(8, 32).mirror().addBox(-0.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(-5.0F, -6.5F, 1.6F));
        body.addOrReplaceChild("dg_spike_l", CubeListBuilder.create()
                .texOffs(14, 52).addBox(-0.5F, -3.5F, -0.5F, 1.0F, 4.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(5.5F, -1.5F, 0.0F));
        body.addOrReplaceChild("dg_spike_r", CubeListBuilder.create()
                .texOffs(14, 52).mirror().addBox(-0.5F, -3.5F, -0.5F, 1.0F, 4.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(-5.5F, -1.5F, 0.0F));

        // ===== 虚空霸主：身份件 =====
        body.addOrReplaceChild("vo_wing_inner_l", CubeListBuilder.create()
                .texOffs(6, 47).addBox(0.0F, -0.5F, 0.0F, 2.0F, 1.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.5F, 3.0F, 2.2F, 0.0F, 0.6F, 0.0F));
        body.addOrReplaceChild("vo_wing_inner_r", CubeListBuilder.create()
                .texOffs(6, 47).mirror().addBox(-2.0F, -0.5F, 0.0F, 2.0F, 1.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.5F, 3.0F, 2.2F, 0.0F, -0.6F, 0.0F));
        body.addOrReplaceChild("vo_wing_outer_l", CubeListBuilder.create()
                .texOffs(0, 36).addBox(0.0F, -0.5F, 0.0F, 3.0F, 1.0F, 5.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.5F, 1.5F, 2.4F, 0.0F, 0.8F, 0.0F));
        body.addOrReplaceChild("vo_wing_outer_r", CubeListBuilder.create()
                .texOffs(0, 36).mirror().addBox(-3.0F, -0.5F, 0.0F, 3.0F, 1.0F, 5.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.5F, 1.5F, 2.4F, 0.0F, -0.8F, 0.0F));
        head.addOrReplaceChild("vo_mask", CubeListBuilder.create()
                .texOffs(18, 47).addBox(-3.0F, -1.5F, -0.5F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -5.5F, -4.6F));

        // ===== 虚空行者：身份件 =====
        head.addOrReplaceChild("vw_hood_top", CubeListBuilder.create()
                .texOffs(16, 41).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 1.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -8.0F, -1.5F));
        head.addOrReplaceChild("vw_hood_back", CubeListBuilder.create()
                .texOffs(18, 51).addBox(-2.5F, -3.0F, -0.5F, 5.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -5.0F, 4.4F));
        rightArm.addOrReplaceChild("vw_blade", CubeListBuilder.create()
                .texOffs(58, 49).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.2F, 9.0F, 0.0F, 0.3F, 0.0F, 0.2F));
        rightArm.addOrReplaceChild("vw_guard", CubeListBuilder.create()
                .texOffs(54, 36).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.2F, 9.0F, 0.0F, 0.3F, 0.0F, 0.2F));

        // ===== 血魔长老：身份件 =====
        body.addOrReplaceChild("be_robe_l", CubeListBuilder.create()
                .texOffs(58, 32).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 16.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.8F, 2.0F, 2.6F, 0.1F, 0.0F, -0.1F));
        body.addOrReplaceChild("be_robe_r", CubeListBuilder.create()
                .texOffs(58, 32).mirror().addBox(-1.0F, 0.0F, 0.0F, 2.0F, 16.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.8F, 2.0F, 2.6F, 0.1F, 0.0F, 0.1F));
        body.addOrReplaceChild("be_bead_1", CubeListBuilder.create()
                .texOffs(54, 38).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(-1.5F, 0.8F, -2.4F));
        body.addOrReplaceChild("be_bead_2", CubeListBuilder.create()
                .texOffs(54, 38).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.9F, -2.5F));
        body.addOrReplaceChild("be_bead_3", CubeListBuilder.create()
                .texOffs(54, 38).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(1.5F, 0.8F, -2.4F));

        // ===== 血魔修士：身份件 =====
        head.addOrReplaceChild("bc_mask", CubeListBuilder.create()
                .texOffs(36, 45).addBox(-3.0F, -1.0F, -0.5F, 6.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -3.5F, -4.5F));
        leftArm.addOrReplaceChild("bc_wrist_l", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.0F, 8.0F, 0.0F, 0.3F, 0.0F, -0.2F));
        rightArm.addOrReplaceChild("bc_wrist_r", CubeListBuilder.create()
                .texOffs(0, 32).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.0F, 8.0F, 0.0F, 0.3F, 0.0F, 0.2F));

        // ===== 劫灰修士：身份件 =====
        body.addOrReplaceChild("ash_tatter_1", CubeListBuilder.create()
                .texOffs(0, 42).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 10.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.6F, 3.0F, 2.7F, 0.1F, 0.0F, 0.1F));
        body.addOrReplaceChild("ash_tatter_2", CubeListBuilder.create()
                .texOffs(0, 42).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 10.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 2.5F, 2.9F, 0.2F, 0.0F, 0.0F));
        body.addOrReplaceChild("ash_tatter_3", CubeListBuilder.create()
                .texOffs(0, 42).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 10.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.6F, 4.0F, 2.7F, 0.1F, 0.0F, -0.1F));
        body.addOrReplaceChild("ash_shawl", CubeListBuilder.create()
                .texOffs(0, 53).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -1.0F, 2.2F, 0.1F, 0.0F, 0.0F));

        // ===== 魔道执事：身份件 =====
        leftArm.addOrReplaceChild("ds_book", CubeListBuilder.create()
                .texOffs(32, 48).addBox(-1.5F, -0.5F, -0.5F, 3.0F, 4.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.5F, 8.5F, -1.8F, -0.3F, 0.2F, 0.0F));
        rightArm.addOrReplaceChild("ds_pen", CubeListBuilder.create()
                .texOffs(54, 51).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-0.8F, 9.0F, -1.5F, 0.3F, 0.0F, 0.2F));

        // ===== 剑冢守灵：身份件 =====
        body.addOrReplaceChild("st_casket", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 3.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 2.0F, 2.8F, 0.1F, 0.0F, 0.0F));
        body.addOrReplaceChild("st_hilt_1", CubeListBuilder.create()
                .texOffs(16, 32).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(-2.0F, 5.0F, 3.6F));
        body.addOrReplaceChild("st_hilt_2", CubeListBuilder.create()
                .texOffs(16, 32).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 5.0F, 3.6F));
        body.addOrReplaceChild("st_hilt_3", CubeListBuilder.create()
                .texOffs(16, 32).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(2.0F, 5.0F, 3.6F));
        head.addOrReplaceChild("st_mask", CubeListBuilder.create()
                .texOffs(18, 47).addBox(-3.0F, -1.5F, -0.5F, 6.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -5.5F, -4.6F));
    }

    // ------------------------------------------------------------------
    // 骨架
    // ------------------------------------------------------------------

    /**
     * 64×64 的骨架。
     *
     * <p>六个人形部件与原版人形完全同位：头 {@code -4,-8,-4} 起 8×8×8；
     * 身 {@code -4,0,-2} 起 8×12×4；臂 {@code ±5,-2,0} 起 4×12×4；
     * 腿 {@code ±1.9,12,0} 起 4×12×4。差别只在多出来的 5 个部件
     * （发髻、两只护肩、披风、背后剑）。</p>
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;

        // ---- 头：贴图 UV 0,0（原版人形头） ----
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, none),
                PartPose.ZERO);

          // **hat 必须挂在【根】上，不能挂在 head 上！**
          //
          // 反编译 HumanoidModel 的构造函数确认过它的字节码：
          //     28: aload_1            <- 参数 1 = pRoot
          //     29: ldc  "head"
          //     31: ModelPart.getChild
          //     38: aload_1            <- 同样是 pRoot
          //     39: ldc  "hat"
          //     41: ModelPart.getChild
          // 也就是说 head 与 hat **都是从根上取的**，hat 不是 head 的子部件。
          // （我第一版修成了 head.addOrReplaceChild("hat", ...)，深了一层，
          //   于是照样抛 NoSuchElementException: Can't find part hat。）
          //
          // 这一个缺失引发了全部三次客户端崩溃：
          //   模型建不出来 -> 资源重载失败 -> **所有资源包被卸载**
          //   -> 核心着色器没了 -> RenderSystem.getShader() 为 null
          //   -> LevelRenderer.renderChunkLayer NPE -> 崩。
          //
          // 空帽子：CubeListBuilder 不带任何盒子，占位用，不画东西。
          root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);


        // 发髻：头顶正中一坨高起来的头发（正／侧／背面都是头发，顶面是发顶）
        head.addOrReplaceChild("hair_bun", CubeListBuilder.create()
                        .texOffs(32, 32).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F, none),
                PartPose.offset(0.0F, -7.5F, 0.0F));

        // ---- 躯干：贴图 UV 16,16 ----
        // 道袍的效果不打在这儿，也不额外加盒子：躯干贴图 v36 以下那 4 行
        // （对应世界坐标 y=8..12 的腰胯）直接画成袍摆，两条腿就"收"进袍子里了。
        // 这是 64×64 人形贴图下半区几乎没有空地的情况下最划算的一招。
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, none),
                PartPose.ZERO);

        // 披风：挂在肩后的一片薄布，静止时就带一点后倾，看起来是"垂"不是"贴"
        body.addOrReplaceChild("cape", CubeListBuilder.create()
                        .texOffs(40, 48).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 6.0F, 1.0F, none),
                PartPose.offsetAndRotation(0.0F, 0.5F, 2.8F, -0.22F, 0.0F, 0.0F));

        // 背后剑：斜挎在背上，剑尖朝左下
        body.addOrReplaceChild("blade", CubeListBuilder.create()
                        .texOffs(48, 32).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F, none),
                PartPose.offsetAndRotation(0.5F, 1.5F, 3.4F, 0.0F, 0.0F, 0.52F));
        body.addOrReplaceChild("blade_hilt", CubeListBuilder.create()
                        .texOffs(48, 40).addBox(-1.5F, -2.0F, -1.0F, 3.0F, 2.0F, 2.0F, none),
                PartPose.offsetAndRotation(-3.0F, 2.6F, 3.4F, 0.0F, 0.0F, 0.52F));

        // ---- 右臂：贴图 UV 40,16 ----
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, none),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        // 护肩：骑在肩膀外侧偏前，前伸的盒子天然像一块甲片
        rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                        .texOffs(32, 56).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 2.0F, 3.0F, none),
                PartPose.offset(-1.5F, -2.5F, 1.0F));

        // ---- 左臂：贴图 UV 40,16（镜像） ----
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, none),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                        .texOffs(32, 56).mirror().addBox(-2.0F, -1.0F, -2.5F, 4.0F, 2.0F, 3.0F, none),
                PartPose.offset(1.5F, -2.5F, 1.0F));

        // ---- 腿：贴图 UV 0,16（左右同一个 texOffs，靠 mirror() 翻面） ----
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        addIdentityParts(head, body, rightArm, leftArm);

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ------------------------------------------------------------------
    // 动画
    // ------------------------------------------------------------------

    /** @param value true 时摆"伸手扑击"的架势 */
    public CultivationHumanoidModel setReaching(boolean value) {
        this.reaching = value;
        return this;
    }

    /**
     * 先跑原版人形的通用动画（下蹲／举物／攻击挥手／左右手姿势都在这），
     * 再把自己那些部件的动作叠上去。
     */
    @Override
    public void setupAnim(CultivationMonsterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        boolean moving = entity.walkAnimation.isMoving();
        float breath = moving ? 0.0F : Mth.sin(ageInTicks * 0.06F) * 0.035F;
        // 保留 sway 只用于日志/后续调整，不再驱动 body 旋转
            float sway = moving ? 0.0F : Mth.cos(ageInTicks * 0.05F) * 0.05F;

        // 扑击架势：双臂前伸（原版僵尸那股压迫感，但袖子护肩都在）
        if (this.reaching) {
            float armSwing = Mth.cos(limbSwing * 0.6662F) * 0.35F * limbSwingAmount;
            this.rightArm.xRot = -1.30F + armSwing;
            this.leftArm.xRot = -1.30F - armSwing;
            this.rightArm.yRot = 0.05F;
            this.leftArm.yRot = -0.05F;
        }

        // 臂：走路时略外扩，免得贴着护肩穿模（护肩是臂的子部件，会跟着一起走）
        this.rightArm.zRot = -0.05F - limbSwingAmount * 0.08F;
        this.leftArm.zRot = 0.05F + limbSwingAmount * 0.08F;

        // 躯干：静止时轻微起伏（呼吸）。
        // **不能写 `this.body.y += breath`**：setupAnim 每帧都跑，
        // 本模组没有 resetPose，累加会把上一帧的值当基准（玩家报的
        // "身体各部分越离越远"就是这个）。先读本帧 super 算好的基准
        // （下蹲时原版会把它设成 3.2），再绝对赋值叠加呼吸。
        float bodyBaseY = this.body.y;
        this.body.y = bodyBaseY + breath;
        // **不要给 body 加 zRot**：原版 HumanoidModel.setupAnim 会把 body 的旋转
        // 复制给头部与双臂（head.yRot = body.yRot 那一套），于是整个上半身一起扭，
        // 看起来就是躯干在旋转、散开了（玩家报过）。
        // 改成极轻的横向呼吸（缩放），头与手臂不受影响。
        // **绝对值赋值，不能 *=**：setupAnim 每帧都跑，乘上去就再也下不来。
        // bodyBaseScale 是 applyVariant() 里记下的基准（= bulk）。
        this.body.xScale = this.bodyBaseScale * (1.0F + breath * 0.15F);
        this.body.zScale = this.bodyBaseScale * (1.0F + breath * 0.15F);

        // 披风：走起来往后掀，站定时轻轻飘
        this.cape.xRot = -0.22F - limbSwingAmount * 0.34F
                - Mth.sin(ageInTicks * 0.08F) * (0.05F + limbSwingAmount * 0.06F);

        // 发髻：走路时轻轻晃
        this.hairBun.xRot = Mth.cos(ageInTicks * 0.1F) * 0.045F;
        this.hairBun.zRot = sway * 0.6F;

        // ---- 身份件的小动作（只对可见件有观感，不可见件赋值无害；绝对赋值）----
        float idSway = Mth.sin(ageInTicks * 0.07F);
        // 傀儡队长：提线横梁轻晃、两条垂线反向摆
        this.puppetBar.zRot = idSway * 0.03F;
        this.puppetStringL.zRot = idSway * 0.08F;
        this.puppetStringR.zRot = -idSway * 0.08F;
        // 虚空霸主：4 片晶翼错相扇动
        this.voWingInnerL.zRot = idSway * 0.10F;
        this.voWingInnerR.zRot = -idSway * 0.10F;
        this.voWingOuterL.zRot = Mth.sin(ageInTicks * 0.07F + 0.8F) * 0.14F;
        this.voWingOuterR.zRot = -Mth.sin(ageInTicks * 0.07F + 0.8F) * 0.14F;
        // 虚空行者：兜帽后片轻飘
        this.vwHoodBack.xRot = -0.05F + idSway * 0.04F;
        // 血魔长老：血袍垂布飘 + 血珠轻摆
        this.beRobeL.xRot = 0.12F + idSway * 0.05F;
        this.beRobeR.xRot = 0.12F + idSway * 0.05F;
        this.beBead1.yRot = idSway * 0.45F;
        this.beBead2.yRot = Mth.sin(ageInTicks * 0.07F + 1.0F) * 0.45F;
        this.beBead3.yRot = Mth.sin(ageInTicks * 0.07F + 2.0F) * 0.45F;
        // 劫灰修士：三片破布不同相位地摆
        this.ashTatter1.xRot = 0.10F + idSway * 0.05F;
        this.ashTatter2.xRot = 0.18F + Mth.sin(ageInTicks * 0.09F + 1.2F) * 0.04F;
        this.ashTatter3.xRot = 0.06F + idSway * 0.05F;
        // 魔道执事：账册/笔跟着手臂微动即可（手臂已由原版动画驱动）
        this.dsBook.yRot = 0.25F + idSway * 0.06F;
        // 剑冢守灵：剑匣随呼吸极轻起伏
        this.stCasket.xRot = 0.12F + idSway * 0.02F;
    }
}
