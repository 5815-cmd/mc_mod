package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.core.CultivationData;
import com.hongmeng.xiuxian.network.ModNetworkBridge;
import com.hongmeng.xiuxian.talent.CareerActive;
import com.hongmeng.xiuxian.talent.Talents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * <b>职业主动的服务端「解锁授予层」</b>（《07-法术内容交接表》§2 / 验收清单第 2 条）。
 *
 * <h2>它解决什么</h2>
 * <p>20 门职业主动里 <b>18 门是新增条目、不生成秘籍也不进坊市</b> —— 玩家没有任何
 * 物品途径能"学会"它们。所以必须由服务端在玩家满足天赋条件时<b>直接授予</b>：</p>
 * <ul>
 *   <li>所在天赋分支累计投入 <b>5 点</b>（{@code Talents.activeUnlocked}）→ 学会该主动；</li>
 *   <li>不满足时：<b>18 门新增</b>会被收回（洗忆丹洗点 / 转道丹转职后自动失效），
 *       并从法术栏槽位上摘掉；<b>2 门复用</b>（万剑归宗 / 金刚护体）本体仍在基础池，
 *       不收回（它们的秘籍/坊市身份还在）。</li>
 * </ul>
 *
 * <h2>为什么"未授予 = 法术栏里看不见"</h2>
 * <p>法术栏只列 {@code TechniqueData.knownList()}（= 已修习）。所以不写任何 UI 代码，
 * 只要"未解锁时 has=false"，法术栏自然看不到、也装不上；服务端 {@code cast()}
 * 再校验一次，封死"改包伪造装配"。</p>
 *
 * <h2>调用点（都是"加一行"）</h2>
 * <ul>
 *   <li>{@link SpellCastHandler#tickPlayer} 的每 20 tick 被动刷新分支；</li>
 *   <li>{@link SpellCastHandler#onLoggedIn} / {@link SpellCastHandler#onRespawn}。</li>
 * </ul>
 *
 * <p>纯服务端逻辑，不碰客户端/网络协议；同步用既有的
 * {@link ModNetworkBridge#sync(ServerPlayer)}（整份修炼数据下发）。</p>
 */
public final class CareerSpellGrant {

    private CareerSpellGrant() {
    }

    /**
     * 刷新玩家的职业主动授予状态（<b>幂等</b>，每 20 tick 调一次也不会重复发消息）。
     *
     * @return true 表示这次真的有变化（新学会 / 被收回），并已同步给客户端
     */
    public static boolean refresh(ServerPlayer player, CultivationData data) {
        if (player == null || data == null) {
            return false;
        }
        TechniqueData techniques = data.getTechniques();
        if (techniques == null) {
            return false;
        }
        boolean changed = false;
        for (CareerActive active : CareerActive.values()) {
            Technique technique = Technique.byName(active.techniqueName());
            if (technique == null) {
                continue;              // 数据层与枚举不同步时静默跳过，不能影响登录
            }
            if (Talents.activeUnlocked(player, active)) {
                // **双门槛**：天赋（分支累投 5 点）+ 境界（该主动的 minTier）。
                // 境界不够时先不授予 —— 否则法术栏会出现"能装、但一放就提示境界不足"的
                // 幽灵条目；境界只升不降，所以学会之后不会被这条收回。
                boolean tierOk = data.realm() != null
                        && data.realm().tier() >= technique.minTier();
                if (tierOk && techniques.learn(technique)) {
                    // 只显示一条动作栏：18 门同时解锁时会刷屏，所以只在"新学会"时提示。
                    changed = true;
                    player.displayClientMessage(Component.literal(
                            "\u00a7a职业功法解锁 \u00a77· " + technique.colored()
                                    + "\u00a78（按 O 装进法术栏）"), true);
                }
            } else if (CareerTechnique.isUnlockOnly(technique)
                    && techniques.has(technique)) {
                // 洗点 / 转职：18 门"只能天赋解锁"的主动必须一起失效，
                // 否则玩家会留下一个已经放不出去、却还占着法术栏的幽灵条目。
                techniques.forget(technique);      // forget 内部会先 unequip 再删
                changed = true;
                player.displayClientMessage(Component.literal(
                        "\u00a77职业功法失效 \u00a78· " + technique.colored()
                                + "\u00a78（天赋分支不足 5 点）"), true);
            }
        }
        if (changed) {
            ModNetworkBridge.sync(player);
        }
        return changed;
    }

    /**
     * 这门功法对这名玩家<b>当前是否已解锁</b>。
     *
     * <p>只对职业主动有意义；非职业功法（旧 52 门）返回 true ——
     * 调用方 {@link TechniqueCaster#cast} 本来就只在 {@code isCareerGated} 为真时才会问。</p>
     */
    public static boolean isUnlocked(ServerPlayer player, Technique technique) {
        if (player == null || technique == null) {
            return false;
        }
        CareerActive active = CareerTechnique.activeOf(technique);
        if (active == null) {
            return true;               // 不是职业主动：不额外加锁，保持旧 52 门行为
        }
        return Talents.activeUnlocked(player, active);
    }

    /** 按职业主动数据判断（给命令/测试用，语义与上面一致）。 */
    public static boolean isUnlocked(ServerPlayer player, CareerActive active) {
        return player != null && active != null && Talents.activeUnlocked(player, active);
    }
}
