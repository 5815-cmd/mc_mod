package com.hongmeng.xiuxian.verify;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 把"抬升 RangedAttribute.maxValue"这条路彻底走通，并打印每一步的真实结果。
 *
 * <p>玩家诊断显示 {@code getMaxValue()=1024}、{@code 反射字段值=n/a}、
 * {@code relax() 失败}——说明 {@code getDeclaredField("maxValue")} 或后续的
 * 读写被访问控制拦住了，而原来的代码把异常吞掉了，只报"失败"。</p>
 *
 * <p>这个探针把**所有可行路径**都试一遍并打印真实异常，从而找出在当前
 * JVM 上到底哪条路能通。</p>
 */
public final class CapReflectProbe {

    private CapReflectProbe() {
    }

    /** 找一个"构造签名像 RangedAttribute"的类来做实验，避开 Minecraft 依赖。 */
    static final class Fake {
        private final double minValue;
        private final double maxValue;

        Fake(double minValue, double maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        double max() {
            return maxValue;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== JVM ===");
        System.out.println("  java.version    = " + System.getProperty("java.version"));
        System.out.println("  java.vendor     = " + System.getProperty("java.vendor"));
        System.out.println("  java.class.version = " + System.getProperty("java.class.version"));
        System.out.println();

        System.out.println("=== 路径 1：Field.setAccessible(true) + setDouble ===");
        try {
            Field f = Fake.class.getDeclaredField("maxValue");
            f.setAccessible(true);
            // 关键：**读写必须是同一个实例**。之前这里各建了一个新对象，
            // 于是"读回"读的是没被改过的那个，得出"写入无效"的错误结论。
            Fake inst = new Fake(0, 1024);
            f.setDouble(inst, 1.0E8);
            double readViaField = f.getDouble(inst);
            double readViaMethod = inst.max();
            System.out.println("  写入后 反射读=" + readViaField + "  方法读=" + readViaMethod);
            System.out.println("  → " + (readViaMethod == 1.0E8 ? "写入**生效**" : "写入**无效**"));
        } catch (Throwable t) {
            System.out.println("  失败：" + t.getClass().getName() + " : " + t.getMessage());
        }
        System.out.println();

        System.out.println("=== 路径 1b：写入后**经 getter 方法**读取（模拟 getMaxValue）===");
        try {
            Field f = Fake.class.getDeclaredField("maxValue");
            f.setAccessible(true);
            Fake inst = new Fake(0, 1024);
            f.setDouble(inst, 1.0E8);
            // 连续多读几次，排除 JIT 常量折叠的干扰
            boolean ok = true;
            for (int i = 0; i < 5; i++) {
                if (inst.max() != 1.0E8) {
                    ok = false;
                    break;
                }
            }
            System.out.println("  连读 5 次一致：" + ok);
        } catch (Throwable t) {
            System.out.println("  失败：" + t.getClass().getName() + " : " + t.getMessage());
        }
        System.out.println();

        System.out.println("=== 路径 2：用 getDeclaredField 读（不 setAccessible）===");
        try {
            Field f = Fake.class.getDeclaredField("maxValue");
            System.out.println("  拿到字段，canAccess=" + f.canAccess(new Fake(0, 1)));
            System.out.println("  尝试直接读……");
            double v = f.getDouble(new Fake(0, 1024));
            System.out.println("  结果：" + v);
        } catch (Throwable t) {
            System.out.println("  失败：" + t.getClass().getName() + " : " + t.getMessage());
        }
        System.out.println();

        System.out.println("=== 路径 3：MethodHandles.privateLookupIn + findSetter ===");
        try {
            MethodHandles.Lookup lookup =
                    MethodHandles.privateLookupIn(Fake.class, MethodHandles.lookup());
            VarHandle handle = lookup.findVarHandle(Fake.class, "maxValue", double.class);
            Fake inst = new Fake(0, 1024);
            handle.set(inst, 1.0E8);
            System.out.println("  结果：成功（读回 " + inst.max() + "）");
        } catch (Throwable t) {
            System.out.println("  失败：" + t.getClass().getName() + " : " + t.getMessage());
        }
        System.out.println();

        System.out.println("=== 路径 4：MethodHandles.findStaticVarHandle? 不适用，试 unreflectSetter ===");
        try {
            MethodHandles.Lookup lookup =
                    MethodHandles.privateLookupIn(Fake.class, MethodHandles.lookup());
            java.lang.invoke.MethodHandle setter = lookup.findSetter(Fake.class, "maxValue", double.class);
            Fake inst = new Fake(0, 1024);
            setter.invoke(inst, 1.0E8);
            System.out.println("  结果：成功（读回 " + inst.max() + "）");
        } catch (Throwable t) {
            System.out.println("  失败：" + t.getClass().getName() + " : " + t.getMessage());
        }
        System.out.println();

        System.out.println("=== 路径 5：改 Field.modifiers 去 final（老办法）===");
        try {
            Field f = Fake.class.getDeclaredField("maxValue");
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            System.out.println("  Field.modifiers 存在");
        } catch (Throwable t) {
            System.out.println("  失败：" + t.getClass().getName() + " : " + t.getMessage());
            System.out.println("  （Java 12+ 已经把 Field.modifiers 移除了，这条路已废）");
        }
        System.out.println();

        System.out.println("=== 路径 6：Unsafe（最后手段）===");
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            Method objectFieldOffset =
                    unsafeClass.getMethod("objectFieldOffset", Field.class);
            Field f = Fake.class.getDeclaredField("maxValue");
            long offset = (Long) objectFieldOffset.invoke(unsafe, f);
            Method putDouble = unsafeClass.getMethod("putDouble", Object.class, long.class, double.class);
            Fake inst = new Fake(0, 1024);
            putDouble.invoke(unsafe, inst, offset, 1.0E8);
            System.out.println("  结果：成功（读回 " + inst.max() + "）");
        } catch (Throwable t) {
            System.out.println("  失败：" + t.getClass().getName() + " : " + t.getMessage());
        }
    }
}
