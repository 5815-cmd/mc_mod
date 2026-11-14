package com.hongmeng.xiuxian.quality;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Random;

/**
 * 把品质<b>写进 ItemStack 的 NBT</b>，并在需要时读回来。
 *
 * <p>路线图要求"品质必须持久化在物品上"。实现口径：</p>
 *
 * <pre>
 *   hongmeng_quality       字符串   品质的英文键（mortal/fine/superior/exquisite/immortal/divine）
 *   hongmeng_quality_rank  整数     0..5，字符串丢了也能兜住（存档被别的工具改过时）
 *   hongmeng_quality_rolled 字节    1 = 这件东西是掷过骰子的（合成出来的不带这个标记）
 * </pre>
 *
 * <p><b>为什么不只用 ordinal</b>：以后在阶梯中间插一档，ordinal 全部错位；
 * 键是稳定的，序号只当兜底。</p>
 *
 * <p><b>读取优先级</b>：NBT 键 → NBT 序号 → 物品天生品质
 * （{@link QualityTable#intrinsic(String)}）→ 调用方给的兜底值。
 * 所以没掷过骰子的普通修为丹依然是 ×1，完美修为丹依然是 ×3，
 * 老存档（完全没有这些 NBT）读出来一个数字都不变。</p>
 *
 * <p>这个类会碰 {@code BuiltInRegistries}（把物品换成注册名），
 * <b>只能在客户端/服务端运行时调用</b>，绝不要放进纯数据自检里——
 * 离线验证器一碰就 {@code Not bootstrapped}。</p>
 */
public final class ItemQuality {

    /** NBT 键：品质英文键。 */
    public static final String TAG_KEY = "hongmeng_quality";
    /** NBT 键：品质序号（兜底）。 */
    public static final String TAG_RANK = "hongmeng_quality_rank";
    /** NBT 键：是否掷过骰子。 */
    public static final String TAG_ROLLED = "hongmeng_quality_rolled";

    private ItemQuality() {
    }

    /** 物品注册名（带命名空间）；空物品返回空串。 */
    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    /** 这件东西有没有品质（表见 {@link QualityTable}）。 */
    public static boolean canHaveQuality(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && QualityTable.canHaveQuality(itemId(stack));
    }

    /** 这件东西身上有没有写死的品质（NBT 里有没有）。 */
    public static boolean hasTag(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && (tag.contains(TAG_KEY) || tag.contains(TAG_RANK));
    }

