package com.gmail.necnionch.myplugin.shaderegen.bukkit.command;

import com.gmail.necnionch.myplugin.shaderegen.bukkit.ShadeRegen;
import net.kyori.adventure.text.Component;

public class MainCommand extends Command {

    private final ShadeRegen regen;

    public MainCommand(String name, ShadeRegen regen) {
        super(name, null);
        this.regen = regen;

        addChild("regen", this::regen);
        addChild("build", this::build);
        addChild("reload", this::reload);
    }

    private void regen(Context context) {
        context.send(Component.text("Start Regeneration!"));
        regen.regenerate().thenAccept(result -> {
            context.send(Component.text("Result: " + result));
        });
    }

    private void build(Context context) {
        regen.startPregenAsync(status -> {
            context.send(Component.text("Shade Regenerating: " + status.name()));
        });
    }

    private void reload(Context context) {
        regen.getConfig().load();
    }


}
