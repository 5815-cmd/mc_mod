package com.hongmeng.xiuxian.spirit;

/**
 * 灵根元素。
 *
 * <p>五行：金、木、水、火、土。
 * 变异：冰、雷、风。
 * 无属性：{@link #NONE}——血魔噬、神识震这类不属于任何五行体系的功法用它。</p>
 *
 * <p>元素决定"对应元素的功法有加成"——道印凝聚出的本命领域会按元素给功法加成。</p>
 *
 * <p><b>注意：{@link #NONE} 必须放在最后。</b>功法/道宫的 NBT 与网络协议里
 * 存的是 ordinal，往中间插值会让老存档读错。</p>
 */
public enum Element {

    METAL("金", 0xFFE8C86A, false),
    WOOD("木", 0xFF6FD08C, false),
    WATER("水", 0xFF5FA8E8, false),
    FIRE("火", 0xFFE8703A, false),
    EARTH("土", 0xFFC2A165, false),
    ICE("冰", 0xFF8FD8FF, true),
    THUNDER("雷", 0xFFB98CFF, true),
    WIND("风", 0xFF4FD8E8, true),
    /** 无属性：不参与五行相生相克，也不吃道宫的元素加成。 */
    NONE("无", 0xFFAAAAAA, false);

    private final String cn;
    private final int color;
    private final boolean mutated;

    Element(String cn, int color, boolean mutated) {
        this.cn = cn;
        this.color = color;
        this.mutated = mutated;
    }

    public String cn() {
        return cn;
    }

    public int color() {
        return color;
    }

    /** 是否是变异元素（冰/雷/风）。 */
    public boolean isMutated() {
        return mutated;
    }

    /** 显示用：变异元素加个"（变异）"。 */
    public String display() {
        return mutated ? cn + "（变异）" : cn;
    }
}
