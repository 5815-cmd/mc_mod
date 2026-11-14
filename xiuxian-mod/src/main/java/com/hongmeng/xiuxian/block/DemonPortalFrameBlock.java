package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.dungeon.Dungeon;

/**
 * 魔道之门框架：通往 {@code hongmeng:demon_realm}（魔道巢穴）。
 *
 * <p>门槛是道印（第 6 大境），不够境界连火都点不着。</p>
 */
public class DemonPortalFrameBlock extends DungeonPortalFrameBlock {

    public DemonPortalFrameBlock(Properties properties) {
        super(properties, Dungeon.DEMON);
    }
}
