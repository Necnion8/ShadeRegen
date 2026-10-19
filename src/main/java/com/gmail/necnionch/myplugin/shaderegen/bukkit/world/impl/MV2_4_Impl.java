package com.gmail.necnionch.myplugin.shaderegen.bukkit.world.impl;

import com.gmail.necnionch.myplugin.shaderegen.bukkit.world.WorldManager;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public class MV2_4_Impl implements WorldManager {

    private final MVWorldManager worlds;

    public MV2_4_Impl(MVWorldManager worlds) {
        this.worlds = worlds;
    }

    public static MV2_4_Impl init() throws RuntimeException {
        try {
            Class.forName("com.onarandombox.MultiverseCore.MultiverseCore");
            return new MV2_4_Impl(JavaPlugin.getPlugin(MultiverseCore.class).getMVWorldManager());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load Multiverse-Core v2-4: Class not found");
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load Multiverse-Core v2-4", e);
        }
    }

    @Override
    public CompletableFuture<Boolean> unloadAndRemoveWorld(String worldName) {
        worlds.deleteWorld(worldName, true, false);
        return CompletableFuture.completedFuture(!worlds.isMVWorld(worldName));
    }

    @Override
    public CompletableFuture<Boolean> importAndLoadWorld(String worldName, World.Environment environment) {
        return CompletableFuture.completedFuture(worlds.addWorld(worldName, environment, null, null, null, null));
    }

}
