package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.ModSounds;
import com.hongmeng.xiuxian.compat.HongmengConfig;
import com.hongmeng.xiuxian.network.BreakthroughFxPacket;
import com.hongmeng.xiuxian.network.ModNetwork;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 突破：把攒下的修为化成境界。
 *
 * <p>修为攒满是"自动跨层"（见 {@link Realm}），但真正意义上的"破境"
 * 需要玩家主动催动——成功率受业力影响，可以用破境丹顶。</p>
 */
public final class Breakthrough {

    /** 破境丹药力剩余 tick。 */
    private static final Map<UUID, Integer> PILL_BOOST = new ConcurrentHashMap<>();

    private Breakthrough() {
    }

    public static void grantPillBoost(ServerPlayer player, int ticks) {
        PILL_BOOST.put(player.getUUID(), ticks);
    }

    public static boolean hasPillBoost(ServerPlayer player) {
        Integer left = PILL_BOOST.get(player.getUUID());
        return left != null && left > 0;
    }

    /**
     * 每秒（20 tick）扣一次丹药剩余时间。
     *
     * <p><b>这里踩过一次崩溃，别再改回 removeIf + setValue 的写法</b>：
     * {@code ConcurrentHashMap.entrySet().removeIf(...)} 传给 lambda 的是
     * <b>不可变快照 Entry</b>（{@code SimpleImmutableEntry}），对它调 {@code setValue()}
     * 会抛 {@code UnsupportedOperationException}。而这段是"每玩家每 tick"跑的，
     * 于是**只要吃过破境丹就必崩**（玩家 19:30 那份崩溃报告就是这个签名）。</p>
     *
     * <p>现在只操作 Map 本身：先收集到期的 key，再 remove / put，全程不碰 Entry 对象。</p>
     */
    public static void tickPillBoosts() {
        if (PILL_BOOST.isEmpty()) {
            return;
        }
        java.util.List<UUID> expired = new java.util.ArrayList<>();
        for (Map.Entry<UUID, Integer> e : PILL_BOOST.entrySet()) {
            int left = e.getValue() - 20;
            if (left <= 0) {
                expired.add(e.getKey());
            } else {
                PILL_BOOST.put(e.getKey(), left);
            }
        }
        for (UUID id : expired) {
            PILL_BOOST.remove(id);
        }
    }

    public static void clear(UUID uuid) {
        PILL_BOOST.remove(uuid);
    }

    /** 实际成功率（含丹药与业力）。 */
    public static double effectiveRate(ServerPlayer player, CultivationData data) {
        if (hasPillBoost(player)) {
            return 1.0D;
        }
        return Karma.successRate(data);
    }

    /**
     * 尝试突破。玩家已经攒满当前层所需修为时才能催动。
     *
     * @return true 表示突破成功
     */
    public static boolean attempt(ServerPlayer player, CultivationData data) {
        if (player == null || data == null || !data.isAwakened()) {
            return false;
        }
        if (data.isMaxRealm()) {
            player.sendSystemMessage(Component.literal("\u00a76已至鸿蒙绝巅，前面没有路了。"));
            return false;
        }

        // 必须先把这一层的修为攒满才能催动。
        //
        // 这里原来是写反的：条件用的是 !hasSpace（还有空间就拦住），
        // 结果变成"任何修为都能突破，只有顶到上限才拒绝"。现在改成
        // "没攒满就拒绝"，并明确告诉玩家还差多少。
        if (HongmengConfig.SERVER.requireFullCultivation.get()) {
            long cap = capOf(data);
            long have = data.getCultivation();
            if (have < cap) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7c修为未满，催动不了这一层。"));
                player.sendSystemMessage(Component.literal(
                        "\u00a77还差 \u00a7e" + (cap - have) + " \u00a77点修为"
                                + "（当前 \u00a7f" + have + "\u00a77 / \u00a7f" + cap + "\u00a77）"));
                player.sendSystemMessage(Component.literal(
                        "\u00a78打坐、杀怪、吃修为丹都能涨修为；按 \u00a7fR \u00a78看进度。"));
                return false;
            }
        }

        // 这一层结束时会不会跨大境？
        boolean major = isMajorBreakthrough(data);

