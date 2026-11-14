package com.hongmeng.xiuxian.quest;

import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 一个玩家的任务进度。
 *
 * <p>计数器（击杀/打坐/采药）存在这里；
 * 其余条件（修为、境界、门派、业力、副本次数）直接从 {@link CultivationData} 现算，
 * 不重复存，避免两份数据打架。</p>
 */
public class QuestProgress {

    /** 每个任务的完成状态：0=可接(未完成) 1=已完成待领奖 2=已领奖。 */
    private final Map<String, Integer> states = new LinkedHashMap<>();

    // ---- 计数器 ----
    private int kills = 0;
    private int meditateSeconds = 0;
    private int grassGathered = 0;
    private int domainCount = 0;
    private boolean trialCleared = false;

    // ---- 从别处同步过来的实时值 ----
    // 刻意"按值拷贝"而不持有 CultivationData 引用：两者互相引用会让
    // CultivationData.clone() / equals 变成无限递归（栈溢出）。
    private transient boolean liveAwakened = false;
    private transient long liveTotalCultivation = 0L;
    private transient int liveRealmTier = 1;
    private transient boolean liveInSect = false;
    private transient int liveKarma = 0;
    private transient int liveDungeonClears = 0;

    // ------------------------------------------------------------------
    // 绑定与构造
    // ------------------------------------------------------------------

    public static QuestProgress of(ServerPlayer player) {
        CultivationData data = CultivationCapability.get(player);
        QuestProgress progress = data.getQuestProgress();
        progress.refresh(data);
        return progress;
    }

    /** 把 CultivationData 的实时值拷进本对象（不保存引用）。 */
    public void refresh(CultivationData data) {
        if (data == null) {
            return;
        }
        this.liveAwakened = data.isAwakened();
        this.liveTotalCultivation = data.getTotalCultivation();
        this.liveRealmTier = data.realm().tier();
        this.liveInSect = !data.sect().isNone();
        this.liveKarma = data.getKarma();
        this.liveDungeonClears = data.getDungeonClears();
    }

    // ------------------------------------------------------------------
    // 状态查询
    // ------------------------------------------------------------------

    /** 0=未完成 1=完成待领奖 2=已领奖。 */
    public int stateOf(String questId) {
        return states.getOrDefault(questId, 0);
    }

    public void setState(String questId, int state) {
        states.put(questId, state);
    }

    public boolean isClaimed(String questId) {
        return stateOf(questId) >= 2;
    }

    /**
     * 该任务现在是否应该显示为"已完成"。
     * 到条件达成的那一刻，从未完成翻成"待领奖"。
     */
    public boolean evaluate(Quest quest) {
        if (isClaimed(quest.id())) {
            return true;
        }
        boolean met = quest.requirement().isMet(this);
        if (met && stateOf(quest.id()) == 0) {
            states.put(quest.id(), 1);
        }
        return met;
    }

    /** 任务是否可接（前一条任务已完成）。 */
    public boolean isUnlocked(Quest quest) {
        int index = Quests.ALL.indexOf(quest);
        if (index <= 0) {
            return true;
        }
        Quest prev = Quests.ALL.get(index - 1);
        return isClaimed(prev.id());
    }

    // ------------------------------------------------------------------
    // 计数器
    // ------------------------------------------------------------------

    public int getKills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }

    public int getMeditateSeconds() {
        return meditateSeconds;
    }

    public void addMeditateSecond() {
        meditateSeconds++;
    }

    public int getGrassGathered() {
        return grassGathered;
    }

    public void addGrass() {
        grassGathered++;
    }

    public int getDomainCount() {
        return domainCount;
    }

    public void setDomainCount(int value) {
        this.domainCount = Math.max(0, value);
    }

    public boolean isTrialCleared() {
        return trialCleared;
    }

    public void setTrialCleared(boolean value) {
        this.trialCleared = value;
    }

    // ---- 实时值访问器 ----

    public boolean isAwakened() {
        return liveAwakened;
    }

    public long getTotalCultivation() {
        return liveTotalCultivation;
    }

    public int getMaxRealmTier() {
        return liveRealmTier;
    }

    public boolean isInSect() {
        return liveInSect;
    }

    public boolean isTechniqueLearned() {
        return liveInSect && liveRealmTier >= 3;
    }

    public int getKarma() {
        return liveKarma;
    }

    public int getDungeonClears() {
        return liveDungeonClears;
    }

    // ------------------------------------------------------------------
    // 存档
    // ------------------------------------------------------------------

    public void save(CompoundTag tag) {
        CompoundTag st = new CompoundTag();
        states.forEach((k, v) -> st.putInt(k, v));
        tag.put("QuestStates", st);
        tag.putInt("QuestKills", kills);
        tag.putInt("QuestMeditate", meditateSeconds);
        tag.putInt("QuestGrass", grassGathered);
        tag.putInt("QuestDomains", domainCount);
        tag.putBoolean("QuestTrial", trialCleared);
    }

    public void load(CompoundTag tag) {
        states.clear();
        CompoundTag st = tag.getCompound("QuestStates");
        for (String key : st.getAllKeys()) {
            states.put(key, st.getInt(key));
        }
        kills = tag.getInt("QuestKills");
        meditateSeconds = tag.getInt("QuestMeditate");
        grassGathered = tag.getInt("QuestGrass");
        domainCount = tag.getInt("QuestDomains");
        trialCleared = tag.getBoolean("QuestTrial");
    }

    public void copyFrom(QuestProgress other) {
        states.clear();
        states.putAll(other.states);
        kills = other.kills;
        meditateSeconds = other.meditateSeconds;
        grassGathered = other.grassGathered;
        domainCount = other.domainCount;
        trialCleared = other.trialCleared;
    }

    // ------------------------------------------------------------------
    // 网络同步（客户端只需要状态表 + 追踪用的少量计数器）
    // ------------------------------------------------------------------

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(states.size());
        states.forEach((k, v) -> {
            buf.writeUtf(k);
            buf.writeVarInt(v);
        });
        buf.writeVarInt(kills);
        buf.writeVarInt(meditateSeconds);
        buf.writeVarInt(grassGathered);
        buf.writeVarInt(domainCount);
        buf.writeBoolean(trialCleared);
    }

    public static QuestProgress fromNetwork(FriendlyByteBuf buf) {
        QuestProgress p = new QuestProgress();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            p.states.put(buf.readUtf(), buf.readVarInt());
        }
        p.kills = buf.readVarInt();
        p.meditateSeconds = buf.readVarInt();
        p.grassGathered = buf.readVarInt();
        p.domainCount = buf.readVarInt();
        p.trialCleared = buf.readBoolean();
        return p;
    }

    /** 调试用文本。 */
    public String debugSummary() {
        return String.format(Locale.ROOT,
                "kills=%d meditate=%d grass=%d domains=%d trial=%s states=%d",
                kills, meditateSeconds, grassGathered, domainCount, trialCleared, states.size());
    }
}
