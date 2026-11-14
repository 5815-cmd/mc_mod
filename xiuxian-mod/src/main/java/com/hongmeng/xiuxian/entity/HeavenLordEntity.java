package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * <b>仙庭之主（heaven_lord）</b> —— 第五维度「天外天 · 凌霄天境」的巨型首领（首领品阶）。
 *
 * <p>本批的"压迫感"招牌：模型本身就有 **110px（6.875 格）**高，再叠品阶 scaleY 1.20
 * 与 B 的巨化系统（giant 1.35）→ 游戏内约 **11.2 格**高。三眼玉面、平天冕板、
 * 背后十二旒与日轮、宽袍大袖、手持玉圭。</p>
 *
 * <p>战斗：具体攻击轮换由 A 的 {@code BossAttackKit} 接管（本类**不写技能**），
 * 这里只保留 FloatGoal + 近战/凝视的基础 AI；BOSS 条与阶段由 {@code MobRank.OVERLORD}
 * / {@code BossPhase} 自动处理。</p>
 *
 * <p>数值（契约 §1.5）：{@code scaledAttributes(OVERLORD, 70, 10, 0.26, 48, 12)}，
 * 经验 base 60；普通掉落走真元晶 / 空冥石 / 空冥精；<b>专属凭证「仙庭之冕」</b>
 * 由 {@code ModEvents.onAltarBossDeath} 在死亡时必定掉落（恶之祭坛第五材料）。</p>
 *
 * <p>模型：{@code client.HeavenLordModel}（layer {@code hongmeng:heaven_lord}，
 * 由 {@code 工程/角色设计/round4/new-mobs/heaven_lord/spec.py} 生成）。</p>
 */
public class HeavenLordEntity extends CultivationMonsterEntity {

    /** 契约 §1.5 的巨化倍率（B 的 giant 统一系统）。 */
    public static final float GIANT_SCALE = 1.35F;

    public HeavenLordEntity(EntityType<? extends HeavenLordEntity> type, Level level) {
        super(type, level, 60);
        // TODO(第四轮 B)：基类尚未落盘 DATA_GIANT_SCALE / setGiantScale(float)。
        //  契约 §2.1 的 B 系统合入后，这里改为：this.setGiantScale(GIANT_SCALE);
        //  （渲染高度 ≈ 110px/16 × rank.scaleY 1.20 × giant 1.35 ≈ 11.2 格，
        //    命中箱由基类 getDimensions 按 giantScale 同步放大）
    }

    @Override
    public MobRank rank() {
        return MobRank.OVERLORD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return scaledAttributes(MobRank.OVERLORD, 70.0D, 10.0D, 0.26D, 48.0D, 12.0D);
    }

    @Override
    protected void registerGoals() {
        // 技能（震地重击/冲击波/召唤/狂暴）由 BossAttackKit 接管，本类不写技能。
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 首领掉落：真元晶 / 空冥石 / 空冥精。
     *
     * <p>契约要求"仙金类复用现有 id"——本模组目前没有"仙金"物品，仙庭档材料用
     * {@code zhenyuan_crystal}（真元）与 {@code kong_ming_shi}/{@code kongming_essence}
     * （空冥）代替；队长起的额外空冥石仍由基类 {@code dropCustomDeathLoot} 给，不改。</p>
     */
    @Override
    protected ItemStack rankDrop(MobRank rank) {
        if (this.random.nextInt(100) < 50) {
            return new ItemStack(ModItems.ZHENYUAN_CRYSTAL.get(), 1 + this.random.nextInt(2));
        }
        if (this.random.nextInt(100) < 35) {
            return new ItemStack(ModItems.KONG_MING_SHI.get(), 1);
        }
        if (this.random.nextInt(100) < 25) {
            return new ItemStack(ModItems.KONGMING_ESSENCE.get(), 1);
        }
        return super.rankDrop(rank);
    }
}
