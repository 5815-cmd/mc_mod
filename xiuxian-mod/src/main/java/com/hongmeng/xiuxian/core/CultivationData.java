package com.hongmeng.xiuxian.core;

import com.hongmeng.xiuxian.spirit.SpiritRoot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 一个玩家的全部修炼状态。纯数据 + 存取，不放逻辑。
 */
public class CultivationData {

    // ---- 灵根 ----
    private SpiritRoot root = null;
    private boolean awakened = false;
    private int rerollCount = 0;

    // ---- 修为 ----
    private long cultivation = 0L;
    private long totalCultivation = 0L;

    // ---- 业力 ----
    private int karma = 0;

    // ---- 门派 ----
    private String sect = "";
    private int reputation = 0;
    /** 门派贡献度，决定身份（外门→内门→真传→执事→长老→太上长老）。 */
    private int contribution = 0;
    /** 是否背负"叛徒"名号：叛门后原门派会派人清理门户。 */
    private boolean betrayed = false;
    /** 上次被清理门户的 tick（用于冷却）。 */
    private long lastSectHunt = 0L;

    // ---- 任务 / 副本 ----
    private String questStage = "none";
    private int epochSeason = 1;
    private int dungeonClears = 0;

    // ------------------------------------------------------------------
    // 坊市：灵叶余额
    // ------------------------------------------------------------------
    /**
     * 存在坊市里的灵叶余额。
     *
     * <p><b>为什么要有这个字段：</b>以前灵叶是纯物品，杀怪与任务奖励往背包里塞，
     * 背包很快被一堆灵叶占满。现在灵叶是"存在坊市的钱"：所有奖励直接进余额，
     * 背包里的实物灵叶只是<b>还没存进去的部分</b>（采灵草掉的是实物，可以一键存入），
     * 花费时先扣余额、再扣背包实物。</p>
     *
     * <p>老存档没有这个字段，读档时按 0 处理（见 {@link #load}），不会报错也不会凭空多钱。</p>
     */
    private int leaves = 0;

    /** 坊市余额。 */
    public int getLeaves() {
        return leaves;
    }

    public void setLeaves(int value) {
        this.leaves = Math.max(0, value);
    }

    /** 增加余额，返回加完之后的余额。 */
    public int addLeaves(int amount) {
        if (amount > 0) {
            this.leaves = (int) Math.min(Integer.MAX_VALUE, (long) this.leaves + amount);
        }
        return this.leaves;
    }

    /**
     * 从余额里扣钱。
     *
     * @return true 表示余额够并已扣除；false 表示余额不足（不改动任何数值）
     */
    public boolean takeLeaves(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (this.leaves < amount) {
            return false;
        }
        this.leaves -= amount;
        return true;
    }

    // ------------------------------------------------------------------
    // 灵力（mana）
    // ------------------------------------------------------------------
    /**
     * 当前灵力。
     *
     * <p><b>刻意不做成 Attribute。</b>Attribute 会撞上 {@code maxValue} 那套上限坑、
     * 还要额外同步，而且没法离线断言。这里就是一个 int，上限由
     * {@link ManaTable#maxForTier(int)} 按大境序号算出来。</p>
     *
     * <p>初值刻意用 {@link #MANA_UNSET} 而不是 0：老存档（还没有 {@code Mana} 键）
     * 读出来应该是<b>满灵力</b>——玩家不该一登录就空蓝。</p>
     */
    private int mana = MANA_UNSET;

    /** 「还没被赋值」的哨兵：任何读取都会把它当成满灵力。 */
    public static final int MANA_UNSET = -1;

    /**
     * 回复的小数余量。
     *
     * <p>0.5%/秒 摊到每 tick 只有 0.025% —— 金丹也就 0.3 点/tick，
     * 直接取整会永远回不了蓝。所以零头攒在这里，攒够 1 点再进位。</p>
     */
    private double manaFraction = 0.0D;

