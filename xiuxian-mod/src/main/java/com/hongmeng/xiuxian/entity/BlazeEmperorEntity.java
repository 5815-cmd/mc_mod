package com.hongmeng.xiuxian.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * <b>焚天炎帝</b>：下界的终局存在（参考灾厄 mod 的巨体型 boss 思路）。
 *
 * <h2>定位</h2>
 * <p>品阶 {@link MobRank#BOSS} —— 自动继承血条、三阶段、转阶段演出、
 * 远程吐息、法术、专属大招（魔影突袭 / 天崩地裂），以及形态渲染。</p>
 *
 * <h2>为什么复用烈焰猿骨架</h2>
 * <p>灾厄那种 boss 的压迫感来自「<b>大</b> + 动作夸张 + 多阶段」，
 * 而不是"必须新骨架"。新骨架在本项目踩过坑（炎猿脚底在地面下 1.5 格），
 * 所以这里复用<b>已验证脚底坐标</b>的骨架，靠体型与配色拉开差距。</p>
 */
public class BlazeEmperorEntity extends CultivationMonsterEntity {

    public BlazeEmperorEntity(EntityType<? extends BlazeEmperorEntity> type,
                              Level level) {
        super(type, level, 18);
    }

    @Override
    public MobRank rank() {
        return MobRank.BOSS;
    }

    public static AttributeSupplier.Builder createAttributes() {
        // 血 90 / 攻 12 / 击退抗性 0.9 / 追踪 48 / 护甲 10
        return scaledAttributes(MobRank.BOSS, 90.0D, 12.0D, 0.9D, 48.0D, 10.0D);
    }
}
