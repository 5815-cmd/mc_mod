package com.hongmeng.xiuxian.client;

import com.hongmeng.xiuxian.combat.CombatFeel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 伤害飘字（{@code docs/combat-feel-spec.md} 第 8 条）。
 *
 * <h2>为什么走 {@code RenderLevelStageEvent} 而不是名牌系统</h2>
 *
 * <p>头顶血条（{@code MobHealthBarRenderer}）挂在 {@code RenderNameTagEvent} 上是对的：
 * 它画的是<b>实体的一部分</b>。但飘字不是——它要离开实体往上飘、要在实体死后继续存在、
 * 还要一次画好几个（AOE 打一群怪）。所以这里选
 * {@code RenderLevelStageEvent（AFTER_PARTICLES）}：那之后的 {@link PoseStack}
 * 停在"相机原点、未旋转"的世界坐标上，正好适合画"世界里的浮动文字"。</p>
 *
 * <h2>文字怎么画进世界</h2>
 *
 * <ol>
 *   <li>{@code translate(位置 - 相机位置)} → 世界坐标；</li>
 *   <li>{@code mulPose(相机朝向)} → billboard，永远正对玩家；</li>
 *   <li>{@code scale(-0.025, -0.025, 0.025)} → 与名牌同一套比例
 *       （1 单位 = 0.025 格，取自原版名牌的写法）；</li>
 *   <li>先画一遍深色的"影子"（向右下偏移 1 单位），再画本体——
 *       这样在任何背景上都读得清，不需要描边 API。</li>
 * </ol>
 *
 * <p>渲染类型用 {@code Font.DisplayMode.SEE_THROUGH}：飘字<b>不被怪物模型挡住</b>。
 * 这是故意的——"我打出了多少"必须永远看得见。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class DamageNumbers {

    /** 同时存在的飘字上限（超出就丢最老的）。 */
    public static final int MAX_ENTRIES = 48;

    /** 飘字世界缩放（1 单位 = 0.025 格，与名牌一致）。 */
    private static final float SCALE = 0.025F;

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private DamageNumbers() {
    }

    private static final class Entry {
        double x;
        double y;
        double z;
        final String text;
        final int color;
        final float scale;
        final int life;
        int age;
        final double driftX;
        final double driftZ;

        Entry(double x, double y, double z, String text, int color, float scale, int life,
              double driftX, double driftZ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.text = text;
            this.color = color;
            this.scale = scale;
            this.life = life;
            this.driftX = driftX;
            this.driftZ = driftZ;
        }
    }

    // ------------------------------------------------------------------
    // 数据
    // ------------------------------------------------------------------

    /**
     * 加一条飘字。
     *
     * @param entityId 挂在哪个实体身上（-1 表示只按坐标飘）
     * @param fraction 伤害占比（决定颜色与字号）
     */
    public static void add(int entityId, double x, double y, double z,
                           float damage, float fraction, boolean incoming) {
        if (!DamageNumbers.enabled()) {
            return;
        }
        String text = format(damage);
        int color;
        float scale = 1.0F;
        if (incoming) {
            // 我挨打：红字、略大（保留"疼"的提示，但 2026-11-02 起从 1.15 缩到 0.95，
            // 配合减弱的挨打震动，避免挨一下满屏都是字）
            color = 0xFFFF5555;
            scale = 0.95F;
        } else if (fraction > CombatFeel.HITSTOP_HEAVY) {
            // 25% 以上：金字 + 更大（"这一下打穿了"）
            color = 0xFFFFD24A;
            scale = 1.45F;
            text = text + "!";
        } else if (fraction > CombatFeel.HITSTOP_MEDIUM) {
            color = 0xFFFFE9A0;
            scale = 1.2F;
        } else {
            // 玩家反馈：「在血条上方白色看不清」。普通命中原本是纯白，
            // 打在怪身上时正好落在自己的血条 / 动作栏那一片亮区里，几乎读不出来。
            // 改成亮黄：既和 25% 以上的金色大招区分（那是金，这是黄），
            // 又能在任何背景上看清。
            color = 0xFFFFE24A;
        }
        // 生成位置：优先用受击者头顶（第一次世界坐标取一次就够了）。
        // 之后飘字<b>留在世界里</b>——怪物继续走开、飘字留在原地，这是伤害数字的通行做法
        // （跟着怪物跑会晃得读不出来；实体死了更不该跟着消失）。
        double sx = x;
        double sy = y;
        double sz = z;
        if (entityId >= 0 && Minecraft.getInstance().level != null) {
            Entity e = Minecraft.getInstance().level.getEntity(entityId);
            if (e != null) {
                sx = e.getX();
                sy = e.getY() + e.getBbHeight() + 0.35D;
                sz = e.getZ();
            }
        }
        if (ENTRIES.size() >= MAX_ENTRIES) {
            ENTRIES.remove(0);
        }
        double drift = (Minecraft.getInstance().level == null ? 0.0D
                : Minecraft.getInstance().level.random.nextDouble() - 0.5D) * 0.35D;
        ENTRIES.add(new Entry(sx, sy, sz, text, color,
                scale, CombatFeel.DAMAGE_NUMBER_TICKS, drift, -drift));
    }

    /** 加一条自定义文字的飘字（击杀/破防之类）。 */
    public static void addText(double x, double y, double z, String text, int color, float scale) {
        if (!DamageNumbers.enabled()) {
            return;
        }
        if (ENTRIES.size() >= MAX_ENTRIES) {
            ENTRIES.remove(0);
        }
        ENTRIES.add(new Entry(x, y, z, text, color, scale,
                CombatFeel.DAMAGE_NUMBER_TICKS + 10, 0.0D, 0.0D));
    }

    /** 每 tick 推进（客户端）。 */
    public static void tick() {
        if (ENTRIES.isEmpty()) {
            return;
        }
        Iterator<Entry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            e.age++;
            if (e.age >= e.life) {
                it.remove();
                continue;
            }
            e.x += e.driftX * 0.02D;
            e.z += e.driftZ * 0.02D;
        }
    }

    public static void clear() {
        ENTRIES.clear();
    }

    /** 当前条数（诊断）。 */
    public static int size() {
        return ENTRIES.size();
    }

    /** 飘字开关（客户端配置，默认开）。 */
    public static boolean enabled() {
        try {
            return com.hongmeng.xiuxian.compat.HongmengConfig.CLIENT.damageNumbers.get();
        } catch (Throwable t) {
            return true;
        }
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    /** 由 {@code RenderLevelStageEvent} 调（AFTER_PARTICLES）。 */
    public static void render(PoseStack pose, Vec3 cameraPos) {
        if (ENTRIES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        boolean any = false;

        // **必须用我们自己的缓冲区，绝不能用关卡的共享 bufferSource。**
        //
        // 玩家 15:19 的崩溃就是这个：
        //   NullPointerException: "shaderinstance" is null
        //     at LevelRenderer.renderChunkLayer(LevelRenderer.java:1481)
        //
        // 机制：AFTER_PARTICLES 触发时 LevelRenderer 自己**还没画完**。
        // 在共享缓冲区上调无参 endBatch() 会把整个关卡的待提交缓冲一起提前刷掉，
        // 并把 RenderSystem.getShader() 留在最后一个渲染类型
        // clearRenderState() 之后的 **null** 状态；紧接着 renderChunkLayer
        // 读 RenderSystem.getShader() 就直接 NPE。
        //
        // MultiBufferSource.immediate 每帧自己的 builder，endBatch() 只提交自己这点。
        MultiBufferSource.BufferSource own =
                MultiBufferSource.immediate(com.mojang.blaze3d.vertex.Tesselator
                        .getInstance().getBuilder());

        for (Entry e : ENTRIES) {
            float alpha = CombatFeel.damageNumberAlpha(e.age);
            if (alpha <= 0.02F) {
                continue;
            }
            double rise = CombatFeel.damageNumberRise(e.age);
            pose.pushPose();
            pose.translate(e.x - cameraPos.x, e.y - cameraPos.y + rise, e.z - cameraPos.z);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            float s = SCALE * e.scale;
            pose.scale(-s, -s, s);
            Matrix4f matrix = pose.last().pose();
            float halfWidth = font.width(e.text) / 2.0F;
            int color = withAlpha(e.color, alpha);
            // 影子（保证在任何背景上都读得清）
            font.drawInBatch(e.text, -halfWidth + 1.0F, 1.0F, withAlpha(0x000000, alpha * 0.75F),
                    false, matrix, own, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            font.drawInBatch(e.text, -halfWidth, 0.0F, color, false, matrix, own,
                    Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            pose.popPose();
            any = true;
        }
        if (any) {
            // 只提交**我们自己**的缓冲区（不再是无参地刷整个关卡）
            own.endBatch();
        }
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * 数值格式化：整数就写整数，小数保留一位。
     *
     * <p>刻意不用"万/亿"那种缩写（血条那边用了，因为血条空间小）——
     * 飘字是"我打出了多少"的即时反馈，缩写会让人读不出量级。</p>
     */
    private static String format(float damage) {
        if (damage >= 1000.0F || Math.abs(damage - Math.round(damage)) < 0.05F) {
            return String.valueOf(Math.round(damage));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", damage);
    }
}
