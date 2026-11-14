package com.hongmeng.xiuxian.block;

import com.hongmeng.xiuxian.dungeon.Dungeon;
import net.minecraft.util.StringRepresentable;

/**
 * {@link DungeonPortalBlock} 的目标副本。
 *
 * <p><b>为什么要多这一层枚举：</b>原版 {@code EnumProperty} 要求枚举实现
 * {@link StringRepresentable} 才能进方块状态；而 {@link Dungeon} 是刻意做成
 * "不含任何 Minecraft 类型"的纯数据枚举（离线验证器要能直接断言，文件头注释里写明了）。
 * 所以这里加一层薄包装：方块状态序列化成维度名（{@code beast_realm} 等），
 * 业务逻辑拿到的仍然是 {@link Dungeon} 本体。</p>
 */
public enum PortalTarget implements StringRepresentable {

    EPOCH(Dungeon.EPOCH),
    BEAST(Dungeon.BEAST),
    DEMON(Dungeon.DEMON),
    VOID(Dungeon.VOID);

    private final Dungeon dungeon;

    PortalTarget(Dungeon dungeon) {
        this.dungeon = dungeon;
    }

    public Dungeon dungeon() {
        return dungeon;
    }

    /** 方块状态里写的就是副本维度名，例如 {@code dungeon=beast_realm}。 */
    @Override
    public String getSerializedName() {
        return dungeon.dimensionId();
    }

    public static PortalTarget of(Dungeon dungeon) {
        for (PortalTarget target : values()) {
            if (target.dungeon == dungeon) {
                return target;
            }
        }
        return BEAST;
    }
}
