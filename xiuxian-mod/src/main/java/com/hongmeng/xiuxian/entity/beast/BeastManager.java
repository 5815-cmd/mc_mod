package com.hongmeng.xiuxian.entity.beast;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.entity.ModEntities;
import com.hongmeng.xiuxian.entity.ThunderEagleEntity;
import com.hongmeng.xiuxian.entity.VoidSerpentEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 灵兽系统的服务端总控：<b>契约、灵兽袋、护甲、成长、灵力消耗</b>。
 *
 * <h3>为什么所有的物品查找都包在 try/catch 里</h3>
 *
 * <p>规格第五节第 5 条：<b>离线验证器不能碰注册表</b>——一旦某个类在静态初始化里
 * 触碰 {@code Items}/{@code ItemStack}，验证器就会以「Not bootstrapped」炸掉，
 * 而且炸的是<b>别的</b>测试，极难排查。</p>
 *
 * <p>本模组已经把这条约束写成了习惯：纯数值（{@link BeastContract}、
 * {@link BeastKind}）与物品查找严格分开。物品查找集中在这里的
 * {@link #itemByPath(String)}，它只做两件事：查 {@link ForgeRegistries#ITEMS}、
 * 失败返回 {@code null}。调用点全部判空，于是<b>"注册表还没起来"这件事
 * 退化成"物品找不到"，而不是抛异常</b>。</p>
 *
 * <p>另外这些 {@code RegistryObject.get()} 调用全部发生在<b>运行期</b>
 * （右键、tick），不在 mod 构造阶段——规格第五节第 1 条针对的是后者。</p>
 */
public final class BeastManager {

    /** 灵兽袋最多能装几只（防止一个袋子当仓库用）。 */
    public static final int POUCH_CAPACITY = 4;

    /** 灵兽袋里那一串 NBT 的键名。 */
    public static final String POUCH_LIST = "HongmengBeasts";

    /** 单只灵兽在袋子里占的 NBT 键。 */
    public static final String POUCH_ENTRY = "Beast";

    /** 骑乘飞行灵兽时，灵力回复的例外：主人还在飞就不回蓝。 */
    private BeastManager() {
    }

    // ------------------------------------------------------------------
    // 注册表查找（唯一的入口，绝不外抛异常）
    // ------------------------------------------------------------------

    /** 按注册路径找物品；找不到返回 null（注册表没起来也走这一条）。 */
    public static Item itemByPath(String path) {
        try {
            return ForgeRegistries.ITEMS.getValue(
                    net.minecraft.resources.ResourceLocation.tryBuild(
                            com.hongmeng.xiuxian.HongmengMod.MOD_ID, path));
        } catch (Throwable t) {
            // 注册表未引导 / 物品不存在：一律当成"没有这个物品"
            return null;
        }
    }

    public static Item talismanItem() {
        return itemByPath("contract_talisman");
    }

    public static Item pouchItem() {
        return itemByPath("beast_pouch");
    }

    public static Item beastArmorItem() {
        return itemByPath("beast_armor");
    }

    /** 妖丹（成长用）——沿用既有的 {@code jidan} 物品，不新造一个。 */
    public static Item growthItem() {
        return itemByPath("jidan");
    }

    public static boolean isPouch(ItemStack stack) {
        Item pouch = pouchItem();
        return pouch != null && !stack.isEmpty() && stack.is(pouch);
    }

    public static boolean isBeastArmor(ItemStack stack) {
        Item armor = beastArmorItem();
        return armor != null && !stack.isEmpty() && stack.is(armor);
    }

    public static boolean isTalisman(ItemStack stack) {
        Item talisman = talismanItem();
        return talisman != null && !stack.isEmpty() && stack.is(talisman);
    }

    public static boolean isGrowthItem(ItemStack stack) {
        Item growth = growthItem();
        return growth != null && !stack.isEmpty() && stack.is(growth);
    }

    // ------------------------------------------------------------------
    // 实体构造
    // ------------------------------------------------------------------

    /** 按种类造一只灵兽（未契约、未进世界）。 */
    public static SpiritBeastEntity createBeast(BeastKind kind, ServerLevel level) {
        if (kind == null || level == null) {
            return null;
        }
        return switch (kind) {
            case CONTRACT_EAGLE ->
                    new ContractEagleEntity(ModEntities.CONTRACT_EAGLE.get(), level);
            case JIAOLONG -> new JiaolongEntity(ModEntities.JIAOLONG.get(), level);
            case CONTRACT_WOLF ->
                    new ContractWolfEntity(ModEntities.CONTRACT_WOLF.get(), level);
        };
    }

    /** 蛟龙之息；实体类型没注册好时返回 null（蛟龙那边判空跳过喷吐）。 */
    public static JiaolongSpitEntity createSpit(LivingEntity shooter) {
        if (!(shooter.level() instanceof ServerLevel level)) {
            return null;
        }
        try {
            return new JiaolongSpitEntity(ModEntities.JIAOLONG_SPIT.get(), level, shooter);
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 契约
    // ------------------------------------------------------------------

    /** 一只妖兽能被契约成哪种灵兽；不能则返回 null。 */
    public static BeastKind contractTargetOf(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof SpiritBeastEntity) {
            return null;   // 已经是灵兽了，不该再走"收服野生妖兽"这条路
        }
        if (entity instanceof ThunderEagleEntity) {
            return BeastKind.CONTRACT_EAGLE;
        }
        if (entity instanceof VoidSerpentEntity) {
            return BeastKind.JIAOLONG;
        }
        if (entity instanceof com.hongmeng.xiuxian.entity.SpiritWolfEntity) {
            return BeastKind.CONTRACT_WOLF;
        }
        return null;
    }

    /**
     * 契约交互的完整流程（潜行右键已在调用方排除）。
     *
     * @return true 表示这次交互已经被灵兽系统接手，调用方要取消原版行为
     */
    public static boolean tryContract(ServerPlayer player, LivingEntity target,
                                      ItemStack stack) {
        if (!isTalisman(stack)) {
            return false;
        }
        BeastKind kind = contractTargetOf(target);
        if (kind == null) {
            return false;
        }

        // 门槛一：必须打到虚弱（规格：<25% 生命）
        if (!BeastContract.isWeakened(target.getHealth(), target.getMaxHealth())) {
            player.displayClientMessage(Component.literal(
                    "\u00a77妖兽仍有余力\u00a78（" + Math.round(target.getHealth()) + " / "
                            + Math.round(target.getMaxHealth()) + "\uff09\u00a77——"
                            + "把它打到 \u00a7c25%\u00a77 以下再上契灵符。"), true);
            return true;
        }

        // 门槛二：境界。不够格时**不消耗符纸**——这是"准备不足"不是"运气不好"
        CultivationData data = CultivationCapability.get(player);
        int ownerTier = data.realm().tier();
        int targetTier = targetTier(target);
        if (!BeastContract.realmAllows(ownerTier, targetTier, kind)) {
            int need = BeastContract.realmShortfall(ownerTier, targetTier, kind);
            player.displayClientMessage(Component.literal(
                    "\u00a7c契灵符毫无反应\u00a77——你的境界压不住它（还差 \u00a7e"
                            + need + " \u00a77个大境）。"), true);
            return true;
        }

        // 结算成功率
        double chance = BeastContract.successChance(ownerTier, targetTier,
                rankOrdinal(target), data.getRoot() == null ? null : data.getRoot().rarity(),
                kind);
        boolean success = player.getRandom().nextDouble() < chance;

        // 无论成败，符纸都消耗（规格明写）
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        if (!success) {
            // 失败：激怒目标
            if (target instanceof Mob mob) {
                mob.setTarget(player);
                mob.setLastHurtByMob(player);
            }
            player.displayClientMessage(Component.literal(
                    "\u00a7c\u5951\u7075\u7b26\u5316\u4f5c\u98de\u7070\u00a77\uff08\u6210\u529f\u7387 "
                            + BeastContract.describeChance(chance) + "\uff09\u00a77——"
                            + "它被激怒了！"), true);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SMOKE,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                        24, 0.4D, 0.5D, 0.4D, 0.03D);
            }
            target.playSound(SoundEvents.WITHER_HURT, 0.8F, 0.7F);
            return true;
        }

        // 成功：原地换成一只幼年灵兽，继承当前血量比例
        if (!(player.level() instanceof ServerLevel level)) {
            return true;
        }
        SpiritBeastEntity beast = createBeast(kind, level);
        if (beast == null) {
            return true;
        }
        beast.moveTo(target.getX(), target.getY(), target.getZ(),
                target.getYRot(), 0.0F);
        beast.onContracted(player);
        float ratio = target.getMaxHealth() <= 0.0F ? 1.0F
                : target.getHealth() / target.getMaxHealth();
        beast.setHealth(Math.max(1.0F, beast.getMaxHealth() * Math.max(0.25F, ratio)));
        level.addFreshEntity(beast);
        target.discard();

        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                beast.getX(), beast.getY() + beast.getBbHeight() * 0.5D, beast.getZ(),
                30, 0.6D, 0.6D, 0.6D, 0.05D);
        level.sendParticles(ParticleTypes.END_ROD,
                beast.getX(), beast.getY() + beast.getBbHeight() * 0.5D, beast.getZ(),
                18, 0.4D, 0.4D, 0.4D, 0.02D);

        player.displayClientMessage(Component.literal(
                "\u00a7b\u00a7l\u5951\u7ea6\u6210\u7acb\u00a77\uff01\u00a7f" + kind.cn()
                        + " \u00a77进入 \u00a78" + BeastContract.STARTING_GROWTH.colored()
                        + " \u00a77阶段\u00a78\uff08\u6210\u529f\u7387 "
                        + BeastContract.describeChance(chance) + "\uff09"), false);
        player.sendSystemMessage(Component.literal(
                "\u00a77\u300c\u5b83\u4f0f\u4e86\u3002\u4ece\u4eca\u4ee5\u540e\uff0c\u5b83\u7684\u547d\u5c31\u7cfb\u5728\u4f60\u8eab\u4e0a\u3002\u300d"));
        return true;
    }

    /** 契约对象的大境序号：妖兽按品阶反推，其它按 1（练气）算。 */
    private static int targetTier(LivingEntity target) {
        if (target instanceof com.hongmeng.xiuxian.entity.CultivationMonsterEntity monster) {
            // 品阶越高，"相当于第几境"越高：普通 2、精英 3、队长 4、头目 5、首领 6
            return 2 + Math.max(0, monster.rank().ordinal());
        }
        return 1;
    }

    private static int rankOrdinal(LivingEntity target) {
        if (target instanceof com.hongmeng.xiuxian.entity.CultivationMonsterEntity monster) {
            return monster.rank().ordinal();
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // 灵兽袋
    // ------------------------------------------------------------------

    /** 袋子里已经装了几只。 */
    public static int storedCount(ItemStack pouch) {
        if (pouch == null || pouch.isEmpty() || !pouch.hasTag()) {
            return 0;
        }
        return pouch.getTag().getList(POUCH_LIST, CompoundTag.TAG_COMPOUND).size();
    }

    /** 取第 index 只的 NBT（拷贝），越界返回 null。 */
    public static CompoundTag storedBeast(ItemStack pouch, int index) {
        if (pouch == null || pouch.isEmpty() || !pouch.hasTag()) {
            return null;
        }
        ListTag list = pouch.getTag().getList(POUCH_LIST, CompoundTag.TAG_COMPOUND);
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.getCompound(index).copy();
    }

    /**
     * 把一只灵兽收进袋子（完整 NBT）。
     *
     * @return true 表示收成功
     */
    public static boolean storeBeast(ItemStack pouch, SpiritBeastEntity beast,
                                     ServerPlayer owner) {
        if (pouch == null || pouch.isEmpty() || beast == null) {
            return false;
        }
        if (storedCount(pouch) >= POUCH_CAPACITY) {
            owner.displayClientMessage(Component.literal(
                    "\u00a7c\u7075\u517d\u888b\u5df2\u6ee1\u00a77\uff08" + POUCH_CAPACITY
                            + " / " + POUCH_CAPACITY + "\uff09\u2014\u2014\u5148\u53d6\u4e00\u53ea\u51fa\u6765\u3002"),
                    true);
            return false;
        }

        CompoundTag tag = new CompoundTag();
        // 把实体完整写进 NBT（UUID 也写，放出来还是同一只）
        beast.saveWithoutId(tag);
        // 乘客里可能还有骑手，绝不能连人一起塞进袋子
        tag.remove("Passengers");
        tag.remove("Leash");
        tag.putString("id", beastKindId(beast));

        CompoundTag wrapper = new CompoundTag();
        wrapper.put(POUCH_ENTRY, tag);
        ListTag list = pouch.getOrCreateTag().getList(POUCH_LIST,
                CompoundTag.TAG_COMPOUND);
        list.add(wrapper);
        pouch.getOrCreateTag().put(POUCH_LIST, list);

        beast.ejectPassengers();
        beast.discard();
        if (owner.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL,
                    beast.getX(), beast.getY() + beast.getBbHeight() * 0.5D, beast.getZ(),
                    20, 0.3D, 0.4D, 0.3D, 0.05D);
        }
        owner.displayClientMessage(Component.literal(
                "\u00a7b" + beast.kind().cn() + " \u00a77\u5df2\u6536\u5165\u7075\u517d\u888b\u00a78\uff08"
                        + storedCount(pouch) + " / " + POUCH_CAPACITY + "\uff09"), true);
        return true;
    }

    /** 野外实体注册名（用来给袋子里的 NBT 标记类型）。 */
    private static String beastKindId(SpiritBeastEntity beast) {
        return com.hongmeng.xiuxian.HongmengMod.MOD_ID + ":" + beast.kind().id();
    }

    /**
     * 从袋子里放出最后一只。
     *
     * <p>放出来的位置是主人面前一格半处；如果那里是墙就落在脚下——
     * "放出来卡进方块里"是收纳系统最经典的事故。</p>
     */
    public static boolean releaseLast(ItemStack pouch, ServerPlayer owner) {
        int count = storedCount(pouch);
        if (count <= 0) {
            owner.displayClientMessage(Component.literal(
                    "\u00a77\u7075\u517d\u888b\u662f\u7a7a\u7684\u3002"), true);
            return false;
        }
        CompoundTag wrapper = storedBeast(pouch, count - 1);
        if (wrapper == null || !(owner.level() instanceof ServerLevel level)) {
            return false;
        }
        CompoundTag tag = wrapper.getCompound(POUCH_ENTRY);
        SpiritBeastEntity beast = spawnFromTag(tag, level);
        if (beast == null) {
            owner.displayClientMessage(Component.literal(
                    "\u00a7c\u91ca\u653e\u5931\u8d25\u00a77\uff08\u6570\u636e\u635f\u574f\uff09\u3002"), true);
            return false;
        }

        Vec3 look = owner.getLookAngle();
        Vec3 spot = new Vec3(owner.getX() + look.x * 1.6D,
                owner.getY() + 0.2D,
                owner.getZ() + look.z * 1.6D);
        if (!level.noCollision(beast, beast.getBoundingBox().move(spot))) {
            spot = owner.position();
        }
        beast.moveTo(spot.x, spot.y, spot.z, owner.getYRot(), 0.0F);
        if (!beast.isTame()) {
            beast.onContracted(owner);
        }
        level.addFreshEntity(beast);
        level.sendParticles(ParticleTypes.CLOUD,
                beast.getX(), beast.getY() + 0.3D, beast.getZ(),
                18, 0.4D, 0.3D, 0.4D, 0.04D);

        // 从袋子里删掉这一条
        ListTag list = pouch.getOrCreateTag().getList(POUCH_LIST,
                CompoundTag.TAG_COMPOUND);
        list.remove(count - 1);
        pouch.getOrCreateTag().put(POUCH_LIST, list);

        owner.displayClientMessage(Component.literal(
                "\u00a7b" + beast.kind().cn() + " \u00a77\u5df2\u653e\u51fa\u00a78\uff08\u5269 "
                        + storedCount(pouch) + " \u53ea\uff09"), true);
        return true;
    }

    /** 按 NBT 造回一只灵兽（灵兽袋与调试都用它）。 */
    public static SpiritBeastEntity spawnFromTag(CompoundTag tag, ServerLevel level) {
        BeastKind kind = kindFromTag(tag);
        SpiritBeastEntity beast = createBeast(kind, level);
        if (beast == null) {
            return null;
        }
        try {
            beast.load(tag);
        } catch (Throwable t) {
            // 老版本存档 / 手改过的 NBT：造一只全新的同类，不让它把玩家卡住
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[hongmeng] 灵兽数据读取失败，已按新个体放出：{}", t.toString());
        }
        return beast;
    }

    /** 从袋子里的 NBT 猜种类。 */
    public static BeastKind kindFromTag(CompoundTag tag) {
        String id = tag == null ? "" : tag.getString("id");
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return BeastKind.byId(path);
    }

    // ------------------------------------------------------------------
    // 指令 / 护甲 / 成长
    // ------------------------------------------------------------------

    /** 潜行右键：循环 跟随 → 停留 → 游荡。 */
    public static void cycleCommand(ServerPlayer player, SpiritBeastEntity beast) {
        BeastCommand next = beast.command().next();
        beast.setCommand(next);
        player.displayClientMessage(Component.literal(
                "\u00a77" + beast.kind().cn() + " \u00a77\u6307\u4ee4 \u2192 " + next.colored()
                        + " \u00a78\uff08\u6f5c\u884c\u53f3\u952e\u5faa\u73af\uff09"), true);
        beast.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.5F);
    }

    /** 给灵兽穿上/脱下灵兽护甲。 */
    public static boolean toggleArmor(ServerPlayer player, SpiritBeastEntity beast,
                                      ItemStack stack) {
        if (!isBeastArmor(stack)) {
            return false;
        }
        if (beast.hasBeastArmor()) {
            // 已经穿了：脱下来还给玩家
            beast.setBeastArmor(false);
            ItemStack back = new ItemStack(beastArmorItem());
            if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
            player.displayClientMessage(Component.literal(
                    "\u00a77\u5df2\u5378\u4e0b\u7075\u517d\u62a4\u7532\u3002"), true);
        } else {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            beast.setBeastArmor(true);
            player.displayClientMessage(Component.literal(
                    "\u00a7b\u7075\u517d\u62a4\u7532\u5df2\u88c5\u5907\u00a77\uff08\u62a4\u7532 +"
                            + (int) SpiritBeastEntity.BEAST_ARMOR_VALUE + "\uff09"), true);
        }
        beast.playSound(SoundEvents.ARMOR_EQUIP_IRON, 0.9F, 1.1F);
        return true;
    }

    /**
     * 喂妖丹：把灵兽往下一阶段推。
     *
     * <p>每次消耗一颗，攒够 {@link BeastGrowth#feedCost()} 颗就升阶。
     * 妖丹在原版里只是掉落物，这里给它唯一的正经用途。</p>
     */
    public static boolean feedGrowth(ServerPlayer player, SpiritBeastEntity beast,
                                     ItemStack stack) {
        if (!isGrowthItem(stack)) {
            return false;
        }
        BeastGrowth growth = beast.growth();
        if (!growth.canGrow()) {
            player.displayClientMessage(Component.literal(
                    "\u00a77\u5b83\u5df2\u7ecf\u662f \u00a76\u5316\u5f62 \u00a77\u4e86\uff0c\u65e0\u6cd5\u518d\u8fdb\u3002"),
                    true);
            return true;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        int fed = beast.getPersistentData().getInt("HmBeastFed") + 1;
        if (fed >= growth.feedCost()) {
            fed = 0;
            BeastGrowth next = growth.next();
            beast.setGrowth(next);
            beast.getPersistentData().putInt("HmBeastFed", 0);
            beast.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.2F);
            if (beast.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        beast.getX(), beast.getY() + beast.getBbHeight() * 0.5D, beast.getZ(),
                        40, 0.6D, 0.7D, 0.6D, 0.1D);
            }
            player.displayClientMessage(Component.literal(
                    "\u00a76\u00a7l\u8fdb\u9636\u00a77\uff01" + beast.kind().cn() + " \u2192 "
                            + next.colored()), false);
        } else {
            beast.getPersistentData().putInt("HmBeastFed", fed);
            beast.playSound(SoundEvents.GENERIC_EAT, 0.8F, 1.2F);
            if (beast.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        beast.getX(), beast.getY() + beast.getBbHeight() * 0.6D, beast.getZ(),
                        8, 0.3D, 0.3D, 0.3D, 0.02D);
            }
            player.displayClientMessage(Component.literal(
                    "\u00a77\u5582\u4e0b\u5996\u4e39\u00a78\uff08" + fed + " / "
                            + growth.feedCost() + "\uff09"), true);
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 灵力：飞行的代价
    // ------------------------------------------------------------------

    /** 玩家骑的如果是飞行灵兽，返回那一只；否则 null。骑乘关闭时恒 null。 */
    public static SpiritBeastEntity flyingMountOf(ServerPlayer player) {
        if (!BeastRiding.ENABLED) {
            return null;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof SpiritBeastEntity beast && beast.kind().flying()) {
            return beast;
        }
        return null;
    }

    /**
     * 主人在骑飞行灵兽时，每 tick 的灵力结算。
     *
     * <p><b>和御剑走同一口灵力池</b>（{@link com.hongmeng.xiuxian.core.ManaTable}）——
     * 这是规格第四节的硬要求：坐骑与御剑必须构成取舍，而不是两套独立的能量。</p>
     *
     * <p>悬停（雷鹰）消耗减半；灵力见底时把主人"请"下坐骑并给一句台词，
     * 而不是让他继续白飞。</p>
     *
     * @return true 表示这一 tick 确实在骑飞行灵兽（调用方据此跳过灵力回复）
     */
    public static boolean tickFlightDrain(ServerPlayer player, CultivationData data) {
        if (!BeastRiding.ENABLED) {
            return false;
        }
        SpiritBeastEntity mount = flyingMountOf(player);
        if (mount == null) {
            return false;
        }

        boolean hovering = mount.hoverCapable() && mount.airborne()
                && !player.isShiftKeyDown() && player.zza == 0.0F && player.xxa == 0.0F;
        double drain = hovering ? mount.kind().hoverManaDrain() : mount.kind().manaDrain();
        data.spendMana(drain);

        if (data.isManaEmpty()) {
            player.stopRiding();
            player.displayClientMessage(Component.literal(
                    "\u00a7c\u7075\u529b\u8017\u5c3d\u00a77\u2014\u2014" + mount.kind().cn()
                            + "\u627f\u4e0d\u4f4f\u4e86\uff0c\u81ea\u5df1\u843d\u4e86\u4e0b\u6765\u3002"), true);
            mount.setDeltaMovement(mount.getDeltaMovement().x, -0.15D,
                    mount.getDeltaMovement().z);
            com.hongmeng.xiuxian.network.ManaSyncPacket.send(player, data.getMana());
            return true;
        }

        // 每 10 tick 推一次灵力条（与御剑同节奏）
        if (player.tickCount % 10 == 0) {
            com.hongmeng.xiuxian.network.ManaSyncPacket.send(player, data.getMana());
        }
        return true;
    }

    /** 现在有没有骑飞行灵兽（供灵力回复判定）。骑乘关闭时恒 false。 */
    public static boolean ridingFlyingBeast(Player player) {
        return BeastRiding.ENABLED
                && player != null && player.getVehicle() instanceof SpiritBeastEntity beast
                && beast.kind().flying();
    }

    // ------------------------------------------------------------------
    // 跳跃输入
    //
    // 骑乘时的"跳跃/上升"必须由客户端主动上报：玩家是乘客，他自己跳不起来，
    // 服务端也就读不到任何跳跃信号（只有 Shift 那一路会在
    // ServerboundPlayerInputPacket 里同步过来）。
    //
    // 上报走的是**既有的命令通道**（`/hongmeng beastjump`），不是新网络包——
    // 规格明确要求不得改动 ModNetwork 的协议 id 顺序，而突破键早就在用
    // "客户端发命令"这条路（见 ClientSetup 里 BREAKTHROUGH_KEY 的处理）。
    // ------------------------------------------------------------------

    /** 跳跃请求的有效期（tick）：太短会因为网络抖动丢输入，太长会连跳。 */
    public static final int JUMP_REQUEST_WINDOW = 4;

    /** 玩家身上的"上次跳跃请求 tick"标记。 */
    private static final String PD_JUMP_TICK = "hongmeng_beast_jump_tick";

    /** 客户端按了一下空格时由命令调用。骑乘关闭时不接受。 */
    public static void requestJump(ServerPlayer player) {
        if (!BeastRiding.ENABLED || player == null) {
            return;
        }
        if (!(player.getVehicle() instanceof SpiritBeastEntity beast)) {
            return;
        }
        player.getPersistentData().putInt(PD_JUMP_TICK, player.tickCount);
        // 顺带在服务端立刻给一次信号：命令与实体 tick 在同一 tick 内时
        // 这条路径比"等下一 tick 读标记"更跟手
        beast.onPlayerJumpRequest(1);
    }

    /**
     * 读走玩家身上的跳跃请求。
     *
     * <p>这条路径是给"命令晚于实体 tick"的时序兜底的：命令在玩家 tick 里执行，
     * 而实体的 {@code aiStep} 可能已经跑过了，那一帧的上升就会丢。
     * 用 tick 计数当窗口，两边都不会漏。</p>
     *
     * @return true 表示这一 tick 有跳跃请求（已被消费）
     */
    public static boolean consumeJumpRequest(ServerPlayer player, SpiritBeastEntity beast) {
        if (!BeastRiding.ENABLED || player == null || beast == null) {
            return false;
        }
        int last = player.getPersistentData().getInt(PD_JUMP_TICK);
        if (last == 0) {
            return false;
        }
        int age = player.tickCount - last;
        if (age < 0 || age > JUMP_REQUEST_WINDOW) {
            return false;
        }
        beast.requestJump(1);
        // 消费掉，避免窗口期内被读很多次变成连升
        player.getPersistentData().putInt(PD_JUMP_TICK, 0);
        return true;
    }

    /** 玩家掉线/死亡时清掉灵兽相关的运行期状态（目前只有袋子的提示，留作扩展点）。 */
    public static void forget(Player player) {
        // 灵兽的状态都在实体自己身上，玩家侧没有需要清的缓存。
        // 保留这个方法是刻意的：以后加"跟随中的灵兽索引"时只有一个落点。
    }

    /** 检测一个实体类型是不是属于灵兽系统（给调试命令用）。 */
    public static boolean isBeastType(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        try {
        // 地面坐骑（灵驹）已按玩家要求删除，不再有这一类
        return false
                    || type == ModEntities.CONTRACT_EAGLE.get()
                    || type == ModEntities.JIAOLONG.get()
                    || type == ModEntities.CONTRACT_WOLF.get();
        } catch (Throwable t) {
            return false;
        }
    }
}
