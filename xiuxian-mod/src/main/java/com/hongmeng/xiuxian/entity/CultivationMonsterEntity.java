package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.difficulty.MobScaling;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 修仙界敌怪的共同父类。
 *
 * <p>把"品阶"这件事收在一处：血量／攻击／速度／经验全部按 {@link MobRank} 放大，
 * 名牌统一写成 {@code §<颜色><品阶> §7<怪名>}，境界缩放也只在这里做一次。</p>
 *
 * <p>数值口径：<b>最终属性 = 基础值 × 品阶倍率 × 玩家境界系数</b>。
 * 前两项在 {@link #scaledAttributes} 里直接乘进注册值；境界系数受"加入世界时玩家是谁"
 * 影响，只能等到实体真正进世界（{@link #onAddedToLevel}）时再补。</p>
 *
 * <p>注意：{@code MobScaling.onEntityJoin} 会对所有 {@code Monster} 生效，
 * 但对本类只做标记不做数值缩放（见 {@code MobScaling}），否则品阶与境界会被乘两遍。</p>
 */
public abstract class CultivationMonsterEntity extends Monster {

    /** 本怪的基础击杀经验，最终经验 = 基础经验 × 品阶经验倍率。 */
    private final int baseExperience;

    /**
     * 这只怪的<b>灵根品质</b>（玩家要求：同境界的小怪也分强弱）。
     *
     * <p>生成时按「混沌 60% / 鸿蒙 30% / 先天 10%」掷点，
     * 强度倍率见 {@link com.hongmeng.xiuxian.difficulty.MonsterSpiritRoot}。</p>
     */
    private com.hongmeng.xiuxian.spirit.Rarity spiritRootRarity =
            com.hongmeng.xiuxian.spirit.Rarity.INNATE;

    /** 灵根品质（不会为 null）。 */
    public com.hongmeng.xiuxian.spirit.Rarity spiritRootRarity() {
        return spiritRootRarity;
    }

    /** 设置灵根品质（生成时由强度缩放调用）。 */
    public void setSpiritRootRarity(com.hongmeng.xiuxian.spirit.Rarity rarity) {
        if (rarity != null) {
            this.spiritRootRarity = rarity;
        }
    }

    protected CultivationMonsterEntity(EntityType<? extends CultivationMonsterEntity> type,
                                       Level level, int baseExperience) {
        super(type, level);
        this.baseExperience = baseExperience;
        this.xpReward = Math.max(1, (int) Math.round(
                baseExperience * rank().xpMultiplier()));
        // 修仙界的妖物不会因为玩家跑远就自己消失——那样追到一半"蒸发"很出戏
        this.setPersistenceRequired();
    }

    // ------------------------------------------------------------------
    // 品阶
    // ------------------------------------------------------------------

    /** 品阶。子类通常返回一个字面量常量。 */
    public abstract MobRank rank();

    // ------------------------------------------------------------------
    // 贴图
    // ------------------------------------------------------------------

    /**
     * 贴图名 → 完整资源路径；同名的只建一次。
     *
     * <p>惰性缓存是刻意的：{@code ResourceLocation} 属于客户端资源命名空间，
     * 类加载时就去碰它会让<b>专用服务端</b>多加载一个用不到的类；
     * 放进 {@code computeIfAbsent} 之后，只有真的画到这只怪才建。</p>
     */
    private static final java.util.Map<String, net.minecraft.resources.ResourceLocation> TEXTURES
            = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 本怪的贴图名（不含 {@code .png}）：默认就是实体注册名。
     *
     * <p><b>为什么贴图归实体管，而不是归渲染器：</b>本模组的渲染器是
     * <b>共用的</b>——{@code CultivationHumanoidRenderer} 一套骨架配十几个变体，
     * 靠"一只怪一张贴图"区分（注册名 == 贴图名）。渲染器构造时绑死一张贴图
     * 就等于"一种怪一个渲染器实例"，与"共用模型"的目标正好相反。</p>
     *
     * <p>于是贴图名在整个项目里只出现在一处：{@code ModEntities} 的注册名。
     * 改名时只改一处，贴图文件、渲染器、离线校验脚本会一起跟着走。</p>
     */
    /**
     * 是否<b>强制</b>绘制发光层（{@code RankGlowLayer}）。
     *
     * <p>默认 {@code false}：普通品阶（{@code MobRank.NORMAL}）不发光，这是"普通杂鱼"
     * 的视觉信号。少数**普通品阶但依赖发光**的怪（泽魅的青磷灯、晶蛛的灵晶、
     * 虚空水母的虚空眼）重写本方法返回 {@code true}，让 {@code _glow.png} 真正亮起来。</p>
     */
    public boolean forceGlow() {
        return false;
    }

    public String textureName() {
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries
                .BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
        return id == null ? "spirit_wolf" : id.getPath();
    }

    /** 贴图名 → 完整资源路径。 */
    public static net.minecraft.resources.ResourceLocation textureFor(String name) {
        return TEXTURES.computeIfAbsent(name, n -> new net.minecraft.resources.ResourceLocation(
                com.hongmeng.xiuxian.HongmengMod.MOD_ID, "textures/entity/" + n + ".png"));
    }

    /**
     * 本怪这一帧要用的贴图。
     *
     * <p>如果实体身上带了 {@code hm_boss_variant}（两只维度 Boss 会带），就用
     * <b>那张专属贴图</b>而不是按注册名取 —— 这就是玩家要的「boss 模型要单独设置」。
     * 关键在于它**完全沿用同一套 UV 版式**，所以不会出现模型错位
     * （玩家强调过「这次我不希望模型出错」）。</p>
     */
    public net.minecraft.resources.ResourceLocation textureLocation() {
        String variant = this.getPersistentData().getString("hm_boss_variant");
        if (variant != null && !variant.isEmpty()) {
            return textureFor(variant);
        }
        return textureFor(this.textureName());
    }

    // ------------------------------------------------------------------
    // 属性
    // ------------------------------------------------------------------

    /**
     * 按品阶放大后的属性表。
     *
     * <p>这是 {@code static} 的：属性只在 {@code EntityAttributeCreationEvent} 里
     * 按<b>实体类型</b>注册一次，那时还没有任何实例存在。</p>
     *
     * @param rank        品阶
     * @param health      基础生命
     * @param damage      基础攻击
     * @param speed       基础移速
     * @param followRange 追击距离
     * @param armor       护甲
     */
    protected static AttributeSupplier.Builder scaledAttributes(MobRank rank, double health,
                                                               double damage, double speed,
                                                               double followRange, double armor) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health * rank.healthMultiplier())
                .add(Attributes.ATTACK_DAMAGE, damage * rank.damageMultiplier())
                .add(Attributes.MOVEMENT_SPEED, speed * rank.speedMultiplier())
                .add(Attributes.FOLLOW_RANGE, followRange)
                .add(Attributes.ARMOR, armor)
                // 飞行速度：**所有妖兽都注册**，哪怕它是地面怪。
                //
                // 踩过的坑：雷鹰用了 FlyingMoveControl，但属性表里没有
                // FLYING_SPEED，于是 FlyingMoveControl 抛
                // IllegalArgumentException: Can't find attribute ... flying_speed。
                // 触发条件很阴——只要**已加载区块里有一只雷鹰**，
                // 服务端 Done 之后约 2 秒就崩，而"服务端能干净启动"这个测试
                // 是抓不到的（那时区块里还没有雷鹰）。
                //
                // 地面怪多一个用不到的属性完全无害，但这样"以后谁再加一只
                // 飞行怪忘了加属性"这一类崩溃就再也不会发生了。
                .add(Attributes.FLYING_SPEED, speed * rank.speedMultiplier());
    }

    /**
     * 品阶 + 玩家境界的总倍率。
     *
     * <p>境界表复用 {@link MobScaling#realmFactor(int)}，不另抄一份——
     * 原版怪与自定义怪必须同步涨，不然玩家一到金丹就会觉得自定义怪"软了"。</p>
     */
    public double rankAndRealmMultiplier() {
        return rank().healthMultiplier()
                * MobScaling.realmScaleFor(this.level(), this);
    }

    /** 标记：境界这一层的倍率已经补过了，别补第二遍。 */
    private static final String REALM_SCALED_TAG = "hongmeng_realm_scaled";

    /** 持久化 + 同步：这只怪自己的境界（1~10，练气~鸿蒙）。 */
    private static final String REALM_TIER_TAG = "hongmeng_realm_tier";

    /**
     * 怪物境界的<b>同步</b>数据。
     *
     * <p>境界在服务端算（要看最近的玩家），但名牌与血条是客户端画的——
     * 所以必须走 {@link net.minecraft.world.entity.SynchedEntityData} 送到客户端，
     * 而不是只塞进 persistentData（那个不同步）。</p>
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_REALM_TIER =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    CultivationMonsterEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);

    /**
     * Boss 阶段（1~3）的<b>同步</b>数据。
     *
     * <p>阶段附件（柜门开合 / 齿轮转速 / 面具脱落 / 核心亮度）是**客户端模型**画的，
     * 而 {@code BossPhase} 原先只写 persistentData（ForgeData 不同步到客户端）——
     * 结果模型永远停在一阶段。这里加一条同步字段：服务端写、客户端读。</p>
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_BOSS_PHASE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    CultivationMonsterEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);

    /**
     * Boss 额外体型倍率（1.0 = 不放大；转阶段 1.18 / 1.42；世界 Boss 1.55 / 1.75）。
     *
     * <p>之前只写在 persistentData 里，而 {@code BossPhaseRenderer} 在**客户端**读——
     * 同步不过去，实机就没有转阶段放大。改成同步字段。</p>
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_BOSS_SCALE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    CultivationMonsterEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /**
     * <b>巨化倍率</b>（第四轮 §2.1）：祭坛四 Boss 1.50 / 焚天炎帝·虚空龙帝 1.30 /
     * 仙庭之主 1.35 / 恶 1.00；1.0 = 不放大。
     *
     * <p>它与 {@link #DATA_BOSS_SCALE}（阶段/入场倍率）是<b>两套独立的倍率</b>：</p>
     * <pre>
     *   渲染最终倍率 = bossRenderScale × giantScale     （BossPhaseRenderer 只乘一次）
     *   命中箱倍率   = min(2.2, giantScale)             （阶段倍率刻意不进 hitbox）
     * </pre>
     *
     * <p>同步字段：服务端在 {@code BossPhase.tick} 的 phase 初始化处设置一次
     * （全项目唯一设置点，按实体注册名映射）；存档重载时从 persistentData 补回。</p>
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_GIANT_SCALE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    CultivationMonsterEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /** 同步数据是否已初始化：构造期（父类构造器里 defineSynchedData 之前）读 giantScale 的兜底。 */
    private boolean syncReady;

    /**
     * <b>当前招式 id</b>（同步 byte；A 的 {@code BossAttackKit} 写，客户端压迫感读）。
     *
     * <table border="1">
     *   <caption>取值</caption>
     *   <tr><th>值</th><th>招式</th></tr>
     *   <tr><td>0</td><td>无</td></tr>
     *   <tr><td>1</td><td>slam 震地重击</td></tr>
     *   <tr><td>2</td><td>shockwave 冲击波</td></tr>
     *   <tr><td>3</td><td>summon 召唤</td></tr>
     *   <tr><td>4</td><td>spell 法术</td></tr>
     *   <tr><td>5</td><td>breath 吐息</td></tr>
     *   <tr><td>6</td><td>ambush 突袭</td></tr>
     *   <tr><td>7</td><td>cataclysm 天崩</td></tr>
     *   <tr><td>8</td><td>charge 冲撞</td></tr>
     *   <tr><td>9</td><td>enrage 狂暴</td></tr>
     * </table>
     *
     * <p><b>为什么走同步字段</b>：{@code CombatShake} 是 {@code @OnlyIn(CLIENT)}，服务端的
     * {@code BossAttackKit} 碰不到；把"当前在放什么招"同步出来，客户端 {@code BossPresenceClient}
     * 就能按招式给不同力度的镜头震动，且战斗逻辑零改动。</p>
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> DATA_BOSS_ATTACK =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    CultivationMonsterEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BYTE);

    /**
     * <b>当前招式状态</b>（同步 byte；0=idle / 1=windup 前摇 / 2=active 判定 / 3=recover 后摇）。
     *
     * <p>客户端只用它做"判定帧的重击震动"：{@code state==2 && attack==1} 时重震一次；
     * 前摇（1）也可以给一点轻微前置震动提示"要来了"。</p>
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> DATA_BOSS_STATE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    CultivationMonsterEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BYTE);

    /** persistentData 里的巨化值 key（同步字段不持久化，存档重载时用它恢复）。 */
    public static final String GIANT_TAG = "hongmeng_giant_scale";

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_REALM_TIER, 1);
        this.entityData.define(DATA_BOSS_PHASE, 0);
        this.entityData.define(DATA_BOSS_SCALE, 1.0F);
        this.entityData.define(DATA_GIANT_SCALE, 1.0F);
        this.entityData.define(DATA_BOSS_ATTACK, (byte) 0);
        this.entityData.define(DATA_BOSS_STATE, (byte) 0);
        this.syncReady = true;
    }

    /** 这只怪自己的境界（1~10）。服务端算、客户端读同一份同步值。 */
    public int realmTier() {
        return this.entityData.get(DATA_REALM_TIER);
    }

    /** Boss 阶段（0 = 不是 Boss / 尚未进入，1~3 = 当前阶段）。客户端可直接读。 */
    public int bossPhase() {
        return this.entityData.get(DATA_BOSS_PHASE);
    }

    /** 写 Boss 阶段（服务端调；自动夹到 0~3 并同步给客户端）。 */
    public void setBossPhase(int phase) {
        this.entityData.set(DATA_BOSS_PHASE, Math.max(0, Math.min(3, phase)));
    }

    /** Boss 额外体型倍率（客户端渲染读；1.0 = 不放大）。 */
    public float bossRenderScale() {
        return this.entityData.get(DATA_BOSS_SCALE);
    }

    /** 写 Boss 额外体型倍率（服务端调；夹到 1.0~2.0 并同步）。 */
    public void setBossRenderScale(float scale) {
        this.entityData.set(DATA_BOSS_SCALE, Math.max(1.0F, Math.min(2.0F, scale)));
    }

    // ------------------------------------------------------------------
    // 巨化（第四轮 §2.1）
    // ------------------------------------------------------------------

    /** 巨化倍率（客户端渲染/命中箱读；1.0 = 不放大）。 */
    public float giantScale() {
        if (!this.syncReady) {
            return 1.0F;
        }
        return this.entityData.get(DATA_GIANT_SCALE);
    }

    /**
     * 写巨化倍率（服务端调；夹到 1.0~2.2 并同步 + 立刻重算命中箱）。
     *
     * <p><b>一只 Boss 只该设置一次</b>：调用点是 {@code BossPhase.tick} 的 phase 初始化
     * （按实体注册名映射，见 {@code BossPhase#giantScaleFor}）；仙庭之主构造器若也调
     * {@code setGiantScale(1.35f)}，值相同、无害。已设置过（存档重载恢复）就不覆盖。</p>
     */
    public void setGiantScale(float scale) {
        if (!this.syncReady) {
            return;
        }
        float v = Math.max(1.0F, Math.min(2.2F, scale));
        this.getPersistentData().putFloat(GIANT_TAG, v);
        this.entityData.set(DATA_GIANT_SCALE, v);
        this.refreshDimensionsQuietly();
    }

    /** 命中箱重算的容错壳（构造期/未入世界时失败无所谓，下一次同步会补）。 */
    private void refreshDimensionsQuietly() {
        try {
            this.refreshDimensions();
        } catch (Throwable ignored) {
            // 忽略：调用点本来就会在下一 tick / 同步回调里再试
        }
    }

    /**
     * <b>命中箱随巨化同步</b>（第四轮 §2.1）。
     *
     * <p>只有 BOSS / OVERLORD 品阶按 {@code min(2.2, giantScale)} 放大；</p>
     * <ul>
     *   <li><b>阶段倍率（bossRenderScale）不进 hitbox</b> —— 否则三阶段碰撞体积会变成
     *       1.42×1.5 倍，对撞、寻路、击退都会失控；</li>
     *   <li>普通/精英/队长/杂兵（含被 {@code hm_rank_override} 降级成杂兵的 Boss 实体）
     *       一律返回注册尺寸，行为与改动前完全一致；</li>
     *   <li>客户端读的是同一个同步字段，所以服务端/客户端命中箱不会不一致。</li>
     * </ul>
     */
    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        net.minecraft.world.entity.EntityDimensions base = super.getDimensions(pose);
        if (!this.syncReady) {
            return base;
        }
        try {
            MobRank r = this.effectiveRank();
            if (r != MobRank.BOSS && r != MobRank.OVERLORD) {
                return base;
            }
            float g = this.giantScale();
            if (g <= 1.001F) {
                return base;
            }
            float k = Math.min(2.2F, g);
            return net.minecraft.world.entity.EntityDimensions.scalable(base.width * k, base.height * k);
        } catch (Throwable t) {
            return base;
        }
    }

    /** 巨化值同步到客户端时重算命中箱（客户端也要有正确的包围盒）。 */
    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_GIANT_SCALE.equals(key)) {
            this.refreshDimensionsQuietly();
        }
    }

    // ------------------------------------------------------------------
    // Boss 招式 / 状态同步（供 A 的 BossAttackKit 写、客户端压感读）
    // ------------------------------------------------------------------

    /** 当前招式 id（见 {@link #DATA_BOSS_ATTACK} 的取值表）。客户端可读。 */
    public byte bossAttack() {
        return this.syncReady ? this.entityData.get(DATA_BOSS_ATTACK) : (byte) 0;
    }

    /** 写当前招式 id（服务端 {@code BossAttackKit} 调；null 安全，范围夹到 0~9）。 */
    public void setBossAttack(byte attackId) {
        if (!this.syncReady) {
            return;
        }
        byte v = attackId < 0 ? 0 : (attackId > 9 ? 9 : attackId);
        this.entityData.set(DATA_BOSS_ATTACK, v);
    }

    /** 当前招式状态（0=idle / 1=windup / 2=active / 3=recover）。客户端可读。 */
    public byte bossState() {
        return this.syncReady ? this.entityData.get(DATA_BOSS_STATE) : (byte) 0;
    }

    /** 写当前招式状态（服务端 {@code BossAttackKit} 调；范围夹到 0~3）。 */
    public void setBossState(byte state) {
        if (!this.syncReady) {
            return;
        }
        byte v = state < 0 ? 0 : (state > 3 ? 3 : state);
        this.entityData.set(DATA_BOSS_STATE, v);
    }

    /** 境界中文名，例如"化神"。 */
    public String realmCn() {
        return MobRank.realmCn(realmTier());
    }

    private void setRealmTier(int tier) {
        this.entityData.set(DATA_REALM_TIER,
                Math.max(1, Math.min(MobRank.REALM_TIER_COUNT, tier)));
    }

    /**
     * 取一只生物的品阶。不是本模组的修仙妖兽就按普通算。
     *
     * <p>领域效果要按品阶决定"对首领封顶 2%、对头目以下秒杀"，所以到处都要问这一句。
     * 放在这里而不是 {@code MobRank} 里：{@code MobRank} 刻意零 Minecraft 依赖，
     * 离线验证器要能直接编译它。</p>
     */
    public static MobRank rankOf(net.minecraft.world.entity.Entity entity) {
        return entity instanceof CultivationMonsterEntity monster
                ? monster.effectiveRank() : MobRank.NORMAL;
    }

    /**
     * <b>这一只怪实际生效的品阶</b>（可以是"被降级"之后的）。
     *
     * <h2>为什么需要"实例级品阶"</h2>
     * <p>{@link #rank()} 是**类级**的（写在每个实体类里）。但刷怪时会遇到
     * 「用 boss 级实体当普通怪」的情况，例如虚空裂隙的刷怪表：</p>
     * <pre>
     * case VOID -&gt; boss ? VOID_OVERLORD
     *            : captain ? VOID_WALKER        // VOID_WALKER 是 BOSS 品阶
     *            : elite   ? (roll &lt; 55 ? VOID_WALKER : ...)
     *            : (roll &lt; 55 ? DEMON_GENERAL   // DEMON_GENERAL 也是 BOSS 品阶
     *               : BLOOD_DEMON_ELDER);
     * </pre>
     * <p>于是**每一只**都带着 boss 血条 —— 玩家截图里 6 条血条叠在一起就是这个。</p>
     *
     * <p>现在刷怪时会给这类怪打一个"降级标记"，{@link #effectiveRank()} 读它。
     * 这样<b>同一套实体既能当 boss、也能当填场小怪</b>，不必再各写一个新类。</p>
     */
    public MobRank effectiveRank() {
        net.minecraft.nbt.CompoundTag pd = this.getPersistentData();
        int forced = pd.getInt("hm_rank_override");
        if (forced > 0) {
            MobRank[] all = MobRank.values();
            if (forced <= all.length) {
                return all[forced - 1];
            }
        }
        return this.rank();
    }

    /**
     * 把这一只怪**降级**（1 基序号，对应 {@link MobRank#values()}）。
     *
     * <p>只有刷怪逻辑该调它；0 或越界表示"不覆盖，用类级品阶"。</p>
     */
    public void demoteRank(MobRank rank) {
        if (rank == null) {
            return;
        }
        this.getPersistentData().putInt("hm_rank_override", rank.ordinal() + 1);
    }

    /**
     * 锚定境界：<b>副本维度用副本的设计基准</b>，其余按最近玩家的境界。
     *
     * <p>⚠ {@code Dungeon.baseTier()} 以前<b>只用于显示文字和刷怪数量</b>，
     * 从来没拿来定怪的境界 —— 于是高境界玩家进低级副本，怪照样跟着变高，
     * <b>副本分级形同虚设</b>。玩家实测反馈「进去后怪物依然是鸿蒙境界」，
     * 一半原因在这里。</p>
     *
     * <p>现在：副本维度 → 怪按该副本的 {@code baseTier} 定境界；
     * 非副本（主世界 / 真世界）→ 仍按最近玩家（真世界另有帝境段位规则）。</p>
     */
    private static int anchorRealmTier(Level level, CultivationMonsterEntity mob) {
        try {
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                com.hongmeng.xiuxian.dungeon.Dungeon d =
                        com.hongmeng.xiuxian.dungeon.Dungeon.byDimension(
                                level.dimension().location().getPath());
                if (d != null) {
                    // 副本里：按副本基准（这就是"这个副本该有的强度"）
                    return Math.max(1, Math.min(10, d.baseTier()));
                }
            }
        } catch (Throwable ignored) {
            // 判不出来就退回按玩家
        }
        ServerPlayer player = nearestPlayer(level, mob);
        if (player == null) {
            return 1;
        }
        return com.hongmeng.xiuxian.core.CultivationCapability.get(player).realm().tier();
    }

    /** 当前维度对应的副本；不是副本维度（或判不出来）返回 {@code null}。 */
    private static com.hongmeng.xiuxian.dungeon.Dungeon dungeonAt(Level level) {
        try {
            if (level instanceof net.minecraft.server.level.ServerLevel
                    && level.dimension().location() != null) {
                return com.hongmeng.xiuxian.dungeon.Dungeon.byDimension(
                        level.dimension().location().getPath());
            }
        } catch (Throwable ignored) {
            // 维度未注册 / 离线断言：当作非副本
        }
        return null;
    }

    /** 加入世界时按玩家境界补一次属性（品阶已经在注册值里了）。 */
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (this.level().isClientSide()) {
            return;
        }
        net.minecraft.nbt.CompoundTag pd = this.getPersistentData();
        // ---- 巨化（第四轮 §2.1）：同步字段不持久化，读档时从 persistentData 补回 ----
        // 放在 REALM_SCALED_TAG 的早退之前，保证读档回来的 Boss 一定恢复巨化/命中箱。
        float savedGiant = pd.getFloat(GIANT_TAG);
        if (savedGiant > 1.001F && this.giantScale() < 1.001F) {
            this.setGiantScale(savedGiant);
        }
        if (pd.getBoolean(REALM_SCALED_TAG)) {
            // 读档回来的怪：境界已经定过，但同步数据要从持久化里补回来
            if (pd.contains(REALM_TIER_TAG)) {
                setRealmTier(pd.getInt(REALM_TIER_TAG));
            }
            return;
        }
        pd.putBoolean(REALM_SCALED_TAG, true);

        // ---- 1. 这只怪自己的境界（规格 §2.3）----
        // 锚定玩家的境界 + 品阶加成（1/3/8/20/64 ≈ 3⁰~3⁴，与标准值每境 ×3 同尺）。
        // 真实世界开启后：怪**不再锚定玩家**，而是按玩家指定的固定帝境段位重排
        // （普通=仙人随机 / 精英=帝境初期 / 队长=帝境中期 / 头目=帝境后期 / 首领=鸿蒙）。
        // 这是玩家明确要求的："怪物强度升级对标帝境……普通小怪是仙人境界随机"。
        int tier;
        boolean realWorld = com.hongmeng.xiuxian.realworld.RealWorld.active(this.level())
                && this.level() instanceof net.minecraft.server.level.ServerLevel;
        if (realWorld) {
            // 真实世界：先看**副本维度的阶梯**（虚空=仙人/魔道=飞升/妖兽=道印/纪元=化神），
            // 不是副本维度（主世界）才按品阶重排。
            int byDungeon = com.hongmeng.xiuxian.realworld.RealWorld.tierForDungeon(
                    this.level().dimension().location().getPath(),
                    this.rank().ordinal());
            tier = byDungeon > 0 ? byDungeon
                    : com.hongmeng.xiuxian.realworld.RealWorld.tierFor(
                            this.rank().ordinal(), this.getRandom().nextFloat());
        } else {
            // 普通规则：境界锚点 = 副本的设计基准，非副本才看最近玩家。
            //
            // ⚠ 这里以前**只有**最近玩家的境界：玩家到了鸿蒙，
            //   进任何副本怪都变成鸿蒙 —— 副本分级完全失效。
            //   Dungeon.baseTier() 明明写着每个副本的设计基准，
            //   却只被拿去显示文字和算刷怪数量。
            tier = MobRank.monsterRealmTier(anchorRealmTier(this.level(), this), this.rank());
        }
        pd.putInt(REALM_TIER_TAG, tier);
        setRealmTier(tier);

        // ---- 2. 境界缩放（2026-11-07 修正：副本内按「副本挑战境界」封顶）----
        //
        // 玩家反馈：「一个头目才元婴 1.5 亿的血，我鸿蒙境界居然不能秒杀」。
        // 根因：显示境界取 baseTier（元婴），真正的缩放却按最近玩家境界（鸿蒙 ×520），
        // 再叠副本倍率 → 元婴头目 1.5 亿血。现在副本内取
        //   scaleTier = min(最近玩家境界, Dungeon.challengeTier())
        // 按门槛境界进来的玩家难度不变；超过门槛的玩家回头刷低副本 = 割草。
        // 真实世界已被重排到固定帝境段位，缩放不超过怪自己的境界。
        int playerTier = MobScaling.nearestPlayerTier(this.level(), this);
        double realmHp = 1.0D;
        double realmAtk = 1.0D;
        if (playerTier > 0) {
            int scaleTier = playerTier;
            if (realWorld) {
                scaleTier = Math.min(playerTier, tier);
            } else {
                com.hongmeng.xiuxian.dungeon.Dungeon dungeon = dungeonAt(this.level());
                if (dungeon != null) {
                    scaleTier = Math.min(playerTier, dungeon.challengeTier());
                }
            }
            scaleTier = Math.max(1, Math.min(MobRank.REALM_TIER_COUNT, scaleTier));
            // 生命与攻击用两张系数表：生命跟标准值（每境 ×3），攻击跟玩家生命曲线
            // （第 9/10 境按 ×10 续接）。原因见 MobScaling.realmAttackFactor 的注释。
            realmHp = MobScaling.realmScaleForTier(scaleTier);
            realmAtk = MobScaling.realmAttackScaleForTier(scaleTier);
        }
        MobScaling.applyScaling(this, realmHp, realmAtk, "hongmeng_cultivator_rank");

        // ---- 3. 标准强度下限（规格 §2.3：不能低于 RealmStats.standard(tier)）----
        applyStandardStrengthFloor(tier, this.effectiveRank(), realmHp, realmAtk);

        this.setHealth(this.getMaxHealth());
    }

    /**
     * 把生命/攻击补到"本境标准强度"以上。
     *
     * <p>规格 §2.3 原话："强力怪物最低也要对标标准强度修为（不能低于
     * {@code RealmStats.standard(tier)}）"。{@code RealmStats} 派生出的两条锚线是</p>
     * <pre>
     *   本境普通妖兽生命 = standard(tier) × {@code MOB_HEALTH_RATIO}（0.32）
     *   本境普通妖兽攻击 = {@code MOB_ATTACK_FLOOR}[锚定境界]（按"同境护甲后 2%/击"锚定）
     * </pre>
     * <p>自定义妖兽的注册值是手调的（品阶 × 基础值），高阶时可能远低于本境标准；
     * 这里只做<b>下限补足</b>（低了才补，高了不动），所以低境界的老数值不受影响。</p>
     *
     * <p><b>2026-11-06「小怪数值下调」的两处修正</b>（玩家鸿蒙实机反馈）：</p>
     * <ol>
     *   <li>攻击下限不再用"标准值 × 0.06"（与玩家生命不是同一刻度，道印/飞升段
     *       小怪一击 6~7% 血），改为 {@code RealmStats.mobAttack} 的显式表
     *       （同境普通怪护甲后 ≈2% 生命/击）；</li>
     *   <li><b>精英不再按"锚定境界 +1"取指数下限</b>：精英的境界标签是 anchor+1，
     *       但玩家此时生命还停在 anchor 段；直接取 {@code standard(anchor+1)} 的下限
     *       等于把「下一个大境」的标准值套在当前玩家身上 —— 飞升玩家遇到的精英
     *       一击可达 20% 血。现在退回锚定境界取普通怪下限、再乘本怪品阶伤害倍率
     *       （精英 ×1.5），品阶强弱由倍率表达，不再由境界指数放大。</li>
     * </ol>
     *
     * <p><b>为什么要除以 realmFactor：</b>{@code MobScaling} 挂的是
     * {@code MULTIPLY_TOTAL}，而原版的属性结算顺序是"先加后乘"——
     * ADDITION 修饰符会被境界倍率再乘一次。想让<b>最终值</b>大致落在下限上，
     * 就得把加成除以那个倍率（灵根随机会在下限之上再乘 1.27~1.90，这是刻意的强弱分层）。</p>
     */
    private void applyStandardStrengthFloor(int tier, MobRank rank,
                                            double realmHpFactor, double realmAtkFactor) {
        double hpFactor = Math.max(1.0D, realmHpFactor);
        double atkFactor = Math.max(1.0D, realmAtkFactor);
        topUpTo(Attributes.MAX_HEALTH, "hongmeng_realm_floor_health",
                com.hongmeng.xiuxian.core.RealmStats.mobHealth(tier), hpFactor);
        // 攻击下限：按「锚定境界」取普通怪下限，再乘本怪当前品阶的伤害倍率。
        // 精英的境界 = 锚定 +1（monsterRealmTier），所以锚定要退回一级；
        // 被副本槽位降级的实体用 effectiveRank（否则 BOSS 实体会按 ×3 取下限）。
        MobRank floorRank = rank == null ? this.effectiveRank() : rank;
        int anchorTier = floorRank == MobRank.ELITE ? Math.max(1, tier - 1) : tier;
        double atkFloor = com.hongmeng.xiuxian.core.RealmStats.mobAttack(anchorTier)
                * floorRank.damageMultiplier();
        topUpTo(Attributes.ATTACK_DAMAGE, "hongmeng_realm_floor_damage", atkFloor, atkFactor);
    }

    private void topUpTo(net.minecraft.world.entity.ai.attributes.Attribute attribute,
                         String name, double floor, double realmFactor) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance instance =
                this.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        java.util.UUID id = java.util.UUID.nameUUIDFromBytes(
                name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        instance.removeModifier(id);
        double current = instance.getValue();
        if (current + 1e-6D >= floor) {
            return;     // 已经不低于本境标准，不动它
        }
        instance.addPermanentModifier(new net.minecraft.world.entity.ai.attributes
                .AttributeModifier(id, name, (floor - current) / realmFactor,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
    }

    // ------------------------------------------------------------------
    // 境界压制减伤（规格 §2.3）
    // ------------------------------------------------------------------

    /**
     * 玩家境界低于怪物境界时，这只怪受到的伤害打折。
     *
     * <p>钩在 {@link #hurt} 上——这是所有伤害的<b>唯一入口</b>：玩家的近战、功法、
     * 领域百分比、原版岩浆/摔落，全部经过这里。减伤表是纯数据
     * （{@link MobRank#SUPPRESSION_BY_GAP}，可离线断言），最低减到 30%，不会免伤。</p>
     *
     * <p><b>领域伤害也吃这个减伤</b>——"怪物有减伤效果"没有例外条款。
     * 唯一的调整是土的秒杀：它打的是 {@code 当前生命 × 10}，
     * 即使被压到 30% 也还剩 3 倍当前生命，所以"秒杀"在压制下依然是秒杀。</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, applyRealmSuppression(source, amount));
    }

    /** 计算减伤后的伤害。非玩家来源、同级或更低境界 → 原样返回。 */
    public float applyRealmSuppression(DamageSource source, float amount) {
        if (amount <= 0.0F || this.level().isClientSide()) {
            return amount;
        }
        int monsterTier = realmTier();
        if (monsterTier <= 1) {
            return amount;
        }
        ServerPlayer attacker = playerOf(source);
        if (attacker == null) {
            return amount;
        }
        int playerTier = com.hongmeng.xiuxian.core.CultivationCapability.get(attacker)
                .realm().tier();
        double multiplier = MobRank.suppressionMultiplier(playerTier, monsterTier);
        if (multiplier >= 1.0D) {
            return amount;
        }
        return (float) (amount * multiplier);
    }

    /** 伤害来源里的玩家（先看归属者，再看直接来源）。 */
    private static ServerPlayer playerOf(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 名牌
    // ------------------------------------------------------------------

    /**
     * 名牌：{@code §<颜色><品阶> §7<怪名>}，例如 {@code §5头目 §7魔道统领}。
     *
     * <p>这里覆盖 {@link #getName()} 而不是在构造器里 {@code setCustomName}：
     * 命名过的怪会被 {@code MobScaling} 当成"剧情怪"跳过缩放，也会被命名牌改坏。</p>
     */
    @Override
    public Component getName() {
        MobRank r = rank();
        int tier = realmTier();
        // 境界是自己算出来的（服务端）+ 同步过来的（客户端），所以名牌上直接写出来：
        // 玩家一眼就知道"这只怪比我高几个境界、会不会有减伤"。
        String realm = tier > 1 ? " \u00a78[" + MobRank.realmCn(tier) + "]" : "";
        return Component.literal("\u00a7" + r.colorCode() + r.cn() + " \u00a77"
                + this.getType().getDescription().getString() + realm);
    }

    /** 高阶怪头顶的名牌默认就要看得见。 */
    @Override
    public boolean shouldShowName() {
        return rank().isRare() || super.shouldShowName();
    }

    // ------------------------------------------------------------------
    // 伤害软上限
    // ------------------------------------------------------------------

    /**
     * 单次攻击最多打掉目标多少比例的生命。
     *
     * <p><b>2026-11-02 已迁移到护甲之后</b>：这个 35% 上限原来压在「攻击力」上，
     * 而护甲减免发生在它之后 —— 护甲 92% 时单次最多只能打掉
     * {@code 35% × 8% = 2.8%} 最大生命，理论上至少 36 下才能打死玩家，
     * 于是「Boss 攻击调到多高都没威胁」。</p>
     *
     * <p>现在同样一条「单次 ≤ 最大生命 35%」的保险放在
     * {@link com.hongmeng.xiuxian.compat.ArmorMitigation#onLivingHurt} 里，
     * 对<b>护甲后的最终伤害</b>生效；本类不再做数值压缩。</p>
     *
     * <p>常量保留仅为兼容旧断言/外部引用（值不变）。</p>
     */
    @Deprecated
    public static final double MAX_HIT_RATIO = 0.35D;

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // 攻击动作广播：让玩家看得出"这一下打出来了"
        try {
            if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                byte kind = (byte) switch (this.effectiveRank()) {
                    case BOSS, OVERLORD -> 1;   // 重砸
                    case CAPTAIN -> 2;          // 突刺
                    default -> 0;               // 横扫
                };
                var pkt = new com.hongmeng.xiuxian.network.MobAttackAnimPacket(
                        this.getId(), kind);
                for (net.minecraft.server.level.ServerPlayer p : sl.players()) {
                    if (p.distanceToSqr(this) <= 48.0D * 48.0D) {
                        com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(p, pkt);
                    }
                }
            }
        } catch (Throwable ignored) {
            // 广播失败不影响伤害
        }
        // 数值上限已交给 ArmorMitigation（护甲后结算）；这里只保留攻击表现。
        return super.doHurtTarget(target);
    }

    // ------------------------------------------------------------------
    // 专属 BOSS 血条（原版 ServerBossEvent）
    // ------------------------------------------------------------------

    /**
     * 头目 / 首领横贯屏幕上方的专属血条。
     *
     * <p><b>用原版 {@code ServerBossEvent}</b>（就是末影龙 / 凋灵那一条）：
     * 它自带同步（{@code ClientboundBossEventPacket}），不需要自定义包；
     * 进度、名字、颜色都是服务端说了算，客户端一行代码都不用写。</p>
     *
     * <h3>出现与消失</h3>
     *
     * <p>照抄原版凋灵的做法（<b>这是唯一被原版验证过的路径</b>）：</p>
     * <ul>
     *   <li>{@link #startSeenByPlayer} / {@link #stopSeenByPlayer}——玩家进入 /
     *       离开<b>实体追踪范围</b>时收放，掉线、换维度、区块卸载都会走到这里；</li>
     *   <li>每 10 tick 再按 {@value #BOSS_BAR_RANGE} 格做一次距离裁剪，
     *       免得"隔着 200 格还挂着一条头目血条"把 HUD 占满；</li>
     *   <li>死亡 / 被移除时 {@link #remove} 里 {@code removeAllPlayers()}，
     *       条子立刻消失，不会留一条空血条在屏幕上方。</li>
     * </ul>
     *
     * <h3>重复条</h3>
     *
     * <p>一条命<b>只有一个</b> {@code ServerBossEvent}（字段惰性创建、只创建一次），
     * 而 {@code addPlayer} 内部是 Set，重复添加不会发第二遍包——
     * 所以同一只怪永远只有一条。场上有两只头目时会有<b>两条</b>：
     * 原版客户端按事件注册顺序自上而下堆叠，这正是末影龙 + 凋灵同时在场时的表现，
     * 也是刻意的——每条都要显示自己的进度，合并成一条反而看不出谁快死了。</p>
     */
    private ServerBossEvent bossEvent;

    /** 玩家离 BOSS 多远还能看见它的血条（格）。 */
    public static final double BOSS_BAR_RANGE = 48.0D;

    /** 距离裁剪的检查间隔（tick）。 */
    private static final int BOSS_BAR_INTERVAL = 10;

    /** 距离裁剪的倒计时。 */
    private int bossBarCooldown;

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.rank().hasBossBar()) {
            this.bossBar().addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (this.bossEvent != null) {
            this.bossEvent.removePlayer(player);
        }
    }

    /** 惰性创建——**只在服务端**（客户端实例永远是 null，也就不会去碰这个字段）。 */
    private ServerBossEvent bossBar() {
        if (this.bossEvent == null) {
            MobRank r = this.rank();
            this.bossEvent = new ServerBossEvent(this.getName(),
                    bossBarColor(r), bossBarOverlay(r));
            this.bossEvent.setProgress(this.getHealth() / Math.max(1.0F, this.getMaxHealth()));
        }
        return this.bossEvent;
    }

    /** 品阶 → 原版 BOSS 条颜色：头目紫、首领红（玩家明确要求，见 {@link MobRank}）。 */
    private static BossEvent.BossBarColor bossBarColor(MobRank rank) {
        return switch (rank.bossTint()) {
            case PURPLE -> BossEvent.BossBarColor.PURPLE;
            case RED -> BossEvent.BossBarColor.RED;
            case NONE -> BossEvent.BossBarColor.WHITE;
        };
    }

    /**
     * 品阶 → 阶段刻度样式。
     *
     * <p>玩家要的是"带名字与阶段刻度的样式"，刻度就是原版那几档 {@code NOTCHED_*}：
     * 头目 6 段、首领 10 段。数字越小刻度越粗，首领的段数多一档，
     * 光看条的密集程度就能分出高下。</p>
     */
    private static BossEvent.BossBarOverlay bossBarOverlay(MobRank rank) {
        return switch (rank.bossTint()) {
            case PURPLE -> BossEvent.BossBarOverlay.NOTCHED_6;
            case RED -> BossEvent.BossBarOverlay.NOTCHED_10;
            case NONE -> BossEvent.BossBarOverlay.PROGRESS;
        };
    }

    /** 每 tick 同步进度 + 每 {@value #BOSS_BAR_INTERVAL} tick 做一次玩家名单裁剪。 */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        // ---- 怪物的格挡（让战斗对称：玩家能弹反它，它也能弹反玩家）----
        MobGuard.tick(this);

        // ---- 精英出场演出：第一次照面时绕它转 1.2 秒 ----
        EliteEntrance.tick(this);

        // ---- 怪物心智：逼近 / 绕后 / 撤退 / 蓄力猛扑 ----
        //
        // 玩家要求「让他们有自己的想法」。原版 MeleeAttackGoal 是**直线追**，
        // 所有怪行为完全一样，打起来像打木桩。这一层在 goal 之上加心情，
        // 不改原版 goal（改了会连锁影响寻路与攻击判定）。
        MobMind.tick(this);

        // ---- Boss 远程攻击与法术（玩家要求：boss 不仅仅会近战还会远程）----
        // 纯服务端判定，内部已按距离分档并节流；只在 BOSS/首领品阶上生效。
        BossAbilities.tick(this);
        if (!this.effectiveRank().hasBossBar()) {
            return;
        }
        ServerBossEvent bar = this.bossBar();
        // ---- 末影龙式血条：**分段随阶段递减** + 名字带阶段 ----
        //
        // 玩家要求「boss 的血条要学习末影龙那种」。
        // 末影龙那条的特征是：顶部通栏 + 紫色名牌 + **分段刻度**。
        // 这里让分段数随三阶段变化（12 → 10 → 6），玩家一眼看出"还剩几段"，
        // 并在名字后缀标出当前形态。
        int phase = com.hongmeng.xiuxian.entity.BossPhase.phaseOf(this);
        bar.setOverlay(switch (phase) {
            case 3 -> BossEvent.BossBarOverlay.NOTCHED_6;
            case 2 -> BossEvent.BossBarOverlay.NOTCHED_10;
            default -> BossEvent.BossBarOverlay.NOTCHED_12;
        });
        String suffix = switch (phase) {
            case 3 -> " \u00a74【第三阶段 · 堕落】";
            case 2 -> " \u00a75【第二阶段 · 魔化】";
            default -> "";
        };
        bar.setName(net.minecraft.network.chat.Component.literal(
                this.getName().getString() + suffix));
        bar.setProgress(Mth.clamp(this.getHealth() / Math.max(1.0F, this.getMaxHealth()),
                0.0F, 1.0F));
        // ---- 第四轮 · BOSS 条颜色也随阶段推进（二阶段紫、三阶段红）----
        // 契约 §2.2「BOSS 条随阶段更新文字/颜色」：文字/刻度上面已经有了，这里补颜色。
        bar.setColor(switch (phase) {
            case 3 -> BossEvent.BossBarColor.RED;
            case 2 -> BossEvent.BossBarColor.PURPLE;
            default -> bossBarColor(this.rank());
        });

        if (--this.bossBarCooldown > 0) {
            return;
        }
        this.bossBarCooldown = BOSS_BAR_INTERVAL;

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double rangeSq = BOSS_BAR_RANGE * BOSS_BAR_RANGE;
        List<ServerPlayer> near = serverLevel.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && p.distanceToSqr(this) <= rangeSq);
        Set<ServerPlayer> keep = new HashSet<>(near);
        for (ServerPlayer p : List.copyOf(bar.getPlayers())) {
            if (!keep.contains(p)) {
                bar.removePlayer(p);
            }
        }
        for (ServerPlayer p : near) {
            bar.addPlayer(p);
        }
    }

    /** 死亡 / 卸载 / 被清除：先把血条收干净，再走正常的移除流程。 */
    @Override
    public void remove(Entity.RemovalReason reason) {
        // 清掉心智状态：实体对象可能被复用，带着旧"心情"复活会很怪
        try {
            MobMind.forget(this);
        } catch (Throwable ignored) {
            // 清状态失败不影响移除
        }
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
            this.bossEvent.setVisible(false);
            this.bossEvent = null;
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------------
    // 品阶光晕粒子（纯客户端）
    // ------------------------------------------------------------------

    /**
     * 品阶越高，身边越"灵气逼人"。
     *
     * <p>粒子<b>只在客户端生成</b>（{@code level.addParticle} 在服务端会被忽略，
     * 但那意味着每 tick 一次无用的调用，所以这里显式判客户端）——
     * 这样不需要任何同步：每台客户端自己画自己看到的。</p>
     *
     * <p>颜色是 {@code addParticle} 做不到的（原版粒子类型自带固定颜色），
     * 所以<b>品阶色由足下光环 / 光晕（渲染层，颜色可控）承担</b>，
     * 粒子只负责"密度与种类"这一维：越高的品阶粒子越多、越"重"。</p>
     */
    private void clientRankAura() {
        int level = this.rank().auraLevel();
        if (level <= 0 || this.isInvisible()) {
            return;
        }
        // 每档的周期：1 档 16 tick 一颗，4 档 5 tick 一颗（还多两种粒子）
        int period = Math.max(4, 18 - level * 3);
        if (this.tickCount % period != 0) {
            return;
        }
        double spread = Math.max(0.35D, this.getBbWidth());
        double x = this.getX() + (this.random.nextDouble() - 0.5D) * spread * 2.0D;
        double y = this.getY() + this.random.nextDouble() * this.getBbHeight();
        double z = this.getZ() + (this.random.nextDouble() - 0.5D) * spread * 2.0D;
        this.level().addParticle(ParticleTypes.ENCHANT, x, y, z, 0.0D, 0.02D, 0.0D);
        if (level >= 2) {
            this.level().addParticle(ParticleTypes.END_ROD, x, this.getY() + 0.05D, z,
                    0.0D, 0.01D, 0.0D);
        }
        if (level >= 3) {
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z,
                    0.0D, 0.01D, 0.0D);
        }
        if (level >= 4) {
            this.level().addParticle(ParticleTypes.DRAGON_BREATH, x, y, z,
                    0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void tick() {
        com.hongmeng.xiuxian.entity.BossPhase.tick(this);
        super.tick();
        if (this.level().isClientSide()) {
            this.clientRankAura();
        }
    }

    // ------------------------------------------------------------------
    // 掉落
    // ------------------------------------------------------------------

    /** 基础经验（未乘品阶）。 */
    public int baseExperience() {
        return this.baseExperience;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        // 品阶越高的怪，怀里越"富"
        MobRank r = rank();
        int rolls = 1 + r.ordinal() / 2;
        for (int i = 0; i < rolls; i++) {
            if (this.random.nextInt(100) >= 45) {
                continue;
            }
            ItemStack drop = rankDrop(r);
            if (!drop.isEmpty()) {
                this.spawnAtLocation(drop);
            }
        }
        // 队长及以上额外掉一块空冥石——本模组最硬的通货
        if (r.isRare() && this.random.nextInt(100) < 5 * r.ordinal()) {
            this.spawnAtLocation(new ItemStack(
                    com.hongmeng.xiuxian.item.ModItems.KONG_MING_SHI.get(), 1));
        }
    }

    /** 单个掉落物；默认给灵叶与回灵丹，子类可覆盖。 */
    protected ItemStack rankDrop(MobRank rank) {
        if (rank.ordinal() >= MobRank.CAPTAIN.ordinal()) {
            return new ItemStack(com.hongmeng.xiuxian.item.ModItems.PILL_HUILING.get(), 1);
        }
        return new ItemStack(com.hongmeng.xiuxian.item.ModItems.LINGYE.get(), 1);
    }

    // ------------------------------------------------------------------
    // 其它
    // ------------------------------------------------------------------

    /** Boss 阶段特效用的元素（取灵根元素，没有就用无属性）。 */
    public com.hongmeng.xiuxian.spirit.Element elementForPhaseFx() {
        try {
            return com.hongmeng.xiuxian.spirit.Element.NONE;
        } catch (Throwable ignored) {
            return com.hongmeng.xiuxian.spirit.Element.NONE;
        }
    }

    /** 妖物皆属阴，视作亡灵：圣光类附魔有效，治疗／伤害药水效果反转。 */
    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    /** 自定义怪都是"精英"，不会因为玩家走远就刷掉。 */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** 最近的玩家（用于境界缩放），没有则返回 null。 */
    protected static ServerPlayer nearestPlayer(Level level, CultivationMonsterEntity mob) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getNearestPlayer(mob, 64.0D) instanceof ServerPlayer sp ? sp : null;
    }
}
