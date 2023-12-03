package io.vproxy.jdkman.action;

import io.vproxy.jdkman.entity.JDKManConfig;

public interface Action {
    String validate(String[] options);

    boolean execute(JDKManConfig config, String[] options) throws Exception;
}
