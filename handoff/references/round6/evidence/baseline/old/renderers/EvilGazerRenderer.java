package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.HongmengMod;
import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.EvilGazerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>「恶」的渲染器</b>：按阶段切换<b>三套独立骨架</b>，并在切模型时做"从上方压入 + 缩放淡入"的过渡。
 *
 * <h2>三套骨架（类名/LAYER 不变，内容由模型组 A 重做）</h2>
 * <ul>
 *   <li>一阶段 {@link EvilArmModel}：从天空裂口垂下的一条巨臂；</li>
 *   <li>二阶段 {@link EvilHeadModel}：裂口中的巨颅 + 再一条臂；</li>
 *   <li>三阶段 {@link EvilUpperModel}：挤满天空的整个上半身。</li>
 * </ul>
 *
 * <h2>阶段从哪读（同步口径）</h2>
 * <p>优先读 {@link CultivationMonsterEntity#bossPhase()} —— 这是 {@code BossPhase} 服务端写、
 * 客户端读的<b>同步</b>字段（恶是 OVERLORD 品阶，一定有三阶段）；读不到（爪牙/尚未初始化的第一 tick）
 * 再回退 {@link EvilGazerEntity#phase()}（客户端本地字段，默认 1）。</p>
 *
 * <h2>切模型为什么不闪跳</h2>
 * <p>三套模型共用"天空锚点"坐标系（原点一致，模型组负责保持），所以只需要在切换后的
 * {@link #TRANSITION_TICKS} tick 内叠一个 <b>相对</b>变换：从"上方 + 缩小"过渡到"原位 + 原大小"
 * （smoothstep 缓动、用 {@code partial} 插值）。过渡结束变换归零，不会与其它缩放
 * （{@code BossPhaseRenderer} 的 bossRenderScale、品阶倍率）叠加冲突；过渡位移按
 * {@code getBbHeight()} 比例算，<b>不写死模型高度</b>（模型组重做后自动适配）。</p>
 *
 * <h2>攻击起手/判定</h2>
 * <p>读同步 {@code bossState()/bossAttack()}：前摇（windup）时整体略微上扬。
 * 镜头震动 / 屏幕闪光 / 地裂粒子统一由 {@link BossPresenceClient} 处理（单点触发，避免两处重复震）。</p>
 *
 * <h2>小眼（爪牙）</h2>
 * <p>B 组会给小眼打 {@code evil_minion} 标记（品阶 ELITE/NORMAL，不显示 BOSS 条）。
 * 客户端能稳定拿到的特征是"没有 BOSS 阶段同步字段 + 血量上限被压低"，这里按此判定并整体缩小；
 * 若 B 之后加同步标记（{@code isMinion()}），这里会通过反射优先读它（无需改本文件）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class EvilGazerRenderer
        extends MobRenderer<CultivationMonsterEntity,
        EntityModel<CultivationMonsterEntity>> {

    public static final ModelLayerLocation LAYER_ARM = new ModelLayerLocation(
            new ResourceLocation(HongmengMod.MOD_ID, "evil_arm"), "main");
    public static final ModelLayerLocation LAYER_HEAD = new ModelLayerLocation(
            new ResourceLocation(HongmengMod.MOD_ID, "evil_head"), "main");
    public static final ModelLayerLocation LAYER_UPPER = new ModelLayerLocation(
            new ResourceLocation(HongmengMod.MOD_ID, "evil_upper"), "main");

    private static final ResourceLocation TEX = new ResourceLocation(
            HongmengMod.MOD_ID, "textures/entity/evil_phases.png");

    // ------------------------------------------------------------------
    // 与 B（EvilAbilities）约定的同步字段取值（契约 §2.4）
    // ------------------------------------------------------------------
    //
    // bossState: 0=idle / 1=windup / 2=active / 3=recover
    // bossAttack: 0=无；1 起编。这里按契约 §2 技能表顺序：
    //   1=GAZE 凝视 / 2=SWEEP 巨掌横扫 / 3=BEAM 眼棱 / 4=SUMMON 唤眼
    //   5=SLAM 裂口重压 / 6=METEOR 天崩 / 7=ENRAGE 狂暴
    // ⚠ 若 B 最终采用别的 id 映射，只改这一组常量（BossPresenceClient 引用同一组常量）。
    public static final byte ATTACK_NONE = 0;
    public static final byte ATTACK_GAZE = 1;
    public static final byte ATTACK_SWEEP = 2;
    public static final byte ATTACK_BEAM = 3;
    public static final byte ATTACK_SUMMON = 4;
    public static final byte ATTACK_SLAM = 5;
    public static final byte ATTACK_METEOR = 6;
    public static final byte ATTACK_ENRAGE = 7;

    public static final byte STATE_IDLE = 0;
    public static final byte STATE_WINDUP = 1;
    public static final byte STATE_ACTIVE = 2;
    public static final byte STATE_RECOVER = 3;

    /** 切模型过渡时长（tick）。 */
    private static final float TRANSITION_TICKS = 14.0F;
    /** 过渡起始时的相对缩小比例。 */
    private static final float TRANSITION_SCALE_FROM = 0.78F;
    /** 过渡起始时"从上方压入"的高度占实体包围盒高度的比例。 */
    private static final float TRANSITION_DROP_RATIO = 0.35F;
    /** 小眼（爪牙）整体缩放。 */
    private static final float MINION_SCALE = 0.34F;

    /**
     * <b>阶段渲染倍率</b>（实机修复 task-40 契约 §1）：phase1 ×1.35 / phase2 ×1.15 / phase3 ×1.05。
     *
     * <p>为什么一阶段最大：一阶段只有一条手臂（模型向下 157px≈9.8 格），×1.35 后约 13.25 格，
     * 配合服务端 HOVER_ABOVE=13 悬停高度，正好垂到台面附近 — 用户要的"巨大手臂从裂缝伸出"；
     * 二/三阶段模型本体更高（头 11.7 格 / 上身 15.6 格），倍率递减使整体观感逐阶段变大而不至于爆屏。</p>
     *
     * <p>这是<b>客户端渲染倍率</b>，不写进实体属性（契约要求）；叠加在现有入场/切阶段过渡之上。</p>
     */
    private static final float[] PHASE_RENDER_SCALE = {1.35F, 1.15F, 1.05F};
    /** 入场（刚出现）时的缩放起点：0.8 → 1.0 过渡。 */
    private static final float ENTRY_SCALE_FROM = 0.8F;
    /** 入场过渡时长（tick）。 */
    private static final float ENTRY_TICKS = 40.0F;

    /** 返回某阶段的渲染倍率（phase 1~3，越界夹取）。 */
    public static float stageScaleOf(int phase) {
        return PHASE_RENDER_SCALE[Mth.clamp(phase, 1, 3) - 1];
    }

    private final EvilArmModel armModel;
    private final EvilHeadModel headModel;
    private final EvilUpperModel upperModel;

    /** 本帧要画的模型（由 {@link #render} 写入，{@link #getModel()} 读）。 */
    private int phaseNow = 1;
    private boolean minionNow = false;
    /** 本帧的阶段渲染倍率（{@link #scale} 读）。 */
    private float stageScaleNow = 1.0F;
    /** 本帧的入场缩放（0.8→1.0；{@link #scale} 读）。 */
    private float entryScaleNow = 1.0F;

    /** 每只恶的"切模型过渡"状态（按实体 id；实体销毁后惰性清理）。 */
    private final Map<Integer, Transition> transitions = new ConcurrentHashMap<>();

    public EvilGazerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EvilArmModel(ctx.bakeLayer(LAYER_ARM)), 6.0F);
        this.armModel = (EvilArmModel) this.model;
        this.headModel = new EvilHeadModel(ctx.bakeLayer(LAYER_HEAD));
        this.upperModel = new EvilUpperModel(ctx.bakeLayer(LAYER_UPPER));
    }

    // ------------------------------------------------------------------
    // 阶段 / 小眼判定（静态，供 BossPresenceClient / 检查脚本复用）
    // ------------------------------------------------------------------

    /** 当前阶段（1~3）。优先同步字段 {@code bossPhase()}，回退实体自身 phase。 */
    public static int phaseOf(EvilGazerEntity e) {
        if (e == null) {
            return 1;
        }
        int synced = e.bossPhase();          // BossPhase 服务端写、客户端读（1/2/3）
        if (synced >= 1 && synced <= 3) {
            return synced;
        }
        int local = 1;
        try {
            local = e.phase();               // 客户端本地/旧存档兜底
        } catch (Throwable ignored) {
            // 取不到就按一阶段
        }
        return Math.max(1, Math.min(3, local));
    }

    /** 是不是"小眼/爪牙"（B 的 evil_minion 模式）。 */
    public static boolean isMinion(EvilGazerEntity e) {
        if (e == null) {
            return false;
        }
        // 优先反射读 B 可能补的同步标记（不产生编译期依赖）
        try {
            java.lang.reflect.Method m = e.getClass().getMethod("isMinion");
            Object v = m.invoke(e);
            if (v instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
            // 没有这个方法：走下面的兜底判定
        }
        // 兜底：爪牙被降成 ELITE/NORMAL → BossPhase 不会给它写阶段（bossPhase()==0），
        //       且血量上限被压到 ≤60（远低于本体的 420）。
        return e.bossPhase() <= 0 && e.getMaxHealth() < 200.0F;
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void render(CultivationMonsterEntity entity, float yaw, float partial,
                       PoseStack pose, MultiBufferSource buf, int light) {
        if (!(entity instanceof EvilGazerEntity eg)) {
            // 类型注册只服务恶；万一别的实体复用这个渲染器就退回默认行为
            this.phaseNow = 1;
            this.minionNow = false;
            this.stageScaleNow = 1.0F;
            this.entryScaleNow = 1.0F;
            super.render(entity, yaw, partial, pose, buf, light);
            return;
        }
        int target = phaseOf(eg);
        boolean minion = isMinion(eg);
        this.phaseNow = target;
        this.minionNow = minion;

        Transition tr = this.transitions.computeIfAbsent(eg.getId(),
                k -> new Transition(target, eg.tickCount));
        if (tr.phase != target) {
            // 1→2 / 2→3：从头压入 + 缩放淡入
            tr.begin(target, eg.tickCount);
        }
        // ---- 阶段渲染倍率 + 入场 0.8→1.0（task-40 契约 §1/§2）----
        if (minion) {
            this.stageScaleNow = 1.0F;
            this.entryScaleNow = 1.0F;
        } else {
            this.stageScaleNow = stageScaleOf(target);
            float entryAge = (eg.tickCount - tr.spawnTick) + partial;
            float ep = Mth.clamp(entryAge / ENTRY_TICKS, 0.0F, 1.0F);
            float ee = ep * ep * (3.0F - 2.0F * ep);       // smoothstep
            this.entryScaleNow = Mth.lerp(ee, ENTRY_SCALE_FROM, 1.0F);
        }
        float drop = 0.0F;
        float scale = 1.0F;
        float age = (eg.tickCount - tr.startTick) + partial;
        if (age >= 0.0F && age < TRANSITION_TICKS) {
            float p = Mth.clamp(age / TRANSITION_TICKS, 0.0F, 1.0F);
            float ease = p * p * (3.0F - 2.0F * p);       // smoothstep
            float h = Math.max(1.0F, eg.getBbHeight());    // 不写死模型高度
            drop = (1.0F - ease) * h * TRANSITION_DROP_RATIO;
            scale = Mth.lerp(ease, TRANSITION_SCALE_FROM, 1.0F);
        }
        // 前摇：整体略微上扬（负 y = 向上；按包围盒比例，不写死）
        float rise = 0.0F;
        if (eg.bossState() == STATE_WINDUP) {
            float h = Math.max(1.0F, eg.getBbHeight());
            rise = -h * 0.018F;
        }
        pose.pushPose();
        pose.translate(0.0D, drop + rise, 0.0D);
        pose.scale(scale, scale, scale);
        if (minion) {
            pose.scale(MINION_SCALE, MINION_SCALE, MINION_SCALE);
        }
        super.render(entity, yaw, partial, pose, buf, light);
        pose.popPose();

        // 过渡状态惰性清理：实体被移除后旧的 id 条目不会一直堆着
        if (this.transitions.size() > 32) {
            this.transitions.keySet().removeIf(id -> id != entity.getId());
        }
    }

    /**
     * <b>阶段渲染倍率</b>：LivingEntityRenderer 在渲染模型前会调这个方法，
     * 这里乘上"阶段倍率 × 入场 0.8→1.0"（task-40 契约 §1）。
     *
     * <p>它与 {@link #render} 里的相对过渡缩放是叠加关系：过渡管"切开/压入的那一下"，
     * 这里管"这个阶段整体多大"。</p>
     */
    @Override
    protected void scale(CultivationMonsterEntity entity, PoseStack pose, float partial) {
        float k = this.stageScaleNow * this.entryScaleNow;
        if (k != 1.0F) {
            pose.scale(k, k, k);
        }
    }

    @Override
    public EntityModel<CultivationMonsterEntity> getModel() {
        if (this.minionNow) {
            return this.armModel;      // 小眼：用最小的手臂骨架 + 整体缩小
        }
        if (this.phaseNow >= 3) {
            return this.upperModel;
        }
        if (this.phaseNow >= 2) {
            return this.headModel;
        }
        return this.armModel;
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationMonsterEntity entity) {
        return TEX;
    }

    /** 一只恶的切模型过渡状态（轻量对象，避免每帧分配）。 */
    private static final class Transition {
        private int phase;
        private int startTick;
        /** 首次被渲染到的 tick（用于入场 0.8→1.0 过渡）。 */
        private final int spawnTick;

        private Transition(int phase, int nowTick) {
            this.phase = phase;
            this.startTick = nowTick - (int) TRANSITION_TICKS;   // 初次出现不做切模型过渡
            this.spawnTick = nowTick;
        }

        private void begin(int newPhase, int nowTick) {
            this.phase = newPhase;
            this.startTick = nowTick;
        }
    }
}
