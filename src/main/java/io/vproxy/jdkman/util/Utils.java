package io.vproxy.jdkman.util;

import io.vproxy.base.util.LogType;
import io.vproxy.base.util.Logger;
import io.vproxy.base.util.OS;
import io.vproxy.jdkman.entity.JDKInfo;
import io.vproxy.jdkman.entity.JDKInfoMatcher;
import io.vproxy.jdkman.entity.JDKManConfig;
import io.vproxy.jdkman.ex.ErrorResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class Utils {
    private Utils() {
    }

    public static String validateJavaHome(String javaHome) {
        var file = new File(javaHome);
        if (!file.exists()) {
            return STR."\{javaHome} does not exist";
        }
        if (!file.isDirectory()) {
            return STR."\{javaHome} is not a directory";
        }
        var bin = Path.of(file.getAbsolutePath(), "bin");
        file = bin.toFile();
        if (!file.exists()) {
            return STR."\{javaHome} does not have bin/ subdirectory";
        }
        if (!file.isDirectory()) {
            return STR."\{file.getAbsolutePath()} is not a directory";
        }
        var java = Path.of(file.getAbsolutePath(), "java" + (OS.isWindows() ? ".exe" : ""));
        file = java.toFile();
        if (!file.exists()) {
            return STR."\{javaHome} does not have bin/java executable";
        }
        if (!file.isFile()) {
            return STR."\{file.getAbsoluteFile()} is not a file";
        }
        if (!file.canExecute()) {
            return STR."\{file.getAbsoluteFile()} is not executable";
        }
        return null;
    }

    public static String readInputStream(InputStream input) throws IOException {
        var sb = new StringBuilder();
        var chars = new char[1024];
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            while (true) {
                var len = reader.read(chars);
                if (len == -1) {
                    break;
                }
                sb.append(chars, 0, len);
            }
        }
        return sb.toString();
    }

    public static boolean isNonNegativeInteger(String s) {
        try {
            var n = Integer.parseInt(s);
            return n >= 0;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    public static JDKInfoMatcher parseVersion(final String version) throws ErrorResult {
        String implementor = null;
        String versionPart;
        if (version.contains(":")) {
            implementor = version.substring(0, version.indexOf(":"));
            versionPart = version.substring(version.indexOf(":") + 1);
        } else {
            versionPart = version;
        }

        int major;
        Integer minor = null;
        Integer patch = null;
        String buildVersion = null;

        String versionStr;
        if (versionPart.contains("+")) {
            versionStr = versionPart.substring(0, versionPart.indexOf("+"));
            buildVersion = versionPart.substring(versionPart.indexOf("+") + 1);
        } else if (versionPart.contains("_")) {
            versionStr = versionPart.substring(0, versionPart.indexOf("_"));
            buildVersion = versionPart.substring(versionPart.indexOf("_") + 1);
        } else {
            versionStr = versionPart;
        }

        var split = versionStr.split("\\.");
        if (split.length == 0) {
            throw new ErrorResult(STR."\{versionStr} is not a valid version: empty string");
        }
        if (!isNonNegativeInteger(split[0])) {
            throw new ErrorResult(STR."\{versionStr} is not a valid version: major version not valid: \{split[0]}");
        }
        major = Integer.parseInt(split[0]);
        if (split.length >= 2) {
            if (!isNonNegativeInteger(split[1])) {
                throw new ErrorResult(STR."\{versionStr} is not a valid version: minor version not valid: \{split[1]}");
            }
            minor = Integer.parseInt(split[1]);
        }
        if (split.length >= 3) {
            if (!isNonNegativeInteger(split[2])) {
                throw new ErrorResult(STR."\{versionStr} is not a valid version: patch version not valid: \{split[2]}");
            }
            patch = Integer.parseInt(split[2]);
        }

        return new JDKInfoMatcher(implementor, major, minor, patch, buildVersion, version);
    }

    private static final String JAVA_VERSION = ".java-version";

    public static JDKInfoMatcher currentVersion() {
        try {
            var dir = new File("").getCanonicalFile();
            do {
                var path = Path.of(dir.getAbsolutePath(), JAVA_VERSION);
                if (path.toFile().exists() && path.toFile().isFile()) {
                    var content = Files.readString(path).trim();
                    try {
                        return parseVersion(content);
                    } catch (ErrorResult e) {
                        assert Logger.lowLevelDebug(STR."unable to parse file \{path}: \{content}");
                        continue;
                    }
                }
                dir = dir.getParentFile();
            } while (dir.getParentFile() != null);
        } catch (IOException e) {
            Logger.warn(LogType.FILE_ERROR, "failed to retrieve current version from file", e);
            return null;
        }
        return null;
    }

    public static JDKInfo currentVersion(JDKManConfig config) {
        var currentMatcher = Utils.currentVersion();
        JDKInfo current = null;
        if (currentMatcher != null) {
            var ls = new ArrayList<JDKInfo>();
            for (var jdk : config.getJdks()) {
                if (currentMatcher.matches(jdk)) {
                    ls.add(jdk);
                }
            }
            if (!ls.isEmpty()) {
                ls.sort(JDKInfo::compareTo);
                current = ls.getLast();
            }
        }
        if (current != null) {
            return current;
        }
        for (var jdk : config.getJdks()) {
            if (jdk.getId().equals(config.getDefaultJDK())) {
                return jdk;
            }
        }
        // still not found
        if (config.getJdks().isEmpty()) {
            // no jdk registered
            return null;
        }
        // has jdk, but non match the default id
        Logger.shouldNotHappen(STR."unable to find jdk with id == \{config.getDefaultJDK()}");
        return config.getJdks().getFirst();
    }

    public static String fileMD5(File file) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Logger.shouldNotHappen("unable to find md alg: MD5");
            throw new RuntimeException(e);
        }
        byte[] buf = new byte[256 * 1024];
        try (var fis = new FileInputStream(file); var dis = new DigestInputStream(fis, md)) {
            //noinspection StatementWithEmptyBody
            while (dis.read(buf) != -1) {
            }
        }
        var md5Hash = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : md5Hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
