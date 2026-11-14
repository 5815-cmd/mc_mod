package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.combat.CombatFeel;
import com.hongmeng.xiuxian.spirit.Element;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：**一次"发生了什么"的事实**（不是"给我震一下"这个指令）。
 *
 * <h2>为什么震动不是服务端喊的</h2>
 *
 * <p>规格 §二 写了两条硬约束：<b>屏幕震动必须可关闭</b>（无障碍）且
 * <b>不要每次命中都发包去喊震动</b>。所以这个包里的字段是
 * <b>伤害占比</b>这类事实，客户端自己用同一张纯数据表
 * （{@link CombatFeel#shakeForDamage(double)}）算出"该震多重"——
 * 于是"关闭屏幕震动"完全发生在客户端：关掉之后这些包照收，只是不震。</p>
 *
 * <h2>只发给当事人</h2>
 *
 * <p>谁打的发给谁（{@link #impact}），谁挨打发给谁（{@link #taken}）。
 * 不做全服广播：别人的命中不需要震你的屏。每 tick 每人还有
 * {@code CombatFeedback.MAX_PACKETS_PER_TICK} 的上限（AOE 打一群怪时不刷包）。</p>
 */
public class CombatFeedbackPacket {

    /** 我打中了别人：震动 + 伤害飘字 + FOV 冲击。 */
    public static final byte KIND_IMPACT = 0;
    /** 我被打中了：震动 + 红色飘字。 */
    public static final byte KIND_TAKEN = 1;
    /** 强者登场：天空变暗 + 横幅从上方压下（规格 §3.2）。 */
    public static final byte KIND_BOSS_ENTRANCE = 2;

    private final byte kind;
    private final double x;
    private final double y;
    private final double z;
    private final float amount;
    private final float fraction;
    private final int entityId;
    private final int element;
    private final int color;
    private final String label;

    public CombatFeedbackPacket(byte kind, double x, double y, double z,
                                float amount, float fraction, int entityId,
                                int element, int color, String label) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.z = z;
        this.amount = amount;
        this.fraction = fraction;
        this.entityId = entityId;
        this.element = element;
        this.color = color;
        this.label = label == null ? "" : label;
    }

    // ---- 工厂 ----

    /** 我打中了：附带受击者 id，客户端把飘字挂在它头上。 */
    public static CombatFeedbackPacket impact(LivingEntity victim, float amount,
                                              float fraction, Element element, Vec3 at) {
        return new CombatFeedbackPacket(KIND_IMPACT, at.x, at.y, at.z, amount, fraction,
                victim.getId(), element == null ? -1 : element.ordinal(), 0xFFFFFF, "");
    }

    /** 我被打中了。 */
    public static CombatFeedbackPacket taken(float amount, float fraction, Vec3 at) {
        return new CombatFeedbackPacket(KIND_TAKEN, at.x, at.y, at.z, amount, fraction,
                -1, -1, 0xFF5555, "");
    }

    /** 强者登场（附近玩家都收）。 */
    public static CombatFeedbackPacket bossEntrance(LivingEntity boss, int auraColor,
                                                    String label) {
        return new CombatFeedbackPacket(KIND_BOSS_ENTRANCE,
                boss.getX(), boss.getY(), boss.getZ(), 0.0F, 1.0F,
                boss.getId(), -1, auraColor, label);
    }

    // ---- 读取 ----

    public byte kind() {
        return kind;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float amount() {
        return amount;
    }

    public float fraction() {
        return fraction;
    }

    public int entityId() {
        return entityId;
    }

    public int color() {
        return color;
    }

    public String label() {
        return label;
    }

    /** 元素；没有就返回 null。 */
    public Element element() {
        if (element < 0 || element >= Element.values().length) {
            return null;
        }
        return Element.values()[element];
    }

    // ---- 编解码 ----

    public static void encode(CombatFeedbackPacket packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.kind);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeFloat(packet.amount);
        buf.writeFloat(packet.fraction);
        buf.writeVarInt(packet.entityId + 1);
        buf.writeVarInt(packet.element + 1);
        buf.writeInt(packet.color);
        buf.writeUtf(packet.label, 64);
    }

    public static CombatFeedbackPacket decode(FriendlyByteBuf buf) {
        byte kind = buf.readByte();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float amount = buf.readFloat();
        float fraction = buf.readFloat();
        int entity = buf.readVarInt() - 1;
        int element = buf.readVarInt() - 1;
        int color = buf.readInt();
        String label = buf.readUtf(64);
        return new CombatFeedbackPacket(kind, x, y, z, amount, fraction,
                entity, element, color, label);
    }

    public static void handle(CombatFeedbackPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // ⚠ 反射调用：直接引用 CombatFeelClient 会让专用服务器崩
            //（`Attempted to load class ... for invalid dist DEDICATED_SERVER`）。
            // 项目里已经踩过一次同类问题（GuideBookItem 的 DistExecutor 那次）。
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                    != net.minecraftforge.api.distmarker.Dist.CLIENT) {
                return;
            }
            try {
                Class.forName("com.hongmeng.xiuxian.client.CombatFeelClient")
                        .getMethod("onPacket", CombatFeedbackPacket.class)
                        .invoke(null, packet);
            } catch (Throwable t) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.debug(
                        "[hongmeng] 战斗反馈包处理失败：{}", t.toString());
            }
        });
        context.setPacketHandled(true);
    }
}
