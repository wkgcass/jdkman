package io.vproxy.jdkman.action;

import io.vproxy.base.util.LogType;
import io.vproxy.base.util.Logger;
import io.vproxy.jdkman.entity.JDKInfo;
import io.vproxy.jdkman.entity.JDKManConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class RefreshAction implements Action {
    @Override
    public String validate(String[] options) {
        if (options.length == 0) {
            return null;
        }
        return STR."unknown options for `refresh`: \{Arrays.toString(options)}";
    }

    @Override
    public boolean execute(JDKManConfig config, String[] options) {
        var homes = new ArrayList<String>();
        JDKInfo oldDefault = null;
        for (var jdk : config.getJdks()) {
            if (new File(jdk.getHome()).exists()) {
                homes.add(jdk.getHome());
                if (jdk.getId().equals(config.getDefaultJDK())) {
                    oldDefault = jdk;
                }
            } else {
                Logger.info(LogType.ALERT, STR."jdk \{jdk} is removed");
            }
        }

        config.setDefaultJDK(null);
        config.setJdks(new ArrayList<>());
        var addAction = new AddAction();
        for (var home : homes) {
            try {
                addAction.execute(config, new String[]{home});
                Logger.alert(STR."jdk \{home} is re-added");
            } catch (Exception e) {
                Logger.error(LogType.ALERT, STR."failed to re-add jdk: \{home}", e);
            }
        }

        if (oldDefault != null) {
            for (var jdk : config.getJdks()) {
                if (oldDefault.getHome().equals(jdk.getHome())) {
                    config.setDefaultJDK(jdk.getId());
                    break;
                }
            }
        }

        return true;
    }
}
