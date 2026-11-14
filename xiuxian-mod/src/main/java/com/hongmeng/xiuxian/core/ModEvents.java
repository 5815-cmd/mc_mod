package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.item.ModItems;
import com.hongmeng.xiuxian.compat.HongmengConfig;
import com.hongmeng.xiuxian.network.ModNetwork;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import com.hongmeng.xiuxian.network.PainFlashPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 游戏内事件总集：觉醒触发、死亡业力、杀怪涨修为、真实痛觉、劫掠清算、境界属性。
 */
public class ModEvents {

    /** 待觉醒队列：进世界后延迟若干 tick 再觉醒，别糊在加载界面上。 */
    private static final Map<UUID, Integer> PENDING_AWAKEN = new HashMap<>();

    /** 已经领过"首次击杀大敌"奖励的玩家。 */
    private static final Set<UUID> BOSS_REWARDED = new HashSet<>();

    /** 已进过世界的玩家。 */
    private static final Set<UUID> SEEN = new HashSet<>();

    /** 痛觉衰减计时。 */
    private static final Map<UUID, Integer> PAIN_DECAY = new HashMap<>();

    /**
     * 玩家上一次被应用属性时的"大境序号"。
     *
     * <p>用来检测境界变化——变了就立刻重算属性。这是修一个真 bug 加的：
     * 之前 {@code applyRealmAttributes} 只在登录/重生/换维度时调用，
     * 玩家一路涨修为升到帝境，境界加成从来不会重新应用。</p>
     */
    private static final Map<UUID, Integer> LAST_REALM_TIER = new HashMap<>();

    /** 待送上云层的玩家（进世界后延迟若干 tick 再传，等区块加载完）。 */
    private static final Map<UUID, Integer> PENDING_SKY_LIFT = new HashMap<>();

    /**
     * 反伤递归深度（防两个"受伤反弹"单位互相反弹形成无限循环）。
     *
     * <p>服务端事件是单线程的，所以一个 int 就够；正常一层反伤 depth=1，
     * 到 2 就停止继续反弹 —— 反伤仍然生效，只是不会无限乒乓。</p>
     */
    private static int REFLECT_DEPTH = 0;

    // 属性修饰符的固定 UUID
    private static final UUID HEALTH_MOD_ID = UUID.nameUUIDFromBytes("hongmeng_realm_health".getBytes(StandardCharsets.UTF_8));
    private static final UUID DAMAGE_MOD_ID = UUID.nameUUIDFromBytes("hongmeng_realm_damage".getBytes(StandardCharsets.UTF_8));
    private static final UUID ROOT_HEALTH_MOD_ID = UUID.nameUUIDFromBytes("hongmeng_root_health".getBytes(StandardCharsets.UTF_8));
    private static final UUID ROOT_DAMAGE_MOD_ID = UUID.nameUUIDFromBytes("hongmeng_root_damage".getBytes(StandardCharsets.UTF_8));
    private static final UUID REALM_ARMOR_MOD_ID = UUID.nameUUIDFromBytes("hongmeng_realm_armor".getBytes(StandardCharsets.UTF_8));
    private static final String REALM_ARMOR_MOD_NAME = "hongmeng_realm_armor";
    private static final UUID SECT_DAMAGE_MOD_ID = UUID.nameUUIDFromBytes("hongmeng_sect_damage".getBytes(StandardCharsets.UTF_8));
    private static final UUID SPEED_MOD_ID = UUID.nameUUIDFromBytes("hongmeng_realm_speed".getBytes(StandardCharsets.UTF_8));
    private static final UUID SPEED2_MOD_ID = UUID.nameUUIDFromBytes("hongmeng_realm_speed2".getBytes(StandardCharsets.UTF_8));

    // 触及距离（手的长度）：BLOCK_REACH / ENTITY_REACH 是 Forge 加的属性，
    // 不在 AttributeCaps.relax() 那份名单里（那份只管生命/攻击/护甲/速度）。
    // 它们的原版上限是 1024，本模组最多加 5 格 —— 上限自检在 ReachTable.capReport()，
    // 由 HongmengMod 在 common setup 里打一行日志。
    private static final UUID REACH_MOD_ID = UUID.nameUUIDFromBytes("hongmeng_realm_reach".getBytes(StandardCharsets.UTF_8));
    private static final UUID ENTITY_REACH_MOD_ID = UUID.nameUUIDFromBytes("hongmeng_realm_entity_reach".getBytes(StandardCharsets.UTF_8));

    private static final String HEALTH_MOD_NAME = "hongmeng_realm_health";
    private static final String DAMAGE_MOD_NAME = "hongmeng_realm_damage";
    private static final String ROOT_HEALTH_MOD_NAME = "hongmeng_root_health";
    private static final String ROOT_DAMAGE_MOD_NAME = "hongmeng_root_damage";
    private static final String SECT_DAMAGE_MOD_NAME = "hongmeng_sect_damage";
    private static final String SPEED_MOD_NAME = "hongmeng_realm_speed";
    private static final String SPEED2_MOD_NAME = "hongmeng_realm_speed2";
    private static final String REACH_MOD_NAME = "hongmeng_realm_reach";
    private static final String ENTITY_REACH_MOD_NAME = "hongmeng_realm_entity_reach";

    // ------------------------------------------------------------------
    // 登录 / 重生 / 维度切换
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        // 第三次核对属性上限：登录是"最后一次能确认"的时机。
        // 属性被别的模组覆盖的话，玩家会看到血条卡在 1024，很难排查。
        com.hongmeng.xiuxian.compat.AttributeCaps.relax("玩家登录");
        ModNetworkBridge.sync(player);
        applyRealmAttributes(player, data);
        dumpAttributeDiagnostics(player, "登录时");
        // 御剑状态跨存档不保留，但客户端那个渲染标记要重置一次：
        // 不重置的话上次退出时"脚下有剑"的缓存会让新世界第一帧闪一把剑出来

        // 开局演出：还没选难度的玩家先送到云层之上。
        // 客户端会在进世界后弹难度界面，选完由 ChooseDifficultyPacket 送他落回地面。
        // 之所以放在登录时而不是构造时：这时玩家的坐标、维度都已经就绪。
        if (!data.hasChosenDifficulty()) {
            // 延后一点，等客户端把区块加载完再传送，否则会看到"掉进虚空"的一帧
            PENDING_SKY_LIFT.put(player.getUUID(), 40);
        }

