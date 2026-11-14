package com.hongmeng.xiuxian.compat;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 解除原版属性的数值上限。
 *
 * <p>Minecraft 的属性是 {@link RangedAttribute}，自带硬编码上下限，
 * 超出部分会被**静默截断**：MAX_HEALTH 1024、ATTACK_DAMAGE 2048……</p>
 *
 * <h3>为什么要写得这么啰嗦</h3>
 *
 * <p>玩家实测反馈过"血条卡在 1024"，而诊断显示
 * {@code 反射字段值 = n/a}、{@code relax() 全部失败}——也就是说
 * <b>字段根本找不到</b>。原来的实现在这里有个真实缺陷：</p>
 *
 * <pre>
 *   // 错的：写死了 RangedAttribute.class
 *   RangedAttribute.class.getDeclaredField("maxValue")
 * </pre>
 *
 * <p>Forge 会对类做**运行时转换**，玩家手里的属性对象未必是
 * {@code RangedAttribute} 本身，可能是转换后的子类或代理。
 * 在父类上查字段就可能落空。所以现在改为<b>按实例的真实类逐层向上查</b>，
 * 并且多条写入路径依次尝试：</p>
 *
 * <ol>
 *   <li>反射 {@code Field.setDouble}（首选，Java 17 实测可用）</li>
 *   <li>{@code sun.misc.Unsafe.putDouble}（反射被拦截时的兜底）</li>
 * </ol>
 *
 * <p>每条路都会**读回验证**，并记录失败的真实异常——不再只报"失败"两个字。</p>
 */
public final class AttributeCaps {

    /** 抬到多少。够用到鸿蒙（最大约 50 万）。 */
    public static final double NEW_MAX = 100_000_000.0D;

    /** 字段名候选：官方映射名 + 两代 SRG 名。 */
    private static final String[] FIELD_NAMES = {"maxValue", "f_22310_", "field_111118"};

    private static String lastReport = "（尚未执行）";
    private static String lastDetail = "";
    private static boolean allRaised = false;

    private AttributeCaps() {
    }

    public static String report() {
        return lastReport;
    }

    /** 失败细节（真实异常），出问题时用来定位。 */
    public static String detail() {
        return lastDetail;
    }

    public static boolean allRaised() {
        return allRaised;
    }

    /** 在 mod 构造阶段调用（服务端与客户端各一次）。 */
    public static void relax() {
        relax("构造阶段");
    }

