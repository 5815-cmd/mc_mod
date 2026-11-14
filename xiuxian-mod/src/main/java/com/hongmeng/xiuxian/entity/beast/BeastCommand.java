package com.hongmeng.xiuxian.entity.beast;

/**
 * 灵兽指令：<b>跟随 → 停留 → 游荡</b> 三态循环。
 *
 * <p>顺序与名称直接对齐 Mythic Mounts（调研见 {@code docs/spirit-beast-spec.md}）：
 * 玩家对这三个词已经有肌肉记忆，不要自创第四种叫法。</p>
 *
 * <p>交互方式是<b>潜行 + 右键循环</b>，不做轮盘菜单——轮盘要多一个界面、
 * 多一套网络包，而三态循环一个键位就够。</p>
 */
public enum BeastCommand {

    /** 跟随主人（默认）。离太远会传送回来，避免"坐骑走丢"。 */
    FOLLOW("跟随", 'a'),

    /** 原地停留。等同于原版狼的坐下：不动、不攻击、仍可骑乘。 */
    SIT("停留", 'e'),

    /** 自由游荡。在出生点附近自己溜达，不跟着主人。 */
    WANDER("游荡", '7');

    private final String cn;
    private final char colorCode;

    BeastCommand(String cn, char colorCode) {
        this.cn = cn;
        this.colorCode = colorCode;
    }

    public String cn() {
        return cn;
    }

    public char colorCode() {
        return colorCode;
    }

    /** 带颜色代码的名字，直接塞进聊天/动作栏文本。 */
    public String colored() {
        return "\u00a7" + colorCode + cn;
    }

    /** 循环到下一个指令：跟随 → 停留 → 游荡 → 跟随。 */
    public BeastCommand next() {
        BeastCommand[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** 越界/缺档时的安全解析。 */
    public static BeastCommand byName(String name) {
        if (name != null) {
            for (BeastCommand c : values()) {
                if (c.name().equals(name)) {
                    return c;
                }
            }
        }
        return FOLLOW;
    }
}
