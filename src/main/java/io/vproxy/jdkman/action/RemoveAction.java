package io.vproxy.jdkman.action;

import io.vproxy.jdkman.entity.JDKManConfig;
import io.vproxy.jdkman.ex.ErrorResult;

import java.util.Arrays;

public class RemoveAction implements Action {
    @Override
    public String validate(String[] options) {
        if (options.length == 0) {
            return "missing id for `remove`";
        }
        if (options.length > 1) {
            return STR."unknown options for `remove`: \{Arrays.toString(options)}";
        }
        return null;
    }

    @Override
    public boolean execute(JDKManConfig config, String[] options) throws Exception {
        var uuid = options[0];
        var removed = config.getJdks().removeIf(e -> uuid.equals(e.getId()));
        if (!removed) {
            throw new ErrorResult(STR."\{options[0]} not found");
        }

        if (uuid.equals(config.getDefaultJDK())) {
            if (config.getJdks().isEmpty()) {
                config.setDefaultJDK(null);
            } else {
                config.setDefaultJDK(config.getJdks().getFirst().getId());
            }
        }
        return true;
    }
}
