package com.hongmeng.xiuxian.network;

import com.hongmeng.xiuxian.HongmengMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * <b>法术贴图特效包</b>：一次施法要画哪些贴图、画在哪、多大、多久。
 *
 * <p>玩家要求：「法术特效不仅仅粒子效果还有贴图表现……贴图配合粒子，
 * 不是纯贴图化。你像轨道炮就有一个激光柱那种的光波，四溢的能量粒子等，
 * 其他法术也要这种」。</p>
 *
 * <h2>为什么用贴图 + 少量粒子，而不是纯粒子</h2>
 * <p>一个贴图面片 = 1 个 draw call；同样的观感用粒子要几百个。
 * 所以"贴图为主、粒子为点缀"**既华丽又省性能** ——
 * 这也正是玩家要的"解决卡顿"。</p>
 *
 * <h2>形状表</h2>
 * <pre>
 *   BEAM_COLUMN  能量光柱（轨道炮击那种激光柱）
 *   BEAM_SOFT    柔光幕（大范围光波）
 *   SHOCK_RING   冲击波环（贴地）
 *   SHOCK_DOME   半球冲击穹顶
 *   IMPACT_FLARE 中心爆闪（八向星芒）
 *   PILLAR_DROP  从天而降的引导光柱
 *   BLADE_GOLD   金色巨剑（斩星）
 *   METEOR_ROCK  燃烧陨石
 *   CRACK_VOID   虚空裂缝
 *   SNOW_FLAKE   雪花（冰系天气）
 *   PETAL_SWIRL   花瓣旋涡（木/风）
 *   RUNE_RING    大号符文环
 * </pre>
 */
public class SpellFxPacket {

    public static final byte BEAM_COLUMN = 0;
    public static final byte BEAM_SOFT = 1;
    public static final byte SHOCK_RING = 2;
    public static final byte SHOCK_DOME = 3;
    public static final byte IMPACT_FLARE = 4;
    public static final byte PILLAR_DROP = 5;
    public static final byte BLADE_GOLD = 6;
    public static final byte METEOR_ROCK = 7;
    public static final byte CRACK_VOID = 8;
    public static final byte SNOW_FLAKE = 9;
    public static final byte PETAL_SWIRL = 10;
    public static final byte RUNE_RING = 11;
    // ---- 第 12~15 种形状（新增的立体几何原语，见 client/SpellFxMesh2）----
    /** <b>圆环体</b>：有厚度的甜甜圈（护体金光 / 领域边界 / 传送法阵）。 */
    public static final byte TORUS = 12;
    /** <b>立体晶格</b>：三轴交叉的棒条笼（禁锢 / 阵法 / 金系的锐利感）。 */
    public static final byte LATTICE = 13;
    /** <b>双螺旋</b>：两条互相缠绕的螺旋带（木/藤缠绕、雷霆、封印）。 */
    public static final byte HELIX = 14;
    /** <b>层叠环塔</b>：多圈半径递减的环叠成塔（升腾 / 威压感）。 */
    public static final byte RING_TOWER = 15;
    // ---- 第 16~19 种形状：飞剑（玩家反馈"法剑的模型也不好看"，所以单独做）----
    /**
     * <b>飞剑</b>：细长剑身 + 剑格 + 剑柄 + 剑首的四件套。
     *
     * <p>⚠ 为什么不能沿用 {@link #BLADE_GOLD}：那是<b>金色巨剑</b>，
     * 本来就是给"斩星"这种从天劈下的重击做的 —— 又宽又大。
     * 拿它当环绕的飞剑，看起来就是"一把大剑在原地转"，很笨重。
     * 飞剑要的是<b>细、长、灵巧</b>，所以单独做这个形状。</p>
     */
    public static final byte FLYING_SWORD = 16;
    /** <b>重剑</b>：宽厚带脊，适合体修/重击。 */
    public static final byte FLYING_SWORD_HEAVY = 17;
    /** <b>匕首/短刃</b>：短小，适合多把齐飞的密集剑阵。 */
    public static final byte FLYING_DAGGER = 18;
    /** <b>骨刺飞刃</b>：弯曲单刃，魔修/血修风格。 */
    public static final byte FLYING_BONEBLADE = 19;

