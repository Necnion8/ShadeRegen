package com.gmail.necnionch.myplugin.shaderegen.bukkit.regen;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ServerWrapper {

    private static final Pattern STARTUP_PATTERN = Pattern.compile("Done .+! For help, type");
    private static final Pattern[] PREPARING_PATTERNS = new Pattern[] {
            Pattern.compile("Preparing level \""),
            Pattern.compile("Preparing start region for level"),  // old ver
    };
    private static final int KEEP_RECENT_LOG_LINES = 20;
    private final Options options;
    private final Handler handler;
    private final List<String> recentLogs = new ArrayList<>();
    private Process process;

    public ServerWrapper(Options options, Handler handler) {
        this.options = options;
        this.handler = handler;
    }

    /**
     * eula.txt ファイルを作成します
     */
    public boolean createEulaFile(boolean accept) {
        File eulaFile = new File(options.workingDirectory, "eula.txt");
        try (FileWriter writer = new FileWriter(eulaFile)) {
            writer.write("eula=" + accept + "\n");
        } catch (IOException ignored) {
            return false;
        }
        return true;
    }

    /**
     * logs フォルダに含まれるファイルを削除します
     */
    public boolean deleteLogs() {
        File logsDir = new File(options.workingDirectory, "logs");
        if (!logsDir.exists() || !logsDir.isDirectory())
            return true;

        Path dirPath = logsDir.toPath();

        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.filter(Files::isRegularFile)  // サブディレクトリは無視
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException | RuntimeException e) {
            return false;
        }

        return true;
    }

    public void start() throws IOException {
        if (process != null && process.isAlive()) {
            throw new IllegalStateException("Already started");
        }

        List<String> command = new ArrayList<>();
        command.add(options.javaExecutable);
        command.add("-Xms" + options.minMemory);
        command.add("-jar");
        command.add(options.serverJarPath);
        command.add("--host");
        command.add(options.bindHost);
        command.add("--port");
        command.add(String.valueOf(options.bindPort));
        command.addAll(options.args);

        if (!options.args.contains("nogui"))
            command.add("nogui");

        process = new ProcessBuilder(command)
                .directory(options.workingDirectory)
                .redirectErrorStream(true)
                .start();
        createWatchThread(process).start();
    }

    private Thread createWatchThread(Process process) {
        recentLogs.clear();
        Thread th = new Thread(() -> {
            boolean started = false, preparing = false;
            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // keep recent logs
                        recentLogs.add(line);
                        if (recentLogs.size() > KEEP_RECENT_LOG_LINES) {
                            recentLogs.remove(0);
                        }

                        // check preparing
                        if (!preparing) {
                            for (Pattern pattern : PREPARING_PATTERNS) {
                                if (pattern.matcher(line).find()) {
                                    preparing = true;
                                    try {
                                        handler.onLevelPreparing();
                                    } catch (Throwable e) {
                                        handler.onError(e);
                                    }
                                    break;
                                }
                            }
                        }

                        // check startup
                        if (!started && STARTUP_PATTERN.matcher(line).find()) {
                            started = true;
                            try {
                                handler.onServerStart();
                            } catch (Throwable e) {
                                handler.onError(e);
                            }
                        }
                    }

                } catch (IOException e) {
                    handler.onError(e);
                }

                try {
                    handler.onServerExit(process.waitFor());
                } catch (InterruptedException e) {
                    handler.onServerExit(-1);
                }

            } finally {
                process.destroyForcibly();
            }

        }, "ShadeRegen-ServerWrapper-Thread");
        th.setDaemon(true);
        return th;
    }

    public void executeCommand(String command) throws IOException {
        if (process == null || !process.isAlive())
            return;

        process.getOutputStream().write((command + "\n").getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().flush();
    }

    public void executeStop() throws IOException {
        executeCommand("stop");
    }

    public void kill() {
        if (process == null || !process.isAlive())
            return;

        process.destroyForcibly();
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public String[] getRecentLogs() {
        return recentLogs.toArray(new String[0]);
    }


    public static class Options {

        public static final int DEFAULT_MIN_MEMORY = 1024 * 1024 * 1024;
        public static final String DEFAULT_BIND_HOST = "localhost";
        public static final int DEFAULT_BIND_PORT = 0;

        private final File workingDirectory;
        private final int minMemory;
        private final String javaExecutable;
        private final String serverJarPath;
        private final String bindHost;
        private final int bindPort;
        private final List<String> args;

        private Options(Builder builder) {
            this.workingDirectory = builder.workingDirectory;
            this.minMemory = builder.minMemory;
            this.javaExecutable = builder.javaExecutable;
            this.serverJarPath = builder.serverJarPath;
            this.bindHost = builder.bindHost;
            this.bindPort = builder.bindPort;
            this.args = Optional.ofNullable(builder.args)
                    .map(ls -> Collections.unmodifiableList(new ArrayList<>(ls)))
                    .orElse(Collections.emptyList());

            if (javaExecutable == null || javaExecutable.isEmpty())
                throw new IllegalArgumentException("javaExecutable is null or empty");
            if (serverJarPath == null || serverJarPath.isEmpty())
                throw new IllegalArgumentException("serverJarPath is null or empty");
        }

        public File getWorkingDirectory() {
            return workingDirectory;
        }

        public int getMinMemory() {
            return minMemory;
        }

        public String getJavaExecutable() {
            return javaExecutable;
        }

        public String getServerJarPath() {
            return serverJarPath;
        }

        public String getBindHost() {
            return bindHost;
        }

        public int getBindPort() {
            return bindPort;
        }

        public List<String> getArgs() {
            return args;
        }

        public static class Builder {

            private final File workingDirectory;
            private int minMemory = DEFAULT_MIN_MEMORY;
            private String javaExecutable;
            private String serverJarPath;
            private String bindHost = DEFAULT_BIND_HOST;
            private int bindPort = DEFAULT_BIND_PORT;
            private List<String> args;

            public Builder(File workingDirectory) {
                this.workingDirectory = workingDirectory;
            }

            public File workingDirectory() {
                return workingDirectory;
            }

            public int minMemory() {
                return minMemory;
            }

            public Builder minMemory(int bytes) {
                this.minMemory = bytes;
                return this;
            }

            public String javaExecutable() {
                return javaExecutable;
            }

            public Builder javaExecutable(String javaExecutable) {
                this.javaExecutable = javaExecutable;
                return this;
            }

            public String serverJarPath() {
                return serverJarPath;
            }

            public Builder serverJarPath(String serverJarPath) {
                this.serverJarPath = serverJarPath;
                return this;
            }

            public String bindHost() {
                return bindHost;
            }

            public Builder bindHost(String bindHost) {
                this.bindHost = bindHost;
                return this;
            }

            public int bindPort() {
                return bindPort;
            }

            public Builder bindPort(int bindPort) {
                this.bindPort = bindPort;
                return this;
            }

            public List<String> args() {
                return args;
            }

            public Builder args(List<String> args) {
                this.args = args;
                return this;
            }

            public Options build() {
                return new Options(this);
            }

        }

        public static Builder builder(File workingDirectory) {
            return new Builder(workingDirectory);
        }
    }

    public interface Handler {
        void onLevelPreparing();
        void onServerStart();
        void onServerExit(int exitValue);
        void onError(Throwable throwable);
    }

}
