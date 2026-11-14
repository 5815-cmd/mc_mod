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
 * 丹药商人：坊市里摆丹药摊子的中年修士。
 *
 * <p>与既有的云游商人<b>共用坊市这一条逻辑</b>：面板走
 * {@link Panel#MERCHANT}，客户端 {@code NpcDialogueScreen.open()} 见到它就直接换成
 * 既有的 {@code ShopScreen}——交易、货币、价格全部原地复用，一行都不用重写。</p>
 *
 * <p>两者的分工在<b>定位</b>而不是在功能：云游商人到处跑（褡裢 + 拐杖），
 * 丹药商人守着一个摊位（丹葫芦 + 短袍）。所以这里只多给一件事：
 * <b>第一次见面送两颗回灵丹</b>——摊主拉客的常规手段，也让新手玩家
 * 在与坊市打交道之前先尝到丹药的用处。</p>
 */
public class PillMerchantEntity extends CultivationNpcEntity {

    public PillMerchantEntity(EntityType<? extends PillMerchantEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String npcTitle() {
        return "丹药商人";
    }

    /** 走坊市：面板类型与云游商人一致，客户端会直接开 {@code ShopScreen}。 */
    @Override
    public Panel panel() {
        return Panel.MERCHANT;
    }

    @Override
    public SoundEvent interactSound() {
        return SoundEvents.VILLAGER_TRADE;
    }

    @Override
    public List<String> dialogueLines() {
        return List.of(
                "丹药都在摊子上摆着，灵叶在这儿就是钱。",
                "回灵丹管够——打怪打空了灵力，含一颗就能接着打。",
                "修为丹分三档：普通的补四分之一重，完美四分之三，仙品一颗顶两重半。",
                "买丹之前先看清自己的境界：高境界的丹，低境界吃了白吃。");
    }

    /** 见面礼：两颗回灵丹（消耗品，不破坏经济）。 */
    @Override
    protected void giveFirstMeetingGift(Player player) {
        this.gift(player, new ItemStack(ModItems.PILL_HUILING.get(), 2));
        this.tell(player, "\u00a7e[丹药商人]\u00a7r 头回见面，这两颗回灵丹送你。");
    }

    /** 商人不该有攻击力：血厚一点站得住就行。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 32.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.44D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}
