package com.hongmeng.xiuxian.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * <b>修行录数据</b>：在资源重载时加载并缓存 {@code assets/hongmeng/guide/guide.json}。
 *
 * <h2>为什么不直接在界面里读</h2>
 * <p>第一版是在 {@code GuideBookScreen.init()} 里临时读的，结果玩家打开书是**空的** ——
 * 界面构造的时机可能早于资源包就绪，而我把异常吞成了"空数据"，
 * 于是界面上一个条目都没有、也看不出为什么。</p>
 *
 * <p>现在改用 Forge 的 {@code RegisterClientReloadListenersEvent}：
 * <b>资源一就绪就加载</b>，界面只读这份缓存，时序问题从根上消失；
 * 而且加载失败会保留 {@link #error()} 供界面显示出来，不再静默。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class GuideData {

    /** 数据文件位置（与 tools/build_guide_data.py 的输出一致）。 */
    private static final ResourceLocation LOC =
            new ResourceLocation("hongmeng", "guide/guide.json");

    private static JsonObject root;
    private static String error = "";
    private static int itemCount;
    private static int sourceCount;

    private GuideData() {
    }

    /** 资源重载时调用（也用于首次兜底加载）。 */
    public static void reload() {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager()
                    .getResource(LOC);
            if (res.isEmpty()) {
                root = null;
                error = "找不到 " + LOC;
                com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                        "[hongmeng] 修行录数据缺失：{}", LOC);
                itemCount = 0;
                sourceCount = 0;
                return;
            }
            try (var in = res.get().open()) {
                root = JsonParser.parseReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            error = "";
            itemCount = root.has("items") ? root.getAsJsonObject("items").size() : 0;
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[hongmeng] 修行录数据已载入：物品 {} 个", itemCount);
            sourceCount = 0;
            if (root.has("items")) {
                for (var e : root.getAsJsonObject("items").entrySet()) {
                    if (e.getValue().isJsonObject()
                            && e.getValue().getAsJsonObject().has("sources")) {
                        sourceCount += e.getValue().getAsJsonObject()
                                .getAsJsonArray("sources").size();
                    }
                }
            }
        } catch (Throwable t) {
            root = null;
            error = t.getClass().getSimpleName() + ": " + t.getMessage();
            itemCount = 0;
            sourceCount = 0;
        }
    }

    /** 根对象；没加载成功时为 null。 */
    public static JsonObject root() {
        if (root == null) {
            reload();       // 兜底：万一重载事件没赶上，这里再试一次
        }
        return root;
    }

    /** 加载失败的原因（空串 = 正常）。 */
    public static String error() {
        return error;
    }

    public static int itemCount() {
        return itemCount;
    }

    public static int sourceCount() {
        return sourceCount;
    }

    /** 数据文件的显示用路径。 */
    public static String location() {
        return LOC.toString();
    }
}
