package com.hongmeng.xiuxian.talent;

import com.hongmeng.xiuxian.core.CultivationData;

/**
 * <b>御剑术的解锁状态</b>（剑阵的获取门槛）。
 *
 * <h2>为什么要单独一个类</h2>
 * <p>玩家问：「剑阵为什么开局就自带呢，是否设置一个获取方式？」。
 * 原来唯一的门槛是"境界 ≥ 第 2 境"，等于第二次突破就白送。</p>
 *
 * <p>现在改成<b>永久解锁式</b>：读过《御剑术秘籍》才解锁。
 * 状态存在角色数据里（跟随存档走），所以：
 * <ul>
 *   <li>换世界 / 重登都还在（不是每次都要吃）；</li>
 *   <li>死了也不丢（不是消耗品）；</li>
 *   <li>但<b>新角色</b>要重新获取 —— 这才是"获取方式"的意义。</li>
 * </ul>
 *
 * <p>为什么不用 {@code TalentSave} 的通用 map：那套按"天赋/副业"分组，
 * 塞一个布尔进去会让读取方困惑。单独一个键最清楚。</p>
 */
public final class SwordArt {

    /** 存档键（放在角色的持久化数据里）。 */
    public static final String KEY = "SwordArtUnlocked";

    private SwordArt() {
    }

    /** 是否已解锁御剑术（也就是能不能开剑阵）。 */
    public static boolean unlocked(CultivationData data) {
        if (data == null) {
            return false;
        }
        try {
            return data.getSwordArtUnlocked();
        } catch (Throwable t) {
            return false;
        }
    }

    /** 永久解锁（幂等）。 */
    public static void unlock(CultivationData data) {
        if (data == null) {
            return;
        }
        try {
            data.setSwordArtUnlocked(true);
        } catch (Throwable ignored) {
            // 数据层不可用时静默失败：宁可不解锁，也不要 NPE 崩掉右键
        }
    }

    /** 给命令/调试用：重置（例如重新体验获取流程）。 */
    public static void lock(CultivationData data) {
        if (data == null) {
            return;
        }
        try {
            data.setSwordArtUnlocked(false);
        } catch (Throwable ignored) {
            // 同上
        }
    }
}
