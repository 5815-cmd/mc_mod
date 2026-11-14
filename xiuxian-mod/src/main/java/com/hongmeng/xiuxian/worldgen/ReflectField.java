package com.hongmeng.xiuxian.worldgen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>不依赖字段名</b>的反射查找。
 *
 * <h2>为什么必须这样写</h2>
 *
 * <p>Forge 在生产环境会把 mod 的字节码重映射到 SRG 名（{@code parameters} →
 * {@code f_223018_} 之类），但<b>字符串常量不会跟着重映射</b>。所以：</p>
 *
 * <pre>
 *   ObfuscationReflectionHelper.findField(ChunkGenerator.class, "featuresPerStep")
 *     开发环境：成功
 *     生产环境：NoSuchFieldException   ← 玩家就是这么撞上的
 * </pre>
 *
 * <p>玩家日志里的原话：</p>
 * <pre>
 *   [hongmeng] 新群系注入失败：NoSuchFieldException: parameters
 * </pre>
 * <p>后果是<b>三个新群系（灵脉山谷 / 云梦泽 / 荒古战场）在整合包里完全不生效</b>，
 * 而开发环境的 {@code runServer} 一切正常——正是这种"只有线上才犯"的错最难查。</p>
 *
 * <h2>做法</h2>
 *
 * <p>按<b>类型</b>找：泛型擦除之后，这些字段的原始类型是唯一的，足以定位。
 * 顺序如下，逐级退让，最后一级与名字完全无关：</p>
 * <ol>
 *   <li>官方名（开发环境快路径）</li>
 *   <li>SRG 名的常见前缀（{@code f_<数字>_}）—— 不行，因为不知道数字</li>
 *   <li><b>扫描类层次里所有声明字段，取类型匹配的那个</b>（生产环境的真正依靠）</li>
 * </ol>
 *
 * <p>注意：<b>绝不</b>用来读写 {@code RangedAttribute} 那种"同类型多字段"的场景——
 * 那种情况 {@code compat/AttributeCaps} 用的是"按值匹配"，比按类型更精确。</p>
 */
public final class ReflectField {

    private ReflectField() {
    }

    /**
     * 找一个字段：先按名字（开发环境），失败就按类型扫（生产环境）。
     *
     * @param owner        字段所属的类（会自动沿父类链向上找）
     * @param officialName 官方名（Mojang 映射下的名字），可以为 {@code null}
     * @param type         字段的原始类型（泛型擦除后的），必须能唯一确定
     * @return 已 {@code setAccessible(true)} 的字段，找不到返回 {@code null}
     */
    public static Field find(Class<?> owner, String officialName, Class<?> type) {
        // 1) 官方名（开发环境）
        if (officialName != null) {
            for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField(officialName);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {
                    // 继续往上看
                } catch (Throwable ignored) {
                    // setAccessible 被模块系统拒绝之类：继续退让
                }
            }
        }
        // 2) 按类型扫（生产环境的真正依靠）
        return findByNameIndependent(owner, type, null);
    }

    /**
     * 按类型找一个字段，可以再要求它与另一个字段的"取值"或"位置"匹配。
     *
     * @param owner    字段所属类
     * @param type     字段原始类型
     * @param skip     要跳过的前 N 个匹配（同类型多字段时用），{@code null} 视为 0
     * @return 已 setAccessible 的字段，找不到返回 {@code null}
     */
    public static Field findByNameIndependent(Class<?> owner, Class<?> type, Integer skip) {
        int toSkip = skip == null ? 0 : Math.max(0, skip);
        // 先在自己类里找（子类字段优先），再沿父类链
        for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
            List<Field> hits = new ArrayList<>();
            for (Field f : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (f.getType() == type) {
                    hits.add(f);
                }
            }
            if (hits.size() > toSkip) {
                Field f = hits.get(toSkip);
                try {
                    f.setAccessible(true);
                    return f;
                } catch (Throwable ignored) {
                    // 换下一个
                }
            }
        }
        return null;
    }

    /** 诊断用：把某个类（含父类）上所有非静态字段的名字与类型列出来。 */
    public static String describe(Class<?> owner, Class<?> typeFilter) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (typeFilter != null && f.getType() != typeFilter) {
                    continue;
                }
                sb.append(c.getSimpleName()).append('.').append(f.getName())
                        .append(" : ").append(f.getType().getSimpleName()).append('\n');
            }
        }
        return sb.toString();
    }
}
