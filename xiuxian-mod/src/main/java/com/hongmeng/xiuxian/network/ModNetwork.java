package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.HongmengMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {

    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                net.minecraft.resources.ResourceLocation.tryBuild(HongmengMod.MOD_ID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

        int id = 0;
        CHANNEL.messageBuilder(SyncCultivationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCultivationPacket::encode)
                .decoder(SyncCultivationPacket::decode)
                .consumerMainThread(SyncCultivationPacket::handle)
                .add();

        // Boss 出场（虚空裂缝 + 缓慢降落）
        CHANNEL.messageBuilder(BossEntrancePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BossEntrancePacket::encode)
                .decoder(BossEntrancePacket::decode)
                .consumerMainThread(BossEntrancePacket::handle)
                .add();

        // 法术**贴图**特效（光柱 / 冲击环 / 穹顶 / 爆闪 / 巨剑 / 陨石 / 虚空裂缝…）
        CHANNEL.messageBuilder(SpellFxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpellFxPacket::encode)
                .decoder(SpellFxPacket::decode)
                .consumerMainThread(SpellFxPacket::handle)
                .add();

        // BOSS 出场演出（关键帧镜头；服务端算好一次发给客户端，避免两端不同步）
        CHANNEL.messageBuilder(CinematicPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CinematicPacket::encode)
                .decoder(CinematicPacket::decode)
                .consumerMainThread(CinematicPacket::handle)
                .add();

        CHANNEL.messageBuilder(PainFlashPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PainFlashPacket::encode)
                .decoder(PainFlashPacket::decode)
                .consumerMainThread(PainFlashPacket::handle)
                .add();

        CHANNEL.messageBuilder(BreakthroughFxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BreakthroughFxPacket::encode)
                .decoder(BreakthroughFxPacket::decode)
                .consumerMainThread(BreakthroughFxPacket::handle)
                .add();

        CHANNEL.messageBuilder(ToggleSpeedPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleSpeedPacket::encode)
                .decoder(ToggleSpeedPacket::decode)
                .consumerMainThread(ToggleSpeedPacket::handle)
                .add();

        CHANNEL.messageBuilder(ClaimQuestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClaimQuestPacket::encode)
                .decoder(ClaimQuestPacket::decode)
                .consumerMainThread(ClaimQuestPacket::handle)
                .add();

        CHANNEL.messageBuilder(DomainActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DomainActionPacket::encode)
                .decoder(DomainActionPacket::decode)
                .consumerMainThread(DomainActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(ChooseDifficultyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChooseDifficultyPacket::encode)
                .decoder(ChooseDifficultyPacket::decode)
                .consumerMainThread(ChooseDifficultyPacket::handle)
                .add();

        CHANNEL.messageBuilder(ShopBuyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShopBuyPacket::encode)
                .decoder(ShopBuyPacket::decode)
                .consumerMainThread(ShopBuyPacket::handle)
                .add();

        CHANNEL.messageBuilder(EquipmentActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EquipmentActionPacket::encode)
                .decoder(EquipmentActionPacket::decode)
                .consumerMainThread(EquipmentActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(BreakthroughPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BreakthroughPacket::encode)
                .decoder(BreakthroughPacket::decode)
                .consumerMainThread(BreakthroughPacket::handle)
                .add();

        // 择道（C2S，带道途序号）—— 修炼面板点五个道途之一时发。
        // 服务端走 Talents.choose（它会自己拒绝已择道），客户端不能伪造。
        CHANNEL.messageBuilder(PathChoosePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PathChoosePacket::encode)
                .decoder(PathChoosePacket::decode)
                .consumerMainThread(PathChoosePacket::handle)
                .add();

        // 副业制作（C2S，带配方 id）—— 副业界面点配方时发。
        // 判定与扣材料全在服务端；客户端只发"我想做这条"。
        CHANNEL.messageBuilder(CraftPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CraftPacket::encode)
                .decoder(CraftPacket::decode)
                .consumerMainThread(CraftPacket::handle)
                .add();

        // 打开界面（S2C）—— 命令让客户端 setScreen。
        // 服务端不直接引用客户端 Screen 类（专服上没有那些类）。
        CHANNEL.messageBuilder(OpenUiPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenUiPacket::encode)
                .decoder(OpenUiPacket::decode)
                .consumerMainThread(OpenUiPacket::handle)
                .add();

        // 天赋加点（C2S，带节点 id）—— 客户端面板点天赋树时发
        CHANNEL.messageBuilder(TalentPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TalentPacket::encode)
                .decoder(TalentPacket::decode)
                .consumerMainThread(TalentPacket::handle)
                .add();

        CHANNEL.messageBuilder(FlightSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FlightSyncPacket::encode)
                .decoder(FlightSyncPacket::decode)
                .consumerMainThread(FlightSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(TechniqueActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TechniqueActionPacket::encode)
                .decoder(TechniqueActionPacket::decode)
                .consumerMainThread(TechniqueActionPacket::handle)
                .add();

        // 新增包一律追加在末尾：协议 id = 注册顺序，插队会让老客户端对不上号
        CHANNEL.messageBuilder(ShopDepositPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShopDepositPacket::encode)
                .decoder(ShopDepositPacket::decode)
                .consumerMainThread(ShopDepositPacket::handle)
                .add();

        // ---- 灵力 / 御剑（同样只追加，不动上面任何一个 id）----

        CHANNEL.messageBuilder(ManaSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ManaSyncPacket::encode)
                .decoder(ManaSyncPacket::decode)
                .consumerMainThread(ManaSyncPacket::handle)
                .add();

        // 御剑开关：上行是"按键事件"（SwordFlightPacket），
        // 下行是"脚下要不要画剑"（SwordVisualPacket）。
        // 拆成两个类是必须的——SimpleChannel 每个包类型只能注册一次。


        // ---- 法术蓄力（同样只追加）----
        // 服务端 → 客户端：蓄力条画在准星下方，只在起手/中断/完成时发一包。
        CHANNEL.messageBuilder(CastProgressPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CastProgressPacket::encode)
                .decoder(CastProgressPacket::decode)
                .consumerMainThread(CastProgressPacket::handle)
                .add();

        // ---- 坊市回收（卖东西给坊市）----
        // 同样**只追加在末尾**：协议 id = 注册顺序，插队会让老客户端对不上号。
        // 注册在 CastProgressPacket 之后，所以它是本频道第 17 个包（id = 16）。
        CHANNEL.messageBuilder(ShopSellPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShopSellPacket::encode)
                .decoder(ShopSellPacket::decode)
                .consumerMainThread(ShopSellPacket::handle)
                .add();

        // ---- 战斗手感（docs/combat-feel-spec.md）----
        // **只追加在末尾**：协议 id = 注册顺序，插队会让老客户端对不上号。
        // 注册在 ShopSellPacket 之后，所以它是本频道第 18 个包（id = 17）。
        //
        // 里面装的是"命中事实"（伤害占比、命中点、受击者 id），不是"给我震一下"——
        // 震多强由客户端用自己的纯数据表算（见 CombatFeedbackPacket 的类注释）。
        CHANNEL.messageBuilder(CombatFeedbackPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CombatFeedbackPacket::encode)
                .decoder(CombatFeedbackPacket::decode)
                .consumerMainThread(CombatFeedbackPacket::handle)
                .add();

        // 攻击动作广播（谁挥了什么武器 + 第几段连击）
        CHANNEL.messageBuilder(AttackAnimPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AttackAnimPacket::encode)
                .decoder(AttackAnimPacket::decode)
                .consumerMainThread(AttackAnimPacket::handle)
                .add();

        // 真世界状态（世界级、极低频：开启一次 / 登录时补一次）
        CHANNEL.messageBuilder(RealWorldStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RealWorldStatePacket::encode)
                .decoder(RealWorldStatePacket::decode)
                .consumerMainThread(RealWorldStatePacket::handle)
                .add();

        // 怪物格挡的气盾（只发起手一次，客户端自己算淡入淡出）
        CHANNEL.messageBuilder(MobGuardFxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MobGuardFxPacket::encode)
                .decoder(MobGuardFxPacket::decode)
                .consumerMainThread(MobGuardFxPacket::handle)
                .add();

        // 雷劫天象（开始/结束，低频）
        CHANNEL.messageBuilder(TribulationFxPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TribulationFxPacket::encode)
                .decoder(TribulationFxPacket::decode)
                .consumerMainThread(TribulationFxPacket::handle)
                .add();
        // 单道落雷坐标（用于焦痕贴花）
        CHANNEL.messageBuilder(TribulationStrikePacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TribulationStrikePacket::encode)
                .decoder(TribulationStrikePacket::decode)
                .consumerMainThread(TribulationStrikePacket::handle)
                .add();

        // 怪物/Boss 攻击动作
        CHANNEL.messageBuilder(MobAttackAnimPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MobAttackAnimPacket::encode)
                .decoder(MobAttackAnimPacket::decode)
                .consumerMainThread(MobAttackAnimPacket::handle)
                .add();

        // 翻滚方向输入（客户端 → 服务端，只在变化时发）
        // 恶的天空状态
        CHANNEL.messageBuilder(com.hongmeng.xiuxian.network.EvilSkyPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(com.hongmeng.xiuxian.network.EvilSkyPacket::encode)
                .decoder(com.hongmeng.xiuxian.network.EvilSkyPacket::decode)
                .consumerMainThread(com.hongmeng.xiuxian.network.EvilSkyPacket::handle)
                .add();

        // 恶：坠落穿越
        CHANNEL.messageBuilder(com.hongmeng.xiuxian.network.EvilFallPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(com.hongmeng.xiuxian.network.EvilFallPacket::encode)
                .decoder(com.hongmeng.xiuxian.network.EvilFallPacket::decode)
                .consumerMainThread(com.hongmeng.xiuxian.network.EvilFallPacket::handle)
                .add();

        // 恶：全屏闪烁
        CHANNEL.messageBuilder(com.hongmeng.xiuxian.network.EvilFlashPacket.class, id++,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(com.hongmeng.xiuxian.network.EvilFlashPacket::encode)
                .decoder(com.hongmeng.xiuxian.network.EvilFlashPacket::decode)
                .consumerMainThread(com.hongmeng.xiuxian.network.EvilFlashPacket::handle)
                .add();

        // 恶之祭坛的召唤按钮
        CHANNEL.messageBuilder(com.hongmeng.xiuxian.network.EvilSummonPacket.class, id++,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.hongmeng.xiuxian.network.EvilSummonPacket::encode)
                .decoder(com.hongmeng.xiuxian.network.EvilSummonPacket::decode)
                .consumerMainThread(com.hongmeng.xiuxian.network.EvilSummonPacket::handle)
                .add();

    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        if (player == null || CHANNEL == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /**
     * 发给某个位置附近的玩家（法术贴图特效用）—— 全服广播既浪费带宽
     * 又让远处玩家白跑渲染，所以按范围发。
     */
    public static void sendToAllAround(net.minecraft.world.level.Level level,
                                       net.minecraft.world.phys.Vec3 at, Object packet) {
        if (CHANNEL == null || level == null || at == null) {
            return;
        }
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.NEAR.with(
                () -> new net.minecraftforge.network.PacketDistributor.TargetPoint(
                        at.x, at.y, at.z, 192.0D, level.dimension())), packet);
    }

    /** 客户端 -> 服务端发包（既有发包口只有服务端->客户端，天赋加点需要这个）。 */
    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToAll(Object packet) {
        if (CHANNEL == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
