package com.hongmeng.xiuxian.domain;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Random;

/**
 * 道宫的服务端逻辑：攒能量、凝聚、破碎、领域开关。
 *
 * <p>道印境起才能凝聚。凝聚消耗能量，品质随机（100 次保底仙品、500 次保底鸿蒙）。
 * 不满意就破碎重来，破碎会把能量退回一部分，避免玩家被一次烂品质卡死。</p>
 */
public final class DomainManager {

    /** 破碎时退回的能量比例。 */
    private static final double SHATTER_REFUND = 0.5D;

    /**
     * 领域持续时间（tick），开启一次维持 30 秒。
     *
     * <p>⚠ 这个常量以前<b>定义了但全文没人用</b> —— 领域其实可以无限开着，
     * 于是"本命绝技"退化成了永久被动。现在由
     * {@link DomainRuntime} 真正执行它。</p>
     */
    public static final int DOMAIN_DURATION = 20 * 30;

    /** 领域收起后的冷却（60 秒）—— 防止"关了立刻再开"绕过时长上限。 */
    public static final int DOMAIN_COOLDOWN = 20 * 60;

    /** 领域运行时每秒消耗的道宫能量。 */
    public static final int DOMAIN_DRAIN_PER_SECOND = 1;

    private static final Random RANDOM = new Random();

    private DomainManager() {
    }

    // ------------------------------------------------------------------
    // 能量
    // ------------------------------------------------------------------

    /** 击杀获得凝聚能量。 */
    public static void onKill(ServerPlayer player, CultivationData data, LivingEntity victim) {
        if (!data.isAwakened()) {
            return;
        }
        DomainData domains = data.getDomainData();
        int gain = 1;
        if (victim.getMaxHealth() >= 40.0F) {
            gain = 3;      // 精英
        }
        if (victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || victim instanceof net.minecraft.world.entity.monster.warden.Warden) {
            gain = 20;     // BOSS
        }
        domains.addEnergy(gain);
        if (data.realm().hasDomain()) {
            player.displayClientMessage(Component.literal(
                    "\u00a75道宫能量 \u00a7f+" + gain + " \u00a78(当前 " + domains.energy() + ")"), true);
        }
    }

    // ------------------------------------------------------------------
    // 凝聚
    // ------------------------------------------------------------------