    /** 从 NBT 读品质；没写返回 null（**不要**在这里回落到天生品质）。 */
    public static Quality read(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return null;
        }
        if (tag.contains(TAG_KEY)) {
            String key = tag.getString(TAG_KEY);
            if (key != null && !key.isBlank()) {
                return Quality.byKey(key);
            }
        }
        if (tag.contains(TAG_RANK)) {
            return Quality.byRank(tag.getInt(TAG_RANK));
        }
        return null;
    }

    /** 把品质写进物品（同时写键与序号，并标记"掷过骰子"）。 */
    public static void set(ItemStack stack, Quality quality) {
        set(stack, quality, true);
    }

    /**
     * 把品质写进物品。
     *
     * @param rolled true = 掷过骰子（掉落 / 炼丹 / 任务）；false = 合成时盖的章
     */
    public static void set(ItemStack stack, Quality quality, boolean rolled) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Quality q = quality == null ? Quality.MORTAL : quality;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_KEY, q.key());
        tag.putInt(TAG_RANK, q.rank());
        if (rolled) {
            tag.putBoolean(TAG_ROLLED, true);
        }
    }

    /** 抹掉品质（自检 / 回滚用）。 */
    public static void clear(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(TAG_KEY);
        tag.remove(TAG_RANK);
        tag.remove(TAG_ROLLED);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    /** 这件东西是不是掷过骰子的（掉落 / 炼丹得来）。 */
    public static boolean isRolled(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_ROLLED);
    }

    /** 有效品质：NBT → 天生品质 → 凡品。 */
    public static Quality of(ItemStack stack) {
        Quality tagged = read(stack);
        if (tagged != null) {
            return tagged;
        }
        return QualityTable.intrinsic(itemId(stack));
    }

    /** 有效品质：NBT → 物品自带的档位（例如丹药的 {@code PillQuality}）→ 天生品质。 */
    public static Quality of(ItemStack stack, Quality intrinsicFallback) {
        Quality tagged = read(stack);
        if (tagged != null) {
            return tagged;
        }
        if (intrinsicFallback != null) {
            return intrinsicFallback;
        }
        return QualityTable.intrinsic(itemId(stack));
    }

    /** 效果倍率（丹药 / 通用）。 */
    public static double effectMultiplier(ItemStack stack) {
        return of(stack).effectMultiplier();
    }

    /** 法宝属性倍率。 */
    public static double gearMultiplier(ItemStack stack) {
        return of(stack).gearMultiplier();
    }

    /** 带品质的回收价（凡品 = 原价，见 {@link QualityTable#sellPrice}）。 */
    public static int sellPrice(ItemStack stack, int baseUnitPrice) {
        return QualityTable.sellPrice(baseUnitPrice, canHaveQuality(stack) ? of(stack) : null);
    }

    /**
     * 掷一次品质并写进物品（已经写过的**不覆盖**）。
     *
     * @return 物品现在身上的品质（本来就有的原样返回）
     */
    public static Quality rollOnce(ItemStack stack, QualitySource source, int shift, Random rng) {
        Quality existing = read(stack);
        if (existing != null) {
            return existing;
        }
        if (!canHaveQuality(stack)) {
            return Quality.MORTAL;
        }
        Quality rolled = source.roll(rng, shift);
        set(stack, rolled, true);
        return rolled;
    }

    /** 合成：盖上"合成品质"的章（max(凡品, 天生品质)，不掷骰子）。 */
    public static Quality applyCrafted(ItemStack stack) {
        if (!canHaveQuality(stack)) {
            return Quality.MORTAL;
        }
        Quality existing = read(stack);
        if (existing != null) {
            return existing;
        }
        Quality crafted = QualityTable.craftedQuality(itemId(stack));
        set(stack, crafted, false);
        return crafted;
    }

    /** 妖兽掉落：按怪物强度掷品质。 */
    public static Quality applyMobDrop(ItemStack stack, double maxHealth, Random rng) {
        return rollOnce(stack, QualitySource.MOB_DROP, QualityTable.dropShift(maxHealth), rng);
    }

    /** 炼丹出锅：按最优灵植品阶掷品质。 */
    public static Quality applyAlchemy(ItemStack stack, int mainTier, int auxTier, Random rng) {
        return rollOnce(stack, QualitySource.ALCHEMY,
                AlchemyQuality.shiftFor(mainTier, auxTier), rng);
    }

    /** 副本掉落 / 任务奖励：起点比野怪高。 */
    public static Quality applySource(ItemStack stack, QualitySource source, int shift, Random rng) {
        return rollOnce(stack, source, shift, rng);
    }

    /**
     * tooltip 里那一行（彩色）。
     *
     * <p>格式统一为「品质：上品 (效果 ×3)」；法宝再补一句属性加成，
     * 回收价加成的百分比也写出来，玩家一眼知道这东西好在哪。</p>
     */
    public static String tooltipLine(ItemStack stack) {
        Quality q = of(stack);
        StringBuilder sb = new StringBuilder("\u00a77品质：").append(q.colored());
        if (q.effectMultiplier() > 1.0D) {
            sb.append("\u00a77 效果 ").append(q.multiplierText());
        }
        if (q.gearMultiplier() > 1.0D) {
            sb.append("\u00a77 属性 ").append(q.gearText());
        }
        if (q.sellPercent() > 100) {
            sb.append("\u00a77 回收价 ").append(q.sellText());
        }
        if (q.rank() >= Quality.IMMORTAL.rank()) {
            sb.append("\u00a7").append(q.colorCode()).append(" \u2726");
        }
        return sb.toString();
    }

    /** 这个物品该不该在 tooltip 里显示品质行。 */
    public static boolean showsTooltip(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return canHaveQuality(stack) || hasTag(stack);
    }
}
