package io.vproxy.jdkman;

import io.vproxy.base.Config;
import io.vproxy.base.util.LogType;
import io.vproxy.base.util.Logger;
import io.vproxy.commons.util.IOUtils;
import io.vproxy.jdkman.action.*;
import io.vproxy.jdkman.entity.JDKManConfig;
import io.vproxy.jdkman.ex.ErrorResult;
import io.vproxy.jdkman.util.CommentLogHandler;
import vjson.JSON;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class Main {
    private static final String VERSION = "0.0.1";
    private static final String HELP_STR = """
        Usage:
            jdkman [action] [options]
        Actions:
            help                          Show this page
            version                       Show version
            list [verbose]                Show registered jdk list
            add <JAVA_HOME>               Add a new jdk
            remove <id>                   Remove an existing jdk
            default <id>                  Set default jdk
            refresh                       Update jdk info and remove invalid ones
            which                         Print current JAVA_HOME
            init [sh|pwsh]                Print shell script to eval
        """.trim();
    private static final Map<String, Action> ACTIONS = new HashMap<>() {{
        var ls = new ListAction();
        put("list", ls);
        put("ls", ls);
        put("add", new AddAction());
        var rm = new RemoveAction();
        put("remove", rm);
        put("rm", rm);
        put("default", new DefaultAction());
        put("refresh", new RefreshAction());
        put("which", new WhichAction());
        put("init", new InitAction());
    }};

    public static void main(String[] args) {
        Logger.logDispatcher.removeLogHandler(Logger.stdoutLogHandler);
        Logger.logDispatcher.addLogHandler(CommentLogHandler.get());
        var exitCode = main0(args);
        System.exit(exitCode);
    }

    private static int main0(String[] args) {
        if (args.length == 0) {
            System.out.println(STR."Version: \{VERSION}");
            System.out.println(HELP_STR);
            return 0;
        }
        var action = args[0];
        var options = new String[args.length - 1];
        System.arraycopy(args, 1, options, 0, options.length);
        switch (action) {
            case "help":
                return help(options);
            case "version":
                return version(options);
        }

        var jdkmanFile = new File(Config.workingDirectoryFile("jdkman"));
        JDKManConfig config;
        if (jdkmanFile.exists()) {
            String str;
            try {
                str = Files.readString(jdkmanFile.toPath());
            } catch (IOException e) {
                Logger.error(LogType.FILE_ERROR, "failed to read jdkman config file", e);
                return 1;
            }
            try {
                config = JSON.deserialize(str, JDKManConfig.rule);
            } catch (Exception e) {
                Logger.error(LogType.INVALID_EXTERNAL_DATA, "invalid jdkman config file", e);
                return 1;
            }
        } else {
            config = new JDKManConfig();
            Logger.logDispatcher.removeLogHandler(CommentLogHandler.get());
            try {
                try {
                    IOUtils.writeFileWithBackup(jdkmanFile.getAbsolutePath(), config.toJson().pretty());
                } finally {
                    Logger.logDispatcher.addLogHandler(CommentLogHandler.get());
                }
            } catch (Exception e) {
                Logger.error(LogType.FILE_ERROR, "failed to create jdkman config file", e);
                return 1;
            }
        }

        var act = ACTIONS.get(action);
        if (act == null) {
            System.out.println(STR."unknown action `\{action}`");
            return 1;
        }
        var err = act.validate(options);
        if (err != null) {
            System.out.println(err);
            return 1;
        }
        boolean isModified;
        try {
            isModified = act.execute(config, options);
        } catch (ErrorResult e) {
            System.out.println(e.getMessage());
            return 1;
        } catch (Exception e) {
            Logger.error(LogType.SYS_ERROR, STR."failed to execute \{action} \{Arrays.toString(options)}", e);
            return 1;
        }

        if (isModified) {
            Logger.logDispatcher.removeLogHandler(CommentLogHandler.get());
            try {
                try {
                    IOUtils.writeFileWithBackup(jdkmanFile.getAbsolutePath(), config.toJson().pretty());
                } finally {
                    Logger.logDispatcher.addLogHandler(CommentLogHandler.get());
                }
            } catch (Exception e) {
                Logger.error(LogType.FILE_ERROR, "failed to persist jdkman config", e);
                return 1;
            }
        }
        return 0;
    }

    private static int help(String[] options) {
        if (options.length != 0) {
            System.out.println(STR."unknown options for `help`: \{Arrays.toString(options)}");
            return 1;
        }
        System.out.println(HELP_STR);
        return 0;
    }

    private static int version(String[] options) {
        if (options.length != 0) {
            System.out.println(STR."unknown options for `version`: \{Arrays.toString(options)}");
            return 1;
        }
        System.out.println(VERSION);
        return 0;
    }
}
