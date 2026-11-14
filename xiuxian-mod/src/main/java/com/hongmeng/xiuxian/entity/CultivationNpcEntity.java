package com.hongmeng.xiuxian.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

/**
 * 修仙界 NPC 的共同父类：门派长老、炼丹师、云游商人。
 *
 * <p><b>他们不是怪。</b>和 {@link CultivationMonsterEntity} 那条线彻底分开：</p>
 * <ul>
 *   <li>继承 {@link PathfinderMob} 而不是 {@code Monster}——
 *       于是 {@code MobScaling} 的境界缩放<b>不会</b>作用到他们身上（它只认 {@code Monster}），
 *       村里长老不会因为玩家到了化神就变成血牛；</li>
 *   <li>{@link #setPersistenceRequired()} + {@link #removeWhenFarAway} = false：
 *       永远不刷掉。结构里刷出来的 NPC 走开两百格再回来，人还在；</li>
 *   <li>{@link #isPushable()} = false：玩家挤不动他们，免得被推进岩浆；</li>
 *   <li>{@link #canBeSeenAsEnemy()} = false：怪物的索敌判定直接跳过 NPC——
 *       这是"自家怪别打自家 NPC"最省事、也最可靠的一道闸；</li>
 *   <li>和平难度不消失（{@link #shouldDespawnInPeaceful()} = false）。</li>
 * </ul>
 *
 * <h2>右键交互</h2>
 *
 * <p>面板在<b>客户端</b>开、状态在<b>服务端</b>改。原版 1.20.1 的
 * {@code MultiPlayerGameMode#interact} 只在旁观模式下拦下 {@code Entity#interact}
 * （已用 javap 核对过字节码），所以生存与创造模式下客户端都会走到
 * {@link #mobInteract}——不需要为"打开界面"额外发一个自定义包。</p>
 *
 * @see SectElderEntity
 * @see AlchemyMasterEntity
 * @see WanderingMerchantEntity
 */
public abstract class CultivationNpcEntity extends PathfinderMob {

    /** 右键之后弹哪一张面板；具体画面由客户端决定。 */
    public enum Panel {
        /** 长老：一段提示文字。 */
        ELDER,
        /** 炼丹师：丹药闲谈。 */
        ALCHEMY,
        /** 云游商人与丹药商人：直接开坊市。 */
        MERCHANT,
        /** 炼器师（第四批）：装备与修理的闲谈。 */
        SMITH,
        /** 秘境向导（第四批）：四个维度怎么去、遗迹在哪。 */
        GUIDE,
        /** 悬赏牌看板（第四批）：今天的悬赏条目。 */
        BOUNTY
    }

    protected CultivationNpcEntity(EntityType<? extends CultivationNpcEntity> type, Level level) {
        super(type, level);
        // 结构里刷出来的 NPC 必须是"常住人口"：不刷掉、不消失
        this.setPersistenceRequired();
        this.xpReward = 0;
        this.setCustomName(Component.literal("\u00a7e" + npcTitle()));
        this.setCustomNameVisible(true);
    }

    // ------------------------------------------------------------------
    // 子类要回答的三件事
    // ------------------------------------------------------------------

    /** 名牌与面板标题，例如"门派长老"。 */
    public abstract String npcTitle();

    /** 面板正文（客户端与服务端都能算，纯字符串）。 */
    public abstract List<String> dialogueLines();

    /** 右键时弹哪张面板。 */
    public abstract Panel panel();

    /** 右键音效；默认是村民的"嗯？"。 */
    public SoundEvent interactSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    /**
     * 这位 NPC 会不会自己溜达。
     *
     * <p>默认会。但<b>悬赏牌看板</b>是一块牌子——它会走就成笑话了，
     * 所以那一类覆盖成 false，{@link #registerGoals()} 里连"闲逛"目标都不会加。</p>
     */
    public boolean canWander() {
        return true;
    }

