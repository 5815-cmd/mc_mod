package com.hongmeng.xiuxian.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * <b>劫灰修士</b>：渡劫失败却未死透的人，被雷火淬成了灰白之物。
 *
 * <h2>定位</h2>
 * <p>真世界（肉后）的<b>标准敌人</b> —— 只在真世界开启后由
 * {@code HardmodeTerrain} 刷出，普通世界见不到。</p>
 *
 * <p>品阶是 {@link MobRank#ELITE}，所以<b>自动继承</b>了整套精英行为：</p>
 * <ul>
 *   <li>{@code MobMind}：绕后包抄、血少撤退、站定蓄力猛扑（<b>可被打断</b>）；</li>
 *   <li>{@code MobGuard}：正面格挡 + 气盾（背后打无效）；</li>
 *   <li>{@code MobDodge}：把你的攻击滚掉（完全免伤，有冷却）；</li>
 *   <li>群体协同：同区域只允许一只蓄力。</li>
 * </ul>
 *
 * <p>数值上"厚而不快"：血比血魔修士多 30%，移速略低 ——
 * 让它更像"推过来的墙"，而不是"追着你跑的快刀"。</p>
 */
public class TribulationAshCultivatorEntity extends CultivationMonsterEntity {

    public TribulationAshCultivatorEntity(EntityType<? extends TribulationAshCultivatorEntity> type, Level level) {
        super(type, level, 14);
        // 灰白的旧剑
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        // 血 34 / 攻 8.5 / 击退抗性 0.35 / 追踪 38 / 护甲 7
        return scaledAttributes(MobRank.ELITE, 34.0D, 8.5D, 0.35D, 38.0D, 7.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.05D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, true));
    }
}
