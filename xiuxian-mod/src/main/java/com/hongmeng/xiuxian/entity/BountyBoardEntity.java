package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 悬赏牌看板：钉在坊市口的一面木牌，上面贴着今天的悬赏。
 *
 * <p><b>它是一块牌子，不是人</b>——这一点在代码里必须体现出来，否则就是"会走路的告示牌"：</p>
 * <ul>
 *   <li>{@link #canWander()} 返回 {@code false}：父类连"闲逛"目标都不会加；</li>
 *   <li>{@link #isNoAi()} 恒为 true，并且把自身速度压到 0——它连转身都懒得转；</li>
 *   <li>没有攻击力、没有掉落，{@code xpReward} 在父类里已经是 0。</li>
 * </ul>
 *
 * <h3>悬赏怎么给</h3>
 *
 * <p>这里<b>刻意不新起一套任务系统</b>——本模组已经有 {@code quest} 包（任务目录 +
 * 进度 + 追踪 HUD）。所以看板只做它该做的那一件事：<b>报出今天的赏格，
 * 并按境界给一份"接榜的盘缠"</b>，玩家拿着提示去打对应的怪（掉落全部走
 * 既有的 {@link MobRank} 掉落表与新怪的 {@code rankDrop}）。</p>
 *
 * <p>每日一次的限制写在玩家自己的 {@code persistentData} 里（键名带天数），
 * 所以不需要任何额外的存档结构，也不会跨维度丢失。</p>
 */
public class BountyBoardEntity extends CultivationNpcEntity {

    /** 每天能领一次的盘缠。 */
    public static final int DAILY_LEAF_REWARD = 2;

    public BountyBoardEntity(EntityType<? extends BountyBoardEntity> type, Level level) {
        super(type, level);
        // 牌子不说话也不动
        this.setNoAi(true);
    }

    @Override
    public String npcTitle() {
        return "悬赏牌看板";
    }

    @Override
    public Panel panel() {
        return Panel.BOUNTY;
    }

    @Override
    public SoundEvent interactSound() {
        return SoundEvents.BOOK_PAGE_TURN;
    }

    /** 牌子不长脚。 */
    @Override
    public boolean canWander() {
        return false;
    }

    /**
     * 今天的悬赏条目。
     *
     * <p>条目<b>与本模组的怪一一对应</b>（说什么怪就真的掉什么），
     * 而且按品阶从低到高排：玩家一眼就能挑出"我现在打得过的那个"。</p>
     */
    @Override
    public List<String> dialogueLines() {
        return List.of(
                "\u00a78（一块被风吹得发白的木板，上面钉着几张黄纸）",
                "",
                "\u00a7f一、\u00a77普通·灵狼\u00a7r——山中成群出没，赏灵叶若干。",
                "\u00a7a二、\u00a77精英·赤炎狼 / 雷纹鹰\u00a7r——火行与雷行的两只，"
                        + "身上带火种与雷草。",
                "\u00a7b三、\u00a77队长·血魔长老 / 玄冰蟒 / 剑冢守灵\u00a7r——"
                        + "血道、冰道、剑道各一只，赏格翻倍。",
                "\u00a75四、\u00a77头目·虚空行者\u00a7r——会瞬移的那个，"
                        + "没到化神别接。",
                "",
                "\u00a76【规矩】\u00a7r打过榜上的怪，回来点一次牌子领当天的盘缠。");
    }

    /**
     * 服务端：每天一次"接榜盘缠"。
     *
     * <p>键名里带 {@code level.getDayTime() / 24000}，所以跨天自动重置；
     * 不做"任务进度校验"是刻意的——那属于 {@code quest} 包的活，
     * 看板只负责"给个由头让人去打怪"。</p>
     */
    @Override
    protected void onServerInteract(Player player) {
        super.onServerInteract(player);
        long day = this.level().getDayTime() / 24000L;
        String key = "hongmeng_bounty_day";
        var pd = player.getPersistentData();
        if (pd.getLong(key) >= day) {
            return;
        }
        pd.putLong(key, day);
        this.gift(player, new ItemStack(ModItems.LINGYE.get(), DAILY_LEAF_REWARD));
        this.tell(player, "\u00a7e[悬赏牌]\u00a7r 榜单上的怪打死一只都算数，"
                + "今天的盘缠先给你。");
    }

    /** 牌子很硬：血多、甲厚、完全免疫击退（免得被玩家推着走）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 8.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}
