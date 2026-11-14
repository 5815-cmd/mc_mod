package com.hongmeng.xiuxian.domain;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.CultivationNpcEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * 领域的"打谁 / 怎么打"。
 *
 * <p>与 {@link DomainEffectTable} 分开：那张表是<b>纯数据</b>（离线可断言），
 * 这里要碰实体与伤害源，只能在服务端跑。</p>
 */
public final class DomainDamage {

    private DomainDamage() {
    }

    // ------------------------------------------------------------------
    // 伤害源
    // ------------------------------------------------------------------

    /**
     * 领域伤害的伤害源：<b>间接魔法，归属者是这个玩家</b>。
     *
     * <p>为什么不用 {@code lightningBolt()}：它带 {@code BYPASSES_ARMOR}，
     * 当初渡劫就是这么变成"雷劫必死"的（规格 §六.6）。</p>
     *
     * <p>为什么不用无归属的 {@code magic()}：那样击杀不计到玩家头上——
     * 领域杀怪不给修为、不给道宫能量、不推进任务。用
     * {@code indirectMagic(玩家, 玩家)} 后 {@code source.getEntity()} 就是玩家，
     * 击杀结算（{@code ModEvents.onLivingDeath}）自然认得。
     * 类型仍是 {@code INDIRECT_MAGIC}，不带任何 armor/invulnerability 绕过标记。</p>
     */
    public static DamageSource source(ServerPlayer owner) {
        return owner.damageSources().indirectMagic(owner, owner);
    }