    /**
     * 带时机标签的版本。
     *
     * <p>属性是注册表对象，别的模组可能在更晚的时机重新注册它；所以除了构造阶段，
     * common setup 与每次玩家登录也会核对一遍。已经是目标值就立刻返回，开销可忽略。</p>
     */
    public static void relax(String when) {
        StringBuilder sb = new StringBuilder();
        StringBuilder detail = new StringBuilder();
        int ok = 0;
        int total = 0;

        Attribute[] targets = {
                Attributes.MAX_HEALTH,
                Attributes.ATTACK_DAMAGE,
                Attributes.ARMOR,
                Attributes.ARMOR_TOUGHNESS,
                Attributes.MOVEMENT_SPEED,
        };
        String[] names = {"MAX_HEALTH", "ATTACK_DAMAGE", "ARMOR", "ARMOR_TOUGHNESS",
                "MOVEMENT_SPEED"};

        for (int i = 0; i < targets.length; i++) {
            total++;
            String err = raiseWithDetail(targets[i]);
            double now = maxOf(targets[i]);
            boolean done = err == null;
            if (done) {
                ok++;
            } else {
                detail.append(names[i]).append(": ").append(err).append('\n');
            }
            sb.append(names[i]).append('=')
                    .append(now < 0 ? "n/a" : String.valueOf((long) now))
                    .append(done ? "" : "(失败)")
                    .append("  ");
        }

        allRaised = ok == total;
        lastReport = sb.toString().trim();
        lastDetail = detail.toString().trim();

        if ("构造阶段".equals(when)) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.info(
                    "[hongmeng] 属性上限解除【{}】：{}/{} 成功 | {}", when, ok, total, lastReport);
            if (!lastDetail.isEmpty()) {
                com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                        "[hongmeng] 失败细节：\n{}", lastDetail);
            }
        } else if (ok < total) {
            com.hongmeng.xiuxian.HongmengMod.LOGGER.warn(
                    "[hongmeng] 属性上限在【{}】核对时 {}/{} 未解除 | {} | 细节：{}",
                    when, total - ok, total, lastReport, lastDetail);
        }
    }

    /**
     * 把某个属性的 maxValue 抬到 {@link #NEW_MAX}。
     *
     * @return true 表示成功（含"本来就已经抬高"）
     */
    public static boolean raise(Attribute attribute) {
        return raiseWithDetail(attribute) == null;
    }

    /**
     * 带错误信息的版本。
     *
     * @return null 表示成功；否则是人类可读的失败原因
     */
    public static String raiseWithDetail(Attribute attribute) {
        if (!(attribute instanceof RangedAttribute ranged)) {
            return "不是 RangedAttribute（实际 " + attribute.getClass().getName() + "）";
        }
        if (ranged.getMaxValue() >= NEW_MAX) {
            return null;    // 已经抬好了
        }

        StringBuilder why = new StringBuilder();

        // ---- 路径 A：按已知名字查（开发环境走这条） ----
        Field field = findFieldByName(attribute.getClass());
        if (field != null) {
            String err = write(attribute, field);
            if (err == null) {
                return null;
            }
            why.append("按名字写入失败(").append(err).append(")；");
        } else {
            why.append("按名字(").append(String.join("/", FIELD_NAMES))
                    .append(")找不到字段；");
        }

        // ---- 路径 B：**不依赖字段名**，按"值等于 getMaxValue()"扫描 ----
        //
        // 这是生产环境真正需要的路径。原因：Forge 在生产环境会把 mod 字节码
        // 重映射到 SRG 名，但**字符串常量不会被重映射**——
        // 所以我们写的 "maxValue" 在运行期根本不存在，而正确的名字
        // （可能既不是 maxValue 也不是我以为的那两个 SRG 名）无从预知。
        //
        // 与其猜名字，不如按**语义**找：maxValue 是类里唯一一个
        // "double 类型、且当前值恰好等于 getMaxValue()" 的字段。
        Field semantic = findMaxValueFieldByValue(ranged);
        if (semantic != null) {
            String err = write(attribute, semantic);
            if (err == null) {
                return null;
            }
            why.append("按语义扫描（字段 ").append(semantic.getName())
                    .append("）写入失败(").append(err).append(")");
        } else {
            why.append("按语义扫描也没找到（类里没有值等于 ")
                    .append((long) ranged.getMaxValue()).append(" 的 double 字段）");
        }
        return why.toString();
    }

    /** 按已知名字在类层级里找字段。 */
    private static Field findFieldByName(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String name : FIELD_NAMES) {
                try {
                    return c.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                    // 换下一个名字
                }
            }
        }
        return null;
    }

    /**
     * **按值找字段**：在类层级里扫描全部 {@code double} 字段，
     * 返回当前值等于 {@code getMaxValue()} 的那个。
     *
     * <p>这招不依赖任何名字，所以对重映射后的生产环境同样有效。
     * {@code maxValue} 是唯一满足这个条件的字段——{@code minValue} 是 0 或负数，
     * 不会撞上。</p>
     */
    private static Field findMaxValueFieldByValue(RangedAttribute attribute) {
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
                    // 读不了就跳过这个字段
                }
            }
        }
        return null;
    }

    /** 写入并验证。返回 null 表示成功。 */
    private static String write(Attribute attribute, Field field) {
        // 先试反射
        try {
            if (!field.canAccess(attribute)) {
                field.setAccessible(true);
            }
            field.setDouble(attribute, NEW_MAX);
            if (readBack(attribute, field) >= NEW_MAX) {
                return null;
            }
        } catch (Throwable t) {
            // 落到 Unsafe
            String u = writeViaUnsafe(attribute, field);
            if (u == null) {
                return null;
            }
            return t.getClass().getSimpleName() + ": " + t.getMessage() + " / Unsafe: " + u;
        }
        // 反射没报错但读回不对（final 字段被 JIT 常量折叠）→ 用 Unsafe
        String u = writeViaUnsafe(attribute, field);
        if (u == null) {
            return null;
        }
        return "反射写入后读回无效，Unsafe 也失败：" + u;
    }

    private static double readBack(Attribute attribute, Field field) {
        try {
            return field.getDouble(attribute);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    /** 用 sun.misc.Unsafe 直接改内存。反射被访问控制或 final 语义拦住时的兜底。 */
    private static String writeViaUnsafe(Object target, Field field) {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);

            Method objectFieldOffset = unsafeClass.getMethod("objectFieldOffset", Field.class);
            long offset = (Long) objectFieldOffset.invoke(unsafe, field);

            Method putDouble = unsafeClass.getMethod("putDouble",
                    Object.class, long.class, double.class);
            putDouble.invoke(unsafe, target, offset, NEW_MAX);

            double back = readBack((Attribute) target, field);
            if (back >= NEW_MAX) {
                return null;
            }
            return "写入后读回仍是 " + (long) back;
        } catch (Throwable t) {
            return t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    /** 读回一个属性的当前上限，用于自检。 */
    public static double maxOf(Attribute attribute) {
        if (attribute instanceof RangedAttribute ranged) {
            return ranged.getMaxValue();
        }
        return -1.0D;
    }
}
