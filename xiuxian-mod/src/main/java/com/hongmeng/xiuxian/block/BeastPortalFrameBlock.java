package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.dungeon.Dungeon;

/**
 * 妖兽之门框架：通往 {@code hongmeng:beast_realm}（妖兽秘境）。
 *
 * <p>搭成 2~4 格宽、3~5 格高的门形，用打火石在门上点火，
 * 门内会亮起传送门——走进去才算进门。门槛是金丹（第 3 大境）。</p>
 */
public class BeastPortalFrameBlock extends DungeonPortalFrameBlock {

    public BeastPortalFrameBlock(Properties properties) {
        super(properties, Dungeon.BEAST);
    }
}