    /**
     * 结算一次领域伤害。
     *
     * <p><b>把无敌帧清零再打</b>：原版 {@code LivingEntity.hurt} 在
     * {@code invulnerableTime > 10} 时会吞掉/削减同一强度的后续伤害，
     * 而领域是按固定节奏结算的（风 0.25 秒一击、金 0.5 秒一击）——
     * 不清零的话"风的频率比别的元素高"这条会直接失效（第二下被打折成 0）。</p>
     *
     * @return 是否真的打中了（没打中就不该记标记/计数）
     */
    /** 领域伤害期间临时顶满击退抗性用的固定修饰符 id。 */
    private static final java.util.UUID NO_KNOCKBACK_ID =
            java.util.UUID.nameUUIDFromBytes(
                    "hongmeng_domain_no_knockback".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    /**
     * 结算一次领域伤害。
     *
     * <p><b>不带击退</b>（玩家要求：「领域攻击不要有击退效果」）。做法是
     * 「临时顶满击退抗性 → 结算 → 立刻还原」：1.20.1 没有"本次伤害不带击退"的开关，
     * 击退是在 {@code LivingEntity.hurt} 内部按 {@code KNOCKBACK_RESISTANCE} 算的，
     * 所以只能把那个属性按住一瞬间。窗口只有一个方法调用，
     * <b>不会影响玩家其它来源的击退</b>；对本来就有击退抗性的怪是幂等的。</p>
     */
    public static boolean hit(LivingEntity target, ServerPlayer owner, float amount) {
        if (target == null || !target.isAlive() || amount <= 0.0F) {
            return false;
        }
        var res = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes
                .KNOCKBACK_RESISTANCE);
        if (res == null) {
            // 没有这个属性的实体（极少）直接照常结算
            target.invulnerableTime = 0;
            return target.hurt(source(owner), amount);
        }
        target.invulnerableTime = 0;
        boolean added = false;
        try {
            // 先摘掉同名旧修饰符（上一次万一没还原干净），再顶满
            res.removeModifier(NO_KNOCKBACK_ID);
            res.addTransientModifier(new net.minecraft.world.entity.ai.attributes
                    .AttributeModifier(NO_KNOCKBACK_ID, "hongmeng_domain_no_knockback",
                    1.0D - res.getValue(),
                    net.minecraft.world.entity.ai.attributes.AttributeModifier
                            .Operation.ADDITION));
            added = true;
        } catch (Throwable ignored) {
            // 加不上就算了，宁可有击退也不要漏伤害
        }
        // 玩家要求：**领域打到怪物都有一下"粘地僵直"**（微微的控制）。
        // 用缓慢 250 级实现"走不动"，时长很短（默认 5 tick），叠不出无限控。
        try {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    // 天赋「领域控制」拉长僵直（等级已夸张到"走不动"，再加级没意义）
                    com.hongmeng.xiuxian.domain.DomainRuntime.controlTicks(
                            owner,
                            com.hongmeng.xiuxian.domain.DomainEffectTable.STUN_TICKS),
                    com.hongmeng.xiuxian.domain.DomainEffectTable.STUN_SLOWNESS_AMPLIFIER,
                    false, false, false));
        } catch (Throwable ignored) {
            // 上不了减速也要照常结算伤害
        }
        try {
            return target.hurt(source(owner), amount);
        } finally {
            if (added) {
                try {
                    res.removeModifier(NO_KNOCKBACK_ID);
                } catch (Throwable ignored) {
                    // 还原失败不影响已经结算的伤害
                }
            }
        }
    }

    /** 按"最大生命的百分比"打一次（自动套首领封顶规则）。 */
    public static boolean hitPercent(LivingEntity target, ServerPlayer owner, double percent) {
        return hit(target, owner, (float) (target.getMaxHealth() * percent));
    }

    /** 品阶（不是修仙妖兽就算普通）。 */
    public static MobRank rankOf(LivingEntity target) {
        return CultivationMonsterEntity.rankOf(target);
    }

    // ------------------------------------------------------------------
    // 目标筛选（友伤开关）
    // ------------------------------------------------------------------

    /**
     * 领域要不要动这个目标。
     *
     * <p>规格 §2.1：<b>默认不伤害友善生物</b>（玩家、驯服的宠物/坐骑、NPC）。
     * 所以默认只打"敌对生物"（{@link Monster} / {@link Enemy}，
     * 本模组的妖兽继承 {@code Monster}，包含在内）；友伤开关打开后，
     * 其余活物（含玩家、宠物、NPC、动物）也一起打，唯一永远免疫的是施法者自己。</p>
     *
     * <p>创造 / 观察者模式的玩家<b>永远</b>不吃领域伤害——不然在自己领域里
     * 造东西都会被自己的火雨追着打，那不是"友伤开关"该管的事。</p>
     */
    public static boolean shouldAffect(ServerPlayer owner, LivingEntity target,
                                       boolean friendlyFire) {
        if (target == null || target == owner || !target.isAlive() || target.isRemoved()) {
            return false;
        }
        if (target instanceof Player player) {
            return friendlyFire && !player.isCreative() && !player.isSpectator();
        }
        if (isFriendly(owner, target)) {
            return friendlyFire;
        }
        return target instanceof Monster || target instanceof Enemy;
    }

    /**
     * 是不是"友善生物"（领域默认放过的那些）。
     *
     * <p>分三类，与规格 §2.1 的括号一一对应：</p>
     * <ol>
     *   <li><b>玩家</b>——{@link Player}（创造/观察者另算，见 {@link #shouldAffect}）；</li>
     *   <li><b>驯服的宠物 / 坐骑</b>——原版 {@link OwnableEntity}（狼/猫/马/鹦鹉……）
     *       以及本模组的灵兽：{@code SpiritBeastEntity} 继承 {@code TamableAnimal}，
     *       契约狼/雷鹰/蛟龙都从它下来，所以一个 {@code getOwnerUUID() != null} 全覆盖；</li>
     *   <li><b>NPC</b>——本模组的 {@link CultivationNpcEntity}（门派长老 / 炼丹师 /
     *       云游商人）+ 原版村民与流浪商人（{@link AbstractVillager}）+ 铁傀儡/雪傀儡。</li>
     * </ol>
     *
     * <p>剩下的（牛、羊、猪这类没有主人的动物）也算友善——领域是 96×96 格的大范围，
     * 默认把整个生态圈犁一遍显然不是玩家要的"不伤友善生物"。</p>
     */
    public static boolean isFriendly(ServerPlayer owner, LivingEntity target) {
        if (target instanceof Player) {
            return true;
        }
        if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return true;
        }
        if (target instanceof CultivationNpcEntity) {
            return true;
        }
        if (target instanceof AbstractVillager) {
            return true;
        }
        if (target instanceof IronGolem || target instanceof SnowGolem) {
            return true;
        }
        return !(target instanceof Monster) && !(target instanceof Enemy);
    }

    /** 面板里显示"领域会打谁"的一行。 */
    public static String targetText(boolean friendlyFire) {
        return friendlyFire
                ? "\u00a7c友伤开 \u00a78· 玩家/宠物/坐骑/NPC 一起打（自己除外）"
                : "\u00a7a友伤关 \u00a78· 只打敌对生物（玩家/宠物/坐骑/NPC 免疫）";
    }
}
