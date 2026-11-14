package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.dungeon.Dungeon;

/**
 * 纪元之门框架。搭成 2~4 格宽、3~5 格高的门形，用打火石在门内点火，
 * 门内会亮起 {@link DungeonPortalBlock}——<b>玩家走进去</b>才算进门。
 *
 * <p>2026-11-07 修复：旧实现点火后直接调用 {@code EpochRealm.enter}，
 * 门里不会生成传送门方块，看起来像"点完火就被吸走"。
 * 现在和其它三座秘境门走同一套 {@link DungeonPortalFrameBlock} 流程：
 * 点火 → 门内注满传送门方块 → 走进方块才传送；
 * 拆框架时也会自动收掉周围属于纪元秘境的传送门方块。</p>
 */
public class EpochPortalFrameBlock extends DungeonPortalFrameBlock {

    public EpochPortalFrameBlock(Properties properties) {
        super(properties, Dungeon.EPOCH);
    }
}
