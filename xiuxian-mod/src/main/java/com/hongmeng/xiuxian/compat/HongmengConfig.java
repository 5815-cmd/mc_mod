package com.hongmeng.xiuxian.compat;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 全部可调数值。服务器配置（单人存档就是存档级配置）。
 */
public final class HongmengConfig {

    /**
     * 物品数量的**上限**：64（原版值）。
 *
 * <p><b>已从 127 改回 64</b>（玩家要求：「取消 127 堆叠，一组 64 就行了，
 * 我后期加其它 mod 增加堆叠」）。历史上放 127 的理由是"有符号字节的最大值、存盘不截断"，
 * 但玩家后期会用专门的堆叠模组做超堆叠，本模组不该抢这件事；
 * 而且 64 是原版语义 —— 与容器、漏斗、比较器、合成表的交互都不会出意外。</p>
     *
     * <p>这个数字不是拍的，是反汇编出来的：{@code ItemStack.save()} 里
     * {@code getfield count:I → i2b → putByte("Count", ...)}，
     * 而 {@code ItemStack(CompoundTag)} 里
     * {@code getByte("Count") → putfield count:I}——<b>读写都是 1 字节</b>。</p>
     *
     * <p>换句话说 1024 这种值在存盘时会走 {@code (byte) 1024 == 0}，
     * 读回来 {@code count == 0}，也就是 {@code isEmpty()} 为真：
     * 整整一堆东西在下线再上线之后<b>静默消失</b>，日志里一个字都没有。
     * 网络同步（{@code FriendlyByteBuf.writeItemStack} 的
     * {@code getCount(); writeByte(I)}）同样只有 1 字节。</p>
     *
     * <p>所以本模组的堆叠上限天花板现在钉在 64（原版值）。要改这个常量，
     * 先去看 {@code _cache/stackprobe/StackSizeProbe.java} 的实测输出。</p>
     */
    public static final int SAFE_STACK_MAX = 64;

    public static final ForgeConfigSpec SPEC;
    public static final Server SERVER;

