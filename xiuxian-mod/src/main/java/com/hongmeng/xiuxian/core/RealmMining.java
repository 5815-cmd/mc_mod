package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 境界挖掘：修到金丹以后，一双肉掌就顶得上一把镐子。
 *
 * <p>规则来自设定：境界就是"空手工具等级"。等级够了，空手也能把方块挖下来；
 * 等级不够，挖得动也不掉东西。到了鸿蒙，什么都能挖——包括基岩。</p>
 *
 * <ul>
 *   <li>金丹 = 石制，化神 = 铁制，道印 = 金制，飞升 = 钻石，仙人及以上 = 下界合金</li>
 *   <li>仙人（下界合金级）：刷怪笼从"只掉经验"变成"连笼子一起掉"</li>
 *   <li>鸿蒙：可破坏一切方块（含基岩），破坏后照常掉落</li>
 * </ul>
 */
public class RealmMining {

    /** 这些方块即使在创造模式外被"挖掉"，也属于不应掉落的东西（除鸿蒙外）。 */
    private static final Set<Block> UNBREAKABLE = new HashSet<>();

    static {
        UNBREAKABLE.add(Blocks.BEDROCK);
        UNBREAKABLE.add(Blocks.BARRIER);
        UNBREAKABLE.add(Blocks.STRUCTURE_VOID);
        UNBREAKABLE.add(Blocks.END_PORTAL);
        UNBREAKABLE.add(Blocks.END_GATEWAY);
        UNBREAKABLE.add(Blocks.NETHER_PORTAL);
        UNBREAKABLE.add(Blocks.END_PORTAL_FRAME);
        UNBREAKABLE.add(Blocks.COMMAND_BLOCK);
        UNBREAKABLE.add(Blocks.CHAIN_COMMAND_BLOCK);
        UNBREAKABLE.add(Blocks.REPEATING_COMMAND_BLOCK);
        UNBREAKABLE.add(Blocks.STRUCTURE_BLOCK);
        UNBREAKABLE.add(Blocks.JIGSAW);
        UNBREAKABLE.add(Blocks.LIGHT);
        UNBREAKABLE.add(Blocks.MOVING_PISTON);
        UNBREAKABLE.add(Blocks.REINFORCED_DEEPSLATE);
    }

