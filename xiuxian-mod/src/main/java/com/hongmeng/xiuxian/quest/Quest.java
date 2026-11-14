package com.hongmeng.xiuxian.quest;

import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一个任务的定义。
 *
 * <p>任务只存"定义"，进度存在玩家身上（见 {@link QuestProgress}）。
 * 每条任务有若干个目标，全部达成就可领奖。</p>
 */
public record Quest(
        String id,
        String chapter,
        String title,
        String description,
        Requirement requirement,
        String hint,
        int rewardCultivation,
        String rewardItemId,
        int rewardItemCount,
        String rewardItemName,
        /** 灵叶奖励（坊市货币）。 */
        int rewardLeaves
) {

    /** 只有一种条件类型，够用且好验证。 */
    public interface Requirement {
        /** 目标描述，例如 "击杀敌对生物 3/5"。 */
        Component describe(QuestProgress progress);

        /** 当前进度是否达成。 */
        boolean isMet(QuestProgress progress);

        /** 进度比例 0~1，用于画进度条。 */
        float ratio(QuestProgress progress);

        /** 目标数值（用于显示 x/y）。 */
        int target();

        int current(QuestProgress progress);
    }

    // ------------------------------------------------------------------
    // 条件工厂
    // ------------------------------------------------------------------

    /** 累计击杀敌对生物。 */
    public static Requirement kills(int amount) {
        return simple("击杀敌对生物", amount,
                QuestProgress::getKills,
                p -> p.getKills() >= amount);
    }

    /** 累计打坐秒数。 */
    public static Requirement meditate(int seconds) {
        return simple("打坐吐纳", seconds,
                QuestProgress::getMeditateSeconds,
                p -> p.getMeditateSeconds() >= seconds);
    }

    /** 累计采集灵草。 */
    public static Requirement gatherGrass(int amount) {
        return simple("采集灵草", amount,
                QuestProgress::getGrassGathered,
                p -> p.getGrassGathered() >= amount);
    }

    /** 达到某个累计修为。 */
    public static Requirement cultivation(long amount) {
        return new Requirement() {
            @Override
            public Component describe(QuestProgress progress) {
                return Component.literal("修为达到 " + amount);
            }

            @Override
            public boolean isMet(QuestProgress progress) {
                return progress.getTotalCultivation() >= amount;
            }

            @Override
            public float ratio(QuestProgress progress) {
                return Math.min(1.0F, progress.getTotalCultivation() / (float) amount);
            }

            @Override
            public int target() {
                return (int) Math.min(Integer.MAX_VALUE, amount);
            }

            @Override
            public int current(QuestProgress progress) {
                return (int) Math.min(Integer.MAX_VALUE, progress.getTotalCultivation());
            }
        };
    }

    /** 境界序号达到 n。 */
    public static Requirement realmTier(int tier, String label) {
        return new Requirement() {
            @Override
            public Component describe(QuestProgress progress) {
                return Component.literal("境界达到 " + label);
            }

            @Override
            public boolean isMet(QuestProgress progress) {
                return progress.getMaxRealmTier() >= tier;
            }

            @Override
            public float ratio(QuestProgress progress) {
                return Math.min(1.0F, progress.getMaxRealmTier() / (float) tier);
            }

            @Override
            public int target() {
                return tier;
            }

            @Override
            public int current(QuestProgress progress) {
                return progress.getMaxRealmTier();
            }
        };
    }

    /** 拜入某个门派。 */
    public static Requirement sectJoin() {
        return flag("拜入宗门", QuestProgress::isInSect);
    }

    /** 灵根觉醒。 */
    public static Requirement awaken() {
        return flag("灵根觉醒", QuestProgress::isAwakened);
    }

    /** 藏经阁学习（用"靠近宗门"简化成境界要求，避免加复杂交互）。 */
    public static Requirement learnTechnique() {
        return flag("入门传功", QuestProgress::isTechniqueLearned);
    }

    /** 清空业力。 */
    public static Requirement clearKarma() {
        return flag("业力清零", p -> p.getKarma() <= 0);
    }

    /** 拥有道宫。 */
    public static Requirement haveDomain(int count) {
        return simple("凝聚道宫", count,
                QuestProgress::getDomainCount,
                p -> p.getDomainCount() >= count);
    }

    /** 通关纪元秘境。 */
    public static Requirement dungeonClear(int count) {
        return simple("通关纪元秘境", count,
                QuestProgress::getDungeonClears,
                p -> p.getDungeonClears() >= count);
    }

    /** 通关"化神试炼"（道印之前的那个本），用于拿双道宫丹。 */
    public static Requirement primalTrialClear() {
        return flag("通关化神试炼", QuestProgress::isTrialCleared);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private interface IntGetter {
        int get(QuestProgress progress);
    }

    private static Requirement simple(String label, int target, IntGetter getter,
                                     java.util.function.Predicate<QuestProgress> done) {
        return new Requirement() {
            @Override
            public Component describe(QuestProgress progress) {
                return Component.literal(label + " " + getter.get(progress) + "/" + target);
            }

            @Override
            public boolean isMet(QuestProgress progress) {
                return done.test(progress);
            }

            @Override
            public float ratio(QuestProgress progress) {
                return target <= 0 ? 1.0F
                        : Math.min(1.0F, getter.get(progress) / (float) target);
            }

            @Override
            public int target() {
                return target;
            }

            @Override
            public int current(QuestProgress progress) {
                return getter.get(progress);
            }
        };
    }

    private static Requirement flag(String label,
                                    java.util.function.Predicate<QuestProgress> done) {
        return new Requirement() {
            @Override
            public Component describe(QuestProgress progress) {
                return Component.literal(label + (done.test(progress) ? " ✓" : ""));
            }

            @Override
            public boolean isMet(QuestProgress progress) {
                return done.test(progress);
            }

            @Override
            public float ratio(QuestProgress progress) {
                return done.test(progress) ? 1.0F : 0.0F;
            }

            @Override
            public int target() {
                return 1;
            }

            @Override
            public int current(QuestProgress progress) {
                return done.test(progress) ? 1 : 0;
            }
        };
    }

    /** 本章节。 */
    public static Map<String, java.util.List<Quest>> byChapter() {
        Map<String, java.util.List<Quest>> map = new LinkedHashMap<>();
        for (Quest q : Quests.ALL) {
            map.computeIfAbsent(q.chapter(), k -> new java.util.ArrayList<>()).add(q);
        }
        return map;
    }
}
