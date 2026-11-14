import net.minecraft.world.entity.ai.attributes.Attributes;

/** 打印原版属性的取值范围，确认是不是被上限截断了。 */
public class AttrRangeProbe {
    public static void main(String[] args) {
        String[] names = {"MAX_HEALTH", "ATTACK_DAMAGE", "MOVEMENT_SPEED",
                "ARMOR", "ARMOR_TOUGHNESS", "ATTACK_SPEED", "KNOCKBACK_RESISTANCE"};
        net.minecraft.world.entity.ai.attributes.Attribute[] attrs = {
                Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED,
                Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.ATTACK_SPEED,
                Attributes.KNOCKBACK_RESISTANCE
        };
        System.out.println("属性                    最小值        最大值");
        System.out.println("-------------------------------------------------");
        for (int i = 0; i < attrs.length; i++) {
            var a = attrs[i];
            if (a instanceof net.minecraft.world.entity.ai.attributes.RangedAttribute ra) {
                System.out.printf("%-22s %-12.1f %-12.1f%n", names[i],
                        ra.getMinValue(), ra.getMaxValue());
            } else {
                System.out.printf("%-22s (非 RangedAttribute)%n", names[i]);
            }
        }
    }
}
