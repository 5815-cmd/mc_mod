package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.dungeon.Dungeon;

/**
 * 虚空之门框架：通往 {@code hongmeng:void_realm}（虚空裂隙）。
 *
 * <p>终局副本，门槛仙人（第 8 大境）。</p>
 */
public class VoidPortalFrameBlock extends DungeonPortalFrameBlock {

    public VoidPortalFrameBlock(Properties properties) {
        super(properties, Dungeon.VOID);
    }
}
