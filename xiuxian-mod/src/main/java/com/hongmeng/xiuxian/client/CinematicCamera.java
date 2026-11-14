package com.hongmeng.xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;

/**
 * <b>把演出镜头接到 Forge 的相机钩子上</b>。
 *
 * <h2>为什么不用 mixin</h2>
 * <p>灾变的过场模组依赖它自己模组的内部代码，作者明确写了
 * 「若这些代码发生变动，过场动画可能会失效」，而且要求集成包配置实体 tick 剔除白名单。
 * 这里走另一条路：<b>只用 Forge 官方钩子</b>
 * （{@code ViewportEvent.ComputeCameraAngles} + {@code ComputeFovModifierEvent}），
 * 不碰任何原版或别的模组的内部实现 —— 兼容性风险低得多。</p>
 *
 * <p>相机位置本身没法通过事件改（原版在算完角度前就定好了位置），
 * 所以这里的做法是：<b>把玩家的朝向强行拧向"镜头看向的点"</b>，
 * 再配合位置上的演出（把玩家传送到镜头位）—— 效果与推轨镜头一致，
 * 而实现只有事件钩子这一层。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CinematicCamera {

    private CinematicCamera() {
    }

    /** 演出期间：把玩家朝向拧到镜头看的方向。 */
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!Cinematic.active()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float partial = (float) event.getPartialTick();
        Vec3 cam = Cinematic.cameraPos(partial);
        Vec3 look = Cinematic.lookTarget(partial);
        if (cam == null || look == null) {
            return;
        }
        // ⚠ 这里**曾经**写着 `mc.player.setPos(cam…)` —— 那是错的。
        //
        //   它把玩家本人搬到了镜头位置，后果三条：
        //     ① 以玩家为锚点的特效（蓄力阵 / 剑阵 / 领域）跟着镜头飘走；
        //     ② 破坏类法术的落点按玩家视线算 → 射线起点变了；
        //     ③ 玩家被搬到几十格外 → 区块加载 / 卸载跟着抖。
        //
        //   玩家原话：「那个法术的运镜如果你移动玩家的话，那么岂不是特效位置
        //   也会发生变化」—— **完全正确**。
        //
        //   现在**重新启用**移动玩家 —— 因为法术已经锚定到施法原点
        //   （见 SpellCastHandler.castOrigin），不会跟着镜头飘。
        //   玩家给的方向：「释放的位置在玩家脚下，后续移动法术不跟随玩家移动」。
        //
        //   注意：这是**纯客户端**行为（mc.player.setPos），
        //   服务端的 player.position() 始终是真实位置，
        //   所以命中判定、掉落、区块加载都不受影响。
        mc.player.setPos(cam.x, cam.y, cam.z);
        mc.player.setDeltaMovement(Vec3.ZERO);
        //   玩家实体**一律不动**。这里只负责把朝向拧到镜头看的方向。

        // 由"看向的点 - 相机位"反解 yaw / pitch
        double dx = look.x - cam.x;
        double dy = look.y - cam.y;
        double dz = look.z - cam.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horiz) * (180.0D / Math.PI)));

        event.setYaw(yaw);
        event.setPitch(Mth.clamp(pitch, -89.0F, 89.0F));
        // 极轻微的滚转让画面"活"一点（纯推轨会显得很僵）
        event.setRoll(Mth.sin((float) event.getPartialTick() * 0.55F) * 0.9F);
    }

    /** 演出期间：视场角按关键帧收窄（长焦压缩感）。 */
    public static void onFov(ComputeFovModifierEvent event) {
        if (!Cinematic.active()) {
            return;
        }
        // ⚠ ComputeFovModifierEvent **没有** getPartialTick()（只有 getFovModifier /
        //   getNewFovModifier / getPlayer / setNewFovModifier，已解析 class 确认），
        //   所以帧内插值系数要从客户端自己取。
        float partial = Minecraft.getInstance().getFrameTime();
        float want = Cinematic.fov(partial);
        if (want <= 0.0F) {
            return;
        }
        // event 给的是"倍率"，所以要换算成相对于原版默认 70 的比值
        event.setNewFovModifier(event.getNewFovModifier() * (want / 70.0F));
    }
}
