package com.hongmeng.xiuxian.achievement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * <b>鸿蒙修仙的自定义成就触发器</b>（玩家要求「增加和原版一样的成就系统」）。
 *
 * <h2>为什么走原版成就框架，而不是自己写一套</h2>
 * <p>原版 {@code advancements} 已经给了我们要的一切：
 * <ul>
 *   <li><b>获得成就时的弹窗</b>（那个滑入的 toast）；</li>
 *   <li><b>成就界面</b>（进度树、图标、标题、描述）；</li>
 *   <li>存档持久化、多人同步、命令 {@code /advancement}。</li>
 * </ul>
 * 自己写一套既要画 UI 又要处理存档，而且玩家按 L 打开的成就界面还会是空的 ——
 * 那样就不叫"和原版一样"了。</p>
 *
 * <h2>怎么用</h2>
 * <p>成就的 JSON 放在 {@code data/hongmeng/advancements/*.json}，
 * 里面写 {@code "trigger": "hongmeng:cultivation"} 并带一个 {@code "id"} 参数；
 * 代码里在合适的时机调用 {@link #grant(ServerPlayer, String)} 即可。</p>
 */
public class CultivationTrigger extends SimpleCriterionTrigger<CultivationTrigger.Instance> {

    /** 全局唯一实例（注册在 {@code ModAdvancements} 里）。 */
    public static final CultivationTrigger INSTANCE = new CultivationTrigger();

    private static final ResourceLocation ID =
            new ResourceLocation("hongmeng", "cultivation");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player,
                                      DeserializationContext ctx) {
        String id = json.has("id") ? json.get("id").getAsString() : "";
        return new Instance(player, id);
    }

    /**
     * 发放成就。
     *
     * @param player 玩家
     * @param id     成就标识（与 JSON 里的 {@code id} 对应）
     */
    public static void grant(ServerPlayer player, String id) {
        if (player == null || id == null || id.isEmpty()) {
            return;
        }
        try {
            INSTANCE.trigger(player, inst -> inst.matches(id));
        } catch (Throwable t) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                    "[hongmeng] 成就 {} 发放失败：{}", id, t.toString());
        }
    }

    /** 一个触发器实例：只比对 id。 */
    public static class Instance extends AbstractCriterionTriggerInstance {
        private final String id;

        public Instance(ContextAwarePredicate player, String id) {
            super(CultivationTrigger.ID, player);
            this.id = id;
        }

        public boolean matches(String want) {
            return want != null && want.equals(this.id);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext ctx) {
            JsonObject o = super.serializeToJson(ctx);
            o.addProperty("id", id);
            return o;
        }
    }
}