    // ------------------------------------------------------------------
    // 采掘判定：让客户端/服务端都认为"你挖得动"
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        // 整段兜住：本模组**不允许**因为自身异常而否决玩家的破坏。
        // 出任何问题就放手，让原版按它自己的规则结算（最多少掉境界加成）。
        try {

            if (event.isCanceled() || !HongmengConfig.SERVER.realmMining.get()) {
                return;
            }
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            if (event.canHarvest() || player.isCreative()) {
                return;
            }
            BlockState state = event.getTargetBlock();
            CultivationData data = CultivationCapability.get(player);
            Tier tier = realmTier(data);
            if (tier == null) {
                return;
            }
            // 镐斧铲锄一视同仁：境界够 + Forge 认可这个等级对该方块有效
            boolean can = realmCanHarvest(tier, state);
            if (can) {
                event.setCanHarvest(true);
            }
            // 黑匣子：服务端**确实参与**了这次挖掘（说明 START_DESTROY_BLOCK 收到了）
            // 诊断包一层：它抛异常会中止本次采集判定 / 破坏事件（那是玩法的命）
            try {
                MiningDiag.recordHarvestCheck(player.getGameProfile().getName(),
                        String.valueOf(state.getBlock()), itemName(player), event.canHarvest(), can);
            } catch (Throwable ignored) {
                // 诊断失败不影响玩法
            }
            } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[hongmeng] 采掘判定异常，已放行（不影响正常掉落）：{}", t.toString());
        }
    }

    // ------------------------------------------------------------------
    // 破坏方块：境界够了就补上掉落（本来因为"工具不对"而不掉的）
    // ------------------------------------------------------------------

    /**
     * 黑匣子第一环：客户端发来的 {@code START_DESTROY_BLOCK} 到了服务端。
     *
     * <p>为什么要单独一个 handler：它必须对**所有**方块都生效（包括普通方块），
     * 所以不能塞进 {@link #onLeftClickBlock} —— 那个方法在"不是不可破坏方块"时
     * 会提前 return，只有鸿蒙才走得到后面。</p>
     *
     * <p>这条只证明"服务端知道你在挖"。它出现、但后面的「采掘判定」与
     * 「破坏事件」都没有 ⇒ 方块被删之前流程就断了（见 {@link MiningDiag}）。</p>
     */
    @SubscribeEvent
    public void onLeftClickStart(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (!HongmengConfig.SERVER.realmMining.get()) {
            return;
        }
        if (event.getAction()
                != net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!CultivationCapability.get(player).isAwakened()) {
            return;
        }
        // 诊断包一层：它抛异常会中止本次采集判定 / 破坏事件（那是玩法的命）
        try {
            MiningDiag.recordLeftClickStart(player.getGameProfile().getName(),
                    String.valueOf(level.getBlockState(event.getPos()).getBlock()),
                    itemName(player));
        } catch (Throwable ignored) {
            // 诊断失败不影响玩法
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        // 整段兜住：本模组**不允许**因为自身异常而否决玩家的破坏。
        // 出任何问题就放手，让原版按它自己的规则结算（最多少掉境界加成）。
        try {

            if (!HongmengConfig.SERVER.realmMining.get()) {
                return;
            }
            if (!(event.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            if (player.isCreative() || player.isSpectator()) {
                return;
            }
            if (!(event.getLevel() instanceof ServerLevel level)) {
                return;
            }

            BlockState state = event.getState();
            BlockPos pos = event.getPos();
            Block block = state.getBlock();

            CultivationData data = CultivationCapability.get(player);
            if (!data.isAwakened()) {
                return;
            }
            Tier tier = realmTier(data);
            if (tier == null) {
                return;
            }

            boolean omni = canBreakAnything(data);

            // 黑匣子：破坏事件真的发出来了 —— 说明它已经过了 canAttackBlock 与
            // blockActionRestricted 两道前置判定，方块接下来必然会被删掉。
            // 所以"有这条记录却没有掉落" ⇒ 问题在掉落环节，不在破坏环节。
            // 诊断包一层：它抛异常会中止本次采集判定 / 破坏事件（那是玩法的命）
            try {
                MiningDiag.recordBreakEvent(player.getGameProfile().getName(),
                        String.valueOf(state.getBlock()), itemName(player),
                        "已放行（omni=" + omni + " 可采=" + realmCanHarvest(tier, state) + "）");
            } catch (Throwable ignored) {
                // 诊断失败不影响玩法
            }

            // 1) 本来就不该掉的方块：只有鸿蒙能带走
            if (isUnbreakable(state) && !omni) {
                logVetoOnce(player, "方块属于不可破坏（境界不够带走）: " + state.getBlock());
                return;
            }

            // 2) 等级不够就不掉（挖得动是挖得动，但东西拿不走）
            if (!realmCanHarvest(tier, state)) {
                logVetoOnce(player, "境界等级不足以采集: " + state.getBlock()
                        + "（境界=" + describeTierObject(tier)
                        + "，方块需求=" + describeTierObject(requiredTier(state)) + "）");
                return;
            }

            // 3) 关键：原版自己会掉东西的情况，绝不插手，否则就是掉双份。
            //
            //    hasCorrectToolForDrops 为 true 表示"原版认为这次破坏是正确工具"，
            //    原版流程会把战利品正常吐出来。我们只在它为 false 时补掉落：
            //    也就是"徒手挖了需要镐子的方块"——这时原版什么都不给，才轮到境界补。
            if (player.hasCorrectToolForDrops(state)) {
                // 放手之前先把"境界时运"的额外收益补上——
                // 飞升以上的玩家挖任何方块都该有时运 III 的收获。
                applyRealmFortune(level, player, state, pos, data);
                return;
            }

            // 4) 补掉落
            Vec3 center = Vec3.atCenterOf(pos);
            boolean spawner = block instanceof SpawnerBlock;

            // 刷怪笼：连笼子一起掉（原版只有精准采集能做到）
            //
            // 原来只看 omni（全能挖掘 = 能破坏一切方块），导致配置项
            // spawnerDropsAtNetherite 成了改了没反应的开关。
            // 现在补上：下界合金级挖掘（等级 5，仙人及以上）+ 配置开启 也算。
            boolean spawnerDrop = omni
                    || (data != null && data.isAwakened()
                    && data.realm().harvestLevel() >= 5
                    && com.hongmeng.xiuxian.compat.HongmengConfig.SERVER
                            .spawnerDropsAtNetherite.get());
            if (spawner && spawnerDrop) {
                popItem(level, center, new ItemStack(Blocks.SPAWNER));
            } else {
                // 其余情况走战利品表，等价于"用对了工具"
                dropLoot(level, player, state, pos);
            }

            // 5) 补经验：刷怪笼原本就是掉经验的
            if (spawner || omni) {
                int xp = xpFor(state, level, pos);
                if (xp > 0) {
                    block.popExperience(level, pos, xp);
                }
            }
            } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[hongmeng] 破坏事件结算异常，已放行交给原版（不影响掉落）：{}", t.toString());
        }
    }

    // ------------------------------------------------------------------
    // 鸿蒙专属：强行破坏"不可破坏方块"
    // ------------------------------------------------------------------

    /**
     * 原版破坏流程里有一条硬编码：{@code state.getDestroySpeed(...) == -1} 就永不破坏
     * （基岩、屏障、末地传送门框架都在此列），任何事件都绕不过去。
     *
     * <p>所以鸿蒙的"破坏一切"必须自己实现：盯着目标方块按住左键达到
     * {@code omni_mining_ticks} 之后，直接把它抹掉并补掉落。</p>
     */
    @SubscribeEvent
    public void onLeftClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (!HongmengConfig.SERVER.realmMining.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (event.getAction() != net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        if (!canBreakAnything(data)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!isVanillaUnbreakable(state)) {
            // 普通方块交给原版流程
            return;
        }
        // 记下目标与开始时间，由每 tick 的推进逻辑负责挖穿
        player.getPersistentData().putLong("hm_omni_pos", pos.asLong());
        player.getPersistentData().putLong("hm_omni_start", level.getGameTime());
        player.getPersistentData().putInt("hm_omni_need",
                Math.max(1, HongmengConfig.SERVER.omniMiningTicks.get()));
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a75\u00a7l鸿蒙之力\t——\t你在硬生生地掰开它"), true);
    }

    /** 由 ModEvents 每 tick 驱动：推进鸿蒙的强行破坏。 */
    public static void tickOmniMining(ServerPlayer player) {
        if (!HongmengConfig.SERVER.realmMining.get()) {
            return;
        }
        net.minecraft.nbt.CompoundTag pd = player.getPersistentData();
        if (!pd.contains("hm_omni_pos")) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = BlockPos.of(pd.getLong("hm_omni_pos"));
        long start = pd.getLong("hm_omni_start");
        int need = pd.getInt("hm_omni_need");

        // 玩家松手 / 换目标 / 距离过远 → 中断
        boolean stillTargeting = player.isAlive()
                && player.swinging
                && player.blockPosition().closerThan(pos, 8.0D)
                && level.getBlockState(pos).getDestroySpeed(level, pos) < 0.0F;

        if (!stillTargeting) {
            pd.remove("hm_omni_pos");
            pd.remove("hm_omni_start");
            pd.remove("hm_omni_need");
            return;
        }

        long elapsed = level.getGameTime() - start;
        if (elapsed < need) {
            int pct = (int) (elapsed * 100L / Math.max(1, need));
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a75裂开中… " + pct + "%"), true);
            return;
        }

        // 到点了：抹掉方块并补掉落
        BlockState state = level.getBlockState(pos);
        dropLoot(level, player, state, pos);
        int xp = xpFor(state, level, pos);
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                40, 0.4D, 0.4D, 0.4D, 0.25D);
        if (xp > 0) {
            state.getBlock().popExperience(level, pos, xp);
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "\u00a75\u00a7l它碎了。"), true);
        pd.remove("hm_omni_pos");
        pd.remove("hm_omni_start");
        pd.remove("hm_omni_need");
    }

    /**
     * 境界对应的**挖掘速度**（作为下限，不会让原版更快的工具变慢）。
     *
     * <p>光有"能挖动"不够——玩家反馈挖得动但慢得像徒手。速度表本身放在
     * {@link Realm#speedForTier(int)}（纯数据，离线可断言），这里只是转发。</p>
     */
    public static float realmMiningSpeed(int tier) {
        return Realm.speedForTier(tier);
    }

    /** 效率五金镐的等效挖掘速度（转发，保持旧常量可用）。 */
    public static final float EFFICIENCY_V_GOLD = Realm.EFFICIENCY_V_GOLD_SPEED;

    /**
     * 玩家当前的境界挖掘速度（未觉醒或还没开始挖掘时返回 0，表示不干预）。
     */
    public static float realmMiningSpeedOf(CultivationData data) {
        if (data == null || !data.isAwakened()) {
            return 0.0F;
        }
        // 练气 / 筑基还是徒手，不给提速
        if (data.realm().harvestLevel() <= 0) {
            return 0.0F;
        }
        return Realm.speedForTier(data.realm().tier());
    }

    /**
     * 挖掘速度：把境界速度作为**下限**。
     *
     * <p>用 max 而不是覆盖——玩家拿着比境界更快的工具（比如效率五下界合金镐）
     * 时不该被拖慢，只有徒手或工具不如境界时才由境界兜底。</p>
     */
    @SubscribeEvent
    public void onBreakSpeed(net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        if (!HongmengConfig.SERVER.realmMining.get()) {
            return;
        }
        // **必须在客户端也生效。**
        //
        // 这里是踩过的坑：原来第一行是
        //     if (!(event.getEntity() instanceof ServerPlayer)) return;
        // 于是客户端那一侧直接返回了。而**破坏进度是客户端本地算的**
        // （LocalPlayer 拿不到 ServerPlayer 类型），所以玩家看到的永远是徒手速度，
        // 服务端算得再快也没用。
        //
        // 玩家观察到的现象精确对应：泥土/沙子硬度低、原版判定宽松，看起来正常；
        // 石头/木头硬度高，就明显"和徒手一样慢"。
        net.minecraft.world.entity.player.Player player = event.getEntity();
        CultivationData data = null;

        if (player instanceof ServerPlayer sp) {
            data = CultivationCapability.get(sp);
        } else if (player.level().isClientSide()) {
            // 客户端：用同步过来的镜像。参考 ClientCultivationCache 会做
            // 分布式安全检查，拿不到就安静退出。
            try {
                data = com.hongmeng.xiuxian.network.ClientCultivationCache.get();
            } catch (Throwable ignored) {
                return;
            }
        }
        if (data == null || !data.isAwakened()) {
            return;
        }

        float realmSpeed = realmMiningSpeedOf(data);
        // 配置：每高一个大境界多加的挖掘速度（默认 0 = 不改原版）。
        // ⚠ 这个配置项以前从未被读取 —— 改了毫无变化。
        try {
            double per = HongmengConfig.SERVER.miningSpeedPerRealmTier.get();
            if (per > 0.0D) {
                realmSpeed += (float) (data.realm().tier() * per);
            }
        } catch (Throwable ignored) {
            // 配置不可用就按基础速度
        }
        if (realmSpeed <= 0.0F) {
            return;
        }
        // 只有境界确实能挖动这个方块时才提速
        if (!realmCanHarvest(realmTier(data), event.getState())) {
            return;
        }
        if (realmSpeed > event.getNewSpeed()) {
            event.setNewSpeed(realmSpeed);
        }
    }

    // ------------------------------------------------------------------
    // 境界时运（飞升起）
    // ------------------------------------------------------------------

    /**
     * 境界附带的**虚拟时运**。
     *
     * <p>按玩家要求：飞升（第 7 境）起挖掘附带时运 III，鸿蒙时运 V。</p>
     *
     * <p><b>做法</b>：把战利品表用"工具带时运 N"的副本重跑一遍，取它比
     * 基础掉落多出来的部分作为额外掉落。这样：</p>
     * <ul>
     *   <li>不需要给玩家真的附魔，也不占用附魔槽</li>
     *   <li>完全跟着原版的时运公式走，不同方块的收益自动合理</li>
     *   <li><b>手持工具已经有时运时不叠加</b>——否则就是双重加成</li>
     * </ul>
     */
    private static void applyRealmFortune(ServerLevel level, ServerPlayer player,
                                          BlockState state, BlockPos pos,
                                          CultivationData data) {
        int fortune = data.realm().miningFortune();
        if (fortune <= 0) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        // 手上的工具已经有时运就不叠（无论等级高低，避免双重收益）
        int heldFortune = net.minecraft.world.item.enchantment.EnchantmentHelper
                .getItemEnchantmentLevel(
                        net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, held);
        if (heldFortune > 0) {
            return;
        }

        // 造一份"带时运 N"的工具副本作为对照
        ItemStack boosted = held.copy();
        if (boosted.isEmpty()) {
            boosted = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        }
        boosted.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, fortune);

        List<ItemStack> base = rollLoot(level, player, state, pos, held);
        List<ItemStack> rich = rollLoot(level, player, state, pos, boosted);

        // 逐项求差：只补"时运多出来的那部分"
        for (ItemStack got : rich) {
            if (got.isEmpty()) {
                continue;
            }
            int already = 0;
            for (ItemStack b : base) {
                if (ItemStack.isSameItemSameTags(b, got)) {
                    already += b.getCount();
                }
            }
            int extra = got.getCount() - already;
            if (extra > 0) {
                ItemStack bonus = got.copyWithCount(extra);
                popItem(level, Vec3.atCenterOf(pos), bonus);
            }
        }
    }

    /** 用指定工具跑一次方块的战利品表。 */
    private static List<ItemStack> rollLoot(ServerLevel level, ServerPlayer player,
                                            BlockState state, BlockPos pos, ItemStack tool) {
        BlockEntity be = level.getBlockEntity(pos);
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool)
                .withParameter(LootContextParams.THIS_ENTITY, player)
                // ⚠ **BLOCK_STATE 不能漏**（与 dropLoot 同一个坑，这里是第二条漏掉的路径）！
                //
                // 这是「境界时运」补掉落用的第二次 roll：飞升及以上玩家**挖任何方块**
                // 都会先走 applyRealmFortune → rollLoot。缺了这个参数，
                // builder.create(BLOCK) 会抛：
                //   IllegalArgumentException: Missing required parameters:
                //       [<parameter minecraft:block_state>]
                // 然后被 onBlockBreak 最外层 catch 吞掉、打一条 WARN ——
                // 表现就是**每挖一个方块刷一条「破坏事件结算异常」**（实机日志 17:21~17:22
                // 的刷屏就是它），同时境界时运的额外收益整个失效。
                // dropLoot 已经补过同一个参数，但这条独立路径当时漏了。
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, be);
        LootTable table = level.getServer().getLootData()
                .getLootTable(state.getBlock().getLootTable());
        return table.getRandomItems(builder.create(LootContextParamSets.BLOCK));
    }

    /** 只判断原版那套"destroySpeed == -1"的不可破坏方块。 */    public static boolean isVanillaUnbreakable(BlockState state) {
        // 必须先查名单：原版不少方块的 getDestroySpeed 会解引用世界参数
        // （比如粘液块的 getDestroySpeed 直接读 level），
        // 传 null 过去会 NPE——而这条路鸿蒙玩家左键任何方块都会走。
        if (isUnbreakable(state)) {
            return true;
        }
        try {
            return state.getDestroySpeed(null, null) < 0.0F;
        } catch (Throwable t) {
            // 拿不到就按"能挖"处理，反正后面还有等级判定兜着
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 工具类型：境界就是"空手工具等级"，镐斧铲锄一视同仁
    // ------------------------------------------------------------------

    /** 挖方块需要的工具类型。 */
    public enum RequiredTool {
        NONE("徒手", Tiers.WOOD),
        PICKAXE("镐", Tiers.WOOD),
        AXE("斧", Tiers.WOOD),
        SHOVEL("铲", Tiers.WOOD),
        HOE("锄", Tiers.WOOD);

        private final String cn;
        private final Tier base;

        RequiredTool(String cn, Tier base) {
            this.cn = cn;
            this.base = base;
        }

        public String cn() {
            return cn;
        }

        /**
         * 挖这个方块需要的工具基准等级（不考虑方块自身的 NEEDS_*_TOOL）。
         * NONE 表示徒手就能拿。
         */
        public Tier baseTier() {
            return base;
        }

        public boolean needsTool() {
            return this != NONE;
        }
    }

    /**
     * 判断一个方块属于哪一类工具。
     *
     * <p>顺序很重要：原版里有些方块同时挂多个 MINEABLE 标签（例如
     * 某些木头也在镐子标签下），按 镐 → 斧 → 铲 → 锄 的顺序取第一个，
     * 与原版工具的实际行为一致。</p>
     */
    public static RequiredTool toolTypeOf(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return RequiredTool.PICKAXE;
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return RequiredTool.AXE;
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return RequiredTool.SHOVEL;
        }
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            return RequiredTool.HOE;
        }
        return RequiredTool.NONE;
    }

    /**
     * 方块的"挖掘等级需求"。
     *
     * <p><b>现在按工具类型分别判定</b>：镐子方块看 {@code NEEDS_*_TOOL} 标签，
     * 斧/铲/锄方块在原版里没有"等级"概念（木斧和钻石斧挖木头一样快），
     * 但对修仙者来说境界应该是一视同仁的——所以这里给它们一个由材质推断的
     * 合理档位（石头/木头类 → 石制，金属类 → 铁制，宝石类 → 钻石）。</p>
     *
     * <p>返回 null 表示"徒手就能拿"。</p>
     */
    public static Tier requiredTier(BlockState state) {
        if (isUnbreakable(state)) {
            return Tiers.NETHERITE;
        }

        RequiredTool tool = toolTypeOf(state);
        Block block = state.getBlock();

        // 镐子方块：直接用原版的标签判定，最准
        if (tool == RequiredTool.PICKAXE) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
                return Tiers.DIAMOND;
            }
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
                return Tiers.IRON;
            }
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
                return Tiers.STONE;
            }
            // 挂了镐子标签但没写 NEEDS（例如石砖），原版木镐就够
            return Tiers.WOOD;
        }

        // 非镐子方块：原版没有等级门槛，按稀有材质给个合理档位
        if (tool.needsTool()) {
            if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN
                    || block == Blocks.ANCIENT_DEBRIS || block == Blocks.RESPAWN_ANCHOR
                    || block == Blocks.NETHERITE_BLOCK) {
                return Tiers.DIAMOND;
            }
            if (block == Blocks.GOLD_BLOCK || block == Blocks.RAW_GOLD_BLOCK
                    || block == Blocks.DIAMOND_BLOCK || block == Blocks.EMERALD_BLOCK
                    || block == Blocks.LAPIS_BLOCK || block == Blocks.REDSTONE_BLOCK
                    || block == Blocks.IRON_BLOCK || block == Blocks.RAW_IRON_BLOCK
                    || block == Blocks.COPPER_BLOCK || block == Blocks.RAW_COPPER_BLOCK) {
                return Tiers.IRON;
            }
            // 木头、泥土、沙子、树叶这类：有工具挖得快，但没工具也能拿
            return null;
        }

        // 徒手方块
        return null;
    }

    /**
     * 等级够不够（含工具类型判定）。
     *
     * <p>境界就是"空手工具等级"——到了某个境界，镐斧铲锄全都按这个等级算。
     * 所以这里不比较"玩家手里拿的是什么工具"，只比较等级够不够，
     * 再用方块自身的标签判断工具<b>类型</b>是否正确。</p>
     */
    public static boolean canHarvest(Tier have, Tier required) {
        if (required == null) {
            return true;
        }
        if (have == null) {
            return false;
        }
        return have.getLevel() >= required.getLevel();
    }

    /**
     * 综合判定：这个境界能不能像"用对了工具"一样挖这个方块。
     *
     * <p>两步：</p>
     * <ol>
     *   <li>等级够（境界自身的挖掘等级 ≥ 方块需求）</li>
     *   <li>Forge 认可这个等级对该方块有效（走 TierSortingRegistry，
     *       照顾模组自定义等级）</li>
     * </ol>
     */
    public static boolean realmCanHarvest(Tier realmTier, BlockState state) {
        if (realmTier == null) {
            return false;
        }
        // **刻意不再问 TierSortingRegistry。**
        //
        // 那是 Forge 给"原版工具等级"设计的表，只认注册过的等级。
        // 我们用的是自定义的金制等级（level 3），没有走 registerTier()
        // （那需要额外的 item_tier_ordering 数据包与加载顺序配合），
        // 于是 isTierSorted() 返回 false，而 isCorrectTierForDrops() 对
        // "需要正确工具的方块"一律返回 false。
        //
        // 玩家观察到的现象精确对应这个行为：
        //   · 泥土 / 沙子 —— 原版不要求工具等级 → 判定通过 → **秒挖**
        //   · 石头 / 木头 —— 有 NEEDS_*_TOOL 要求 → 判定 false → **退回徒手速度**
        //
        // 我们自己的 requiredTier() + canHarvest() 已经完整覆盖了
        // "工具类型对不对 + 等级够不够"这两件事，不需要那张表。
        return canHarvest(realmTier, requiredTier(state));
    }

    // ------------------------------------------------------------------
    // 掉落与经验
    // ------------------------------------------------------------------

    private static void dropLoot(ServerLevel level, ServerPlayer player, BlockState state, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                // ⚠ **BLOCK_STATE 不能漏**！
                //
                // 方块战利品表要求这个参数（例如"带不带水""朝向""成熟度"都靠它算），
                // 少了它会抛：
                //   IllegalArgumentException: Missing required parameters:
                //       [<parameter minecraft:block_state>]
                // 玩家日志里刷了 235 条这个错，就是这个漏项（挖矿时"补掉落"那一步全废了）。
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, be);

        LootTable table = level.getServer().getLootData()
                .getLootTable(state.getBlock().getLootTable());
        List<ItemStack> drops = table.getRandomItems(builder.create(LootContextParamSets.BLOCK));
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                popItem(level, Vec3.atCenterOf(pos), stack);
            }
        }
    }

    /** 手持物品的可读名（黑匣子用）。**取不到名字也不能抛** —— 见 MiningDiag 的注释。 */
    /** 每个玩家只打一次的"判定为不掉落"日志（用于定位玩家报的"挖了不掉"）。 */
    private static final java.util.Set<java.util.UUID> VETO_LOGGED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void logVetoOnce(net.minecraft.server.level.ServerPlayer player, String why) {
        try {
            if (VETO_LOGGED.add(player.getUUID())) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                        "[hongmeng] {} 本次世界第一次「判定为不掉落」：{}（之后不再重复记录）",
                        player.getGameProfile().getName(), why);
            }
        } catch (Throwable ignored) {
            // 日志出问题也不能影响玩法
        }
    }

    private static String itemName(net.minecraft.world.entity.player.Player player) {
        try {
            var stack = player.getMainHandItem();
            return stack.isEmpty() ? "（空手）" : stack.getHoverName().getString()
                    + " x" + stack.getCount();
        } catch (Throwable t) {
            return "（取名字失败）";
        }
    }

    private static void popItem(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level,
                at.x + (level.random.nextDouble() - 0.5D) * 0.5D,
                at.y + 0.25D,
                at.z + (level.random.nextDouble() - 0.5D) * 0.5D,
                stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
        level.playSound(null, BlockPos.containing(at), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 0.15F, 1.4F);
    }

    /** 方块的经验掉落：能查到就用，查不到按方块类型给默认。 */
    private static int xpFor(BlockState state, ServerLevel level, BlockPos pos) {
        Block block = state.getBlock();
        int fromBlock = block.getExpDrop(state, level, level.random, pos, 0, 0);
        if (fromBlock > 0) {
            return fromBlock;
        }
        if (block instanceof SpawnerBlock) {
            // 原版刷怪笼固定 15~43 点经验
            return 15 + level.random.nextInt(29);
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // 等级比较
    // ------------------------------------------------------------------

    /** 该玩家当前的"空手挖掘等级"；未觉醒或境界不足返回 null。 */
    public static Tier realmTier(CultivationData data) {
        if (data == null || !data.isAwakened()) {
            return null;
        }
        int level = data.realm().harvestLevel();
        Tier tier = tierFor(level);
        // 兜底：只要"这个人本该有挖掘等级"（level > 0），就绝不允许返回 null。
        // null 的后果是"能挖破但不掉东西"，玩家完全无法自查，只能报 bug。
        if (tier == null && level > 0) {
            return Tiers.NETHERITE;
        }
        return tier;
    }

    /**
     * 把境界的挖掘等级换算成实际的 {@link Tier} 对象。
     *
     * <p>刻意不用原版 {@code Tiers.GOLD}：它的挖掘等级是 0（比铁还低），
     * 会让"道印"比"化神"倒退。这里用 Forge 的自定义等级补一个真正的金制档位。</p>
     */
    public static Tier tierFor(int harvestLevel) {
        // **1..5 之外必须夹住，不能返回 null。**
        //
        // 曾经这里是 `default -> null`，而调用方 `realmCanHarvest()` 拿到 null 就一律
        // 判"挖不动"→ 破坏事件里直接 return → **方块能破但什么都不掉**。
        // 玩家在"突破到仙人境"之后撞上了这个（他存档里读出的挖掘等级超出了 1..5），
        // 现象就是"仙人之后挖任何方块都没掉落，拿工具也不行，创造模式正常"。
        //
        // 现在：<=0 表示空手（保留 null 语义），>=5 一律当下界合金级。
        if (harvestLevel <= 0) {
            return null;
        }
        return switch (harvestLevel) {
            case 1 -> Tiers.STONE;
            case 2 -> Tiers.IRON;
            case 3 -> xianjin();
            case 4 -> Tiers.DIAMOND;
            default -> Tiers.NETHERITE;
        };
    }

    /** "金制"档位：与原版金同速同耐久，但挖掘等级为 3（铁之上、钻石之下）。延迟创建。 */
    private static volatile Tier XIANJIN;

    private static Tier xianjin() {
        Tier cached = XIANJIN;
        if (cached != null) {
            return cached;
        }
        synchronized (RealmMining.class) {
            if (XIANJIN == null) {
                XIANJIN = new net.minecraftforge.common.ForgeTier(
                        3,
                        Tiers.GOLD.getUses(),
                        Tiers.GOLD.getSpeed(),
                        Tiers.GOLD.getAttackDamageBonus(),
                        0,
                        net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL,
                        () -> net.minecraft.world.item.crafting.Ingredient.EMPTY);
            }
            return XIANJIN;
        }
    }

    public static boolean canBreakAnything(CultivationData data) {
        if (data == null || !data.isAwakened()) {
            return false;
        }
        return data.realm().canBreakAnything();
    }

    /** 是否是"正常情况下拿不走"的方块。 */
    public static boolean isUnbreakable(BlockState state) {
        if (UNBREAKABLE.contains(state.getBlock())) {
            return true;
        }
        return false;
    }

    /**
     * 给面板/HUD 用的显示文本。
     *
     * <p>注意：这个方法本身不碰注册表，但类里别的地方会（{@code Blocks} 静态集合），
     * 所以离线验证器不要调用它——它用 {@link Realm#harvestTierLabel()} 代替。</p>
     */
    public static String describeTier(CultivationData data) {
        if (data == null || !data.isAwakened()) {
            return "空手";
        }
        return data.realm().harvestTierLabel();
    }

    /** 把 {@link Tier} 对象映射成中文名，用于调试命令输出。 */
    public static String describeTierObject(Tier tier) {
        if (tier == null) {
            return "空手";
        }
        if (tier == Tiers.WOOD) {
            return "木制";
        }
        if (tier == Tiers.STONE) {
            return "石制";
        }
        if (tier == Tiers.IRON) {
            return "铁制";
        }
        if (tier == Tiers.DIAMOND) {
            return "钻石";
        }
        if (tier == Tiers.NETHERITE) {
            return "下界合金";
        }
        if (tier == Tiers.GOLD) {
            return "原版金制";
        }
        return "金制（自定义）";
    }

    /** 面板/HUD 用：这个境界能徒手挖什么。 */
    public static String describeToolCoverage(CultivationData data) {
        if (data == null || !data.isAwakened()) {
            return "空手（只能用对工具）";
        }
        Tier tier = realmTier(data);
        if (tier == null) {
            return "空手（只能用对工具）";
        }
        if (data.realm().canBreakAnything()) {
            return "镐斧铲锄全通 + 可破坏一切";
        }
        return "镐斧铲锄全通（"
                + describeTierObject(tier) + "级）";
    }
}
