package com.hongmeng.xiuxian.entity.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 灵兽的共同父类：<b>契约、指令、骑乘、成长</b>四件事都在这里实现一次。
 *
 * <h3>骑乘走的是哪一条路</h3>
 *
 * <p>规格要求"挑一种成熟的写法照抄"。这里选的是<b>原版猪／炽足兽那条
 * {@code Entity#getControllingPassenger()} + {@code travel()} 路线</b>
 * （也就是 {@code ItemSteerable} 的宿主侧），原因：</p>
 *
 * <ul>
 *   <li>它<b>不需要</b>给玩家加任何 Attribute，也不碰 {@code abilities}——
 *       本模组已经有一条 {@code ModEvents.applyFlightPermission} 的飞行通道，
 *       再动 {@code abilities} 必然跟御剑打架；</li>
 *   <li>{@code travel()} 是完全自写的，所以"飞行用空格上升 / Shift 下降"
 *       和"地面用空格起跳 + 二段跳"可以在同一个方法里自然分叉；</li>
 *   <li>乘客的 {@code WASD} 由客户端照常写入玩家自己的 {@code xxa/zza}，
 *       服务端读 {@link #getRiddenInput} 即可，不需要额外的输入包。</li>
 * </ul>
 *
 * <p>原版马的 {@code PlayerRideableJumping} 那条路要多一个蓄力条、多一套
 * 跳跃强度同步包，对"二段跳"这个需求来说是过度设计。</p>
 *
 * <h3>数据同步</h3>
 *
 * <p>指令、成长阶段、有没有护甲都进 {@link SynchedEntityData}——
 * 规格第五节第 3 条要求客户端也拿得到（名牌要写"跟随/停留/游荡"、
 * HUD 要写成长阶段），不能只存服务端。</p>
 */
public abstract class SpiritBeastEntity extends TamableAnimal {

    // ------------------------------------------------------------------
    // 同步数据
    // ------------------------------------------------------------------

    private static final EntityDataAccessor<String> DATA_COMMAND =
            SynchedEntityData.defineId(SpiritBeastEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_GROWTH =
            SynchedEntityData.defineId(SpiritBeastEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_ARMORED =
            SynchedEntityData.defineId(SpiritBeastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_AIRBORNE =
            SynchedEntityData.defineId(SpiritBeastEntity.class, EntityDataSerializers.BOOLEAN);

    /** 护甲值。写成常量而不是读物品——物品在 BeastManager 里，不在这里碰注册表。 */
    public static final double BEAST_ARMOR_VALUE = 8.0D;

    /** 护甲带来的击退抗性。 */
    public static final double BEAST_ARMOR_KNOCKBACK = 0.25D;

    private static final String NBT_COMMAND = "HmBeastCommand";
    private static final String NBT_GROWTH = "HmBeastGrowth";
    private static final String NBT_ARMOR = "HmBeastArmor";
    private static final String NBT_FED = "HmBeastFed";
    private static final String NBT_CONTRACTED = "HmBeastContracted";

    // ------------------------------------------------------------------
    // 运行期状态（不进存档）
    // ------------------------------------------------------------------

    /** 乘客这次按下的跳跃力度；0 = 没按。 */
    private float playerJumpPower;

    /** 地面坐骑还能跳几次（二段跳用的空中次数）。 */
    private int airJumpsLeft;

    /** 上一个 tick 的骑手，用来检测"换人骑"并清掉残留的跳跃输入。 */
    private UUID lastRider;

    protected SpiritBeastEntity(EntityType<? extends SpiritBeastEntity> type, Level level) {
        super(type, level);
        // 灵兽不会因为玩家跑远就消失——丢了坐骑是最劝退的体验
        this.setPersistenceRequired();
        if (kind().flying()) {
            // 20 = 每秒最多转多少度，false = 不吃重力（重力由 travel 自己管，
            // 交给 FlyingMoveControl 会导致"松开空格就往下掉"）
            this.moveControl = new FlyingMoveControl(this, 20, false);
        }
    }

    /** 这只灵兽的静态数值（子类返回字面量）。 */
    public abstract BeastKind kind();

    // ------------------------------------------------------------------
    // 同步数据读写
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_COMMAND, BeastCommand.FOLLOW.name());
        this.entityData.define(DATA_GROWTH, BeastGrowth.JUVENILE.name());
        this.entityData.define(DATA_ARMORED, Boolean.FALSE);
        this.entityData.define(DATA_AIRBORNE, Boolean.FALSE);
    }

    public BeastCommand command() {
        return BeastCommand.byName(this.entityData.get(DATA_COMMAND));
    }

    public void setCommand(BeastCommand command) {
        this.entityData.set(DATA_COMMAND, (command == null ? BeastCommand.FOLLOW : command).name());
        applyCommand();
    }

    public BeastGrowth growth() {
        return BeastGrowth.byName(this.entityData.get(DATA_GROWTH));
    }

    public void setGrowth(BeastGrowth growth) {
        this.entityData.set(DATA_GROWTH, (growth == null ? BeastGrowth.JUVENILE : growth).name());
        // 幼年／成年／化形同时改体型与 hitbox：只看渲染缩放会出现
        // "看着还是幼崽，其实判定已经是化形的块头"
        refreshGrowthAttributes();
    }

    public boolean hasBeastArmor() {
        return this.entityData.get(DATA_ARMORED);
    }

    public void setBeastArmor(boolean armored) {
        this.entityData.set(DATA_ARMORED, armored);
        refreshGrowthAttributes();
    }

    public boolean airborne() {
        return this.entityData.get(DATA_AIRBORNE);
    }

    private void setAirborne(boolean value) {
        this.entityData.set(DATA_AIRBORNE, value);
    }

    // ------------------------------------------------------------------
    // AI
    // ------------------------------------------------------------------

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (kind().flying()) {
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
            nav.setCanOpenDoors(false);
            nav.setCanFloat(true);
            nav.setCanPassDoors(true);
            return nav;
        }
        return new GroundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.25D, 8.0F, 2.5F, false));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        // 挨打会还手；主人的敌人就是自己的敌人
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    /**
     * 把"跟随 / 停留 / 游荡"翻译到原版的坐着标记上。
     *
     * <p>原版 {@link SitWhenOrderedToGoal} 与 {@link FollowOwnerGoal} 只看
     * {@code isOrderedToSit()}，所以只要在 {@link #setCommand} 时把
     * "停留"映射过去，这两条 goal 就免费复用了，不必自己重写。</p>
     */
    private void applyCommand() {
        BeastCommand c = command();
        // setOrderedToSit 在客户端与服务端都会走，这里做的只是同步标记
        super.setOrderedToSit(c == BeastCommand.SIT);
        if (c == BeastCommand.SIT) {
            // 立刻停下，不然会滑出去半格才站住
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** 指令变了要让客户端也知道（名牌上写着当前指令）。 */
    @Override
    public void setOrderedToSit(boolean sitting) {
        super.setOrderedToSit(sitting);
        if (!this.level().isClientSide()) {
            BeastCommand want = sitting ? BeastCommand.SIT : command();
            if (want == BeastCommand.SIT && command() != BeastCommand.SIT) {
                this.entityData.set(DATA_COMMAND, BeastCommand.SIT.name());
            } else if (!sitting && command() == BeastCommand.SIT) {
                this.entityData.set(DATA_COMMAND, BeastCommand.FOLLOW.name());
            }
        }
    }

    /** 坐着 = 停留指令。两者永远一致。 */
    @Override
    public boolean isInSittingPose() {
        return super.isInSittingPose() || command() == BeastCommand.SIT;
    }

    // ------------------------------------------------------------------
    // 属性与成长
    // ------------------------------------------------------------------

    /**
     * 把成长阶段与护甲折算进属性。
     *
     * <p>做法：记下"基础值 × 倍率"的目标值，直接 {@code setBaseValue}。
     * 本模组的灵兽<b>不挂 AttributeModifier</b>（那需要固定 UUID，
     * 存档迁移时容易残留），倍率变化时重算一次即可——灵兽的属性本来就只由
     * 自身三个状态（成长、护甲、被骑）决定，没有外部来源。</p>
     */
    protected void refreshGrowthAttributes() {
        if (this.level().isClientSide()) {
            return;
        }
        BeastKind k = kind();
        double mult = growth().statMultiplier();

        setBase(Attributes.MAX_HEALTH, k.baseHealth() * mult);
        setBase(Attributes.ATTACK_DAMAGE, k.baseDamage() * mult);
        setBase(Attributes.MOVEMENT_SPEED, k.baseSpeed());

        // 护甲：直接覆盖 ARMOR 属性，卸下护甲就回到 0
        setBase(Attributes.ARMOR, hasBeastArmor() ? BEAST_ARMOR_VALUE : 0.0D);
        // 击退抗性：化形后的巨兽不该被骷髅一箭推着走
        double kb = hasBeastArmor() ? BEAST_ARMOR_KNOCKBACK : 0.0D;
        if (growth() == BeastGrowth.TRANSFORMED) {
            kb = Math.max(kb, 0.45D);
        }
        setBase(Attributes.KNOCKBACK_RESISTANCE, kb);

        // 属性变小之后当前血量可能超上限，夹一下
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void setBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        var instance = this.getAttribute(attribute);
        if (instance != null && Math.abs(instance.getBaseValue() - value) > 1.0E-6D) {
            instance.setBaseValue(value);
        }
    }

    /** 体型缩放：幼年 0.45、成年 1.0、化形 1.6（同时缩渲染与 hitbox）。 */
    @Override
    public float getScale() {
        return growth().scale();
    }

    /** 契约成立时的初始化：主人 + 幼年 + 跟随。 */
    public void onContracted(Player owner) {
        this.tame(owner);
        this.setCommand(BeastCommand.FOLLOW);
        setGrowth(BeastContract.STARTING_GROWTH);
        this.setHealth(this.getMaxHealth());
        this.setPersistenceRequired();
        this.getPersistentData().putBoolean(NBT_CONTRACTED, true);
    }

    /** 是不是已经契约过的灵兽（野生个体 false）。 */
    public boolean isContracted() {
        return this.getPersistentData().getBoolean(NBT_CONTRACTED) || this.isTame();
    }

    /** 主人是不是这个玩家（含创造模式旁观修正）。 */
    public boolean isOwnedBy(Player player) {
        if (player == null || !this.isTame()) {
            return false;
        }
        // TamableAnimal#getOwnerUUID 在 1.20.1 直接返回 UUID（可能为 null），
        // 不再包在 Optional 里
        UUID owner = this.getOwnerUUID();
        return owner != null && owner.equals(player.getUUID());
    }

    /** 主人的大境序号；拿不到返回 -1。 */
    public int ownerTier() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return -1;
        }
        LivingEntity owner = getOwner();
        if (owner instanceof ServerPlayer sp) {
            return com.hongmeng.xiuxian.core.CultivationCapability.get(sp).realm().tier();
        }
        // getOwner() 依赖实体加载；主人离线时退回"按 UUID 查"
        UUID id = this.getOwnerUUID();
        if (id != null) {
            ServerPlayer sp = serverLevel.getServer().getPlayerList().getPlayer(id);
            if (sp != null) {
                return com.hongmeng.xiuxian.core.CultivationCapability.get(sp).realm().tier();
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // 骑乘
    // ------------------------------------------------------------------

    /** 控制这只灵兽的乘客：第一个玩家乘客。骑乘关闭时恒为 null。 */
    @Override
    public LivingEntity getControllingPassenger() {
        if (!BeastRiding.ENABLED) {
            return null;
        }
        Entity first = this.getFirstPassenger();
        if (first instanceof Player player) {
            return player;
        }
        return super.getControllingPassenger();
    }

    /** 骑手（玩家）。没人在骑返回 null。 */
    public Player rider() {
        LivingEntity controller = this.getControllingPassenger();
        return controller instanceof Player player ? player : null;
    }

    public boolean isBeingRidden() {
        return rider() != null;
    }

    /** 能不能被这个玩家骑：契约过且是他的。骑乘系统关闭时永久 false。 */
    public boolean canBeRiddenBy(Player player) {
        return BeastRiding.ENABLED
                && this.isContracted() && this.isOwnedBy(player) && !player.isShiftKeyDown();
    }

    /**
     * 繁殖：灵兽<b>不繁殖</b>。
     *
     * <p>{@code AgeableMob} 要求实现它，返回 {@code null} 就是"生不出后代"，
     * 原版 {@code Turtle}、{@code Villager} 这些也是这么处理的。
     * 灵兽的来源只有一个：契灵符。</p>
     */
    @Override
    public SpiritBeastEntity getBreedOffspring(ServerLevel level,
                                               net.minecraft.world.entity.AgeableMob partner) {
        return null;
    }

    /**
     * 乘客坐在背上的高度（<b>模型像素 / 16</b>，不是"身高乘以某个系数"）。
     *
     * <p>这一点很容易搞反：原版 {@code Entity#startRiding} 把乘客放在
     * {@code 车.getY() + 车.getPassengersRidingOffset()} 处，而
     * {@code Entity.getY()} 是<b>碰撞箱底部</b>；模型的原点也在脚底
     * （模型 y=24 就是地面）。所以"鞍位在模型 y=6"翻译过来就是
     * {@code (24 - 6) / 16 = 1.125} 格——是一个可以从模型直接读出来的数，
     * 拿 {@code getBbHeight()} 去乘系数只会得到一个偏高的座位，
     * 表现是"人浮在坐骑背上半个格子"。</p>
     */
    protected double riderYOffset() {
        return feetToRider(6.0D);
    }

    /**
     * 模型坐标 → 乘客偏移：{@code (24 - modelY) / 16}。
     *
     * <p>24 = 模型里脚底所在的像素行，16 = 一格 16 像素。</p>
     */
    protected static double feetToRider(double modelY) {
        return (24.0D - modelY) / 16.0D;
    }

    /**
     * 乘客的落座高度。
     *
     * <p>原版 {@code Entity#startRiding} 最终走的是
     * {@code vehicle.getPassengersRidingOffset()}，所以这里覆盖它就够了——
     * 不需要（也不该）去碰 {@code getPassengerRidingPosition}：
     * 后者在 1.20.1 已经改名成 {@code getPassengerAttachmentPoint} 那一族，
     * 强行覆写只会得到一个永远不被调用的方法。</p>
     */
    @Override
    public double getPassengersRidingOffset() {
        return riderYOffset();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return BeastRiding.ENABLED
                && this.getPassengers().isEmpty() && passenger instanceof Player;
    }

    /**
     * 玩家按了一下"跳跃/上升"。
     *
     * <p><b>为什么需要这么一条外部调用：</b>原版的鞍类坐骑（猪、炽足兽）
     * 会实现 {@code ItemSteerable} 并覆写 {@code Mob#onPlayerJump}——
     * 但那个方法在 1.20.1 里<b>只由 {@code ItemSteerable} 的调用链调用</b>，
     * 而那条链要求实体先实现该接口。本模组的飞行坐骑用的是
     * {@code getControllingPassenger} + 自写行进 那条路（见类注释的选型说明），
     * 因此没有免费的跳跃回调可拿。</p>
     *
     * <p>替代方案是 {@code /hongmeng beastjump}：客户端在骑乘时按空格发一次
     * 这条命令，服务端把"跳跃请求"打在玩家身上，本方法在下一 tick 读走它。
     * 走命令而不是自定义网络包是刻意的——规格要求<b>不得改动既有协议 id 顺序</b>，
     * 而攻击/突破早就在用"客户端发命令"这条既有通道。</p>
     */
    public void requestJump(int power) {
        this.playerJumpPower = Math.max(this.playerJumpPower, power);
    }

    /** 玩家按空格时由 {@code /hongmeng beastjump} 的落点调用。 */
    public void onPlayerJumpRequest(int jumpPower) {
        if (!isBeingRidden()) {
            return;
        }
        if (kind().flying()) {
            this.playerJumpPower = jumpPower;
            return;
        }
        // 地面坐骑：二段跳。第一次跳是原版的，空中再按才是"二段"
        if (!this.onGround() && this.airJumpsLeft > 0) {
            this.airJumpsLeft--;
            double forward = riderForward();
            this.setDeltaMovement(this.getDeltaMovement().add(
                    Math.sin(Math.toRadians(this.getYRot())) * forward, 0.0D,
                    -Math.cos(Math.toRadians(this.getYRot())) * forward));
            this.setDeltaMovement(this.getDeltaMovement().x, 0.72D,
                    this.getDeltaMovement().z);
            this.hasImpulse = true;
            this.playSound(SoundEvents.RABBIT_JUMP, 1.0F, 0.9F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        this.getX(), this.getY() + 0.15D, this.getZ(),
                        10, 0.35D, 0.05D, 0.35D, 0.05D);
            }
            // 二段跳的余量用掉了，这一帧不能再当"按住空格"用
            this.playerJumpPower = 0.0F;
        } else {
            this.playerJumpPower = jumpPower;
        }
    }

    /** 骑乘时向前的冲量（二段跳用）。 */
    private double riderForward() {
        Player p = rider();
        if (p == null) {
            return 0.0D;
        }
        return p.zza > 0.0F ? 0.35D : 0.0D;
    }

    /**
     * 骑乘时的一 tick。
     *
     * <p><b>为什么飞行坐骑的位移写在这里而不是 {@code travel()}：</b>
     * {@code LivingEntity.travel} 里那句
     * {@code if (isControlledByLocalInstance())} 对服务端的坐骑永远是 false——
     * 本地实例是骑在它背上的<b>玩家</b>，不是它自己。于是服务端会掉进
     * "用 AI 寻路移动"的分支，而玩家看到的客户端预测又会被服务端不断纠正，
     * 表现就是"骑上去能走但一直在抖、速度也不对"。</p>
     *
     * <p>原版马没这个问题是因为它走 {@code PlayerRideableJumping} +
     * 输入包那条完全不同的链路。既然本模组选了
     * {@code getControllingPassenger} + 自写行进（见类注释的选型说明），
     * 就<b>必须</b>在服务端权威地算位移，所以落在 {@code aiStep} 里。</p>
     *
     * <p>地面坐骑仍然交给 {@code travel()}：那条路原版是通的
     * （{@code getRiddenInput}/{@code getRiddenSpeed} 会被真正调用），
     * 不必另写一份地面物理。</p>
     */
    @Override
    public void aiStep() {
        if (kind().flying() && isBeingRidden() && !this.level().isClientSide()) {
            Player driver = rider();
            if (driver != null) {
                travelFlyingRidden(driver);
            }
        }
        super.aiStep();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        // 骑乘系统关闭：老存档里已经骑在背上的玩家，登录后自动请下来。
        if (!BeastRiding.ENABLED && !this.getPassengers().isEmpty()) {
            this.ejectPassengers();
        }
        if (this.onGround()) {
            this.airJumpsLeft = AIR_JUMPS;
        }
        if (isBeingRidden()) {
            // 骑手的跳跃输入是一次性的，用完就清。
            // 放在 tick 的<b>末尾</b>：aiStep（飞行位移）这一 tick 已经先读过它了。
            this.playerJumpPower = 0.0F;
            if (kind().flying()) {
                setAirborne(true);
            }
            this.fallDistance = 0.0F;
        } else if (kind().flying()) {
            setAirborne(!this.onGround());
        }
    }

    @Override
    public void rideTick() {
        super.rideTick();
        // 换骑手 / 被骑的第一帧：把上一位的残留输入清掉
        UUID now = this.getFirstPassenger() == null ? null : this.getFirstPassenger().getUUID();
        if (now == null ? lastRider != null : !now.equals(lastRider)) {
            this.playerJumpPower = 0.0F;
            this.airJumpsLeft = AIR_JUMPS;
            lastRider = now;
        }
    }

    /** 地面坐骑的空中跳跃次数。 */
    public static final int AIR_JUMPS = 1;

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(NBT_COMMAND, command().name());
        tag.putString(NBT_GROWTH, growth().name());
        tag.putBoolean(NBT_ARMOR, hasBeastArmor());
        tag.putInt(NBT_FED, this.getPersistentData().getInt(NBT_FED));
        if (this.getPersistentData().contains(NBT_CONTRACTED)) {
            tag.putBoolean(NBT_CONTRACTED, this.getPersistentData().getBoolean(NBT_CONTRACTED));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_COMMAND)) {
            this.entityData.set(DATA_COMMAND, BeastCommand.byName(tag.getString(NBT_COMMAND)).name());
        }
        if (tag.contains(NBT_GROWTH)) {
            this.entityData.set(DATA_GROWTH, BeastGrowth.byName(tag.getString(NBT_GROWTH)).name());
        }
        this.entityData.set(DATA_ARMORED, tag.getBoolean(NBT_ARMOR));
        if (tag.getBoolean(NBT_CONTRACTED)) {
            this.getPersistentData().putBoolean(NBT_CONTRACTED, true);
        }
        // 妖丹进度存在 PersistentData 里（BeastManager.feedGrowth 读写它），
        // 这里跟着实体一起存，否则放回袋子再拿出来会把喂食进度清空
        this.getPersistentData().putInt(NBT_FED, tag.getInt(NBT_FED));
        applyCommand();
        refreshGrowthAttributes();
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level().isClientSide()) {
            refreshGrowthAttributes();
        }
    }

    // ------------------------------------------------------------------
    // 行进
    // ------------------------------------------------------------------

    /** 乘客的 WASD。和原版猪一样：读乘客自己的输入。 */
    @Override
    protected Vec3 getRiddenInput(Player driver, Vec3 travelVector) {
        float strafe = driver.xxa;
        float forward = driver.zza;
        if (this.isInWater()) {
            forward *= 0.6F;
        }
        Vec3 v = new Vec3(strafe, 0.0D, forward);
        // 原版猪就是这么归一化的：斜着走不该比直着快
        if (v.lengthSqr() > 1.0D) {
            v = v.normalize();
        }
        return v;
    }

    @Override
    protected float getRiddenSpeed(Player driver) {
        return (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED)
                * kind().rideSpeed());
    }

    /**
     * 行进。<b>骑乘与自由行动在这一处分叉</b>。
     *
     * <p>地面坐骑骑乘时走原版 {@code LivingEntity.travel} 的"有乘客"分支：
     * 它会自动调用本类覆写的 {@link #getRiddenInput} / {@link #getRiddenSpeed}，
     * 摩擦、跳跃、落水这些细节都是白送的。</p>
     *
     * <p>飞行坐骑<b>不</b>走这里——服务端坐骑的
     * {@code isControlledByLocalInstance()} 恒为 false（本地实例是背上的玩家），
     * 位移改在 {@link #aiStep} 里权威结算，详见那里的说明。</p>
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (kind().flying() && isBeingRidden()) {
            // 飞行且有人骑：位移已在 aiStep 里算完，这里不能再动一次
            return;
        }
        // 没人骑的飞行灵兽：用"移动控制自己管"的飞行分支
        // （照抄雷鹰那套，保证它能自己起飞、不会在地上走到天荒地老）
        if (kind().flying() && !this.onGround() && this.getNavigation().isInProgress()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.91D));
            return;
        }
        super.travel(travelVector);
    }

    /** 被骑时的转向 + 输入解算；返回是否已经处理掉（飞行）还是交给 travel（地面）。 */
    private boolean steerRidden() {
        Player driver = rider();
        if (driver == null) {
            return false;
        }
        // 1) 转向：骑手看哪边就朝哪边（飞行坐骑连俯仰一起跟）
        float yaw = driver.getYRot();
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(driver.getXRot() * 0.5F);
        this.setRot(yaw, this.getXRot());
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        return true;
    }

    /** 由输入算出世界方向上的水平速度。 */
    private Vec3 riddenDirection(Player driver, float yaw, double[] lenOut) {
        Vec3 input = getRiddenInput(driver, Vec3.ZERO);
        float speed = getRiddenSpeed(driver);
        double len = Math.min(1.0D, input.length());
        lenOut[0] = len;
        if (len <= 1.0E-4D) {
            return Vec3.ZERO;
        }
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        double rx = -fz;
        double rz = fx;
        return new Vec3((fx * input.z + rx * input.x) * speed, 0.0D,
                (fz * input.z + rz * input.x) * speed);
    }

    /** 地面坐骑骑乘时的位移（在 {@code travel} 的"有乘客"分支里被原版调用）。 */
    private void travelGroundRidden(Player driver) {
        // 地面坐骑交给原版：它会调 getRiddenInput/getRiddenSpeed 与 jumpFromGround
        super.travel(new Vec3(driver.xxa, 0.0D, driver.zza));
    }

    /**
     * 飞行坐骑：空格上升 / Shift 下降，不按就缓缓下沉（悬停由 kind 决定）。
     *
     * <p>服务端权威结算，调用点只有一个：{@link #aiStep()}。</p>
     */
    private void travelFlyingRidden(Player driver) {
        if (!steerRidden()) {
            return;
        }
        double[] lenOut = new double[1];
        Vec3 horizontal = riddenDirection(driver, this.getYRot(), lenOut);
        double dirX = horizontal.x;
        double dirZ = horizontal.z;

        Vec3 motion = this.getDeltaMovement();
        double vy = motion.y * 0.60D;

        boolean ascend = this.playerJumpPower > 0.0F;
        boolean descend = driver.isShiftKeyDown();

        if (ascend) {
            vy += 0.20D;
        } else if (descend) {
            vy -= 0.24D;
        } else if (hoverCapable()) {
            // 悬停：抵消重力。只有"能悬停"的灵兽（雷鹰）吃这一条，
            // 蛟龙不悬停，松开就会慢慢往下沉——这也是两者的手感差别
            vy *= 0.55D;
            if (vy < 0.0D) {
                vy *= 0.35D;
            }
        } else {
            vy -= 0.045D;
        }
        vy = Math.max(-0.85D, Math.min(0.85D, vy));

        this.setDeltaMovement(dirX, vy, dirZ);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.94D));

        if (this.onGround() && vy < 0.0D) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0.0D,
                    this.getDeltaMovement().z);
        }
        this.fallDistance = 0.0F;
        setAirborne(!this.onGround() || ascend);

        if (this.tickCount % 6 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.05D, this.getZ(),
                    2, 0.3D, 0.02D, 0.3D, 0.01D);
        }
    }

    /** 能不能悬停（雷鹰的招牌）。子类覆盖。 */
    public boolean hoverCapable() {
        return kind().role() == BeastRole.AIR_SCOUT;
    }

    /** 飞行灵兽不摔伤。 */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return !kind().flying() && super.causeFallDamage(distance, multiplier, source);
    }

    // ------------------------------------------------------------------
    // 交互
    // ------------------------------------------------------------------

    /** 显示名牌：契约过的灵兽一直显示（名牌上写着指令与阶段）。 */
    @Override
    public boolean shouldShowName() {
        return this.isContracted() || super.shouldShowName();
    }

    @Override
    public net.minecraft.network.chat.Component getName() {
        if (!this.isContracted()) {
            return super.getName();
        }
        String base = this.getType().getDescription().getString();
        return net.minecraft.network.chat.Component.literal(
                "\u00a7b" + growth().colored() + " \u00a7f" + base
                        + " \u00a78[" + command().colored() + "\u00a78]"
                        + (hasBeastArmor() ? " \u00a77\u26e8" : ""));
    }

    /**
     * 右键交互的总入口。
     *
     * <p>优先级（与规格第四节的交互表一致）：</p>
     * <ol>
     *   <li>潜行右键 → 循环指令 跟随 → 停留 → 游荡</li>
     *   <li>手持灵兽袋 → 收进袋子（由 {@code BeastManager} 处理，这里放行）</li>
     *   <li>手持灵兽护甲 → 给灵兽穿上</li>
     *   <li>空手右键自己的灵兽 → 骑上</li>
     * </ol>
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);

        // 灵兽袋与护甲交给 BeastManager（它才认识物品，本类不许碰注册表）
        if (com.hongmeng.xiuxian.entity.beast.BeastManager.isPouch(stack)
                || com.hongmeng.xiuxian.entity.beast.BeastManager.isBeastArmor(stack)) {
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!this.isContracted() || !this.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        // 潜行：循环指令
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            BeastCommand next = command().next();
            setCommand(next);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a77" + this.getType().getDescription().getString()
                            + " \u00a77指令 \u2192 " + next.colored()), true);
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.4F);
            return InteractionResult.CONSUME;
        }

        // 空手：骑上（骑乘系统 2026-11-07 起关闭；此分支只保留给以后恢复）
        if (BeastRiding.ENABLED && stack.isEmpty() && !player.isShiftKeyDown()) {
            if (this.isInSittingPose() && command() == BeastCommand.SIT) {
                setCommand(BeastCommand.FOLLOW);
            }
            if (player.startRiding(this)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        kind().flying()
                                ? "\u00a7b\u7a7a\u683c\u4e0a\u5347 / Shift \u4e0b\u964d\uff0c"
                                + "\u6f5c\u884c\u53f3\u952e\u4e0b\u5750\u9a91"
                                : "\u00a7bWASD \u9a7e\u9a6d\uff0c\u7a7a\u683c\u4e8c\u6bb5\u8df3\uff0c"
                                + "\u6f5c\u884c\u53f3\u952e\u4e0b\u5750\u9a91"), true);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        // 契约不靠喂食；妖丹是"成长"的道具，由 BeastManager 专门处理
        return false;
    }

    @Override
    public MobType getMobType() {
        // 灵兽不是亡灵：不让"治疗药水伤害妖兽"这条规则生效，
        // 否则玩家喂一口治疗药水会把坐骑喂死
        return MobType.UNDEFINED;
    }

    /** 灵兽不会因为难度的境界缩放而被加成一圈（它们是玩家的资产）。 */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** 没有主人的野生灵兽不会主动攻击玩家。 */
    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player p && this.isOwnedBy(p)) {
            return false;
        }
        return super.canAttack(target);
    }

    /** 走路时给一点地面粒子，让"跑得很快"看得出来。 */
    protected void spawnRunParticles() {
        if (this.level() instanceof ServerLevel serverLevel
                && this.tickCount % 3 == 0 && !this.onGround() && kind().flying()) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY(), this.getZ(), 1, 0.2D, 0.0D, 0.2D, 0.0D);
        }
    }

    /** 空 AI 目标占位，子类可以用来挂自己的战斗行为。 */
    protected void addBeastGoals() {
        // 默认没有额外 goal
    }

    /** 让子类也能注册一个"什么都不做"的 goal（保留给未来）。 */
    protected static Goal noopGoal() {
        return new Goal() {
            @Override
            public boolean canUse() {
                return false;
            }
        };
    }

    /** 供 {@code MoveControl} 子类使用（避免未使用告警）。 */
    protected MoveControl beastMoveControl() {
        return this.moveControl;
    }
}
