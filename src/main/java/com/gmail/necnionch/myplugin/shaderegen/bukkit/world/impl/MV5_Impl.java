package com.gmail.necnionch.myplugin.shaderegen.bukkit.world.impl;

import org.bukkit.World;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.ImportWorldOptions;

import java.util.concurrent.CompletableFuture;

public class MV5_Impl implements com.gmail.necnionch.myplugin.shaderegen.bukkit.world.WorldManager {

    private final WorldManager worlds;

    public MV5_Impl(MultiverseCoreApi coreApi) {
        this.worlds = coreApi.getWorldManager();
    }

    public static MV5_Impl init() throws RuntimeException {
        try {
            Class.forName("org.mvplugins.multiverse.core.MultiverseCoreApi");
            return new MV5_Impl(MultiverseCoreApi.get());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load Multiverse-Core v5: Class not found");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Multiverse-Core v5: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Boolean> unloadAndRemoveWorld(String worldName) {
        CompletableFuture<Boolean> f = new CompletableFuture<>();
        worlds.removeWorld(worldName)
                .thenRun(() -> f.complete(!worlds.isLoadedWorld(worldName)));
        return f;
    }

    @Override
    public CompletableFuture<Boolean> importAndLoadWorld(String worldName, World.Environment environment) {
        CompletableFuture<Boolean> f = new CompletableFuture<>();
        worlds.importWorld(ImportWorldOptions.worldName(worldName).environment(environment))
                .onSuccess(world -> {
                    f.complete(true);
                })
                .onFailure(() -> f.complete(false));
        return f;
    }

}
