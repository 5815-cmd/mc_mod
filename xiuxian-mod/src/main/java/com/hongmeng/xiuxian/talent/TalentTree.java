package com.hongmeng.xiuxian.talent;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * <b>职业天赋点系统</b>——新五职业的四支树（数据在 {@link CareerTalentTree}）。
 *
 * <h2>点数</h2>
 * <ul>
 *   <li>首次选定职业送 {@link #POINTS_ON_CHOOSE} 点；</li>
 *   <li>每突破一个大境 +{@link #POINTS_PER_REALM} 点；</li>
 *   <li>点数不单独存"余额"，只存每个节点的等级，
 *       剩余 = 应得 − 已花（洗点/转职永不自洽出错）。</li>
 * </ul>
 *
 * <h2>前置</h2>
 * <p>同一支的第 2 个节点需要该支累计投入
 * {@link CareerTalentTree#SECOND_NODE_REQUIRES} 点。技能解锁不占节点：
 * 分支累计 2 / 4 点解锁该支被动，5 点解锁该支主动（见 {@link CareerSkill} /
 * {@link CareerActive}）。</p>
 */
public final class TalentTree {

    /** 每突破一个大境给几点。 */
    public static final int POINTS_PER_REALM = 2;

    /** 首次选定职业赠送的点数。 */
    public static final int POINTS_ON_CHOOSE = 3;

    private TalentTree() {
    }

    /** 某职业的 8 个节点。 */
    public static List<CareerTalentTree.Node> forCareer(Career career) {
        return CareerTalentTree.ofCareer(career);
    }

    /** 某分支的 2 个节点。 */
    public static List<CareerTalentTree.Node> ofBranch(CareerBranch branch) {
        return CareerTalentTree.ofBranch(branch);
    }

    /** 按 id 找节点；找不到返回 null。 */
    public static CareerTalentTree.Node find(List<CareerTalentTree.Node> tree, String id) {
        for (CareerTalentTree.Node n : tree) {
            if (n.id().equals(id)) {
                return n;
            }
        }
        return null;
    }

    /**
     * 能不能把 {@code id} 再点一级。
     *
     * @param tree        该职业的节点表
     * @param id          节点 id
     * @param freePoints  剩余点数
     * @param branchSpent 该节点所在分支已投入的点数（用于第 2 节点前置）
     * @param rankOf      取某节点当前等级
     * @return 不能点的原因；能点返回 {@code null}
     */
    public static String canRankUp(List<CareerTalentTree.Node> tree, String id,
                                   int freePoints, int branchSpent,
                                   ToIntFunction<String> rankOf) {
        CareerTalentTree.Node node = find(tree, id);
        if (node == null) {
            return "没有这个天赋";
        }
        if (freePoints <= 0) {
            return "天赋点不够";
        }
        if (rankOf.applyAsInt(id) >= node.maxRank()) {
            return "已经满级";
        }
        if (branchSpent < node.branchRequired()) {
            return "需要这条分支先投入 " + node.branchRequired() + " 点";
        }
        return null;
    }
}
