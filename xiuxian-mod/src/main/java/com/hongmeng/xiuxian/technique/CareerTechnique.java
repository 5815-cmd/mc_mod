package com.hongmeng.xiuxian.technique;

import com.hongmeng.xiuxian.talent.CareerActive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>职业主动 → 法术条目</b>的桥（《07-法术内容交接表》）。
 *
 * <h2>为什么要有这一层</h2>
 * <p>20 门职业主动里 <b>18 门是新增条目</b>（追加在 {@link Technique} 末尾），
 * 另 <b>2 门「复用已有」</b>：剑修·万剑归宗 = {@code WAN_JIAN_GUI_ZONG}、
 * 体修·金刚护体 = {@code JIN_GANG_HU_TI}。两套身份要能区分：</p>
 * <ul>
 *   <li><b>18 门新增</b>：不生成秘籍、不进坊市，只能靠天赋树解锁后由服务端自动授予；</li>
 *   <li><b>2 门复用</b>：本体仍在基础 52 门池里（秘籍/坊市照旧），
 *       但作为职业主动使用时同样要过"所在分支累计 5 点"的装配校验。</li>
 * </ul>
 *
 * <p>本类只做<b>纯数据映射</b>（不引用任何 Minecraft 类型），可离线断言；
 * 真正的解锁判定在 {@code talent.Talents.activeUnlocked(...)}，服务端接线在
 * {@code technique.CareerSpellGrant}。</p>
 */
public final class CareerTechnique {

    /** Technique 枚举名 → 职业主动数据。 */
    private static final Map<String, CareerActive> BY_TECHNIQUE;

    static {
        Map<String, CareerActive> out = new LinkedHashMap<>();
        for (CareerActive active : CareerActive.values()) {
            // techniqueName 是"该主动对应的 Technique 枚举常量名"
            out.put(active.techniqueName(), active);
        }
        BY_TECHNIQUE = Collections.unmodifiableMap(out);
    }

    private CareerTechnique() {
    }

    /** 这门功法是不是「职业主动」（20 门里的任意一门，含 2 门复用）。 */
    public static boolean isCareerGated(Technique technique) {
        return technique != null && BY_TECHNIQUE.containsKey(technique.name());
    }

    /**
     * 是不是「只能天赋解锁」的新增主动（18 门）——
     * 这类功法<b>不生成秘籍、不进坊市、图鉴标"职业功法"</b>。
     */
    public static boolean isUnlockOnly(Technique technique) {
        if (technique == null) {
            return false;
        }
        CareerActive active = BY_TECHNIQUE.get(technique.name());
        return active != null && !active.reusesExisting();
    }

    /** 按名字判断是否"只能天赋解锁"（注册表/图鉴在还没有 Technique 值时也能用）。 */
    public static boolean isUnlockOnlyName(String techniqueName) {
        if (techniqueName == null) {
            return false;
        }
        CareerActive active = BY_TECHNIQUE.get(techniqueName);
        return active != null && !active.reusesExisting();
    }

    /** 取对应的职业主动数据；不是职业主动返回 {@code null}。 */
    public static CareerActive activeOf(Technique technique) {
        return technique == null ? null : BY_TECHNIQUE.get(technique.name());
    }

    /** 按名字取职业主动数据。 */
    public static CareerActive activeOfName(String techniqueName) {
        return techniqueName == null ? null : BY_TECHNIQUE.get(techniqueName);
    }

    /** 18 门新增主动的 Technique 枚举名（顺序与 {@link CareerActive} 声明顺序一致）。 */
    public static List<String> unlockOnlyNames() {
        List<String> out = new ArrayList<>();
        for (CareerActive active : CareerActive.values()) {
            if (!active.reusesExisting()) {
                out.add(active.techniqueName());
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** 2 门复用已有功法的名字（基础池身份仍保留）。 */
    public static List<String> reusedNames() {
        List<String> out = new ArrayList<>();
        for (CareerActive active : CareerActive.values()) {
            if (active.reusesExisting()) {
                out.add(active.techniqueName());
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** 已映射的条目总数（20）。 */
    public static int size() {
        return BY_TECHNIQUE.size();
    }
}
