package com.hongmeng.xiuxian.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 破坏方块「黑匣子」：把服务端看到的每一次破坏尝试记在一个小环形缓冲里。
 *
 * <h2>为什么需要它</h2>
 *
 * <p>玩家报过一次很难复现的问题：**挖掉方块后它看不见了，但踩上去还在、也没有掉落物**
 * （幽灵方块）。服务端造成这种现象的链路只有一条 ——
 * {@code ForgeHooks.onBlockBreakEvent} 返回 -1（破坏事件被取消），
 * 于是 {@code ServerPlayerGameMode.destroyBlock} 直接 {@code return false}：
 * 方块不删、不掉落，而客户端早就本地预测"挖掉了"。</p>
 *
 * <p>要判断是"事件被取消"还是"服务端**根本没收到**破坏请求"，
 * 光看当前状态（{@code /hongmeng breakdiag}）不够 —— 必须在**事发当时**留痕。
 * 所以这里在两条关键路径上各记一笔：</p>
 *
 * <ul>
 *   <li>{@link #recordHarvestCheck}：服务端问过"这个人能不能采这个方块吗"
 *       —— 说明服务端**确实参与**了这次挖掘；</li>
 *   <li>{@link #recordBreakEvent}：破坏事件真的发出来了 —— 说明它通过了
 *       `canAttackBlock` 与 `blockActionRestricted` 两道前置判定，
 *       方块接下来必然会被删掉。</li>
 * </ul>
 *
 * <p>于是："客户端说挖了、但缓冲区里既没有 check 也没有 break" ⇒ 服务端没收到请求
 * （网络/客户端侧）；"有 check 没有 break" ⇒ 事件被取消（第六步之后被拦）；
 * "有 break 却没有掉落" ⇒ 问题在掉落环节而不是破坏环节。**三种情况的修法完全不同。**</p>
 *
 * <h2>开销</h2>
 *
 * <p>只在 {@code realmMining} 开着、且玩家已觉醒时记录；每条只存字符串，
 * 环形缓冲上限 {@value #CAPACITY} 条。**不写日志文件、不发包**，正常玩没有感知。</p>
 */
public final class MiningDiag {

    /** 环形缓冲上限（够看最近几十秒）。 */
    public static final int CAPACITY = 24;

    /** 最近的事件（新的在前）。 */
    private static final Deque<String> EVENTS = new ArrayDeque<>();

    /** 总共记录了多少条（缓冲被挤掉也能看出"发生过很多次"）。 */
    private static long total = 0L;

    private MiningDiag() {
    }

    private static void push(String line) {
        // **诊断绝不允许影响玩法。**
        //
        // 这三条 record* 是从采集判定 / 破坏事件里调的；那里抛出异常会被 Forge
        // 当成"事件处理失败"而中止整条事件 —— 采集判定被中止 = canHarvest 保持 false
        // = 方块能破但什么都不掉。玩家因此报过"挖矿依旧不掉落"。
        // 所以这里把任何异常都吞掉：黑匣子记不到就算了，玩法必须照常。
        try {
            total++;
            EVENTS.addFirst(line);
            while (EVENTS.size() > CAPACITY) {
                EVENTS.removeLast();
            }
        } catch (Throwable ignored) {
            // 故意忽略
        }
    }

    /**
     * 左键**按下**到了服务端（{@code START_DESTROY_BLOCK} / {@code LeftClickBlock}）。
     *
     * <p>这是挖掘链的第一环。它出现只能说明"服务端知道你在挖"，
     * 不代表方块会被删 —— 后面的采掘判定与破坏事件才是关键。</p>
     */
    public static void recordLeftClickStart(String player, String block, String item) {
        push("[左键按下] " + player + " " + block + " 手持 " + item);
    }

    /** 服务端问过"能不能采"（{@code PlayerEvent.HarvestCheck}）。 */
    public static void recordHarvestCheck(String player, String block, String item,
                                          boolean before, boolean after) {
        push("[采掘判定] " + player + " " + block + " 手持 " + item
                + " 判定前 " + (before ? "可采" : "不可采")
                + " → 判定后 " + (after ? "可采" : "不可采"));
    }

    /** 服务端真的发出了破坏事件（{@code BlockEvent.BreakEvent}）。 */
    public static void recordBreakEvent(String player, String block, String item,
                                        String action) {
        push("[破坏事件] " + player + " " + block + " 手持 " + item + " → " + action);
    }

    /** 记录一次"看到了但没管"的情况，便于对照。 */
    public static void recordNote(String note) {
        push("[说明] " + note);
    }

    /** 缓冲区内容（新的在前）。 */
    public static String[] recent() {
        return EVENTS.toArray(new String[0]);
    }

    public static long totalRecorded() {
        return total;
    }

    public static boolean isEmpty() {
        return EVENTS.isEmpty();
    }
}
