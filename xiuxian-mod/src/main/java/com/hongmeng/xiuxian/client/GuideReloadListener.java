package com.hongmeng.xiuxian.client;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <b>修行录数据的重载监听器</b>：资源一就绪就把 {@code guide.json} 读进
 * {@link GuideData} 的缓存。
 *
 * <p>写成独立类而不是匿名类：{@code SimplePreparableReloadListener} 的
 * {@code prepare/apply} 签名在匿名类里容易写错（第一版就编译不过）。
 * 独立类还能让 {@link GuideData} 保持"纯数据"、不掺事件。</p>
 */
@OnlyIn(Dist.CLIENT)
public class GuideReloadListener extends SimplePreparableReloadListener<Void> {

    @Override
    protected Void prepare(ResourceManager rm, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager rm, ProfilerFiller profiler) {
        GuideData.reload();
    }
}
