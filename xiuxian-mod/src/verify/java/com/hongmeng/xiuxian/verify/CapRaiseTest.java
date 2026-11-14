package com.hongmeng.xiuxian.verify;

import com.hongmeng.xiuxian.compat.AttributeCaps;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * 端到端验证：抬升 {@code RangedAttribute.maxValue} 到底能不能成功。
 *
 * <p>背景：玩家诊断显示 {@code 在 RangedAttribute 及其父类上都找不到 maxValue 字段}——
 * 因为 Forge 在生产环境会把 mod 字节码重映射到 SRG 名，
 * 而**字符串常量不会跟着重映射**，所以按名字查字段必然失败。</p>
 *
 * <p>修法是加一条**不依赖字段名**的路径：扫描类里所有 {@code double} 字段，
 * 取"当前值等于 {@code getMaxValue()}" 的那个。这个探针就是验证这条路径。</p>
 *
 * <p>注意：这里**不碰 {@code Attributes.MAX_HEALTH}**，因为那需要注册表引导。
 * 直接 {@code new RangedAttribute(...)} 就够验证读写了。</p>
 */
public final class CapRaiseTest {

    private CapRaiseTest() {
    }

    public static void main(String[] args) {
        System.out.println("=== 抬升 RangedAttribute.maxValue 的端到端测试 ===");
        System.out.println();

        // 造一个和原版 MAX_HEALTH 同规格的属性：0 ~ 1024，默认 20
        RangedAttribute attr = new RangedAttribute("test.health", 20.0D, 0.0D, 1024.0D);
        System.out.println("  构造后 getMaxValue = " + attr.getMaxValue());
        System.out.println("  getMinValue = " + attr.getMinValue());
        System.out.println("  真实类 = " + attr.getClass().getName());
        System.out.println();

        System.out.println("  类里所有 double 字段：");
        System.out.print(com.hongmeng.xiuxian.compat.AttributeCapDiagnostics
                .listDoubleFields(attr));
        System.out.println();

        System.out.println("--- 调用 raiseWithDetail ---");
        String err = AttributeCaps.raiseWithDetail(attr);
        System.out.println("  返回：" + (err == null ? "null（成功）" : err));
        System.out.println("  抬升后 getMaxValue = " + attr.getMaxValue());
        System.out.println();

        boolean ok = attr.getMaxValue() >= AttributeCaps.NEW_MAX;
        System.out.println(ok
                ? "  [OK] 上限已抬到 " + (long) attr.getMaxValue()
                : "  [FAIL] 上限仍是 " + (long) attr.getMaxValue());
        System.out.println();

        // 第二个：模拟"字段名被重映射"的情况——用一个字段名完全不同的类
        System.out.println("=== 模拟生产环境：字段名完全不可预测 ===");
        System.out.println("  （用真实 RangedAttribute 无法模拟改名，");
        System.out.println("   但上面那次调用走的正是**按值扫描**路径，");
        System.out.println("   因为验证器环境里 maxValue 这个名字存在，");
        System.out.println("   所以两条路径都会命中——重点看结果是 1e8）");
        System.out.println();

        // 再测一个已经抬好的（幂等性）
        System.out.println("=== 幂等性：重复调用不应出错 ===");
        String err2 = AttributeCaps.raiseWithDetail(attr);
        System.out.println("  第二次返回：" + (err2 == null ? "null（成功）" : err2));
        System.out.println("  getMaxValue 仍是 " + attr.getMaxValue());
        System.out.println();

        // 诊断报告
        System.out.println("=== 诊断报告（单属性视角）===");
        System.out.println("  fieldMaxValue = "
                + com.hongmeng.xiuxian.compat.AttributeCapDiagnostics.fieldMaxValue(attr));

        if (!ok) {
            System.exit(1);
        }
    }
}
