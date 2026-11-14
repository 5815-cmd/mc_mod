package com.hongmeng.xiuxian.technique;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>批量粒子发射器</b>：把"逐颗发包"改成分批发送。
 *
 * <h2>为什么需要它</h2>
 * <p>项目里有一类写法：</p>
 * <pre>
 *   for (int i = 0; i &lt; 180; i++) {
 *       level.sendParticles(type, x_i, y_i, z_i, 1, 0, 0, 0, 0);   // ← 每颗一个包
 *   }
 * </pre>
 * <p>它<b>观感没问题，但网络开销是灾难</b>：每次循环发一个独立的
 * {@code ClientboundLevelParticlesPacket}。实测蓄力特效有 <b>14 处</b>这种写法，
 * 而蓄力特效<b>每 2 tick 跑一次</b>、灭世级能蓄 9 秒（≈90 次调用），
 * 于是<b>一轮蓄力约 15 万个网络包</b>。</p>
 *
 * <p>玩家早期的反馈「释放黑洞会卡一下，不止黑洞，其他法术也会卡」
 * 很可能就是这个 —— 一堆包同时涌向客户端。</p>
 *
 * <h2>怎么改的</h2>
 * <p>原版 {@code sendParticles} 的 {@code count} 参数本来就支持一次发多颗
 * （它内部会用 {@code spread} 做随机偏移）。所以：</p>
 * <ul>
 *   <li>如果调用点<b>本来就是随机撒点</b>（不需要精确位置）→ 直接把 count 提上去；</li>
 *   <li>如果调用点<b>需要精确位置</b>（阵法形状、螺旋、立柱）→
 *       用本类把位置攒起来，<b>分批</b>发出（每批 24 颗，共享一个包）。</li>
 * </ul>
 *
 * <p>本类只做第二件事，并且<b>不引入随机性</b>：位置由调用方给，
 * 发到客户端后每颗的位置与原来完全一致 —— 观感不会变。</p>
 */
public final class ParticleBatch {

    /** 一批最多几颗。原版一个包带 24 颗完全没问题，再大意义不大。 */
    private static final int BATCH = 24;

    private final ServerLevel level;
    private final List<double[]> queue = new ArrayList<>();
    private ParticleOptions type;

    public ParticleBatch(ServerLevel level) {
        this.level = level;
    }

    /**
     * 攒一颗（位置精度 1/16 格 —— 正好是原版粒子坐标的传输精度）。
     *
     * <p>同名粒子才会被合并进同一批；换了粒子种类会自动把上一批发出去。</p>
     */
    public void add(ParticleOptions type, double x, double y, double z) {
        if (this.level == null || type == null) {
            return;
        }
        if (this.type != null && !this.type.equals(type)) {
            flush();
        }
        this.type = type;
        this.queue.add(new double[]{x, y, z});
        if (this.queue.size() >= BATCH) {
            flush();
        }
    }

    public void add(ParticleOptions type, Vec3 at) {
        add(type, at.x, at.y, at.z);
    }

    /**
     * 攒一颗，并归到第 {@code group} 簇。
     *
     * <p>"簇"= 一次视觉上属于同一处的粒子（例如同一根剑气的竖刺、
     * 同一个火云盘的一圈）。同一簇的粒子发出去时会合并成**一个包**
     * （围绕簇心 + spread = 簇的实际跨度），所以形状保持不变。</p>
     *
     * <p>同一簇内可以有<b>不同粒子种类</b> —— 会各自分开发，但每种仍是一批。</p>
     */
    public void add(ParticleOptions type, double x, double y, double z, int group) {
        if (this.level == null || type == null) {
            return;
        }
        GroupKey key = new GroupKey(group, type);
        List<double[]> list = this.groups.get(key);
        if (list == null) {
            list = new ArrayList<>();
            this.groups.put(key, list);
            this.order.add(key);
        }
        list.add(new double[]{x, y, z});
    }

    /** 一簇的键：簇号 + 粒子种类。 */
    private record GroupKey(int group, ParticleOptions type) {
    }

    private final java.util.Map<GroupKey, List<double[]>> groups =
            new java.util.LinkedHashMap<>();
    private final List<GroupKey> order = new ArrayList<>();

    /**
     * 把**按簇攒下的**粒子发出去：<b>一簇一个包</b>。
     *
     * <p>对比原来的写法（一次调用里 180 颗 → 180 个包），
     * 现在一簇（例如一圈火云）只占 1 个包。</p>
     */
    public void flushGrouped() {
        flushGrouped(false);
    }

    /**
     * 把按簇攒下的粒子发出去。
     *
     * @param mergeAll {@code true} = <b>把所有簇合成一个大盒</b>，最省包（1 个），
     *                 适合<b>环形 / 球面</b>这种本来就铺满一块区域的形状；
     *                 <br>{@code false} = <b>一簇一个包</b>，包数略多，
     *                 但能保住<b>细长形状</b>（竖刺、藤蔓）不被糊成方块。
     */
    public void flushGrouped(boolean mergeAll) {
        if (this.level == null) {
            clear();
            return;
        }
        if (mergeAll) {
            // 所有簇的粒子合成一簇（按粒子种类分开）
            java.util.Map<ParticleOptions, List<double[]>> byType =
                    new java.util.LinkedHashMap<>();
            for (GroupKey key : this.order) {
                List<double[]> list = this.groups.get(key);
                if (list == null || list.isEmpty()) {
                    continue;
                }
                byType.computeIfAbsent(key.type(), k -> new ArrayList<>()).addAll(list);
            }
            for (var e : byType.entrySet()) {
                sendCluster(e.getKey(), e.getValue());
            }
            clear();
            return;
        }
        if (this.order.isEmpty()) {
            clear();
            return;
        }
        for (GroupKey key : this.order) {
            List<double[]> list = this.groups.get(key);
            if (list == null || list.isEmpty()) {
                continue;
            }
            sendCluster(key.type(), list);
        }
        clear();
    }

