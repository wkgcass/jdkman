package io.vproxy.jdkman.entity;

import vjson.JSON;
import vjson.JSONObject;
import vjson.deserializer.rule.ArrayRule;
import vjson.deserializer.rule.NullableStringRule;
import vjson.deserializer.rule.ObjectRule;
import vjson.deserializer.rule.Rule;
import vjson.util.ObjectBuilder;

import java.util.ArrayList;
import java.util.List;

public class JDKManConfig implements JSONObject {
    private String defaultJDK;
    private List<JDKInfo> jdks;

    public static final Rule<JDKManConfig> rule = new ObjectRule<>(() -> new JDKManConfig(null))
        .put("defaultJDK", JDKManConfig::setDefaultJDK, NullableStringRule.get())
        .put("jdks", JDKManConfig::setJdks, new ArrayRule<>(ArrayList::new, List::add, JDKInfo.rule));

    @Override
    public JSON.Object toJson() {
        return new ObjectBuilder()
            .put("defaultJDK", defaultJDK)
            .putArray("jdks", a -> jdks.forEach(e -> a.addInst(e.toJson())))
            .build();
    }

    public JDKManConfig() {
        jdks = new ArrayList<>();
    }

    private JDKManConfig(@SuppressWarnings("unused") Void unused) {
    }

    public String getDefaultJDK() {
        return defaultJDK;
    }

    public void setDefaultJDK(String defaultJDK) {
        this.defaultJDK = defaultJDK;
    }

    public List<JDKInfo> getJdks() {
        return jdks;
    }

    public void setJdks(List<JDKInfo> jdks) {
        this.jdks = jdks;
    }
}
