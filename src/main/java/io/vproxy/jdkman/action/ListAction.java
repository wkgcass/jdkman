package io.vproxy.jdkman.action;

import io.vproxy.base.util.OS;
import io.vproxy.base.util.display.TableBuilder;
import io.vproxy.jdkman.entity.JDKManConfig;
import io.vproxy.jdkman.util.Utils;

import java.util.Arrays;

public class ListAction implements Action {
    @Override
    public String validate(String[] options) {
        if (options.length == 0) {
            return null;
        }
        if (options.length == 1) {
            if (options[0].equals("verbose")) {
                return null;
            }
        }
        return STR."unknown options for `list`: \{Arrays.toString(options)}";
    }

    @SuppressWarnings("StringTemplateMigration")
    @Override
    public boolean execute(JDKManConfig config, String[] options) {
        var table = new TableBuilder();
        var tr = table.tr();
        tr.td("id").td("version").td("build").td("full").td("vendor")
            .td("default").td("current");
        var isVerbose = Arrays.asList(options).contains("verbose");
        if (isVerbose) {
            tr.td("home");
        }

        var current = Utils.currentVersion(config);

        for (var jdk : config.getJdks()) {
            tr = table.tr();
            tr.td(jdk.getId());

            var version = "" + jdk.getMajorVersion();
            boolean minorAdded = false;
            if (jdk.getMinorVersion() != 0) {
                version += "." + jdk.getMinorVersion();
                minorAdded = true;
            }
            if (jdk.getPatchVersion() != 0) {
                if (!minorAdded) {
                    version += ".0";
                }
                version += "." + jdk.getPatchVersion();
            }
            tr.td(version);

            if (jdk.getBuildVersion() == null) {
                tr.td("");
            } else {
                tr.td(jdk.getBuildVersion());
            }

            tr.td(jdk.getFullVersion());

            if (jdk.getImplementor() == null) {
                tr.td("");
            } else {
                tr.td(jdk.getImplementor());
            }

            if (jdk.getId().equals(config.getDefaultJDK())) {
                if (OS.isWindows()) {
                    tr.td("   *");
                } else {
                    tr.td("   ○");
                }
            } else {
                tr.td("");
            }

            if (current == jdk) {
                if (OS.isWindows()) {
                    tr.td("   *");
                } else {
                    tr.td("   ●");
                }
            } else {
                tr.td("");
            }

            if (isVerbose) {
                tr.td(jdk.getHome());
            }
        }

        System.out.println(table.toString().trim());
        return false;
    }
}
