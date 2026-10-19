package com.gmail.necnionch.myplugin.shaderegen.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public class ShadeRegenConfig extends BukkitConfiguration {

    public ShadeRegenConfig(Plugin plugin) {
        super(plugin);
    }

    public final GenerateServer generateServer = new GenerateServer(getSection("generate-server"));
    public final PreGenerateTimings pregenTimings = new PreGenerateTimings(getSection("pre-generate-timings"));
    public final AutorunConfig autorun = new AutorunConfig(getSection("regen.autorun"));

    @Override
    protected boolean onLoaded() {
        autorun.onLoaded(log);
        return true;
    }

    private Supplier<ConfigurationSection> getSection(String key) {
        return () -> Optional.ofNullable(config.getConfigurationSection(key)).orElseGet(MemoryConfiguration::new);
    }

    private @Nullable String getRegenName(String worldType) {
        if (!config.contains("regen.world-name." + worldType))
            return null;
        String tmp = config.getString("regen.world-name." + worldType);
        return "".equalsIgnoreCase(tmp) ? null : tmp;
    }

    public @Nullable String getRegenName() {
        return getRegenName("normal");
    }

    public @Nullable String getRegenNameNether() {
        return getRegenName("nether");
    }

    public @Nullable String getRegenNameTheEnd() {
        return getRegenName("the_end");
    }


    public static class GenerateServer {

        private final Supplier<ConfigurationSection> config;

        public GenerateServer(Supplier<ConfigurationSection> config) {
            this.config = config;
        }

        public String getJava() {
            String value = config.get().getString("java", "auto");
            return "auto".equalsIgnoreCase(value) ? null : value;
        }

        public String getServerJar() {
            String value = config.get().getString("server-jar", "auto");
            return "auto".equalsIgnoreCase(value) ? null : value;
        }

        public int getMinMemory() {
            return config.get().getInt("min-memory", 0);
        }
    }

    public static class PreGenerateTimings {

        private final Supplier<ConfigurationSection> config;

        public PreGenerateTimings(Supplier<ConfigurationSection> config) {
            this.config = config;
        }

        public boolean startup() {
            return config.get().getBoolean("startup");
        }

        public boolean playerEmpty() {
            return config.get().getBoolean("player-empty");
        }

        public boolean worldRegen() {
            return config.get().getBoolean("world-regen");
        }
    }

}