    /** 灵力上限：纯粹由当前大境推出。 */
    public int maxMana() {
        return ManaTable.maxForTier(realm().tier());
    }

    /** 当前灵力（已夹进 [0, maxMana]）。 */
    public int getMana() {
        if (mana == MANA_UNSET) {
            // 老存档 / 从没被写过：满灵力开局
            mana = maxMana();
        }
        int max = maxMana();
        if (mana > max) {
            mana = max;
        } else if (mana < 0) {
            mana = 0;
        }
        return mana;
    }

    public void setMana(int value) {
        int max = maxMana();
        this.mana = Math.max(0, Math.min(max, value));
    }

    /** 直接扣灵力。返回扣完之后的值（不会扣成负数）。 */
    public int spendMana(double amount) {
        if (amount <= 0.0D) {
            return getMana();
        }
        setMana((int) Math.floor(getMana() - amount));
        return getMana();
    }

    public void addMana(double amount) {
        if (amount <= 0.0D) {
            return;
        }
        setMana((int) Math.floor(getMana() + amount));
    }

    public void refillMana() {
        this.manaFraction = 0.0D;
        setMana(maxMana());
    }

    /** 是否已经空了（御剑中断的判据）。 */
    public boolean isManaEmpty() {
        return getMana() <= 0;
    }

