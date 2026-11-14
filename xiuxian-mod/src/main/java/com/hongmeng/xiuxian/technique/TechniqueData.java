package com.hongmeng.xiuxian.technique;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家已修习的功法、每门功法的冷却，以及**法术栏**（5 被动 + 4 主动）。
 *
 * <p>数据量很小（最多 40 门），所以直接用 {@link LinkedHashMap} 存"功法 → 冷却结束的 tick"。
 * 没学过的功法不在表里，学过的即使已冷却完也保留（值为 0）。</p>
 *
 * <h3>法术栏</h3>
 * <pre>
 *   passiveSlots[5]   —— 装备即生效，不消耗灵力、不主动释放
 *   activeSlots[4]    —— 对应 4 个按键（默认 Z X C V）
 * </pre>
 *
 * <p><b>规则</b>：只有已修习的功法能装备；被动只能进被动槽，主动只能进主动槽；
 * 同一门功法不会同时占两个槽（装备到新槽会自动从旧槽摘掉）。</p>
 *
 * <h3>蓄力状态</h3>
 *
 * <p>施法前摇的状态也放在这里：{@code castingTechnique / castingStartTick / castingUntilTick}。
 * 它跟着 {@link com.hongmeng.xiuxian.core.CultivationData} 一起存档，
 * 所以死亡重生、掉线重连都不会留下"卡在蓄力中"的幽灵状态——
 * {@code SpellCastHandler} 在登录/重生时会检查这几个字段是否还有效。</p>
 *
 * <p>纯逻辑，不碰 Minecraft 世界与注册表——离线验证器可以断言学习/冷却/装备的全部行为。</p>
 */
public class TechniqueData {

    /** 被动槽数量。 */
    public static final int PASSIVE_SLOTS = 5;
    /** 主动槽数量。 */
    public static final int ACTIVE_SLOTS = 4;

    /** 功法 → 冷却结束的游戏刻。0 表示已就绪。 */
    private final Map<Technique, Long> known = new LinkedHashMap<>();

    /** 当前选中的功法序号（在已学列表中循环）。 */
    private int selected = 0;

    /** 被动槽（5 个）：装备即生效。 */
    private final Technique[] passiveSlots = new Technique[PASSIVE_SLOTS];

    /** 主动槽（4 个）：对应 4 个按键。 */
    private final Technique[] activeSlots = new Technique[ACTIVE_SLOTS];

    // ---- 蓄力状态（服务端权威） ----

    /** 正在蓄力的功法；null 表示没在蓄力。 */
    private Technique castingTechnique;
    /** 蓄力开始刻。 */
    private long castingStartTick;
    /** 蓄力完成刻（到这一刻结算效果）。 */
    private long castingUntilTick;
    /**
     * 本次蓄力的**实际总时长**（tick）。
     *
     * <p>道宫的「释放速度」会缩短它；进度条与完成判定都必须读这个值，
     * 而不是 {@code technique.castTicks()}（那是未加成的基础时长）。</p>
     */
    private int castingTotalTicks;
    /** 这一发是否已经扣过灵力（防止重复扣费/漏扣）。 */
    private boolean castingCharged;

    public TechniqueData() {
    }

    // ------------------------------------------------------------------
    // 学习 / 遗忘
    // ------------------------------------------------------------------

    /** 是否已修习。 */
    public boolean has(Technique technique) {
        return technique != null && known.containsKey(technique);
    }

    /**
     * 修习一门功法。
     *
     * @return true 表示这次真的学会了（之前没有）
     */
    public boolean learn(Technique technique) {
        if (technique == null || known.containsKey(technique)) {
            return false;
        }
        known.put(technique, 0L);
        return true;
    }

    /** 遗忘一门功法（洗点用）。已经从法术栏上摘下来。 */
    public boolean forget(Technique technique) {
        if (technique == null) {
            return false;
        }
        unequip(technique);
        return known.remove(technique) != null;
    }

    /** 已修习的功法，按枚举声明顺序排列——面板显示顺序稳定。 */
    public List<Technique> knownList() {
        List<Technique> out = new ArrayList<>();
        for (Technique t : Technique.values()) {
            if (known.containsKey(t)) {
                out.add(t);
            }
        }
        return out;
    }

    public int knownCount() {
        return known.size();
    }

    public boolean isEmpty() {
        return known.isEmpty();
    }

