package com.hongmeng.xiuxian.dungeon;

/**
 * <b>副本挑战的阶段判定</b>（纯数据枚举，不引用任何 Minecraft 类）。
 *
 * <p>为什么单独抽出来：副本流程其实是"一个挑战、两个合法维度"——</p>
 * <ol>
 *   <li><b>怪潮阶段</b>：玩家在 {@code *_realm}（副本维度）里打波次；</li>
 *   <li><b>决战阶段</b>：最后一波清完，挑战转成 arenaSent=true，
 *       玩家被传送到 {@code arena_*}（boss 决战维度），等祭坛召唤首领。</li>
 * </ol>
 *
 * <p>以前只有一个"是否在副本维度"的判断，于是第二步刚传过去就被当成
 * "玩家跑出副本了"把挑战收掉；下一 tick 的"卡在 boss 维度"兜底又把玩家送回原世界。
 * 玩家实测表现就是"打完所有波次进不了决战场地 / 进去一秒被弹走"。</p>
 *
 * <p>把规则写成纯函数之后，可以用 javac 单独编译做离线断言
 * （见 {@code _cache/arena_stage_probe} 与交付报告），不需要跑 Minecraft。</p>
 */
public enum ArenaStage {

    /** 还在副本维度打怪潮。 */
    WAVE,

    /** 怪潮已尽，人在对应的 boss 决战维度里等召唤/打首领。 */
    ARENA,

    /** 挑战应该结束（作废或已通关）：人既不在副本维度、也不在该副本的决战维度。 */
    DONE;

    /**
     * 当前挑战阶段。
     *
     * @param inDungeonRealm 玩家是否在<b>本副本</b>的副本维度里
     * @param inArena        玩家是否在<b>本副本</b>对应的 boss 决战维度里
     * @param arenaSent      挑战里记录的"最后一波已清、已送进决战维度"标记
     */
    public static ArenaStage of(boolean inDungeonRealm, boolean inArena, boolean arenaSent) {
        if (!arenaSent) {
            return inDungeonRealm ? WAVE : DONE;
        }
        return inArena ? ARENA : DONE;
    }

    /** 挑战是否仍然有效（换维度时用它决定"保留还是作废"）。 */
    public static boolean keep(boolean inDungeonRealm, boolean inArena, boolean arenaSent) {
        return of(inDungeonRealm, inArena, arenaSent) != DONE;
    }

    /** 挑战是否应该被作废。 */
    public static boolean shouldAbort(boolean inDungeonRealm, boolean inArena, boolean arenaSent) {
        return of(inDungeonRealm, inArena, arenaSent) == DONE;
    }
}
