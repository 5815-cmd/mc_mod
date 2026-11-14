package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.network.SpellFxPacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * <b>道途机制的状态特效</b> —— 阵域 / 血池 / 激荡 / 魔气。
 *
 * <h2>为什么需要这一层</h2>
 * <p>四个机制此前<b>只有功能、没有视觉</b>：玩家看到界面上的百分比，
 * 但世界里什么都没有。结果就是"我知道我有血池，但我感觉不到它"。</p>
 *
 * <p>机制类玩法靠的是<b>持续反馈</b>：血池满了要有血色涌动、
 * 阵域展开要有地面阵纹、激荡引爆要有一次明显的光爆。
 * 没有这些，"资源管理"就退化成盯着几个数字。</p>
 *
 * <h2>⚠ 为什么全部走"一个形状包 + 少量粒子"</h2>
 * <p>项目有一条硬线（见 {@code check_particle_packets.py}）：
 * <b>禁止在循环里逐颗发包</b>。所以这里：</p>
 * <ul>
 *   <li>主要的视觉是<b>一个 {@link SpellFxPacket}</b>（客户端画三维几何）——
 *       一个包就把整个阵纹/圆环画出来，比撒几百颗粒子便宜得多；</li>
 *   <li>粒子只用 {@code sendParticles(count)} 一次发完（让原版在范围内撒），
 *       不写 {@code for} 循环；</li>
 *   <li>粒子量走 {@code SpellVfx.take(...)} 的预算闸门，不会拖垮低配机器。</li>
 * </ul>
 *
 * <h2>⚠ 颜色统一由形状的 element 决定</h2>
 * <p>不在这里传颜色 —— 客户端按 {@code SpellFxPacket.element} 上色。
 * 各机制借用最接近的元素色：阵域=木（青玉）、血池=血（用火红）、
 * 激荡=雷、魔气=暗（用金的反面不好表达，借水紫）。这样"看颜色就知道是哪个机制"。</p>
 */
public final class PathMechanicFx {

    private PathMechanicFx() {
    }

    /** 元素编号（与 {@code SpellFxPacket.element} 的口径一致）。 */
    private static final int EL_METAL = 0;
    private static final int EL_WOOD = 1;
    private static final int EL_WATER = 2;
    private static final int EL_FIRE = 3;
    private static final int EL_EARTH = 4;
    private static final int EL_ICE = 5;
    private static final int EL_THUNDER = 6;
    private static final int EL_WIND = 7;

    // ------------------------------------------------------------------
    // 阵域：地面阵纹 + 边界
    // ------------------------------------------------------------------

