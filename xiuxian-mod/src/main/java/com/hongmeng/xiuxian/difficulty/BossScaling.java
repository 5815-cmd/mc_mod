package com.hongmeng.xiuxian.difficulty;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * <b>Boss 的统一加强</b>（玩家要求：「原本的 boss 也要有加强，和副本 boss 一样强，
 * 毕竟都是不同维度的 boss」）。
 *
 * <h2>为什么单独一个类</h2>
 * <p>{@link MobScaling} 是按<b>玩家境界</b>缩放的，所以"同一个 boss 在低境界玩家眼里很弱"。
 * 但 boss 的<b>身份</b>不该随玩家变 —— 它属于哪个维度、什么品阶，就该有多强。
 * 这里按「维度档位 × 品阶」给一层固定加成，与玩家境界无关。</p>
 *
 * <h2>两套口径（重要）</h2>
 * <ol>
 *   <li><b>阶梯 Boss（8 只，见 {@link BossLadder}）</b>：固定层数值<b>精确等于</b>
 *       {@link BossLadder.Step#targetHp()} / {@link BossLadder.Step#targetAtk()}。
 *       做法是 {@link #applyLadder} 把「目标值 ÷ (createAttributes 基础值)」换算成一条
 *       {@code MULTIPLY_BASE} 修饰符 —— 因为 {@code scaledAttributes()} 已经把品阶倍率
 *       乘进基础值了，所以这条修饰符正好把它顶到目标值。这两只（/只）Boss 的旧维度倍率、
 *       灵根随机、旧的「鸿蒙灵根 ×1.6」都会被<b>同一次</b>折算清掉（见 {@link #applyLadder}），
 *       否则 ±50% 的灵根随机会把 1.30 的台阶打得看不出阶梯。</li>
 *   <li><b>其它 BOSS / 首领品阶散怪</b>（魔道统领、虚空霸主、虚空行者、末地虚空巨蟒…）：
 *       完全沿用老口径 {@code multiplierOf(rank, dimension)}（维度档位 × 首领 1.6），
 *       与改动前一致，只是维度路由表补了三个维度（下界 / 末地 / 天外天），见
 *       {@link BossLadder#tierOfDimension}。</li>
 * </ol>
 *
 * <h2>档位</h2>
 * <pre>
 *   主世界 / 真世界 boss          1.0
 *   纪元 / 妖兽 维度 boss          2.2
 *   魔道巢穴 boss                 4.0
 *   下界世界 boss（新增）          5.0
 *   末地世界 boss（新增）          6.0
 *   虚空裂隙 boss                 6.5
 *   天外天 boss（新增）            8.0
 * </pre>
 *
 * <p>再乘品阶：{@code 头目 ×1.0}、{@code 首领 ×1.6}。</p>
 *
 * <h2>老存档兼容</h2>
 * <p>改动前已经刷出来的 Boss 身上带着 {@code hongmeng_boss_scaled} 标记和旧修饰符。
 * 这里的做法是：阶梯 Boss 用<b>独立</b>版本标记 {@value #LADDER_VERSION_TAG} 判断是否折算过；
 * 第一次读到旧 Boss 时，先把旧维度倍率 / 灵根 / 阶段 / 玩家境界那几层修饰符摘掉，
 * 再把目标值写死一次，并清掉 {@code hongmeng_realm_scaled} 让 {@code onAddedToWorld}
 * 用<b>当前</b>玩家境界重算 —— 于是老 Boss 不需要等它重新刷就能跟上新阶梯。
 * 旧标记 {@code hongmeng_boss_scaled} 保持原语义，只对非阶梯 Boss 生效，不会重复叠乘。</p>
 */
public final class BossScaling {

    /** 主世界。 */
    public static final double TIER_OVERWORLD = BossLadder.TIER_OVERWORLD;
    /** 纪元秘境 / 妖兽秘境。 */
    public static final double TIER_LOW = BossLadder.TIER_LOW;
    /** 魔道巢穴。 */
    public static final double TIER_MID = BossLadder.TIER_MID;
    /** 下界（世界 Boss）。 */
    public static final double TIER_NETHER = BossLadder.TIER_NETHER;
    /** 末地（世界 Boss）。 */
    public static final double TIER_END = BossLadder.TIER_END;
    /** 虚空裂隙。 */
    public static final double TIER_HIGH = BossLadder.TIER_HIGH;
    /** 天外天。 */
    public static final double TIER_HEAVEN = BossLadder.TIER_HEAVEN;

    /** 旧口径的「这次加成已经算过」标记（非阶梯 Boss 用；老存档兼容，别改字符串）。 */
    private static final String LEGACY_SCALED_TAG = "hongmeng_boss_scaled";

    /**
     * 阶梯归一化的版本标记（值 = {@link BossLadder#VERSION}）。
     *
     * <p>只对 8 只阶梯 Boss 生效。读到的值等于当前版本 = 已经折算过、直接跳过；
     * 缺失/旧版本 = 按新表重算一次。这样"同一只只折算一次"并且改表后老 Boss 能迁移。</p>
     */
    static final String LADDER_VERSION_TAG = "hongmeng_boss_ladder";

    /** 阶梯归一化写在生命上的修饰符名（固定 UUID，重复调用只覆盖不叠加）。 */
    private static final String LADDER_HP = "hongmeng_ladder_hp";
    /** 阶梯归一化写在攻击上的修饰符名。 */
    private static final String LADDER_ATK = "hongmeng_ladder_atk";

    /** 旧的维度倍率修饰符名（老存档里可能存在，迁移时摘掉）。 */
    private static final String LEGACY_HP = "hongmeng_boss_hp";
    private static final String LEGACY_ATK = "hongmeng_boss_atk";

    /**
     * 老存档迁移时要摘掉的「玩家境界缩放」修饰符。
     *
     * <p>名字来自 {@link MobScaling#applyScaling} 的 {@code modifierName + "_health"/"_damage"}；
     * 这里用同一套 UUID 公式（{@code UUID.nameUUIDFromBytes(name)}）反查，避免跨类访问私有方法。
     * 摘掉后再清 {@code hongmeng_realm_scaled}，{@code onAddedToWorld} 会用当前玩家境界重算一次。</p>
     */
    private static final String REALM_SCALED_HP = "hongmeng_cultivator_rank_health";
    private static final String REALM_SCALED_ATK = "hongmeng_cultivator_rank_damage";
    /** 老存档迁移时要摘掉的「本境标准下限」修饰符（{@code applyStandardStrengthFloor} 用的名字）。 */
    private static final String REALM_FLOOR_HP = "hongmeng_realm_floor_health";
    private static final String REALM_FLOOR_ATK = "hongmeng_realm_floor_damage";
    /** 老存档迁移时要摘掉的「Boss 三阶段」修饰符（{@code BossPhase} 用的名字）。 */
    private static final String PHASE_HP = "hm_phase_hp";
    private static final String PHASE_ATK = "hm_phase_atk";
    /** 老存档迁移时要摘掉的「三阶段移速」修饰符。 */
    private static final String PHASE_SPD = "hm_phase_spd";
    /** 老存档迁移时要摘掉的「狂暴」攻击修饰符（{@code BossAttackKit} 用的名字）。 */
    private static final String ENRAGE_ATK = "hm_boss_enrage_atk";

    private BossScaling() {
    }

    /**
     * boss 的强度倍率（与玩家境界无关的固定加成）。
     *
     * <p><b>只用于非阶梯 Boss 与「自然值」推导</b>；8 只阶梯 Boss 走
     * {@link BossLadder} 的目标值，不再乘这里。</p>
     *
     * @param rank      品阶
     * @param dimension 维度档位（见 {@link BossLadder}）
     */
    public static double multiplierOf(MobRank rank, double dimension) {
        if (rank != MobRank.BOSS && rank != MobRank.OVERLORD) {
            return 1.0D;
        }
        double rankMul = rank == MobRank.OVERLORD ? BossLadder.OVERLORD_RANK_SCALE : 1.0D;
        return Math.max(1.0D, dimension) * rankMul;
    }

    /**
     * 给 boss 施加固定的加强（生命 + 攻击）。
     *
     * <p>幂等：靠 persistentData 的 {@value #LEGACY_SCALED_TAG} 标记，同一只只加一次。</p>
     *
     * <p><b>阶梯 Boss 这里直接返回</b>：它们的数值由
     * {@link MobScaling#applyHongmengRootIfBoss} → {@link #applyLadder} 统一写死。
     * 时序上必须在「鸿蒙灵根」之后，否则旧的 ×1.6 会叠在目标值上面。</p>
     */
    public static void apply(LivingEntity boss, double dimension) {
        try {
            MobRank rank = CultivationMonsterEntity.rankOf(boss);
            // 阶梯 Boss：交给 applyLadder（在 applyHongmengRootIfBoss 里、鸿蒙灵根之后调用）
            if (BossLadder.isLadderBoss(entityId(boss))) {
                return;
            }
            var pd = boss.getPersistentData();
            if (pd.getBoolean(LEGACY_SCALED_TAG)) {
                return;
            }
            pd.putBoolean(LEGACY_SCALED_TAG, true);
            double mul = multiplierOf(rank, dimension);
            if (mul <= 1.0D) {
                return;
            }
            multiply(boss, Attributes.MAX_HEALTH, LEGACY_HP, mul);
            multiply(boss, Attributes.ATTACK_DAMAGE, LEGACY_ATK, mul);
            boss.setHealth(boss.getMaxHealth());
        } catch (Throwable ignored) {
            // 属性加强失败绝不影响游戏
        }
    }

    /**
     * 阶梯 Boss 的归一化：把生命 / 攻击<b>精确</b>写成 {@link BossLadder} 的目标值。
     *
     * <h2>为什么要单独一条路径</h2>
     * <p>原先 Boss 的有效值 = 基础 × 品阶 × (维度档位 × 品阶系数)，六只 Boss 用了六把尺子
     * （下界/末地/天外天还都落到主世界 1.0），所以「仙庭之主比虚空道主还弱」。
     * 阶梯表给出每一只的目标值后，这里只做一件事：把属性顶到目标值。</p>
     *
     * <h2>为什么要把灵根 / 旧 1.6 摘掉</h2>
     * <p>BOSS 品阶的怪进世界时会先掷一次灵根（×1.27~1.90），
     * 再被「鸿蒙灵根」加成顶成 ×1.6 —— 两次都是 {@code MULTIPLY_TOTAL}。
     * 阶梯的相邻台阶最窄只有 1.30，如果留着 ±50% 的随机，玩家看到的阶梯顺序会被随机数打乱。
     * 所以阶梯 Boss 的固定层数值以阶梯表为<b>唯一真源</b>：灵根随机/旧 1.6 的影响被折算掉
     * （只保留「鸿蒙」名牌）；<b>非阶梯</b>的 BOSS 品阶散怪一律不动。</p>
     *
     * <h2>幂等</h2>
     * <p>用 {@value #LADDER_VERSION_TAG}（值 = {@link BossLadder#VERSION}）判断：
     * 已经折算过的直接返回 {@code false}；改阶梯表 +1 版本后，老 Boss 会再折算一次。</p>
     *
     * @param boss 目标 Boss（必须已是 BOSS / 首领品阶的实例）
     * @return true 表示这次真的写了属性（false = 不是阶梯 Boss / 品阶被降级 / 已是最新版本）
     */
    public static boolean applyLadder(CultivationMonsterEntity boss) {
        try {
            if (boss == null) {
                return false;
            }
            BossLadder.Step step = BossLadder.byId(entityId(boss));
            if (step == null) {
                return false;
            }
            MobRank rank = boss.effectiveRank();
            if (rank != MobRank.BOSS && rank != MobRank.OVERLORD) {
                // 被 hm_rank_override 降级的同款实体（当杂兵用的小眼 / 槽位降级怪）不归一化
                return false;
            }
            CompoundTag pd = boss.getPersistentData();
            if (pd.getInt(LADDER_VERSION_TAG) >= BossLadder.VERSION) {
                return false;
            }
            AttributeInstance hp = boss.getAttribute(Attributes.MAX_HEALTH);
            AttributeInstance atk = boss.getAttribute(Attributes.ATTACK_DAMAGE);
            if (hp == null || atk == null) {
                return false;
            }
            // ---- 1. 先摘掉所有会污染目标值的旧加成（含老存档迁移） ----
            stripModifiers(hp, uuid(LEGACY_HP), uuid(LADDER_HP), MobScaling.ROOT_HEALTH_ID,
                    uuid(PHASE_HP), uuid(REALM_SCALED_HP), uuid(REALM_FLOOR_HP));
            stripModifiers(atk, uuid(LEGACY_ATK), uuid(LADDER_ATK), MobScaling.ROOT_DAMAGE_ID,
                    uuid(PHASE_ATK), uuid(ENRAGE_ATK), uuid(REALM_SCALED_ATK), uuid(REALM_FLOOR_ATK));
            // 阶段移速是"读档回阶段 1"的一部分：不清的话，中途存档的 Boss 会一直带着三阶段加速
            stripModifiers(boss.getAttribute(Attributes.MOVEMENT_SPEED), uuid(PHASE_SPD));
            // 老存档：Boss 的玩家境界倍率是"进世界时"算的。摘掉修饰符后还要清标记，
            // 让 onAddedToWorld 用当前玩家境界重算一次，否则目标值会被旧倍率再乘一遍。
            pd.remove("hongmeng_realm_scaled");
            pd.remove("hongmeng_realm_tier");
            pd.remove("hongmeng_boss_phase");

            // ---- 2. 写目标值（基础值 = createAttributes × 品阶倍率，已经被 scaledAttributes 乘好） ----
            applyTarget(hp, step.targetHp(), LADDER_HP);
            applyTarget(atk, step.targetAtk(), LADDER_ATK);

            // ---- 3. 标记版本 + 满血 ----
            pd.putInt(LADDER_VERSION_TAG, BossLadder.VERSION);
            boss.setHealth(boss.getMaxHealth());
            return true;
        } catch (Throwable t) {
            // 数值失败绝不影响游戏；下一次进世界会再试
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 阶梯Boss归一化失败：{}", t.toString());
            return false;
        }
    }

    /**
     * 把一条属性精确写到 {@code target}：修饰符 = {@code target / 基础值 - 1}（MULTIPLY_BASE）。
     *
     * <p>用 {@link AttributeInstance#getBaseValue()} 而不是 {@code getValue()}：
     * 基础值里已经含了品阶倍率（{@code scaledAttributes} 在注册时乘好的），
     * 而 {@code getValue()} 还掺着别的修饰符，拿它反推会很脆。</p>
     */
    private static void applyTarget(AttributeInstance inst, double target, String name) {
        double base = inst.getBaseValue();
        if (base <= 0.0D || target <= 0.0D) {
            return;
        }
        UUID id = uuid(name);
        inst.removeModifier(id);
        double amount = target / base - 1.0D;
        if (Math.abs(amount) < 1.0E-9D) {
            return;
        }
        inst.addPermanentModifier(new AttributeModifier(id, name, amount,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    /** 幂等地摘掉若干条修饰符（不存在就跳过）。 */
    private static void stripModifiers(AttributeInstance inst, UUID... ids) {
        if (inst == null) {
            return;
        }
        for (UUID id : ids) {
            if (inst.getModifier(id) != null) {
                inst.removeModifier(id);
            }
        }
    }

    /** 修饰符 UUID：与全项目其它地方一致，用名字的 UUIDv3 派生，重复调用不会叠出第二条。 */
    private static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 实体的注册名 short id（例如 {@code void_boss}）；取不到时返回空串。
     *
     * <p>阶梯表按<b>注册名</b>匹配而不是按类，和 {@code BossPhase.giantScaleFor}
     * 的做法一致：不需要引用别的 agent 的实体类，也不会把"同类的降级杂兵"误判成阶梯 Boss
     * （降级会在 {@link #applyLadder} 里被 effectiveRank 拦掉）。</p>
     */
    public static String entityId(net.minecraft.world.entity.Entity entity) {
        try {
            var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(entity.getType());
            return key == null ? "" : key.getPath();
        } catch (Throwable t) {
            return "";
        }
    }

    private static void multiply(LivingEntity e, Attribute attr, String name, double mul) {
        var inst = e.getAttribute(attr);
        if (inst == null) {
            return;
        }
        var id = uuid(name);
        // 旧维度倍率只对非阶梯 Boss 生效：同 UUID 先摘再挂，读档重算不会叠乘
        if (inst.getModifier(id) != null) {
            inst.removeModifier(id);
        }
        inst.addPermanentModifier(new AttributeModifier(
                id, name, mul - 1.0D, AttributeModifier.Operation.MULTIPLY_BASE));
    }
}