    // ------------------------------------------------------------------
    // 选中
    // ------------------------------------------------------------------

    /** 当前选中的功法；一门都没学返回 null。 */
    public Technique selectedTechnique() {
        List<Technique> list = knownList();
        if (list.isEmpty()) {
            return null;
        }
        int idx = Math.floorMod(selected, list.size());
        return list.get(idx);
    }

    public int selectedIndex() {
        List<Technique> list = knownList();
        if (list.isEmpty()) {
            return 0;
        }
        return Math.floorMod(selected, list.size());
    }

    /** 切换到下一门，返回新的选中项。 */
    public Technique next() {
        List<Technique> list = knownList();
        if (list.isEmpty()) {
            return null;
        }
        selected = (Math.floorMod(selected, list.size()) + 1) % list.size();
        return list.get(selected);
    }

    /** 切换到上一门。 */
    public Technique previous() {
        List<Technique> list = knownList();
        if (list.isEmpty()) {
            return null;
        }
        selected = Math.floorMod(Math.floorMod(selected, list.size()) - 1, list.size());
        return list.get(selected);
    }

    /** 直接选中某一门；没学过则忽略。 */
    public boolean select(Technique technique) {
        List<Technique> list = knownList();
        int idx = list.indexOf(technique);
        if (idx < 0) {
            return false;
        }
        selected = idx;
        return true;
    }

    // ------------------------------------------------------------------
    // 法术栏：5 被动 + 4 主动
    // ------------------------------------------------------------------

    /** 装备一门被动功法到第 slot 号被动槽。 */
    public boolean equipPassive(Technique technique, int slot) {
        if (!canEquip(technique, slot, PASSIVE_SLOTS, true)) {
            return false;
        }
        detach(technique, passiveSlots, activeSlots);
        passiveSlots[slot] = technique;
        return true;
    }

    /** 装备一门主动功法到第 slot 号主动槽。 */
    public boolean equipActive(Technique technique, int slot) {
        if (!canEquip(technique, slot, ACTIVE_SLOTS, false)) {
            return false;
        }
        detach(technique, activeSlots, passiveSlots);
        activeSlots[slot] = technique;
        return true;
    }

    /** 卸下第 slot 号被动槽。 */
    public Technique unequipPassive(int slot) {
        if (slot < 0 || slot >= PASSIVE_SLOTS) {
            return null;
        }
        Technique old = passiveSlots[slot];
        passiveSlots[slot] = null;
        return old;
    }

    /** 卸下第 slot 号主动槽。 */
    public Technique unequipActive(int slot) {
        if (slot < 0 || slot >= ACTIVE_SLOTS) {
            return null;
        }
        Technique old = activeSlots[slot];
        activeSlots[slot] = null;
        return old;
    }

    /** 这门功法当前装在哪个被动槽；没装返回 -1。 */
    public int passiveSlotOf(Technique technique) {
        return indexOf(passiveSlots, technique);
    }

    /** 这门功法当前装在哪个主动槽；没装返回 -1。 */
    public int activeSlotOf(Technique technique) {
        return indexOf(activeSlots, technique);
    }

    /** 这门功法装在任意一个槽上（返回槽位编码：被动 0..4，主动 10..13），没装返回 -1。 */
    public int equippedSlotCode(Technique technique) {
        int p = passiveSlotOf(technique);
        if (p >= 0) {
            return p;
        }
        int a = activeSlotOf(technique);
        return a >= 0 ? 10 + a : -1;
    }

    /** 这门功法是否已装备（任意槽）。 */
    public boolean isEquipped(Technique technique) {
        return passiveSlotOf(technique) >= 0 || activeSlotOf(technique) >= 0;
    }

    /** 把一门功法从所有槽上摘掉（遗忘 / 换槽时用）。 */
    public boolean unequip(Technique technique) {
        boolean removed = false;
        for (int i = 0; i < PASSIVE_SLOTS; i++) {
            if (passiveSlots[i] == technique) {
                passiveSlots[i] = null;
                removed = true;
            }
        }
        for (int i = 0; i < ACTIVE_SLOTS; i++) {
            if (activeSlots[i] == technique) {
                activeSlots[i] = null;
                removed = true;
            }
        }
        return removed;
    }

