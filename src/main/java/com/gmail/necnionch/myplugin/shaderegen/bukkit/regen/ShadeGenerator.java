package com.gmail.necnionch.myplugin.shaderegen.bukkit.regen;

import com.gmail.necnionch.myplugin.shaderegen.bukkit.config.ShadeRegenConfig;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.util.FileUtil;
import com.gmail.necnionch.myplugin.shaderegen.bukkit.util.PathResolver;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShadeGenerator {

    public static final String DEFAULT_GENERATE_WORLD_NAME = "shade_world";
    private final File workingDirectory;
    private final ShadeRegenConfig.GenerateServer config;
    private final Logger log;
    private final File worldNormalDirectory;
    private final File worldNetherDirectory;
    private final File worldTheEndDirectory;
    private ServerWrapper wrapper;

    public ShadeGenerator(File workingDirectory, ShadeRegenConfig.GenerateServer config, Logger logger) {
        this.workingDirectory = new File(workingDirectory, "generate-server");
        this.config = config;
        this.log = logger;
        this.worldNormalDirectory = new File(this.workingDirectory, DEFAULT_GENERATE_WORLD_NAME);
        this.worldNetherDirectory = new File(this.workingDirectory, DEFAULT_GENERATE_WORLD_NAME + "_nether");
        this.worldTheEndDirectory = new File(this.workingDirectory, DEFAULT_GENERATE_WORLD_NAME + "_the_end");
    }

    private ServerWrapper.Options createOptions() {
        String java = Optional.ofNullable(config.getJava())
                .orElseGet(() -> Optional.ofNullable(PathResolver.resolveJavaExecutable())
                        .map(File::getAbsolutePath)
                        .orElse(null));

        if (java == null) {
            log.warning("Could not automatically detect a Java executable. Please specify it in the configuration.");
            java = "java";
        }

        String serverJar = Optional.ofNullable(config.getServerJar())
                .orElseGet(() -> Optional.ofNullable(PathResolver.resolveServerJar())
                        .map(File::getAbsolutePath)
                        .orElse(null));

        if (serverJar == null) {
            log.warning("Could not automatically detect a server jar file. Please specify it in the configuration.");
            serverJar = "server.jar";
        }

        return ServerWrapper.Options.builder(workingDirectory)
                .javaExecutable(java)
                .serverJarPath(serverJar)
                .minMemory(Math.max(100, config.getMinMemory()) * 1024)
                .args(Arrays.asList(
                        "--world-dir", workingDirectory.getAbsolutePath(),
                        "--level-name", DEFAULT_GENERATE_WORLD_NAME
                ))
                .build();
    }

    public void build(@Nullable ProgressListener progress, boolean keepOldShade) throws IOException {
        if (wrapper != null && wrapper.isAlive())
            return;

        wrapper = new ServerWrapper(createOptions(), new ServerWrapper.Handler() {
            @Override
            public void onLevelPreparing() {
                safeSetProgress(progress, Status.WORLD_PREPARING);
            }

            @Override
            public void onServerStart() {
                safeSetProgress(progress, Status.WORLD_SAVING);
                try {
                    wrapper.executeStop();
                } catch (IOException e) {
                    log.warning("Failed to execute stop command: " + e.getMessage());
                    wrapper.kill();
                }
            }

            @Override
            public void onServerExit(int exitValue) {
                if (exitValue != 0) {
                    log.warning("Gen-Server exited with code " + exitValue);
                    log.warning("===== Last Output =====");
                    for (String line : wrapper.getRecentLogs()) {
                        log.warning(line);
                    }
                    log.warning("===== Last Output =====");
                    safeSetProgress(progress, Status.ERROR);
                } else {
                    log.info("Exited Gen-Server");
                    safeSetProgress(progress, Status.SUCCESS);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.warning("Error in server wrapper: " + throwable.getMessage());
            }
        });

        log.info("Starting Gen-Server");
        safeSetProgress(progress, Status.SERVER_PREPARING);
        //noinspection ResultOfMethodCallIgnored
        workingDirectory.mkdirs();
        wrapper.createEulaFile(true);
        wrapper.deleteLogs();

        if (!keepOldShade) {
            try {
                FileUtil.deleteDirectory(worldNormalDirectory.toPath());
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to delete old shade world", e);
            }
            try {
                FileUtil.deleteDirectory(worldNetherDirectory.toPath());
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to delete old shade world nether", e);
            }
            try {
                FileUtil.deleteDirectory(worldTheEndDirectory.toPath());
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to delete old shade world the end", e);
            }
        }

        safeSetProgress(progress, Status.SERVER_STARTING);
        wrapper.start();
    }

    public void close()  {
        if (wrapper == null || !wrapper.isAlive()) return;
        wrapper.kill();
    }

    public ServerWrapper getServerWrapper() {
        return wrapper;
    }

    public File getWorldNormalDirectory() {
        return worldNormalDirectory;
    }

    public File getWorldNetherDirectory() {
        return worldNetherDirectory;
    }

    public File getWorldTheEndDirectory() {
        return worldTheEndDirectory;
    }


    public enum Status {
        SERVER_PREPARING(0),
        SERVER_STARTING(1),
        WORLD_PREPARING(2),
        WORLD_SAVING(3),
        SUCCESS(-2),
        ERROR(-1);

        private final int value;

        Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public boolean isExited() {
            return value < 0;
        }
    }

    public interface ProgressListener {
        void onProgress(Status status);
    }

    private void safeSetProgress(@Nullable ProgressListener listener, Status status) {
        if (listener == null)
            return;
        try {
            listener.onProgress(status);
        } catch (Exception e) {
            log.log(Level.WARNING, "Exception in set progress (" + status.name() + ") to listener.onProgress", e);
        }
    }

}
