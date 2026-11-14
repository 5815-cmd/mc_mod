package com.hongmeng.xiuxian.difficulty;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 怪物强度缩放。
 *
 * <p>两重驱动：</p>
 * <ol>
 *   <li><b>开局难度</b>：普通 ×1.00 到鸿蒙 ×3.00（生命），攻击略低一档；</li>
 *   <li><b>玩家境界</b>：玩家越强，新刷出的怪也越强——即"怪物也会质变"。
 *       否则道印之后玩家真的一路割草到鸿蒙，毫无压力。</li>
 * </ol>
 *
 * <p>已被命名（比如活动 BOSS、商队）或属于原版 BOSS 的实体不做缩放，
 * 免得把特定玩法打乱。</p>
 */
public final class MobScaling {

    private static final UUID HEALTH_ID =
            UUID.nameUUIDFromBytes("hongmeng_mob_health".getBytes(StandardCharsets.UTF_8));
    private static final UUID DAMAGE_ID =
            UUID.nameUUIDFromBytes("hongmeng_mob_damage".getBytes(StandardCharsets.UTF_8));

    /** 标记：这只怪的强度已经算过了。 */
    private static final String SCALED_TAG = "hongmeng_scaled";

    private MobScaling() {
    }

    /**
     * 新实体加入世界时调用。
     *
     * @param level  所在世界
     * @param mob    实体（非敌对生物会被忽略）
     * @param player 用于取境界的参照玩家（可为 null）
     */
    public static void onEntityJoin(ServerLevel level, Mob mob, ServerPlayer player) {
        if (!(mob instanceof Monster)) {
            return;
        }
        if (mob instanceof EnderDragon || mob instanceof WitherBoss || mob instanceof Warden) {
            return;
        }
        CompoundTag pd = mob.getPersistentData();
        if (pd.getBoolean(SCALED_TAG)) {
            return;
        }
        pd.putBoolean(SCALED_TAG, true);

        // 修仙界自定义怪自己算数值：它们的属性里已经含了品阶倍率，
        // 再走一遍"开局难度"会跟品阶叠乘，变成一头血牛。
        if (mob instanceof com.hongmeng.xiuxian.entity.CultivationMonsterEntity cm) {
            // ---- 走"修仙界怪"自己的两条加成，然后才 return ----
            //
            // ⚠ 这里曾经是个**死代码 bug**：原来直接 `return;`，
            // 于是下面两条加强永远执行不到 ——
            // 玩家要求的"小怪按灵根分强弱"其实一直没生效。
            //
            // ① 灵根品质加权（混沌 60% / 鸿蒙 30% / 先天 10%）
            applyMonsterSpiritRoot(cm);
            // ② BOSS / 首领：按"它属于哪个维度"给一层与玩家境界无关的固定加强
            //    （玩家要求：不同维度的 boss 要一样强）
            BossScaling.apply(cm, dimensionTierOf(level, mob));
            return;
        }

        CultivationDifficulty diff = player == null
                ? CultivationDifficulty.NORMAL : player.getCapability(
                com.hongmeng.xiuxian.core.CultivationCapability.CAP)
                .map(d -> d.difficulty())
                .orElse(CultivationDifficulty.NORMAL);

        int realmTier = player == null ? 1
                : com.hongmeng.xiuxian.core.CultivationCapability.get(player).realm().tier();

        double healthMul = diff.mobHealthMultiplier() * realmFactor(realmTier);
        double damageMul = diff.mobDamageMultiplier() * realmAttackFactor(realmTier);
        double strength = HongmengConfig.SERVER.mobScalingStrength.get();
        // 把倍率按 strength 往 1.0 收敛
        healthMul = 1.0D + (healthMul - 1.0D) * strength;
        damageMul = 1.0D + (damageMul - 1.0D) * strength;

        apply(mob, Attributes.MAX_HEALTH, HEALTH_ID, "hongmeng_mob_health", healthMul);
        apply(mob, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "hongmeng_mob_damage", damageMul);
        mob.setHealth(mob.getMaxHealth());

        // 高阶怪物给个名号，让玩家一眼看出"这只不一样"
        if (healthMul >= 4.0D && !mob.hasCustomName()) {
            mob.setCustomName(Component.literal("\u00a7c凶戾 · "
                    + mob.getType().getDescription().getString()));
            mob.setCustomNameVisible(true);
        }
    }