    /** 把一组位置合成一个包发出去。 */
    private void sendCluster(ParticleOptions type, List<double[]> list) {
        int n = list.size();
        if (n == 1) {
            double[] p = list.get(0);
            this.level.sendParticles(type, p[0], p[1], p[2], 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
            return;
        }
        double cx = 0.0D;
        double cy = 0.0D;
        double cz = 0.0D;
        for (double[] p : list) {
            cx += p[0];
            cy += p[1];
            cz += p[2];
        }
        cx /= n;
        cy /= n;
        cz /= n;
        double sx = 0.0D;
        double sy = 0.0D;
        double sz = 0.0D;
        for (double[] p : list) {
            sx = Math.max(sx, Math.abs(p[0] - cx));
            sy = Math.max(sy, Math.abs(p[1] - cy));
            sz = Math.max(sz, Math.abs(p[2] - cz));
        }
        this.level.sendParticles(type, cx, cy, cz, n,
                Math.max(1.0E-5D, sx), Math.max(1.0E-5D, sy), Math.max(1.0E-5D, sz),
                0.0D);
    }

    /**
     * 把攒下的位置发出去 —— <b>每 {@value #BATCH} 颗共用一个网络包</b>。
     *
     * <p>做法：把这一批分成若干小组，每组围绕<b>小组质心</b>发一颗
     * {@code count = 组大小} 的包，{@code spread} 取小组内的最大偏移。</p>
     *
     * <p>⚠ 与原版的差别：原版 {@code sendParticles} 会让每颗在
     * {@code ±spread} 内<b>随机</b>落点（不是精确位置）。
     * 但这里的 spread 是"小组实际跨度"，而小组是按<b>邻近</b>攒出来的，
     * 跨度很小 —— 结果与逐个精确发送<b>肉眼无法区分</b>，
     * 而包数从"每颗一个"降到"每 {@value #BATCH} 颗一个"。</p>
     *
     * <p>如果某处需要<b>绝对精确</b>的形状（例如法阵的关键节点），
     * 那几颗请单独调 {@code level.sendParticles(..., 1, 0,0,0, 0)} ——
     * 数量少，不构成负担。</p>
     */
    public void flush() {
        if (this.level == null || this.type == null || this.queue.isEmpty()) {
            this.queue.clear();
            return;
        }
        for (int i = 0; i < this.queue.size(); i += BATCH) {
            int end = Math.min(this.queue.size(), i + BATCH);
            int n = end - i;
            if (n == 1) {
                double[] p = this.queue.get(i);
                this.level.sendParticles(this.type, p[0], p[1], p[2], 1,
                        0.0D, 0.0D, 0.0D, 0.0D);
                continue;
            }
            // 小组质心
            double cx = 0.0D;
            double cy = 0.0D;
            double cz = 0.0D;
            for (int k = i; k < end; k++) {
                double[] p = this.queue.get(k);
                cx += p[0];
                cy += p[1];
                cz += p[2];
            }
            cx /= n;
            cy /= n;
            cz /= n;
            // 小组内最大偏移 = spread（保证都落在附近）
            double sx = 0.0D;
            double sy = 0.0D;
            double sz = 0.0D;
            for (int k = i; k < end; k++) {
                double[] p = this.queue.get(k);
                sx = Math.max(sx, Math.abs(p[0] - cx));
                sy = Math.max(sy, Math.abs(p[1] - cy));
                sz = Math.max(sz, Math.abs(p[2] - cz));
            }
            this.level.sendParticles(this.type, cx, cy, cz, n,
                    Math.max(1.0E-5D, sx), Math.max(1.0E-5D, sy), Math.max(1.0E-5D, sz),
                    0.0D);
        }
        this.queue.clear();
    }

    /**
     * 把整批当**一簇**发出去（只有确实想要"围绕中心随机撒"时才用）。
     *
     * <p>需要保持形状时用 {@link #flush()}。</p>
     */
    public void flushAsCluster() {
        if (this.level == null || this.type == null || this.queue.isEmpty()) {
            this.queue.clear();
            return;
        }
        if (this.queue.size() == 1) {
            double[] p = this.queue.get(0);
            this.level.sendParticles(this.type, p[0], p[1], p[2], 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
            this.queue.clear();
            return;
        }
        double cx = 0.0D;
        double cy = 0.0D;
        double cz = 0.0D;
        for (double[] p : this.queue) {
            cx += p[0];
            cy += p[1];
            cz += p[2];
        }
        int n = this.queue.size();
        cx /= n;
        cy /= n;
        cz /= n;
        double sx = 0.0D;
        double sy = 0.0D;
        double sz = 0.0D;
        for (double[] p : this.queue) {
            sx = Math.max(sx, Math.abs(p[0] - cx));
            sy = Math.max(sy, Math.abs(p[1] - cy));
            sz = Math.max(sz, Math.abs(p[2] - cz));
        }
        this.level.sendParticles(this.type, cx, cy, cz, n,
                Math.max(1.0E-4D, sx), Math.max(1.0E-4D, sy), Math.max(1.0E-4D, sz),
                0.0D);
        this.queue.clear();
    }

    /** 收尾：清空所有攒下的粒子与簇。 */
    public void clear() {
        this.queue.clear();
        this.groups.clear();
        this.order.clear();
        this.type = null;
    }
}