    /** 第 slot 号被动槽上的功法；空槽或越界返回 null。 */
    public Technique passiveAt(int slot) {
        return slot >= 0 && slot < PASSIVE_SLOTS ? passiveSlots[slot] : null;
    }

    /** 第 slot 号主动槽上的功法；空槽或越界返回 null。 */
    public Technique activeAt(int slot) {
        return slot >= 0 && slot < ACTIVE_SLOTS ? activeSlots[slot] : null;
    }

    /** 已装备的被动，按槽位顺序（跳过空槽）。 */
    public List<Technique> equippedPassives() {
        List<Technique> out = new ArrayList<>();
        for (Technique t : passiveSlots) {
            if (t != null) {
                out.add(t);
            }
        }
        return out;
    }

    /** 已装备的主动，按槽位顺序（跳过空槽）。 */
    public List<Technique> equippedActives() {
        List<Technique> out = new ArrayList<>();
        for (Technique t : activeSlots) {
            if (t != null) {
                out.add(t);
            }
        }
        return out;
    }

    /** 装备数量（被动 + 主动）。 */
    public int equippedCount() {
        return equippedPassives().size() + equippedActives().size();
    }

    /** 清空全部槽位（洗点用）。 */
    public void clearSlots() {
        java.util.Arrays.fill(passiveSlots, null);
        java.util.Arrays.fill(activeSlots, null);
    }

    /**
     * 装备校验。
     *
     * @param passive true 表示目标是被动槽
     */
    private boolean canEquip(Technique technique, int slot, int slotCount, boolean passive) {
        if (technique == null || slot < 0 || slot >= slotCount) {
            return false;
        }
        // 只有已修习的功法才能装备
        if (!known.containsKey(technique)) {
            return false;
        }
        // 被动只能进被动槽，主动只能进主动槽
        return technique.isPassive() == passive;
    }

    /** 把功法从另一组槽里摘掉（同一门功法不能同时占两个槽）。 */
    private static void detach(Technique technique, Technique[] primary, Technique[] other) {
        for (int i = 0; i < primary.length; i++) {
            if (primary[i] == technique) {
                primary[i] = null;
            }
        }
        for (int i = 0; i < other.length; i++) {
            if (other[i] == technique) {
                other[i] = null;
            }
        }
    }

    private static int indexOf(Technique[] slots, Technique technique) {
        if (technique == null) {
            return -1;
        }
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == technique) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // 蓄力（施法前摇）
    // ------------------------------------------------------------------

    /** 是否正在蓄力。 */
    public boolean isCasting() {
        return castingTechnique != null;
    }

    /** 正在蓄力的功法；没在蓄力返回 null。 */
    public Technique castingTechnique() {
        return castingTechnique;
    }

    public long castingStartTick() {
        return castingStartTick;
    }

    public long castingUntilTick() {
        return castingUntilTick;
    }

    /** 这一发是否已经扣过灵力。 */
    public boolean castingCharged() {
        return castingCharged;
    }

    /**
     * 开始蓄力。
     *
     * @param now 当前游戏刻
     */
    public void beginCast(Technique technique, long now) {
        beginCast(technique, now, technique == null ? 0 : technique.castTicks());
    }

    /**
     * 开始蓄力（可指定<b>实际总时长</b>）。
     *
     * <p>为什么要参数：道宫的「释放速度」会缩短蓄力。若这里仍直接写
     * {@code technique.castTicks()}，就会出现"进度条按原速走、实际提前打完"
     * 或者"提前 completion 判定"的错位。</p>
     *
     * @param totalTicks 本次实际总时长（已含释放速度加成）
     */
    public void beginCast(Technique technique, long now, int totalTicks) {
        this.castingTechnique = technique;
        this.castingStartTick = now;
        this.castingTotalTicks = Math.max(0, totalTicks);
        this.castingUntilTick = now + this.castingTotalTicks;
        this.castingCharged = true;
    }

    /** 记下"这一发还没扣费"（预留给"完成时才扣费"的实现）。 */
    public void markUncharged() {
        this.castingCharged = false;
    }

    public void markCharged() {
        this.castingCharged = true;
    }

    /** 结束蓄力（完成或中断都走这里）。 */
    public void endCast() {
        this.castingTechnique = null;
        this.castingStartTick = 0L;
        this.castingUntilTick = 0L;
        this.castingCharged = false;
    }

    /** 蓄力是否已经完成（该结算了）。 */
    public boolean castFinished(long now) {
        return castingTechnique != null && now >= castingUntilTick;
    }

