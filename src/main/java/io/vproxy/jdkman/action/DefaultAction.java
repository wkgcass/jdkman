package io.vproxy.jdkman.action;

import io.vproxy.jdkman.entity.JDKManConfig;
import io.vproxy.jdkman.ex.ErrorResult;

import java.util.Arrays;

public class DefaultAction implements Action {
    @Override
    public String validate(String[] options) {
        if (options.length == 0) {
            return "missing id for `default`";
        }
        if (options.length > 1) {
            return STR."unknown options for `default`: \{Arrays.toString(options)}";
        }
        return null;
    }

    @Override
    public boolean execute(JDKManConfig config, String[] options) throws Exception {
        var uuid = options[0];
        for (var jdk : config.getJdks()) {
            if (jdk.getId().equals(uuid)) {
                config.setDefaultJDK(uuid);
                return true;
            }
        }
        throw new ErrorResult(STR."unable to find jdk with id \{uuid}");
    }
}
