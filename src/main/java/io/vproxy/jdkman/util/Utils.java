package io.vproxy.jdkman.util;

import io.vproxy.base.util.LogType;
import io.vproxy.base.util.Logger;
import io.vproxy.base.util.OS;
import io.vproxy.jdkman.entity.JDKInfo;
import io.vproxy.jdkman.entity.JDKInfoMatcher;
import io.vproxy.jdkman.entity.JDKManConfig;
import io.vproxy.jdkman.entity.MatchOptions;
import io.vproxy.jdkman.ex.ErrorResult;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

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
    private static final LinkedHashMap<String, Function<byte[], String>> TRY_CHARSETS = new LinkedHashMap<>() {{
        put("UTF-8", tryCharset(StandardCharsets.UTF_8));
        put("UTF-16LE", tryCharset(StandardCharsets.UTF_16LE));
        put("UTF-16BE", tryCharset(StandardCharsets.UTF_16BE));
        put("BOM", tryCharsetWithBOM());
    }};

    private static Function<byte[], String> tryCharset(Charset charset) {
        return b -> new String(b, charset);
    }

    private static Function<byte[], String> tryCharsetWithBOM() {
        return b -> {
            if (b.length < 2) {
                return null;
            }
            if (b[0] == (byte) 0xFF && b[1] == (byte) 0xFE) {
                var bb = new byte[b.length - 2];
                System.arraycopy(b, 2, bb, 0, b.length - 2);
                return new String(bb, StandardCharsets.UTF_16LE);
            } else if (b[0] == (byte) 0xFE && b[1] == (byte) 0xFF) {
                var bb = new byte[b.length - 2];
                System.arraycopy(b, 2, bb, 0, b.length - 2);
                return new String(bb, StandardCharsets.UTF_16BE);
            }
            if (b.length < 3) {
                return null;
            }
            if (b[0] == (byte) 0xEF && b[1] == (byte) 0xBB && b[2] == (byte) 0xBF) {
                var bb = new byte[b.length - 3];
                System.arraycopy(b, 3, bb, 0, b.length - 3);
                return new String(bb, StandardCharsets.UTF_8);
            }
            return null;
        };
    }

    public static JDKInfoMatcher currentVersion() {
        File dir;
        try {
            dir = new File("").getCanonicalFile();
        } catch (IOException e) {
            Logger.warn(LogType.FILE_ERROR, "failed to retrieve current version from file", e);
            return null;
        }
        do {
            var path = Path.of(dir.getAbsolutePath(), JAVA_VERSION);
            if (path.toFile().exists() && path.toFile().isFile()) {
                byte[] contentBytes;
                try {
                    contentBytes = Files.readAllBytes(path);
                } catch (IOException e) {
                    Logger.warn(LogType.FILE_ERROR, STR."failed to retrieve current version from file \{path}", e);
                    return null;
                }
                var lastErrors = new ArrayList<Throwable>();
                for (var entry : TRY_CHARSETS.entrySet()) {
                    var charsetName = entry.getKey();
                    var f = entry.getValue();
                    var content = f.apply(contentBytes);
                    if (content == null) {
                        lastErrors.add(new Exception(STR."unable to parse bytes to string with \{charsetName} in file \{path}"));
                        continue;
                    }
                    content = content.trim();
                    content = new String(content.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                    try {
                        return parseVersion(content);
                    } catch (ErrorResult e) {
                        assert Logger.lowLevelDebug(STR."unable to parse file \{path}: \{content}");
                        lastErrors.add(e);
                    }
                }
                Logger.warn(LogType.FILE_ERROR, STR."failed to retrieve current version from file \{path}: \{
                    lastErrors.stream().map(io.vproxy.base.util.Utils::formatErr).toList()
                    }");
                return null;
            }
            dir = dir.getParentFile();
        } while (dir != null);

        return null;
    }

    public static JDKInfo currentVersion(JDKManConfig config) {
        var currentMatcher = Utils.currentVersion();
        JDKInfo current = null;
        if (currentMatcher != null) {
            current = findProperJDK(config, currentMatcher);
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

    private static JDKInfo findProperJDK(JDKManConfig config, JDKInfoMatcher currentMatcher) {
        var ls = new ArrayList<JDKInfo>();
        var optsList = List.of(
            // most strict matching
            new MatchOptions(),
            // skip build version
            new MatchOptions()
                .setMatchBuildVersion(false),
            // skip build version and implementor
            new MatchOptions()
                .setMatchBuildVersion(false)
                .setMatchImplementor(false),
            // most loose matching
            new MatchOptions(false)
        );
        for (var opts : optsList) {
            for (var jdk : config.getJdks()) {
                if (currentMatcher.match(jdk, opts)) {
                    ls.add(jdk);
                }
            }
            if (!ls.isEmpty()) {
                ls.sort(JDKInfo::compareTo);
                return ls.getFirst();
            }
        }
        return null;
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
