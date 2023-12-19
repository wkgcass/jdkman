package io.vproxy.jdkman.entity;

public class MatchOptions {
    boolean matchMajorVersion = true;
    boolean matchBuildVersion = true;
    boolean matchImplementor = true;

    public MatchOptions() {
        this(true);
    }

    public MatchOptions(boolean init) {
        matchMajorVersion = init;
        matchBuildVersion = init;
        matchImplementor = init;
    }

    public MatchOptions setMatchMajorVersion(boolean matchMajorVersion) {
        this.matchMajorVersion = matchMajorVersion;
        return this;
    }

    public MatchOptions setMatchBuildVersion(boolean matchBuildVersion) {
        this.matchBuildVersion = matchBuildVersion;
        return this;
    }

    public MatchOptions setMatchImplementor(boolean matchImplementor) {
        this.matchImplementor = matchImplementor;
        return this;
    }
}