    /**
     * 给"首领级"怪物挂上<b>鸿蒙灵根</b>（玩家要求：BOSS 一定是鸿蒙灵根）。
     *
     * <p>在已有的境界/品阶倍率之上再乘 {@link #BOSS_HONGMENG_ROOT_BONUS} 一档，
     * 并把名字标成「鸿蒙 · …」，让它一眼就是同境里最硬的那只。</p>
     *
     * <p>只对品阶为 BOSS 的怪生效；普通怪调用它是空操作。</p>
     *
     * <h2>阶梯 Boss 的分支（放在最前面）</h2>
     * <p>七首领 + 恶这 8 只（{@link BossLadder}）不走"再乘 ×1.6"的老路：
     * 它们的固定层数值由阶梯表给定，这里只调 {@link BossScaling#applyLadder} 把属性
     * 精确归一化到目标值（并把灵根随机 / 旧 ×1.6 折算掉）。
     * 为什么必须在这里做而不在 {@link #onEntityJoin}：{@code onEntityJoin} 之后
     * 才轮到这个方法，而旧的 ×1.6 是乘在 {@code MULTIPLY_TOTAL} 上的 ——
     * 晚一步归一化，目标值才不会被 ×1.6 再顶一次。</p>
     *
     * <p>非阶梯的 BOSS 品阶散怪（魔道统领 / 虚空霸主 / 末地虚空巨蟒…）保持原逻辑不变。</p>
     */
    public static void applyHongmengRootIfBoss(Mob mob,
                                               com.hongmeng.xiuxian.entity.MobRank rank) {
        if (mob == null) {
            return;
        }
        // ---- ① 阶梯 Boss：按 BossLadder 目标值归一化（唯一入口；含老存档迁移） ----
        if (mob instanceof com.hongmeng.xiuxian.entity.CultivationMonsterEntity cm) {
            String id = BossScaling.entityId(mob);
            BossLadder.Step step = BossLadder.byId(id);
            if (step != null) {
                if (BossScaling.applyLadder(cm)) {
                    // 名牌与旧行为保持一致；getName() 被基类覆盖，这里主要是 NBT 层标记
                    try {
                        String base = mob.getType().getDescription().getString();
                        mob.setCustomName(Component.literal(
                                "\u00a75\u00a7l鸿蒙\u00a7r \u00a7c" + base));
                        mob.setCustomNameVisible(true);
                    } catch (Throwable ignored) {
                        // 名牌失败不影响数值
                    }
                }
                return;
            }
        }
        // ---- ② 非阶梯 Boss：老逻辑（只有 BOSS 品阶吃 ×1.6，首领不吃） ----
        if (rank != com.hongmeng.xiuxian.entity.MobRank.BOSS) {
            return;
        }
        try {
            AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
            AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            // ⚠ `addPermanentModifier` 在**同 UUID 修饰符已存在时会抛**
            //   IllegalArgumentException("Modifier is already applied on this attribute!")。
            //   而"鸿蒙灵根标记"对同一只怪会被调用不止一次（重复生成、区块重载、
            //   难度重算都会走到这里），所以必须**先摘同名的再挂** ——
            //   项目里 GearManager / DomainDamage 早就是这个写法，这里当初漏了。
            //   症状：服务端日志刷 `[hongmeng] 鸿蒙灵根标记失败：…Modifier is already applied…`
            //   （实测一次起服 10 条），且怪物的鸿蒙加成**根本没能挂上**。
            applyRootModifier(hp, ROOT_HEALTH_ID, "hongmeng_root_health");
            applyRootModifier(atk, ROOT_DAMAGE_ID, "hongmeng_root_damage");
            mob.setHealth(mob.getMaxHealth());
            String base = mob.getType().getDescription().getString();
            mob.setCustomName(Component.literal("\u00a75\u00a7l鸿蒙\u00a7r \u00a7c" + base));
            mob.setCustomNameVisible(true);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 鸿蒙灵根标记失败：{}", t.toString());
        }
    }