    /** 蓄力进度 0~1（客户端画蓄力条用）。 */
    public float castProgress(long now) {
        if (castingTechnique == null) {
            return 0.0F;
        }
        // 用**本次实际总时长**（已含道宫释放速度），否则进度条与真实完成刻不一致
        int total = Math.max(0, castingTotalTicks);
        if (total <= 0) {
            return 1.0F;
        }
        long done = now - castingStartTick;
        return Math.max(0.0F, Math.min(1.0F, (float) done / (float) total));
    }

    /** 蓄力是否还有效（供同步用：超过完成刻太多就是脏状态，直接清掉）。 */
    public boolean castStillRunning(long now) {
        return castingTechnique != null && now <= castingUntilTick + 40L;
    }

    // ------------------------------------------------------------------
    // 冷却
    // ------------------------------------------------------------------

    /** 这门功法是否已就绪。 */
    public boolean isReady(Technique technique, long now) {
        Long until = known.get(technique);
        return until != null && now >= until;
    }

    /** 剩余冷却刻数；就绪返回 0。 */
    public long remaining(Technique technique, long now) {
        Long until = known.get(technique);
        if (until == null) {
            return 0L;
        }
        return Math.max(0L, until - now);
    }

    /** 剩余冷却秒数（向上取整，显示用）。 */
    public long remainingSeconds(Technique technique, long now) {
        long ticks = remaining(technique, now);
        return ticks <= 0L ? 0L : (ticks + 19L) / 20L;
    }

    /**
     * 进入冷却。
     *
     * @return true 表示成功进入冷却（之前是就绪状态）
     */
    public boolean startCooldown(Technique technique, long now) {
        return startCooldown(technique, now, 0.0D);
    }

    /**
     * 带<b>冷却缩减</b>的起冷却。
     *
     * @param cut 缩减比例（0.15 = 少 15%）。由 {@code TalentBonus.cooldownCut} 给出，
     *            已含道途专属（法修 −8%）+ 天赋 + 上限 60%。
     */
    public boolean startCooldown(Technique technique, long now, double cut) {
        if (!known.containsKey(technique) || !isReady(technique, now)) {
            return false;
        }
        // cut 可以到 1.0 —— 那是"规则之力：万法无冷却"，设计上就该是 0 冷却。
        // 上一版封顶 0.90 会让它最少还留 10%，那是对终局能力的削弱。
        double c = Math.max(0.0D, Math.min(1.0D, cut));
        if (c >= 1.0D) {
            known.put(technique, now + 1L);   // 只隔 1 tick，避免同 tick 内连发
            return true;
        }
        long ticks = Math.max(1L, Math.round(technique.cooldown() * (1.0D - c)));
        known.put(technique, now + ticks);
        return true;
    }

    /** 把全部冷却清空（调试用）。 */
    public void clearCooldowns() {
        known.replaceAll((t, v) -> 0L);
    }

    // ------------------------------------------------------------------
    // 存档 / 网络
    // ------------------------------------------------------------------

    public void save(CompoundTag tag) {
        CompoundTag root = new CompoundTag();
        for (Map.Entry<Technique, Long> e : known.entrySet()) {
            root.putLong(e.getKey().name(), e.getValue());
        }
        root.putInt("Selected", selected);

        // 法术栏：存枚举名，读的时候忽略已经不存在的名字（改枚举也不会炸存档）
        ListTag passives = new ListTag();
        for (Technique t : passiveSlots) {
            passives.add(StringTag.valueOf(t == null ? "" : t.name()));
        }
        root.put("PassiveSlots", passives);
        ListTag actives = new ListTag();
        for (Technique t : activeSlots) {
            actives.add(StringTag.valueOf(t == null ? "" : t.name()));
        }
        root.put("ActiveSlots", actives);

        // 蓄力状态：跨存档/重生保留，登录时会校验是否还有效
        if (castingTechnique != null) {
            root.putString("CastingTechnique", castingTechnique.name());
            root.putLong("CastingStart", castingStartTick);
            root.putLong("CastingUntil", castingUntilTick);
            root.putBoolean("CastingCharged", castingCharged);
        }

        tag.put("HongmengTechniques", root);
    }

