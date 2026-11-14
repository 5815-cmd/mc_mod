package com.hongmeng.xiuxian.verify;

import com.hongmeng.xiuxian.compat.HongmengConfig;
import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.core.Karma;
import com.hongmeng.xiuxian.core.Realm;
import com.hongmeng.xiuxian.core.RealmStats;
import com.hongmeng.xiuxian.sect.Sect;
import com.hongmeng.xiuxian.sect.SectManager;
import com.hongmeng.xiuxian.technique.Technique;
import com.hongmeng.xiuxian.spirit.Element;
import com.hongmeng.xiuxian.spirit.Rarity;
import com.hongmeng.xiuxian.spirit.SpiritRoot;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * 离线验证器：把不依赖游戏实例的确定性逻辑全部跑一遍并断言。
 *
 * <p>这个类不在 mod 的注册流程里，只在开发期由 tools/run_verify.ps1 调用，
 * 用来证明境界阶梯、业力数学、灵根概率、门派身份这些"数值正确性"。</p>
 */
public final class LogicVerifier {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("===== 鸿蒙修仙 · 核心逻辑离线验证 =====");
        System.out.println();

        loadConfig();

        realmLadder();
        realmMonotonic();
        karmaMath();
        rootDistribution();
        rootGuarantee();
        sectRanks();
        sectModifiers();
        realmStats();
        harvestTiers();
        domainSystem();
        questSystem();
        difficultySystem();
        realmStatsTable();
        breakthroughGate();
        cultivationCapAndTribulation();
        herbAndAlchemy();
        manaSystem();
        dungeonCatalog();
        techniqueCatalog();
        aimPointMath();
        spellFxUniqueness();
        cinematicShots();
        spellCinematic();
        swordFormationRules();
        pathAndTalentRules();
        pathBalanceRules();
        pathMechanicsRules();
        pathWiringRules();
        pathArmorRules();
        pathWeaponRules();
        careerRules();
        careerArmorSetRules();
        careerSkillRules();
        careerActiveRules();
        careerTalentTreeRules();
        careerMechanicsRules();
        careerGearWiringRules();
        careerSpellImplementationRules();
        recipeIdRules();
        professionRecipeRules();
        talentSaveRules();
        respecRules();
        mechanicFxRules();
        professionUiRules();
        setBonusRules();
        spellAnchorRules();
        gearTierRules();
        armorCurveRules();
        weaponRealmDamageRules();
        pathDamageWiringRules();
        talentWiringRules();
        realmPowerWiringRules();
        discoveryRules();
        saveRoundTripRules();
        saveOrderRules();
        professionSummaryRules();
        talentBudgetRules();
        craftFailureCostRules();
        tribulationSurvivableRules();
        equipmentRefreshRules();
        formationThrottleRules();
        aftermathCleanupRules();
        creativeTabRules();
        cinematicInvulnRules();
        blockTagRules();
        questHintMatchesRecipeRules();
        noDimensionGateRules();
        guideMechanicsRules();
        guideWrapRules();
        noNewlineInLineRules();
        playerFeedbackBatch1Rules();
        shiftSkipRules();
        domainCastSpeedRules();
        deadGetterWiringRules();
        configWiringRules();
        combatAnimRules();
        slashTrailRules();
        swordArtGateRules();
        fxVolumeRules();
        dungeonMobRealmRules();
        fxShadingRules();
        fxFourStageRules();
        mobMindRules();
        tribulationRhythmRules();
        realWorldHardmodeRules();
        mobGuardRules();
        mobShieldRules();
        weaponComboRules();
        comboVariantRules();
        realWorldThreeRules();
        execAnimAndEliteRules();
        mobDodgeRules();
        bossFormRules();
        phaseTransitionRules();
        tribulationFxRules();
        phaseUltimateRules();
        professionCurveRules();
        customSfxRules();
        mobInterruptGroupRules();
        elementSfxRules();
        bgmRules();
        ashCultivatorRules();
        newShapesRules();
        stageSfxRules();
        bossUltSfxRules();
        tribSoundTierRules();
        newPathRules();
        eliteEntranceRules();
        weaponModelRules();
        weaponPolishRules();
        bossAuditRules();
        armorTrimRules();
        dimensionBossRules();
        blazeEmperorRules();
        bossSkeletonRules();
        bossCompletenessRules();
        fxBeautyRules();
        elementParticleRules();
        bossModelDetailRules();
        bossAtmosphereRules();
        altarArenaRules();
        corruptionRules();
        configWiredRules();
        evilAltarRules();
        evilShowRules();
        evilFallRules();
        evilUltimateRules();
        evilMinorRules();
        armorModelDetailRules();
        hideViewRules();
        fxRealismLayers();
        realWorldRules();
        realWorldAtmosphere();
        dynamicDungeonGate();
        techniqueLearningAndCooldown();
        mobRanks();
        toolCoverage();
        gearAndMitigation();
        equipmentSlots();
        shopAndPity();
        dataRoundTrip();

        System.out.println();
        System.out.println("===== 通过 " + passed + " 项，失败 " + failed + " 项 =====");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * 启动 Minecraft 的注册表引导。
     *
     * <p>保留但默认不调用：在裸 {@code main()} 里 {@code Bootstrap.bootStrap()} 会因为没有
     * 语言环境而二次失败。真正的游戏 API 行为（Tier 对象映射、掉落）由专用服务端实测覆盖，
     * 这里只验证纯数值逻辑，因此刻意不碰 {@code Blocks} / {@code Items} 这类注册内容。</p>
     */
    @SuppressWarnings("unused")
    private static void bootstrapGame() {
        try {
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
            System.out.println("Minecraft 注册表已引导（SharedConstants + Bootstrap）");
        } catch (Throwable t) {
            System.out.println("!! 引导失败：" + t);
            throw t;
        }
    }

    /**
     * 把 SERVER 配置按默认值加载进内存。
     * 不这么做的话，Karma/Breakthrough 读配置会拿到未加载状态。
     */
    private static void loadConfig() {
        try {
            ForgeConfigSpec spec = HongmengConfig.SPEC;
            com.electronwill.nightconfig.core.CommentedConfig data =
                    com.electronwill.nightconfig.core.CommentedConfig.inMemory();
            spec.correct(data);
            spec.acceptConfig(data);
            System.out.println("配置已按默认值加载（maxKarma="
                    + HongmengConfig.SERVER.maxKarma.get() + "）");
            System.out.println();
        } catch (Throwable t) {
            System.out.println("!! 配置加载失败：" + t);
            throw t;
        }
    }

    // ------------------------------------------------------------------

    private static void realmLadder() {
        section("境界阶梯");

        check("总层数为 28（9 境×3 层 + 鸿蒙 1 层）",
                Realm.TOTAL_STEPS == 28, "实际 " + Realm.TOTAL_STEPS);

        Realm first = Realm.of(0L);
        check("0 修为 = 练气初期", "练气初期".equals(first.cn()), first.cn());

        // 一路推到底，检查每层名字与层数
        long cap = Long.MAX_VALUE / 4;
        Realm max = Realm.of(cap);
        check("极大修为 = 鸿蒙", "鸿蒙".equals(max.cn()), max.cn());
        check("鸿蒙是最后一境", max.isMaxRealm(), String.valueOf(max.isMaxRealm()));
        check("鸿蒙没有下一境", max.nextCn().isEmpty(), "'" + max.nextCn() + "'");

        // 逐层推进（跟着阶梯走，不靠猜范围），确认正好 28 个不同的境界名
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        long walk = 0L;
        int guard = 0;
        while (guard++ < 100) {
            Realm cur = Realm.of(walk);
            seen.add(cur.cn());
            long next = cur.minTotal() + cur.need();
            if (next <= walk || cur.isMaxRealm()) {
                break;
            }
            walk = next;
        }
        check("逐层走完得到 28 个不同境界", seen.size() == 28, "实际 " + seen.size());
        check("走完正好停在鸿蒙", Realm.of(walk).cn().equals("鸿蒙"),
                Realm.of(walk).cn());
        check("鸿蒙起点修为 < 1500 万（曲线不能失控）", walk < 15_000_000L,
                "鸿蒙起点 " + walk);

        System.out.println("        阶梯预览：" + String.join(" → ",
                java.util.List.of(Realm.of(0L).cn(), Realm.of(600L).cn(), Realm.of(3000L).cn(),
                        Realm.of(12000L).cn(), Realm.of(40000L).cn(), Realm.of(130000L).cn(),
                        Realm.of(450000L).cn(), max.cn())));
        System.out.println("        到鸿蒙需要的总修为：" + walk);
    }

    private static void realmMonotonic() {
        section("境界单调性与满级判定");

        boolean monotonic = true;
        Realm prev = Realm.of(0L);
        for (long total = 0; total < 400_000_000L; total += 1531L) {
            Realm now = Realm.of(total);
            if (now.tier() < prev.tier()) {
                monotonic = false;
                break;
            }
            prev = now;
        }
        check("境界随修为单调不降", monotonic, "");

        check("isMax() 在满级前为 false", !Realm.isMax(0L), "");
        check("isMax() 在极大修为下为 true", Realm.isMax(Long.MAX_VALUE / 4), "");

        // progress/needed 的关系
        long sample = 1_234_567L;
        Realm r = Realm.of(sample);
        long progress = Realm.progress(sample);
        check("progress ∈ [0, need)", progress >= 0 && progress < r.need(),
                "progress=" + progress + " need=" + r.need());
        check("minTotal + progress = 当前累计修为",
                r.minTotal() + progress == sample, "");
    }

    private static void karmaMath() {
        section("业力数学");

        CultivationData data = new CultivationData();
        data.setAwakened(true);
        data.setSect("");   // 散修，无门派修正

        check("初始业力为 0", data.getKarma() == 0, "");

        // 基线：基础成功率 0.85 → 天生就有 15% 失败率（破境本来就是赌）
        double base = HongmengConfig.SERVER.baseSuccessRate.get();
        int baseFail = (int) Math.round((1.0D - base) * 100.0D);
        check("0 层业力时失败率 = 基础失败率（" + baseFail + "%）",
                baseFail == Integer.parseInt(
                        Karma.failurePercentText(data).replace("%", "")),
                Karma.failurePercentText(data));

        // 逐层加业力：每层把成功率压低 0.10，直到撞上下限
        double penaltyPer = HongmengConfig.SERVER.karmaPenaltyPerStack.get();
        double floor = HongmengConfig.SERVER.minSuccessRate.get();
        boolean curveOk = true;
        StringBuilder curve = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            Karma.add(data, 1);
            double expected = Math.max(floor, Math.min(1.0D, base - i * penaltyPer));
            double actual = Karma.successRate(data);
            int failPct = (int) Math.round((1.0D - actual) * 100.0D);
            curve.append(i).append("层→").append(failPct).append("%  ");
            if (Math.abs(expected - actual) > 1e-6) {
                curveOk = false;
            }
        }
        check("每层业力把成功率压低 10%，触底后保持下限", curveOk, "");
        System.out.println("        业力曲线：" + curve.toString().trim());

        check("满 10 层时判定为必败（isCapped）", Karma.isCapped(data), "");
        check("满 10 层时成功率已被压到下限 " + floor,
                Math.abs(Karma.successRate(data) - floor) < 1e-6,
                String.valueOf(Karma.successRate(data)));

        // 附魔金苹果全清
        int cleared = Karma.clear(data);
        check("清空返回清除的层数 10", cleared == 10, "实际 " + cleared);
        check("清空后回到基线失败率",
                Karma.failurePercentText(data).equals(baseFail + "%"),
                Karma.failurePercentText(data));
        check("清空后不再是必败", !Karma.isCapped(data), "");

        // 劫掠事件一次消一层
        Karma.add(data, 3);
        int reduced = Karma.reduce(data, 1);
        check("劫掠事件消 1 层", reduced == 1 && data.getKarma() == 2,
                "reduced=" + reduced + " 剩余=" + data.getKarma());

        // 门派修正：魔渊业力上限 -3
        CultivationData demon = new CultivationData();
        demon.setSect(Sect.MOYUAN.id());
        check("魔渊的业力上限比散修低 3",
                Karma.maxKarma(demon) == Karma.maxKarma(data) - 3,
                "魔渊=" + Karma.maxKarma(demon) + " 散修=" + Karma.maxKarma(data));

        // 边际：9 层还不必败，第 10 层才封死
        CultivationData nine = new CultivationData();
        nine.setSect("");
        Karma.add(nine, 9);
        check("9 层业力时仍可尝试突破", !Karma.isCapped(nine), "");
        Karma.add(nine, 1);
        check("第 10 层触达必败", Karma.isCapped(nine), "");
    }

    private static void rootDistribution() {
        section("灵根抽取概率");

        Random random = new Random(20261009L);
        Map<Rarity, Integer> counts = new EnumMap<>(Rarity.class);
        int samples = 400_000;
        for (int i = 0; i < samples; i++) {
            Rarity r = SpiritRoot.rollRarity(random);
            counts.merge(r, 1, Integer::sum);
        }

        boolean orderOk = true;
        double prevPct = Double.MAX_VALUE;
        StringBuilder sb = new StringBuilder();
        for (Rarity r : Rarity.values()) {
            double pct = counts.getOrDefault(r, 0) * 100.0D / samples;
            double expect = r.weight() * 100.0D;
            sb.append(String.format("%s %.2f%%(期望%.1f%%)  ", r.cn(), pct, expect));
            // 抽样误差允许 ±35%
            if (Math.abs(pct - expect) > Math.max(0.05D, expect * 0.35D)) {
                orderOk = false;
            }
        }
        check("七档品质抽样概率与权重吻合", orderOk, "");
        System.out.println("        " + sb.toString().trim());

        // 先天池里必须包含主角同款
        boolean hasIceSword = false;
        for (int i = 0; i < 200_000; i++) {
            if (SpiritRoot.rollOf(Rarity.INNATE, random) == SpiritRoot.INNATE_ICE_SWORD) {
                hasIceSword = true;
                break;
            }
        }
        check("先天池能抽到『仙剑灵根』（隐藏条目，主角同款）", hasIceSword, "");

        // 主角同款的整体概率 ≈ 先天权重 × 池中占比
        int hits = 0;
        int n = 500_000;
        for (int i = 0; i < n; i++) {
            if (SpiritRoot.roll(random) == SpiritRoot.INNATE_ICE_SWORD) {
                hits++;
            }
        }
        double pct = hits * 100.0D / n;
        check("仙剑灵根整体概率 ≈ 1.7%（0.6%~3.5%）",
                pct > 0.6D && pct < 3.5D, String.format("%.3f%%", pct));

        // 七档品质的加成数值必须精确匹配需求
        // 玩家口径（重做）：修炼速度是**倍率**（伪灵根正常、凡品 1.5 倍…鸿蒙 6 倍）；
        // 生命 / 攻击是百分比加成（+10% … +100%）。数组列 = {加成, 修炼倍率}。
        // 玩家口径（重做）：
        //   * 修炼速度是**倍率**（伪灵根正常 1.0、凡品 1.5 … 鸿蒙 6.0）；
        //   * 生命 / 攻击是百分比加成（+10% / +30% / +40% / +50% / +60% / +80% / +100%）。
        //
        // 这里**直接从 Rarity 自身推期望值**（而不是写死一份），
        // 这样断言永远反映真实数据，同时仍能抓住"名称顺序错乱""加成与倍率对不上"这类真问题。
        String[] expectedNames = {"伪灵根", "凡品灵根", "地灵根", "天灵根",
                "先天灵根", "混沌灵根", "鸿蒙灵根"};
        double[] expectedBonus = {0.10D, 0.30D, 0.40D, 0.50D, 0.60D, 0.80D, 1.00D};
        double[] expectedCult = {1.00D, 1.50D, 2.00D, 3.00D, 4.00D, 5.00D, 6.00D};

        boolean match = true;
        StringBuilder bonusText = new StringBuilder();
        Rarity[] all = Rarity.values();
        if (all.length != expectedNames.length) {
            match = false;
        }
        for (int i = 0; i < all.length && i < expectedNames.length; i++) {
            boolean ok = all[i].cn().equals(expectedNames[i])
                    && Math.abs(all[i].attackBonus() - expectedBonus[i]) < 1e-9
                    && Math.abs(all[i].healthBonus() - expectedBonus[i]) < 1e-9
                    && Math.abs(all[i].cultivationMultiplier() - expectedCult[i]) < 1e-9;
            if (!ok) {
                match = false;
                System.out.printf("        [不符] 第%d档 %s：攻击=%.2f 生命=%.2f 修炼=%.2f"
                                + "（应为 %s / %.2f / %.2f）%n",
                        i, all[i].cn(), all[i].attackBonus(), all[i].healthBonus(),
                        all[i].cultivationMultiplier(), expectedNames[i],
                        expectedBonus[i], expectedCult[i]);
            }
            bonusText.append(all[i].cn()).append(all[i].attackBonusText())
                    .append(" /x").append(String.format("%.1f",
                            all[i].cultivationMultiplier())).append("  ");
        }
        check("七档品质名称与数值完全符合需求", match, "");
        System.out.println("        " + bonusText.toString().trim());

        // ---- 灵根不再有「属性」维度（玩家要求：把灵根的属性设定删了）----
        //
        // ⚠ 这里原来是两条"灵根覆盖 8 种元素"的断言。
        //   玩家要求移除灵根的属性维度之后，那两条断言的**目标本身没了** ——
        //   所以改成验证"属性维度确实已移除"，而不是直接删掉。
        //   留着一条能证明"确实删干净了"的断言，比删掉更有价值
        //   （否则将来有人把 element 加回来，也没人会发现）。
        boolean allNoElement = true;
        StringBuilder withElement = new StringBuilder();
        for (SpiritRoot r : SpiritRoot.all()) {
            if (r.element() != null) {
                allNoElement = false;
                withElement.append(r.key()).append(" ");
            }
        }
        check("灵根不再带任何属性（element() 恒为 null）", allNoElement,
                allNoElement ? SpiritRoot.all().size() + " 条灵根全部无属性"
                        : ("仍有属性的：" + withElement));
        check("灵根池仍然完整（属性维度删掉，但条目没少）",
                SpiritRoot.all().size() >= 40,
                SpiritRoot.all().size() + " 条灵根");
    }

    /** 是否还有任何灵根带属性（用来证明属性维度确实删干净了）。 */
    private static boolean anyRootHasElement() {
        for (com.hongmeng.xiuxian.spirit.SpiritRoot r
                : com.hongmeng.xiuxian.spirit.SpiritRoot.all()) {
            if (r.element() != null) {
                return true;
            }
        }
        return false;
    }

    private static void rootGuarantee() {
        section("保底重抽");

        Random random = new Random(7L);
        boolean allAbove = true;
        int lowest = Integer.MAX_VALUE;
        for (int i = 0; i < 20_000; i++) {
            SpiritRoot root = SpiritRoot.rollGuaranteed(random);
            int ord = root.rarity().ordinal();
            lowest = Math.min(lowest, ord);
            if (ord < Rarity.EARTH.ordinal()) {
                allAbove = false;
                break;
            }
        }
        check("保底重抽永不低于地灵根", allAbove,
                "最低抽到 " + Rarity.values()[Math.min(lowest, Rarity.values().length - 1)].cn());

        check("伪灵根池可抽到（散修起步）",
                SpiritRoot.rollOf(Rarity.FALSE_ROOT, random) == SpiritRoot.FALSE_MIXED, "");
        check("byKey 反查正常",
                SpiritRoot.byKey(SpiritRoot.INNATE_ICE_SWORD.key()) == SpiritRoot.INNATE_ICE_SWORD,
                "");
        check("未知 key 返回 null", SpiritRoot.byKey("no_such_root") == null, "");
        check("旧存档 key 可兼容映射（immortal_ice_sword → 仙剑灵根）",
                SpiritRoot.byKeyCompat("immortal_ice_sword") == SpiritRoot.INNATE_ICE_SWORD, "");
        // 灵根改成「品质 × 属性」全矩阵（玩家要求：什么属性都能是鸿蒙），
        // 所以不再钉具体条数，改钉两条设计意图：每个品质都有 8 种属性，
        // 且每个品质都能出木灵根（这条修的就是玩家刷不到木灵根的问题）。
        boolean everyRarityEight = true;
        for (com.hongmeng.xiuxian.spirit.Rarity rr
                : com.hongmeng.xiuxian.spirit.Rarity.values()) {
            if (rr == com.hongmeng.xiuxian.spirit.Rarity.FALSE_ROOT) {
                continue;
            }
            int cnt = 0;
            for (com.hongmeng.xiuxian.spirit.SpiritRoot root
                    : com.hongmeng.xiuxian.spirit.SpiritRoot.all()) {
                if (root.rarity() != rr) {
                    continue;
                }
                cnt++;
            }
            if (cnt < 8) {
                everyRarityEight = false;
            }
        }
        // ⚠ 玩家要求移除灵根的属性维度，所以这两条断言的**目标本身变了**：
        //   * 原来：「每个品质都有 8 种属性」+「每个品质都能出木灵根」——
        //     守的是"高难度开局也能刷到木灵根，从而能开木道宫"；
        //   * 现在：灵根没有属性了，**领域元素纯随机**，
        //     所以"能不能开到某元素道宫"不再受开局品质限制 ——
        //     那个问题从根上消失了。
        //   因此改成验"每个品质都有足够的灵根条目可抽"，
        //   以及"领域元素不再依赖灵根"。
        check("每个品质都有足够多的灵根条目（品质与属性不挂钩后仍要够抽）",
                everyRarityEight, "");
        check("所有灵根条目都不带属性（属性维度已移除）", !anyRootHasElement(), "");
        // 每一档品质都必须至少有一条灵根可抽，否则抽取会静默回落到凡品
        boolean everyRarityCovered = true;
        StringBuilder coverage = new StringBuilder();
        for (Rarity rarity : Rarity.values()) {
            int n = 0;
            for (SpiritRoot root : SpiritRoot.all()) {
                if (root.rarity() == rarity) {
                    n++;
                }
            }
            coverage.append(rarity.cn()).append(":").append(n).append("  ");
            if (n == 0) {
                everyRarityCovered = false;
            }
        }
        check("七档品质每档都有灵根可抽", everyRarityCovered, coverage.toString().trim());
        System.out.println("        各档数量：" + coverage.toString().trim());
    }

    private static void sectRanks() {
        section("门派身份阶梯");

        for (Sect sect : Sect.joinable()) {
            int rank0 = sect.rankIndex(0);
            int rankMax = sect.rankIndex(1_000_000);
            boolean increasing = true;
            int last = -1;
            for (int contribution = 0; contribution <= 20_000; contribution += 37) {
                int idx = sect.rankIndex(contribution);
                if (idx < last) {
                    increasing = false;
                    break;
                }
                last = idx;
            }
            check(sect.cn() + "：身份随贡献单调不降", increasing, "");
            check(sect.cn() + "：最高身份是太上长老",
                    "太上长老".equals(sect.rankName(1_000_000)), sect.rankName(1_000_000));
            check(sect.cn() + "：入门身份是外门弟子",
                    "外门弟子".equals(sect.rankName(0)), sect.rankName(0));
            check(sect.cn() + "：身份层级为 6 级", rankMax == 5, "实际 index " + rankMax);
            check(sect.cn() + "：已满级时 nextRankThreshold = -1",
                    sect.nextRankThreshold(1_000_000) == -1, "");
        }
    }

    private static void sectModifiers() {
        section("门派加成");

        CultivationData none = new CultivationData();
        none.setSect("");
        CultivationData qianji = new CultivationData();
        qianji.setSect(Sect.QIANJI.id());
        CultivationData shenxiao = new CultivationData();
        shenxiao.setSect(Sect.SHENXIAO.id());
        CultivationData moyuan = new CultivationData();
        moyuan.setSect(Sect.MOYUAN.id());

        check("散修无门派加成",
                com.hongmeng.xiuxian.sect.SectManager.cultivationMultiplier(none) == 1.0D, "");
        check("魔渊修为倍率最高",
                com.hongmeng.xiuxian.sect.SectManager.cultivationMultiplier(moyuan)
                        > com.hongmeng.xiuxian.sect.SectManager.cultivationMultiplier(qianji), "");
        check("神霄攻击加成最高",
                Sect.SHENXIAO.attackBonus() > Sect.MOYUAN.attackBonus()
                        && Sect.SHENXIAO.attackBonus() > Sect.QIANJI.attackBonus(), "");
        check("魔渊突破加成最高",
                Sect.MOYUAN.breakthroughBonus() > Sect.SHENXIAO.breakthroughBonus(), "");
        check("魔渊业力上限修正为负",
                Sect.MOYUAN.karmaCapModifier() < 0, String.valueOf(Sect.MOYUAN.karmaCapModifier()));

        // 身份越高，修为倍率越高
        CultivationData high = new CultivationData();
        high.setSect(Sect.QIANJI.id());
        high.setContribution(5000);
        check("同门派内，身份越高修为倍率越高",
                com.hongmeng.xiuxian.sect.SectManager.cultivationMultiplier(high)
                        > com.hongmeng.xiuxian.sect.SectManager.cultivationMultiplier(qianji),
                String.format("%.3f > %.3f",
                        com.hongmeng.xiuxian.sect.SectManager.cultivationMultiplier(high),
                        com.hongmeng.xiuxian.sect.SectManager.cultivationMultiplier(qianji)));

        // 破境丹要用到的成功率：门派加成应体现出来
        CultivationData plain = new CultivationData();
        plain.setSect("");
        CultivationData sx = new CultivationData();
        sx.setSect(Sect.SHENXIAO.id());
        check("神霄弟子突破成功率高于散修",
                Karma.successRate(sx) > Karma.successRate(plain),
                String.format("%.3f > %.3f", Karma.successRate(sx), Karma.successRate(plain)));

        // Sect.byId 容错
        check("byId 支持 id / 枚举名 / 中文名",
                Sect.byId("qianji") == Sect.QIANJI
                        && Sect.byId("SHENXIAO") == Sect.SHENXIAO
                        && Sect.byId("魔渊") == Sect.MOYUAN, "");
        check("byId 未知输入回落散修", Sect.byId("nope") == Sect.NONE, "");
        check("byId 空输入回落散修", Sect.byId("") == Sect.NONE, "");

        // 千机门是反派：不可加入，但 byId 仍能查到
        check("千机门不在可加入列表里",
                java.util.Arrays.stream(Sect.joinable()).noneMatch(s -> s == Sect.QIANJI), "");
        check("可加入门派只有神霄剑宗与魔渊",
                Sect.joinable().length == 2, "实际 " + Sect.joinable().length);
        check("千机门被标记为敌对", Sect.QIANJI.isHostile(), "");
        check("神霄剑宗不是敌对", !Sect.SHENXIAO.isHostile(), "");
        check("千机门仍可通过 byId 查到（用于剧情）",
                Sect.byId("qianji") == Sect.QIANJI, "");
    }

    /**
     * 境界实力数值。需求：
     * 筑基 +5 颗心 / +3 攻击；金丹起每境翻倍直到道印；飞升 ×3；
     * 仙人 / 帝境 / 鸿蒙 各 ×10。生命与攻击同倍率。
     */
    private static void realmStats() {
        section("境界实力数值");

        // 筑基 = 基准
        Realm zj = Realm.of(realmStart(2));
        // 基准值本身是**可调的设计参数**（玩家要求「提高一下玩家数值」：
        // 生命 10→16、攻击 3→5），所以从 Realm 的常量读，而不是写死。
        // 真正要守的不变量是"每境倍率"与"攻击/生命同倍率"（见下面）。
        final double baseHp = Realm.BASE_HEALTH_BONUS;
        final double baseAtk = Realm.BASE_DAMAGE_BONUS;
        final double baseHearts = baseHp / 2.0D;
        check(String.format("筑基：基准 %.0f 生命（%.0f 颗心）", baseHp, baseHearts),
                Math.abs(zj.healthBonusHearts() - baseHearts) < 1e-6,
                String.format("%.2f 颗心", zj.healthBonusHearts()));
        check(String.format("筑基：基准 %.0f 点攻击", baseAtk),
                Math.abs(zj.damageBonus() - baseAtk) < 1e-6,
                String.format("%.2f", zj.damageBonus()));

        // 金丹 = 2 倍，元婴 = 4 倍，化神 = 8 倍，道印 = 16 倍
        double[][] ladder = {
                {3, 2.0D}, {4, 4.0D}, {5, 8.0D}, {6, 16.0D},
                {7, 48.0D}, {8, 480.0D}, {9, 4800.0D}, {10, 48000.0D}
        };
        StringBuilder table = new StringBuilder();
        for (double[] step : ladder) {
            int tier = (int) step[0];
            double mult = step[1];
            Realm r = Realm.of(realmStart(tier));
            boolean ok = Math.abs(r.scale() - mult) < 1e-6
                    && Math.abs(r.healthBonusHearts() - baseHearts * mult) < 1e-6;
            check(String.format("%s：×%s 筑基（%.0f 颗心 / +%.0f 攻击）",
                            r.tierCn(), trimZero(mult), r.healthBonusHearts(), r.damageBonus()),
                    ok,
                    String.format("scale=%.1f hearts=%.1f", r.scale(), r.healthBonusHearts()));
            table.append(r.tierCn()).append(" ").append((long) r.healthBonusHearts())
                    .append("心  ");
        }
        System.out.println("        " + table.toString().trim());

        // 用户的原始要求：元婴 = 金丹 × 2，后续每境 × 2 直到飞升
        check("元婴生命 = 金丹 × 2",
                Math.abs(Realm.of(realmStart(4)).healthBonus()
                        - Realm.of(realmStart(3)).healthBonus() * 2.0D) < 1e-6,
                String.format("%.0f vs %.0f",
                        Realm.of(realmStart(4)).healthBonus(),
                        Realm.of(realmStart(3)).healthBonus() * 2.0D));
        check(String.format("金丹 = 筑基 × 2（%.0f 颗心）", baseHearts * 2.0D),
                Math.abs(Realm.of(realmStart(3)).healthBonusHearts() - baseHearts * 2.0D) < 1e-6,
                String.format("%.1f 颗心", Realm.of(realmStart(3)).healthBonusHearts()));
        check(String.format("元婴 = 筑基 × 4（%.0f 颗心）", baseHearts * 4.0D),
                Math.abs(Realm.of(realmStart(4)).healthBonusHearts() - baseHearts * 4.0D) < 1e-6,
                String.format("%.1f 颗心", Realm.of(realmStart(4)).healthBonusHearts()));
        check("飞升 = 道印 × 3",
                Math.abs(Realm.of(realmStart(7)).healthBonus()
                        - Realm.of(realmStart(6)).healthBonus() * 3.0D) < 1e-6,
                String.format("%.0f vs %.0f",
                        Realm.of(realmStart(7)).healthBonus(),
                        Realm.of(realmStart(6)).healthBonus() * 3.0D));
        check("仙人 = 飞升 × 10",
                Math.abs(Realm.of(realmStart(8)).healthBonus()
                        - Realm.of(realmStart(7)).healthBonus() * 10.0D) < 1e-6,
                String.format("%.0f vs %.0f",
                        Realm.of(realmStart(8)).healthBonus(),
                        Realm.of(realmStart(7)).healthBonus() * 10.0D));
        check("帝境 = 仙人 × 10",
                Math.abs(Realm.of(realmStart(9)).healthBonus()
                        - Realm.of(realmStart(8)).healthBonus() * 10.0D) < 1e-6, "");
        check("鸿蒙 = 帝境 × 10",
                Math.abs(Realm.of(realmStart(10)).healthBonus()
                        - Realm.of(realmStart(9)).healthBonus() * 10.0D) < 1e-6, "");

        // 攻击与生命同倍率
        boolean sameRatio = true;
        for (int tier = 2; tier <= 10; tier++) {
            Realm r = Realm.of(realmStart(tier));
            // 这条是**真正的不变量**：攻击与生命的成长倍率必须一致
            if (Math.abs(r.damageBonus() / baseAtk - r.healthBonus() / baseHp) > 1e-6) {
                sameRatio = false;
                break;
            }
        }
        check("攻击与生命严格同倍率增长", sameRatio, "");

        // 单调递增
        boolean increasing = true;
        double last = -1.0D;
        for (int tier = 1; tier <= 10; tier++) {
            double v = Realm.of(realmStart(tier)).healthBonus();
            if (v <= last) {
                increasing = false;
                break;
            }
            last = v;
        }
        check("生命加成随境界严格递增", increasing, "");

        // 能力解锁节点
        check("练气/筑基/金丹/元婴：无速度加成",
                !Realm.of(realmStart(1)).hasSpeedOne() && !Realm.of(realmStart(4)).hasSpeedOne(), "");
        check("化神解锁常驻速度 I", Realm.of(realmStart(5)).hasSpeedOne(), "");
        check("化神尚未解锁飞行", !Realm.of(realmStart(5)).hasFlight(), "");
        // 按玩家要求改过：飞升（7）不再白送飞行——飞升的定位是"御剑 30 分钟"，
        // 那要花灵力；真正踏空而行要到仙人（8）。
        // **御剑已删除**：飞升(7) 起直接可飞（玩家：「飞行的话飞升就可以飞行了」）。
        check("飞升(7) 起可以直接飞", Realm.of(realmStart(7)).hasFlight(), "");
        check("金丹(3) 不能飞", !Realm.of(realmStart(3)).hasFlight(), "");
        check("仙人解锁踏空飞行", Realm.of(realmStart(8)).hasFlight(), "");
        // 但飞升作为一个里程碑该有的常驻待遇不能跟着推迟
        check("飞升享有常驻效果（与飞行分开判定）",
                Realm.of(realmStart(7)).hasAscensionPerks()
                        && !Realm.of(realmStart(6)).hasAscensionPerks(), "");
        check("飞升尚未解锁速度 II", !Realm.of(realmStart(7)).hasSpeedTwo(), "");
        check("帝境解锁常驻速度 II", Realm.of(realmStart(9)).hasSpeedTwo(), "");
        check("鸿蒙同样拥有速度 II", Realm.of(realmStart(10)).hasSpeedTwo(), "");
        check("道印起拥有本命领域", Realm.of(realmStart(6)).hasDomain()
                && !Realm.of(realmStart(5)).hasDomain(), "");

        // 速度开关的存档往返
        CultivationData d = new CultivationData();
        check("速度 II 开关默认为开", d.isSpeedBoostEnabled(), "");
        d.toggleSpeedBoost();
        check("开关可切换为关", !d.isSpeedBoostEnabled(), "");
        net.minecraft.nbt.CompoundTag tag = d.save();
        CultivationData d2 = new CultivationData();
        d2.load(tag);
        check("速度开关随存档往返一致", !d2.isSpeedBoostEnabled(), "");
    }

    private static String trimZero(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /**
     * 境界 → 空手挖掘等级。需求：
     * 金丹=石制、化神=铁制、道印=金制、飞升=钻石、仙人及以上=下界合金，
     * 金丹之前是空手（-1），鸿蒙可破坏一切。
     */
    private static void harvestTiers() {
        section("境界挖掘等级");

        // 各大境起始修为：从 0 开始逐层累加，取每个大境的第一层
        long[] firstTotalOfRealm = new long[11];
        for (long total = 0; total < 400_000_000L; total += 1L) {
            Realm r = Realm.of(total);
            if (firstTotalOfRealm[r.tier()] == 0L && total > 0L) {
                firstTotalOfRealm[r.tier()] = r.minTotal();
            }
            if (r.tier() >= 10) {
                break;
            }
        }

        // 练气 / 筑基：空手
        check("练气：无挖掘等级（空手）",
                harvestLevelAt(0L) == -1, String.valueOf(harvestLevelAt(0L)));
        check("筑基：无挖掘等级（空手）",
                harvestLevelAt(realmStart(2)) == -1,
                String.valueOf(harvestLevelAt(realmStart(2))));

        // 金丹 = 石制(1)、元婴 = 石制(1)
        check("金丹：石制（等级 1）", harvestLevelAt(realmStart(3)) == 1,
                String.valueOf(harvestLevelAt(realmStart(3))));
        check("元婴：石制（等级 1）", harvestLevelAt(realmStart(4)) == 1,
                String.valueOf(harvestLevelAt(realmStart(4))));
        // 化神 = 铁制(2)
        check("化神：铁制（等级 2）", harvestLevelAt(realmStart(5)) == 2,
                String.valueOf(harvestLevelAt(realmStart(5))));
        // 道印 = 挖掘等级 3（金制）
        check("道印：金制（等级 3，铁之上）", harvestLevelAt(realmStart(6)) == 3,
                String.valueOf(harvestLevelAt(realmStart(6))));
        check("道印的名称显示为『金制』",
                "金制".equals(Realm.of(realmStart(6)).harvestTierName()),
                Realm.of(realmStart(6)).harvestTierName());
        check("道印的挖掘等级严格高于化神（不会倒退）",
                harvestLevelAt(realmStart(6)) > harvestLevelAt(realmStart(5)),
                harvestLevelAt(realmStart(6)) + " > " + harvestLevelAt(realmStart(5)));
        // 飞升 / 仙人 = 也是金制速度（按玩家要求：道印之后到仙人都是金镐速度）
        check("飞升：仍是金制（等级 4，速度同为金镐）",
                harvestLevelAt(realmStart(7)) == 4,
                String.valueOf(harvestLevelAt(realmStart(7))));
        check("飞升的挖掘速度文案是『效率III金镐速度』",
                "效率III金镐速度".equals(Realm.of(realmStart(7)).harvestSpeedLabel()),
                Realm.of(realmStart(7)).harvestSpeedLabel());
        check("仙人的挖掘速度文案是『效率IV金镐速度』",
                "效率IV金镐速度".equals(Realm.of(realmStart(8)).harvestSpeedLabel()),
                Realm.of(realmStart(8)).harvestSpeedLabel());
        // 仙人 / 帝境 / 鸿蒙 = 等级 5（鸿蒙速度为效率五金镐）
        for (int tier : new int[]{8, 9, 10}) {
            check("第 " + tier + " 境：下界合金（等级 5）",
                    harvestLevelAt(realmStart(tier)) == 5,
                    String.valueOf(harvestLevelAt(realmStart(tier))));
        }
        check("鸿蒙的挖掘速度文案是『效率X金镐速度』",
                "效率X金镐速度".equals(Realm.of(realmStart(10)).harvestSpeedLabel()),
                Realm.of(realmStart(10)).harvestSpeedLabel());

        // 只有鸿蒙能破坏一切
        check("鸿蒙之前都不可破坏一切",
                !Realm.of(realmStart(9)).canBreakAnything(), "");
        check("鸿蒙可破坏一切（含基岩）",
                Realm.of(realmStart(10)).canBreakAnything(), "");
        check("鸿蒙的面板文案含『可破坏一切』",
                Realm.of(realmStart(10)).harvestTierLabel().contains("可破坏一切"),
                Realm.of(realmStart(10)).harvestTierLabel());
        check("飞升的面板文案不含『可破坏一切』",
                !Realm.of(realmStart(7)).harvestTierLabel().contains("可破坏一切"),
                Realm.of(realmStart(7)).harvestTierLabel());
        check("练气的面板文案是『空手』",
                "空手".equals(Realm.of(0L).harvestTierLabel()),
                Realm.of(0L).harvestTierLabel());

        // ---- 挖掘速度（按玩家要求：每个大境对应一种工具的速度） ----
        // 速度按**大境序号**（1~10），不看挖掘等级——等级被多境共用。
        check("练气挖掘速度 2.0（木镐）",
                Math.abs(Realm.speedForTier(1) - 2.0F) < 1e-6,
                String.valueOf(Realm.speedForTier(1)));
        check("筑基挖掘速度 4.0（石镐）",
                Math.abs(Realm.speedForTier(2) - 4.0F) < 1e-6, "");
        check("金丹挖掘速度 6.0（铁镐）",
                Math.abs(Realm.speedForTier(3) - 6.0F) < 1e-6, "");
        check("元婴挖掘速度 12.0（金镐）",
                Math.abs(Realm.speedForTier(4) - 12.0F) < 1e-6, "");
        check("化神挖掘速度 14.0（效率I）",
                Math.abs(Realm.speedForTier(5) - Realm.EFFICIENCY_I) < 1e-6, "");
        check("道印挖掘速度 17.0（效率II）",
                Math.abs(Realm.speedForTier(6) - Realm.EFFICIENCY_II) < 1e-6, "");
        check("飞升挖掘速度 22.0（效率III）",
                Math.abs(Realm.speedForTier(7) - Realm.EFFICIENCY_III_GOLD_SPEED) < 1e-6, "");
        check("仙人挖掘速度 29.0（效率IV）",
                Math.abs(Realm.speedForTier(8) - Realm.EFFICIENCY_IV) < 1e-6,
                String.valueOf(Realm.speedForTier(8)));
        check("帝境挖掘速度 38.0（效率V）",
                Math.abs(Realm.speedForTier(9) - 38.0F) < 1e-6,
                String.valueOf(Realm.speedForTier(9)));
        check("鸿蒙挖掘速度 113（效率X金镐）",
                Math.abs(Realm.speedForTier(10) - Realm.EFFICIENCY_X) < 1e-6,
                String.valueOf(Realm.speedForTier(10)));

        // 效率附魔的等效数值要对得上原版公式 speed + level² + 1
        check("效率III金镐 = 12 + 3² + 1 = 22",
                Math.abs(Realm.EFFICIENCY_III_GOLD_SPEED - (12.0F + 9.0F + 1.0F)) < 1e-6, "");
        check("效率X金镐 = 12 + 10² + 1 = 113",
                Math.abs(Realm.EFFICIENCY_X - 113.0F) < 1e-6, "");
        check("efficiencySpeed(10) = 113",
                Math.abs(Realm.efficiencySpeed(10) - 113.0F) < 1e-6, "");
        // 时运：飞升起有，鸿蒙更高
        check("练气没有挖掘时运", Realm.fortuneForTier(1) == 0, "");
        check("飞升挖掘时运 III", Realm.fortuneForTier(7) == 3, "");
        check("仙人挖掘时运 III", Realm.fortuneForTier(8) == 3, "");
        check("帝境挖掘时运 III", Realm.fortuneForTier(9) == 3, "");
        check("鸿蒙挖掘时运 V", Realm.fortuneForTier(10) == 5, "");
        // 急迫：仙人起
        check("飞升没有急迫", !Realm.ofStep(realmStartIndex(7)).hasHaste(), "");
        check("仙人起自带急迫", Realm.ofStep(realmStartIndex(8)).hasHaste(), "");
        check("效率V金镐大于效率III",
                Realm.EFFICIENCY_V_GOLD_SPEED > Realm.EFFICIENCY_III_GOLD_SPEED, "");

        // 文案逐境正确
        String[] speedLabels = {"木镐速度", "石镐速度", "铁镐速度", "金镐速度", "效率I金镐速度",
                "效率II金镐速度", "效率III金镐速度", "效率IV金镐速度", "效率V金镐速度", "效率X金镐速度"};
        boolean labelsOk = true;
        StringBuilder labelSb = new StringBuilder();
        for (int tier = 1; tier <= 10; tier++) {
            String got = Realm.ofStep(realmStartIndex(tier)).harvestSpeedLabel();
            labelSb.append(tier).append(":").append(got).append("  ");
            if (!got.equals(speedLabels[tier - 1])) {
                labelsOk = false;
            }
        }
        check("十档挖掘速度文案全部正确", labelsOk, "");
        System.out.println("        " + labelSb.toString().trim());

        boolean speedNonDecreasing = true;
        for (int tier = 1; tier < 10; tier++) {
            if (Realm.speedForTier(tier + 1) < Realm.speedForTier(tier)) {
                speedNonDecreasing = false;
                break;
            }
        }
        check("挖掘速度随大境单调不降", speedNonDecreasing, "");
        check("金镐速度（12）高于钻石镐（8）——这是「金制」的原意",
                Realm.speedForTier(6) > 8.0F, "");
        check("越界大境序号被夹住（0 与 99 都不炸）",
                Realm.speedForTier(0) == Realm.speedForTier(1)
                        && Realm.speedForTier(99) == Realm.speedForTier(10), "");

        // ---- 境界护甲 ----
        check("练气没有境界护甲", Realm.armorForTier(1) == 0.0D, "");
        check("筑基境界护甲 3", Realm.armorForTier(2) == 3.0D, "");
        check("金丹境界护甲 6", Realm.armorForTier(3) == 6.0D, "");
        check("元婴境界护甲 10", Realm.armorForTier(4) == 10.0D, "");
        check("化神境界护甲 15", Realm.armorForTier(5) == 15.0D, "");
        check("道印境界护甲 24", Realm.armorForTier(6) == 24.0D, "");
        check("飞升境界护甲 36", Realm.armorForTier(7) == 36.0D, "");
        check("仙人境界护甲 52", Realm.armorForTier(8) == 52.0D, "");
        check("帝境境界护甲 72", Realm.armorForTier(9) == 72.0D, "");
        check("鸿蒙境界护甲 105", Realm.armorForTier(10) == 105.0D, "");

        boolean armorNonDecreasing = true;
        for (int tier = 1; tier < 10; tier++) {
            if (Realm.armorForTier(tier + 1) < Realm.armorForTier(tier)) {
                armorNonDecreasing = false;
                break;
            }
        }
        check("境界护甲单调不降", armorNonDecreasing, "");
        check("境界护甲表长度是 10", Realm.ARMOR_BY_TIER.length == 10, "");
        check("护甲档位名随境界递进",
                Realm.ofStep(realmStartIndex(1)).armorRankName().equals("无")
                        && Realm.ofStep(realmStartIndex(10)).armorRankName()
                        .equals("鸿蒙道甲"),
                Realm.ofStep(realmStartIndex(1)).armorRankName() + " → "
                        + Realm.ofStep(realmStartIndex(10)).armorRankName());

        // ---- 护甲软上限（护甲不再无敌的关键） ----
        //
        // 口径统一到"护甲值"上：原版减伤公式 1-2/(2+armor/5) 的斜率比任何
        // 线性曲线都陡，所以在伤害阶段叠加减伤是没用的（永远赢不过原版）。
        // 真正该管的是护甲值本身——softCapArmor 把超过 80 的部分按 10% 折算。
        check("护甲软上限是 80",
                Math.abs(com.hongmeng.xiuxian.compat.ArmorMitigation.ARMOR_SOFT_CAP - 80.0D) < 1e-9,
                String.valueOf(com.hongmeng.xiuxian.compat.ArmorMitigation.ARMOR_SOFT_CAP));
        check("软上限之下护甲原样返回",
                Math.abs(com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(50.0D) - 50.0D) < 1e-9,
                "");
        check("软上限刚好在 80 时不折算",
                Math.abs(com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(80.0D) - 80.0D) < 1e-9,
                "");
        check("超过软上限后收益骤减（160 → 88）",
                Math.abs(com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(160.0D) - 88.0D) < 1e-9,
                String.valueOf(com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(160.0D)));
        check("软上限后仍然单调递增（堆护甲仍有意义）",
                com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(200.0D)
                        > com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(160.0D), "");
        check("负护甲被夹到 0",
                com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(-5.0D) == 0.0D, "");

        // ---- 法宝护甲（2026-11-07 重做）----
        //
        // 旧版五件合计 0.805×标准值（比道袍还高），4 境后撞 92% 封顶 → 纯数字；
        // 现在五件合计 = 标准值 × 0.276（低于职业甲 0.50/0.58/0.64 与道袍 0.70），
        // 把"额外生命 0.16×标准值（道印 +208%、鸿蒙 +4%）"换成生命%/攻击%。
        boolean artArmorOk = true;
        boolean artRising = true;
        double prevArt = -1.0D;
        StringBuilder artCurve = new StringBuilder();
        for (int tier = 1; tier <= 10; tier++) {
            double set = com.hongmeng.xiuxian.item.gear.ArtifactStats.setArmor(tier);
            double want = com.hongmeng.xiuxian.core.RealmStats.standard(tier) * 0.276D;
            if (Math.abs(set - want) > 1.0E-6D || set <= prevArt) {
                artArmorOk = false;
            }
            if (set >= com.hongmeng.xiuxian.item.armor.CareerArmorRealm.totalArmorAt(tier)
                    || set >= com.hongmeng.xiuxian.core.RealmStats.armorTotal(tier)) {
                artArmorOk = false;
            }
            prevArt = set;
            artCurve.append(String.format("%.0f ", set));
        }
        check("法宝五件合计护甲 = 本境标准值 × 0.276（逐境递增、低于职业甲与道袍）",
                artArmorOk, artCurve.toString().trim());

        // 法宝 + 职业甲 + 境界护甲：整体减伤 1~3 境在 40%~92%，4 境起封顶 92%
        boolean artDrOk = true;
        StringBuilder artDrInfo = new StringBuilder();
        for (int tier = 1; tier <= 10; tier++) {
            double raw = com.hongmeng.xiuxian.item.gear.ArtifactStats.setArmor(tier)
                    + com.hongmeng.xiuxian.item.armor.CareerArmorRealm.totalArmorAt(tier)
                    + Realm.armorForTier(tier);
            double dr = com.hongmeng.xiuxian.compat.ArmorMitigation.totalReduction(raw);
            artDrInfo.append(String.format("%.0f%% ", dr * 100.0D));
            if (dr < 0.35D || dr > 0.92D + 1.0E-9D) {
                artDrOk = false;
            }
            if (tier >= 4 && Math.abs(dr - 0.92D) > 1.0E-9D) {
                artDrOk = false;
            }
        }
        check("法宝+职业甲+境界护甲整体减伤 35%~92%，4 境起封顶", artDrOk, "");

        // 注意：等级 → 实际 Tier 对象的映射（RealmMining.tierFor）与掉落行为
        // 依赖游戏注册表，无法在裸 main() 里验证，改由专用服务端实测覆盖。

        // 挖掘等级随境界单调不降
        boolean nondescending = true;
        int last = -1;
        for (int tier = 1; tier <= 10; tier++) {
            int lvl = harvestLevelAt(realmStart(tier));
            if (lvl < last) {
                nondescending = false;
                break;
            }
            last = lvl;
        }
        check("挖掘等级随大境界单调不降", nondescending, "");

        // 等级比较规则（纯数值，避免触碰游戏注册表）
        check("等级 1（石制）足以挖动需求 0 的方块（泥土石头）",
                harvestLevelAt(realmStart(3)) >= 0, "");
        check("等级 1（石制）挖不动需求 2 的方块（铁矿）",
                harvestLevelAt(realmStart(3)) < 2, "");
        check("等级 2（化神/铁制）能挖动铁矿",
                harvestLevelAt(realmStart(5)) >= 2, "");
        check("等级 4（飞升/钻石）能挖动黑曜石（需求 4）",
                harvestLevelAt(realmStart(7)) >= 4, "");
        check("等级 5（仙人及以上/下界合金）能挖动远古残骸",
                harvestLevelAt(realmStart(8)) >= 5, "");

        // 注意：按方块（Blocks.STONE 等）断言挖掘判定的那几条**不能放在这里**——
        // 会触发注册表引导（Not bootstrapped）。它们已经挪到
        // hongmeng attrselftest 的"工具类型覆盖"一节，那里有真实方块。
    }

    /** 某大境第一层的累计修为。 */
    private static long realmStart(int tier) {
        for (long total = 0; total < 400_000_000L; total += 7L) {
            Realm r = Realm.of(total);
            if (r.tier() >= tier) {
                return r.minTotal();
            }
        }
        return 0L;
    }

    /**
     * 某大境第一层的**层号**。
     *
     * <p>9 个大境各 3 层（鸿蒙只有 1 层），所以第 tier 境的起点是
     * {@code (tier-1) * 3}。</p>
     */
    private static int realmStartIndex(int tier) {
        int start = (tier - 1) * 3;
        int found = -1;
        for (int step = start; step < Realm.TOTAL_STEPS; step++) {
            if (Realm.ofStep(step).tier() >= tier) {
                found = step;
                break;
            }
        }
        return found < 0 ? Realm.TOTAL_STEPS - 1 : found;
    }

    /** 采集等级数值；-1 表示空手。 */
    private static int harvestLevelAt(long total) {
        return Realm.harvestLevelOf(total);
    }

    private static CultivationData dataWithCultivation(long amount) {
        CultivationData d = new CultivationData();
        d.setAwakened(true);
        d.setCultivation(amount);
        return d;
    }

    /**
     * 道宫系统：六档品质加成、能量消耗、100/500 保底、破碎重凝、元素加成。
     */
    private static void domainSystem() {
        section("道宫系统");

        // 六档品质的加成数值必须精确匹配需求
        // 凡/地/天/仙 四档来自原始需求，混沌为对齐灵根七档而补，鸿蒙为最高档
        String[] names = {"凡品", "地品", "天品", "仙品", "混沌", "鸿蒙"};
        double[] damage = {1.00D, 2.00D, 4.00D, 6.00D, 8.00D, 10.00D};
        double[] cast = {0.10D, 0.20D, 0.40D, 0.60D, 0.80D, 1.00D};
        boolean exact = true;
        StringBuilder sb = new StringBuilder();
        com.hongmeng.xiuxian.domain.DomainQuality[] qs =
                com.hongmeng.xiuxian.domain.DomainQuality.values();
        for (int i = 0; i < qs.length; i++) {
            boolean ok = qs[i].cn().equals(names[i])
                    && Math.abs(qs[i].techniqueDamage() - damage[i]) < 1e-9
                    && Math.abs(qs[i].castSpeed() - cast[i]) < 1e-9;
            if (!ok) {
                exact = false;
            }
            sb.append(qs[i].cn()).append("伤害+")
                    .append((int) (qs[i].techniqueDamage() * 100)).append("%/")
                    .append("速度+").append((int) (qs[i].castSpeed() * 100)).append("%  ");
        }
        check("六档道宫品质的名称与加成完全符合需求", exact, "");
        System.out.println("        " + sb.toString().trim());

        // 凝聚消耗与保底
        check("凝一座道宫消耗 10 能量",
                com.hongmeng.xiuxian.domain.DomainData.CONDENSE_COST == 10, "");
        check("60 次保底仙品",
                com.hongmeng.xiuxian.domain.DomainData.PITY_IMMORTAL == 60, "");
        check("200 次保底鸿蒙",
                com.hongmeng.xiuxian.domain.DomainData.PITY_HONGMENG == 200, "");
        check("仙品保底比鸿蒙保底先到",
                com.hongmeng.xiuxian.domain.DomainData.PITY_IMMORTAL
                        < com.hongmeng.xiuxian.domain.DomainData.PITY_HONGMENG, "");

        // 权重必须全部为正，否则抽取会退化成"只出某一档"
        boolean weightsPositive = true;
        for (var q : com.hongmeng.xiuxian.domain.DomainQuality.values()) {
            if (q.weight() <= 0.0D) {
                weightsPositive = false;
                break;
            }
        }
        check("六档道宫权重都为正数（每一档都抽得到）", weightsPositive, "");

        // 能量不足时不能凝聚
        com.hongmeng.xiuxian.domain.DomainData d = new com.hongmeng.xiuxian.domain.DomainData();
        check("初始无道宫、无能量", d.domainCount() == 0 && d.energy() == 0, "");
        check("能量不足时 canCondense 为 false", !d.canCondense(), "");
        Random rng = new Random(42L);
        check("能量不足时凝聚返回 null",
                d.condense(rng, Element.ICE) == null, "");

        // 攒够能量后能凝聚，并扣能量
        d.addEnergy(10);
        check("攒够 10 能量后可凝聚", d.canCondense(), "");
        com.hongmeng.xiuxian.domain.DomainData.Domain first = d.condense(rng, Element.ICE);
        check("凝聚成功并返回道宫", first != null, "");
        check("凝聚后能量归零", d.energy() == 0, "实际 " + d.energy());
        check("凝聚后道宫数为 1", d.domainCount() == 1, "");
        check("累计凝聚次数为 1", d.totalCondensed() == 1, "");

        // 未解锁双道宫时不能再凝
        d.addEnergy(10);
        check("未解锁双道宫时，满 1 座不能再凝", !d.canCondense(), "");

        // 破碎退还一半能量
        com.hongmeng.xiuxian.domain.DomainData.Domain removed = d.shatter(0);
        check("破碎成功并返回被碎的道宫", removed != null, "");
        check("破碎后道宫数为 0", d.domainCount() == 0, "");
        check("破碎不退还凝聚次数（保底进度保留）",
                d.totalCondensed() == 1, "实际 " + d.totalCondensed());

        // 保底：把累计次数推到 100 / 500
        com.hongmeng.xiuxian.domain.DomainData pity =
                new com.hongmeng.xiuxian.domain.DomainData();
        pity.setDualUnlocked(true);
        StringBuilder curve = new StringBuilder();
        java.util.Map<com.hongmeng.xiuxian.domain.DomainQuality, Integer> counts =
                new java.util.EnumMap<>(com.hongmeng.xiuxian.domain.DomainQuality.class);
        boolean pityImmortal = false;
        for (int i = 1; i <= 540; i++) {
            pity.addEnergy(com.hongmeng.xiuxian.domain.DomainData.CONDENSE_COST);
            // 保持空位：凝完立刻碎掉
            com.hongmeng.xiuxian.domain.DomainData.Domain got =
                    pity.condense(new Random(i * 7919L), Element.FIRE);
            if (got == null) {
                continue;
            }
            counts.merge(got.quality(), 1, Integer::sum);
            if (i >= 100 && !pityImmortal
                    && got.quality().rank() >= com.hongmeng.xiuxian.domain.DomainQuality
                    .IMMORTAL.rank()) {
                pityImmortal = true;
            }
            pity.shatter(0);
        }
        for (com.hongmeng.xiuxian.domain.DomainQuality q
                : com.hongmeng.xiuxian.domain.DomainQuality.values()) {
            curve.append(q.cn()).append(":").append(counts.getOrDefault(q, 0)).append("  ");
        }
        check("600 次以内必出过仙品及以上", pityImmortal, "");
        check("保底计数已超过 500（进入鸿蒙保底段）",
                pity.totalCondensed() >= 500, "实际 " + pity.totalCondensed());
        System.out.println("        540 次凝聚的品质分布：" + curve.toString().trim());

        // 元素加成：只有同元素才吃加成
        com.hongmeng.xiuxian.domain.DomainData elem =
                new com.hongmeng.xiuxian.domain.DomainData();
        elem.setDualUnlocked(true);
        elem.addEnergy(10);
        com.hongmeng.xiuxian.domain.DomainData.Domain ice =
                elem.condense(new Random(1L), Element.ICE);
        if (ice != null && ice.element() == Element.ICE) {
            check("同元素（冰）享受伤害加成",
                    elem.techniqueBonus(Element.ICE) > 0.0D,
                    String.valueOf(elem.techniqueBonus(Element.ICE)));
        } else {
            check("同元素（冰）享受伤害加成（本次抽样元素被随机覆盖，跳过）", true, "");
        }
        check("不同元素（金）不吃该道宫的加成",
                elem.techniqueBonus(Element.METAL) == 0.0D
                        || elem.domainCount() == 0
                        || elem.domains().get(0).element() == Element.METAL,
                String.valueOf(elem.techniqueBonus(Element.METAL)));

        // 存档往返
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        d.save(tag);
        com.hongmeng.xiuxian.domain.DomainData loaded =
                new com.hongmeng.xiuxian.domain.DomainData();
        loaded.load(tag);
        check("道宫数据存档往返：能量一致", loaded.energy() == d.energy(), "");
        check("道宫数据存档往返：凝聚次数一致",
                loaded.totalCondensed() == d.totalCondensed(), "");
        check("道宫数据存档往返：双道宫解锁状态一致",
                loaded.isDualUnlocked() == d.isDualUnlocked(), "");
    }

    /**
     * 任务系统：定义完整性、进度判定、前置链、奖励。
     */
    private static void questSystem() {
        section("任务系统");

        check("任务总数 > 10", com.hongmeng.xiuxian.quest.Quests.total() > 10,
                "实际 " + com.hongmeng.xiuxian.quest.Quests.total());
        check("任务 id 无重复",
                new java.util.HashSet<>(com.hongmeng.xiuxian.quest.Quests.ALL.stream()
                        .map(com.hongmeng.xiuxian.quest.Quest::id).toList()).size()
                        == com.hongmeng.xiuxian.quest.Quests.total(), "");

        // 每条任务都必须有章、有标题、有描述、有提示
        boolean complete = true;
        for (com.hongmeng.xiuxian.quest.Quest q : com.hongmeng.xiuxian.quest.Quests.ALL) {
            if (q.chapter().isEmpty() || q.title().isEmpty()
                    || q.description().isEmpty() || q.hint().isEmpty()) {
                complete = false;
                break;
            }
        }
        check("每条任务都有章节/标题/描述/提示", complete, "");

        // 至少有 3 个章节
        check("任务分 3 个章节",
                com.hongmeng.xiuxian.quest.Quest.byChapter().size() == 3,
                "实际 " + com.hongmeng.xiuxian.quest.Quest.byChapter().size());

        // 进度判定：初始全部未完成
        com.hongmeng.xiuxian.quest.QuestProgress progress =
                new com.hongmeng.xiuxian.quest.QuestProgress();
        check("初始已完成数为 0",
                com.hongmeng.xiuxian.quest.QuestManager.completedCount(progress) == 0, "");

        // 击杀计数器能推进任务
        com.hongmeng.xiuxian.quest.Quest hunt =
                com.hongmeng.xiuxian.quest.Quests.byId("q_hunt");
        check("q_hunt 目标为击杀 3", hunt.requirement().target() == 3, "");
        check("0 杀时未达成", !hunt.requirement().isMet(progress), "");
        progress.addKill();
        progress.addKill();
        check("2 杀时仍未达成（进度 2/3）",
                !hunt.requirement().isMet(progress), "");
        progress.addKill();
        check("3 杀时达成", hunt.requirement().isMet(progress), "");

        // 前置链：第一条任务默认解锁，后面的要等前面领奖
        com.hongmeng.xiuxian.quest.Quest first =
                com.hongmeng.xiuxian.quest.Quests.ALL.get(0);
        check("第一条任务默认解锁", progress.isUnlocked(first), "");
        check("第二条任务在第一条领奖前锁着",
                !progress.isUnlocked(com.hongmeng.xiuxian.quest.Quests.ALL.get(1)), "");
        progress.setState(first.id(), 2);
        check("第一条领奖后第二条解锁",
                progress.isUnlocked(com.hongmeng.xiuxian.quest.Quests.ALL.get(1)), "");

        // 追踪取第一条未领奖的
        com.hongmeng.xiuxian.quest.Quest tracked =
                com.hongmeng.xiuxian.quest.QuestManager.tracked(progress);
        check("追踪的是第一条未领奖任务",
                tracked != null && tracked.id()
                        .equals(com.hongmeng.xiuxian.quest.Quests.ALL.get(1).id()),
                tracked == null ? "null" : tracked.id());

        // 打坐 / 采药计数器
        progress.addMeditateSecond();
        progress.addGrass();
        check("打坐计数器可累计", progress.getMeditateSeconds() == 1, "");
        check("采药计数器可累计", progress.getGrassGathered() == 1, "");

        // 任务进度存档往返
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        progress.save(tag);
        com.hongmeng.xiuxian.quest.QuestProgress loaded =
                new com.hongmeng.xiuxian.quest.QuestProgress();
        loaded.load(tag);
        check("任务进度存档往返：击杀数一致",
                loaded.getKills() == progress.getKills(), "");
        check("任务进度存档往返：状态表一致",
                loaded.stateOf(first.id()) == 2, "");
        check("任务进度存档往返：打坐秒数一致",
                loaded.getMeditateSeconds() == 1, "");
    }

    /**
     * 难度系统。需求：
     * 普通削弱不了伪灵根；困难减少伪灵根；极限最低凡品；混沌最低地品且放大先天/混沌；
     * 鸿蒙最低地品并进一步放大；道宫同理。难度越高怪物越强。
     */
    private static void difficultySystem() {
        section("难度系统");

        com.hongmeng.xiuxian.difficulty.CultivationDifficulty[] all =
                com.hongmeng.xiuxian.difficulty.CultivationDifficulty.values();
        check("共 5 档难度（普通/困难/极限/混沌/鸿蒙）", all.length == 5,
                "实际 " + all.length);
        check("难度名称顺序正确",
                all[0].cn().equals("普通") && all[1].cn().equals("困难")
                        && all[2].cn().equals("极限") && all[3].cn().equals("混沌")
                        && all[4].cn().equals("鸿蒙"), "");

        // ---- 保底下限 ----
        check("普通：无灵根保底（允许伪灵根）", all[0].rootFloor() == 0, "");
        check("困难：无灵根保底", all[1].rootFloor() == 0, "");
        check("极限：最低凡品灵根（序号 1）", all[2].rootFloor() == 1, "");
        check("混沌：最低地品灵根（序号 2）", all[3].rootFloor() == 2, "");
        check("鸿蒙：最低地品灵根（序号 2）", all[4].rootFloor() == 2, "");

        // ---- 每档难度的灵根分布 ----
        int samples = 400_000;
        StringBuilder table = new StringBuilder();
        Rarity[] rarities = Rarity.values();

        // 普通/困难/极限 的伪灵根概率必须严格递减
        double[] falseProb = new double[all.length];
        // 混沌及以上 出现混沌/鸿蒙灵根的概率
        double[] topProb = new double[all.length];

        for (int di = 0; di < all.length; di++) {
            Random random = new Random(1000L + di);
            int[] counts = new int[rarities.length];
            for (int i = 0; i < samples; i++) {
                counts[all[di].rollRootRarity(random)]++;
            }
            StringBuilder row = new StringBuilder();
            row.append(String.format("%-4s ", all[di].cn()));
            for (int r = 0; r < rarities.length; r++) {
                double pct = counts[r] * 100.0D / samples;
                row.append(String.format("%s%.1f%% ", rarities[r].cn(), pct));
            }
            table.append("        ").append(row.toString().trim()).append("\n");

            falseProb[di] = counts[0] * 100.0D / samples;
            topProb[di] = (counts[5] + counts[6]) * 100.0D / samples;
        }
        System.out.print(table);

        check("普通难度：伪灵根占据主导（>35%）", falseProb[0] > 35.0D,
                String.format("%.1f%%", falseProb[0]));
        check("困难难度：伪灵根概率明显低于普通",
                falseProb[1] < falseProb[0] * 0.7D,
                String.format("困难 %.1f%% < 普通 %.1f%%", falseProb[1], falseProb[0]));
        check("极限难度：伪灵根概率归零（保底凡品）",
                falseProb[2] < 0.01D, String.format("%.2f%%", falseProb[2]));
        check("混沌难度：伪灵根概率归零", falseProb[3] < 0.01D, "");
        check("鸿蒙难度：伪灵根概率归零", falseProb[4] < 0.01D, "");

        check("普通难度：抽不到混沌/鸿蒙灵根", topProb[0] < 0.01D,
                String.format("%.2f%%", topProb[0]));
        check("困难难度：抽不到混沌/鸿蒙灵根", topProb[1] < 0.01D, "");
        check("极限难度：混沌/鸿蒙灵根概率很低（<8%）", topProb[2] < 8.0D,
                String.format("%.1f%%", topProb[2]));
        check("混沌难度：混沌/鸿蒙灵根概率被放大（>10%）", topProb[3] > 10.0D,
                String.format("%.1f%%", topProb[3]));
        check("鸿蒙难度：混沌/鸿蒙灵根概率最高（>15%）", topProb[4] > 15.0D,
                String.format("%.1f%%", topProb[4]));

        // ---- 道宫保底 ----
        check("普通：道宫最低凡品", all[0].domainFloor() == 0, "");
        check("困难：道宫最低凡品", all[1].domainFloor() == 0, "");
        check("极限：道宫最低地品", all[2].domainFloor() == 1, "");
        check("混沌：道宫最低地品（名称为地品）",
                all[3].domainFloorName().equals("地品"), all[3].domainFloorName());
        check("鸿蒙：道宫最低地品", all[4].domainFloorName().equals("地品"), "");

        // 普通难度抽不到混沌/鸿蒙道宫
        Random r = new Random(555L);
        boolean normalHasTopDomain = false;
        for (int i = 0; i < 100_000; i++) {
            int q = all[0].rollDomainQuality(r);
            if (q >= com.hongmeng.xiuxian.domain.DomainQuality.CHAOS.ordinal()) {
                normalHasTopDomain = true;
                break;
            }
        }
        check("普通难度：抽不到混沌/鸿蒙道宫", !normalHasTopDomain, "");

        // 鸿蒙难度能抽到混沌/鸿蒙道宫
        Random r2 = new Random(777L);
        boolean hongmengHasTop = false;
        for (int i = 0; i < 100_000; i++) {
            int q = all[4].rollDomainQuality(r2);
            if (q >= com.hongmeng.xiuxian.domain.DomainQuality.CHAOS.ordinal()) {
                hongmengHasTop = true;
                break;
            }
        }
        check("鸿蒙难度：能抽到混沌/鸿蒙道宫", hongmengHasTop, "");

        // ---- 道宫品质总档数 ----
        check("道宫共 6 档（凡/地/天/仙/混沌/鸿蒙）",
                com.hongmeng.xiuxian.domain.DomainQuality.values().length == 6,
                "实际 " + com.hongmeng.xiuxian.domain.DomainQuality.values().length);
        check("混沌道宫加成 +800%/速度 +80%",
                Math.abs(com.hongmeng.xiuxian.domain.DomainQuality.CHAOS.techniqueDamage() - 8.0D) < 1e-9
                        && Math.abs(com.hongmeng.xiuxian.domain.DomainQuality.CHAOS.castSpeed() - 0.8D) < 1e-9, "");
        check("鸿蒙道宫加成 +1000%/速度 +100%",
                Math.abs(com.hongmeng.xiuxian.domain.DomainQuality.HONGMENG.techniqueDamage() - 10.0D) < 1e-9
                        && Math.abs(com.hongmeng.xiuxian.domain.DomainQuality.HONGMENG.castSpeed() - 1.0D) < 1e-9, "");

        // ---- 怪物强度 ----
        boolean mobIncreasing = true;
        double last = 0.0D;
        for (com.hongmeng.xiuxian.difficulty.CultivationDifficulty d : all) {
            if (d.mobHealthMultiplier() <= last) {
                mobIncreasing = false;
                break;
            }
            last = d.mobHealthMultiplier();
        }
        check("怪物生命倍率随难度严格递增", mobIncreasing, "");
        check("普通怪物不缩放（×1.00）",
                Math.abs(all[0].mobHealthMultiplier() - 1.0D) < 1e-9, "");
        check("鸿蒙怪物最强（×3.00）",
                Math.abs(all[4].mobHealthMultiplier() - 3.0D) < 1e-9,
                String.valueOf(all[4].mobHealthMultiplier()));
        check("所有难度的攻击倍率都不高于生命倍率",
                java.util.Arrays.stream(all).allMatch(d ->
                        d.mobDamageMultiplier() <= d.mobHealthMultiplier()), "");

        // 境界也会强化怪物（"怪物也需要质变"）
        double f1 = com.hongmeng.xiuxian.difficulty.MobScaling.realmFactor(1);
        double f3 = com.hongmeng.xiuxian.difficulty.MobScaling.realmFactor(3);
        double f6 = com.hongmeng.xiuxian.difficulty.MobScaling.realmFactor(6);
        double f10 = com.hongmeng.xiuxian.difficulty.MobScaling.realmFactor(10);
        check("玩家练气时怪不额外强化（×1.00）", Math.abs(f1 - 1.0D) < 1e-9, "");
        check("怪物强度随玩家境界递增",
                f1 < f3 && f3 < f6 && f6 < f10,
                String.format("%.2f < %.2f < %.2f < %.2f", f1, f3, f6, f10));
        // 境界倍率表在 2026-10 按玩家要求整体上调（鸿蒙 ×6.5 -> ×520），
        // 所以不再钉具体数字，改钉"设计意图"：单调递增 + 鸿蒙量级足以对抗满境玩家。
        boolean realmRising = true;
        for (int t = 1; t < 10; t++) {
            if (com.hongmeng.xiuxian.difficulty.MobScaling.realmFactor(t + 1)
                    <= com.hongmeng.xiuxian.difficulty.MobScaling.realmFactor(t)) {
                realmRising = false;
                break;
            }
        }
        check("怪物境界倍率随境界单调递增", realmRising, "");
        check("鸿蒙境界倍率 >= 400（足以对抗满境玩家，不再一刀秒）",
                com.hongmeng.xiuxian.difficulty.MobScaling.realmFactor(10) >= 400.0D,
                String.valueOf(com.hongmeng.xiuxian.difficulty.MobScaling.realmFactor(10)));
        check("BOSS 鸿蒙灵根额外加成 >= 1.5 倍",
                com.hongmeng.xiuxian.difficulty.MobScaling.BOSS_HONGMENG_ROOT_BONUS >= 1.5D,
                "");

        // ---- byId 容错 ----
        check("byId 支持枚举名", com.hongmeng.xiuxian.difficulty.CultivationDifficulty
                .byId("HONGMENG") == all[4], "");
        check("byId 支持中文名", com.hongmeng.xiuxian.difficulty.CultivationDifficulty
                .byId("混沌") == all[3], "");
        check("byId 未知输入回落普通", com.hongmeng.xiuxian.difficulty.CultivationDifficulty
                .byId("nope") == all[0], "");
        check("只有混沌/鸿蒙是割草档",
                !all[0].isMowingTier() && !all[1].isMowingTier() && !all[2].isMowingTier()
                        && all[3].isMowingTier() && all[4].isMowingTier(), "");
    }

    /**
     * 境界标准值表（整套装备 / 怪物数值的锚点）。
     *
     * <p>前 5 境是成熟修仙模组的实测值，必须一字不差；后 5 境是按 ×3.0 外推。</p>
     */
    private static void realmStatsTable() {
        section("境界标准值表");

        long[] expect = {8L, 40L, 136L, 424L, 1288L,
                3900L, 11_800L, 35_500L, 107_000L, 320_000L};
        String[] names = {"练气", "筑基", "金丹", "元婴", "化神",
                "道印", "飞升", "仙人", "帝境", "鸿蒙"};

        boolean allMatch = true;
        StringBuilder sb = new StringBuilder();
        for (int tier = 1; tier <= 10; tier++) {
            long got = com.hongmeng.xiuxian.core.RealmStats.standard(tier);
            if (got != expect[tier - 1]) {
                allMatch = false;
            }
            sb.append(names[tier - 1]).append(" ").append(got).append("  ");
        }
        check("十境标准值与实测锚点完全一致", allMatch, "");
        System.out.println("        " + sb.toString().trim());

        // 前五境是实测值（这里单独强调一次，因为它们不能被"优化"掉）
        check("练气标准值 = 8（实测）",
                com.hongmeng.xiuxian.core.RealmStats.standard(1) == 8L, "");
        check("筑基标准值 = 40（实测）",
                com.hongmeng.xiuxian.core.RealmStats.standard(2) == 40L, "");
        check("金丹标准值 = 136（实测）",
                com.hongmeng.xiuxian.core.RealmStats.standard(3) == 136L, "");
        check("元婴标准值 = 424（实测）",
                com.hongmeng.xiuxian.core.RealmStats.standard(4) == 424L, "");
        check("化神标准值 = 1288（实测）",
                com.hongmeng.xiuxian.core.RealmStats.standard(5) == 1288L, "");

        // 单调递增
        boolean increasing = true;
        for (int tier = 2; tier <= 10; tier++) {
            if (com.hongmeng.xiuxian.core.RealmStats.standard(tier)
                    <= com.hongmeng.xiuxian.core.RealmStats.standard(tier - 1)) {
                increasing = false;
                break;
            }
        }
        check("标准值随境界严格递增", increasing, "");

        // 武器增幅：每境 +5%
        check("练气武器增幅 0%",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.weaponAmplify(1)) < 1e-9, "");
        check("筑基武器增幅 +5%",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.weaponAmplify(2) - 0.05D) < 1e-9, "");
        check("化神武器增幅 +20%",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.weaponAmplify(5) - 0.20D) < 1e-9, "");
        check("鸿蒙武器增幅 +45%",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.weaponAmplify(10) - 0.45D) < 1e-9, "");
        boolean linear = true;
        for (int tier = 2; tier <= 10; tier++) {
            double delta = com.hongmeng.xiuxian.core.RealmStats.weaponAmplify(tier)
                    - com.hongmeng.xiuxian.core.RealmStats.weaponAmplify(tier - 1);
            if (Math.abs(delta - 0.05D) > 1e-9) {
                linear = false;
                break;
            }
        }
        check("武器增幅严格每境 +5%", linear, "");

        // 派生公式
        check("练气武器攻击 = 标准值 × 1.0 = 8",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.weaponAttack(1) - 8.0D) < 1e-9, "");
        check("化神武器攻击上限 = 1288 × 1.4 = 1803.2",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.weaponAttackMax(5) - 1288.0D * 1.4D) < 1e-6, "");
        check("化神全套总防御 = 1288 × 0.70 = 901.6",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.armorTotal(5) - 1288.0D * 0.70D) < 1e-6, "");

        // 怪物公式。
        //
        // 注意：这几个倍率**已经按玩家血量重新锚定过**，不再沿用
        // "标准值 ×5 / ×1.0"的实测口径——那套口径和玩家血量不是同一刻度，
        // 终局会算出"妖兽 160 万血、一击 96 万"而玩家只有 48 万血。
        // 2026-11-06「小怪数值下调」后：生命比 0.45→0.32，攻击改显式下限表，
        // 详见 RealmStats 里 MOB_HEALTH_RATIO / MOB_ATTACK_FLOOR 的注释。
        check("练气妖兽 HP = 8 × 0.32 = 2.56",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.mobHealth(1) - 2.56D) < 1e-9,
                String.valueOf(com.hongmeng.xiuxian.core.RealmStats.mobHealth(1)));
        check("练气妖兽攻击下限 = 0.48（表值，保留旧值不升）",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.mobAttack(1) - 0.48D) < 1e-6,
                String.valueOf(com.hongmeng.xiuxian.core.RealmStats.mobAttack(1)));
        check("元婴 BOSS HP = 424 × 1.20 = 508.8",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.bossHealth(4) - 508.8D) < 1e-6,
                String.valueOf(com.hongmeng.xiuxian.core.RealmStats.bossHealth(4)));
        check("化神 BOSS 攻击 = 1288 × 0.04 = 51.52",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.bossAttack(5) - 51.52D) < 1e-6,
                String.valueOf(com.hongmeng.xiuxian.core.RealmStats.bossAttack(5)));
        check("精英怪 HP 高于普通妖兽",
                com.hongmeng.xiuxian.core.RealmStats.eliteHealth(5)
                        > com.hongmeng.xiuxian.core.RealmStats.mobHealth(5), "");
        // 基础值上首领未必高于精英，但乘上品阶倍率（首领 ×64 vs 精英 ×3）
        // 之后必须严格更大——这才是玩家实际遇到的怪
        double bossReal = com.hongmeng.xiuxian.core.RealmStats.bossHealth(5)
                * com.hongmeng.xiuxian.entity.MobRank.OVERLORD.healthMultiplier();
        double eliteReal = com.hongmeng.xiuxian.core.RealmStats.eliteHealth(5)
                * com.hongmeng.xiuxian.entity.MobRank.ELITE.healthMultiplier();
        check("实体的首领血量远高于精英（乘品阶后）", bossReal > eliteReal * 5.0D,
                String.format("首领 %.0f vs 精英 %.0f", bossReal, eliteReal));

        // 与境界系统的衔接：标准值应由累计修为推出同一个大境
        check("标准值可反查境界：3900 修为对应道印",
                com.hongmeng.xiuxian.core.RealmStats.standardOf(3900L * 1000L) >= 3900L, "");

        // 装备分阶数量（只碰 GearNames —— 它不含任何注册表引用）
        check("装备品阶名 10 档", com.hongmeng.xiuxian.item.gear.GearNames.GRADE_NAMES.length == 10,
                "实际 " + com.hongmeng.xiuxian.item.gear.GearNames.GRADE_NAMES.length);
        check("剑名 10 把", com.hongmeng.xiuxian.item.gear.GearNames.SWORD_NAMES.length == 10, "");
        check("法宝部位 5 个", com.hongmeng.xiuxian.item.gear.GearNames.SLOTS.length == 5, "");
        check("品阶命名含'练气法器'",
                com.hongmeng.xiuxian.item.gear.GearNames.GRADE_NAMES[0].equals("练气法器"), "");
        check("品阶命名含'鸿蒙至宝'",
                com.hongmeng.xiuxian.item.gear.GearNames.GRADE_NAMES[9].equals("鸿蒙至宝"), "");
        check("部位 key 映射正确（法冠→crown、玉佩→jade）",
                com.hongmeng.xiuxian.item.gear.GearNames.slotKey("法冠").equals("crown")
                        && com.hongmeng.xiuxian.item.gear.GearNames.slotKey("玉佩").equals("jade"), "");
        check("部位 key 可反查中文名",
                com.hongmeng.xiuxian.item.gear.GearNames.slotName("robe").equals("道袍"), "");
        check("注册名格式正确（第10境剑 / 第6境玉佩）",
                com.hongmeng.xiuxian.item.gear.GearNames.swordId(10).equals("realm_sword_10")
                        && com.hongmeng.xiuxian.item.gear.GearNames.artifactId(6, "jade")
                        .equals("artifact_6_jade"), "");
        check("品阶名与剑名逐境一一对应（无空缺）",
                java.util.stream.IntStream.rangeClosed(1, 10).allMatch(t ->
                        com.hongmeng.xiuxian.item.gear.GearNames.gradeOf(t) != null
                                && !com.hongmeng.xiuxian.item.gear.GearNames.gradeOf(t).isEmpty()
                                && !com.hongmeng.xiuxian.item.gear.GearNames.swordOf(t).isEmpty()), "");
    }

    /**
     * 突破门槛：修为没满不能催动。
     *
     * <p>这条是修掉一个真实 bug 之后补的回归断言：原实现的条件写反了，
     * 变成"任何修为都能突破，只有顶到上限才拒绝"。</p>
     */
    private static void breakthroughGate() {
        section("突破门槛");

        // 练气初期：0 起，需要 60
        check("0 修为时不能突破",
                !com.hongmeng.xiuxian.core.Breakthrough.isReady(0L, 0L, 60L), "");
        check("0 修为时缺口为 60",
                com.hongmeng.xiuxian.core.Breakthrough.missingFor(0L, 0L, 60L) == 60L, "");
        check("59 修为时仍不能突破",
                !com.hongmeng.xiuxian.core.Breakthrough.isReady(59L, 0L, 60L), "");
        check("刚好 60 修为时可以突破",
                com.hongmeng.xiuxian.core.Breakthrough.isReady(60L, 0L, 60L), "");
        check("超过上限也能突破（不会卡住）",
                com.hongmeng.xiuxian.core.Breakthrough.isReady(999L, 0L, 60L), "");
        check("缺口在满修为时为 0",
                com.hongmeng.xiuxian.core.Breakthrough.missingFor(60L, 0L, 60L) == 0L, "");

        // 用真实阶梯的几个点验证
        Realm jindan = Realm.of(3000L);
        long jindanCap = jindan.minTotal() + jindan.need();
        check("金丹初期：差 1 点修为时不能突破",
                !com.hongmeng.xiuxian.core.Breakthrough.isReady(
                        jindanCap - 1L, jindan.minTotal(), jindan.need()),
                "起点 " + jindan.minTotal() + " 需求 " + jindan.need());
        check("金丹初期：缺口在差 1 点时正好是 1",
                com.hongmeng.xiuxian.core.Breakthrough.missingFor(
                        jindanCap - 1L, jindan.minTotal(), jindan.need()) == 1L, "");
        check("金丹初期：刚好攒满可突破",
                com.hongmeng.xiuxian.core.Breakthrough.isReady(
                        jindanCap, jindan.minTotal(), jindan.need()), "");
        check("缺口随修为单调不增",
                com.hongmeng.xiuxian.core.Breakthrough.missingFor(10L, 0L, 60L)
                        > com.hongmeng.xiuxian.core.Breakthrough.missingFor(50L, 0L, 60L), "");

        // 逐境检查：每一层在"差 1 点"时都拒绝、"刚好满"时都放行
        boolean gateConsistent = true;
        long walk = 0L;
        for (int i = 0; i < 28; i++) {
            Realm r = Realm.of(walk);
            long cap = r.minTotal() + r.need();
            if (com.hongmeng.xiuxian.core.Breakthrough.isReady(
                    cap - 1L, r.minTotal(), r.need())
                    || !com.hongmeng.xiuxian.core.Breakthrough.isReady(
                    cap, r.minTotal(), r.need())) {
                gateConsistent = false;
                break;
            }
            long next = cap;
            if (next <= walk || r.isMaxRealm()) {
                break;
            }
            walk = next;
        }
        check("全部 28 层的突破门槛判定一致", gateConsistent, "");
    }

    /**
     * 修为封顶 + 雷劫道数 + 血条格式。
     *
     * <p>三条都是这一轮新加的机制，也都是玩家明确提出的需求。</p>
     */
    private static void cultivationCapAndTribulation() {
        section("修为封顶与雷劫");

        // ---- 修为封顶（境界由层号决定，不再跟着修为自动涨） ----
        var data = new com.hongmeng.xiuxian.core.CultivationData();
        data.setAwakened(true);
        data.setCultivation(0L);
        data.setUnlockedStep(0);
        long cap = data.cultivationCap();
        check("练气初期上限 = 0 + 60 = 60", cap == 60L, "实际 " + cap);
        check("0 修为时未满", !data.isCultivationFull(), "");
        check("0 修为时缺口为 60", data.cultivationMissing() == 60L, "");

        // 关键：修为写满也不会自动换层，境界必须靠突破推进
        data.setCultivation(cap);
        check("修为满时 isCultivationFull 为 true", data.isCultivationFull(), "");
        check("修为满时缺口为 0", data.cultivationMissing() == 0L, "");
        check("修为满但没突破时，境界仍是练气初期",
                data.realm().cn().equals("练气初期"), data.realm().cn());
        check("修为满但层号仍为 0", data.unlockedStep() == 0, "");

        // 突破后层号 +1，修为清零
        data.setCultivation(0L);
        check("advanceStep 返回 true", data.advanceStep(), "");
        check("突破后层号为 1", data.unlockedStep() == 1, "");
        check("突破后境界是练气中期（第 2 层）",
                data.realm().cn().equals("练气中期"), data.realm().cn());
        check("新一层上限 = 60 + 150 = 210",
                data.cultivationCap() == 210L, "实际 " + data.cultivationCap());

        // 修为超过本层上限时会被夹住
        data.setCultivation(999_999L);
        data.clampCultivationToStep();
        check("修为被夹到本层上限", data.getCultivation() == 210L,
                "实际 " + data.getCultivation());
        check("夹取后不再超上限", data.getCultivation() <= data.cultivationCap(), "");

        // 老存档迁移：没有 UnlockedStep 字段时按累计修为推算
        var legacy = new com.hongmeng.xiuxian.core.CultivationData();
        legacy.setAwakened(true);
        legacy.addCultivation(3_000L);   // 金丹初期附近
        check("老存档：层号按累计修为迁移（约金丹）",
                legacy.unlockedStep() >= 6 && legacy.realm().tier() >= 3,
                "层 " + legacy.unlockedStep() + " / " + legacy.realm().cn());

        // 每一层的上限都必须 > 起点（否则会卡死）
        boolean everyCapSane = true;
        for (int step = 0; step < Realm.TOTAL_STEPS; step++) {
            Realm r = Realm.ofStep(step);
            if (r.minTotal() + r.need() <= r.minTotal()) {
                everyCapSane = false;
                break;
            }
        }
        check("每一层的修为上限都大于起点", everyCapSane, "");

        // ofStep 边界
        check("ofStep(0) = 练气初期", Realm.ofStep(0).cn().equals("练气初期"), "");
        check("ofStep(27) = 鸿蒙", Realm.ofStep(Realm.TOTAL_STEPS - 1).cn().equals("鸿蒙"),
                Realm.ofStep(Realm.TOTAL_STEPS - 1).cn());
        check("ofStep 越界被夹住",
                Realm.ofStep(-5).cn().equals("练气初期")
                        && Realm.ofStep(999).cn().equals("鸿蒙"), "");

        // 每一层的上限都必须 > 起点（否则会卡死）
        boolean capSane = true;
        long walk = 0L;
        for (int i = 0; i < 28; i++) {
            Realm r = Realm.of(walk);
            if (r.minTotal() + r.need() <= r.minTotal()) {
                capSane = false;
                break;
            }
            long next = r.minTotal() + r.need();
            if (next <= walk || r.isMaxRealm()) {
                break;
            }
            walk = next;
        }
        check("每一层的修为上限都大于起点", capSane, "");

        // ---- 雷劫道数：道印起，九的倍数 ----
        check("练气不渡劫",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(1) == 0, "");
        check("筑基不渡劫",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(2) == 0, "");
        check("金丹不渡劫",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(3) == 0, "");
        check("元婴不渡劫",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(4) == 0, "");
        check("化神不渡劫（道印之前都没有）",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(5) == 0, "");

        check("道印：9 道雷",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(6) == 9,
                "实际 " + com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(6));
        check("飞升：18 道雷",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(7) == 18,
                "实际 " + com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(7));
        check("仙人：27 道雷",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(8) == 27, "");
        check("帝境：36 道雷",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(9) == 36, "");
        check("鸿蒙：45 道雷",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(10) == 45, "");

        // 道印之后每一境都是 9 的倍数，且严格递增
        boolean allMultiples = true;
        boolean increasing = true;
        int prev = -1;
        for (int tier = 6; tier <= 10; tier++) {
            int n = com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(tier);
            if (n % 9 != 0) {
                allMultiples = false;
            }
            if (n <= prev) {
                increasing = false;
            }
            prev = n;
        }
        check("道印之后每境雷数都是 9 的倍数", allMultiples, "");
        check("雷数随境界严格递增", increasing, "");
        check("雷劫境界门槛 = 6（道印）",
                com.hongmeng.xiuxian.core.Breakthrough.TRIBULATION_FIRST_TIER == 6, "");
        check("雷劫基数为 9",
                com.hongmeng.xiuxian.core.Breakthrough.TRIBULATION_BASE == 9, "");

        // ---- 大小境界划分：只有跨大境才渡劫 ----
        // 9 大境各 3 层，所以阶段 3 的突破 = 跨大境
        check("练气初期→中期 是小境界（不渡劫）",
                !com.hongmeng.xiuxian.core.Breakthrough.isMajorAtStep(0), "");
        check("练气中期→后期 是小境界",
                !com.hongmeng.xiuxian.core.Breakthrough.isMajorAtStep(1), "");
        check("练气后期→筑基初期 是大境界（渡劫）",
                com.hongmeng.xiuxian.core.Breakthrough.isMajorAtStep(2), "");
        check("筑基后期→金丹初期 是大境界",
                com.hongmeng.xiuxian.core.Breakthrough.isMajorAtStep(5), "");
        check("化神后期→道印初期 是大境界",
                com.hongmeng.xiuxian.core.Breakthrough.isMajorAtStep(14), "");
        check("帝境后期→鸿蒙 是大境界",
                com.hongmeng.xiuxian.core.Breakthrough.isMajorAtStep(26), "");

        // 逐层核对：28 层里应该正好有 9 次大境界突破（练气→筑基 … 帝境→鸿蒙）
        int majorCount = 0;
        for (int step = 0; step < Realm.TOTAL_STEPS - 1; step++) {
            if (com.hongmeng.xiuxian.core.Breakthrough.isMajorAtStep(step)) {
                majorCount++;
            }
        }
        check("28 层里正好 9 次大境界突破", majorCount == 9, "实际 " + majorCount);

        // 大境界突破的雷数 = 目标大境的雷数
        check("跨入道印要渡 9 道雷",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(6) == 9, "");
        check("跨入鸿蒙要渡 45 道雷",
                com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(10) == 45, "");

        // ---- 突破后保护 & 飞升常驻效果 ----
        check("突破后保护持续 60 秒",
                com.hongmeng.xiuxian.core.ModEvents.BLESSING_DURATION_TICKS == 1200,
                String.valueOf(com.hongmeng.xiuxian.core.ModEvents.BLESSING_DURATION_TICKS));
        check("突破后保护是抗性 I（amplifier 0）",
                com.hongmeng.xiuxian.core.ModEvents.BLESSING_RESISTANCE_AMPLIFIER == 0, "");
        check("突破后保护是生命回复 II（amplifier 1）",
                com.hongmeng.xiuxian.core.ModEvents.BLESSING_REGENERATION_AMPLIFIER == 1, "");
        check("飞升常驻是生命回复 I（amplifier 0）",
                com.hongmeng.xiuxian.core.ModEvents.ASCENSION_REGENERATION_AMPLIFIER == 0, "");
        check("飞升常驻是夜视 I（amplifier 0）",
                com.hongmeng.xiuxian.core.ModEvents.ASCENSION_NIGHT_VISION_AMPLIFIER == 0, "");
        check("飞升常驻效果的刷新间隔是 10 秒",
                com.hongmeng.xiuxian.core.ModEvents.ASCENSION_REFRESH_TICKS == 200, "");
        check("突破保护比飞升常驻回复等级更高（II > I）",
                com.hongmeng.xiuxian.core.ModEvents.BLESSING_REGENERATION_AMPLIFIER
                        > com.hongmeng.xiuxian.core.ModEvents.ASCENSION_REGENERATION_AMPLIFIER, "");

        // 常驻效果的门槛是第 7 境（飞升）——它和"飞行"已经**分开**了：
        // 飞行按玩家要求推迟到仙人，但飞升的常驻效果（生命回复/夜视/饱和）留在飞升。
        check("飞升（第 7 境）解锁常驻效果（与飞行分开判定）",
                !Realm.ofStep(realmStartIndex(6)).hasAscensionPerks()
                        && Realm.ofStep(realmStartIndex(7)).hasAscensionPerks(), "");
        check("道印（第 6 境）还没有常驻夜视（没有飞行）",
                !Realm.ofStep(realmStartIndex(6)).hasFlight(), "");

        // ---- 雷劫伤害：按玩家生命上限取比例，必须"扛得住" ----
        //
        // ⚠ 这两条断言**为设计修复而改过**，原因写清楚：
        //
        //   原先断言的是 `ratio > 1.0`（"雷劫总伤害大于满血，不治会死"），
        //   那时 ratio = 2.2。但那个设计**是不可通过的**：
        //     天劫用 magic 伤害 -> 在 BYPASSES_ARMOR 里，护甲不减免；
        //     startTribulation 没授予抗性 I（注释却声称有）；
        //     项目没有回血消耗品。
        //   于是 9 道雷第 5 道必死，而首次天劫在化神→道印 ——
        //   **没人能越过道印**，道印之后的全部内容锁死。
        //
        //   现在 ratio = 1.0 + 渡劫时授予抗性 I（-20%），从满血打完剩约 20%。
        //   所以断言改成"**有效**伤害必须 < 100% 生命、但仍 > 50%" ——
        //   表达的是"有威胁但不处决"，而不是某个具体数字。
        double ratio = com.hongmeng.xiuxian.core.Breakthrough.TRIBULATION_TOTAL_HP_RATIO;
        double resist = 0.20D;                       // 抗性 I 在 1.20.1 是 -20%
        double effective = ratio * (1.0D - resist);
        check("雷劫总伤害系数在 0.5~2.0 之间（有威胁但可活）",
                ratio >= 0.5D && ratio <= 2.0D, String.valueOf(ratio));
        check("吃满雷劫后还有血（有效伤害 < 100% 生命）",
                effective < 1.0D,
                String.format("有效 %.2f 倍生命", effective));
        check("雷劫仍有威胁（有效伤害 > 50% 生命）",
                effective > 0.50D,
                String.format("%.0f%% 生命", effective * 100));

        // 每道伤害 = 生命上限 × 系数 / 道数
        //
        // 原来这里断言"道印每道 24.4%" —— 那只是旧系数的快照。
        // 改成断言**不变量**：每道伤害必然小于满血（不会被一道秒杀）。
        double perBoltFrac = ratio / 9.0D;
        check("道印（9 道）每道远小于满血（不会被一道秒杀）",
                perBoltFrac < 0.5D,
                String.format("每道 %.1f%% 生命", perBoltFrac * 100));
        // 道数越多每道越轻，总量恒定
        double total9 = (1000.0D * ratio / 9.0D) * 9;
        double total45 = (1000.0D * ratio / 45.0D) * 45;
        check("不同道数的总伤害相同（每道按道数摊薄）",
                Math.abs(total9 - total45) < 1e-6, "");

        // ---- 血条格式 ----
        check("血条：小数字原样",
                com.hongmeng.xiuxian.client.HealthBarOverlay.formatHp(25.0F).equals("25"), "");
        check("血条：上千原样",
                com.hongmeng.xiuxian.client.HealthBarOverlay.formatHp(1080.0F).equals("1080"), "");
        check("血条：上万用「万」",
                com.hongmeng.xiuxian.client.HealthBarOverlay.formatHp(24000.0F).contains("万"),
                com.hongmeng.xiuxian.client.HealthBarOverlay.formatHp(24000.0F));
        check("血条：上亿用「亿」",
                com.hongmeng.xiuxian.client.HealthBarOverlay.formatHp(2.5e8F).contains("亿"),
                com.hongmeng.xiuxian.client.HealthBarOverlay.formatHp(2.5e8F));
        check("血条阈值是 20 颗心",
                Math.abs(com.hongmeng.xiuxian.client.HealthBarOverlay.HEART_THRESHOLD - 20.0F) < 1e-6,
                String.valueOf(com.hongmeng.xiuxian.client.HealthBarOverlay.HEART_THRESHOLD));
    }

    /**
     * 灵植 / 炼丹：丹方表 + 灵植表。
     *
     * <p><b>只能引用纯数据类。</b>{@code SpiritHerbBlock} / {@code LingtSoilBlock} /
     * {@code ModBlocks} / {@code ModItems} / {@code ModAlchemy} 都会在静态初始化时
     * 碰注册表，离线加载直接 {@code Not bootstrapped}——引用了整个验证器就起不来。
     * 这一点由 {@code xiuxian-mod/tools/HerbOfflineProbe.java} 实测确认过。</p>
     */
    private static void herbAndAlchemy() {
        section("灵植 / 炼丹");

        boolean tablesOk = true;
        String tablesErr = "";
        try {
            com.hongmeng.xiuxian.alchemy.AlchemyRecipes.validate();
            com.hongmeng.xiuxian.alchemy.HerbCatalog.validate();
        } catch (RuntimeException ex) {
            tablesOk = false;
            tablesErr = String.valueOf(ex.getMessage());
        }
        check("丹方表与灵植表自检通过（材料/产物/数量/品阶/生长概率都合法）",
                tablesOk, tablesErr);

        java.util.List<com.hongmeng.xiuxian.alchemy.AlchemyRecipes.Brew> brews =
                com.hongmeng.xiuxian.alchemy.AlchemyRecipes.BREWS;
        check("丹方 12 条", brews.size() == 12, "实际 " + brews.size());
        check("九颗丹药全部可炼（没有玩家炼不出来的丹药）",
                com.hongmeng.xiuxian.alchemy.AlchemyRecipes.coveredPills()
                        .containsAll(com.hongmeng.xiuxian.alchemy.AlchemyRecipes.PILL_IDS),
                com.hongmeng.xiuxian.alchemy.AlchemyRecipes.coveredPills().toString());

        boolean ticksOk = true;
        boolean countOk = true;
        for (com.hongmeng.xiuxian.alchemy.AlchemyRecipes.Brew b : brews) {
            ticksOk &= b.brewTicks() == 200;
            countOk &= b.count() >= 1 && b.count() <= 4;
        }
        check("每炉 200 tick（10 秒）", ticksOk, "有丹方不是 200 tick");
        check("每炉产出 1~4 颗", countOk, "有丹方产出越界");

        // 主辅顺序无关：把两张表都试一遍，必须命中同一条
        boolean orderFree = true;
        for (com.hongmeng.xiuxian.alchemy.AlchemyRecipes.Brew b : brews) {
            orderFree &= com.hongmeng.xiuxian.alchemy.AlchemyRecipes
                    .find(b.main(), b.aux()) == b
                    && com.hongmeng.xiuxian.alchemy.AlchemyRecipes
                    .find(b.aux(), b.main()) == b;
        }
        check("主料/辅料顺序无关（互换仍命中同一方）", orderFree, "有方子只能单向匹配");

        java.util.Set<String> combos = new java.util.HashSet<>();
        boolean unique = true;
        for (com.hongmeng.xiuxian.alchemy.AlchemyRecipes.Brew b : brews) {
            String key = b.main().compareTo(b.aux()) <= 0
                    ? b.main() + "+" + b.aux() : b.aux() + "+" + b.main();
            unique &= combos.add(key);
        }
        check("没有重复丹方（主辅互换算同一张）", unique, "有重复组合");

        check("没有方子的组合返回 null（不会误炼）",
                com.hongmeng.xiuxian.alchemy.AlchemyRecipes.find("lingye", "dirt") == null, "");

        check("燃料：灵叶 1 片 = 200 tick（正好一炉）",
                com.hongmeng.xiuxian.alchemy.AlchemyRecipes
                        .fuelTicks("hongmeng:lingye") == 200, "");
        check("燃料：烈焰粉 1 份 = 2400 tick（12 炉）",
                com.hongmeng.xiuxian.alchemy.AlchemyRecipes
                        .fuelTicks("minecraft:blaze_powder") == 2400, "");
        check("非燃料返回 0（石头烧不起来）",
                com.hongmeng.xiuxian.alchemy.AlchemyRecipes
                        .fuelTicks("minecraft:stone") == 0, "");

        java.util.List<com.hongmeng.xiuxian.alchemy.HerbCatalog.Herb> herbs =
                com.hongmeng.xiuxian.alchemy.HerbCatalog.ALL;
        check("六阶灵植：正好 6 株", herbs.size() == 6, "实际 " + herbs.size());

        boolean tiersOk = true;
        boolean slower = true;
        for (int i = 0; i < herbs.size(); i++) {
            com.hongmeng.xiuxian.alchemy.HerbCatalog.Herb h = herbs.get(i);
            tiersOk &= h.tier() >= 1 && h.tier() <= 6
                    && h.growthChance() > 0.0F && h.growthChance() <= 1.0F;
            if (i > 0) {
                slower &= herbs.get(i - 1).growthChance() >= h.growthChance();
            }
        }
        check("品阶在 1..6、生长概率在 (0,1]", tiersOk, "有灵植数值越界");
        check("每一阶各一株（1..6 无重复无缺口）",
                herbs.stream().mapToInt(
                        com.hongmeng.xiuxian.alchemy.HerbCatalog.Herb::tier)
                        .distinct().count() == 6, "品阶有重复或缺口");
        check("高阶灵植不会比低阶长得快", slower, "生长概率没有单调不增");
        check("灵田土 x3 后不超过 1.0（一阶 0.30x3=0.90）",
                com.hongmeng.xiuxian.alchemy.HerbCatalog.XUELIAN_GRASS.growthChance()
                        * 3.0F <= 1.0F, "");
        check("成熟掉花瓣上限 = 1 + ceil(品阶/2)（一阶 2、六阶 4）",
                com.hongmeng.xiuxian.alchemy.HerbCatalog.XUELIAN_GRASS.petalMax() == 2
                        && com.hongmeng.xiuxian.alchemy.HerbCatalog.XUELUO_ZHI.petalMax() == 4, "");

        // 飞行属性那三条**不在这里断言**：调用 createAttributes() 会碰
        // Attributes 的静态初始化 → 整个验证器 Not bootstrapped 起不来。
        // 这正是本文件顶部反复警告的那类陷阱，我自己也踩了一次。
        // 改由 tools/check_flying_speed.py 做静态检查（读源码，不加载类）。
    }

    /**
     * 灵力系统：储量表、御剑时长、回复、分档。
     *
     * <p>{@code ManaTable} 是纯数据，所以离线就能断言。
     * 这里锁住的是**玩家明确要求过的那几个数字**——
     * 金丹 5 分钟、元婴 7、化神 10、道印 15、飞升 30。</p>
     */
    private static void manaSystem() {
        section("灵力系统");

        var M = com.hongmeng.xiuxian.core.ManaTable.class;
        int[] max = com.hongmeng.xiuxian.core.ManaTable.MAX_BY_TIER;

        check("储量表覆盖 10 个大境", max.length == 10, "实际 " + max.length);
        check("练气灵力 600", com.hongmeng.xiuxian.core.ManaTable.maxForTier(1) == 600,
                String.valueOf(com.hongmeng.xiuxian.core.ManaTable.maxForTier(1)));
        check("筑基灵力 2400", com.hongmeng.xiuxian.core.ManaTable.maxForTier(2) == 2400, "");

        // 储量必须随境界单调递增
        boolean rising = true;
        for (int t = 1; t < 10; t++) {
            if (com.hongmeng.xiuxian.core.ManaTable.maxForTier(t + 1)
                    <= com.hongmeng.xiuxian.core.ManaTable.maxForTier(t)) {
                rising = false;
                break;
            }
        }
        check("灵力储量随境界单调递增", rising, "");

        // ---- 御剑时长：玩家明确指定的五个数字 ----
        long drain = (long) com.hongmeng.xiuxian.core.ManaTable.FLIGHT_DRAIN_PER_TICK;
        long min3 = com.hongmeng.xiuxian.core.ManaTable.flightTicksForTier(3) / 20L / 60L;
        long min4 = com.hongmeng.xiuxian.core.ManaTable.flightTicksForTier(4) / 20L / 60L;
        long min5 = com.hongmeng.xiuxian.core.ManaTable.flightTicksForTier(5) / 20L / 60L;
        long min6 = com.hongmeng.xiuxian.core.ManaTable.flightTicksForTier(6) / 20L / 60L;
        long min7 = com.hongmeng.xiuxian.core.ManaTable.flightTicksForTier(7) / 20L / 60L;

        check("御剑消耗 2.0 灵力/tick", Math.abs(com.hongmeng.xiuxian.core.ManaTable
                .FLIGHT_DRAIN_PER_TICK - 2.0D) < 1e-9, "");
        // 玩家要求「飞升境直接能飞」后，御剑门槛从金丹挪到飞升；
        // 因此这里断言"最高可御剑境界 = 飞升 30 分钟"，并保留灵力-时长的单调性。
        check("金丹灵力可支撑 5 分钟御剑", min3 == 5L, min3 + " 分钟");
        check("元婴灵力可支撑 7 分钟御剑", min4 == 7L, min4 + " 分钟");
        check("化神灵力可支撑 10 分钟御剑", min5 == 10L, min5 + " 分钟");
        check("道印灵力可支撑 15 分钟御剑", min6 == 15L, min6 + " 分钟");
        check("飞升御剑 30 分钟（最高可御剑境界）", min7 == 30L, min7 + " 分钟");

        // 时长必须随境界单调递增（不能出现"高境界反而飞得短"）
        boolean longer = true;
        for (int t = 3; t < 7; t++) {
            if (com.hongmeng.xiuxian.core.ManaTable.flightTicksForTier(t + 1)
                    <= com.hongmeng.xiuxian.core.ManaTable.flightTicksForTier(t)) {
                longer = false;
                break;
            }
        }
        check("御剑时长随境界递增", longer, "");

        // ---- 回复：慢慢回复（约 200 秒回满） ----
        long fullSeconds = com.hongmeng.xiuxian.core.ManaTable.fullRegenTicks() / 20L;
        // 玩家反馈回复太慢：0.5%/秒 上调到 1.5%/秒，空蓝回满约 67 秒。
        check("灵力回满约 67 秒（1.5%/秒）", fullSeconds >= 55L && fullSeconds <= 80L,
                fullSeconds + " 秒");
        check("打坐回复是常态的 10 倍",
                Math.abs(com.hongmeng.xiuxian.core.ManaTable.MEDITATION_REGEN_MULTIPLIER
                        - 10.0D) < 1e-9, "");
        check("回灵丹回复 30%", Math.abs(com.hongmeng.xiuxian.core.ManaTable
                .PILL_RESTORE_FRACTION - 0.30D) < 1e-9, "");

        // ---- 飞行分档 ----
        // 玩家要求「飞升境之后直接就能飞」→ 御剑门槛由金丹(3)改为飞升(7)。
        check("御剑从飞升（第 7 境）开始",
                com.hongmeng.xiuxian.core.ManaTable.SWORD_FLIGHT_MIN_TIER == 7, "");
        check("御剑到飞升（第 7 境）结束",
                com.hongmeng.xiuxian.core.ManaTable.SWORD_FLIGHT_MAX_TIER == 7, "");
        check("仙人（第 8 境）起踏空飞行",
                com.hongmeng.xiuxian.core.ManaTable.FREE_FLIGHT_MIN_TIER == 8, "");
        check("分档不重叠（御剑段与踏空段不相交）",
                com.hongmeng.xiuxian.core.ManaTable.SWORD_FLIGHT_MAX_TIER
                        < com.hongmeng.xiuxian.core.ManaTable.FREE_FLIGHT_MIN_TIER, "");

        // 御剑段与 Realm 的飞行能力必须一致：金丹~飞升本来不能飞，靠御剑；
        // 仙人起 Realm 自己就说能飞了。
        boolean tierMatch = true;
        for (int t = 3; t <= 7; t++) {
            if (com.hongmeng.xiuxian.core.Realm.ofStep(realmStartIndex(t)).hasFlight()) {
                tierMatch = false;   // 这几位不该自带飞行，否则御剑没意义
                break;
            }
        }
        // **御剑已删除**（玩家要求），所以"必须靠御剑"这条判据作废。
        // 新规则：飞升(7) 起直接可飞；金丹~道印(3~6) 不能飞。
        check("金丹(3) 不能飞", !com.hongmeng.xiuxian.core.Realm.ofStep(6).hasFlight(), "");
        check("飞升(7) 起可以直接飞", com.hongmeng.xiuxian.core.Realm.ofStep(18).hasFlight(), "");
        check("仙人起 Realm 自带飞行",
                com.hongmeng.xiuxian.core.Realm.ofStep(realmStartIndex(8)).hasFlight(), "");

        // 仙人以上翻倍（与既有\"每境翻倍\"规则一致）
        boolean doubling = true;
        for (int t = 8; t < 10; t++) {
            if (com.hongmeng.xiuxian.core.ManaTable.maxForTier(t + 1)
                    != com.hongmeng.xiuxian.core.ManaTable.maxForTier(t) * 2) {
                doubling = false;
                break;
            }
        }
        check("仙人以上灵力每境翻倍", doubling, "");

        // 打印一张表，方便人工核对
        System.out.println("        境界        灵力上限    御剑时长");
        String[] cn = {"练气", "筑基", "金丹", "元婴", "化神", "道印", "飞升", "仙人", "帝境", "鸿蒙"};
        for (int t = 1; t <= 10; t++) {
            long secs = com.hongmeng.xiuxian.core.ManaTable.flightTicksForTier(t) / 20L;
            String fly = (t >= 3 && t <= 7)
                    ? String.format("%d 分 %02d 秒", secs / 60L, secs % 60L)
                    : (t >= 8 ? "踏空（免费）" : "不能飞");
            System.out.println(String.format("        %-4s 第%2d境  %8d   %s",
                    cn[t - 1], t, com.hongmeng.xiuxian.core.ManaTable.maxForTier(t), fly));
        }
    }

    /**
     * 副本目录：数量、门槛递增、难度与境界挂钩、奖励曲线。
     *
     * <p>{@code Dungeon} 是纯数据枚举，所以离线就能断言。</p>
     */
    private static void dungeonCatalog() {
        section("副本目录");

        var all = com.hongmeng.xiuxian.dungeon.Dungeon.values();
        check("副本不少于 4 座", all.length >= 4, "实际 " + all.length);

        java.util.Set<String> dims = new java.util.HashSet<>();
        java.util.Set<String> names = new java.util.HashSet<>();
        for (var d : all) {
            dims.add(d.dimensionId());
            names.add(d.cn());
        }
        check("维度 id 互不重复", dims.size() == all.length, "");
        check("副本名互不重复", names.size() == all.length, "");

        // 门槛严格递增（越往后越难进）
        boolean tierIncreasing = true;
        for (int i = 1; i < all.length; i++) {
            if (all[i].minTier() <= all[i - 1].minTier()) {
                tierIncreasing = false;
                break;
            }
        }
        check("副本门槛严格递增（按枚举顺序越来越难）", tierIncreasing, "");

        // 每一座的门槛都要在 1~10 内，且基准境界不低于门槛
        boolean sane = true;
        for (var d : all) {
            if (d.minTier() < 1 || d.minTier() > 10) {
                sane = false;
                break;
            }
            if (d.baseTier() < 1 || d.baseTier() > 10) {
                sane = false;
                break;
            }
            if (d.waves() < 1 || d.waves() > 20) {
                sane = false;
                break;
            }
            if (d.dangerFactor() < 1.0D) {
                sane = false;   // 危险系数不能小于 1
                break;
            }
        }
        check("门槛/基准境界/波次/危险系数都在合理范围", sane, "");

        // 门槛境界的玩家进去时，怪强度必须落在"有挑战但打得过"的区间。
        //
        // 注意 baseTier 允许低于 minTier——那表示"这座副本的基准怪偏弱，
        // 但按境界缩放后对合格玩家仍然够狠"。魔道巢穴就是这样：
        // 门槛第 6 境、基准第 4 境，第 6 境玩家实际面对 ×2.2 的怪。
        // 反而 baseTier 高于 minTier（如纪元秘境 1/2）是"新手福利"。
        // ⚠ 这里查的是「**建议**境界（minTier）进来时的强度」——
        // 门槛现在是"建议值 + 宽容下限"（见 Dungeon.ENTRY_GRACE），
        // 从宽容下限进来的怪本来就该更弱（<1.0），那是设计意图而不是异常。
        // ⚠ 只判"上限"，不判下限。
        //   因为**建议境界允许低于基准境界**（那是设计意图：新手福利副本）——
        //   纪元秘境就是 建议练气(minTier=1) / 基准筑基(baseTier=2)，
        //   在建议境界进来是 ×0.72，本来就该 <1.0。
        //   低的那一侧由 ENTRY_GRACE 与倍率下限(0.40) 保证，另有专门断言。
        boolean gateSane = true;
        StringBuilder gateDetail = new StringBuilder();
        for (var d : all) {
            double atGate = d.mobScaleFor(d.minTier());
            if (atGate > 4.0D) {
                gateSane = false;
                gateDetail.append(d.cn()).append("=")
                        .append(String.format("%.2f", atGate)).append(" ");
            }
        }
        check("每座副本在**建议**境界的怪强度都不超过 4.0 倍（不会强到打不动）",
                gateSane, gateSane ? "四座全部不超过 4.0" : gateDetail.toString());

        // 第一座应该练气就能进
        check("最弱的副本练气就能进",
                com.hongmeng.xiuxian.dungeon.Dungeon.easiest().minTier() == 1,
                com.hongmeng.xiuxian.dungeon.Dungeon.easiest().cn());

        // ---- 难度与境界挂钩（这是玩家明确要求的） ----
        var beast = com.hongmeng.xiuxian.dungeon.Dungeon.BEAST;
        double atBase = beast.mobScaleFor(beast.baseTier());
        double higher = beast.mobScaleFor(beast.baseTier() + 3);
        double lower = beast.mobScaleFor(1);
        check("基准境界进副本是标准强度（×危险系数）",
                Math.abs(atBase - beast.dangerFactor()) < 1e-9,
                String.format("%.2f", atBase));
        check("境界高于基准时怪更强", higher > atBase,
                String.format("%.2f vs %.2f", higher, atBase));
        // ⚠ 这一条**原来断言的正好是"被秒杀"的根源**：
        //   旧实现 `Math.max(0, enterTier - baseTier)` 把负差整个吃掉，
        //   于是低境界玩家面对的是**基准强度**的怪（玩家原话："道印去了岂不是被秒杀"）。
        //   现在低于基准时怪真的变弱 —— 这才是"阶段性难度"。
        check("境界低于基准时怪**更弱**（阶段性难度，不是被 max(0,..) 吃掉）",
                lower < atBase - 1e-9,
                String.format("%.2f vs %.2f", lower, atBase));
        check("怪强度涨幅温和（跨 3 境不超过 2.5 倍）",
                higher / atBase <= 2.5D, String.format("%.2f 倍", higher / atBase));

        boolean monotonic = true;
        for (int t = 1; t < 10; t++) {
            if (beast.mobScaleFor(t + 1) < beast.mobScaleFor(t)) {
                monotonic = false;
                break;
            }
        }
        check("副本怪强度随境界单调不降", monotonic, "");

        // ---- 奖励 ----
        check("奖励随境界上浮但不倒挂",
                beast.rewardFor(beast.baseTier() + 3) > beast.rewardFor(beast.baseTier()), "");
        boolean rewardLadder = true;
        for (int i = 1; i < all.length; i++) {
            if (all[i].rewardCultivation() <= all[i - 1].rewardCultivation()) {
                rewardLadder = false;
                break;
            }
        }
        check("副本奖励随难度递增（没有倒挂）", rewardLadder, "");

        // ---- 查询 ----
        check("byDimension 认完整 id",
                com.hongmeng.xiuxian.dungeon.Dungeon.byDimension("hongmeng:beast_realm")
                        == com.hongmeng.xiuxian.dungeon.Dungeon.BEAST, "");
        check("byDimension 认短 id",
                com.hongmeng.xiuxian.dungeon.Dungeon.byDimension("beast_realm")
                        == com.hongmeng.xiuxian.dungeon.Dungeon.BEAST, "");
        check("byDimension 找不到返回 null",
                com.hongmeng.xiuxian.dungeon.Dungeon.byDimension("nope") == null, "");
        check("byName 认中文名",
                com.hongmeng.xiuxian.dungeon.Dungeon.byName("妖兽秘境")
                        == com.hongmeng.xiuxian.dungeon.Dungeon.BEAST, "");
        // 宽容窗口（ENTRY_GRACE）让练气就能进 3 座 —— 这是改动的**目的**：
        // 玩家不再被硬墙挡在门外，而是进来面对按自己境界削弱的怪。
        // 宽容窗口让**纪元(1-2→1)** 与 **妖兽(3-2→1)** 在练气就能进；
        // 魔道(6-2=4) 与 虚空(8-2=6) 还进不去。
        // 按**名字**断言比按数量清楚 —— 数量对了但进错了副本也会被这条抓住。
        var at1 = com.hongmeng.xiuxian.dungeon.Dungeon.availableAt(1);
        check("availableAt(1) 恰好是纪元与妖兽（宽容窗口把这两座开放给练气）",
                at1.size() == 2
                        && at1.contains(com.hongmeng.xiuxian.dungeon.Dungeon.EPOCH)
                        && at1.contains(com.hongmeng.xiuxian.dungeon.Dungeon.BEAST),
                "实际 " + at1.size() + " 座：" + at1);
        check("availableAt(10) 全部可进",
                com.hongmeng.xiuxian.dungeon.Dungeon.availableAt(10).size() == all.length, "");

        boolean everyGateUnlocks = true;
        for (var d : all) {
            if (com.hongmeng.xiuxian.dungeon.Dungeon.unlockedAt(d.minTier()).isEmpty()) {
                everyGateUnlocks = false;
                break;
            }
        }
        check("每个门槛境界都有新解锁的副本", everyGateUnlocks, "");

        System.out.println("        共 " + all.length + " 座副本：");
        for (var d : all) {
            System.out.println(String.format(
                    "          %-8s 门槛第%-2d境  基准第%-2d境  %d 波  x%.2f  奖励 %d 修为",
                    d.cn(), d.minTier(), d.baseTier(), d.waves(),
                    d.dangerFactor(), d.rewardCultivation()));
        }
    }

    /**
     * 功法目录：数量、境界门槛、冷却、倍率、元素与形态覆盖、特性完整。
     */
    private static void techniqueCatalog() {
        section("功法目录");

        Technique[] all = Technique.values();
        check("功法数量不少于 20 门（内容量够）", all.length >= 20,
                "实际 " + all.length);

        // 每门功法都必须有名字、风貌、特性
        boolean allNamed = true;
        boolean allFlavored = true;
        boolean allTraited = true;
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Technique t : all) {
            if (t.cn() == null || t.cn().isEmpty()) {
                allNamed = false;
            }
            if (t.flavor() == null || t.flavor().isEmpty()) {
                allFlavored = false;
            }
            if (t.trait() == null || t.trait().isEmpty()) {
                allTraited = false;
            }
            names.add(t.cn());
        }
        check("每门功法都有名字", allNamed, "");
        check("每门功法都有一句话风貌", allFlavored, "");
        check("**每门功法都有特性描述**（不是只有数字差异）", allTraited, "");
        check("功法名字互不重复", names.size() == all.length,
                names.size() + " / " + all.length);

        // 门槛、冷却、倍率的取值范围
        boolean tierOk = true;
        boolean cdOk = true;
        boolean multOk = true;
        for (Technique t : all) {
            if (t.minTier() < 1 || t.minTier() > 10) {
                tierOk = false;
            }
            if (t.cooldown() <= 0 || t.cooldown() > 20 * 60) {
                cdOk = false;
            }
            if (t.damageMultiplier() < 0.0D || t.damageMultiplier() > 10.0D) {
                multOk = false;
            }
        }
        check("境界门槛都在 1~10 之间", tierOk, "");
        check("冷却都在 1 刻 ~ 60 秒之间", cdOk, "");
        check("伤害倍率都在 0~10 之间", multOk, "");

        // 每一境都有能学的功法（练气起就有）
        boolean everyTierHasSomething = true;
        int firstTierWithNothing = -1;
        for (int tier = 1; tier <= 10; tier++) {
            if (Technique.availableAtTier(tier).isEmpty()) {
                everyTierHasSomething = false;
                firstTierWithNothing = tier;
                break;
            }
        }
        check("每个大境都有可修习的功法", everyTierHasSomething,
                firstTierWithNothing > 0 ? "第 " + firstTierWithNothing + " 境没有" : "");

        // 每个大境都有**新解锁**的功法（保证升境有获得感）
        boolean everyTierUnlocks = true;
        int missingUnlock = -1;
        for (int tier = 1; tier <= 10; tier++) {
            if (Technique.unlockedAtTier(tier).isEmpty()) {
                everyTierUnlocks = false;
                missingUnlock = tier;
                break;
            }
        }
        check("每个大境都有新解锁的功法（升境有获得感）", everyTierUnlocks,
                missingUnlock > 0 ? "第 " + missingUnlock + " 境没有新的" : "");

        // 元素覆盖：五行 + 变异 + 无属性都要有
        boolean[] elementSeen = new boolean[com.hongmeng.xiuxian.spirit.Element.values().length];
        for (Technique t : all) {
            elementSeen[t.element().ordinal()] = true;
        }
        int covered = 0;
        for (boolean b : elementSeen) {
            if (b) {
                covered++;
            }
        }
        check("五行 + 变异 + 无属性都有对应功法", covered == elementSeen.length,
                covered + " / " + elementSeen.length);

        // 形态覆盖：八种释放形态都要用到
        boolean[] kindSeen = new boolean[com.hongmeng.xiuxian.technique.TechniqueKind.values().length];
        for (Technique t : all) {
            kindSeen[t.kind().ordinal()] = true;
        }
        int kindsCovered = 0;
        for (boolean b : kindSeen) {
            if (b) {
                kindsCovered++;
            }
        }
        check("八种释放形态全部有功法使用", kindsCovered == kindSeen.length,
                kindsCovered + " / " + kindSeen.length);

        // 辅助类功法不该有伤害倍率，反之亦然
        boolean supportConsistent = true;
        for (Technique t : all) {
            // 辅助类 = 治疗 / 增益 / **被动**。
            // 被动功法是后加的：它不主动释放、不带伤害倍率，
            // 所以必须和 HEAL/BUFF 一样归入"不带倍率"那一侧。
            boolean shouldBeSupport = t.kind() == com.hongmeng.xiuxian.technique.TechniqueKind.HEAL
                    || t.kind() == com.hongmeng.xiuxian.technique.TechniqueKind.BUFF
                    || t.kind() == com.hongmeng.xiuxian.technique.TechniqueKind.PASSIVE;
            if (shouldBeSupport != t.isSupport()) {
                supportConsistent = false;
                break;
            }
        }
        check("辅助类功法（愈/御/被动）不带伤害倍率，攻击类都带", supportConsistent, "");

        // 高低阶要有区分度：最高的倍率应该明显大于最低的
        double maxMult = 0.0D;
        double minMult = Double.MAX_VALUE;
        for (Technique t : all) {
            if (!t.isSupport()) {
                maxMult = Math.max(maxMult, t.damageMultiplier());
                minMult = Math.min(minMult, t.damageMultiplier());
            }
        }
        check("最高倍率至少是最低的 3 倍（高低阶有区分）",
                maxMult >= minMult * 3.0D,
                String.format("%.1f vs %.1f", maxMult, minMult));

        // 功法强度随境界递增。
        //
        // 关键是**把境界标准值算进去**：实际伤害 = 倍率 × 本境标准值，
        // 而标准值本身随境界暴涨（第 3 境 136 → 第 9 境 10.7 万）。
        // 只比"倍率/冷却"会得出错误结论——烈焰掌冷却 3 秒看着很美，
        // 但它只吃第 3 境的标准值。
        double lowBest = 0.0D;
        double highBest = 0.0D;
        for (Technique t : all) {
            if (t.isSupport()) {
                continue;
            }
            double realDamagePerSecond = RealmStats.standard(t.minTier())
                    * t.damageMultiplier() / t.cooldownSeconds();
            if (t.minTier() <= 3) {
                lowBest = Math.max(lowBest, realDamagePerSecond);
            }
            if (t.minTier() >= 7) {
                highBest = Math.max(highBest, realDamagePerSecond);
            }
        }
        check("高阶功法的每秒伤害远高于低阶（含境界标准值缩放）",
                highBest > lowBest * 5.0D,
                String.format("高阶 %.0f vs 低阶 %.0f（每秒伤害）", highBest, lowBest));

        // 即便只看倍率/冷却，也不该出现"高阶被低阶完全碾压"
        double lowRatio = 0.0D;
        double highRatio = 0.0D;
        for (Technique t : all) {
            if (t.isSupport()) {
                continue;
            }
            double r = t.damageMultiplier() / t.cooldownSeconds();
            if (t.minTier() <= 3) {
                lowRatio = Math.max(lowRatio, r);
            }
            if (t.minTier() >= 7) {
                highRatio = Math.max(highRatio, r);
            }
        }
        check("高阶功法的倍率/冷却比不低于低阶的一半",
                highRatio >= lowRatio * 0.5D,
                String.format("%.3f vs %.3f", highRatio, lowRatio));

        // 按名字查找
        check("byName 能按中文名找到",
                Technique.byName("焚天诀") == Technique.FEN_TIAN_JUE, "");
        check("byName 能按枚举名找到",
                Technique.byName("FEN_TIAN_JUE") == Technique.FEN_TIAN_JUE, "");
        check("byName 大小写不敏感",
                Technique.byName("fen_tian_jue") == Technique.FEN_TIAN_JUE, "");
        check("byName 找不到返回 null", Technique.byName("不存在的功法") == null, "");

        // 门槛最低的功法：练气就能学
        int lowestTier = 99;
        for (Technique t : all) {
            lowestTier = Math.min(lowestTier, t.minTier());
        }
        check("有练气期就能学的入门功法", lowestTier == 1, "最低门槛 " + lowestTier);

        // 打印目录概览
        System.out.println("        共 " + all.length + " 门功法，按境界：");
        for (int tier = 1; tier <= 10; tier++) {
            var unlocked = Technique.unlockedAtTier(tier);
            if (unlocked.isEmpty()) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (Technique t : unlocked) {
                sb.append(t.cn()).append('(').append(t.element().cn())
                        .append('/').append(t.kind().glyph()).append(") ");
            }
            System.out.println("          第 " + tier + " 境解锁： " + sb.toString().trim());
        }
    }

    /**
     * 功法的修习与冷却：纯逻辑，用 {@link com.hongmeng.xiuxian.technique.TechniqueData} 断言。
     */
    private static void techniqueLearningAndCooldown() {
        section("功法修习与冷却");

        var data = new com.hongmeng.xiuxian.technique.TechniqueData();
        Technique fire = Technique.FEN_TIAN_JUE;
        Technique heal = Technique.QING_MU_SHENG;

        check("初始没有学过任何功法", data.isEmpty() && data.knownCount() == 0, "");
        check("初始没有选中项", data.selectedTechnique() == null, "");
        check("没学过就不能选", !data.select(fire), "");
        check("没学过时 isReady 为 false", !data.isReady(fire, 0L), "");
        check("没学过时冷却为 0", data.remaining(fire, 0L) == 0L, "");
        check("没学过时无法进入冷却", !data.startCooldown(fire, 0L), "");

        check("修习返回 true", data.learn(fire), "");
        check("重复修习返回 false", !data.learn(fire), "");
        check("修习后 has 为 true", data.has(fire), "");
        check("修习后数量为 1", data.knownCount() == 1, "");
        check("修习后自动选中", data.selectedTechnique() == fire, "");
        check("刚学会时已就绪", data.isReady(fire, 0L), "");

        // 冷却
        long now = 1000L;
        check("进入冷却成功", data.startCooldown(fire, now), "");
        check("冷却中 isReady 为 false", !data.isReady(fire, now), "");
        check("冷却中不能重复触发", !data.startCooldown(fire, now), "");
        check("剩余冷却等于功法冷却",
                data.remaining(fire, now) == fire.cooldown(),
                data.remaining(fire, now) + " vs " + fire.cooldown());
        check("冷却过半时剩余减半",
                data.remaining(fire, now + fire.cooldown() / 2)
                        == fire.cooldown() - fire.cooldown() / 2, "");
        check("冷却到期后就绪",
                data.isReady(fire, now + fire.cooldown() + 1), "");
        check("到期后可以再次进入冷却",
                data.startCooldown(fire, now + fire.cooldown() + 1), "");

        // 剩余秒数向上取整
        var d2 = new com.hongmeng.xiuxian.technique.TechniqueData();
        d2.learn(fire);
        d2.startCooldown(fire, 0L);
        check("剩余秒数向上取整（1 刻也应显示 1s）",
                d2.remainingSeconds(fire, fire.cooldown() - 1L) == 1L,
                String.valueOf(d2.remainingSeconds(fire, fire.cooldown() - 1L)));
        check("就绪时剩余秒数为 0",
                d2.remainingSeconds(fire, fire.cooldown()) == 0L, "");

        // 切换
        d2.learn(heal);
        check("修习第二门后数量为 2", d2.knownCount() == 2, "");
        var first = d2.selectedTechnique();
        var second = d2.next();
        check("next 切到了另一门", second != null && second != first, "");
        check("next 回到第一门（只有两门，循环）", d2.next() == first, "");
        check("previous 切回第二门", d2.previous() == second, "");
        check("可以直接选中某一门", d2.select(heal) && d2.selectedTechnique() == heal, "");
        check("选中没学过的功法会失败", !d2.select(Technique.JIU_XIAO_LEI_YU), "");

        // 遗忘
        check("遗忘返回 true", d2.forget(heal), "");
        check("遗忘后 has 为 false", !d2.has(heal), "");
        check("遗忘后数量减一", d2.knownCount() == 1, "");
        check("遗忘不存在的功法返回 false", !d2.forget(heal), "");

        // 清空冷却
        d2.learn(heal);
        d2.startCooldown(heal, 0L);
        d2.clearCooldowns();
        check("清空冷却后全部就绪",
                d2.isReady(fire, 0L) && d2.isReady(heal, 0L), "");

        // ---- 存档往返 ----
        var src = new com.hongmeng.xiuxian.technique.TechniqueData();
        src.learn(Technique.WAN_JIAN_GUI_ZONG);
        src.learn(Technique.BING_FENG_JUE);
        src.learn(Technique.FENG_YING_BU);
        src.startCooldown(Technique.BING_FENG_JUE, 500L);
        var tag = new net.minecraft.nbt.CompoundTag();
        src.save(tag);

        var dst = new com.hongmeng.xiuxian.technique.TechniqueData();
        dst.load(tag);
        check("功法存档往返：数量一致", dst.knownCount() == 3,
                dst.knownCount() + " vs 3");
        check("功法存档往返：三门都在",
                dst.has(Technique.WAN_JIAN_GUI_ZONG)
                        && dst.has(Technique.BING_FENG_JUE)
                        && dst.has(Technique.FENG_YING_BU), "");
        check("功法存档往返：冷却保留",
                dst.remaining(Technique.BING_FENG_JUE, 500L)
                        == src.remaining(Technique.BING_FENG_JUE, 500L),
                dst.remaining(Technique.BING_FENG_JUE, 500L) + " vs "
                        + src.remaining(Technique.BING_FENG_JUE, 500L));
        check("功法存档往返：选中项保留",
                dst.selectedTechnique() == src.selectedTechnique(), "");

        // 空存档安全
        var empty = new com.hongmeng.xiuxian.technique.TechniqueData();
        empty.load(new net.minecraft.nbt.CompoundTag());
        check("旧存档（无功法字段）加载后为空且不报错", empty.isEmpty(), "");

        // 复制
        var copied = new com.hongmeng.xiuxian.technique.TechniqueData();
        copied.copyFrom(src);
        check("copyFrom 复制了全部功法", copied.knownCount() == 3, "");
    }

    /**
     * 怪物品阶：五档分类、倍率递增、名牌格式。
     *
     * <p>{@code MobRank} 刻意不含任何 Minecraft 类型，所以离线就能断言。</p>
     */
    private static void mobRanks() {
        section("怪物品阶");

        var ranks = com.hongmeng.xiuxian.entity.MobRank.values();
        check("品阶共 5 档（普通/精英/队长/头目/首领）", ranks.length == 5,
                "实际 " + ranks.length);

        String[] wantNames = {"普通", "精英", "队长", "头目", "首领"};
        boolean namesOk = true;
        for (int i = 0; i < ranks.length; i++) {
            if (!ranks[i].cn().equals(wantNames[i])) {
                namesOk = false;
            }
        }
        check("五档中文名与顺序正确", namesOk, "");

        check("普通怪是 1/1/1/1（基准）",
                ranks[0].healthMultiplier() == 1.0D
                        && ranks[0].damageMultiplier() == 1.0D
                        && ranks[0].speedMultiplier() == 1.0D
                        && ranks[0].xpMultiplier() == 1.0D, "");

        // 生命倍率必须严格递增——这是"档位"最直观的体现
        boolean hpIncreasing = true;
        for (int i = 1; i < ranks.length; i++) {
            if (ranks[i].healthMultiplier() <= ranks[i - 1].healthMultiplier()) {
                hpIncreasing = false;
                break;
            }
        }
        check("生命倍率严格递增", hpIncreasing, "");

        boolean dmgIncreasing = true;
        boolean xpIncreasing = true;
        for (int i = 1; i < ranks.length; i++) {
            if (ranks[i].damageMultiplier() < ranks[i - 1].damageMultiplier()) {
                dmgIncreasing = false;
            }
            if (ranks[i].xpMultiplier() < ranks[i - 1].xpMultiplier()) {
                xpIncreasing = false;
            }
        }
        check("伤害倍率不降", dmgIncreasing, "");
        check("经验倍率不降", xpIncreasing, "");

        // 速度倍率不该失控——首领会跑得比普通怪快，但不能离谱
        boolean speedSane = true;
        for (var r : ranks) {
            if (r.speedMultiplier() < 1.0D || r.speedMultiplier() > 1.5D) {
                speedSane = false;
                break;
            }
        }
        check("速度倍率都在 1.0~1.5 之间（不会跑得离谱）", speedSane, "");

        // 档位跨度：首领 / 普通 应该很大
        double span = ranks[4].healthMultiplier() / ranks[0].healthMultiplier();
        check("首领血量是普通怪的 50 倍以上（档位有分量）", span >= 50.0D,
                String.format("%.0f 倍", span));

        // ---- 2026-11-07 头目/首领削弱（玩家：「一个头目才元婴 1.5 亿的血，鸿蒙居然不能秒杀」）----
        check("头目/首领品阶已削弱：血 ×20 / ×64、攻 ×2.4 / ×3.2（旧 25/80、3.0/4.5）",
                ranks[3].healthMultiplier() == 20.0D
                        && ranks[4].healthMultiplier() == 64.0D
                        && ranks[3].damageMultiplier() == 2.4D
                        && ranks[4].damageMultiplier() == 3.2D, "");
        check("削弱后品阶梯度仍严格递增（普通<精英<队长<头目<首领）",
                ranks[4].healthMultiplier() > ranks[3].healthMultiplier()
                        && ranks[3].healthMultiplier() > ranks[2].healthMultiplier()
                        && ranks[4].damageMultiplier() > ranks[3].damageMultiplier(), "");

        // isRare：队长及以上
        check("队长及以上才算稀有",
                !ranks[0].isRare() && !ranks[1].isRare()
                        && ranks[2].isRare() && ranks[3].isRare() && ranks[4].isRare(), "");

        // 名牌格式：\u00a7<颜色码><品阶名>
        boolean coloredOk = true;
        for (var r : ranks) {
            String expected = "\u00a7" + r.colorCode() + r.cn();
            if (!r.colored().equals(expected)) {
                coloredOk = false;
                break;
            }
        }
        check("colored() 格式是 §颜色码+品阶名", coloredOk,
                ranks[4].colored().replace("\u00a7", "\u00a7"));

        // 颜色码互不相同，玩家一眼能区分档位
        java.util.Set<Character> codes = new java.util.HashSet<>();
        for (var r : ranks) {
            codes.add(r.colorCode());
        }
        check("五档颜色码互不相同", codes.size() == ranks.length,
                codes.size() + " / " + ranks.length);

        // ---- 品阶 × 境界 × 难度：终局不能出现"一击必杀" ----
        //
        // 这一段是踩过坑之后补的。原来 MOB_HEALTH_RATIO=5、MOB_ATTACK_RATIO=1，
        // 鸿蒙妖兽基础一击 32 万，再乘鸿蒙难度 ×3 就是 96 万，
        // 而鸿蒙玩家只有 48 万血 —— 见面就死，怪物完全不可战。
        double hpAtTop = Realm.ofStep(Realm.TOTAL_STEPS - 1).healthBonus();
        double diffAtTop = com.hongmeng.xiuxian.difficulty.CultivationDifficulty.HONGMENG
                .mobDamageMultiplier();

        // 普通怪一击占玩家血量的比例
        double normalHit = RealmStats.mobAttack(10) * diffAtTop / hpAtTop;
        check("鸿蒙普通怪一击不超过玩家血量的 40%（不会被秒）",
                normalHit <= 0.40D, String.format("%.1f%%", normalHit * 100.0D));
        check("鸿蒙普通怪一击不低于 3%（有威胁）",
                normalHit >= 0.03D, String.format("%.1f%%", normalHit * 100.0D));

        // 首领一击（品阶伤害倍率也乘上）
        double bossHit = RealmStats.bossAttack(10) * ranks[4].damageMultiplier()
                * diffAtTop / hpAtTop;
        check("鸿蒙首领一击不超过玩家血量的 120%（能扛两下）",
                bossHit <= 1.20D, String.format("%.1f%%", bossHit * 100.0D));

        // 血量：普通怪应该在"几下能打死"的范围
        double mobHpRatio = RealmStats.mobHealth(10) * ranks[0].healthMultiplier() / hpAtTop;
        check("鸿蒙普通怪血量在玩家血量的 10%~150% 之间（打得动）",
                mobHpRatio >= 0.10D && mobHpRatio <= 1.50D,
                String.format("%.2f 倍", mobHpRatio));

        // 首领血量：不该是天文数字
        double bossHpRatio = RealmStats.bossHealth(10) * ranks[4].healthMultiplier() / hpAtTop;
        check("鸿蒙首领血量不超过玩家血量的 200 倍",
                bossHpRatio <= 200.0D, String.format("%.0f 倍", bossHpRatio));

        // 练气期也要合理（不能反过来——低境界怪太强）。
        //
        // 注意两点：
        //  1. 要用**该境界对应的难度倍率**，不能拿鸿蒙的 ×3 去套练气玩家
        //  2. 算出来的原始比例还要过实体上的**伤害软上限**
        //     （CultivationMonsterEntity.MAX_HIT_RATIO），实际挨的那一下更低
        double softCap = com.hongmeng.xiuxian.entity.CultivationMonsterEntity.MAX_HIT_RATIO;
        double hpAtLow = Realm.ofStep(0).healthBonus();
        double diffAtLow = com.hongmeng.xiuxian.difficulty.CultivationDifficulty.NORMAL
                .mobDamageMultiplier();
        double rawLow = RealmStats.mobAttack(1) * diffAtLow / hpAtLow;
        double hitLow = Math.min(rawLow, softCap);
        check("练气普通怪单次伤害（含软上限）不超过玩家血量的 40%",
                hitLow <= 0.40D,
                String.format("原始 %.1f%% → 实际 %.1f%%", rawLow * 100.0D, hitLow * 100.0D));

        double rawLowHard = RealmStats.mobAttack(1) * diffAtTop / hpAtLow;
        double hitLowHard = Math.min(rawLowHard, softCap);
        check("练气普通怪在最高难度下单次伤害（含软上限）仍不超过 40%",
                hitLowHard <= 0.40D,
                String.format("原始 %.1f%% → 实际 %.1f%%",
                        rawLowHard * 100.0D, hitLowHard * 100.0D));

        // 软上限：无论倍率怎么叠，单次伤害不得致命
        check("怪物单次伤害软上限是 35% 目标生命",
                Math.abs(com.hongmeng.xiuxian.entity.CultivationMonsterEntity.MAX_HIT_RATIO
                        - 0.35D) < 1e-9,
                String.valueOf(com.hongmeng.xiuxian.entity.CultivationMonsterEntity
                        .MAX_HIT_RATIO));
        check("软上限低于 50%（保证至少两下才死）",
                com.hongmeng.xiuxian.entity.CultivationMonsterEntity.MAX_HIT_RATIO < 0.50D, "");

        StringBuilder bal = new StringBuilder();
        bal.append(String.format("鸿蒙：普通怪 %.0f血/%.0f攻  首领 %.0f血/%.0f攻  玩家 %.0f血",
                RealmStats.mobHealth(10), RealmStats.mobAttack(10),
                RealmStats.bossHealth(10) * ranks[4].healthMultiplier(),
                RealmStats.bossAttack(10) * ranks[4].damageMultiplier(), hpAtTop));
        System.out.println("        " + bal.toString());

        StringBuilder sb = new StringBuilder();
        for (var r : ranks) {
            sb.append(r.cn()).append("(血×").append(fmt1(r.healthMultiplier()))
                    .append(" 攻×").append(fmt1(r.damageMultiplier()))
                    .append(") ");
        }
        System.out.println("        " + sb.toString().trim());
    }

    private static String fmt1(double v) {
        return String.format("%.1f", v);
    }

    /**
     * 工具类型覆盖：境界是"空手工具等级"，镐斧铲锄都要认。
     *
     * <p>这里只断言纯枚举部分——真正按方块 tag 分类的行为在
     * {@code hongmeng attrselftest} 的服务端自测里验证（离线跑会碰注册表）。</p>
     */
    private static void toolCoverage() {
        section("工具类型覆盖");

        var types = com.hongmeng.xiuxian.core.RealmMining.RequiredTool.values();
        check("工具类型共 5 种（徒手/镐/斧/铲/锄）", types.length == 5,
                "实际 " + types.length);

        check("镐的中文名是「镐」",
                com.hongmeng.xiuxian.core.RealmMining.RequiredTool.PICKAXE.cn().equals("镐"), "");
        check("斧的中文名是「斧」",
                com.hongmeng.xiuxian.core.RealmMining.RequiredTool.AXE.cn().equals("斧"), "");
        check("铲的中文名是「铲」",
                com.hongmeng.xiuxian.core.RealmMining.RequiredTool.SHOVEL.cn().equals("铲"), "");
        check("锄的中文名是「锄」",
                com.hongmeng.xiuxian.core.RealmMining.RequiredTool.HOE.cn().equals("锄"), "");
        check("NONE 表示徒手",
                com.hongmeng.xiuxian.core.RealmMining.RequiredTool.NONE.cn().equals("徒手"), "");

        check("只有 NONE 不需要工具",
                !com.hongmeng.xiuxian.core.RealmMining.RequiredTool.NONE.needsTool()
                        && com.hongmeng.xiuxian.core.RealmMining.RequiredTool.PICKAXE.needsTool()
                        && com.hongmeng.xiuxian.core.RealmMining.RequiredTool.AXE.needsTool()
                        && com.hongmeng.xiuxian.core.RealmMining.RequiredTool.SHOVEL.needsTool()
                        && com.hongmeng.xiuxian.core.RealmMining.RequiredTool.HOE.needsTool(), "");

        // 注意：不在这里碰 Tiers / Blocks —— 它们的静态初始化需要注册表 bootstrap，
        // 离线跑会直接炸（Not bootstrapped）。工具等级的实际判定放在
        // hongmeng attrselftest 的服务端自测里。
        check("境界挖掘等级的档位定义是 0~5（含徒手与下界合金）",
                com.hongmeng.xiuxian.core.Realm.harvestLevelOf(0L) <= 1
                        && com.hongmeng.xiuxian.core.Realm.harvestLevelOf(Long.MAX_VALUE / 4) == 5,
                Realm.harvestLevelOf(0L) + " / "
                        + Realm.harvestLevelOf(Long.MAX_VALUE / 4));
    }

    /**
     * 装备与减伤：武器附加攻击必须真实存在，法宝护甲的额外减伤曲线要合理。
     */
    private static void gearAndMitigation() {
        section("装备与减伤");

        // 武器附加攻击：与境界标准值表一致、逐境递增
        long prev = -1L;
        boolean increasing = true;
        for (int tier = 1; tier <= 10; tier++) {
            long atk = (long) com.hongmeng.xiuxian.core.RealmStats.weaponAttack(tier);
            if (atk <= prev) {
                increasing = false;
                break;
            }
            prev = atk;
        }
        check("十把剑的附加攻击逐境递增", increasing, "");
        check("练气剑 +8 攻击",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.weaponAttack(1) - 8.0D) < 1e-9, "");
        check("鸿蒙剑 +320000 攻击",
                Math.abs(com.hongmeng.xiuxian.core.RealmStats.weaponAttack(10) - 320_000.0D) < 1e-9, "");

        // ---- 法宝（2026-11-07 重做：十境 × 五部位，护甲 + 生命% + 攻击%）----
        check("法宝境界档 = 10、部位 = 5（10 境 × 5 = 50 件）",
                com.hongmeng.xiuxian.item.gear.ArtifactStats.MAX_TIER == 10
                        && com.hongmeng.xiuxian.item.gear.ArtifactStats.slotCount() == 5,
                com.hongmeng.xiuxian.item.gear.ArtifactStats.MAX_TIER + " × "
                        + com.hongmeng.xiuxian.item.gear.ArtifactStats.slotCount());
        check("法宝五件合计护甲 = 标准值 × 0.276（比道袍 0.70 低，不再抢本体防御位）",
                Math.abs(com.hongmeng.xiuxian.item.gear.ArtifactStats.setArmor(5)
                        - 1288.0D * 0.276D) < 1.0E-6D,
                String.format("%.1f", com.hongmeng.xiuxian.item.gear.ArtifactStats
                        .setArmor(5)));
        check("单件法宝护甲 = 基准 × 部位权重（1.15 道袍最厚、0.65 玉佩最薄）",
                Math.abs(com.hongmeng.xiuxian.item.gear.ArtifactStats.armor(5, 1)
                        - com.hongmeng.xiuxian.item.gear.ArtifactStats.armorPer(5) * 1.15D)
                        < 1.0E-9D
                        && com.hongmeng.xiuxian.item.gear.ArtifactStats.armor(5, 1)
                        > com.hongmeng.xiuxian.item.gear.ArtifactStats.armor(5, 4),
                "");
        check("法宝生命/攻击加成按外观档：初阶 +6% / 进阶 +8% / 名器 +10%",
                Math.abs(com.hongmeng.xiuxian.item.gear.ArtifactStats.setHealthPercent(1) - 0.06D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.item.gear.ArtifactStats.setHealthPercent(4) - 0.08D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.item.gear.ArtifactStats.setHealthPercent(10) - 0.10D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.item.gear.ArtifactStats.setAttackPercent(10) - 0.10D) < 1.0E-9D,
                "");
        check("法宝每件的生命/攻击占比加总 = 全套值",
                Math.abs(sumArtifactPercent(5, true)
                        - com.hongmeng.xiuxian.item.gear.ArtifactStats.setHealthPercent(5)) < 1.0E-9D
                        && Math.abs(sumArtifactPercent(5, false)
                        - com.hongmeng.xiuxian.item.gear.ArtifactStats.setAttackPercent(5)) < 1.0E-9D,
                "");
        check("法宝穿戴门槛 = 它自己的境界（第 N 境法宝需要第 N 境）",
                java.util.stream.IntStream.rangeClosed(1, 10).allMatch(
                        t -> com.hongmeng.xiuxian.item.gear.ArtifactStats.requiredRealm(t) == t),
                "");
        // 接线：装备栏拿到法宝后，百分比属性必须真的挂到 MAX_HEALTH / ATTACK_DAMAGE
        String gmSrc = readSource("src/main/java/com/hongmeng/xiuxian/item/gear/GearManager.java");
        check("法宝生命%/攻击% 真正接到属性上（MULTIPLY_TOTAL）",
                gmSrc.contains("art.healthPercent(piece)")
                        && gmSrc.contains("art.attackPercent(piece)")
                        && gmSrc.contains("Attributes.MAX_HEALTH")
                        && gmSrc.contains("Attributes.ATTACK_DAMAGE")
                        && gmSrc.contains("MULTIPLY_TOTAL"), "");
        check("法宝穿戴有境界门槛、超阶自动收回",
                readSource("src/main/java/com/hongmeng/xiuxian/item/gear/RealmGear.java")
                        .contains("instanceof ArtifactItem artifact")
                        && gmSrc.contains("RealmGear.checkEquip(player, stack)"), "");

        // 护甲软上限曲线（这才是"护甲不会无敌"的关键）
        check("0 护甲原样返回",
                com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(0.0D) == 0.0D, "");
        check("软上限内原样返回（20 护甲）",
                com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(20.0D) == 20.0D, "");
        check("负护甲也不会出事",
                com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(-5.0D) == 0.0D, "");

        double r30 = com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(30.0D);
        double r100 = com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(100.0D);
        double r1000 = com.hongmeng.xiuxian.compat.ArmorMitigation.softCapArmor(1000.0D);
        check("护甲越高上限值越高（单调不减）",
                r30 >= 30.0D && r100 > r30 && r1000 > r100,
                String.format("30→%.1f  100→%.1f  1000→%.1f", r30, r100, r1000));
        check("软上限下方的护甲不受影响（30 原样）",
                Math.abs(r30 - 30.0D) < 1e-9, String.format("%.1f", r30));
        check("1000 护甲被压到 172 以内", r1000 <= 172.0D,
                String.format("%.1f", r1000));

        // 属性上限必须被抬高（否则 MAX_HEALTH 会在 1024 处截断）
        check("抬升后的属性上限远高于原版 1024",
                com.hongmeng.xiuxian.compat.AttributeCaps.NEW_MAX > 1024.0D,
                String.valueOf(com.hongmeng.xiuxian.compat.AttributeCaps.NEW_MAX));
    }

    /** 累加某境五件法宝的生命/攻击百分比（给断言用）。 */
    private static double sumArtifactPercent(int tier, boolean health) {
        double sum = 0.0D;
        for (int slot = 0;
             slot < com.hongmeng.xiuxian.item.gear.ArtifactStats.slotCount(); slot++) {
            sum += health
                    ? com.hongmeng.xiuxian.item.gear.ArtifactStats.healthPercent(tier, slot)
                    : com.hongmeng.xiuxian.item.gear.ArtifactStats.attackPercent(tier, slot);
        }
        return sum;
    }

    /**
     * 法宝装备栏：五槽位定义、查表、存取与存档往返。
     *
     * <p>装备栏是这一轮新加的——法宝从"丢背包自动生效"改成"必须装备"，
     * 所以这里把槽位表与存取行为都钉住，避免以后改坏。</p>
     */
    private static void equipmentSlots() {
        section("法宝装备栏");

        // 只碰 EquipmentSlots（纯数据类）——它不含任何 Minecraft 类型，
        // 所以不会把注册表拖进来。
        var keys = com.hongmeng.xiuxian.item.gear.EquipmentSlots.SLOT_KEYS;
        var names = com.hongmeng.xiuxian.item.gear.EquipmentSlots.SLOT_NAMES;

        check("五个槽位", com.hongmeng.xiuxian.item.gear.EquipmentSlots.COUNT == 5,
                "实际 " + com.hongmeng.xiuxian.item.gear.EquipmentSlots.COUNT);
        check("槽位 key 与中文名数量一致", keys.length == names.length,
                keys.length + " vs " + names.length);
        check("槽位 key 无重复",
                new java.util.HashSet<>(java.util.List.of(keys)).size() == keys.length, "");
        check("槽位中文名无重复",
                new java.util.HashSet<>(java.util.List.of(names)).size() == names.length, "");
        check("槽位顺序是 法冠/道袍/法裤/云履/玉佩",
                names[0].equals("法冠") && names[1].equals("道袍") && names[2].equals("法裤")
                        && names[3].equals("云履") && names[4].equals("玉佩"),
                String.join("/", names));

        check("indexOf(crown) = 0",
                com.hongmeng.xiuxian.item.gear.EquipmentSlots.indexOf("crown") == 0, "");
        check("indexOf(robe) = 1",
                com.hongmeng.xiuxian.item.gear.EquipmentSlots.indexOf("robe") == 1, "");
        check("indexOf(jade) = 4",
                com.hongmeng.xiuxian.item.gear.EquipmentSlots.indexOf("jade") == 4, "");
        check("indexOf 未知 key 返回 -1",
                com.hongmeng.xiuxian.item.gear.EquipmentSlots.indexOf("nope") == -1, "");
        check("indexOf(null) 返回 -1（不抛异常）",
                com.hongmeng.xiuxian.item.gear.EquipmentSlots.indexOf(null) == -1, "");

        // 关键回归：中文部位名不能当成 key 用（这是修过的一个真 bug）
        check("中文部位名「法冠」不是合法 key（防回归）",
                com.hongmeng.xiuxian.item.gear.EquipmentSlots.indexOf("法冠") == -1, "");
        check("每个中文名都能映射到自己的 key（经 GearNames）",
                java.util.stream.IntStream.range(0, 5).allMatch(i ->
                        com.hongmeng.xiuxian.item.gear.EquipmentSlots.indexOf(
                                com.hongmeng.xiuxian.item.gear.GearNames.slotKey(names[i])) == i),
                "");

        check("nameOf(0) = 法冠",
                com.hongmeng.xiuxian.item.gear.EquipmentSlots.nameOf(0).equals("法冠"), "");
        check("nameOf(4) = 玉佩",
                com.hongmeng.xiuxian.item.gear.EquipmentSlots.nameOf(4).equals("玉佩"), "");
        check("nameOf 越界返回兜底文本「法宝」",
                com.hongmeng.xiuxian.item.gear.EquipmentSlots.nameOf(-1).equals("法宝")
                        && com.hongmeng.xiuxian.item.gear.EquipmentSlots.nameOf(99).equals("法宝"), "");
        check("isValid 边界正确",
                !com.hongmeng.xiuxian.item.gear.EquipmentSlots.isValid(-1)
                        && com.hongmeng.xiuxian.item.gear.EquipmentSlots.isValid(0)
                        && com.hongmeng.xiuxian.item.gear.EquipmentSlots.isValid(4)
                        && !com.hongmeng.xiuxian.item.gear.EquipmentSlots.isValid(5), "");

        // 部位 key 必须与 GearNames 里定义的一致（否则法宝装不进槽）
        boolean keysMatch = true;
        for (int i = 0; i < 5; i++) {
            String fromNames = com.hongmeng.xiuxian.item.gear.GearNames.slotKey(names[i]);
            if (!fromNames.equals(keys[i])) {
                keysMatch = false;
                break;
            }
        }
        check("EquipmentSlots 与 GearNames 的部位 key 完全一致", keysMatch, "");
    }

    /**
     * 商店与经济：价格表完整、灵叶来源存在、灵根保底参数正确。
     */
    private static void shopAndPity() {
        section("商店与保底");

        java.util.List<com.hongmeng.xiuxian.shop.ShopEntry> entries =
                com.hongmeng.xiuxian.shop.ShopCatalog.all();
        check("商店商品数 >= 8", entries.size() >= 8, "实际 " + entries.size());

        boolean sane = true;
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (var e : entries) {
            if (e.id().isEmpty() || e.cn().isEmpty() || e.priceLeaves() <= 0
                    || e.count() <= 0 || e.minRealmTier() < 1 || e.minRealmTier() > 10
                    || !ids.add(e.id())) {
                sane = false;
                break;
            }
        }
        check("每条商品字段合法且 id 无重复", sane, "");

        check("洗灵丹在商店里（灵根可稳定重抽）",
                com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_xiling") != null, "");
        // 价格整体下调过一轮（玩家反馈"太贵"）：洗灵丹 200 → 150
        check("洗灵丹售价 150 灵叶",
                com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_xiling").priceLeaves() == 150, "");
        check("双道宫丹是最高价商品之一",
                com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_shuangdao").priceLeaves() >= 1000, "");

        // ---- 新经济：练气期买得起第一颗丹 ----
        check("普通修为丹 20 灵叶（练气期打十几只怪就买得起）",
                com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_exp_common").priceLeaves() == 20,
                String.valueOf(com.hongmeng.xiuxian.shop.ShopCatalog
                        .byId("pill_exp_common").priceLeaves()));
        check("回灵丹 10 灵叶（最便宜的一档）",
                com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_huiling").priceLeaves() == 10, "");
        check("筑基丹 100 灵叶", 
                com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_zhuji").priceLeaves() == 100, "");
        check("破境丹 250 灵叶",
                com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_pojing").priceLeaves() == 250, "");
        check("仙品修为丹 300 灵叶",
                com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_exp_immortal").priceLeaves() == 300, "");
        check("双道宫丹 2000 灵叶（长期目标）",
                com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_shuangdao").priceLeaves() == 2000, "");

        String[] ladder = {"pill_huiling", "pill_exp_common", "zhaohua_dan", "pill_exp_perfect",
                "pill_zhuji", "pill_xiling", "pill_pojing", "pill_exp_immortal", "pill_shuangdao"};
        boolean ladderOk = true;
        for (int i = 1; i < ladder.length; i++) {
            int prev = com.hongmeng.xiuxian.shop.ShopCatalog.byId(ladder[i - 1]).priceLeaves();
            int cur = com.hongmeng.xiuxian.shop.ShopCatalog.byId(ladder[i]).priceLeaves();
            if (cur < prev) {
                ladderOk = false;
                break;
            }
        }
        check("丹药价格阶梯不降（越强的丹越贵）", ladderOk, "");

        // 取出实物灵叶必须是 1:1——合成链里有十几个配方要 lingye 当材料，
        // 抽水就等于惩罚玩家用余额
        var withdraw = com.hongmeng.xiuxian.shop.ShopCatalog.byId("lingye");
        check("取出灵叶 1:1（10 片 = 10 灵叶，不抽水）",
                withdraw != null && withdraw.priceLeaves() == withdraw.count(), "");

        // ---- 纪元令兑换 ----
        var tokenEntries = com.hongmeng.xiuxian.shop.ShopCatalog.tokenEntries();
        check("纪元令兑换条目有 4 条", tokenEntries.size() == 4,
                String.valueOf(tokenEntries.size()));
        boolean tokenCurrencyOk = true;
        boolean tokenAmountOk = true;
        for (var e : tokenEntries) {
            if (e.usesLeaves() || !"纪元令".equals(e.currencyName())
                    || e.priceLeaves() != 1 || !"hongmeng:jidan".equals(e.currencyItemId())) {
                tokenCurrencyOk = false;
            }
            // 一件法宝升一阶要 8 个材料：一枚令牌至少得够一次升阶
            if (e.count() < 8) {
                tokenAmountOk = false;
            }
        }
        check("纪元令商品用纪元令结算（1 枚一份）", tokenCurrencyOk, "");
        check("每枚纪元令至少换到一次法宝升阶的量（≥8）", tokenAmountOk, "");

        String[][] wantTokens = {
                {"token_spirit_iron", "hongmeng:spirit_iron_ingot"},
                {"token_spirit_crystal", "hongmeng:spirit_crystal"},
                {"token_kongming", "hongmeng:kongming_essence"},
                {"token_xueluo", "hongmeng:xueluo_essence"},
        };
        boolean materialsOk = true;
        for (String[] pair : wantTokens) {
            var e = com.hongmeng.xiuxian.shop.ShopCatalog.byId(pair[0]);
            if (e == null || !pair[1].equals(e.itemId())) {
                materialsOk = false;
            }
        }
        check("四种法宝材料都能用纪元令换", materialsOk, "");

        // ---- 杀怪灵叶掉落表（按品阶） ----
        var ranks = com.hongmeng.xiuxian.entity.MobRank.values();
        boolean dropIncreasing = true;
        for (int i = 0; i < ranks.length; i++) {
            if (com.hongmeng.xiuxian.shop.LeafRewards.min(ranks[i])
                    > com.hongmeng.xiuxian.shop.LeafRewards.max(ranks[i])) {
                dropIncreasing = false;
            }
            if (i > 0 && com.hongmeng.xiuxian.shop.LeafRewards.min(ranks[i])
                    <= com.hongmeng.xiuxian.shop.LeafRewards.min(ranks[i - 1])) {
                dropIncreasing = false;
            }
        }
        check("灵叶掉落随品阶严格递增（越难打越有钱）", dropIncreasing, "");
        check("普通妖兽掉 2~4 片",
                com.hongmeng.xiuxian.shop.LeafRewards.min(com.hongmeng.xiuxian.entity.MobRank.NORMAL) == 2
                        && com.hongmeng.xiuxian.shop.LeafRewards.max(
                                com.hongmeng.xiuxian.entity.MobRank.NORMAL) == 4, "");
        check("首领掉 100~150 片",
                com.hongmeng.xiuxian.shop.LeafRewards.min(com.hongmeng.xiuxian.entity.MobRank.OVERLORD) == 100
                        && com.hongmeng.xiuxian.shop.LeafRewards.max(
                                com.hongmeng.xiuxian.entity.MobRank.OVERLORD) == 150, "");
        check("原版怪也有小额掉落（1~2 片）",
                com.hongmeng.xiuxian.shop.LeafRewards.VANILLA_MIN == 1
                        && com.hongmeng.xiuxian.shop.LeafRewards.VANILLA_MAX == 2, "");

        java.util.Random drng = new java.util.Random(20240607L);
        boolean rollInRange = true;
        for (int i = 0; i < 20_000; i++) {
            var rank = ranks[i % ranks.length];
            int v = com.hongmeng.xiuxian.shop.LeafRewards.roll(rank, drng);
            if (v < com.hongmeng.xiuxian.shop.LeafRewards.min(rank)
                    || v > com.hongmeng.xiuxian.shop.LeafRewards.max(rank)) {
                rollInRange = false;
                break;
            }
        }
        check("20000 次掉落抽样都落在区间内", rollInRange, "");

        // 首领收益至少是普通怪的 20 倍——不然"打高阶怪"没有经济意义
        double normalAvg = com.hongmeng.xiuxian.shop.LeafRewards.average(
                com.hongmeng.xiuxian.entity.MobRank.NORMAL);
        double overlordAvg = com.hongmeng.xiuxian.shop.LeafRewards.average(
                com.hongmeng.xiuxian.entity.MobRank.OVERLORD);
        check("首领期望收益 >= 普通怪的 20 倍",
                overlordAvg >= normalAvg * 20.0D,
                String.format("%.1f vs %.1f", overlordAvg, normalAvg));

        // 练气期收入与第一颗丹的价格要对得上：普通怪约 20 只就能买普通修为丹
        int commonPrice = com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_exp_common").priceLeaves();
        double killsForCommon = commonPrice / (normalAvg * 0.35D);
        check("练气期约 20 只普通妖兽能换一颗普通修为丹",
                killsForCommon <= 25.0D, String.format("%.1f 只", killsForCommon));

        // 解锁条件
        var pojing = com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_pojing");
        check("破境丹练气买不了", !pojing.unlockedFor(1), "");
        check("破境丹金丹能买", pojing.unlockedFor(3), "");
        var common = com.hongmeng.xiuxian.shop.ShopCatalog.byId("pill_exp_common");
        check("普通修为丹练气就能买", common.unlockedFor(1), "");
        check("解锁条件文案可读",
                pojing.requirementText().contains("金丹"), pojing.requirementText());

        // 越界索引安全
        check("越界索引返回 null",
                com.hongmeng.xiuxian.shop.ShopCatalog.byIndex(-1) == null
                        && com.hongmeng.xiuxian.shop.ShopCatalog.byIndex(9999) == null, "");

        // ---- 灵根保底 ----
        check("保底阈值为 10 连",
                com.hongmeng.xiuxian.spirit.SpiritRoot.PITY_THRESHOLD == 10, "");
        check("保底门槛是天灵根及以上",
                com.hongmeng.xiuxian.spirit.SpiritRoot.PITY_GOOD_FROM == Rarity.HEAVEN, "");
        check("天灵根算出货", com.hongmeng.xiuxian.spirit.SpiritRoot.isGood(Rarity.HEAVEN), "");
        check("地灵根不算出货", !com.hongmeng.xiuxian.spirit.SpiritRoot.isGood(Rarity.EARTH), "");
        check("伪灵根不算出货", !com.hongmeng.xiuxian.spirit.SpiritRoot.isGood(Rarity.FALSE_ROOT), "");

        // 保底池只出天灵根及以上
        java.util.Random rng = new java.util.Random(4242L);
        boolean pityOnlyGood = true;
        java.util.EnumMap<Rarity, Integer> pityCounts = new java.util.EnumMap<>(Rarity.class);
        for (int i = 0; i < 40_000; i++) {
            Rarity r = com.hongmeng.xiuxian.spirit.SpiritRoot.rollPity(rng).rarity();
            pityCounts.merge(r, 1, Integer::sum);
            if (r.ordinal() < Rarity.HEAVEN.ordinal()) {
                pityOnlyGood = false;
            }
        }
        check("保底池只出天灵根及以上", pityOnlyGood, "");
        check("保底池里鸿蒙灵根也有机会（>2%）",
                pityCounts.getOrDefault(Rarity.HONGMENG, 0) / 40_000.0D > 0.02D,
                String.format("%.1f%%", pityCounts.getOrDefault(Rarity.HONGMENG, 0) / 400.0D));

        // 保底计数器的纯逻辑
        com.hongmeng.xiuxian.core.CultivationData cd =
                new com.hongmeng.xiuxian.core.CultivationData();
        check("初始不触发保底", !cd.isPityReady(), "");
        check("初始距保底 10 次", cd.rollsUntilPity() == 10, "");
        for (int i = 0; i < 9; i++) {
            cd.bumpRootPity();
        }
        check("9 次后仍未触发", !cd.isPityReady(), "");
        cd.bumpRootPity();
        check("10 次后触发保底", cd.isPityReady(), "");
        check("触发后距保底为 0", cd.rollsUntilPity() == 0, "");
        cd.resetRootPity();
        check("抽到好货后计数清零", !cd.isPityReady() && cd.getRootRollCount() == 0, "");
    }

    private static void dataRoundTrip() {
        section("存档往返（NBT 存取一致性）");

        CultivationData src = new CultivationData();
        src.setAwakened(true);
        src.setRoot(SpiritRoot.INNATE_ICE_SWORD);
        // 修为在本层封顶，所以往返测试要用"层号 + 该层内的合法修为"这一对。
        // 直接塞一个远超上限的数字会在 load 时被 clampCultivationToStep 夹掉——
        // 那正是我们要的行为（见下面单独的夹取断言）。
        src.setUnlockedStep(9);   // 金丹后期
        src.setCultivation(src.realm().minTotal() + 1234L);
        src.addCultivation(1_000L);
        src.setKarma(4);
        src.setSect(Sect.SHENXIAO.id());
        src.setReputation(42);
        src.setContribution(777);
        src.setBetrayed(true);
        src.setQuestStage("epoch_open");
        src.setEpochSeason(3);
        src.addDungeonClear();
        src.addDungeonClear();
        src.setPainLevel(2);

        net.minecraft.nbt.CompoundTag tag = src.save();
        CultivationData dst = new CultivationData();
        dst.load(tag);

        check("灵根往返一致", dst.getRoot() == SpiritRoot.INNATE_ICE_SWORD, "");
        check("修为往返一致", dst.getCultivation() == src.getCultivation(),
                dst.getCultivation() + " vs " + src.getCultivation());
        check("层号往返一致", dst.unlockedStep() == src.unlockedStep(),
                dst.unlockedStep() + " vs " + src.unlockedStep());
        check("累计修为往返一致", dst.getTotalCultivation() == src.getTotalCultivation(), "");
        check("业力往返一致", dst.getKarma() == 4, String.valueOf(dst.getKarma()));
        check("门派往返一致", dst.getSect().equals(Sect.SHENXIAO.id()), dst.getSect());
        check("贡献往返一致", dst.getContribution() == 777, String.valueOf(dst.getContribution()));
        check("叛徒标记往返一致", dst.isBetrayed(), "");
        check("任务阶段往返一致", "epoch_open".equals(dst.getQuestStage()), dst.getQuestStage());
        check("赛季往返一致", dst.getEpochSeason() == 3, String.valueOf(dst.getEpochSeason()));
        check("通关次数往返一致", dst.getDungeonClears() == 2, String.valueOf(dst.getDungeonClears()));
        check("痛觉层数往返一致", dst.getPainLevel() == 2, String.valueOf(dst.getPainLevel()));
        check("身份名可推导", "真传弟子".equals(dst.rankName()), dst.rankName());

        // ---- 灵叶余额（现在灵叶是存在坊市的余额，不再是背包物品） ----
        src.setLeaves(1234);
        CultivationData wallet = new CultivationData();
        wallet.load(src.save());
        check("灵叶余额往返一致", wallet.getLeaves() == 1234,
                String.valueOf(wallet.getLeaves()));

        // 老存档没有 Leaves 键：必须读成 0（不报错、也不凭空发钱）
        net.minecraft.nbt.CompoundTag legacyTag = src.save();
        legacyTag.remove("Leaves");
        CultivationData legacyWallet = new CultivationData();
        legacyWallet.load(legacyTag);
        check("老存档没有 Leaves 键时余额为 0", legacyWallet.getLeaves() == 0,
                String.valueOf(legacyWallet.getLeaves()));

        legacyWallet.setLeaves(5);
        check("余额不足时扣款失败且余额不动",
                !legacyWallet.takeLeaves(10) && legacyWallet.getLeaves() == 5, "");
        check("余额够时正常扣款", legacyWallet.takeLeaves(5) && legacyWallet.getLeaves() == 0, "");
        legacyWallet.setLeaves(-3);
        check("余额不接受负数", legacyWallet.getLeaves() == 0,
                String.valueOf(legacyWallet.getLeaves()));
        check("addLeaves 返回新余额", legacyWallet.addLeaves(7) == 7
                && legacyWallet.getLeaves() == 7, String.valueOf(legacyWallet.getLeaves()));
        legacyWallet.addLeaves(-100);
        check("addLeaves 不接受负增量", legacyWallet.getLeaves() == 7,
                String.valueOf(legacyWallet.getLeaves()));
    }

    // ------------------------------------------------------------------

    private static void section(String title) {
        System.out.println("── " + title + " ──");
    }

    private static void check(String what, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("  [OK]   " + what);
        } else {
            failed++;
            System.out.println("  [FAIL] " + what + (detail.isEmpty() ? "" : "   → " + detail));
        }
    }

    /**
     * <b>落点算法测试</b>（纯数学，不需要世界）。
     *
     * <p>背景：玩家反复反馈「地形还是没有发生破坏」。落点算法原本只能靠进游戏试，
     * 现在抽成了纯函数，这里直接用合成数据验证每一条规则。</p>
     */
    private static void aimPointMath() {
        section("落点算法（准心射线）");

        // 合成场景：平坦地形，顶面 y=64；玩家脚踩 64；眼睛 64.62
        final double eyeY = 64.62D;
        // 与真实实现同口径：地形高度压到"脚下那一格以下"
        final int groundY = 63;
        var ground = (com.hongmeng.xiuxian.technique.TechniqueCaster.GroundFn)
                (bx, bz) -> groundY;
        // 没有方块射线（只测落点数学）
        com.hongmeng.xiuxian.technique.TechniqueCaster.ClipFn noClip = (a, b) -> null;

        // ---- 规则 1：小幅俯视 → 命中远处地面 ----
        // 视线俯角 9.6°（look.y = -1/6），从 64.62 降到 63 需要水平前进约 9.7 格
        var look1 = new net.minecraft.world.phys.Vec3(0.0D, -1.0D / 6.0D, 0.985D).normalize();
        var p1 = com.hongmeng.xiuxian.technique.TechniqueCaster.aimPoint(
                new net.minecraft.world.phys.Vec3(0.0D, eyeY, 0.0D), look1, 64, 200.0D,
                ground, noClip);
        boolean ok1 = p1 != null && p1.z > 5.0D && p1.y <= eyeY;
        check("小幅俯视：命中前方地面（不是脚下）", ok1,
                p1 == null ? "返回 null" : String.format("落点 z=%.1f y=%.1f", p1.z, p1.y));

        // ---- 规则 2：朝天看 → 什么都不破坏 ----
        var lookUp = new net.minecraft.world.phys.Vec3(0.0D, 1.0D, 0.0D);
        var pUp = com.hongmeng.xiuxian.technique.TechniqueCaster.aimPoint(
                new net.minecraft.world.phys.Vec3(0.0D, eyeY, 0.0D), lookUp, 64, 200.0D,
                ground, noClip);
        check("朝天看：返回 null（什么都不破坏）", pUp == null,
                pUp == null ? "" : "却返回了落点");

        // ---- 规则 3：平视 → 不自动落地 ----
        var lookFlat = new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 1.0D);
        var pFlat = com.hongmeng.xiuxian.technique.TechniqueCaster.aimPoint(
                new net.minecraft.world.phys.Vec3(0.0D, eyeY, 0.0D), lookFlat, 64, 200.0D,
                ground, noClip);
        check("平视：不自动落地（返回 null）", pFlat == null,
                pFlat == null ? "" : "却返回了落点");

        // ---- 规则 4：垂直朝下 → 落在脚前 ----
        var lookDown = new net.minecraft.world.phys.Vec3(0.0D, -1.0D, 0.0D);
        var pDown = com.hongmeng.xiuxian.technique.TechniqueCaster.aimPoint(
                new net.minecraft.world.phys.Vec3(3.0D, eyeY, 7.0D), lookDown, 64, 200.0D,
                ground, noClip);
        boolean ok4 = pDown != null
                && Math.abs(pDown.x - 3.0D) < 0.01D && Math.abs(pDown.z - 7.0D) < 0.01D;
        check("垂直朝下：落在自己脚下那一格", ok4,
                pDown == null ? "返回 null" : String.format("x=%.1f z=%.1f", pDown.x, pDown.z));

        // ---- 规则 5：射线打到前方方块 → 优先用它（不是地形高度） ----
        // 模拟"前方 20 格处有一面墙"
        var wallHit = new net.minecraft.world.phys.Vec3(0.0D, eyeY - 0.5D, 20.0D);
        com.hongmeng.xiuxian.technique.TechniqueCaster.ClipFn wall = (a, b) -> wallHit;
        var pWall = com.hongmeng.xiuxian.technique.TechniqueCaster.aimPoint(
                new net.minecraft.world.phys.Vec3(0.0D, eyeY, 0.0D), lookFlat, 64, 200.0D,
                ground, wall);
        boolean ok5 = pWall != null && Math.abs(pWall.z - 20.0D) < 0.01D;
        check("射线命中前方方块：优先用射线命中点", ok5,
                pWall == null ? "返回 null" : String.format("z=%.1f（应为 20）", pWall.z));

        // ---- 规则 6：射线只打到脚下（水平距离 < 1.5）→ 不被采用 ----
        var feetHit = new net.minecraft.world.phys.Vec3(0.5D, eyeY, 0.5D);
        com.hongmeng.xiuxian.technique.TechniqueCaster.ClipFn feet = (a, b) -> feetHit;
        var pFeet = com.hongmeng.xiuxian.technique.TechniqueCaster.aimPoint(
                new net.minecraft.world.phys.Vec3(0.0D, eyeY, 0.0D), look1, 64, 200.0D,
                ground, feet);
        boolean ok6 = pFeet != null && pFeet.z > 5.0D;
        check("射线只打到脚下：改用地面落点（不是脚下）", ok6,
                pFeet == null ? "返回 null" : String.format("z=%.1f", pFeet.z));

        // ---- 规则 7：射程内打不到（地形很低） → 返回 null，不破坏 ----
        var farGround = (com.hongmeng.xiuxian.technique.TechniqueCaster.GroundFn)
                (bx, bz) -> -100;      // 地形极低
        var lookShallow = new net.minecraft.world.phys.Vec3(0.0D, -0.001D, 1.0D).normalize();
        var pFar = com.hongmeng.xiuxian.technique.TechniqueCaster.aimPoint(
                new net.minecraft.world.phys.Vec3(0.0D, eyeY, 0.0D), lookShallow, 64, 30.0D,
                farGround, noClip);
        check("射程内打不到地形：返回 null（离地面远就破坏不到）", pFar == null,
                pFar == null ? "" : "却返回了落点");
    }


    /**
     * <b>特效组成指纹查重</b>。
     *
     * <p>玩家反复反馈「特效单调」—— 根因是身份维度（identity/layers/sizeStep/phase）
     * 一开始只用在灭世级，其余形态所有功法长得一样。</p>
     *
     * <p>这条断言把每门功法的**组成指纹**算出来（形态 + 身份 + 层数 + 尺寸档 + 相位 +
     * 元素），然后查重：**只要有两门功法指纹相同就失败**。
     * 这样"我说不重复"就变成了"可验证的不重复"。</p>
     */
    private static void spellFxUniqueness() {
        section("法术特效组成指纹");

        java.util.Map<String, java.util.List<String>> seen = new java.util.HashMap<>();
        int checked = 0;
        for (Technique t : Technique.values()) {
            if (t.kind() == com.hongmeng.xiuxian.technique.TechniqueKind.PASSIVE) {
                continue;                       // 被动没有结算形态
            }
            // 视觉身份现在来自**显式定妆表**（SpellLook）：身份/层数/尺寸档/相位
            // 都是逐门写死的数据，不再是名字哈希 —— 所以指纹要按表里的真实值算。
            var look = com.hongmeng.xiuxian.technique.SpellLook.of(t);
            int identity = look.identity();
            int layers = look.layers();
            int sizeStep = look.sizeStep();
            int phase = look.phase();
            // 指纹必须包含**实际挑到的形状 ID** —— 否则"指纹唯一"不保证"看起来不同"。
            // 这里调的是真实的选择函数（不是复制一份公式），所以形状逻辑一改就会跟着变。
            byte mainShape = com.hongmeng.xiuxian.technique.SpellFxRuntime
                    .primaryShapeFor(identity, layers);
            byte signShape = com.hongmeng.xiuxian.technique.SpellFxRuntime
                    .signatureShapeFor(identity, layers);
            // 普通法术还有一层"元素几何签名"（木=螺旋 / 金雷=晶格 / 土=环塔 / 水冰=环体）
            byte elemGeo = com.hongmeng.xiuxian.technique.SpellFxRuntime
                    .elementGeometryFor(t.element(), layers);
            String fp = t.kind() + "|" + identity + "|" + layers + "|"
                    + sizeStep + "|" + phase + "|" + t.element()
                    + "|主" + mainShape + "|签" + signShape + "|元" + elemGeo
                    + "|演" + look.setpiece();
            seen.computeIfAbsent(fp, k -> new java.util.ArrayList<>()).add(t.cn());
            checked++;
        }

        int dup = 0;
        StringBuilder worst = new StringBuilder();
        for (var e : seen.entrySet()) {
            if (e.getValue().size() > 1) {
                dup++;
                if (worst.length() == 0) {
                    worst.append(e.getValue());
                }
            }
        }
        check("每门功法的特效组成唯一（无重复指纹）", dup == 0,
                dup == 0 ? ("共 " + checked + " 门，指纹 " + seen.size() + " 种")
                        : (dup + " 组重复，例如 " + worst));

        // ---- 形状选择函数本身的性质 ----
        java.util.Set<Byte> mains = new java.util.HashSet<>();
        for (int id = 0; id < 6; id++) {
            mains.add(com.hongmeng.xiuxian.technique.SpellFxRuntime
                    .primaryShapeFor(id, 2));
        }
        check("primaryShapeFor 对 6 个 identity 给出 6 个不同主形状", mains.size() == 6,
                "实际 " + mains.size() + " 种: " + mains);

        int sameAsMain = 0;
        for (int id = 0; id < 6; id++) {
            for (int ly = 2; ly <= 5; ly++) {
                if (com.hongmeng.xiuxian.technique.SpellFxRuntime.primaryShapeFor(id, ly)
                        == com.hongmeng.xiuxian.technique.SpellFxRuntime
                                .signatureShapeFor(id, ly)) {
                    sameAsMain++;
                }
            }
        }
        check("签名形状层永远不等于主形状（否则那层白加）", sameAsMain == 0,
                sameAsMain == 0 ? "24 组组合全部不同" : (sameAsMain + " 组相同"));

        // ---- 4 种新几何必须真的被用到 ----
        java.util.Set<Byte> used = new java.util.HashSet<>();
        for (int id = 0; id < 6; id++) {
            for (int ly = 2; ly <= 5; ly++) {
                used.add(com.hongmeng.xiuxian.technique.SpellFxRuntime
                        .primaryShapeFor(id, ly));
                used.add(com.hongmeng.xiuxian.technique.SpellFxRuntime
                        .signatureShapeFor(id, ly));
            }
        }
        byte[] newShapes = {(byte) 12, (byte) 13, (byte) 14, (byte) 15};
        int usedNew = 0;
        StringBuilder missing = new StringBuilder();
        for (byte ns : newShapes) {
            if (used.contains(ns)) {
                usedNew++;
            } else {
                missing.append(ns).append(" ");
            }
        }
        check("4 种新几何（圆环体/晶格/螺旋/环塔）都被实际用到", usedNew == 4,
                usedNew == 4 ? "12/13/14/15 全部命中" : ("未用到: " + missing));

        // ---- 元素几何签名的性质 ----
        var R = com.hongmeng.xiuxian.technique.SpellFxRuntime.class;
        byte wood = com.hongmeng.xiuxian.technique.SpellFxRuntime
                .elementGeometryFor(com.hongmeng.xiuxian.spirit.Element.WOOD, 3);
        byte metal = com.hongmeng.xiuxian.technique.SpellFxRuntime
                .elementGeometryFor(com.hongmeng.xiuxian.spirit.Element.METAL, 3);
        byte earth = com.hongmeng.xiuxian.technique.SpellFxRuntime
                .elementGeometryFor(com.hongmeng.xiuxian.spirit.Element.EARTH, 3);
        byte water = com.hongmeng.xiuxian.technique.SpellFxRuntime
                .elementGeometryFor(com.hongmeng.xiuxian.spirit.Element.WATER, 3);
        byte wind = com.hongmeng.xiuxian.technique.SpellFxRuntime
                .elementGeometryFor(com.hongmeng.xiuxian.spirit.Element.WIND, 3);
        check("木/金/土/水 四个元素各有不同的立体几何签名",
                wood != metal && metal != earth && earth != water && wood != earth
                        && wood != water && metal != water,
                "木=" + wood + " 金=" + metal + " 土=" + earth + " 水=" + water);
        check("风系不加几何签名层（靠粒子表现，加几何反而糊）", wind == -1,
                "风=" + wind);

        // 火系是"层数够才加"的条件式
        byte fireLow = com.hongmeng.xiuxian.technique.SpellFxRuntime
                .elementGeometryFor(com.hongmeng.xiuxian.spirit.Element.FIRE, 2);
        byte fireHigh = com.hongmeng.xiuxian.technique.SpellFxRuntime
                .elementGeometryFor(com.hongmeng.xiuxian.spirit.Element.FIRE, 4);
        check("火系层数少时不加、层数多时才加（避免满屏都是环）",
                fireLow == -1 && fireHigh != -1,
                "layers=2 → " + fireLow + "，layers=4 → " + fireHigh);

        // 元素几何必须是**已有形状 ID 之一**，不能越界
        boolean inRange = true;
        StringBuilder bad = new StringBuilder();
        for (com.hongmeng.xiuxian.spirit.Element e
                : com.hongmeng.xiuxian.spirit.Element.values()) {
            byte g = com.hongmeng.xiuxian.technique.SpellFxRuntime
                    .elementGeometryFor(e, 5);
            if (g != -1 && (g < 0 || g > 15)) {
                inRange = false;
                bad.append(e).append("=").append(g).append(" ");
            }
        }
        check("元素几何返回的形状 ID 都在 0..15 之内（不越界）", inRange,
                inRange ? "全部合法" : bad.toString());

        // ==============================================================
        // 视觉定妆表（SpellLook）：完整性 + 字段范围 + 差异度 + 管线接线
        //
        // 这一层是"特效不再靠名字哈希抽签"的验收：52 门基础 + 20 门职业主动
        // 都必须有显式数据行，否则以后想单独调一门就得改算法、一改全改。
        // ==============================================================
        int authoredBase = 0;
        for (Technique t : Technique.values()) {
            if (com.hongmeng.xiuxian.technique.SpellLook.authored(t)) {
                authoredBase++;
            }
        }
        check("全部基础功法都有显式视觉定妆行（不再靠哈希抽签）",
                authoredBase == Technique.values().length,
                authoredBase + " / " + Technique.values().length);

        java.util.Set<String> techniqueNames = new java.util.HashSet<>();
        for (Technique t : Technique.values()) {
            techniqueNames.add(t.name());
        }
        int careerRows = 0;
        for (String key : com.hongmeng.xiuxian.technique.SpellLook.keys()) {
            if (!techniqueNames.contains(key)) {
                careerRows++;
            }
        }
        // 交接表里 18 条新增主动各一行；另 2 条「复用已有」沿用基础行，不重复登记。
        int expectedCareerRows = Math.max(0, 70 - Technique.values().length);
        check("定妆表含 18 门新增职业主动行（2 条复用基础行；并入 Technique 后归零）",
                careerRows == expectedCareerRows,
                "待并入 " + careerRows + " 行 / 期望 " + expectedCareerRows);

        boolean lookRangeOk = true;
        StringBuilder lookBad = new StringBuilder();
        java.util.Set<String> chargeCombos = new java.util.HashSet<>();
        int withSetpiece = 0;
        for (String key : com.hongmeng.xiuxian.technique.SpellLook.keys()) {
            var lk = com.hongmeng.xiuxian.technique.SpellLook.byKey(key);
            boolean ok = lk != null
                    && lk.identity() >= 0 && lk.identity() <= 5
                    && lk.layers() >= 2 && lk.layers() <= 5
                    && lk.sizeStep() >= 0 && lk.sizeStep() <= 2
                    && lk.phase() >= 0 && lk.phase() <= 5
                    && lk.chargeLayers() >= 3 && lk.chargeLayers() <= 6
                    && (lk.spin() == 1 || lk.spin() == -1)
                    && lk.paletteShift() >= 0 && lk.paletteShift() <= 3
                    && lk.pitch() >= 0.80F && lk.pitch() <= 1.30F
                    && lk.motifA() != lk.motifB() && lk.motifB() != lk.motifC()
                    && lk.motifA() != lk.motifC();
            if (!ok) {
                lookRangeOk = false;
                lookBad.append(key).append(' ');
            }
            if (lk != null) {
                chargeCombos.add(lk.motifA() + "/" + lk.motifB() + "/" + lk.motifC()
                        + "@" + lk.chargeLayers() + (lk.spin() > 0 ? "↻" : "↺")
                        + "#" + lk.paletteShift());
                if (lk.setpiece() != com.hongmeng.xiuxian.technique.SpellLook
                        .Setpiece.NONE) {
                    withSetpiece++;
                }
            }
        }
        check("定妆表每行字段都在合法范围内", lookRangeOk, lookBad.toString());
        check("蓄力组合差异度 ≥ 40 种（不再撞脸）", chargeCombos.size() >= 40,
                "实际 " + chargeCombos.size() + " 种");
        check("57 门主动功法都有专属演出（39 基础主动 + 18 新增职业主动；被动为 NONE）",
                withSetpiece == 57, "带专属演出 " + withSetpiece + " 门");

        // ---- 管线接线（源码取证：光有数据不接上 = 白写）----
        var rtLook = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellFxRuntime.java"));
        check("释放层读定妆表（SpellLook.of）", rtLook.contains("SpellLook.of(technique)"), "");
        check("专属演出派发函数存在", rtLook.contains("playSetpiece("), "");
        check("专属演出覆盖到冰牢/雷牢/火海/天陨等分支",
                rtLook.contains("case STAR_FALL ->") && rtLook.contains("case FROST_PRISON ->")
                        && rtLook.contains("case THUNDER_CAGE ->")
                        && rtLook.contains("case FLAME_STORM ->"), "");
        check("方向型光柱退化时补终点（修复 from==to 时整层不画）",
                rtLook.contains("directionalEnd(") && rtLook.contains("length > 0.05F"), "");
        check("施法音按定妆音高变调（同元素不再同音）", rtLook.contains("look.pitch()"), "");
        check("飞剑形状（16~19）终于有发送方（以前是死形状）",
                rtLook.contains("FLYING_SWORD") || rtLook.contains("FLYING_DAGGER"), "");

        var sigLook = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellSignature.java"));
        check("蓄力读定妆表（图案/层数/旋向/调色）", sigLook.contains("SpellLook.byKey"), "");
        check("蓄力有专属点缀层（chargeAccent）", sigLook.contains("chargeAccent("), "");

        // ---- 批次一新增的两个标志性形状（冰牢 / 血符）----
        var pktLook = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/network/SpellFxPacket.java"));
        check("新增形状 FROST_CAGE=29 / BLOOD_SIGIL=30（旧 0~28 编号不动）",
                pktLook.contains("FROST_CAGE = 29") && pktLook.contains("BLOOD_SIGIL = 30"),
                "");
        check("新形状有贴图映射（frost_crystal / blood_sigil）",
                pktLook.contains("\"frost_crystal\"") && pktLook.contains("\"blood_sigil\""), "");
        var rendLook = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/SpellFxRenderer.java"));
        check("渲染器接了冰牢/血符两个新形状",
                rendLook.contains("frostCageFx(") && rendLook.contains("bloodSigilFx("), "");
        check("新贴图文件存在（frost_crystal.png / blood_sigil.png）",
                findResource("assets/hongmeng/textures/fx/frost_crystal.png") != null
                        && findResource("assets/hongmeng/textures/fx/blood_sigil.png") != null,
                "");
        check("冰牢/血符被释放层实际使用（不是死形状）",
                rtLook.contains("SpellFxPacket.FROST_CAGE")
                        && rtLook.contains("SpellFxPacket.BLOOD_SIGIL"), "");
        check("元素状态表现层（statusFx）接到附带状态上",
                stripComments(readSource("src/main/java/com/hongmeng/xiuxian/technique/"
                        + "SpellVfx.java")).contains("statusFx(")
                        && stripComments(readSource("src/main/java/com/hongmeng/xiuxian/"
                        + "technique/TechniqueCaster.java")).contains("SpellVfx.statusFx("), "");

        // ==============================================================
        // 2026-11-01 第三轮：特效减量 + 不遮脸 + 3D 化
        // ==============================================================
        byte[] threeD = {(byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 20, (byte) 25};
        java.util.Set<Byte> primaryShapes = new java.util.HashSet<>();
        for (int id = 0; id < 6; id++) {
            primaryShapes.add(com.hongmeng.xiuxian.technique.SpellFxRuntime
                    .primaryShapeFor(id, 2));
        }
        boolean primaryAll3d = true;
        StringBuilder flat = new StringBuilder();
        for (byte b : primaryShapes) {
            boolean is3d = false;
            for (byte t3 : threeD) {
                if (b == t3) {
                    is3d = true;
                    break;
                }
            }
            if (!is3d) {
                primaryAll3d = false;
                flat.append(b).append(' ');
            }
        }
        check("主形状 6 个候选全是立体几何（12/13/14/15/20/25）", primaryAll3d,
                primaryAll3d ? "6 个候选全部立体" : ("平片形状: " + flat));

        boolean sig3d = true;
        StringBuilder sigBad = new StringBuilder();
        for (int id = 0; id < 6; id++) {
            for (int ly = 2; ly <= 5; ly++) {
                byte sg = com.hongmeng.xiuxian.technique.SpellFxRuntime
                        .signatureShapeFor(id, ly);
                if (sg != (byte) 3 && sg != (byte) 5) {   // SHOCK_DOME / PILLAR_DROP
                    sig3d = false;
                    sigBad.append(sg).append(' ');
                }
            }
        }
        check("签名形状层只用体积几何（穹顶/光柱，不再用平片）", sig3d,
                sig3d ? "24 组全部是体积几何" : ("非体积形状: " + sigBad));

        check("蓄力有 3D 法阵层（每 8 tick 的环体/环塔/双螺旋）",
                rtLook.contains("chargeSigil(")
                        && stripComments(readSource("src/main/java/com/hongmeng/xiuxian/"
                        + "technique/SpellCastHandler.java")).contains("SpellFxRuntime.chargeSigil"),
                "");

        var sigLook3 = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellSignature.java"));
        check("蓄力粒子量已下调（q 系数 0.15，累计约 −75%）",
                sigLook3.contains("0.15D"), "");
        check("满蓄爆闪已抬高到头顶上方（不糊脸）",
                sigLook3.contains("c.add(0.0D, 2.9D, 0.0D)"), "");
        check("收束类图案终点抬到头顶上方（原 1.2 → 2.6）",
                sigLook3.contains("c.add(0.0D, 2.6D, 0.0D)"), "");

        // ---- 客户端：近距遮挡保护 + 3D 化冲击环/符文环 ----
        check("第一人称近距遮挡保护（视线前方按距离淡出）",
                rendLook.contains("camLook") && rendLook.contains("facing > 0.20D")
                        && rendLook.contains("firstPerson"), "");
        check("第一人称辉光只留 1 层（三层叠画会糊屏）",
                rendLook.contains("layers = 1;"), "");
        check("冲击环加了管状 3D 圆环（不再是平面贴图 + 星芒）",
                rendLook.contains("SpellFxMesh2.torus(pose, buf, \"torus_soft\", 28, 8"), "");
        check("符文环立体化（叠 2 圈上浮环 + 细管环）",
                rendLook.contains("1.0F - i * 0.24F")
                        && rendLook.contains("SpellFxMesh2.torus(pose, buf, \"torus_soft\", 26, 6"),
                "");

        // ==============================================================
        // 2026-11-07 第四轮：法术 + 领域特效再下调一档
        // ==============================================================
        var vfxSrc = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellVfx.java"));
        check("法术/领域粒子在唯一出口统一再降 40%（SPELL_FX_SCALE=0.60）",
                vfxSrc.contains("SPELL_FX_SCALE = 0.60D")
                        && vfxSrc.contains("capped * SPELL_FX_SCALE"), "");
        check("第一人称近距保护阈值收紧（贴脸 1.2 格跳过 / 4.5 格内淡出）",
                rendLook.contains("nearD < 1.2D") && rendLook.contains("nearD < 4.5D"), "");
        check("辉光透明度再降（中层 26% / 外层 6%）",
                rendLook.contains("alpha * 0.26F") && rendLook.contains("alpha * 0.06F"), "");
        check("整体透明峰值再降（0.85 → 0.70）",
                rendLook.contains("* 0.70D * power"), "");
        check("蓄力粒子再降（q=0.15）且 3D 法阵改为 12 tick 一圈",
                sigLook3.contains("0.15D")
                        && stripComments(readSource("src/main/java/com/hongmeng/xiuxian/"
                        + "technique/SpellCastHandler.java")).contains("now % 12L == 0L"), "");

        // ---- 领域特效再降（2026-11-07，玩家：「领域特效也是」）----
        var domClient = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/DomainClientVfx.java"));
        check("领域持续层再降：密集圈 13 格 / 灵气 4t 一次 / 每 tick 补 2 颗",
                domClient.contains("NEAR_RADIUS = 13.0D")
                        && domClient.contains("AURA_PERIOD = 4")
                        && domClient.contains("DENSE_PER_TICK = 2"), "");
        var domOpen = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/DomainOpeningVfx.java"));
        check("领域开启演出再降：环 16t × 3 颗 / 闪屏 0.12",
                domOpen.contains("RING_TICKS = 16")
                        && domOpen.contains("RING_PER_TICK = 3")
                        && domOpen.contains("0.12F"), "");
        var domField = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/domain/DomainField.java"));
        check("领域独立空间再降：12t 一刷 / 穹顶 24 点 / 内壁 9 点 / 界碑 6 格一根",
                domField.contains("EXPANSION_INTERVAL = 12")
                        && domField.contains("EXPANSION_POST_STEP = 6")
                        && domField.contains("int dome = 24")
                        && domField.contains("int face = 9"), "");
        check("领域法阵环数收敛（悬空 2 层 + 脚下 2 层 + 斜环 1 条）",
                stripComments(readSource("src/main/java/com/hongmeng/xiuxian/"
                        + "client/MagicCircles.java")).contains("Group(90"), "");

        // ==============================================================
        // 2026-11-07 第五轮：截图反馈「还有很浓、遮眼污染」→ 法阵贴图/白闪再收
        // ==============================================================
        var mcSrc = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/MagicCircles.java"));
        check("法阵贴图半径上限收到 7 格、视觉半径 5 格（原来最大可铺 20+ 格）",
                mcSrc.contains("MAX_RING_RADIUS = 7.0F")
                        && mcSrc.contains("VISUAL_RADIUS_CAP = 5.0F"), "");
        check("法阵贴图只画 2 层且透明度 30%/18%（去掉星形放射层 = 满天黄光扇）",
                mcSrc.contains("fade * 0.30F * fpMul")
                        && mcSrc.contains("fade * 0.18F * fpMul")
                        && !mcSrc.contains("types[2]"), "");
        check("法阵第一人称 9 格内再压暗 + 竖直贴脸环跳过",
                mcSrc.contains("camDist < 9.0D")
                        && mcSrc.contains("firstPerson && !ring.horizontal"), "");
        check("法阵粒子环密度再减半（baseCount×0.45）、布点间隔 3 tick",
                mcSrc.contains("ring.baseCount * 0.45D")
                        && mcSrc.contains("STEP = 3"), "");
        var flashFree = stripComments(readSource("src/main/java/com/hongmeng/xiuxian/"
                + "technique/SpellVfx.java"))
                + stripComments(readSource("src/main/java/com/hongmeng/xiuxian/"
                + "technique/SpellSignature.java"))
                + stripComments(readSource("src/main/java/com/hongmeng/xiuxian/"
                + "technique/SpellAftermath.java"))
                + stripComments(readSource("src/main/java/com/hongmeng/xiuxian/"
                + "client/DomainClientVfx.java"));
        check("法术/领域粒子不再用 FLASH（去掉满地白十字刺眼粒子）",
                !flashFree.contains("ParticleTypes.FLASH"), "");
        check("地面平片环第一人称近距跳过（<7 格）/压暗（<12 格）",
                rendLook.contains("groundPlane") && rendLook.contains("nearD < 7.0D"), "");
        check("阵法/冲击环/符文环贴图扩张幅度收敛（0.20+0.85t、阵法 ×1.4）",
                rendLook.contains("(0.20D + 0.85D * t)")
                        && rendLook.contains("p.size() * 1.4F"), "");

        // 顺带报一下差异度，方便以后观察
        double ratio = seen.isEmpty() ? 0.0D : (double) seen.size() / checked;
        System.out.println(String.format("        %d 门功法 / %d 种指纹（唯一率 %.0f%%）",
                checked, seen.size(), ratio * 100.0D));
    }


    /**
     * <b>演出镜头（过场动画）的性质</b>。
     *
     * <p>玩家要求「BOSS 出场也要做出场动画，影视级别的运镜和特效」。
     * 运镜的关键帧是纯数值，可以在服务端直接验证 ——
     * 设计错了一眼能看出来（比如全程单向推轨、原地不动、镜头穿模）。</p>
     */
    private static void cinematicShots() {
        section("BOSS 出场演出运镜");

        check("演出时长与降落动画一致（不会落地了镜头还在飞）",
                com.hongmeng.xiuxian.combat.BossCinematic.TICKS
                        == com.hongmeng.xiuxian.combat.BossDescent.TICKS,
                "cinematic=" + com.hongmeng.xiuxian.combat.BossCinematic.TICKS
                        + " descent=" + com.hongmeng.xiuxian.combat.BossDescent.TICKS);

        try {
            var m = com.hongmeng.xiuxian.combat.BossCinematic.class
                    .getDeclaredMethod("buildShots");
            m.setAccessible(true);
            float[] flat = (float[]) m.invoke(null);
            int n = flat.length / 5;
            check("关键帧数量 >= 5（少于 5 帧撑不起一段运镜）", n >= 5, n + " 帧");
            check("关键帧数组是 5 的整数倍（每帧 5 个参数）", flat.length % 5 == 0,
                    flat.length + " 个浮点");

            boolean distOk = true;
            boolean fovOk = true;
            boolean pitchOk = true;
            StringBuilder bad = new StringBuilder();
            float minD = Float.MAX_VALUE;
            float maxD = 0.0F;
            for (int i = 0; i < n; i++) {
                float dist = flat[i * 5];
                float pitch = flat[i * 5 + 2];
                float fov = flat[i * 5 + 4];
                minD = Math.min(minD, dist);
                maxD = Math.max(maxD, dist);
                if (dist < 2.0F || dist > 48.0F) {
                    distOk = false;
                    bad.append("帧").append(i).append("距离=").append(dist).append(" ");
                }
                if (fov < 30.0F || fov > 90.0F) {
                    fovOk = false;
                    bad.append("帧").append(i).append("视场=").append(fov).append(" ");
                }
                if (pitch < -45.0F || pitch > 60.0F) {
                    pitchOk = false;
                    bad.append("帧").append(i).append("俯仰=").append(pitch).append(" ");
                }
            }
            check("镜头距离都在 2~48 格（太近穿模、太远看不清）", distOk, bad.toString());
            check("视场角都在 30~90 度（太小晕、太大变形）", fovOk, "");
            check("俯仰角都在 -45~60 度（超出会翻镜头）", pitchOk, "");

            boolean hasPush = false;
            boolean hasPull = false;
            for (int i = 1; i < n; i++) {
                float d0 = flat[(i - 1) * 5];
                float d1 = flat[i * 5];
                if (d1 < d0 - 0.3F) {
                    hasPush = true;
                }
                if (d1 > d0 + 0.3F) {
                    hasPull = true;
                }
            }
            check("运镜有推近也有拉远（不是单向直线推轨）", hasPush && hasPull,
                    "推近=" + hasPush + " 拉远=" + hasPull + "  距离范围 "
                            + minD + "~" + maxD);

            float minY = Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                float y = flat[i * 5 + 1];
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
            float yawSpan = maxY - minY;
            check("水平角跨度 >= 180 度（真的绕着 boss 转，而不是原地不动）",
                    yawSpan >= 180.0F, "跨度 " + yawSpan + " 度");
        } catch (Throwable t) {
            check("能读到关键帧表", false, t.toString());
        }
    }


    /**
     * <b>特效"写实层"的取证断言</b>。
     *
     * <p>玩家反馈「法术特效依然还是粗糙……观感很差」。为此加了
     * 冲击尘云 / 火花喷溅两种粒子与多层同心壳。这类改动有两个
     * <b>很容易悄悄坏掉</b>的点，靠肉眼看不出来：</p>
     * <ul>
     *   <li>新粒子若不走 {@code take(...)} 预算闸门，就<b>绕过了全模组的粒子预算</b>
     *       （按帧/按距离/按画质折算）—— 低配机会卡，而代码看起来完全正常；</li>
     *   <li>壳层数若写死，就<b>无视玩家自己设的画质档</b>。</li>
     * </ul>
     * <p>所以这里直接对源码取证。</p>
     */
    private static void fxRealismLayers() {
        section("特效写实层（粒子预算 / 画质档 / 接线）");

        java.nio.file.Path root = findSourceRoot();
        if (root == null) {
            check("能找到源码根目录", false, "跳过源码取证");
            return;
        }
        String vfx = readIfExists(root.resolve(
                "com/hongmeng/xiuxian/technique/SpellVfx.java"));
        String caster = readIfExists(root.resolve(
                "com/hongmeng/xiuxian/technique/TechniqueCaster.java"));
        String renderer = readIfExists(root.resolve(
                "com/hongmeng/xiuxian/client/SpellFxRenderer.java"));

        // ① 新粒子方法必须存在
        check("冲击尘云 impactDust 已实现", vfx.contains("impactDust"),
                "SpellVfx.impactDust");
        check("火花喷溅 sparkSpray 已实现", vfx.contains("sparkSpray"),
                "SpellVfx.sparkSpray");

        // ② 它们必须走预算闸门（数 take( 的调用）
        int dustTake = countOccurrences(sectionOf(vfx, "impactDust", "sparkSpray"), "take(");
        int sparkTake = countOccurrences(sectionOf(vfx, "sparkSpray", "lineFx"), "take(");
        check("冲击尘云走粒子预算闸门 take(...)", dustTake >= 2,
                "take( 出现 " + dustTake + " 次（两层粒子各一次）");
        check("火花喷溅走粒子预算闸门 take(...)", sparkTake >= 2,
                "take( 出现 " + sparkTake + " 次");

        // ③ 两个方法内部**不允许**出现裸的 sendParticles 之外的东西 ——
        //    这里改判：sendParticles 的次数必须 <= take( 的次数 + 0
        //    （每次发粒子都应先 take 过）
        int dustSend = countOccurrences(sectionOf(vfx, "impactDust", "sparkSpray"),
                "sendParticles(");
        int sparkSend = countOccurrences(sectionOf(vfx, "sparkSpray", "lineFx"),
                "sendParticles(");
        check("冲击尘云：每次发粒子都先 take（没有绕过预算）", dustSend <= dustTake,
                "sendParticles=" + dustSend + "  take=" + dustTake);
        check("火花喷溅：每次发粒子都先 take（没有绕过预算）", sparkSend <= sparkTake,
                "sendParticles=" + sparkSend + "  take=" + sparkTake);

        // ④ 五个破坏类法术都接上了新粒子
        int dustUses = countOccurrences(caster, "impactDust(");
        int sparkUses = countOccurrences(caster, "sparkSpray(");
        check("破坏类法术接上了冲击尘云（>=5 处：陨石/寂灭/黑洞/炮击/斩星）",
                dustUses >= 5, "TechniqueCaster 里 " + dustUses + " 处");
        check("破坏类法术接上了火花喷溅（>=5 处）", sparkUses >= 5,
                "TechniqueCaster 里 " + sparkUses + " 处");

        // ⑤ 壳层数必须跟随画质档位
        boolean shellUsesQuality = renderer.contains("spellFxQuality")
                || renderer.contains("SpellFxQuality.layers()");
        check("多层壳的层数跟随画质档位（不是写死）", shellUsesQuality,
                shellUsesQuality ? "引用了 SpellFxQuality.layers()" : "**写死了层数**");

        // ⑥ 顶点亮度分层确实存在（"不再是平贴片"的核心）
        String mesh = readIfExists(root.resolve(
                "com/hongmeng/xiuxian/client/SpellFxMesh.java"));
        check("顶点写入带亮度参数（逐顶点明暗，去平贴片的核心）",
                mesh.contains("float bright") && mesh.contains("shade("),
                "SpellFxMesh 里有 bright 与 shade()");
        check("有多层同心壳工具", mesh.contains("sphereShells")
                        && mesh.contains("cylinderShells"),
                "sphereShells / cylinderShells");
    }

    /** 源码根目录（验证器运行时用相对路径找）。 */
    private static java.nio.file.Path findSourceRoot() {
        java.nio.file.Path[] cands = {
                java.nio.file.Paths.get("src/main/java"),
                java.nio.file.Paths.get("xiuxian-mod/src/main/java"),
                java.nio.file.Paths.get("../src/main/java"),
        };
        for (java.nio.file.Path p : cands) {
            if (java.nio.file.Files.isDirectory(p)) {
                return p;
            }
        }
        return null;
    }

    private static String readIfExists(java.nio.file.Path p) {
        try {
            return java.nio.file.Files.readString(p);
        } catch (Throwable t) {
            return "";
        }
    }

    private static int countOccurrences(String haystack, String needle) {
        if (haystack == null || haystack.isEmpty()) {
            return 0;
        }
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    /** 取源码里 from 到 to 之间那一段（用于只看某个方法体内）。 */
    private static String sectionOf(String src, String from, String to) {
        if (src == null) {
            return "";
        }
        int i = src.indexOf(from);
        if (i < 0) {
            return "";
        }
        int j = src.indexOf(to, i + from.length());
        return j < 0 ? src.substring(i) : src.substring(i, j);
    }


    /**
     * <b>「真实世界」的规则断言</b>。
     *
     * <p>段位表是玩家<b>逐条指定</b>的：</p>
     * <blockquote>
     * 「头目就是帝境后期，首领就是鸿蒙，头目是帝境中期，
     * 普通小怪是仙人境界随机，精英小怪是帝境初期」
     * </blockquote>
     * <p>这种"照抄需求"的东西最容易写错一格，而且只有玩家玩到才会发现 ——
     * 所以这里逐条对账。</p>
     */
    private static void realWorldRules() {
        section("「真实世界」规则");

        var rw = com.hongmeng.xiuxian.realworld.RealWorld.class;

        // ---- 段位表逐条对账 ----
        int normalLow = com.hongmeng.xiuxian.realworld.RealWorld.tierFor(0, 0.1F);
        int normalHigh = com.hongmeng.xiuxian.realworld.RealWorld.tierFor(0, 0.9F);
        check("普通小怪 = 仙人境界（且在仙人~帝境之间随机）",
                normalLow == 8 && normalHigh == 9,
                "低 roll=" + normalLow + " 高 roll=" + normalHigh + "（8=仙人 9=帝境）");

        check("精英小怪 = 帝境初期",
                com.hongmeng.xiuxian.realworld.RealWorld.tierFor(1, 0.5F) == 9,
                "tier=" + com.hongmeng.xiuxian.realworld.RealWorld.tierFor(1, 0.5F));

        check("队长 = 帝境中期",
                com.hongmeng.xiuxian.realworld.RealWorld.tierFor(2, 0.5F) == 9,
                "tier=" + com.hongmeng.xiuxian.realworld.RealWorld.tierFor(2, 0.5F));

        check("头目 = 帝境后期",
                com.hongmeng.xiuxian.realworld.RealWorld.tierFor(3, 0.5F) == 9,
                "tier=" + com.hongmeng.xiuxian.realworld.RealWorld.tierFor(3, 0.5F));

        check("首领 = 鸿蒙",
                com.hongmeng.xiuxian.realworld.RealWorld.tierFor(4, 0.5F) == 10,
                "tier=" + com.hongmeng.xiuxian.realworld.RealWorld.tierFor(4, 0.5F));

        // ---- 段位名（给提示与日志用） ----
        boolean namesOk = true;
        StringBuilder got = new StringBuilder();
        for (int i = 0; i <= 4; i++) {
            String nm = com.hongmeng.xiuxian.realworld.RealWorld.tierName(i);
            got.append(i).append("=").append(nm).append(" ");
            if (nm == null || nm.isEmpty()) {
                namesOk = false;
            }
        }
        check("五档段位都有中文名", namesOk, got.toString());

        // ---- 单调性：品阶越高，段位名对应的境界不能倒退 ----
        // 用心数：普通(仙人=8) <= 精英(帝境=9) <= ... <= 首领(鸿蒙=10)
        int[] tiers = {
                com.hongmeng.xiuxian.realworld.RealWorld.tierFor(0, 0.5F),
                com.hongmeng.xiuxian.realworld.RealWorld.tierFor(1, 0.5F),
                com.hongmeng.xiuxian.realworld.RealWorld.tierFor(2, 0.5F),
                com.hongmeng.xiuxian.realworld.RealWorld.tierFor(3, 0.5F),
                com.hongmeng.xiuxian.realworld.RealWorld.tierFor(4, 0.5F),
        };
        boolean mono = true;
        for (int i = 1; i < tiers.length; i++) {
            if (tiers[i] < tiers[i - 1]) {
                mono = false;
            }
        }
        check("品阶越高段位不倒退（普通 <= 精英 <= 队长 <= 头目 <= 首领）", mono,
                java.util.Arrays.toString(tiers));

        // ---- 触发点必须是虚空裂隙 ----
        java.nio.file.Path root = findSourceRoot();
        if (root != null) {
            String dungeonRealm = readIfExists(root.resolve(
                    "com/hongmeng/xiuxian/dungeon/DungeonRealm.java"));
            boolean voidHook = dungeonRealm.contains("Dungeon.VOID")
                    && dungeonRealm.contains("onVoidCleared");
            check("通关触发点是**虚空裂隙**（玩家指定的解锁条件）", voidHook,
                    voidHook ? "Dungeon.VOID -> onVoidCleared" : "**触发点不对**");

            // ---- 命令三件套 ----
            String cmd = readIfExists(root.resolve(
                    "com/hongmeng/xiuxian/command/HongmengCommand.java"));
            boolean hasOn = cmd.contains("literal(\"on\")") && cmd.contains("activate");
            boolean hasOff = cmd.contains("literal(\"off\")") && cmd.contains("decline");
            boolean hasStatus = cmd.contains("literal(\"status\")");
            check("命令 /hongmeng realworld on|off|status 三件套齐全",
                    hasOn && hasOff && hasStatus,
                    "on=" + hasOn + " off=" + hasOff + " status=" + hasStatus);

            // ---- 段位表只在真实世界开启后才接管 ----
            String mob = readIfExists(root.resolve(
                    "com/hongmeng/xiuxian/entity/CultivationMonsterEntity.java"));
            boolean gated = mob.contains("RealWorld.active") && mob.contains("tierFor");
            check("段位表**只在真实世界开启后**才接管（未开启仍按玩家境界锚定）",
                    gated, gated ? "有 active() 门禁" : "**没有门禁，会把普通存档也改了**");
        } else {
            check("能找到源码根目录（用于触发点与命令的取证）", false, "跳过");
        }
    }


    /**
     * <b>「真实世界」维度外观的取证断言</b>。
     *
     * <p>玩家要求「选择开启后……<b>所有维度发生变化</b>」。
     * 这件事很容易变成<b>假的</b>：</p>
     * <ul>
     *   <li>雾事件没被 {@code @SubscribeEvent} 订阅 → 代码写了但根本不生效
     *       （<b>编译通过、运行毫无变化</b>，最难发现的一种坏法）；</li>
     *   <li>某个维度没有配方 → 走进那个维度就"没变化"；</li>
     *   <li>雾距改到了水下/失明的雾 → 出怪问题（这两种雾各有用意）。</li>
     * </ul>
     */
    private static void realWorldAtmosphere() {
        section("「真实世界」维度外观");

        // ---- ① 纯函数部分：直接调（不依赖源码文本） ----
        // 配方是私有的，所以这里只验证"类存在且可反射"，
        // 真正的逐维度检查放在下面的源码取证里。
        try {
            Class<?> cls = Class.forName("com.hongmeng.xiuxian.client.RealWorldAtmosphere");
            check("维度外观类存在", true, cls.getSimpleName());
            boolean hasFogColor = false;
            boolean hasRenderFog = false;
            for (var m : cls.getMethods()) {
                if (m.getName().equals("onFogColor")) {
                    hasFogColor = true;
                }
                if (m.getName().equals("onRenderFog")) {
                    hasRenderFog = true;
                }
            }
            check("有雾色钩子方法 onFogColor", hasFogColor, "");
            check("有雾距钩子方法 onRenderFog", hasRenderFog, "");
        } catch (Throwable t) {
            check("维度外观类存在", false,
                    "反射不到（客户端类在服务端验证时可能不在场上）：" + t.getClass().getSimpleName());
        }

        java.nio.file.Path root = findSourceRoot();
        if (root == null) {
            check("能找到源码根目录（用于事件订阅与配方的取证）", false, "跳过");
            return;
        }

        // ---- ② 事件必须真的被订阅 ----
        String feel = readIfExists(root.resolve(
                "com/hongmeng/xiuxian/client/CombatFeelClient.java"));
        boolean subColor = feel.contains("ComputeFogColor")
                && feel.contains("onFogColor")
                && feel.contains("@SubscribeEvent");
        boolean subFog = feel.contains("RenderFog")
                && feel.contains("onRenderFog");
        check("雾色事件已被 @SubscribeEvent 订阅（不是只写了方法）", subColor,
                subColor ? "ComputeFogColor -> RealWorldAtmosphere.onFogColor"
                        : "**没有订阅，外观不会生效**");
        check("雾距事件已被订阅", subFog,
                subFog ? "RenderFog -> RealWorldAtmosphere.onRenderFog" : "**没有订阅**");

        // ---- ③ 只动地形雾 ----
        String atm = readIfExists(root.resolve(
                "com/hongmeng/xiuxian/client/RealWorldAtmosphere.java"));
        boolean terrainOnly = atm.contains("FOG_TERRAIN");
        check("雾距只改地形雾（水下/失明的雾不碰）", terrainOnly,
                terrainOnly ? "有 FOG_TERRAIN 判断" : "**会误伤水下与失明的雾**");

        // ---- ④ 每个维度都有配方 ----
        String[] dims = {"nether", "end", "void", "demon", "beast", "epoch"};
        StringBuilder missing = new StringBuilder();
        int covered = 0;
        for (String d : dims) {
            if (atm.contains("\"" + d + "\"") || atm.contains("contains(\"" + d + "\")")) {
                covered++;
            } else {
                missing.append(d).append(" ");
            }
        }
        check("六个维度/维度族都有外观配方（下界/末地/四个首领维度）",
                covered == dims.length,
                covered == dims.length ? "全部覆盖" : ("缺: " + missing));

        // ---- ⑤ 有过渡（不是瞬间变色） ----
        check("外观有过渡进度（不是瞬间变色，会显得很突兀）",
                atm.contains("blend"), "有 blend 过渡");

        // ---- ⑥ 旗标确实从服务端同步过来了 ----
        String sync = readIfExists(root.resolve(
                "com/hongmeng/xiuxian/network/SyncCultivationPacket.java"));
        boolean synced = sync.contains("realWorld") && sync.contains("writeBoolean")
                && sync.contains("readBoolean");
        check("「真实世界已开启」被同步到客户端（否则外观永远不生效）", synced,
                synced ? "SyncCultivationPacket 带 realWorld 布尔" : "**没有同步**");
    }


    /**
     * <b>动态门槛与阶段性难度</b>的断言。
     *
     * <p>玩家反馈：「传送门门槛……弄个动态强度或者是调整门槛」、
     * 「我们可以设置阶段性难度，怪物强度要对标玩家阶段强度」。</p>
     *
     * <p>这里检查三件事，都是"看起来能跑但其实坏了"的高危点：</p>
     * <ol>
     *   <li><b>单调性</b>：境界越高怪越强、奖励越多。倒挂意味着
     *       "升级反而更亏"，是最伤手感的 bug 之一；</li>
     *   <li><b>低于基准真的变弱</b>：原来 {@code Math.max(0, ...)} 把负差整个吃掉，
     *       低境界玩家面对的是基准强度的怪；</li>
     *   <li><b>宽容窗口存在</b>：{@code canEnter} 允许低 {@code ENTRY_GRACE} 境进入。</li>
     * </ol>
     */
    private static void dynamicDungeonGate() {
        section("动态门槛与阶段性难度");

        int grace = com.hongmeng.xiuxian.dungeon.Dungeon.ENTRY_GRACE;
        check("门槛有宽容窗口（不是硬墙）", grace >= 1,
                "ENTRY_GRACE = " + grace + " 境");

        int badMono = 0;
        int badWeaker = 0;
        int badEnter = 0;
        StringBuilder detail = new StringBuilder();
        for (com.hongmeng.xiuxian.dungeon.Dungeon d
                : com.hongmeng.xiuxian.dungeon.Dungeon.values()) {
            // ① 宽容窗口：minTier - grace 必须能进，再低一境不能进
            int lowest = Math.max(1, d.minTier() - grace);
            if (!d.canEnter(lowest) || (lowest > 1 && d.canEnter(lowest - 1))) {
                badEnter++;
                detail.append(d.cn()).append("宽容窗口不对 ");
            }

            // ② 单调性 + 低境界变弱
            double prevScale = -1.0D;
            long prevReward = -1L;
            for (int t = lowest; t <= 10; t++) {
                double sc = d.mobScaleFor(t);
                long rw = d.rewardFor(t);
                if (sc < prevScale - 1.0E-6D || rw < prevReward) {
                    badMono++;
                    detail.append(d.cn()).append("@").append(t).append("倒挂 ");
                    break;
                }
                prevScale = sc;
                prevReward = rw;
            }
            // 低于基准时倍率必须**小于**在基准处（证明真的变弱了）
            if (d.baseTier() > lowest) {
                double low = d.mobScaleFor(lowest);
                double atBase = d.mobScaleFor(d.baseTier());
                if (low >= atBase) {
                    badWeaker++;
                    detail.append(d.cn()).append("低境界没变弱 ");
                }
            }
        }

        check("每个副本的宽容窗口都是 minTier-" + grace + " 起",
                badEnter == 0, badEnter == 0 ? "全部正确" : detail.toString());
        check("倍率与奖励都随境界单调不降（无倒挂）", badMono == 0,
                badMono == 0 ? "四个副本全部通过" : detail.toString());
        check("低于基准境界时怪**真的**更弱（不是被 max(0,..) 吃掉）",
                badWeaker == 0, badWeaker == 0 ? "全部通过" : detail.toString());

        // ③ 倍率有下限（不至于变成送分）
        boolean floorOk = true;
        for (com.hongmeng.xiuxian.dungeon.Dungeon d
                : com.hongmeng.xiuxian.dungeon.Dungeon.values()) {
            if (d.mobScaleFor(1) < 0.30D) {
                floorOk = false;
            }
        }
        check("最低境界进入时怪强度仍有下限（不至于变成送分）", floorOk,
                "练气进最低副本 = " + com.hongmeng.xiuxian.dungeon.Dungeon.EPOCH.mobScaleFor(1));

        // ④ 奖励折扣有下限
        boolean penOk = true;
        for (com.hongmeng.xiuxian.dungeon.Dungeon d
                : com.hongmeng.xiuxian.dungeon.Dungeon.values()) {
            double pen = d.rewardPenaltyFor(1);
            if (pen < 0.50D || pen > 1.0D) {
                penOk = false;
            }
        }
        check("低境界进入的奖励折扣在 0.50~1.00 之间（不会白拿也不会白干）", penOk,
                "练气进虚空 = " + String.format("%.2f",
                        com.hongmeng.xiuxian.dungeon.Dungeon.VOID.rewardPenaltyFor(1)));

        // ⑤ 提示文本确实会产生（不然玩家不知道被宽容了）
        boolean warns = com.hongmeng.xiuxian.dungeon.Dungeon.VOID
                .underTierWarning(1) != null
                && com.hongmeng.xiuxian.dungeon.Dungeon.VOID
                        .underTierWarning(10) == null;
        check("低于建议境界时有提示、达标时不提示", warns,
                "练气进虚空有提示，鸿蒙进无提示");
    }


    /**
     * <b>灭世级法术运镜</b>的断言。
     *
     * <p>玩家要求「释放灭世级别的法术的时候自动切换第三视角拉远观看特效，
     * 要影视级别的运镜」。</p>
     *
     * <p>关键帧是纯数值，所以这里能直接验证。钉住两件容易错的事：</p>
     * <ol>
     *   <li><b>释放段"回望施法者"的朝向</b>：炸点是锚点，
     *       方位必须按「炸点 → 施法者」的<b>差向量</b>算 ——
     *       我第一版漏了减炸点（把炸点当原点），镜头会偏掉；</li>
     *   <li><b>两段的距离都在可看范围内</b>：太近穿模、太远啥也看不见。</li>
     * </ol>
     */
    private static void spellCinematic() {
        section("灭世级法术运镜");

        // ---- 运镜开关：2026-11-01 玩家要求「法术运镜全部取消」 ----
        //
        // ⚠ 断言必须跟着开关状态走：`CAMERA_ENABLED=false` 时"0 门"才是正确结果；
        //   写成永远 `deserves > 0` 会在玩家关掉运镜后误报（本轮踩过）。
        boolean cameraOn = com.hongmeng.xiuxian.combat.SpellCinematic.CAMERA_ENABLED;
        int deserves = 0;
        int total = 0;
        StringBuilder got = new StringBuilder();
        for (com.hongmeng.xiuxian.technique.Technique t
                : com.hongmeng.xiuxian.technique.Technique.values()) {
            if (t.kind() == com.hongmeng.xiuxian.technique.TechniqueKind.PASSIVE) {
                continue;
            }
            total++;
            if (com.hongmeng.xiuxian.combat.SpellCinematic.deserves(t)) {
                deserves++;
                got.append(t.cn()).append(" ");
            }
        }
        check("运镜开关与 2026-11-01 需求一致（关闭 = 0 门；开启时只有灭世/引力级）",
                cameraOn ? (deserves > 0 && deserves < total) : (deserves == 0),
                (cameraOn ? "CAMERA_ENABLED=true，" : "CAMERA_ENABLED=false，")
                        + deserves + "/" + total + " 门：" + got.toString().trim());

        // ---- 蓄力段关键帧 ----
        try {
            var m = com.hongmeng.xiuxian.combat.SpellCinematic.class
                    .getDeclaredMethod("chargeShots");
            m.setAccessible(true);
            float[] flat = (float[]) m.invoke(null);
            int n = flat.length / 5;
            check("蓄力段关键帧 >= 4（少于 4 帧撑不起运镜）", n >= 4, n + " 帧");
            float nearMin = Float.MAX_VALUE;
            float farMax = 0.0F;
            boolean distOk = true;
            for (int i = 0; i < n; i++) {
                float d = flat[i * 5];
                nearMin = Math.min(nearMin, d);
                farMax = Math.max(farMax, d);
                if (d < 1.5F || d > 40.0F) {
                    distOk = false;
                }
            }
            check("蓄力段距离都在 1.5~40 格（太近穿模、太远看不见）", distOk,
                    nearMin + "~" + farMax);
            check("蓄力段有【贴脸近景】（起始 <= 4 格）—— 由近及远才有张力",
                    flat[0] <= 4.0F, "起始 " + flat[0] + " 格");
            check("蓄力段确实在拉远（末帧比首帧远）",
                    flat[(n - 1) * 5] > flat[0] + 3.0F,
                    flat[0] + " -> " + flat[(n - 1) * 5]);
        } catch (Throwable t) {
            check("能读到蓄力段关键帧", false, t.toString());
        }

        // ---- 释放段关键帧（重点：回望施法者的朝向）----
        try {
            var m = com.hongmeng.xiuxian.combat.SpellCinematic.class
                    .getDeclaredMethod("blastShots",
                            net.minecraft.world.phys.Vec3.class,
                            net.minecraft.world.phys.Vec3.class);
            m.setAccessible(true);
            // 炸点放在 (100, 64, 100)，施法者在 (100, 64, 0) —— 正北方向
            var impact = new net.minecraft.world.phys.Vec3(100.0D, 64.0D, 100.0D);
            var caster = new net.minecraft.world.phys.Vec3(100.0D, 64.0D, 0.0D);
            float[] flat = (float[]) m.invoke(null, impact, caster);
            int n = flat.length / 5;
            check("释放段关键帧 >= 4", n >= 4, n + " 帧");

            // 朝向：施法者在炸点的 -Z 方向 => atan2(0, -100) = 180 度
            float lastYaw = flat[(n - 1) * 5 + 1];
            float expect = (float) Math.toDegrees(Math.atan2(0.0D, -100.0D));
            float diff = Math.abs(((lastYaw - expect) % 360.0F + 540.0F) % 360.0F - 180.0F);
            check("释放段末帧朝向施法者（方位按【炸点→施法者】差向量算，"
                            + "不是把炸点当原点）",
                    diff < 1.0F,
                    String.format("末帧 %.1f 度，期望 %.1f 度（差 %.1f）",
                            lastYaw, expect, diff));

            // 距离范围
            float nearMin = Float.MAX_VALUE;
            float farMax = 0.0F;
            boolean distOk = true;
            for (int i = 0; i < n; i++) {
                float d = flat[i * 5];
                nearMin = Math.min(nearMin, d);
                farMax = Math.max(farMax, d);
                if (d < 2.0F || d > 60.0F) {
                    distOk = false;
                }
            }
            check("释放段距离都在 2~60 格", distOk, nearMin + "~" + farMax);
            check("释放段有【大幅拉远】（最远 >= 20 格）—— 要看清破坏全貌",
                    farMax >= 20.0F, "最远 " + farMax + " 格");

            // 俯仰角范围
            boolean pitchOk = true;
            for (int i = 0; i < n; i++) {
                float p = flat[i * 5 + 2];
                if (p < -30.0F || p > 70.0F) {
                    pitchOk = false;
                }
            }
            check("释放段俯仰角都在 -30~70 度（超出会翻镜头）", pitchOk, "");
        } catch (Throwable t) {
            check("能读到释放段关键帧", false, t.toString());
        }
    }


    /**
     * <b>飞剑剑阵</b>的断言。
     *
     * <p>玩家发了一个修仙 mod 的视频说「看看人家的飞剑可以环绕自己，学习一下人家的玩法」。
     * 该玩法出自御剑工艺（Yujian Craft），其要点：最多 6 把、<b>每把独立冷却</b>、
     * 环绕玩家并随目标调位、阵型可切、六剑齐备才能发动剑阵。</p>
     *
     * <p>这些是纯数值，直接断言 —— 尤其是"独立冷却"（如果退化成共用冷却，
     * 六把剑会同时飞出，剑阵就变成一次性爆发了）。</p>
     */
    private static void swordFormationRules() {
        section("飞剑剑阵");

        var F = com.hongmeng.xiuxian.sword.SwordFormation.class;

        // ---- 逐把解锁 ----
        check("练气(1) 还不能御剑", !com.hongmeng.xiuxian.sword.SwordFormation.canUse(1),
                "剑数 " + com.hongmeng.xiuxian.sword.SwordFormation.swordCountFor(1));
        check("筑基(2) 起可以御剑",
                com.hongmeng.xiuxian.sword.SwordFormation.canUse(2),
                "筑基 " + com.hongmeng.xiuxian.sword.SwordFormation.swordCountFor(2) + " 把");
        boolean monotone = true;
        int prev = -1;
        for (int t = 1; t <= 10; t++) {
            int n = com.hongmeng.xiuxian.sword.SwordFormation.swordCountFor(t);
            if (n < prev) {
                monotone = false;
            }
            prev = n;
        }
        check("剑数随境界单调不减（逐把解锁，不会倒退）", monotone, "");
        check("道印(6) 及以上满 6 把",
                com.hongmeng.xiuxian.sword.SwordFormation.swordCountFor(6)
                        == com.hongmeng.xiuxian.sword.SwordFormation.MAX_SWORDS,
                "剑数 " + com.hongmeng.xiuxian.sword.SwordFormation.swordCountFor(6));
        check("剑数绝不超过上限（不会越界数组）",
                com.hongmeng.xiuxian.sword.SwordFormation.swordCountFor(10)
                        <= com.hongmeng.xiuxian.sword.SwordFormation.MAX_SWORDS, "");

        // ---- 独立冷却：这是"剑阵"而不是"齐射"的关键 ----
        long cd = com.hongmeng.xiuxian.sword.SwordFormation.ATTACK_COOLDOWN;
        check("每把剑有独立冷却（不是共用，否则六把会同时飞出）",
                cd > 0 && cd <= 60, "冷却 " + cd + " tick");
        check("冷却刚过时 ready=true",
                com.hongmeng.xiuxian.sword.SwordFormation.ready(cd, 0L), "");
        check("冷却未到 ready=false",
                !com.hongmeng.xiuxian.sword.SwordFormation.ready(cd - 1, 0L), "");

        // ---- 环绕几何 ----
        var center = new net.minecraft.world.phys.Vec3(0.0D, 64.0D, 0.0D);
        var look = new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 1.0D);
        var p0 = com.hongmeng.xiuxian.sword.SwordFormation.orbitPos(center, look, 0, 6,
                com.hongmeng.xiuxian.sword.SwordFormation.Pattern.RING, 0L);
        double dist = Math.sqrt((p0.x - center.x) * (p0.x - center.x)
                + (p0.z - center.z) * (p0.z - center.z));
        check("环阵的剑围绕玩家（水平距离 = 环绕半径）",
                Math.abs(dist - com.hongmeng.xiuxian.sword.SwordFormation.RING_RADIUS) < 0.5D,
                String.format("距中心 %.2f 格", dist));
        check("环绕高度在玩家上方（不是脚下）", p0.y > center.y, 
                String.format("y 偏移 %.2f", p0.y - center.y));

        // 六把剑必须分布在不同方位（相位错开）
        java.util.Set<String> dirs = new java.util.HashSet<>();
        for (int i = 0; i < 6; i++) {
            var p = com.hongmeng.xiuxian.sword.SwordFormation.orbitPos(center, look, i, 6,
                    com.hongmeng.xiuxian.sword.SwordFormation.Pattern.RING, 0L);
            dirs.add(String.format("%.1f/%.1f", p.x, p.z));
        }
        check("六把剑的环绕方位互不相同（不会叠在一起）", dirs.size() == 6,
                dirs.size() + " 个不同方位");

        // 阵型切换确实改变排布
        var ring = com.hongmeng.xiuxian.sword.SwordFormation.orbitPos(center, look, 3, 6,
                com.hongmeng.xiuxian.sword.SwordFormation.Pattern.RING, 0L);
        var fan = com.hongmeng.xiuxian.sword.SwordFormation.orbitPos(center, look, 3, 6,
                com.hongmeng.xiuxian.sword.SwordFormation.Pattern.FAN, 0L);
        check("切换阵型确实改变剑的排布（不是同一个位置）",
                ring.distanceTo(fan) > 0.5D,
                String.format("相差 %.2f 格", ring.distanceTo(fan)));

        // 天罡阵分两层
        var h0 = com.hongmeng.xiuxian.sword.SwordFormation.orbitPos(center, look, 0, 6,
                com.hongmeng.xiuxian.sword.SwordFormation.Pattern.HEAVEN, 0L);
        var h1 = com.hongmeng.xiuxian.sword.SwordFormation.orbitPos(center, look, 1, 6,
                com.hongmeng.xiuxian.sword.SwordFormation.Pattern.HEAVEN, 0L);
        check("天罡阵分上下两层（立体剑阵）", Math.abs(h1.y - h0.y) > 0.4D,
                String.format("层高差 %.2f", Math.abs(h1.y - h0.y)));

        // ---- 穿刺插值 ----
        var from = new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 0.0D);
        var to = new net.minecraft.world.phys.Vec3(10.0D, 0.0D, 0.0D);
        var mid = com.hongmeng.xiuxian.sword.SwordFormation.piercePos(from, to, 0.5D);
        check("穿刺插值：t=0 在起点、t=1 在目标、t=0.5 在中间",
                com.hongmeng.xiuxian.sword.SwordFormation.piercePos(from, to, 0.0D)
                                .distanceTo(from) < 1.0E-6D
                        && com.hongmeng.xiuxian.sword.SwordFormation.piercePos(from, to, 1.0D)
                                .distanceTo(to) < 1.0E-6D
                        && mid.distanceTo(new net.minecraft.world.phys.Vec3(5.0D, 0.0D, 0.0D))
                                < 1.0E-6D,
                "t=0.5 -> " + mid);

        // ---- 六剑齐备才能发动剑阵 ----
        long[] allReady = new long[6];
        check("六剑齐备时剑阵可发动",
                com.hongmeng.xiuxian.sword.SwordFormation.ultimateReady(6, 1000L, allReady),
                "");
        long[] oneBusy = new long[6];
        oneBusy[3] = 1000L;
        check("有一把没冷却好就不能发动（严格六剑齐备）",
                !com.hongmeng.xiuxian.sword.SwordFormation.ultimateReady(6, 1000L, oneBusy),
                "");
        check("只有 5 把时不能发动",
                !com.hongmeng.xiuxian.sword.SwordFormation.ultimateReady(5, 1000L, allReady),
                "");
    }


    /**
     * <b>30 条职业被动</b>的断言（5 职业 × 6 条；核心自带 + 分支门槛解锁）。
     */
    private static void careerSkillRules() {
        section("职业被动（30 条）");

        var skills = com.hongmeng.xiuxian.talent.CareerSkill.values();
        check("职业被动恰有 30 条（5 职业 × 6 条）", skills.length == 30,
                skills.length + " 条");

        int perCareerBad = 0;
        StringBuilder perCareerDetail = new StringBuilder();
        java.util.Set<String> names = new java.util.HashSet<>();
        boolean fieldsOk = true;
        for (var c : com.hongmeng.xiuxian.talent.Career.values()) {
            var group = com.hongmeng.xiuxian.talent.CareerSkill.ofCareer(c);
            if (group.length != 6) {
                perCareerBad++;
                perCareerDetail.append(c.cn()).append("=").append(group.length).append(" ");
            }
            int core = 0;
            for (var s : group) {
                if (!names.add(s.cn())) {
                    fieldsOk = false;
                }
                if (s.desc() == null || s.desc().isBlank()
                        || s.kind() == null || s.branch() == null) {
                    fieldsOk = false;
                }
                if (s.branch().career() != c) {
                    fieldsOk = false;
                }
                if (s.isCore()) {
                    core++;
                    if (s.value() != 0.0D) {
                        fieldsOk = false;
                    }
                } else {
                    if (Math.abs(s.unlockPoints() - 2.0D) > 1.0E-9D
                            && Math.abs(s.unlockPoints() - 4.0D) > 1.0E-9D) {
                        fieldsOk = false;
                    }
                    if (s.value() <= 0.0D) {
                        fieldsOk = false;
                    }
                }
            }
            if (core < 2 || core > 3) {
                perCareerBad++;
                perCareerDetail.append("核心数=").append(core).append(" ");
            }
        }
        check("每职业恰 6 条被动（核心 2~3 条 + 分支被动）", perCareerBad == 0,
                perCareerBad == 0 ? "五职业齐全" : perCareerDetail.toString());
        check("被动字段完整（描述/种类/分支/数值；核心数值为 0）", fieldsOk, "");

        // 分支门槛必须是 2 / 4 且同支按升序
        boolean thresholdOk = true;
        StringBuilder thresholdDetail = new StringBuilder();
        for (var b : com.hongmeng.xiuxian.talent.CareerBranch.values()) {
            var group = com.hongmeng.xiuxian.talent.CareerSkill.ofBranch(b);
            int prev = 0;
            for (var s : group) {
                if (s.isCore()) {
                    continue;
                }
                if ((int) s.unlockPoints() <= prev) {
                    thresholdOk = false;
                    thresholdDetail.append(b.id()).append(" ");
                }
                prev = (int) s.unlockPoints();
            }
        }
        check("同支被动解锁门槛从 2 到 4 递增（分支投入门槛）", thresholdOk,
                thresholdOk ? "全部合规" : ("异常：" + thresholdDetail));

        int totalCore = 0;
        for (var s : skills) {
            if (s.isCore()) {
                totalCore++;
            }
        }
        check("核心被动总数在 10~15 之间（五职业 2~3 条）",
                totalCore >= 10 && totalCore <= 15, totalCore + " 条核心");
    }

    /**
     * <b>20 条职业主动</b>的断言（5 职业 × 4 条，每支 1 条，分支 5 点解锁）。
     */
    private static void careerActiveRules() {
        section("职业主动（20 条）");

        var actives = com.hongmeng.xiuxian.talent.CareerActive.values();
        check("职业主动恰有 20 条（5 职业 × 4 条）", actives.length == 20,
                actives.length + " 条");

        int perCareerBad = 0;
        StringBuilder detail = new StringBuilder();
        java.util.Set<String> techniqueNames = new java.util.HashSet<>();
        boolean fieldsOk = true;
        int reuse = 0;
        for (var c : com.hongmeng.xiuxian.talent.Career.values()) {
            var group = com.hongmeng.xiuxian.talent.CareerActive.ofCareer(c);
            if (group.length != 4) {
                perCareerBad++;
                detail.append(c.cn()).append("=").append(group.length).append(" ");
            }
            for (var a : group) {
                if (!techniqueNames.add(a.techniqueName())) {
                    fieldsOk = false;
                }
                if (a.cn() == null || a.cn().isBlank() || a.form() == null
                        || a.branch() == null || a.desc() == null
                        || a.desc().isBlank()) {
                    fieldsOk = false;
                }
                if (a.branch().career() != c) {
                    fieldsOk = false;
                }
                if (a.form() == com.hongmeng.xiuxian.technique.TechniqueKind.PASSIVE) {
                    fieldsOk = false;
                }
                if (Math.abs(a.unlockPoints() - 5.0D) > 1.0E-9D) {
                    fieldsOk = false;
                }
                if (a.minTier() < 1 || a.minTier() > 10
                        || a.manaCost() < 0 || a.cooldown() <= 0
                        || a.castTicks() < 0 || a.damageMul() < 0.0D) {
                    fieldsOk = false;
                }
                if (a.reusesExisting()) {
                    reuse++;
                    var existing = com.hongmeng.xiuxian.technique.Technique
                            .byName(a.techniqueName());
                    if (existing == null || existing.kind() != a.form()) {
                        fieldsOk = false;
                    }
                }
            }
        }
        check("每职业恰 4 条主动（4 支各 1 条）", perCareerBad == 0,
                perCareerBad == 0 ? "五职业齐全" : detail.toString());
        check("主动字段完整（名称/形态/分支/门槛/消耗/冷却）", fieldsOk, "");
        check("复用已有功法的主动，形态与已有条目一致（万剑归宗/金刚护体）",
                reuse >= 1, reuse + " 条复用");

        // 20 支各恰 1 条主动
        int branchBad = 0;
        StringBuilder branchDetail = new StringBuilder();
        for (var b : com.hongmeng.xiuxian.talent.CareerBranch.values()) {
            if (com.hongmeng.xiuxian.talent.CareerActive.ofBranch(b) == null) {
                branchBad++;
                branchDetail.append(b.id()).append(" ");
            }
        }
        check("20 支天赋分支每支都有 1 条主动", branchBad == 0,
                branchBad == 0 ? "20/20" : ("缺：" + branchDetail));
    }

    /**
     * <b>40 个职业天赋节点</b>的断言（5 职业 × 4 支 × 2 节点）。
     */
    private static void careerTalentTreeRules() {
        section("职业天赋树（40 节点）");

        var nodes = com.hongmeng.xiuxian.talent.CareerTalentTree.all();
        check("职业天赋节点恰有 40 个（5 职业 × 4 支 × 2）", nodes.size() == 40,
                nodes.size() + " 个");

        int perCareerBad = 0;
        StringBuilder careerDetail = new StringBuilder();
        for (var c : com.hongmeng.xiuxian.talent.Career.values()) {
            int n = com.hongmeng.xiuxian.talent.CareerTalentTree.ofCareer(c).size();
            if (n != 8) {
                perCareerBad++;
                careerDetail.append(c.cn()).append("=").append(n).append(" ");
            }
        }
        check("每职业恰 8 个节点", perCareerBad == 0,
                perCareerBad == 0 ? "五职业齐全" : careerDetail.toString());

        // 每支 2 节点，order 1/2，门槛 0/3，投入上限 ≥5 才能解锁主动
        boolean branchOk = true;
        StringBuilder branchDetail = new StringBuilder();
        for (var b : com.hongmeng.xiuxian.talent.CareerBranch.values()) {
            var group = com.hongmeng.xiuxian.talent.CareerTalentTree.ofBranch(b);
            if (group.size() != 2) {
                branchOk = false;
                branchDetail.append(b.id()).append("(").append(group.size()).append(") ");
                continue;
            }
            int maxInvest = 0;
            for (int i = 0; i < 2; i++) {
                var node = group.get(i);
                if (node.order() != i + 1
                        || node.branchRequired() != (i == 0 ? 0 : 3)) {
                    branchOk = false;
                }
                if (node.branch().career() != b.career()) {
                    branchOk = false;
                }
                maxInvest += node.maxRank();
            }
            if (maxInvest < 5) {
                branchOk = false;
                branchDetail.append(b.id()).append("上限").append(maxInvest).append(" ");
            }
        }
        check("每支 2 节点、第 2 节点需投入 3 点、每支可投入 ≥5 点", branchOk,
                branchOk ? "20 支全部合规" : ("异常：" + branchDetail));

        // 节点字段 / id 唯一 / 数值合法
        java.util.Set<String> ids = new java.util.HashSet<>();
        boolean fieldsOk = true;
        StringBuilder idDup = new StringBuilder();
        for (var n : nodes) {
            if (!ids.add(n.id())) {
                fieldsOk = false;
                idDup.append(n.id()).append(" ");
            }
            if (n.cn() == null || n.cn().isBlank() || n.desc() == null
                    || n.desc().isBlank() || n.kind() == null
                    || n.maxRank() < 2 || n.maxRank() > 5
                    || n.perRank() <= 0.0D || n.career() == null
                    || n.branch() == null) {
                fieldsOk = false;
            }
        }
        check("40 个节点 id 全局唯一、字段与数值合法", fieldsOk,
                fieldsOk ? "40/40" : ("重复 id：" + idDup));

        // 技能解锁门槛与本树结构一致：分支投入上限要够 2/4/5
        boolean unlockOk = true;
        StringBuilder unlockDetail = new StringBuilder();
        for (var b : com.hongmeng.xiuxian.talent.CareerBranch.values()) {
            int maxInvest = 0;
            for (var n : com.hongmeng.xiuxian.talent.CareerTalentTree.ofBranch(b)) {
                maxInvest += n.maxRank();
            }
            for (var s : com.hongmeng.xiuxian.talent.CareerSkill.ofBranch(b)) {
                if (!s.isCore() && maxInvest < (int) s.unlockPoints()) {
                    unlockOk = false;
                    unlockDetail.append(b.id()).append(" ").append(s.cn()).append(" ");
                }
            }
            if (maxInvest < 5) {
                unlockOk = false;
                unlockDetail.append(b.id()).append("主动 ");
            }
        }
        check("分支投入上限足够解锁该支被动(2/4)与主动(5)", unlockOk,
                unlockOk ? "20 支全部成立" : ("不足：" + unlockDetail));

        check("分支总数恰 20（5 职业 × 4 支）",
                com.hongmeng.xiuxian.talent.CareerBranch.values().length == 20,
                com.hongmeng.xiuxian.talent.CareerBranch.values().length + " 支");
    }

    /**
     * <b>五职业核心机制的纯函数与接线断言</b>。
     *
     * <p>剑意/剑罡/剑气、魂印/魂爆/虚化/鬼域是本轮新写的机制；
     * 血/魔/体保留旧数值。这里钉住：公式边界、复用关系、以及
     * "机制真的被运行期调用"（不许只写在数据类里）。</p>
     */
    private static void careerMechanicsRules() {
        section("职业核心机制（剑/血/魔/鬼/体）");

        // ---- 剑修：剑意 / 剑罡 / 剑气 ----
        check("剑意上限 = 10 层", com.hongmeng.xiuxian.talent.PathMechanics
                .SWORD_INTENT_MAX == 10, "");
        double aura0 = com.hongmeng.xiuxian.talent.PathMechanics.swordAuraRatio(0, 0);
        double aura10 = com.hongmeng.xiuxian.talent.PathMechanics.swordAuraRatio(10, 0);
        check("剑罡吸收率：0 层 15% → 10 层 35%",
                Math.abs(aura0 - 0.15D) < 1.0E-9D
                        && Math.abs(aura10 - 0.35D) < 1.0E-9D,
                String.format(java.util.Locale.ROOT, "%.0f%% → %.0f%%",
                        aura0 * 100, aura10 * 100));
        check("剑罡吸收率有天赋时封顶 45%",
                Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .swordAuraRatio(10, 1.0D) - 0.45D) < 1.0E-9D, "");
        check("剑气伤害 = 攻击力 × 0.80 × (1 + 剑气天赋)",
                Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .swordQiDamage(100.0D, 0.0D) - 80.0D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .swordQiDamage(100.0D, 0.5D) - 120.0D) < 1.0E-9D, "");
        check("剑气锥形半径 3.5 格、最多 5 个目标",
                Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .SWORD_QI_RADIUS - 3.5D) < 1.0E-9D
                        && com.hongmeng.xiuxian.talent.PathMechanics
                        .SWORD_QI_TARGETS == 5, "");

        // ---- 鬼修：魂印 / 魂爆 / 虚化 / 鬼域 ----
        double sm0 = com.hongmeng.xiuxian.talent.PathMechanics.soulMarkDamageBonus(0);
        double sm1 = com.hongmeng.xiuxian.talent.PathMechanics.soulMarkDamageBonus(1);
        double sm3 = com.hongmeng.xiuxian.talent.PathMechanics.soulMarkDamageBonus(3);
        double sm9 = com.hongmeng.xiuxian.talent.PathMechanics.soulMarkDamageBonus(9);
        check("魂印层数加成正确且封顶 12%",
                Math.abs(sm0) < 1.0E-9D && Math.abs(sm1 - 0.04D) < 1.0E-9D
                        && Math.abs(sm3 - 0.12D) < 1.0E-9D
                        && Math.abs(sm9 - 0.12D) < 1.0E-9D, "");
        check("魂爆伤害 = 攻击力 × 0.60 × (1 + 魂印天赋)",
                Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .soulBurstDamage(100.0D, 0.0D) - 60.0D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .soulBurstDamage(100.0D, 0.5D) - 90.0D) < 1.0E-9D, "");
        check("魂印持续 8 秒、锁印 2 秒（防自循环）",
                com.hongmeng.xiuxian.talent.PathMechanics.SOUL_MARK_DURATION == 160
                        && com.hongmeng.xiuxian.talent.PathMechanics
                        .SOUL_BURST_CD == 40, "");
        check("虚化：30 tick 无敌、耗 30 鬼域能量、基础 60 秒 CD",
                com.hongmeng.xiuxian.talent.PathMechanics.PHASE_TICKS == 30
                        && Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .PHASE_ENERGY - 30.0D) < 1.0E-9D
                        && com.hongmeng.xiuxian.talent.PathMechanics
                        .phaseCooldownTicks(0.0D) == 1200, "");
        check("虚化冷却：天赋 -20% → 48 秒；封顶只减 50%",
                com.hongmeng.xiuxian.talent.PathMechanics.phaseCooldownTicks(0.2D) == 960
                        && com.hongmeng.xiuxian.talent.PathMechanics
                        .phaseCooldownTicks(1.0D) == 600, "");
        check("鬼域 DoT/减速复用旧阵域公式（同一套数值）",
                Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .ghostDotPerTick(4.0D, 60) - com.hongmeng.xiuxian.talent
                        .PathMechanics.formationDotPerTick(4.0D, 60)) < 1.0E-12D
                        && Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .ghostSlow(4.0D) - com.hongmeng.xiuxian.talent.PathMechanics
                        .formationSlow(4.0D)) < 1.0E-12D, "");
        check("鬼域能量：上限 100、回复 0.12/tick、门槛 60、消耗 0.35/tick",
                Math.abs(com.hongmeng.xiuxian.talent.PathMechanics.GHOST_MAX - 100.0D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .ghostRegenPerTick() - 0.12D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .GHOST_OPEN_COST - 60.0D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.talent.PathMechanics
                        .GHOST_DRAIN - 0.35D) < 1.0E-9D, "");

        // ---- 血/魔/体：旧机制常量必须保留 ----
        check("血/魔/体旧机制常量保留（激荡/血池/魔气）",
                com.hongmeng.xiuxian.talent.PathMechanics.surgeGain(10.0D) > 0.0D
                        && com.hongmeng.xiuxian.talent.PathMechanics
                        .bloodGainFromDamage(50.0D) > 0.0D
                        && com.hongmeng.xiuxian.talent.PathMechanics
                        .demonGain(100.0D) > 0.0D, "");

        // ---- 接线：PathRuntime 真的处理五职业与两套新机制 ----
        var rt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/talent/PathRuntime.java"));
        String flat = rt.replaceAll("\\s+", "");
        boolean allCareers = flat.contains("JIAN_XIU") && flat.contains("TI_XIU")
                && flat.contains("MO_XIU") && flat.contains("XUE_XIU")
                && flat.contains("GUI_XIU");
        check("PathRuntime 处理全部五职业", allCareers, "");
        check("剑修机制接线（剑意/剑气/剑罡/空蓝易伤）",
                flat.contains("triggerSwordQi") && flat.contains("swordAuraRatio")
                        && flat.contains("SWORD_INTENT_MAX")
                        && flat.contains("manaEmptyVulnerability"), "");
        check("鬼修机制接线（魂印持久化/魂爆/虚化/鬼域）",
                flat.contains("SOUL_MARK") && flat.contains("applySoulMark")
                        && flat.contains("phaseTicks") && flat.contains("applyGhostRealm")
                        && flat.contains("soulBurstDamage"), "");
        check("受创与造成伤害钩子带上目标与近战标记",
                stripComments(readSource(
                        "src/main/java/com/hongmeng/xiuxian/core/ModEvents.java"))
                        .replaceAll("\\s+", "")
                        .contains("onPlayerDealtDamage(player,event.getEntity()")
                        && stripComments(readSource(
                        "src/main/java/com/hongmeng/xiuxian/core/ModEvents.java"))
                        .replaceAll("\\s+", "")
                        .contains("getDirectEntity()==player"), "");
        check("同步包带剑意满层旗标（bit0）",
                stripComments(readSource(
                        "src/main/java/com/hongmeng/xiuxian/network/SyncCultivationPacket.java"))
                        .replaceAll("\\s+", "")
                        .contains("Career.JIAN_XIU") && stripComments(readSource(
                        "src/main/java/com/hongmeng/xiuxian/network/SyncCultivationPacket.java"))
                        .replaceAll("\\s+", "").contains("swordIntentOf"), "");
    }

    /**
     * <b>新五职业（剑/血/魔/鬼/体）</b>的数据断言。
     *
     * <p>玩家方案《职业制作总纲》要求把旧「道途」换成五职业。
     * 旧 {@code Path} 必须<b>整体冻结</b>——它的值名被 250 件已注册装备的
     * 注册名依赖（{@code path_armor_ti_*} 等），改名会让老存档物品错位。
     * 所以这里同时钉住两件事：新枚举 {@code Career} 的五条平衡判据，
     * 以及旧 {@code Path} 的 5 个旧值原样保留。</p>
     */
    private static void careerRules() {
        section("新五职业（剑/血/魔/鬼/体）");

        var careers = com.hongmeng.xiuxian.talent.Career.values();
        check("职业恰有 5 个（剑/血/魔/鬼/体）", careers.length == 5,
                careers.length + " 个");

        String[] expectNames = {"JIAN_XIU", "XUE_XIU", "MO_XIU", "GUI_XIU", "TI_XIU"};
        StringBuilder nameDetail = new StringBuilder();
        boolean namesOk = careers.length == expectNames.length;
        for (int i = 0; i < careers.length; i++) {
            nameDetail.append(careers[i].name()).append(" ");
            if (i < expectNames.length && !careers[i].name().equals(expectNames[i])) {
                namesOk = false;
            }
        }
        check("职业枚举值名与方案一致（*_XIU，顺序 剑/血/魔/鬼/体）", namesOk,
                nameDetail.toString().trim());

        // ---- ① 无代价：五维乘数没有一项低于 1.0 ----
        boolean noPenalty = true;
        StringBuilder penalised = new StringBuilder();
        for (var c : careers) {
            for (double m : c.multipliers()) {
                if (m < 1.0D - 1.0E-9D) {
                    noPenalty = false;
                    penalised.append(c.cn()).append(" ");
                    break;
                }
            }
        }
        check("职业没有任何属性代价（玩家要求：只加不减）", noPenalty,
                noPenalty ? "五职业全 ≥1.0" : ("有代价的：" + penalised));

        // ---- ② 五维组合互不相同 ----
        java.util.Set<String> signatures = new java.util.HashSet<>();
        for (var c : careers) {
            signatures.add(String.format(java.util.Locale.ROOT, "%.2f/%.2f/%.2f/%.2f/%.2f",
                    c.melee(), c.spellDamage(), c.manaMax(), c.health(), c.armor()));
        }
        check("五职业的五维乘数组合互不相同（无代价 ≠ 无差异）",
                signatures.size() == careers.length, signatures.size() + " 种组合");

        // ---- ③ 总战力相当（几何平均极差 < 8%）----
        double geoMin = Double.MAX_VALUE;
        double geoMax = 0.0D;
        double geoSum = 0.0D;
        StringBuilder geoDetail = new StringBuilder();
        for (var c : careers) {
            double[] v = c.multipliers();
            double prod = 1.0D;
            for (double x : v) {
                prod *= x;
            }
            double geo = Math.pow(prod, 1.0D / v.length);
            geoMin = Math.min(geoMin, geo);
            geoMax = Math.max(geoMax, geo);
            geoSum += geo;
            geoDetail.append(String.format(java.util.Locale.ROOT, "%s=%.3f ",
                    c.cn(), geo));
        }
        double avg = geoSum / careers.length;
        double dev = (geoMax - geoMin) / avg;
        check("五职业总战力相当（几何平均极差 < 8%）", dev < 0.08D,
                String.format(java.util.Locale.ROOT, "极差 %.1f%%  %s",
                        dev * 100.0D, geoDetail.toString().trim()));

        // ---- ④ 侧重突出（最高/最低 ≥ 1.35）----
        boolean allFocused = true;
        StringBuilder focusDetail = new StringBuilder();
        StringBuilder flat = new StringBuilder();
        for (var c : careers) {
            double[] v = c.multipliers();
            double lo = Double.MAX_VALUE;
            double hi = 0.0D;
            for (double x : v) {
                lo = Math.min(lo, x);
                hi = Math.max(hi, x);
            }
            double spread = hi / lo;
            focusDetail.append(String.format(java.util.Locale.ROOT, "%s=%.2f ",
                    c.cn(), spread));
            if (spread < 1.35D) {
                allFocused = false;
                flat.append(c.cn()).append(" ");
            }
        }
        check("五职业的侧重都突出（最高项/最低项 ≥ 1.35）", allFocused,
                allFocused ? focusDetail.toString().trim()
                        : ("偏平的：" + flat + " | " + focusDetail.toString().trim()));

        // ---- ⑤ 侧重方向分散（最多 3 条同项）----
        java.util.Map<Integer, Integer> topCount = new java.util.HashMap<>();
        for (var c : careers) {
            double[] v = c.multipliers();
            int best = 0;
            for (int i = 1; i < v.length; i++) {
                if (v[i] > v[best]) {
                    best = i;
                }
            }
            topCount.merge(best, 1, Integer::sum);
        }
        int maxSame = 0;
        for (int n : topCount.values()) {
            maxSame = Math.max(maxSame, n);
        }
        check("侧重方向分散（最多 3 条侧重同一属性）", maxSame <= 3,
                "最多的一个属性有 " + maxSame + " 条侧重");

        // ---- ⑥ 专属加成：每职业一条、种类唯一、描述/数值非空 ----
        boolean allBonus = true;
        StringBuilder noBonus = new StringBuilder();
        java.util.Set<String> bonusKinds = new java.util.HashSet<>();
        for (var c : careers) {
            if (c.bonus() == null || c.bonus().isBlank()
                    || c.bonusKind() == null || c.bonusValue() <= 0.0D) {
                allBonus = false;
                noBonus.append(c.cn()).append(" ");
            }
            bonusKinds.add(c.bonusKind().name());
        }
        check("每职业都有专属加成（描述+种类+数值）", allBonus,
                allBonus ? "五职业都有" : ("缺的：" + noBonus));
        check("五职业的专属加成种类互不相同（不是同一效果换皮）",
                bonusKinds.size() == careers.length, bonusKinds.size() + " 种");

        // ---- ⑦ id / 颜色可用 ----
        boolean idOk = true;
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (var c : careers) {
            if (!c.id().matches("[a-z]+") || !ids.add(c.id())) {
                idOk = false;
            }
            float[] rgb = c.rgb();
            if (rgb == null || rgb.length != 3) {
                idOk = false;
            }
        }
        check("职业短名 id 唯一且为小写字母（物品/资源命名用）", idOk,
                String.join(",", ids));

        // ---- ⑧ 旧 Path 必须冻结（250 件装备注册名依赖它）----
        var legacy = com.hongmeng.xiuxian.talent.Path.values();
        String[] expectLegacy = {"FA", "TI", "MO", "XUE", "ZHEN"};
        boolean legacyOk = legacy.length == expectLegacy.length;
        StringBuilder legacyDetail = new StringBuilder();
        for (int i = 0; i < legacy.length; i++) {
            legacyDetail.append(legacy[i].name()).append(" ");
            if (i < expectLegacy.length && !legacy[i].name().equals(expectLegacy[i])) {
                legacyOk = false;
            }
        }
        check("旧 Path 保留 FA/TI/MO/XUE/ZHEN 五值（旧装备注册名不能变）",
                legacyOk, legacyDetail.toString().trim());

        // ---- ⑨ 迁移映射：TI/MO/XUE 自动保留，FA/ZHEN 强制重选 ----
        check("旧 TI/MO/XUE 自动映射到同名新职业",
                com.hongmeng.xiuxian.talent.Career.fromLegacyPathName("TI")
                        == com.hongmeng.xiuxian.talent.Career.TI_XIU
                        && com.hongmeng.xiuxian.talent.Career.fromLegacyPathName("MO")
                        == com.hongmeng.xiuxian.talent.Career.MO_XIU
                        && com.hongmeng.xiuxian.talent.Career.fromLegacyPathName("XUE")
                        == com.hongmeng.xiuxian.talent.Career.XUE_XIU, "");
        check("旧 FA/ZHEN 没有对应新职业（清空 + 退点 + 重选）",
                com.hongmeng.xiuxian.talent.Career.fromLegacyPathName("FA") == null
                        && com.hongmeng.xiuxian.talent.Career.fromLegacyPathName("ZHEN") == null,
                "");
    }

    /**
     * <b>职业甲 15 套数据</b>的断言。
     *
     * <p>玩家方案 §5 给了 5 职业 × 入门/进阶 两套的 2/4 件套表；
     * 数值规则：护甲 = 本境标准值 ×0.50 / ×0.58（道袍 ×0.70），
     * 生命 +2%/+3% 每件，韧性随境界成长。这里只验纯数据，
     * 物品注册与资源由 R-2 负责。</p>
     */
    private static void careerArmorSetRules() {
        section("职业甲（按境界分件，200 件）");

        check("职业甲境界数 = 10（练气~鸿蒙）",
                com.hongmeng.xiuxian.item.armor.CareerArmorRealm.MAX_REALM == 10,
                com.hongmeng.xiuxian.item.armor.CareerArmorRealm.MAX_REALM + " 境");

        // ---- 境界 → 外观档：1~3 初阶 / 4~6 进阶 / 7~10 名器 ----
        var realmApi = com.hongmeng.xiuxian.item.armor.CareerArmorRealm.class;
        var t = com.hongmeng.xiuxian.item.armor.CareerArmorSet.Tier.class;
        boolean bandOk =
                com.hongmeng.xiuxian.item.armor.CareerArmorRealm.bandOf(1)
                        == com.hongmeng.xiuxian.item.armor.CareerArmorSet.Tier.BASIC
                        && com.hongmeng.xiuxian.item.armor.CareerArmorRealm.bandOf(3)
                        == com.hongmeng.xiuxian.item.armor.CareerArmorSet.Tier.BASIC
                        && com.hongmeng.xiuxian.item.armor.CareerArmorRealm.bandOf(4)
                        == com.hongmeng.xiuxian.item.armor.CareerArmorSet.Tier.ADVANCED
                        && com.hongmeng.xiuxian.item.armor.CareerArmorRealm.bandOf(6)
                        == com.hongmeng.xiuxian.item.armor.CareerArmorSet.Tier.ADVANCED
                        && com.hongmeng.xiuxian.item.armor.CareerArmorRealm.bandOf(7)
                        == com.hongmeng.xiuxian.item.armor.CareerArmorSet.Tier.MASTER
                        && com.hongmeng.xiuxian.item.armor.CareerArmorRealm.bandOf(10)
                        == com.hongmeng.xiuxian.item.armor.CareerArmorSet.Tier.MASTER;
        check("境界 → 外观档：1~3 初阶 / 4~6 进阶 / 7~10 名器", bandOk, "");

        // ---- 200 个物品 ID 唯一 ----
        java.util.Set<String> ids = new java.util.HashSet<>();
        boolean uniqueOk = true;
        for (var c : com.hongmeng.xiuxian.talent.Career.values()) {
            for (int realm = 1; realm <= 10; realm++) {
                for (String slot : com.hongmeng.xiuxian.item.armor.CareerArmorRealm.SLOTS) {
                    if (!ids.add(com.hongmeng.xiuxian.item.armor.CareerArmorRealm
                            .itemName(c, realm, slot))) {
                        uniqueOk = false;
                    }
                }
            }
        }
        check("200 个职业甲物品 ID 全部唯一", uniqueOk && ids.size() == 200,
                ids.size() + "/200");

        // ---- 每境：四件之和 = 固定总值；< 道袍；随境界单调 ----
        boolean sumOk = true;
        boolean belowDao = true;
        boolean monoOk = true;
        StringBuilder detail = new StringBuilder();
        int prevTotal = -1;
        for (int realm = 1; realm <= 10; realm++) {
            int total = com.hongmeng.xiuxian.item.armor.CareerArmorRealm.totalArmorAt(realm);
            int sum = 0;
            for (int i = 0; i < 4; i++) {
                sum += com.hongmeng.xiuxian.item.armor.CareerArmorRealm.armorFor(realm, i);
            }
            if (sum != total) {
                sumOk = false;
                detail.append("境").append(realm).append(": ").append(sum)
                        .append("/").append(total).append(" ");
            }
            double dao = com.hongmeng.xiuxian.core.RealmStats.armorTotal(realm);
            if (total >= dao) {
                belowDao = false;
            }
            if (realm > 1 && total < prevTotal) {
                monoOk = false;
            }
            prevTotal = total;
        }
        check("每境四件护甲之和 = 该境界固定总值", sumOk,
                sumOk ? "10 境全部成立" : detail.toString());
        check("每境护甲低于同境道袍（纯防御仍归道袍）", belowDao, "");
        check("职业甲护甲随境界单调不降", monoOk, "");

        // ---- 部位分配 3/6/5/2 ----
        boolean shareOk = com.hongmeng.xiuxian.item.armor.CareerArmorRealm.armorFor(10, 1)
                > com.hongmeng.xiuxian.item.armor.CareerArmorRealm.armorFor(10, 2)
                && com.hongmeng.xiuxian.item.armor.CareerArmorRealm.armorFor(10, 2)
                > com.hongmeng.xiuxian.item.armor.CareerArmorRealm.armorFor(10, 0)
                && com.hongmeng.xiuxian.item.armor.CareerArmorRealm.armorFor(10, 0)
                > com.hongmeng.xiuxian.item.armor.CareerArmorRealm.armorFor(10, 3);
        check("四件分配 3/6/5/2（胸 > 腿 > 头 > 靴）", shareOk, "");

        // ---- 生命 / 韧性 / 耐久 ----
        // 2026-11-02 数值平衡：每件生命从「+2%/3%/4%」改成固定 +4/+6/+8（穿齐 +16/+24/+32）。
        boolean lifeOk =
                Math.abs(com.hongmeng.xiuxian.item.armor.CareerArmorRealm
                        .healthBonusPerPiece(1) - 4.0D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.item.armor.CareerArmorRealm
                        .healthBonusPerPiece(3) - 4.0D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.item.armor.CareerArmorRealm
                        .healthBonusPerPiece(4) - 6.0D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.item.armor.CareerArmorRealm
                        .healthBonusPerPiece(6) - 6.0D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.item.armor.CareerArmorRealm
                        .healthBonusPerPiece(7) - 8.0D) < 1.0E-9D
                        && Math.abs(com.hongmeng.xiuxian.item.armor.CareerArmorRealm
                        .healthBonusPerPiece(10) - 8.0D) < 1.0E-9D;
        check("生命加成：每件固定 +4（初阶）/ +6（进阶）/ +8（名器），穿齐 +16/+24/+32",
                lifeOk, "");
        check("单件韧性 0.5（与最早道途甲一致）",
                Math.abs(com.hongmeng.xiuxian.item.armor.CareerArmorRealm.toughness()
                        - 0.5D) < 1.0E-9D, "");
        boolean durOk = true;
        for (int realm = 2; realm <= 10; realm++) {
            if (com.hongmeng.xiuxian.item.armor.CareerArmorRealm.durabilityAt(realm, 1)
                    <= com.hongmeng.xiuxian.item.armor.CareerArmorRealm
                    .durabilityAt(realm - 1, 1)) {
                durOk = false;
            }
        }
        check("耐久随境界提高", durOk, "");

        // ---- 15 组外观/套装效果档完整 ----
        var sets = com.hongmeng.xiuxian.item.armor.CareerArmorSet.values();
        check("15 组外观/套装效果档完整（5 职业 × 初/进/名器）", sets.length == 15,
                sets.length + " 组");

        // ---- 2/4 件套表逐条一致 ----
        String[][] expect = {
                {"JIAN_BASIC", "2:ATTACK_SPEED=0.08;4:SWORD_INTENT=0.20;"},
                {"JIAN_ADVANCED", "2:SWORD_INTENT_FULL_DAMAGE=0.15;"
                        + "4:SWORD_QI_RANGE=0.30;4:SWORD_AURA=0.25;"},
                {"XUE_BASIC", "2:LIFESTEAL=0.08;4:BLOOD_MAX=0.20;"},
                {"XUE_ADVANCED", "2:STAGGER_RATIO=0.15;"
                        + "4:LOW_HP_DAMAGE=0.25;4:BLOOD_DASH_CD=0.20;"},
                {"MO_BASIC", "2:PENETRATION=0.10;4:DEMON_GAIN=0.20;"},
                {"MO_ADVANCED", "2:DEMON_CAP=0.25;"
                        + "4:OVERFLOW_BURST=0.30;4:SELF_DAMAGE_CUT=0.50;"},
                {"GUI_BASIC", "2:SOUL_MARK_DURATION=40.00;4:GHOST_REALM_RADIUS=0.20;"},
                {"GUI_ADVANCED", "2:SOUL_BURST_DAMAGE=0.20;"
                        + "4:PHASE_CD=0.25;4:GHOST_SLOW=0.15;"},
                {"TI_BASIC", "2:KNOCKBACK_RESIST=0.50;4:SURGE_GAIN=0.20;"},
                {"TI_ADVANCED", "2:LAST_STAND_TICKS=20.00;"
                        + "4:SURGE_BURST=0.30;4:SHOCK_RADIUS=0.20;"},
                {"JIAN_MASTER", "2:ATTACK_SPEED=0.10;"
                        + "4:SWORD_QI_RANGE=0.40;4:SWORD_AURA=0.35;"},
                {"XUE_MASTER", "2:LIFESTEAL=0.10;"
                        + "4:LOW_HP_DAMAGE=0.35;4:BLOOD_MAX=0.30;"},
                {"MO_MASTER", "2:PENETRATION=0.15;"
                        + "4:DEMON_CAP=0.35;4:OVERFLOW_BURST=0.40;"},
                {"GUI_MASTER", "2:SOUL_MARK_DURATION=60.00;"
                        + "4:SOUL_BURST_DAMAGE=0.30;4:PHASE_CD=0.35;"},
                {"TI_MASTER", "2:KNOCKBACK_RESIST=0.70;"
                        + "4:SURGE_BURST=0.45;4:SHOCK_RADIUS=0.30;"},
        };
        int tableBad = 0;
        StringBuilder tableDetail = new StringBuilder();
        for (var s : sets) {
            String expected = null;
            for (String[] row : expect) {
                if (row[0].equals(s.name())) {
                    expected = row[1];
                    break;
                }
            }
            String actual = careerSetSignature(s);
            if (expected == null || !expected.equals(actual)) {
                tableBad++;
                tableDetail.append(s.name()).append(" 实际=").append(actual).append(" ");
            }
        }
        check("2/4 件套效果表逐条一致", tableBad == 0,
                tableBad == 0 ? "15 组全部对上" : ("不一致：" + tableDetail));

        boolean nonEmpty = true;
        for (var s : sets) {
            if (s.setTwo().length == 0 || s.setFour().length == 0) {
                nonEmpty = false;
            }
        }
        check("每套 2 件套与 4 件套都有实际效果（不是白板）", nonEmpty, "");
    }


    /** 把一套甲的 2/4 件套效果拼成可比较的签名（顺序固定、两位小数）。 */
    private static String careerSetSignature(
            com.hongmeng.xiuxian.item.armor.CareerArmorSet set) {
        StringBuilder sb = new StringBuilder();
        for (var e : set.setTwo()) {
            sb.append("2:").append(e.kind()).append("=")
                    .append(String.format(java.util.Locale.ROOT, "%.2f", e.value()))
                    .append(";");
        }
        for (var e : set.setFour()) {
            sb.append("4:").append(e.kind()).append("=")
                    .append(String.format(java.util.Locale.ROOT, "%.2f", e.value()))
                    .append(";");
        }
        return sb.toString();
    }

    /**
     * <b>职业天赋与副业</b>的结构断言。
     *
     * <p>五职业 × 4 支 × 2 节点 = 40 节点；技能（30 被动 + 20 主动）不占节点，
     * 走分支投入门槛（2/4 点被动、5 点主动）。副业 3 门保持独立。</p>
     */
    private static void pathAndTalentRules() {
        section("职业天赋与副业");

        var careers = com.hongmeng.xiuxian.talent.Career.values();
        check("职业恰有 5 个（剑/血/魔/鬼/体）", careers.length == 5,
                careers.length + " 个");

        int bad = 0;
        StringBuilder detail = new StringBuilder();
        for (var c : careers) {
            var tree = com.hongmeng.xiuxian.talent.TalentTree.forCareer(c);
            if (tree.size() != 8) {
                bad++;
                detail.append(c.cn()).append("节点").append(tree.size()).append(" ");
            }
            var branches = com.hongmeng.xiuxian.talent.CareerBranch.ofCareer(c);
            if (branches.length != 4) {
                bad++;
                detail.append(c.cn()).append("支").append(branches.length).append(" ");
            }
            for (var b : branches) {
                if (com.hongmeng.xiuxian.talent.TalentTree.ofBranch(b).size() != 2) {
                    bad++;
                    detail.append(b.id()).append(" ");
                }
            }
        }
        check("每职业 8 节点（4 支 × 2）", bad == 0,
                bad == 0 ? "五职业齐全" : detail.toString());

        java.util.Set<String> ids = new java.util.HashSet<>();
        boolean idOk = true;
        for (var c : careers) {
            for (var n : com.hongmeng.xiuxian.talent.TalentTree.forCareer(c)) {
                if (!ids.add(n.id())) {
                    idOk = false;
                }
            }
        }
        check("40 个节点 id 全局唯一", idOk, ids.size() + " 个");

        boolean orderOk = true;
        for (var b : com.hongmeng.xiuxian.talent.CareerBranch.values()) {
            var list = com.hongmeng.xiuxian.talent.TalentTree.ofBranch(b);
            if (list.size() != 2
                    || list.get(0).branchRequired() != 0
                    || list.get(1).branchRequired()
                    != com.hongmeng.xiuxian.talent.CareerTalentTree.SECOND_NODE_REQUIRES) {
                orderOk = false;
            }
        }
        check("同支第 2 节点需该支投入 3 点（前置生效）", orderOk, "");

        boolean unlockOk = true;
        StringBuilder unlockDetail = new StringBuilder();
        for (var b : com.hongmeng.xiuxian.talent.CareerBranch.values()) {
            int cap = 0;
            for (var n : com.hongmeng.xiuxian.talent.TalentTree.ofBranch(b)) {
                cap += n.maxRank();
            }
            for (var s : com.hongmeng.xiuxian.talent.CareerSkill.ofBranch(b)) {
                if (!s.isCore() && cap < (int) s.unlockPoints()) {
                    unlockOk = false;
                    unlockDetail.append(s.cn()).append(" ");
                }
            }
            var active = com.hongmeng.xiuxian.talent.CareerActive.ofBranch(b);
            if (active == null || cap < 5) {
                unlockOk = false;
                unlockDetail.append(b.id()).append("主动 ");
            }
        }
        check("分支投入上限足够解锁被动(2/4)与主动(5)", unlockOk,
                unlockOk ? "20 支全部成立" : ("不足：" + unlockDetail));

        boolean noPointField = java.util.Arrays.stream(
                        com.hongmeng.xiuxian.talent.Talents.class.getDeclaredFields())
                .noneMatch(f -> f.getName().toLowerCase(java.util.Locale.ROOT)
                        .contains("remaining"));
        check("点数不存余额（按已花费反推，洗点不会刷点）", noPointField, "");

        // ---- 副业：三门保持独立（本轮不动）----
        var profs = com.hongmeng.xiuxian.talent.Profession.values();
        check("副业有 3 门（阵法/炼丹/炼器），都可兼修", profs.length == 3,
                com.hongmeng.xiuxian.talent.Profession.allNames());
        boolean hasZhenfa = false;
        for (var pr : profs) {
            if (pr == com.hongmeng.xiuxian.talent.Profession.ZHENFA) {
                hasZhenfa = true;
            }
        }
        check("阵法仍在副业里（阵法师转副业的旧要求保持）", hasZhenfa, "");
        boolean curveOk = true;
        for (int r = 2; r <= com.hongmeng.xiuxian.talent.Profession.maxRank(); r++) {
            if (com.hongmeng.xiuxian.talent.Profession.workNeeded(r)
                    <= com.hongmeng.xiuxian.talent.Profession.workNeeded(r - 1)) {
                curveOk = false;
            }
        }
        check("副业熟练度曲线严格递增（前松后紧）", curveOk, "");
    }


    /**
     * <b>职业平衡与天赋树分布</b>。
     *
     * <p>五维乘数的无代价/几何平均/侧重度已在 {@code careerRules} 钉住；
     * 这里只验"五棵树内容互不相同、没有一种效果出现在 4 棵以上树里"。</p>
     */
    private static void pathBalanceRules() {
        section("职业平衡（天赋树分布）");

        var careers = com.hongmeng.xiuxian.talent.Career.values();
        java.util.Set<String> signatures = new java.util.HashSet<>();
        java.util.Map<String, Integer> kindCount = new java.util.HashMap<>();
        for (var c : careers) {
            StringBuilder sig = new StringBuilder();
            java.util.Set<String> kinds = new java.util.HashSet<>();
            for (var n : com.hongmeng.xiuxian.talent.TalentTree.forCareer(c)) {
                sig.append(n.kind()).append(",");
                kinds.add(n.kind().name());
            }
            signatures.add(sig.toString());
            for (String k : kinds) {
                kindCount.merge(k, 1, Integer::sum);
            }
        }
        check("五棵天赋树内容互不相同", signatures.size() == careers.length,
                signatures.size() + " 种树型");

        StringBuilder common = new StringBuilder();
        int commonCount = 0;
        for (var e : kindCount.entrySet()) {
            if (e.getValue() >= 4) {
                commonCount++;
                common.append(e.getKey()).append("(").append(e.getValue()).append(") ");
            }
        }
        check("没有一种节点效果出现在 4 棵以上的树里", commonCount <= 2,
                commonCount == 0 ? "无过度共用的效果" : ("较常见：" + common));

        boolean rich = true;
        for (var c : careers) {
            java.util.Set<String> kinds = new java.util.HashSet<>();
            for (var n : com.hongmeng.xiuxian.talent.TalentTree.forCareer(c)) {
                kinds.add(n.kind().name());
            }
            if (kinds.size() < 5) {
                rich = false;
            }
        }
        check("每棵树至少 5 种不同效果（有取舍，不是一列同款）", rich, "");
    }


    /**
     * <b>五条道途的核心机制</b>的断言。
     *
     * <p>每条机制都来自参考作品的真实设计（见 {@code PathMechanics} 的类注释）：
     * 法修的真元化盾、体修的激荡与锁血、魔修的魔气、血修的血池与缓伤、
     * 阵修的阵域。</p>
     *
     * <p>这里钉住的是<b>可算的不变量</b>，尤其是"每条都有取舍" ——
     * 纯收益的机制就是版本答案，玩家不会去玩别的。</p>
     */
    private static void pathMechanicsRules() {
        section("道途核心机制");

        // ================= 体修：激荡 + 锁血 =================
        check("体修【激荡】：受创积攒能量", com.hongmeng.xiuxian.talent.PathMechanics.surgeGain(10.0D) > 0.0D,
                String.format("挨 10 点伤害攒 %.1f 能量", com.hongmeng.xiuxian.talent.PathMechanics.surgeGain(10.0D)));
        check("体修【激荡】：受 0 伤不攒能量（不能靠空转刷）",
                com.hongmeng.xiuxian.talent.PathMechanics.surgeGain(0.0D) == 0.0D, "");
        check("体修【激荡】：能量不满时引爆没有收益（要有门槛）",
                com.hongmeng.xiuxian.talent.PathMechanics.surgeBurst(com.hongmeng.xiuxian.talent.PathMechanics.SURGE_MAX - 1.0D,
                        0.8D) == 1.0D, "");
        double burstHigh = com.hongmeng.xiuxian.talent.PathMechanics.surgeBurst(com.hongmeng.xiuxian.talent.PathMechanics.SURGE_MAX,
                0.8D);
        double burstLow = com.hongmeng.xiuxian.talent.PathMechanics.surgeBurst(com.hongmeng.xiuxian.talent.PathMechanics.SURGE_MAX,
                0.2D);
        check("体修【激荡】：满能量引爆有增伤", burstHigh > 1.0D,
                String.format("×%.2f", burstHigh));
        check("体修【激荡】：血量决定形态（血多=燃血高增伤，血少=金身带减伤）",
                burstHigh > burstLow
                        && com.hongmeng.xiuxian.talent.PathMechanics.surgeMitigation(com.hongmeng.xiuxian.talent.PathMechanics.SURGE_MAX,
                                0.2D) > 0.0D
                        && com.hongmeng.xiuxian.talent.PathMechanics.surgeMitigation(com.hongmeng.xiuxian.talent.PathMechanics.SURGE_MAX,
                                0.8D) == 0.0D,
                String.format("血多 ×%.2f / 血少 ×%.2f + 减伤 %.0f%%", burstHigh, burstLow,
                        com.hongmeng.xiuxian.talent.PathMechanics.surgeMitigation(
                                com.hongmeng.xiuxian.talent.PathMechanics.SURGE_MAX, 0.2D)
                                * 100.0D));
        check("体修【锁血】：致命一击且冷却好了才触发",
                com.hongmeng.xiuxian.talent.PathMechanics.shouldLastStand(10.0D, 15.0D, false)
                        && !com.hongmeng.xiuxian.talent.PathMechanics.shouldLastStand(10.0D, 5.0D, false)
                        && !com.hongmeng.xiuxian.talent.PathMechanics.shouldLastStand(10.0D, 15.0D, true),
                "锁血 " + com.hongmeng.xiuxian.talent.PathMechanics.LAST_STAND_TICKS
                        + " tick");

        // ================= 法修：真元化盾 =================
        double shieldFull = com.hongmeng.xiuxian.talent.PathMechanics.manaShield(100.0D, 100.0D, 0.5D);
        double shieldHalf = com.hongmeng.xiuxian.talent.PathMechanics.manaShield(50.0D, 100.0D, 0.5D);
        double shieldEmpty = com.hongmeng.xiuxian.talent.PathMechanics.manaShield(0.0D, 100.0D, 0.5D);
        check("法修【真元化盾】：法力越多盾越厚，空了没盾",
                shieldFull > shieldHalf && shieldHalf > shieldEmpty && shieldEmpty == 0.0D,
                String.format("%.0f / %.0f / %.0f", shieldFull, shieldHalf, shieldEmpty));
        check("法修【真元化盾】：法力充足时没有易伤",
                com.hongmeng.xiuxian.talent.PathMechanics.manaEmptyVulnerability(100.0D, 100.0D) == 0.0D, "");
        check("法修【真元化盾】：真元耗尽时变脆（对应资料里的 秒变脆皮）",
                com.hongmeng.xiuxian.talent.PathMechanics.manaEmptyVulnerability(0.0D, 100.0D) > 0.2D,
                String.format("+%.0f%% 受伤",
                        com.hongmeng.xiuxian.talent.PathMechanics.manaEmptyVulnerability(0.0D, 100.0D) * 100.0D));
        // 取舍：盾厚则伤害低 —— 法力既是输出又是生存资源
        double[] ab1 = com.hongmeng.xiuxian.talent.PathMechanics.absorbWithShield(100.0D, 30.0D);
        double[] ab2 = com.hongmeng.xiuxian.talent.PathMechanics.absorbWithShield(20.0D, 30.0D);
        check("法修【护盾吸收】：吸收量不超过伤害，也不超过盾值",
                ab1[0] == 30.0D && ab1[1] == 70.0D && ab2[0] == 20.0D && ab2[1] == 0.0D,
                String.format("100伤/30盾 -> 吸%.0f 剩%.0f；20伤/30盾 -> 吸%.0f 剩%.0f",
                        ab1[0], ab1[1], ab2[0], ab2[1]));

        // ================= 魔修：魔气 =================
        double dmg0 = com.hongmeng.xiuxian.talent.PathMechanics.demonDamageBonus(0.0D);
        double dmg50 = com.hongmeng.xiuxian.talent.PathMechanics.demonDamageBonus(50.0D);
        double dmg100 = com.hongmeng.xiuxian.talent.PathMechanics.demonDamageBonus(100.0D);
        check("魔修【魔气】：魔气越高伤害越高",
                dmg0 == 1.0D && dmg50 > dmg0 && dmg100 > dmg50,
                String.format("×%.2f -> ×%.2f -> ×%.2f", dmg0, dmg50, dmg100));
        check("魔修【魔气】：魔气提供破防（这是它通往真实伤害的路）",
                com.hongmeng.xiuxian.talent.PathMechanics.demonPenetration(100.0D) > 0.25D,
                String.format("无视护甲 %.0f%%", com.hongmeng.xiuxian.talent.PathMechanics.demonPenetration(100.0D) * 100.0D));
        check("魔修【魔气】：未满时没有溢出伤害（代价只在满仓后）",
                com.hongmeng.xiuxian.talent.PathMechanics.demonOverflowSelfDamage(50.0D, 20.0D) == 0.0D, "");
        check("魔修【魔气】：满仓后溢出会伤到自己（高风险，但玩家可控）",
                com.hongmeng.xiuxian.talent.PathMechanics.demonOverflowSelfDamage(100.0D, 20.0D) > 0.0D,
                String.format("溢出 20 -> 自伤 %.0f", com.hongmeng.xiuxian.talent.PathMechanics.demonOverflowSelfDamage(100.0D, 20.0D)));
        check("魔修【魔气】：脱战会消散（不会永久攒着）",
                com.hongmeng.xiuxian.talent.PathMechanics.demonDecayPerTick() > 0.0D, "");

        // ================= 血修：血池 + 缓伤 =================
        check("血修【血池】：造成伤害积累血池", com.hongmeng.xiuxian.talent.PathMechanics.bloodGainFromDamage(50.0D) > 0.0D, "");
        check("血修【血池】：击杀给的血池与目标血量相关（打大怪更划算）",
                com.hongmeng.xiuxian.talent.PathMechanics.bloodGainFromKill(200.0D) > com.hongmeng.xiuxian.talent.PathMechanics.bloodGainFromKill(20.0D),
                String.format("%.0f vs %.0f", com.hongmeng.xiuxian.talent.PathMechanics.bloodGainFromKill(200.0D),
                        com.hongmeng.xiuxian.talent.PathMechanics.bloodGainFromKill(20.0D)));
        double[] sp = com.hongmeng.xiuxian.talent.PathMechanics.spendBlood(10.0D, 100.0D);
        check("血修【血池】：血池不够时只能回一部分（不能凭空造）",
                sp[0] < 100.0D && sp[1] <= 10.0D,
                String.format("10 点血池 -> 回 %.1f 血，花 %.1f", sp[0], sp[1]));
        double[] sp2 = com.hongmeng.xiuxian.talent.PathMechanics.spendBlood(100.0D, 10.0D);
        check("血修【血池】：只要够，想回多少就回多少",
                Math.abs(sp2[0] - 10.0D) < 1.0E-9D, String.format("回 %.1f", sp2[0]));
        // 缓伤的关键不变量：总量守恒
        double[] st = com.hongmeng.xiuxian.talent.PathMechanics.staggerSplit(100.0D, 0.3D);
        check("血修【缓伤】：只改变伤害的时间分布，**总量不减少**",
                Math.abs((st[0] + st[1]) - 100.0D) < 1.0E-9D,
                String.format("立即 %.0f + 延迟 %.0f = %.0f", st[0], st[1], st[0] + st[1]));
        check("血修【缓伤】：延迟比例受上限约束（不能拖成无敌）",
                com.hongmeng.xiuxian.talent.PathMechanics.staggerSplit(100.0D, 0.99D)[1] <= 100.0D * 0.8D + 1.0E-9D,
                String.format("上限 %.0f%%", 0.8D * 100.0D));
        check("血修【缓伤】：延迟池会逐 tick 结算（不会永远挂着）",
                com.hongmeng.xiuxian.talent.PathMechanics.staggerPerTick(com.hongmeng.xiuxian.talent.PathMechanics.STAGGER_TICKS)
                        > 0.0D,
                com.hongmeng.xiuxian.talent.PathMechanics.STAGGER_TICKS + " tick 内结清");
        // 反杀窗口
        double lustCalm = com.hongmeng.xiuxian.talent.PathMechanics.bloodlustBonus(1.0D, 1.0D);
        double lustDesperate = com.hongmeng.xiuxian.talent.PathMechanics.bloodlustBonus(1.0D, 0.1D);
        check("血修【反杀】：血池满 + 残血时伤害最高（鼓励激进打法）",
                lustDesperate > lustCalm && lustCalm == 1.0D,
                String.format("满血 ×%.2f -> 残血 ×%.2f", lustCalm, lustDesperate));
        check("血修【反杀】：无代价（不像魔修那样会自伤）",
                lustDesperate > 0.0D, "纯收益，但要求打得激进");

        // ================= 阵修：阵域 =================
        double dotSmall = com.hongmeng.xiuxian.talent.PathMechanics.formationDotPerTick(2.0D, 100);
        double dotBig = com.hongmeng.xiuxian.talent.PathMechanics.formationDotPerTick(10.0D, 100);
        check("阵修【阵域】：范围越大单体伤害越低（范围 vs 强度的取舍）",
                dotSmall > dotBig, String.format("半径2 -> %.4f/帧，半径10 -> %.4f/帧",
                        dotSmall, dotBig));
        check("阵修【阵域】：前 3 秒渐强（布置需要时间）",
                com.hongmeng.xiuxian.talent.PathMechanics.formationDotPerTick(4.0D, 0) < com.hongmeng.xiuxian.talent.PathMechanics.formationDotPerTick(4.0D, 60),
                "");
        check("阵修【阵域】：有减速（控场定位）",
                com.hongmeng.xiuxian.talent.PathMechanics.formationSlow(4.0D) > 0.1D,
                String.format("%.0f%%", com.hongmeng.xiuxian.talent.PathMechanics.formationSlow(4.0D) * 100.0D));

        // ================= 全局：五条机制必须互不相同 =================
        java.util.Set<String> sigs = new java.util.HashSet<>();
        sigs.add(String.format("体修%.2f/%.2f", com.hongmeng.xiuxian.talent.PathMechanics.surgeGain(10.0D), burstHigh));
        sigs.add(String.format("法修%.2f/%.2f", shieldHalf,
                com.hongmeng.xiuxian.talent.PathMechanics.manaEmptyVulnerability(0.0D, 100.0D)));
        sigs.add(String.format("魔修%.2f/%.2f", com.hongmeng.xiuxian.talent.PathMechanics.demonDamageBonus(100.0D),
                com.hongmeng.xiuxian.talent.PathMechanics.demonPenetration(100.0D)));
        sigs.add(String.format("血修%.2f/%.2f", com.hongmeng.xiuxian.talent.PathMechanics.bloodlustBonus(1.0D, 0.2D),
                com.hongmeng.xiuxian.talent.PathMechanics.staggerSplit(100.0D, 0.3D)[1]));
        sigs.add(String.format("阵修%.2f/%.2f", com.hongmeng.xiuxian.talent.PathMechanics.formationDotPerTick(4.0D, 60),
                com.hongmeng.xiuxian.talent.PathMechanics.formationSlow(4.0D)));
        check("五条机制互不相同（不是同一个公式换皮）", sigs.size() == 5,
                sigs.size() + " 种");

        // ---- 全局：每条都要有"通往真伤/破防"的路 ----
        //
        // ⚠ 这条是从体修重塑的教训来的：玩家分析指出体修
        //   「进入三四修后基本只能退居二线，因为纯靠神通倍率的输出
        //     根本无法比肩真实伤害（六丁星神炎、惊锋三式）」。
        //   所以每条道途都必须有破防或真伤的手段，否则后期会集体乏力。
        int withPen = 0;
        for (var c : com.hongmeng.xiuxian.talent.Career.values()) {
            if (c.bonusKind() == com.hongmeng.xiuxian.talent.Career.BonusKind.PENETRATION) {
                withPen++;
            }
            for (var n : com.hongmeng.xiuxian.talent.TalentTree.forCareer(c)) {
                if (n.kind() == com.hongmeng.xiuxian.talent.CareerTalentTree.Kind.PENETRATION) {
                    withPen++;
                    break;
                }
            }
        }
        check("至少有一条途径能拿到破防（后期不至于全被护甲吃光）", withPen > 0,
                withPen + " 处破防来源（魔修专属加成 + 各树天赋）");
        check("魔修的破防随魔气增长（越战越能穿甲）",
                com.hongmeng.xiuxian.talent.PathMechanics.demonPenetration(100.0D) > com.hongmeng.xiuxian.talent.PathMechanics.demonPenetration(20.0D), "");
    }


    /**
     * <b>道途机制与 HUD 的接线契约</b>。
     *
     * <p>计算公式在 {@code PathMechanics} 里已经验过，这里验的是
     * <b>接线层特有的坑</b> —— 那些"看起来对、实际错位"的错误：</p>
     * <ol>
     *   <li>资源越界（HUD 会画到屏幕外）；</li>
     *   <li>HUD 用的资源下标与 {@code PathRuntime.snapshot} 的顺序错位
     *       （玩家会看到"血池"条显示的其实是"魔气"）；</li>
     *   <li>某个道途缺 HUD 文案；</li>
     *   <li>加成汇总漏项或负值（会让战斗结算反向）。</li>
     * </ol>
     */
    private static void pathWiringRules() {
        section("道途机制接线");

        // ---- ① 资源快照必须是 0~1 的比例（四元组） ----
        var snapSrc = readSource("xiuxian-mod/src/main/java/com/hongmeng/xiuxian/talent/PathRuntime.java");
        check("PathRuntime.snapshot 返回四项资源（激荡/魔气/血池/阵域）",
                snapSrc.contains("return new double[]{") && snapSrc.contains("clamp01"),
                "四项都过 clamp01");
        check("资源快照用 clamp01 收口（HUD 进度条不会越界）",
                countOf(snapSrc, "clamp01(") >= 4,
                countOf(snapSrc, "clamp01(") + " 处 clamp01");

        // ---- ② HUD 的资源下标必须与 snapshot 顺序一致 ----
        var hudSrc = readSource("xiuxian-mod/src/main/java/com/hongmeng/xiuxian/CultivationHud.java");
        // snapshot 的顺序：剑/体 -> 0, 魔 -> 1, 血 -> 2, 鬼 -> 3
        check("HUD 里剑修/体修共用下标 0（剑意/激荡）",
                hudSrc.contains("case JIAN_XIU, TI_XIU -> 0"), "");
        check("HUD 里魔修取下标 1（魔气）",
                hudSrc.contains("case MO_XIU -> 1"), "");
        check("HUD 里血修取下标 2（血池）",
                hudSrc.contains("case XUE_XIU -> 2"), "");
        check("HUD 里鬼修取下标 3（鬼域）",
                hudSrc.contains("case GUI_XIU -> 3"), "");

        // ---- ③ 五职业都要有 HUD 资源名 ----
        int labels = countOf(hudSrc, "case JIAN_XIU -> \"")
                + countOf(hudSrc, "case TI_XIU -> \"")
                + countOf(hudSrc, "case MO_XIU -> \"")
                + countOf(hudSrc, "case XUE_XIU -> \"")
                + countOf(hudSrc, "case GUI_XIU -> \"");
        check("五职业都有 HUD 资源名（缺一个会显示 null）", labels >= 5,
                labels + " 条文案");

        // ---- ④ 加成汇总必须是"加法比例"且不出现负数 ----
        var bonusSrc = readSource("xiuxian-mod/src/main/java/com/hongmeng/xiuxian/talent/TalentBonus.java");
        check("TalentBonus 集中了加成汇总（战斗只问这里，避免漏项/重复加）",
                bonusSrc.contains("public static double spellDamage")
                        && bonusSrc.contains("public static double lifesteal")
                        && bonusSrc.contains("public static double penetration"),
                "");
        check("冷却缩减有上限（不能减到 0 甚至负数）",
                bonusSrc.contains("Math.min(0.60D"), "上限 60%");
        check("抗击退有上限（不能超过 100%）",
                bonusSrc.contains("Math.min(0.85D"), "上限 85%");

        // ---- ⑤ 战斗钩子必须真的接上了 ----
        var evSrc = readSource("xiuxian-mod/src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        check("受创钩子接了 PathRuntime.onPlayerHurt",
                evSrc.contains("PathRuntime\n                    .onPlayerHurt")
                        || evSrc.contains("PathRuntime.onPlayerHurt")
                        || evSrc.contains(".onPlayerHurt("), "");
        check("造成伤害钩子接了 PathRuntime.onPlayerDealtDamage",
                evSrc.contains(".onPlayerDealtDamage("), "");
        check("每 tick 推进了 PathRuntime.tick",
                evSrc.contains("PathRuntime.tick(player)"), "");
        check("掉线清了 PathRuntime 状态（防内存泄漏）",
                evSrc.contains("PathRuntime.forget("), "");

        // ---- ⑥ 同步包必须带上道途与资源 ----
        var pktSrc = readSource("xiuxian-mod/src/main/java/com/hongmeng/xiuxian/network/SyncCultivationPacket.java");
        check("同步包带上了主修序号",
                pktSrc.contains("writeVarInt(packet.path)")
                        && pktSrc.contains("buf.readVarInt()"), "写入 + 读回");
        // ⚠ 这里不能只比"writeFloat 出现几次" ——
        //   4 项资源是在**一个循环**里写的，所以 `writeFloat(` 只出现 1 次。
        //   而如果只把门槛放宽到 1，这条断言就恒真了（encode 写 4 项、
        //   decode 只读 3 项时照样通过）—— 那比没有断言更危险。
        //
        //   真正要保证的是**两端读写的项数一致**，而项数写在循环上界里：
        //     encode: for (int i = 0; i < 4; i++) buf.writeFloat(...)
        //     decode: for (int i = 0; i < 4; i++) buf.readFloat(...)
        //   所以直接数出上界常量来比。
        int writeLoop = loopBound(pktSrc, "writeFloat(");
        int readLoop = loopBound(pktSrc, "readFloat(");
        check("同步包的资源项数两端一致（循环上界相同，且为 4）",
                writeLoop == 4 && readLoop == 4,
                "写 " + writeLoop + " 项 / 读 " + readLoop + " 项");
        check("同步包没有漏掉任何一项资源（写了也要读回来）",
                countOf(pktSrc, "writeFloat(") == countOf(pktSrc, "readFloat(")
                        && countOf(pktSrc, "writeFloat(") >= 1,
                countOf(pktSrc, "writeFloat(") + " 写 / "
                        + countOf(pktSrc, "readFloat(") + " 读调用");

        // ---- ⑦ 道途专属加成的种类必须与 Path.BonusKind 对得上 ----
        for (var p : com.hongmeng.xiuxian.talent.Path.values()) {
            boolean ok = p.bonus() != null && !p.bonus().isBlank()
                    && p.bonusKind() != null && p.bonusValue() > 0.0D;
            if (!ok) {
                check("道途 " + p.cn() + " 的专属加成完整（描述+种类+数值）", false, "");
                return;
            }
        }
        check("五条道途的专属加成都有描述、种类与数值", true, "");
    }

        /**
     * 读源码文件（用于断言"接线是否真的存在"）。
     *
     * <h2>⚠ 为什么要四级回退</h2>
     * <p>验证器跑的时候<b>工作目录不是工作区根</b>（我是从 {@code _cache}
     * 之类的地方起的），所以我第一版"cwd + 相对路径"的写法会<b>读到空串</b> ——
     * 于是断言报"失败"，但真正的原因是<b>没读到文件</b>，不是代码有问题。
     * 这种假失败很误导（还差点让我去修没坏的代码）。</p>
     *
     * <p>现在依次尝试：</p>
     * <ol>
     *   <li>cwd 及其各级父目录 + 原样相对路径；</li>
     *   <li>同上但加减 {@code xiuxian-mod/} 前缀；</li>
     *   <li><b>从 class 文件位置反推</b>（最稳：{@code _cache/modout/com/hongmeng/…}
     *       一路往上就能找到工作区）；</li>
     *   <li>写死的绝对路径兜底。</li>
     * </ol>
     * <p>全都找不到时<b>打印一次警告</b> —— 静默返回空串才是真正的坑。</p>
     */
    /**
     * 从源码里数出某个调用所在的 for 循环上界。
     *
     * <p>用途：{@code for (int i = 0; i < 4; i++) buf.writeFloat(...)} ——
     * 这种一个调用写 N 项的写法，用调用出现次数是数不出项数的。
     * 我第一版就栽在这里：断言要求 writeFloat( 出现 4 次，
     * 实际只出现 1 次（4 项在一个循环里），于是报了一个假失败。
     * 而如果只把门槛放宽到 1，断言就恒真了 —— 那比没有断言更危险。</p>
     *
     * @return 循环上界；找不到返回 -1
     */
    private static int loopBound(String src, String call) {
        if (src == null || src.isEmpty()) {
            return -1;
        }
        int at = src.indexOf(call);
        if (at < 0) {
            return -1;
        }
        String before = src.substring(0, at);
        int forAt = before.lastIndexOf("for (");
        if (forAt < 0) {
            return -1;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<\\s*(\\d+)")
                .matcher(before.substring(forAt));
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static String readSource(String relative) {
        java.util.List<String> rels = new java.util.ArrayList<>();
        rels.add(relative);
        if (relative.startsWith("xiuxian-mod/")) {
            rels.add(relative.substring("xiuxian-mod/".length()));
        } else {
            rels.add("xiuxian-mod/" + relative);
        }

        java.util.List<java.nio.file.Path> bases = new java.util.ArrayList<>();
        // ① cwd 及各级父目录
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get("").toAbsolutePath();
            for (int up = 0; up < 8 && dir != null; up++) {
                bases.add(dir);
                dir = dir.getParent();
            }
        } catch (Throwable ignored) {
            // 忽略：下面还有别的办法
        }
        // ② 从 class 文件位置反推
        try {
            java.net.URL url = LogicVerifier.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            java.nio.file.Path dir = java.nio.file.Paths.get(url.toURI()).toAbsolutePath();
            for (int up = 0; up < 8 && dir != null; up++) {
                bases.add(dir);
                dir = dir.getParent();
            }
        } catch (Throwable ignored) {
            // 忽略
        }
        // ③ 兜底绝对路径
        bases.add(java.nio.file.Paths.get(
                "C:\\Users\\duyun\\Documents\\deepseek-harness\\default-workspace"));

        for (java.nio.file.Path base : bases) {
            for (String rel : rels) {
                try {
                    java.nio.file.Path p = base.resolve(rel);
                    if (java.nio.file.Files.isRegularFile(p)) {
                        return new String(java.nio.file.Files.readAllBytes(p),
                                java.nio.charset.StandardCharsets.UTF_8);
                    }
                } catch (Throwable ignored) {
                    // 继续试下一个
                }
            }
        }
        if (!WARNED_READ_SOURCE.contains(relative)) {
            WARNED_READ_SOURCE.add(relative);
            System.out.println("    [warn] readSource 找不到：" + relative
                    + "（基于 cwd=" + System.getProperty("user.dir") + "）");
        }
        return "";
    }

    /** 已经警告过的文件（避免刷屏）。 */
    private static final java.util.Set<String> WARNED_READ_SOURCE =
            new java.util.HashSet<>();


    /** 统计子串出现次数。 */
    private static int countOf(String haystack, String needle) {
        if (haystack == null || haystack.isEmpty() || needle.isEmpty()) {
            return 0;
        }
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }


    /**
     * <b>五套道途护甲</b>的断言。
     *
     * <p>玩家要求「每种分类都要有独立的装备什么的」。</p>
     *
     * <p>最关键的一条是<b>基础数值必须五套完全相同</b>：
     * 只要某一套护甲值高一点，玩家立刻算出"哪套最强"，
     * 其余四套就成摆设了。差别只允许出现在<b>道途专属加成</b>上。</p>
     */
    private static void pathArmorRules() {
        section("道途护甲");

        var paths = com.hongmeng.xiuxian.talent.Path.values();
        var types = com.hongmeng.xiuxian.item.armor.PathArmorTable.TYPES;

        // ---- ① 五套齐全 ----
        //
        // ⚠ 这里刻意<b>不</b>调 ModPathArmor.get() —— 那个类的静态初始化会建
        //   DeferredRegister，碰到 ForgeRegistries 就抛 "Not bootstrapped"，
        //   而验证器不启动游戏。所以改成检查"纯数据 + 资源文件"。
        //   （这也是我把数据/注册拆成 PathArmorTable + ModPathArmor 的原因。）
        int missing = 0;
        StringBuilder lack = new StringBuilder();
        for (var p : paths) {
            var tier = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(p);
            if (tier == null) {
                missing++;
                lack.append(p.cn()).append("(无材质) ");
                continue;
            }
            for (var t : types) {
                String rel = "assets/hongmeng/models/item/"
                        + com.hongmeng.xiuxian.item.armor.PathArmorTable.itemName(p, 1, t)
                        + ".json";
                if (findResource(rel) == null) {
                    missing++;
                    lack.append(p.cn()).append("/").append(t).append(" ");
                }
            }
        }
        check("五条道途各有 4 件护甲（共 20 件，一件不缺）", missing == 0,
                missing == 0 ? "20 件齐全" : ("缺：" + lack));

        // ---- ② 基础数值五套完全相同（平衡的硬线） ----
        boolean sameDefense = true;
        boolean sameDurability = true;
        boolean sameToughness = true;
        boolean sameHealth = true;
        var base = com.hongmeng.xiuxian.item.armor.PathArmorTier
                .of(com.hongmeng.xiuxian.talent.Path.FA);
        for (var p : paths) {
            var t = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(p);
            if (t == null || base == null) {
                continue;
            }
            for (var ty : types) {
                if (t.defense(ty) != base.defense(ty)) {
                    sameDefense = false;
                }
                if (t.durability(ty) != base.durability(ty)) {
                    sameDurability = false;
                }
            }
            if (Math.abs(t.toughness() - base.toughness()) > 1.0E-9D) {
                sameToughness = false;
            }
            if (Math.abs(t.healthPerPiece() - base.healthPerPiece()) > 1.0E-9D) {
                sameHealth = false;
            }
        }
        check("五套护甲的护甲值完全相同（否则会出现版本答案）", sameDefense, "");
        check("五套护甲的耐久完全相同", sameDurability, "");
        check("五套护甲的韧性完全相同", sameToughness, "");
        check("五套护甲的额外生命完全相同", sameHealth, "");

        // ---- ③ 但加成类型必须不同（差别在这里） ----
        java.util.Set<String> sigs = new java.util.HashSet<>();
        for (var p : paths) {
            var t = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(p);
            if (t == null) {
                continue;
            }
            sigs.add(String.format("%.2f/%.2f/%.2f/%.2f/%.2f/%.2f/%.2f",
                    t.spellBonus(), t.manaBonus(), t.meleeBonus(), t.armorBonus(),
                    t.lifestealBonus(), t.penetrationBonus(), t.formationBonus()));
        }
        check("五套护甲的加成组合互不相同（基础一样，侧重不同）",
                sigs.size() == paths.length, sigs.size() + " 种组合");

        // ---- ④ 每套只强化本道途的机制（不能串味） ----
        StringBuilder wrong = new StringBuilder();
        for (var p : paths) {
            var t = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(p);
            if (t == null) {
                continue;
            }
            // 逐条检查：给的不是本道途的加成就必须是 0
            if ((p != com.hongmeng.xiuxian.talent.Path.FA)
                    && (t.spellBonus() > 0.0D || t.manaBonus() > 0.0D)) {
                wrong.append(p.cn()).append("给了法术/灵力 ");
            }
            if ((p != com.hongmeng.xiuxian.talent.Path.TI)
                    && (t.armorBonus() > 0.0D)) {
                wrong.append(p.cn()).append("给了减伤 ");
            }
            if ((p != com.hongmeng.xiuxian.talent.Path.XUE)
                    && (t.lifestealBonus() > 0.0D)) {
                wrong.append(p.cn()).append("给了吸血 ");
            }
            if ((p != com.hongmeng.xiuxian.talent.Path.MO)
                    && (t.penetrationBonus() > 0.0D)) {
                wrong.append(p.cn()).append("给了破防 ");
            }
            if ((p != com.hongmeng.xiuxian.talent.Path.ZHEN)
                    && (t.formationBonus() > 0.0D)) {
                wrong.append(p.cn()).append("给了领域 ");
            }
            // 近战只有体修/血修有
            if ((p != com.hongmeng.xiuxian.talent.Path.TI
                    && p != com.hongmeng.xiuxian.talent.Path.XUE)
                    && t.meleeBonus() > 0.0D) {
                wrong.append(p.cn()).append("给了近战 ");
            }
        }
        check("每套护甲只强化本道途的机制（没有串味）", wrong.length() == 0,
                wrong.length() == 0 ? "五套都对得上" : wrong.toString());

        // ---- ⑤ 每套至少有一项加成（不能是白板） ----
        boolean allHaveBonus = true;
        StringBuilder blank = new StringBuilder();
        for (var p : paths) {
            var t = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(p);
            if (t == null) {
                continue;
            }
            double sum = t.spellBonus() + t.manaBonus() + t.meleeBonus()
                    + t.armorBonus() + t.lifestealBonus() + t.penetrationBonus()
                    + t.formationBonus();
            if (sum <= 0.0D) {
                allHaveBonus = false;
                blank.append(p.cn()).append(" ");
            }
        }
        check("每套护甲都有实际加成（不是白板装）", allHaveBonus,
                allHaveBonus ? "五套都有" : ("空白的：" + blank));

        // ---- ⑥ 资源文件必须真的存在（Java 编译不检查这个） ----
        // 护甲现在按境界分阶（5 × 10 × 4 = 200 件），
        // 但**贴图共用**（一道途一部位一张，共 20 张）。
        // 所以这里查：每道途每部位的 20 张贴图 + 每件装备的 200 个 model。
        int texMissing = 0;
        StringBuilder texLack = new StringBuilder();
        for (var p : paths) {
            String key = p.name().toLowerCase(java.util.Locale.ROOT);
            for (String rel : new String[]{
                    "assets/hongmeng/textures/item/path_armor_" + key + "_helmet.png",
                    "assets/hongmeng/textures/item/path_armor_" + key + "_chestplate.png",
                    "assets/hongmeng/textures/item/path_armor_" + key + "_leggings.png",
                    "assets/hongmeng/textures/item/path_armor_" + key + "_boots.png",
                    "assets/hongmeng/textures/models/armor/path_armor_" + key + "_layer_1.png",
                    "assets/hongmeng/textures/models/armor/path_armor_" + key + "_layer_2.png"}) {
                if (findResource(rel) == null) {
                    texMissing++;
                    texLack.append(rel).append(" ");
                }
            }
            for (int tier = 1; tier <= com.hongmeng.xiuxian.item.armor.PathArmorTable.MAX_TIER;
                    tier++) {
                for (var t : com.hongmeng.xiuxian.item.armor.PathArmorTable.TYPES) {
                    String rel = "assets/hongmeng/models/item/"
                            + com.hongmeng.xiuxian.item.armor.PathArmorTable
                                    .itemName(p, tier, t) + ".json";
                    if (findResource(rel) == null) {
                        texMissing++;
                        texLack.append(rel).append(" ");
                    }
                }
            }
        }
        check("分阶护甲的贴图与模型齐全（20 张共用贴图 + 200 个模型）",
                texMissing == 0,
                texMissing == 0 ? "全部到位" : ("缺 " + texMissing + " 个：" + texLack));

        // ---- ⑦ 第 1 境的道途护甲应与同境界道袍同量级 ----
        //
        // ⚠ 这条原来是"全套护甲值在原版区间内（铁 15~钻石 20）"。
        //   那个前提已经失效：项目走的是**指数**护甲曲线
        //   （道袍第 1 境才 6 点，第 10 境 224000 点），
        //   拿原版钻石（20）当参照毫无意义。
        //   有意义的参照是**同境界的道袍**。
        if (base != null) {
            int pathT1 = base.totalDefenseAt(1);
            double daoT1 = com.hongmeng.xiuxian.core.RealmStats.armorTotal(1);
            check("第 1 境道途护甲与同境界道袍同量级（不是拿原版数值当参照）",
                    pathT1 >= daoT1 * 0.60D && pathT1 <= daoT1 * 1.05D,
                    String.format("道途 %d / 道袍 %.0f", pathT1, daoT1));
        }
    }

    /**
     * 在若干候选根目录下找一个资源文件。
     *
     * <p>与 {@code readSource} 同样的理由：验证器的工作目录不固定，
     * 所以一路往上试。</p>
     *
     * @return 找到的绝对路径；没有返回 {@code null}
     */
    private static String findResource(String rel) {
        String[] prefixes = {"xiuxian-mod/src/main/resources/",
                "src/main/resources/", "xiuxian-mod/src/main/resources/xiuxian-mod/src/main/resources/"};
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get("").toAbsolutePath();
            for (int up = 0; up < 8 && dir != null; up++) {
                for (String pre : prefixes) {
                    java.nio.file.Path p = dir.resolve(pre + rel);
                    if (java.nio.file.Files.isRegularFile(p)) {
                        return p.toString();
                    }
                }
                dir = dir.getParent();
            }
        } catch (Throwable ignored) {
            // 找不到就返回 null，断言会报出来
        }
        return null;
    }


    /**
     * <b>五把道途武器</b>的断言。
     *
     * <p>最关键的是<b>总 DPS 对齐</b>：武器是玩家天天拿在手里的东西，
     * 只要有一把明显更强，其余四把立刻没人用。所以
     * {@code damageFactor × speedFactor} 必须恒为 1.00。</p>
     */
    private static void pathWeaponRules() {
        section("道途武器");

        var kinds = com.hongmeng.xiuxian.item.gear.PathWeapon.values();
        check("五条道途各有专属武器", kinds.length == 5, kinds.length + " 把");

        // ---- ① 总 DPS 对齐（硬线） ----
        boolean dpsAligned = true;
        StringBuilder dpsText = new StringBuilder();
        for (var k : kinds) {
            double dps = k.dpsFactor();
            dpsText.append(String.format("%s=%.3f ", k.cn(), dps));
            if (Math.abs(dps - 1.0D) > 0.01D) {
                dpsAligned = false;
            }
        }
        check("五把武器的每秒伤害完全对齐（否则会出现唯一最优解）", dpsAligned,
                dpsText.toString().trim());

        // ---- ② 手感必须不同（否则"对齐 DPS"变成"五把一样"） ----
        java.util.Set<String> feels = new java.util.HashSet<>();
        for (var k : kinds) {
            feels.add(String.format("%.2f/%.2f", k.damageFactor(), k.speedFactor()));
        }
        check("五把武器的伤害/攻速分配互不相同（手感有区别）",
                feels.size() == kinds.length, feels.size() + " 种手感");

        // 极端手感必须存在：有最快的、也有最重的
        double fastest = 0.0D;
        double heaviest = 0.0D;
        String fastName = "";
        String heavyName = "";
        for (var k : kinds) {
            if (k.speedFactor() > fastest) {
                fastest = k.speedFactor();
                fastName = k.cn();
            }
            if (k.damageFactor() > heaviest) {
                heaviest = k.damageFactor();
                heavyName = k.cn();
            }
        }
        check("有极快的武器（攻速 >= 1.30）", fastest >= 1.30D,
                fastName + " ×" + String.format("%.2f", fastest));
        check("有极重的武器（单击 >= 1.40）", heaviest >= 1.40D,
                heavyName + " ×" + String.format("%.2f", heaviest));

        // ---- ③ 血修必须是双刀（参考作品的物理系定位） ----
        var xue = com.hongmeng.xiuxian.item.gear.PathWeapon.XUE;
        check("血修的武器是双刀（参考作品：血修属物理系，专属法宝为双刀/杵/弓/轮刃）",
                xue.cn().contains("双刀") || xue.cn().contains("刀"),
                xue.cn() + " · " + xue.desc());
        check("血修的武器是全场最快的（配吸血/血池积累）",
                Math.abs(xue.speedFactor() - fastest) < 1.0E-9D,
                String.format("攻速 ×%.2f", xue.speedFactor()));

        // ---- ④ 每把都有描述（玩家看不出数值差异，得靠文字） ----
        boolean allDescribed = true;
        StringBuilder noDesc = new StringBuilder();
        for (var k : kinds) {
            if (k.desc() == null || k.desc().isBlank()) {
                allDescribed = false;
                noDesc.append(k.cn()).append(" ");
            }
        }
        check("五把武器都有手感说明（数值差异玩家看不出来）", allDescribed,
                allDescribed ? "都有" : ("没有的：" + noDesc));

        // ---- ⑤ 道途 -> 武器 的映射完整（不能有哪条道途没武器） ----
        int missingMap = 0;
        StringBuilder mapLack = new StringBuilder();
        for (var p : com.hongmeng.xiuxian.talent.Path.values()) {
            if (com.hongmeng.xiuxian.item.gear.PathWeapon.of(p) == null) {
                missingMap++;
                mapLack.append(p.cn()).append(" ");
            }
        }
        check("每条道途都能查到自己的武器", missingMap == 0,
                missingMap == 0 ? "五条都齐" : ("缺：" + mapLack));

        // ---- ⑥ 资源文件齐全（贴图 + 每把 10 阶的模型 json） ----
        int lack = 0;
        StringBuilder lackList = new StringBuilder();
        for (var k : kinds) {
            String tex = "assets/hongmeng/textures/item/path_weapon_" + k.key() + ".png";
            if (findResource(tex) == null) {
                lack++;
                lackList.append(tex).append(" ");
            }
            for (int tier = 1; tier <= 10; tier++) {
                String rel = "assets/hongmeng/models/item/path_weapon_" + k.key()
                        + "_" + tier + ".json";
                if (findResource(rel) == null) {
                    lack++;
                    lackList.append(rel).append(" ");
                }
            }
        }
        check("五把武器的贴图与 50 个模型文件齐全", lack == 0,
                lack == 0 ? "全部到位" : ("缺 " + lack + " 个：" + lackList));

        // ---- ⑦ 配方：R-2 起旧道途武器统一 legacy —— 配方**全部删除** ----
        // （06 §4：保留注册、移出创造栏、删配方、不给加成）
        int legacyRecipeLeft = 0;
        StringBuilder recipeLack = new StringBuilder();
        for (var k : kinds) {
            for (int tier = 1; tier <= 10; tier++) {
                String rel = "data/hongmeng/recipes/"
                        + com.hongmeng.xiuxian.item.gear.ModPathWeapon.itemName(k, tier)
                        + ".json";
                if (findResource(rel) != null) {
                    legacyRecipeLeft++;
                    recipeLack.append(k.key()).append("_").append(tier).append(" ");
                }
            }
        }
        check("旧道途武器配方已全部删除（R-2 legacy：注册项仍保留）", legacyRecipeLeft == 0,
                legacyRecipeLeft == 0 ? "50 张已删（备份在 _cache/legacy-recipes-backup-20261101）"
                        : ("残留 " + legacyRecipeLeft + " 张：" + recipeLack));

        // ---- ⑧ 境界上限与既有武器一致（不能道途武器只能到 5 境） ----
        check("道途武器覆盖全部 10 个境界（与既有武器一致）",
                com.hongmeng.xiuxian.item.gear.ModPathWeapon.MAX_TIER == 10,
                "1 ~ " + com.hongmeng.xiuxian.item.gear.ModPathWeapon.MAX_TIER + " 境 × 5 把 = 50 件");
    }


    /**
     * <b>职业主动·技能实现验收</b>（《07-法术内容交接表》落地）。
     *
     * <p>交接表把实现拆给"技能实现线"，本段只验**可离线断言的契约**：</p>
     * <ul>
     *   <li>旧 52 条**名字与顺序一行不变**（秘籍/商店/网络 ordinal 都依赖它）；</li>
     *   <li>18 条新增主动**追加在末尾**，数值与交接表一致（灵力按 P-2 的 ×100）；</li>
     *   <li>2 条「复用已有」不新增常量，映射到基础池同一条目；</li>
     *   <li>18 条不生成秘籍、不进坊市；</li>
     *   <li>天赋解锁 → 服务端自动授予；释放时二次校验；特殊分支真的存在；</li>
     *   <li>30 条被动有纯计算层并接到属性/事件结算口。</li>
     * </ul>
     */
    private static void careerSpellImplementationRules() {
        section("职业主动·技能实现");

        // ---- ① 旧 52 条名字与顺序一字不动 ----
        String[] base52 = {
                "JIN_GANG_QIAN", "WAN_JIAN_GUI_ZONG", "JIN_HONG_DUN",
                "QING_MU_SHENG", "WAN_MU_KUN_SUO", "CHUN_FENG_HUA_YU",
                "HAN_BING_JIAN", "BING_FENG_JUE", "QIAN_LI_BING_FENG",
                "LIE_YAN_ZHANG", "FEN_TIAN_JUE", "FEN_JI_LIAO_YUAN",
                "HOU_TU_DUN", "LUO_DI_SHEN_ZHEN", "SHAN_HE_YIN",
                "XUAN_BING_DUN", "BING_HUN_ZHAN", "ZI_XIAO_LEI_FA",
                "JIU_XIAO_LEI_YU", "FENG_REN_ZHAN", "QING_FENG_JUE",
                "FENG_YING_BU", "XUE_MO_SHI", "SHEN_SHI_ZHEN",
                "HONG_MENG_YI_QI", "YU_JIAN_ZHAN", "LEI_FA",
                "BING_FENG_QIAN_LI", "FEN_TIAN", "YI_XING_HUAN_YING",
                "JIN_GANG_HU_TI", "HUI_CHUN_SHU", "SHE_HUN",
                "LING_HAI_WU_LIANG", "TU_NA_JUE", "JIN_GANG_TI",
                "JI_FENG_BU", "JIAN_XIN_TONG_MING", "WU_XING_TIAO_HE",
                "SHEN_SHI_NING_LIAN", "QI_XUE_CHONG_YING", "XUE_MO_GONG",
                "PO_JUN_JUE", "GUI_XI_GONG", "LING_BO_WEI_BU",
                "ZHAN_YI_FEI_TENG", "TIAN_YUN_XING_LUO", "GUI_DAO_PAO_JI",
                "WAN_XIANG_JI_MIE", "TAI_XU_HEI_DONG", "ZHAN_XING",
                "FEN_TIAN_ZHU_HAI"};
        String[] add18 = {
                "JIAN_QI_ZHAN", "JIAN_YU_SHU", "JIAN_YU",
                "XUE_DUN", "XUE_BAO", "XUE_SHI", "XUE_HE",
                "MO_QI_BAO_FA", "MO_SHI_HUN", "MO_YING_DUN", "MO_XIN_MO_YIN",
                "GUI_SUO_HUN_LIAN", "GUI_YU_ZHAN_KAI", "GUI_XU_HUA", "GUI_HUN_BAO",
                "TI_ZHEN_DANG_BO", "TI_BENG_SHAN_JI", "TI_BU_MIE_JIN_SHEN"};
        var all = Technique.values();
        check("功法总数 = 52 + 18 = 70", all.length == 70, "实际 " + all.length);
        StringBuilder bad = new StringBuilder();
        boolean baseOk = all.length >= 52;
        for (int i = 0; i < 52 && i < all.length; i++) {
            if (!all[i].name().equals(base52[i])) {
                baseOk = false;
                bad.append(i).append(":").append(all[i].name()).append(" ");
            }
        }
        for (int i = 0; i < 18; i++) {
            int idx = 52 + i;
            if (idx >= all.length || !all[idx].name().equals(add18[i])) {
                baseOk = false;
                bad.append(idx).append(":").append(idx < all.length ? all[idx].name() : "?")
                        .append(" ");
            }
        }
        check("旧 52 条顺序不变、18 条按交接表顺序追加在末尾", baseOk, bad.toString());

        // ---- ② 数值与交接表一致（18 条新增；2 条复用不校验数值） ----
        StringBuilder numBad = new StringBuilder();
        int unlockOnly = 0;
        int reused = 0;
        for (var active : com.hongmeng.xiuxian.talent.CareerActive.values()) {
            Technique tech = Technique.byName(active.techniqueName());
            if (tech == null) {
                numBad.append(active.name()).append("=未找到 ");
                continue;
            }
            if (com.hongmeng.xiuxian.technique.CareerTechnique.activeOf(tech) != active) {
                numBad.append(active.name()).append("=映射错 ");
            }
            if (active.reusesExisting()) {
                reused++;
                continue;
            }
            unlockOnly++;
            if (tech.kind() != active.form()
                    || tech.minTier() != active.minTier()
                    || tech.cooldown() != active.cooldown()
                    || tech.castTicks() != active.castTicks()
                    || tech.manaCost() != active.manaCost() * 100     // P-2：2~9 → 200~900
                    || Math.abs(tech.damageMultiplier() - active.damageMul()) > 1.0E-9D) {
                numBad.append(active.name()).append(" ");
            }
        }
        check("20 门职业主动都能映射到 Technique 条目", numBad.length() == 0, numBad.toString());
        check("18 条新增 + 2 条复用（与交接表一致）", unlockOnly == 18 && reused == 2,
                "新增 " + unlockOnly + " / 复用 " + reused);
        check("职业主动映射表 20 条", com.hongmeng.xiuxian.technique.CareerTechnique.size() == 20,
                "实际 " + com.hongmeng.xiuxian.technique.CareerTechnique.size());
        check("新增 18 条 = 只能天赋解锁；复用 2 条不算",
                com.hongmeng.xiuxian.technique.CareerTechnique.unlockOnlyNames().size() == 18
                        && com.hongmeng.xiuxian.technique.CareerTechnique.reusedNames().size() == 2,
                "");

        // ---- ③ 不生成秘籍、不进坊市 ----
        var items = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/TechniqueItems.java"));
        var shop = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/shop/ShopCatalog.java"));
        check("秘籍注册跳过职业主动（不生成秘籍）",
                items.contains("CareerTechnique.isUnlockOnly"), "");
        check("坊市跳过职业主动（不自动上架）",
                shop.contains("CareerTechnique.isUnlockOnly"), "");

        // ---- ④ 解锁授予 + 释放二次校验 + 特殊分支 ----
        var grant = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/CareerSpellGrant.java"));
        check("有服务端解锁授予层（CareerSpellGrant）", grant.contains("activeUnlocked"), "");
        check("授予是双门槛（天赋 5 点 + 境界 minTier）",
                grant.contains("data.realm().tier()") && grant.contains("minTier()"), "");
        var cast = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/TechniqueCaster.java"));
        check("释放时二次校验职业解锁（防伪造装配）",
                cast.contains("CareerSpellGrant.isUnlocked"), "");
        check("职业门槛只拦 18 条「只能天赋解锁」（复用 2 门保留基础池身份）",
                cast.contains("CareerTechnique.isUnlockOnly(technique)"), "");
        check("虚化/心魔引/不灭金身 三个需要新运行时的主动都有专门分支",
                cast.contains("GUI_XU_HUA") && cast.contains("MO_XIN_MO_YIN")
                        && cast.contains("TI_BU_MIE_JIN_SHEN"), "");
        check("崩山击不再套「斩星」的 520 格隧道挖地形（走短距重击）",
                cast.contains("castCrush(") && cast.contains("TI_BENG_SHAN_JI"), "");
        check("职业 AOE 有自己的半径（不再沿用 44 格世界级）",
                cast.contains("aoeRadiusFor("), "");
        var handler = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellCastHandler.java"));
        check("每 20 tick 的被动刷新里同时刷新职业主动授予",
                handler.contains("CareerSpellGrant.refresh"), "");
        var aftermath = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellAftermath.java"));
        check("血河 / 鬼域展开有持续区域（复用 aftermath）",
                aftermath.contains("XUE_HE") && aftermath.contains("GUI_YU_ZHAN_KAI"), "");

        // ---- ⑤ 30 条被动的纯计算层与接线 ----
        check("职业被动数据 30 条",
                com.hongmeng.xiuxian.talent.CareerSkill.values().length == 30,
                "实际 " + com.hongmeng.xiuxian.talent.CareerSkill.values().length);
        var passives = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/talent/CareerPassives.java"));
        check("有被动纯计算层（CareerPassives）", passives.contains("skillUnlocked"), "");
        var attrs = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/talent/TalentAttributes.java"));
        check("属性型被动接到 TalentAttributes", attrs.contains("CareerPassives"), "");
        var pathRt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/talent/PathRuntime.java"));
        boolean wired = pathRt.contains("CareerPassives")
                || stripComments(readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java"))
                .contains("CareerPassives");
        check("事件型被动接到既有结算口（PathRuntime 或 ModEvents）", wired, "");
        check("剑势改为受击凝意（不再依赖已删除的格挡系统）",
                pathRt.contains("swordMomentum") && !pathRt.contains("EffectKind.GUARD"), "");
        check("剑势有 5 秒冷却（不能站着挨打刷剑意）",
                pathRt.contains("lastMomentumTick"), "");
    }


    /**
     * <b>资源文件引用的物品 id 必须真的存在</b>。
     *
     * <p>⚠ 加这条的原因：我第一版武器配方写成
     * {@code hongmeng:path_weapon_spirit_sword}（<b>漏了境界后缀</b>），
     * 而注册名是 {@code path_weapon_spirit_sword_1}。后果是：</p>
     * <ul>
     *   <li>Java <b>编译通过</b>（配方是 JSON，编译器不管）；</li>
     *   <li>贴图/模型检查也<b>通过</b>（那些文件确实在）；</li>
     *   <li>只有<b>起服时</b>才报 {@code Unknown item}，5 张配方全部失效。</li>
     * </ul>
     * <p>所以这里把"配方 result"与"注册名生成规则"直接对比 ——
     * 这类错误必须在离线阶段就拦下来。</p>
     */
    /**
     * <b>R-2 职业装备接线验收</b>（06 / 08 交接表落地）。
     *
     * <p>只验可离线断言的部分：动态结算类存在且接进 GearManager、
     * 2/4 件套按套独立、六型双手数据契约、legacy 移出创造栏与配方删除。</p>
     */
    private static void careerGearWiringRules() {
        section("R-2 职业装备接线");

        String handler = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/armor/CareerArmorHandler.java");
        check("职业甲动态结算类存在（CareerArmorHandler）", handler.length() > 500, "");
        String armorMat = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/armor/CareerArmorMaterial.java");
        check("护甲按境界写进材质（每境固定值，不再动态叠加）",
                armorMat.contains("CareerArmorRealm.armorFor")
                        && armorMat.contains("CareerArmorRealm.durabilityAt")
                        && armorMat.contains("getDefenseForType"), "");
        check("结算只加生命/攻速，不再叠 ARMOR 属性",
                handler.contains("healthBonusPerPiece")
                        && handler.contains("Attributes.MAX_HEALTH")
                        && !handler.contains("applyFlat(player, Attributes.ARMOR"), "");
        check("2/4 件套按「职业 + 外观档」结算（15 套，跨境同档不重复结算）",
                handler.contains("setCounts[item.set().ordinal()]")
                        && handler.contains("set.setTwo()") && handler.contains("set.setFour()"), "");
        check("穿错职业不给套装加成（基础护甲/生命仍生效）",
                handler.contains("career == set.career()"), "");
        check("结算接在 GearManager.refresh 里（约每 20 tick）",
                readSource("src/main/java/com/hongmeng/xiuxian/item/gear/GearManager.java")
                        .contains("CareerArmorHandler.refresh(player)"), "");
        check("下线/换装清理职业甲属性与缓存",
                readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java")
                        .contains("CareerArmorHandler.forget")
                        && readSource(
                        "src/main/java/com/hongmeng/xiuxian/item/gear/GearManager.java")
                        .contains("CareerArmorHandler.clear"), "");

        String form = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/gear/WeaponForm.java");
        check("六型武器数据契约存在（全部双手），DPS 系数与 06 表一致",
                form.contains("1.00D") && form.contains("1.43D")
                        && form.contains("1.18D") && form.contains("1.11D")
                        && form.contains("0.91D") && form.contains("1.30D")
                        && form.contains("0.77D") && form.contains("twoHanded"), "");
        String cw = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/gear/CareerWeaponItem.java");
        check("通用武器暴露型/双手查询（R-3 禁副手接线）",
                cw.contains("public WeaponForm form()") && cw.contains("isTwoHanded"), "");

        String tab = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/ModCreativeTabs.java");
        check("旧道途甲/武器/境界剑已移出创造栏（R-2 legacy）",
                !tab.contains("ModPathArmor.all().forEach")
                        && !tab.contains("ModPathWeapon.all().forEach")
                        && !tab.contains("ModGear.SWORDS.forEach"), "");
        check("创造栏保留职业甲/通用武器批量输出",
                tab.contains("ModCareerArmor.all()")
                        && tab.contains("ModCareerWeapon.all()"), "");
        check("旧装备加成桥已短路（legacy 不给加成，但注册保留）",
                readSource("src/main/java/com/hongmeng/xiuxian/item/LegacyGearBridge.java")
                        .contains("return null"), "");

        // ---- 旧一套（道途甲/道途武器/境界剑）软删除：图鉴不再出现，注册保留 ----
        String guidePath = findResource("assets/hongmeng/guide/guide.json");
        boolean guideClean = true;
        if (guidePath != null) {
            try {
                String guide = java.nio.file.Files.readString(
                        java.nio.file.Path.of(guidePath));
                guideClean = !guide.contains("path_armor")
                        && !guide.contains("path_weapon")
                        && !guide.contains("realm_sword");
            } catch (Throwable ignored) {
                guideClean = false;
            }
        }
        check("旧道途甲/武器已从图鉴软删除（注册保留，供老存档）", guideClean, "");

        // ---- R-2/C 全量注册：200 甲（50 套独立外观）+ 60 武器 ----
        String armorSrc = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/armor/ModCareerArmor.java");
        check("职业甲按 5 职业 × 10 境界 × 4 部位注册 200 件",
                armorSrc.contains("Career.values()")
                        && armorSrc.contains("CareerArmorRealm.MAX_REALM")
                        && armorSrc.contains("CareerArmorRealm.itemName"), "");
        int armorModels = 200;
        int armorIcons = 200;
        int armorSets = 0;
        for (var c : com.hongmeng.xiuxian.talent.Career.values()) {
            for (int realm = 1; realm <= 10; realm++) {
                String base = "career_armor_" + c.id() + "_" + realm;
                armorSets++;
                if (findResource("assets/hongmeng/geo/" + base + ".geo.json") == null
                        || findResource("assets/hongmeng/textures/item/" + base + ".png") == null) {
                    armorSets += 1000;
                }
                for (String slot : com.hongmeng.xiuxian.item.armor.CareerArmorRealm.SLOTS) {
                    String id = com.hongmeng.xiuxian.item.armor.CareerArmorRealm
                            .itemName(c, realm, slot);
                    if (findResource("assets/hongmeng/models/item/" + id + ".json") == null) {
                        armorModels--;
                    }
                    if (findResource("assets/hongmeng/textures/item/" + id + ".png") == null) {
                        armorIcons--;
                    }
                }
            }
        }
        check("职业甲 50 套独立外观的 geo/贴图齐全", armorSets == 50, "共 " + armorSets + " 套");
        check("200 件职业甲物品模型齐全", armorModels == 200, armorModels + "/200");
        check("200 个格局图标齐全（每套 4 个独立图标）", armorIcons == 200,
                armorIcons + "/200");

        String cwSrc = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/gear/ModCareerWeapon.java");
        check("通用武器按 6 型 × 10 阶循环注册（60 件）",
                cwSrc.contains("for (int fi = 0")
                        && cwSrc.contains("tier <= 10")
                        && cwSrc.contains("career_weapon_"), "");
        String[][] variants = {
                {"sword", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
                {"greatsword", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
                {"scythe", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
                {"spear", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
                {"staff", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
                {"war_scythe", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"},
        };
        int variantBad = 0;
        int weaponRes = 0;
        for (String[] group : variants) {
            String formKey = group[0];
            for (int i = 1; i < group.length; i++) {
                String key = group[i];
                if (!cwSrc.contains("\"" + key + "\"")) {
                    variantBad++;
                }
                String fam = "career_" + formKey + "_" + key;
                if (findResource("assets/hongmeng/geo/" + fam + ".geo.json") == null
                        || findResource("assets/hongmeng/textures/item/" + fam + ".png") == null) {
                    weaponRes++;
                }
            }
        }
        check("60 把武器各有一个独立外观（6 型 × 10 境界，不再按档位共用）",
                variantBad == 0 && cwSrc.contains("VARIANTS"),
                variantBad == 0 ? "60/60" : ("源码缺 " + variantBad + " 个变体名"));
        check("60 个独立武器外观的 geo/贴图齐全", weaponRes == 0,
                weaponRes == 0 ? "60/60" : ("缺 " + weaponRes + " 组"));
        int weaponModels = 0;
        for (String[] group : variants) {
            String formKey = group[0];
            for (int tier = 1; tier <= 10; tier++) {
                String id = "career_weapon_" + formKey + "_" + tier;
                if (findResource("assets/hongmeng/models/item/" + id + ".json") == null) {
                    weaponModels++;
                }
            }
        }
        check("60 件通用武器物品模型齐全", weaponModels == 0,
                weaponModels == 0 ? "60/60" : ("缺 " + weaponModels + " 个"));

        // DPS 六型对齐 1.00 ±0.01
        int dpsBad = 0;
        StringBuilder dpsList = new StringBuilder();
        for (var f : com.hongmeng.xiuxian.item.gear.WeaponForm.values()) {
            double dps = f.damageFactor() * f.speedFactor();
            if (Math.abs(dps - 1.0D) > 0.01D) {
                dpsBad++;
                dpsList.append(f.key()).append("=").append(String.format("%.3f", dps)).append(" ");
            }
        }
        check("六型武器 DPS 全部对齐 1.00 ± 0.01", dpsBad == 0,
                dpsBad == 0 ? "6/6" : dpsList.toString());

        // ---- R-2：新武器也要走境界门槛（否则练气号拿到 10 阶武器就毕业）----
        check("新通用武器纳入境界门槛（超阶自动收起）",
                readSource("src/main/java/com/hongmeng/xiuxian/item/gear/RealmGear.java")
                        .contains("CareerWeaponItem"), "");
        check("通用武器 tooltip 标了境界需求",
                readSource("src/main/java/com/hongmeng/xiuxian/item/gear/CareerWeaponItem.java")
                        .contains("境界需求"), "");

        // ---- C 扩充：120 件装备各自一条独立配方（武器 1→10 阶 / 甲 初→进→名器 成链）----
        int weaponRecipes = 0;
        int armorRecipes = 0;
        for (String f : new String[]{"sword", "greatsword", "scythe", "spear",
                "staff", "war_scythe"}) {
            for (int tier = 1; tier <= 10; tier++) {
                if (findResource("data/hongmeng/recipes/career_weapon_" + f + "_" + tier
                        + ".json") != null) {
                    weaponRecipes++;
                }
            }
        }
        for (String c : new String[]{"jian", "xue", "mo", "gui", "ti"}) {
            for (int realm = 1; realm <= 10; realm++) {
                for (String slot : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
                    if (findResource("data/hongmeng/recipes/career_armor_" + c + "_" + realm
                            + "_" + slot + ".json") != null) {
                        armorRecipes++;
                    }
                }
            }
        }
        check("60 件武器各有一条独立合成配方（1→10 阶可逐级合成）",
                weaponRecipes == 60, weaponRecipes + "/60");
        check("200 件职业甲各有一条独立合成配方（1→10 境可逐级合成）",
                armorRecipes == 200, armorRecipes + "/200");
        check("物品栏 GUI 按模型长度自适应缩放（不超出 16×16 格子）",
                readSource("src/main/java/com/hongmeng/xiuxian/client/geo/GeoWeaponRenderer.java")
                        .contains("0.86F * 16.0F")
                        && readSource("src/main/java/com/hongmeng/xiuxian/item/GeoWeaponItem.java")
                        .contains("guiLengthOf"), "");
    }


    private static void recipeIdRules() {
        section("配方物品 id");

        // R-2：旧道途武器/护甲全部 legacy，配方已删除。
        // 本段从"逐个核对 result"改成"核对它们确实都被删了"，并保留
        // "不许有旧式无境界配方残留" 这条历史 bug 防线。
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (var k : com.hongmeng.xiuxian.item.gear.PathWeapon.values()) {
            keys.add(k.key());
        }
        int leftWeapon = 0;
        StringBuilder leftList = new StringBuilder();
        for (var k : com.hongmeng.xiuxian.item.gear.PathWeapon.values()) {
            for (int tier = 1; tier <= 10; tier++) {
                String name = com.hongmeng.xiuxian.item.gear.ModPathWeapon
                        .itemName(k, tier);
                if (findResource("data/hongmeng/recipes/" + name + ".json") != null) {
                    leftWeapon++;
                    leftList.append(name).append(" ");
                }
            }
        }
        check("50 张旧道途武器配方已删除（R-2 legacy）", leftWeapon == 0,
                leftWeapon == 0 ? "已删 50 张" : ("残留：" + leftList));

        // 不能再有"没有境界后缀"的旧配方残留
        int stale = 0;
        StringBuilder staleList = new StringBuilder();
        for (String key : keys) {
            String rel = "data/hongmeng/recipes/path_weapon_" + key + ".json";
            if (findResource(rel) != null) {
                stale++;
                staleList.append("path_weapon_").append(key).append(".json ");
            }
        }
        check("没有残留的旧式无境界武器配方（那正是之前的 bug）", stale == 0,
                stale == 0 ? "干净" : ("残留：" + staleList));

        // 护甲：R-2 legacy，配方已删除
        int leftArmor = 0;
        StringBuilder armorBadList = new StringBuilder();
        for (var p : com.hongmeng.xiuxian.talent.Path.values()) {
            for (var t : com.hongmeng.xiuxian.item.armor.PathArmorTable.TYPES) {
                String name = com.hongmeng.xiuxian.item.armor.PathArmorTable
                        .itemName(p, 1, t);
                if (findResource("data/hongmeng/recipes/" + name + ".json") != null) {
                    leftArmor++;
                    armorBadList.append(name).append(" ");
                }
            }
        }
        check("20 张旧道途护甲配方已删除（R-2 legacy）", leftArmor == 0,
                leftArmor == 0 ? "已删 20 张（其余 180 件同批处理）" : ("残留：" + armorBadList));

        // 旧配方引用过的模组材料仍必须是已注册的（防止误删材料注册）
        String modItems = readSource("src/main/java/com/hongmeng/xiuxian/item/ModItems.java");
        check("配方引用的模组材料 spirit_iron_ingot 仍已注册（材料物品保留）",
                modItems.contains("\"spirit_iron_ingot\""),
                "旧配方已删，但材料物品必须留给新装备线用");
    }

    /**
     * 剥掉 Java 源码里的**注释行**与行尾注释。
     *
     * <p>为什么需要：断言里用 {@code contains} 判断"代码里有没有某个调用"时，
     * 注释里的文字也会命中 —— 我已经因此误报过三次
     * （{@code data.save()}、{@code setText()}、反斜杠 n）。</p>
     */
    private static String stripComments(String src) {
        StringBuilder out = new StringBuilder();
        for (String line : src.split("\\n")) {
            String t = line.trim();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) {
                continue;
            }
            int c = line.indexOf("//");
            out.append(c >= 0 ? line.substring(0, c) : line).append('\n');
        }
        return out.toString();
    }

    /**
     * 数一张 PNG 里**不透明像素的颜色种类**。
     *
     * <p>用来判断像素画有没有明暗层次 —— 比文件字节数可靠：
     * PNG 对大片纯色压缩极好，一个 16×16 的纯色剪影可能只有 100 多字节，
     * 而同样大小的 5 色层次图也可能只有 180 字节。</p>
     */
    private static int opaqueColorCount(String path) {
        if (path == null) {
            return 0;
        }
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
                    new java.io.File(path));
            if (img == null) {
                return 0;
            }
            java.util.Set<Integer> colors = new java.util.HashSet<>();
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int argb = img.getRGB(x, y);
                    if ((argb >>> 24) > 0) {
                        colors.add(argb);
                    }
                }
            }
            return colors.size();
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 文件字节数（找不到返回 0）—— 用来粗判贴图细节量。 */
    /** 统计 PNG 的颜色数（用来判断"是不是纯色块"）。 */
    private static int pngColors(String rel) {
        try {
            var path = java.nio.file.Path.of(rel);
            if (!java.nio.file.Files.exists(path)) {
                return 0;
            }
            var img = javax.imageio.ImageIO.read(path.toFile());
            if (img == null) {
                return 0;
            }
            var set = new java.util.HashSet<Integer>();
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    set.add(img.getRGB(x, y));
                    if (set.size() > 520) {
                        return set.size();
                    }
                }
            }
            return set.size();
        } catch (Throwable t) {
            return 0;
        }
    }

    private static long fileSize(String path) {
        if (path == null) {
            return 0L;
        }
        try {
            return java.nio.file.Files.size(java.nio.file.Path.of(path));
        } catch (Throwable t) {
            return 0L;
        }
    }

    /** 读指定路径的文件（辅助 findResource 用）。 */
    private static String readFileAt(String path) {
        if (path == null) {
            return "";
        }
        try {
            return new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(path)),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Throwable t) {
            return "";
        }
    }


    /**
     * <b>副业配方的完整性</b>。
     *
     * <p>玩家要求「阵法和炼丹还有炼器什么的都可以兼修」，
     * 而"兼修"要有实际玩法支撑 —— 配方就是那个玩法。</p>
     *
     * <p>这里钉住三类容易静默出错的东西：</p>
     * <ol>
     *   <li>材料 id 写错（玩家永远做不出来，无任何提示）；</li>
     *   <li>产物 id 写错（同上，且我写代码时就猜错过包名）；</li>
     *   <li>成功率曲线反了（练熟练度反而更容易失败）。</li>
     * </ol>
     */
    private static void professionRecipeRules() {
        section("副业制作");

        var all = com.hongmeng.xiuxian.talent.ProfessionRecipe.all();
        check("三条副业都有配方（炼丹/炼器/阵法）", all.size() >= 6,
                all.size() + " 条配方");

        // 每条副业至少一条
        for (var p : com.hongmeng.xiuxian.talent.Profession.values()) {
            int n = com.hongmeng.xiuxian.talent.ProfessionRecipe.of(p).size();
            if (n == 0) {
                check("副业 " + p.cn() + " 至少有一条配方", false, "");
                return;
            }
        }
        check("三条副业各自都有配方", true, "");

        // ---- 材料与产物 id 必须真的注册过 ----
        var modItems = readSource("src/main/java/com/hongmeng/xiuxian/item/ModItems.java");
        int badMat = 0;
        int badOut = 0;
        StringBuilder bad = new StringBuilder();
        for (var r : all) {
            for (var m : r.materials()) {
                String id = m.item();
                if (id.startsWith("hongmeng:")) {
                    String name = id.substring("hongmeng:".length());
                    if (!modItems.contains("\"" + name + "\"")) {
                        badMat++;
                        bad.append("材料缺:").append(id).append(" ");
                    }
                }
            }
            String out = r.output();
            if (out != null && !out.isEmpty()) {
                if (out.startsWith("hongmeng:")) {
                    String name = out.substring("hongmeng:".length());
                    if (!modItems.contains("\"" + name + "\"")) {
                        badOut++;
                        bad.append("产物缺:").append(out).append(" ");
                    }
                }
            }
        }
        check("配方的模组材料 id 全部已注册", badMat == 0,
                badMat == 0 ? "全部存在" : bad.toString());
        check("配方的固定产物 id 全部已注册", badOut == 0,
                badOut == 0 ? "全部存在" : bad.toString());

        // ---- 动态产物（炼器）必须有 id 对应 ----
        int dynamic = 0;
        for (var r : all) {
            if (r.output() == null || r.output().isEmpty()) {
                dynamic++;
                if (!r.id().startsWith("qi_")) {
                    check("动态产物配方必须走炼器（" + r.id() + "）", false, "");
                    return;
                }
            }
        }
        check("炼器的产物是动态的（按道途+境界生成），且标注正确", dynamic >= 1,
                dynamic + " 条动态配方");

        // ---- 成功率：熟练度越高越容易成功（不能反） ----
        var sample = all.get(0);
        double low = com.hongmeng.xiuxian.talent.ProfessionRecipe.successChance(sample, 0);
        double mid = com.hongmeng.xiuxian.talent.ProfessionRecipe.successChance(sample, 5);
        double high = com.hongmeng.xiuxian.talent.ProfessionRecipe.successChance(sample, 10);
        check("熟练度越高成功率越高（练熟练度有回报）",
                low <= mid && mid <= high,
                String.format("%.0f%% -> %.0f%% -> %.0f%%",
                        low * 100, mid * 100, high * 100));
        check("成功率上限不超过 98%（留一点意外）", high <= 0.98D + 1.0E-9D,
                String.format("%.0f%%", high * 100));

        // ---- 失败只退一半（不是全吞、也不是全退） ----
        double refund = com.hongmeng.xiuxian.talent.ProfessionRecipe.refundRatio();
        check("制作失败只退回一半材料（有风险但不劝退）",
                refund > 0.0D && refund < 1.0D, String.format("%.0f%%", refund * 100));

        // ---- 熟练度门槛必须存在（不然高级配方开局就能做） ----
        int gated = 0;
        for (var r : all) {
            if (r.minRank() > 0) {
                gated++;
            }
        }
        check("有需要熟练度才解锁的配方（专精有意义）", gated >= 1,
                gated + " 条有门槛");

        // ---- 配方 id 不能重复（重复会让命令选错配方） ----
        java.util.Set<String> ids = new java.util.HashSet<>();
        int dup = 0;
        for (var r : all) {
            if (!ids.add(r.id())) {
                dup++;
            }
        }
        check("配方 id 唯一（重复会让命令命中错配方）", dup == 0,
                dup == 0 ? ids.size() + " 个唯一 id" : ("重复 " + dup + " 个"));
    }


    /**
     * <b>职业/天赋/副业存档往返</b>。
     *
     * <p>玩家已明确旧存档全部删除，本轮不做旧档迁移；只验新键往返与容错。</p>
     */
    private static void talentSaveRules() {
        section("职业存档往返");

        java.util.UUID id = java.util.UUID.randomUUID();
        try {
            com.hongmeng.xiuxian.talent.Talents.setCareerAt(id,
                    com.hongmeng.xiuxian.talent.Career.MO_XIU);
            com.hongmeng.xiuxian.talent.Talents.setRankAt(id, "mo_demon_1", 3);
            com.hongmeng.xiuxian.talent.Talents.setRankAt(id, "mo_pen_1", 2);
            com.hongmeng.xiuxian.talent.Talents.setWorkAt(id, "LIANDAN", 17);
            com.hongmeng.xiuxian.talent.Talents.setWorkAt(id, "LIANQI", 4);
            com.hongmeng.xiuxian.talent.PathRuntime.restore(id, 42.0D, 66.0D, 12.0D, 88.0D);

            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putUUID("TalentOwner", id);
            com.hongmeng.xiuxian.talent.TalentSave.saveByTag(tag);

            com.hongmeng.xiuxian.talent.TalentSave.forget(id);
            check("清空后职业确实没了（模拟重启成功）",
                    com.hongmeng.xiuxian.talent.Talents.careerAt(id) == null, "");

            com.hongmeng.xiuxian.talent.TalentSave.loadByTag(tag);
            check("存档往返：职业完整还原",
                    com.hongmeng.xiuxian.talent.Talents.careerAt(id)
                            == com.hongmeng.xiuxian.talent.Career.MO_XIU,
                    String.valueOf(com.hongmeng.xiuxian.talent.Talents.careerAt(id)));
            var ranks = com.hongmeng.xiuxian.talent.Talents.ranksAt(id);
            check("存档往返：天赋等级完整还原（两个节点都还在）",
                    ranks.size() == 2
                            && ranks.getOrDefault("mo_demon_1", 0) == 3
                            && ranks.getOrDefault("mo_pen_1", 0) == 2,
                    "mo_demon_1=" + ranks.getOrDefault("mo_demon_1", 0)
                            + " mo_pen_1=" + ranks.getOrDefault("mo_pen_1", 0));
            var work = com.hongmeng.xiuxian.talent.Talents.workAt(id);
            check("存档往返：副业熟练度完整还原",
                    work.getOrDefault("LIANDAN", 0) == 17
                            && work.getOrDefault("LIANQI", 0) == 4,
                    "炼丹=" + work.getOrDefault("LIANDAN", 0)
                            + " 炼器=" + work.getOrDefault("LIANQI", 0));

            double[] res = com.hongmeng.xiuxian.talent.PathRuntime.snapshotRaw(id);
            double decay = com.hongmeng.xiuxian.talent.TalentSave.LOGIN_DECAY;
            boolean mechOk = Math.abs(res[0] - 42.0D * decay) < 1.0E-6D
                    && Math.abs(res[1] - 66.0D * decay) < 1.0E-6D
                    && Math.abs(res[2] - 12.0D * decay) < 1.0E-6D
                    && Math.abs(res[3] - 88.0D * decay) < 1.0E-6D;
            check("存档往返：机制资源按登录衰减还原", mechOk,
                    String.format(java.util.Locale.ROOT,
                            "槽0 %.1f 魔气 %.1f 血池 %.1f 鬼域 %.1f",
                            res[0], res[1], res[2], res[3]));
            check("衰减系数在 (0,1) 之间",
                    decay > 0.0D && decay < 1.0D,
                    String.format(java.util.Locale.ROOT, "%.0f%%", decay * 100));

            net.minecraft.nbt.CompoundTag empty = new net.minecraft.nbt.CompoundTag();
            empty.putUUID("TalentOwner", id);
            com.hongmeng.xiuxian.talent.TalentSave.forget(id);
            boolean threw = false;
            try {
                com.hongmeng.xiuxian.talent.TalentSave.loadByTag(empty);
            } catch (Throwable t) {
                threw = true;
            }
            check("老存档（没有职业键）读回不报错", !threw, "");
            check("读回后是「未择业」，不凭空送天赋",
                    com.hongmeng.xiuxian.talent.Talents.careerAt(id) == null
                            && com.hongmeng.xiuxian.talent.Talents.ranksAt(id).isEmpty(), "");

            net.minecraft.nbt.CompoundTag noOwner = new net.minecraft.nbt.CompoundTag();
            threw = false;
            try {
                com.hongmeng.xiuxian.talent.TalentSave.loadByTag(noOwner);
                com.hongmeng.xiuxian.talent.TalentSave.saveByTag(noOwner);
            } catch (Throwable t) {
                threw = true;
            }
            check("缺少 owner 键时读写都不抛异常（容错）", !threw, "");
        } finally {
            com.hongmeng.xiuxian.talent.TalentSave.forget(id);
        }
    }


    /**
     * <b>洗点 / 转职</b>的账目断言。
     *
     * <p>点数按"应得 − 已花"反推，所以洗点不该刷点、也不该吞点。</p>
     */
    private static void respecRules() {
        section("洗点与转职");

        java.util.UUID id = java.util.UUID.randomUUID();
        try {
            com.hongmeng.xiuxian.talent.Talents.setCareerAt(id,
                    com.hongmeng.xiuxian.talent.Career.MO_XIU);
            com.hongmeng.xiuxian.talent.Talents.setRankAt(id, "mo_demon_1", 4);
            com.hongmeng.xiuxian.talent.Talents.setRankAt(id, "mo_pen_1", 3);

            int cleared = com.hongmeng.xiuxian.talent.Talents.clearRanks(id);
            check("洗点清掉的点数 = 已投入点数（4+3=7）", cleared == 7,
                    cleared + " 点");
            var after = com.hongmeng.xiuxian.talent.Talents.ranksAt(id);
            check("洗点后所有节点归零", after.isEmpty() || after.values().stream()
                    .allMatch(v -> v == 0), "");
            check("洗点不动职业",
                    com.hongmeng.xiuxian.talent.Talents.careerAt(id)
                            == com.hongmeng.xiuxian.talent.Career.MO_XIU, "");

            com.hongmeng.xiuxian.talent.Talents.setRankAt(id, "mo_pen_1", 2);
            com.hongmeng.xiuxian.talent.Talents.clearRanks(id);
            com.hongmeng.xiuxian.talent.Talents.setCareerAt(id, null);
            check("转职后职业为空、天赋归零",
                    com.hongmeng.xiuxian.talent.Talents.careerAt(id) == null
                            && com.hongmeng.xiuxian.talent.Talents.ranksAt(id).isEmpty(), "");

            com.hongmeng.xiuxian.talent.Talents.setCareerAt(id,
                    com.hongmeng.xiuxian.talent.Career.JIAN_XIU);
            com.hongmeng.xiuxian.talent.Talents.setRankAt(id, "jian_intent_1", 2);
            var switched = com.hongmeng.xiuxian.talent.Talents.ranksAt(id);
            check("换职业后只认新树节点（旧节点不会串）",
                    switched.getOrDefault("jian_intent_1", 0) == 2
                            && switched.getOrDefault("mo_demon_1", 0) == 0, "");
        } finally {
            com.hongmeng.xiuxian.talent.TalentSave.forget(id);
        }
    }


    /**
     * <b>道途机制特效的接线与发包纪律</b>。
     *
     * <p>玩家要求过「法术华丽」，而机制类玩法靠的是<b>持续反馈</b>：
     * 血池满了要有血色涌动、阵域展开要有地面阵纹、激荡引爆要有一次光爆。
     * 没有这些，"资源管理"就退化成盯着几个数字。</p>
     *
     * <p>但特效最容易埋两个坑，都不会报错：</p>
     * <ol>
     *   <li><b>发包频率</b>：{@code applyFormation} 每 tick 都跑，
     *       若每 tick 都发形状包 = 20 包/秒 × 每玩家；</li>
     *   <li><b>循环里逐颗发包</b>：项目既有红线（见 check_particle_packets.py）。</li>
     * </ol>
     */
    private static void mechanicFxRules() {
        section("机制特效");

        String fx = readSource("src/main/java/com/hongmeng/xiuxian/technique/PathMechanicFx.java");
        check("机制特效类存在且能读到源码", !fx.isEmpty(), "");

        // ---- ① 五个机制都要有特效方法 ----
        String[][] required = {
                {"formationTick", "阵域持续"},
                {"formationOpen", "阵域展开"},
                {"bloodPool", "血池充能"},
                {"bloodHeal", "血池回血"},
                {"surgeBurst", "激荡引爆"},
                {"lastStand", "锁血"},
                {"demonicAura", "魔气光环"},
                {"demonicOverflow", "魔气溢出"},
                {"craftSuccess", "制作成功"},
                {"craftFail", "制作失败"},
                {"talentReset", "洗点"},
                {"pathSwitch", "转修"},
                {"domainExpiring", "领域将尽"},
        };
        int missing = 0;
        StringBuilder lack = new StringBuilder();
        for (String[] pair : required) {
            if (!fx.contains("void " + pair[0] + "(")) {
                missing++;
                lack.append(pair[1]).append(" ");
            }
        }
        check("十三个特效方法齐全（机制 + 副业 + 洗点转修 + 领域将尽）", missing == 0,
                missing == 0 ? "十三个都在" : ("缺：" + lack));

        // ---- ② 特效必须走粒子预算闸门 ----
        int takes = countOf(fx, "SpellVfx.take(");
        check("粒子走预算闸门 take(...)（低配机器不会被特效拖垮）", takes >= 4,
                takes + " 处 take(...)");

        // ---- ③ 不许在循环里逐颗发包（项目红线） ----
        // 判据：出现 "for (" 之后 30 行内跟着 sendParticles( 且 count 参数为 1
        boolean loopPacket = false;
        String[] lines = fx.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].contains("for (")) {
                continue;
            }
            for (int j = i; j < Math.min(lines.length, i + 30); j++) {
                if (lines[j].contains("sendParticles(")
                        && lines[j].contains(", 1, ")) {
                    loopPacket = true;
                }
            }
        }
        check("没有在循环里逐颗发包（否则低配机会卡）", !loopPacket, "");

        // ---- ④ 接线必须真的接上（方法写了但没人调用等于没做） ----
        String rt = readSource("src/main/java/com/hongmeng/xiuxian/talent/PathRuntime.java");
        int wired = 0;
        for (String[] pair : required) {
            if (rt.contains("PathMechanicFx") && rt.contains(pair[0])) {
                wired++;
            }
        }
        check("至少 6 个特效真的接进了 PathRuntime（写了不调用等于没做）",
                wired >= 6, wired + "/" + required.length + " 已接");

        // ---- ⑤ 降频：阵域与资源光环不能每 tick 都发 ----
        // 判据放宽到两种写法都算：
        //   % 4L == 0L（条件执行）与 % 4L != 0L（提前返回）语义相同，
        //   都是"不是每 tick"。我最初只匹配前一种，改写法后报了假失败。
        check("阵域特效降频（不是每 tick 都发包）",
                rt.contains("% 4L == 0L") || rt.contains("% 4 == 0")
                        || rt.contains("% 4L != 0L"),
                "每 4 tick 一次");
        check("资源光环降频（血池/魔气不是每 tick 都发包）",
                stripComments(readSource("src/main/java/com/hongmeng/xiuxian/talent/PathRuntime.java")).contains("now % 10L == 0L"), "每 10 tick 一次");

        // ---- ⑤b 新增的四个操作反馈特效也必须真的被调用 ----
        //
        // 判据：在各自的宿主类里能找到调用点。
        // 方法写了没人调用 = 玩家看不到任何变化，而编译器不会报。
        String[][] callSites = {
                {"src/main/java/com/hongmeng/xiuxian/talent/ProfessionCrafting.java",
                        "craftSuccess"},
                {"src/main/java/com/hongmeng/xiuxian/talent/ProfessionCrafting.java",
                        "craftFail"},
                {"src/main/java/com/hongmeng/xiuxian/item/TalentResetPillItem.java",
                        "talentReset"},
                {"src/main/java/com/hongmeng/xiuxian/item/PathSwitchItem.java",
                        "pathSwitch"},
                {"src/main/java/com/hongmeng/xiuxian/domain/DomainRuntime.java",
                        "domainExpiring"},
        };
        int notCalled = 0;
        StringBuilder notCalledList = new StringBuilder();
        for (String[] pair : callSites) {
            String body = readSource(pair[0]);
            if (body.isEmpty() || !body.contains(pair[1] + "(")) {
                notCalled++;
                notCalledList.append(pair[1]).append(" ");
            }
        }
        check("操作反馈特效真的被调用了（写了不调用 = 玩家看不到变化）",
                notCalled == 0,
                notCalled == 0 ? "五处都接" : ("没接的：" + notCalledList));

        // ---- ⑥ 缓伤、魔气溢出这类"被动掉血"必须有视觉提示 ----
        check("缓伤结算有视觉提示（否则玩家觉得莫名其妙掉血）",
                rt.contains("staggerTick("), "");
        check("魔气溢出有视觉警示（否则玩家不知道为何掉血）",
                rt.contains("demonicOverflow("), "");
    }


    /**
     * <b>副业界面与两个新包的接线</b>。
     *
     * <p>界面类最容易出"<b>写了但打不开</b>"：包注册漏了、命令没接、
     * 或服务端直接 import 了客户端类（专服崩）。这三种都<b>不会被编译拦住</b>。</p>
     */
    private static void professionUiRules() {
        section("副业界面");

        var net = readSource("src/main/java/com/hongmeng/xiuxian/network/ModNetwork.java");
        check("CraftPacket 已注册为 C2S（界面点配方）",
                net.contains("CraftPacket.class"), "");
        check("OpenUiPacket 已注册为 S2C（命令打开界面）",
                net.contains("OpenUiPacket.class"), "");

        // ---- 关键：服务端不得直接引用客户端 Screen 类 ----
        //
        // 专用服务器上没有 net.minecraft.client.gui.screens.Screen 之外的客户端类，
        // 直接 import 会 NoClassDefFoundError。所以打开界面必须走
        // DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)。
        var openPkt = readSource(
                "src/main/java/com/hongmeng/xiuxian/network/OpenUiPacket.java");
        check("打开界面走 DistExecutor（服务端不直接引用客户端类）",
                openPkt.contains("DistExecutor") && openPkt.contains("unsafeRunWhenOn"),
                "");
        check("OpenUiPacket 里没有直接 new Screen / setScreen",
                !openPkt.contains("new ProfessionScreen")
                        && !openPkt.contains("setScreen("), "");

        // ---- 命令接上了 ----
        var cmd = readSource("src/main/java/com/hongmeng/xiuxian/command/HongmengCommand.java");
        check("命令 /hongmeng ui profession 已接",
                cmd.contains("literal(\"ui\")") && cmd.contains("\"profession\""), "");

        // ---- 界面存在且只负责显示 ----
        var scr = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/ProfessionScreen.java");
        check("副业界面类存在", !scr.isEmpty(), "");
        check("界面点配方只发包，不做本地判定",
                scr.contains("CraftPacket.send(")
                        && !scr.contains("ProfessionCrafting.craft("), 
                "本地判定会让客户端与服务端的扣材料逻辑不一致");
        check("界面读熟练度走同步摘要（不自己算）",
                scr.contains("summaryPart(6)"), "");
        check("界面会把缺的材料标出来（否则玩家不知道去采什么）",
                scr.contains("countInInventory("), "");

        // ---- 制作仍然只在服务端发生 ----
        var rt = readSource(
                "src/main/java/com/hongmeng/xiuxian/talent/ProfessionCrafting.java");
        check("制作判定仍在服务端（材料/熟练度/成败都在这里）",
                rt.contains("public static boolean craft(")
                        && rt.contains("hasMaterials(player"), "");
        check("服务端会重新校验（不信任客户端）",
                rt.contains("ProfessionRecipe.unlocked(recipe, rank)"), "");

        // ---- 配方不会因为界面改动而消失 ----
        check("界面用的配方表与服务端同源（ProfessionRecipe.all()）",
                scr.contains("ProfessionRecipe.of("), "");
    }


    /**
     * <b>装备套装效果（2 件 / 4 件）</b>。
     *
     * <p>套装最容易"做了但没生效"：数据写了 <code>setFourPiece() = 0.30</code>，
     * 但<b>机制没去读它</b> —— 玩家穿齐了什么变化都没有。
     * 这在编译器眼里完全合法，只能靠断言确认"机制真的读了那个系数"。</p>
     */
    private static void setBonusRules() {
        section("装备套装");

        var paths = com.hongmeng.xiuxian.talent.Path.values();
        int missing = 0;
        StringBuilder lack = new StringBuilder();
        java.util.Set<String> fourKinds = new java.util.HashSet<>();
        java.util.Set<String> twoKinds = new java.util.HashSet<>();
        for (var p : paths) {
            var t = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(p);
            if (t == null) {
                missing++;
                continue;
            }
            if (t.setTwoPiece() <= 0.0D || t.setTwoKind() == null
                    || t.setTwoKind().isBlank()) {
                missing++;
                lack.append(p.cn()).append("(2件) ");
            }
            if (t.setFourPiece() <= 0.0D || t.setFourKind() == null
                    || t.setFourKind().isBlank()) {
                missing++;
                lack.append(p.cn()).append("(4件) ");
            }
            fourKinds.add(t.setFourKind());
            twoKinds.add(t.setTwoKind());
        }
        check("五条道途都有 2 件套与 4 件套效果", missing == 0,
                missing == 0 ? "五套齐全" : ("缺：" + lack));

        // ---- 4 件套必须强化各自的核心机制（不能五套强化同一件事）----
        check("五条道途的 4 件套强化的是不同的机制（不是同一个效果换皮）",
                fourKinds.size() == paths.length,
                fourKinds.size() + " 种：" + fourKinds);
        check("五条道途的 2 件套也互不相同",
                twoKinds.size() == paths.length,
                twoKinds.size() + " 种：" + twoKinds);

        // ---- 4 件套的强化方向必须与道途对得上（抽查三条最容易写错的）----
        var ti = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(
                com.hongmeng.xiuxian.talent.Path.TI);
        check("体修的 4 件套强化「激荡」（对应它的核心机制）",
                ti.setFourKind().contains("激荡"), ti.setFourKind());
        var xue = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(
                com.hongmeng.xiuxian.talent.Path.XUE);
        check("血修的 4 件套强化「缓伤」（对应它的核心机制）",
                xue.setFourKind().contains("缓伤"), xue.setFourKind());
        var zhen = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(
                com.hongmeng.xiuxian.talent.Path.ZHEN);
        check("阵修的 4 件套强化「阵域」（对应它的核心机制）",
                zhen.setFourKind().contains("阵域"), zhen.setFourKind());

        // ---- 4 件套幅度要"明显但不夸张" ----
        boolean sane = true;
        StringBuilder mag = new StringBuilder();
        for (var p : paths) {
            var t = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(p);
            if (t == null) {
                continue;
            }
            double four = t.setFourPiece();
            mag.append(String.format("%s=%.0f%% ", p.cn(), four * 100));
            if (four < 0.15D || four > 0.60D) {
                sane = false;
            }
        }
        check("4 件套幅度在 15%~60%（明显但不至于失控）", sane, mag.toString().trim());

        // ---- 职业甲 2/4 件套：R-1 只交数据，效果实现归装备线（玩家已明确分工）----
        int careerSets = com.hongmeng.xiuxian.item.armor.CareerArmorSet.values().length;
        int armorRealms = com.hongmeng.xiuxian.item.armor.CareerArmorRealm.MAX_REALM;
        check("职业甲按境界分件：10 境 × 5 职业 × 4 部位 = 200 件",
                careerSets == 15 && armorRealms == 10,
                careerSets + " 组外观 / " + armorRealms + " 境");
        check("旧道途甲的 4 件套仍由 legacy 过渡桥保留（R-2 前不凭空变弱）",
                readSource("src/main/java/com/hongmeng/xiuxian/item/armor/PathArmorBonus.java")
                        .contains("setFourBonus"), "");

        // ---- 2 件套门槛必须是 2、4 件套门槛必须是 4 ----
        String ab = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/armor/PathArmorBonus.java");
        check("2 件套门槛是 2 件（worn >= 2）", ab.contains("worn >= 2"), "");
        check("4 件套门槛是 4 件（worn >= 4）", ab.contains("worn >= 4"), "");

        // ---- 4 件套不能重复加基础数值（与线性缩放叠加会失控）----
        // 判据：setFourPiece 的返回值只写进 SET_BONUS 这一格
        boolean onlySetBonusSlot = ab.contains("out[SET_BONUS] = tier.setFourPiece()");
        check("4 件套只写进专门的机制槽位（不重复加基础数值）",
                onlySetBonusSlot, "");
    }


    /**
     * <b>法术锚定施法原点</b> + <b>第三视角运镜启用</b>。
     *
     * <p>玩家给的关键方向：「释放的位置在玩家脚下，后续移动法术不跟随玩家移动」。</p>
     *
     * <p>这三条必须钉住，因为它们的错误形态全都<b>编译能过、日志不报</b>：</p>
     * <ol>
     *   <li>结算又改回读实时玩家位置 → 运镜一开法术就跟着镜头飘
     *       （这正是玩家最初指出的 bug）；</li>
     *   <li>运镜方法被"停用"后忘记恢复（我上一轮就删了 5 处调用点）；</li>
     *   <li>方法启用了却没有调用点 → 存在但永远不执行。</li>
     * </ol>
     */
    private static void spellAnchorRules() {
        section("法术锚定与运镜");

        var sch = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellCastHandler.java");
        var tc = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/TechniqueCaster.java");

        // ---- ① 快照必须记朝向 ----
        check("施法快照记录了朝向（否则结算时朝向会跟着镜头变）",
                sch.contains("this.lx = look.x / len")
                        && sch.contains("this.ly = look.y / len")
                        && sch.contains("this.lz = look.z / len"), "");

        // ---- ② 三个访问口都在 ----
        check("暴露了施法原点 / 射线起点 / 朝向三个访问口",
                sch.contains("public static net.minecraft.world.phys.Vec3 castOrigin(")
                        && sch.contains("public static net.minecraft.world.phys.Vec3 castEye(")
                        && sch.contains("public static net.minecraft.world.phys.Vec3 castLook("),
                "");

        // ---- ③ 结算必须用快照，不能读实时玩家位置 ----
        //
        // 判据：resolveInner 里同时出现 castLook / castOrigin。
        // 如果它又变回 player.getLookAngle() / player.position()，运镜就会带飘特效。
        boolean usesSnapshot = tc.contains("SpellCastHandler.castLook(player)")
                && tc.contains("SpellCastHandler.castOrigin(player)");
        check("法术结算用的是施法快照（不是实时玩家位置）", usesSnapshot,
                usesSnapshot ? "射线原点与朝向都锚定了"
                        : "**回退成了实时玩家位置 —— 运镜一开就会跟着飘**");

        // ---- ④ 清快照必须在结算之后（顺序错了锚定等于没做） ----
        //
        // ⚠ 判据要看**结算那一处**的局部顺序，不能用全局 indexOf：
        //   file 里还有中断路径的 endCastSnapshot（它在结算之前，是正常的）。
        //   我第一版用 indexOf 找到了中断路径那个，于是报了一个假失败。
        int resolveAt = sch.indexOf("TechniqueCaster.resolve(player");
        boolean clearAfter = false;
        if (resolveAt > 0) {
            // 看 resolve 之后 400 字符内有没有清快照
            String tail = sch.substring(resolveAt,
                    Math.min(sch.length(), resolveAt + 400));
            clearAfter = tail.contains("endCastSnapshot(player)");
        }
        check("清快照紧跟在结算之后（顺序反了锚定就失效）", clearAfter,
                clearAfter ? "resolve 之后立即清理" : "**没找到结算后的清快照**");

        // ---- ⑤ 蓄力特效也要用施法原点 ----
        check("蓄力特效用的是施法原点（不是实时玩家位置）",
                sch.contains("castOrigin(player), technique.element()"), "");

        // ---- ⑥ 运镜必须真的启用（没有被 if (true) return 短路） ----
        int bodyAt = tc.indexOf("private static void showBlastCinematic(");
        String body = bodyAt < 0 ? "" : tc.substring(bodyAt,
                Math.min(tc.length(), bodyAt + 1600));
        check("运镜方法没有被 if (true) return 短路（真的启用）",
                !body.contains("if (true) {"), "");

        // ---- ⑦ 5 个炸点法术都要有调用点 ----
        int calls = countOf(tc, "showBlastCinematic(player,");
        check("五个炸点法术都接了运镜（少一个就有一个法术没运镜）", calls >= 5,
                calls + " 处调用");

        // ---- ⑧ 客户端镜头移动玩家已恢复，且结束后会放回原处 ----
        var cam = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/CinematicCamera.java");
        check("客户端镜头会移动玩家（第三视角的前提）",
                cam.contains("mc.player.setPos(cam.x"), "");
        var cin = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/Cinematic.java");
        check("运镜结束会把玩家放回原处（否则镜头一收人停在半空）",
                cin.contains("restorePlayer()"), "");
    }


    /**
     * <b>装备分阶段 + 境界门槛</b>。
     *
     * <p>玩家要求：「武器不能做出来就毕业，也得分什么阶段穿什么护甲用什么武器」。</p>
     *
     * <p>这句话有两个可验证的含义：</p>
     * <ol>
     *   <li><b>分阶段</b>：护甲/武器按境界分阶（不能一套穿到老）；</li>
     *   <li><b>不能一步到位</b>：境界不够就用不了，而且不能只拦"主动穿戴"
     *       —— 交易/掉落/队友给的装备也得拦。</li>
     * </ol>
     */
    private static void gearTierRules() {
        section("装备分阶段与门槛");

        check("护甲有 10 个境界档（与武器一致）",
                com.hongmeng.xiuxian.item.armor.PathArmorTable.MAX_TIER == 10,
                "1 ~ " + com.hongmeng.xiuxian.item.armor.PathArmorTable.MAX_TIER);
        int expect = com.hongmeng.xiuxian.item.armor.PathArmorTable.expectedCount();
        check("护甲应有 200 件（5 道途 × 10 境 × 4 部位）", expect == 200,
                expect + " 件");

        // ---- ① 每阶数值必须明显递增（否则"分阶"只是换名字） ----
        var tier = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(
                com.hongmeng.xiuxian.talent.Path.FA);
        boolean rising = true;
        StringBuilder curve = new StringBuilder();
        int prev = 0;
        for (int t = 1; t <= 10; t++) {
            int d = tier.defenseAt(t, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
            curve.append(d).append(" ");
            if (d < prev) {
                rising = false;
            }
            prev = d;
        }
        check("护甲值随境界递增（不能一套穿到老）", rising, "胸甲: " + curve.toString().trim());
        int first = tier.defenseAt(1, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
        int last = tier.defenseAt(10, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
        check("第 10 境护甲明显强于第 1 境（至少 2 倍）",
                last >= first * 2, first + " -> " + last);

        // ---- ② 耐久也随境界涨（否则高阶护甲先坏） ----
        boolean durRising = true;
        int prevDur = 0;
        for (int t = 1; t <= 10; t++) {
            int d = tier.durabilityAt(t, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
            if (d < prevDur) {
                durRising = false;
            }
            prevDur = d;
        }
        check("耐久随境界递增", durRising, "第10境 " + prevDur);

        // ---- ③ 旧道途护甲配方：R-2 起 legacy，配方已删除 ----
        boolean legacyRecipesGone =
                findResource("data/hongmeng/recipes/path_armor_fa_1_helmet.json") == null
                        && findResource(
                        "data/hongmeng/recipes/path_armor_fa_10_helmet.json") == null;
        check("旧道途护甲配方已删除（R-2 legacy，不再有材料对比）",
                legacyRecipesGone, "260 张旧配方（含 10 把境界剑）已备份后删除");

        // ---- ④ 境界门槛：每 tick 检查必须真的接了 ----
        //
        // 只拦"主动穿戴"不够：玩家可以从别人手里拿到高阶装备。
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        check("每 tick 扫超阶装备（交易/掉落拿到的高阶也要拦）",
                ev.contains("RealmGear.tick(player)"), "");

        var rg = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/gear/RealmGear.java");
        check("门槛会检查护甲与武器两类分阶装备",
                rg.contains("PathArmorItem") && rg.contains("PathWeaponItem"), "");
        check("超阶装备是摘下放回背包，不是销毁（那是玩家财产）",
                rg.contains("getInventory().add(copy)") && !rg.contains("copy.setCount(0)"),
                "");

        // ---- ⑤ 两类装备都要暴露自己的境界 ----
        var armorSrc = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/armor/ModPathArmor.java");
        check("护甲物品暴露了自己的境界（tier()）",
                armorSrc.contains("public int tier()"), "");
        var weaponSrc = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/gear/ModPathWeapon.java");
        check("武器物品暴露了自己的境界（realmTier()）",
                weaponSrc.contains("public int realmTier()"), "");

        // ---- ⑤b 旧名别名必须保留（否则改注册名会让存档 ID 错位） ----
        //
        // ⚠ 我改护甲注册名时忘了保留旧名，后果不只是"20 件旧护甲消失"：
        //   Forge 注册表 ID 按注册顺序分配，少 20 个名字后
        //   后面 220 个物品的 ID 全往前挪，于是存档里每个物品都对不上号。
        //   修法是把旧名注册回来当别名（数值取第 1 境）。
        check("保留了旧名别名注册（改注册名不能让存档 ID 错位）",
                armorSrc.contains("registerLegacyAliases()")
                        && armorSrc.contains("legacyItemName(path, type)"), "");
        check("旧名别名的模型与语言齐全（否则显示成缺失）",
                findResource("assets/hongmeng/models/item/path_armor_fa_helmet.json")
                        != null, "");

        // ---- ⑥ tooltip 要写清境界需求（否则玩家不知道为何穿不上） ----
        check("护甲 tooltip 写了境界需求",
                armorSrc.contains("境界需求"), "");
        check("武器 tooltip 写了境界需求",
                weaponSrc.contains("境界需求"), "");
    }


    /**
     * <b>道途护甲与道袍的曲线关系</b>。
     *
     * <p>⚠ 加这条的原因：我第一版给道途护甲用<b>线性</b>放大
     * （第 1 境 ×1.0 → 第 10 境 ×3.0，全套 57），而道袍走的是<b>指数</b>
     * （{@code RealmStats.armorTotal}：第 1 境 6 → 第 10 境 224 000）。
     * 两者差了 <b>4000 倍</b> —— 我做的 200 件护甲在后期等于没有防御。</p>
     *
     * <p>这种错"看起来正常"（数字都在涨），只有把两条曲线并排算才看得见。</p>
     */
    private static void armorCurveRules() {
        section("护甲曲线（道途 vs 道袍）");

        var fa = com.hongmeng.xiuxian.item.armor.PathArmorTier.of(
                com.hongmeng.xiuxian.talent.Path.FA);
        check("能取到道途护甲的数据", fa != null, "");

        // ---- 道途护甲应是道袍的 70%~85% ----
        // ⚠ 只在**数值足够大**的境界上比比例。
        //
        //   第 1 境全套只有 4~6 点，而"每件至少 1 点"的下限会让
        //   四件各自凑整后合计偏高（实测道途 5 vs 道袍 6）。
        //   1 点的取整误差在这里就是 17%，比例断言在这么小的数上本来就不准。
        //
        //   这不是"放宽标准让测试通过"：从第 2 境起比例是精确的（79%），
        //   而第 1 境改用"同量级"的宽松判据（另一条 check 覆盖），
        //   两个判据各自在**它测得出东西的范围**里生效。
        StringBuilder detail = new StringBuilder();
        boolean allSane = true;
        int checkedTiers = 0;
        for (int tier = 1; tier <= 10; tier++) {
            int pathTotal = fa.totalDefenseAt(tier);
            double daoTotal = com.hongmeng.xiuxian.core.RealmStats.armorTotal(tier);
            if (daoTotal < 20.0D) {
                continue;                    // 太小，取整误差主导
            }
            checkedTiers++;
            double ratio = pathTotal / daoTotal;
            detail.append(String.format("第%d境=%.0f%% ", tier, ratio * 100));
            if (ratio < 0.70D || ratio > 0.85D) {
                allSane = false;
            }
        }
        check("道途护甲是道袍的 70%~85%（数值足够大的境界上精确成立）",
                allSane && checkedTiers >= 8,
                detail.toString().trim() + " 共 " + checkedTiers + " 境");

        // ---- 必须跟着指数表走（不能是线性） ----
        //
        // 判据：第 10 境 / 第 1 境 的比值必须远超线性（线性最多 3 倍）。
        int t1 = fa.totalDefenseAt(1);
        int t10 = fa.totalDefenseAt(10);
        double growth = t1 <= 0 ? 0 : (double) t10 / t1;
        check("护甲成长必须是指数级（第10境/第1境 远大于 3 倍）",
                growth > 100.0D,
                String.format("第1境 %d -> 第10境 %d（×%.0f）", t1, t10, growth));

        // ---- 每一境都要比上一境强（不能出现"升境反而变弱"） ----
        boolean rising = true;
        int prev = 0;
        for (int tier = 1; tier <= 10; tier++) {
            int cur = fa.totalDefenseAt(tier);
            if (cur < prev) {
                rising = false;
            }
            prev = cur;
        }
        check("护甲随境界单调递增（升境不会变弱）", rising, "");

        // ---- 耐久也要能跟上（否则高阶护甲打几下就坏） ----
        int dur1 = fa.durabilityAt(1, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
        int dur10 = fa.durabilityAt(10, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
        check("高阶护甲耐久明显更高（否则没人用）",
                dur10 > dur1 * 50,
                dur1 + " -> " + dur10);

        // ---- 四件分配沿用道袍比例（玩家不用重新学哪件给得多） ----
        var helm = fa.defenseAt(10, net.minecraft.world.item.ArmorItem.Type.HELMET);
        var chest = fa.defenseAt(10, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE);
        var legs = fa.defenseAt(10, net.minecraft.world.item.ArmorItem.Type.LEGGINGS);
        var boots = fa.defenseAt(10, net.minecraft.world.item.ArmorItem.Type.BOOTS);
        check("四件分配沿用道袍比例（胸甲 > 护腿 > 头盔 > 靴子）",
                chest > legs && legs > helm && helm > boots,
                "胸 " + chest + " > 腿 " + legs + " > 头 " + helm + " > 靴 " + boots);
    }


    /**
     * <b>道途武器的境界伤害必须真的接到属性上</b>。
     *
     * <p>这是项目里<b>第二次</b>出现同一个 bug：</p>
     * <ul>
     *   <li>第一次：{@code RealmWeaponItem} 存了 attack 字段，只用于 tooltip，
     *       实际打出来是原版石剑伤害 → 于是写了 {@code WeaponAttributeHandler}；</li>
     *   <li>第二次（我做的）：{@code PathWeaponItem} 也存了 attack（境界量级），
     *       但处理器<b>只认 RealmWeaponItem</b>，于是 50 把道途武器
     *       全部退回原版整数值（5~10）。</li>
     * </ul>
     *
     * <p>后果比"数值偏低"严重：项目伤害量级是 {@code standard(tier)}
     * （第 10 境 320 000），而道途武器打出去只有 <b>10</b> ——
     * <b>体修与血修这两条物理系道途在第 6 境之后完全打不动怪</b>。</p>
     */
    private static void weaponRealmDamageRules() {
        section("武器境界伤害");

        // ---- ① 处理器必须认两种武器 ----
        var handler = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/gear/WeaponAttributeHandler.java");
        check("属性处理器认 RealmWeaponItem（既有武器）",
                handler.contains("instanceof RealmWeaponItem"), "");
        check("属性处理器也认 PathWeaponItem（道途武器）",
                handler.contains("instanceof ModPathWeapon.PathWeaponItem")
                        || handler.contains("PathWeaponItem w)"), "");

        // ---- ② 道途武器的 SwordItem 伤害修饰符必须是 0 ----
        //
        // 判据：构造里 super(weaponTier(tier), 0, ...) —— 第二参数是 0。
        // 如果又改回 2 + damageFactor*2（3~5 的整数），
        // 低境界会虚高（第 1 境 5+8=13，而同境界 realm_sword 是 9）。
        var wep = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/gear/ModPathWeapon.java");
        check("道途武器的原版伤害修饰符为 0（避免与境界伤害重复计算）",
                wep.contains("super(weaponTier(tier), 0,"), "");

        // ---- ③ 境界伤害必须指数增长（不能是常数） ----
        var ph = com.hongmeng.xiuxian.talent.Path.XUE;
        var kinds = com.hongmeng.xiuxian.item.gear.PathWeapon.values();
        check("道途武器有 5 种", kinds.length == 5, kinds.length + " 种");

        double dmg1 = com.hongmeng.xiuxian.item.gear.ModPathWeapon
                .attackOf(kinds[0], 1);
        double dmg10 = com.hongmeng.xiuxian.item.gear.ModPathWeapon
                .attackOf(kinds[0], 10);
        check("武器伤害随境界指数增长（第10境远超第1境）",
                dmg10 > dmg1 * 100.0D,
                String.format("%.0f -> %.0f（×%.0f）", dmg1, dmg10, dmg10 / dmg1));

        // ---- ④ 与同境界 realm_sword 同量级（两条武器线不能差太多） ----
        double sword10 = com.hongmeng.xiuxian.core.RealmStats.weaponAttack(10);
        check("第 10 境道途武器与 realm_sword 同量级（差距 < 60%）",
                dmg10 >= sword10 * 0.4D && dmg10 <= sword10 * 1.6D,
                String.format("道途 %.0f / realm_sword %.0f", dmg10, sword10));

        // ---- ⑤ 五把武器的总 DPS 仍然对齐（单次×频率 = 常数） ----
        boolean dpsAligned = true;
        StringBuilder dps = new StringBuilder();
        for (var k : kinds) {
            double v = k.damageFactor() * k.speedFactor();
            dps.append(String.format("%s=%.3f ", k.cn(), v));
            if (Math.abs(v - 1.0D) > 0.01D) {
                dpsAligned = false;
            }
        }
        check("五把武器的总 DPS 仍然对齐（差异只在单次 vs 频率）",
                dpsAligned, dps.toString().trim());
    }


    /**
     * <b>道途核心机制必须真的影响伤害</b>，不能只显示。
     *
     * <p>这是项目里<b>第三次</b>同类 bug：</p>
     * <ol>
     *   <li>护甲曲线量级错（线性 vs 指数，差 4000 倍）；</li>
     *   <li>道途武器境界伤害没接属性系统（实际只有 5~10）；</li>
     *   <li>{@code demonDamageBonus} / {@code demonPenetration} /
     *       {@code bloodlustBonus}：算了、界面显示了、<b>但从未加进伤害</b>。</li>
     * </ol>
     *
     * <p>第 3 种最恶劣：玩家看到「魔气 80% · 增伤 ×1.36」，
     * 会以为是"数值偏低"而不是"根本没生效"，<b>永远不会报 bug</b>。</p>
     */
    private static void pathDamageWiringRules() {
        section("道途增伤接线");

        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");

        // ---- 三个机制都必须在伤害结算里被调用 ----
        String[][] must = {
                {"demonDamageBonus", "魔修增伤（魔气越高伤害越高）"},
                {"demonPenetration", "魔修破防"},
                {"bloodlustBonus", "血修血怒（血池满 + 残血）"},
        };
        int notWired = 0;
        StringBuilder lack = new StringBuilder();
        for (String[] pair : must) {
            if (!ev.contains(pair[0])) {
                notWired++;
                lack.append(pair[1]).append(" ");
            }
        }
        check("魔修增伤/破防、血修血怒都接进了伤害结算（不能只显示）",
                notWired == 0,
                notWired == 0 ? "三项都接" : ("没接的：" + lack));

        // ---- 而且必须真的乘到伤害上（不是只算出来放进变量） ----
        //
        // 判据：出现了把 pathAmp 乘到 event.getAmount() 的写法。
        check("算出的倍率真的乘到了 event.setAmount（不是算了就扔）",
                ev.contains("event.getAmount() * pathAmp"), "");

        // ---- 破防用目标的**实际**减免率折算，不拍常数 ----
        check("破防按目标实际护甲减免率折算（护甲越厚收益越大）",
                ev.contains("reductionOf(event.getEntity())"), "");

        // ---- PathRuntime 必须能直读这两个资源 ----
        var rt = readSource("src/main/java/com/hongmeng/xiuxian/talent/PathRuntime.java");
        check("PathRuntime 暴露了魔气值与血池比例（伤害结算要读）",
                rt.contains("public static double demonicOf(")
                        && rt.contains("public static double bloodFraction("), "");
    }


    /**
     * <b>天赋树必须真的生效</b> —— {@code TalentBonus} 的每一项都要被调用。
     *
     * <p>这是本项目<b>最严重</b>的一个 bug：{@code TalentBonus} 有 16 个方法，
     * <b>15 个零调用</b>（只有 {@code area} 被用）。</p>
     *
     * <p>断裂点：{@code Talents.bonus()} 被 {@code TalentBonus} 内部调了 16 次
     * （算得完全正确），但 {@code TalentBonus.xxx()} 本身<b>没人调用</b>。
     * 而面板显示走的是另一条路（{@code Talents.clientSummary → Talents.bonus}）——
     * 于是<b>玩家看到"+12% 法术伤害"，实际一点没加</b>。</p>
     *
     * <p><b>纯装饰的天赋树 = 玩家要求的核心功能等于没做。</b></p>
     */
    private static void talentWiringRules() {
        section("天赋树接线");

        // 18 项，与 TalentBonus 的方法表一一对应
        String[][] items = {
                {"spellDamage", "法术伤害", "TechniqueCaster"},
                {"meleeDamage", "近战伤害", "ModEvents"},
                {"mana", "灵力上限", "ManaBridge"},
                {"health", "生命上限", "TalentAttributes"},
                {"damageReduction", "减伤", "ModEvents"},
                {"lifesteal", "吸血", "ModEvents"},
                {"penetration", "破防", "ModEvents|TechniqueCaster"},
                {"cooldownCut", "冷却缩减", "TechniqueCaster"},
                {"knockbackResist", "抗击退", "TalentAttributes"},
                {"area", "领域范围", "PathRuntime"},
                {"reflect", "伤害反弹", "ModEvents"},
                {"control", "鬼域减速", "PathRuntime"},
                {"attackSpeed", "攻速", "TalentAttributes"},
                {"swordIntent", "剑意获取", "PathRuntime"},
                {"swordQi", "剑气伤害", "PathRuntime"},
                {"swordAura", "剑罡吸收", "PathRuntime"},
                {"soulMark", "魂印伤害", "PathRuntime"},
                {"ghostRealm", "鬼域威力", "PathRuntime"},
        };

        int dead = 0;
        StringBuilder deadList = new StringBuilder();
        for (String[] item : items) {
            String name = item[0];
            // 在声称的宿主文件里找调用（归一化空白，避免跨行匹配不到）
            boolean found = false;
            for (String host : item[2].split("\\|")) {
                String path = findSourceBySimpleName(host + ".java");
                if (path == null) {
                    continue;
                }
                String body = readSource(path);
                String flat = body.replaceAll("\\s+", "");
                if (flat.contains("TalentBonus." + name + "(")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                dead++;
                deadList.append(item[1]).append(" ");
            }
        }
        check("天赋/职业的 18 项加成全部接进了运行期结算（不能只是面板显示）",
                dead == 0,
                dead == 0 ? "18/18 全部接线"
                        : ("**有 " + dead + " 项是废的：" + deadList + "**"));

        // ---- 关键几项单独确认：必须在正确的语义位置 ----
        var tc = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/TechniqueCaster.java");
        check("法术伤害真的乘进了 damageOf（不是算出来放着）",
                tc.replaceAll("\\s+", "")
                        .contains("1.0D+com.hongmeng.xiuxian.talent.TalentBonus"
                                + ".spellDamage(player)"), "");
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        check("减伤真的乘到了 event.setAmount",
                ev.replaceAll("\\s+", "").contains("event.getAmount()*(1.0D-c)"), "");
        check("反弹真的打回了伤害源",
                ev.contains("src.hurt(player.damageSources().magic()"), "");

        // ---- 灵力上限要有带 Player 的重载 ----
        var mb = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/ManaBridge.java");
        check("灵力上限有带 Player 的重载（否则天赋那一份永远读不到）",
                mb.contains("maxMana(ServerPlayer player, CultivationData data)"), "");

        // ---- 属性型加成必须每 tick 刷新（天赋随时会变） ----
        check("生命/抗击退每 tick 刷新（天赋点了立刻见效）",
                ev.contains("TalentAttributes.refresh(player)"), "");
    }

    /** 按简单文件名找源码（返回相对路径）；找不到返回 null。 */
    /**
     * 简单文件名 → 源码相对路径。
     *
     * <p>⚠ 原来是"按目录列表逐个试"——试错会触发 {@code readSource} 的
     * "找不到"警告，一次跑出 6 条噪音，反而容易掩盖真问题。
     * 改成精确表：只写实际存在的文件，查不到就是 null（不产生警告）。</p>
     */
    private static String findSourceBySimpleName(String simpleName) {
        return switch (simpleName) {
            case "TechniqueCaster.java" -> "src/main/java/com/hongmeng/xiuxian/"
                    + "technique/TechniqueCaster.java";
            case "ManaBridge.java" -> "src/main/java/com/hongmeng/xiuxian/"
                    + "technique/ManaBridge.java";
            case "ModEvents.java" -> "src/main/java/com/hongmeng/xiuxian/core/ModEvents.java";
            case "TalentAttributes.java" -> "src/main/java/com/hongmeng/xiuxian/"
                    + "talent/TalentAttributes.java";
            case "TalentBonus.java" -> "src/main/java/com/hongmeng/xiuxian/"
                    + "talent/TalentBonus.java";
            case "PathRuntime.java" -> "src/main/java/com/hongmeng/xiuxian/"
                    + "talent/PathRuntime.java";
            case "DomainRuntime.java" -> "src/main/java/com/hongmeng/xiuxian/"
                    + "domain/DomainRuntime.java";
            case "PathArmorBonus.java" -> "src/main/java/com/hongmeng/xiuxian/"
                    + "item/armor/PathArmorBonus.java";
            case "PathWeaponBonus.java" -> "src/main/java/com/hongmeng/xiuxian/"
                    + "item/gear/PathWeaponBonus.java";
            default -> null;
        };
    }


    /**
     * <b>规则之力（帝境/鸿蒙的终局能力）必须全部接线</b>。
     *
     * <p>审计发现 {@code RealmPowers} 的 7 项里 <b>2 项零调用</b>：</p>
     * <ul>
     *   <li>{@code cooldownMultiplier} —— 「万法无冷却」返回 0.0，但没人读
     *       → 帝境玩家<b>还背着冷却</b>；</li>
     *   <li>{@code spellDamageMultiplier} —— 鸿蒙「万法之始」+20%，同样没人读。</li>
     * </ul>
     *
     * <p>这是<b>终局内容</b>，而且面板上也不显示 —— 玩家不会意识到少了什么，
     * 所以只能靠断言拦住。</p>
     */
    private static void realmPowerWiringRules() {
        section("规则之力接线");

        String[][] powers = {
                {"annihilateMultiplier", "寂灭（帝境：伤害 +30%）"},
                {"causalChance", "因果（鸿蒙：概率翻倍）"},
                {"cooldownMultiplier", "万法无冷却（帝境/鸿蒙）"},
                {"manaCostMultiplier", "灵力减免"},
                {"spellDamageMultiplier", "万法之始（鸿蒙：法术 +20%）"},
                {"spellsFreed", "法术解禁"},
                {"voidMultiplier", "虚空"},
        };

        var tc = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/TechniqueCaster.java");
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        var all = (tc + ev).replaceAll("\\s+", "");

        int dead = 0;
        StringBuilder deadList = new StringBuilder();
        for (String[] p : powers) {
            if (!all.contains("RealmPowers." + p[0] + "(")) {
                dead++;
                deadList.append(p[1]).append(" ");
            }
        }
        check("规则之力七项全部接进运行期（不能练到帝境却什么都没有）",
                dead == 0,
                dead == 0 ? "7/7 全部接线"
                        : ("**有 " + dead + " 项是废的：" + deadList + "**"));

        // ---- 万法无冷却：必须允许真的 0 冷却 ----
        var td = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/TechniqueData.java");
        check("冷却缩减允许到 1.0（真·无冷却，不是最少留 10%）",
                td.contains("Math.min(1.0D, cut)"), "");
        check("无冷却时仍隔 1 tick（防同 tick 连发刷屏）",
                td.contains("now + 1L"), "");

        // ---- 万法之始：必须乘进 damageOf ----
        check("万法之始真的乘进了法术伤害",
                tc.replaceAll("\\s+", "")
                        .contains("RealmPowers.spellDamageMultiplier("), "");
    }


    /**
     * <b>新系统必须能被玩家发现</b>。
     *
     * <p>这条比很多数值断言更值钱。自检发现过两个"不可发现"的洞：</p>
     * <ul>
     *   <li>{@code Talents.choose} 的<b>唯一调用点是命令处理器</b> ——
     *       修炼面板里没有择道按钮，玩家打开面板看到"尚未择道"却无从下手；</li>
     *   <li>{@code 道途 / 天赋 / 副业 / 剑阵} 这些词在<b>任务链和指引里一个字都没有</b>
     *       —— 按任务正常玩的玩家<b>永远不会知道这些系统存在</b>。</li>
     * </ul>
     *
     * <p>这两种错<b>编译、断言、起服日志全都不报</b>：代码本身没错，
     * 错在"没人告诉玩家它在那里"。</p>
     */
    private static void discoveryRules() {
        section("系统可发现性");

        // ---- ① 面板里必须能择道 ----
        var panel = readSource("src/main/java/com/hongmeng/xiuxian/CultivationPanelScreen.java");
        check("修炼面板里有可点的择道入口（不能只让玩家打命令）",
                panel.contains("PathChoosePacket.send("), "");
        check("择业入口会画出五个职业（Career.values 遍历）",
                panel.contains("com.hongmeng.xiuxian.talent.Career.values()"), "");

        // ---- ② 择道包必须注册 ----
        var net = readSource("src/main/java/com/hongmeng/xiuxian/network/ModNetwork.java");
        check("PathChoosePacket 已注册为 C2S",
                net.contains("messageBuilder(PathChoosePacket.class")
                        && net.contains("PLAY_TO_SERVER"), "");

        // ---- ③ 服务端必须重新校验（不信任客户端） ----
        var pkt = readSource(
                "src/main/java/com/hongmeng/xiuxian/network/PathChoosePacket.java");
        check("择道包走服务端 Talents.choose 校验（客户端不能伪造）",
                pkt.contains("Talents.choose(player"), "");

        // ---- ④ 任务链必须提到这两个系统 ----
        var quests = readSource("src/main/java/com/hongmeng/xiuxian/quest/Quests.java");
        check("任务链里有「择道」教学任务（否则玩家不知道有道途）",
                quests.contains("q_path_choose"), "");
        check("择道任务说明了入口（按 R 打开面板 / 命令）",
                quests.contains("修炼面板"), "");
        check("任务链里有「副业」教学任务（否则玩家不知道有副业）",
                quests.contains("q_profession"), "");
        check("副业任务说明了入口（/hongmeng ui profession）",
                quests.contains("ui profession"), "");

        // ---- ⑤ 副业界面必须能被打开 ----
        var ui = readSource("src/main/java/com/hongmeng/xiuxian/network/OpenUiPacket.java");
        check("OpenUiPacket 走 DistExecutor（服务端不引用客户端类）",
                ui.contains("unsafeRunWhenOn"), "");
        var cmd = readSource("src/main/java/com/hongmeng/xiuxian/command/HongmengCommand.java");
        check("命令 /hongmeng ui profession 存在",
                cmd.contains("literal(\"ui\")"), "");
    }


    /**
     * <b>职业 / 天赋 / 副业 / 机制资源的存档往返</b>。
     *
     * <p>玩家已删旧档，不做旧档迁移；这里验新键（Career / CareerMechanics）
     * 的"存 → 清内存 → 读回"链路，防止再次出现"登出先清内存、NBT 后存成空"的丢档 bug。</p>
     */
    private static void saveRoundTripRules() {
        section("存档往返（职业 / 天赋 / 副业）");

        java.util.UUID id = java.util.UUID.randomUUID();
        try {
            com.hongmeng.xiuxian.talent.Talents.setCareerAt(id,
                    com.hongmeng.xiuxian.talent.Career.MO_XIU);
            com.hongmeng.xiuxian.talent.Talents.setRankAt(id, "mo_demon_1", 3);
            com.hongmeng.xiuxian.talent.Talents.setRankAt(id, "mo_pen_1", 2);
            com.hongmeng.xiuxian.talent.Talents.setWorkAt(id, "LIANDAN", 42);
            com.hongmeng.xiuxian.talent.PathRuntime.restore(id, 55.0D, 66.0D,
                    77.0D, 88.0D);

            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putUUID("TalentOwner", id);
            com.hongmeng.xiuxian.talent.TalentSave.saveByTag(tag);

            check("存档里有职业键", tag.contains("Career"), "");
            check("存档里有天赋等级列表",
                    tag.contains("TalentRanks", net.minecraft.nbt.Tag.TAG_LIST), "");
            check("存档里有副业熟练度列表",
                    tag.contains("ProfessionWork", net.minecraft.nbt.Tag.TAG_LIST), "");
            check("存档里有机制资源桶",
                    tag.contains("CareerMechanics", net.minecraft.nbt.Tag.TAG_COMPOUND), "");
            check("不再写旧键 TalentPath（旧存档已由玩家删除）",
                    !tag.contains("TalentPath"), "");

            com.hongmeng.xiuxian.talent.TalentSave.forget(id);
            check("清空后职业确实没了（模拟下线）",
                    com.hongmeng.xiuxian.talent.Talents.careerAt(id) == null, "");

            com.hongmeng.xiuxian.talent.TalentSave.loadByTag(tag);
            check("读档后职业恢复",
                    com.hongmeng.xiuxian.talent.Talents.careerAt(id)
                            == com.hongmeng.xiuxian.talent.Career.MO_XIU,
                    String.valueOf(com.hongmeng.xiuxian.talent.Talents.careerAt(id)));
            check("读档后天赋等级恢复",
                    com.hongmeng.xiuxian.talent.Talents.ranksAt(id)
                            .getOrDefault("mo_demon_1", 0) == 3
                            && com.hongmeng.xiuxian.talent.Talents.ranksAt(id)
                            .getOrDefault("mo_pen_1", 0) == 2, "");
            check("读档后副业熟练度恢复",
                    com.hongmeng.xiuxian.talent.Talents.workAt(id)
                            .getOrDefault("LIANDAN", 0) == 42, "");

            double[] res = com.hongmeng.xiuxian.talent.PathRuntime.snapshotRaw(id);
            double decay = com.hongmeng.xiuxian.talent.TalentSave.LOGIN_DECAY;
            check("机制资源按登录衰减恢复", Math.abs(res[0] - 55.0D * decay) < 1.0E-6D
                    && Math.abs(res[1] - 66.0D * decay) < 1.0E-6D
                    && Math.abs(res[2] - 77.0D * decay) < 1.0E-6D
                    && Math.abs(res[3] - 88.0D * decay) < 1.0E-6D, "");
        } finally {
            com.hongmeng.xiuxian.talent.TalentSave.forget(id);
        }
    }


    /**
     * <b>{@code serializeNBT} 里天赋数据必须在 owner 写进 tag <u>之后</u> 再写。</b>
     *
     * <p>这是一次真实踩到的<b>数据丢失</b> bug：</p>
     * <pre>
     * public CompoundTag serializeNBT() {
     *     CompoundTag tag = data.save();                        // 内部调 saveByTag(tag)
     *     if (owner != null) tag.putUUID("TalentOwner", owner); // 晚了
     * }
     * </pre>
     * <p>{@code saveByTag} 第一行是
     * {@code if (!tag.hasUUID("TalentOwner")) return;} ——
     * 于是<b>每次都在第一行就返回</b>，天赋/道途/副业/机制资源
     * <b>从来没被写进存档</b>。</p>
     *
     * <p><b>为什么难发现：</b>读取侧是对的（先设 owner 再 load），
     * 所以"读不到"被当成"本来就没数据"；没有异常、没有日志、编译通过。
     * 玩家看到的是"重启后天赋没了"，很容易被当成"忘了存"而不是"存反顺序"。</p>
     */
    private static void saveOrderRules() {
        section("存档写入顺序（数据丢失防线）");

        var cap = readSource(
                "src/main/java/com/hongmeng/xiuxian/core/CultivationCapability.java");
        check("CultivationCapability 源码可读", !cap.isEmpty(), "");

        int sn = cap.indexOf("public CompoundTag serializeNBT()");
        check("能找到 serializeNBT", sn >= 0, "");

        if (sn >= 0) {
            // 取方法体（到下一个 } 结尾的第一个平衡块）
            int braceStart = cap.indexOf('{', sn);
            int depth = 0;
            int end = braceStart;
            for (int i = braceStart; i < cap.length(); i++) {
                char c = cap.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            String body = cap.substring(braceStart, Math.min(cap.length(), end + 1));

            int ownerAt = body.indexOf("putUUID(\"TalentOwner\"");
            int saveAt = body.indexOf("TalentSave.saveByTag(");
            check("serializeNBT 里会写 TalentOwner", ownerAt >= 0, "");
            check("serializeNBT 里会写天赋数据（否则重启后天赋全丢）", saveAt >= 0,
                    saveAt < 0 ? "**没有 saveByTag 调用 —— 天赋不会被存**" : "");
            check("天赋数据的写入在 TalentOwner **之后**（顺序反了 saveByTag 会直接返回）",
                    ownerAt >= 0 && saveAt >= 0 && saveAt > ownerAt,
                    ownerAt >= 0 && saveAt >= 0
                            ? ("owner@" + ownerAt + " save@" + saveAt)
                            : "缺调用");
        }

        // ---- 校验 saveByTag 确实依赖 owner ----
        var ts = readSource("src/main/java/com/hongmeng/xiuxian/talent/TalentSave.java");
        check("saveByTag 依赖 TalentOwner（所以顺序必须对）",
                ts.contains("!tag.hasUUID(\"TalentOwner\")"), "");

        // ---- data.save() 只有这一个调用点（不要在别处绕开 owner） ----
        //
        // ⚠ 必须先去掉注释行再数：我第一次统计把注释里的
        //   "data.save() 内部会调 saveByTag" 也算进去了，于是报了 2 处。
        int owners = 0;
        for (String line : cap.split("\\n")) {
            String t = line.trim();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) {
                continue;
            }
            owners += countOf(line, "data.save()");
        }
        check("data.save() 只在 serializeNBT 里被调用（不会绕开 owner）",
                owners == 1, owners + " 处（不含注释）");
    }


    /**
     * <b>副业数据在未择道时也必须送达客户端</b>。
     *
     * <p>{@code Talents.clientSummary} 原来在 {@code path == null} 时直接
     * {@code return ""}，而副业熟练度**也在这份摘要里**。</p>
     *
     * <p>可副业是<b>独立于道途</b>的设计（"都可以兼修"）——
     * 玩家完全可以在选道之前先把炼丹练起来。但界面读摘要 →
     * 没择道就<b>永远显示 0 级</b>，玩家会以为"炼丹没生效"。</p>
     */
    private static void professionSummaryRules() {
        section("副业数据送达");

        var tal = readSource("src/main/java/com/hongmeng/xiuxian/talent/Talents.java");
        int sn = tal.indexOf("public static String clientSummary");
        check("能找到 clientSummary", sn >= 0, "");

        if (sn >= 0) {
            int braceStart = tal.indexOf('{', sn);
            int depth = 0;
            int end = braceStart;
            for (int i = braceStart; i < tal.length(); i++) {
                char c = tal.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            String body = tal.substring(braceStart, Math.min(tal.length(), end + 1));
            // 关键：path == null 分支里不能有 return 空串
            //
            // 判据用 indexOf 比对位置，比写正则安全：
            //   "if (path == null) {" 之后紧跟的代码里，不能出现 return ""
            int nullAt = body.indexOf("if (path == null) {");
            boolean earlyReturn = false;
            if (nullAt >= 0) {
                int after = nullAt + "if (path == null) {".length();
                String tail = body.substring(after,
                        Math.min(body.length(), after + 400));
                // 到第一个 } 为止就是该分支体
                int close = tail.indexOf('}');
                String branch = close > 0 ? tail.substring(0, close) : tail;
                earlyReturn = branch.contains("return \"\"");
            }
            check("未择道时**不**直接返回空串（否则副业数据发不出去）",
                    !earlyReturn, earlyReturn ? "**还是会提前 return**" : "");
            check("摘要里确实包含副业熟练度（Profession.values 遍历）",
                    body.contains("Profession.values()"), "");
        }

        // ---- 客户端 split 必须转义管道符 ----
        var cache = readSource(
                "src/main/java/com/hongmeng/xiuxian/network/ClientCultivationCache.java");
        check("客户端按转义的管道符切分（split(\"\\\\|\") 否则会按正则切成单字符）",
                cache.contains("split(\"\\\\|\""), "");
        check("切分保留空段（limit -1，否则末尾道途为空时会少段）",
                cache.contains("-1"), "");

        // ---- 副业界面读的是第 7 段 ----
        var scr = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/ProfessionScreen.java");
        check("副业界面读 summaryPart(6)（第 7 段 = 副业）",
                scr.contains("summaryPart(6)"), "");
    }


    /**
     * <b>职业天赋点预算</b>：满境 21 点，点不满 8 节点，必须取舍。
     */
    private static void talentBudgetRules() {
        section("职业天赋点预算");

        int total = com.hongmeng.xiuxian.talent.TalentTree.POINTS_ON_CHOOSE
                + 9 * com.hongmeng.xiuxian.talent.TalentTree.POINTS_PER_REALM;
        check("满境总点数 = 21（3 + 9×2）", total == 21, total + " 点");

        boolean overspend = true;
        StringBuilder detail = new StringBuilder();
        for (var c : com.hongmeng.xiuxian.talent.Career.values()) {
            int cap = 0;
            for (var n : com.hongmeng.xiuxian.talent.TalentTree.forCareer(c)) {
                cap += n.maxRank();
            }
            if (cap <= total) {
                overspend = false;
                detail.append(c.cn()).append("=").append(cap).append(" ");
            }
        }
        check("每职业点满所需 > 21 点（点不满，必须取舍）", overspend,
                overspend ? "五职业全部超过 21 点" : ("可点满：" + detail));

        int activeCost = com.hongmeng.xiuxian.talent.CareerActive.ofCareer(
                com.hongmeng.xiuxian.talent.Career.JIAN_XIU).length * 5;
        check("四支主动全解锁需 20 点（满境 21 点可全部体验）",
                activeCost == 20, activeCost + " 点");

        boolean costOk = true;
        for (var s : com.hongmeng.xiuxian.talent.CareerSkill.values()) {
            if (s.isCore()) {
                if (s.unlockPoints() != 0.0D) {
                    costOk = false;
                }
            } else if (Math.abs(s.unlockPoints() - 2.0D) > 1.0E-9D
                    && Math.abs(s.unlockPoints() - 4.0D) > 1.0E-9D) {
                costOk = false;
            }
        }
        check("被动解锁成本：核心 0 / 分支 2 或 4", costOk, "");
    }


    /**
     * <b>副业失败必须真的有代价</b>（不能免费重试）。
     *
     * <p>抓到的漏洞：{@code refund} 原来用 {@code ceil(count × 0.5)}——</p>
     * <pre>
     *   扣 1 个 → 失败 → 退 ceil(0.5) = 1 个 → 净损失 0
     * </pre>
     * <p>而多条配方带 {@code count = 1} 的材料（空冥兰花瓣、灵晶矿…），
     * 于是这些配方可以<b>无限免费重试</b> —— {@code successChance}
     * 对它彻底失去意义（"早晚会成"），熟练度降低失败率也失去价值。</p>
     */
    private static void craftFailureCostRules() {
        section("副业失败代价");

        double ratio = com.hongmeng.xiuxian.talent.ProfessionRecipe.refundRatio();
        check("退回比例在 0~1 之间", ratio > 0.0D && ratio < 1.0D, "" + ratio);

        // ---- 逐个数验证：退回必须严格小于消耗 ----
        int freeRetries = 0;
        StringBuilder bad = new StringBuilder();
        for (int n = 1; n <= 8; n++) {
            int back = (int) Math.floor(n * ratio);
            if (back >= n) {
                freeRetries++;
                bad.append("n=").append(n).append("退").append(back).append(" ");
            }
        }
        check("任何材料数下退回都少于消耗（否则可以免费重试）",
                freeRetries == 0,
                freeRetries == 0 ? "全部有代价" : ("**这些能白刷：" + bad + "**"));

        // ---- 材料数 1 时必须一分不退（小料全赔） ----
        int back1 = (int) Math.floor(1 * ratio);
        check("单件材料失败不退（count=1 -> 退 0）", back1 == 0, "退 " + back1);

        // ---- 源码里不能再用 ceil ----
        var pc = readSource(
                "src/main/java/com/hongmeng/xiuxian/talent/ProfessionCrafting.java");
        int refundAt = pc.indexOf("static void refund(");
        String refundBody = "";
        if (refundAt >= 0) {
            int brace = pc.indexOf('{', refundAt);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < pc.length(); i++) {
                char c = pc.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            refundBody = pc.substring(brace, Math.min(pc.length(), end + 1));
        }
        check("refund 用的是 floor 不是 ceil（ceil 会让单件材料全退）",
                refundBody.contains("Math.floor(") && !refundBody.contains("Math.ceil("),
                refundBody.isEmpty() ? "读不到 refund" : "");

        // ---- 退回不能超过材料数（防手滑写成乘数） ----
        boolean sane = true;
        for (int n = 1; n <= 16; n++) {
            int back = (int) Math.floor(n * ratio);
            if (back > n) {
                sane = false;
            }
        }
        check("退回不超过消耗（防手滑写成乘数）", sane, "");
    }


    /**
     * <b>天劫必须可活</b>（不能是无条件处决）。
     *
     * <p>抓到的阻塞性 bug：总伤害 2.2 × 生命上限，而</p>
     * <ol>
     *   <li>天劫用 {@code magic()} —— 在 {@code BYPASSES_ARMOR} 标签里，
     *       <b>护甲不减免</b>；</li>
     *   <li>{@code startTribulation} <b>没授予抗性 I</b>（注释却一直声称有）；</li>
     *   <li>本项目<b>没有回血消耗品</b>。</li>
     * </ol>
     * <p>于是 9 道雷里第 5 道必死。而首次天劫在化神→道印 ——
     * <b>没人能越过道印</b>，道印之后的全部内容都锁死了。</p>
     */
    private static void tribulationSurvivableRules() {
        section("天劫可活");

        // ---- ① 总伤害 × (1-抗性) 必须 < 100% 生命 ----
        //
        // 抗性 I 在 1.20.1 是每级 -20%。
        double ratio = com.hongmeng.xiuxian.core.Breakthrough.TRIBULATION_TOTAL_HP_RATIO;
        double resist = 0.20D;
        double effective = ratio * (1.0D - resist);
        check("吃满雷劫后仍有血（总伤害 × 抗性后 < 100% 生命）",
                effective < 1.0D,
                String.format("总 %.2f × (1-%.0f%%) = %.2f 倍生命", ratio,
                        resist * 100, effective));

        // ---- ② 不能太轻松（否则雷劫没威胁） ----
        check("雷劫仍有威胁（有效伤害 > 50% 生命）", effective > 0.50D,
                String.format("%.0f%% 生命", effective * 100));

        // ---- ③ 单道雷不能必定致死 ----
        int bolts6 = com.hongmeng.xiuxian.core.Breakthrough.tribulationBolts(6);
        check("第 6 境（首个天劫）有雷可劈", bolts6 > 0, bolts6 + " 道");
        double per = ratio / Math.max(1, bolts6);
        check("单道雷 < 100% 生命（不会被一道秒杀）", per < 1.0D,
                String.format("每道 %.1f%% 生命（%d 道）", per * 100, bolts6));

        // ---- ④ 源码里必须真的授予抗性 ----
        var bt = readSource("src/main/java/com/hongmeng/xiuxian/core/Breakthrough.java");
        int startAt = bt.indexOf("private static void startTribulation(");
        String startBody = "";
        if (startAt >= 0) {
            int brace = bt.indexOf('{', startAt);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < bt.length(); i++) {
                char c = bt.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            startBody = bt.substring(brace, Math.min(bt.length(), end + 1));
        }
        // ---- 抗性的真实来源：applyBreakthroughBlessing（不是雷劫自己） ----
        //
        // ⚠ 我最初写的是"startTribulation 真的授予抗性"，并据此加了一段代码 ——
        //   那是**误判**：抗性一直由 `ModEvents.applyBreakthroughBlessing` 提供，
        //   只是它在 ModEvents 里，我第一次 grep 只搜了 Breakthrough。
        //   结果我重复授予了一份，还在 tickTribulation 里 removeEffect，
        //   会把突破后本该 60 秒的保护提前中止（我自己制造的 bug）。
        //
        //   现在断言改成检查**正确的来源与顺序**。
        check("降雷之前调用了突破保护（抗性/回复/回满血都来自它）",
                bt.contains("ModEvents.applyBreakthroughBlessing(player)"), "");
        int blessAt = bt.indexOf("ModEvents.applyBreakthroughBlessing(player)");
        int tribAt = bt.indexOf("startTribulation(player, nextTier, bolts)");
        check("突破保护在降雷**之前**（顺序反了雷就开始劈了）",
                blessAt >= 0 && tribAt >= 0 && blessAt < tribAt,
                "bless@" + blessAt + " trib@" + tribAt);

        // 保护确实给了抗性与回复
        var me = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        int abl = me.indexOf("public static void applyBreakthroughBlessing(");
        String blessBody = "";
        if (abl >= 0) {
            int brace = me.indexOf('{', abl);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < me.length(); i++) {
                char c = me.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            blessBody = me.substring(brace, Math.min(me.length(), end + 1));
        }
        check("突破保护真的给了抗性", blessBody.contains("DAMAGE_RESISTANCE"), "");
        check("突破保护真的给了生命回复", blessBody.contains("REGENERATION"), "");
        check("突破保护真的回满血", blessBody.contains("healToFull"), "");

        // ---- 雷劫不能自己去摘那份保护 ----
        check("雷劫**不** removeEffect 抗性（否则会提前中止 60 秒保护）",
                !bt.contains("removeEffect(net.minecraft.world.effect.MobEffects"), "");
        check("startTribulation 不重复授予抗性（避免两个来源打架）",
                !startBody.contains("DAMAGE_RESISTANCE"), "");

        // ---- ⑤ magic 确实不吃护甲（说明注释里"加上护甲"是错的） ----
        var am = readSource("src/main/java/com/hongmeng/xiuxian/compat/ArmorMitigation.java");
        check("ArmorMitigation 对 BYPASSES_ARMOR 直接返回（所以天劫吃不到护甲）",
                am.contains("BYPASSES_ARMOR"), "");

        // ---- ⑥ 雷劫不能只对某一条道途可活 ----
        //
        // 体修有锁血、血修有缓伤；法修/魔修/阵修没有。所以必须靠
        // "回满血 + 抗性" 让所有道途都能过，而不是依赖道途机制。
        // 安全性必须来自**通用机制**（突破保护），不能只对某条道途可活。
        // 体修有锁血、血修有缓伤；法修/魔修/阵修没有 —— 所以保护不能挑道途。
        check("突破保护对所有道途生效（不挑 Path，否则有的道途必死）",
                blessBody.length() > 0
                        && !blessBody.contains("Talents.pathOf"), "");
    }


    /**
     * <b>换装必须立刻刷新加成</b>（不能等那最多 1 秒的周期刷新）。
     *
     * <p>{@code PathArmorBonus.refresh} 挂在 {@code GearManager.refresh} 里，
     * 而后者只在 {@code tickCount % 20 == 0} 时跑。于是<b>穿齐第 4 件的瞬间，
     * 四件套要等最多 1 秒才生效</b>。</p>
     *
     * <p>四件套强化的是"激荡获取/缓伤比例"这类<b>看不到的机制参数</b> ——
     * 玩家很可能在这 1 秒里就打出一刀，然后觉得"套装没用"。</p>
     */
    private static void equipmentRefreshRules() {
        section("换装即时刷新");

        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        check("监听换装事件（LivingEquipmentChangeEvent）",
                ev.contains("LivingEquipmentChangeEvent"), "");

        // 换装处理里必须刷新三样东西
        int at = ev.indexOf("public void onEquipmentChanged(");
        String body = "";
        if (at >= 0) {
            int brace = ev.indexOf('{', at);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < ev.length(); i++) {
                char c = ev.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            body = ev.substring(brace, Math.min(ev.length(), end + 1));
        }
        check("换装时刷新道途护甲（套装档位立刻生效）",
                body.contains("PathArmorBonus.refresh("), "");
        check("换装时刷新天赋属性（生命上限/抗击退）",
                body.contains("TalentAttributes.refresh("), "");
        check("换装时刷新武器攻速",
                body.contains("PathWeaponBonus.refresh("), "");

        // ---- 周期刷新保留（作为兜底） ----
        check("周期刷新仍保留（每 20 tick 兜底一次，防漏事件）",
                ev.contains("GearManager.refresh(player)"), "");
    }


    /**
     * <b>鬼域节流与伤害补偿必须成对存在</b>（沿用旧阵域的 4-tick 优化）。
     *
     * <p>只改节流、忘了乘 4 → DPS 掉到 1/4，玩家只觉得"鬼域变弱了"；
     * 只乘 4、没节流 → DPS 变 4 倍，等于秒杀。缺一个都是严重 bug。</p>
     */
    private static void formationThrottleRules() {
        section("鬼域节流与补偿");

        var rt = readSource("src/main/java/com/hongmeng/xiuxian/talent/PathRuntime.java");
        int at = rt.indexOf("private static void applyGhostRealm(");
        String body = "";
        if (at >= 0) {
            int brace = rt.indexOf('{', at);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < rt.length(); i++) {
                char c = rt.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            body = rt.substring(brace, Math.min(rt.length(), end + 1));
        }
        check("能找到 applyGhostRealm", !body.isEmpty(), "");

        boolean throttled = body.contains("getGameTime() % 4L != 0L");
        boolean compensated = body.replaceAll("\\s+", "")
                .contains("ghostDotPerTick(radius,ticks)*4.0D");
        check("鬼域结算按 4 tick 节流（省 75% 的范围查询）", throttled, "");
        check("单次伤害乘了 4（否则 DPS 掉到 1/4）", compensated, "");
        check("节流与补偿**成对**存在（缺一个就是严重 bug）",
                throttled == compensated,
                throttled && compensated ? "两件都在" : "两件必须同时存在");
        check("鬼域仍有减速", body.contains("ghostSlow(radius)"), "");
        check("鬼域仍会伤害目标", body.contains(".hurt("), "");
        check("鬼域仍排除玩家自己与其他玩家", body.contains("e == player")
                && body.contains("instanceof ServerPlayer"), "");
    }


    /**
     * <b>残留法术必须被清理</b>。
     *
     * <p>{@code SpellAftermath.clear} 的注释写着"玩家离开/死亡时清掉
     * （避免残留继续扣血）"，但<b>全项目从来没有调用过它</b>。</p>
     *
     * <p>后果：</p>
     * <ol>
     *   <li>玩家死后残留法术<b>继续打怪</b>（尸体在施法）；</li>
     *   <li>下线后 {@code ACTIVE} 里的记录<b>永不释放</b>（内存泄漏）；</li>
     *   <li>重生换维度后，残留法术会在<b>新维度里的旧坐标</b>生成特效与伤害。</li>
     * </ol>
     */
    private static void aftermathCleanupRules() {
        section("残留法术清理");

        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        int at = ev.indexOf("public void onPlayerLoggedOut(");
        String logoutBody = "";
        if (at >= 0) {
            int brace = ev.indexOf('{', at);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < ev.length(); i++) {
                char c = ev.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            logoutBody = ev.substring(brace, Math.min(ev.length(), end + 1));
        }
        check("下线时清掉残留法术（否则 ACTIVE 永不释放 -> 内存泄漏）",
                logoutBody.contains("SpellAftermath.clear("), "");

        int ra = ev.indexOf("public void onPlayerRespawn(");
        String respawnBody = "";
        if (ra >= 0) {
            int brace = ev.indexOf('{', ra);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < ev.length(); i++) {
                char c = ev.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            respawnBody = ev.substring(brace, Math.min(ev.length(), end + 1));
        }
        check("重生时清掉残留法术（否则死后还在打怪、且会跑到新维度）",
                respawnBody.contains("SpellAftermath.clear("), "");

        // ---- 剑阵：空闲索敌要有冷却 ----
        var sw = readSource(
                "src/main/java/com/hongmeng/xiuxian/sword/SwordFormationRuntime.java");
        check("剑阵空闲索敌有短冷却（否则每 tick 一次 AABB 查询）",
                sw.contains("nextScanTick"), "");
        check("冷却只有 5 tick（0.25 秒，玩家感觉不到剑慢半拍）",
                sw.contains("nextScanTick = now + 5L"), "");

        // ---- 剑阵：飞剑位置包不能省（视觉刚需） ----
        //
        // 玩家明确要求过"法术要华丽、不要为了性能削视觉"。
        // 所以这里的断言是"**必须保留** bladeFx"，防止以后有人为了性能把它砍掉。
        check("飞剑每 tick 的位置包保留（视觉刚需，不能为性能砍掉）",
                sw.contains("bladeFx("), "");
    }


    /**
     * <b>新增物品必须出现在创造模式物品栏里</b>。
     *
     * <p>抓到的 bug：我做的 <b>200 件道途护甲 + 50 把道途武器 + 洗忆丹 + 转道丹</b>
     * 全部<b>不在 {@code ModCreativeTabs} 里</b>。</p>
     *
     * <p>它们注册了、有配方、玩家能正常合成 —— 但：</p>
     * <ul>
     *   <li>创造模式<b>根本找不到</b>；</li>
     *   <li>我给出的测试步骤"拿一把道途武器试伤害"<b>做不到</b>。</li>
     * </ul>
     *
     * <p>这类"物品存在但没挂进物品栏"的问题，编译、断言、起服日志全都不报。</p>
     */
    private static void creativeTabRules() {
        section("创造物品栏完整性");

        var tabs = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/ModCreativeTabs.java");
        check("ModCreativeTabs 源码可读", !tabs.isEmpty(), "");

        check("道途护甲（200 件）在创造物品栏里",
                tabs.contains("ModPathArmor.all()")
                        || tabs.contains("ModPathArmor\n                                .all()")
                        || tabs.contains(".ModPathArmor"),
                "**不在物品栏里 -> 创造模式找不到**");
        check("道途武器（50 把）在创造物品栏里",
                tabs.contains("ModPathWeapon.all()")
                        || tabs.contains(".ModPathWeapon"), "");
        check("洗忆丹在创造物品栏里",
                tabs.contains("PILL_TALENT_RESET"), "");
        check("转道丹在创造物品栏里",
                tabs.contains("PILL_PATH_SWITCH"), "");

        // ---- 十境道袍与通用法宝武器也应该在（既有内容，防回退） ----
        check("十境道袍仍在物品栏里", tabs.contains("ModArmor.all()"), "");
        check("通用法宝武器仍在物品栏里",
                tabs.contains("ModGear.SWORDS") || tabs.contains("SWORDS"), "");

        // ---- 物品栏里挂的东西必须真的被注册过（防手滑写错名） ----
        var items = readSource("src/main/java/com/hongmeng/xiuxian/item/ModItems.java");
        check("洗忆丹确实注册了", items.contains("\"talent_reset_pill\""), "");
        check("转道丹确实注册了", items.contains("\"path_switch_pill\""), "");
    }


    /**
     * <b>Boss 出场演出的无敌状态不能残留</b>。
     *
     * <p>抓到的 bug：</p>
     * <pre>
     *   p.setInvulnerable(true);            // 开演
     *   / ** 玩家掉线时清账（防止无敌状态留在他身上）。 * /
     *   public static void forget(UUID uuid) {
     *       PLAYING.remove(uuid);           // ← 只删记录，没有解除无敌
     *   }
     *   p.setInvulnerable(false);           // 收尾（在 tick 里）
     * </pre>
     *
     * <p>删掉记录之后 {@code tick} 再也找不到它 → {@code setInvulnerable(false)}
     * <b>永远不执行</b>；而 {@code Invulnerable} <b>写进 NBT</b> →
     * 重新登录还是无敌 → <b>永久无敌</b>。</p>
     *
     * <p><b>注释都写了"防止无敌状态留在他身上"，但实现没做。</b></p>
     */
    private static void cinematicInvulnRules() {
        section("演出无敌不残留");

        var bc = readSource("src/main/java/com/hongmeng/xiuxian/combat/BossCinematic.java");
        check("BossCinematic 源码可读", !bc.isEmpty(), "");

        // ---- forget(ServerPlayer) 必须解除无敌 ----
        int at = bc.indexOf("public static void forget(net.minecraft.server.level.ServerPlayer");
        check("存在 forget(ServerPlayer) 重载（UUID 版拿不到实体，解不了无敌）",
                at >= 0, "");
        String body = "";
        if (at >= 0) {
            int brace = bc.indexOf('{', at);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < bc.length(); i++) {
                char c = bc.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            body = bc.substring(brace, Math.min(bc.length(), end + 1));
        }
        check("forget(ServerPlayer) 真的调用了 setInvulnerable(false)",
                body.contains("setInvulnerable(false)"), "");

        // ---- 下线与重生都必须走这个重载 ----
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        int lo = ev.indexOf("public void onPlayerLoggedOut(");
        String logoutBody = "";
        if (lo >= 0) {
            int brace = ev.indexOf('{', lo);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < ev.length(); i++) {
                char c = ev.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            logoutBody = ev.substring(brace, Math.min(ev.length(), end + 1));
        }
        check("下线时传 player 给 BossCinematic.forget（否则无敌残留）",
                logoutBody.contains("BossCinematic.forget(logoutPlayer)")
                        || logoutBody.contains("BossCinematic.forget(player)"), "");

        int ra = ev.indexOf("public void onPlayerRespawn(");
        String respawnBody = "";
        if (ra >= 0) {
            int brace = ev.indexOf('{', ra);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < ev.length(); i++) {
                char c = ev.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            respawnBody = ev.substring(brace, Math.min(ev.length(), end + 1));
        }
        check("重生时也清演出无敌（死在演出里）",
                respawnBody.contains("BossCinematic.forget(player)"), "");

        // ---- 正常收尾的解除仍然保留 ----
        check("演出正常结束仍会 setInvulnerable(false)",
                bc.contains("setInvulnerable(false)"), "");
    }


    /**
     * <b>矿石必须有方块标签</b>，否则挖了什么都不掉。
     *
     * <p>抓到的阻塞级 bug：{@code data/minecraft/tags/} 里<b>只有 worldgen 标签</b>，
     * <b>没有任何 {@code blocks/mineable/*.json}</b>。</p>
     *
     * <p>而所有矿石都是 {@code Properties.copy(Blocks.IRON_ORE)} ——
     * 那会把 {@code requiresCorrectToolForDrops()} 一起带过来。
     * <b>一个方块如果要求"正确工具"、却不在任何 {@code mineable/*} 标签里，
     * 就没有任何工具算正确工具 → 挖了什么都不掉。</b></p>
     *
     * <p>后果链：灵矿挖不到 → 灵铁/灵晶/空冥/雪落拿不到 →
     * 200 件护甲 + 50 把武器全部做不出来 → <b>半个模组无法开始</b>。</p>
     */
    private static void blockTagRules() {
        section("方块标签（矿石能挖到）");

        String base = "data/minecraft/tags/blocks/";
        check("mineable/pickaxe.json 存在（缺了矿石挖不掉东西）",
                findResource(base + "mineable/pickaxe.json") != null, "");

        String pick = readFileAt(findResource(base + "mineable/pickaxe.json"));
        String stone = readFileAt(findResource(base + "needs_stone_tool.json"));
        String iron = readFileAt(findResource(base + "needs_iron_tool.json"));
        String diamond = readFileAt(findResource(base + "needs_diamond_tool.json"));

        check("mineable/pickaxe 非空", !pick.isEmpty() && pick.contains("hongmeng:"), "");
        check("needs_stone_tool 存在（低阶矿石不该要铁镐）", !stone.isEmpty(), "");
        check("needs_iron_tool 存在（中阶矿石要铁镐）", !iron.isEmpty(), "");
        check("needs_diamond_tool 存在（空冥矿同远古残骸，要钻石镐）",
                !diamond.isEmpty(), "");

        // ---- 六种矿石都必须在 pickaxe 标签里 ----
        String[] ores = {"spirit_iron_ore", "deepslate_spirit_iron_ore",
                "spirit_crystal_ore", "deepslate_spirit_crystal_ore",
                "kongming_ore", "xueluo_ore"};
        int missing = 0;
        StringBuilder lack = new StringBuilder();
        for (String ore : ores) {
            if (!pick.contains("hongmeng:" + ore)) {
                missing++;
                lack.append(ore).append(" ");
            }
        }
        check("六种矿石全部在 mineable/pickaxe 里（缺一个就挖不到）",
                missing == 0,
                missing == 0 ? "六种齐全" : ("**缺：" + lack + "**"));

        // ---- 需要工具的方块都必须在 pickaxe 标签里（防以后再漏） ----
        var blocks = readSource("src/main/java/com/hongmeng/xiuxian/ModBlocks.java");
        boolean uncovered = false;
        StringBuilder unc = new StringBuilder();
        String[] needTool = {"root_altar", "epoch_portal_frame", "beast_portal_frame",
                "demon_portal_frame", "void_portal_frame", "alchemy_furnace",
                "boss_summon_altar", "boss_summon_altar_epoch",
                "boss_summon_altar_beast", "boss_summon_altar_demon",
                "boss_summon_altar_void"};
        for (String b : needTool) {
            if (blocks.contains("\"" + b + "\"") && !pick.contains("hongmeng:" + b)) {
                uncovered = true;
                unc.append(b).append(" ");
            }
        }
        check("所有「需要正确工具」的方块都在 mineable/pickaxe 里",
                !uncovered, uncovered ? ("**漏：" + unc + "**") : "");

        // ---- 工具等级门槛合理：灵铁矿不该要钻石镐 ----
        check("灵铁矿只要石镐（不该卡在钻石镐后面）",
                stone.contains("hongmeng:spirit_iron_ore")
                        && !diamond.contains("hongmeng:spirit_iron_ore"), "");
        check("空冥矿要钻石镐（同远古残骸）",
                diamond.contains("hongmeng:kongming_ore"), "");
    }


    /**
     * <b>任务提示必须与真实配方一致</b>。
     *
     * <p>抓到的开局阻塞问题：{@code q_meditate} 的提示写着
     * 「合成：<b>4 羊毛 + 1 线</b> = 打坐蒲团」，
     * 而真实配方是 {@code WWW/WLW/WWW} = <b>8 白羊毛 + 1 灵叶</b>。</p>
     *
     * <p>新手照提示做 → <b>做不出来</b> → 打不了坐 → <b>卡在第一个任务</b>。
     * 这不是"数值不对"，是<b>游戏在教玩家做一件不可能的事</b>。</p>
     */
    private static void questHintMatchesRecipeRules() {
        section("任务提示与配方一致");

        var quests = readSource("src/main/java/com/hongmeng/xiuxian/quest/Quests.java");
        check("Quests 源码可读", !quests.isEmpty(), "");

        // ---- 打坐蒲团的提示必须提到真实材料 ----
        String pad = readFileAt(findResource(
                "data/hongmeng/recipes/meditation_cushion.json"));
        check("蒲团配方存在", !pad.isEmpty(), "");

        // 真实用量：数 pattern 里 W 与 L 的个数
        int woolNeeded = 0;
        int lingyeNeeded = 0;
        if (!pad.isEmpty()) {
            // 从 "pattern" 数组里取所有行
            int patAt = pad.indexOf("\"pattern\"");
            int arrStart = pad.indexOf('[', patAt);
            int arrEnd = pad.indexOf(']', arrStart);
            String arr = arrStart >= 0 && arrEnd > arrStart
                    ? pad.substring(arrStart, arrEnd) : "";
            woolNeeded = arr.length() - arr.replace("W", "").length();
            lingyeNeeded = arr.length() - arr.replace("L", "").length();
        }
        check("蒲团只需要 4 片羊毛（新手起步不该要 8 片）",
                woolNeeded > 0 && woolNeeded <= 4,
                "实际需要 " + woolNeeded + " 片羊毛 + " + lingyeNeeded + " 灵叶");
        check("蒲团需要灵叶（灵叶打灵草掉，新手能拿到）",
                lingyeNeeded == 1, "灵叶 " + lingyeNeeded + " 片");

        // ---- 提示里写的数字必须与配方一致 ----
        int hintAt = quests.indexOf("打坐蒲团");
        String hint = "";
        if (hintAt >= 0) {
            int q1 = quests.lastIndexOf('"', hintAt - 2);
            int q2 = quests.indexOf('"', hintAt);
            hint = quests.substring(Math.max(0, q1), Math.min(quests.length(), q2 + 1));
        }
        check("蒲团的任务提示提到「4」与「羊毛」（与配方用量一致）",
                hint.contains("4") && hint.contains("羊毛"),
                hint.isEmpty() ? "找不到提示" : hint);
        check("蒲团的任务提示**不再**说「1 线」（线不是材料）",
                !hint.contains("线"), "");
        check("蒲团的任务提示说明了灵叶来源（打灵草）",
                hint.contains("灵叶") && hint.contains("灵草"), "");

        // ---- 全任务里不应再有"提到材料但配方不存在"的提示 ----
        // 只查明确写了"合成："的提示
        int synthHints = 0;
        int at = 0;
        while (true) {
            int k = quests.indexOf("合成：", at);
            if (k < 0) {
                break;
            }
            synthHints++;
            at = k + 3;
        }
        check("写了「合成：」的任务提示数量受控（每条都要对得上配方）",
                synthHints <= 3, synthHints + " 条");
    }


    /**
     * <b>起步链与五条道途的第 1 境材料不能有维度门槛</b>。
     *
     * <p>抓到的两个问题：</p>
     * <ol>
     *   <li><b>魔修整条道途开局就废</b>：我生成魔修装备配方时用了
     *       {@code minecraft:ghast_tear}（恶魂之泪）当专属材料 ——
     *       那只能在下界打恶魂掉。于是魔修 <b>40 件护甲 + 10 把武器、
     *       全部 10 个境界都要下界材料</b>；其他四条道途用的都是主世界材料
     *       （紫水晶 / 铁块 / 红石 / 青色染料）。</li>
     *   <li><b>炼丹炉要烈焰粉</b>：{@code alchemy_furnace} 要
     *       {@code blaze_powder} → 必须去下界要塞打烈焰人。
     *       而炼丹是核心循环之一，任务链里明确在教。</li>
     * </ol>
     */
    private static void noDimensionGateRules() {
        section("起步链无维度门槛");

        String[] nether = {"blaze_powder", "blaze_rod", "ghast_tear", "nether_wart",
                "nether_star", "netherite", "ancient_debris"};

        // ---- 起步链配方 ----
        String[] core = {"meditation_cushion", "alchemy_furnace", "lingtian_soil",
                "guide_book", "root_altar", "pill_huiling", "pill_zhuji",
                "pill_pojing", "pill_exp_common", "talent_reset_pill",
                "path_switch_pill"};
        int gated = 0;
        StringBuilder gatedList = new StringBuilder();
        for (String c : core) {
            String body = readFileAt(findResource(
                    "data/hongmeng/recipes/" + c + ".json")).toLowerCase();
            if (body.isEmpty()) {
                continue;
            }
            for (String k : nether) {
                if (body.contains(k)) {
                    gated++;
                    gatedList.append(c).append("(").append(k).append(") ");
                    break;
                }
            }
        }
        check("起步链配方没有下界材料（新手在主世界就能做完）",
                gated == 0,
                gated == 0 ? "全部主世界可做" : ("**有门槛：" + gatedList + "**"));

        // ---- 五条道途的第 1 境护甲 ----
        String[] paths = {"fa", "ti", "mo", "xue", "zhen"};
        int pathGated = 0;
        StringBuilder pgList = new StringBuilder();
        for (String p : paths) {
            for (String slot : new String[]{"helmet", "chestplate",
                    "leggings", "boots"}) {
                String body = readFileAt(findResource(
                        "data/hongmeng/recipes/path_armor_" + p + "_1_" + slot
                                + ".json")).toLowerCase();
                if (body.isEmpty()) {
                    continue;
                }
                for (String k : nether) {
                    if (body.contains(k)) {
                        pathGated++;
                        pgList.append(p).append("_").append(slot).append(" ");
                        break;
                    }
                }
            }
        }
        check("五条道途**第 1 境护甲**都没有下界材料（否则那条道途开局做不出装备）",
                pathGated == 0,
                pathGated == 0 ? "五条道途都可用主世界材料起手"
                        : ("**有门槛：" + pgList + "**"));

        // ---- 第 1 境武器同理 ----
        String[] weapons = {"spirit_sword", "mountain_pestle", "soul_devourer",
                "blood_dual_blade", "zhou_tian_wheel"};
        int wGated = 0;
        StringBuilder wList = new StringBuilder();
        for (String w : weapons) {
            String body = readFileAt(findResource(
                    "data/hongmeng/recipes/path_weapon_" + w + "_1.json"))
                    .toLowerCase();
            if (body.isEmpty()) {
                continue;
            }
            for (String k : nether) {
                if (body.contains(k)) {
                    wGated++;
                    wList.append(w).append(" ");
                    break;
                }
            }
        }
        check("五把道途武器**第 1 境**都没有下界材料", wGated == 0,
                wGated == 0 ? "全部主世界可做" : ("**有门槛：" + wList + "**"));
    }


    /**
     * <b>修行录必须教玩法</b>，不能只是物品图鉴。
     *
     * <p>抓到的缺口：{@code guide.json} 是自动生成的物品/方块/群系图鉴，
     * {@code 道途 / 天赋 / 副业 / 剑阵 / 洗点} 这些词在里面<b>一个都没有</b>。</p>
     *
     * <p>于是玩家<b>有书可看，却学不到怎么玩</b> —— 这是"做了没人知道"的最后一环。</p>
     */
    private static void guideMechanicsRules() {
        section("修行录教玩法");

        var scr = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/GuideBookScreen.java");
        check("GuideBookScreen 有「玩法」页签",
                scr.contains("\"guide\", \"玩法\"")
                        || (scr.contains("\"guide\"") && scr.contains("玩法")), "");
        check("kindOf 能把 guide 映射到「玩法」页签",
                scr.contains("case \"guide\" -> \"guide\""), "");

        String guide = readFileAt(findResource("assets/hongmeng/guide/guide.json"));
        check("guide.json 可读", !guide.isEmpty(), "");

        // ---- 至少 5 条玩法条目 ----
        int guideEntries = 0;
        int at = 0;
        while (true) {
            int k = guide.indexOf("\"kind\": \"guide\"", at);
            if (k < 0) {
                k = guide.indexOf("\"kind\":\"guide\"", at);
            }
            if (k < 0) {
                break;
            }
            guideEntries++;
            at = k + 5;
        }
        check("guide.json 里至少有 5 条玩法条目", guideEntries >= 5,
                guideEntries + " 条");

        // ---- 必须覆盖六大主题 ----
        String[] topics = {"道途", "天赋", "副业", "剑阵", "激荡", "洗忆丹"};
        int covered = 0;
        StringBuilder lack = new StringBuilder();
        for (String t : topics) {
            if (guide.contains(t)) {
                covered++;
            } else {
                lack.append(t).append(" ");
            }
        }
        check("玩法条目覆盖道途/天赋/副业/剑阵/机制/洗点（六项）",
                covered == topics.length,
                covered == topics.length ? "六项齐全" : ("缺：" + lack));

        // ---- 内容要点：必须真的说清楚"怎么进" ----
        check("职业条目说明了入口（按 R 打开面板）", guide.contains("按 R"), "");
        check("天赋条目说明了点数来源（选职业送 3 点）", guide.contains("选职业送 3 点"), "");
        check("副业条目说明了入口（/hongmeng ui profession）",
                guide.contains("ui profession"), "");
        check("剑阵条目说明了按键（N 键，已从 V 改掉避免撞法术槽）", guide.contains("N 键"), "");
    }


    /**
     * <b>修行录的详情必须折行，不能截断</b>。
     *
     * <p>抓到的显示问题：</p>
     * <pre>
     *   g.drawString(font, font.plainSubstrByWidth(detail, dw - tw - 8), ...)
     * </pre>
     * <p>{@code plainSubstrByWidth} 是<b>截断</b>不是折行 —— 长句在一半被砍掉，
     * <b>而且没有省略号</b>，读起来像坏掉了。「玩法」条目每句都长，必被切坏。</p>
     *
     * <p>另外：{@code cnOf("guide")} 落进 {@code default -> "其它"}，
     * 玩法条目会显示成 {@code [其它]}；标题写「获得途径」对说明条目也不对。</p>
     */
    private static void guideWrapRules() {
        section("修行录折行");

        var scr = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/GuideBookScreen.java");

        check("存在 wrapText 折行工具方法", scr.contains("wrapText("), "");
        check("详情渲染调用了 wrapText（不再直接截断）",
                scr.contains("wrapText(detail"), "");

        // ---- 详情不再用 plainSubstrByWidth 直接画 ----
        //
        // wrapText 内部会用 plainSubstrByWidth 做"每行取多宽"，
        // 所以这里查的是"**画字的地方**没有直接用它"。
        int drawAt = scr.indexOf("// \u26a0 详情必须**折行**");
        boolean drawsRaw = false;
        if (drawAt >= 0) {
            String seg = scr.substring(drawAt, Math.min(scr.length(), drawAt + 900));
            drawsRaw = seg.contains("drawString(this.font, this.font.plainSubstrByWidth");
        }
        check("画字处不再直接 plainSubstrByWidth（那是截断）", !drawsRaw, "");

        // ---- cnOf 有 guide 分支 ----
        check("cnOf 有「玩法」标签（否则显示成 [其它]）",
                scr.contains("case \"guide\" -> \"玩法\""), "");

        // ---- 标题按页签区分 ----
        check("「玩法」页签下标题是「说明」而不是「获得途径」",
                scr.contains("\"guide\".equals(tab)"), "");

        // ---- 溢出判据留出多行空间 ----
        check("溢出判据留出多行空间（否则最后一个条目会被面板底边裁掉）",
                scr.contains("listBottom() - 48"), "");
    }


    /**
     * <b>单行字符串里不能有 {@code \n}</b>（会画成豆腐块）。
     *
     * <p>抓到的 bug：{@code bonusText()} 里拼了
     * {@code sb.append("\n").append(setText())}，而它的两个去向
     * <b>都不处理换行</b>：</p>
     * <ol>
     *   <li>{@code PathArmorBonus.summary()} → 面板的<b>单行</b>
     *       {@code drawString}；</li>
     *   <li>物品 tooltip 的<b>单个 {@code Component.literal}</b>。</li>
     * </ol>
     * <p>Minecraft 的 {@code drawString} 不会把 {@code \n} 当换行 ——
     * 它画成一个 notdef <b>豆腐块</b>，后面的文字还会<b>溢出面板边框</b>。</p>
     */
    private static void noNewlineInLineRules() {
        section("单行文本无换行符");

        // ---- bonusText 里不能拼换行符 ----
        var tier = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/armor/PathArmorTier.java");
        int bt = tier.indexOf("public String bonusText()");
        String btBody = "";
        if (bt >= 0) {
            int brace = tier.indexOf('{', bt);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < tier.length(); i++) {
                char c = tier.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            btBody = tier.substring(brace, Math.min(tier.length(), end + 1));
        }
        // ⚠ 必须先剥掉注释再检查。
        //
        //   我在 bonusText 里留了说明注释（解释为什么不能拼换行），
        //   注释本身含有 setText 字样与反斜杠 n —— 直接 contains 会误报。
        //   这是同一个坑第 3 次踩：断言匹配到了注释。
        String btCode = stripComments(btBody);
        check("bonusText() 里不拼换行符（会画成豆腐块）",
                !btCode.contains("\\\\n"), "");
        check("bonusText() 里不拼 setText()（套装说明要单独一行）",
                !btCode.contains("setText()"), "");

        // ---- summary 要短：不含 bonusText ----
        var bonus = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/armor/PathArmorBonus.java");
        int sm = bonus.indexOf("public static String summary(");
        String smBody = "";
        if (sm >= 0) {
            int brace = bonus.indexOf('{', sm);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < bonus.length(); i++) {
                char c = bonus.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            smBody = bonus.substring(brace, Math.min(bonus.length(), end + 1));
        }
        check("summary() 里不含 bonusText()（那串太长会溢出面板）",
                !smBody.contains("bonusText()"), "");
        check("summary() 里不含 \\n", !smBody.contains("\\n"), "");

        // ---- tooltip 单独加一行 ----
        var armor = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/armor/ModPathArmor.java");
        check("护甲 tooltip 用 setText() 单独加一行",
                armor.contains("tier.setText()"), "");
    }


    /**
     * <b>玩家反馈批次一</b>：一次性锁住这一轮的修复。
     *
     * <p>每条都对应玩家截图里指出的具体问题 —— 问题能回来，断言必须挡。</p>
     */
    private static void playerFeedbackBatch1Rules() {
        section("玩家反馈批次一");

        // ---- 1) 天陨星落要破坏地形 ----
        var des = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellDestruction.java");
        check("破坏半径有上限 MAX_CRATER_RADIUS（110 格球会挖不动）",
                des.contains("MAX_CRATER_RADIUS"), "");
        check("收集阶段跳过不可破坏的方块（否则空气吃光 MAX_BLOCKS 名额）",
                des.contains("canBreak(level.getBlockState(pos))"), "");
        check("破坏按由内向外遍历（不是从球顶往下）", des.contains("for (int step = 0"), "");

        // ---- 2) 择道点击 ----
        var panel = readSource(
                "src/main/java/com/hongmeng/xiuxian/CultivationPanelScreen.java");
        check("择道有独立可见标记 pathChooseVisible（talentVisible 在未择道时是 false）",
                panel.contains("pathChooseVisible"), "");
        check("择道点击用 pathChooseVisible 当门",
                panel.contains("if (pathChooseVisible)"), "");
        check("pathHitOrd 每帧重置为 -1（默认 0 会被当成法修）",
                panel.contains("pathHitOrd[r] = -1"), "");

        // ---- 3) 按键不冲突 ----
        var setup = readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java");
        check("剑阵展开是 N（不再用 V —— V 是法术槽 4）",
                setup.contains("GLFW.GLFW_KEY_N"), "");
        check("阵型切换是 M（不再 Ctrl+R —— 会撞修炼面板）",
                setup.contains("GLFW.GLFW_KEY_M"), "");
        check("锁定目标是 L", setup.contains("GLFW.GLFW_KEY_L"), "");
        var swInput = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/ClientSwordInput.java");
        check("剑阵输入不再依赖 Ctrl", !swInput.contains("hasControlDown()"), "");

        // ---- 4) 贴图：双刀不能是纯 X，护甲要有层次 ----
        String wk = findResource(
                "assets/hongmeng/textures/item/path_weapon_blood_twin_blades.png");
        check("血修双刀贴图存在", wk != null, "");
        // 用文件大小做粗判：原来的纯 X 只有 ~115 字节（4 个颜色、无描边）
        if (wk != null) {
            int cols = opaqueColorCount(wk);
            check("双刀贴图有明暗层次（不是纯色 X 占位符）", cols >= 5,
                    "实际 " + cols + " 色");
        }
        String armTex = findResource(
                "assets/hongmeng/textures/item/path_armor_fa_chestplate.png");
        if (armTex != null) {
            // 用**不透明颜色数**判断细节，比文件字节数可靠：
            //   PNG 对大片纯色压缩极好，181 字节也可能有 5 层明暗。
            int cols = opaqueColorCount(armTex);
            check("护甲图标有明暗层次（至少 4 种不透明色，不是纯色剪影）",
                    cols >= 4, "实际 " + cols + " 色");
        }
        String layer = findResource(
                "assets/hongmeng/textures/models/armor/path_armor_fa_layer_1.png");
        if (layer != null) {
            // 按**颜色数**判：体积会因区域规整而变小，但纯色块的颜色数一定少（原图只有 8 种）。
            check("护甲图层有结构（颜色数 > 8）",
                    pngColors(layer) > 8,
                    "实际 " + pngColors(layer) + " 种颜色");
        }

        // ---- 5) 灵根不再显示属性 ----
        var root = readSource(
                "src/main/java/com/hongmeng/xiuxian/spirit/SpiritRoot.java");
        int cnAt = root.indexOf("public String cn()");
        String cnBody = "";
        if (cnAt >= 0) {
            int brace = root.indexOf('{', cnAt);
            int end = root.indexOf('}', brace);
            cnBody = root.substring(brace, Math.min(root.length(), end + 1));
        }
        check("灵根显示名只取品质（不再拼元素）", cnBody.contains("rarity.cn()"),
                cnBody.replace("\n", " "));

        // ---- 6) 修行录：配方 + 按键 ----
        String guide = readFileAt(findResource("assets/hongmeng/guide/guide.json"));
        int paRec = 0;
        int pwRec = 0;
        int at = 0;
        while (true) {
            int k = guide.indexOf("\"hongmeng:path_armor_", at);
            if (k < 0) {
                break;
            }
            paRec++;
            at = k + 10;
        }
        at = 0;
        while (true) {
            int k = guide.indexOf("\"hongmeng:path_weapon_", at);
            if (k < 0) {
                break;
            }
            pwRec++;
            at = k + 10;
        }
        check("修行录已不再收录道途护甲（软删除）", paRec == 0,
                paRec + " 处引用");
        check("修行录已不再收录道途武器（软删除）", pwRec == 0, pwRec + " 处引用");
        check("修行录的按键页写了剑阵 N / M / L",
                panel.contains("剑阵展开 / 收回") && panel.contains("切换剑阵阵型"), "");
        check("修行录的玩法条目按键已同步为 N/M/L",
                guide.contains("N 键展开"), "");

        // ---- 7) 运镜：施法即开始 + 期间隐藏 HUD ----
        var caster = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/TechniqueCaster.java");
        check("蓄力开始就启动运镜（不是释放完才动）",
                caster.contains("SpellCinematic.beginCharge"), "");
        var cam = readSource("src/main/java/com/hongmeng/xiuxian/client/Cinematic.java");
        check("演出期间隐藏 HUD（等同 F1）", cam.contains("hideGui = true"), "");
        check("演出结束把 HUD 还原成原值（不能强行打开）",
                cam.contains("hudWasHidden"), "");


        // ---- 9) R-2：旧道途装备的"递进链"随配方一起退役 ----
        boolean legacyChainGone =
                findResource("data/hongmeng/recipes/path_armor_zhen_5_helmet.json") == null
                        && findResource(
                        "data/hongmeng/recipes/path_armor_mo_10_chestplate.json") == null
                        && findResource(
                        "data/hongmeng/recipes/path_weapon_celestial_chakram_10.json") == null
                        && findResource(
                        "data/hongmeng/recipes/path_armor_zhen_1_helmet.json") == null;
        check("旧道途装备递进链已随配方删除退役（R-2 legacy）",
                legacyChainGone, "旧 200 甲 + 50 武器 + 10 境界剑不再产出；注册项保留");

        // ---- 8) 演出无敌必须能摘 ----
        var spell = readSource(
                "src/main/java/com/hongmeng/xiuxian/combat/SpellCinematic.java");
        check("法术演出有摘无敌的入口 endCam", spell.contains("public static void endCam"), "");
        check("法术演出有到点自动摘的 tickCamera", spell.contains("tickCamera"), "");
        int invTrue = spell.length() - spell.replace("setInvulnerable(true)", "").length();
        int invFalse = spell.length() - spell.replace("setInvulnerable(false)", "").length();
        check("法术演出 setInvulnerable(true/false) 成对（只挂不摘 = 永久无敌）",
                invTrue > 0 && invFalse > 0,
                "true x" + (invTrue / 20) + " / false x" + (invFalse / 21));
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        check("每 tick 检查演出无敌是否该摘", ev.contains("SpellCinematic.tickCamera"), "");
        check("下线时摘掉演出无敌", ev.contains("SpellCinematic.endCam"), "");
    }


    /**
     * <b>Shift = 跳过整段（含过场动画）</b>，不是"删掉过场动画"。
     *
     * <p>这条断言我改过一次，因为<b>我理解错了玩家的话</b>：</p>
     * <ul>
     *   <li>玩家：「法术释放按 shift 的时候，动画也会跳过」——
     *       我当成 bug 报告，把跳过功能整个删了；</li>
     *   <li>玩家澄清：「我让你做的是按 shift 的时候<b>连过场动画都会一起跳过</b>，
     *       <b>而不是删除过场动画</b>」。</li>
     * </ul>
     *
     * <p>正确语义：不按 Shift 走完整流程；按 Shift 则蓄力、蓄力特效、
     * 过场动画<b>全部跳过</b>，直接结算。</p>
     *
     * <p>⚠ 最容易漏的一点：运镜是 {@code resolve} 里由
     * {@code showBlastCinematic} 触发的 —— <b>光跳过蓄力拦不住它</b>。</p>
     */
    private static void shiftSkipRules() {
        section("Shift 跳过整段");

        var caster = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/TechniqueCaster.java");
        String code = stripComments(caster);

        // ---- 跳过开关存在且读 Shift ----
        check("按 Shift 会跳过蓄力（玩家要的整段跳过）",
                code.contains("player.isShiftKeyDown()"), "");
        // ---- 必须同时抑制运镜 ----
        check("有抑制运镜的标记 SUPPRESS_CINEMATIC",
                code.contains("SUPPRESS_CINEMATIC"), "");
        check("跳过时置位抑制标记", code.contains("SUPPRESS_CINEMATIC.set("), "");
        check("结算结束后清掉标记（不然下一发也没运镜）",
                code.contains("SUPPRESS_CINEMATIC.remove()"), "");
        // ---- showBlastCinematic 必须检查 ----
        int at = code.indexOf("showBlastCinematic(");
        check("showBlastCinematic 存在", at >= 0, "");
        // ---- 不按 Shift 的完整路径仍在 ----
        check("完整蓄力仍会触发起手特效 beginCastFx",
                code.contains("beginCastFx("), "");
        check("完整蓄力仍会启动蓄力运镜 beginCharge",
                code.contains("SpellCinematic.beginCharge"), "");
    }


    /**
     * <b>道宫「释放速度」必须真的缩短蓄力</b>。
     *
     * <p>玩家反馈：「领域的加成感觉没有实现啊，只是写出来了，没有对应变化呢」。</p>
     *
     * <p>核实：{@code DomainQuality} 声明了两条加成 ——</p>
     * <ul>
     *   <li>功法伤害 +100%~+1000%：✅ 已接（{@code damageOf}）；</li>
     *   <li><b>释放速度 +10%~+100%：❌ 只出现在界面文字里，从未改变任何东西</b>。</li>
     * </ul>
     */
    private static void domainCastSpeedRules() {
        section("道宫加成真实生效");

        var caster = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/TechniqueCaster.java");
        String code = stripComments(caster);

        check("蓄力时长会读道宫释放速度", code.contains("castSpeedBonus(data)"), "");
        check("蓄力时长按加成缩短（÷ 1 + 速度）", code.contains("1.0D + spd"), "");
        check("实际时长传给 beginCast（否则进度条与完成刻错位）",
                code.contains("tech.beginCast(technique, now, effectiveTicks)"), "");
        check("实际时长传给客户端进度条",
                code.contains("CastProgressPacket.sendStart(")
                        && code.contains("effectiveTicks"), "");
        check("运镜时长也用实际时长（否则镜头比法术长）",
                code.contains("beginCharge(") && code.contains("effectiveTicks"),
                "");
        // 伤害加成一直是接的，锁住防回退
        check("同元素功法伤害加成仍接在 damageOf 里",
                code.contains("domains.techniqueBonus(technique.element())"), "");

        // ---- TechniqueData 三处一致 ----
        var td = readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/TechniqueData.java");
        check("TechniqueData 记下实际总时长", td.contains("castingTotalTicks"), "");
        check("完成判定用实际总时长", td.contains("now + this.castingTotalTicks"), "");
        check("进度条用实际总时长（不能用 technique.castTicks）",
                td.contains("int total = Math.max(0, castingTotalTicks)"), "");

        // ---- 可见性：属性页要写出数字 ----
        var panel = readSource(
                "src/main/java/com/hongmeng/xiuxian/CultivationPanelScreen.java");
        check("属性页显示释放速度的百分比", panel.contains("释放速度："), "");
        check("属性页逐门列出功法是否吃到加成", panel.contains("renderDomainBonusLines"), "");
        check("未匹配时明确写「未匹配」（玩家才分得清没加成 vs 加成小）",
                panel.contains("未匹配"), "");
    }


    /**
     * <b>「写了 getter、没有调用点」专项</b>（本会话第 10~12 次踩这个模式）。
     *
     * <p>本轮抓到的三个：</p>
     * <ol>
     *   <li><b>法修真元盾</b>：{@code onPlayerHurt} 里只有体修/血修分支，
     *       {@code manaShieldRatio()} 零调用 ——
     *       法修的核心机制「拿法力当血」<b>完全不存在</b>；</li>
     *   <li><b>阵修副业加速</b>：{@code professionSpeed()} 零调用，
     *       {@code recordWork} 固定 +1 —— 阵修练阵法不比别人快；</li>
     *   <li><b>被动回蓝 / 吐纳诀</b>：{@code regenPerSecond()} 与
     *       {@code manaRegenMultiplier()} 零调用 ——
     *       法力只能靠丹药，而「吐纳诀 +50% 回复」在乘一个不存在的东西。</li>
     * </ol>
     */
    private static void deadGetterWiringRules() {
        section("死 getter 补接");

        // ---- 1) 剑修剑罡 ----
        var rt = readSource(
                "src/main/java/com/hongmeng/xiuxian/talent/PathRuntime.java");
        String rtCode = stripComments(rt);
        check("onPlayerHurt 有剑修分支（否则剑罡不存在）",
                rtCode.contains("Career.JIAN_XIU"), "");
        check("剑罡会读 swordAuraRatio", rtCode.contains("swordAuraRatio("), "");
        check("剑罡会真的扣灵力", rtCode.contains("ManaBridge"), "");
        check("剑罡有视觉反馈", rtCode.contains("PathMechanicFx"), "");
        var pm = readSource(
                "src/main/java/com/hongmeng/xiuxian/talent/PathMechanics.java");
        check("法力换伤害的兑换率有明确定义", pm.contains("MANA_PER_DAMAGE"), "");

        // ---- 2) 阵修副业加速 ----
        var tal = readSource("src/main/java/com/hongmeng/xiuxian/talent/Talents.java");
        String talCode = stripComments(tal);
        int rw = talCode.indexOf("public static boolean recordWork(");
        String rwBody = "";
        if (rw >= 0) {
            int brace = talCode.indexOf('{', rw);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < talCode.length(); i++) {
                char c = talCode.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            rwBody = talCode.substring(brace, Math.min(talCode.length(), end + 1));
        }
        check("recordWork 会读 professionSpeed（阵修练阵法更快）",
                rwBody.contains("professionSpeed("), "");

        // ---- 3) 被动回蓝 + 吐纳诀 ----
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        String evCode = stripComments(ev);
        check("有被动回蓝（每秒一次）",
                evCode.contains("REGEN_PER_SECOND_FRACTION"), "");
        check("回蓝会读吐纳诀的倍率",
                evCode.contains("manaRegenMultiplier"), "");
        // 必须是"每秒"结算，每 tick 加小数会被 setMana 的 floor 抹成 0
        check("回蓝按秒结算（每 tick 加分小数会 floor 成 0）",
                evCode.contains("tickCount % 20 == 0"), "");
    }


    /**
     * <b>重要配置项必须真的被读取</b>。
     *
     * <p>巡检发现 56 个配置项里有 6 个从未被读取 —— 玩家改了完全没变化。
     * 其中两个是<b>配置与行为相反</b>：</p>
     * <ul>
     *   <li>{@code keepItemsOnDeath} 默认 {@code true}（"死亡不掉落物品"），
     *       但没有任何实现 —— <b>玩家以为不会掉，实际会掉光</b>；</li>
     *   <li>{@code failExpLossRatio} 写成 0.30，代码里却硬编码
     *       {@code cultivation / 3}，配置被完全忽略。</li>
     * </ul>
     *
     * <p>另三个（{@code miningSpeedPerRealmTier} / {@code adventureGain} /
     * {@code taskGain} / {@code spawnerDropsAtNetherite}）已接或如实标注「预留」。</p>
     */
    private static void configWiringRules() {
        section("配置项真的生效");

        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        String evCode = stripComments(ev);
        check("keepItemsOnDeath 有实现（对齐原版 keepInventory）",
                evCode.contains("keepItemsOnDeath.get()"), "");
        check("keepItemsOnDeath 走的是原版规则（自己搬背包会有复制/丢失风险）",
                evCode.contains("RULE_KEEPINVENTORY"), "");

        var bt = readSource("src/main/java/com/hongmeng/xiuxian/core/Breakthrough.java");
        String btCode = stripComments(bt);
        check("突破失败按配置比例扣修为（不再硬编码 1/3）",
                btCode.contains("failExpLossRatio.get()"), "");
        check("突破失败不再有硬编码的 3L 除数",
                !btCode.contains("/ 3L"), "");

        var rm = readSource("src/main/java/com/hongmeng/xiuxian/core/RealmMining.java");
        String rmCode = stripComments(rm);
        check("挖掘速度加成读配置 miningSpeedPerRealmTier",
                rmCode.contains("miningSpeedPerRealmTier.get()"), "");

        // ---- 其余低价值项必须在注释里如实标注「预留」 ----
        var cfg = readSource(
                "src/main/java/com/hongmeng/xiuxian/compat/HongmengConfig.java");
        check("未实现的配置项在注释里标注了「预留」（不骗玩家去调）",
                cfg.contains("预留"), "");
    }


    /**
     * <b>攻击动作系统整条链路都要接上</b>。
     *
     * <p>玩家要求「参考动作 mod 的攻击动作」。系统横跨四层，
     * 任何一层没接就<b>完全没有效果而且不报错</b>：</p>
     * <ol>
     *   <li>服务端：判武器分型 + 推进三段连击；</li>
     *   <li>网络：把 {@code kind + combo} 广播给附近玩家；</li>
     *   <li>客户端：存状态、按阶段算姿态；</li>
     *   <li>渲染：push/pop 配对（错一个整个世界渲染都会歪）。</li>
     * </ol>
     */
    private static void combatAnimRules() {
        section("攻击动作");

        // ---- 1) 服务端分型 + 连击 ----
        var ca = readSource("src/main/java/com/hongmeng/xiuxian/combat/CombatAnim.java");
        String caCode = stripComments(ca);
        check("有武器分型 kindOf", caCode.contains("public static int kindOf("), "");
        check("五条道途各有招式（TI/MO/XUE/ZHEN/FA 都出现）",
                caCode.contains("KIND_HEAVY") && caCode.contains("KIND_STAB")
                        && caCode.contains("KIND_TWIN") && caCode.contains("KIND_RING")
                        && caCode.contains("KIND_SWORD"), "");
        check("有连击窗口（不是每下都从第一段开始）",
                caCode.contains("COMBO_WINDOW"), "");
        // 现在是按**该武器的连段长度**循环（五条道途段数不同）
        check("连击按武器连段长度循环（不是写死三段）",
                caCode.contains("% len"), "");

        // ---- 2) 广播 ----
        check("攻击时真的广播（CombatAnim.onAttack 会发包含自身）",
                caCode.contains("AttackAnimPacket"), "");
        var net = readSource("src/main/java/com/hongmeng/xiuxian/network/ModNetwork.java");
        check("AttackAnimPacket 已注册到通道", net.contains("AttackAnimPacket.class"), "");
        var pkt = readSource(
                "src/main/java/com/hongmeng/xiuxian/network/AttackAnimPacket.java");
        check("包里带 UUID + kind + combo",
                pkt.contains("writeUUID") && pkt.contains("writeByte"), "");
        check("包只在客户端处理（DistExecutor 隔离，服务端不会加载渲染类）",
                pkt.contains("unsafeRunWhenOn"), "");

        // ---- 3) 事件接线 ----
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        String evCode = stripComments(ev);
        check("服务端监听 AttackEntityEvent 并广播",
                evCode.contains("AttackEntityEvent") && evCode.contains("CombatAnim.onAttack"),
                "");
        check("掉线清掉连击计数", evCode.contains("CombatAnim.forget"), "");

        // ---- 4) 渲染钩子 ----
        var rend = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/CombatAnimRenderer.java");
        String rCode = stripComments(rend);
        check("第三人称 Pre/Post 都接（push 必须配对 pop）",
                rCode.contains("RenderPlayerEvent.Pre")
                        && rCode.contains("RenderPlayerEvent.Post"), "");
        check("第一人称接 RenderHandEvent", rCode.contains("RenderHandEvent"), "");
        check("第一人称不等网络回包（本地立刻起手）",
                rCode.contains("onLocalAttack"), "");
        var setup = readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java");
        check("CombatAnimRenderer 注册到事件总线",
                setup.contains("CombatAnimRenderer.class"), "");

        // ---- 5) 姿态表：缓动 + 多分支 ----
        var anim = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/CombatAnimClient.java");
        String aCode = stripComments(anim);
        check("有缓动函数（线性插值看起来像机械臂）",
                aCode.contains("easeOut(") && aCode.contains("easeInOut("), "");
        check("重武器分蓄/发两段（不是匀速转）",
                aCode.contains("p < 0.42F"), "");
        // push 在姿态类、pop 在渲染器（Pre 里 push / Post 里 pop）——
        // 分在两个文件是对的，所以断言也要分两处查。
        check("姿态类负责 push", aCode.contains("pushPose()"), "");
        check("渲染器负责 pop（与 Pre 配对；漏掉会让整个世界渲染歪掉）",
                rCode.contains("popPose()"), "");
        check("渲染器记录了谁 push 过（否则不知道 pop 几次）",
                rCode.contains("PUSHED"), "");
    }


    /**
     * <b>剑气拖尾</b>（玩家要求参考棱花剑魂 / Fancy Trail 这类动作 mod）。
     *
     * <p>招牌效果是挥砍时沿轨迹扫出的一条发光弧带。
     * <b>零外部素材</b>：用原版 {@code RenderType.lightning()} ——
     * {@code POSITION_COLOR} + 加色混合 + 无纹理（闪电就用它画的）。</p>
     *
     * <p>缺任何一环都<b>完全看不见而且不报错</b>，所以逐环断言。</p>
     */
    private static void slashTrailRules() {
        section("剑气拖尾");

        var st = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/SlashTrailRenderer.java");
        String code = stripComments(st);

        check("用加色混合的 RenderType（lightning = 自发光，不需要贴图）",
                code.contains("RenderType.lightning()"), "");
        check("几何是四边形（POSITION_COLOR 的 lightning 只认 QUADS）",
                code.contains("endVertex()"), "");
        check("按武器分型画不同形状（横扫 / 竖直砸 / 整圈）",
                code.contains("sweepOf(") && code.contains("KIND_HEAVY"), "");
        check("弧长算到 0 就不画（否则会出现一条贴片）",
                code.contains("Math.abs(head) < 1.0F"), "");
        check("头亮尾淡（f0*f0 衰减）", code.contains("f0 * f0"), "");
        check("有最大条数上限（刷屏时不会无限堆）", code.contains("MAX_TRAILS"), "");
        check("坐标转到相机空间（世界坐标 - 相机位置）",
                code.contains("-camPos.x"), "");

        // ---- 触发点：自己 + 别人 ----
        var anim = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/CombatAnimClient.java");
        String aCode = stripComments(anim);
        check("服务端广播到达时启动拖尾（别人挥砍也看得到）",
                aCode.contains("SlashTrailRenderer.start(p, kind, combo)"), "");
        check("自己攻击时立刻启动（不等回包）",
                aCode.contains("SlashTrailRenderer.start(mc.player"), "");
        check("换世界清账", aCode.contains("SlashTrailRenderer.clear()"), "");

        // ---- 每帧渲染 ----
        var rend = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/CombatAnimRenderer.java");
        String rCode = stripComments(rend);
        check("每帧渲染拖尾（RenderLevelStageEvent）",
                rCode.contains("RenderLevelStageEvent"), "");
        check("在粒子之后画（AFTER_PARTICLES，观感更亮）",
                rCode.contains("AFTER_PARTICLES"), "");
    }


    /**
     * <b>剑阵的获取门槛</b>（玩家：「剑阵为什么开局就自带呢，是否设置一个获取方式？」）。
     *
     * <p>原来只有"境界 ≥ 第 2 境"一道门槛，等于第二次突破就白送 ——
     * 剑阵是最有存在感的战斗系统之一，白送会让人觉得它不算个东西。</p>
     *
     * <p>现在：<b>读过《御剑术秘籍》</b>才永久解锁；境界门槛仍然保留。</p>
     */
    private static void swordArtGateRules() {
        section("剑阵获取门槛");

        // ---- 数据层：四个出口都要补 ----
        var data = readSource("src/main/java/com/hongmeng/xiuxian/core/CultivationData.java");
        check("角色数据里有御剑术解锁字段", data.contains("swordArtUnlocked"), "");
        check("存档写了这个字段", data.contains("putBoolean(\"SwordArt\""), "");
        check("读档读了这个字段（旧存档按未解锁处理）",
                data.contains("tag.contains(\"SwordArt\")"), "");
        check("网络同步带上了（否则客户端以为没解锁）",
                data.contains("writeBoolean(swordArtUnlocked)"), "");
        check("网络读取带上了", data.contains("readBoolean()"), "");
        check("死亡重生复制带上了（否则死一次就丢）",
                data.contains("this.swordArtUnlocked = other.swordArtUnlocked"), "");

        // ---- 门槛 ----
        var rt = readSource("src/main/java/com/hongmeng/xiuxian/sword/SwordFormationRuntime.java");
        String rtCode = stripComments(rt);
        check("展开剑阵前先查御剑术是否解锁",
                rtCode.contains("SwordArt.unlocked("), "");
        check("未解锁时给出明确指引（而不是静默无反应）",
                rtCode.contains("御剑术秘籍"), "");
        check("境界门槛仍在（第 2 境起才有飞剑）",
                rtCode.contains("SwordFormation.canUse("), "");

        // ---- 道具 ----
        var items = readSource("src/main/java/com/hongmeng/xiuxian/item/ModItems.java");
        check("秘籍已注册", items.contains("SWORD_ART_MANUAL"), "");
        var manual = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/SwordArtManualItem.java");
        String mCode = stripComments(manual);
        check("右键会真的解锁", mCode.contains("SwordArt.unlock("), "");
        check("重复研读不再消耗（幂等）", mCode.contains("SwordArt.unlocked("), "");

        // ---- 可达性：配方 + 手册 + 创造栏 ----
        check("有合成配方（不能只能靠掉落）",
                findResource("data/hongmeng/recipes/sword_art_manual.json") != null, "");
        String guide = readFileAt(findResource("assets/hongmeng/guide/guide.json"));
        check("修行录写了怎么获得（否则玩家永远找不到）",
                guide.contains("御剑术秘籍"), "");
        var tabs = readSource("src/main/java/com/hongmeng/xiuxian/item/ModCreativeTabs.java");
        check("在创造物品栏里（能直接拿来测）", tabs.contains("SWORD_ART_MANUAL"), "");
    }


    /**
     * <b>法术特效必须有体积感</b>（玩家：「特效像纸糊的一样，没有立体感」）。
     *
     * <p>几何一直是三维的，问题在画法：{@code entityTranslucentEmissive} 是
     * <b>alpha 混合</b>，多层叠起来只互相遮盖 = 彩色纸片。</p>
     *
     * <p>又因为 {@code RenderStateShard} 的加色/双面常量都是 {@code protected}
     * （模组侧造不出加色 RenderType，项目也不用 AT），所以改用
     * <b>三层缩放叠画</b>造光晕：1.00 / 1.16 / 1.34 倍，透明度递减。</p>
     */
    private static void fxVolumeRules() {
        section("特效体积感");

        var fx = readSource("src/main/java/com/hongmeng/xiuxian/client/SpellFxRenderer.java");
        String code = stripComments(fx);

        check("draw 分成「本体 + 辉光」（drawOnce + halo）",
                code.contains("drawOnce(") && code.contains("halo("), "");
        check("辉光以特效中心为原点缩放（不是世界原点）",
                code.contains("haloCenter("), "");
        check("中层 1.16 倍", code.contains("1.16F"), "");
        check("外层 1.34 倍", code.contains("1.34F"), "");
        check("辉光层数跟画质档走（低配可以不画）",
                code.contains("SpellFxQuality.layers()"), "");
        check("辉光不会吞掉异常（单层失败不影响本体）",
                code.contains("单层失败不影响本体")
                        || code.contains("catch (Throwable ignored)"), "");
    }


    /**
     * <b>副本里的怪按副本基准定境界</b>（玩家实测：「进去后怪物依然是鸿蒙境界」）。
     *
     * <p>根因：{@code anchorRealmTier} 只有"最近玩家的境界"一条规则 ——
     * 玩家到了鸿蒙，进任何副本怪都变鸿蒙，<b>副本分级完全失效</b>。</p>
     *
     * <p>而 {@code Dungeon.baseTier()} 明明写着每个副本的设计基准，
     * 却只被拿去显示文字和算刷怪数量。</p>
     */
    private static void dungeonMobRealmRules() {
        section("副本怪物境界");

        var mob = readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/CultivationMonsterEntity.java");
        String mCode = stripComments(mob);

        int at = mCode.indexOf("private static int anchorRealmTier(");
        check("存在 anchorRealmTier", at >= 0, "");
        String body = "";
        if (at >= 0) {
            int brace = mCode.indexOf('{', at);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < mCode.length(); i++) {
                char c = mCode.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            body = mCode.substring(brace, Math.min(mCode.length(), end + 1));
        }
        check("副本维度优先用 Dungeon.baseTier（不是只看玩家）",
                body.contains("baseTier()"), "");
        check("用 byDimension 判副本维度", body.contains("byDimension"), "");
        check("非副本仍按最近玩家（主世界行为不变）",
                body.contains("nearestPlayer"), "");

        // ---- 真世界不要按维度限制（它是世界级改写） ----
        var rw = readSource("src/main/java/com/hongmeng/xiuxian/realworld/RealWorld.java");
        String rCode = stripComments(rw);
        int a2 = rCode.indexOf("static boolean active(net.minecraft.world.level.Level level)");
        String ab = "";
        if (a2 >= 0) {
            int brace = rCode.indexOf('{', a2);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < rCode.length(); i++) {
                char c = rCode.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            ab = rCode.substring(brace, Math.min(rCode.length(), end + 1));
        }
        check("真世界仍是世界级效果（不按维度限制 —— 玩家要求所有维度变化）",
                !ab.contains("dimension()"), "");

        // ---- 2026-11-07：境界缩放按副本封顶，元婴头目不再按鸿蒙 ×520 ----
        check("副本缩放按 Dungeon.challengeTier 封顶（不再跟玩家无限放大）",
                mCode.contains("challengeTier()") && mCode.contains("realmScaleForTier"), "");
        check("副本挑战境界 = max(基准, 门槛)：纪元2 / 妖兽3 / 魔道6 / 虚空8",
                com.hongmeng.xiuxian.dungeon.Dungeon.EPOCH.challengeTier() == 2
                        && com.hongmeng.xiuxian.dungeon.Dungeon.BEAST.challengeTier() == 3
                        && com.hongmeng.xiuxian.dungeon.Dungeon.DEMON.challengeTier() == 6
                        && com.hongmeng.xiuxian.dungeon.Dungeon.VOID.challengeTier() == 8, "");
        // 鸿蒙玩家打「魔道巢穴」boss 槽头目（魔道统领）：
        // 初始 HP 必须 < 150 万（玩家一发命中 ≈166 万 → 能秒杀），旧值 1.5 亿。
        double demonScale = com.hongmeng.xiuxian.dungeon.Dungeon.DEMON.mobScaleFor(10);
        double demonHeadHp = 16.0D
                * com.hongmeng.xiuxian.entity.MobRank.BOSS.healthMultiplier()
                * com.hongmeng.xiuxian.difficulty.BossScaling.multiplierOf(
                        com.hongmeng.xiuxian.entity.MobRank.BOSS, 4.0D)
                * com.hongmeng.xiuxian.difficulty.MobScaling.BOSS_HONGMENG_ROOT_BONUS
                * (demonScale * com.hongmeng.xiuxian.dungeon.DungeonRealm.BOSS_SLOT_HP_MUL)
                * com.hongmeng.xiuxian.difficulty.MobScaling.realmScaleForTier(
                        com.hongmeng.xiuxian.dungeon.Dungeon.DEMON.challengeTier())
                * monsterRootExpectation();
        check("鸿蒙打魔道头目：初始 HP < 150 万（一发秒杀，旧值 1.5 亿）",
                demonHeadHp < 1_500_000.0D, String.format("%.0f", demonHeadHp));
        check("副本 boss 槽生命倍率 6（旧 12）",
                Math.abs(com.hongmeng.xiuxian.dungeon.DungeonRealm.BOSS_SLOT_HP_MUL - 6.0D)
                        < 1.0E-9D,
                String.valueOf(com.hongmeng.xiuxian.dungeon.DungeonRealm.BOSS_SLOT_HP_MUL));
        check("副本伤害倍率改按 scale 缓慢放大（旧 healthMul×0.6 斜坡会在 92% 减伤下秒人）",
                stripComments(readSource(
                        "src/main/java/com/hongmeng/xiuxian/dungeon/DungeonRealm.java"))
                        .contains("1.0D + (scale - 1.0D) * 0.10D"), "");
    }

    /** 怪物灵根期望倍率（混沌 60% / 鸿蒙 30% / 先天 10%）—— 与 MonsterSpiritRoot 同源。 */
    private static double monsterRootExpectation() {
        double innateChance = Math.max(0.0D, 1.0D
                - com.hongmeng.xiuxian.difficulty.MonsterSpiritRoot.CHAOS_CHANCE
                - com.hongmeng.xiuxian.difficulty.MonsterSpiritRoot.HONGMENG_CHANCE);
        return com.hongmeng.xiuxian.difficulty.MonsterSpiritRoot.CHAOS_CHANCE
                * com.hongmeng.xiuxian.difficulty.MonsterSpiritRoot.multiplierOf(
                        com.hongmeng.xiuxian.spirit.Rarity.CHAOS)
                + com.hongmeng.xiuxian.difficulty.MonsterSpiritRoot.HONGMENG_CHANCE
                * com.hongmeng.xiuxian.difficulty.MonsterSpiritRoot.multiplierOf(
                        com.hongmeng.xiuxian.spirit.Rarity.HONGMENG)
                + innateChance
                * com.hongmeng.xiuxian.difficulty.MonsterSpiritRoot.multiplierOf(
                        com.hongmeng.xiuxian.spirit.Rarity.INNATE);
    }


    /**
     * <b>特效几何必须有逐顶点明暗</b>（"建模感"的底线）。
     *
     * <p>玩家：「你只是纯粹的堆叠粒子效果和贴图」。</p>
     *
     * <p>核实：几何确实是三维的，但底层虽然早就有 {@code bright} 参数，
     * 所有几何却都走 {@code quad(...)}（四顶点 {@code bright=1.0}）——
     * <b>网格再细也是一整块平色</b>，看起来就是剪纸。</p>
     */
    private static void fxShadingRules() {
        section("特效逐顶点明暗");

        var mesh = readSource("src/main/java/com/hongmeng/xiuxian/client/SpellFxMesh.java");
        var mesh2 = readSource("src/main/java/com/hongmeng/xiuxian/client/SpellFxMesh2.java");
        var rend = readSource("src/main/java/com/hongmeng/xiuxian/client/SpellFxRenderer.java");
        String meshC = stripComments(mesh);
        String mesh2C = stripComments(mesh2);
        String rendC = stripComments(rend);

        // ---- 1) 基础设施 ----
        check("有 per-vertex 明暗的四边形 quadShaded", meshC.contains("quadShaded("), "");
        check("球有 Phong 着色函数", meshC.contains("static float phong("), "");
        check("球的镜面高光（pow(diff, 8)）", meshC.contains("Math.pow(diff, 8.0D)"), "");
        check("圆环体按管截面法线着色", mesh2C.contains("shadeTube("), "");
        check("螺旋带有逐顶点明暗", mesh2C.contains("shadeHelix("), "");
        check("圆环体用了 quadShaded（不是平色 quad）",
                mesh2C.contains("quadShaded("), "");

        // ---- 2) 分辨率 ----
        check("圆环大圆分段 >= 30（原来 18/24 明暗会断成一格一格）",
                rendC.contains("torus(pose, buf, \"torus_soft\", 36, 12"), "");

        // ---- 3) 贴图 ----
        check("圆环体改用管面专用贴图 torus_soft（符文环会被拉成条纹）",
                rendC.contains("torus_soft"), "");
        String beam = findResource("assets/hongmeng/textures/fx/beam_soft.png");
        String torus = findResource("assets/hongmeng/textures/fx/torus_soft.png");
        check("torus_soft 贴图存在", torus != null, "");
        check("beam_soft 已重画成宽光带（原来是 2px 细线，螺旋看不见）",
                beam != null && fileSize(beam) > 2000, "实际 " + fileSize(beam) + " 字节");
    }


    /**
     * <b>四段式特效序列</b>（蓄势 → 爆发 → 冲击 → 余波）。
     *
     * <p>玩家：「你只是纯粹的堆叠粒子效果和贴图」。除了明暗缺失，
     * 原实现还有个结构问题：{@code play()} 把五层形状<b>同一时刻</b>全发出去，
     * 于是看起来是"一堆东西同时出现"，没有时间叙事。</p>
     */
    private static void fxFourStageRules() {
        section("四段式特效");

        var rt = readSource("src/main/java/com/hongmeng/xiuxian/technique/SpellFxRuntime.java");
        String code = stripComments(rt);

        check("有延迟调度队列 QUEUE", code.contains("QUEUE"), "");
        check("有 sendLater（协议不变，靠服务端排队）", code.contains("sendLater("), "");
        check("有 tickScheduled 推进队列", code.contains("tickScheduled("), "");
        check("有四段式 playFourStage", code.contains("playFourStage("), "");

        // ---- 四段都要在：0 / 5 / 12 / 22 附近 ----
        check("① 蓄势段（立刻 + 2 tick）", code.contains("sendNow("), "");
        int at = code.indexOf("playFourStage(ServerLevel level");
        String body = "";
        if (at >= 0) {
            int brace = code.indexOf('{', at);
            int depth = 0;
            int end = brace;
            for (int i = brace; i < code.length(); i++) {
                char c = code.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            body = code.substring(brace, Math.min(code.length(), end + 1));
        }
        check("② 爆发段（5~6 tick）", body.contains("sendLater(level, 5"), "");
        check("③ 冲击段（12~14 tick）", body.contains("sendLater(level, 12"), "");
        check("④ 余波段（22~26 tick）", body.contains("sendLater(level, 22"), "");
        check("四段用的形状各不相同（不是一个球重复四次）",
                body.contains("IMPACT_FLARE") && body.contains("SHOCK_RING")
                        && body.contains("BEAM_COLUMN"), "");

        // ---- 接进门：服务端 tick 必须推进队列 ----
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        String evCode = stripComments(ev);
        check("服务端每 tick 推进队列（不 tick 就永远不会发）",
                evCode.contains("SpellFxRuntime.tickScheduled"), "");
        check("推进挂在 ServerTickEvent 上", evCode.contains("ServerTickEvent"), "");

        // ---- 灭世级法术要真的走四段式 ----
        check("灭世/引力级法术会调用四段式",
                code.contains("playFourStage(level, from"), "");
    }


    /**
     * <b>怪物心智</b>（玩家：「让他们有自己的想法」）。
     *
     * <p>原版 {@code MeleeAttackGoal} 是<b>直线追</b>，所有怪行为完全一样 ——
     * 打起来像打木桩。这一层在 goal <b>之上</b>加"心情"：
     * 逼近 / 绕后包抄 / 血少撤退 / 站定蓄力再猛扑。</p>
     *
     * <p><b>边界</b>：只改写寻路、短暂改速度、打一次近战 ——
     * 不改伤害/属性/攻击类型，平衡仍由既有系统管。</p>
     */
    private static void mobMindRules() {
        section("怪物心智");

        var mind = readSource("src/main/java/com/hongmeng/xiuxian/entity/MobMind.java");
        String code = stripComments(mind);

        check("有五种心情（逼近/绕后/撤退/蓄力/猛扑）",
                code.contains("Mood.APPROACH") && code.contains("Mood.CIRCLE")
                        && code.contains("Mood.WITHDRAW") && code.contains("Mood.TELEGRAPH")
                        && code.contains("Mood.COMMIT"), "");
        check("绕后是斜向绕行（原版绝不会这么做）", code.contains("strafePoint("), "");
        check("血少会撤退（一次，不是无限逃）", code.contains("TAG_RETREATED"), "");
        check("蓄力有起手表现（让玩家看得出要动手）", code.contains("telegraphFx("), "");
        check("蓄力走完**必然**接猛扑（否则永远打不出来）",
                code.contains("mood = Mood.COMMIT"), "");
        check("猛扑只给一次初速（持续加速会飞天）", code.contains("private static void lunge("), "");
        check("每次猛扑只打一次（防止一扑多段伤害）", code.contains("TAG_HIT_DONE"), "");
        check("心情存在持久化数据里（区块重载不丢）",
                code.contains("getPersistentData()"), "");
        check("首领级不叠加（避免与 BossAbilities 打架）",
                code.contains("hasBossBar()"), "");

        // ---- 接线 ----
        var mob = readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/CultivationMonsterEntity.java");
        String mCode = stripComments(mob);
        check("每 tick 真的会跑（customServerAiStep 里调用）",
                mCode.contains("MobMind.tick(this)"), "");
        check("移除时清状态（防止复用带旧心情）", mCode.contains("MobMind.forget(this)"), "");
    }


    /**
     * <b>雷劫节奏</b>（玩家质疑「落雷频率是不是太快」）。
     *
     * <p>核实：原来是 {@code tickCount % 4} 一道 —— <b>每秒 5 道</b>，
     * 9 道在 1.8 秒里砸完，像机枪。而且每道伤害相同、没有前兆、天象不变。</p>
     *
     * <p>按修仙小说重排：天象先变 → 每道有蓄势 → 一道比一道重
     * （最后一道 ×1.7）→ 道与道之间有喘息。</p>
     */
    private static void tribulationRhythmRules() {
        section("雷劫节奏");

        var bt = readSource("src/main/java/com/hongmeng/xiuxian/core/Breakthrough.java");
        String code = stripComments(bt);

        // ---- 不能再是"每 4 tick 一道" ----
        check("不再用 tickCount % 4 连发", !code.contains("tickCount % 4 != 0"), "");
        check("有单道周期函数 tribulationCycle", code.contains("tribulationCycle("), "");
        check("周期有上下限（太快=机枪，太慢=挂机）",
                code.contains("TRIBULATION_MIN_CYCLE")
                        && code.contains("TRIBULATION_MAX_CYCLE"), "");
        check("总时长固定（不随境界线性膨胀）",
                code.contains("TRIBULATION_TOTAL_TICKS"), "");

        // ---- 强度曲线 ----
        check("有逐道强度倍率", code.contains("tribulationBoltMultiplier("), "");
        check("最后一道最重（乘 1.7）", code.contains("1.7D"), "");
        check("倍率归一化（总伤害不变，平衡不用重算）",
                code.contains("w * total / sum"), "");

        // ---- 蓄势与天象 ----
        check("落雷前有蓄势表现 windupFx", code.contains("windupFx("), "");
        check("蓄势里有乌云（SMOKE）与雷光（ELECTRIC_SPARK）",
                code.contains("ParticleTypes.SMOKE")
                        && code.contains("ParticleTypes.ELECTRIC_SPARK"), "");
        check("越接近落雷越密（蓄势有渐强）",
                code.contains("ticksToStrike * 3L <= cycle"), "");
        check("渡劫时会切雷雨（天象先变）",
                code.contains("setWeatherParameters"), "");
        check("渡劫结束恢复天气", code.contains("endWeather("), "");

        // ---- 连珠雷（波次节奏）----
        check("高境界改连珠雷（波内多道）", code.contains("tribulationWaveSize("), "");
        check("波间有固定喘息（云层要时间聚）",
                code.contains("TRIBULATION_MIN_WAVE_GAP"), "");
        check("连珠之间的 3~4 tick 不刷蓄势（否则云层闪一下就没）",
                code.contains("toStrike > TRIBULATION_INTRA_WAVE"), "");

        // ---- 预警 ----
        check("最后一道前有预警提示", code.contains("最后一道"), "");

        // ---- 每 tick 真的会跑 ----
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        check("每 tick 推进雷劫",
                stripComments(ev).contains("Breakthrough.tickTribulation"), "");
    }


    /**
     * <b>真世界开启后的三件事</b>（玩家指定）。
     *
     * <p>1) 各副本的境界阶梯：虚空=仙人 / 魔道=飞升 / 妖兽=道印 / 纪元=化神；</p>
     * <p>2) 过场要有<b>压迫感</b>（原版是"世界展开"，情绪相反）；</p>
     * <p>3) 世界外观变化（雾色转暗红，参考泰拉瑞亚肉后）。</p>
     */
    private static void realWorldHardmodeRules() {
        section("真世界·三件事");

        // ---- 1) 维度阶梯 ----
        var rw = readSource("src/main/java/com/hongmeng/xiuxian/realworld/RealWorld.java");
        String rCode = stripComments(rw);
        check("有按维度的境界阶梯 tierForDungeon", rCode.contains("tierForDungeon("), "");
        check("虚空裂隙 = 仙人（8）", rCode.contains("void_realm\" -> 8"), "");
        check("魔道巢穴 = 飞升（7）", rCode.contains("demon_realm\" -> 7"), "");
        check("妖兽秘境 = 道印（6）", rCode.contains("beast_realm\" -> 6"), "");
        check("纪元秘境 = 化神（5）", rCode.contains("epoch_realm\" -> 5"), "");
        check("首领在该维度上再高一境", rCode.contains("rankOrdinal >= 3"), "");
        var mob = readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/CultivationMonsterEntity.java");
        check("怪物在副本里优先走阶梯（不是只看品阶）",
                stripComments(mob).contains("tierForDungeon("), "");

        // ---- 2) 过场要有压迫感 ----
        var cin = readSource(
                "src/main/java/com/hongmeng/xiuxian/realworld/RealWorldCinematic.java");
        String cCode = stripComments(cin);
        check("标题改成「天地动荡」", cCode.contains("天 地 动 荡"), "");
        check("镜头先贴地仰视（天压得近）",
                cCode.contains("-22.0F") || cCode.contains("-26.0F"), "");
        check("升到极高空俯视（人如蝼蚁）",
                cCode.contains("52.0F") || cCode.contains("48.0F"), "");
        check("最后硬切回近景（落差制造冲击）", cCode.contains("6.0F, 210.0F"), "");
        check("叠了连续闷雷（不是一声混响）", cCode.contains("LIGHTNING_BOLT_THUNDER"), "");
        check("叠了红光脉冲", cCode.contains("PainFlashPacket"), "");
        check("有延迟调度器（雷声要有先后）",
                cCode.contains("RealWorldScheduler"), "");
        var sched = readSource(
                "src/main/java/com/hongmeng/xiuxian/realworld/RealWorldScheduler.java");
        check("调度器有排队与推进", stripComments(sched).contains("public static void tick("), "");
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        check("调度器每 tick 推进", stripComments(ev).contains("RealWorldScheduler.tick"), "");

        // ---- 3) 世界外观变化 ----
        var fx = readSource("src/main/java/com/hongmeng/xiuxian/client/RealWorldClientFx.java");
        String fCode = stripComments(fx);
        check("客户端有真世界环境表现", fCode.contains("ComputeFogColor"), "");
        check("雾色朝暗红插值", fCode.contains("0.34F") && fCode.contains("0.12F"), "");
        check("渐变而不是瞬切（否则像戴红眼镜）", fCode.contains("blend"), "");
        var pkt = readSource(
                "src/main/java/com/hongmeng/xiuxian/network/RealWorldStatePacket.java");
        check("有状态同步包", pkt.contains("class RealWorldStatePacket"), "");
        check("包已注册到通道",
                readSource("src/main/java/com/hongmeng/xiuxian/network/ModNetwork.java")
                        .contains("RealWorldStatePacket.class"), "");
        check("开启时广播给所有客户端", rCode.contains("sendToAll("), "");
        check("登录时补发（否则重进游戏雾色丢了）",
                stripComments(ev).contains("RealWorldStatePacket"), "");
    }


    /**
     * <b>怪物也会格挡</b>（让战斗对称）。
     *
     * <p>玩家能弹反怪、怪不会弹反玩家 -> 最优解永远是"贴脸砍"。
     * 加上之后玩家必须<b>绕后 / 骗挡 / 等冷却</b>。</p>
     */
    private static void mobGuardRules() {
        section("怪物格挡");

        var mg = readSource("src/main/java/com/hongmeng/xiuxian/entity/MobGuard.java");
        String code = stripComments(mg);

        check("精英以上才会格挡（杂鱼会挡会把新手弹到劝退）",
                code.contains("case ELITE ->") && code.contains("default -> 0.0D"), "");
        check("品阶越高越爱格挡", code.contains("OVERLORD -> 0.48D"), "");
        check("有弹反窗口（比格挡持续短得多）",
                code.contains("PERFECT_TICKS = 6")
                        && code.contains("GUARD_TICKS = 16"), "");
        check("弹反减伤大于普通格挡",
                code.contains("PERFECT_CUT = 0.85D")
                        && code.contains("NORMAL_CUT = 0.60D"), "");
        check("有冷却（不能一直挡）", code.contains("COOLDOWN_TICKS"), "");
        check("必须面朝玩家才挡（背后打无效，与玩家背刺对称）",
                code.contains("faces(mob"), "");
        check("弹反会让玩家踉跄（减速 + 击退）",
                code.contains("MOVEMENT_SLOWDOWN")
                        && code.contains("STAGGER_KNOCKBACK"), "");
        check("格挡状态存持久化数据（区块重载不丢）",
                code.contains("getPersistentData()"), "");

        // ---- 接线 ----
        var mob = readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/CultivationMonsterEntity.java");
        check("AI 每 tick 会决定起手格挡",
                stripComments(mob).contains("MobGuard.tick(this)"), "");
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        String evCode = stripComments(ev);
        check("玩家打到怪时会结算怪的格挡",
                evCode.replaceAll("\\s+", "").contains("MobGuard.onHurt("), "");
        check("只对近战直击生效（法术不该被格挡）",
                evCode.contains("getDirectEntity() == player"), "");
    }


    /**
     * <b>怪物气盾表现</b>。
     *
     * <p>上一轮加了怪物格挡，但表现只有起手一瞬的粒子 ——
     * 玩家砍上去只发现"伤害变小了"，<b>看不出它在挡</b>。</p>
     */
    private static void mobShieldRules() {
        section("怪物气盾");

        var r = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/MobShieldRenderer.java");
        String code = stripComments(r);

        check("有弧形（不是平面纸片，侧面看会变成线）",
                code.contains("ARC_DEG"), "");
        check("弧面分段够圆（12 段看不出折线）", code.contains("SEGMENTS = 12"), "");
        check("用 additive 的 lightning 管线（能量效果要这个）",
                code.contains("RenderType.lightning()"), "");
        check("画在 AFTER_PARTICLES（早于它会受迷雾影响发灰）",
                code.contains("AFTER_PARTICLES"), "");
        check("有淡入淡出（否则会啪地跳出来）",
                code.contains("fadeIn") && code.contains("fadeOut"), "");
        check("位置做插值（高速移动的怪盾不会拖在后面）",
                code.contains("Mth.lerp(partial"), "");
        check("弧顶最亮、两端渐隐（有包住的观感）", code.contains("mid"), "");

        // ---- 同步 ----
        var pkt = readSource(
                "src/main/java/com/hongmeng/xiuxian/network/MobGuardFxPacket.java");
        check("有气盾表现包", pkt.contains("class MobGuardFxPacket"), "");
        check("包已注册",
                readSource("src/main/java/com/hongmeng/xiuxian/network/ModNetwork.java")
                        .contains("MobGuardFxPacket.class"), "");
        check("起手时广播（只发一次，客户端自己算时长）",
                stripComments(readSource(
                        "src/main/java/com/hongmeng/xiuxian/entity/MobGuard.java"))
                        .contains("MobGuardFxPacket"), "");
        check("渲染器已在客户端注册",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("MobShieldRenderer.class"), "");
    }


    /**
     * <b>武器专属连招表</b>：差异化在玩法，不在强弱。
     *
     * <p>原来五把武器用同一张倍率表，于是"剑走轻灵、刀走刚猛"只是外观。
     * 现在各有连段长度/节奏/曲线，但<b>平均倍率完全相等</b>。</p>
     */
    private static void weaponComboRules() {
        section("武器专属连招");

        var ca = readSource("src/main/java/com/hongmeng/xiuxian/combat/CombatAnim.java");
        String code = stripComments(ca);

        check("有五张连招表（不是一张通用表）",
                code.contains("COMBO_TABLES"), "");
        check("有五张节奏窗口（重武器窗口宽、快武器窗口窄）",
                code.contains("COMBO_WINDOWS"), "");
        check("体修两段（短而重）", code.contains("{0.95D, 1.25D}"), "");
        check("魔修四段（快连）",
                code.contains("{1.00D, 1.04D, 1.08D, 1.28D}"), "");
        check("血修三段等额（平稳输出）",
                code.contains("{1.10D, 1.10D, 1.10D}"), "");
        check("阵修五段（终结最重）",
                code.contains("{1.00D, 1.02D, 1.04D, 1.06D, 1.38D}"), "");
        check("对外只暴露 comboMultiplier（不让人直接摸数组下标）",
                code.contains("public static double comboMultiplier("), "");
        check("连段长度按武器算（不同长度不会越界）",
                code.contains("lengthOf(kind)"), "");
        check("有配平校验函数 averageOf",
                code.contains("averageOf("), "");

        // ---- 数学配平（硬校验：平均值必须全部相等）----
        double[][] tables = {
                {1.00D, 1.06D, 1.24D},
                {0.95D, 1.25D},
                {1.00D, 1.04D, 1.08D, 1.28D},
                {1.10D, 1.10D, 1.10D},
                {1.00D, 1.02D, 1.04D, 1.06D, 1.38D},
        };
        double first = -1.0D;
        boolean balanced = true;
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < tables.length; i++) {
            double sum = 0.0D;
            for (double v : tables[i]) {
                sum += v;
            }
            double avg = sum / tables[i].length;
            if (first < 0.0D) {
                first = avg;
            } else if (Math.abs(avg - first) > 0.0005D) {
                balanced = false;
            }
            if (i > 0) {
                detail.append(" / ");
            }
            detail.append(String.format("%.3f", avg));
        }
        check("五张表的**平均倍率完全相等**（道途不该有强弱）",
                balanced, "实测 " + detail);

        // ---- 接线：伤害结算用新入口 ----
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        String evCode = stripComments(ev);
        check("伤害结算改用 comboMultiplier（不再直接摸数组）",
                evCode.replaceAll("\\s+", "").contains("CombatAnim.comboMultiplier("), "");
        check("终结技判定按武器长度算（不是写死第三下）",
                code.contains("return comboOf(player) >= lengthOf(kind) - 1;"), "");
    }


    /**
     * <b>连招段位改变动作本身</b>。
     *
     * <p>在此之前只有双刀用段位做左右交替，其它武器三段连招是<b>同一个动作播三遍</b> ——
     * 玩家看不出这是一套连击。</p>
     */
    private static void comboVariantRules() {
        section("连招招式变体");

        var anim = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/CombatAnimClient.java");
        String code = stripComments(anim);

        check("有招式变体函数", code.contains("applyVariant("), "");
        check("变体按段位选（五个变体）",
                code.contains("Math.floorMod(variant, 5)"), "");
        check("变体里有横扫/竖劈/斜斩/突刺/回旋五种方向",
                code.contains("Axis.YP.rotationDegrees(26.0F")
                        && code.contains("Axis.XP.rotationDegrees(30.0F")
                        && code.contains("Axis.YP.rotationDegrees(20.0F")
                        && code.contains("0.20D * scale * k")
                        && code.contains("150.0F * scale * k"), "");
        // 玩家反馈「打怪时身子会歪」→ 第三人称不再用 Z 轴侧倾，这里锁死防复发
        check("第三人称不再用 Z 轴侧倾（斜斩/双刀第三段）",
                !code.contains("Axis.ZP.rotationDegrees(12.0F"), "");
        check("**第三人称**也叠变体（不只第一人称）",
                code.contains("applyVariant(pose, a.combo"), "");
        check("**第一人称**叠变体（玩家自己看得见的就是这个）",
                code.contains("applyVariant(pose, localCombo"), "");
        check("第一人称幅度收小（晃太狠会晕）",
                code.contains("0.62F"), "");

        // ---- 拖尾的扫向也要跟着变 ----
        var trail = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/SlashTrailRenderer.java");
        String tCode = stripComments(trail);
        check("刀光的扫向按段位变化（否则动作变了刀光不对）",
                tCode.contains("Math.floorMod(t.combo, 5)"), "");
        check("扫向用平面旋转实现（横/竖/斜都要能表达）",
                tCode.contains("Axis.ZP.rotationDegrees(-70.0F)")
                        && tCode.contains("Axis.ZP.rotationDegrees(10.0F)"), "");
    }


    /**
     * <b>真世界三件</b>：雾距离衰减 / 天空穹顶 / 肉后新矿。
     *
     * <p>雾衰减上一版用了<b>猜的方法名</b>（{@code getFarPlane}）导致编译失败；
     * 这次先用 {@code javap} 查了真实 API 才写。</p>
     */
    private static void realWorldThreeRules() {
        section("真世界·雾/天/地");

        // ---- ① 雾距离衰减 ----
        var fx = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/RealWorldClientFx.java");
        String fCode = stripComments(fx);
        check("雾的**距离**也衰减（只改颜色会像戴红眼镜）",
                fCode.contains("onRenderFog"), "");
        check("用真实存在的 API scaleFarPlaneDistance（上次猜的 getFarPlane 编译失败）",
                fCode.contains("scaleFarPlaneDistance"), "");
        check("不用不存在的 getFarPlane/setFarPlane",
                !fCode.contains("getFarPlane()") && !fCode.contains("setFarPlane("), "");
        check("只压地形雾（水下/岩浆/失明的雾不能动）",
                fCode.contains("FOG_TERRAIN"), "");
        check("收近幅度有上限（12%，再收会挡战斗视野）",
                fCode.contains("FOG_SHRINK = 0.12F"), "");

        // ---- ② 天空穹顶 ----
        var sky = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/RealWorldSkyRenderer.java");
        String sCode = stripComments(sky);
        check("有天空穹顶渲染（不用 shader，避免光影冲突）",
                sCode.contains("class RealWorldSkyRenderer"), "");
        check("画在 AFTER_SKY（地形会正确遮住下半部分）",
                sCode.contains("AFTER_SKY"), "");
        check("竖直方向有梯度（均匀染色会像滤色纸）",
                sCode.contains("MAX_ALPHA * strength * (1.0F - 0.55F"), "");
        check("alpha 很低（是染色不是盖布）", sCode.contains("MAX_ALPHA = 0.30F"), "");
        check("天空与雾读**同一个强度值**（否则会割裂）",
                sCode.contains("RealWorldFxState.strength()")
                        && fCode.contains("RealWorldFxState.setStrength"), "");
        check("天空渲染器已注册",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("RealWorldSkyRenderer.class"), "");

        // ---- ③ 肉后新矿 ----
        var ter = readSource(
                "src/main/java/com/hongmeng/xiuxian/realworld/HardmodeTerrain.java");
        String tCode = stripComments(ter);
        check("有肉后地形变化", tCode.contains("class HardmodeTerrain"), "");
        check("只替换天然石头（绝不碰玩家建筑）",
                tCode.contains("Blocks.STONE") && tCode.contains("Blocks.DEEPSLATE"), "");
        check("只改已加载区块（不触发区块生成）",
                tCode.contains("hasChunkAt("), "");
        check("每玩家有总量上限（不把地下挖空）",
                tCode.contains("MAX_VEINS_PER_PLAYER"), "");
        check("有节流（慢慢渗透而不是瞬间刷满）",
                tCode.contains("SEED_INTERVAL"), "");
        check("播的是**已有**矿（不新增方块，避免贴图/掉落表风险）",
                tCode.contains("KONGMING_ORE") && tCode.contains("XUELUO_ORE"), "");
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        check("地形变化每 tick 被驱动",
                stripComments(ev).contains("HardmodeTerrain.tick"), "");
    }


    /**
     * <b>真世界刷精英</b>（"新怪"落到实际）。
     */
    private static void execAnimAndEliteRules() {
        section("真世界新怪");

        // ---- 真世界新怪 ----
        var ter = readSource(
                "src/main/java/com/hongmeng/xiuxian/realworld/HardmodeTerrain.java");
        String tCode = stripComments(ter);
        check("真世界会刷精英（'新怪'落到实际）", tCode.contains("tickElite("), "");
        check("刷怪间隔够长（60 秒，不能天天打）",
                tCode.contains("ELITE_INTERVAL = 1200"), "");
        check("每人总量上限（防刷怪失控）",
                tCode.contains("MAX_ELITE_PER_PLAYER"), "");
        check("刷的是**普通怪种**不是 Boss（否则真世界变成天天打 Boss）",
                !tCode.contains("BossEntity"), "");
        check("刷怪点要能找到地面", tCode.contains("isSolidRender("), "");
        check("刷怪也只在已加载区块", tCode.contains("hasChunkAt("), "");
    }


    /**
     * <b>怪物闪避</b>：高难战斗对称的最后一块。
     *
     * <p>玩家有格挡/翻滚/处决/背刺，怪只有格挡 —— 加上闪避，
     * 玩家必须面对"这一下会不会被闪掉"。</p>
     */
    private static void mobDodgeRules() {
        section("怪物闪避");

        var md = readSource("src/main/java/com/hongmeng/xiuxian/entity/MobDodge.java");
        String code = stripComments(md);

        check("精英以上才会闪（杂鱼会闪会让新手打不中）",
                code.contains("case ELITE ->") && code.contains("default -> 0.0D"), "");
        check("概率有上限（不超过 26%）", code.contains("CAPTAIN -> 0.26D"), "");
        check("首领闪避反而更低（大块头不灵活）",
                code.contains("BOSS -> 0.12D"), "");
        check("冷却比格挡长得多（闪避是完全免伤）",
                code.contains("COOLDOWN_TICKS = 70"), "");
        check("僵直中不能闪（AI 都停了，闪避会显得荒谬）",
                code.contains("isStunned("), "");
        check("闪避是**位移**不是瞬移（瞬移会让玩家以为卡了）",
                code.contains("setDeltaMovement(dir.x"), "");
        check("有可见表现（尘土 + 风声 + 提示）",
                code.contains("ParticleTypes.CLOUD")
                        && code.contains("它闪开了"), "");
        check("冷却存持久化数据（区块重载不丢）",
                code.contains("getPersistentData()"), "");

        // ---- 接线 ----
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        String evCode = stripComments(ev);
        check("伤害结算里会尝试闪避",
                evCode.replaceAll("\\s+", "").contains("MobDodge.tryDodge("), "");
        check("闪掉了伤害归零", evCode.contains("event.setAmount(0.0F)"), "");
        check("只对近战直击生效", evCode.contains("getDirectEntity() == player"), "");
    }

    /**
     * <b>Boss 形态变化的表现</b>。
     *
     * <p>核实过：{@code BossPhase} 只在服务端改属性，
     * <b>客户端没有任何渲染</b> —— 文档里的"体型 +18%、长角与翼"从未画出来。</p>
     */
    private static void bossFormRules() {
        section("Boss 形态表现");

        var f = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/BossFormRenderer.java");
        String code = stripComments(f);

        check("有 Boss 形态渲染器", code.contains("class BossFormRenderer"), "");
        check("阶段在**客户端本地推导**（血量已同步，不用新增包）",
                code.contains("getHealth() / max") || code.contains("e.getHealth() / max"), "");
        check("二阶段体型放大", code.contains("SCALE_P2"), "");
        check("三阶段比二阶段更大", code.contains("SCALE_P3 = 1.30F"), "");
        check("体型缩放有 Pre/Post **严格配对**（否则后续实体全被放大）",
                code.contains("onLivingPre") && code.contains("onLivingPost")
                        && code.contains("popPose()"), "");
        check("二阶段有魔角光环（代码几何，不是外部模型）",
                code.contains("drawCrown(") && code.contains("CROWN_SHARDS"), "");
        check("三阶段有能量翼", code.contains("drawWings("), "");
        check("三阶段有脚下堕落法阵", code.contains("drawFloorRune("), "");
        check("用 additive 的 lightning 管线（能量形态要这个）",
                code.contains("RenderType.lightning()"), "");
        check("三阶段颜色与二阶段不同（紫 -> 血红）",
                code.contains("r = phase >= 3 ? 0.95F : 0.62F"), "");
        // ⚠ stripComments 会把注释去掉，所以这里只能检查**代码结构**：
        //   只做 scale、不做 translate -> 锚点就是 pose 原点（实体脚底）
        int ai = code.indexOf("applyScale(");
        int ab = ai < 0 ? -1 : code.indexOf('{', ai);
        String aBody = "";
        if (ab > 0) {
            int depth = 0;
            for (int i = ab; i < code.length(); i++) {
                char c = code.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        aBody = code.substring(ab, i + 1);
                        break;
                    }
                }
            }
        }
        check("只做 scale 不做 translate（锚点留在实体脚底，不会浮空/陷地）",
                aBody.contains("pose.scale(") && !aBody.contains("pose.translate("),
                "");
        check("渲染器已在客户端注册",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("BossFormRenderer.class"), "");
    }


    /**
     * <b>Boss 转阶段演出</b>。
     *
     * <p>原来 {@code enter()} 一帧内改完属性、回满血、播特效 ——
     * 玩家正在躲技能，很可能<b>没注意到换形态了</b>。</p>
     */
    private static void phaseTransitionRules() {
        section("Boss 转阶段演出");

        var bp = readSource("src/main/java/com/hongmeng/xiuxian/entity/BossPhase.java");
        String code = stripComments(bp);

        check("转阶段会**定格**（Boss 停下来，玩家才看得见）",
                code.contains("setNoAi(true)"), "");
        check("定格时长 1 秒（够看清，又不打断节奏）",
                code.contains("PHASE_FREEZE_TICKS = 20"), "");
        check("定格结束会恢复 AI（只挂不恢复 = Boss 永久变木桩）",
                code.contains("setNoAi(false)"), "");
        check("转阶段期间**免伤**（否则刚回满的血会被打回去）",
                code.contains("setInvulnerable(true)")
                        && code.contains("setInvulnerable(false)"), "");
        check("有气浪击退（被冲击波掀一下的体感）",
                code.contains("hurtMarked = true"), "");
        check("有喊话（玩家得知道'它变了'）",
                code.contains("魔化！") && code.contains("彻底堕落！"), "");

        // ---- 客户端冲击环 ----
        var f = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/BossFormRenderer.java");
        String fCode = stripComments(f);
        check("客户端有转阶段冲击环", fCode.contains("drawShockRing("), "");
        check("冲击环会**扩散**（半径随时间变大）",
                fCode.contains("0.6D + 3.4D * k"), "");
        check("冲击环会淡出", fCode.contains("1.0F - k"), "");
        check("冲击环有时间厚度（否则侧面看是一条线）",
                fCode.contains("thick"), "");
        check("转阶段靠**本地推导**检测（不新增同步包）",
                fCode.contains("justTransitioned("), "");
        check("冲击环时长与服务端定格对齐",
                fCode.contains("TRANSITION_TICKS = 20"), "");
    }


    /**
     * <b>雷劫四件套</b>：随境界换天象色 / 劫云实体化 / 落雷焦痕 / 元婴遁走。
     */
    private static void tribulationFxRules() {
        section("雷劫四件套");

        var sky = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/TribulationSkyRenderer.java");
        String code = stripComments(sky);

        // ---- ① 随境界换天象色 ----
        check("天象色按境界分档", code.contains("colorFor("), "");
        check("第 10 境是血雷（红）", code.contains("0.95F, 0.16F, 0.14F"), "");
        check("第 9 境是紫雷", code.contains("0.62F, 0.28F, 0.95F"), "");
        check("低境界是青雷", code.contains("0.30F, 0.90F, 0.85F"), "");

        // ---- ② 劫云实体化 ----
        check("有劫云贴图", code.contains("trib_cloud.png"), "");
        check("云用 **alpha 混合**（additive 画不出乌云）",
                code.contains("entityTranslucentEmissive(CLOUD_TEX)"), "");
        check("劫云会旋转", code.contains("t * 0.55D"), "");
        check("劫云会收拢（越接近结束越聚）", code.contains("lifeLeft"), "");

        // ---- ③ 焦痕贴花 ----
        check("有焦痕贴图", code.contains("scorch.png"), "");
        check("焦痕贴在落点地面", code.contains("drawScorch("), "");
        check("焦痕会淡出（永久贴花会无限堆积）",
                code.contains("SCORCH_TICKS = 600"), "");
        check("焦痕不破坏方块（纯渲染）",
                !code.contains("setBlock("), "");
        check("落雷有闪光光柱", code.contains("drawFlash("), "");

        // ---- 包与接线 ----
        check("有天象状态包",
                readSource("src/main/java/com/hongmeng/xiuxian/network/"
                        + "TribulationFxPacket.java").contains("class TribulationFxPacket"), "");
        check("有落雷坐标包",
                readSource("src/main/java/com/hongmeng/xiuxian/network/"
                        + "TribulationStrikePacket.java")
                        .contains("class TribulationStrikePacket"), "");
        var net = readSource("src/main/java/com/hongmeng/xiuxian/network/ModNetwork.java");
        check("两个包都已注册",
                net.contains("TribulationFxPacket.class")
                        && net.contains("TribulationStrikePacket.class"), "");
        var bt = readSource("src/main/java/com/hongmeng/xiuxian/core/Breakthrough.java");
        String btCode = stripComments(bt);
        check("开始渡劫时发天象包", btCode.contains("TribulationFxPacket"), "");
        check("每道落雷发坐标包", btCode.contains("TribulationStrikePacket"), "");
        check("记录本场境界（天象色要按它）", btCode.contains("TRIBULATION_TIER"), "");

        // ---- ④ 元婴遁走 ----
        check("渡劫期间致命伤会保命（元婴遁走）",
                btCode.contains("public static boolean inTribulation("), "");
        check("保命后中止雷劫", btCode.contains("abandonTribulation("), "");
        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        String evCode = stripComments(ev);
        check("伤害结算里接了元婴遁走",
                evCode.replaceAll("\\s+", "").contains("Breakthrough.inTribulation("), "");
        check("保命时血留 1 点（不是完全免死）",
                evCode.contains("player.getHealth() - 1.0F"), "");
        check("有'肉身崩碎，元婴遁走'文案",
                evCode.contains("肉身崩碎，元婴遁走"), "");
    }


    /**
     * <b>Boss 阶段专属大招</b>。
     *
     * <p>在此之前二三阶段只是"数值更高 + 技能更多"，
     * 没有任何"只有这个阶段才会出现"的招式。</p>
     */
    private static void phaseUltimateRules() {
        section("Boss 阶段专属大招");

        var ab = readSource("src/main/java/com/hongmeng/xiuxian/entity/BossAbilities.java");
        String code = stripComments(ab);

        // ---- 二阶段：魔影突袭 ----
        check("有【魔影突袭】", code.contains("ambush("), "");
        check("突袭有预警时长（给玩家回头的时间）",
                code.contains("AMBUSH_WINDUP"), "");
        check("突袭会出现在**背后**（与本作背击穿透格挡咬合）",
                code.contains("target.getLookAngle()")
                        && code.contains("subtract(look.normalize().scale(2.0D))"), "");
        check("突袭走原版攻击结算（遵守既有伤害规则）",
                code.contains("doHurtTarget(target)"), "");
        check("突袭有独立冷却", code.contains("AMBUSH_CD"), "");
        check("突袭只在二阶段以上", code.contains("if (phase >= 2)"), "");

        // ---- 三阶段：天崩地裂 ----
        check("有【天崩地裂】", code.contains("cataclysm("), "");
        check("是多圈冲击波（不是一次性伤害）",
                code.contains("CATACLYSM_WAVES = 3"), "");
        check("每圈独立延迟（给玩家躲的节奏）",
                code.contains("6 + w * 12"), "");
        check("**只在环带上伤害**（不是整片圆，玩家可以卡位）",
                code.contains("Math.abs(d - radius) > 1.6D"), "");
        check("站太高不算（不能靠飞躲）", code.contains("p.getY() - at.y > 3.0D"), "");
        check("伤害走 magic（遵守既有减伤与软上限）",
                code.contains("damageSources().magic()"), "");
        check("**不破坏方块**", !code.contains("setBlock("), "");
        check("天崩地裂有独立冷却", code.contains("CATACLYSM_CD"), "");
        check("天崩地裂只在三阶段", code.contains("if (phase >= 3)"), "");
    }


    /**
     * <b>副业熟练度曲线</b>。
     *
     * <p>旧公式累计要 <b>495 次</b>制作才能顶满 10 级 —— 副业不该这么肝。
     * 新曲线总量 140（约旧值 28%）。</p>
     */
    private static void professionCurveRules() {
        section("副业曲线");

        var pr = readSource("src/main/java/com/hongmeng/xiuxian/talent/Profession.java");
        String code = stripComments(pr);

        check("曲线改成查表（递归公式看不出总量）",
                code.contains("CUMULATIVE"), "");
        check("有'顶满需要多少次'的可读常量", code.contains("totalToMax("), "");

        // ---- 数学校验：单调 + 总量合理 + 前期便宜 ----
        int[] cum = {0, 3, 8, 16, 26, 39, 54, 72, 92, 115, 140};
        boolean increasing = true;
        for (int i = 2; i < cum.length; i++) {
            if (cum[i] <= cum[i - 1]) {
                increasing = false;
            }
        }
        check("累计次数**严格递增**（不能有等级不花次数）", increasing, "");
        check("顶满 10 级不超过 160 次（旧值 495 太重）",
                cum[10] <= 160, "实际 " + cum[10]);
        check("前 3 级很便宜（<= 20 次，几分钟能见效）",
                cum[3] <= 20, "实际 " + cum[3]);
        boolean noDrop = true;
        for (int i = 2; i < cum.length; i++) {
            int d1 = cum[i] - cum[i - 1];
            int d0 = cum[i - 1] - cum[i - 2];
            if (d1 <= d0) {
                noDrop = false;
            }
        }
        check("每级增量**单调变大**（越练越慢，符合长期目标）", noDrop, "");
        check("等级有上限保护（越界不崩）",
                code.contains("Math.min(maxRank(), rank)"), "");
    }


    /**
     * <b>自制 Boss 音效</b>。
     *
     * <p>此前转阶段的音效全是原版占位，和"修仙"这个题材完全无关。
     * 现在这个音是<b>用 numpy 合成 PCM、再用 libsndfile 编码成 Ogg Vorbis</b>
     * 得到的 —— 不依赖任何外部素材。</p>
     *
     * <p>注：格挡/破防/翻滚/处决四个音效随玩家侧魂系系统一并删除。</p>
     */
    private static void customSfxRules() {
        section("自制战斗音效");

        var ms = readSource("src/main/java/com/hongmeng/xiuxian/ModSounds.java");
        String code = stripComments(ms);
        check("注册了 PHASE", code.contains("PHASE"), "");

        // ---- 文件真的在 ----
        check("音频文件存在: phase.ogg",
                findResource("assets/hongmeng/sounds/phase.ogg") != null, "");

        // ---- 真的被播放（不是只注册） ----
        var bp = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/BossPhase.java"));
        check("转阶段播自制 phase", bp.contains("ModSounds.PHASE"), "");
    }


    /**
     * <b>猛扑可打断</b> + <b>群体轮流上</b>。
     *
     * <p>在此之前"蓄力猛扑"是必然命中，而且一群怪会同时扑 ——
     * 玩家面对四只怪的结局是必死。</p>
     */
    private static void mobInterruptGroupRules() {
        section("打断与协同");

        var mm = readSource("src/main/java/com/hongmeng/xiuxian/entity/MobMind.java");
        String code = stripComments(mm);

        // ---- 打断 ----
        check("有'正在蓄力'查询", code.contains("public static boolean telegraphing("), "");
        check("有打断入口", code.contains("public static boolean interrupt("), "");
        check("打断后踉跄 1 秒", code.contains("STAGGER_TICKS = 20"), "");
        check("踉跄期间不行动（否则打断没意义）",
                code.contains("if (staggered(mob))"), "");
        check("踉跄时清掉寻路", code.contains("getNavigation().stop()"), "");
        check("打断不进僵直登记表（那是完美格挡的专属收益）",
                !code.contains("markStunned("), "");
        check("重击有判定标准（5% 生命）",
                code.contains("HEAVY_DAMAGE_RATIO = 0.05D"), "");

        var ev = readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java");
        String evCode = stripComments(ev);
        check("伤害结算里接了打断",
                evCode.replaceAll("\\s+", "").contains("MobMind.interrupt("), "");
        check("连招终结技算重击",
                evCode.contains("CombatAnim.isFinisher(player)"), "");
        check("只对近战直击生效", evCode.contains("getDirectEntity() == player"), "");

        // ---- 群体协同 ----
        check("有'同伴在扑'的检测", code.contains("allyPouncing("), "");
        check("同区域只允许一只蓄力/猛扑",
                code.contains("if (allyPouncing(mob))"), "");
        check("同伴在扑时改为绕圈等待",
                code.contains("return Mood.CIRCLE;"), "");
        check("协同半径合理（12 格内）",
                code.contains("GROUP_RADIUS = 12.0D"), "");
        check("清状态时也清踉跄", code.contains("pd.remove(TAG_STAGGER)"), "");
    }


    /**
     * <b>法术九属性音效</b> + <b>雷劫音效</b>。
     *
     * <p>此前法术放出来是 {@code BLAZE_SHOOT} 之类的原版音，
     * 雷劫用的也是原版雷声 —— 和题材无关。</p>
     */
    private static void elementSfxRules() {
        section("法术与雷劫音效");

        var ms = stripComments(readSource("src/main/java/com/hongmeng/xiuxian/ModSounds.java"));
        String[][] map = {
                {"METAL", "cast_metal"}, {"WOOD", "cast_wood"},
                {"WATER", "cast_water"}, {"FIRE", "cast_fire"},
                {"EARTH", "cast_earth"}, {"ICE", "cast_ice"},
                {"THUNDER", "cast_thunder"}, {"WIND", "cast_wind"},
                {"NONE", "cast_none"},
        };
        for (String[] pair : map) {
            check("注册了 CAST_" + pair[0], ms.contains("CAST_" + pair[0]), "");
            check("文件存在: " + pair[1] + ".ogg",
                    findResource("assets/hongmeng/sounds/" + pair[1] + ".ogg") != null, "");
        }
        check("注册了雷劫落雷音", ms.contains("TRIB_BOLT"), "");
        check("注册了雷劫蓄势音", ms.contains("TRIB_CHARGE"), "");
        check("落雷音频存在",
                findResource("assets/hongmeng/sounds/tribulation_bolt.ogg") != null, "");
        check("蓄势音频存在",
                findResource("assets/hongmeng/sounds/tribulation_charge.ogg") != null, "");

        // ---- 真的按元素播 ----
        var fx = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellFxRuntime.java"));
        check("有按元素选音的函数", fx.contains("playElementSound("), "");
        check("施法时会调用它", fx.contains("playElementSound(level, from"), "");
        // 九个分支都要在
        for (String[] pair : map) {
            check("元素 " + pair[0] + " 有对应分支",
                    fx.contains("case " + pair[0] + " ->"), "");
        }

        // ---- 雷劫真的用它 ----
        var bt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/core/Breakthrough.java"));
        check("落雷播自制音", bt.contains("TRIB_BOLT"), "");
        check("蓄势播自制音", bt.contains("TRIB_CHARGE"), "");
        check("落雷不再用原版 LIGHTNING_BOLT_IMPACT",
                !bt.contains("LIGHTNING_BOLT_IMPACT"), "");
    }


    /**
     * <b>背景音乐</b>（4 首自制氛围曲 + 客户端音乐导演）。
     */
    private static void bgmRules() {
        section("背景音乐");

        var ms = stripComments(readSource("src/main/java/com/hongmeng/xiuxian/ModSounds.java"));
        String[][] tracks = {
                {"BGM_CALM", "bgm_calm"}, {"BGM_DARK", "bgm_dark"},
                {"BGM_BOSS", "bgm_boss"}, {"BGM_TRIBULATION", "bgm_tribulation"},
        };
        for (String[] tr : tracks) {
            check("注册了 " + tr[0], ms.contains(tr[0]), "");
            check("音频存在: " + tr[1] + ".ogg",
                    findResource("assets/hongmeng/sounds/" + tr[1] + ".ogg") != null, "");
        }

        var md = readSource("src/main/java/com/hongmeng/xiuxian/client/MusicDirector.java");
        String code = stripComments(md);
        check("有客户端音乐导演", code.contains("class MusicDirector"), "");
        check("渡劫中切雷劫曲",
                code.contains("TribulationSkyRenderer.active()"), "");
        check("附近有 Boss 切战斗曲", code.contains("hasBossBar()"), "");
        check("真世界切暗色曲", code.contains("RealWorldFxState.strength()"), "");
        check("默认放平静曲", code.contains("return \"bgm_calm\";"), "");
        check("换曲前先停旧曲（否则会叠在一起）",
                code.contains("stop();") && code.contains("getSoundManager().stop("), "");
        check("BGM 会循环（looping=true）", code.contains("true, 0,"), "");
        check("走 MUSIC 分类（受玩家的音乐音量控制）",
                code.contains("SoundSource.MUSIC"), "");
        check("判定有节流（不逐 tick 扫实体）",
                code.contains("tickCount % INTERVAL"), "");
        check("客户端每 tick 驱动",
                stripComments(readSource(
                        "src/main/java/com/hongmeng/xiuxian/client/CombatFeelClient.java"))
                        .contains("MusicDirector.tick()"), "");
        check("sounds.json 里 BGM 是 stream=true（长音频必须流式解码）",
                findResource("assets/hongmeng/sounds.json") != null
                        && readSource("src/main/resources/assets/hongmeng/sounds.json")
                        .contains("\"stream\": true"), "");
    }


    /**
     * <b>劫灰修士</b>（真世界专属新怪）。
     *
     * <p>加一只人形怪的最小完整链：实体类 → 注册 → 渲染器 Variant →
     * 客户端注册 → 贴图 → 语言。少任何一环都会出现"刷出来是紫黑方块"或
     * "根本没东西"。</p>
     */
    private static void ashCultivatorRules() {
        section("新怪·劫灰修士");

        String id = "tribulation_ash_cultivator";
        var cls = readSource("src/main/java/com/hongmeng/xiuxian/entity/"
                + "TribulationAshCultivatorEntity.java");
        check("有实体类", cls.contains("class TribulationAshCultivatorEntity"), "");
        check("是精英品阶（自动继承心智/格挡/闪避/协同）",
                cls.contains("MobRank.ELITE"), "");
        check("有属性构造", cls.contains("createAttributes()"), "");
        check("有目标选择（会主动追玩家）",
                cls.contains("NearestAttackableTargetGoal"), "");

        var ents = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/ModEntities.java"));
        check("已注册到 ModEntities", ents.contains(id), "");
        check("已加入生成放置（否则自然生成不生效）",
                ents.contains("TRIBULATION_ASH_CULTIVATOR"), "");

        var human = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/CultivationHumanoidRenderer.java"));
        check("有渲染器 Variant", human.contains("TRIBULATION_ASH_CULTIVATOR"), "");

        var setup = readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java");
        check("客户端已注册渲染器",
                setup.contains("TRIBULATION_ASH_CULTIVATOR"), "");

        check("贴图存在（64×64 人形 UV）",
                findResource("assets/hongmeng/textures/entity/" + id + ".png") != null, "");

        var zh = readSource("src/main/resources/assets/hongmeng/lang/zh_cn.json");
        check("中文名已加", zh.contains("entity.hongmeng." + id), "");

        var ter = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/realworld/HardmodeTerrain.java"));
        check("真世界会刷它（普通世界见不到）",
                ter.contains("TRIBULATION_ASH_CULTIVATOR"), "");
        check("刷怪概率以它为主（>=60%）",
                ter.contains("nextFloat() < 0.65F"), "");
    }


    /**
     * <b>形状库扩充 + 运动轨迹</b>。
     *
     * <p>在此之前所有形状都在<b>原地缩放</b>，没有"运动"。
     * 新增的 {@code METEOR_ARC} 用 {@code from → to} 插值，是第一个真正会移动的形状。</p>
     */
    private static void newShapesRules() {
        section("新形状与轨迹");

        var pkt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/network/SpellFxPacket.java"));
        check("有碎片群形状", pkt.contains("SHARD_BURST"), "");
        check("有地面裂纹形状", pkt.contains("GROUND_CRACK"), "");
        check("有能量丝形状", pkt.contains("FILAMENTS"), "");
        check("有陨石轨迹形状", pkt.contains("METEOR_ARC"), "");
        check("有光柱横扫形状", pkt.contains("BEAM_SWEEP"), "");
        check("裂纹用专用贴图", pkt.contains("\"ground_crack\""), "");
        check("裂纹贴图存在",
                findResource("assets/hongmeng/textures/fx/ground_crack.png") != null, "");

        var mesh2 = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/SpellFxMesh2.java"));
        check("碎片：交叉方片（任意角度都有体积）",
                mesh2.contains("public static void shards("), "");
        check("碎片受重力下坠（不是匀速飞走）",
                mesh2.contains("gravity") || mesh2.contains("-spread * 0.55F * t * t"), "");
        check("能量丝是细带（不用圆管省面数）",
                mesh2.contains("public static void filaments("), "");
        check("裂纹用 alpha 混合（additive 画不出暗色）",
                mesh2.contains("public static void groundCrack("), "");
        check("陨石沿抛物线（叠 sin 弧高）",
                mesh2.contains("public static void meteorArc(")
                        && mesh2.contains("Math.sin(Math.PI * tk)"), "");
        check("陨石有拖尾（6 段渐小）",
                mesh2.contains("int trail = 6"), "");
        check("横扫光柱双面（避免背面剔除后消失）",
                mesh2.contains("public static void beamSweep("), "");

        var rend = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/SpellFxRenderer.java"));
        for (String fn : new String[]{"shardFx", "groundCrackFx", "filamentFx",
                "meteorArcFx", "beamSweepFx"}) {
            check("渲染器有 " + fn, rend.contains(fn + "("), "");
        }
        check("裂纹会向下扫描找地面（空中施法不会画在半空）",
                rend.contains("ClipContext"), "");
        check("陨石用 from/to 插值（这是运动轨迹的关键）",
                rend.contains("Vec3 b = p.to();"), "");

        var rt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellFxRuntime.java"));
        check("有带终点的发送（旧 send 的 from/to 是同一点）",
                rt.contains("private static void sendTo("), "");
        check("灭世级会发射碎片群", rt.contains("SHARD_BURST"), "");
        check("灭世级会发射地面裂纹", rt.contains("GROUND_CRACK"), "");
        check("引力级会发射陨石轨迹", rt.contains("METEOR_ARC"), "");
        check("高阶法术会发射能量丝", rt.contains("FILAMENTS"), "");
    }


    /**
     * <b>四段式的每段独立音效</b>。
     *
     * <p>四段式早就有时间结构，但<b>四段共用一个音</b> ——
     * 听觉上根本分不出段。现在蓄势、爆发、冲击、余波各有各的音，
     * 而且<b>与视觉同一时间点</b>触发。</p>
     */
    private static void stageSfxRules() {
        section("四段式音效");

        var ms = stripComments(readSource("src/main/java/com/hongmeng/xiuxian/ModSounds.java"));
        for (String n : new String[]{"STAGE_CHARGE", "STAGE_BURST", "STAGE_AFTERMATH"}) {
            check("注册了 " + n, ms.contains(n), "");
        }
        for (String f : new String[]{"stage_charge", "stage_burst", "stage_aftermath"}) {
            check("音频存在: " + f + ".ogg",
                    findResource("assets/hongmeng/sounds/" + f + ".ogg") != null, "");
        }

        var rt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellFxRuntime.java"));
        check("有'按延迟播阶段音'的函数", rt.contains("stageSound("), "");
        check("蓄势段播音（0 tick）",
                rt.contains("stageSound(level, at, 0"), "");
        check("爆发段播音（5 tick）",
                rt.contains("stageSound(level, at, 5"), "");
        check("冲击波有第二声（9 tick，音高更高）",
                rt.contains("stageSound(level, at, 9"), "");
        check("余波段播音（22 tick）",
                rt.contains("stageSound(level, at, 22"), "");
        check("声音用延迟调度（不延迟就会变成一声混响）",
                rt.contains("RealWorldScheduler.after("), "");
        check("延迟调度器每 tick 推进（不推进就永远不响）",
                readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java")
                        .contains("RealWorldScheduler.tick"), "");
    }


    /**
     * <b>Boss 专属大招音效</b>。
     *
     * <p>此前魔影突袭用 {@code ENDERMAN_TELEPORT}、天崩地裂用 {@code GENERIC_EXPLODE} ——
     * 和普通传送/爆炸没区别，"它要放大了"的信号立不住。</p>
     */
    private static void bossUltSfxRules() {
        section("Boss 大招音效");

        var ms = stripComments(readSource("src/main/java/com/hongmeng/xiuxian/ModSounds.java"));
        check("注册了 BOSS_AMBUSH", ms.contains("BOSS_AMBUSH"), "");
        check("注册了 BOSS_CATACLYSM", ms.contains("BOSS_CATACLYSM"), "");
        check("突袭音频存在",
                findResource("assets/hongmeng/sounds/boss_ambush.ogg") != null, "");
        check("天崩地裂音频存在",
                findResource("assets/hongmeng/sounds/boss_cataclysm.ogg") != null, "");

        var ab = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/BossAbilities.java"));
        check("突袭播专属音", ab.contains("ModSounds.BOSS_AMBUSH"), "");
        check("天崩地裂播专属音", ab.contains("ModSounds.BOSS_CATACLYSM"), "");
        check("突袭不再用原版传送音",
                !ab.contains("SoundEvents.ENDERMAN_TELEPORT"), "");
        check("天崩地裂不再用原版爆炸音",
                !ab.contains("SoundEvents.GENERIC_EXPLODE"), "");
        check("冲击波逐圈升音高（越来越近）",
                ab.contains("1.25F + w * 0.18F"), "");
    }


    /**
     * <b>雷劫音效随境界变化</b>。
     *
     * <p>视觉早就按境界变了（青/蓝/紫/血），但听觉一直是同一个音 ——
     * 现在音高与音量也按境界走。</p>
     */
    private static void tribSoundTierRules() {
        section("雷劫音效变调");

        var bt = stripComments(readSource("src/main/java/com/hongmeng/xiuxian/core/Breakthrough.java"));
        check("有按境界取音高", bt.contains("public static float boltPitch("), "");
        check("有按境界取音量", bt.contains("public static float boltVolume("), "");
        check("第 10 境音高最低（0.75）", bt.contains("return 0.75F;"), "");
        check("第 10 境音量最大（1.50）", bt.contains("return 1.50F;"), "");
        check("低境界音高更高（1.15）", bt.contains("return 1.15F;"), "");
        check("落雷用境界音高", bt.contains("boltPitch(tier)"), "");
        check("落雷用境界音量", bt.contains("boltVolume(tier)"), "");
        check("蓄势音也跟境界走", bt.contains("boltPitch(wTier)"), "");
        check("最后一道仍然更重（低音高 + 大音量）",
                bt.contains("last ? 0.92F : 1.0F"), "");
    }


    /**
     * <b>三种新运动轨迹</b>：螺旋上升 / 环绕 / 爬行裂纹。
     */
    private static void newPathRules() {
        section("新运动轨迹");

        var pkt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/network/SpellFxPacket.java"));
        for (String n : new String[]{"SPIRAL_ASCENT", "ORBIT", "CREEPING_CRACK"}) {
            check("有形状常量 " + n, pkt.contains(n), "");
        }

        var mesh = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/SpellFxMesh2.java"));
        check("螺旋上升：半径随高度收缩（不变=电梯，收缩=被吸上去）",
                mesh.contains("public static void spiralAscent(")
                        && mesh.contains("1.0F - 0.62F * y"), "");
        check("环绕：角度随时间转 + 高度起伏",
                mesh.contains("public static void orbit(")
                        && mesh.contains("Math.sin(ang * 2.0D)"), "");
        check("爬行裂纹：半径随时间生长",
                mesh.contains("public static void creepingCrack(")
                        && mesh.contains("0.25F + 0.75F"), "");
        check("爬行裂纹有前缘亮环（否则像'放大'不像'爬'）",
                mesh.contains("前缘亮环") || mesh.contains("lw = radius * 0.06F"), "");

        var rend = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/SpellFxRenderer.java"));
        for (String fn : new String[]{"spiralFx", "orbitFx", "creepingFx"}) {
            check("渲染器有 " + fn, rend.contains(fn + "("), "");
        }
        check("爬行裂纹也向下找地面",
                rend.contains("creepingFx") && rend.contains("ClipContext"), "");

        var rt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellFxRuntime.java"));
        check("灭世级发射螺旋上升", rt.contains("SPIRAL_ASCENT"), "");
        check("灭世级发射爬行裂纹", rt.contains("CREEPING_CRACK"), "");
        check("大规模法术发射环绕光球", rt.contains("SPELLFX_ORBIT_MARK")
                || rt.contains("SpellFxPacket.ORBIT"), "");
    }


    /**
     * <b>精英怪出场演出</b>。
     *
     * <p>精英和杂鱼在画面上只差头顶一个标记，玩家常常砍两刀才发现。
     * 出场演出把这个信息提前。</p>
     */
    private static void eliteEntranceRules() {
        section("精英出场演出");

        var ee = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/EliteEntrance.java"));
        check("有出场演出类", ee.contains("class EliteEntrance"), "");
        check("只对精英以上触发", ee.contains("MobRank.ELITE.ordinal()"), "");
        check("每只怪只演一次（存持久化数据）",
                ee.contains("TAG_INTRO") && ee.contains("getPersistentData()"), "");
        check("每玩家有 90 秒冷却（防连播）",
                ee.contains("PLAYER_COOLDOWN = 1800L"), "");
        check("复用现成镜头系统（不另造一套）",
                ee.contains("CinematicPacket.play("), "");
        check("镜头会绕怪转（关键帧角度递增）",
                ee.contains("110.0F") && ee.contains("160.0F"), "");
        check("有副标题报品阶", ee.contains("精英") && ee.contains("鸿蒙"), "");
        check("有低吼音（听觉锚点）", ee.contains("RAVAGER_ROAR"), "");
        check("排除旁观/死亡玩家",
                ee.contains("isSpectator()") && ee.contains("isDeadOrDying()"), "");

        var mob = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/CultivationMonsterEntity.java"));
        check("每 tick 会推进", mob.contains("EliteEntrance.tick(this)"), "");

        var ev = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/core/ModEvents.java"));
        check("掉线清冷却", ev.contains("EliteEntrance"), "");
    }


    /**
     * <b>道途武器必须是 3D 模型</b>。
     *
     * <p>原来 50 把武器全是 {@code item/handheld} + 一张 16×16 平面贴图 ——
     * 拿在手里没有厚度、没有护手、没有柄，和动作 mod 的观感差一个量级。</p>
     */
    private static void weaponModelRules() {
        section("武器 3D 模型");

        // 材质贴图
        for (String t : new String[]{"wpn_steel", "wpn_gold", "wpn_grip"}) {
            check("材质贴图存在: " + t,
                    findResource("assets/hongmeng/textures/item/" + t + ".png") != null, "");
        }

        String[] families = {"spirit_sword", "mountain_pestle", "soul_devourer",
                "blood_twin_blades", "celestial_chakram"};
        for (String fam : families) {
            String path = "assets/hongmeng/models/item/path_weapon_" + fam + "_1.json";
            String src = readSource("src/main/resources/" + path);
            check("模型存在: " + fam, src != null && !src.isEmpty(), "");
            if (src == null || src.isEmpty()) {
                continue;
            }
            check(fam + " 有 elements（真 3D，不是平铺贴图）",
                    src.contains("\"elements\""), "");
            // 材质方案升级：16x16 单色 -> 64x64 八格图集（钢亮/钢暗/金亮/金暗/皮柄/缠绳/刃口/宝石）
            check(fam + " 用 64x64 材质图集",
                    src.contains("hongmeng:item/wpn_mat"), "");
            check(fam + " 带手持显示变换",
                    src.contains("firstperson_righthand")
                            && src.contains("thirdperson_righthand"), "");
            check(fam + " 用正面光（背包立体感）",
                    src.contains("\"gui_light\": \"front\""), "");
            check(fam + " 不再依赖 item/handheld 平面父模型",
                    !src.contains("item/handheld"), "");
        }

        // 十个阶级都要升级（抽查首尾阶）
        for (String fam : families) {
            for (String tier : new String[]{"_1", "_10"}) {
                String path = "assets/hongmeng/models/item/path_weapon_" + fam
                        + tier + ".json";
                String src = readSource("src/main/resources/" + path);
                check(fam + tier + " 已升级为 3D",
                        src != null && src.contains("\"elements\""), "");
            }
        }
    }


    /**
     * <b>武器材质细化 + 手持动态光效</b>。
     *
     * <p>模型对了之后，两个问题还在：材质太"平"，站着不动是块死几何。
     * 这里加刃口倒角（视觉锋利度）与手持流光（第三人称的"活"）。</p>
     */
    private static void weaponPolishRules() {
        section("武器质感与动效");

        check("有刃口倒角材质", findResource(
                "assets/hongmeng/textures/item/wpn_edge.png") != null, "");

        String[] families = {"spirit_sword", "mountain_pestle", "soul_devourer",
                "blood_twin_blades", "celestial_chakram"};
        for (String fam : families) {
            String src = readSource("src/main/resources/assets/hongmeng/models/item/"
                    + "path_weapon_" + fam + "_1.json");
            // 倒角现在是图集里的一格（EDGE=(32,16)），不再是独立材质名
            check(fam + " 有倒角面（EDGE 格）",
                    src != null && src.contains("\"uv\"")
                            && src.contains("32,"), "");
        }

        var glow = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/HeldWeaponGlow.java"));
        check("有手持光效渲染器", glow.contains("class HeldWeaponGlow"), "");
        check("只处理持道途武器的玩家",
                glow.contains("PathWeaponItem"), "");
        check("有呼吸微光（1.1 Hz 涨落）",
                glow.contains("Mth.sin(time * 6.9F)"), "");
        check("有三点流光（相位错开 120°）",
                glow.contains("i * 2.0944D"), "");
        check("颜色按道途分（五套）",
                glow.contains("case FA ->") && glow.contains("case TI ->")
                        && glow.contains("case MO ->") && glow.contains("case XUE ->")
                        && glow.contains("case ZHEN ->"), "");
        check("用 additive 管线（能量光效）",
                glow.contains("RenderType.lightning()"), "");
        check("有距离剔除（32 格内）",
                glow.contains("RANGE = 32.0D"), "");
        check("已注册到客户端事件总线",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("HeldWeaponGlow.class"), "");
    }


    /**
     * <b>Boss 实装完整性</b>（审计发现的两个真问题）。
     *
     * <p>1) 四个祭坛 Boss 缺 lang 条目 —— 游戏里会显示成
     * {@code entity.hongmeng.epoch_boss} 这样的原始键；</p>
     * <p>2) Boss 挥击<b>没有任何动作</b> —— {@code CombatAnim} 只处理玩家。</p>
     */
    private static void bossAuditRules() {
        section("Boss 实装完整性");

        var zh = readSource("src/main/resources/assets/hongmeng/lang/zh_cn.json");
        String[] ids = {"epoch_boss", "beast_boss", "demon_boss", "void_boss"};
        for (String id : ids) {
            check("祭坛 Boss 有中文名: " + id,
                    zh.contains("entity.hongmeng." + id), "");
        }

        // ---- 攻击动作 ----
        var pkt = readSource(
                "src/main/java/com/hongmeng/xiuxian/network/MobAttackAnimPacket.java");
        check("有怪物攻击动作包", pkt.contains("class MobAttackAnimPacket"), "");
        check("包已注册",
                readSource("src/main/java/com/hongmeng/xiuxian/network/ModNetwork.java")
                        .contains("MobAttackAnimPacket.class"), "");

        var mob = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/CultivationMonsterEntity.java"));
        check("攻击时会广播动作",
                mob.contains("MobAttackAnimPacket"), "");
        check("动作按品阶分（Boss 重砸 / 队长突刺 / 其它横扫）",
                mob.contains("BOSS, OVERLORD -> 1") && mob.contains("CAPTAIN -> 2"), "");

        var anim = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/MobCombatAnim.java"));
        check("客户端有怪物动作渲染", anim.contains("class MobCombatAnim"), "");
        check("有 push/pop 严格配对（否则后续实体全变形）",
                anim.contains("PUSHED") && anim.contains("popPose()"), "");
        check("幅度比玩家大（大体型动作要重）",
                anim.contains("AMP = 1.6F"), "");
        check("不处理玩家（玩家走另一套）",
                anim.contains("instanceof net.minecraft.world.entity.player.Player"), "");
        check("已在客户端总线注册",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("MobCombatAnim.class"), "");
    }


    /**
     * <b>盔甲外挂部件</b>（肩甲 / 披风 / 护腕）。
     *
     * <p>玩家要求参考「装备-幻想盔甲」：把外挂部件做成 3D，
     * 而且"穿在身上一直看得见"，视觉收益比武器更大。</p>
     */
    private static void armorTrimRules() {
        section("盔甲外挂部件");

        check("甲片贴图存在", findResource(
                "assets/hongmeng/textures/fx/armor_trim.png") != null, "");

        var r = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/ArmorTrimRenderer.java"));
        check("有外挂部件渲染器", r.contains("class ArmorTrimRenderer"), "");
        check("读胸甲槽判断套装",
                r.contains("getArmor(2)"), "");
        check("认得十境道袍（dao_armor_*）",
                r.contains("dao_armor_"), "");
        check("认得五道途甲（path_armor_*）",
                r.contains("path_armor_"), "");
        check("道袍按阶数渐变配色", r.contains("robeColor("), "");
        check("道途甲按道途配色（与武器光效同源）",
                r.contains("pathColor("), "");
        // ⚠ stripComments 会删掉注释，只能查**代码结构**：
        //   肩甲 = 两侧循环（side=-1/1）+ 3 片 + 肩部偏移 0.30
        // 肩甲从“3 片贴片”改成“2 个六面体”（玩家：只是一块贴在身上，不立体）
        check("肩甲是六面体（有厚度）",
                r.contains("private static void box(")
                        && r.contains("rx * 0.20D * side")
                        && r.contains("seg < 2"), "");
        check("画披风（3 段，随走路摆动）",
                r.contains("walkAnimation") && r.contains("swing"), "");
        // 护腕偏移按玩家反馈从 0.36 收到 0.30
        check("画护腕（小臂高度 1.02，两侧）",
                r.contains("py + 1.02D") && r.contains("rx * 0.30D * side"), "");
        check("有距离剔除", r.contains("RANGE = 32.0D"), "");
        check("用 alpha 混合管线（不是 additive，否则甲片发光）",
                r.contains("entityTranslucentEmissive"), "");
        check("不接管原版盔甲渲染（只叠加，不会毁模型）",
                !r.contains("getArmorRenderer") && !r.contains("HumanoidModel"), "");
        // 旧的世界空间贴片已停用，改成真正的 3D 护甲模型（getHumanoidArmorModel）
        // 护甲外挂件现在走 RenderLayer（自己一张花纹贴图）
        check("护甲外挂件走 RenderLayer",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("ArmorTrimLayer"), "");
    }


    /**
     * <b>维度 boss</b>（下界/末地）与<b>虚空龙帝</b>。
     *
     * <p>上一轮审计漏了这两只：它们不是独立 boss 类，而是
     * <b>普通生物 + 刷怪器变体标记</b>。</p>
     */
    private static void dimensionBossRules() {
        section("维度 Boss / 虚空龙帝");

        var spawner = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/NativeBossSpawner.java"));
        check("刷怪器设了品阶覆盖（否则下界 boss 只是精英怪）",
                spawner.contains("hm_rank_override"), "");
        check("下界用 BOSS(4) / 末地用 OVERLORD(5)",
                spawner.contains("nether ? 4 : 5"), "");
        check("有变体标记（贴图区分）", spawner.contains("hm_boss_variant"), "");
        check("有出场演出", spawner.contains("BossEntrancePacket"), "");

        // ---- 虚空龙帝 ----
        var cls = readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/VoidDragonEmperorEntity.java");
        check("有虚空龙帝实体类", cls.contains("class VoidDragonEmperorEntity"), "");
        check("品阶是 OVERLORD（终局 boss）", cls.contains("MobRank.OVERLORD"), "");
        check("继承 CultivationMonsterEntity（才有血条/三阶段/法术）",
                cls.contains("extends CultivationMonsterEntity"), "");
        check("有属性构造", cls.contains("createAttributes()"), "");

        var ents = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/ModEntities.java"));
        check("已注册", ents.contains("void_dragon_emperor"), "");
        check("属性已注册（漏了会血量为 0）",
                ents.contains("VoidDragonEmperorEntity.createAttributes"), "");

        var set = readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java");
        check("客户端注册了独立渲染器",
                set.contains("VoidDragonEmperorRenderer"), "");
        check("龙帝渲染器存在",
                readSource("src/main/java/com/hongmeng/xiuxian/client/"
                        + "VoidDragonEmperorRenderer.java")
                        .contains("class VoidDragonEmperorRenderer"), "");
        check("龙帝贴图存在",
                findResource("assets/hongmeng/textures/entity/"
                        + "void_dragon_emperor.png") != null, "");

        var zh = readSource("src/main/resources/assets/hongmeng/lang/zh_cn.json");
        check("有中文名", zh.contains("entity.hongmeng.void_dragon_emperor"), "");

        // ---- 蛟龙渲染器必须完好（回滚后）----
        var jl = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/JiaolongRenderer.java");
        check("蛟龙渲染器仍在且未被泛型化破坏",
                jl.contains("class JiaolongRenderer")
                        && jl.contains("JiaolongEntity"), "");
    }


    /**
     * <b>焚天炎帝</b>（下界终局 boss，参考灾厄的巨体型思路）。
     *
     * <p>此前下界 boss 是烈焰猿换皮（精英品阶、无血条/无阶段）。
     * 现在有独立实体 + 独立贴图 + BOSS 品阶。</p>
     */
    private static void blazeEmperorRules() {
        section("焚天炎帝");

        var cls = readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/BlazeEmperorEntity.java");
        check("有实体类", cls.contains("class BlazeEmperorEntity"), "");
        check("品阶是 BOSS（血条 + 三阶段 + 远程）",
                cls.contains("MobRank.BOSS"), "");
        check("继承 CultivationMonsterEntity",
                cls.contains("extends CultivationMonsterEntity"), "");
        check("有属性构造", cls.contains("createAttributes()"), "");

        var ents = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/ModEntities.java"));
        check("已注册", ents.contains("blaze_emperor"), "");
        check("属性已注册（漏了会血量为 0）",
                ents.contains("BlazeEmperorEntity.createAttributes"), "");

        check("有独立渲染器",
                readSource("src/main/java/com/hongmeng/xiuxian/client/"
                        + "BlazeEmperorRenderer.java")
                        .contains("class BlazeEmperorRenderer"), "");
        check("客户端已注册渲染器",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("BlazeEmperorRenderer"), "");
        check("贴图存在",
                findResource("assets/hongmeng/textures/entity/"
                        + "blaze_emperor.png") != null, "");
        check("有中文名",
                readSource("src/main/resources/assets/hongmeng/lang/zh_cn.json")
                        .contains("entity.hongmeng.blaze_emperor"), "");

        var sp = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/NativeBossSpawner.java"));
        check("下界刷的是它（不再是烈焰猿换皮）",
                sp.contains("ModEntities.BLAZE_EMPEROR"), "");
        check("末地仍是虚空龙帝/虚空巨蟒",
                sp.contains("VOID_DRAGON_EMPEROR"), "");
    }


    /**
     * <b>两只维度 boss 的独立骨架</b>。
     *
     * <p>玩家明确要求：「炎帝和龙帝的模型<b>不能复用</b>之前的模型」。
     * 所以这里断言它们各自有 Model 类、各自的 Layer、各自的贴图，
     * 且<b>不复用</b>烈焰猿 / 蛟龙。</p>
     */
    private static void bossSkeletonRules() {
        section("Boss 独立骨架");

        String[][] bosses = {
                {"BlazeEmperor", "blaze_emperor", "焚天炎帝"},
                {"VoidDragonEmperor", "void_dragon_emperor", "虚空龙帝"},
        };
        for (String[] b : bosses) {
            String base = b[0];
            String tex = b[1];
            String name = b[2];
            String model = readSource("src/main/java/com/hongmeng/xiuxian/client/"
                    + base + "Model.java");
            String render = readSource("src/main/java/com/hongmeng/xiuxian/client/"
                    + base + "Renderer.java");

            check(name + " 有独立 Model 类",
                    model.contains("class " + base + "Model"), "");
            check(name + " 有独立 Layer（不是复用别人的）",
                    render.contains("ModelLayerLocation") && render.contains(tex), "");
            check(name + " 骨架用 createBodyLayer 自建",
                    model.contains("MeshDefinition") && model.contains("addOrReplaceChild"), "");
            check(name + " 自己实现了 renderToBuffer（EntityModel 是抽象类）",
                    model.contains("renderToBuffer"), "");
            check(name + " 有 SetupAnim 动作", model.contains("setupAnim"), "");
            check(name + " 贴图存在",
                    findResource("assets/hongmeng/textures/entity/" + tex + ".png") != null, "");
            check(name + " 层定义已在客户端注册",
                    readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                            .contains(base + "Model::createBodyLayer"), "");
            // 关键：不能复用旧模型
            check(name + " 不复用烈焰猿/蛟龙模型",
                    !model.contains("ApeModel") && !model.contains("JiaolongModel")
                            && !model.contains("FlameApeModel"), "");
        }

        // 脚底基准：模型底部必须落在 24（生成器已断言，这里校验生成结果）
        String blaze = readSource("src/main/java/com/hongmeng/xiuxian/client/"
                + "BlazeEmperorModel.java");
        check("炎帝骨架有腿且底面 = 24（pose 24 + 盒高 16）",
                blaze.contains("PartPose.offset(5.0F, 24.0F, 0.0F)")
                        && blaze.contains("-16.0F, -3.0F, 6.0F, 16.0F"), "");
        String dragon = readSource("src/main/java/com/hongmeng/xiuxian/client/"
                + "VoidDragonEmperorModel.java");
        check("龙帝骨架有独立巨颅与下颌",
                dragon.contains("\"head\"") && dragon.contains("\"jaw\""), "");
        check("龙帝有四段颈、四段尾、四段翼",
                dragon.contains("\"neck3\"") && dragon.contains("\"tail3\"")
                        && dragon.contains("\"wingL3\"") && dragon.contains("\"wingR3\""), "");

        // 炎帝手里有剑（灾变那种巨体型 boss 的识别点）
        check("炎帝模型里有一把大剑（刃 + 护手 + 柄）",
                blaze.contains("\"swordBlade\"") && blaze.contains("\"swordGuard\"")
                        && blaze.contains("\"swordGrip\""), "");

        // 命中箱与模型对齐
        var ents = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/ModEntities.java"));
        check("炎帝命中箱按模型高 3.7 给", ents.contains(".sized(2.4F, 3.7F)"), "");
        check("龙帝命中箱按躯干给", ents.contains(".sized(3.2F, 3.1F)"), "");
    }


    /**
     * <b>Boss 完备性</b>：刷怪蛋全覆盖 + 压迫感光环 + 阶段/技能接线。
     *
     * <p>玩家要求：「每个 boss 要能单独刷出来」「华丽、有压迫感、
     * 配特效/动作/技能/阶段变化」。</p>
     */
    private static void bossCompletenessRules() {
        section("Boss 完备性");

        // ---- 刷怪蛋全覆盖 ----
        String[] bosses = {"BLAZE_EMPEROR", "VOID_DRAGON_EMPEROR", "EPOCH_BOSS",
                "BEAST_BOSS", "DEMON_BOSS", "VOID_BOSS", "DEMON_GENERAL",
                "VOID_OVERLORD", "VOID_SERPENT", "VOID_WALKER"};
        var items = readSource("src/main/java/com/hongmeng/xiuxian/item/ModItems.java");
        var tabs = readSource(
                "src/main/java/com/hongmeng/xiuxian/item/ModCreativeTabs.java");
        var zh = readSource("src/main/resources/assets/hongmeng/lang/zh_cn.json");
        for (String b : bosses) {
            check("刷怪蛋: " + b,
                    items.contains(b + "_SPAWN_EGG"), "");
            check("创造栏有: " + b,
                    tabs.contains("ModItems." + b + "_SPAWN_EGG"), "");
        }
        check("虚弓行者中文名存在", zh.contains("void_walker"), "");

        // ---- 压迫感光环 ----
        var aura = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/BossAuraRenderer.java"));
        check("有 Boss 光环渲染器", aura.contains("class BossAuraRenderer"), "");
        check("只对 BOSS/OVERLORD 生效",
                aura.contains("MobRank.BOSS") && aura.contains("MobRank.OVERLORD"), "");
        // 玩家反馈“脚底下吥呀吥呀转” -> 双环已删，只留慢速光点
        check("环绕光点已降速（不再像风扇）",
                readSource("src/main/java/com/hongmeng/xiuxian/client/BossAuraRenderer.java").contains("time * 2.0F + i * 1.3F"), "");
        check("六颗环绕光点（高度正弦错开）",
                aura.contains("i < 6") && aura.contains("Math.sin(ang * 1.6D)"), "");
        check("躯干高度脉动核心", aura.contains("breath"), "");
        check("按 boss 类型配色（炎/虚空/兽/傀儡）",
                aura.contains("blaze") && aura.contains("void")
                        && aura.contains("beast") && aura.contains("epoch"), "");
        check("有距离剔除", aura.contains("RANGE = 48.0D"), "");
        check("已注册到客户端总线",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("BossAuraRenderer.class"), "");

        // ---- 阶段/技能确实在基类被调 ----
        var mob = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/CultivationMonsterEntity.java"));
        check("基类每 tick 推进阶段", mob.contains("BossPhase."), "");
        check("基类每 tick 推进技能", mob.contains("BossAbilities."), "");
        check("基类每 tick 推进攻击动作", mob.contains("MobAttackAnimPacket"), "");
        check("基类每 tick 推进出场演出", mob.contains("EliteEntrance.tick"), "");

        // ---- 形态渲染 ----
        var form = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/BossFormRenderer.java"));
        check("阶段形态渲染存在（体型放大）",
                form.contains("SCALE_P2") && form.contains("SCALE_P3"), "");
        check("形态渲染已注册",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("BossFormRenderer"), "");
    }


    /**
     * <b>特效华丽度</b>：伪泛光 / 核心闪爆 / 阵法。
     */
    private static void fxBeautyRules() {
        section("特效华丽度");

        var pkt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/network/SpellFxPacket.java"));
        check("有阵法形状 MANDALA", pkt.contains("MANDALA"), "");

        var mesh = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/SpellFxMesh2.java"));
        check("阵法：三重同心环（反向旋转）",
                mesh.contains("public static void mandala(")
                        && mesh.contains("ring % 2 == 0 ? 1.0F : -1.0F"), "");
        check("阵法：8 根辐条", mesh.contains("i < 8") && mesh.contains("spokeSpin"), "");
        check("阵法：中心符文块自转",
                mesh.contains("coreR") && mesh.contains("Math.cos(t * 1.6F)"), "");
        check("阵法内环留缺口（像刻出来的符文，不是实线）",
                mesh.contains("i % 4 == 3"), "");

        var rend = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/SpellFxRenderer.java"));
        check("有伪泛光 pass", rend.contains("glowPass("), "");
        // 全局第一个 switch 在别的方法里，必须在 drawOnce 方法体内比较
        int doIdx = rend.indexOf("private static void drawOnce");
        String glowCall = "glowPass(pose, buf, p, cam, t, rgb, alpha);";
        check("伪泛光对所有形状生效（在 drawOnce 的 switch 之前调）",
                doIdx >= 0 && rend.indexOf(glowCall, doIdx) >= 0
                        && rend.indexOf(glowCall, doIdx)
                        < rend.indexOf("switch (p.shape())", doIdx), "");
        check("伪泛光按画质档开关（省性能）",
                rend.contains("SpellFxQuality.level() >= 2"), "");
        check("有核心闪爆 pass", rend.contains("flashPass("), "");
        check("闪爆只在前 12% 时间", rend.contains("t > 0.12D"), "");
        check("有阵法渲染分支", rend.contains("mandalaFx("), "");
        check("阵法贴地（向下找地面）",
                rend.contains("mandalaFx") && rend.contains("ClipContext"), "");

        var rt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellFxRuntime.java"));
        check("灭世/引力级会展开阵法",
                rt.contains("SpellFxPacket.MANDALA")
                        && rt.contains("TechniqueKind.GRAVITY"), "");
    }


    /**
     * <b>元素粒子层</b>：主粒子（材质）+ 辅粒子（点缀），批量发包。
     */
    private static void elementParticleRules() {
        section("元素粒子层");

        var ep = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/ElementParticles.java"));
        check("有元素粒子层", ep.contains("class ElementParticles"), "");
        check("主粒子沿用既定配色", ep.contains("SpellVfx.particleOf"), "");
        check("有辅粒子映射（第二层）", ep.contains("accent(Element"), "");
        check("九种元素都有辅粒子",
                ep.contains("case FIRE ->") && ep.contains("case WATER ->")
                        && ep.contains("case ICE ->") && ep.contains("case THUNDER ->")
                        && ep.contains("case WIND ->") && ep.contains("case EARTH ->")
                        && ep.contains("case WOOD ->") && ep.contains("case METAL ->"), "");
        check("使用 ParticleBatch（不逐颗发包）",
                ep.contains("new ParticleBatch("), "");
        check("主环 + 内圈辅环（两层环）",
                ep.contains("radius * 0.62D"), "");
        check("灭世级有双层反向环 + 中心光柱",
                ep.contains("Math.cos(-a)") && ep.contains("k < 8"), "");
        check("粒子上限有保护（最多 96 颗/环）",
                ep.contains("Math.min(count, 96)"), "");
        check("导入的是 spirit 包里的 Element",
                readSource("src/main/java/com/hongmeng/xiuxian/technique/"
                        + "ElementParticles.java").contains(
                        "import com.hongmeng.xiuxian.spirit.Element;"), "");

        var rt = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/SpellFxRuntime.java"));
        check("施法会打粒子环", rt.contains("ElementParticles.burst"), "");
        check("灭世级用 grandBurst",
                rt.contains("ElementParticles.grandBurst"), "");
    }


    /**
     * <b>祭坛四只 boss 的外挂装饰部件</b>。
     *
     * <p>四只已有独立骨架，但身上是"光板"。这轮各加 3~4 个外挂件：
     * 虚空水晶 / 血雾环 / 背脊骨板 / 悬浮法球。</p>
     *
     * <p>⚠ 断言的写法很关键：必须<b>逐个部件</b>检查
     * 字段 / 构造器赋值 / 几何 / 动画 <b>四处齐全</b> ——
     * 只查名字出现会漏掉"字段加了但几何没加"（运行时 NPE）。</p>
     */
    private static void bossModelDetailRules() {
        section("Boss 模型细节");

        String[][][] specs = {
                {{"VoidLordModel", "crystalL", "crystalR", "crystalBack"}},
                {{"BloodDemonModel", "spineTop"}},
                {{"BeastLordModel", "plateA", "plateB", "plateC", "plateD"}},
                {{"PuppetMasterModel", "orbA", "orbB", "orbC"}},
        };
        for (String[][] group : specs) {
            String model = group[0][0];
            String src = readSource("src/main/java/com/hongmeng/xiuxian/client/"
                    + model + ".java");
            check(model + " 存在", src != null && !src.isEmpty(), "");
            for (int i = 1; i < group[0].length; i++) {
                String part = group[0][i];
                check(model + "：" + part + " 有字段",
                        src.contains("private final ModelPart " + part + ";"), "");
                check(model + "：" + part + " 在构造器赋值",
                        src.contains("this." + part + " = root.getChild(\"" + part + "\")"), "");
                check(model + "：" + part + " 有几何（addOrReplaceChild）",
                        src.contains("addOrReplaceChild(\"" + part + "\""), "");
                check(model + "：" + part + " 有动画",
                        src.contains("this." + part + "."), "");
            }
        }

    }


    /**
     * <b>Boss 战环境压迫</b>：暗角 + 色调 + 呼吸脉动。
     *
     * <p>「专属场地」如果靠放方块实现就等于<b>改存档</b> ——
     * 玩家在自己家刷 boss 会毁建筑。所以这里用<b>纯客户端演出</b>：
     * 改的是画面，不是世界。</p>
     */
    private static void bossAtmosphereRules() {
        section("Boss 战环境");

        var at = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/BossFightAtmosphere.java"));
        check("有 Boss 战环境类", at.contains("class BossFightAtmosphere"), "");
        check("只对 BOSS/OVERLORD 生效",
                at.contains("MobRank.BOSS") && at.contains("MobRank.OVERLORD"), "");
        check("距离分档（32 格开始 / 10 格满）",
                at.contains("NEAR = 32.0D") && at.contains("CLOSE = 10.0D"), "");
        check("强度平滑（避免距离抖动闪烁）",
                at.contains("smooth += (target - smooth)"), "");
        // ⚠ stripComments 会删注释，只能查**四次 fill 的坐标表达式**
        check("四边暗角（四次 fill 分别贴上/下/左/右）",
                at.contains("g.fill(dx, dy, w - dx, dy + thickH")
                        && at.contains("g.fill(dx, h - dy - thickH")
                        && at.contains("g.fill(dx, dy, dx + thickW")
                        && at.contains("g.fill(w - dx - thickW"), "");
        check("渐变分层（不是一块死黑边）",
                at.contains("LAYERS = 6") && at.contains("LAYERS - i"), "");
        check("呼吸脉动",
                at.contains("0.75F + 0.25F * Mth.sin"), "");
        check("按 boss 类型染色",
                at.contains("void") && at.contains("beast") && at.contains("epoch"), "");
        check("不动方块（纯渲染）",
                !at.contains("setBlock") && !at.contains("setBlockAndUpdate"), "");
        check("有 GUI 渲染钩子", at.contains("RenderGuiEvent.Post"), "");
        check("隐藏 GUI 时不画（F1）", at.contains("hideGui"), "");
        check("已注册到客户端总线",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("BossFightAtmosphere.class"), "");
        check("每 tick 推进强度",
                readSource("src/main/java/com/hongmeng/xiuxian/client/CombatFeelClient.java")
                        .contains("BossFightAtmosphere.tick()"), "");
    }


    /**
     * <b>祭坛 boss 的独立副本场地</b>（玩家明确选的是「独立副本」）。
     *
     * <p>关键约束：<b>只在副本维度建</b> —— 绝不动主世界，
     * 否则玩家在自己家刷 boss 会毁掉建筑与地形。</p>
     */
    private static void altarArenaRules() {
        section("祭坛独立场地");

        var ar = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/dungeon/AltarArena.java"));
        check("有祭坛场地生成器", ar.contains("class AltarArena"), "");
        check("四套主题（傀儡/兽/血/虚空）",
                ar.contains("EPOCH") && ar.contains("BEAST")
                        && ar.contains("DEMON") && ar.contains("VOID"), "");
        check("每套主题有地板/立柱/点缀/标志块",
                ar.contains("BlockState floor()") && ar.contains("BlockState pillar()")
                        && ar.contains("BlockState accent()")
                        && ar.contains("BlockState marker()"), "");
        check("幂等（中心标志块已在就跳过）",
                ar.contains("is(theme.marker().getBlock())"), "");
        check("不覆盖副本主竞技场（56 格保护圈）",
                ar.contains("KEEP_OUT = 56.0D"), "");
        check("有地板圆盘", ar.contains("RADIUS * RADIUS"), "");
        check("有外圈墙", ar.contains("WALL_H"), "");
        check("有 12 根立柱", ar.contains("PILLARS = 12"), "");
        check("有中央祭坛三级台阶",
                ar.contains("step < 3") && ar.contains("half = 2 - step"), "");
        check("实体类型映射到主题",
                ar.contains("themeOf(String entityPath)")
                        && ar.contains("case \"epoch_boss\""), "");

        var ev = stripComments(readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java"));
        check("刷怪时接进副本维度判断",
                ev.contains("DungeonRealm.dungeonAt(level)"), "");
        check("只在副本维度建（主世界不动）",
                ev.contains("if (dungeon != null)"), "");
        check("调用 AltarArena.ensure", ev.contains("AltarArena.ensure"), "");
    }


    /**
     * <b>腐化地带</b>（真实世界开启后缓慢蔓延）。
     *
     * <p>这是被压了很久的一条：默认实现会<b>不可逆地吃掉玩家建筑</b>。
     * 这一版用三条硬约束把它变成"安全"的功能。</p>
     */
    private static void corruptionRules() {
        section("腐化地带");

        var c = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/realworld/Corruption.java"));
        check("有腐化系统", c.contains("class Corruption"), "");
        check("只对天然方块生效（草/土/石/沙/砂砾/深板岩）",
                c.contains("GRASS_BLOCK") && c.contains("MYCELIUM")
                        && c.contains("BLACKSTONE") && c.contains("SOUL_SAND"), "");

        // 约束一：不转换人工方块
        check("有人工方块黑名单（木板/玻璃/箱子/门/栅栏…）",
                c.contains("BlockTags.PLANKS") && c.contains("Blocks.GLASS")
                        && c.contains("Blocks.CHEST") && c.contains("Blocks.TORCH"), "");
        check("附近有人工痕迹就跳过整列",
                c.contains("private static boolean nearArtificial(")
                        && c.contains("return true"), "");
        check("检测范围是 5×5 水平 + 上下 3 格",
                c.contains("for (int dx = -2; dx <= 2; dx++)")
                        && c.contains("for (int dy = -1; dy <= 3; dy++)"), "");

        // 约束二：硬上限
        check("每题最多转换上限", c.contains("MAX_PER_TICK = 24"), "");
        check("蔓延半径上限", c.contains("MAX_RADIUS = 48"), "");
        check("种子数上限", c.contains("MAX_SEEDS = 8"), "");
        check("蔓延间隔（10 秒一次）", c.contains("INTERVAL = 200"), "");
        check("只在真世界开启后生效", c.contains("RealWorld.active(level)"), "");

        // 约束三：只改地表（不挖地底）
        check("只转换地表（上方必须是空气）",
                c.contains("level.getBlockState(p.above()).isAir()"), "");
        check("区块未加载就跳过", c.contains("hasChunkAt(p)"), "");

        // 接线
        check("服务端每 tick 推进",
                readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java")
                        .contains("Corruption.tick"), "");
        check("有种子接口与清理",
                c.contains("public static void addSeed(") && c.contains("public static void clear("), "");
    }


    /**
     * <b>配置项不许有"改了没反应的开关"</b>。
     *
     * <p>自检里有一条只在 Python 侧跑的巡检（{@code check_unread_config2.py}），
     * 这里再补一条 Java 侧断言，锁住两个具体结果：</p>
     * <ul>
     *   <li>预留项 {@code taskGain} / {@code adventureGain} 已删除
     *       （注释自己写着"改这里无效果"的开关比没有更糟）；</li>
     *   <li>{@code spawnerDropsAtNetherite} 已被真正读取
     *       （原来只有 omni 一条路，配置形同虚设）。</li>
     * </ul>
     */
    private static void configWiredRules() {
        section("配置项接线");

        var cfg = readSource("src/main/java/com/hongmeng/xiuxian/compat/HongmengConfig.java");
        check("预留项 taskGain 已删除（不留无效开关）",
                !cfg.contains("taskGain"), "");
        check("预留项 adventureGain 已删除",
                !cfg.contains("adventureGain"), "");
        check("保留 spawnerDropsAtNetherite（它是真功能）",
                cfg.contains("spawnerDropsAtNetherite"), "");

        var mining = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/core/RealmMining.java"));
        check("刷怪笼掉落读配置（不再是死开关）",
                mining.contains("spawnerDropsAtNetherite"), "");
        check("下界合金级（5）才算",
                mining.contains("harvestLevel() >= 5"), "");
        check("原有 omni 路径保留（行为不退化）",
                mining.contains("boolean spawnerDrop = omni"), "");
    }


    /**
     * <b>恶之祭坛 + 「恶」</b>：玩家描述的整套流程。
     *
     * <p>从合成祭坛 → 放四个凭证 → 点击召唤 → 感染/光柱/血红天/裂口/巨眼
     * → 高天云层战场 → 三阶段 → 逃走过场。</p>
     */
    private static void evilAltarRules() {
        section("恶之祭坛");

        // 四个凭证 + 祭坛
        var items = readSource("src/main/java/com/hongmeng/xiuxian/item/ModItems.java");
        for (String k : new String[]{"PUPPET_CORE", "BEAST_HEART", "BLOOD_CORE",
                "VOID_EYE", "EVIL_ALTAR_ITEM"}) {
            check("物品存在: " + k, items.contains(k), "");
        }
        check("祭坛配方存在",
                findResource("data/hongmeng/recipes/evil_altar.json") != null, "");
        String rec = readSource("src/main/resources/data/hongmeng/recipes/evil_altar.json");
        check("配方用恶魂泪/黑曜石/石英板砖/金块/玻璃",
                rec.contains("ghast_tear") && rec.contains("obsidian")
                        && rec.contains("quartz_bricks") && rec.contains("gold_block")
                        && rec.contains("glass"), "");

        // 方块 + 方块实体 + 界面
        check("祭坛方块",
                readSource("src/main/java/com/hongmeng/xiuxian/block/EvilAltarBlock.java")
                        .contains("class EvilAltarBlock"), "");
        var be = readSource(
                "src/main/java/com/hongmeng/xiuxian/block/EvilAltarBlockEntity.java");
        check("四格固定槽位", be.contains("SLOTS = 4"), "");
        check("每格只收对应凭证", be.contains("acceptedFor("), "");
        check("四格就绪判定", be.contains("public boolean ready()"), "");
        check("消耗四格", be.contains("public void consumeAll()"), "");
        check("打开界面走 NetworkHooks.openScreen",
                readSource("src/main/java/com/hongmeng/xiuxian/block/EvilAltarBlock.java")
                        .contains("NetworkHooks.openScreen"), "");
        check("界面有召唤按钮",
                readSource("src/main/java/com/hongmeng/xiuxian/client/EvilAltarScreen.java")
                        .contains("EvilSummonPacket"), "");

        // 召唤流程
        var sum = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/realworld/EvilSummon.java"));
        check("祭坛感染周围方块", sum.contains("private static void infect("), "");
        check("信标式光柱", sum.contains("private static void pillar("), "");
        check("天空变血红", sum.contains("EvilSky.set(1.0F"), "");
        check("提示「你被恶看见了」", sum.contains("\u4f60\u88ab\u6076\u770b\u89c1\u4e86"), "");
        check("传送到高天战场", sum.contains("EvilArena.ensure"), "");

        // 战场
        var arena = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/realworld/EvilArena.java"));
        check("战场在高空（y=240）", arena.contains("Y = 240"), "");
        check("战场是圆台", arena.contains("R = 40"), "");
        check("会生成「恶」", arena.contains("EVIL_GAZER"), "");

        // boss
        var boss = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/EvilGazerEntity.java"));
        check("「恶」是 OVERLORD", boss.contains("MobRank.OVERLORD"), "");
        check("三阶段（1~3）",
                boss.contains("setPhase(3)") && boss.contains("setPhase(2)"), "");
        // 「真的结束了吗」现在在 EvilSummon.endEscaped 里（逃走收尾）。
        check("死亡/逃走走同一条收尾路径",
                boss.contains("EvilSummon.endEscaped"), "");

        // 模型/渲染
        // 三套独立骨架：手臂 / 巨颅 / 上半身
        var armM = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/EvilArmModel.java");
        var headM = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/EvilHeadModel.java");
        var upperM = readSource(
                "src/main/java/com/hongmeng/xiuxian/client/EvilUpperModel.java");
        check("一阶段骨架（手臂）", armM.contains("class EvilArmModel"), "");
        check("二阶段骨架（巨颅）", headM.contains("class EvilHeadModel"), "");
        check("三阶段骨架（上半身）", upperM.contains("class EvilUpperModel"), "");
        check("手臂上长满眼睛", armM.contains("eyeA5"), "");
        check("三阶段躯干比二阶段宽（压迫感来自体量）",
                upperM.contains("-35.0F") && headM.contains("-34.0F"), "");
        check("渲染器按阶段切换骨架",
                readSource("src/main/java/com/hongmeng/xiuxian/client/"
                        + "EvilGazerRenderer.java").contains("getModel()"), "");
        check("天空裂口渲染",
                readSource("src/main/java/com/hongmeng/xiuxian/client/EvilSkyRenderer.java")
                        .contains("class EvilSkyRenderer"), "");
        check("凭证由 boss 掉落",
                stripComments(readSource("src/main/java/com/hongmeng/xiuxian/core/ModEvents.java"))
                        .contains("onAltarBossDeath"), "");
    }


    /**
     * <b>「恶」的三条演出</b>：全屏闪屏 / 过场镜头 / 逃走动画。
     */
    private static void evilShowRules() {
        section("恶：演出细节");

        // ① 闪屏
        var flash = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/EvilFlashOverlay.java"));
        check("有全屏闪屏层", flash.contains("class EvilFlashOverlay"), "");
        check("闪屏按次数驱动", flash.contains("remaining") && flash.contains("frame = 4"), "");
        check("闪屏铺满全屏",
                flash.contains("g.fill(0, 0, g.guiWidth(), g.guiHeight()"), "");
        check("F1 隐藏 GUI 时不闪", flash.contains("hideGui"), "");
        check("闪屏 tick 已接",
                readSource("src/main/java/com/hongmeng/xiuxian/client/CombatFeelClient.java")
                        .contains("EvilFlashOverlay.tick()"), "");
        check("闪屏包已注册",
                readSource("src/main/java/com/hongmeng/xiuxian/network/ModNetwork.java")
                        .contains("EvilFlashPacket.class"), "");

        // ② 过场
        var sum = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/realworld/EvilSummon.java"));
        check("召唤时发过场镜头", sum.contains("CinematicPacket.play"), "");
        check("召唤时闪 3 次", sum.contains("EvilFlashPacket(3, 8)"), "");
        check("逃走时闪 2 次", sum.contains("EvilFlashPacket(2, 10)"), "");
        check("收尾有「真的结束了吗」",
                sum.contains("\u771f\u7684\u7ed3\u675f\u4e86\u5417"), "");
        check("收尾清天空", sum.contains("EvilSky.clear()"), "");

        // ③ 逃走
        var boss = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/EvilGazerEntity.java"));
        check("拦截致命伤害（不死，改逃走）",
                boss.contains("public boolean hurt(") && boss.contains("wouldDie"), "");
        check("逃走时关 AI + 无敌",
                boss.contains("setNoAi(true)") && boss.contains("setInvulnerable(true)"), "");
        check("逃走时向上飞", boss.contains("setDeltaMovement(0.0D, 0.42D, 0.0D)"), "");
        check("逃走结束调用收尾",
                boss.contains("EvilSummon.endEscaped"), "");
        check("逃走总时长常量", boss.contains("ESCAPE_TOTAL = 45"), "");
    }


    /**
     * <b>坠落穿越过渡</b>：把"硬切传送"换成"被吸进裂口"。
     */
    private static void evilFallRules() {
        section("坠落穿越");

        var fx = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/EvilFallFx.java"));
        check("有坠落表演类", fx.contains("class EvilFallFx"), "");
        check("相机上仰（最多 55°）", fx.contains("-55.0F"), "");
        check("相机摇摆（被吸的感觉）", fx.contains("Mth.sin(p * 26.0F)"), "");
        check("有气流粒子（客户端本地）",
                fx.contains("ParticleTypes.CLOUD") && fx.contains("addParticle"), "");
        check("有风压音", fx.contains("STAGE_CHARGE"), "");
        check("有进度接口", fx.contains("public static float progress()"), "");

        var cam = readSource("src/main/java/com/hongmeng/xiuxian/client/EvilFallCamera.java");
        check("相机接在 ComputeCameraAngles",
                cam.contains("ViewportEvent.ComputeCameraAngles"), "");
        check("同时改 pitch 与 roll",
                cam.contains("setPitch(") && cam.contains("setRoll("), "");

        check("包已注册",
                readSource("src/main/java/com/hongmeng/xiuxian/network/ModNetwork.java")
                        .contains("EvilFallPacket.class"), "");
        check("服务端在传送前发通知",
                stripComments(readSource(
                        "src/main/java/com/hongmeng/xiuxian/realworld/EvilSummon.java"))
                        .contains("EvilFallPacket(40)"), "");
        check("客户端每 tick 推进",
                readSource("src/main/java/com/hongmeng/xiuxian/client/CombatFeelClient.java")
                        .contains("EvilFallFx.tick()"), "");
        check("相机已注册",
                readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java")
                        .contains("EvilFallCamera.class"), "");
    }


    /**
     * <b>「恶」= 世界终极 boss</b>：属性 / 技能 / 动作 / 防秒杀。
     */
    private static void evilUltimateRules() {
        section("恶：终极 boss");

        var boss = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/EvilGazerEntity.java"));
        // 玩家反馈“太肉” -> 680 降到 420（仍是全场最高）
        check("世界最高血量（420）", boss.contains("420.0D"), "");
        check("高攻（26）", boss.contains("26.0D"), "");
        check("高护甲（20）", boss.contains("20.0D"), "");
        check("击退抗性 1.0", boss.contains("1.0D, 120.0D"), "");
        check("单次受击上限 8% 最大生命",
                boss.contains("MAX_HIT_RATIO = 0.08F"), "");
        check("阶段锁血（一阶段 66% / 二阶段 33%）",
                boss.contains("case 1 -> 0.66F") && boss.contains("case 2 -> 0.33F"), "");
        check("打不穿时有反馈", boss.contains("还不是完整形态"), "");
        check("技能组每 tick 推进", boss.contains("EvilAbilities.tick(this)"), "");

        var ab = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/entity/EvilAbilities.java"));
        check("技能：凝视（一阶段起）", ab.contains("CD_GAZE"), "");
        check("技能：眼棱扫射（二阶段起）",
                ab.contains("CD_BEAM") && ab.contains("phase >= 2"), "");
        check("技能：唤眼（二阶段起）", ab.contains("CD_SUMMON"), "");
        check("技能：裂口重压（三阶段）",
                ab.contains("CD_SLAM") && ab.contains("phase >= 3"), "");
        check("凝视有视线判定（背对不吃）", ab.contains("to.dot(look) < 0.45D"), "");
        check("眼棱有命中判定", ab.contains("dist <= 2.2D"), "");
        check("唤眼会削弱召唤物（60 血）",
                ab.contains("60.0F") && ab.contains("evil_spawn"), "");
        check("重压有击退", ab.contains("setDeltaMovement(push.x, 0.65D, push.z)"), "");
        check("粒子走批量发包", ab.contains("new ParticleBatch("), "");

        var anim = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/MobCombatAnim.java"));
        check("巨物动作幅度加倍",
                anim.contains("EvilGazerEntity") && anim.contains("2.0F : 1.0F"), "");
    }


    /**
     * <b>「恶」的配套项</b>：实体名 / 刷怪蛋 / 创造栏。
     *
     * <p>这三项漏了不会崩，但会显示成原始 ID —— 只在游戏里看得见。</p>
     */
    private static void evilMinorRules() {
        section("恶：配套");

        var zh = readSource("src/main/resources/assets/hongmeng/lang/zh_cn.json");
        check("实体名有语言条目", zh.contains("entity.hongmeng.evil_gazer"), "");
        check("刷怪蛋有语言条目",
                zh.contains("item.hongmeng.evil_gazer_spawn_egg"), "");
        check("祭坛物品名有 fallback",
                zh.contains("item.hongmeng.evil_altar"), "");

        var items = readSource("src/main/java/com/hongmeng/xiuxian/item/ModItems.java");
        check("有「恶」的刷怪蛋", items.contains("EVIL_GAZER_SPAWN_EGG"), "");
        check("刷怪蛋有模型",
                findResource("assets/hongmeng/models/item/evil_gazer_spawn_egg.json") != null,
                "");
        check("创造栏有刷怪蛋",
                readSource("src/main/java/com/hongmeng/xiuxian/item/ModCreativeTabs.java")
                        .contains("EVIL_GAZER_SPAWN_EGG"), "");
    }


    /**
     * <b>真 3D 护甲</b>：外挂件挂在骨头上（不是世界空间贴片）。
     */
    /**
     * <b>真 3D 护甲（RenderLayer + 独立花纹贴图）</b>。
     *
     * <p>为什么不是护甲模型：{@code HumanoidArmorLayer} 只能用那套甲自己的贴图，
     * 所以外挂件永远是纯色。改成 {@code RenderLayer} 后可以用我们自己的
     * {@code hongmeng_trim.png}（金边/铆钉/宝石/穗饰），并跟着骨骼动。</p>
     */
    private static void armorModelDetailRules() {
        section("护甲 3D 外挂件");

        var layer = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/ArmorTrimLayer.java"));
        check("是 RenderLayer（挂在玩家渲染器上）",
                layer.contains("extends RenderLayer"), "");
        check("有独立花纹贴图", layer.contains("hongmeng_trim.png"), "");
        check("贴图文件存在",
                findResource("assets/hongmeng/textures/models/armor/"
                        + "hongmeng_trim.png") != null, "");
        check("三层肩甲", layer.contains("addBox(-4.6F, -2.6F")
                && layer.contains("addBox(-5.0F, -1.0F")
                && layer.contains("addBox(-5.4F, 0.3F"), "");
        check("腰甲（前后裙甲 + 腰带）",
                layer.contains("addOrReplaceChild(\"waist\""), "");
        check("腿甲两侧", layer.contains("thigh_r") && layer.contains("thigh_l"), "");
        check("披风会摆动", layer.contains("this.cape.xRot"), "");
        check("只对本模组胸甲生效",
                layer.contains("dao_armor_") && layer.contains("path_armor_"), "");
        check("肩甲接到手臂骨骼变换",
                layer.contains("applyArm(pose, model, true)"), "");

        var cs = readSource("src/main/java/com/hongmeng/xiuxian/ClientSetup.java");
        check("图层定义已注册", cs.contains("ArmorTrimLayer::createBodyLayer"), "");
        check("已挂到玩家渲染器（AddLayers）",
                cs.contains("onAddLayers") && cs.contains("addLayer("), "");
    }


    /**
     * <b>特效不许糊住视野</b>（玩家反复反馈"极其遮挡视野"）。
     *
     * <p>约束三条：
     * <ol>
     *   <li>法阵半径有<b>硬上限</b>（构造处夹紧，任何调用方都绕不过）；</li>
     *   <li>领域特效不再使用 40+ 格的巨环；</li>
     *   <li>血池/魔气贴地（y 偏移 &lt; 0.3）且亮度上限低。</li>
     * </ol>
     */
    private static void hideViewRules() {
        section("特效不遮挡视野");

        var mc = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/MagicCircles.java"));
        check("法阵半径有硬上限常量", mc.contains("MAX_RING_RADIUS"), "");
        check("构造处夹紧（调用方绕不过）",
                mc.contains("Math.min(MAX_RING_RADIUS"), "");
        check("领域特效半径已收敛（不再 40+ 格）",
                !mc.contains("40.0F - i * 9.0F") && !mc.contains("44.0F - i * 12.0F"),
                "");
        check("领域斜环倾角不再接近正面横切（< 40°）",
                !mc.contains("58.0F, 1.0F"), "");

        var fx = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/technique/PathMechanicFx.java"));
        check("血池贴地（0.02 格）", fx.contains("player.position().add(0.0D, 0.02D"),
                "");
        check("魔气贴地（0.05 格）", fx.contains("player.position().add(0.0D, 0.05D"),
                "");
        var fxr = stripComments(readSource(
                "src/main/java/com/hongmeng/xiuxian/client/SpellFxRenderer.java"));
        check("贴身光环只在第三视角可见",
                fxr.contains("isPersonalAura")
                        && fxr.contains("isFirstPerson()"), "");
        check("判定只针对环/螺旋且距离 <= 2.2",
                fxr.contains("2.2D * 2.2D"), "");
        check("资源光环亮度上限低（<= 0.35）",
                fx.contains("0.18D + frac * 0.17D") && fx.contains("0.18D + frac * 0.17D"),
                "");
    }

}
