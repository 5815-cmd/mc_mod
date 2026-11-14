package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.dungeon.Dungeon;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 秘境向导：常年蹲在传送门边上的老江湖。
 *
 * <p>他回答的是本模组最容易迷路的一个问题：<b>四个维度分别怎么去、门在哪、几境能进</b>。</p>
 *
 * <h3>台词是从数据里生成的，不是手抄的</h3>
 *
 * <p>四条路线的名字、门槛、说明全部来自 {@link Dungeon} 这个纯数据枚举
 * （{@link Dungeon#values()}），台词里只有"门是什么方块"这一句是写死的。
 * 这样以后谁改了副本门槛（比如把妖兽秘境从三境提到四境），向导嘴里的数字
 * <b>自动跟着变</b>——不会出现"NPC 说三境、门却要四境"这种最伤信任的错。</p>
 *
 * <p>另外他还会按玩家当前境界标一句"你现在能进哪座"，
 * 这一层判断放在服务端（{@code CultivationCapability}）是安全的：
 * 这台 NPC 只在服务端算台词，客户端拿的是算好的字符串。</p>
 */
public class RealmGuideEntity extends CultivationNpcEntity {

    public RealmGuideEntity(EntityType<? extends RealmGuideEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String npcTitle() {
        return "秘境向导";
    }

    @Override
    public Panel panel() {
        return Panel.GUIDE;
    }

    @Override
    public SoundEvent interactSound() {
        return SoundEvents.VILLAGER_YES;
    }

    /**
     * 门的位置与材料（写死的只有这一层：方块归建筑管，不归副本表管）。
     *
     * <p>四座门分别由 {@code BeastPortalFrameBlock} / {@code DemonPortalFrameBlock} /
     * {@code VoidPortalFrameBlock} 与纪元祭坛构成，全部用手里的材料在野外搭出来——
     * 向导的职责是把这件事说明白，而不是替玩家搭。</p>
     */
    private static final String[] PORTAL_HINT = {
            "门在野外自己搭：一阶的纪元祭坛最容易，四块石头围一圈就行。",
            "妖兽之门要妖兽框架石，魔道之门要魔道框架石——都在高阶矿脉里挖。",
            "虚空之门最贵：虚空框架石只在空冥矿上出，八境之前别费那个劲。",
            "门搭好站进去就行，掉出来的碎片别丢，那是开门的引子。"
    };

    /** 台词的"路线"部分：逐条从 {@link Dungeon} 读，门槛变了台词就变。 */
    private static List<String> routeLines() {
        List<String> out = new ArrayList<>();
        int i = 0;
        for (Dungeon d : Dungeon.values()) {
            out.add("\u00a7" + d.colorCode() + "【" + d.cn() + "\u00a7r\u00a7f】"
                    + "大境 " + d.minTier() + " 境可入，共 " + d.waves() + " 波。"
                    + d.desc() + "。");
            if (i < PORTAL_HINT.length) {
                out.add("    " + PORTAL_HINT[i]);
            }
            i++;
        }
        return out;
    }

    /**
     * 面板正文：路线 + 一句"你现在能进哪座"。
     *
     * <p>{@link #playerTierHint()} 在服务端取玩家境界；取不到（例如客户端侧的
     * 兜底调用）就退化成"从最低的那座开始"。</p>
     */
    @Override
    public List<String> dialogueLines() {
        List<String> lines = new ArrayList<>();
        lines.add("要进秘境，先认路。这里四条路，我一条一条给你说。");
        lines.add("");
        lines.addAll(routeLines());
        lines.add("");
        lines.add("\u00a76【你现在的脚程】\u00a7r" + playerTierHint());
        return lines;
    }

    /** 按玩家境界给一句"能进 / 还不能进"。 */
    private String playerTierHint() {
        int tier = 1;
        Player nearest = this.level().getNearestPlayer(this, 16.0D);
        if (nearest instanceof net.minecraft.server.level.ServerPlayer sp) {
            tier = com.hongmeng.xiuxian.core.CultivationCapability.get(sp).realm().tier();
        }
        List<Dungeon> open = Dungeon.availableAt(tier);
        if (open.isEmpty()) {
            return "你现在一座都进不去——先去把境界提上来。";
        }
        Dungeon best = open.get(open.size() - 1);
        return "以你现在的境界，\u00a7a" + best.cn() + "\u00a7r\u00a7f 及以下的都能进；"
                + "再往上就得先突破了。";
    }

    /** 见面礼：三片灵叶——路上买丹药用的盘缠。 */
    @Override
    protected void giveFirstMeetingGift(Player player) {
        this.gift(player, new ItemStack(ModItems.LINGYE.get(), 3));
        this.tell(player, "\u00a7e[秘境向导]\u00a7r 这点灵叶拿着当盘缠，路上别乱花。");
    }

    /** 常年跑野外的：血厚、跑得快。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.48D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}