    /** 灵力条用的比例（0~1）。 */
    public float manaRatio() {
        int max = maxMana();
        if (max <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (float) getMana() / (float) max));
    }

    // ---- 痛觉 / 演出 ----
    private int painLevel = 0;

    // ---- 帝境的速度 II 开关（修炼面板里可切换） ----
    private boolean speedBoostEnabled = true;

    /**
     * <b>御剑术是否已解锁</b>（剑阵的获取门槛）。
     *
     * <p>玩家问「剑阵为什么开局就自带」。原来只看境界 ≥ 第 2 境，
     * 等于第二次突破就白送；现在要读过《御剑术秘籍》才解锁。</p>
     *
     * <p>⚠ 这个字段有四个出口，都得补：{@code save/load}、{@code toNetwork/fromNetwork}、
     * {@code copyFrom}。漏一个就会出现"重登又锁上 / 死一次就丢"这类怪现象。</p>
     */
    private boolean swordArtUnlocked = false;

    // ---- 任务进度 ----
    private final com.hongmeng.xiuxian.quest.QuestProgress questProgress =
            new com.hongmeng.xiuxian.quest.QuestProgress();

    public com.hongmeng.xiuxian.quest.QuestProgress getQuestProgress() {
        return questProgress;
    }

    // ---- 道宫 ----
    private final com.hongmeng.xiuxian.domain.DomainData domainData =
            new com.hongmeng.xiuxian.domain.DomainData();

    public com.hongmeng.xiuxian.domain.DomainData getDomainData() {
        return domainData;
    }

    // ---- 本命领域的友伤开关（规格 §2.1：默认关，R 面板里可切） ----
    //
    // 放这里而不是 DomainData：DomainData 是"道宫本身"（品质/元素/能量），
    // 友伤是玩家的一个<b>战斗设置</b>，和速度 II 开关（speedBoostEnabled）同类。
    // NBT 与网络都跟着 CultivationData 走，客户端也拿得到（R 面板与领域特效都要读它）。
    private boolean domainFriendlyFire = false;

    /** 领域是否伤害友善目标（玩家 / 驯服的宠物坐骑 / NPC）。默认 false。 */
    public boolean isDomainFriendlyFire() {
        return domainFriendlyFire;
    }

    public void setDomainFriendlyFire(boolean value) {
        this.domainFriendlyFire = value;
    }

    // ---- 灵根保底：连续没出"天灵根及以上"就累积，满 10 次下次强制走高档池 ----
    private int rootRollCount = 0;

    /** 累计"没出好货"的次数。 */
    public int getRootRollCount() {
        return rootRollCount;
    }

    public void setRootRollCount(int value) {
        this.rootRollCount = Math.max(0, value);
    }

    /** 还差几次触发保底。 */
    public int rollsUntilPity() {
        return Math.max(0,
                com.hongmeng.xiuxian.spirit.SpiritRoot.PITY_THRESHOLD - rootRollCount);
    }

    /** 下一次抽取是否已经触发保底。 */
    public boolean isPityReady() {
        return rootRollCount >= com.hongmeng.xiuxian.spirit.SpiritRoot.PITY_THRESHOLD;
    }

    /** 抽到好灵根时重置计数。 */
    public void resetRootPity() {
        this.rootRollCount = 0;
    }

    /** 抽到不理想的灵根时累加。 */
    public void bumpRootPity() {
        this.rootRollCount++;
    }

    // ---- 功法（已修习 + 冷却） ----
    // 和 PlayerEquipment 一样惰性创建：TechniqueData 本身不碰注册表，
    // 但保持一致的风格，也避免以后有人往里加注册表依赖时踩坑。
    private com.hongmeng.xiuxian.technique.TechniqueData techniques;

    public com.hongmeng.xiuxian.technique.TechniqueData getTechniques() {
        if (techniques == null) {
            techniques = new com.hongmeng.xiuxian.technique.TechniqueData();
        }
        return techniques;
    }
    // ---- 法宝装备栏（五个槽位，只有装备上的才生效） ----
    // 刻意惰性创建：PlayerEquipment 的构造会碰 ItemStack.EMPTY，
    // 而 ItemStack 的静态初始化需要注册表 bootstrap——如果在这里直接 new，
    // 离线验证器一构造 CultivationData 就会炸（Not bootstrapped）。
    private com.hongmeng.xiuxian.item.gear.PlayerEquipment equipment;

    public com.hongmeng.xiuxian.item.gear.PlayerEquipment getEquipment() {
        if (equipment == null) {
            equipment = new com.hongmeng.xiuxian.item.gear.PlayerEquipment();
        }
        return equipment;
    }

    // ---- 开局难度 ----
    private String difficulty = "";

    /** 是否已经选过难度（未选时开局会弹难度选择界面）。 */
    public boolean hasChosenDifficulty() {
        return difficulty != null && !difficulty.isEmpty();
    }

    public String getDifficultyId() {
        return difficulty == null ? "" : difficulty;
    }

    public void setDifficultyId(String id) {
        this.difficulty = id == null ? "" : id;
    }

    public com.hongmeng.xiuxian.difficulty.CultivationDifficulty difficulty() {
        return com.hongmeng.xiuxian.difficulty.CultivationDifficulty.byId(getDifficultyId());
    }

    public boolean isSpeedBoostEnabled() {
        return speedBoostEnabled;
    }

    public void setSpeedBoostEnabled(boolean value) {
        this.speedBoostEnabled = value;
    }

    /**
     * 御剑术是否已解锁（剑阵的获取门槛）。
     *
     * <p>玩家问「剑阵为什么开局就自带」—— 原来只看境界 ≥ 第 2 境，
     * 等于第二次突破就白送。现在要读过《御剑术秘籍》才永久解锁。</p>
     */
    public boolean getSwordArtUnlocked() {
        return swordArtUnlocked;
    }

    public void setSwordArtUnlocked(boolean value) {
        this.swordArtUnlocked = value;
    }

    public void toggleSpeedBoost() {
        this.speedBoostEnabled = !this.speedBoostEnabled;
    }

    public SpiritRoot getRoot() {
        return root;
    }

    public void setRoot(SpiritRoot root) {
        this.root = root;
    }

    public boolean isAwakened() {
        return awakened;
    }

    public void setAwakened(boolean awakened) {
        this.awakened = awakened;
    }

    public int getRerollCount() {
        return rerollCount;
    }

    public void addReroll() {
        rerollCount++;
    }

    public long getCultivation() {
        return cultivation;
    }

    public void setCultivation(long value) {
        this.cultivation = Math.max(0L, value);
    }

    public void addCultivation(long amount) {
        if (amount > 0) {
            this.cultivation += amount;
            this.totalCultivation += amount;
        }
    }

    /**
     * 当前小境界的修为上限（累计值）。
     *
     * <p>修为到这里就封顶，必须主动突破才能继续涨。</p>
     */
    public long cultivationCap() {
        Realm r = realm();
        return r.minTotal() + r.need();
    }

    /** 修为是否已经攒满，可以催动突破。 */
    public boolean isCultivationFull() {
        return getCultivation() >= cultivationCap();
    }

    /** 还差多少修为才满。 */
    public long cultivationMissing() {
        return Math.max(0L, cultivationCap() - getCultivation());
    }

    // ------------------------------------------------------------------
    // 已解锁的境界层数：突破的唯一产物
    // ------------------------------------------------------------------

    /**
     * 玩家已经突破到的层号（0 = 练气初期）。
     *
     * <p><b>为什么需要这个：</b>以前境界是"累计修为的纯函数"，修为一到就自动跨层——
     * 结果玩家会一路无声无息滑到高境界，永远不需要点突破，修为封顶也就无从谈起。
     *
     * <p>现在境界由**突破次数**决定：修为在本层封顶，只有主动破境才会推进层号。
     * 存档里没这个字段的老档（{@code STEP} 缺失）会按累计修为自动补一个合理的值，
     * 不会让老玩家掉境界。</p>
     */
    private int unlockedStep = -1;

    /** 当前层号（会按需初始化）。 */
    public int unlockedStep() {
        if (unlockedStep < 0) {
            // 老存档迁移：按累计修为取"如果一路自动晋级会到哪一层"
            unlockedStep = Realm.indexOfTotal(totalCultivation);
        }
        return unlockedStep;
    }

    public void setUnlockedStep(int step) {
        this.unlockedStep = Math.max(0, Math.min(Realm.TOTAL_STEPS - 1, step));
    }

    /** 突破成功时调用：层号 +1。 */
    public boolean advanceStep() {
        int cur = unlockedStep();
        if (cur >= Realm.TOTAL_STEPS - 1) {
            return false;
        }
        unlockedStep = cur + 1;
        return true;
    }

    /** 该层是否已经是最后一层。 */
    public boolean isAtLastStep() {
        return unlockedStep() >= Realm.TOTAL_STEPS - 1;
    }

    /**
     * 把修为夹到当前层的上限内。
     *
     * <p>读档后必须调一次：境界现在由层号决定，而老存档里的修为可能远高于
     * 当前层的上限（以前境界跟着修为自动涨，现在不跟了）。不夹的话会出现
     * "修为 1.8 亿 / 本层需要 148 万"这种荒谬显示。</p>
     *
     * <p>夹掉的部分不退还，但 {@code totalCultivation} 保留原值作为历史记录。</p>
     */
    public void clampCultivationToStep() {
        long cap = cultivationCap();
        if (cultivation > cap) {
            cultivation = cap;
        }
        if (cultivation < 0L) {
            cultivation = 0L;
        }
    }

    public long getTotalCultivation() {
        return totalCultivation;
    }

    public int getKarma() {
        return karma;
    }

    public void setKarma(int value) {
        this.karma = Math.max(0, value);
    }

    public String getSect() {
        return sect == null ? "" : sect;
    }

    public void setSect(String sect) {
        this.sect = sect == null ? "" : sect;
    }

    public int getReputation() {
        return reputation;
    }

    public void setReputation(int value) {
        this.reputation = value;
    }

    public int getContribution() {
        return contribution;
    }

    public void setContribution(int value) {
        this.contribution = Math.max(0, value);
    }

    public void addContribution(int amount) {
        if (amount > 0) {
            this.contribution += amount;
        }
    }

    public boolean isBetrayed() {
        return betrayed;
    }

    public void setBetrayed(boolean betrayed) {
        this.betrayed = betrayed;
    }

    public long getLastSectHunt() {
        return lastSectHunt;
    }

    public void setLastSectHunt(long tick) {
        this.lastSectHunt = tick;
    }

    /** 当前门派对象。 */
    public com.hongmeng.xiuxian.sect.Sect sect() {
        return com.hongmeng.xiuxian.sect.Sect.byId(getSect());
    }

    /** 当前身份名。 */
    public String rankName() {
        return sect().rankName(contribution);
    }

    public String getQuestStage() {
        return questStage == null ? "none" : questStage;
    }

    public void setQuestStage(String stage) {
        this.questStage = stage;
    }

    public int getEpochSeason() {
        return epochSeason;
    }

    public void setEpochSeason(int season) {
        this.epochSeason = Math.max(1, season);
    }

    public int getDungeonClears() {
        return dungeonClears;
    }

    public void addDungeonClear() {
        this.dungeonClears++;
    }

    public int getPainLevel() {
        return painLevel;
    }

    public void setPainLevel(int value) {
        this.painLevel = Math.max(0, Math.min(10, value));
    }

    // ------------------------------------------------------------------
    // 便捷方法
    // ------------------------------------------------------------------

    /**
     * 当前境界 = **已解锁层号** 对应的那一层。
     *
     * <p>刻意不再用 {@code Realm.of(cultivation)}——那样修为一到就自动跨层，
     * 玩家一路滑上去，突破就成了摆设。</p>
     */
    public Realm realm() {
        return Realm.ofStep(unlockedStep());
    }

    /** 相对本层的修为进度（0 ~ need-1）。 */
    public long progress() {
        return Math.max(0L, cultivation - realm().minTotal());
    }

    public long needed() {
        return realm().need();
    }

    public boolean isMaxRealm() {
        return isAtLastStep();
    }

    public double successRate() {
        return Karma.successRate(this);
    }

    // ------------------------------------------------------------------
    // 存档
    // ------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Root", root == null ? "" : root.key());
        tag.putBoolean("Awakened", awakened);
        tag.putInt("RerollCount", rerollCount);
        tag.putLong("Cultivation", cultivation);
        tag.putLong("TotalCultivation", totalCultivation);
        tag.putInt("Karma", karma);
        tag.putString("Sect", getSect());
        tag.putInt("Reputation", reputation);
        tag.putInt("Contribution", contribution);
        tag.putBoolean("Betrayed", betrayed);
        tag.putLong("LastSectHunt", lastSectHunt);
        tag.putString("QuestStage", getQuestStage());
        tag.putInt("EpochSeason", epochSeason);
        tag.putInt("DungeonClears", dungeonClears);
        tag.putInt("PainLevel", painLevel);
        tag.putBoolean("SpeedBoost", speedBoostEnabled);
        tag.putBoolean("SwordArt", swordArtUnlocked);
        // 坊市余额：老存档没这个键，读的时候按 0 处理
        tag.putInt("Leaves", leaves);
        questProgress.save(tag);
        domainData.save(tag);
        // 天赋 / 副业 / 道途机制资源（原来是内存 Map，重启就丢）
        // 这里拿不到 ServerPlayer，所以由调用方把 UUID 塞进 tag
        // —— 见 CultivationCapability 的 serializeNBT/deserializeNBT。
        com.hongmeng.xiuxian.talent.TalentSave.saveByTag(tag);
        // 友伤开关（老存档没这个键 → 读出 false，正好是"默认无友伤"）
        tag.putBoolean("DomainFriendlyFire", domainFriendlyFire);
        tag.putString("Difficulty", getDifficultyId());
        tag.putInt("RootRollCount", rootRollCount);
        tag.putInt("UnlockedStep", unlockedStep());
        // 灵力：老存档没这个键，读的时候按"满"处理（见 load）
        tag.putInt("Mana", getMana());
        if (equipment != null) { equipment.save(tag); }
        if (techniques != null) { techniques.save(tag); }
        return tag;
    }

    public void load(CompoundTag tag) {
        this.root = SpiritRoot.byKey(tag.getString("Root"));
        this.awakened = tag.getBoolean("Awakened");
        this.rerollCount = tag.getInt("RerollCount");
        this.cultivation = tag.getLong("Cultivation");
        this.totalCultivation = tag.getLong("TotalCultivation");
        this.karma = tag.getInt("Karma");
        this.sect = tag.getString("Sect");
        this.reputation = tag.getInt("Reputation");
        this.contribution = tag.getInt("Contribution");
        this.betrayed = tag.getBoolean("Betrayed");
        this.lastSectHunt = tag.getLong("LastSectHunt");
        this.questStage = tag.getString("QuestStage");
        if (this.questStage.isEmpty()) {
            this.questStage = "none";
        }
        this.epochSeason = Math.max(1, tag.getInt("EpochSeason"));
        this.dungeonClears = tag.getInt("DungeonClears");
        this.painLevel = tag.getInt("PainLevel");
        this.speedBoostEnabled = !tag.contains("SpeedBoost") || tag.getBoolean("SpeedBoost");
        // 旧存档没有这个键 -> 按"未解锁"读（要重新获取，符合"这是件东西"的定位）
        this.swordArtUnlocked = tag.contains("SwordArt") && tag.getBoolean("SwordArt");
        // 老存档（没有 Leaves 键）读出来是 0：不报错、不凭空发钱
        this.leaves = Math.max(0, tag.getInt("Leaves"));
        questProgress.load(tag);
        domainData.load(tag);
        com.hongmeng.xiuxian.talent.TalentSave.loadByTag(tag);
        // 默认关：老存档读出来是 false，符合"领域默认不伤友善生物"
        this.domainFriendlyFire = tag.getBoolean("DomainFriendlyFire");
        this.difficulty = tag.getString("Difficulty");
        this.rootRollCount = tag.getInt("RootRollCount");
        // 老存档没有这个字段时置 -1，交给 unlockedStep() 按修为迁移
        this.unlockedStep = tag.contains("UnlockedStep") ? tag.getInt("UnlockedStep") : -1;
        // 灵力：老存档没有 Mana 键 → 保持 MANA_UNSET，getMana() 会按"满灵力"返回。
        // 这一点是刻意的：不能让玩家更新模组后一登录就空蓝。
        this.mana = tag.contains("Mana") ? tag.getInt("Mana") : MANA_UNSET;
        clampCultivationToStep();
        if (equipment != null) { equipment.load(tag); }
        if (techniques != null) { techniques.load(tag); }
    }

    // ------------------------------------------------------------------
    // 网络同步
    // ------------------------------------------------------------------

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUtf(root == null ? "" : root.key());
        buf.writeBoolean(awakened);
        buf.writeVarInt(rerollCount);
        buf.writeVarLong(cultivation);
        buf.writeVarLong(totalCultivation);
        buf.writeVarInt(karma);
        buf.writeUtf(getSect());
        buf.writeVarInt(reputation);
        buf.writeVarInt(contribution);
        buf.writeBoolean(betrayed);
        buf.writeUtf(getQuestStage());
        buf.writeVarInt(epochSeason);
        buf.writeVarInt(dungeonClears);
        buf.writeVarInt(painLevel);
        buf.writeBoolean(speedBoostEnabled);
        buf.writeBoolean(swordArtUnlocked);
        questProgress.toNetwork(buf);
        domainData.toNetwork(buf);
        buf.writeUtf(getDifficultyId());
        buf.writeVarInt(rootRollCount);
        buf.writeVarInt(unlockedStep());
        getEquipment().toNetwork(buf);
        getTechniques().toNetwork(buf);
        // 灵叶余额放最后：新增字段只追加在尾部，客户端的坊市界面直接读它显示"余额"
        buf.writeVarInt(leaves);
        // 灵力也追加在最后（必须排在 leaves 之后：插队会让老客户端读错位）
        buf.writeVarInt(getMana());
        // 领域的友伤开关同样追加在最后（R 面板与客户端领域特效都要读它）
        buf.writeBoolean(domainFriendlyFire);
    }

    public static CultivationData fromNetwork(FriendlyByteBuf buf) {
        CultivationData d = new CultivationData();
        d.root = SpiritRoot.byKey(buf.readUtf());
        d.awakened = buf.readBoolean();
        d.rerollCount = buf.readVarInt();
        d.cultivation = buf.readVarLong();
        d.totalCultivation = buf.readVarLong();
        d.karma = buf.readVarInt();
        d.sect = buf.readUtf();
        d.reputation = buf.readVarInt();
        d.contribution = buf.readVarInt();
        d.betrayed = buf.readBoolean();
        d.questStage = buf.readUtf();
        d.epochSeason = buf.readVarInt();
        d.dungeonClears = buf.readVarInt();
        d.painLevel = buf.readVarInt();
        d.speedBoostEnabled = buf.readBoolean();
        d.swordArtUnlocked = buf.readBoolean();
        d.questProgress.copyFrom(com.hongmeng.xiuxian.quest.QuestProgress.fromNetwork(buf));
        d.domainData.fromNetwork(buf);
        d.difficulty = buf.readUtf();
        d.rootRollCount = buf.readVarInt();
        d.unlockedStep = buf.readVarInt();
        d.getEquipment().fromNetwork(buf);
        d.getTechniques().fromNetwork(buf);
        d.leaves = Math.max(0, buf.readVarInt());
        // 灵力在最后：读到之后再夹一次上限（大境刚变时同步包里的值可能略微超上限）
        d.mana = Math.max(0, buf.readVarInt());
        d.domainFriendlyFire = buf.readBoolean();
        return d;
    }

    /** 死亡重生时保留修炼成果（只是业力+1）。 */
    public void copyFrom(CultivationData other) {
        this.root = other.root;
        this.awakened = other.awakened;
        this.rerollCount = other.rerollCount;
        this.cultivation = other.cultivation;
        this.totalCultivation = other.totalCultivation;
        this.karma = other.karma;
        this.sect = other.sect;
        this.reputation = other.reputation;
        this.contribution = other.contribution;
        this.betrayed = other.betrayed;
        this.lastSectHunt = other.lastSectHunt;
        this.questStage = other.questStage;
        this.epochSeason = other.epochSeason;
        this.dungeonClears = other.dungeonClears;
        this.painLevel = 0;
        this.speedBoostEnabled = other.speedBoostEnabled;
        this.swordArtUnlocked = other.swordArtUnlocked;
        // 灵叶余额跟着人走：死了钱还在坊市里，不会掉在尸体上
        this.leaves = other.leaves;
        this.questProgress.copyFrom(other.questProgress);
        this.domainData.copyFrom(other.domainData);
        // 友伤开关跟着人走：死一次不该把设置改回默认
        this.domainFriendlyFire = other.domainFriendlyFire;
        this.difficulty = other.difficulty;
        this.rootRollCount = other.rootRollCount;
        this.unlockedStep = other.unlockedStep;
        // 灵力跟着人走：死一次不该把灵力清空（否则重生后连御剑都起不来）
        this.mana = other.mana;
        this.manaFraction = other.manaFraction;
        if (other.equipment != null) { this.getEquipment().copyFrom(other.equipment); }
        if (other.techniques != null) { this.getTechniques().copyFrom(other.techniques); }
    }
}
