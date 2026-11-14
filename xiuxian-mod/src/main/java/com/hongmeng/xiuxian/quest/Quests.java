package com.hongmeng.xiuxian.quest;

import java.util.List;

/**
 * 全部任务定义。
 *
 * <p>按原文的开局线编排：落地 → 吐纳 → 试手 → 采药 → 入宗 → 修行 → 道印 → 双道宫。</p>
 */
public final class Quests {

    public static final String CHAPTER_OPENING = "第一章 · 初入鸿蒙";
    public static final String CHAPTER_SECT = "第二章 · 神霄剑宗";
    public static final String CHAPTER_DAO = "第三章 · 道印之路";

    public static final List<Quest> ALL = List.of(

            // ---------------- 第一章 ----------------
            new Quest("q_awaken", CHAPTER_OPENING, "灵根既定",
                    "你被塞进了《鸿蒙修仙传》，连一句解释都没有。先看看自己是什么灵根。",
                    Quest.awaken(), "觉醒会自动发生，稍等几秒。",
                    80, "", 0, "", 30),

            new Quest("q_meditate", CHAPTER_OPENING, "吐纳入门",
                    "找张蒲团坐下，把灵气引进经脉。这一步没人能替你走。",
                    Quest.meditate(30), "合成：4 白羊毛 + 1 灵叶 = 打坐蒲团（灵叶打野外发光的灵草掉）。坐下就能吐纳。",
                    120, "hongmeng:zhaohua_dan", 1, "造化丹", 40),

            new Quest("q_hunt", CHAPTER_OPENING, "试试身手",
                    "光会打坐没用。出去杀几只，看看你这身修为够不够看。",
                    Quest.kills(3), "夜里怪物多，或者去洞里找。",
                    200, "hongmeng:pill_huiling", 2, "回灵丹 ×2", 50),

            new Quest("q_grass", CHAPTER_OPENING, "一株灵草",
                    "灵气不只在经脉里，也长在野地里。采一株回来。",
                    Quest.gatherGrass(1), "野外会发光的草就是灵草，空手就能采。",
                    260, "hongmeng:lingye", 3, "灵叶 ×3", 60),

            new Quest("q_join", CHAPTER_OPENING, "拜入神霄剑宗",
                    "散修没前途。神霄剑宗是剑修正统，宗内只认剑——去拜个山门。",
                    Quest.sectJoin(), "输入 /sect join shenxiao 即可拜入（练气就能进）。",
                    300, "hongmeng:sect_token_shenxiao", 1, "神霄剑宗令牌", 80),

            // ⚠ 下面两条是"系统发现"任务：道途与副业此前在任务链里
            //   **一个字都没有**，玩家按正常流程玩根本不会知道它们存在。

            new Quest("q_path_choose", CHAPTER_OPENING, "择一条道",
                    "修行有五条大路：剑修重剑、血修重命、魔修重势、"
                            + "鬼修重魂、体修重身。没有高低，只有合不合你。",
                    Quest.realmTier(2, "筑基"),
                    "按 R 打开修炼面板，在「职业」一栏选一个"
                            + "（也可以 /hongmeng path <剑修|血修|魔修|鬼修|体修>）。"
                            + "选了会送 3 点天赋。",
                    320, "hongmeng:pill_huiling", 3, "回灵丹 ×3", 90),

            new Quest("q_profession", CHAPTER_OPENING, "不止一条路",
                    "修为之外还有手艺。炼丹出丹药、炼器出装备、阵法出符阵 ——"
                            + "三门副业可以兼修，不占天赋点。",
                    Quest.gatherGrass(5),
                    "输入 /hongmeng ui profession 打开副业界面，或者直接输"
                            + " /hongmeng craft 看配方。熟练度越高成功率越高。",
                    280, "hongmeng:lingye", 5, "灵叶 ×5", 70),

            // ---------------- 第二章 ----------------
            new Quest("q_grass10", CHAPTER_SECT, "宗门的差事",
                    "宗门不养闲人。再采一些灵草，权当交差。",
                    Quest.gatherGrass(10), "灵草分布在草地与沙地上。",
                    500, "hongmeng:pill_zhuji", 1, "筑基丹", 100),

            new Quest("q_kill30", CHAPTER_SECT, "剑试三十",
                    "三十条命换一个手感。宗里的执事会看着你。",
                    Quest.kills(30), "累计击杀，之前的击杀也算。",
                    1200, "hongmeng:pill_pojing", 1, "破境丹", 140),

            new Quest("q_zhuji", CHAPTER_SECT, "筑基",
                    "练气是引气入体，筑基才是真正把根扎下去。",
                    Quest.realmTier(2, "筑基"), "修为够了会自动破境，也可以用 B 键主动催动。",
                    2000, "hongmeng:pill_zhuji", 2, "筑基丹 ×2", 180),

            new Quest("q_jindan", CHAPTER_SECT, "金丹",
                    "金丹一成，天地之间才算有你一号人。",
                    Quest.realmTier(3, "金丹"), "金丹之后空手就顶得上石镐了。",
                    4000, "hongmeng:pill_pojing", 1, "破境丹", 220),

            new Quest("q_karma", CHAPTER_SECT, "洗去业力",
                    "修行路上死过几次，道基沾了灰。清干净再往上走。",
                    Quest.clearKarma(), "附魔金苹果一次清空，或者替村庄解决劫掠（每次 -1 层）。",
                    3000, "hongmeng:pill_xiling", 1, "洗灵丹", 260),

            // ---------------- 第三章 ----------------
            new Quest("q_huashen", CHAPTER_DAO, "化神",
                    "元婴之后是化神。到了这一境，你的脚程就该比常人快一截了。",
                    Quest.realmTier(5, "化神"), "化神解锁常驻速度 I。",
                    8000, "hongmeng:pill_pojing", 2, "破境丹 ×2", 320),

            new Quest("q_domain", CHAPTER_DAO, "凝道宫",
                    "道印境的标志，是在灵海里凝出一座道宫。去突破到道印，把它凝出来。",
                    Quest.realmTier(6, "道印"), "突破到道印后，按 G 打开道宫界面凝聚。",
                    15000, "hongmeng:daoyin", 1, "道印", 400),

            new Quest("q_trial", CHAPTER_DAO, "化神试炼",
                    "化神之后、道印之前，有一处试炼秘境。闯过它，才有资格谈双道宫。",
                    Quest.primalTrialClear(), "用纪元之门框架搭门，点火进入化神试炼。",
                    20000, "hongmeng:pill_shuangdao", 1, "双道宫丹", 480),

            new Quest("q_dual", CHAPTER_DAO, "双道宫",
                    "服下双道宫丹，再凝一座道宫。两座道宫，两种法则。",
                    Quest.haveDomain(2), "道宫界面里可以凝第二座（需要双道宫丹）。",
                    40000, "hongmeng:daoyin", 2, "道印 ×2", 600),

            new Quest("q_epoch", CHAPTER_DAO, "纪元任务",
                    "该去纪元秘境走一趟了。三波敌潮，熬过去。",
                    Quest.dungeonClear(1), "搭纪元之门，进去打三波。",
                    60000, "hongmeng:jidan", 2, "纪元令 ×2", 800)
    );

    private Quests() {
    }

    public static Quest byId(String id) {
        for (Quest q : ALL) {
            if (q.id().equals(id)) {
                return q;
            }
        }
        return null;
    }

    /** 任务在总表里的序号（1 开始），用于显示。 */
    public static int indexOf(Quest quest) {
        return ALL.indexOf(quest) + 1;
    }

    public static int total() {
        return ALL.size();
    }
}
