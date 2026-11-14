package com.hongmeng.xiuxian.compat;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.lang.reflect.Field;

/**
 * 属性上限的**权威自检**。
 *
 * <p>玩家反馈"面板里还是 1024"，而 {@link AttributeCaps#relax()} 报告成功。
 * 两者矛盾，说明"报告成功"这一环不可信——可能 {@code getMaxValue()} 读到的
 * 不是真正的字段，或者读回验证本身有问题。</p>
 *
 * <p>所以这里**绕过 getMaxValue()**，直接用反射把 {@code maxValue} 字段的原始值
 * 读出来对比。两条路读数不一致，就说明有 JIT 内联或字段名问题。</p>
 */
public final class AttributeCapDiagnostics {

    private AttributeCapDiagnostics() {
    }

    /** 反射读 maxValue 字段（不经过 getter），拿不到返回 NaN。 */
    public static double fieldMaxValue(Attribute attribute) {
        if (!(attribute instanceof RangedAttribute)) {
            return Double.NaN;
        }
        // 先按名字找，找不到就按"值等于 getMaxValue()"扫描。
        // 生产环境里字段名会被重映射，字符串常量却不会——所以名字不可靠，
        // 必须有一条不依赖名字的路径。
        Field f = findByName(attribute.getClass());
        if (f == null) {
            f = findByValue((RangedAttribute) attribute);
        }
        if (f == null) {
            return Double.NaN;
        }
        try {
            if (!f.canAccess(attribute)) {
                f.setAccessible(true);
            }
            return f.getDouble(attribute);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static Field findByName(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String fieldName : new String[]{"maxValue", "f_22310_", "field_111118"}) {
                try {
                    return c.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                    // 换下一个名字
                }
            }
        }
        return null;
    }

    private static Field findByValue(RangedAttribute attribute) {
        double want = attribute.getMaxValue();
        for (Class<?> c = attribute.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != double.class) {
                    continue;
                }
                try {
                    if (!f.canAccess(attribute)) {
                        f.setAccessible(true);
                    }
                    if (Double.compare(f.getDouble(attribute), want) == 0) {
                        return f;
                    }
                } catch (Throwable ignored) {
                    // 跳过
                }
            }
        }
        return null;
    }

    /** 列出属性类里**所有** double 字段的名字与当前值——排查字段名问题用。 */
    public static String listDoubleFields(Attribute attribute) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> c = attribute.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != double.class) {
                    continue;
                }
                String value;
                try {
                    if (!f.canAccess(attribute)) {
                        f.setAccessible(true);
                    }
                    value = String.valueOf((long) f.getDouble(attribute));
                } catch (Throwable t) {
                    value = "读不到";
                }
                sb.append("    ").append(c.getSimpleName()).append('.')
                        .append(f.getName()).append(" = ").append(value).append('\n');
            }
        }
        return sb.length() == 0 ? "    （没有 double 字段）\n" : sb.toString();
    }

    /** 诊断报告：每一行是"属性名 getter读数 / 字段读数 / 判定"。 */
    public static String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 属性上限诊断 ===\n");
        sb.append(String.format("%-18s %14s %14s %12s%n",
                "属性", "getMaxValue", "反射字段值", "判定"));

        Attribute[] targets = {
                Attributes.MAX_HEALTH,
                Attributes.ATTACK_DAMAGE,
                Attributes.ARMOR,
                Attributes.ARMOR_TOUGHNESS,
                Attributes.MOVEMENT_SPEED,
        };
        String[] names = {"MAX_HEALTH", "ATTACK_DAMAGE", "ARMOR", "ARMOR_TOUGHNESS",
                "MOVEMENT_SPEED"};

        boolean allOk = true;
        for (int i = 0; i < targets.length; i++) {
            Attribute a = targets[i];
            double viaGetter = AttributeCaps.maxOf(a);
            double viaField = fieldMaxValue(a);
            boolean ok;
            String verdict;
            if (Double.isNaN(viaField)) {
                ok = false;
                verdict = "字段读不到";
            } else if (viaField >= AttributeCaps.NEW_MAX) {
                ok = true;
                verdict = "OK";
            } else {
                ok = false;
                verdict = "**未抬高**";
            }
            if (!ok) {
                allOk = false;
            }
            sb.append(String.format("%-18s %14s %14s %12s%n",
                    names[i],
                    viaGetter < 0 ? "n/a" : String.valueOf((long) viaGetter),
                    Double.isNaN(viaField) ? "n/a" : String.valueOf((long) viaField),
                    verdict));
        }
        sb.append('\n');
        sb.append("属性对象的真实类：").append(Attributes.MAX_HEALTH.getClass().getName())
                .append('\n');
        // 把类里所有 double 字段列出来——字段名被重映射时，
        // 这一行能直接告诉我们是哪个字段在起作用。
        sb.append("MAX_HEALTH 上的全部 double 字段：\n")
                .append(listDoubleFields(Attributes.MAX_HEALTH));
        sb.append("relax() 自报：").append(AttributeCaps.report()).append('\n');
        String detail = AttributeCaps.detail();
        if (!detail.isEmpty()) {
            sb.append("失败细节：\n").append(detail).append('\n');
        }
        sb.append("结论：").append(allOk
                ? "所有属性上限已解除，高境界加成不会被截断"
                : "**有属性上限没解除！这就是血条卡 1024 的原因**").append('\n');
        return sb.toString();
    }
}
