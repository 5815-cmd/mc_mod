package com.hongmeng.xiuxian.compat;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.item.gear.ArtifactItem;
import com.hongmeng.xiuxian.item.gear.GearManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * <b>护甲减伤的唯一真源</b>（2026-11-02 数值平衡重做）。
 *
 * <h2>一句话口径</h2>
 *
 * <pre>
 *   总减伤 T(护甲值 A) = min(92%, max( 1 - 2/(2 + A/5),  1 - 20/(20 + 1.5A) ))
 *   最终伤害 = 原伤害 × (1 - T)            ← 先过甲、再封顶
 *   单次最终伤害 ≤ 目标最大生命 × 35%      ← 35% 上限改成「护甲后」结算
 * </pre>
 *
 * <h2>为什么整类重写（旧实现的三个致命问题）</h2>
 *
 * <ol>
 *   <li><b>事件在护甲之前</b>：1.20.1 的 {@code ForgeHooks.onLivingHurt} 排在
 *       {@code getDamageAfterArmorAbsorb} <b>之前</b>（已用 javap 核对）。旧代码在事件里
 *       把伤害乘 {@code (1-extra)}，随后原版护甲再减一次；两把尺子叠乘后真实减伤
 *       {@code = 1 - 18/(10+A)}，随 A 单调趋向 100%：鸿蒙 20 万护甲 → 99.991%
 *       （相当于血量 ×11378），后期玩家对物理伤害基本无敌，Boss 攻击数值毫无意义。</li>
 *   <li><b>92% 从未进过伤害路径</b>：{@code MAX_TOTAL_REDUCTION=0.92} 只被
 *       {@link #targetReduction(double)} 用于面板/文档；事件只用了
 *       {@link #vanillaReduction(double)} 的参考值。所以文档里的「92% 上限」是假的。</li>
 *   <li><b>原版公式认错</b>：代码以为原版是 {@code 1 - 2/(2+armor/5)}；实际 1.20.1 是
 *       {@code CombatRules.getDamageAfterAbsorb}：{@code f1 = clamp(armor - dmg/(2+tough/4),
 *       armor×0.2, 20)}，单次物伤封顶 <b>80%</b>，且大伤害会吃「armor×0.2 下限」。</li>
 * </ol>
 *
 * <h2>本类现在的做法</h2>
 *
 * <p><b>① 属性层封顶</b>：{@link #capVanillaArmor(ServerPlayer)} 把玩家
 * {@code Attributes.ARMOR} 压到 {@value #ARMOR_SOFT_CAP} 点——这是原版公式的安全区
 * （≤80 点时 {@code armor×0.2 ≤ 16 < 20}，不会触发原版那个「大伤害反而免伤」的负数分支）。
 * 玩家面板上的「护甲值」仍保留真实大数值（职业甲 × 境界标准值），由
 * {@link #rawArmorOf(ServerPlayer)} 通过溢出修正量还原。</p>
 *
 * <p><b>② 伤害层接管</b>：{@link #onLivingHurt} 以 {@link EventPriority#LOWEST} 收尾
 * （所有天赋/功法/职业减伤都算完之后），把目标总减伤改写为上面那条唯一曲线，
 * 并反解出「原版护甲结算之前应该填多少」，使最终落地伤害精确等于
 * {@code 原伤害 × (1-T)}（受 35% 单次上限约束）。</p>
 *
 * <p>这样：护甲曲线、面板、离线脚本、Boss 攻击反推都用同一把尺子；护甲超过
 * 115 点后 92% 封顶，职业甲三档的差异集中在 1~3 境（与
 * {@code 工程/13-职业甲数值平衡-结论.md} 的结论一致）。</p>
 *
 * <h2>数值表（真实结算口径）</h2>
 *
 * <pre>
 *   护甲 A     总减伤 T     说明
 *   5          33.3%       练气（职业甲初阶）
 *   20         66.7%       —
 *   23         69.7%       筑基
 *   74         88.1%       金丹（职业甲初阶）
 *   115        92.0%       到达封顶
 *   246+       92.0%       元婴起全部封顶
 * </pre>
 */
public final class ArmorMitigation {

    /** 原版护甲减伤公式里的常数（护甲 / 5）；仅用于兼容旧名字。 */
    private static final double VANILLA_DIVISOR = 5.0D;

    /** 我们这条曲线的陡度：护甲 × 1.5。 */
    public static final double CURVE_SLOPE = 1.5D;

    /** 曲线在护甲为 0 时的基准值。 */
    public static final double CURVE_BASE = 20.0D;

    /** <b>总减伤上限</b>：护甲再高也不会超过 92%。 */
    public static final double MAX_TOTAL_REDUCTION = 0.92D;

    /** 兼容旧名字：原版护甲减伤的参考点。 */
    public static final double VANILLA_ARMOR_CAP = 20.0D;

    /**
     * <b>Attribute 层护甲硬上限</b>。
     *
     * <p>超过它的护甲点只保留在面板数字里，不再进入原版伤害公式——
     * 因为原版公式在 armor≥100 时有「大伤害反而 0 伤」的负数分支，
     * 而 80 点以内是最稳的（{@code armor×0.2=16 ≤ 20}）。</p>
     *
     * <p>真实减伤由 {@link #totalReduction(double)} 用<b>未截断的原始护甲值</b>计算。</p>
     */
    public static final double ARMOR_SOFT_CAP = 80.0D;

    /**
     * 溢出护甲在旧版软上限里的折算比例。
     *
     * <p>仅供 {@link #softCapArmor(double)} 与旧断言兼容；真正生效的是
     * {@link #capVanillaArmor(ServerPlayer)} 的硬封顶。</p>
     */
    public static final double OVERFLOW_FACTOR = 0.10D;

    /**
     * <b>单次最终伤害上限</b>（占目标最大生命的比例）。
     *
     * <p>这是原先 {@code CultivationMonsterEntity.MAX_HIT_RATIO} 的 35% —— 但改为
     * <b>护甲之后</b>结算：以前压在「攻击力」上，护甲 92% 时单次最多只能打掉
     * 2.8% 最大生命，导致「无论 Boss 攻击调到多高，都要至少 36 下才能打死玩家」。
     * 现在它只是防秒杀保险，不参与正常数值设计。</p>
     */
    public static final double MAX_HIT_FRACTION = 0.35D;

    /** 属性层封顶修饰符的固定 UUID（幂等：先摘后挂）。 */
    private static final UUID CAP_MOD_ID = UUID.nameUUIDFromBytes(
            "hongmeng_armor_attribute_cap".getBytes(StandardCharsets.UTF_8));

    /** 属性层封顶修饰符名。 */
    private static final String CAP_MOD_NAME = "hongmeng_armor_attribute_cap";

    /**
     * 公开构造：这个类要被注册成事件监听器
     * （{@code forgeBus.register(new ArmorMitigation())}），
     * 所以不能是 private——私有构造会让注册直接编译失败。
     */
    public ArmorMitigation() {
    }

    // ------------------------------------------------------------------
    // 曲线
    // ------------------------------------------------------------------

    /**
     * 按总护甲算出的**目标总减伤**（设计曲线，未封顶）。
     *
     * @param totalArmor 玩家当前总护甲（法宝 + 境界 + 原版装备）
     */
    public static double targetReduction(double totalArmor) {
        if (totalArmor <= 0.0D) {
            return 0.0D;
        }
        double r = 1.0D - CURVE_BASE / (CURVE_BASE + totalArmor * CURVE_SLOPE);
        return Math.min(MAX_TOTAL_REDUCTION, Math.max(0.0D, r));
    }

    /** 原版（旧版注释口径）的相对减伤曲线，用于兼容旧断言与文档对照。 */
    public static double vanillaReduction(double armor) {
        if (armor <= 0.0D) {
            return 0.0D;
        }
        return 1.0D - 2.0D / (2.0D + armor / VANILLA_DIVISOR);
    }

    /**
     * <b>当前生效的总减伤</b>（唯一真源）。
     *
     * <p>取「旧版相对曲线」与「设计曲线」的较大者，再封 92%。两条曲线在
     * A &gt; 0 时旧版更大（它更陡），所以实际等价于
     * {@code min(92%, 1 - 2/(2 + A/5))}——这正是
     * {@code 工程/13-职业甲数值平衡-结论.md §3.1} 里那张「实际减伤」表的口径。</p>
     */
    public static double totalReduction(double rawArmor) {
        if (rawArmor <= 0.0D) {
            return 0.0D;
        }
        return Math.min(MAX_TOTAL_REDUCTION,
                Math.max(vanillaReduction(rawArmor), targetReduction(rawArmor)));
    }

    /**
     * 需要在事件里**追加**的减伤比例（旧语义，保留给旧断言）。
     *
     * <p>现在已经不靠它结算，见类注释。返回值 = (目标 - 相对) / (1 - 相对)。</p>
     */
    public static double extraReduction(double totalArmor) {
        double target = targetReduction(totalArmor);
        double vanilla = vanillaReduction(totalArmor);
        return Math.max(0.0D, target - vanilla) / Math.max(1e-6D, 1.0D - vanilla);
    }

    /**
     * 兼容旧签名：现在返回「追加减伤」。
     *
     * @deprecated 语义已经从"额外减伤"改成"追加减伤"，用
     * {@link #targetReduction(double)} 或 {@link #totalReduction(double)} 更清楚。
     */
    @Deprecated
    public static double reduction(double totalArmor) {
        return extraReduction(totalArmor);
    }

    /** 面板用的百分比文本（显示真实总减伤）。 */
    public static String reductionText(double totalArmor) {
        return String.format("%.1f%%", totalReduction(totalArmor) * 100.0D);
    }

    // ------------------------------------------------------------------
    // 玩家原始护甲值与属性层封顶
    // ------------------------------------------------------------------

    /**
     * 玩家**原始护甲值**（未截断，含职业甲/法宝/境界/天赋/原版装备）。
     *
     * <p>先读当前 ARMOR 属性（已被 {@link #capVanillaArmor(ServerPlayer)} 压到 80），
     * 再减去封顶修饰符的量，即可还原出真实数值。</p>
     */
    public static double rawArmorOf(ServerPlayer player) {
        if (player == null) {
            return 0.0D;
        }
        AttributeInstance inst = player.getAttribute(Attributes.ARMOR);
        if (inst == null) {
            return 0.0D;
        }
        double value = inst.getValue();
        AttributeModifier cap = inst.getModifier(CAP_MOD_ID);
        if (cap != null) {
            value -= cap.getAmount();
        }
        return Math.max(0.0D, value);
    }

    /**
     * 把玩家的 {@code Attributes.ARMOR} 压到 {@value #ARMOR_SOFT_CAP}。
     *
     * <p>幂等：每次先摘自己的修饰符、重算，再按需挂。由
     * {@code GearManager.refresh}（每 20 tick）与本类在境界变化后调用，
     * 所以换装备/突破后的 1 秒内会自动跟上。</p>
     */
    public static void capVanillaArmor(ServerPlayer player) {
        if (player == null) {
            return;
        }
        AttributeInstance inst = player.getAttribute(Attributes.ARMOR);
        if (inst == null) {
            return;
        }
        inst.removeModifier(CAP_MOD_ID);
        double raw = inst.getValue();
        if (raw > ARMOR_SOFT_CAP) {
            inst.addTransientModifier(new AttributeModifier(CAP_MOD_ID, CAP_MOD_NAME,
                    ARMOR_SOFT_CAP - raw, AttributeModifier.Operation.ADDITION));
        }
    }

    /**
     * 给护甲值做旧版软上限（保留给旧断言/工具，实际伤害路径不再使用）。
     *
     * <p>超过 {@value #ARMOR_SOFT_CAP} 的部分按 {@value #OVERFLOW_FACTOR} 折算。</p>
     */
    public static double softCapArmor(double rawArmor) {
        if (rawArmor <= ARMOR_SOFT_CAP) {
            return Math.max(0.0D, rawArmor);
        }
        double over = rawArmor - ARMOR_SOFT_CAP;
        return ARMOR_SOFT_CAP + over * OVERFLOW_FACTOR;
    }

    /**
     * <b>某个实体当前的实际护甲减免率</b>（0~1）。
     *
     * <p>给「破防」用：{@code LivingHurtEvent} 拿不到"这一击被护甲减了多少"，
     * 所以魔修的破防只能按<b>目标当前的实际减免率</b>折算成等效增伤 ——
     * 这样"护甲越厚，破防收益越大"，符合直觉，也不用拍常数。</p>
     */
    public static double reductionOf(net.minecraft.world.entity.LivingEntity entity) {
        if (entity == null) {
            return 0.0D;
        }
        try {
            if (entity instanceof ServerPlayer player) {
                return totalReduction(rawArmorOf(player));
            }
            return totalReduction(entity.getArmorValue());
        } catch (Throwable t) {
            return 0.0D;
        }
    }

    // ------------------------------------------------------------------
    // 事件：接管总减伤（护甲后） + 35% 单次上限
    // ------------------------------------------------------------------

    /**
     * 在 {@link EventPriority#LOWEST} 接管伤害：所有其它减伤都算完之后，
     * 用同一条护甲曲线改写最终伤害，并做 35% 单次封顶。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }
        double rawArmor = rawArmorOf(player);
        double total = totalReduction(rawArmor);
        double finalDamage = amount * (1.0D - total);
        // 35% 单次上限：只防秒杀，不参与正常数值设计
        double hitCap = player.getMaxHealth() * MAX_HIT_FRACTION;
        if (finalDamage > hitCap) {
            finalDamage = hitCap;
        }
        if (finalDamage <= 0.0D) {
            event.setAmount(0.0F);
            return;
        }
        // 属性层已经被压到 ≤80；这里再夹一次，防止同一 tick 内装备变化导致越界
        double attributeArmor = Math.min(rawArmor, ARMOR_SOFT_CAP);
        double toughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        double preArmor = solvePreArmor(attributeArmor, toughness, finalDamage);
        if (!Double.isFinite(preArmor) || preArmor < 0.0D) {
            preArmor = finalDamage;
        }
        event.setAmount((float) preArmor);
    }

    /**
     * 反解「原版护甲结算之前应该填多少」。
     *
     * <p>原版会再乘一次 {@code (1 - v(x))}，其中 {@code x} 是我们填进去的值。
     * 要求 {@code x × (1 - v(x)) = finalDamage}，对 x 做不动点迭代即可
     * （v 是分段线性的，收敛很快）。</p>
     */
    private static double solvePreArmor(double armor, double toughness, double finalDamage) {
        double x = finalDamage;
        for (int i = 0; i < 12; i++) {
            double remain = 1.0D - vanillaAbsorb(armor, toughness, x);
            if (remain < 1.0E-4D) {
                remain = 1.0E-4D;
            }
            double next = finalDamage / remain;
            if (!Double.isFinite(next) || next < 0.0D) {
                return finalDamage;
            }
            if (Math.abs(next - x) <= 1.0E-6D * Math.max(1.0D, finalDamage)) {
                return next;
            }
            x = next;
        }
        return x;
    }

    /**
     * 与 1.20.1 {@code CombatRules.getDamageAfterAbsorb} 完全同构的吸收比例。
     *
     * <pre>
     *   f  = 2 + toughness/4
     *   f1 = clamp(armor - damage/f, armor×0.2, 20)
     *   absorb = f1 / 25
     * </pre>
     */
    private static double vanillaAbsorb(double armor, double toughness, double damage) {
        double f = 2.0D + toughness / 4.0D;
        double f1 = clamp(armor - damage / f, armor * 0.2D, 20.0D);
        f1 = clamp(f1, 0.0D, 20.0D);
        return f1 / 25.0D;
    }

    /** 与 {@code Mth.clamp} 同语义（value &lt; min → min；value &gt; max → max）。 */
    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    // ------------------------------------------------------------------
    // 面板 / 兼容
    // ------------------------------------------------------------------

    /** 只统计法宝提供的护甲（不含原版装备），用于面板显示。 */
    public static double artifactArmor(ServerPlayer player) {
        return GearManager.artifactArmorOf(player);
    }

    /** 给玩家的一条摘要（面板用）。 */
    public static String describe(Player player) {
        double armor = 0.0D;
        double art = 0.0D;
        if (player instanceof ServerPlayer sp) {
            armor = rawArmorOf(sp);
            art = artifactArmor(sp);
        } else {
            armor = player.getArmorValue();
        }
        double total = totalReduction(armor);
        return String.format("护甲值 %.0f（法宝 %.0f）  实际减伤 %.1f%%",
                armor, art, total * 100.0D);
    }

    /** 判断一件物品是不是法宝（给 GearManager 用）。 */
    public static boolean isArtifact(Object item) {
        return item instanceof ArtifactItem;
    }
}
