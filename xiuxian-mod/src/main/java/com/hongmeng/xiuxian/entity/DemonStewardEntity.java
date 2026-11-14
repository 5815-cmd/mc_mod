package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.Level;

/**
 * 魔道执事（精英）——魔道设在山门外的"跑腿的"。
 *
 * <p>他是人形系里最弱的一档，也是玩家第一次碰上"会追、会打、会叫人"的人形怪。
 * 与既有的血魔修士（同样是精英、同样是瘦人形）的分工是这样切的：</p>
 * <ul>
 *   <li>血魔修士——<b>远</b>：剑气能递出五格，逼你贴身；</li>
 *   <li>魔道执事——<b>近</b>：伤害低一档，但跑得快、追得远（followRange 36），
 *       而且<b>残血会喊同伙</b>。玩家对付他的正确解法是"就地解决，别拖"。</li>
 * </ul>
 *
 * <p>模型直接用既有的 {@code CultivationHumanoidModel}：瘦一圈的身板 + 发髻，
 * 不配护肩／披风／剑——"执事"是个办事的，身上不该有甲。外观差异全部落在贴图上
 * （{@code demon_steward.png}，64×64，由 {@code tools/gen_new_monster_textures.py} 生成）。</p>
 *
 * @see CultivationMonsterEntity
 * @see MobRank
 */
public class DemonStewardEntity extends CultivationMonsterEntity {

    /** 掉到这个血量比例以下就叫一次人。 */
    private static final float CALL_HELP_HEALTH_RATIO = 0.5F;

    /** 喊人的半径（格）。 */
    private static final double CALL_HELP_RADIUS = 16.0D;

    /** 叫过一次就不再叫（不落盘：读档后血量本来就满了）。 */
    private boolean calledHelp;

    public DemonStewardEntity(EntityType<? extends DemonStewardEntity> type, Level level) {
        super(type, level, 14);
    }

    @Override
    public MobRank rank() {
        return MobRank.ELITE;
    }

    /**
     * 基础属性；最终值 = 基础值 × 品阶倍率（精英 ×3 生命 / ×1.5 攻击 / ×1.05 移速）。
     *
     * <pre>
     *   生命 7  × 3.0  = 21            （比血魔修士的 27 脆，换来"叫帮手"）
     *   攻击 3  × 1.5  = 4.5
     *   移速 0.33 × 1.05 ≈ 0.347       （人形怪里最快的一档）
     *   护甲 1.0
     * </pre>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.ELITE, 7.0D, 3.0D, 0.33D, 36.0D, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.35D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 残血叫同伙：把半径内所有修仙妖兽的目标设成同一个人。
     *
     * <p>刻意只喊"自己这一类"（同一个父类的怪），不碰原版僵尸——
     * 让玩家摸清"魔道的怪会扎堆"这条规律，而不是"全世界的怪都会联动"。</p>
     *
     * <p>只喊<b>一次</b>：{@code calledHelp} 立起来之后就不再触发，
     * 否则血量在阈值上下抖动时会变成每 tick 一次的扩音器。</p>
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.calledHelp) {
            return;
        }
        if (this.getHealth() > this.getMaxHealth() * CALL_HELP_HEALTH_RATIO) {
            return;
        }
        this.calledHelp = true;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F, 1.3F);
        for (CultivationMonsterEntity ally : this.level().getEntitiesOfClass(
                CultivationMonsterEntity.class,
                this.getBoundingBox().inflate(CALL_HELP_RADIUS))) {
            if (ally != this && ally.getTarget() == null) {
                ally.setTarget(target);
            }
        }
    }

    /** 遭遇音：阴恻恻的一声。 */
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VINDICATOR_AMBIENT;
    }

    /**
     * 掉落：执事身上带着魔道的"路引"。
     *
     * <p>经验是 {@code 14 × 2.0 = 28}（精英档），掉落走 {@link MobRank} 表，
     * 只有物品这一层是本类自己定的。</p>
     */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 20) {
            return new ItemStack(ModItems.KONG_MING_SHI.get(), 1);
        }
        if (this.random.nextInt(100) < 55) {
            return new ItemStack(ModItems.LINGYE.get(), 2);
        }
        return super.rankDrop(rank);
    }
}