    // ---- 第 20~24 种：碎片 / 裂纹 / 能量丝 / 陨石轨迹 / 光柱横扫 ----
    /** <b>碎片群</b>：多面体碎片向外飞散，带翻滚与重力下坠。 */
    public static final byte SHARD_BURST = 20;
    /** <b>地面裂纹</b>：贴花，不破坏方块。 */
    public static final byte GROUND_CRACK = 21;
    /** <b>能量丝</b>：多股细长管束缠绕上升。 */
    public static final byte FILAMENTS = 22;
    /** <b>陨石拖尾</b>：沿抛物线从起点的飞到终点，带拖尾。 */
    public static final byte METEOR_ARC = 23;
    /** <b>光柱横扫</b>：竖直光柱绕原点扫过。 */
    public static final byte BEAM_SWEEP = 24;

    /** <b>螺旋上升</b>：多个光球沿螺旋升空。 */
    public static final byte SPIRAL_ASCENT = 25;
    /** <b>环绕</b>：几个光球绕施法点旋转。 */
    public static final byte ORBIT = 26;
    /** <b>爬行裂纹</b>：裂纹从原点向外生长。 */
    public static final byte CREEPING_CRACK = 27;

    /** <b>阵法</b>：三重同心环 + 辐条 + 中心符文。 */
    public static final byte MANDALA = 28;

    // ---- 第 29~30 种形状：冰牢 / 血符（批次一；服务端按 byte 29 / 30 发送）----
    /**
     * <b>冰牢</b>：一圈冰晶柱从地面升起并向内合拢，顶部冰环把笼子锁上，
     * 中心悬一颗内亮的霜核。是「围合 + 闭合」的牢，不是一颗冰球。
     */
    public static final byte FROST_CAGE = 29;
    /**
     * <b>血符</b>：贴地的暗红符文法阵（同心环 + 辐条 + 缺口），
     * 外圈血环逐层上升、四周獠牙张开。灾变暗黑（F-3）只给血系用。
     */
    public static final byte BLOOD_SIGIL = 30;

    /** 形状 → 贴图文件名后缀（与 assets/hongmeng/textures/fx/ 对应）。 */
    public static String textureOf(byte shape) {
        return switch (shape) {
            case BEAM_COLUMN -> "beam_column";
            case BEAM_SOFT -> "beam_soft";
            case SHOCK_RING -> "shock_ring";
            case SHOCK_DOME -> "shock_dome";
            case IMPACT_FLARE -> "impact_flare";
            case PILLAR_DROP -> "pillar_drop";
            case BLADE_GOLD -> "blade_gold";
            case METEOR_ROCK -> "meteor_rock";
            case CRACK_VOID -> "crack_void";
            case SNOW_FLAKE -> "snow_flake";
            case PETAL_SWIRL -> "petal_swirl";
            case RUNE_RING -> "rune_ring_wide";
            // 新形状复用已有的贴图（几何是新的，贴图沿用最接近的那张）
            case TORUS -> "rune_ring_wide";
            case LATTICE -> "blade_gold";
            case HELIX -> "beam_soft";
            case RING_TOWER -> "shock_ring";
            // 飞剑系列：剑身用 blade_gold（它是为"有厚度的剑"画的），
            // 灵巧感由 client 端的几何比例决定，不靠贴图。
            case FLYING_SWORD, FLYING_SWORD_HEAVY, FLYING_DAGGER,
                 FLYING_BONEBLADE -> "blade_gold";
            // ---- 新增五形状 ----
            case SHARD_BURST -> "blade_gold";     // 金属碎片
            case GROUND_CRACK -> "ground_crack";  // 专用裂纹贴花
            case FILAMENTS -> "beam_soft";        // 柔和光带当细丝
            case METEOR_ARC -> "impact_flare";    // 核心爆闪
            case BEAM_SWEEP -> "beam_column";     // 竖向光柱
            case SPIRAL_ASCENT, ORBIT -> "impact_flare";
            case CREEPING_CRACK -> "ground_crack";
            case MANDALA -> "torus_soft";
            // 批次一：两个新形状各有一张专门画的新贴图（裸名，M1.type() 自动拼路径）
            case FROST_CAGE -> "frost_crystal";
            case BLOOD_SIGIL -> "blood_sigil";
            default -> "impact_flare";
        };
    }

