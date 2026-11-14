package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.Set;

/**
 * <b>模组自带的爆炸保护</b>（玩家要求：「防苦力怕爆炸」）。
 *
 * <p><b>死亡不掉落已按玩家要求取消</b>（原先这里还有一段"死亡时暂存背包、
 * 重生时归还"的逻辑）。现在这个类只做爆炸保护。</p>
 *
 * <h2>防苦力怕爆炸</h2>
 * <p>在爆炸<b>开始</b>时把方块破坏清单清空 —— <b>保留对实体的伤害</b>，
 * 所以苦力怕照样疼，只是不再把家炸没。</p>
 * <p>顺带把本模组方块（灵植/灵矿）与容器/光源从<b>任何</b>爆炸的清单里剔除，
 * 这样 TNT、床、其它模组的爆炸也伤不到它们。</p>
 */
/**
 * <p>注：类名保留 {@code ServerProtections} 是因为它还承担本模组方块与容器的
 * 爆炸保护；死亡不掉落已经移除。</p>
 */
public final class ServerProtections {

    /** 爆炸绝不破坏的方块（容器/光源/功能性）。 */
    private static final Set<net.minecraft.world.level.block.Block> BLAST_PROOF = Set.of(
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.SHULKER_BOX,
            Blocks.ENDER_CHEST, Blocks.HOPPER, Blocks.DISPENSER, Blocks.DROPPER,
            Blocks.BREWING_STAND, Blocks.ENCHANTING_TABLE, Blocks.ANVIL,
            Blocks.BEACON, Blocks.SPAWNER, Blocks.TORCH, Blocks.WALL_TORCH,
            Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.CAMPFIRE,
            Blocks.GLOWSTONE, Blocks.SEA_LANTERN, Blocks.SHROOMLIGHT);

    /** 公开构造：Forge 事件总线要实例化它。 */
    public ServerProtections() {
    }

    /** "书已经送过"的标记。 */
    private static final String GIFTED = "hongmeng_guide_gifted";

    /**
     * <b>开局送一本《鸿蒙修行录》</b>（玩家要求：「这个书开局就送」）。
     *
     * <p>只送一次：标记存在玩家的 {@code persistentData} 里，
     * 所以退出重进、换维度、死亡重生都不会重复发。</p>
     *
     * <p>背包满了就掉在脚下，不会凭空吞掉。</p>
     */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onPlayerJoin(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        try {
            net.minecraft.nbt.CompoundTag pd = player.getPersistentData();
            if (pd.getBoolean(GIFTED)) {
                return;
            }
            pd.putBoolean(GIFTED, true);
            net.minecraft.world.item.ItemStack book = new net.minecraft.world.item.ItemStack(
                    com.hongmeng.xiuxian.item.ModItems.GUIDE_BOOK.get());
            if (!player.getInventory().add(book)) {
                net.minecraft.world.entity.item.ItemEntity drop =
                        new net.minecraft.world.entity.item.ItemEntity(player.level(),
                                player.getX(), player.getY() + 0.5D, player.getZ(), book);
                drop.setNoPickUpDelay();
                player.level().addFreshEntity(drop);
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7d获得了《鸿蒙修行录》\u00a77 —— 右键翻开，或按 \u00a7eF\u00a77 打开"
                            + "（查合成、获得途径、建筑、群系、NPC）"), false);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[hongmeng] 送修行录失败：{}", t.toString());
        }
    }

    // ------------------------------------------------------------------
    // 防苦力怕爆炸
    // ------------------------------------------------------------------

    /** 爆炸开始时剔除不该被破坏的方块；苦力怕的爆炸全部剔除。 */
    @SubscribeEvent
    public void onExplosionStart(ExplosionEvent.Start event) {
        try {
            var explosion = event.getExplosion();
            var level = event.getLevel();
            if (level == null || explosion == null) {
                return;
            }
            // ① 苦力怕：完全不动方块（对实体的伤害保留 —— 那不在这个清单里）
            boolean creeper = explosion.getExploder() instanceof Creeper
                    || explosion.getDirectSourceEntity() instanceof Creeper;
            if (creeper) {
                explosion.getToBlow().clear();
                return;
            }
            // ② 其它爆炸：护住本模组方块与容器/光源
            Iterator<BlockPos> it = explosion.getToBlow().iterator();
            while (it.hasNext()) {
                BlockPos pos = it.next();
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState st = level.getBlockState(pos);
                if (BLAST_PROOF.contains(st.getBlock())) {
                    it.remove();
                    continue;
                }
                ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS
                        .getKey(st.getBlock());
                if (id != null && "hongmeng".equals(id.getNamespace())) {
                    it.remove();
                }
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[hongmeng] 爆炸保护异常：{}", t.toString());
        }
    }
}
