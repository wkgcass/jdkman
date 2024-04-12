package io.vproxy.jdkman.action;

import io.vproxy.base.util.LogType;
import io.vproxy.base.util.Logger;
import io.vproxy.base.util.OS;
import io.vproxy.base.util.Utils;
import io.vproxy.jdkman.entity.JDKManConfig;
import io.vproxy.jdkman.ex.ErrorResult;
import io.vproxy.jdkman.res.ResConsts;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class InitAction implements Action {
    private static final Set<String> EXECUTABLES = new HashSet<>() {{
        addAll(Arrays.asList(
            "jar", "jarsigner", "java", "javac",
            "javadoc", "javap", "jcmd", "jconsole",
            "jdb", "jdeprscan", "jdeps", "jfr",
            "jhsdb", "jimage", "jinfo", "jlink",
            "jmap", "jmod", "jpackage", "jps",
            "jrunscript", "jshell", "jstack",
            "jstat", "jstatd", "jwebserver",
            "keytool", "rmiregistry", "serialver"));
        addAll(Arrays.asList(
            "jaotc", "jpackager"
        ));
        addAll(Arrays.asList(
            "appletviewer", "extcheck", "idlj",
            "javafxpackager", "javah", "javapackager",
            "jhat", "jjs", "jmc", "jsadebugd",
            "jvisualvm", "native2ascii", "orbd",
            "pack200", "policytool", "rmic", "rmid",
            "schemagen", "servertool", "tnameserv",
            "unpack200", "wsgen", "wsimport", "xjc"));
        addAll(Arrays.asList(
            "javaws", "jcontrol", "jweblauncher"
        ));
    }};

    @Override
    public String validate(String[] options) {
        if (options.length != 0 && options.length != 1) {
            return STR."unknown options for `init`: \{Arrays.toString(options)}";
        }
        if (options.length > 0 && !Set.of("sh", "pwsh").contains(options[0])) {
            return STR."the first option must be 'sh|pwsh': \{options[0]}";
        }
        return null;
    }

    private enum ShellType {
        shell,
        pwsh,
    }

    @Override
    public boolean execute(JDKManConfig config, String[] options) throws Exception {
        var shellType = OS.isWindows() ? ShellType.pwsh : ShellType.shell;
        for (var o : options) {
            switch (o) {
                case "sh" -> shellType = ShellType.shell;
                case "pwsh" -> shellType = ShellType.pwsh;
            }
        }

        var jdkmanScriptDir = jdkmanScriptPathFile();

        for (var exe : EXECUTABLES) {
            var suffix = "";
            if (OS.isWindows()) {
                suffix = ".exe"; // use jdkman-proxy binary
            }
            var path = Path.of(jdkmanScriptDir.getAbsolutePath(), exe + suffix);
            var file = path.toFile();
            if (file.exists()) {
                if (!file.isFile()) {
                    Logger.error(LogType.INVALID_EXTERNAL_DATA, STR."\{file} is not a valid file");
                    continue;
                }
                if (OS.isWindows()) {
                    // check file md5 for windows, because windows doesn't allow files to be deleted while they are running
                    var md5 = io.vproxy.jdkman.util.Utils.fileMD5(file);
                    if (md5.equals(ResConsts.MD5_JDKMAN_PROXY_WINDOWS_X86_64)) {
                        // the file is the latest, no need to release it again
                        continue;
                    }
                }
                var ok = file.delete();
                if (!ok) {
                    Logger.error(LogType.FILE_ERROR, STR."failed to delete file: \{file}");
                    continue;
                }
            }
            var ok = file.createNewFile();
            if (!ok) {
                Logger.error(LogType.INVALID_EXTERNAL_DATA, STR."failed to create file: \{file}");
                continue;
            }

            var scriptContent = getScriptContent(exe);
            try (scriptContent; var fos = new FileOutputStream(file)) {
                scriptContent.transferTo(fos);
                //noinspection ResultOfMethodCallIgnored
                file.setExecutable(true);
            } catch (IOException e) {
                Logger.error(LogType.FILE_ERROR, STR."failed to create file \{path}", e);
            }
        }

        var scriptToEval = getScriptToEval(jdkmanScriptDir, shellType);
        System.out.println(scriptToEval.trim());

        return false;
    }

    private File jdkmanScriptPathFile() throws ErrorResult {
        var jdkmanScriptDir = Path.of(Utils.homedir(), "jdkman-scripts").toFile();
        if (jdkmanScriptDir.exists()) {
            if (!jdkmanScriptDir.isDirectory()) {
                throw new ErrorResult(STR."\{jdkmanScriptDir} is not a directory");
            }
        } else {
            var ok = jdkmanScriptDir.mkdirs();
            if (!ok) {
                throw new ErrorResult(STR."failed to create directory: \{jdkmanScriptDir}");
            }
        }
        return jdkmanScriptDir;
    }

    private static InputStream getScriptContent(String exe) {
        if (OS.isWindows()) {
            // use jdkman-proxy
            return InitAction.class.getResourceAsStream("/io/vproxy/jdkman/res/jdkman_proxy-windows-x86_64.exe");
        } else {
            // use shell scripts
            return new ByteArrayInputStream(STR."""
                    #!/bin/bash
                    set -e
                    export JAVA_HOME=`jdkman which`
                    exec "$JAVA_HOME/bin/\{exe}" "$@"
                    """.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String getScriptToEval(File jdkmanScriptDir, ShellType shellType) throws ErrorResult {
        if (shellType == ShellType.pwsh) {
            return buildPowershellEval(jdkmanScriptDir);
        } else {
            return buildBashEval(jdkmanScriptDir);
        }
    }

    private static String writeJDKManScriptDirPathToTmpFile(File jdkmanScriptDir) throws ErrorResult {
        try {
            var file = File.createTempFile("jdkman-script-dir-path", ".txt");
            Files.writeString(file.toPath(), jdkmanScriptDir.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new ErrorResult("failed to create temporary file for jdkman-script path", e);
        }
    }

    private static String buildBashEval(File jdkmanScriptDir) throws ErrorResult {
        var jdkmanScriptDirPathTmpFile = writeJDKManScriptDirPathToTmpFile(jdkmanScriptDir);
        return STR."""
                JDKMAN_SCRIPT_PATH=`cat \{jdkmanScriptDirPathTmpFile}`
                export PATH="$JDKMAN_SCRIPT_PATH:$PATH"
                function cdjh() {
                    builtin cd "$@"
                    export JAVA_HOME="`jdkman which`"
                }
                alias cd=cdjh
                export JAVA_HOME="`jdkman which`"
                rm -f \{jdkmanScriptDirPathTmpFile}
                """;
    }

    private static String buildPowershellEval(File jdkmanScriptDir) throws ErrorResult {
        var jdkmanScriptDirPathTmpFile = writeJDKManScriptDirPathToTmpFile(jdkmanScriptDir);
        Path initPs1File;
        try {
            initPs1File = Files.createTempFile("jdkman-init", ".ps1");
        } catch (IOException e) {
            Logger.error(LogType.FILE_ERROR, "failed to create jdkman-init.ps1 tmp file", e);
            throw new ErrorResult("failed to create jdkman-init.ps1 file");
        }

        var sb = new StringBuilder();
        for (var exe : EXECUTABLES) {
            sb.append("function ").append(exe).append(" {\n");
            sb.append("    $JAVA_HOME = jdkman which\n");
            sb.append("    $env:JAVA_HOME = $JAVA_HOME\n");
            sb.append("    & \"$JAVA_HOME\\bin\\").append(exe);
            if (OS.isWindows()) {
                sb.append(".exe");
            }
            sb.append("\" $args");
            sb.append("\n");
            sb.append("}\n");
        }
        sb.append("$JDKMAN_SCRIPT_PATH = (Get-Content ")
            .append(jdkmanScriptDirPathTmpFile)
            .append(")\n");
        sb.append(STR."""
            $env:PATH = "$JDKMAN_SCRIPT_PATH\{pathSeparatorInPSStr()}${env:PATH}"
            $env:JAVA_HOME = jdkman which
            function cdjh {
              param([string]$path)
                Set-Location $path
                $env:JAVA_HOME = jdkman which
            }
            Set-Alias -Name cd -Value cdjh -Option AllScope
            Remove-Item \{initPs1File}
            Remove-Item \{jdkmanScriptDirPathTmpFile}
            """);

        try {
            Files.writeString(initPs1File, sb.toString());
        } catch (IOException e) {
            Logger.error(LogType.FILE_ERROR, "failed to release jdkman-init.ps1 tmp file", e);
            throw new ErrorResult("failed to release jdkman-init.ps1 file");
        }
        return STR.". \{initPs1File}";
    }

    private static String pathSeparatorInPSStr() {
        var s = File.pathSeparator;
        if (s.equals(":")) {
            return "\\:";
        }
        return s;
    }
}
