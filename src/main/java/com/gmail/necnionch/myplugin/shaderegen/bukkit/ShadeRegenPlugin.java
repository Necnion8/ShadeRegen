package com.gmail.necnionch.myplugin.shaderegen.bukkit;

import com.gmail.necnionch.myplugin.shaderegen.bukkit.command.BukkitCommand;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.command.MainCommand;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.config.ShadeRegenConfig;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.listener.PlayerEventListener;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.regen.ShadeGenerator;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.util.FileUtil;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.world.WorldManager;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.world.impl.MV2_4_Impl;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.world.impl.MV5_Impl;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class ShadeRegenPlugin extends JavaPlugin implements Listener {

    private final BukkitCommand.Compat compat = BukkitCommand.compat(this);
    private final ShadeRegenConfig config = new ShadeRegenConfig(this);
    private final ShadeGenerator generator = new ShadeGenerator(getDataFolder(), config.generateServer, getLogger());
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        compat.init();
        if (!setupMultiverse())
            return;

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
        worldManager = null;
    }

    private boolean setupMultiverse() {
        worldManager = null;
        try {
            worldManager = MV5_Impl.init();
        } catch (RuntimeException e) {
            try {
                worldManager = MV2_4_Impl.init();
            } catch (RuntimeException ex) {
                getLogger().log(Level.SEVERE, "Failed to hook to Multiverse-Core v2-5 API", e);
                setEnabled(false);
                return false;
            }
        }
        return true;
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

        @Override
        public CompletableFuture<Boolean> regenerate() {
            String normalName = config.getRegenName();
            String netherName = config.getRegenNameNether();
            String theEndName = config.getRegenNameTheEnd();
            List<String> worldNames = new ArrayList<>();
            if (normalName != null)
                worldNames.add(normalName);
            if (netherName != null)
                worldNames.add(netherName);
            if (theEndName != null)
                worldNames.add(theEndName);
            if (worldNames.isEmpty()) {
                return CompletableFuture.completedFuture(true);
            }

            getLogger().info("Start Regenerating: " + String.join(", ", worldNames));

            AtomicBoolean fails = new AtomicBoolean(false);
            return CompletableFuture.allOf(
                    // 1. アンロード
                    worldNames.stream()
                            .map(name -> worldManager.unloadAndRemoveWorld(name).whenComplete((r, err) -> {
                                if (err != null) {
                                    getLogger().log(Level.SEVERE, "Failed to unload/remove world: " + name, err);
                                    fails.set(true);
                                } else if (!r) {
                                    getLogger().warning("Failed to unload/remove world: " + name);
                                    fails.set(true);
                                }
                            })).toArray(CompletableFuture[]::new))
                    .whenComplete((v, err) -> {
                        if (err != null) {
                            throw new RuntimeException("Failed to unload/remove worlds", err);
                        } else if (fails.get()) {
                            throw new RuntimeException("Failed to unload/remove worlds");
                        }
                    })
                    .thenRunAsync(() -> {
                        // 2. 必要なら生成を非同期開始
                        CompletableFuture<Void> pregenFuture = new CompletableFuture<>();
                        if (needsPregen()) {
                            startPregen(status -> {
                                if (status.isExited()) {
                                    if (ShadeGenerator.Status.ERROR.equals(status)) {
                                        pregenFuture.completeExceptionally(new RuntimeException("Failed to pregen"));
                                    } else {
                                        pregenFuture.complete(null);
                                    }
                                }
                            });
                        } else {
                            pregenFuture.complete(null);
                        }

                        // 3. 古い .old を削除 ＆ 現在のフォルダを .old へ
                        for (String worldName : worldNames) {
                            File old = new File(worldName + ".old");
                            try {
                                FileUtil.deleteDirectory(old.toPath());
                            } catch (IOException e) {
                                getLogger().log(Level.WARNING, "Failed to delete world: " + worldName, e);
                            }

                            if (!new File(worldName).renameTo(old)) {
                                getLogger().warning("Failed to rename world: " + worldName + " to " + old);
                            }
                        }

                        // 4. pregenを待機
                        pregenFuture.join();

                        // 5. 生成済みワールドを定位置に配置
                        if (normalName != null) {
                            File preDir = generator.getWorldNormalDirectory();
                            if (!preDir.renameTo(new File(normalName))) {
                                getLogger().warning("Failed to move world: " + preDir + " to " + normalName);
                                worldNames.remove(normalName);
                            }
                        }
                        if (netherName != null) {
                            File preDir = generator.getWorldNetherDirectory();
                            if (!preDir.renameTo(new File(netherName))) {
                                getLogger().warning("Failed to move world: " + preDir + " to " + netherName);
                                worldNames.remove(netherName);
                            }
                        }
                        if (theEndName != null) {
                            File preDir = generator.getWorldTheEndDirectory();
                            if (!preDir.renameTo(new File(theEndName))) {
                                getLogger().warning("Failed to move world: " + preDir + " to " + theEndName);
                                worldNames.remove(theEndName);
                            }
                        }

                        if (worldNames.isEmpty()) {
                            throw new RuntimeException("Failed to move worlds");
                        }

                    }, asyncExecutor)

                    .thenComposeAsync(v -> {
                        // 6. ロード
                        return CompletableFuture.allOf(worldNames.stream().map(worldName -> {
                            World.Environment environment;
                            if (worldName.equals(netherName)) {
                                environment = World.Environment.NETHER;
                            } else if (worldName.equals(theEndName)) {
                                environment = World.Environment.THE_END;
                            } else {
                                environment = World.Environment.NORMAL;
                            }
                            return worldManager.importAndLoadWorld(worldName, environment).whenComplete((r, err) -> {
                                if (err != null) {
                                    getLogger().log(Level.SEVERE, "Failed to load world: " + worldName, err);
                                } else if (!r) {
                                    getLogger().warning("Failed to load world: " + worldName);
                                }
                            });
                        }).toArray(CompletableFuture[]::new));

                    }, normalExecutor)

                    .handle((v, err) -> {
                        if (err != null) {
                            getLogger().log(Level.SEVERE, "Failed to regenerate", err);
                            return false;
                        }

                        if (config.pregenTimings.worldRegen()) {
                            startPregenAsync(null);
                        }
                        return true;
                    });
        }
    };

    public ShadeRegen getImpl() {
        return impl;
    }

    private final Executor normalExecutor = task -> getServer().getScheduler().runTask(ShadeRegenPlugin.this, task);
    private final Executor asyncExecutor = task -> getServer().getScheduler().runTaskAsynchronously(ShadeRegenPlugin.this, task);

}