    /**
     * 阵域每 tick 的持续表现（半径随剩余时间收缩，像"正在消耗"）。
     *
     * @param radius      当前半径（格）
     * @param remainFrac  剩余能量比例（0~1），用来让阵纹缓慢内收
     */
    public static void formationTick(ServerPlayer player, double radius, double remainFrac) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position().add(0.0D, 0.12D, 0.0D);
        // 主视觉：一圈立体阵纹（一个包就够，客户端画三维圆环）
        sendShape(level, at, SpellFxPacket.RUNE_RING, (float) radius, 20,
                EL_WOOD, 0.9F);
        // 边界再套一层会缓慢转动的环塔（"阵在运转"的感觉）
        sendShape(level, at, SpellFxPacket.TORUS, (float) radius * 1.02F, 18,
                EL_WOOD, 0.55F);
        // 粒子：一次发完，在环上撒（不循环）
        int want = (int) Math.round(10 * Math.min(1.0D, remainFrac + 0.2D));
        int n = SpellVfx.take(level, want);
        if (n > 0) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    at.x, at.y + 0.2D, at.z, n, radius * 0.9D, 0.15D, radius * 0.9D,
                    0.01D);
        }
    }

    /** 阵域展开的一次性表现。 */
    public static void formationOpen(ServerPlayer player, double radius) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position();
        sendShape(level, at, SpellFxPacket.SHOCK_RING, (float) radius, 12, EL_WOOD, 1.0F);
        sendShape(level, at, SpellFxPacket.RING_TOWER, (float) radius, 16, EL_WOOD, 1.0F);
        int n = SpellVfx.take(level, 26);
        if (n > 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    at.x, at.y + 0.4D, at.z, n, radius * 0.7D, 0.4D, radius * 0.7D, 0.02D);
        }
    }

    // ------------------------------------------------------------------
    // 血池：地面血色波纹 + 上升血雾
    // ------------------------------------------------------------------

    /**
     * 血池充能表现。
     *
     * @param frac 血池比例（0~1）—— 越满波纹越大、越密
     */
    /**
     * 法修·真元盾抵伤：一层淡青色护盾碎开（玩家要能看出"这次是蓝量顶的"）。
     *
     * <p>走 {@code SpellVfx.take(...)} 预算，不逐颗发包。</p>
     */
    public static void manaShield(net.minecraft.server.level.ServerPlayer player,
                                  double absorbed) {
        if (!(player.level() instanceof ServerLevel level) || absorbed <= 0.5D) {
            return;
        }
        Vec3 at = player.position().add(0.0D, 1.0D, 0.0D);
        // 护盾被顶碎：一圈水色波纹 + 少量碎屑（一个形状包 + 一次粒子，不逐颗发包）
        sendShape(level, at, SpellFxPacket.RING_TOWER,
                (float) (0.9D + Math.min(1.2D, absorbed * 0.02D)), 20,
                EL_WATER, 0.55F);
        int want = (int) Math.round(6 + Math.min(22.0D, absorbed));
        int n = SpellVfx.take(level, want);
        if (n > 0) {
            level.sendParticles(ParticleTypes.END_ROD,
                    at.x, at.y, at.z, n, 0.5D, 0.6D, 0.5D, 0.02D);
        }
    }

    public static void bloodPool(ServerPlayer player, double frac) {
        if (!(player.level() instanceof ServerLevel level) || frac <= 0.02D) {
            return;
        }
        // ⚠ 贴地：环压在脚踝高度（y+0.02），半径 ≤1.2 格。
        // 原来贴在 y+0.1、半径能到 2.4 格，第一人称像套了个圈挡住脚下。
        Vec3 at = player.position().add(0.0D, 0.02D, 0.0D);
        if (frac >= 0.6D) {
            sendShape(level, at, SpellFxPacket.TORUS, (float) (0.7D + frac * 0.5D), 16,
                    EL_FIRE, (float) (0.18D + frac * 0.17D));
        }
        int want = (int) Math.round(2 + frac * 5);
        int n = SpellVfx.take(level, want);
        if (n > 0) {
            // 血雾：只在脚边一小片，高度不超过膝盖
            level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    at.x, at.y + 0.22D, at.z, n, 0.45D, 0.18D, 0.45D, 0.01D);
        }
    }

    /** 血池回血时的一次性爆发。 */
    public static void bloodHeal(ServerPlayer player, double healed) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position();
        sendShape(level, at, SpellFxPacket.IMPACT_FLARE, 1.2F, 10, EL_FIRE, 0.85F);
        int n = SpellVfx.take(level, 14);
        if (n > 0) {
            level.sendParticles(ParticleTypes.HEART,
                    at.x, at.y + 1.0D, at.z, n, 0.5D, 0.4D, 0.5D, 0.0D);
        }
    }

    /** 缓伤结算时的提示（让"延迟的伤害"看得见）。 */
    public static void staggerTick(ServerPlayer player, double damage) {
        if (!(player.level() instanceof ServerLevel level) || damage < 0.5D) {
            return;
        }
        int n = SpellVfx.take(level, 4);
        if (n > 0) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    player.getX(), player.getY() + 1.1D, player.getZ(),
                    n, 0.25D, 0.2D, 0.25D, 0.0D);
        }
    }

    // ------------------------------------------------------------------
    // 激荡（体修）：引爆的冲击与减伤的金光
    // ------------------------------------------------------------------

    /**
     * 激荡引爆。
     *
     * @param golden {@code true} = 金身（减伤），{@code false} = 燃血（增伤）
     */
    public static void surgeBurst(ServerPlayer player, boolean golden) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position();
        int el = golden ? EL_METAL : EL_THUNDER;
        sendShape(level, at, SpellFxPacket.SHOCK_RING, 2.4F, 12, el, 1.0F);
        sendShape(level, at, SpellFxPacket.SHOCK_DOME, 2.0F, 10, el,
                golden ? 0.9F : 0.6F);
        if (golden) {
            // 金身：护体金光（一圈环 + 少量火花）
            sendShape(level, at, SpellFxPacket.TORUS, 1.1F, 16, EL_METAL, 0.95F);
            int n = SpellVfx.take(level, 16);
            if (n > 0) {
                level.sendParticles(ParticleTypes.END_ROD,
                        at.x, at.y + 1.0D, at.z, n, 0.55D, 0.6D, 0.55D, 0.03D);
            }
        } else {
            // 燃血：狂暴的雷火
            int n = SpellVfx.take(level, 24);
            if (n > 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        at.x, at.y + 1.0D, at.z, n, 0.7D, 0.8D, 0.7D, 0.12D);
            }
        }
    }

    /** 锁血（不灭）触发。 */
    public static void lastStand(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position();
        sendShape(level, at, SpellFxPacket.RING_TOWER, 1.6F, 14, EL_METAL, 1.0F);
        int n = SpellVfx.take(level, 18);
        if (n > 0) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    at.x, at.y + 1.2D, at.z, n, 0.7D, 0.9D, 0.7D, 0.06D);
        }
    }

    // ------------------------------------------------------------------
    // 魔气（魔修）：缠绕的暗紫
    // ------------------------------------------------------------------

    /**
     * 魔气积攒表现。
     *
     * @param frac 魔气比例（0~1）
     */
    public static void demonicAura(ServerPlayer player, double frac) {
        if (!(player.level() instanceof ServerLevel level) || frac <= 0.02D) {
            return;
        }
        // ⚠ 贴地：原来螺旋从脚下一直缠到头顶，正对视线。
        // 现在放在脚边（y+0.05），半径收到 0.85，亮度不超过 0.35。
        Vec3 at = player.position().add(0.0D, 0.05D, 0.0D);
        if (frac >= 0.5D) {
            sendShape(level, at, SpellFxPacket.HELIX, 0.85F, 18, EL_WATER,
                    (float) (0.18D + frac * 0.17D));
        }
        int want = (int) Math.round(2 + frac * 4);
        int n = SpellVfx.take(level, want);
        if (n > 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    at.x, at.y + 0.25D, at.z, n, 0.4D, 0.15D, 0.4D, 0.01D);
        }
    }

    /** 魔气满仓溢出（自伤）的警示。 */
    public static void demonicOverflow(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position();
        sendShape(level, at, SpellFxPacket.CRACK_VOID, 1.4F, 10, EL_WATER, 0.9F);
        int n = SpellVfx.take(level, 10);
        if (n > 0) {
            level.sendParticles(ParticleTypes.SQUID_INK,
                    at.x, at.y + 1.0D, at.z, n, 0.4D, 0.5D, 0.4D, 0.02D);
        }
    }

    // ------------------------------------------------------------------
    // 副业 / 洗点 / 转修 / 领域将尽
    // ------------------------------------------------------------------

    /**
     * <b>制作成功</b>：一记向上的光柱。
     *
     * <p>与失败的灰烟形成鲜明对比 —— 玩家不用看聊天栏就知道结果。
     * 颜色按副业区分（炼丹=火、炼器=金、阵法=木）。</p>
     */
    public static void craftSuccess(ServerPlayer player,
                                    com.hongmeng.xiuxian.talent.Profession p) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int el = switch (p) {
            case LIANDAN -> EL_FIRE;
            case LIANQI -> EL_METAL;
            case ZHENFA -> EL_WOOD;
        };
        Vec3 at = player.position();
        sendShape(level, at, SpellFxPacket.BEAM_COLUMN, 1.2F, 14, el, 0.9F);
        sendShape(level, at, SpellFxPacket.RUNE_RING, 1.1F, 12, el, 0.8F);
        int n = SpellVfx.take(level, 16);
        if (n > 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    at.x, at.y + 0.9D, at.z, n, 0.5D, 0.7D, 0.5D, 0.03D);
        }
    }

    /**
     * <b>制作失败</b>：一团下沉的灰烟。
     *
     * <p>刻意做得<b>蔫</b>（向下、灰白）—— 与成功的光柱形成直觉上的对比，
     * 玩家一眼能分辨结果。</p>
     */
    public static void craftFail(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position();
        int n = SpellVfx.take(level, 14);
        if (n > 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    at.x, at.y + 0.8D, at.z, n, 0.4D, 0.3D, 0.4D, 0.0D);
        }
    }

    /** <b>洗点</b>：一圈向内收拢的光环（念头被抹平）。 */
    public static void talentReset(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position();
        sendShape(level, at, SpellFxPacket.RUNE_RING, 2.2F, 16, EL_WATER, 0.95F);
        int n = SpellVfx.take(level, 18);
        if (n > 0) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    at.x, at.y + 1.2D, at.z, n, 0.9D, 0.8D, 0.9D, 0.0D);
        }
    }

    /** <b>转修</b>：地面上炸开的裂纹 + 环塔（破道重立）。 */
    public static void pathSwitch(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position();
        sendShape(level, at, SpellFxPacket.CRACK_VOID, 2.0F, 18, EL_WATER, 1.0F);
        sendShape(level, at, SpellFxPacket.RING_TOWER, 2.6F, 20, EL_WATER, 0.9F);
        int n = SpellVfx.take(level, 22);
        if (n > 0) {
            level.sendParticles(ParticleTypes.SOUL,
                    at.x, at.y + 0.8D, at.z, n, 0.9D, 1.0D, 0.9D, 0.02D);
        }
    }

    /**
     * <b>领域将尽</b>：让地面阵纹闪一下（每秒一次）。
     *
     * <p>只有一行小字的话，玩家往往在领域<b>突然消失</b>时才反应过来。</p>
     *
     * @param secondsLeft 剩余整秒数（越小闪得越明显）
     */
    public static void domainExpiring(ServerPlayer player, int secondsLeft) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position().add(0.0D, 0.12D, 0.0D);
        float power = Math.min(1.0F, 0.35F + (5 - Math.max(0, secondsLeft)) * 0.12F);
        sendShape(level, at, SpellFxPacket.SHOCK_RING, 3.2F, 8, EL_WOOD, power);
    }

    // ------------------------------------------------------------------
    // 新职业（R-1）：剑修 · 鬼修
    // ------------------------------------------------------------------

    /** <b>剑气</b>：前方一道弧形冲击环 + 割裂粒子（满层剑意释放）。 */
    public static void swordQi(ServerPlayer player, double radius) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 at = player.position().add(look.scale(radius * 0.5D)).add(0.0D, 0.9D, 0.0D);
        sendShape(level, at, SpellFxPacket.SHOCK_RING, (float) radius, 12, EL_METAL, 0.95F);
        sendShape(level, at, SpellFxPacket.IMPACT_FLARE, (float) (radius * 0.7D), 10,
                EL_WATER, 0.75F);
        int n = SpellVfx.take(level, 14);
        if (n > 0) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    at.x, at.y, at.z, n, 0.7D, 0.4D, 0.7D, 0.01D);
        }
    }

    /** <b>剑罡</b>（灵力抵伤）沿用真元盾表现，这里给剑修一个银青版本。 */
    public static void swordAura(ServerPlayer player, double absorbed) {
        if (!(player.level() instanceof ServerLevel level) || absorbed <= 0.5D) {
            return;
        }
        Vec3 at = player.position().add(0.0D, 1.0D, 0.0D);
        sendShape(level, at, SpellFxPacket.RING_TOWER,
                (float) (0.8D + Math.min(1.0D, absorbed * 0.02D)), 16, EL_METAL, 0.6F);
        int n = SpellVfx.take(level, (int) Math.round(4 + Math.min(16.0D, absorbed)));
        if (n > 0) {
            level.sendParticles(ParticleTypes.END_ROD,
                    at.x, at.y, at.z, n, 0.45D, 0.5D, 0.45D, 0.02D);
        }
    }

    /** <b>魂印</b>：目标身上升起幽色魂火（层数越高越亮）。 */
    public static void soulMark(ServerPlayer player, net.minecraft.world.entity.LivingEntity target,
                                int stacks) {
        if (!(player.level() instanceof ServerLevel level) || target == null) {
            return;
        }
        Vec3 at = target.position().add(0.0D, target.getBbHeight() * 0.8D, 0.0D);
        int n = SpellVfx.take(level, 3 + stacks * 2);
        if (n > 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    at.x, at.y, at.z, n, 0.25D, 0.35D, 0.25D, 0.01D);
        }
    }

    /** <b>魂爆</b>：印记引爆的小范围魂焰冲击。 */
    public static void soulBurst(ServerPlayer player,
                                 net.minecraft.world.entity.LivingEntity target) {
        if (!(player.level() instanceof ServerLevel level) || target == null) {
            return;
        }
        Vec3 at = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
        sendShape(level, at, SpellFxPacket.SHOCK_RING, 2.0F, 10, EL_WATER, 0.9F);
        sendShape(level, at, SpellFxPacket.IMPACT_FLARE, 1.2F, 8, EL_WATER, 0.8F);
        int n = SpellVfx.take(level, 18);
        if (n > 0) {
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    at.x, at.y, at.z, n, 0.4D, 0.4D, 0.4D, 0.02D);
        }
    }

    /** <b>虚化</b>：身体化虚的一圈幽光（无敌提示）。 */
    public static void phase(ServerPlayer player, int ticks) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = player.position().add(0.0D, 1.0D, 0.0D);
        sendShape(level, at, SpellFxPacket.SHOCK_DOME, 1.6F, 12, EL_WATER, 0.9F);
        sendShape(level, at, SpellFxPacket.RING_TOWER, 1.2F, 14, EL_WATER, 0.7F);
        int n = SpellVfx.take(level, 16);
        if (n > 0) {
            level.sendParticles(ParticleTypes.SOUL,
                    at.x, at.y, at.z, n, 0.5D, 0.5D, 0.5D, 0.01D);
        }
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    /** 发一个形状包（客户端画三维几何）。 */
    private static void sendShape(ServerLevel level, Vec3 at, byte shape, float size,
                                  int life, int element, float power) {
        try {
            com.hongmeng.xiuxian.network.ModNetwork.sendToAllAround(level, at,
                    new SpellFxPacket(shape, at, at, size, 0.0F, life, element, power));
        } catch (Throwable t) {
            HongmengMod.LOGGER.debug("[path-fx] 形状包发送失败：{}", t.toString());
        }
    }
}
