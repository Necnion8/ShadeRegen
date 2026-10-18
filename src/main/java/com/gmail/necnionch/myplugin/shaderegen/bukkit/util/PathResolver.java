package com.gmail.necnionch.myplugin.shaderegen.bukkit.util;

import java.io.File;

public class PathResolver {

    /**
     * システムプロパティから起動しているサーバーJarのパスを取得します。
     * ファイルが存在しないか、取得できない場合は null を返します。
     */
    public static File resolveServerJar() {
        String command = System.getProperty("sun.java.command");
        if (command == null || command.trim().isEmpty())
            return null;

        String firstArg = command.trim().split("\\s+")[0];
        if (!firstArg.endsWith(".jar") && !firstArg.endsWith(".JAR"))
            return null;

        File jarFile = new File(firstArg);
        return  jarFile.exists() && jarFile.isFile() ? jarFile.getAbsoluteFile() : null;
    }

    /**
     * システムプロパティから現在のJava実行可能ファイル（java / javaw）のパスを取得します。
     * ファイルが存在しないか、取得できない場合は null を返します。
     */
    public static File resolveJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isEmpty())
            return null;

        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String binName = isWindows ? "java.exe" : "java";

        File javaBin = new File(new File(javaHome, "bin"), binName);
        return javaBin.exists() && javaBin.isFile() ? javaBin.getAbsoluteFile() : null;
    }

}
