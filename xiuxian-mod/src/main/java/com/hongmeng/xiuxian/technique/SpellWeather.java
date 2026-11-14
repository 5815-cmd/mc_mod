package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>环境影响类法术会改变天气</b>（玩家要求）。
 *
 * <p>玩家原话：「释放那些影响环境的法术，可以在释放法术开始到特效消失后一段时间内
 * 影响天气，比如冰系就是下雪」。</p>
 *
 * <table border="1">
 *   <caption>元素 → 天气</caption>
 *   <tr><th>元素</th><th>天气</th></tr>
 *   <tr><td>冰 / 水</td><td>降雨（寒冷群系由原版机制自动变成下雪）</td></tr>
 *   <tr><td>雷</td><td>雷暴</td></tr>
 *   <tr><td>火 / 金 / 土 / 风 / 木 / 无</td><td>放晴（"焚天"之后云开雾散）</td></tr>
 * </table>
 *
 * <p><b>为什么不用一次性硬设</b>：那会让天气瞬间跳变、观感生硬，而且万一玩家
 * 本来就在下雨，我们会把他的世界永久改掉。所以这里记一个"到期时间戳"，
 * 到期后<b>恢复原天气</b>。</p>
 */
public final class SpellWeather {

    /** 影响时长（tick）：特效 140 + 之后再多留 200（10 秒）。 */
    private static final int HOLD_TICKS = 340;

    /** 每个维度：slot[0] = 原天气快照（bit0 下雨, bit1 雷暴），slot[1] = 到期 tick。 */
    private static final Map<ResourceKey<Level>, long[]> TOUCHED = new ConcurrentHashMap<>();

    private SpellWeather() {
    }

    /**
     * 按元素影响天气。
     *
     * @param level    施法所在维度
     * @param element  法术元素
     * @param grandeur 气势（{@code TechniqueKind.chargeGrandeur()}）——
     *                 只有"够大"的法术才改天，小法术改天气会很烦
     */
    public static void apply(ServerLevel level, Element element, double grandeur) {
        if (level == null || element == null || grandeur < 0.6D) {
            return;
        }
        try {
            boolean wantRain = element == Element.ICE || element == Element.WATER;
            boolean wantThunder = element == Element.THUNDER;
            long[] slot = TOUCHED.computeIfAbsent(level.dimension(),
                    k -> new long[]{-1L, 0L});
            if (slot[0] < 0L) {
                // 第一次动这个维度的天气：先把原状记下来
                long snap = 0L;
                if (level.isRaining()) {
                    snap |= 1L;
                }
                if (level.isThundering()) {
                    snap |= 2L;
                }
                slot[0] = snap;
            }
            slot[1] = level.getGameTime() + HOLD_TICKS;
            if (wantThunder) {
                level.setWeatherParameters(0, HOLD_TICKS, true, true);
            } else if (wantRain) {
                level.setWeatherParameters(0, HOLD_TICKS, true, false);
            } else {
                level.setWeatherParameters(HOLD_TICKS, 0, false, false);
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[hongmeng] 天气影响失败：{}", t.toString());
        }
    }

    /** 每 tick 检查到期，恢复原天气（由 {@code ModEvents} 的玩家 tick 转发）。 */
    public static void tick(ServerLevel level) {
        if (level == null || TOUCHED.isEmpty()) {
            return;
        }
        long[] slot = TOUCHED.get(level.dimension());
        if (slot == null || slot[0] < 0L || slot[1] == 0L
                || level.getGameTime() <= slot[1]) {
            return;
        }
        try {
            boolean wasRain = (slot[0] & 1L) != 0L;
            boolean wasThunder = (slot[0] & 2L) != 0L;
            level.setWeatherParameters(wasRain ? 6000 : 6000, wasRain ? 6000 : 0,
                    wasRain, wasThunder);
        } catch (Throwable ignored) {
            // 恢复失败无所谓，天气本来就会自然变化
        }
        slot[0] = -1L;
        slot[1] = 0L;
    }
}
