package com.hongmeng.xiuxian.domain;

import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 玩家的道宫数据。
 *
 * <p>道印境起可以把灵海里的道宫"凝出来"，道宫有品质与元素属性。
 * 凝出来不满意就破碎掉，重新凝——每次凝聚消耗能量，能量靠杀怪攒。</p>
 */
public class DomainData {

    /** 凝一座道宫需要的能量。 */
    public static final int CONDENSE_COST = 10;

    /**
     * 累积凝聚次数到 60 时保底仙品。
     *
     * <p>原来是 100——玩家反馈"好的道宫太难刷"。降下来之后，
     * 加上基础权重也整体上移，仙品以上的体验顺畅很多。</p>
     */
    public static final int PITY_IMMORTAL = 60;

    /** 累积凝聚次数到 200 时保底鸿蒙（原来 500，同样下调）。 */
    public static final int PITY_HONGMENG = 200;

    /** 一座已凝聚的道宫。 */
    public record Domain(DomainQuality quality, Element element) {
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeVarInt(quality.ordinal());
            buf.writeVarInt(element.ordinal());
        }

        public static Domain fromNetwork(FriendlyByteBuf buf) {
            // ⚠ 必须做边界检查：读的是**枚举序号**，服务端/客户端枚举长度不一致时
            // （例如一边加了新元素、或玩家回退了版本）直接 values()[越界] → 客户端崩。
            // 存档路径的 load() 本来就有 clamp，这条网络路径原来漏了。
            DomainQuality[] qs = DomainQuality.values();
            Element[] es = Element.values();
            int q = Math.max(0, Math.min(qs.length - 1, buf.readVarInt()));
            int e = Math.max(0, Math.min(es.length - 1, buf.readVarInt()));
            return new Domain(qs[q], es[e]);
        }

        public void save(CompoundTag tag) {
            tag.putInt("Quality", quality.ordinal());
            tag.putInt("Element", element.ordinal());
        }

