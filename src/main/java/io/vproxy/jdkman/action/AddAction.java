package io.vproxy.jdkman.action;

import io.vproxy.base.util.LogType;
import io.vproxy.base.util.Logger;
import io.vproxy.jdkman.entity.JDKInfo;
import io.vproxy.jdkman.entity.JDKInfoMatcher;
import io.vproxy.jdkman.entity.JDKManConfig;
import io.vproxy.jdkman.ex.ErrorResult;
import io.vproxy.jdkman.util.Utils;
import vjson.JSON;
import vjson.ex.JsonParseException;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;

public class AddAction implements Action {
    @Override
    public String validate(String[] options) {
        if (options.length == 0) {
            return "missing JAVA_HOME for `add`";
        }
        if (options.length > 1) {
            return STR."unknown options for `add`: \{Arrays.toString(options)}";
        }
        return Utils.validateJavaHome(options[0]);
    }

    @Override
    public boolean execute(JDKManConfig config, String[] options) throws Exception {
        var javaHome = new File(options[0]).getCanonicalPath();
        for (var jdk : config.getJdks()) {
            if (javaHome.equals(jdk.getHome())) {
                throw new ErrorResult(STR."JDK with JAVA_HOME \{javaHome} is already registered");
            }
        }

        var javaPath = Path.of(javaHome, "bin", "java").toAbsolutePath();
        var process = new ProcessBuilder()
            .command(javaPath.toString(), "-version")
            .start();

        var exitCode = process.waitFor();
        var stdout = Utils.readInputStream(process.getInputStream());
        var stderr = Utils.readInputStream(process.getErrorStream());
        if (exitCode != 0) {
            throw new ErrorResult(STR."""
            unable to retrieve java version: exit code: \{exitCode}
            stdout:
            \{stdout}
            stderr:
            \{stderr}""");
        }

        var lines = stderr.split("\n");
        if (lines.length == 0) {
            throw new ErrorResult(STR."""
            missing java version: empty output
            stdout:
            \{stdout}
            stderr:
            \{stderr}""");
        }
        var firstLine = lines[0].trim();
        if (!firstLine.contains("\"")) {
            throw new ErrorResult(STR."""
            missing java version: first line doesn't contain `"`
            stdout:
            \{stdout}
            stderr:
            \{stderr}""");
        }
        var split = firstLine.split("\"");
        if (split.length < 2) {
            throw new ErrorResult(STR."""
            missing java version: invalid first line
            stdout:
            \{stdout}
            stderr:
            \{stderr}""");
        }
        var versionStr = split[1];
        var versionInfo = Utils.parseVersion(versionStr);

        if (lines.length >= 2) {
            var detailedVersionInfo = parseLine2(lines[1]);
            if (detailedVersionInfo != null) {
                if (versionInfo.majorVersion != detailedVersionInfo.majorVersion) {
                    throw new ErrorResult(STR."""
                    major version mismatch:
                    \{stderr}""");
                }
                if (!Objects.equals(versionInfo.minorVersion, detailedVersionInfo.minorVersion)) {
                    throw new ErrorResult(STR."""
                    minor version mismatch:
                    \{stderr}""");
                }
                if (!Objects.equals(versionInfo.patchVersion, detailedVersionInfo.patchVersion)) {
                    throw new ErrorResult(STR."""
                    patch version mismatch:
                    \{stderr}""");
                }
                versionInfo = detailedVersionInfo;
            }
        }

        // check release file
        String implementor = null;
        var releaseFile = Path.of(javaHome, "release").toFile();
        if (releaseFile.exists() && releaseFile.isFile()) {
            var p = new Properties();
            try (var input = new FileInputStream(releaseFile)) {
                p.load(input);
            }
            implementor = p.getProperty("IMPLEMENTOR");
            if (implementor != null && implementor.startsWith("\"")) {
                JSON.String jsonStr = null;
                try {
                    var o = JSON.parse(implementor);
                    if (o instanceof JSON.String s) {
                        jsonStr = s;
                    }
                } catch (JsonParseException e) {
                    Logger.warn(LogType.ALERT, STR."implementor field is not a valid json string: \{implementor}", e);
                }
                if (jsonStr != null) {
                    implementor = jsonStr.toJavaObject();
                }
            }
        }

        var uuid = UUID.randomUUID().toString();
        var version = new JDKInfo(versionInfo);
        version.setId(uuid);
        version.setHome(javaHome);
        version.setImplementor(implementor);
        config.getJdks().add(version);

        config.getJdks().sort(Comparator.reverseOrder());

        if (config.getDefaultJDK() == null) {
            config.setDefaultJDK(uuid);
        }

        return true;
    }

    private JDKInfoMatcher parseLine2(String secondLine) throws ErrorResult {
        if (!secondLine.contains("(")) {
            return null;
        }
        secondLine = secondLine.substring(secondLine.lastIndexOf("(") + 1);
        if (!secondLine.contains(")")) {
            return null;
        }
        secondLine = secondLine.substring(0, secondLine.indexOf(")"));
        if (!secondLine.startsWith("build ")) {
            return null;
        }
        secondLine = secondLine.substring("build ".length());
        return Utils.parseVersion(secondLine);
    }
}
