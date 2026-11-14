package com.hongmeng.xiuxian.dungeon;

import com.hongmeng.xiuxian.item.ModItems;
import com.hongmeng.xiuxian.core.CultivationCapability;
import com.hongmeng.xiuxian.core.CultivationData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 维度进度锁 + 指引（方案 B5，对应暮色森林的"结界 + 成就链"的低成本版）。
 *
 * <h2>锁什么</h2>
 *
 * <p>四座副本维度各有一件**凭证**（本模组已有物品，不新增注册）：</p>
 *
 * <table border="1">
 *   <caption>凭证与效果</caption>
 *   <tr><th>维度</th><th>凭证</th><th>没有凭证时</th></tr>
 *   <tr><td>纪元秘境</td><td>无（练气就能进，新手场）</td><td>—</td></tr>
 *   <tr><td>妖兽秘境</td><td>{@code hongmeng:jidan} 纪元令</td><td>妖气侵蚀（饥饿 I）</td></tr>
 *   <tr><td>魔道巢穴</td><td>{@code hongmeng:daoyin} 道印</td><td>魔气侵蚀（饥饿 I + 虚弱 I）</td></tr>
 *   <tr><td>虚空裂隙</td><td>{@code hongmeng:kongming_jing} 空冥精</td><td>虚空侵蚀（饥饿 I + 虚弱 I + 缓慢 I）</td></tr>
 * </table>
 *
 * <p><b>为什么用"背包里有凭证"当判据</b>：{@code CultivationData.dungeonClears} 是
 * <b>一个全局 int</b>，分不出"通关的是哪一座副本"；改成每维度一个字段就要动存档结构
 * （老存档迁移风险）。而这三件物品本来就有稳定来源（纪元令每次通关必给 1 个、
 * 道印 35%、空冥精 45%），拿它们当钥匙既零迁移、又符合"钥匙是可携带的战利品"。</p>
 *
 * <h2>为什么不直接拦住玩家</h2>
 *
 * <p>硬结界（不能挖 / 不能开箱）要拦 {@code BreakEvent} 与 {@code RightClickBlock}，
 * 一旦写漏条件就会变成"玩家在主世界也挖不动"这种大事故。这里先做**软锁**：
 * 没凭证会持续挂负面效果，战斗压力自己把没准备好的玩家赶出去——
 * 与暮色"地形结界叠负面 BUFF"是同一个思路，代价却低一个数量级。</p>
 */
public final class RealmProgression {

    /** 侵蚀的施加间隔（tick）。20 = 每秒，取 40 是为了不刷屏。 */
    public static final int MIASMA_INTERVAL = 40;

    /** 侵蚀效果的持续时间（tick）：要比重施间隔长，否则中间会断档。 */
    public static final int MIASMA_DURATION = 100;

    private RealmProgression() {
    }

    /** 一个维度的进度锁定义。 */
    public record Lock(Dungeon dungeon, Item key, String keyName, String miasma) {
    }

    /**
     * 四座维度的锁。**顺序与 {@link Dungeon} 一致**，离线可断言。
     *
     * <p>{@code EPOCH} 的 {@code key} 为 null：新手场不上锁。</p>
     */
    public static List<Lock> locks() {
        List<Lock> list = new ArrayList<>();
        list.add(new Lock(Dungeon.EPOCH, null, "（不需要）", "无"));
        list.add(new Lock(Dungeon.BEAST, ModItems.JIDAN.get(), "纪元令", "妖气侵蚀：饥饿"));
        list.add(new Lock(Dungeon.DEMON, ModItems.DAOYIN.get(), "道印",
                "魔气侵蚀：饥饿 + 虚弱"));
        list.add(new Lock(Dungeon.VOID, ModItems.KONGMING_ESSENCE.get(), "空冥精",
                "虚空侵蚀：饥饿 + 虚弱 + 缓慢"));
        return list;
    }

