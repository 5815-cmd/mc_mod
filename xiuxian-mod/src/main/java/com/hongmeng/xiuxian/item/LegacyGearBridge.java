package com.hongmeng.xiuxian.item;

import com.hongmeng.xiuxian.talent.Career;
import com.hongmeng.xiuxian.talent.Path;

/**
 * <b>旧装备（legacy）过渡桥</b>：R-1 阶段把新职业 {@link Career} 映射回旧道途 {@link Path}。
 *
 * <h2>R-2 状态（2026-11-01 起）</h2>
 * <p>职业甲 / 通用武器已经上线，旧 200 件道途甲 + 50 件道途武器 + 10 把境界剑
 * 按 06 §4 统一进 <b>legacy</b>：<b>保留注册、移出创造栏、删配方、不给加成</b>。
 * 所以本桥现在<b>恒返回 {@code null}</b>——旧装备的加成代码路径全部短路，
 * 但类与方法保留（回滚只需恢复 switch，不必改调用方）。</p>
 *
 * <p>红线：不物理删除已注册物品（老存档里的旧装备继续存在，只是不再有用）。</p>
 */
public final class LegacyGearBridge {

    private LegacyGearBridge() {
    }

    /**
     * 新职业 → 旧道途。<b>R-2 起恒为 {@code null}</b>：旧装备统一 legacy、不给加成。
     *
     * @return 一律 {@code null}（保留签名供 PathArmorBonus / PathWeaponBonus 调用）
     */
    public static Path toLegacyPath(Career career) {
        return null;
    }
}