    /**
     * 客户端配置（{@code hongmeng-client.toml}）。
     *
     * <p><b>为什么"关闭屏幕震动"必须放在客户端配置里</b>：多人游戏里客户端读不到服务端
     * 配置。而这一条是<b>无障碍选项</b>——晕 3D 的玩家要能在进服务器之前就关掉，
     * 更不能因为服主没开这个选项而关不掉。所以它跟 {@link #SPEC}（服务端）分家。</p>
     *
     * <p>注册在 {@code HongmengMod} 的客户端分支里（{@code ModConfig.Type.CLIENT}），
     * 所以专用服务器上既不会加载它，也不会去读它。</p>
     */
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Server, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = pair.getLeft();
        SPEC = pair.getRight();
        Pair<Client, ForgeConfigSpec> clientPair =
                new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
    }

    private HongmengConfig() {
    }

    public static final class Server {

        // ---------- 灵根觉醒 ----------
        public final ForgeConfigSpec.BooleanValue awakenOnJoin;
        public final ForgeConfigSpec.IntValue awakenDelayTicks;
        public final ForgeConfigSpec.BooleanValue allowRerollByCommand;

        // ---------- 修炼 ----------
        public final ForgeConfigSpec.DoubleValue cultivationMultiplier;
        public final ForgeConfigSpec.DoubleValue meditationPerSecond;
        public final ForgeConfigSpec.DoubleValue killBaseGain;
        public final ForgeConfigSpec.DoubleValue killHostileMultiplier;
        public final ForgeConfigSpec.DoubleValue bossGain;
        public final ForgeConfigSpec.DoubleValue breakthroughGain;
        public final ForgeConfigSpec.BooleanValue meditationAtNightBonus;
        public final ForgeConfigSpec.DoubleValue nightBonusMultiplier;

        // ---------- 突破 ----------
        public final ForgeConfigSpec.DoubleValue baseSuccessRate;
        public final ForgeConfigSpec.DoubleValue minSuccessRate;
        public final ForgeConfigSpec.DoubleValue karmaPenaltyPerStack;
        public final ForgeConfigSpec.IntValue maxKarma;
        public final ForgeConfigSpec.DoubleValue failExpLossRatio;
        public final ForgeConfigSpec.BooleanValue failDropRealm;
        public final ForgeConfigSpec.BooleanValue requireFullCultivation;

        // ---------- 业力 ----------
        public final ForgeConfigSpec.BooleanValue karmaEnabled;
        public final ForgeConfigSpec.BooleanValue karmaOnDeath;
        public final ForgeConfigSpec.BooleanValue enchantedAppleClearsAll;
        public final ForgeConfigSpec.IntValue raidKarmaReduction;

        // ---------- 沉浸感 ----------
        public final ForgeConfigSpec.BooleanValue hideExitButton;
        public final ForgeConfigSpec.BooleanValue painEffects;
        public final ForgeConfigSpec.BooleanValue deathDrama;
        public final ForgeConfigSpec.BooleanValue keepItemsOnDeath;

        // ---------- 境界实力数值 ----------
        public final ForgeConfigSpec.DoubleValue realmHpScale;
        public final ForgeConfigSpec.DoubleValue realmDamageScale;
        /** 境界护甲的倍率（1.0 = 按 Realm.ARMOR_BY_TIER 原值）。 */
        public final ForgeConfigSpec.DoubleValue realmArmorScale;

        // ---------- 难度与怪物 ----------
        public final ForgeConfigSpec.DoubleValue mobScalingStrength;

        // ---------- 本命领域 ----------
        /** 领域边长（区块）。规格：10 × 10 → 6 × 6。 */
        public final ForgeConfigSpec.IntValue domainChunks;
        /** 领域效果总开关。 */
        public final ForgeConfigSpec.BooleanValue domainEffectsEnabled;
        /** 一次扫描最多处理多少目标。 */
        public final ForgeConfigSpec.IntValue domainMaxTargets;

        // ---------- 堆叠上限 ----------
        /**
         * 材料类物品的堆叠上限。
         *
         * <p><b>为什么是 64：</b>{@code ItemStack.save()} 用
         * {@code putByte("Count", (byte) count)} 写数量，{@code ItemStack(CompoundTag)}
         * 用 {@code getByte("Count")} 读回来——<b>上下都是 1 字节</b>。
         * 数量 &gt; 127 会在存盘/读盘时截断（256 的倍数直接变 0，
         * 也就是"这一整堆静默消失"）。实测见
         * {@code _cache/stackprobe/StackSizeProbe.java}。</p>
         *
         * <p>曾经用 127（有符号字节能表示的最大值，也是"存盘不截断"的上限），
         * 但那会与"后期再装一个专门做超堆叠的模组"打架 ——
         * 玩家明确要求回到原版的 <b>64</b>，所以这里就钉在 64。</p>
         */
        public final ForgeConfigSpec.IntValue materialStackSize;

        // ---------- 特效（法术 / 领域） ----------
        /** 法术与领域粒子密度倍率。见 {@code technique/SpellVfx}。 */
        public final ForgeConfigSpec.DoubleValue particleDensity;

        // ---------- 战斗手感（docs/combat-feel-spec.md） ----------
        /** 战斗手感总开关（顿帧 / 白闪 / 击退 / 音效 / 粒子 / 登场演出）。 */
        public final ForgeConfigSpec.BooleanValue combatFeelEnabled;
        /** 顿帧开关。 */
        public final ForgeConfigSpec.BooleanValue hitstopEnabled;
        /** 顿帧时长倍率。 */
        public final ForgeConfigSpec.DoubleValue hitstopScale;
        /** 顿帧是否连攻击者一起冻。 */
        public final ForgeConfigSpec.BooleanValue hitstopAttacker;
        /** 击退强度倍率。 */
        public final ForgeConfigSpec.DoubleValue knockbackScale;
        /** 每维度每 tick 的战斗粒子硬预算（战斗手感这一路自己那一份）。 */
        public final ForgeConfigSpec.IntValue maxParticlesPerTick;
        /** 强者登场演出开关。 */
        public final ForgeConfigSpec.BooleanValue bossEntrance;

        // ---------- 经济（灵叶） ----------
        public final ForgeConfigSpec.DoubleValue leafDropChance;
        public final ForgeConfigSpec.IntValue leafDropMax;

        // ---------- 境界挖掘 ----------
        public final ForgeConfigSpec.BooleanValue realmMining;
        public final ForgeConfigSpec.BooleanValue spawnerDropsAtNetherite;
        public final ForgeConfigSpec.DoubleValue miningSpeedPerRealmTier;
        public final ForgeConfigSpec.IntValue omniMiningTicks;

        Server(ForgeConfigSpec.Builder b) {
            b.comment("灵根觉醒：进入世界的第一次抽灵根").push("awakening");
            awakenOnJoin = b.comment("首次进入世界时自动觉醒灵根")
                    .define("awakenOnJoin", true);
            awakenDelayTicks = b.comment("进入世界后延迟多少 tick 才觉醒（20 tick = 1 秒）")
                    .defineInRange("awakenDelayTicks", 60, 0, 12000);
            allowRerollByCommand = b.comment("允许用 /hongmeng reroll 重抽灵根（用于测试，正式玩法请关掉）")
                    .define("allowRerollByCommand", true);
            b.pop();

            b.comment("修炼与修为获取").push("cultivation");
            cultivationMultiplier = b.comment("全局修为获取倍率")
                    .defineInRange("cultivationMultiplier", 1.0D, 0.0D, 1000.0D);
            meditationPerSecond = b.comment("打坐每秒钟获得的基础修为")
                    .defineInRange("meditationPerSecond", 1.0D, 0.0D, 10000.0D);
            killBaseGain = b.comment("击杀生物的基础修为（再乘生物强度系数）")
                    .defineInRange("killBaseGain", 3.0D, 0.0D, 10000.0D);
            killHostileMultiplier = b.comment("敌对生物额外倍率")
                    .defineInRange("killHostileMultiplier", 2.0D, 0.0D, 100.0D);
            bossGain = b.comment("击杀 BOSS（末影龙/凋灵/监守者等）的额外固定修为")
                    .defineInRange("bossGain", 800.0D, 0.0D, 1000000.0D);
            breakthroughGain = b.comment("突破成功后的额外修为")
                    .defineInRange("breakthroughGain", 50.0D, 0.0D, 1000000.0D);
            meditationAtNightBonus = b.comment("夜晚打坐有额外加成（" + "子时修炼" + "）")
                    .define("meditationAtNightBonus", true);
            nightBonusMultiplier = b.comment("夜晚打坐倍率")
                    .defineInRange("nightBonusMultiplier", 1.5D, 1.0D, 100.0D);
            b.pop();

            b.comment("境界突破").push("breakthrough");
            baseSuccessRate = b.comment("基础突破成功率")
                    .defineInRange("baseSuccessRate", 0.85D, 0.0D, 1.0D);
            minSuccessRate = b.comment("成功率下限")
                    .defineInRange("minSuccessRate", 0.05D, 0.0D, 1.0D);
            karmaPenaltyPerStack = b.comment("每层业力降低的突破成功率")
                    .defineInRange("karmaPenaltyPerStack", 0.10D, 0.0D, 1.0D);
            maxKarma = b.comment("业力上限（达到上限则必然突破失败）")
                    .defineInRange("maxKarma", 10, 1, 100);
            failExpLossRatio = b.comment("突破失败损失的修为比例")
                    .defineInRange("failExpLossRatio", 0.30D, 0.0D, 1.0D);
            failDropRealm = b.comment("突破失败是否掉一个小境界")
                    .define("failDropRealm", false);
            requireFullCultivation = b.comment(
                            "突破前必须把当前小境界的修为攒满（关闭则任何修为都能催动）")
                    .define("requireFullCultivation", true);
            b.pop();

            b.comment("业力：死亡的代价").push("karma");
            karmaEnabled = b.comment("启从业力系统")
                    .define("karmaEnabled", true);
            karmaOnDeath = b.comment("每次死亡增加一层业力")
                    .define("karmaOnDeath", true);
            enchantedAppleClearsAll = b.comment("附魔金苹果一次性清除全部业力")
                    .define("enchantedAppleClearsAll", true);
            raidKarmaReduction = b.comment("每解决一次劫掠袭击事件消除的业力层数")
                    .defineInRange("raidKarmaReduction", 1, 0, 100);
            b.pop();

            b.comment("沉浸机制：没有退出键").push("immersion");
            hideExitButton = b.comment("隐藏暂停菜单里的『保存并退出』按钮（保留一个需要长按的紧急退出）")
                    .define("hideExitButton", true);
            painEffects = b.comment("受伤时的真实痛觉表现（画面晃动、痛觉层数叠加）")
                    .define("painEffects", true);
            deathDrama = b.comment("死亡时的『角色死亡』演出")
                    .define("deathDrama", true);
            keepItemsOnDeath = b.comment("死亡不掉落物品")
                    .define("keepItemsOnDeath", true);
            b.pop();

            b.comment("境界挖掘：境界当空手工具等级用").push("realm_mining");
            realmMining = b.comment(
                            "开启后：金丹=石制、化神=铁制、道印=金制、飞升=钻石、仙人=下界合金；"
                                    + "等级不够时挖得动但不掉东西；鸿蒙可破坏一切方块（含基岩）")
                    .define("realmMining", true);
            spawnerDropsAtNetherite = b.comment(
                            "下界合金级（仙人及以上）破坏刷怪笼时，连笼子一起掉落（原本只有精准采集能做到）"
            + "（预留：当前版本未接入刷怪笼掉落，改这里无效果）")
                    .define("spawnerDropsAtNetherite", true);
            miningSpeedPerRealmTier = b.comment(
                            "每高一个大境界，空手挖掘速度加成（0 表示不改原版速度）"
            + "（预留：当前版本未接入挖掘速度，改这里无效果）")
                    .defineInRange("miningSpeedPerRealmTier", 0.0D, 0.0D, 10.0D);
            omniMiningTicks = b.comment(
                            "鸿蒙强行破坏『不可破坏方块』（基岩等）需要盯住多少 tick（20 tick = 1 秒）")
                    .defineInRange("omniMiningTicks", 60, 5, 600);
            b.pop();

            b.comment("境界实力数值（生命与攻击）").push("realm_scale");
            realmHpScale = b.comment(
                            "生命加成的整体缩放系数。基准：筑基 = +5 颗心（10 生命），"
                                    + "金丹起每境翻倍，飞升 ×3，仙人/帝境/鸿蒙各 ×10；"
                                    + "鸿蒙原值是 80 万颗心，觉得夸张就调小这个系数（不影响境界本身）")
                    .defineInRange("realmHpScale", 1.0D, 0.0D, 1000.0D);
            realmDamageScale = b.comment(
                            "攻击加成的整体缩放系数。基准：筑基 = +3 点攻击，后续与生命同倍率增长")
                    .defineInRange("realmDamageScale", 1.0D, 0.0D, 1000.0D);
            realmArmorScale = b.comment(
                            "境界护甲的倍率。基准：练气 0 / 筑基 2 / 金丹 4 / 元婴 6 / 化神 10 /"
                                    + " 道印 16 / 飞升 24 / 仙人 36 / 帝境 52 / 鸿蒙 72。"
                                    + "注意原版护甲 20 点就接近封顶减伤，超出部分由递减公式处理，"
                                    + "拉高这个值不会变成无敌")
                    .defineInRange("realmArmorScale", 1.0D, 0.0D, 100.0D);
            b.pop();

            b.comment("难度与怪物强度").push("difficulty");
            mobScalingStrength = b.comment(
                            "怪物强度缩放的整体力度。1.0 = 完整生效（普通 ×1.00 ~ 鸿蒙 ×3.00，"
                                    + "再乘玩家境界系数）；0.0 = 完全不缩放（等价原版）")
                    .defineInRange("mobScalingStrength", 1.0D, 0.0D, 5.0D);
            b.pop();

            b.comment("经济：灵叶（坊市货币）").push("economy");
            leafDropChance = b.comment(
                            "杀敌对生物掉落灵叶的概率（0.35 = 35%）。灵叶是坊市货币，"
                                    + "存入坊市余额后不占背包；来源还有采灵草、任务与副本奖励。"
                                    + "掉落数量按怪物品阶走（普通 2~4 / 精英 6~10 / 队长 15~25 /"
                                    + " 头目 40~60 / 首领 100~150），见 shop/LeafRewards")
                    .defineInRange("leafDropChance", 0.35D, 0.0D, 1.0D);
            leafDropMax = b.comment(
                            "原版敌对生物（僵尸、骷髅等）一次掉落的灵叶上限（至少掉 1 片）。"
                                    + "修仙界妖兽按品阶表掉落，不受这个值影响")
                    .defineInRange("leafDropMax", 2, 1, 64);
            b.pop();

            // ---- 本命领域（docs/domain-spec.md） ----
            //
            // 玩家明确要求：领域范围从 10×10 区块改成 6×6 区块，**并且做成配置项**，
            // 方便整合包里再调。6×6 区块 = 96×96 格。
            //
            // 八种元素的效果数值**不在这里**：那些数字玩家说了一个都不能改
            // （见 domain/DomainEffectTable，纯数据表 + 离线可断言）。
            b.comment("本命领域（道宫技）").push("domain");
            domainChunks = b.comment(
                            "领域边长（区块，正方形）。默认 6 = 6×6 区块 = 96×96 格，"
                                    + "以玩家为中心。原来是 10×10 区块（160×160 格），玩家嫌太大。"
                                    + "竖直方向固定为上下 32 格")
                    .defineInRange("domainChunks", 6, 1, 64);
            domainEffectsEnabled = b.comment(
                            "领域效果总开关。关掉后领域只剩功法加成（同元素伤害/释放速度），"
                                    + "不再对怪物结算八种元素效果")
                    .define("domainEffectsEnabled", true);
            domainMaxTargets = b.comment(
                            "领域一次扫描最多结算多少个目标（防止 96×96 格怪海里一帧跑几千次结算）")
                    .defineInRange("domainMaxTargets", 64, 1, 512);
            b.pop();

            // ---- 特效密度 ----
            // 玩家要"特效多一点华丽一点"。粒子是白送的观感，但也是唯一
            // 能把服务器拖垮的东西：一次法术几百个粒子、几十个人一起放就完了。
            // 所以密度必须可调，而且默认值要保守（1.0 = 设计值）。
            b.comment("法术 / 领域特效").push("vfx");
            particleDensity = b.comment(
                            "粒子密度倍率（默认 1.0，0 = 完全关粒子但伤害与音效不受影响）。"
                                    + "**全模组只有这一个粒子旋钮**：战斗命中、法术特效、领域特效"
                                    + "都读它，由 combat/ParticleBudget 统一执行预算。"
                                    + "觉得卡就往下调，觉得不够华丽就往上调（上限 4.0）")
                    .defineInRange("particleDensity", 1.0D, 0.0D, 4.0D);
            b.pop();

            // ---- 堆叠上限 ----
            b.comment("物品堆叠上限").push("stack");
            materialStackSize = b.comment(
                            "本模组材料类物品的堆叠上限。默认 64（原版值）—— 这是**上限**。"
                                    + "历史上放过 127，但那会与后期加装的超堆叠模组打架，"
                                    + "玩家要求回到原版的 64。"
                                    + "ItemStack.save() 用 putByte 写 Count、读回来用 getByte，"
                                    + "上下都是 1 字节（256 的倍数直接变 0 = 整堆静默消失），"
                                    + "所以无论怎么调都不该超过 127。")
                    .defineInRange("materialStackSize", 64, 1, 64);
            b.pop();

            // ---- 战斗手感（docs/combat-feel-spec.md） ----
            //
            // 规格 §零 的两条硬约束：必须有粒子预算 + 配置倍率；顿帧绝不真暂停游戏。
            // 屏幕震动的开关**不在这里**，在客户端配置（CLIENT.screenShake）——
            // 那条是无障碍选项，多人游戏里客户端读不到服务端配置，必须放在客户端那份。
            b.comment("战斗手感（顿帧 / 击退 / 命中粒子）").push("combat");
            combatFeelEnabled = b.comment(
                            "战斗手感总开关。关掉后：顿帧、命中白闪、击退、命中音效分层、"
                                    + "命中粒子爆发、强者登场演出全部停止。"
                                    + "伤害与数值一点不受影响（这里只做表现）")
                    .define("combatFeelEnabled", true);
            hitstopEnabled = b.comment(
                            "顿帧（hitstop）开关。命中造成目标最大生命 2% 以上时，"
                                    + "攻击者与受击者冻结几 tick（2~6 tick，按伤害占比）。"
                                    + "这是【爽】的第一来源：冻结实体，绝不暂停游戏 tick")
                    .define("hitstopEnabled", true);
            hitstopScale = b.comment(
                            "顿帧时长倍率。1.0 = 规格值（2% →2t / 10% →4t / 25% →6t）；"
                                    + "0.5 = 全部减半；0.0 = 等于关掉顿帧；上限 4.0")
                    .defineInRange("hitstopScale", 1.0D, 0.0D, 4.0D);
            hitstopAttacker = b.comment(
                            "顿帧时连攻击者也一起冻住（规格要求【双方冻结】，"
                                    + "这是【打实了】的手感来源；极少数人会觉得像输入延迟，可关）")
                    .define("hitstopAttacker", true);
            knockbackScale = b.comment(
                            "击退强度倍率。击退本身按伤害占比放大（2% →0.30、25% →1.10 格/tick），"
                                    + "这里再整体乘一次。0 = 不击退")
                    .defineInRange("knockbackScale", 1.0D, 0.0D, 4.0D);
            maxParticlesPerTick = b.comment(
                            "每个维度每 tick 的战斗粒子硬预算（默认 400）。"
                                    + "所有命中爆发 / 余韵 / 登场演出共用这一个池子，花完就静默丢弃。"
                                    + "调大只会让【怪海里群架】更卡，不会让画面更好看")
                    .defineInRange("maxParticlesPerTick", 400, 0, 4096);
            bossEntrance = b.comment(
                            "强者登场演出（头目 / 首领在附近生成时：天变暗 + 钟声 + 冲击波 + 横幅）")
                    .define("bossEntrance", true);
            b.pop();
        }

        public double clampedMeditation() {
            return Math.max(0.0D, meditationPerSecond.get() * cultivationMultiplier.get());
        }

        /**
         * 材料堆叠上限（夹到安全区间）。
         *
         * <p>配置读不到时退回 {@link #SAFE_STACK_MAX}——配置只有在
         * Forge 加载完成后才有值，而物品注册比它更早。</p>
         */
        public int clampedMaterialStackSize() {
            try {
                return Math.max(1, Math.min(SAFE_STACK_MAX, materialStackSize.get()));
            } catch (Throwable ignored) {
                return SAFE_STACK_MAX;
            }
        }

        /** 粒子密度（0.0 ~ 4.0），配置读不到时按 1.0。 */
        public double clampedParticleDensity() {
            try {
                return Math.max(0.0D, Math.min(4.0D, particleDensity.get()));
            } catch (Throwable ignored) {
                return 1.0D;
            }
        }
    }

    /**
     * 客户端配置：**只影响这一台客户端看得见/听得到的东西**。
     *
     * <h2>为什么这些必须是客户端选项</h2>
     *
     * <p>规格 §二 把"关闭屏幕震动"写成硬约束，理由是<b>有人会晕</b>——那是无障碍问题。
     * 无障碍选项的判定权必须在玩家自己手上：</p>
     *
     * <ul>
     *   <li>多人游戏里客户端读不到服务端配置，放服务端就等于"服主不开你就晕着"；</li>
     *   <li>它影响的全是本地渲染/音效，服务端根本不需要知道（发过来的包只是"命中事实"，
     *       要不要震由本地决定——这也顺手满足了"不要为震动发包"）。</li>
     * </ul>
     *
     * <p>三个入口：R 面板（修行面板）标题行上的按钮 · 本文件 ·
     * Forge 自带的"模组 → 鸿蒙修仙 → 配置 → 客户端"界面。</p>
     */
    public static final class Client {
        /**
         * 法术贴图特效档位（玩家要求「可以在设置里改」）。
         *
         * <p>0 = 关（只留粒子，最省）· 1 = 低 · 2 = 中 · 3 = 高（默认）。
         * 同时影响可见距离、叠画层数、是否画粒子点缀层。</p>
         */
        public final ForgeConfigSpec.IntValue spellFxQuality;


        // ---------- 战斗手感（客户端） ----------
        /** 屏幕震动总开关（无障碍）。 */
        public final ForgeConfigSpec.BooleanValue screenShake;
        /** 屏幕震动强度倍率。 */
        public final ForgeConfigSpec.DoubleValue screenShakeStrength;
        /** 伤害飘字。 */
        public final ForgeConfigSpec.BooleanValue damageNumbers;
        /** 境界压制的 vignette + 心跳（晕 3D 的人可能只想关这一条）。 */
        public final ForgeConfigSpec.BooleanValue oppressionEffects;
        /** 强者登场的天空变暗 + 横幅。 */
        public final ForgeConfigSpec.BooleanValue bossEntrance;

        /**
         * FOV 冲击：击中目标时视野"推"一下。
         *
         * <p>玩家反馈过两次"打怪时屏幕放大抖动"，确认就是这一项 —— 所以<b>默认关闭</b>；
         * 打击感仍由顿帧、命中火花、伤害数字与音效承担，想要推背感可以在配置里打开。</p>
         */
        public final ForgeConfigSpec.BooleanValue fovPunch;

        Client(ForgeConfigSpec.Builder b) {
            b.comment("spell fx").push("spell_fx");
            spellFxQuality = b
                    .comment("spell texture fx level: 0=off 1=low 2=mid 3=high")
                    .defineInRange("spellFxQuality", 3, 0, 3);
            b.pop();

            b.comment("战斗手感（客户端）：只影响这台机器的画面与声音").push("combat");
            screenShake = b.comment(
                            "屏幕震动开关。2026-11-07 起全局强制关闭，默认 false；"
                                    + "即使手动改 true 也会被 CombatShake.FORCE_DISABLED 拦住。"
                                    + "【无障碍选项】会晕 3D / 晕动的玩家不需要再手动关。"
                                    + "关掉之后命中的其他反馈（顿帧、白闪、击退、飘字、音效）全部保留")
                    .define("screenShake", false);
            screenShakeStrength = b.comment(
                            "屏幕震动强度倍率。0.5 = 减半（还想留一点手感但觉得晃）；"
                                    + "1.0 = 规格值（普通命中 0.72 度 / 大招 2.5~3 度 / 领域与首领 4.5~6 度）；"
                                    + "上限 2.0")
                    .defineInRange("screenShakeStrength", 1.0D, 0.0D, 2.0D);
            damageNumbers = b.comment(
                            "伤害飘字。打中时在命中点飘出伤害数字（大招是金色的、"
                                    + "自己挨打是红色的大字）")
                    .define("damageNumbers", true);
            oppressionEffects = b.comment(
                            "境界压制表现：被比自己境界高的妖兽盯上时，屏幕边缘变暗 + 心跳声 + "
                                    + "准星轻抖。数值上的压制减伤不受这个开关影响")
                    .define("oppressionEffects", true);
            bossEntrance = b.comment(
                            "强者登场演出：头目 / 首领在附近生成时天空短暂变暗 + 名牌从上方压下")
                    .define("bossEntrance", true);
            fovPunch = b.comment(
                            "击中目标时视野\"推\"一下（FOV 冲击）。玩家反馈过两次"
                                    + "\"打怪时屏幕放大抖动\"，确认就是这一项，所以默认关闭。"
                                    + "想要更重的打击感可以打开；顿帧、火花、数字、音效都不受影响。")
                    .define("fovPunch", false);
            b.pop();
        }
    }
}