        public static Domain load(CompoundTag tag) {
            int q = Math.max(0, Math.min(DomainQuality.values().length - 1, tag.getInt("Quality")));
            int e = Math.max(0, Math.min(Element.values().length - 1, tag.getInt("Element")));
            return new Domain(DomainQuality.values()[q], Element.values()[e]);
        }
    }

    private final List<Domain> domains = new ArrayList<>();
    /** 当前攒下的凝聚能量。 */
    private int energy = 0;
    /** 累计凝聚次数（用于保底）。 */
    private int totalCondensed = 0;
    /** 是否解锁了第二座道宫（需要双道宫丹）。 */
    private boolean dualUnlocked = false;
    /** 领域是否开启（道宫技）。 */
    private boolean domainActive = false;

    public List<Domain> domains() {
        return domains;
    }

    public int domainCount() {
        return domains.size();
    }

    public int maxDomains() {
        return dualUnlocked ? 2 : 1;
    }

    public int energy() {
        return energy;
    }

    public void addEnergy(int amount) {
        if (amount > 0) {
            this.energy = Math.min(9999, this.energy + amount);
        }
    }

    /**
     * <b>消耗道宫能量</b>（领域运行时按秒扣）。
     *
     * <p>加上这个方法之后，「道宫能量」就不再只是"凝聚道宫的一次性货币"，
     * 而成了<b>需要经营的资源</b>：杀怪攒能量 ↔ 开领域烧能量，
     * 玩家要决定攒来的能量是拿去<b>凝新道宫</b>还是<b>开领域</b>。</p>
     *
     * @param amount 想花多少（正数）
     * @return 实际花掉多少（不足时按剩余量给，不会变负）
     */
    public int spendEnergy(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int spent = Math.min(this.energy, amount);
        this.energy -= spent;
        return spent;
    }

    public int totalCondensed() {
        return totalCondensed;
    }

    public boolean isDualUnlocked() {
        return dualUnlocked;
    }

    public void setDualUnlocked(boolean value) {
        this.dualUnlocked = value;
    }

    public boolean isDomainActive() {
        return domainActive;
    }

    public void setDomainActive(boolean value) {
        this.domainActive = value;
    }

    public int energyIntoPityImmortal() {
        return Math.max(0, PITY_IMMORTAL - totalCondensed);
    }

    public int energyIntoPityHongmeng() {
        return Math.max(0, PITY_HONGMENG - totalCondensed);
    }

    public boolean canCondense() {
        return domainCount() < maxDomains() && energy >= CONDENSE_COST;
    }

    /**
     * 凝聚一座道宫。
     *
     * <p>品质按权重随机，但累计次数到 100 保底仙品、到 500 保底鸿蒙。</p>
     *
     * @return 新凝聚的道宫；条件不满足返回 null
     */
    public Domain condense(Random random, Element preferredElement) {
        return condense(random, preferredElement, null);
    }

    /**
     * 凝聚一座道宫（带难度）。
     *
     * <p>品质先按难度权重抽，再套保底下限；累计次数到 100 保底仙品、到 500 保底鸿蒙。</p>
     */
    public Domain condense(Random random, Element preferredElement,
                           com.hongmeng.xiuxian.difficulty.CultivationDifficulty difficulty) {
        if (!canCondense()) {
            return null;
        }
        energy -= CONDENSE_COST;
        totalCondensed++;

        DomainQuality quality = rollQuality(random, difficulty);
        // 元素：<b>领域自己决定</b>。
        //
        // ⚠ 原来是「优先玩家灵根的元素，小概率变异成别的」——
        //   但灵根的属性维度已被移除（玩家要求），所以现在调用方一律传 null，
        //   走纯随机分支。保留 preferredElement 形参只是为了让签名不变、
        //   也方便将来加"定向凝聚"的道具。
        Element element = preferredElement == null ? randomElement(random) : preferredElement;
        if (random.nextInt(100) < 20) {
            element = randomElement(random);
        }
        Domain domain = new Domain(quality, element);
        domains.add(domain);
        return domain;
    }

    private DomainQuality rollQuality(Random random,
                                     com.hongmeng.xiuxian.difficulty.CultivationDifficulty difficulty) {
        // 保底优先（与难度无关，纯看累计次数）
        if (totalCondensed >= PITY_HONGMENG) {
            return DomainQuality.HONGMENG;
        }
        if (totalCondensed >= PITY_IMMORTAL) {
            // 仙品起步，小概率直接鸿蒙
            return random.nextInt(100) < 8 ? DomainQuality.HONGMENG : DomainQuality.IMMORTAL;
        }
        if (difficulty != null) {
            return DomainQuality.values()[difficulty.rollDomainQuality(random)];
        }
        double total = 0.0D;
        for (DomainQuality q : DomainQuality.values()) {
            total += q.weight();
        }
        double pick = random.nextDouble() * total;
        double cursor = 0.0D;
        for (DomainQuality q : DomainQuality.values()) {
            cursor += q.weight();
            if (pick <= cursor) {
                return q;
            }
        }
        return DomainQuality.MORTAL;
    }

    private static Element randomElement(Random random) {
        Element[] all = Element.values();
        return all[random.nextInt(all.length)];
    }

    /** 破碎第 index 座道宫。 */
    public Domain shatter(int index) {
        if (index < 0 || index >= domains.size()) {
            return null;
        }
        return domains.remove(index);
    }

    /** 领域内、某元素的功法伤害加成（取所有道宫里最高的那一座）。 */
    public double techniqueBonus(Element element) {
        double best = 0.0D;
        for (Domain d : domains) {
            if (d.element() == element) {
                best = Math.max(best, d.quality().techniqueDamage());
            }
        }
        return best;
    }

    /** 释放功法速度加成。 */
    public double castSpeedBonus() {
        double best = 0.0D;
        for (Domain d : domains) {
            best = Math.max(best, d.quality().castSpeed());
        }
        return best;
    }

    // ------------------------------------------------------------------

    public void save(CompoundTag tag) {
        CompoundTag root = new CompoundTag();
        root.putInt("Energy", energy);
        root.putInt("TotalCondensed", totalCondensed);
        root.putBoolean("DualUnlocked", dualUnlocked);
        root.putBoolean("Active", domainActive);
        CompoundTag list = new CompoundTag();
        for (int i = 0; i < domains.size(); i++) {
            CompoundTag d = new CompoundTag();
            domains.get(i).save(d);
            list.put("D" + i, d);
        }
        root.put("Domains", list);
        tag.put("HongmengDomains", root);
    }

    public void load(CompoundTag tag) {
        domains.clear();
        if (!tag.contains("HongmengDomains")) {
            return;
        }
        CompoundTag root = tag.getCompound("HongmengDomains");
        energy = root.getInt("Energy");
        totalCondensed = root.getInt("TotalCondensed");
        dualUnlocked = root.getBoolean("DualUnlocked");
        domainActive = root.getBoolean("Active");
        CompoundTag list = root.getCompound("Domains");
        for (int i = 0; i < 8; i++) {
            if (list.contains("D" + i)) {
                domains.add(Domain.load(list.getCompound("D" + i)));
            }
        }
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(energy);
        buf.writeVarInt(totalCondensed);
        buf.writeBoolean(dualUnlocked);
        buf.writeBoolean(domainActive);
        buf.writeVarInt(domains.size());
        for (Domain d : domains) {
            d.toNetwork(buf);
        }
    }

    public void fromNetwork(FriendlyByteBuf buf) {
        domains.clear();
        energy = buf.readVarInt();
        totalCondensed = buf.readVarInt();
        dualUnlocked = buf.readBoolean();
        domainActive = buf.readBoolean();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            domains.add(Domain.fromNetwork(buf));
        }
    }

    public void copyFrom(DomainData other) {
        domains.clear();
        domains.addAll(other.domains);
        energy = other.energy;
        totalCondensed = other.totalCondensed;
        dualUnlocked = other.dualUnlocked;
        domainActive = other.domainActive;
    }
}
