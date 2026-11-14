package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.entity.CultivationMonsterEntity;
import com.hongmeng.xiuxian.entity.MobRank;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * <b>Boss 形态渲染</b>：按阶段放大体型。
 *
 * <p>玩家要求：「boss 有二阶段、三阶段……第二阶段半人半兽/魔化，第三阶段堕落/完全兽化」，
 * 并强调「<b>这次我不希望模型出错</b>」。</p>
 *
 * <h2>为什么用"缩放"而不是三套模型</h2>
 * <p>三套独立模型要三个骨架 + 三张贴图 + 三套动画，出错概率高。这里用**同一骨架**：
 * 体型（二阶段 ×1.18 / 三阶段 ×1.42）+ 颜色（见 {@code BossPhaseRenderer} 的配色）
 * + 粒子做的角与翼（不依赖模型文件，就不可能出错）。</p>
 *
 * <h2>⚠ 缩放必须补偿"脚底偏移"</h2>
 * <p>本项目的人形模型**原点在脚底**（特征点 y=24、{@code 1.501×16 = 24.016}），
 * 而 {@code LivingEntityRenderer} 会先 {@code translate(0, -1.501, 0)} 把脚对到地面。
 * 所以直接 {@code pose.scale(s,s,s)} 会**以脚底为支点向下放大**，boss 会沉进地里
 * （这个项目里已经因为坐标系搞错出现过"一个浮空一个埋土里"的事故）。</p>
 *
 * <p>正确做法：先把支点抬回原点、缩放、再抬回脚底 —— 也就是
 * {@code translate(0, +lift, 0) → scale(s) → translate(0, -lift*s, 0)}，
 * 其中 {@code lift = 1.501}（模型单位）。</p>
 */
public final class BossPhaseRenderer {

    private BossPhaseRenderer() {
    }

    /**
     * 按同步倍率放大 Boss。
     *
     * <h2>为什么直接 scale，不做"1.501 脚底补偿"</h2>
     * <p>{@code RenderLivingEvent.Pre} 是在 {@code LivingEntityRenderer.render} 的
     * 最开头触发的 —— <b>早于</b> {@code pushPose() / setupRotations() / translate(0,-1.501,0)}。
     * 所以此刻 pose 栈的原点就在<b>实体脚底</b>，直接 {@code scale} 就是以脚底为锚放大，
     * 不会浮空也不会陷地。</p>
     *
     * <p>⚠ 旧实现写成 {@code translate(+1.501) → scale(s) → translate(-1.501*s)}，
     * 数学上等于给最终位置加了一个 {@code 1.501·(1-s²)} 的**向下**偏移：三阶段 s=1.42
     * 时 Boss 会沉进地里约 <b>1.53 格</b>（实机截图里"半个身子埋进草地"的根因）。</p>
     */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        float scale = scaleOf(event);
        if (scale <= 1.001F) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.scale(scale, scale, scale);
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (scaleOf(event) <= 1.001F) {
            return;
        }
        event.getPoseStack().popPose();
    }

    /** 读取这个 boss 当前的体型倍率（0 或 1 = 不缩放）。 */
    private static float scaleOf(RenderLivingEvent<?, ?> event) {
        if (!(event.getEntity() instanceof CultivationMonsterEntity boss)) {
            return 1.0F;
        }
        // ⚠ 必须用 effectiveRank()（实例级品阶）：世界 Boss 是靠 hm_rank_override
        // 把 ELITE 实体临时升到 BOSS/OVERLORD 的，用 rank()（类级）会漏掉它们 → 入场放大不生效。
        MobRank rank = boss.effectiveRank();
        if (rank != MobRank.BOSS && rank != MobRank.OVERLORD) {
            return 1.0F;
        }
        // ---- 第四轮 §2.1：最终倍率 = bossRenderScale（阶段/入场）× giant（巨化）----
        // 只在这里乘一次；血魔/虚空等渲染器自己乘的品阶倍率不参与，互不重复。
        float giant = boss.giantScale();
        if (giant < 1.0F) {
            giant = 1.0F;
        }
        // 优先读**同步**倍率（服务端写、客户端读；persistentData 不会下发到客户端，
        // 之前实机上转阶段放大/世界 Boss 入场放大都因此不生效）。
        float synced = boss.bossRenderScale();
        if (synced > 1.001F) {
            return synced * giant;
        }
        float s = boss.getPersistentData().getFloat("hongmeng_boss_scale");
        float base = s <= 0.0F ? 1.0F : s;
        return base * giant;
    }
}
