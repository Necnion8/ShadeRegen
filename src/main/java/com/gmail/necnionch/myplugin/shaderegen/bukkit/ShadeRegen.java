package com.gmail.necnionch.myplugin.shaderegen.bukkit;

import com.gmail.necnionch.myplugin.shaderegen.bukkit.config.ShadeRegenConfig;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.regen.ShadeGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface ShadeRegen {

    static ShadeRegen get() {
        return ShadeRegenPlugin.getPlugin(ShadeRegenPlugin.class).getImpl();
    }


    ShadeRegenConfig getConfig();

    ShadeGenerator getGenerator();

    /**
     * 再生成ワールドの設定に基づき、事前生成が必要なら true を返します
     */
    boolean needsPregen();

    /**
     * 生成サーバーを起動して、生成済みワールドを準備します
     * また、サーバーの起動前に処理すべきファイルをこの実行スレッドで実行します。必要なら非同期スレッドで実行してください。
     * 実行を開始したら true を返します
     */
    boolean startPregen(@Nullable ShadeGenerator.ProgressListener listener);

    /**
     * @see #startPregen(ShadeGenerator.ProgressListener)
     */
    CompletableFuture<Boolean> startPregenAsync(@Nullable ShadeGenerator.ProgressListener listener);

    /**
     * 連携しているマルチワールドプラグインを介して、設定ワールドを再生成します
     */
    CompletableFuture<Boolean> regenerate();

}
