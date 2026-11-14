package com.hongmeng.xiuxian.talent;

import net.minecraft.server.level.ServerPlayer;

/**
 * <b>职业与天赋加成的汇总读取口</b>。
 *
 * <h2>为什么集中到一处</h2>
 * <p>战斗结算需要的是"最终加成"，它由三处叠加：</p>
 * <ol>
 *   <li>职业基础乘数（{@link Career#multipliers()}，五维）；</li>
 *   <li>职业专属加成（{@link Career#bonus()}，选了就有）；</li>
 *   <li>天赋树点出来的（{@link Talents#bonus}）。</li>
 * </ol>
 * <p>如果让每处战斗代码自己相加，迟早漏一处或加重复；所以战斗只问这里。</p>
 *
 * <h2>⚠ 口径</h2>
 * <p>全部返回<b>加法比例</b>（0.15 = +15%），不是乘数 —— 多处相加不会互相放大。</p>
 */
public final class TalentBonus {

    private TalentBonus() {
    }

    /** 法术伤害总加成。 */
    public static double spellDamage(ServerPlayer player) {
        Career c = Talents.pathOf(player);
        double base = c == null ? 1.0D : c.spellDamage();
        return (base - 1.0D) + Talents.bonus(player, CareerTalentTree.Kind.SPELL);
    }

    /** 近战伤害总加成（职业乘数 + 天赋）。 */
    public static double meleeDamage(ServerPlayer player) {
        Career c = Talents.pathOf(player);
        double base = c == null ? 1.0D : c.melee();
        return (base - 1.0D) + Talents.bonus(player, CareerTalentTree.Kind.MELEE);
    }

    /** 灵力上限总加成。 */
    public static double mana(ServerPlayer player) {
        Career c = Talents.pathOf(player);
        double base = c == null ? 1.0D : c.manaMax();
        return (base - 1.0D) + Talents.bonus(player, CareerTalentTree.Kind.MANA);
    }

    /** 生命上限总加成。 */
    public static double health(ServerPlayer player) {
        Career c = Talents.pathOf(player);
        double base = c == null ? 1.0D : c.health();
        return (base - 1.0D) + Talents.bonus(player, CareerTalentTree.Kind.HEALTH);
    }

    /** 护甲 / 减伤总加成。 */
    public static double damageReduction(ServerPlayer player) {
        Career c = Talents.pathOf(player);
        double base = c == null ? 1.0D : c.armor();
        return (base - 1.0D) * 0.33D + Talents.bonus(player, CareerTalentTree.Kind.ARMOR);
    }

    /** 吸血比例（职业专属 + 天赋）。 */
    public static double lifesteal(ServerPlayer player) {
        double base = Talents.careerBonus(player, Career.BonusKind.LIFESTEAL);
        return base + Talents.bonus(player, CareerTalentTree.Kind.LIFESTEAL);
    }

    /** 破防（无视护甲比例）。 */
    public static double penetration(ServerPlayer player) {
        double base = Talents.careerBonus(player, Career.BonusKind.PENETRATION);
        return base + Talents.bonus(player, CareerTalentTree.Kind.PENETRATION);
    }

    /** 冷却缩减（封顶 60%）。 */
    public static double cooldownCut(ServerPlayer player) {
        return Math.min(0.60D, Talents.bonus(player, CareerTalentTree.Kind.COOLDOWN));
    }

    /** 抗击退（封顶 85%）。 */
    public static double knockbackResist(ServerPlayer player) {
        double base = Talents.careerBonus(player, Career.BonusKind.KNOCKBACK_RESIST);
        return Math.min(0.85D, base
                + Talents.bonus(player, CareerTalentTree.Kind.KNOCKBACK_RESIST));
    }

    /** 领域范围加成。 */
    public static double area(ServerPlayer player) {
        return Talents.bonus(player, CareerTalentTree.Kind.AREA);
    }

    /** 受伤反弹。 */
    public static double reflect(ServerPlayer player) {
        return Talents.bonus(player, CareerTalentTree.Kind.REFLECT);
    }

    /** 鬼域内减速加成。 */
    public static double control(ServerPlayer player) {
        return Talents.bonus(player, CareerTalentTree.Kind.GHOST_SLOW);
    }

    // ------------------------------------------------------------------
    // 新职业机制相关
    // ------------------------------------------------------------------

    /** 攻速加成（2 件套 + 天赋；R-1 阶段天赋树暂无攻速节点，先给套装预留）。 */
    public static double attackSpeed(ServerPlayer player) {
        return Talents.bonus(player, CareerTalentTree.Kind.ATTACK_SPEED);
    }

    /** 剑意获取加成（专属 + 天赋）。 */
    public static double swordIntent(ServerPlayer player) {
        double base = Talents.careerBonus(player, Career.BonusKind.SWORD_INTENT);
        return base + Talents.bonus(player, CareerTalentTree.Kind.SWORD_INTENT);
    }

    /** 剑气伤害加成。 */
    public static double swordQi(ServerPlayer player) {
        return Talents.bonus(player, CareerTalentTree.Kind.SWORD_QI);
    }

    /** 剑罡吸收率加成。 */
    public static double swordAura(ServerPlayer player) {
        return Talents.bonus(player, CareerTalentTree.Kind.SWORD_AURA);
    }

    /** 魂印伤害加成（专属 + 天赋）。 */
    public static double soulMark(ServerPlayer player) {
        double base = Talents.careerBonus(player, Career.BonusKind.SOUL_MARK);
        return base + Talents.bonus(player, CareerTalentTree.Kind.SOUL_MARK);
    }

    /** 鬼域威力加成。 */
    public static double ghostRealm(ServerPlayer player) {
        return Talents.bonus(player, CareerTalentTree.Kind.GHOST_REALM);
    }
}
