package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.ModBlocks;
import com.hongmeng.xiuxian.alchemy.HerbCatalog;
import com.hongmeng.xiuxian.core.CultivationSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.function.Supplier;

/**
 * 灵植：可以**种**的灵草（灵植系统的核心方块），六阶各一株。
 *
 * <p>与野生的 {@link SpiritGrassBlock} 不同，灵植走的是原版 {@link CropBlock} 那一套：
 * 有 {@code age 0..7} 的生长阶段、能用骨粉催熟、能被踩坏、必须种在"能长东西的方块"上。
 * 也就是说它掉的花瓣不是"野外捡的"，而是**种出来的**——这是
 * "种 → 采 → 炼 → 用"闭环的第一环。</p>
 *
 * <h2>生长速度（土决定一切）</h2>
 * <p>原版 {@code CropBlock#getGrowthSpeed} 会把生长速度算成一个 static 方法，
 * 子类没法按"脚下是什么方块"改倍率，所以这里直接覆盖 {@link #randomTick} 自己掷骰子：</p>
 * <table border="1">
 *   <caption>每次随机刻的进阶概率</caption>
 *   <tr><th>脚下</th><th>倍率</th><th>一阶（雪莲草）实际概率</th></tr>
 *   <tr><td>灵田土</td><td>×{@value #LINGTIAN_MULTIPLIER}</td><td>{@value #LINGTIAN_MULTIPLIER} × 基础</td></tr>
 *   <tr><td>耕地</td><td>×1.0</td><td>基础</td></tr>
 *   <tr><td>泥土 / 草方块 / 沙子等野外地面</td><td>×{@value #WILD_MULTIPLIER}</td><td>半速</td></tr>
 * </table>
 *
 * <p><b>注意</b>：{@code Supplier} 而不是直接拿 {@code Item}。灵植方块是在
 * {@code RegisterEvent} 注册**方块**时构造的，那时**物品**还没注册完，
 * 构造期调 {@code RegistryObject.get()} 会直接抛 "Registry Object not present"
 * 让整个模组加载失败（这个坑踩过）。所以种子/花瓣一律存 {@code Supplier}，
 * 只在掉落、骨粉这类**运行时**才 {@code get()}。</p>
 */
public class SpiritHerbBlock extends CropBlock {

    /** 灵田土的生长倍率。见 docs/herb-alchemy-spec.md。 */
    public static final float LINGTIAN_MULTIPLIER = 3.0F;

    /** 野外地面（非耕地）的生长倍率：能长，但慢一半。 */
    public static final float WILD_MULTIPLIER = 0.5F;

    private final HerbCatalog.Herb herb;
    private final Supplier<Item> seedItem;
    private final Supplier<Item> petalItem;

    public SpiritHerbBlock(Properties properties, HerbCatalog.Herb herb,
                           Supplier<Item> seedItem, Supplier<Item> petalItem) {
        super(properties);
        // 只存 Supplier 与纯数据记录 —— 构造期绝不 .get()
        this.herb = herb;
        this.seedItem = seedItem;
        this.petalItem = petalItem;
    }

    /** 这株灵植的静态数据（品阶 / 用途 / 生长概率）。 */
    public HerbCatalog.Herb herb() {
        return herb;
    }

    /** 注册名，例：{@code xuelian_grass}。 */
    public String herbId() {
        return herb.id();
    }

    public String cnName() {
        return herb.cn();
    }

    /** 品阶 1..6，与 docs/herb-alchemy-spec.md 的表格一致。 */
    public int tier() {
        return herb.tier();
    }

    public String use() {
        return herb.use();
    }

    public float baseGrowthChance() {
        return herb.growthChance();
    }

    public Item seedItem() {
        return seedItem.get();
    }

    public Item petalItem() {
        return petalItem.get();
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return BlockStateProperties.AGE_7;
    }

    @Override
    public int getMaxAge() {
        return 7;
    }

    /** 长满了就别再随机刻了，省一点算力。 */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return !isMaxAge(state);
    }

    /**
     * 能种在哪。
     *
     * <p>耕地是"正经农田"，灵田土是"灵田"（长得快），野外地面能活但长不动。
     * 世界生成把灵植撒在草地/泥土/沙子上时也走这里，所以野外地面必须在名单里。</p>
     */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.FARMLAND)
                || state.is(ModBlocks.LINGTIAN_SOIL.get())
                || state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SAND);
    }

    /** 脚下这块地给出的进阶概率（0..1）。 */
    public float growthChanceOn(BlockState below) {
        float base = herb.growthChance();
        if (below.is(ModBlocks.LINGTIAN_SOIL.get())) {
            return Math.min(1.0F, base * LINGTIAN_MULTIPLIER);
        }
        if (below.is(Blocks.FARMLAND)) {
            return base;
        }
        return base * WILD_MULTIPLIER;
    }

    /**
     * 自己掷生长骰子。
     *
     * <p>没有照抄 {@code CropBlock#randomTick}：那一版把"四周耕地数量 / 湿润度"也算进去，
     * 但对灵植来说玩家真正能感知的只有**脚下是不是灵田土**，
     * 所以这里只保留"光照 ≥ 9 + 土质倍率"两条，行为可预期、也方便写在文档里。</p>
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getRawBrightness(pos, 0) < 9) {
            return;
        }
        int age = getAge(state);
        if (age >= getMaxAge()) {
            return;
        }
        float chance = growthChanceOn(level.getBlockState(pos.below()));
        if (random.nextFloat() < chance) {
            level.setBlock(pos, getStateForAge(age + 1), 2);
        }
    }

    /** 骨粉催熟：原版一次 2~5 阶，灵植照旧。 */
    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return 2 + level.random.nextInt(3);
    }

    /** 用种子当"复制物品"，和原版小麦一致。 */
    @Override
    protected Item getBaseSeedId() {
        return seedItem.get();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(seedItem.get());
    }

    /**
     * 采药：除了掉落表里的花瓣/种子，再补两点。
     *
     * <ul>
     *   <li>给一点"奇遇"修为——种田也能修炼，虽然不是主要途径；</li>
     *   <li>长熟的灵植有概率多爆一片花瓣（品阶越高越容易），
     *       让"等到熟再采"这件事有明确收益。</li>
     * </ul>
     */
    @Override
    public void playerDestroy(Level level, net.minecraft.world.entity.player.Player player,
                              BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CultivationSource.ADVENTURE.grant(serverPlayer, 6.0D + tier() * 4.0D, true);

        if (isMaxAge(state) && serverPlayer.getRandom().nextInt(100) < 20 + tier() * 5) {
            popResource(serverLevel, pos, new ItemStack(petalItem.get(), 1));
        }
    }
}
