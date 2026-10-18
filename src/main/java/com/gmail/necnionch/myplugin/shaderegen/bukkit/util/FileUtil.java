package com.gmail.necnionch.myplugin.shaderegen.bukkit.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class FileUtil {

    public static void deleteDirectory(Path targetDir) throws IOException {
        if (Files.exists(targetDir)) {
            try (Stream<Path> pathStream = Files.walk(targetDir)) {
                pathStream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
    }

}
