package com.gmail.necnionch.myplugin.shaderegen.bukkit.listener;

import com.gmail.necnionch.myplugin.shaderegen.bukkit.ShadeRegen;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventListener implements Listener {

    private final ShadeRegen regen;

    public PlayerEventListener(ShadeRegen regen) {
        this.regen = regen;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (regen.getConfig().pregenTimings.playerEmpty() && Bukkit.getOnlinePlayers().size() <= 1) {
            if (regen.needsPregen())
                regen.startPregenAsync(null);
        }
    }

}
