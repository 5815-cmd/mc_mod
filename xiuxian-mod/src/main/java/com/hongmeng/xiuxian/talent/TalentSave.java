package com.hongmeng.xiuxian.talent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

/**
 * <b>职业 / 天赋 / 副业 / 机制状态的存档</b>。
 *
 * <h2>本轮变更（R-1）</h2>
 * <p>玩家已明确旧存档全部删除，所以<b>不做旧档迁移</b>：直接写新键
 * {@code Career}（职业枚举名）+ 新天赋节点 id。旧键 {@code TalentPath}
 * 不再读取——读到也没有用（旧存档会被删掉）。</p>
 *
 * <h2>容错</h2>
 * <p>读的时候一律用默认值兜底：没有这些键 → 未择业、无天赋、副业 0 级，
 * 不报错、不凭空送。</p>
 */
public final class TalentSave {

    private static final String KEY_CAREER = "Career";
    private static final String KEY_RANKS = "TalentRanks";
    private static final String KEY_WORK = "ProfessionWork";
    /** 机制资源桶（槽0 / 魔气 / 血池 / 鬼域）。 */
    private static final String KEY_MECH = "CareerMechanics";

    /** 登录时的衰减系数（存下来的资源乘这个）。 */
    public static final double LOGIN_DECAY = 0.5D;

    private TalentSave() {
    }

    /** 写进玩家的 CompoundTag（由 {@code CultivationData.save} 调）。 */
    public static void saveByTag(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("TalentOwner")) {
            return;
        }
        UUID id = tag.getUUID("TalentOwner");
        // ---- 职业 ----
        Career career = Talents.careerAt(id);
        tag.putString(KEY_CAREER, career == null ? "" : career.name());
        // ---- 天赋等级 ----
        ListTag ranks = new ListTag();
        for (Map.Entry<String, Integer> e : Talents.ranksAt(id).entrySet()) {
            CompoundTag one = new CompoundTag();
            one.putString("Id", e.getKey());
            one.putInt("Rank", e.getValue());
            ranks.add(one);
        }
        tag.put(KEY_RANKS, ranks);
        // ---- 副业熟练度 ----
        ListTag work = new ListTag();
        for (Map.Entry<String, Integer> e : Talents.workAt(id).entrySet()) {
            CompoundTag one = new CompoundTag();
            one.putString("Id", e.getKey());
            one.putInt("Count", e.getValue());
            work.add(one);
        }
        tag.put(KEY_WORK, work);
        // ---- 机制资源 ----
        double[] res = PathRuntime.snapshotRaw(id);
        CompoundTag mech = new CompoundTag();
        mech.putDouble("Slot0", res[0]);
        mech.putDouble("Demonic", res[1]);
        mech.putDouble("Blood", res[2]);
        mech.putDouble("Ghost", res[3]);
        tag.put(KEY_MECH, mech);
    }

    /** 从玩家的 CompoundTag 读回来（由 {@code CultivationData.load} 调）。 */
    public static void loadByTag(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("TalentOwner")) {
            return;
        }
        UUID id = tag.getUUID("TalentOwner");
        // 先把该 UUID 的状态清掉：单机换存档时 UUID 不变，
        // 不重置会把上一存档的职业/天赋留在内存里（项目踩过）。
        Talents.setCareerAt(id, null);
        // ---- 职业 ----
        if (tag.contains(KEY_CAREER)) {
            Talents.setCareerAt(id, Career.byName(tag.getString(KEY_CAREER)));
        }
        // ---- 天赋等级 ----
        if (tag.contains(KEY_RANKS, Tag.TAG_LIST)) {
            ListTag ranks = tag.getList(KEY_RANKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < ranks.size(); i++) {
                CompoundTag one = ranks.getCompound(i);
                String kid = one.getString("Id");
                int rank = one.getInt("Rank");
                if (!kid.isEmpty() && rank > 0) {
                    Talents.setRankAt(id, kid, rank);
                }
            }
        }
        // ---- 副业熟练度 ----
        if (tag.contains(KEY_WORK, Tag.TAG_LIST)) {
            ListTag work = tag.getList(KEY_WORK, Tag.TAG_COMPOUND);
            for (int i = 0; i < work.size(); i++) {
                CompoundTag one = work.getCompound(i);
                String pid = one.getString("Id");
                int count = one.getInt("Count");
                if (!pid.isEmpty() && count > 0) {
                    Talents.setWorkAt(id, pid, count);
                }
            }
        }
        // ---- 机制资源：按衰减恢复 ----
        if (tag.contains(KEY_MECH, Tag.TAG_COMPOUND)) {
            CompoundTag mech = tag.getCompound(KEY_MECH);
            PathRuntime.restore(id,
                    mech.getDouble("Slot0") * LOGIN_DECAY,
                    mech.getDouble("Demonic") * LOGIN_DECAY,
                    mech.getDouble("Blood") * LOGIN_DECAY,
                    mech.getDouble("Ghost") * LOGIN_DECAY);
        }
    }

    /** 清空某个玩家在内存里的全部记录（掉线时调，避免内存泄漏）。 */
    public static void forget(UUID uuid) {
        if (uuid == null) {
            return;
        }
        Talents.forget(uuid);
        PathRuntime.forget(uuid);
    }
}
