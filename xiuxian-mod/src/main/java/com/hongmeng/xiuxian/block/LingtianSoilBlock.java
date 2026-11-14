package com.hongmeng.xiuxian.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 灵田土：灵植的"高级耕地"。
 *
 * <p>灵植种在耕地上是正常速度，种在灵田土上生长概率 ×{@link SpiritHerbBlock#LINGTIAN_MULTIPLIER}
 * （见 {@link SpiritHerbBlock#growthChanceOn}）。它本身不是耕地，
 * 不需要锄头，也不能被踩坏——它是一块"自带灵气的土"。</p>
 *
 * <p>做成整方块而不是像耕地那样 15/16 高：一来省掉"没有支撑就变回泥土"的一整套逻辑，
 * 二来不会出现"锄头一碰就掉"的意外，三来碰撞箱是标准立方体，
 * <b>不必也不该</b>在 {@code getShape} 里碰 level（历史事故：某些方块在
 * {@code getShape} 里解引用 level，左键一点就 NPE）。</p>
 */
public class LingtianSoilBlock extends Block {

    public LingtianSoilBlock(Properties properties) {
        super(properties);
    }

    /** 灵气外溢：偶尔冒一点绿色微光，让"这是灵田"一眼能看出来。 */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) != 0) {
            return;
        }
        level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + random.nextDouble(),
                pos.getY() + 1.02D,
                pos.getZ() + random.nextDouble(),
                0.0D, 0.0D, 0.0D);
    }

    /** 供灵植判断"这块地是不是灵田"。 */
    public static boolean isLingtian(BlockState state) {
        return state != null && state.getBlock() instanceof LingtianSoilBlock;
    }
}
