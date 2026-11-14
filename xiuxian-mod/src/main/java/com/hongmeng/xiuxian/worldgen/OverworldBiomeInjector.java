package com.hongmeng.xiuxian.worldgen;

import com.hongmeng.xiuxian.HongmengMod;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 把 {@link BiomePlan} 里的三个新群系<b>塞进主世界的 multi-noise 群系源</b>。
 *
 * <h2>为什么需要这个类（而不是纯 JSON）</h2>
 * <p>Forge 1.20.1 没有 TerraBlender 那种"往已有维度追加群系"的接口：
 * {@code forge:biome_modifier} 只能往<b>已有群系</b>里加特征/刷怪，不能新增群系。
 * 纯数据包的做法是覆盖 {@code data/minecraft/dimension/overworld.json}，
 * 把整张 61 条的原版气候点位表抄一遍——那既碰别人的维度文件，
 * 又必须逐字抄对原版表（抄错一位，全世界的地形与群系分布就悄悄变了），
 * 而且会和别的群系模组打架。</p>
 *
 * <p>这里换一条更稳的路：<b>启动时读出现成的原版点位表，只把其中几个点位的群系换掉</b>。</p>
 *
 * <h2>做法：接管（不是插入）</h2>
 * <ol>
 *   <li>从主世界的 {@link MultiNoiseBiomeSource} 里读出全部
 *       {@code (气候点位, 群系)} 对（约 61 条，原版 {@code OverworldBiomeBuilder} 生成的）；</li>
 *   <li>按 {@link BiomeSpec#donors()} 的优先级挑一个原版群系，要求它在表里
 *       <b>至少还有 {@link BiomePlan#MIN_DONOR_POINTS} 个点位</b>（这样它不会因此消失），
 *       然后把它<b>体积最大</b>的那个陆地点位换成新群系；</li>
 *   <li>用换好的表重建 {@code Climate.ParameterList}，写回群系源。</li>
 * </ol>
 *
 * <p><b>为什么是"换"而不是"加"</b>：multi-noise 的查询是"找包含该气候坐标的点位，
 * 并列时由 R 树遍历顺序决定"。新加一个和原版重叠的点位，会得到<b>斑块状</b>的
 * 花屏群系（一会儿新群系一会儿原版）；而替换点位保持了点位集合与顺序不变，
 * R 树结构与原版逐字节一致，新群系出现的位置就是被接管点位原本出现的位置——
 * 一整片、连贯、必然找得到。</p>
 *
 * <h2>代价（写清楚）</h2>
 * <p>被接管的那个原版群系会少一个气候点位（例如森林少一片纬度带）。
 * 这是"不抄原版表"必须付的代价，也是它比抄表安全得多的原因。
 * 三个新群系共接管 3 个点位，占原版 61 条里的 3 条。</p>
 *
 * <h2>健壮性</h2>
 * <p>整个过程包在 try/catch 里：任何一步失败（群系源不是 multi-noise、
 * 反射拿不到字段、群系没注册上）都只<b>打一条警告日志</b>，绝不让服务器起不来。
 * 反射用 {@link ObfuscationReflectionHelper}（生产环境字段名是 SRG，直接写
 * {@code getDeclaredField("parameters")} 会在线上失效）。</p>
 *
 * <p>插入完成时必须同时刷新两个<b>惰性缓存</b>，否则新群系的特征（灵植）不会生成：</p>
 * <ul>
 *   <li>{@code BiomeSource.possibleBiomes}（可能群系集合）；</li>
 *   <li>{@code ChunkGenerator.featuresPerStep}（每个装饰阶段的全量特征表，
 *       由 {@code possibleBiomes} 推出）。</li>
 * </ul>
 */
public final class OverworldBiomeInjector {

    /** 只在主世界注入（四个秘境保持原版群系分布，不掺新群系）。 */
    private static final Set<ResourceKey<Level>> INJECTED = new LinkedHashSet<>();

    /** 需要事件总线实例化。 */
    public OverworldBiomeInjector() {
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return;
        }
        if (INJECTED.contains(level.dimension())) {
            return;
        }
        try {
            if (inject(level)) {
                INJECTED.add(level.dimension());
            }
        } catch (Throwable t) {
            HongmengMod.LOGGER.warn("[hongmeng] 新群系注入失败（不影响游戏，只是找不到这三个群系）：{}",
                    t.toString());
        }
    }

    /** 真正干活的地方。返回 true 表示注入成功。 */
    private static boolean inject(ServerLevel level) throws ReflectiveOperationException {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        BiomeSource source = generator.getBiomeSource();
        if (!(source instanceof MultiNoiseBiomeSource noiseSource)) {
            HongmengMod.LOGGER.warn("[hongmeng] 主世界的群系源不是 multi-noise（{}），跳过新群系注入。",
                    source.getClass().getName());
            return false;
        }

        Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);

        // ---- 1. 读出现成的点位表 ----
        Field parametersField = ReflectField.find(
                MultiNoiseBiomeSource.class, "parameters", com.mojang.datafixers.util.Either.class);
        parametersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Either<Climate.ParameterList<Holder<Biome>>,
                Holder<MultiNoiseBiomeSourceParameterList>> either =
                (Either<Climate.ParameterList<Holder<Biome>>,
                        Holder<MultiNoiseBiomeSourceParameterList>>) parametersField.get(noiseSource);
        if (either == null) {
            HongmengMod.LOGGER.warn("[hongmeng] 群系源里没有点位表，跳过新群系注入。");
            return false;
        }
        Climate.ParameterList<Holder<Biome>> current =
                either.map(list -> list, holder -> holder.value().parameters());
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> values =
                new ArrayList<>(current.values());

        // ---- 2. 每个新群系挑一个点位接管 ----
        Map<String, Holder<Biome>> holders = new HashMap<>();
        for (BiomeSpec spec : BiomePlan.ALL) {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME,
                    new ResourceLocation(HongmengMod.MOD_ID, spec.id()));
            Optional<Holder.Reference<Biome>> holder = biomes.getHolder(key);
            if (holder.isEmpty()) {
                HongmengMod.LOGGER.warn("[hongmeng] 找不到群系 {}（data/hongmeng/worldgen/biome/{}.json "
                        + "没加载上？），跳过它的注入。", key.location(), spec.id());
                continue;
            }
            holders.put(spec.id(), holder.get());
        }
        if (holders.isEmpty()) {
            return false;
        }

        List<Integer> claimed = new ArrayList<>();
        boolean changed = false;
        for (BiomeSpec spec : BiomePlan.ALL) {
            Holder<Biome> holder = holders.get(spec.id());
            if (holder == null) {
                continue;
            }
            int index = pickDonorIndex(values, spec, claimed);
            if (index < 0) {
                HongmengMod.LOGGER.warn("[hongmeng] {} 找不到可接管的原版点位（备选链 {} 都没匹配上），"
                        + "这个群系这次不会出现。", spec.cn(), spec.donors());
                continue;
            }
            Pair<Climate.ParameterPoint, Holder<Biome>> old = values.get(index);
            values.set(index, Pair.of(old.getFirst(), holder));
            claimed.add(index);
            changed = true;
            HongmengMod.LOGGER.info("[hongmeng] 群系注入：{}（{}）接管了 {} 的第 {} 个气候点位（该群系原有 {} 个点位，"
                            + "接管后还剩 {} 个）{}",
                    spec.cn(), spec.fullId(), biomeId(old.getSecond()), index + 1,
                    countPoints(values, biomeId(old.getSecond())) + 1,
                    countPoints(values, biomeId(old.getSecond())),
                    describe(old.getFirst()));
        }
        if (!changed) {
            return false;
        }

        Climate.ParameterList<Holder<Biome>> replacement = new Climate.ParameterList<>(values);
        parametersField.set(noiseSource, Either.left(replacement));

        // ---- 3. 刷新两个惰性缓存（否则新群系的灵植不会生成）----
        Set<Holder<Biome>> possible = new LinkedHashSet<>();
        for (Pair<Climate.ParameterPoint, Holder<Biome>> pair : values) {
            possible.add(pair.getSecond());
        }
        Set<Holder<Biome>> frozen = java.util.Collections.unmodifiableSet(possible);

        Field possibleField = ReflectField.find(BiomeSource.class, "possibleBiomes", java.util.function.Supplier.class);
        possibleField.setAccessible(true);
        possibleField.set(source, (Supplier<Set<Holder<Biome>>>) () -> frozen);

        Field featuresField = ReflectField.find(ChunkGenerator.class, "featuresPerStep", java.util.function.Supplier.class);
        featuresField.setAccessible(true);
        featuresField.set(generator, (Supplier<List<FeatureSorter.StepFeatureData>>)
                com.google.common.base.Suppliers.memoize(() -> FeatureSorter.buildFeaturesPerStep(
                        List.copyOf(frozen),
                        h -> generator.getBiomeGenerationSettings(h).features(),
                        true)));

        HongmengMod.LOGGER.info("[hongmeng] 新群系注入完成：{} 个群系，主世界点位表 {} 条 → {} 条",
                holders.size(), current.values().size(), values.size());
        return true;
    }

    /**
     * 挑一个可接管的点位。
     *
     * <p>规则（按顺序）：</p>
     * <ol>
     *   <li>沿备选链找第一个<b>点位够多</b>（≥ {@link BiomePlan#MIN_DONOR_POINTS}）的原版群系，
     *       保证它不会因为被接管而消失；</li>
     *   <li>在它的点位里只考虑<b>地表</b>点位（深度区间包含 0，排除洞穴群系）；</li>
     *   <li>取气候体积最大的那个（体积大 = 一定找得到，不会变成针尖大的稀有群系）；</li>
     *   <li>已经被别的新群系接管过的点位跳过；</li>
     *   <li>都不满足时退一步：允许只占 1 个点位的原版群系（宁可它变稀有，也要让新群系出现）。</li>
     * </ol>
     *
     * @return 点位下标；找不到返回 -1
     */
    private static int pickDonorIndex(List<Pair<Climate.ParameterPoint, Holder<Biome>>> values,
                                      BiomeSpec spec, List<Integer> claimed) {
        int fallback = -1;
        for (String donor : spec.donors()) {
            List<Integer> candidates = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                if (claimed.contains(i) || !donor.equals(biomeId(values.get(i).getSecond()))) {
                    continue;
                }
                if (isSurface(values.get(i).getFirst())) {
                    candidates.add(i);
                }
            }
            if (candidates.isEmpty()) {
                continue;
            }
            boolean richEnough = candidates.size() >= BiomePlan.MIN_DONOR_POINTS;
            int best = candidates.get(0);
            for (int i : candidates) {
                if (volume(values.get(i).getFirst()) > volume(values.get(best).getFirst())) {
                    best = i;
                }
            }
            if (richEnough) {
                return best;
            }
            if (fallback < 0) {
                fallback = best;
            }
        }
        return fallback;
    }

    /** 地表点位：深度区间包含 0（洞穴群系的深度区间不含 0）。 */
    private static boolean isSurface(Climate.ParameterPoint point) {
        return point.depth().min() <= 0L && point.depth().max() >= 0L;
    }

    /** 气候体积（六维跨度之和，单位是原版的 1/10000）。越大越"一定找得到"。 */
    private static long volume(Climate.ParameterPoint point) {
        return span(point.temperature()) + span(point.humidity())
                + span(point.continentalness()) + span(point.erosion())
                + span(point.depth()) + span(point.weirdness());
    }

    private static long span(Climate.Parameter p) {
        return Math.max(0L, p.max() - p.min());
    }

    /** 群系注册名（日志用）。 */
    private static String biomeId(Holder<Biome> holder) {
        return holder.unwrapKey().map(key -> key.location().toString()).orElse("?");
    }

    /** 某个群系在点位表里占多少个气候点位（日志用：确认被接管的原版群系还剩几个）。 */
    private static int countPoints(List<Pair<Climate.ParameterPoint, Holder<Biome>>> values,
                                   String biomeId) {
        int n = 0;
        for (Pair<Climate.ParameterPoint, Holder<Biome>> pair : values) {
            if (biomeId.equals(biomeId(pair.getSecond()))) {
                n++;
            }
        }
        return n;
    }

    /** 点位文本（日志用，方便事后核对到底接管了哪一片气候）。 */
    private static String describe(Climate.ParameterPoint p) {
        return String.format(java.util.Locale.ROOT,
                "[温度 %s 湿度 %s 大陆 %s 侵蚀 %s 深度 %s 怪异 %s]",
                range(p.temperature()), range(p.humidity()), range(p.continentalness()),
                range(p.erosion()), range(p.depth()), range(p.weirdness()));
    }

    private static String range(Climate.Parameter p) {
        return String.format(java.util.Locale.ROOT, "%.2f~%.2f",
                p.min() / 10000.0D, p.max() / 10000.0D);
    }

    /** 已经注入过哪些维度（自测用）。 */
    public static Set<ResourceKey<Level>> injectedDimensions() {
        return java.util.Collections.unmodifiableSet(INJECTED);
    }
}
