package com.hongmeng.xiuxian.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * <b>虚空龙帝</b>：末地的终局存在。
 *
 * <h2>定位</h2>
 * <p>品阶 {@link MobRank#OVERLORD} —— 所以自动继承：</p>
 * <ul>
 *   <li>血条 + 三阶段 + 转阶段演出（{@code BossPhase}）；</li>
 *   <li>远程吐息 / 法术 / 专属大招（{@code BossAbilities}）；</li>
 *   <li>形态渲染（体型放大 + 魔角光环 + 能量翼 + 脚下法阵）；</li>
 *   <li>专属音效（转阶段、突袭、天崩地裂）。</li>
 * </ul>
 *
 * <h2>为什么复用蛟龙骨架</h2>
 * <p>新骨架最容易出"浮空 / 埋土里 / 部件错位"这类问题，而且这个项目的
 * {@code BossFormRenderer} 是按<b>脚底锚点</b>缩放的 —— 骨架的坐标原点必须正确。
 * 蛟龙那套已经验证过，所以只换贴图与体型。</p>
 */
public class VoidDragonEmperorEntity extends CultivationMonsterEntity {

    public VoidDragonEmperorEntity(EntityType<? extends VoidDragonEmperorEntity> type,
                                   Level level) {
        super(type, level, 20);
    }

    @Override
    public MobRank rank() {
        return MobRank.OVERLORD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        // 血 120 / 攻 14 / 击退抗性 1.0 / 追踪 64 / 护甲 12
        return scaledAttributes(MobRank.OVERLORD, 120.0D, 14.0D, 1.0D, 64.0D, 12.0D);
    }
}
