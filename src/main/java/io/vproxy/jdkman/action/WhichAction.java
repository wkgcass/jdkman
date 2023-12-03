package io.vproxy.jdkman.action;

import io.vproxy.jdkman.entity.JDKManConfig;
import io.vproxy.jdkman.util.Utils;

import java.util.Arrays;

public class WhichAction implements Action {
    @Override
    public String validate(String[] options) {
        if (options.length == 0) {
            return null;
        }
        return STR."unknown options for `which`: \{Arrays.toString(options)}";
    }

    @Override
    public boolean execute(JDKManConfig config, String[] options) {
        var jdk = Utils.currentVersion(config);
        if (jdk == null) {
            // not found, so print nothing
            return false;
        }
        System.out.println(jdk.getHome());
        return false;
    }
}