    public static void condense(ServerPlayer player, CultivationData data) {
        DomainData domains = data.getDomainData();
        if (!data.isAwakened()) {
            player.sendSystemMessage(Component.literal("\u00a7c灵根都没觉醒，谈什么道宫。"));
            return;
        }
        if (!data.realm().hasDomain()) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c道宫是道印境的事。你现在是 \u00a7e" + data.realm().cn()));
            player.sendSystemMessage(Component.literal(
                    "\u00a78（先突破到道印。）"));
            return;
        }
        if (domains.domainCount() >= domains.maxDomains()) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c道宫已满（" + domains.maxDomains() + " 座）。"
                            + "不满意就破碎重凝，或者用双道宫丹解锁第二座。"));
            return;
        }
        if (!domains.canCondense()) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c能量不足：需要 " + DomainData.CONDENSE_COST
                            + "，当前 " + domains.energy() + "。\u00a77（杀怪攒能量）"));
            return;
        }

        // ---- 元素由【领域自己】决定（不再看灵根）----
        //
        // ⚠ 这里原来是 `Element preferred = data.getRoot().element()` ——
        //   灵根的属性维度被移除后（玩家要求），领域必须自己决定元素。
        //   于是"领域属性"成了唯一的元素来源：想要某个元素的道宫，
        //   就得多凝聚/破碎重凝，而不是靠开局抽到那条灵根。
        DomainData.Domain domain = domains.condense(RANDOM, null, data.difficulty());
        if (domain == null) {
            return;
        }
        ModNetworkBridge.sync(player);

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a75\u00a7l═══ 道 宫 凝 成 ═══"));
        player.sendSystemMessage(Component.literal(
                "\u00a77灵海震动，一座宫殿自识海深处升起——"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f品质：" + domain.quality().colored()
                        + "   \u00a7f元素：\u00a7b" + domain.element().display()));
        player.sendSystemMessage(Component.literal(
                "\u00a7f领域内同元素功法伤害：\u00a7c" + domain.quality().damageText()
                        + "   \u00a7f释放速度：\u00a7e" + domain.quality().castText()));
        player.sendSystemMessage(Component.literal(
                "\u00a77累计凝聚：" + domains.totalCondensed() + " 次"));
        if (domains.totalCondensed() < DomainData.PITY_IMMORTAL) {
            player.sendSystemMessage(Component.literal(
                    "\u00a78距保底仙品还差 " + domains.energyIntoPityImmortal() + " 次"));
        } else if (domains.totalCondensed() < DomainData.PITY_HONGMENG) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7d已进入仙品保底段，距鸿蒙保底还差 "
                            + domains.energyIntoPityHongmeng() + " 次"));
        }
        player.sendSystemMessage(Component.literal(""));

        if (domain.quality() == DomainQuality.HONGMENG) {
            player.sendSystemMessage(Component.literal(
                    "\u00a75\u00a7l鸿蒙品质——灵海里的那座宫殿，连天都不敢细看。"));
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    // ------------------------------------------------------------------
    // 破碎
    // ------------------------------------------------------------------

    public static void shatter(ServerPlayer player, CultivationData data, int index) {
        DomainData domains = data.getDomainData();
        DomainData.Domain removed = domains.shatter(index);
        if (removed == null) {
            player.sendSystemMessage(Component.literal("\u00a7c没有这座道宫。"));
            return;
        }
        int refund = (int) Math.round(DomainData.CONDENSE_COST * SHATTER_REFUND);
        domains.addEnergy(refund);
        ModNetworkBridge.sync(player);

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a74\u00a7l道宫破碎"));
        player.sendSystemMessage(Component.literal(
                "\u00a77你亲手把灵海里那座 " + removed.quality().colored()
                        + "\u00a77 道宫碾碎。"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f残存的能量回流：\u00a7a+" + refund
                        + " \u00a77（当前 " + domains.energy() + "）"));
        player.sendSystemMessage(Component.literal(
                "\u00a78（破碎不退还凝聚次数，保底进度保留。）"));
        player.sendSystemMessage(Component.literal(""));

        player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    // ------------------------------------------------------------------
    // 领域（道宫技）
    // ------------------------------------------------------------------

    public static void toggleDomain(ServerPlayer player, CultivationData data) {
        DomainData domains = data.getDomainData();
        if (domains.domainCount() == 0) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c还没有道宫，哪来的领域。"));
            return;
        }
        boolean active = !domains.isDomainActive();
        // ---- 开领域前的两道门：冷却 + 能量 ----
        //
        // 没有这两道门的话，"30 秒时长上限"会被"关了立刻再开"绕过 ——
        // 那限制就等于没做。
        if (active) {
            int left = DomainRuntime.cooldownLeft(player);
            if (left > 0) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7c道宫尚未平复，还需 \u00a7e"
                                + String.format("%.1f", left / 20.0D) + " \u00a7c秒。"));
                return;
            }
            if (domains.energy() <= 0) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7c道宫能量枯竭，无法开启领域。\u00a77（杀怪补充能量）"));
                return;
            }
        }
        domains.setDomainActive(active);
        // 记账：开启时清零计时，收起时开始冷却
        DomainRuntime.onToggled(player, active);
        ModNetworkBridge.sync(player);

        if (active) {
            StringBuilder elements = new StringBuilder();
            double bestDamage = 0.0D;
            for (DomainData.Domain d : domains.domains()) {
                if (elements.length() > 0) {
                    elements.append("\u00a77 / ");
                }
                elements.append("\u00a7b").append(d.element().cn());
                bestDamage = Math.max(bestDamage, d.quality().techniqueDamage());
            }
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal(
                    "\u00a75\u00a7l本命领域 · 开"));
            player.sendSystemMessage(Component.literal(
                    "\u00a7f范围：\u00a7e" + DomainField.rangeText()
                            + "   \u00a7f元素：" + elements));
            player.sendSystemMessage(Component.literal(
                    "\u00a7f领域内同元素功法伤害：\u00a7c+"
                            + (int) Math.round(bestDamage * 100) + "%"
                            + "   \u00a7f释放速度：\u00a7e+"
                            + (int) Math.round(domains.castSpeedBonus() * 100) + "%"));
            // 每种元素的效果一句话——数值全来自纯数据表，这里不抄第二份
            for (DomainData.Domain d : domains.domains()) {
                if (DomainEffectTable.hasEffect(d.element())) {
                    player.sendSystemMessage(Component.literal(
                            "  " + DomainEffectTable.summary(d.element())));
                }
            }
            player.sendSystemMessage(Component.literal(
                    "\u00a77持续：\u00a7f" + (DOMAIN_DURATION / 20) + " 秒"
                            + " \u00a77· 每秒耗能量 \u00a7f" + DOMAIN_DRAIN_PER_SECOND
                            + " \u00a77· 收起后冷却 \u00a7f" + (DOMAIN_COOLDOWN / 20)
                            + " 秒"));
            player.sendSystemMessage(Component.literal(
                    "\u00a7f" + DomainDamage.targetText(data.isDomainFriendlyFire())));
            player.sendSystemMessage(Component.literal(
                    "\u00a78（R 面板里可以开关友伤。）"));
            player.sendSystemMessage(Component.literal(""));
            player.level().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.PLAYERS, 0.6F, 1.2F);
        } else {
            // 关领域：把束缚类效果与各元素的计时标记收干净（不然下次开领域会"续上"）
            DomainField.releaseAll(player);
            player.sendSystemMessage(Component.literal(
                    "\u00a77本命领域 · 收"));
        }
    }

    /**
     * 切换领域的友伤开关（规格 §2.1，默认关）。
     *
     * <p>只改一个布尔值 + 回同步；客户端 R 面板与领域特效都读这个值。</p>
     */
    public static void toggleFriendlyFire(ServerPlayer player, CultivationData data) {
        boolean next = !data.isDomainFriendlyFire();
        data.setDomainFriendlyFire(next);
        ModNetworkBridge.sync(player);
        player.sendSystemMessage(Component.literal(
                "\u00a75本命领域\u00a77 · " + DomainDamage.targetText(next)));
        if (next) {
            player.sendSystemMessage(Component.literal(
                    "\u00a78（玩家、驯服的宠物/坐骑、NPC 现在都会被领域打。）"));
        } else {
            player.sendSystemMessage(Component.literal(
                    "\u00a78（领域只打敌对生物了。）"));
        }
    }

    /**
     * 领域效果的每 tick 入口。由 {@code ModEvents.onPlayerTick} 调用一次。
     *
     * <p>真正的结算在 {@link DomainField}（八种元素、纯数据表在
     * {@link DomainEffectTable}）。这里只做一次转发，方便 ModEvents 少写点字。</p>
     */
    public static void tickField(ServerPlayer player, CultivationData data) {
        DomainField.tick(player, data);
    }

    /** 是否处于领域开启状态（供伤害计算用）。 */
    public static boolean isDomainActive(CultivationData data) {
        return data.getDomainData().isDomainActive()
                && data.getDomainData().domainCount() > 0;
    }

    /** 领域内某元素功法的伤害加成。 */
    public static double techniqueBonus(CultivationData data, Element element) {
        if (!isDomainActive(data)) {
            return 0.0D;
        }
        return data.getDomainData().techniqueBonus(element);
    }

    /** 领域带来的释放速度加成。 */
    public static double castSpeedBonus(CultivationData data) {
        if (!isDomainActive(data)) {
            return 0.0D;
        }
        return data.getDomainData().castSpeedBonus();
    }

    /** 面板里显示的一行摘要。 */
    public static String summary(CultivationData data) {
        DomainData domains = data.getDomainData();
        if (!data.realm().hasDomain()) {
            return "\u00a78未至道印";
        }
        if (domains.domainCount() == 0) {
            return "\u00a7e未凝聚 \u00a78(能量 " + domains.energy() + "/"
                    + DomainData.CONDENSE_COST + ")";
        }
        StringBuilder sb = new StringBuilder();
        for (DomainData.Domain d : domains.domains()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(d.quality().colored()).append("\u00a77·").append(d.element().cn());
        }
        return sb.toString();
    }
}
