package com.hongmeng.xiuxian.client.geo;

import com.hongmeng.xiuxian.item.armor.CareerArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 职业甲 GeoModel：10 套共用一套资源命名规则 ——
 * {@code career_armor_<career>_<1|2>}（career = jian/xue/mo/gui/ti）。
 *
 * <p>模型/贴图/动画都按 {@link CareerArmorItem#set()} 派生，
 * 所以 40 件物品只需要 10 组资源，R-2 铺开时不用为每件写类。</p>
 *
 * <p>PERF(C)：这三个 getter 会被渲染端<b>每帧每件</b>调用（GeoArmorRenderer 每帧取
 * 贴图/模型路径）。原来每次都要拼 {@code "career_armor_..."} 字符串并 new 三个
 * ResourceLocation。物品是注册期单例（40 件），所以按物品缓存整套资源路径，
 * 第一次取用时建好、之后只是查表。</p>
 */
public class CareerArmorModel extends GeoModel<CareerArmorItem> {

    private static final String ROOT = "hongmeng";

    /** PERF(C): 物品 → [geo, 贴图, 动画]。 */
    private static final Map<CareerArmorItem, ResourceLocation[]> RESOURCES =
            new ConcurrentHashMap<>();

    private static ResourceLocation[] resourcesOf(CareerArmorItem item) {
        return RESOURCES.computeIfAbsent(item, it -> {
            String base = base(it);
            return new ResourceLocation[]{
                    new ResourceLocation(ROOT, "geo/" + base + ".geo.json"),
                    new ResourceLocation(ROOT, "textures/item/" + base + ".png"),
                    new ResourceLocation(ROOT, "animations/" + base + ".animation.json"),
            };
        });
    }

    @Override
    public ResourceLocation getModelResource(CareerArmorItem item) {
        return resourcesOf(item)[0];
    }

    @Override
    public ResourceLocation getTextureResource(CareerArmorItem item) {
        return resourcesOf(item)[1];
    }

    @Override
    public ResourceLocation getAnimationResource(CareerArmorItem item) {
        return resourcesOf(item)[2];
    }

    /** career_armor_<career>_<realm>：每个境界一套独立外观，例如 career_armor_jian_7。 */
    private static String base(CareerArmorItem item) {
        return "career_armor_" + item.set().career().id() + "_" + item.realm();
    }
}