        if (major) {
            return attemptMajor(player, data);
        }
        return attemptMinor(player, data);
    }

    /**
     * 这层突破是否会跨大境。
     *
     * <p>规则：9 个大境各 3 层，所以**阶段 3 的突破 = 跨大境**
     * （练气后期→筑基初期、筑基后期→金丹初期……）。鸿蒙只有 1 层，是终点。</p>
     */
    public static boolean isMajorBreakthrough(CultivationData data) {
        return isMajorAtStep(data.unlockedStep());
    }

    /**
     * 纯函数版：第 {@code step} 层突破会不会跨大境。
     *
     * <p>不碰任何游戏对象，离线验证器可以直接断言。</p>
     */
    public static boolean isMajorAtStep(int step) {
        if (step < 0 || step >= Realm.TOTAL_STEPS - 1) {
            return false;
        }
        Realm cur = Realm.ofStep(step);
        Realm next = Realm.ofStep(step + 1);
        return next.tier() != cur.tier();
    }

    /**
     * 小境界突破：**看业力**，失败损失**三分之一的修为**。
     *
     * <p>不渡雷劫——雷劫只在大境界突破时降下。</p>
     */
    private static boolean attemptMinor(ServerPlayer player, CultivationData data) {
        if (Karma.isCapped(data)) {
            BreakthroughAnnouncer.karmaSealed(player, data);
            ModNetwork.sendToPlayer(player, new BreakthroughFxPacket(
                    BreakthroughFxPacket.Kind.TRIBULATION, ""));
            return false;
        }

        boolean boosted = hasPillBoost(player);
        double rate = effectiveRate(player, data);
        boolean success = boosted || player.getRandom().nextDouble() < rate;

        if (success) {
            applyAdvance(player, data);
            playSuccess(player);
            // 突破后的保护：抗性 I + 生命回复 II，60 秒
            ModEvents.applyBreakthroughBlessing(player);
            BreakthroughAnnouncer.announce(player, data.realm());
            clear(player.getUUID());
            ModNetworkBridge.sync(player);
            return true;
        }

        // 失败：损失三分之一的修为（玩家要求，原来按配置比例）
        // 按配置的比例扣（以前硬编码 `cultivation / 3`，配置项 failExpLossRatio
        // 声明了却从未被读取 —— 玩家改成 0.1 也毫无变化）。
        double lossRatio = HongmengConfig.SERVER.failExpLossRatio.get();
        long loss = Math.max(1L,
                (long) Math.round(data.getCultivation() * Math.max(0.0D, lossRatio)));
        data.setCultivation(Math.max(0L, data.getCultivation() - loss));
        if (HongmengConfig.SERVER.failDropRealm.get() && data.getCultivation() > 0) {
            long need = data.needed();
            if (data.progress() < need * 0.5D) {
                data.setCultivation(0L);
            }
        }
        playFail(player);
        player.hurt(player.damageSources().magic(), 4.0F);
        BreakthroughAnnouncer.fail(player, data, loss);
        if (!boosted) {
            clear(player.getUUID());
        }
        ModNetworkBridge.sync(player);
        return false;
    }

    /**
     * 大境界突破：**不看业力，看能不能扛过雷劫**。
     *
     * <p>流程：先降下雷劫（道数按大境递增），同时直接推进境界——
     * 玩家在接下来几秒里一道道挨雷，血扛不住就会死。所以"成功"是靠命换的，
     * 而不是掷骰子。</p>
     *
     * <p>道印之前的三个大境（练气→筑基、筑基→金丹、金丹→元婴）按玩家要求
     * <b>不渡劫</b>，直接成功。</p>
     */
    private static boolean attemptMajor(ServerPlayer player, CultivationData data) {
        int nextTier = Realm.ofStep(data.unlockedStep() + 1).tier();
        int bolts = tribulationBolts(nextTier);

        if (bolts <= 0) {
            // 道印之前的大境界：不渡劫，直接过
            applyAdvance(player, data);
            playSuccess(player);
            ModEvents.applyBreakthroughBlessing(player);
            BreakthroughAnnouncer.announce(player, data.realm());
            clear(player.getUUID());
            ModNetworkBridge.sync(player);
            player.sendSystemMessage(Component.literal(
                    "\u00a77（道印之前无需渡劫，此境安然跨过。）"));
            return true;
        }

        // 先推进与大境界相关的状态，再降雷——玩家是在"新境界"上挨雷的
        applyAdvance(player, data);
        playSuccess(player);
        // 渡劫前也给上保护：这 60 秒的抗性+回复正好帮玩家扛雷
        ModEvents.applyBreakthroughBlessing(player);
        BreakthroughAnnouncer.announce(player, data.realm());
        clear(player.getUUID());
        ModNetworkBridge.sync(player);

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "\u00a75\u00a7l═══ 大 境 界 · 雷 劫 ═══"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f你踏入了 \u00a7d" + data.realm().cn() + "\u00a7f，天威随之而来。"));
        player.sendSystemMessage(Component.literal(
                "\u00a7f共 \u00a7e" + bolts + " \u00a7f道，每道 \u00a7c"
                        + Math.round(boltDamageFor(player, nextTier, bolts)) + " \u00a7f点雷伤。"));
        player.sendSystemMessage(Component.literal(
                "\u00a7c扛不过去就是死。\u00a77（吃药、跑动、找掩体都行）"));
        player.sendSystemMessage(Component.literal(""));

        startTribulation(player, nextTier, bolts);
        return true;
    }

    /** 推进一层：清修为、层号 +1、给突破奖励。 */
    private static void applyAdvance(ServerPlayer player, CultivationData data) {
        long overflow = Math.max(0L, data.getCultivation() - data.realm().need());
        // 先推进层号，再写修为——否则 cultivationCap 算的还是旧层
        data.advanceStep();
        data.setCultivation(overflow);

        long bonus = Math.round(HongmengConfig.SERVER.breakthroughGain.get()
                * HongmengConfig.SERVER.cultivationMultiplier.get());
        CultivationSource.BREAKTHROUGH.grant(player, bonus, true);
        // 天赋点：按境界反推应得点数（**幂等**，反复调用不会白送）
        com.hongmeng.xiuxian.talent.Talents.onBreakthrough(player);
    }

    private static void playSuccess(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    80, 0.6D, 0.8D, 0.6D, 0.35D);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.BREAKTHROUGH_SUCCESS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void playFail(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    50, 0.5D, 0.6D, 0.5D, 0.05D);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.BREAKTHROUGH_FAIL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** 当前这层的修为上限（累计值）。 */
    private static long capOf(CultivationData data) {
        return data.realm().minTotal() + data.realm().need();
    }

    // ------------------------------------------------------------------
    // 纯逻辑（离线验证器可直接断言，不碰游戏对象）
    // ------------------------------------------------------------------

    /** 这一层还差多少修为才能催动突破；已满返回 0。 */
    public static long missingFor(long cultivation, long layerMinTotal, long layerNeed) {
        long cap = layerMinTotal + layerNeed;
        return Math.max(0L, cap - cultivation);
    }

    /** 修为是否已经攒满，可以催动突破。 */
    public static boolean isReady(long cultivation, long layerMinTotal, long layerNeed) {
        return missingFor(cultivation, layerMinTotal, layerNeed) <= 0L;
    }

    /**
     * 雷劫道数：**道印境起**才有，且此后每个境界都是九的倍数。
     *
     * <pre>
     *   道印（第 6 境）  9 道
     *   飞升（第 7 境） 18 道
     *   仙人（第 8 境） 27 道
     *   帝境（第 9 境） 36 道
     *   鸿蒙（第 10 境）45 道
     * </pre>
     *
     * <p>道印以下（练气~化神）不渡劫。</p>
     *
     * @param tier 当前大境序号（1~10）
     * @return 雷劫道数；0 表示不渡劫
     */
    public static int tribulationBolts(int tier) {
        if (tier < TRIBULATION_FIRST_TIER) {
            return 0;
        }
        return (tier - TRIBULATION_FIRST_TIER + 1) * TRIBULATION_BASE;
    }

    /** 开始渡雷劫的境界序号：道印。 */
    public static final int TRIBULATION_FIRST_TIER = 6;
    /** 每境的雷劫基数，道印 9 道，之后每境 +9。 */
    public static final int TRIBULATION_BASE = 9;

    /**
     * 每道雷的伤害。
     *
     * <p><b>调过一次。</b>原来按"本境标准值 × 20%"算——道印标准值 3900，
     * 每道就是 780 点，9 道 7000 点；而道印玩家也就 1080 血，
     * **必死无疑**，玩家反馈"根本就扛不住直接死"。</p>
     *
     * <p>现在改成按"**玩家自己的生命上限**"取比例，总伤害控制在
     * 约 {@value #TRIBULATION_TOTAL_HP_RATIO}% 的血量：</p>
     * <pre>
     *   每道伤害 = 生命上限 × TOTAL_RATIO / 道数
     * </pre>
     *
     * <p>这样无论哪个境界，雷劫都是"很痛但扛得住"——道数越多每道越轻。
     * 加上护甲与减伤，实际大概掉 4~5 成血，吃药能稳过、硬扛会死。</p>
     */
    public static double boltDamageFor(ServerPlayer player, int tier, int bolts) {
        if (bolts <= 0) {
            return 0.0D;
        }
        double maxHealth = player.getMaxHealth();
        return maxHealth * TRIBULATION_TOTAL_HP_RATIO / bolts;
    }

    /**
     * 雷劫总伤害占玩家生命上限的比例。
     *
     * <h2>⚠ 这个数改过，原因是一次<b>必死</b>的实现</h2>
     * <p>原来是 <b>2.2</b>。当时确实有保护：
     * {@code applyBreakthroughBlessing} 会给抗性 I（-20%）、生命回复 II、回满血。</p>
     *
     * <p>但这点保护远远不够，因为：</p>
     * <ol>
     *   <li>天劫用的是 {@code magic()} 伤害 —— 它在
     *       {@code BYPASSES_ARMOR} 标签里，<b>护甲完全不减免</b>
     *       （注释里"加上护甲"那半句是错的）；</li>
     *   <li>2.2 × 0.8 = <b>1.76 倍生命</b>，而 9 道雷只有 <b>36 tick</b> ——
     *       生命回复 II 在这段时间只回约 1.4 点血，杯水车薪；</li>
     *   <li>本项目没有回血<b>消耗品</b>（造化丹/回灵丹都不回血）。</li>
     * </ol>
     *
     * <p>于是 2.2 倍生命、零减免、零回血 → 9 道雷里第 5 道<b>必死</b>。
     * 而首次天劫在化神→道印，等于<b>没人能越过道印</b>，
     * 道印之后的全部内容（规则之力、30 把武器、80 件护甲、灭世法术）
     * 都锁在一个不可能通过的检查后面。</p>
     *
     * <p>现在取 <b>1.0</b>，配合渡劫时授予的抗性 I（−20%）：
     * 从满血打完剩约 <b>20%</b> —— 很痛、需要一点准备，但不是无条件处决。</p>
     */
    public static final double TRIBULATION_TOTAL_HP_RATIO = 1.0D;

    /**
     * 启动雷劫：一道道劈下来，每道都造成伤害。
     *
     * <p>每 4 tick 劈一道，玩家可以吃药、跑动、找掩体。</p>
     */
    private static void startTribulation(ServerPlayer player, int tier, int bolts) {
        if (bolts <= 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 表现：雷声 + 闪光
        level.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 2.0D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 0.8F, 1.4F);

        // ⚠ 渡劫前**回满血**。
        //
        //   否则"来的时候半血"就必死 —— 那和掷骰子没区别。
        //   玩家该准备的是"回满再来"，而不是被随机状态处决。
        player.setHealth(player.getMaxHealth());

        // 抗性 / 生命回复由 `ModEvents.applyBreakthroughBlessing` 提供
        // （`attemptMajor` 在降雷之前就调了它，抗性 I + 回复 II + 回满血，60 秒）。
        // **不要在这里再挂一次**：两个同等级的抗性不会叠加，
        // 但多一个来源会让人误以为"雷劫自己管增益"，以后改坏了对不上。
        TRIBULATION_PENDING.put(player.getUUID(), bolts);
        TRIBULATION_DAMAGE.put(player.getUUID(), boltDamageFor(player, tier, bolts));
        // 节奏状态：第一道要留出"天象先变"的时间（40 tick = 2 秒）
        TRIBULATION_INDEX.put(player.getUUID(), 0);
        TRIBULATION_TIER.put(player.getUUID(), tier);
        TRIBULATION_TOTAL.put(player.getUUID(), bolts);
        TRIBULATION_NEXT.put(player.getUUID(),
                level.getGameTime() + 40L);
        // 天象包：告诉客户端"渡劫开始了、这一场是什么色"
        try {
            var sky = new com.hongmeng.xiuxian.network.TribulationFxPacket(
                    true, tier, TRIBULATION_TOTAL_TICKS + 120);
            com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(player, sky);
        } catch (Throwable ignored) {
            // 天象包失败不影响雷劫
        }

        // 天象：雷雨（渡劫完恢复）
        boolean wasThunder = level.isThundering();
        TRIBULATION_WAS_THUNDER.put(player.getUUID(), wasThunder);
        try {
            level.setWeatherParameters(0, 20 * 60, true, true);
        } catch (Throwable ignored) {
            // 改天气失败不影响渡劫
        }
        // 节奏状态：第一道要留出"天象先变"的时间（40 tick = 2 秒）
        TRIBULATION_INDEX.put(player.getUUID(), 0);
        TRIBULATION_TIER.put(player.getUUID(), tier);
        TRIBULATION_TOTAL.put(player.getUUID(), bolts);
        TRIBULATION_NEXT.put(player.getUUID(),
                level.getGameTime() + 40L);


        player.displayClientMessage(Component.literal(
                "\u00a75\u00a7l雷劫降临——\u00a7f共 \u00a7e" + bolts + " \u00a7f道"), false);
        player.displayClientMessage(Component.literal(
                "\u00a77天威压顶，撑住。"), true);
    }

    /** 待劈的雷数。 */
    private static final Map<UUID, Integer> TRIBULATION_PENDING = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------
    // 雷劫节奏（玩家质疑"落雷频率太快"，据此重排）
    // ------------------------------------------------------------------

    /** 下一道雷的 gameTime。 */
    private static final Map<UUID, Long> TRIBULATION_NEXT = new ConcurrentHashMap<>();
    /** 本场雷劫的境界（天象色与落雷包要用）。 */
    private static final Map<UUID, Integer> TRIBULATION_TIER = new ConcurrentHashMap<>();
    /** 已经落下几道（从 0 开始）。 */
    private static final Map<UUID, Integer> TRIBULATION_INDEX = new ConcurrentHashMap<>();
    /** 本次雷劫总共几道（用于强度曲线）。 */
    private static final Map<UUID, Integer> TRIBULATION_TOTAL = new ConcurrentHashMap<>();
    /** 渡劫前的天气（结束后恢复）。 */
    private static final Map<UUID, Boolean> TRIBULATION_WAS_THUNDER =
            new ConcurrentHashMap<>();

    /**
     * 整场雷劫的目标总时长（tick）。
     *
     * <p>330 tick ≈ 16.5 秒。**不随境界线性变长** —— 否则大境界渡劫要站四五分钟，
     * 那不是"渡劫"，是挂机。道数多了就缩短单道周期。</p>
     */
    public static final int TRIBULATION_TOTAL_TICKS = 330;

    /** 单道周期上限（道数少时要够慢，otherwise 看起来像连发）。 */
    public static final int TRIBULATION_MAX_CYCLE = 36;

    /** 单道周期下限（道数多时也不能快过这个，否则又变成机枪）。 */
    public static final int TRIBULATION_MIN_CYCLE = 11;

    /**
     * 单道周期：道数越多越短，但始终在 [MIN, MAX] 内。
     *
     * <p>9 道 → 36 tick（1.8 秒一道，有充分的蓄势）；
     * 45 道 → 11 tick（0.55 秒一道，确实急，但那已经是"九九重劫"的尾声）。</p>
     */
    public static int tribulationCycle(int bolts) {
        if (bolts <= 0) {
            return TRIBULATION_MAX_CYCLE;
        }
        int ideal = TRIBULATION_TOTAL_TICKS / bolts;
        return Math.max(TRIBULATION_MIN_CYCLE,
                Math.min(TRIBULATION_MAX_CYCLE, ideal));
    }

    /** 波内每道间隔（tick）—— "连珠雷"里那几道几乎同时落下。 */
    /** 波与波之间的最短间隔（tick）—— 云层重新翻涌的时间。 */
    public static final int TRIBULATION_MIN_WAVE_GAP = 22;

    public static final int TRIBULATION_INTRA_WAVE = 4;

    /**
     * 一波里落几道雷（"连珠雷"）。
     *
     * <p>低境界一道一道来（每道都有充分蓄势）；高境界两三道连珠，
     * 然后云层重新翻涌 —— <b>"密集一段 + 停顿一段"</b>比均匀连发有张力得多。</p>
     */
    public static int tribulationWaveSize(int bolts) {
        if (bolts <= 18) {
            return 1;
        }
        return bolts <= 35 ? 2 : 3;
    }

    /** 第 {@code index} 道雷的**强度倍率**（已归一化）。
     *
     * <p>修仙小说的惯例：开头试雷偏轻，中段正劫，<b>最后一道最重</b>
     * （常常是"最后这一道才是真的"）。</p>
     *
     * <p>归一化保证<b>总伤害不变</b> —— 只改分布，不改总量，平衡不用重算。</p>
     */
    public static double tribulationBoltMultiplier(int index, int total) {
        if (total <= 0) {
            return 1.0D;
        }
        int i = Math.max(0, Math.min(total - 1, index));
        double t = total <= 1 ? 0.5D : (double) i / (total - 1);
        // 0.6 -> 1.4 线性，最后一道 ×1.7
        double w = 0.6D + 0.8D * t;
        if (i == total - 1) {
            w *= 1.7D;
        }
        // 归一化：让总和 = total（平均 1.0）
        double sum = 0.0D;
        for (int k = 0; k < total; k++) {
            double kt = total <= 1 ? 0.5D : (double) k / (total - 1);
            double kw = 0.6D + 0.8D * kt;
            if (k == total - 1) {
                kw *= 1.7D;
            }
            sum += kw;
        }
        return sum <= 0.0D ? 1.0D : w * total / sum;
    }

    /** 每道雷的伤害。 */
    private static final Map<UUID, Double> TRIBULATION_DAMAGE = new ConcurrentHashMap<>();

    /**
     * 由每 tick 驱动，把待劈的雷一道道落下来。
     *
     * <p>每 4 tick 劈一道；期间玩家可以吃药、可以跑，但不影响道数。</p>
     */
    /**
     * 推进雷劫：<b>蓄势 → 落雷 → 间隔</b>。
     *
     * <p>⚠ 原来是 {@code tickCount % 4} 一道 —— <b>每秒 5 道</b>，
     * 9 道在 1.8 秒里砸完，像机枪。玩家质疑"落雷频率是不是太快"，确实是。</p>
     *
     * <p>现在按修仙小说的写法重排：</p>
     * <ol>
     *   <li><b>蓄势</b>：头顶乌云翻涌 + 闷雷渐强（每 2 tick 一点点，不是一次性撒满）；</li>
     *   <li><b>落雷</b>：闪电 + 伤害，<b>一道比一道重</b>（最后一道 ×1.7）；</li>
     *   <li><b>间隔</b>：给玩家吞丹/运功的时间。</li>
     * </ol>
     */
    public static void tickTribulation(ServerPlayer player) {
        Integer left = TRIBULATION_PENDING.get(player.getUUID());
        if (left == null || left <= 0) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        Long nextAt = TRIBULATION_NEXT.get(player.getUUID());
        if (nextAt == null) {
            return;
        }
        int total = TRIBULATION_TOTAL.getOrDefault(player.getUUID(), left);
        int index = TRIBULATION_INDEX.getOrDefault(player.getUUID(), 0);

        // ---- 蓄势阶段：还没到落雷时刻 ----
        if (now < nextAt) {
            long toStrike = nextAt - now;
            // 连珠雷之间的 3~4 tick 不刷蓄势（否则云层会闪一下又没）
            if (toStrike > TRIBULATION_INTRA_WAVE + 1L) {
                windupFx(level, player, index, total, toStrike);
            }
            return;
        }

        // ---- 落雷 ----
        // 通知客户端"这一道落在哪"（焦痕贴花 + 闪光）
        try {
            var strike = new com.hongmeng.xiuxian.network.TribulationStrikePacket(
                    player.getX(), player.getY(), player.getZ(), TRIBULATION_TIER.getOrDefault(player.getUUID(), 6));
            com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(player, strike);
        } catch (Throwable ignored) {
            // 落雷包失败不影响判定
        }
        int cycle = tribulationCycle(total);
        double base = TRIBULATION_DAMAGE.getOrDefault(player.getUUID(), 1.0D);
        double dmg = base * tribulationBoltMultiplier(index, total);

        strikeFx(level, player, index, total);

        // 伤害源用 magic 而不是 lightningBolt：
        // 原版 LIGHTNING_BOLT 带 BYPASSES_ARMOR，护甲完全不生效 ——
        // 那正是玩家反馈"根本扛不住直接死"的直接原因之一。
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().magic(), (float) dmg);

        int nextIndex = index + 1;
        int remain = total - nextIndex;
        if (remain <= 0) {
            // ---- 渡劫结束 ----
            TRIBULATION_PENDING.remove(player.getUUID());
            TRIBULATION_DAMAGE.remove(player.getUUID());
            TRIBULATION_NEXT.remove(player.getUUID());
            TRIBULATION_INDEX.remove(player.getUUID());
            TRIBULATION_TOTAL.remove(player.getUUID());
            endWeather(level, player);
            try {
                com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(player,
                        new com.hongmeng.xiuxian.network.TribulationFxPacket(
                                false, 6, 0));
            } catch (Throwable ignored) {
                // 同上
            }
            player.displayClientMessage(Component.literal(
                    "\u00a7a\u00a7l雷劫已过。\u00a77云开雾散，天地重归清明。"), false);
            return;
        }
        // 下一道：波内就快速连珠，波与波之间留出蓄势
        int waveSize = tribulationWaveSize(total);
        boolean intraWave = waveSize > 1 && (nextIndex % waveSize) != 0;
        // 波间**固定至少 22 tick（1.1 秒）** —— 云层要时间重新翻涌，
        // 否则三连雷之间只隔 0.5 秒，蓄势表现根本看不清。
        long delay = intraWave
                ? TRIBULATION_INTRA_WAVE
                : Math.max(TRIBULATION_MIN_WAVE_GAP, cycle - 1);
        TRIBULATION_INDEX.put(player.getUUID(), nextIndex);
        TRIBULATION_PENDING.put(player.getUUID(), remain);
        TRIBULATION_NEXT.put(player.getUUID(), now + delay);
        player.displayClientMessage(Component.literal(
                "\u00a7d雷劫 \u00a7f剩余 " + remain + " \u00a77道"), true);
        // 最后一道前给一次预警（小说里也是"最后这一道最重"）
        if (remain == 1) {
            player.displayClientMessage(Component.literal(
                    "\u00a74\u00a7l最后一道 —— \u00a7c此雷最重，稳住心神。"), false);
        }
    }

    /** 蓄势：乌云翻涌 + 闷雷渐强（越接近落雷越密）。 */
    private static void windupFx(ServerLevel level, ServerPlayer player, int index,
                                 int total, long ticksToStrike) {
        int cycle = tribulationCycle(total);
        // 越接近落雷越密：剩余 <= 1/3 周期时每 2 tick，否则每 5 tick
        long step = ticksToStrike * 3L <= cycle ? 2L : 5L;
        if (level.getGameTime() % step != 0L) {
            return;
        }
        double y = player.getY() + 3.2D;
        // 乌云：头顶一圈慢慢收拢的暗色粒子
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX(), y, player.getZ(), 6, 1.6D, 0.25D, 1.6D, 0.01D);
        // 雷光在云里翻
        if (ticksToStrike * 3L <= cycle) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), y + 0.4D, player.getZ(), 8, 1.2D, 0.2D, 1.2D, 0.05D);
            // 闷雷（音量随逼近而升）
            if (ticksToStrike % 6L == 0L) {
                // 蓄势：低频上升音（越接近落雷越紧）
                // 蓄势音也跟着境界走（低境界更"尖"，高境界更"闷"）
                int wTier = TRIBULATION_TIER.getOrDefault(player.getUUID(), 6);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        com.hongmeng.xiuxian.ModSounds.TRIB_CHARGE.get(),
                        SoundSource.WEATHER,
                        0.30F + 0.10F * (wTier - 6),
                        boltPitch(wTier) * 1.15F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.WEATHER, 0.35F,
                        0.55F + (float) (index % 3) * 0.1F);
            }
        }
    }

    /** 落雷那一下：闪 + 雷劈 + 电火花（最后一道更夸张）。 */
    private static void strikeFx(ServerLevel level, ServerPlayer player, int index,
                                 int total) {
        boolean last = index >= total - 1;
        level.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1.0D, player.getZ(), 1,
                0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1.2D, player.getZ(),
                last ? 60 : 26, 0.55D, 0.9D, 0.55D, last ? 0.55D : 0.35D);
        // 音效按境界变：越高越沉越响（青 -> 蓝 -> 紫 -> 血）
        int tier = TRIBULATION_TIER.getOrDefault(player.getUUID(), 6);
        float vol = boltVolume(tier) * (last ? 1.25F : 1.0F);
        float pitch = boltPitch(tier) * (last ? 0.92F : 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.hongmeng.xiuxian.ModSounds.TRIB_BOLT.get(),
                SoundSource.WEATHER, vol, pitch);
        // 原版闪电实体：只有视觉与音效，伤害仍由上面统一结算
        spawnVisualBolt(level, player);
    }

    /** 这个玩家现在是不是在渡劫（给元婴遁走判定用）。 */
    public static boolean inTribulation(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) {
            return false;
        }
        Integer left = TRIBULATION_PENDING.get(player.getUUID());
        return left != null && left > 0;
    }

    /**
     * 中止雷劫（元婴遁走：肉身崩碎但保住性命）。
     *
     * <p>只清状态、不再落雷；<b>不撤销境界</b> ——
     * 撤销境界会让玩家倒退一个大境，惩罚过重，而且会引出
     * 属性/天赋要不要一起回滚的一堆问题。</p>
     */
    public static void abandonTribulation(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) {
            return;
        }
        java.util.UUID id = player.getUUID();
        TRIBULATION_PENDING.remove(id);
        TRIBULATION_DAMAGE.remove(id);
        TRIBULATION_NEXT.remove(id);
        TRIBULATION_INDEX.remove(id);
        TRIBULATION_TOTAL.remove(id);
        TRIBULATION_TIER.remove(id);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            endWeather(level, player);
            try {
                com.hongmeng.xiuxian.network.ModNetwork.sendToPlayer(player,
                        new com.hongmeng.xiuxian.network.TribulationFxPacket(false, 6, 0));
            } catch (Throwable ignored) {
                // 同上
            }
        }
    }

    /**
     * 该境界雷声的<b>音高</b>。
     *
     * <p>境界越高音越低 —— 能量越大，低频成分越多（物理直觉）。
     * 与视觉的"青 -> 蓝 -> 紫 -> 血"一一对应。</p>
     */
    public static float boltPitch(int tier) {
        if (tier >= 10) {
            return 0.75F;
        }
        if (tier >= 9) {
            return 0.88F;
        }
        if (tier >= 8) {
            return 1.00F;
        }
        return 1.15F;
    }

    /** 该境界雷声的<b>音量</b>（越高越响、越沉）。 */
    public static float boltVolume(int tier) {
        if (tier >= 10) {
            return 1.50F;
        }
        if (tier >= 9) {
            return 1.25F;
        }
        if (tier >= 8) {
            return 1.10F;
        }
        return 0.90F;
    }

    /** 恢复渡劫前的天气。 */
    private static void endWeather(ServerLevel level, ServerPlayer player) {
        try {
            Boolean wasThunder = TRIBULATION_WAS_THUNDER.remove(player.getUUID());
            if (wasThunder != null && !wasThunder) {
                // 原来是晴天：雨停、雷停
                level.setWeatherParameters(6000, 0, false, false);
            }
        } catch (Throwable ignored) {
            // 恢复失败不影响游戏
        }
    }


    /** 剩下多少道雷没劈。 */
    public static int pendingBolts(UUID uuid) {
        Integer n = TRIBULATION_PENDING.get(uuid);
        return n == null ? 0 : n;
    }

    /**
     * 召唤一道**纯视觉**的原版闪电。
     *
     * <p>{@code setVisualOnly(true)} 是关键——它让闪电只有光效与雷声、
     * 不造成伤害也不点燃方块。雷劫的伤害由 {@link #tickTribulation} 用
     * {@code magic()} 伤害源按生命上限结算，两者分开才能保证数值可控。</p>
     *
     * <p>落点在玩家周围 1.5 格内随机偏移，这样多道雷看起来是"围着你劈"，
     * 而不是每次都精确劈在同一点。</p>
     */
    private static void spawnVisualBolt(ServerLevel level, ServerPlayer player) {
        try {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 1.0D + level.random.nextDouble() * 1.5D;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) {
                return;
            }
            bolt.moveTo(x, player.getY(), z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        } catch (Throwable t) {
            // 万一版本差异导致 API 不可用，也不能让渡劫中断
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 召唤视觉闪电失败：{}", t.toString());
        }
    }

    public static void clearTribulation(UUID uuid) {
        TRIBULATION_PENDING.remove(uuid);
        TRIBULATION_DAMAGE.remove(uuid);
    }
}
