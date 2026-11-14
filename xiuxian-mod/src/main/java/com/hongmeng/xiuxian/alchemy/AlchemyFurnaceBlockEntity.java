package com.hongmeng.xiuxian.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 丹炉的方块实体：主料 / 辅料 / 燃料 / 产物 四格 + 炼制进度。
 *
 * <p>逻辑就是"熔炉那一套"的炼丹版：有燃料就烧、有方子就推进度、
 * 进度到 200 tick 就把主料与辅料各扣 1 份、把丹药推进产物槽。
 * 差别在于**产物由"主料 + 辅料"的组合决定**，而不是单料熔炼。</p>
 *
 * <p>客户端要看到进度条，所以进度/燃料用 {@link ContainerData} 同步
 * （和原版熔炉同一个机制，不需要自己发包）。</p>
 */
public class AlchemyFurnaceBlockEntity extends BaseContainerBlockEntity {

    public static final int SLOT_MAIN = 0;
    public static final int SLOT_AUX = 1;
    public static final int SLOT_FUEL = 2;
    public static final int SLOT_RESULT = 3;
    public static final int SIZE = 4;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_BREW_TOTAL = 1;
    public static final int DATA_FUEL = 2;
    public static final int DATA_FUEL_TOTAL = 3;
    public static final int DATA_COUNT = 4;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    private int progress;
    private int brewTotal = AlchemyRecipes.BREW_TICKS;
    private int fuel;
    private int fuelTotal;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_BREW_TOTAL -> brewTotal;
                case DATA_FUEL -> fuel;
                case DATA_FUEL_TOTAL -> fuelTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_BREW_TOTAL -> brewTotal = value;
                case DATA_FUEL -> fuel = value;
                case DATA_FUEL_TOTAL -> fuelTotal = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AlchemyFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModAlchemy.ALCHEMY_FURNACE_BE.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Container
    // ------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            // 出锅品质：按主料/辅料的灵植品阶掷骰（六阶仍可能出锅凡品）。
        //
        // 必须放在**产物槽写好之后**——它要对产物 ItemStack 写 NBT。
        // 规则在 quality/AlchemyQuality 里，是纯数据、可离线断言。
        com.hongmeng.xiuxian.quality.ItemQuality.applyAlchemy(
                items.get(SLOT_RESULT),
                com.hongmeng.xiuxian.quality.AlchemyQuality.herbTierOf(
                        itemId(items.get(SLOT_MAIN))),
                com.hongmeng.xiuxian.quality.AlchemyQuality.herbTierOf(
                        itemId(items.get(SLOT_AUX))),
                java.util.concurrent.ThreadLocalRandom.current());

        setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    /** 产物槽只出不进；燃料槽只收燃料；主料/辅料槽不收燃料。 */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_RESULT) {
            return false;
        }
        boolean isFuel = AlchemyRecipes.isFuel(itemId(stack));
        if (slot == SLOT_FUEL) {
            return isFuel;
        }
        return !isFuel;
    }

    @Override
    public void clearContent() {
        // 逐个清空而不是 items.clear()：NonNullList 的 clear 语义在各版本间变过，
        // 这里要的是"四格都变成空栈"，写死最稳。
        for (int i = 0; i < SIZE; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hongmeng.alchemy_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new AlchemyFurnaceMenu(id, inventory, this, this.dataAccess,
                level == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(level, worldPosition));
    }

    // ------------------------------------------------------------------
    // 存读档
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Progress", progress);
        tag.putInt("BrewTotal", brewTotal);
        tag.putInt("Fuel", fuel);
        tag.putInt("FuelTotal", fuelTotal);
        // 雷劫动画状态：区块卸载时不保存的话，玩家回来会看到"雷劈了一半就没了"
        tag.putInt("TribulationTicks", tribulationTicks);
        if (tribulationBrew != null) {
            tag.putString("TribulationResult", tribulationBrew.result());
            tag.putInt("TribulationCount", tribulationBrew.count());
            tag.putInt("TribulationTier", tribulationBrew.tier());
        }
        // 调试字段（0 = 正常随机）。只有 /data merge 能写它。
        tag.putInt("TribulationDebug", tribulationDebugOutcome);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        progress = tag.getInt("Progress");
        brewTotal = tag.getInt("BrewTotal") > 0 ? tag.getInt("BrewTotal") : AlchemyRecipes.BREW_TICKS;
        fuel = tag.getInt("Fuel");
        fuelTotal = tag.getInt("FuelTotal");
        tribulationTicks = tag.getInt("TribulationTicks");
        tribulationDebugOutcome = tag.getInt("TribulationDebug");
        if (tag.contains("TribulationResult")) {
            // 按产物名回查丹方：丹方表是纯数据，不需要额外存一份
            String result = tag.getString("TribulationResult");
            tribulationBrew = null;
            for (AlchemyRecipes.Brew b : AlchemyRecipes.BREWS) {
                if (b.result().equals(result) && b.tier() == tag.getInt("TribulationTier")) {
                    tribulationBrew = b;
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 炼丹
    // ------------------------------------------------------------------

    public ContainerData dataAccess() {
        return dataAccess;
    }

    /** 当前主料 + 辅料命中的丹方；没有就是 {@code null}。 */
    public AlchemyRecipes.Brew currentBrew() {
        return AlchemyRecipes.find(itemId(items.get(SLOT_MAIN)), itemId(items.get(SLOT_AUX)));
    }

    /** 产物槽放得下这一炉的产出吗。 */
    public boolean canAccept(AlchemyRecipes.Brew brew) {
        Item result = resolveItem(brew.result());
        if (result == Items.AIR) {
            return false;
        }
        ItemStack out = items.get(SLOT_RESULT);
        if (out.isEmpty()) {
            return brew.count() <= new ItemStack(result).getMaxStackSize();
        }
        return out.is(result) && out.getCount() + brew.count() <= out.getMaxStackSize();
    }

    /** 服务端每 tick 跑一次。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyFurnaceBlockEntity furnace) {
        boolean changed = false;
        boolean wasLit = state.getValue(AlchemyFurnaceBlock.LIT);

        // 雷劫动画优先：正在挨雷的时候不再开新的一炉（丹药还在天上飞）
        if (furnace.tribulationTicks > 0) {
            furnace.tickTribulation(level, pos);
            // 动画期间照样烧燃料：不然"看两秒雷"等于白送两秒燃料
            if (furnace.fuel > 0) {
                furnace.fuel--;
            }
            setChanged(level, pos, state);
            boolean litWhileStorm = furnace.fuel > 0;
            if (wasLit != litWhileStorm) {
                level.setBlock(pos, state.setValue(AlchemyFurnaceBlock.LIT, litWhileStorm), 3);
            }
            return;
        }

        if (furnace.fuel > 0) {
            furnace.fuel--;
            changed = true;
        }

        AlchemyRecipes.Brew brew = furnace.currentBrew();
        boolean brewing = brew != null && furnace.canAccept(brew);

        if (brewing) {
            if (furnace.fuel <= 0 && furnace.consumeFuel()) {
                changed = true;
            }
            if (furnace.fuel > 0) {
                furnace.brewTotal = brew.brewTicks();
                furnace.progress++;
                if (furnace.progress >= furnace.brewTotal) {
                    furnace.finishBrew(level, pos, brew);
                    changed = true;
                }
            } else if (furnace.progress > 0) {
                // 没燃料了：进度归零（和原版熔炉一致，已经烧掉的燃料不退）
                furnace.progress = 0;
                changed = true;
            }
        } else if (furnace.progress > 0) {
            furnace.progress = 0;
            changed = true;
        }

        boolean lit = furnace.fuel > 0;
        if (wasLit != lit) {
            level.setBlock(pos, state.setValue(AlchemyFurnaceBlock.LIT, lit), 3);
        }

        if (changed) {
            setChanged(level, pos, state);
        }
    }

    /** 吃一份燃料。 */
    private boolean consumeFuel() {
        ItemStack fuelStack = items.get(SLOT_FUEL);
        if (fuelStack.isEmpty()) {
            return false;
        }
        int ticks = AlchemyRecipes.fuelTicks(itemId(fuelStack));
        if (ticks <= 0) {
            return false;
        }
        ItemStack remainder = fuelStack.getCraftingRemainingItem();
        fuelStack.shrink(1);
        if (fuelStack.isEmpty()) {
            items.set(SLOT_FUEL, remainder.isEmpty() ? ItemStack.EMPTY : remainder);
        }
        fuel = ticks;
        fuelTotal = ticks;
        setChanged();
        return true;
    }

    /**
     * 出丹：扣 1 份主料 + 1 份辅料，产物进槽，并给"成了"的视听反馈。
     *
     * <h2>炼丹雷劫（见 {@link AlchemyTribulation}）</h2>
     * <p>炼<b>最好的丹药</b>（品阶等于全表最高的那一档，当前是第六阶血珞芝的两条方子）
     * 时会挨雷：5% 炸炉、15% 出 5 个、30% 出 3 个、50% 出 1 个。
     * 其他丹药没有雷，但"一颗变五个"的概率是 20%。</p>
     *
     * <p>产物在 {@link #applyOutcome} 里当场结算——刻意的：如果先塞满产物槽再炸炉，
     * 玩家会看到"丹药闪了一下就没了"，还得自己去猜发生什么。现在是一条消息说清结果。</p>
     */
    private void finishBrew(Level level, BlockPos pos, AlchemyRecipes.Brew brew) {
        Item result = resolveItem(brew.result());
        if (result == Items.AIR) {
            progress = 0;
            return;
        }
        // 先扣材料：无论雷劫结果如何，这一炉的料都吃掉了
        items.get(SLOT_MAIN).shrink(1);
        if (items.get(SLOT_MAIN).isEmpty()) {
            items.set(SLOT_MAIN, ItemStack.EMPTY);
        }
        items.get(SLOT_AUX).shrink(1);
        if (items.get(SLOT_AUX).isEmpty()) {
            items.set(SLOT_AUX, ItemStack.EMPTY);
        }
        progress = 0;
        setChanged();

        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.15F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                    10, 0.35D, 0.25D, 0.35D, 0.02D);
        }

        if (AlchemyTribulation.isTribulation(brew)) {
            beginTribulation(level, pos, brew);
        } else {
            // 非雷劫丹方：没有雷，但有两成机会"一颗变五个"
            int base = brew.count();
            int count = AlchemyTribulation.rollPlainCount(base,
                    java.util.concurrent.ThreadLocalRandom.current());
            if (count > base) {
                announce(level, pos, "\u00a7a药力圆融，一炉得丹 \u00a7f×"
                        + count + "\u00a7a！");
            }
            applyOutcomePills(level, pos, brew, count);
        }
    }

    // ------------------------------------------------------------------
    // 炼丹雷劫
    // ------------------------------------------------------------------

    /** 雷劫动画剩余 tick（0 = 没有雷劫在跑）。存进 NBT，区块卸载也不会丢动画。 */
    private int tribulationTicks;

    /** 正在挨雷的那一炉炼的是什么（落雷那一刻才知道产出几个）。 */
    private AlchemyRecipes.Brew tribulationBrew;

    /** 触发雷劫：丹药飞上天 → 天雷劈下 → 落回产物槽。 */
    private void beginTribulation(Level level, BlockPos pos, AlchemyRecipes.Brew brew) {
        announce(level, pos, AlchemyTribulation.OMEN);
        tribulationTicks = AlchemyTribulation.ANIM_TICKS;
        tribulationBrew = brew;
        setChanged();
    }

    /**
     * 雷劫动画每 tick 推进一步。
     *
     * <p>丹药用粒子"飞上天"（{@code END_ROD} 拖尾），顶点处劈一道
     * <b>纯视觉</b>闪电，落回时才 {@link AlchemyTribulation#roll} 出结果。</p>
     */
    private void tickTribulation(Level level, BlockPos pos) {
        if (tribulationTicks <= 0) {
            return;
        }
        tribulationTicks--;
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double cx = pos.getX() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        double baseY = pos.getY() + 1.1D;
        int total = AlchemyTribulation.ANIM_TICKS;
        double t = 1.0D - (tribulationTicks / (double) total);   // 0 → 1

        if (tribulationTicks > total / 2) {
            // 前半段：丹药往上飞，拖一条灵光
            double y = baseY + AlchemyTribulation.SKY_HEIGHT * (t * 2.0D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, cx, y, cz,
                    3, 0.08D, 0.02D, 0.08D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, cx, y, cz,
                    2, 0.25D, 0.1D, 0.25D, 0.05D);
        } else if (tribulationTicks == total / 2) {
            // 顶点：天雷落下。
            //
            // setVisualOnly(true) 是**必须**的——原版闪电的伤害类型带
            // BYPASSES_ARMOR 标签，真雷会把站在炉边的玩家当场劈死。
            // 这个 codebase 已经因为这条踩过一次"雷劫必死"，绝不能再犯。
            spawnVisualBolt(serverLevel, cx, baseY + AlchemyTribulation.SKY_HEIGHT, cz);
            serverLevel.sendParticles(ParticleTypes.FLASH, cx,
                    baseY + AlchemyTribulation.SKY_HEIGHT, cz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.BLOCKS, 0.9F, 1.1F);
        } else {
            // 后半段：丹药落回炉里
            double y = baseY + AlchemyTribulation.SKY_HEIGHT
                    * (tribulationTicks / (total / 2.0D));
            serverLevel.sendParticles(ParticleTypes.END_ROD, cx, y, cz,
                    2, 0.06D, 0.02D, 0.06D, 0.0D);
        }

        if (tribulationTicks <= 0) {
            // 雷劫落定：现在才决定这一炉出几颗
            AlchemyTribulation.Outcome outcome = forcedOutcome();
            if (outcome == null) {
                outcome = AlchemyTribulation.roll(
                        java.util.concurrent.ThreadLocalRandom.current());
            }
            announce(level, pos, outcome.message());
            if (!outcome.isFailure()) {
                applyOutcomePills(level, pos, tribulationBrew, outcome.pills());
            }
            tribulationBrew = null;
            tribulationDebugOutcome = 0;
            setChanged();
        }
    }

    /**
     * 调试用的"指定雷劫结果"（0 = 不指定，正常随机）。
     *
     * <p>只能通过 {@code /data merge block ... {TribulationDebug:1..4}} 设置，
     * 游戏里没有任何途径写它，验证完就会清 0。存在的理由：5% 的炸炉分支
     * 靠随机抽样验不出来（要跑上百炉），而它偏偏是"最该确认不会留下坏状态"
     * 的那一条。和 {@code hongmeng attrselftest} 那类自检钩子一个口径。</p>
     *
     * <p>1 = 一颗、2 = 三颗、3 = 五颗、4 = 强制炸炉。</p>
     */
    private int tribulationDebugOutcome;

    /** 按调试字段取结果；没设就返回 null（走随机）。 */
    private AlchemyTribulation.Outcome forcedOutcome() {
        int idx = tribulationDebugOutcome - 1;
        if (idx < 0 || idx >= AlchemyTribulation.OUTCOMES.size()) {
            return null;
        }
        return AlchemyTribulation.OUTCOMES.get(idx);
    }

    /** 劈一道纯视觉闪电。任何异常都不许打断丹炉的 tick。 */
    private static void spawnVisualBolt(ServerLevel level, double x, double y, double z) {
        try {
            net.minecraft.world.entity.LightningBolt bolt =
                    net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) {
                return;
            }
            bolt.moveTo(x, y, z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 丹炉视觉闪电召唤失败：{}", t.toString());
        }
    }

    /** 把结果数量的丹药放进产物槽。 */
    private void applyOutcomePills(Level level, BlockPos pos, AlchemyRecipes.Brew brew, int count) {
        if (brew == null || count <= 0) {
            return;
        }
        Item result = resolveItem(brew.result());
        if (result == Items.AIR) {
            return;
        }
        ItemStack out = items.get(SLOT_RESULT);
        if (out.isEmpty()) {
            items.set(SLOT_RESULT, new ItemStack(result, Math.min(count, getMaxStackSize())));
        } else if (out.is(result)) {
            out.grow(count);
            if (out.getCount() > out.getMaxStackSize()) {
                out.setCount(out.getMaxStackSize());
            }
        } else {
            // 产物槽是别的东西（理论上 canAccept 已挡住，这里兜底）
            return;
        }
        setChanged();
    }

    /** 给炉子周围 12 格内的玩家发一条消息（没人在附近就安静）。 */
    private static void announce(Level level, BlockPos pos, String text) {
        if (!(level instanceof ServerLevel serverLevel) || text == null || text.isEmpty()) {
            return;
        }
        Component msg = Component.literal(text);
        for (net.minecraft.server.level.ServerPlayer p
                : serverLevel.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,
                new net.minecraft.world.phys.AABB(pos).inflate(12.0D))) {
            p.sendSystemMessage(msg);
        }
    }


    /** 物品 id（带命名空间），例：{@code hongmeng:lingye}。 */
    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** 配方表里的 id → 物品。表是纯数据，所以这里做一次解析；解析不到就是空气。 */
    public static Item resolveItem(String id) {
        ResourceLocation key = ResourceLocation.tryParse(AlchemyRecipes.fullId(id));
        if (key == null) {
            return Items.AIR;
        }
        Item item = BuiltInRegistries.ITEM.get(key);
        return item == null ? Items.AIR : item;
    }
}