    private final byte shape;
    private final double x;
    private final double y;
    private final double z;
    /** 终点（光柱/裂缝用；其他形状等于起点）。 */
    private final double ex;
    private final double ey;
    private final double ez;
    /**
     * PERF(C): 起点/终点缓存成不可变 Vec3，构造时建一次。
     *
     * <p>渲染端每帧会对同一条特效反复调 {@link #from()} / {@link #to()}
     * （本体 + 两/三层辉光叠画，每种形状里还要再取几次），原来每次调用都
     * {@code new Vec3(...)} —— 48 条特效 × 十几帧就是每帧几百个临时对象。
     * Vec3 是不可变的，缓存后语义完全等价。</p>
     */
    private final Vec3 fromPos;
    private final Vec3 toPos;
    /** 尺寸（半径或宽度）。 */
    private final float size;
    /** 长度（光柱/裂缝用）。 */
    private final float length;
    /** 寿命（tick）。 */
    private final int life;
    /** 元素序号（用于染色）。 */
    private final int element;
    /** 强度 0~2（1 = 标准）。 */
    private final float power;
    /**
     * 是否<b>不触发镜头震动/FOV</b>。
     *
     * <p>Boss 转阶段的五层爆发只保留画面冲击，不推镜头（玩家 2026-11-02 要求）；
     * 普通法术包默认 false，行为不变。</p>
     */
    private final boolean noCamera;

    public SpellFxPacket(byte shape, Vec3 from, Vec3 to, float size, float length,
                         int life, int element, float power) {
        this(shape, from, to, size, length, life, element, power, false);
    }

    public SpellFxPacket(byte shape, Vec3 from, Vec3 to, float size, float length,
                         int life, int element, float power, boolean noCamera) {
        this.shape = shape;
        this.x = from.x;
        this.y = from.y;
        this.z = from.z;
        this.ex = to == null ? from.x : to.x;
        this.ey = to == null ? from.y : to.y;
        this.ez = to == null ? from.z : to.z;
        // PERF(C): 不可变 Vec3 只建一次，渲染端每帧多次取用不再分配
        this.fromPos = from;
        this.toPos = to == null ? from : to;
        this.size = size;
        this.length = length;
        this.life = life;
        this.element = element;
        this.power = power;
        this.noCamera = noCamera;
    }

    public byte shape() {
        return shape;
    }

    public Vec3 from() {
        // PERF(C): 返回构造时缓存的不可变实例（Vec3 不可变，安全共享）
        return fromPos;
    }

    public Vec3 to() {
        // PERF(C): 同上 —— 原来每帧每次调用都 new 一个 Vec3
        return toPos;
    }

    public float size() {
        return size;
    }

    public float length() {
        return length;
    }

    public int life() {
        return life;
    }

    public int element() {
        return element;
    }

    public float power() {
        return power;
    }

    /** 这个包是否跳过镜头震动/FOV（Boss 转阶段用）。 */
    public boolean noCamera() {
        return noCamera;
    }

    public static void encode(SpellFxPacket p, FriendlyByteBuf buf) {
        buf.writeByte(p.shape);
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeDouble(p.ex);
        buf.writeDouble(p.ey);
        buf.writeDouble(p.ez);
        buf.writeFloat(p.size);
        buf.writeFloat(p.length);
        buf.writeVarInt(p.life);
        buf.writeVarInt(p.element);
        buf.writeFloat(p.power);
        buf.writeBoolean(p.noCamera);
    }

    public static SpellFxPacket decode(FriendlyByteBuf buf) {
        byte shape = buf.readByte();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        double ex = buf.readDouble();
        double ey = buf.readDouble();
        double ez = buf.readDouble();
        float size = buf.readFloat();
        float length = buf.readFloat();
        int life = buf.readVarInt();
        int element = buf.readVarInt();
        float power = buf.readFloat();
        boolean noCamera = buf.readBoolean();
        return new SpellFxPacket(shape, new Vec3(x, y, z), new Vec3(ex, ey, ez),
                size, length, life, element, power, noCamera);
    }

    public static void handle(SpellFxPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            // 客户端专用类：**不能直接引用**，否则专用服务器会崩
            //（项目里已经踩过一次：DistExecutor 会提前链接客户端类）。
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist
                    != net.minecraftforge.api.distmarker.Dist.CLIENT) {
                return;
            }
            try {
                Class.forName("com.hongmeng.xiuxian.client.ClientSpellFx")
                        .getMethod("accept", SpellFxPacket.class)
                        .invoke(null, p);
            } catch (Throwable t) {
                HongmengMod.LOGGER.debug("[hongmeng] 法术特效包处理失败：{}", t.toString());
            }
        });
        c.setPacketHandled(true);
    }
}
