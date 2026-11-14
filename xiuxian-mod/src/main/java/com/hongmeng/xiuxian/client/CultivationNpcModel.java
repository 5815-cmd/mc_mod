package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 修仙界 NPC 的人形模型（门派长老／炼丹师／云游商人共用一个骨架）。
 *
 * <p><b>为什么不直接复用 {@link CultivationHumanoidModel}：</b>那个模型的泛型被写死成
 * {@code HumanoidModel<CultivationMonsterEntity>}，而 NPC 继承的是
 * {@code PathfinderMob}——不是怪，不能塞进那条继承链（塞进去就会跟着怪的境界缩放涨血，
 * 而且会变成 {@code Monster} 被同伴索敌）。所以这里另起一个模型类，
 * 但<b>骨架与 UV 约定完全沿用原版人形那一套</b>。</p>
 *
 * <h2>UV 布局（64×64）</h2>
 *
 * <p>六个基础部件直接调 {@code HumanoidModel.createMesh(...)} 拿原版骨架，
 * 不手抄 texOffs——这样任何标准 64×64 人形皮肤贴上来都不会花脸：</p>
 *
 * <pre>
 *   头部   texOffs( 0,  0)              8×8×8
 *   身体   texOffs(16, 16)              8×12×4
 *   右臂   texOffs(40, 16)              4×12×4
 *   左臂   texOffs(32, 48).mirror()     4×12×4
 *   右腿   texOffs( 0, 16)              4×12×4
 *   左腿   texOffs(16, 48).mirror()     4×12×4
 *   帽子层 texOffs(32,  0)              8×8×8   ← 贴图上留空即不可见
 * </pre>
 *
 * <p>三个自建部件全部塞在原版人形贴图 v32..47 那条空白横带里（u32..63），
 * 与原版部件<b>零相交</b>（由 {@code tools/gen_beast_textures.py} 的
 * {@code check_layouts()} 逐面校验）：</p>
 *
 * <pre>
 *   发髻   texOffs(32, 32)  4×5×4     包围盒 u32..48  v32..45
 *   随配件 texOffs(48, 32)  3×3×1     包围盒 u48..56  v32..37
 *   长物   texOffs(56, 32)  2×10×2    包围盒 u56..64  v32..46
 * </pre>
 *
 * <p>第三个部件（{@code pouch}）与第四个（{@code staff}）刻意做成"换位置就是另一件东西"：
 * 同一个盒子挂在下巴上是长老的白须，挂在腰上是炼丹师的丹葫芦，
 * 挂在背上是商人的褡裢——省贴图，也省得为三个人写三套骨架。</p>
 *
 * @see CultivationNpcRenderer
 */
@OnlyIn(Dist.CLIENT)
public class CultivationNpcModel extends HumanoidModel<CultivationNpcEntity> {

