package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.network.ModNetwork;
import com.hongmeng.xiuxian.network.SpellFxPacket;
import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * <b>法术贴图特效的派发层</b>：一次施法要播哪些贴图，由这里决定。
 *
 * <p>玩家要求：「法术特效不仅仅粒子效果还有贴图表现……贴图配合粒子，
 * 不是纯贴图化。你像轨道炮就有一个激光柱那种的光波，四溢的能量粒子等，
 * <b>其他法术也要这种</b>」。</p>
 *
 * <h2>形状怎么挑（保证"每个法术都不一样"）</h2>
 * <p>先按 {@link TechniqueKind} 定"骨架形状"（光柱 / 冲击环 / 巨剑 / 陨石…），
 * 再用**法术名的哈希**做几件事：</p>
 * <ul>
 *   <li>决定**叠几层**、每层的尺寸倍率；</li>
 *   <li>决定**要不要额外加一圈符文环 / 花瓣旋涡 / 爆闪**（同名法术每次都一样，
 *       不同法术必然不同）；</li>
 *   <li>与 {@code SpellSignature} 的配色天然一致（都用 {@code Element} 的颜色）。</li>
 * </ul>
 */
public final class SpellFxRuntime {

    private SpellFxRuntime() {
    }

    /** 按法术的"骨架形状"播一次贴图特效。 */
    // ------------------------------------------------------------------
    // 时间结构：延迟调度 + 四段式
    // ------------------------------------------------------------------

    /** 一条待发的特效（{@code to} 与 {@code at} 相同 = 原地特技）。 */
    private record Pending(long dueTick, ServerLevel level, Vec3 at, Vec3 to,
                           byte shape, float size, float length, int life,
                           int element, float power) {
    }

    /** 待发队列（按 dueTick 排序靠插入时保持；量很小，线性扫足够）。 */
    private static final java.util.List<Pending> QUEUE = new java.util.ArrayList<>();

    /**
     * <b>延迟发送一条特效</b>（服务端）。
     *
     * <p>{@code SpellFxPacket} 没有"延迟"字段 —— 与其改协议（客户端也要跟着改、
     * 旧客户端还会读错），不如在服务端排队：到点再发原包，<b>客户端零改动</b>。</p>
     */
    public static void sendLater(ServerLevel level, int delayTicks, Vec3 at, byte shape,
                                 float size, float length, int life, int element,
                                 float power) {
        sendLaterTo(level, delayTicks, at, at, shape, size, length, life, element, power);
    }

    /** 带终点的延迟发送（轨迹类形状：陨石弧、横扫光柱）。 */
    public static void sendLaterTo(ServerLevel level, int delayTicks, Vec3 from, Vec3 to,
                                   byte shape, float size, float length, int life,
                                   int element, float power) {
        if (level == null || from == null || delayTicks < 0) {
            return;
        }
        long due = level.getGameTime() + delayTicks;
        if (QUEUE.size() > 256) {
            QUEUE.clear();   // 异常情况下的兜底，避免无限堆积
        }
        QUEUE.add(new Pending(due, level, from, to == null ? from : to, shape, size,
                length, life, element, power));
    }

