package com.gmail.necnionch.myplugin.shaderegen.bukkit;

import com.gmail.necnionch.myplugin.shaderegen.bukkit.command.BukkitCommand;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.command.MainCommand;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.config.ShadeRegenConfig;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.listener.PlayerEventListener;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.regen.ShadeGenerator;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class ShadeRegenPlugin extends JavaPlugin implements Listener {

    private final BukkitCommand.Compat compat = BukkitCommand.compat(this);
    private final ShadeRegenConfig config = new ShadeRegenConfig(this);
    private final ShadeGenerator generator = new ShadeGenerator(getDataFolder(), config.generateServer, getLogger());

    @Override
    public void onLoad() {
        compat.init();
    }

    @Override
    public void onEnable() {
        config.load();
        getServer().getPluginManager().registerEvents(new PlayerEventListener(impl), this);
        compat.register(new MainCommand("shaderegen", impl));

        if (config.pregenTimings.startup()) {
            // サーバーの起動完了を待たずに非同期タスクを開始できないので同期的に実行を開始する
            if (impl.needsPregen())
                impl.startPregen(null);
        }
    }

    @Override
    public void onDisable() {
        compat.close();
        generator.close();
    }


    private final ShadeRegen impl = new ShadeRegen() {
        @Override
        public ShadeRegenConfig getConfig() {
            return config;
        }

        @Override
        public ShadeGenerator getGenerator() {
            return generator;
        }

        @Override
        public boolean needsPregen() {
            if (config.getRegenName() != null && !generator.getWorldNormalDirectory().exists())
                return true;
            if (config.getRegenNameNether() != null && !generator.getWorldNetherDirectory().exists())
                return true;
            return config.getRegenNameTheEnd() != null && !generator.getWorldTheEndDirectory().exists();
        }

        @Override
        public boolean startPregen(@Nullable ShadeGenerator.ProgressListener listener) {
            try {
                generator.build(listener, true);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Failed to start to regen", e);
                return false;
            }
            return true;
        }

        @Override
        public CompletableFuture<Boolean> startPregenAsync(@Nullable ShadeGenerator.ProgressListener listener) {
            CompletableFuture<Boolean> f = new CompletableFuture<>();
            getServer().getScheduler().runTaskAsynchronously(ShadeRegenPlugin.this, () -> f.complete(startPregen(listener)));
            return f;
        }
    };

    public ShadeRegen getImpl() {
        return impl;
    }

}