    /** 模型层：三个 NPC 共用一套骨架。 */
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(com.hongmeng.xiuxian.HongmengMod.MOD_ID, "cultivation_npc"), "main");

    /** 发髻：头顶束起来的那一坨。 */
    private final ModelPart hairBun;

    /** body 的基准横向缩放（构造参数 bulk）；每帧呼吸用它做基准。 */
    private float bodyBaseScale = 1.0F;

    /** 随身件：长老=白须，炼丹师=丹葫芦，商人=褡裢。 */
    private final ModelPart pouch;

    /** pouch 的基准俯仰（构造时定下）；每帧绝对赋值要用它做基准。 */
    private final float pouchBaseXRot;

    /** 长物：商人的拐杖（挂在左臂上，跟着手臂摆）。 */
    private final ModelPart staff;

    private final boolean staffVisible;

    /**
     * @param root      {@link #createBodyLayer()} 烘出来的根部件
     * @param bulk      横向体型倍率（1.0 标准）
     * @param staff     是否显示拐杖
     * @param pouchPose 随身件的挂点；null 表示不显示
     */
    public CultivationNpcModel(ModelPart root, float bulk, boolean staff, PartPose pouchPose) {
        super(root);
        this.staffVisible = staff;

        this.hairBun = child(root, "head", "hair_bun");
        this.pouch = child(root, "body", "pouch");
        this.staff = child(root, "left_arm", "staff");

        // 随身件的位置按 NPC 走：下巴 / 腰 / 背
        this.pouch.visible = pouchPose != null;
        if (pouchPose != null) {
            this.pouch.setPos(pouchPose.x, pouchPose.y, pouchPose.z);
            this.pouch.xRot = pouchPose.xRot;
            this.pouch.yRot = pouchPose.yRot;
            this.pouch.zRot = pouchPose.zRot;
        }
        this.pouchBaseXRot = pouchPose != null ? pouchPose.xRot : 0.0F;
        this.staff.visible = staff;

        // 体型只压横向：头不动，腿只在 x 上压（照抄 CultivationHumanoidModel 的口径）
        this.bodyBaseScale = bulk;
        this.body.xScale = bulk;
        this.body.zScale = bulk;
        this.rightArm.xScale = bulk;
        this.rightArm.zScale = bulk;
        this.leftArm.xScale = bulk;
        this.leftArm.zScale = bulk;
        this.rightLeg.xScale = bulk;
        this.leftLeg.xScale = bulk;

        // NPC 永远空手站着：不摆攻击架势
        this.rightArmPose = ArmPose.EMPTY;
        this.leftArmPose = ArmPose.EMPTY;
    }

    /** 取子部件；部件树是本文件自己搭的，缺了就是写错了。 */
    private static ModelPart child(ModelPart root, String parent, String name) {
        ModelPart p = root.getChild(parent);
        if (!p.hasChild(name)) {
            throw new IllegalStateException("CultivationNpcModel 缺少部件: " + parent + "/" + name);
        }
        return p.getChild(name);
    }

    /** 骨架：原版人形六件套 + 帽子层 + 三个自建件，64×64。 */
    public static LayerDefinition createBodyLayer() {
        // 直接用原版的人形骨架，UV 与原版皮肤 100% 对齐
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // 发髻
        root.getChild("head").addOrReplaceChild("hair_bun", CubeListBuilder.create()
                        .texOffs(32, 32).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F,
                                CubeDeformation.NONE),
                PartPose.offset(0.0F, -7.0F, 0.0F));

        // 随身件：默认挂在下巴下方（长老的白须）；另外两位在构造器里改挂点
        root.getChild("body").addOrReplaceChild("pouch", CubeListBuilder.create()
                        .texOffs(48, 32).addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F,
                                CubeDeformation.NONE),
                POUCH_BEARD);

        // 拐杖：挂在左手末端，默认不可见
        root.getChild("left_arm").addOrReplaceChild("staff", CubeListBuilder.create()
                        .texOffs(56, 32).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 10.0F, 2.0F,
                                CubeDeformation.NONE),
                PartPose.offset(1.0F, 11.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * 三个 NPC 的挂点常量，渲染器直接引用，免得数字散落在两处。
     *
     * <p>都是 {@code body} 部件的子空间（躯干原点在角色腰部高度 y=0，脚在 y=12）。</p>
     */
    /** 长老：下巴底下垂着的白须。 */
    public static final PartPose POUCH_BEARD = PartPose.offset(0.0F, -9.0F, -3.5F);
    /** 炼丹师：系在丹田前的那只丹葫芦。 */
    public static final PartPose POUCH_HIP = PartPose.offset(1.8F, 10.0F, -2.9F);
    /** 商人：斜背在背后的褡裢。 */
    public static final PartPose POUCH_BACK = PartPose.offset(-2.0F, 6.5F, 2.8F);

    @Override
    public void setupAnim(CultivationNpcEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        boolean moving = entity.walkAnimation.isMoving();
        // 保留 sway 只用于日志/后续调整，不再驱动 body 旋转
            float sway = moving ? 0.0F : Mth.cos(ageInTicks * 0.045F) * 0.05F;
        float breath = moving ? 0.0F : Mth.sin(ageInTicks * 0.05F) * 0.03F;

        // 胸口轻微起伏：站着不动也像活人。
        // **绝对赋值**：`+=` 会把上一帧的值当基准一路累上去（见同目录
        // CultivationHumanoidModel 的注释）。先取本帧 super 算好的基准。
        float bodyBaseY = this.body.y;
        this.body.y = bodyBaseY + breath;
        // 同上：body.zRot 会传染给头与双臂，改成横向呼吸缩放。
        // 绝对值赋值（理由同 CultivationHumanoidModel：*= 会逐帧累乘）。
        this.body.xScale = this.bodyBaseScale * (1.0F + breath * 0.12F);
        this.body.zScale = this.bodyBaseScale * (1.0F + breath * 0.12F);

        // 发髻跟着晃
        this.hairBun.xRot = Mth.cos(ageInTicks * 0.09F) * 0.04F;
        this.hairBun.zRot = sway * 0.5F;

        // 拐杖：走路时随手臂自然摆（它是左臂的子部件，这里只补一点点自身摆动）
        if (this.staffVisible) {
            this.staff.zRot = 0.06F + Mth.cos(ageInTicks * 0.06F) * 0.02F;
        }

        // 随身件：白须随呼吸、葫芦/褡裢随走动轻晃。
        // **绝对赋值**：原来是 `this.pouch.xRot += ...`，而 pouch 不在
        // super.setupAnim 的管辖范围内（它只重置原版那六件），于是每帧
        // 都往上加 —— 站着不动白须也会一直转到天上去（玩家报的"部件乱飞"）。
        this.pouch.xRot = this.pouchBaseXRot
                + (moving ? Mth.cos(limbSwing * 0.6662F) * 0.05F * limbSwingAmount
                          : sway * 0.3F);
    }
}