    /**
     * 幂等地挂一条鸿蒙灵根修饰符：**先摘同 UUID 的旧修饰符，再挂新的**。
     *
     * <p>直接调 {@code addPermanentModifier} 会在重复调用时抛
     * {@code IllegalArgumentException}，而本方法可以被安全地反复调用。</p>
     */
    public static void applyRootModifier(AttributeInstance inst, UUID id, String name) {
        applyRootModifier(inst, id, name, BOSS_HONGMENG_ROOT_BONUS - 1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /**
     * 幂等地挂一条<b>自定义值</b>的修饰符：先摘同 UUID 的旧修饰符，再挂新的。
     *
     * <p>为什么要复用这个方法而不是各处自己写：重点是<b>幂等</b>。
     * 直接 {@code addPermanentModifier} 在重复调用时会抛
     * {@code IllegalArgumentException: Modifier is already applied on this attribute!}，
     * 而这类异常通常被 try/catch 吃掉 —— 结果是<b>buff 静默失效</b>，
     * 表面没报错但玩家拿不到效果（这个坑我踩过一次）。</p>
     *
     * <p>阵修的"阵域减速"用它：减速值随半径动态变化，而且每 tick 都要重挂。</p>
     */
    public static void applyRootModifier(AttributeInstance inst, UUID id, String name,
                                         double amount, AttributeModifier.Operation op) {
        if (inst == null) {
            return;
        }
        // 先摘掉同 UUID 的（可能来自上一次调用）
        if (inst.getModifier(id) != null) {
            inst.removeModifier(id);
        }
        inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
    }

    /**
     * 鸿蒙灵根用的固定修饰符 id（重复调用不会叠加）。
     *
     * <p>包内可见（而不是 private）：{@link BossScaling#applyLadder} 归一化阶梯 Boss 时
     * 要把这一层灵根随机/旧 1.6 加成摘掉，否则相邻 1.30 的台阶会被 ±50% 的随机抹平。</p>
     */
    static final UUID ROOT_HEALTH_ID =
            UUID.nameUUIDFromBytes("hongmeng_root_health".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    static final UUID ROOT_DAMAGE_ID =
            UUID.nameUUIDFromBytes("hongmeng_root_damage".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    /**
     * 玩家境界带来的怪物**生命**强化系数（攻击倍率见 {@link #realmAttackFactor(int)}）。
     *
     * <h2>为什么把曲线拉到这么大</h2>
     *
     * <p>玩家反馈「突破鸿蒙之后杀什么怪都是一下，太超标了」。原因是旧曲线
     * 到第 10 境只有 <b>×6.5</b>，而玩家在鸿蒙的攻击力是 <b>86 万</b>量级 ——
     * 一只基础 20 血的僵尸就算乘 6.5 也只有 130 血，当然一下就没。</p>
     *
     * <h2>目标：同境约 5.5 : 4.5，配装后 7 : 3</h2>
     *
     * <p>所以这次的系数是**按玩家的属性成长反推**的，量级必须跟得上：
     * 第 10 境给到 <b>×520</b>。这样同境界的普通怪能扛住几下、
     * 头目/首领才有"打得有来有回"的压迫感；配上一身道袍与被动之后
     * 玩家自然落到 7 : 3 的优势区间。</p>
     *
     * <p><b>强弱分层不受影响</b>：品阶倍率（{@code MobRank}）是在本系数
     * <b>之上</b>再乘的，所以"同境界里普通怪 &lt; 精英 &lt; 队长 &lt; 头目 &lt; 首领"
     * 的梯度完全保留（玩家要的"同境界也分强弱"本来就有）。</p>
     *
     * <table border="1">
     *   <caption>境界倍率表</caption>
     *   <tr><th>大境</th><th>倍率</th></tr>
     *   <tr><td>1 练气</td><td>1.00</td></tr>
     *   <tr><td>2 筑基</td><td>1.35</td></tr>
     *   <tr><td>3 金丹</td><td>2.20</td></tr>
     *   <tr><td>4 元婴</td><td>3.30</td></tr>
     *   <tr><td>5 化神</td><td>5.50</td></tr>
     *   <tr><td>6 道印</td><td>13</td></tr>
     *   <tr><td>7 飞升</td><td>34</td></tr>
     *   <tr><td>8 仙人</td><td>85</td></tr>
     *   <tr><td>9 帝境</td><td>210</td></tr>
     *   <tr><td>10 鸿蒙</td><td><b>520</b></td></tr>
     * </table>
     */

    // ------------------------------------------------------------------
    // 修仙界怪的两条固定加成
    // ------------------------------------------------------------------

    /** "灵根已经掷过"的标记。 */
    private static final String ROOT_TAG = "hongmeng_monster_root";

    /**
     * 给修仙界怪掷一个**灵根品质**并按它加权强度。
     *
     * <p>玩家要求：「每个境界的小怪 100 个里面都有六成混沌灵根、三成鸿蒙灵根、
     * 一成先天灵根的同境界怪物」。掷点 → 记在实体上（名牌会用它上色）→
     * 在既有的境界/品阶倍率之外再乘一层灵根倍率
     * （倍率对怪物做过压缩，见 {@link MonsterSpiritRoot#STRENGTH}）。</p>
     */
    public static void applyMonsterSpiritRoot(
            com.hongmeng.xiuxian.entity.CultivationMonsterEntity mob) {
        try {
            if (mob.getPersistentData().getBoolean(ROOT_TAG)) {
                return;      // 同一只只掷一次
            }
            mob.getPersistentData().putBoolean(ROOT_TAG, true);
            // MonsterSpiritRoot.roll 收的是 java.util.Random；
            // 实体给的是 RandomSource，这里用一个一次性 Random（只用一次，够随机）。
            com.hongmeng.xiuxian.spirit.Rarity rarity =
                    MonsterSpiritRoot.roll(new java.util.Random(mob.getRandom().nextLong()));
            mob.setSpiritRootRarity(rarity);
            double mul = MonsterSpiritRoot.multiplierOf(rarity);
            if (mul > 1.0D) {
                apply(mob, Attributes.MAX_HEALTH, ROOT_HEALTH_ID,
                        "hongmeng_monster_root_health", mul);
                apply(mob, Attributes.ATTACK_DAMAGE, ROOT_DAMAGE_ID,
                        "hongmeng_monster_root_damage", mul);
                mob.setHealth(mob.getMaxHealth());
            }
            // 名牌标出品阶颜色（鸿蒙 / 混沌 / 先天一眼可辨）
            String base = mob.getType().getDescription().getString();
            mob.setCustomName(net.minecraft.network.chat.Component.literal(
                    MonsterSpiritRoot.labelOf(rarity) + "\u00a7c" + base));
            mob.setCustomNameVisible(false);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 怪物灵根加权失败：{}", t.toString());
        }
    }

    /**
     * 这只 boss 属于哪个维度档位（决定它的固定加强倍率）。
     *
     * <p>路由表已经搬到 {@link BossLadder#tierOfDimension(String)}（纯数据、可离线校对），
     * 这里只做「取不到维度 → 主世界」的兜底。改动前只有 void/demon/epoch|beast 三档，
     * 下界、末地、天外天全部落到 1.0 —— 正是「仙庭之主比虚空道主还弱」的根因之一；
     * 现在补了：下界 5.0、末地 6.0、天外天 8.0。</p>
     */
    public static double dimensionTierOf(net.minecraft.world.level.Level level, Mob mob) {
        try {
            return BossLadder.tierOfDimension(level.dimension().location().getPath());
        } catch (Throwable ignored) {
            // 取不到维度就当主世界
            return BossScaling.TIER_OVERWORLD;
        }
    }

    public static double realmFactor(int realmTier) {
        return switch (Math.max(1, Math.min(10, realmTier))) {
            case 1 -> 1.00D;
            case 2 -> 1.35D;
            case 3 -> 2.20D;
            case 4 -> 3.30D;
            case 5 -> 5.50D;
            case 6 -> 13.0D;
            case 7 -> 34.0D;
            case 8 -> 85.0D;
            case 9 -> 210.0D;
            default -> 520.0D;
        };
    }

    /**
     * 玩家境界带来的<b>攻击</b>强化系数（与 {@link #realmFactor(int)} 分开）。
     *
     * <h2>为什么要拆成两张表</h2>
     *
     * <p>生命表要照顾「怪有多肉」：它跟标准值（每境 ×3）同尺，终局 ×520。
     * 但<b>攻击</b>要照顾的是「怪几下打死玩家」，玩家生命在第 8→9→10 境是
     * ×10、×10 地跳（7,700 → 76,820 → 768,020），而旧表只给 ×210/×520，
     * 于是 帝境/鸿蒙 的 Boss 每击只掉玩家 1%~3% 血 —— 数值文档里
     * 「恶要砍 114 下」就是这么来的。</p>
     *
     * <p>本表前 7 境与 {@link #realmFactor(int)} 完全一致（前期手感不变），
     * 从仙人段开始按玩家生命曲线续接：</p>
     *
     * <pre>
     *   8 仙人   ×105      （约 ×1.24 旧值；虚空龙帝/虚空道主/仙庭之主 13.5/9.6/6.9 下）
     *   9 帝境   ×513      （约 ×2.44 旧值；恶一记约 10% 玩家血）
     *   10 鸿蒙  ×5,129    （约 ×9.86 旧值；恶一记约 10% 玩家血）
     * </pre>
     *
     * <p>8 境为什么也要动：玩家生命 飞升(788) → 仙人(7,700) 是 ×9.8，而旧系数只给
     * ×2.5，导致「仙人 Boss 比飞升 Boss 还软」。×105 取的是「虚空龙帝 13.5 下、
     * 仙庭之主 6.9 下」都落在 5~14 下区间的折中值（同境三只 Boss 的攻击梯度由
     * 阶梯 ATK 68.2/95.5/133.7 自然给出）。</p>
     *
     * <p>推导（同一只 Boss 在 9/10 境威胁一致）：</p>
     * <pre>
     *   期望「10 下打死玩家」→ 需要每击护甲后伤害 = 玩家最大生命 / 10；
     *   减伤 92% → 护甲前伤害 = 生命 × 1.25；
     *   恶的固定层攻击 = 187.2 → 系数 = 玩家生命 × 1.25 / 187.2：
     *     第 9 境 76,852 × 1.25 / 187.2 ≈ 513
     *     第 10 境 768,052 × 1.25 / 187.2 ≈ 5,129
     * </pre>
     *
     * <p>离线校验：{@code tools/check_balance.py}。</p>
     */
    public static double realmAttackFactor(int realmTier) {
        return switch (Math.max(1, Math.min(10, realmTier))) {
            case 1 -> 1.00D;
            case 2 -> 1.35D;
            case 3 -> 2.20D;
            case 4 -> 3.30D;
            case 5 -> 5.50D;
            case 6 -> 13.0D;
            case 7 -> 34.0D;
            case 8 -> 105.0D;
            case 9 -> 513.0D;
            default -> 5129.0D;
        };
    }

    /**
     * BOSS 额外的「鸿蒙灵根」加成（玩家要求：BOSS 一定是鸿蒙灵根）。
     *
     * <p>在境界倍率与品阶倍率之上再乘一档，让首领始终是"同境里最硬的那只"，
     * 并且在名字上标出来。</p>
     */
    // 原来 6.0：boss 血量是同境界的 6 倍 —— 玩家看到化神却打成鸿蒙。
    // 降到 1.6：仍是强化，但标签与手感一致。
    public static final double BOSS_HONGMENG_ROOT_BONUS = 1.6D;

    private static void apply(Mob mob, net.minecraft.world.entity.ai.attributes.Attribute attr,
                              UUID id, String name, double multiplier) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (Math.abs(multiplier - 1.0D) < 1e-6) {
            return;
        }
        inst.addPermanentModifier(new AttributeModifier(
                id, name, multiplier - 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    /**
     * 一次把生命与攻击按倍率写进属性（永久修饰符，可重复调用）。
     *
     * <p>给自定义怪（{@code CultivationMonsterEntity}）复用：它们的基础属性里
     * 已经含了品阶倍率，这里只补"境界"这一层。</p>
     *
     * @param mob          目标生物
     * @param healthMul    生命总倍率
     * @param damageMul    攻击总倍率
     * @param modifierName 修饰符名（同一个怪上的多次调用会互相覆盖）
     */
    public static void applyScaling(Mob mob, double healthMul, double damageMul,
                                    String modifierName) {
        UUID healthId = UUID.nameUUIDFromBytes(
                (modifierName + "_health").getBytes(StandardCharsets.UTF_8));
        UUID damageId = UUID.nameUUIDFromBytes(
                (modifierName + "_damage").getBytes(StandardCharsets.UTF_8));
        apply(mob, Attributes.MAX_HEALTH, healthId, modifierName + "_health", healthMul);
        apply(mob, Attributes.ATTACK_DAMAGE, damageId, modifierName + "_damage", damageMul);
    }

    /**
     * 玩家境界给自定义怪带来的**生命**强化系数。
     *
     * <p>自己找最近的玩家取境界——自定义怪的属性是在实体类型注册时算好的，
     * 那时根本没有玩家，只能等到进世界再补这一层。</p>
     *
     * @param level 所在世界（客户端或找不到玩家时为 ×1.00）
     * @param mob   参照中心（通常传怪自己）
     * @return 境界系数，范围为 {@code realmFactor}(1..10)，即 1.00 ~ 520
     */
    public static double realmScaleFor(net.minecraft.world.level.Level level,
                                       net.minecraft.world.entity.Entity mob) {
        return realmScaleFor(level, mob, false);
    }

    /**
     * 玩家境界给自定义怪带来的**攻击**强化系数。
     *
     * <p>见 {@link #realmAttackFactor(int)}：帝境/鸿蒙按玩家生命曲线续接，
     * 前 8 境与生命系数一致。</p>
     */
    public static double realmScaleForAttack(net.minecraft.world.level.Level level,
                                             net.minecraft.world.entity.Entity mob) {
        return realmScaleFor(level, mob, true);
    }

    /**
     * 最近玩家的境界（1~10）；找不到玩家 / 不在服务端时返回 <b>0</b>（调用方按"不缩放"处理）。
     *
     * <p>2026-11-07 新增：副本内的怪要按「副本基准境界」封顶，需要先知道玩家境界，
     * 再取 {@code min(玩家境界, 副本挑战境界)}。</p>
     */
    public static int nearestPlayerTier(net.minecraft.world.level.Level level,
                                        net.minecraft.world.entity.Entity mob) {
        if (!(level instanceof ServerLevel serverLevel) || mob == null) {
            return 0;
        }
        ServerPlayer player = serverLevel.getNearestPlayer(mob, 64.0D) instanceof ServerPlayer sp
                ? sp : null;
        if (player == null) {
            return 0;
        }
        return com.hongmeng.xiuxian.core.CultivationCapability.get(player).realm().tier();
    }

    /**
     * 按**指定境界**取生命强化系数（不找玩家）。
     *
     * <p>用途：副本怪的境界缩放要用「副本挑战境界」而不是最近玩家境界——
     * 否则鸿蒙玩家进元婴副本，元婴头目会被 ×520 生命撑到 1.5 亿。</p>
     */
    public static double realmScaleForTier(int tier) {
        return scaleForFactor(realmFactor(tier));
    }

    /** 按**指定境界**取攻击强化系数（不找玩家）。 */
    public static double realmAttackScaleForTier(int tier) {
        return scaleForFactor(realmAttackFactor(tier));
    }

    private static double scaleForFactor(double factor) {
        return 1.0D + (factor - 1.0D)
                * HongmengConfig.SERVER.mobScalingStrength.get();
    }

    private static double realmScaleFor(net.minecraft.world.level.Level level,
                                        net.minecraft.world.entity.Entity mob,
                                        boolean attack) {
        int tier = nearestPlayerTier(level, mob);
        if (tier <= 0) {
            return 1.0D;
        }
        return scaleForFactor(attack ? realmAttackFactor(tier) : realmFactor(tier));
    }

    /** 面板/HUD 里显示一句当前难度下的怪物强度。 */
    public static String describe(CultivationDifficulty diff, int realmTier) {
        double hp = diff.mobHealthMultiplier() * realmFactor(realmTier);
        double dmg = diff.mobDamageMultiplier() * realmAttackFactor(realmTier);
        return String.format("生命 ×%.2f  攻击 ×%.2f", hp, dmg);
    }
}
