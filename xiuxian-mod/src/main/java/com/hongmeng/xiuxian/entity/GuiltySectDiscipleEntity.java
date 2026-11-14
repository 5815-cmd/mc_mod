package com.hongmeng.xiuxian.entity;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.sect.SectManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * 门派执事：叛门之后被派来"清理门户"的追杀者。
 *
 * <p>他认人不认理——只追当初叛门的那个人（记在 NBT 里的 {@code HuntTarget}）。
 * 杀了他可以洗掉叛徒名号。</p>
 */
public class GuiltySectDiscipleEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> DATA_SECT =
            SynchedEntityData.defineId(GuiltySectDiscipleEntity.class, EntityDataSerializers.STRING);

    @Nullable
    private UUID huntTarget = null;

    public GuiltySectDiscipleEntity(EntityType<? extends GuiltySectDiscipleEntity> type, Level level) {
        super(type, level);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new ItemStack(Items.IRON_SWORD));
        this.setPersistenceRequired();
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SECT, "旧主");
    }

    public void setHunting(String sectName, UUID target) {
        this.entityData.set(DATA_SECT, sectName == null ? "旧主" : sectName);
        this.huntTarget = target;
        this.setCustomName(net.minecraft.network.chat.Component.literal(
                "\u00a7c" + this.entityData.get(DATA_SECT) + "执事"));
        this.setCustomNameVisible(true);
    }

    public String sectName() {
        return this.entityData.get(DATA_SECT);
    }

    @Nullable
    public UUID huntTarget() {
        return huntTarget;
    }

    /** 只追被记名的那个人。 */
    @Nullable
    private Player resolveHuntTarget() {
        if (huntTarget == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(huntTarget);
        if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            return null;
        }
        if (player.level() != this.level()) {
            return null;
        }
        return player;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(2, new HuntTargetGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // 主手始终握着剑（被缴械也会补回来）
        if (this.tickCount % 100 == 0
                && this.getMainHandItem().isEmpty()) {
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    new ItemStack(Items.IRON_SWORD));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // 掉一枚门派信物与几颗丹药
        if (this.random.nextInt(100) < 60) {
            this.spawnAtLocation(new ItemStack(
                    com.hongmeng.xiuxian.item.ModItems.PILL_HUILING.get(), 1));
        }
        if (this.random.nextInt(100) < 25) {
            this.spawnAtLocation(new ItemStack(
                    com.hongmeng.xiuxian.item.ModItems.KONG_MING_SHI.get(), 1));
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // 被记名者亲手所杀 → 洗清叛徒名号
        if (source.getEntity() instanceof ServerPlayer killer) {
            CultivationData data = CultivationCapability.get(killer);
            if (huntTarget != null && huntTarget.equals(killer.getUUID())) {
                SectManager.onHunterKilled(killer, data);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SectName", sectName());
        if (huntTarget != null) {
            tag.putUUID("HuntTarget", huntTarget);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SectName")) {
            this.entityData.set(DATA_SECT, tag.getString("SectName"));
        }
        if (tag.hasUUID("HuntTarget")) {
            this.huntTarget = tag.getUUID("HuntTarget");
        }
        if (tag.contains("SectName")) {
            this.setCustomName(net.minecraft.network.chat.Component.literal(
                    "\u00a7c" + sectName() + "执事"));
            this.setCustomNameVisible(true);
        }
    }

    /** 主动扑向被记名者。 */
    static final class HuntTargetGoal extends Goal {

        private final GuiltySectDiscipleEntity mob;
        private int cooldown = 0;

        HuntTargetGoal(GuiltySectDiscipleEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity current = mob.getTarget();
            if (current != null && current.isAlive()) {
                return false;
            }
            Player target = mob.resolveHuntTarget();
            if (target == null) {
                cooldown = 60;
                return false;
            }
            mob.setTarget(target);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive() && mob.resolveHuntTarget() != null;
        }
    }
}