    // ------------------------------------------------------------------
    // 不刷掉、推不动、不被当敌人
    // ------------------------------------------------------------------

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    /**
     * 怪物索敌时会问这一句；NPC 永远回答"不"。
     *
     * <p>比一个个去改怪的 {@code targetSelector} 便宜得多，而且对本模组之外的
     * 模组怪也一起生效。</p>
     */
    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0F));
        // 牌子不会溜达（见 canWander）：不加这一条，"悬赏牌看板"就会在村里散步
        if (this.canWander()) {
            this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6D, 80));
        }
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    // ------------------------------------------------------------------
    // 右键
    // ------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            // 面板只在客户端开；服务端那份走下面的分支做状态变更
            this.openClientPanel();
            return InteractionResult.SUCCESS;
        }
        this.playSound(this.interactSound(), 1.0F, 1.0F);
        this.onServerInteract(player);
        return InteractionResult.CONSUME;
    }

    /** 客户端界面类的**字符串**名 —— 服务端不会因它而链接客户端类。 */
    private static final String CLIENT_PANEL =
            "com.hongmeng.xiuxian.entity.CultivationNpcEntity$ClientPanel";

    /**
     * 客户端专用：打开 NPC 对话面板。
     *
     * <h2>⚠ 为什么用反射而不是 DistExecutor（踩过一次专用服务端崩溃）</h2>
     * <p>原来的写法是
     * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NpcDialogueScreen.open(...))}。
     * 问题在于：**即使服务端不会执行这条 lambda，JVM 链接它时也要解析
     * {@code NpcDialogueScreen}**（它 {@code extends Screen}），
     * FML 的 {@code RuntimeDistCleaner} 于是抛
     * <i>"Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist
     * DEDICATED_SERVER"</i> —— 在专用服务端上一点 NPC 就报错。</p>
     *
     * <p>现在改成：先判 {@link net.minecraftforge.fml.loading.FMLEnvironment#dist}，
     * 再用 {@code Class.forName} 加载持有者。服务端走不到那一步，
     * 编译到的也只是一句字符串。</p>
     */
    private void openClientPanel() {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                != net.minecraftforge.api.distmarker.Dist.CLIENT) {
            return;
        }
        String title = this.npcTitle();
        List<String> lines = this.dialogueLines();
        Panel which = this.panel();
        try {
            Class<?> holder = Class.forName(CLIENT_PANEL);
            holder.getMethod("open", Panel.class, String.class, List.class)
                    .invoke(null, which, title, lines);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[hongmeng] NPC 对话面板打不开：{}", t.toString());
        }
    }

    /**
     * <b>客户端专用持有者</b>：真正碰 {@code Screen} 的那一段全在这里，
     * 只会被 {@link #openClientPanel()} 通过反射加载。
     */
    public static final class ClientPanel {

        private ClientPanel() {
        }

        /** 客户端才存在：开对话/商店面板。 */
        public static void open(Panel which, String title, List<String> lines) {
            com.hongmeng.xiuxian.client.NpcDialogueScreen.open(which, title, lines);
        }
    }

    /**
     * 服务端专用：真正影响存档的事在这里做。
     *
     * <p>默认只记一笔"见过面"——NPC 的"作用"就是把话说给你听。子类要发东西、
     * 记标记就覆盖这里，别在客户端分支里改世界。</p>
     */
    protected void onServerInteract(Player player) {
        // 记录"见过面"，方便以后做任务链时判断玩家有没有拜访过
        net.minecraft.resources.ResourceLocation id =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
        if (id == null) {
            return;
        }
        String key = "hongmeng_met_" + id.getPath();
        if (player.getPersistentData().getBoolean(key)) {
            return;
        }
        player.getPersistentData().putBoolean(key, true);
        // 第一次见面给一份"见面礼"（子类可覆盖；默认没有）
        this.giveFirstMeetingGift(player);
    }

    /**
     * 第一次右键时给的东西。
     *
     * <p><b>只在第一次</b>给：标记写在玩家自己的 {@code persistentData} 里
     * （跨维度、跨死亡都保留），所以刷不出来，也不需要任何额外的存档结构。</p>
     *
     * <p>默认什么都不给——只有真正"做买卖 / 发悬赏"的 NPC 覆盖它。
     * 给的东西刻意是<b>消耗品</b>而不是钱：见面礼是让玩家"愿意再点一次"的钩子，
     * 不是经济来源。</p>
     */
    protected void giveFirstMeetingGift(Player player) {
    }

    /**
     * 把一份物品塞给玩家；背包满了就掉在脚下。
     *
     * <p>走 {@code addItem} 的返回值判断有没有塞进去，塞不进就 {@code drop}，
     * 免得"背包满了右键一次东西就没了"这种最容易被骂的小毛病。</p>
     */
    protected void gift(Player player, net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /**
     * 给玩家一句系统提示。
     *
     * <p>面板由客户端画，但"东西真的到手了"这件事必须由<b>服务端</b>说，
     * 否则玩家会怀疑自己是不是空手而归。</p>
     */
    protected void tell(Player player, String text) {
        player.displayClientMessage(Component.literal(text), false);
    }
}
