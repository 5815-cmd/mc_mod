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
 * 炼器师：守着炉子与铁砧的匠人。
 *
 * <p>本模组的装备是<b>自己炼</b>的（灵铁锭 / 灵晶 / 空冥精，见炼器链），
 * 所以这里不做"卖成品装备"的商店——那会把整条采集与合成链一次废掉。
 * 他给的是一句实话和一份<b>开炉料</b>：第一次见面送一块灵铁锭，
 * 让玩家拿去试第一次升阶。</p>
 *
 * <p>修理这件事也刻意不在这里做：原版铁砧 + 本模组的材料合成已经是修理路径，
 * 再塞一个"点一下修好"的 NPC 只会让铁砧变成摆设。他负责<b>告诉你</b>怎么做。</p>
 */
public class ArtifactRefinerEntity extends CultivationNpcEntity {

    public ArtifactRefinerEntity(EntityType<? extends ArtifactRefinerEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String npcTitle() {
        return "炼器师";
    }

    @Override
    public Panel panel() {
        return Panel.SMITH;
    }

    @Override
    public SoundEvent interactSound() {
        return SoundEvents.ANVIL_LAND;
    }

    @Override
    public List<String> dialogueLines() {
        return List.of(
                "法宝这东西，一阶一阶往上喂材料，急不来。",
                "灵铁锭管低阶，灵晶管中阶，空冥精管高阶——喂错了不认。",
                "升阶要八块料，围着中心空一格摆，摆错了炉子不接。",
                "打坏了别急着扔，铁砧上垫一块同样的料就能修回来。",
                "（他递过来一块灵铁锭）拿去，第一炉总得有人给你起个头。");
    }

    /** 见面礼：一块灵铁锭（法宝升阶的基础料）。 */
    @Override
    protected void giveFirstMeetingGift(Player player) {
        this.gift(player, new ItemStack(ModItems.SPIRIT_IRON_INGOT.get(), 1));
        this.tell(player, "\u00a7e[炼器师]\u00a7r 这块灵铁锭收着，第一炉的材料算我的。");
    }

    /** 匠人身体壮：血厚、护甲高，站着不动。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.40D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}