        boolean firstTime = SEEN.add(player.getUUID());
        if (!data.isAwakened() && HongmengConfig.SERVER.awakenOnJoin.get()) {
            int delay = HongmengConfig.SERVER.awakenDelayTicks.get();
            PENDING_AWAKEN.put(player.getUUID(), Math.max(1, delay));
            if (firstTime) {
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("\u00a7b\u00a7l【鸿蒙修仙传】"));
                player.sendSystemMessage(Component.literal(
                        "\u00a77\u201c欢迎进入鸿蒙修仙传！系统正在为你分配初始灵根。\u201d"));
                player.sendSystemMessage(Component.literal(
                        "\u00a78（注意：本世界没有退出键。这不是 bug。）"));
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal(
                        "\u00a7a新来的？输入 \u00a7f/hongmeng start \u00a7a看新手八步。"));
                player.sendSystemMessage(Component.literal(
                        "\u00a77屏幕右上角会一直显示\u00a7e当前任务\u00a77；按 \u00a7fJ\u00a77 看全部任务。"));
            }
        } else if (firstTime) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77你回来了。当前境界：\u00a7e" + data.realm().cn()));
        }

        if (data.getKarma() > 0) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c业力缠身：" + data.getKarma() + " 层，突破失败率 +"
                            + (data.getKarma() * 10) + "%"));
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        CultivationData oldData = CultivationCapability.get(event.getOriginal());
        CultivationData newData = CultivationCapability.get(event.getEntity());
        newData.copyFrom(oldData);
        event.getOriginal().invalidateCaps();

        if (event.getEntity() instanceof ServerPlayer player) {
            applyRealmAttributes(player, newData);
            ModNetworkBridge.sync(player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyRealmAttributes(player, CultivationCapability.get(player));
            ModNetworkBridge.sync(player);
            // 离开秘境（或任何换维度）时，挑战作废
            if (!player.level().dimension().equals(
                    com.hongmeng.xiuxian.epoch.EpochRealm.EPOCH_REALM)) {
                com.hongmeng.xiuxian.epoch.EpochRealm.abort(player);
            }
            // 腐化地带：真世界已开启时，在玩家附近撒种子（有上限、会去重）
            try {
                if (com.hongmeng.xiuxian.realworld.RealWorld.active(player.serverLevel())
                        && player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    com.hongmeng.xiuxian.realworld.Corruption.seedAround(player, sl);
                }
            } catch (Throwable ignored) {
                // 撒种失败不影响换维度
            }
        }
    }

    /**
     * <b>换装即刷新</b>装备类加成，不等那最多 1 秒的周期刷新。
     *
     * <p>⚠ 加这条的原因：`PathArmorBonus.refresh` 挂在 `GearManager.refresh` 里，
     * 而后者只在 {@code tickCount % 20 == 0} 时跑 —— 每 20 tick（1 秒）一次。</p>
     *
     * <p>于是玩家<b>穿齐第 4 件的瞬间，四件套要等最多 1 秒才生效</b>。
     * 四件套强化的是"激荡获取/缓伤比例"这类<b>看不到的机制参数</b>，
     * 玩家很可能在这 1 秒里就打出一刀，然后觉得"套装没用"。</p>
     *
     * <p>换装事件本身是低频的（只在真正换装时触发），没有性能顾虑。</p>
     */
    @SubscribeEvent
    public void onEquipmentChanged(
            net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            // 旧道途护甲（legacy 过渡）：按新职业→旧道途桥结算；
            // R-2 新职业甲上线后删桥、改由 CareerArmorBonus 结算。
            com.hongmeng.xiuxian.item.armor.PathArmorBonus.refresh(player,
                    com.hongmeng.xiuxian.item.LegacyGearBridge.toLegacyPath(
                            com.hongmeng.xiuxian.talent.Talents.pathOf(player)));
            // 生命上限 / 抗击退（属性修饰符）
            com.hongmeng.xiuxian.talent.TalentAttributes.refresh(player);
            // 主手武器的攻速差异
            com.hongmeng.xiuxian.item.gear.PathWeaponBonus.refresh(player);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[gear] 换装刷新失败：{}", t.toString());
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CultivationData data = CultivationCapability.get(player);
            applyRealmAttributes(player, data);
            data.setPainLevel(0);
            // 死亡会重置 abilities —— 飞行必须在重生后重新给，
            // 否则飞升以上的玩家死一次就再也飞不起来了
            if (data.realm().hasFlight()) {
                applyFlight(player, true);
            }
            LAST_REALM_TIER.put(player.getUUID(), data.realm().tier());
            // ---- 道途机制资源必须归零 ----
            //
            // ⚠ 原来 `PathRuntime.forget` **只在登出时**调用，死亡/重生都不清。
            //   于是死后重生还留着满血池 + 满魔气 → 一起身就吃最高增伤
            //   （血修 ×1.60 / 魔修 ×1.34），缓伤里挂的伤害也会带进新的一条命。
            //
            //   一条命该有一套资源，死了就该清空。天赋点与道途不受影响
            //   （那些存在 Talents / NBT 里，与此处无关）。
            try {
                com.hongmeng.xiuxian.talent.PathRuntime.forget(player.getUUID());
            } catch (Throwable t) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                        "[path] 重生重置失败：{}", t.toString());
            }
            // 领域也归位（否则尸体会继续开着领域打怪）
            try {
                com.hongmeng.xiuxian.domain.DomainRuntime.forget(player.getUUID());
            } catch (Throwable ignored) {
                // 领域系统不可用就跳过
            }
            // Boss 出场演出的无敌：死在演出里也要摘掉
            try {
                com.hongmeng.xiuxian.combat.BossCinematic.forget(player);
            } catch (Throwable ignored) {
                // 演出系统不可用就跳过
            }
            // 法术演出的无敌同理
            try {
                com.hongmeng.xiuxian.combat.SpellCinematic.endCam(player);
            } catch (Throwable ignored) {
                // 同上
            }
            // 持续法术（黑洞/余震/余焰…）也要清。
            //
            // ⚠ `SpellAftermath.clear` 的注释写着"玩家离开/死亡时清掉
            //   （避免残留继续扣血）"，但**全项目从来没有调用过它** ——
            //   于是死后残留法术会继续打怪；重生换维度之后，
            //   它还会在**新维度里的旧坐标**生成特效与伤害。
            try {
                com.hongmeng.xiuxian.technique.SpellAftermath.clear(player.getUUID());
            } catch (Throwable ignored) {
                // 法术余韵系统不可用就跳过
            }
            // 复活直接回满血（玩家要求）。
            // 注意顺序：applyRealmAttributes 已经先跑过，血量上限是对的，
            // 这里 setHealth 才不会被旧上限截断。
            healToFull(player);
            ModNetworkBridge.sync(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 清理该玩家的进度缓存（否则这两个 Map 会一直涨）
        // 注意：QuestChain 在 core 包（不是 quest 包）
        com.hongmeng.xiuxian.core.QuestChain.forget(event.getEntity().getUUID());
        // 演出进行中掉线：清账，否则无敌状态会留在他的玩家数据里
        // ⚠ 必须传 player 而不是 uuid —— 否则只删记录、不解除无敌，
        //   而 Invulnerable 会写进 NBT -> 玩家永久无敌。
        if (event.getEntity() instanceof ServerPlayer logoutPlayer) {
            com.hongmeng.xiuxian.combat.BossCinematic.forget(logoutPlayer);
            // 法术演出的无敌也要摘（否则下线后 Invulnerable 留在 NBT 里）
            try {
                com.hongmeng.xiuxian.combat.SpellCinematic.endCam(logoutPlayer);
            } catch (Throwable ignored) {
                // 同上
            }
            // 格挡状态
            try {
                com.hongmeng.xiuxian.entity.EliteEntrance
                        .forget(logoutPlayer.getUUID());
            } catch (Throwable ignored) {
                // 同上
            }
            // 攻击动作的连击计数也要清（否则重登会接着上次的连击）
            try {
                com.hongmeng.xiuxian.combat.CombatAnim.forget(logoutPlayer.getUUID());
            } catch (Throwable ignored) {
                // 同上
            }
        } else {
            com.hongmeng.xiuxian.combat.BossCinematic.forget(
                    event.getEntity().getUUID());
        }
        com.hongmeng.xiuxian.sword.SwordFormationRuntime.forget(
                event.getEntity().getUUID());
        // ⚠ 这三个**不能**在这里 forget！
        //
        //   `TalentSave.saveByTag` 是从这三个类的**内存 Map** 里读数据写进 NBT 的，
        //   而玩家 NBT 的保存发生在实体移除时 —— **晚于**本事件。
        //   所以这里一清空，后面存进去的就是空数据：
        //       **每次下线都丢道途、天赋点、副业熟练度。**
        //
        //   内存代价只是"每个玩家几条 int"，留到服务端关闭即可。
        //   （其余几个 forget 是纯运行期缓存，没有 NBT 依赖，清掉是对的。）
        com.hongmeng.xiuxian.item.armor.PathArmorBonus.forget(
                event.getEntity().getUUID());
        com.hongmeng.xiuxian.item.armor.CareerArmorHandler.forget(
                event.getEntity().getUUID());
        com.hongmeng.xiuxian.achievement.CultivationAchievements.clear(
                event.getEntity().getUUID());
        UUID id = event.getEntity().getUUID();
        LAST_REALM_TIER.remove(id);
        PENDING_AWAKEN.remove(id);
        PAIN_DECAY.remove(id);
        PENDING_SKY_LIFT.remove(id);
        SkyIntro.forget(id);
        // 持续法术：不清会**永不释放**（每个下线玩家留一条在 ACTIVE 里）
        try {
            com.hongmeng.xiuxian.technique.SpellAftermath.clear(id);
        } catch (Throwable ignored) {
            // 法术余韵系统不可用就跳过
        }
        Breakthrough.clearTribulation(id);
        // 御剑状态是纯运行期的：掉线就清，免得下次进来"人在地上却在扣灵力"
    }

    // ------------------------------------------------------------------
    // 每 tick：觉醒倒计时、打坐、痛觉、丹药计时、劫掠清算
    // ------------------------------------------------------------------

    /**
     * <b>把玩家的装备锁成"耐久无法破坏"</b>（玩家要求）。
     *
     * <p>做法是给有耐久的物品打上原版的 {@code Unbreakable} 标记 ——
     * 打上之后原版**不再扣任何耐久**，而且物品提示里会显示"无法破坏"，
     * 观感清楚（不会像"耐久快见底"那样让人担心）。</p>
     *
     * <p>覆盖范围：主手、副手、四件护甲、以及**快捷栏与背包里的装备**。
     * 只处理"本身有耐久"的物品，工具/武器/护甲都算，
     * 而方块、材料这些没有耐久的会被自动跳过。</p>
     *
     * <p>开销：每 20 tick 一次；已打标记的物品由 {@code hasTag} 短路，
     * 所以稳定状态下几乎零成本。</p>
     */
    private static void lockDurability(net.minecraft.server.level.ServerPlayer player) {
        try {
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                markUnbreakable(inv.getItem(i));
            }
            for (net.minecraft.world.entity.EquipmentSlot slot
                    : net.minecraft.world.entity.EquipmentSlot.values()) {
                markUnbreakable(player.getItemBySlot(slot));
            }
        } catch (Throwable t) {
            // 绝不能因为锁耐久失败而影响游戏
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 耐久锁定失败：{}", t.toString());
        }
    }

    /** 给一件有耐久的物品打上 Unbreakable 标记（幂等）。 */
    private static void markUnbreakable(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
            return;
        }
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean("Unbreakable")) {
            return;                     // 已经打过，直接走
        }
        stack.getOrCreateTag().putBoolean("Unbreakable", true);
    }

    /**
     * <b>每 tick 推进延迟特效队列</b>。
     *
     * <p>四段式演出靠 {@code SpellFxRuntime.sendLater(...)} 排队，
     * <b>不 tick 就永远不会发</b> —— 这正是本项目反复出现的写了没接。</p>
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        try {
            com.hongmeng.xiuxian.technique.SpellFxRuntime.tickScheduled(
                    event.getServer().overworld().getGameTime());
            // 僵直到期恢复（不恢复的话那只怪永久变木桩）
            try {
                // 腐化地带：真世界开启后缓慢蔓延（有硬上限与人工方块保护）
                com.hongmeng.xiuxian.realworld.Corruption.tick(
                        event.getServer().overworld());
                com.hongmeng.xiuxian.combat.StunScheduler.tick(
                        event.getServer().overworld().getGameTime());
            } catch (Throwable ignored) {
                // 恢复失败不影响主循环
            }
            // 真世界演出的延迟动作（雷声/闪屏在同一场演出里要有先后）
            com.hongmeng.xiuxian.realworld.RealWorldScheduler.tick(
                    event.getServer().overworld().getGameTime());
        } catch (Throwable ignored) {
            // 特效调度失败绝不能影响主循环
        }
    }

    /**
     * 停服清理：把**跨世界残留会出事**的静态队列/表清掉。
     *
     * <p>PERF(B)/正确性：单人模式"退出到标题再进另一个世界"时 JVM 不重启，
     * 这些静态表会活到下一个存档里：</p>
     * <ul>
     *   <li>{@code SpellDestruction.QUEUE} —— 排队中的方块破坏会按**旧世界坐标**
     *       在新世界继续挖（真实事故级别），且最多占十几 MB；</li>
     *   <li>{@code SpellFxRuntime.QUEUE} —— 延迟特效会在新世界乱放，并拽住旧
     *       {@code ServerLevel} 无法回收；</li>
     *   <li>{@code StunScheduler} / {@code NativeBossSpawner} —— 过期僵直任务、
     *       旧世界的 boss UUID 会残留。</li>
     * </ul>
     *
     * <p>纯清理、不碰玩法状态；异常吞掉（清不掉也不能影响关服）。</p>
     */
    @SubscribeEvent
    public void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        try {
            com.hongmeng.xiuxian.technique.SpellDestruction.clear();
        } catch (Throwable ignored) {
            // 清理失败不影响关服
        }
        try {
            com.hongmeng.xiuxian.technique.SpellFxRuntime.clear();
        } catch (Throwable ignored) {
            // 同上
        }
        try {
            com.hongmeng.xiuxian.combat.StunScheduler.clear();
        } catch (Throwable ignored) {
            // 同上
        }
        try {
            com.hongmeng.xiuxian.entity.NativeBossSpawner.clear();
        } catch (Throwable ignored) {
            // 同上
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        // ---- 装备耐久无法破坏（玩家要求）----
        //
        // 玩家原话：「玩家的装备耐久要无法破坏」。
        //
        // 做法：给有耐久的装备打上原版的 `Unbreakable` 标记 ——
        // 打上之后原版就**不再扣任何耐久**，观感上也不会出现"耐久快见底"。
        // 每 20 tick 扫一次背包就够（已打标记的会被 hasTag 短路，开销极小）。
        if (player.level().getGameTime() % 20L == 0L) {
            lockDurability(player);
        }

        // 云层之上：延迟 40 tick 再传送，等客户端把区块加载完，
        // 否则玩家会看到"掉进虚空"的一帧
        Integer skyLift = PENDING_SKY_LIFT.get(player.getUUID());
        if (skyLift != null) {
            int left = skyLift - 1;
            if (left <= 0) {
                PENDING_SKY_LIFT.remove(player.getUUID());
                SkyIntro.lift(player);
            } else {
                PENDING_SKY_LIFT.put(player.getUUID(), left);
            }
        }

        // 觉醒倒计时
        Integer pending = PENDING_AWAKEN.get(player.getUUID());        if (pending != null) {
            CultivationData data0 = CultivationCapability.get(player);
            if (!data0.hasChosenDifficulty()) {
                // 还没选难度：等他选完再抽灵根。
                // 客户端会在进世界后弹难度界面，选完由 ChooseDifficultyPacket 触发觉醒。
                PENDING_AWAKEN.put(player.getUUID(), 40);
            } else {
                int left = pending - 1;
                if (left <= 0) {
                    PENDING_AWAKEN.remove(player.getUUID());
                    if (!data0.isAwakened()) {
                        Awakening.awaken(player, data0, "天地");
                    }
                } else {
                    PENDING_AWAKEN.put(player.getUUID(), left);
                }
            }
        }

        CultivationData data = CultivationCapability.get(player);

        // 打坐
        boolean meditating = false;
        if (MeditationTracker.onCushion(player)) {
            meditating = true;
            MeditationTracker.track(player);
            QuestChain.onMeditateTick(player, data);
            if (player.tickCount % 20 == 0) {
                com.hongmeng.xiuxian.quest.QuestManager.onMeditateSecond(player);
            }
        } else {
            MeditationTracker.stop(player);
        }

        // ---- 灵力：御剑结算 + 回复 ----
        //
        // 再跑回复。御剑中的那一 tick 一律不回蓝 —— 否则御剑就成了无限续航。
        // 维度进度锁：没带凭证就叠侵蚀（见 dungeon/RealmProgression）
        com.hongmeng.xiuxian.dungeon.RealmProgression.tickMiasma(player);
        boolean airborne = !player.onGround() && !player.isInWater()
                && !player.onClimbable();
        // 灵力条要实时，但没必要每 tick 发包：每 10 tick（0.5 秒）推一次
        if (player.tickCount % 10 == 0) {
            com.hongmeng.xiuxian.network.ManaSyncPacket.send(player, data.getMana());
        }

        // 痛觉自然衰减
        if (data.getPainLevel() > 0) {
            int timer = PAIN_DECAY.getOrDefault(player.getUUID(), 0) + 1;
            if (timer >= 120) {
                timer = 0;
                data.setPainLevel(data.getPainLevel() - 1);
            }
            PAIN_DECAY.put(player.getUUID(), timer);
        }

        if (player.tickCount % 20 == 0) {
            Breakthrough.tickPillBoosts();
        }

        // 门派：叛徒有人来清理门户
        if (player.tickCount % 40 == 0) {
            com.hongmeng.xiuxian.sect.SectManager.tickBetrayal(player, data);
        }

        // 纪元秘境：波次推进
        com.hongmeng.xiuxian.epoch.EpochRealm.tick(player, data);

        // 副本怪潮：分批入场（每 20 tick 放一批，做出"潮水一波波涌上来"的观感）
        com.hongmeng.xiuxian.dungeon.DungeonRealm.tickSiege(player);

        // 副本禁足：20×20 区块（320×320 格），打完首领才解除
        com.hongmeng.xiuxian.dungeon.DungeonRealm.tickBanField(player);

        // 法术改过的天气到期后恢复原状（免得永久改掉玩家的世界）
        if (player.level() instanceof net.minecraft.server.level.ServerLevel srv) {
            com.hongmeng.xiuxian.technique.SpellWeather.tick(srv);
            com.hongmeng.xiuxian.technique.SpellDestruction.tickQueue(srv);
                // Boss 出场：从天上缓缓降落
                com.hongmeng.xiuxian.combat.BossDescent.tick(srv);
            // 演出收尾：到时解除无敌、给客户端发 stop
            com.hongmeng.xiuxian.combat.BossCinematic.tick(srv);
            // 飞剑剑阵：每 tick 算环绕位 + 独立冷却 + 自动索敌 + 穿刺
            com.hongmeng.xiuxian.sword.SwordFormationRuntime.tick(player);
            // 道途机制：激荡计时 / 缓伤结算 / 血池回血 / 魔气消散 / 阵域
            com.hongmeng.xiuxian.talent.PathRuntime.tick(player);
            // 道途武器攻速差异（主手是道途武器时才施加）
            //
            // PERF(B)：原先是**每 tick** 调一次，而 refresh() 是"先摘再挂"一个
            //   ATTACK_SPEED 属性修饰符 —— 每 tick 摘挂会让属性实例每 tick 标脏、
            //   每 tick 触发一次属性同步包，并反复重算属性快照。
            //   换装事件（onEquipmentChanged 第 225 行）已经**即时**刷新过了，
            //   这里降到 20 tick 兜底一次即可（主手切换也走装备变更事件）。
            if (player.tickCount % 20 == 0) {
                com.hongmeng.xiuxian.item.gear.PathWeaponBonus.refresh(player);
            }
            // 天赋的属性型加成（生命上限 / 抗击退）。
            //
            // ⚠ **不要每 tick 刷**：这个方法是"先摘再挂"两个属性修饰符，
            //   每 tick 做一次会让属性系统不停重算（10 个玩家 = 每秒 400 次增删）。
            //   天赋只在加点/择道时变，那些地方会显式刷新（见 TalentPacket），
            //   这里 10 tick 兜底一次就够，玩家完全感觉不到延迟。
            if (player.tickCount % 10 == 0) {
                com.hongmeng.xiuxian.talent.TalentAttributes.refresh(player);
            }
            // ---- 灵力自然回复（每秒一次）----
            //
            // ⚠ 以前**没有被动回蓝**：`ManaTable.regenPerSecond()` /
            //   `fullRegenTicks()` 全项目零调用，法力只能靠丹药或打坐瞬间回满。
            //   于是被动功法「吐纳诀：灵力回复 ×1.50」在乘一个不存在的东西 ——
            //   `TechniquePassive.manaRegenMultiplier()` 同样零调用。
            //
            // 按秒结算（而不是每 tick 加 0.15）：`setMana` 会 floor 成整数，
            // 每 tick 加分小数会被抹成 0，等于一点没回。
            if (player.tickCount % 20 == 0) {
                try {
                    int maxMana = data.maxMana();
                    if (maxMana > 0 && data.getMana() < maxMana) {
                        double per = maxMana
                                * com.hongmeng.xiuxian.core.ManaTable
                                        .REGEN_PER_SECOND_FRACTION;
                        per *= com.hongmeng.xiuxian.technique.TechniquePassive
                                .manaRegenMultiplier(data.getTechniques());
                        if (per > 0.0D) {
                            com.hongmeng.xiuxian.technique.ManaBridge
                                    .add(player, data, per);
                        }
                    }
                } catch (Throwable ignored) {
                    // 灵力系统不可用就跳过
                }
            }
            // 真世界状态：登录时补一次（否则重进游戏雾色就丢了）
            if (player.tickCount == 20) {
                try {
                    com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(player,
                            new com.hongmeng.xiuxian.network.RealWorldStatePacket(
                                    com.hongmeng.xiuxian.realworld.RealWorld
                                            .active(player.level())));
                } catch (Throwable ignored) {
                    // 同步失败不影响别的
                }
            }
            // 法术演出的无敌到点自动摘（只挂不摘的兜底）
            try {
                com.hongmeng.xiuxian.combat.SpellCinematic.tickCamera(player);
            } catch (Throwable ignored) {
                // 演出系统不可用就跳过
            }
            // 境界门槛：身上有超阶装备就摘下来放回背包（分阶段穿装备的关键）
            com.hongmeng.xiuxian.item.gear.RealmGear.tick(player);
        }
        // 成就判定（原版 advancement 框架，自带弹窗与成就界面）
        com.hongmeng.xiuxian.achievement.CultivationAchievements.tick(player);

        // ---- 下界 / 末地的原生 Boss：低频轮询，玩家得自己去找 ----
        com.hongmeng.xiuxian.entity.NativeBossSpawner.tick(player);


        // 持续法术效果：法术放出去后留在场上几秒，逐 tick 结算与播特效
        com.hongmeng.xiuxian.technique.SpellAftermath.tick(player);

        // 雷劫：道印之后每次突破都要一道道挨
        Breakthrough.tickTribulation(player);

        // 境界挖掘：鸿蒙的强行破坏进度
        RealmMining.tickOmniMining(player);

        // 任务：每秒评估一次完成状态
        if (player.tickCount % 20 == 0) {
            com.hongmeng.xiuxian.quest.QuestManager.tick(player);
            // 装备加成结算（法宝 + 武器增幅）
            com.hongmeng.xiuxian.item.gear.GearManager.refresh(player);

            // 境界属性：每秒核对一次。
            //
            // 这里以前是缺的——applyRealmAttributes 只在登录/重生/换维度时调用，
            // 所以玩家一路从练气涨到帝境，境界加成从来不会重新应用。
            // 表现就是"法宝加成有了，境界加成没有"（法宝走的是上面那行每秒刷新）。
            // 现在只要境界层数变了就立刻重算，顺带每秒兜底一次。
            int nowTier = data.realm().tier();
            Integer lastTier = LAST_REALM_TIER.get(player.getUUID());
            if (lastTier == null || lastTier != nowTier) {
                LAST_REALM_TIER.put(player.getUUID(), nowTier);
                applyRealmAttributes(player, data);
            } else {
                // 兜底：确保数值没被别的系统改掉（例如属性被重置）
                ensureRealmAttributesPresent(player, data);
            }

            // 飞升常驻效果续期（生命回复 I + 夜视）
            if (data.realm().hasFlight()
                    && player.tickCount % ASCENSION_REFRESH_TICKS == 0) {
                applyAscensionEffects(player, data.realm());
            }
        }

        // 劫掠清算：拿过村庄英雄效果就算你替村子出手了
        if (player.tickCount % 60 == 0 && player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            Raid raid = player.level() instanceof net.minecraft.server.level.ServerLevel level
                    ? level.getRaidAt(player.blockPosition()) : null;
            boolean victory = raid != null && raid.isVictory();
            if (victory && !player.getPersistentData().getBoolean("hongmeng_raid_paid")) {
                player.getPersistentData().putBoolean("hongmeng_raid_paid", true);
                BlessingTracker.onRaidCleared(player);
            } else if (!victory) {
                player.getPersistentData().putBoolean("hongmeng_raid_paid", false);
            }
        }

        // 本命领域：八种元素的效果结算（数值表见 domain/DomainEffectTable）。
        // 领域没开、或配置里关掉了领域效果时，这一行什么都不做。
        com.hongmeng.xiuxian.domain.DomainManager.tickField(player, data);
        // 领域限制：时长上限 + 每秒消耗能量（耗尽或超时自动收）
        com.hongmeng.xiuxian.domain.DomainRuntime.tick(player, data);
    }

    // ------------------------------------------------------------------
    // 怪物强度缩放（难度 + 玩家境界）
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onEntityJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)) {
            return;
        }
        // 取最近的玩家作为参照（他的难度与境界决定这只怪多强）
        ServerPlayer nearest = level.getNearestPlayer(mob, 64.0D) instanceof ServerPlayer sp
                ? sp : null;
        com.hongmeng.xiuxian.difficulty.MobScaling.onEntityJoin(level, mob, nearest);

        // 祭坛 boss 在副本维度里落地时，就地建一座**主题祭坛场地**（独立房间）
        // —— 玩家选的是「独立副本」，所以只在副本维度建，绝不动主世界。
        try {
            var dungeon = com.hongmeng.xiuxian.dungeon.DungeonRealm.dungeonAt(level);
            if (dungeon != null) {
                String path = net.minecraft.core.registries.BuiltInRegistries
                        .ENTITY_TYPE.getKey(mob.getType()).getPath();
                var theme = com.hongmeng.xiuxian.dungeon.AltarArena.themeOf(path);
                if (theme != null) {
                    com.hongmeng.xiuxian.dungeon.AltarArena.ensure(
                            level, mob.blockPosition().below(), theme);
                }
            }
        } catch (Throwable ignored) {
            // 场地建造失败不影响刷怪
        }
        // 首领级怪物额外挂「鸿蒙灵根」（玩家要求：BOSS 一定是鸿蒙灵根）。
        // 只对 MobRank.BOSS 生效，普通怪调用是空操作。
        if (mob instanceof com.hongmeng.xiuxian.entity.CultivationMonsterEntity cm) {
            com.hongmeng.xiuxian.difficulty.MobScaling.applyHongmengRootIfBoss(mob, cm.rank());
        }
    }

    // ------------------------------------------------------------------
    // 战斗类被动（玩家要求：吸血 / 增伤 / 减伤 / 闪避 / 攻击）
    // ------------------------------------------------------------------

    /**
     * 玩家打出去的伤害：**破军诀**（增伤）与**血魔功**（吸血）。
     *
     * <p>只认玩家本人打出的（近战与法术都会走 {@code LivingHurtEvent}），
     * 且不吃"自己打自己"这种异常情形。</p>
     */
    @SubscribeEvent
    public void onPlayerDealsDamage(
            net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        try {
            if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
                return;
            }
            if (event.getEntity() == player) {
                return;
            }
            var data = CultivationCapability.get(player);
            if (data == null) {
                return;
            }
            var tech = data.getTechniques();

            // 破军诀：增伤
            double amp = com.hongmeng.xiuxian.technique.TechniquePassive
                    .amplifyMultiplier(tech);
            if (amp != 1.0D) {
                event.setAmount((float) (event.getAmount() * amp));
            }

            // ---- 规则之力（玩家要求：帝境/鸿蒙解锁）----
            final int tier = data.realm().tier();
            // 寂灭（帝境）：造成伤害 +30%
            double annihilate = com.hongmeng.xiuxian.core.RealmPowers
                    .annihilateMultiplier(tier);
            if (annihilate != 1.0D) {
                event.setAmount((float) (event.getAmount() * annihilate));
            }
            // 因果（鸿蒙）：25% 概率伤害翻倍
            double causal = com.hongmeng.xiuxian.core.RealmPowers.causalChance(tier);
            if (causal > 0.0D && player.getRandom().nextDouble() < causal) {
                event.setAmount(event.getAmount() * 2.0F);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a75因果之力\u00a77——这一击命中注定"), true);
            }

            // 血魔功：吸血（按**最终伤害**的比例回血，不回超过上限）
            double steal = com.hongmeng.xiuxian.technique.TechniquePassive
                    .lifesteal(tech);
            if (steal > 0.0D) {
                float heal = (float) (event.getAmount() * steal);
                if (heal > 0.0F && player.getHealth() < player.getMaxHealth()) {
                    player.heal(heal);
                }
            }

            // ---- 天赋树的增伤（近战 / 斩杀 / 天赋吸血）----
            //
            // ⚠ `TalentBonus` 的这些方法以前**全是零调用** ——
            //   面板显示"+12% 近战伤害"，实际一点没加。
            try {
                boolean spell = event.getSource().is(
                        net.minecraft.world.damagesource.DamageTypes.MAGIC);
                // 近战加成只在非法术伤害上生效（法术走 TechniqueCaster 那条路）
                if (!spell) {
                    double melee = com.hongmeng.xiuxian.talent.TalentBonus
                            .meleeDamage(player);
                    if (melee > 0.0D) {
                        event.setAmount((float) (event.getAmount() * (1.0D + melee)));
                    }
                    // 破防：按目标实际减免率折算
                    // 职业被动【锋芒】（剑修 护甲穿透 +10%）与天赋/职业专属同一口径叠加
                    double pen = com.hongmeng.xiuxian.talent.TalentBonus
                            .penetration(player)
                            + com.hongmeng.xiuxian.talent.CareerPassives
                                    .penetration(player);
                    if (pen > 0.0D) {
                        double mit = com.hongmeng.xiuxian.compat.ArmorMitigation
                                .reductionOf(event.getEntity());
                        event.setAmount((float) (event.getAmount() * (1.0D + pen * mit)));
                    }
                }
                // 天赋吸血（与血魔功的被动吸血叠加）
                // 职业被动【嗜血】（血修 近战吸血 +5%）也在这里合并 ——
                // 吸血只保留这一个回血口，避免两处各回一次造成双倍吸血。
                double talentSteal = com.hongmeng.xiuxian.talent.TalentBonus
                        .lifesteal(player)
                        + com.hongmeng.xiuxian.talent.CareerPassives
                                .lifesteal(player);
                if (talentSteal > 0.0D) {
                    float heal = (float) (event.getAmount() * talentSteal);
                    if (heal > 0.0F) {
                        player.heal(heal);
                    }
                }
            } catch (Throwable t) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                        "[talent] 攻击加成结算失败：{}", t.toString());
            }

            // ---- 重击打断：把怪的「蓄力猛扑」打回去 ----
            //
            // 在这之前"蓄力猛扑"是必然命中的 —— 玩家看到它站定也没有反制手段。
            // 现在只要在它扑出来之前砸一记重击，就能让它踉跄 1 秒。
            try {
                if (event.getEntity()
                        instanceof com.hongmeng.xiuxian.entity.CultivationMonsterEntity victim
                        && event.getSource().getDirectEntity() == player
                        && com.hongmeng.xiuxian.entity.MobMind.telegraphing(victim)) {
                    boolean heavy = event.getAmount() >= victim.getMaxHealth()
                            * com.hongmeng.xiuxian.entity.MobMind.HEAVY_DAMAGE_RATIO
                            || com.hongmeng.xiuxian.combat.CombatAnim.isFinisher(player);
                    if (heavy) {
                        com.hongmeng.xiuxian.entity.MobMind.interrupt(victim, player);
                    }
                }
            } catch (Throwable ignored) {
                // 打断判定失败不影响伤害
            }

            // ---- 怪物闪避：精英以上可能把这一下滚掉（完全免伤）----
            //
            // 放在格挡之前：闪避是没被打到，格挡是被打到但挡住，
            // 两者的优先级很明确 —— 能闪就闪，闪不了才挡。
            try {
                if (event.getEntity()
                        instanceof com.hongmeng.xiuxian.entity.CultivationMonsterEntity dodger
                        && event.getSource().getDirectEntity() == player
                        && com.hongmeng.xiuxian.entity.MobDodge
                        .tryDodge(dodger, player, event.getAmount())) {
                    event.setAmount(0.0F);
                    return;
                }
            } catch (Throwable ignored) {
                // 闪避判定失败就按普通伤害
            }

            // ---- 怪物的格挡 / 弹反（让战斗对称）----
            //
            // 放在连招之前：先让怪有机会挡下来，再算连招倍率 ——
            // 否则"终结技"会无视怪的格挡，玩家又会退化成无脑砍。
            try {
                if (event.getEntity()
                        instanceof com.hongmeng.xiuxian.entity.CultivationMonsterEntity mob
                        && event.getSource().getDirectEntity() == player) {
                    double blocked = com.hongmeng.xiuxian.entity.MobGuard
                            .onHurt(mob, event.getAmount(), player);
                    if (blocked != event.getAmount()) {
                        event.setAmount((float) blocked);
                    }
                }
            } catch (Throwable ignored) {
                // 格挡结算失败按原伤害
            }

            // ---- 连招：第三下是终结技（只对近战直击生效）----
            //
            // 为什么要求 directEntity == player：法术也会走 LivingHurtEvent，
            // 若按连击序号给法术加伤，就会出现"放法术也算连招"的荒谬情况。
            try {
                if (event.getSource().getDirectEntity() == player) {
                    // 按**武器分型 + 连击序号**查表（五条道途各有自己的连段）
                    double mul = com.hongmeng.xiuxian.combat.CombatAnim
                            .comboMultiplier(player);
                    if (mul != 1.0D) {
                        event.setAmount((float) (event.getAmount() * mul));
                    }
                    if (com.hongmeng.xiuxian.combat.CombatAnim.isFinisher(player)) {
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.literal(
                                        "\u00a76\u00a7l终结 \u00a77· \u00a7e×"
                                                + String.format("%.2f", mul)), true);
                        if (player.level() instanceof net.minecraft.server.level
                                .ServerLevel sl) {
                            sl.sendParticles(net.minecraft.core.particles
                                            .ParticleTypes.SWEEP_ATTACK,
                                    event.getEntity().getX(),
                                    event.getEntity().getY() + 1.0D,
                                    event.getEntity().getZ(),
                                    2, 0.2D, 0.2D, 0.2D, 0.0D);
                        }
                    }
                }
            } catch (Throwable ignored) {
                // 连招加成失败不影响这一下
            }

            // ---- 道途核心机制的**实际增伤** ----
            //
            // ⚠ 这三项以前只出现在界面显示里（`PathRuntime` 的摘要字符串），
            //   **从来没有加进伤害** —— 玩家看到「魔气 80% · 增伤 ×1.36」，
            //   实际打出去一点没变。这是"看起来在工作"的最糟形式。
            try {
                double pathAmp = 1.0D;
                if (com.hongmeng.xiuxian.talent.Talents.pathOf(player)
                        == com.hongmeng.xiuxian.talent.Career.MO_XIU) {
                    double demonic = com.hongmeng.xiuxian.talent.PathRuntime
                            .demonicOf(player);
                    // 魔修：魔气越高，伤害越高（这是它的核心收益）
                    pathAmp *= com.hongmeng.xiuxian.talent.PathMechanics
                            .demonDamageBonus(demonic);
                    // 魔修：破防。`LivingHurtEvent` 拿不到"护甲减免了多少"，
                    // 所以按 ArmorMitigation 的**实际减免率**把破防折算成等效增伤，
                    // 而不是拍一个常数。
                    double pen = com.hongmeng.xiuxian.talent.PathMechanics
                            .demonPenetration(demonic);
                    double mitigation = com.hongmeng.xiuxian.compat.ArmorMitigation
                            .reductionOf(event.getEntity());
                    pathAmp *= 1.0D + pen * mitigation;
                    // 职业被动【魔溢】（魔修 满仓爆发增伤 +20%）：接在既有
                    // "魔气增伤 + 破防"同一乘区里；自伤仍由 PathRuntime 照旧结算。
                    if (demonic >= com.hongmeng.xiuxian.talent.PathMechanics.DEMON_MAX
                            - 1.0E-6D) {
                        pathAmp *= 1.0D + com.hongmeng.xiuxian.talent.CareerPassives
                                .demonOverflowBurst(player);
                    }
                } else if (com.hongmeng.xiuxian.talent.Talents.pathOf(player)
                        == com.hongmeng.xiuxian.talent.Career.XUE_XIU) {
                    // 血修：血池越满 + 生命越低 → 伤害越高（反杀窗口）
                    double poolFrac = com.hongmeng.xiuxian.talent.PathRuntime
                            .bloodFraction(player);
                    double hpFrac = player.getMaxHealth() <= 0.0F ? 1.0D
                            : player.getHealth() / player.getMaxHealth();
                    pathAmp *= com.hongmeng.xiuxian.talent.PathMechanics
                            .bloodlustBonus(poolFrac, hpFrac);
                    // 职业被动【血怒】（血修 生命 <40% 时增伤 +15%）：
                    // 接在既有"血修伤害乘区"这一段，与血池反杀加成同口径相加。
                    double lowHp = com.hongmeng.xiuxian.talent.CareerPassives
                            .lowHpDamage(player);
                    if (lowHp > 0.0D
                            && player.getHealth() < player.getMaxHealth() * 0.40F) {
                        pathAmp *= 1.0D + lowHp;
                    }
                }
                if (Math.abs(pathAmp - 1.0D) > 1.0E-6D) {
                    event.setAmount((float) (event.getAmount() * pathAmp));
                }
            } catch (Throwable t) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                        "[path] 道途增伤结算失败：{}", t.toString());
            }

            // ---- 职业机制：造成伤害（魔修攒魔气 / 血修攒血池 /
            //      剑修叠剑意并放剑气 / 鬼修叠魂印）----
            //
            // 放在最后：前面所有增伤都算完了，这里记的才是"实际打出的伤害"。
            // 近战判定用 directEntity == player（法术的直击实体不是玩家本人）。
            com.hongmeng.xiuxian.talent.PathRuntime
                    .onPlayerDealtDamage(player, event.getEntity(),
                            event.getAmount(),
                            event.getSource().getDirectEntity() == player);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 战斗被动（增伤/吸血）结算失败：{}", t.toString());
        }
    }

    /**
     * 玩家挨打：**龟息功**（减伤）与**凌波微步**（闪避）。
     *
     * <p>闪避用"取消事件 + 完全不掉血"实现：这样连击退与击退附带的位移也一并免掉，
     * 观感上才是"闪开了"。会计一次动作栏提示，否则玩家不知道自己闪了。</p>
     */
    @SubscribeEvent
    public void onPlayerTakesDamage(
            net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            // ---- 真世界地形：在脚下渗出稀有矿（肉后新矿）----
            try {
                com.hongmeng.xiuxian.realworld.HardmodeTerrain.tick(player);
            } catch (Throwable ignored) {
                // 地形系统不可用就跳过
            }

            // ---- 元婴遁走：渡劫期间本该致命的一击，留一命 ----
            //
            // 玩家要求：「渡劫失败演出 —— 现在失败就是死；小说里是
            // '肉身崩碎、元婴遁走'」。这里做成：**每次渡劫一次保命**，
            // 挨到致命伤时不死，血留 1 点，雷劫中止，并提示"元婴遁走"。
            try {
                if (com.hongmeng.xiuxian.core.Breakthrough.inTribulation(player)
                        && event.getAmount() >= player.getHealth()) {
                    com.hongmeng.xiuxian.core.Breakthrough.abandonTribulation(player);
                    event.setAmount(Math.max(0.0F, player.getHealth() - 1.0F));
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "\u00a75\u00a7l肉身崩碎，元婴遁走。"), false);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "\u00a77这一世渡劫未成，但命保住了。"), false);
                    if (player.level() instanceof net.minecraft.server.level
                            .ServerLevel sl) {
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.TOTEM_USE,
                                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
                        sl.sendParticles(net.minecraft.core.particles
                                        .ParticleTypes.TOTEM_OF_UNDYING,
                                player.getX(), player.getY() + 1.0D, player.getZ(),
                                40, 0.6D, 0.8D, 0.6D, 0.35D);
                    }
                }
            } catch (Throwable ignored) {
                // 保命失败就按普通致命伤处理
            }

            // ---- 天赋：减伤与反弹 ----
            //
            // ⚠ `TalentBonus.damageReduction` / `reflect` 以前**零调用** ——
            //   面板显示"减伤 +10%"，实际一点没减。
            try {
                double cut = com.hongmeng.xiuxian.talent.TalentBonus
                        .damageReduction(player);
                if (cut > 0.0D) {
                    // 封顶 80%：天赋不该让人完全免疫
                    double c = Math.min(0.80D, cut);
                    event.setAmount((float) (event.getAmount() * (1.0D - c)));
                }
                double back = com.hongmeng.xiuxian.talent.TalentBonus.reflect(player)
                        // 职业被动【震荡】（体修 受伤反弹 10%）：与天赋反弹同一结算口，
                        // 直接相加，不另开第二处反弹（避免反两次）。未解锁为 0。
                        + com.hongmeng.xiuxian.talent.CareerPassives.reflect(player);
                if (back > 0.0D && event.getSource().getEntity()
                        instanceof net.minecraft.world.entity.LivingEntity src
                        && src != player) {
                    // 反弹：按"这一击最终伤害"的比例打回去（魔法伤害，不吃二次反弹）
                    //
                    // ⚠ 防死循环：若对方也是带反弹的单位（例如两个体修对打），
                    //   反伤会再次触发对方的反弹 → 无限乒乓。用深度闸门限到 2 层：
                    //   第 1 层正常反伤，第 2 层（对方再反回来）之后不再继续。
                    if (REFLECT_DEPTH < 2) {
                        REFLECT_DEPTH++;
                        try {
                            src.hurt(player.damageSources().magic(),
                                    (float) (event.getAmount() * back));
                        } finally {
                            REFLECT_DEPTH--;
                        }
                    }
                }
            } catch (Throwable t) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                        "[talent] 减伤/反弹结算失败：{}", t.toString());
            }
            var data = CultivationCapability.get(player);
            if (data == null) {
                return;
            }
            var tech = data.getTechniques();

            // 凌波微步：概率完全闪避
            double dodge = com.hongmeng.xiuxian.technique.TechniquePassive
                    .dodgeChance(tech);
            if (dodge > 0.0D && player.getRandom().nextDouble() < dodge) {
                event.setCanceled(true);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7b凌波微步\u00a77——闪开了"), true);
                return;
            }

            // 龟息功：减伤
            double reduce = com.hongmeng.xiuxian.technique.TechniquePassive
                    .reduceMultiplier(tech);
            if (reduce != 1.0D) {
                event.setAmount((float) (event.getAmount() * reduce));
            }

            // 虚无（鸿蒙规则之力）：受到伤害 -30%
            double voidMul = com.hongmeng.xiuxian.core.RealmPowers
                    .voidMultiplier(data.realm().tier());
            if (voidMul != 1.0D) {
                event.setAmount((float) (event.getAmount() * voidMul));
            }

            // ---- 职业机制：受创（剑修剑罡 / 体修攒激荡 / 血修缓伤 / 鬼修虚化）----
            //
            // 放在最后：前面的减伤都算完了，这里拿到的才是"真正会掉的血"。
            // 返回值是"立即结算的部分" —— 血修把 30% 挪进缓伤池，虚化直接归零。
            double afterPath = com.hongmeng.xiuxian.talent.PathRuntime
                    .onPlayerHurt(player, event.getAmount());
            event.setAmount((float) afterPath);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 战斗被动（减伤/闪避）结算失败：{}", t.toString());
        }
    }

    /**
     * <b>负面效果抗性</b>（魔修职业被动【心魔】：负面抗性 +20%）。
     *
     * <h2>为什么接这个事件</h2>
     * <p>"受控"在 1.20.1 里没有统一入口：减速/虚弱/致盲/中毒/饥饿分别来自
     * 不同实体与系统（{@code MobGuard}、
     * {@code RealmProgression.tickMiasma}、各种怪的 {@code addEffect}）。
     * 逐个改那些文件既越界又必然漏。{@code MobEffectEvent.Applicable} 是 Forge
     * 提供的"负面效果即将挂上"的统一判定口，在这里按 20% 概率 DENY，
     * 覆盖全部 {@code MobEffectCategory.HARMFUL} 的效果，且不改任何调用方。</p>
     *
     * <p>⚠ 未选魔修 / 未解锁时 {@code CareerPassives.controlResist} 返回 0 →
     * 直接 return，原版行为完全不变（工具/丹药给的增益也不受影响）。</p>
     */
    @SubscribeEvent
    public void onMobEffectApplicable(
            net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            var inst = event.getEffectInstance();
            if (inst == null
                    || inst.getEffect().getCategory()
                    != net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                return;
            }
            double resist = com.hongmeng.xiuxian.talent.CareerPassives
                    .controlResist(player);
            if (resist <= 0.0D) {
                return;
            }
            // 20% 概率完全免疫这次负面效果（封顶 80%，不允许变成彻底免控）
            if (player.getRandom().nextDouble() < Math.min(0.80D, resist)) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            }
        } catch (Throwable ignored) {
            // 抗性判定失败就照常挂效果
        }
    }

    // ------------------------------------------------------------------
    // 死亡 / 击杀
    // ------------------------------------------------------------------

    /**
     * Boss 受伤 → 阶段激荡（玩家要求「战斗也要有激荡的音效」）。
     *
     * <p>转发给 {@link com.hongmeng.xiuxian.combat.BossCombat}；
     * 只有 BOSS / 首领品阶会真的触发，普通怪不受影响。</p>
     */
    /**
     * 击杀回血（天赋 {@code killHeal}）。
     *
     * <p>⚠ 这个天赋以前<b>零调用</b> —— 点了没有任何效果。</p>
     */
    /**
     * 把 {@code keepItemsOnDeath} 落实成原版 {@code keepInventory} 规则。
     *
     * <p>⚠ 这个配置项以前从未被读取：默认值是 true（“死亡不掉落物品”），
     * 但代码里没有对应实现 —— 玩家以为不会掉，实际会掉光。
     * 配置与行为相反是最坏的一类不一致。</p>
     *
     * <p>为什么走原版规则而不是自己搬背包：死亡 → 掉装备 → 重生复制
     * 这条链上自己动手极易产生复制或丢失。原版 keepInventory
     * 就是为这件事设计的，用它零风险。</p>
     *
     * <p>单向：配置 true 就强制打开；配置 false 时不动世界规则
     * （尊重玩家自己设的 /gamerule keepInventory）。</p>
     */
    @SubscribeEvent
    public void onServerStarted(
            net.minecraftforge.event.server.ServerStartedEvent event) {
        try {
            if (!HongmengConfig.SERVER.keepItemsOnDeath.get()) {
                return;
            }
            var server = event.getServer();
            var rule = server.getGameRules()
                    .getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
            if (!rule.get()) {
                rule.set(true, server);
                com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                        "[hongmeng] keepItemsOnDeath=true -> 已把 keepInventory 打开");
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[hongmeng] 应用 keepItemsOnDeath 失败：{}", t.toString());
        }
    }

    /**
     * <b>攻击 → 广播动作</b>（三段连击 + 武器分型）。
     *
     * <p>玩家要求「参考动作 mod 的攻击动作」。招式由服务端判定并广播，
     * 客户端只按 {@code kind + combo} 摆姿势 —— 这样别人看你攻击时
     * 也能看到同样的招式（客户端各自猜会不一致）。</p>
     *
     * <p>失败绝不能挡着攻击本身，所以整段包在 try 里。</p>
     */
    @SubscribeEvent
    public void onPlayerAttack(
            net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        try {
            if (event.getEntity() instanceof ServerPlayer attacker) {
                com.hongmeng.xiuxian.combat.CombatAnim.onAttack(attacker);
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[combat-anim] 动作广播失败：{}", t.toString());
        }
    }

    @SubscribeEvent
    public void onPlayerKillLiving(
            net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        try {
            if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
                return;
            }
            if (event.getEntity() == player) {
                return;
            }
            // 职业机制：击杀结算（血修血池 / 体修锁血冷却缩短 / 鬼修回鬼域能量）
            com.hongmeng.xiuxian.talent.PathRuntime.onPlayerKill(
                    player, event.getEntity().getMaxHealth());
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[talent] 击杀回血结算失败：{}", t.toString());
        }
    }

    @SubscribeEvent
    public void onBossHurt(LivingHurtEvent hurt) {
        // 写成"先取成方法局部变量、再判断类型"的朴素形式：
        // 这一版踩过 Java 模式变量（instanceof X x）的作用域坑 ——
        // 本类里有多个同名表达式变量，模式变量会报"已定义"，
        // 换个名字也照报。朴素写法没有这个限制。
        LivingEntity maybeBoss = hurt.getEntity();
        if (!(maybeBoss.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        com.hongmeng.xiuxian.combat.BossCombat.onBossHurt(maybeBoss, serverLevel);
    }

    /**
     * <b>五个祭坛 boss 掉「恶之祭坛」的凭证</b>。
     *
     * <p>没有这五个东西，恶之祭坛永远合不出来 —— 所以它们在死亡时<b>必定</b>掉落。</p>
     */
    @SubscribeEvent
    public void onAltarBossDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(event.getEntity().getType()).getPath();
        net.minecraft.world.item.Item drop = switch (path) {
            case "epoch_boss" -> com.hongmeng.xiuxian.item.ModItems.PUPPET_CORE.get();
            case "beast_boss" -> com.hongmeng.xiuxian.item.ModItems.BEAST_HEART.get();
            case "demon_boss" -> com.hongmeng.xiuxian.item.ModItems.BLOOD_CORE.get();
            case "void_boss" -> com.hongmeng.xiuxian.item.ModItems.VOID_EYE.get();
            case "heaven_lord" ->
                    com.hongmeng.xiuxian.item.heaven.HeavenItems.HEAVEN_CROWN.get();
            case "evil_gazer" -> net.minecraft.world.item.Items.NETHER_STAR;
            default -> null;
        };
        if (drop == null) {
            return;
        }
        var pos = event.getEntity().blockPosition();
        net.minecraft.world.Containers.dropItemStack(level,
                pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                new net.minecraft.world.item.ItemStack(drop));
        if ("evil_gazer".equals(path)) {
            // 「恶」逃走后把玩家送回祭坛附近的落脚点
            for (var p : level.players()) {
                p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7d\u00a7l这一切结束了。"), false);
                p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7c\u00a7l真的结束了吗！？"), true);
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        // ---- 祭坛召唤的 boss 被打死：**立刻结算并送回原世界** ----
        //
        // ⚠ 原来 `DungeonRealm.onArenaBossKilled(...)` **定义了却从没被调用**，
        // 所以打死 boss 不结算也不返回，玩家被困在 boss 维度里
        //（玩家反馈「打完 boss 也走不了，没办法传送出去」就是这个）。
        if (!(event.getEntity() instanceof ServerPlayer)
                && event.getEntity() instanceof com.hongmeng.xiuxian.entity
                        .CultivationMonsterEntity deadBoss
                && deadBoss.getPersistentData().getBoolean("hm_arena_boss")) {
            // boss 死亡运镜（玩家限定：只有出场/死亡/法术三种场合运镜）
            try {
                if (event.getEntity().level() instanceof ServerLevel srvLevel) {
                    com.hongmeng.xiuxian.combat.BossCinematic.playDeath(
                            srvLevel, event.getEntity(),
                            event.getEntity().getName().getString());
                }
            } catch (Throwable ignored) {
                // 演出失败不影响结算
            }
            // 结算对象：优先给亲手击杀的玩家；否则给该 boss 维度里所有进行中的决战挑战者
            // （摔死/岩浆/被别的怪打死时也要结算，否则挑战卡死 —— 审计 A-2）。
            ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sp
                    ? sp : null;
            if (killer != null) {
                com.hongmeng.xiuxian.dungeon.DungeonRealm.onArenaBossKilled(killer);
            } else if (event.getEntity().level() instanceof ServerLevel arenaLevel) {
                for (ServerPlayer p : new java.util.ArrayList<>(arenaLevel.players())) {
                    if (com.hongmeng.xiuxian.dungeon.ArenaRealm.isInArena(p)
                            && com.hongmeng.xiuxian.dungeon.DungeonRealm.inChallenge(p)) {
                        com.hongmeng.xiuxian.dungeon.DungeonRealm.onArenaBossKilled(p);
                    }
                }
            }
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            CultivationData data = CultivationCapability.get(player);
            deathDrama(player);
            Karma.onDeath(player, data);
            Breakthrough.clear(player.getUUID());
            PENDING_AWAKEN.remove(player.getUUID());
            // 御剑中死了：先收剑（否则重生后 abilities 与状态对不上）
            // 死了就别惦记秘境了
            com.hongmeng.xiuxian.epoch.EpochRealm.abort(player);
            return;
        }

        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            awardKillCultivation(killer, event.getEntity());
            // 灵叶另算：修为会封顶，坊市的钱不该封顶
            awardKillLeavesSafely(killer, event.getEntity());
            // **道宫能量也另算**，理由同上：它原来挂在 awardKillCultivation 的尾部，
            // 而那条链第一句就是"满境就返回"—— 玩家在鸿蒙（满境）杀怪时
            // 修为不该涨是对的，但**能量也不涨就成了 bug**（玩家报过"杀怪不会获得道宫能量"）。
            awardKillDomainEnergy(killer, event.getEntity());
        }
    }

    private void awardKillCultivation(ServerPlayer killer, LivingEntity victim) {
        CultivationData data = CultivationCapability.get(killer);
        if (!data.isAwakened() || data.isMaxRealm()) {
            return;
        }
        double base = HongmengConfig.SERVER.killBaseGain.get();
        double mult = HongmengConfig.SERVER.cultivationMultiplier.get()
                * com.hongmeng.xiuxian.sect.SectManager.cultivationMultiplier(data);

        double strength = 1.0D + (victim.getMaxHealth() / 20.0D) * 0.5D;
        if (victim instanceof Monster) {
            strength *= HongmengConfig.SERVER.killHostileMultiplier.get();
        }
        if (victim.getMobType() == MobType.UNDEAD) {
            strength *= 1.15D;
        }

        double gain = base * strength * mult;
        boolean isBoss = victim instanceof EnderDragon
                || victim instanceof WitherBoss
                || victim instanceof Warden;

        if (isBoss) {
            gain += HongmengConfig.SERVER.bossGain.get() * mult;
            CultivationSource.BOSS.grant(killer, gain, false);
            onBossKill(killer, victim);
        } else {
            CultivationSource.KILL.grant(killer, gain, false);
        }

        QuestChain.onKill(killer, victim);
        com.hongmeng.xiuxian.sect.SectManager.onKill(killer, data);
        com.hongmeng.xiuxian.quest.QuestManager.onKill(killer);
    }

    /**
     * 杀怪掉灵叶——坊市货币的主要来源。
     *
     * <p>掉落量按怪物品阶走（见 {@link com.hongmeng.xiuxian.shop.LeafRewards}）：
     * 普通 2~4、精英 6~10、队长 15~25、头目 40~60、首领 100~150；
     * 原版敌对生物（僵尸、骷髅这些）也有 1~{@code leafDropMax} 的小额掉落，
     * 让玩家在没遇到修仙界妖兽时也有收入。</p>
     *
     * <p>灵叶直接进<b>坊市余额</b>（不再塞背包），概率由
     * {@code economy.leafDropChance} 控制，默认 0.35。</p>
     */
    /**
     * 杀怪给道宫能量（**满境也要给**）。
     *
     * <p>从 {@link #awardKillCultivation} 里搬出来的：那条链以
     * {@code isMaxRealm()} 提前返回，导致满境玩家（鸿蒙）杀怪永远拿不到能量。</p>
     */
    private void awardKillDomainEnergy(ServerPlayer killer, LivingEntity victim) {
        try {
            CultivationData data = CultivationCapability.get(killer);
            com.hongmeng.xiuxian.domain.DomainManager.onKill(killer, data, victim);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 道宫能量结算失败：{}", t.toString());
        }
    }

    /**
     * 灵叶掉落的**自保外壳**。
     *
     * <p><b>为什么要这层壳</b>：玩家报过一次服务器崩溃：</p>
     * <pre>
     *   java.lang.NoClassDefFoundError: com/hongmeng/xiuxian/shop/LeafRewards
     *     at ModEvents.awardKillLeaves(ModEvents.java:596)
     * </pre>
     * <p>根因是我**覆盖写 jar 的那几秒里游戏正在读 jar**（读到半写的 jar），
     * 不是代码缺失 —— 但现在多一层保险：灵叶掉落只是锦上添花的收益，
     * <b>任何 Throwable 都不该让服务器崩</b>。</p>
     *
     * <p>注意必须 catch {@code Throwable} 而不是 {@code Exception}：
     * {@code NoClassDefFoundError} / {@code LinkageError} 都是 {@code Error} 的子类，
     * 只 catch Exception 抓不住 —— 这正是上次把服务器打崩的原因。</p>
     */
    private void awardKillLeavesSafely(ServerPlayer killer, LivingEntity victim) {
        try {
            awardKillLeavesSafely(killer, victim);
        } catch (Throwable t) {
            // 只记一次警告，不刷屏
            if (LEAF_FAILURE_LOGGED.compareAndSet(false, true)) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                        "[hongmeng] 灵叶掉落结算失败（已跳过，不影响游戏）：{}", t.toString());
            }
        }
    }

    /** 灵叶掉落失败只记一次，避免每只怪刷一条日志。 */
    private static final java.util.concurrent.atomic.AtomicBoolean LEAF_FAILURE_LOGGED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private void awardKillLeaves(ServerPlayer killer, LivingEntity victim) {
        int lo;
        int hi;
        boolean ranked = false;
        if (victim instanceof com.hongmeng.xiuxian.entity.CultivationMonsterEntity monster) {
            com.hongmeng.xiuxian.entity.MobRank rank = monster.rank();
            lo = com.hongmeng.xiuxian.shop.LeafRewards.min(rank);
            hi = com.hongmeng.xiuxian.shop.LeafRewards.max(rank);
            ranked = true;
        } else if (victim instanceof Monster) {
            // 原版敌对生物：给个零头，别让"打僵尸"完全没收益
            lo = com.hongmeng.xiuxian.shop.LeafRewards.VANILLA_MIN;
            hi = Math.max(lo, HongmengConfig.SERVER.leafDropMax.get());
        } else {
            // 牛羊猪鸡不给钱：不然刷牧场就成了刷钱
            return;
        }

        if (killer.getRandom().nextDouble()
                >= HongmengConfig.SERVER.leafDropChance.get()) {
            return;
        }
        int howMany = lo + (hi <= lo ? 0 : killer.getRandom().nextInt(hi - lo + 1));
        int balance = com.hongmeng.xiuxian.shop.ShopManager.giveLeaves(killer, howMany);

        // 头目／首领级别的掉落值得单独报一句，让玩家知道"杀怪能挣钱"
        if (ranked && howMany >= 40) {
            killer.sendSystemMessage(Component.literal(
                    "\u00a76击杀 " + victim.getName().getString() + " \u00a77掉落 \u00a7e"
                            + howMany + " \u00a77灵叶\u00a78（余额 " + balance + "）"));
        }
    }

    private void onBossKill(ServerPlayer killer, LivingEntity victim) {
        if (!BOSS_REWARDED.add(killer.getUUID())) {
            return;
        }
        killer.sendSystemMessage(Component.literal(""));
        killer.sendSystemMessage(Component.literal(
                        "\u00a74\u00a7l✦ 你竟然真的把它宰了 \u00a7f" + victim.getName().getString())
                .withStyle(ChatFormatting.DARK_RED));
        killer.sendSystemMessage(Component.literal(
                "\u00a77按鸿蒙难度的标准，这不该是你这个境界能做到的事。"));
        ItemStack reward = new ItemStack(ModItems.JIDAN.get(), 1);
        if (!killer.getInventory().add(reward)) {
            killer.drop(reward, false);
        }
        killer.sendSystemMessage(Component.literal("\u00a7a获得：纪元令 ×1"));
        killer.sendSystemMessage(Component.literal(""));
    }

    // ------------------------------------------------------------------
    // 真实痛觉
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!HongmengConfig.SERVER.painEffects.get()) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }
        CultivationData data = CultivationCapability.get(player);
        int add = amount >= 6.0F ? 2 : 1;
        data.setPainLevel(data.getPainLevel() + add);
        PAIN_DECAY.put(player.getUUID(), 0);
        ModNetwork.sendToPlayer(player, new PainFlashPacket(16, amount));

        if (data.getPainLevel() >= 5 && player.tickCount % 40 == 0) {
            player.displayClientMessage(
                    Component.literal("\u00a7c疼。这不是游戏，这是真的。"), true);
        }
    }

    // ------------------------------------------------------------------
    // 附魔金苹果 → 清业力
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getItemStack().is(Items.ENCHANTED_GOLDEN_APPLE)) {
            BlessingTracker.onEnchantedApple(player, event.getItemStack());
        }
    }

    // ------------------------------------------------------------------
    // 境界属性加成
    // ------------------------------------------------------------------

    public static void applyRealmAttributes(ServerPlayer player, CultivationData data) {
        if (player == null || data == null) {
            return;
        }
        Realm realm = data.realm();
        double hpScale = HongmengConfig.SERVER.realmHpScale.get();
        double dmgScale = HongmengConfig.SERVER.realmDamageScale.get();

        // 境界加成：在境界自身基础上倍增（不含灵根）
        double realmHealth = realm.healthBonus() * hpScale;
        double realmDamage = realm.damageBonus() * dmgScale;

        // 灵根加成：独立于境界，只作用于"最终值"，不参与境界翻倍
        double rootPercent = 0.0D;
        if (data.getRoot() != null) {
            rootPercent = data.getRoot().rarity().healthBonus();
        }

        // 门派攻击加成
        double sectAttack = com.hongmeng.xiuxian.sect.SectManager.attackBonus(data);

        applyModifier(player, Attributes.MAX_HEALTH, HEALTH_MOD_ID, HEALTH_MOD_NAME,
                realmHealth, AttributeModifier.Operation.ADDITION);
        applyModifier(player, Attributes.MAX_HEALTH, ROOT_HEALTH_MOD_ID, ROOT_HEALTH_MOD_NAME,
                rootPercent, AttributeModifier.Operation.MULTIPLY_TOTAL);

        applyModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_MOD_ID, DAMAGE_MOD_NAME,
                realmDamage, AttributeModifier.Operation.ADDITION);
        applyModifier(player, Attributes.ATTACK_DAMAGE, ROOT_DAMAGE_MOD_ID, ROOT_DAMAGE_MOD_NAME,
                rootPercent, AttributeModifier.Operation.MULTIPLY_TOTAL);
        applyModifier(player, Attributes.ATTACK_DAMAGE, SECT_DAMAGE_MOD_ID, SECT_DAMAGE_MOD_NAME,
                sectAttack, AttributeModifier.Operation.MULTIPLY_TOTAL);

        // 境界护甲：走 Attributes.ARMOR 的 ADDITION，**原样加上**。
        //
        // 2026-11-02 数值平衡重做：
        //   旧实现在这里用 softCapArmor 反推「本模组该出多少」，本意是防高护甲无敌；
        //   但软上限只压了「境界护甲」这一项，职业甲/道袍/法宝的几十万点护甲
        //   完全不经过它——第 4 境起反推值恒为 0，境界护甲等于被架空。
        //   真正的护甲封顶现在统一在 ArmorMitigation 里做（属性层 clamp 80 →
        //   原始值仍参与 92% 减伤曲线），所以这里只需要老实加值。
        double wantedRealmArmor = realm.armorBonus()
                * HongmengConfig.SERVER.realmArmorScale.get();
        applyModifier(player, Attributes.ARMOR, REALM_ARMOR_MOD_ID, REALM_ARMOR_MOD_NAME,
                wantedRealmArmor, AttributeModifier.Operation.ADDITION);
        // 属性值变了 → 立刻重算一次属性层封顶（原始值仍保留在 ArmorMitigation 里）
        com.hongmeng.xiuxian.compat.ArmorMitigation.capVanillaArmor(player);

        // 化神：常驻速度 I
        applyModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MOD_ID, SPEED_MOD_NAME,
                realm.hasSpeedOne() ? 0.20D : 0.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        // 帝境：可切换的速度 II
        boolean speedTwo = realm.hasSpeedTwo() && data.isSpeedBoostEnabled();
        applyModifier(player, Attributes.MOVEMENT_SPEED, SPEED2_MOD_ID, SPEED2_MOD_NAME,
                speedTwo ? 0.40D : 0.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);

        // ---- 触及距离（手的长度，规格 §四）----
        // 金丹 +1、道印 +1、仙人 +1，仙人之后每境 +1（累计：鸿蒙 +5）。
        // 走 Forge 的 BLOCK_REACH / ENTITY_REACH 两个属性，和别的一样本方法负责
        // "删旧的再加新的"（applyModifier 内部先 removeModifier，所以可以反复调用）。
        double reach = com.hongmeng.xiuxian.core.ReachTable.reachBonus(realm.tier());
        applyModifier(player, net.minecraftforge.common.ForgeMod.BLOCK_REACH.get(),
                REACH_MOD_ID, REACH_MOD_NAME, reach, AttributeModifier.Operation.ADDITION);
        applyModifier(player, net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(),
                ENTITY_REACH_MOD_ID, ENTITY_REACH_MOD_NAME, reach,
                AttributeModifier.Operation.ADDITION);

        // 飞升：飞行。注意这里**也**覆盖御剑（金丹~飞升）——"能不能飞"只在这一处裁决，
        applyFlight(player, realm.hasFlight());

        // 把"能不能飞"直接告诉客户端（见 applyFlightPermission 的说明）。
        // 御剑中的金丹~飞升玩家也必须收到 true，否则客户端会把 abilities 纠正回去。
        com.hongmeng.xiuxian.network.FlightSyncPacket.sendTo(player,
                realm.hasFlight());

        // 飞升起：常驻生命回复 I + 夜视 + 饱和（仙人起再加急迫 I）
        applyAscensionEffects(player, realm);

        // 血量上限变了，把当前血量也补齐，避免"血条空了但上限很高"
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ------------------------------------------------------------------
    // 道袍不给怪物穿
    // ------------------------------------------------------------------

    /**
     * 怪物捡起并穿上道袍时，立刻脱下来丢回地上。
     *
     * <p>原因：原版僵尸/骷髅会捡装备穿上，而僵尸的头是 8×8×8 格、身体只有 12 格高，
     * 套上道袍头盔之后头会夸张到看起来像"建模错位"（玩家截图报过这个观感）。</p>
     *
     * <p>1.20.1 的 Forge 没有 {@code canEquip} 之类的可覆盖点（试过，编译不过），
     * 所以用装备变更事件兜底。只有非玩家换装备时才会走到下面的分支。</p>
     */
    @SubscribeEvent
    public void onMobEquipsDaoArmor(
            net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            return;      // 玩家穿道袍是正常的，第一句就挡掉
        }
        var stack = event.getTo();
        if (stack.isEmpty()
                || !(stack.getItem() instanceof com.hongmeng.xiuxian.item.armor.DaoArmorItem)) {
            return;
        }
        var living = event.getEntity();
        var slot = event.getSlot();
        if (!(living.level() instanceof ServerLevel level)) {
            return;
        }
        try {
            // 先丢回地上再清空，避免"清空即丢失"。
            // 注意用 `level.addFreshEntity`：LivingEntity 没有 drop 方法（那是 Mob 的）。
            var entity = new net.minecraft.world.entity.item.ItemEntity(level,
                    living.getX(), living.getY() + 0.5D, living.getZ(), stack.copy());
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            living.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 已脱下 {} 身上的道袍（{}）",
                    living.getName().getString(), slot);
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 脱下道袍失败：{}", t.toString());
        }
    }

    // ------------------------------------------------------------------
    // 普攻横扫（玩家要求：普攻也带"横扫之刃"）
    // ------------------------------------------------------------------

    /** 横扫半径（格）。 */
    public static final double SWEEP_RADIUS = 3.0D;

    /** 横扫伤害占本次伤害的比例。 */
    public static final double SWEEP_DAMAGE_RATIO = 0.35D;

    /** 横扫最多命中几个（防群刷时爆量）。 */
    public static final int SWEEP_MAX_TARGETS = 6;

    /**
     * 普攻带横扫：每次有效命中，对目标周围的其他怪补一次范围伤害。
     *
     * <p>原版横扫只对**剑**生效、威力由附魔等级决定；玩家的要求是"普攻也带"，
     * 所以这里做成修仙者的常驻被动：</p>
     * <ul>
     *   <li>只在<b>服务端</b>、只对<b>玩家造成的伤害</b>生效（{@link ServerPlayer}）；</li>
     *   <li>伤害源仍走原版的 {@code playerAttack}，所以护甲/减伤/仇恨都按正常规则走；</li>
     *   <li><b>不会递归</b>：横扫伤害用 {@code setSweep()} 打标记，下一次遇到直接跳过。</li>
     * </ul>
     */
    @SubscribeEvent
    public void onSweepAttack(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        try {
            if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
                return;
            }
            if (event.getSource().getDirectEntity() != player) {
                return;      // 只认玩家本人打出去的（弓/法术/横扫补刀都不算）
            }
            if (SWEEP_MARK.contains(player.getUUID())) {
                return;      // 这是横扫自己打出来的，别再横扫一次
            }
            var victim = event.getEntity();
            float sweep = (float) (event.getAmount() * SWEEP_DAMAGE_RATIO);
            if (sweep <= 0.0F) {
                return;
            }
            var level = player.serverLevel();
            var box = victim.getBoundingBox().inflate(SWEEP_RADIUS);
            int hit = 0;
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (other == victim || other == player || !other.isAlive()) {
                    continue;
                }
                if (other instanceof ServerPlayer) {
                    continue;      // 不误伤玩家（友伤另有领域开关）
                }
                if (hit >= SWEEP_MAX_TARGETS) {
                    break;
                }
                SWEEP_MARK.add(player.getUUID());
                try {
                    other.hurt(player.damageSources().playerAttack(player), sweep);
                } finally {
                    SWEEP_MARK.remove(player.getUUID());
                }
                hit++;
            }
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 横扫结算失败：{}", t.toString());
        }
    }

    /** 正在结算横扫的玩家（防递归）。 */
    private static final java.util.Set<java.util.UUID> SWEEP_MARK =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    // ------------------------------------------------------------------
    // 境界带来的药水效果
    // ------------------------------------------------------------------

    /** 突破后保护效果的持续时长：60 秒。 */
    public static final int BLESSING_DURATION_TICKS = 20 * 60;

    /** 突破后保护的抗性等级（0 = 抗性 I）。 */
    public static final int BLESSING_RESISTANCE_AMPLIFIER = 0;

    /** 突破后保护的生命回复等级（1 = 生命回复 II）。 */
    public static final int BLESSING_REGENERATION_AMPLIFIER = 1;

    /** 飞升常驻生命回复等级（0 = 生命回复 I）。 */
    public static final int ASCENSION_REGENERATION_AMPLIFIER = 0;

    /** 飞升常驻夜视等级（0 = 夜视 I）。 */
    public static final int ASCENSION_NIGHT_VISION_AMPLIFIER = 0;

    /** 飞升常驻饱和等级（0 = 饱和 I）。 */
    public static final int ASCENSION_SATURATION_AMPLIFIER = 0;

    /** 飞升常驻水下呼吸等级（0 = 水下呼吸 I）。 */
    public static final int ASCENSION_WATER_BREATHING_AMPLIFIER = 0;

    /** 飞升常驻抗火等级（0 = 抗火 I）。 */
    public static final int ASCENSION_FIRE_RESISTANCE_AMPLIFIER = 0;

    /** 飞升及以上的常驻效果刷新间隔（tick）。 */
    public static final int ASCENSION_REFRESH_TICKS = 200;

    /**
     * 飞升起常驻：**生命回复 I + 夜视 + 饱和**。
     *
     * <p>为了不出现"效果还剩 3 秒就开始闪"的闪烁，每次给一个较长的时长
     * （5 分钟），由每秒的 tick 兜底续期，且只在剩余不足 30 秒时才续——
     * 避免每秒都刷同步包。三个效果都用 {@code ambient=true}
     * （淡色粒子、不显示图标边框动画），屏幕不会一直叠色。</p>
     */
    public static void applyAscensionEffects(ServerPlayer player, Realm realm) {
        if (player == null || realm == null) {
            return;
        }
        // ---- 境界常驻效果：**按境界逐条给**（玩家指定的门槛）----
        //
        // 金丹(3)  生命回复 I
        // 元婴(4)  饱和 + 抗火 + 水下呼吸
        // 化神(5)  夜视
        // 飞升(7)  生命回复 II
        // 帝境(9)  生命回复 III
        //
        // 门槛全部定义在 Realm 里（hasRegenOne / hasElementalWard / hasNightVision …），
        // 这里只负责"按判据发效果"，不再自己写数字。
        int regen = realm.regenerationAmplifier();
        if (regen >= 0) {
            keepEffect(player, MobEffects.REGENERATION, regen, true, false, true);
        }
        if (realm.hasNightVision()) {
            keepEffect(player, MobEffects.NIGHT_VISION,
                    ASCENSION_NIGHT_VISION_AMPLIFIER, true, false, false);
        }
        if (realm.hasElementalWard()) {
            // 饱和：不掉饥饿，也就不会因为饿而掉血
            keepEffect(player, MobEffects.SATURATION,
                    ASCENSION_SATURATION_AMPLIFIER, true, false, false);
            // 抗火 + 水下呼吸：元婴起水里与岩浆里都来去自如
            keepEffect(player, MobEffects.FIRE_RESISTANCE,
                    ASCENSION_FIRE_RESISTANCE_AMPLIFIER, true, false, true);
            keepEffect(player, MobEffects.WATER_BREATHING,
                    ASCENSION_WATER_BREATHING_AMPLIFIER, true, false, true);
        }
    }

    /**
     * 续期一个常驻效果。
     *
     * <p>只在"没有这个效果"或"剩余不足 30 秒"时重新施加，
     * 这样既不会闪，也不会每秒产生同步流量。</p>
     */
    private static void keepEffect(ServerPlayer player,
                                   net.minecraft.world.effect.MobEffect effect,
                                   int amplifier, boolean ambient, boolean visible,
                                   boolean showIcon) {
        var current = player.getEffect(effect);
        if (current != null && current.getDuration() > 20 * 30
                && current.getAmplifier() >= amplifier) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, 20 * 300,   // 5 分钟
                amplifier, ambient, visible, showIcon));
    }

    /**
     * 突破成功后的一分钟保护：**抗性 I + 生命回复 II**，持续 60 秒，
     * 并且**直接把生命回满**。
     *
     * <p>刚破境时血量上限会跳一大截、人也虚，这一段给玩家缓一口气。
     * 大境界渡劫时这 60 秒正好帮玩家扛雷。</p>
     */
    public static void applyBreakthroughBlessing(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                BLESSING_DURATION_TICKS, BLESSING_RESISTANCE_AMPLIFIER,
                false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                BLESSING_DURATION_TICKS, BLESSING_REGENERATION_AMPLIFIER,
                false, false, true));
        healToFull(player);
    }

    /**
     * 把玩家治疗到满血。
     *
     * <p>顺序很重要：先让属性重算完（血量上限可能刚刚变大），
     * 再 {@code setHealth(getMaxHealth())}——否则会被旧上限截断。</p>
     */
    public static void healToFull(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) {
            return;
        }
        float max = player.getMaxHealth();
        if (max > 0.0F && player.getHealth() < max) {
            player.setHealth(max);
        }
    }

    /** 飞升及以上的境界可以飞行。 */
    private static void applyFlight(ServerPlayer player, boolean canFly) {
        applyFlightPermission(player, canFly);
    }

    /**
     * 给/收飞行许可（既有的唯一一条通道）。
     *
     * <p>御剑飞行（见 {@link SwordFlight}）也走这里，<b>不另起一套机制</b>：
     * {@code abilities.mayfly} + {@code onUpdateAbilities()} 负责原版同步，
     * 尾部的 {@link com.hongmeng.xiuxian.network.FlightSyncPacket} 负责兜底
     * （客户端收到后每 tick 自行纠正，解决登录/重生时 abilities 包丢失的老问题）。</p>
     *
     * @param canFly true = 允许飞；false = 收回（只在当前是 true 时才会真的改，
     *               避免每秒无谓地重发 abilities 包）
     */
    public static void applyFlightPermission(ServerPlayer player, boolean canFly) {
        if (player == null) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        var abilities = player.getAbilities();
        if (canFly) {
            // 即使 mayfly 已经是 true 也要重发一次 abilities：
            // 玩家刚登录/刚重生时那次包可能丢了，客户端仍然是 false。
            abilities.mayfly = true;
            player.onUpdateAbilities();
        } else if (abilities.mayfly) {
            abilities.mayfly = false;
            abilities.flying = false;
            player.onUpdateAbilities();
        }
        // 把"能不能飞"直接告诉客户端。
        //
        // 原版的 abilities 同步在登录/重生时刻经常丢，导致玩家按空格飞不起来，
        // 除非先开创造飞一次再切生存（切模式会强制重发一次 abilities）。
        // 客户端每 tick 会用这个值自行纠正（ClientSetup.applyFlightPermission）。
        com.hongmeng.xiuxian.network.FlightSyncPacket.sendTo(player, canFly);
    }

    /**
     * 兜底：检查境界生命/攻击修饰符是否真的挂在属性上。
     *
     * <p>如果因为死亡、属性重置、其它模组覆盖之类的原因丢了，就立刻补上。
     * 这层保险是必要的——境界加成丢了玩家只会觉得"没生效"，很难自己排查。</p>
     */
    private static void ensureRealmAttributesPresent(ServerPlayer player, CultivationData data) {
        double wantHealth = data.realm().healthBonus()
                * HongmengConfig.SERVER.realmHpScale.get();
        double wantDamage = data.realm().damageBonus()
                * HongmengConfig.SERVER.realmDamageScale.get();

        AttributeInstance hp = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);

        boolean healthMissing = hp != null && hp.getModifier(HEALTH_MOD_ID) == null
                && Math.abs(wantHealth) > 1e-6;
        boolean damageMissing = atk != null && atk.getModifier(DAMAGE_MOD_ID) == null
                && Math.abs(wantDamage) > 1e-6;

        // 触及距离同样兜底：属性被重置（死亡、别的模组覆盖）后要能补回来
        double wantReach = com.hongmeng.xiuxian.core.ReachTable.reachBonus(
                data.realm().tier());
        AttributeInstance blockReach =
                player.getAttribute(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());
        boolean reachMissing = wantReach > 1e-6 && blockReach != null
                && blockReach.getModifier(REACH_MOD_ID) == null;

        if (healthMissing || damageMissing || reachMissing) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[hongmeng] 检测到境界属性修饰符缺失（生命 {} / 攻击 {} / 触及 {}），正在补回。玩家={}",
                    healthMissing, damageMissing, reachMissing,
                    player.getGameProfile().getName());
            applyRealmAttributes(player, data);
        }

        // 飞行也要兜底：死亡重生会把 abilities 重置。
        // （御剑已删除，所以这里只看境界：飞升及以上就该能飞。）
        boolean wantFly = data.realm().hasFlight();
        if (wantFly && !player.getAbilities().mayfly
                && !player.isCreative() && !player.isSpectator()) {
            applyFlight(player, true);
        }
    }

    /**
     * 当前挂在 ARMOR 上的"境界护甲"修饰符数值。
     *
     * <p>软上限计算需要知道"去掉本模组的加成之后，玩家自己的护甲是多少"，
     * 所以要把我们自己挂上去的那一份减掉。没挂过就返回 0。</p>
     */
    private static double currentRealmArmorModifier(ServerPlayer player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return 0.0D;
        }
        AttributeModifier mod = armor.getModifier(REALM_ARMOR_MOD_ID);
        return mod == null ? 0.0D : mod.getAmount();
    }

    private static void applyModifier(ServerPlayer player,                                      net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                      UUID id, String name, double amount,
                                      AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0D) {
            instance.addPermanentModifier(new AttributeModifier(id, name, amount, op));
        }
    }

    /**
     * 把玩家的关键属性打一份到服务端控制台。
     *
     * <p>专门为"加成看起来没生效"这类问题准备的：控制台里能直接看到
     * 最大生命/攻击力的最终值，以及每一个生效的属性修饰符。</p>
     */
    public static void dumpAttributeDiagnostics(ServerPlayer player, String when) {
        CultivationData data = CultivationCapability.get(player);
        Realm realm = data.realm();
        var src = player.getServer();
        if (src == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[hongmeng] === 属性诊断（").append(when).append("）玩家=")
                .append(player.getGameProfile().getName()).append(" ===\n");
        sb.append(String.format("  境界=%s (第%d境) 修为=%d 觉醒=%s 难度=%s 灵根=%s%n",
                realm.cn(), realm.tier(), data.getTotalCultivation(), data.isAwakened(),
                data.difficulty().cn(),
                data.getRoot() == null ? "无" : data.getRoot().cn()));
        sb.append(String.format("  境界生命加成=%.1f  境界攻击加成=%.1f%n",
                realm.healthBonus(), realm.damageBonus()));

        var hp = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        sb.append(String.format("  MAX_HEALTH 最终值=%.2f%n", hp == null ? -1.0D : hp.getValue()));
        if (hp != null) {
            hp.getModifiers().forEach(m -> sb.append(String.format(
                    "      · %s = %.2f (%s)%n", m.getName(), m.getAmount(), m.getOperation())));
        }

        var atk = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        sb.append(String.format("  ATTACK_DAMAGE 最终值=%.2f%n", atk == null ? -1.0D : atk.getValue()));
        if (atk != null) {
            atk.getModifiers().forEach(m -> sb.append(String.format(
                    "      · %s = %.2f (%s)%n", m.getName(), m.getAmount(), m.getOperation())));
        }

        var spd = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        sb.append(String.format("  MOVEMENT_SPEED 最终值=%.4f  mayfly=%s flying=%s creative=%s%n",
                spd == null ? -1.0D : spd.getValue(),
                player.getAbilities().mayfly, player.getAbilities().flying, player.isCreative()));

        var tier = RealmMining.realmTier(data);
        sb.append(String.format("  空手挖掘=%s (harvestLevel=%d, 内部 tier=%s, 可破坏一切=%s)%n",
                tier == null ? "无" : RealmMining.describeTierObject(tier),
                realm.harvestLevel(), tier == null ? "null" : String.valueOf(tier.getLevel()),
                realm.canBreakAnything()));

        // 准星方块的挖掘判定——这是"破坏不掉落"最直接的证据
        var hit = player.pick(8.0D, 0.0F, false);
        if (hit instanceof net.minecraft.world.phys.BlockHitResult bhr
                && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            var st = player.level().getBlockState(bhr.getBlockPos());
            var req = RealmMining.requiredTier(st);
            sb.append(String.format("  准星方块=%s  需求=%s  原版hasCorrectTool=%s  canHarvest=%s%n",
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()),
                    RealmMining.describeTierObject(req),
                    player.hasCorrectToolForDrops(st),
                    tier != null && RealmMining.canHarvest(tier, req)));
        } else {
            sb.append("  准星方块=（没对着方块）\n");
        }

        sb.append("[hongmeng] === 诊断结束 ===");
        com.hongmeng.xiuxian.HongmengMod.LOGGER.info(sb.toString());
    }

    /** 死亡演出：不掉物品，但要说点什么。 */
    public static void deathDrama(ServerPlayer player) {
        if (!HongmengConfig.SERVER.deathDrama.get()) {
            return;
        }
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00a74\u00a7l═══ 角 色 死 亡 ═══"));
        player.sendSystemMessage(Component.literal(
                "\u00a77视野一黑。你听见远处有人在喊你的名字，但声音越来越远。"));
        player.sendSystemMessage(Component.literal(
                "\u00a77\u201c……这退出键，到底在哪？\u201d"));
        player.sendSystemMessage(Component.literal(""));
    }

    /** 供命令与面板查询。 */
    public static void forceAwaken(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        if (data.isAwakened()) {
            Awakening.reroll(player, data, true);
        } else {
            Awakening.awaken(player, data, "天地");
        }
    }

    static void dummy(Player player) {
        player.getPersistentData();
    }
}
