package com.hongmeng.devshim;

import net.minecraftforge.fml.common.Mod;

/**
 * 开发环境专用：GeckoLib 官方发布的 jar 里入口是 {@code software.bernie.example.GeckoLibMod}
 * （example 模块），它在 MCP named 开发环境会因 SRG 字段名不存在而崩（NoSuchFieldError f_279569_）。
 *
 * <p>这个空壳只是为了让 <b>开发服务器</b>能加载 geckolib 这个 mod 文件：
 * 脚本会把 example 包从开发用副本里剥掉，再把本类塞进去当 {@code @Mod("geckolib")} 入口。
 * <b>正式构建/发布仍用原 jar</b>，不进游戏内容。</p>
 */
@Mod("geckolib")
public final class GeckoShim {

    public GeckoShim() {
        // GeckoLib 是纯库，运行期不需要初始化动作；example 模块才需要。
    }
}
