package com.gmail.necnionch.myplugin.shaderegen.bukkit.world;

import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

public interface WorldManager {

    /**
     * 指定されたワールドをアンロードして、設定から削除します
     * @return 削除できたか、既に削除されているなら true を返す
     */
    CompletableFuture<Boolean> unloadAndRemoveWorld(String worldName);

    /**
     * 指定されたワールドをロードします
     * @return ワールドをロードできたら true を返す
     */
    CompletableFuture<Boolean> importAndLoadWorld(String worldName, World.Environment environment);

}