    public void load(CompoundTag tag) {
        known.clear();
        selected = 0;
        clearSlots();
        endCast();
        if (!tag.contains("HongmengTechniques")) {
            return;
        }
        CompoundTag root = tag.getCompound("HongmengTechniques");
        for (Technique t : Technique.values()) {
            if (root.contains(t.name())) {
                known.put(t, root.getLong(t.name()));
            }
        }
        selected = root.getInt("Selected");

        readSlots(root.getList("PassiveSlots", Tag.TAG_STRING), passiveSlots);
        readSlots(root.getList("ActiveSlots", Tag.TAG_STRING), activeSlots);

        if (root.contains("CastingTechnique")) {
            Technique t = Technique.byName(root.getString("CastingTechnique"));
            // 只恢复"还在蓄力中"的状态：功法得学过、时间戳得有效
            if (t != null && known.containsKey(t)) {
                castingTechnique = t;
                castingStartTick = root.getLong("CastingStart");
                castingUntilTick = root.getLong("CastingUntil");
                castingCharged = root.getBoolean("CastingCharged");
            }
        }
    }

    private static void readSlots(ListTag list, Technique[] target) {
        for (int i = 0; i < target.length && i < list.size(); i++) {
            String name = list.getString(i);
            if (name.isEmpty()) {
                continue;
            }
            Technique t = Technique.byName(name);
            // 被动/主动必须放对槽：改过枚举或手改存档都不会让状态错乱
            boolean wantPassive = target.length == PASSIVE_SLOTS;
            if (t != null && t.isPassive() == wantPassive) {
                target[i] = t;
            }
        }
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(known.size());
        for (Map.Entry<Technique, Long> e : known.entrySet()) {
            buf.writeVarInt(e.getKey().ordinal());
            buf.writeLong(e.getValue());
        }
        buf.writeVarInt(selected);

        // ---- 以下为法术栏扩充：**只追加在尾部**，不重排上面的既有字段 ----
        for (Technique t : passiveSlots) {
            writeSlot(buf, t);
        }
        for (Technique t : activeSlots) {
            writeSlot(buf, t);
        }
        // 蓄力状态：客户端据此画蓄力条
        buf.writeBoolean(castingTechnique != null);
        if (castingTechnique != null) {
            buf.writeVarInt(castingTechnique.ordinal());
            buf.writeVarLong(castingStartTick);
            buf.writeVarLong(castingUntilTick);
        }
    }

    public void fromNetwork(FriendlyByteBuf buf) {
        known.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            // ⚠ 边界检查：越界会让客户端崩（详见 DomainData.fromNetwork 的注释）
        Technique[] all = Technique.values();
        Technique t = all[Math.max(0, Math.min(all.length - 1, buf.readVarInt()))];
            known.put(t, buf.readLong());
        }
        selected = buf.readVarInt();

        clearSlots();
        for (int i = 0; i < PASSIVE_SLOTS; i++) {
            passiveSlots[i] = readSlot(buf);
        }
        for (int i = 0; i < ACTIVE_SLOTS; i++) {
            activeSlots[i] = readSlot(buf);
        }
        endCast();
        if (buf.readBoolean()) {
            Technique[] allC = Technique.values();
                castingTechnique = allC[Math.max(0,
                        Math.min(allC.length - 1, buf.readVarInt()))];
            castingStartTick = buf.readVarLong();
            castingUntilTick = buf.readVarLong();
            castingCharged = true;
        }
    }

    /** 槽位编码：0 = 空；其余为 ordinal + 1。 */
    private static void writeSlot(FriendlyByteBuf buf, Technique t) {
        buf.writeVarInt(t == null ? 0 : t.ordinal() + 1);
    }

    private static Technique readSlot(FriendlyByteBuf buf) {
        int code = buf.readVarInt();
        if (code <= 0 || code > Technique.values().length) {
            return null;
        }
        return Technique.values()[code - 1];
    }

    public void copyFrom(TechniqueData other) {
        known.clear();
        known.putAll(other.known);
        selected = other.selected;
        System.arraycopy(other.passiveSlots, 0, passiveSlots, 0, PASSIVE_SLOTS);
        System.arraycopy(other.activeSlots, 0, activeSlots, 0, ACTIVE_SLOTS);
        castingTechnique = other.castingTechnique;
        castingStartTick = other.castingStartTick;
        castingUntilTick = other.castingUntilTick;
        castingCharged = other.castingCharged;
    }
}