    /** 每 tick 调用：把到点的特效发出去。 */
    public static void tickScheduled(long now) {
        if (QUEUE.isEmpty()) {
            return;
        }
        java.util.Iterator<Pending> it = QUEUE.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.dueTick() > now) {
                continue;
            }
            it.remove();
            try {
                if (p.to().distanceToSqr(p.at()) < 1.0E-4D) {
                    send(p.level(), p.at(), p.shape(), p.size(), p.length(),
                            p.life(), p.element(), p.power());
                } else {
                    sendTo(p.level(), p.at(), p.to(), p.shape(), p.size(), p.length(),
                            p.life(), p.element(), p.power());
                }
            } catch (Throwable ignored) {
                // 单条失败不能拖垮队列
            }
        }
    }

    /**
     * 换存档 / 停服清理。
     *
     * <p>PERF(B)/正确性：{@link #QUEUE} 里存着持有 {@code ServerLevel} 引用的延迟任务，
     * 不清的话同一个 JVM 换存档后，<b>旧世界的特效会按旧坐标发到新世界</b>，
     * 而且旧 ServerLevel 会被静态表拽住无法回收。</p>
     */
    public static void clear() {
        QUEUE.clear();
    }

    /**
     * <b>四段式演出</b>：蓄势 → 爆发 → 冲击 → 余波。
     *
     * <p>为什么要分四段：原来 {@code play()} 把所有层<b>同一时刻</b>发出 ——
     * 一放法术五层一起炸，看起来就是"一堆东西同时出现"，
     * 没有"起势 → 爆发 → 冲击 → 余波"的叙事。分四段之后，
     * 观众能读出发生了什么。</p>
     *
     * @param s 整体尺寸（与 {@code play()} 同口径）
     */
    public static void playFourStage(ServerLevel level, Vec3 at, Element element,
                                     float s, int identity) {
        if (level == null || at == null || element == null) {
            return;
        }
        int el = element.ordinal();
        float j = 1.0F + (identity % 3) * 0.08F;

        // 每段一个音：与视觉同时间点触发，音画才同步。
        // 用 RealWorldScheduler（通用延迟执行器，已在 ServerTick 里推进）。
        stageSound(level, at, 0, com.hongmeng.xiuxian.ModSounds.STAGE_CHARGE,
                0.75F, 1.0F);
        stageSound(level, at, 5, com.hongmeng.xiuxian.ModSounds.STAGE_BURST,
                1.35F, 1.0F);
        stageSound(level, at, 9, com.hongmeng.xiuxian.ModSounds.STAGE_BURST,
                0.85F, 1.35F);   // 冲击波：同一个音升高音高，像"第二波"
        stageSound(level, at, 22, com.hongmeng.xiuxian.ModSounds.STAGE_AFTERMATH,
                0.9F, 1.0F);

        // ① 蓄势（0 tick）：小而亮的核心（原 0+2 两层环，减量后只留核心）
        sendNow(level, at, SpellFxPacket.IMPACT_FLARE, s * 0.55F * j, 0.0F, 34, el, 1.9F);

        // ② 爆发（5 tick）：爆闪星 + 主形状（立体几何）
        sendLater(level, 5, at, SpellFxPacket.IMPACT_FLARE, s * 1.25F * j, 0.0F, 46, el, 2.0F);
        sendLater(level, 6, at, primaryShapeFor(identity, 3), s * 0.9F * j, 0.0F, 60, el, 1.6F);

        // ③ 冲击（12 tick）：大扩散环（原 12+14 两层，减量后只留环）
        sendLater(level, 12, at, SpellFxPacket.SHOCK_RING, s * 1.9F * j, 0.0F, 58, el, 1.2F);

        // ④ 余波（22 tick）：光柱（原 22+26 两层，减量后只留光柱）
        sendLater(level, 22, at, SpellFxPacket.BEAM_COLUMN, s * 0.8F * j, 2.4F, 60, el, 0.9F);
    }

    /**
     * <b>3D 蓄力法阵</b>：每 8 tick 在脚下补一圈立体几何（环体/环塔/双螺旋轮换）。
     *
     * <h2>为什么加这个（2026-11-01 第三轮）</h2>
     * <p>玩家要求「特效减一减 + 要 3D 特效」。粒子减到约 45% 之后，
     * 光靠粒子会显得"空"——这一层用 <b>3D 几何</b>补华丽度：
     * 环体有厚度、环塔有层次、双螺旋有缠绕感，而且全部落在<b>脚下</b>、
     * 不进视线中心（不遮脸）。服务端 1 包 / 8 tick，成本可以忽略。</p>
     *
     * @param progress 蓄力进度 0→1（决定法阵尺寸）
     * @param tick     当前 gameTime（用来轮换几何种类）
     */
    public static void chargeSigil(ServerLevel level, ServerPlayer player,
                                   Technique technique, double progress, int tick) {
        if (level == null || player == null || technique == null
                || technique.kind() == TechniqueKind.PASSIVE) {
            return;
        }
        SpellLook.Look look = SpellLook.of(technique);
        int el = technique.element().ordinal();
        double pr = Math.max(0.0D, Math.min(1.0D, progress));
        float size = (float) ((1.5D + 1.6D * look.sizeMul()) * (0.7D + 0.5D * pr));
        // 三种立体几何轮换（同一法术的组合由定妆表身份决定，不同法术不同）
        int step = Math.floorMod(look.identity() + tick / 8, 3);
        byte shape = switch (step) {
            case 0 -> SpellFxPacket.TORUS;
            case 1 -> SpellFxPacket.RING_TOWER;
            default -> SpellFxPacket.HELIX;
        };
        // 放在**身前 2.4 格**（不是脚下）：
        //   ① 第一人称看得见（脚下会被 isPersonalAura 判为"贴脚光环"跳过）；
        //   ② 在视野下方，不进视线中心 —— "华丽但不遮脸"。
        Vec3 dir = player.getLookAngle();
        Vec3 at = player.position().add(dir.x * 2.4D, 0.05D, dir.z * 2.4D);
        send(level, at, shape, size, 0.0F, 16, el, 0.8F);
    }

    /**
     * 在 {@code delayTicks} 之后播一个音（四段式配乐用）。
     *
     * <p>为什么要延迟：四段式的视觉是<b>按时间排的</b>，
     * 声音如果全部立刻播，就变成"一声混响"，听不出分段。</p>
     */
    private static void stageSound(ServerLevel level, Vec3 at, int delayTicks,
                                   net.minecraftforge.registries.RegistryObject<
                                           net.minecraft.sounds.SoundEvent> sound,
                                   float volume, float pitch) {
        if (level == null || at == null || sound == null) {
            return;
        }
        com.hongmeng.xiuxian.realworld.RealWorldScheduler.after(level, delayTicks,
                () -> {
                    try {
                        level.playSound(null, at.x, at.y, at.z, sound.get(),
                                net.minecraft.sounds.SoundSource.PLAYERS,
                                volume, pitch);
                    } catch (Throwable ignored) {
                        // 单个音失败不影响演出
                    }
                });
    }

    /** 立刻发一条（内部用，避免和 {@link #play} 的签名纠缠）。 */
    private static void sendNow(ServerLevel level, Vec3 at, byte shape, float size,
                                float length, int life, int element, float power) {
        try {
            send(level, at, shape, size, length, life, element, power);
        } catch (Throwable ignored) {
            // 单条失败忽略
        }
    }

    public static void play(ServerPlayer player, Technique technique, ServerLevel level,
                            Vec3 from, Vec3 to, double scale) {
        if (player == null || technique == null || level == null || from == null) {
            return;
        }
        int el = technique.element().ordinal();
        // ---- 视觉定妆表（52 门基础 + 20 门职业主动都有显式数据行；
        //      未登记的新法术自动退回名字哈希，行为与改造前一致）----
        SpellLook.Look look = SpellLook.of(technique);
        // 法术释放音：按元素选（自行合成，9 种属性各一个），音高按定妆表变调 ——
        // 同一元素的几十门法术不再是同一个音。
        playElementSound(level, from, technique.element(), look.pitch());

        // 粒子层：主环 + 内圈辅环（批量发包，不会逐颗发）
        // 2026-11-07 再下调：26/40 → 20 → **13/20 → 9**（粒子只做提示，立体几何撑主视觉）
        try {
            if (technique.kind() == TechniqueKind.ULTIMATE) {
                ElementParticles.grandBurst(level, technique.element(), from,
                        Math.max(0.6D, scale) * 0.9D, 13);
            } else {
                ElementParticles.burst(level, technique.element(), from,
                        Math.max(0.6D, scale) * 0.7D, 9);
            }
        } catch (Throwable ignored) {
            // 粒子失败不影响特效主体
        }
        float s = (float) Math.max(0.6D, scale);

        // ---- 法术的"视觉身份"（玩家：特效太单调，每个法术要不一样）----
        //
        // 以前这些维度全部由**名字哈希**派生 —— 想单独调一门就得改算法，
        // 等于全改；而且哈希不知道这是剑法还是冰牢。现在改为读
        // {@link SpellLook} 的显式数据行（改一门只动表里一行），
        // 未登记的法术仍由 SpellLook 内部哈希兜底，行为不变：
        //   * identity   0~5：决定主形状从候选里挑哪一个、追不追加签名层；
        //   * layers     2~5：总共铺几层；
        //   * sizeStep   0~2：整体尺寸档（0.75 / 1.0 / 1.35）；
        //   * phase      0~5：每层的相位偏置（同名法术每次一样，不同法术必然不同）。
        int identity = look.identity();
        int layers = look.layers();
        int sizeStep = look.sizeStep();
        int phase = look.phase();
        float sizeMul = look.sizeMul();
        float jitter = (0.85F + phase * 0.05F) * sizeMul;
        s = s * sizeMul;

        // ---- 骨架：按释放形态挑 ----
        // 灭世级 / 引力级：走**四段式**（蓄势 → 爆发 → 冲击 → 余波）。
        //
        // 下面那段"五层同时发"的老骨架保留：它负责"层次丰富度"，
        // 四段式负责"时间结构"。两者叠在一起 —— 既丰富又有节奏。
        if (technique.kind() == TechniqueKind.ULTIMATE
                || technique.kind() == TechniqueKind.GRAVITY) {
            playFourStage(level, from, technique.element(), s, identity);
        }

        // ---- 3D 签名层（第三轮减量：原 6~8 层 → 2 层）----
        //
        // 原来灭世级在这里一次塞：碎片群 + 地面裂纹 + 陨石轨迹 + 螺旋上升 +
        // 爬行裂纹 + 阵法 + 环绕 + 能量丝（叠上四段式与专属演出，单次施法 20+ 个形状，
        // "华丽"变成"糊墙"，第一人称根本看不清战场）。
        // 现在只保留最立体的两层：**爬行裂纹**（地面 3D 走势）+ **陨石轨迹**（空间运动），
        // 其余交给定妆表的专属演出按需补。
        try {
            Vec3 dest = to != null ? to : from;
            if (technique.kind() == TechniqueKind.ULTIMATE
                    || technique.kind() == TechniqueKind.GRAVITY) {
                send(level, from, SpellFxPacket.GROUND_CRACK,
                        s * 0.9F, 0.0F, 75, el, 0.7F);
                Vec3 sky = from.add(0.0D, Math.max(12.0D, s * 3.2D), 0.0D);
                sendTo(level, sky, dest, SpellFxPacket.METEOR_ARC,
                        s * 0.9F, 0.0F, 44, el, 1.0F);
            }
        } catch (Throwable ignored) {
            // 新形状失败不影响既有表现
        }

        switch (technique.kind()) {
            case ULTIMATE, GRAVITY -> {
                // 灭世级（第三轮减量）：爆闪 + 冲击环 + 主形状（立体几何）+ 签名形状（立体几何）。
                // 删掉原有的穹顶/符文环/额外符文/裂缝追加层 —— 四段式 + 专属演出已经够满，
                // 再多就是互相盖住。
                send(level, from, SpellFxPacket.IMPACT_FLARE,
                        s * 0.9F * jitter, 0.0F, 55 + phase * 4, el, 1.6F);
                send(level, from, SpellFxPacket.SHOCK_RING,
                        s * (1.0F + identity * 0.05F) * jitter, 0.0F,
                        70 + phase * 6, el, 1.2F);
                // 主形状：由纯函数挑（现在候选集全是**立体几何**，见 primaryShapeFor）
                send(level, from, primaryShapeFor(identity, layers),
                        s * 1.05F * jitter, 0.0F, 90, el, 1.4F);
                // 签名形状层：与主形状不同的另一层立体几何
                send(level, from, signatureShapeFor(identity, layers),
                        s * (0.8F + layers * 0.05F) * jitter, 0.0F,
                        85 + phase * 5, el, 1.0F);
                // 黑洞专属：额外一层穹顶（与万象寂灭拉开区别）
                if (technique == Technique.TAI_XU_HEI_DONG) {
                    send(level, from, SpellFxPacket.SHOCK_DOME, s * 1.1F, 0.0F, 110, el, 1.0F);
                }
            }
            case SLASH -> {
                // 斩击（减量）：巨剑 + 一层立体碎裂/裂缝，不再按 identity 叠 2~3 层
                send(level, from, SpellFxPacket.BLADE_GOLD,
                        s * (0.85F + identity * 0.05F) * jitter, 0.0F,
                        45 + phase * 3, el, 1.5F);
                if (identity % 2 == 0) {
                    send(level, from, SpellFxPacket.CRACK_VOID, s * 0.7F, 0.0F, 60, el, 0.9F);
                } else {
                    send(level, from, SpellFxPacket.SHARD_BURST, s * 0.6F, 0.0F, 50, el, 0.9F);
                }
            }
            case LINE, PROJECTILE, FAN -> {
                // 直线 / 弹道 / 扇形：光柱（玩家说的"激光柱那种光波"）
                //
                // ⚠ 旧版从 TechniqueCaster 传进来的是 aim, aim（同一点），而渲染端
                //    对 from==to 的光柱会直接跳过（beam() 里 len < 0.05 即 return）——
                //    也就是说这几十门法术的"激光柱"层其实从来没画出来过。
                //    现在退化时用施法者朝向补一个够长的终点，光柱才真正可见。
                Vec3 beamEnd = directionalEnd(from, to, player, s);
                byte beam = (identity % 2 == 0)
                        ? SpellFxPacket.BEAM_COLUMN : SpellFxPacket.BEAM_SOFT;
                sendBeam(level, from, beamEnd,
                        s * (0.45F + identity * 0.05F) * jitter, 55 + phase * 4,
                        el, 1.5F, beam);
                send(level, from, SpellFxPacket.IMPACT_FLARE,
                        s * (0.6F + identity * 0.05F) * jitter, 0.0F, 40, el, 1.3F);
                if (layers >= 4) {
                    sendBeam(level, from, beamEnd, s * 0.95F * jitter, 45, el, 0.7F,
                            SpellFxPacket.BEAM_SOFT);
                }
            }
            case AOE -> {
                // 范围爆发（减量 + 3D）：冲击环 + 一层**立体主形状**。
                // 原来按 identity 分 5 个平铺形状（穹顶/花瓣/裂缝/爆闪/天降柱）再叠穹顶/符文环，
                // 一次 3~5 层；现在主形状统一走 primaryShapeFor 的立体几何，只留 2 层骨架。
                send(level, from, SpellFxPacket.SHOCK_RING,
                        s * (1.0F + identity * 0.07F) * jitter, 0.0F,
                        70 + phase * 5, el, 1.1F);
                send(level, from, primaryShapeFor(identity, layers),
                        s * (0.9F + identity * 0.05F) * jitter, 0.0F,
                        72 + phase * 4, el, 1.1F);
            }
            case DASH -> {
                // 位移（减量 + 3D）：光幕 + 立体环绕残影（原来花瓣/雪花是朝向相机的平片）
                Vec3 dashEnd = directionalEnd(from, to, player, s * 0.6F);
                sendBeam(level, from, dashEnd, s * 0.5F, 22, el, 0.9F,
                        SpellFxPacket.BEAM_SOFT);
                send(level, from, SpellFxPacket.ORBIT, s * 0.55F * jitter, 0.0F,
                        26, el, 0.8F);
            }
            case HEAL, BUFF -> {
                // 辅助（减量 + 3D）：立体环体 + 天降柱；元素补充层只留一层
                send(level, from, SpellFxPacket.TORUS,
                        s * (0.8F + identity * 0.05F) * jitter, 0.0F,
                        85 + phase * 4, el, 0.9F);
                send(level, from, SpellFxPacket.PILLAR_DROP, s * 0.45F, 0.0F, 60, el, 0.7F);
                switch (technique.element()) {
                    case ICE, WATER -> send(level, from, SpellFxPacket.SNOW_FLAKE,
                            s * 0.45F, 0.0F, 70, el, 0.65F);
                    case FIRE -> send(level, from, SpellFxPacket.HELIX,
                            s * 0.5F, 0.0F, 60, el, 0.8F);
                    case WOOD -> send(level, from, SpellFxPacket.HELIX,
                            s * 0.5F, 0.0F, 60, el, 0.7F);
                    case THUNDER -> send(level, from, SpellFxPacket.LATTICE,
                            s * 0.6F, 0.0F, 55, el, 0.8F);
                    default -> send(level, from, SpellFxPacket.RING_TOWER,
                            s * 0.5F, 0.0F, 65, el, 0.7F);
                }
            }
            default -> {
                // 兜底形态（减量 + 3D）：爆闪 + 一层立体几何
                send(level, from, SpellFxPacket.IMPACT_FLARE,
                        s * (0.7F + identity * 0.06F) * jitter, 0.0F, 45, el, 1.1F);
                send(level, from, SpellFxPacket.TORUS, s * 0.8F, 0.0F, 55, el, 0.85F);
            }
        }

        // ---- 元素几何签名层（**普通法术也有自己的立体几何**）----
        //
        // 玩家说过「特效不能只是贴图，要立体」。灭世级已经有主形状 + 签名形状两层，
        // 但**普通法术**到这一步为止还只有平面感的环/爆闪。
        //
        // 所以这里再叠一层**按元素决定的三维几何**：
        // 木系长螺旋、金系生晶格、土系压环塔……同一形态下的不同元素
        // 从此不只是换色，**连多出来的那块几何都不一样**。
        switch (technique.kind()) {
            case ULTIMATE, GRAVITY -> {
                // 灭世级已经有签名形状层了，这里不重复加
            }
            default -> {
                byte geo = elementGeometryFor(technique.element(), layers);
                if (geo >= 0) {
                    send(level, from, geo, s * 0.9F * jitter, 0.0F,
                            60 + phase * 3, el, 0.95F);
                }
            }
        }

        // ---- 星陨专属：追加一颗从天外飞来的燃烧陨石 ----
        if (technique == Technique.TIAN_YUN_XING_LUO) {
            send(level, from, SpellFxPacket.METEOR_ROCK, s * 1.4F, 0.0F, 90, el, 1.5F);
        }

        // ---- 专属演出：每门法术的最后一块"签名层" ----
        //
        // 骨架负责"这个形态该有的样子"，专属演出负责"这门法术自己的叙事"：
        // 冰牢要看到冰晶笼、雷牢要看到电弧立柱、火海要看到一圈火柱、
        // 天陨要看到陨石坠落 + 爬行裂纹……全部由 {@link SpellLook} 逐门指派。
        if (look.setpiece() != SpellLook.Setpiece.NONE
                && technique.kind() != TechniqueKind.PASSIVE) {
            playSetpiece(level, player, technique, look, from, to, s, el);
        }
    }

    // ------------------------------------------------------------------
    // 专属演出（每门法术的签名层）
    // ------------------------------------------------------------------

    /**
     * 方向型特效的终点兜底：{@code from} 与 {@code to} 重合时，
     * 用施法者朝向补一个"够长"的终点 —— 否则渲染端的 beam 会因长度为 0 直接跳过。
     */
    private static Vec3 directionalEnd(Vec3 from, Vec3 to, ServerPlayer player, float s) {
        if (to != null && from.distanceToSqr(to) > 1.0E-4D) {
            return to;
        }
        Vec3 dir = player != null ? player.getLookAngle()
                : new Vec3(0.0D, 0.0D, 1.0D);
        double len = Math.max(10.0D, (double) s * 3.0D);
        return from.add(dir.normalize().scale(len));
    }

    /** 绕 Y 轴旋转一个方向向量。 */
    private static Vec3 rotateY(Vec3 v, double ang) {
        double c = Math.cos(ang);
        double sn = Math.sin(ang);
        return new Vec3(v.x * c + v.z * sn, v.y, -v.x * sn + v.z * c);
    }

    /** 按定妆表派发专属演出。 */
    private static void playSetpiece(ServerLevel level, ServerPlayer player, Technique t,
                                     SpellLook.Look look, Vec3 from, Vec3 to,
                                     float s, int el) {
        Vec3 dir = player != null ? player.getLookAngle().normalize()
                : new Vec3(0.0D, 0.0D, 1.0D);
        switch (look.setpiece()) {
            case PIERCE_LINE -> pierceLine(level, from, to, dir, s, el);
            case SWORD_FAN -> {
                // 玩家 2026-11-08：剑气/飞剑只在主手拿着武器时显示。
                if (player == null || holdingWeapon(player)) {
                    swordFan(level, from, to, dir, s, el);
                }
            }
            case FROST_PRISON -> frostPrison(level, from, s, el);
            case THUNDER_CAGE -> thunderCage(level, from, s, el);
            case FLAME_STORM -> flameStorm(level, from, s, el);
            case QUAKE -> quake(level, from, s, el);
            case SOUL_DRAIN -> soulDrain(level, from, s, el);
            case BLOOD_POOL -> bloodPool(level, from, s, el);
            case AFTERIMAGE -> afterimage(level, from, to, dir, s, el);
            case GUARD_AURA -> guardAura(level, from, s, el);
            case HEAL_BLOOM -> healBloom(level, from, s, el);
            case VOID_SINGULARITY -> voidSingularity(level, from, s, el);
            case SPIRIT_BURST -> spiritBurst(level, from, s, el);
            case STAR_FALL -> starFall(level, from, s, el);
            case MOON_SLASH -> moonSlash(level, from, dir, s, el);
            case RAILGUN -> railgun(level, player, from, s, el);
            case GOLD_DOMAIN -> goldDomain(level, from, s, el);
            case VINE_BIND -> vineBind(level, from, s, el);
            default -> {
                // NONE：没有专属演出
            }
        }
    }

    /** 贯穿：细光柱 + 柔光幕 + 末端爆闪 + 能量丝（金刚指 / 御剑斩 / 锁魂链）。 */
    private static void pierceLine(ServerLevel level, Vec3 from, Vec3 to, Vec3 dir,
                                   float s, int el) {
        Vec3 a = from.add(0.0D, 1.0D, 0.0D);
        Vec3 d = (to != null && from.distanceToSqr(to) > 1.0E-4D)
                ? to.subtract(from).normalize() : dir;
        Vec3 far = a.add(d.scale(Math.max(14.0D, (double) s * 3.0D)));
        // 两道圆柱光柱（3D）+ 末端爆闪 + 能量丝；去掉原来第 5 层的延迟冲击环
        sendBeam(level, a, far, s * 0.22F, 42, el, 1.3F, SpellFxPacket.BEAM_COLUMN);
        sendBeam(level, a, far, s * 0.45F, 48, el, 0.55F, SpellFxPacket.BEAM_SOFT);
        send(level, far, SpellFxPacket.IMPACT_FLARE, s * 0.70F, 0.0F, 42, el, 1.3F);
        send(level, from, SpellFxPacket.FILAMENTS, s * 0.45F, 0.0F, 44, el, 0.6F);
    }

    /** 剑雨：5 把飞剑沿扇形出去（万剑归宗 / 风刃斩 / 剑气斩）（减量：7 → 5 把）。 */
    private static void swordFan(ServerLevel level, Vec3 from, Vec3 to, Vec3 dir,                                 float s, int el) {
        Vec3 a = from.add(0.0D, 1.1D, 0.0D);
        Vec3 d = (to != null && from.distanceToSqr(to) > 1.0E-4D)
                ? to.subtract(from).normalize()
                : (dir.lengthSqr() > 1.0E-6D ? dir : new Vec3(0.0D, 0.0D, 1.0D));
        // 金属→长剑，风→短刃；其余按长剑
        byte sword = el == Element.WIND.ordinal()
                ? SpellFxPacket.FLYING_DAGGER : SpellFxPacket.FLYING_SWORD;
        double len = Math.max(12.0D, (double) s * 2.8D);
        for (int i = -2; i <= 2; i++) {
            Vec3 side = rotateY(d, Math.toRadians(i * 11.0D));
            sendTo(level, a, a.add(side.scale(len)), sword,
                    s * 0.55F, 0.0F, 40 + Math.abs(i) * 2, el, 1.1F);
        }
        send(level, a, SpellFxPacket.IMPACT_FLARE, s * 0.75F, 0.0F, 40, el, 1.1F);
    }

    /** 冰牢：冰晶笼 + 晶格 + 延迟环体（减量：原 6 层 → 3 层，全 3D）。 */
    private static void frostPrison(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.FROST_CAGE, s * 1.05F, 0.0F, 70, el, 1.0F);
        send(level, from, SpellFxPacket.LATTICE, s * 1.0F, 0.0F, 60, el, 0.85F);
        sendLater(level, 6, from, SpellFxPacket.TORUS, s * 0.75F, 0.0F, 60, el, 0.65F);
    }

    /** 雷牢：立体晶格 + 两根落雷光柱 + 爆闪（减量：3 根 → 2 根）。 */
    private static void thunderCage(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.LATTICE, s * 1.05F, 0.0F, 60, el, 0.9F);
        for (int i = 0; i < 2; i++) {
            double ang = Math.PI * 2.0D * i / 2.0D + 0.4D;
            Vec3 at = from.add(Math.cos(ang) * (double) s * 0.55D, 0.0D,
                    Math.sin(ang) * (double) s * 0.55D);
            sendLater(level, i * 3, at, SpellFxPacket.PILLAR_DROP,
                    s * 0.30F, 0.0F, 45, el, 1.0F);
        }
        send(level, from, SpellFxPacket.IMPACT_FLARE, s * 1.0F, 0.0F, 40, el, 1.4F);
    }

    /** 火海：四根火柱依次升起 + 地面裂纹（减量：6 根 → 4 根，柱子本身是 3D 圆柱）。 */
    private static void flameStorm(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.SHOCK_RING, s * 1.05F, 0.0F, 70, el, 0.95F);
        for (int i = 0; i < 4; i++) {
            double ang = Math.PI * 2.0D * i / 4.0D;
            Vec3 at = from.add(Math.cos(ang) * (double) s * 0.6D, 0.0D,
                    Math.sin(ang) * (double) s * 0.6D);
            sendLater(level, 2 + i * 3, at, SpellFxPacket.PILLAR_DROP,
                    s * 0.45F, 0.0F, 50, el, 0.9F);
        }
        send(level, from, SpellFxPacket.GROUND_CRACK, s * 0.8F, 0.0F, 80, el, 0.7F);
    }

    /** 震地：地面裂纹 + 爬行裂纹 + 碎片 + 外扩冲击（减量：删掉环塔层）。 */
    private static void quake(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.GROUND_CRACK, s * 1.1F, 0.0F, 90, el, 0.9F);
        send(level, from, SpellFxPacket.CREEPING_CRACK, s * 0.95F, 0.0F, 90, el, 0.8F);
        send(level, from, SpellFxPacket.SHARD_BURST, s * 0.85F, 0.0F, 55, el, 1.0F);
        sendLater(level, 5, from, SpellFxPacket.SHOCK_RING, s * 1.1F, 0.0F, 55, el, 0.9F);
    }

    /** 摄魂：双螺旋 + 环绕 + 收束环（减量：删掉虚空裂缝平片层）。 */
    private static void soulDrain(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.HELIX, s * 1.0F, 0.0F, 62, el, 0.9F);
        send(level, from, SpellFxPacket.ORBIT, s * 0.75F, 0.0F, 55, el, 0.7F);
        sendLater(level, 4, from, SpellFxPacket.SHOCK_RING, s * 0.85F, 0.0F, 50, el, 0.7F);
    }

    /** 血池：血符法阵 + 双螺旋 + 血环（减量：删掉地面裂纹与环绕两层）。 */
    private static void bloodPool(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.BLOOD_SIGIL, s * 1.0F, 0.0F, 95, el, 1.0F);
        send(level, from, SpellFxPacket.HELIX, s * 0.8F, 0.0F, 65, el, 0.8F);
        sendLater(level, 6, from, SpellFxPacket.SHOCK_RING, s * 0.95F, 0.0F, 60, el, 0.7F);
    }

    /** 残影：光幕 + 落点环绕（减量：花瓣平片 → 3D 环绕光球）。 */
    private static void afterimage(ServerLevel level, Vec3 from, Vec3 to, Vec3 dir,
                                   float s, int el) {
        Vec3 a = from.add(0.0D, 1.0D, 0.0D);
        Vec3 end = directionalEnd(from, to, null, s);
        sendBeam(level, a, end.add(0.0D, 1.0D, 0.0D), s * 0.4F, 30, el, 0.8F,
                SpellFxPacket.BEAM_SOFT);
        send(level, to == null ? from : to, SpellFxPacket.ORBIT,
                s * 0.55F, 0.0F, 40, el, 0.7F);
    }

    /** 护体：环塔 + 晶格 + 护罩（减量：删掉符文平片，符文位置换成延迟环体）。 */
    private static void guardAura(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.RING_TOWER, s * 0.8F, 0.0F, 80, el, 0.8F);
        send(level, from, SpellFxPacket.LATTICE, s * 0.7F, 0.0F, 70, el, 0.7F);
        sendLater(level, 6, from, SpellFxPacket.TORUS, s * 0.75F, 0.0F, 60, el, 0.6F);
    }

    /** 回春：螺旋上升 + 环体 + 天降光柱（减量：花瓣/符文平片换成 3D 几何）。 */
    private static void healBloom(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.SPIRAL_ASCENT, s * 0.85F, 0.0F, 65, el, 0.8F);
        send(level, from, SpellFxPacket.TORUS, s * 0.8F, 0.0F, 85, el, 0.8F);
        sendLater(level, 8, from, SpellFxPacket.PILLAR_DROP, s * 0.4F, 0.0F, 60, el, 0.6F);
    }

    /** 奇点：圆环体 + 双螺旋 + 阵法（3 层立体；删掉裂缝/环绕/符文平片）。 */
    private static void voidSingularity(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.TORUS, s * 1.0F, 0.0F, 90, el, 0.9F);
        send(level, from, SpellFxPacket.HELIX, s * 0.85F, 0.0F, 80, el, 0.8F);
        sendLater(level, 3, from, SpellFxPacket.MANDALA, s * 1.1F, 0.0F, 100, el, 0.75F);
    }

    /** 神识：螺旋上升 + 环体 + 裂隙（减量：删掉符文平片与延迟环）。 */
    private static void spiritBurst(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.SPIRAL_ASCENT, s * 0.85F, 0.0F, 65, el, 0.85F);
        send(level, from, SpellFxPacket.TORUS, s * 0.8F, 0.0F, 85, el, 0.75F);
        send(level, from, SpellFxPacket.CRACK_VOID, s * 0.55F, 0.0F, 50, el, 0.7F);
    }

    /** 天陨：陨石轨迹 + 陨石 + 爬行裂纹（减量：删掉碎片与延迟冲击环）。 */
    private static void starFall(ServerLevel level, Vec3 from, float s, int el) {
        Vec3 sky = from.add(0.0D, Math.max(18.0D, (double) s * 4.0D), 0.0D);
        sendTo(level, sky, from, SpellFxPacket.METEOR_ARC, s * 1.1F, 0.0F, 46, el, 1.2F);
        send(level, from, SpellFxPacket.METEOR_ROCK, s * 1.2F, 0.0F, 80, el, 1.4F);
        send(level, from, SpellFxPacket.CREEPING_CRACK, s * 0.95F, 0.0F, 90, el, 0.85F);
    }

    /** 斩星：金色巨剑 + 横扫光柱 + 碎片（减量：删掉裂缝平片）。 */
    private static void moonSlash(ServerLevel level, Vec3 from, Vec3 dir, float s, int el) {
        Vec3 a = from.add(0.0D, 1.0D, 0.0D);
        send(level, from, SpellFxPacket.BLADE_GOLD, s * 1.15F, 0.0F, 60, el, 1.5F);
        sendBeam(level, a.subtract(dir.scale((double) s * 2.0D)),
                a.add(dir.scale((double) s * 2.0D)), s * 0.45F, 40, el, 1.1F,
                SpellFxPacket.BEAM_SWEEP);
        send(level, from, SpellFxPacket.SHARD_BURST, s * 0.85F, 0.0F, 55, el, 1.0F);
    }

    /** 轨道炮：天顶贯穿光柱 + 扫过光柱 + 爆闪（减量：删掉碎片层）。 */
    private static void railgun(ServerLevel level, ServerPlayer player, Vec3 from,
                                float s, int el) {
        Vec3 p0 = player != null ? player.position().add(0.0D, 1.2D, 0.0D) : from;
        sendBeam(level, p0, from, s * 0.35F, 44, el, 1.3F, SpellFxPacket.BEAM_COLUMN);
        send(level, from, SpellFxPacket.BEAM_SWEEP, s * 0.75F, 0.0F, 50, el, 0.95F);
        send(level, from, SpellFxPacket.IMPACT_FLARE, s * 0.95F, 0.0F, 45, el, 1.3F);
    }

    /** 剑域：5 把飞剑悬停成阵 + 晶格 + 环塔（减量：8 → 5 把，删符文平片）。 */
    private static void goldDomain(ServerLevel level, Vec3 from, float s, int el) {
        for (int i = 0; i < 5; i++) {
            double ang = Math.PI * 2.0D * i / 5.0D;
            Vec3 at = from.add(Math.cos(ang) * (double) s * 0.9D, 1.2D,
                    Math.sin(ang) * (double) s * 0.9D);
            // from==to：渲染端按"悬停自转"处理（有终点才朝运动方向）
            sendTo(level, at, at, SpellFxPacket.FLYING_SWORD,
                    s * 0.5F, 0.0F, 90, el, 1.0F);
        }
        send(level, from, SpellFxPacket.LATTICE, s * 0.85F, 0.0F, 70, el, 0.8F);
        send(level, from, SpellFxPacket.RING_TOWER, s * 0.85F, 0.0F, 80, el, 0.8F);
    }

    /** 缚藤：双螺旋 + 环塔 + 地面裂纹（减量：删掉花瓣平片层）。 */
    private static void vineBind(ServerLevel level, Vec3 from, float s, int el) {
        send(level, from, SpellFxPacket.HELIX, s * 0.9F, 0.0F, 60, el, 0.85F);
        send(level, from, SpellFxPacket.RING_TOWER, s * 0.8F, 0.0F, 70, el, 0.8F);
        sendLater(level, 4, from, SpellFxPacket.GROUND_CRACK, s * 0.7F, 0.0F, 80, el, 0.6F);
    }

    /**
     * <b>九种元素的"立体几何签名"（纯函数，可在服务端断言）</b>。
     *
     * <h2>为什么要有这一层</h2>
     * <p>到这一步为止，**普通法术**只有环 / 爆闪 / 光柱这些平面感强的层。
     * 玩家要的"立体"只在灭世级与几个专属法术上体现。
     * 这一层让<b>每个元素都多出一块自己专属的三维几何</b>：</p>
     *
     * <table border="1">
     *   <caption>元素 → 几何</caption>
     *   <tr><th>元素</th><th>几何</th><th>为什么</th></tr>
     *   <tr><td>木</td><td>{@code HELIX} 双螺旋</td><td>藤蔓缠绕、生生不息</td></tr>
     *   <tr><td>金</td><td>{@code LATTICE} 立体晶格</td><td>锋锐、切割、剑阵</td></tr>
     *   <tr><td>土</td><td>{@code RING_TOWER} 层叠环塔</td><td>层岩堆叠、厚重下沉</td></tr>
     *   <tr><td>雷</td><td>{@code LATTICE} 立体晶格</td><td>电网、雷牢</td></tr>
     *   <tr><td>水 / 冰</td><td>{@code TORUS} 圆环体</td><td>水环、冰环</td></tr>
     *   <tr><td>火</td><td>{@code TORUS} 圆环体（层数多时）</td><td>火环，层数够才有</td></tr>
     *   <tr><td>风 / 无</td><td>不加</td><td>这两个本来就靠粒子表现，加几何反而糊</td></tr>
     * </table>
     *
     * <p>返回 {@code -1} 表示"这个元素不加几何签名层"。</p>
     */
    public static byte elementGeometryFor(Element element, int layers) {
        if (element == null) {
            return -1;
        }
        return switch (element) {
            case WOOD -> SpellFxPacket.HELIX;
            case METAL, THUNDER -> SpellFxPacket.LATTICE;
            case EARTH -> SpellFxPacket.RING_TOWER;
            case WATER, ICE -> SpellFxPacket.TORUS;
            // 火：只有层数够多时才加（否则满屏都是环，糊）
            case FIRE -> layers >= 4 ? SpellFxPacket.TORUS : (byte) -1;
            default -> -1;
        };
    }

    /**
     * <b>法术的"主形状"选择（纯函数，可在服务端断言）</b>。
     *
     * <h2>为什么抽成纯函数</h2>
     * <p>形状以前是在 send 的地方写一长串 switch 挑的 —— 那样：
     * 两端（服务端选、客户端画）容易不一致，而且**没法在服务端验证**。</p>
     *
     * <p>现在形状由这个纯函数决定，**服务端算好随包发出**，客户端只是按 ID 画 ——
     * 构造上不可能不一致；同时这条映射可以被 {@code LogicVerifier} 直接断言。</p>
     *
     * <h2>2026-11-01 第三轮：候选集全部换成<b>立体几何</b></h2>
     * <p>玩家要求「要 3D 特效」。旧候选集里 PILLAR_DROP / SHOCK_DOME /
     * CRACK_VOID / PETAL_SWIRL 里有两层是"朝向相机的平片"，远看像贴纸。
     * 现在 6 个候选<b>全是真几何</b>：</p>
     * <pre>
     *   identity 0 → TORUS         有厚度的圆环体（还会自转）
     *   identity 1 → RING_TOWER    层叠环塔（半径递减的环堆）
     *   identity 2 → LATTICE       立体晶格（三轴棒条笼）
     *   identity 3 → HELIX         双螺旋（两条缠绕的管带）
     *   identity 4 → SHARD_BURST   碎片群（多面体碎片带翻滚+重力）
     *   identity 5 → SPIRAL_ASCENT 螺旋上升（光球沿螺旋升空）
     *   layers ≥ 5 时额外偏移一位    → 让"层数更多"的法术主形状也不同
     * </pre>
     */
    public static byte primaryShapeFor(int identity, int layers) {
        // layers 越多，主形状往前挪一位（同一 identity 的功法也能因层数不同而不同）
        int idx = Math.floorMod(identity + (layers >= 5 ? 1 : 0), 6);
        return switch (idx) {
            case 0 -> SpellFxPacket.TORUS;
            case 1 -> SpellFxPacket.RING_TOWER;
            case 2 -> SpellFxPacket.LATTICE;
            case 3 -> SpellFxPacket.HELIX;
            case 4 -> SpellFxPacket.SHARD_BURST;
            default -> SpellFxPacket.SPIRAL_ASCENT;
        };
    }

    /**
     * <b>灭世级法术的"签名形状"</b>（纯函数）：由 identity 与 layers 决定。
     *
     * <h2>⚠ 第一版这里是错的</h2>
     * <p>第一版写成 {@code idx = floorMod(identity * 2 + layers, 4)}
     * 再在 {HELIX, LATTICE, TORUS, RING_TOWER} 里挑 —— 但 TORUS 与 RING_TOWER
     * <b>也在主形状的候选集里</b>，于是 24 组组合里有 <b>3 组签名 == 主形状</b>，
     * 那一层几何就白加了（等于把同一个形状画两遍）。</p>
     *
     * <p>是 {@code LogicVerifier} 的断言抓出来的（"签名形状层永远不等于主形状"）。</p>
     *
     * <h2>正确做法：两个空间不相交</h2>
     * <p>主形状只用 {TORUS, RING_TOWER, LATTICE, HELIX, SHARD_BURST,
     * SPIRAL_ASCENT}；签名形状只在 <b>{SHOCK_DOME, PILLAR_DROP}</b> 里挑
     * （穹顶是球壳、光柱是圆柱，都是<b>体积几何</b>，不是朝向相机的平片）。
     * 两个集合没有任何交集，所以<b>构造上</b>不可能相同。</p>
     */
    public static byte signatureShapeFor(int identity, int layers) {
        // 只在 {SHOCK_DOME, PILLAR_DROP} 里挑 —— 与主形状候选集不相交，且都是立体几何
        return (Math.floorMod(identity + layers, 2) == 0)
                ? SpellFxPacket.SHOCK_DOME : SpellFxPacket.PILLAR_DROP;
    }

    /**
     * 按<b>元素</b>播放施法音效。
     *
     * <p>九个属性各有自己的合成音 —— 玩家闭眼也能听出"这是火还是冰"。
     * 音高由视觉定妆表逐门调（{@link SpellLook#pitch()}），所以同元素的
     * 几十门法术在听觉上也不再完全一样。</p>
     */
    private static void playElementSound(ServerLevel level, Vec3 at,
                                         Element element, float pitch) {
        if (level == null || at == null || element == null) {
            return;
        }
        float p = Math.max(0.8F, Math.min(1.3F, pitch));
        try {
            var sound = switch (element) {
                case METAL -> com.hongmeng.xiuxian.ModSounds.CAST_METAL;
                case WOOD -> com.hongmeng.xiuxian.ModSounds.CAST_WOOD;
                case WATER -> com.hongmeng.xiuxian.ModSounds.CAST_WATER;
                case FIRE -> com.hongmeng.xiuxian.ModSounds.CAST_FIRE;
                case EARTH -> com.hongmeng.xiuxian.ModSounds.CAST_EARTH;
                case ICE -> com.hongmeng.xiuxian.ModSounds.CAST_ICE;
                case THUNDER -> com.hongmeng.xiuxian.ModSounds.CAST_THUNDER;
                case WIND -> com.hongmeng.xiuxian.ModSounds.CAST_WIND;
                case NONE -> com.hongmeng.xiuxian.ModSounds.CAST_NONE;
            };
            level.playSound(null, at.x, at.y, at.z, sound.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, p);
        } catch (Throwable ignored) {
            // 音效失败不影响法术
        }
    }

    /**
     * 带<b>终点</b>的发送：运动轨迹类形状（陨石、横扫）靠 from/to 两个点插值。
     *
     * <p>旧的 {@code send} 把 from 和 to 设成同一点 —— 所以所有形状都只能"在原地"。 </p>
     */
    private static void sendTo(ServerLevel level, Vec3 from, Vec3 to, byte shape,
                               float size, float length, int life, int element,
                               float power) {
        ModNetwork.sendToAllAround(level, from, new SpellFxPacket(shape, from, to,
                size, length, life, element, power));
    }

    private static void send(ServerLevel level, Vec3 at, byte shape, float size,
                             float length, int life, int element, float power) {
        send(level, at, shape, size, length, life, element, power, false);
    }

    /**
     * @param noCamera true = 只画特效，客户端不触发镜头震动/FOV（Boss 转阶段用）
     */
    private static void send(ServerLevel level, Vec3 at, byte shape, float size,
                             float length, int life, int element, float power,
                             boolean noCamera) {
        // 光柱 / 天降柱类形状：渲染端要求 from≠to 才有长度（beam() 里 len<0.05 直接跳过）。
        // 以前这里 from=to，于是四段式余波、护体光柱、Boss 落雷柱全都没画出来。
        // 现在给这几类形状一个"竖直向上"的终点；长度参数优先，否则按尺寸给。
        Vec3 end = at;
        if (shape == SpellFxPacket.BEAM_COLUMN || shape == SpellFxPacket.BEAM_SOFT
                || shape == SpellFxPacket.PILLAR_DROP) {
            double h = length > 0.05F ? (double) length * 4.0D
                    : Math.max(4.0D, (double) size * 1.8D);
            end = at.add(0.0D, h, 0.0D);
        }
        ModNetwork.sendToAllAround(level, at, new SpellFxPacket(shape, at, end, size,
                length, life, element, power, noCamera));
    }

    private static void sendBeam(ServerLevel level, Vec3 from, Vec3 to, float size,
                                 int life, int element, float power, byte shape) {
        ModNetwork.sendToAllAround(level, from, new SpellFxPacket(shape, from, to, size,
                0.0F, life, element, power));
    }

    /**
     * <b>Boss 阶段切换的爆发特效</b>（玩家要求：boss 特效也要对标法术特效）。
     *
     * <p>比法术更重：爆闪 + 冲击环 + 半球气浪 + 虚空裂缝 + 天降光柱五层同时上，
     * 阶段越高强度越大 —— 这就是玩家要的压迫感。</p>
     *
     * @param power 1.2（二阶段）或 1.8（三阶段）
     */
    public static void playPhase(ServerLevel level, Vec3 at, Element element, float power) {
        if (level == null || at == null) {
            return;
        }
        int el = (element == null ? Element.NONE : element).ordinal();
        // 转阶段五层爆发：只保留特效，不推镜头（noCamera=true）。
        send(level, at, SpellFxPacket.IMPACT_FLARE, 14.0F * power, 0.0F, 45, el, power, true);
        send(level, at, SpellFxPacket.SHOCK_RING, 22.0F * power, 0.0F, 70, el, power, true);
        send(level, at, SpellFxPacket.SHOCK_DOME, 16.0F * power, 0.0F, 60, el, power * 0.8F, true);
        send(level, at, SpellFxPacket.CRACK_VOID, 9.0F * power, 0.0F, 90, el, power * 0.9F, true);
        send(level, at.add(0.0D, 26.0D, 0.0D), SpellFxPacket.BEAM_COLUMN,
                4.0F * power, 0.0F, 50, el, power, true);
    }

    /** 主手是否拿着武器；剑气/飞剑特效只在持械时生成。 */
    private static boolean holdingWeapon(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof net.minecraft.world.item.SwordItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.CareerWeaponItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.ModPathWeapon.PathWeaponItem) {
            return true;
        }
        if (stack.getItem() instanceof com.hongmeng.xiuxian.item.gear.RealmWeaponItem) {
            return true;
        }
        return com.hongmeng.xiuxian.item.gear.WeaponAttributeHandler.attackOf(stack) > 0.0D;
    }
}