    /** 玩家背包（含副手）里有没有这件凭证。 */
    public static boolean hasKey(ServerPlayer player, Item key) {
        if (key == null) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(key)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.is(key)) {
                return true;
            }
        }
        return false;
    }

    /** 这个维度对应的锁；不是副本维度返回 null。 */
    public static Lock lockFor(ResourceKey<Level> dimension) {
        if (dimension == null) {
            return null;
        }
        String id = dimension.location().toString();
        for (Lock lock : locks()) {
            if (("hongmeng:" + lock.dungeon().dimensionId()).equals(id)) {
                return lock;
            }
        }
        return null;
    }

    /**
     * 每 {@value #MIASMA_INTERVAL} tick 给未解锁的玩家叠一次侵蚀。
     *
     * <p>境界已经远超门槛的玩家（大境 ≥ 门槛 + 2）不再受侵蚀——
     * 不然"回新手场刷材料"会一直被恶心，而那时玩家早就打得过看守了。</p>
     *
     * @return 施加了侵蚀返回 true（给调用方做提示用）
     */
    public static boolean tickMiasma(ServerPlayer player) {
        Lock lock = lockFor(player.level().dimension());
        if (lock == null || lock.key() == null) {
            return false;
        }
        if (player.tickCount % MIASMA_INTERVAL != 0) {
            return false;
        }
        CultivationData data = CultivationCapability.get(player);
        // 用 var 推断，免得为了一个局部变量再 import core.Realm
        var realm = data.realm();
        if (realm.tier() >= lock.dungeon().minTier() + 2) {
            return false;
        }
        if (hasKey(player, lock.key())) {
            return false;
        }
        // 侵蚀本身（与原版效果，不新增注册；"魔气侵蚀"用饥饿 + 虚弱表达）
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, MIASMA_DURATION, 0,
                true, false));
        if (lock.dungeon() == Dungeon.DEMON || lock.dungeon() == Dungeon.VOID) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, MIASMA_DURATION, 0,
                    true, false));
        }
        if (lock.dungeon() == Dungeon.VOID) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MIASMA_DURATION,
                    0, true, false));
        }
        return true;
    }

    /**
     * 给 RCON / 命令用的指路文案：每一座维度地标的坐标与距离。
     *
     * <p>这是"原则 5（指引）"的最便宜版本：不做地图物品，直接把坐标算出来打印。</p>
     */
    public static List<String> guide(ServerPlayer player, Dungeon dungeon) {
        List<String> out = new ArrayList<>();
        Lock lock = lockFor(ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation("hongmeng", dungeon.dimensionId())));
        boolean unlocked = lock == null || hasKey(player, lock.key());
        out.add("\u00a7b=== " + dungeon.colored() + "\u00a7b === \u00a78(需要第 "
                + dungeon.minTier() + " 大境)");
        out.add("\u00a7f维度：\u00a7e" + dungeon.fullDimensionId()
                + "\u00a77   凭证：\u00a7e" + (lock == null ? "（不需要）" : lock.keyName())
                + "\u00a77   状态：" + (unlocked ? "\u00a7a已解锁" : "\u00a7c未解锁"));
        if (lock != null && lock.key() != null) {
            out.add("\u00a77侵蚀：\u00a7c" + lock.miasma()
                    + "\u00a77（背包里带上 " + lock.keyName() + " 即可解除）");
        }
        out.add("\u00a77地标（用 \u00a7f/locate structure hongmeng:<id>\u00a77 定位，"
                + "再 /tp 过去）：");
        for (String sid : landmarkIds(dungeon)) {
            out.add("\u00a78  · \u00a7fhongmeng:" + sid);
        }
        return out;
    }

    /** 每个维度的地标 id（与 B4 产出的 9 座一致）。 */
    public static List<String> landmarkIds(Dungeon dungeon) {
        return switch (dungeon) {
            case EPOCH -> List.of("epoch_altar", "epoch_watchtower");
            case BEAST -> List.of("beast_totem", "beast_pen");
            case DEMON -> List.of("demon_gate", "demon_altar");
            case VOID -> List.of("void_dao_tai", "void_pavilion", "void_rift_stone");
        };
    }

    /** 给玩家的动作栏提示（进维度时提醒一次）。 */
    public static Component unlockHint(Dungeon dungeon, String keyName) {
        return Component.literal("\u00a7c此地有" + dungeon.cn() + "的结界：带上\u00a7e"
                + keyName + "\u00a7c才能不被侵蚀。");
    }
}
