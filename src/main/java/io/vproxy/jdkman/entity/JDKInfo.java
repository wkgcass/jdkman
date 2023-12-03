package io.vproxy.jdkman.entity;

import vjson.JSON;
import vjson.JSONObject;
import vjson.deserializer.rule.*;
import vjson.util.ObjectBuilder;

public class JDKInfo implements JSONObject, Comparable<JDKInfo> {
    private String id;
    private int majorVersion;
    private int minorVersion;
    private int patchVersion;
    private String buildVersion; // nullable
    private String fullVersion;
    private String implementor; // nullable
    private String home;

    public static final Rule<JDKInfo> rule = new ObjectRule<>(JDKInfo::new)
        .put("id", JDKInfo::setId, StringRule.get())
        .put("majorVersion", JDKInfo::setMajorVersion, IntRule.get())
        .put("minorVersion", JDKInfo::setMinorVersion, IntRule.get())
        .put("patchVersion", JDKInfo::setPatchVersion, IntRule.get())
        .put("buildVersion", JDKInfo::setBuildVersion, NullableStringRule.get())
        .put("fullVersion", JDKInfo::setFullVersion, StringRule.get())
        .put("implementor", JDKInfo::setImplementor, NullableStringRule.get())
        .put("home", JDKInfo::setHome, StringRule.get());

    @Override
    public JSON.Object toJson() {
        return new ObjectBuilder()
            .put("id", id)
            .put("majorVersion", majorVersion)
            .put("minorVersion", minorVersion)
            .put("patchVersion", patchVersion)
            .put("buildVersion", buildVersion)
            .put("fullVersion", fullVersion)
            .put("implementor", implementor)
            .put("home", home)
            .build();
    }

    public JDKInfo() {
    }

    public JDKInfo(JDKInfoMatcher m) {
        this.majorVersion = m.majorVersion;
        this.minorVersion = m.minorVersion == null ? 0 : m.minorVersion;
        this.patchVersion = m.patchVersion == null ? 0 : m.patchVersion;
        this.buildVersion = m.buildVersion;
        this.fullVersion = m.fullVersion;
        this.implementor = m.implementor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public int getPatchVersion() {
        return patchVersion;
    }

    public void setPatchVersion(int patchVersion) {
        this.patchVersion = patchVersion;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public void setBuildVersion(String buildVersion) {
        this.buildVersion = buildVersion;
    }

    public String getFullVersion() {
        return fullVersion;
    }

    public void setFullVersion(String fullVersion) {
        this.fullVersion = fullVersion;
    }

    public String getImplementor() {
        return implementor;
    }

    public void setImplementor(String implementor) {
        this.implementor = implementor;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    @Override
    public int compareTo(JDKInfo that) {
        if (majorVersion > that.majorVersion)
            return 1;
        if (majorVersion < that.majorVersion)
            return -1;
        if (minorVersion > that.minorVersion)
            return 1;
        if (minorVersion < that.minorVersion)
            return -1;
        if (patchVersion > that.patchVersion)
            return 1;
        if (patchVersion < that.patchVersion)
            return -1;
        if (buildVersion != null && that.buildVersion == null)
            return 1;
        if (buildVersion == null && that.buildVersion != null)
            return -1;
        if (implementor != null && that.implementor == null)
            return 1;
        if (implementor == null && that.implementor != null)
            return -1;
        return 0;
    }

    @SuppressWarnings("StringTemplateMigration")
    @Override
    public String toString() {
        return "JDKInfo{" +
               "id=" + (id == null ? "null" : ("'" + id + "'")) +
               ", majorVersion=" + majorVersion +
               ", minorVersion=" + minorVersion +
               ", patchVersion=" + patchVersion +
               ", buildVersion=" + (buildVersion == null ? "null" : ("'" + buildVersion + "'")) +
               ", fullVersion=" + (fullVersion == null ? "null" : ("'" + fullVersion + "'")) +
               ", implementor=" + (implementor == null ? "null" : ("'" + implementor + "'")) +
               ", home=" + (home == null ? "null" : ("'" + home + "'")) +
               '}';
    }
}
