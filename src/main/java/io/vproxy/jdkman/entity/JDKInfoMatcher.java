package io.vproxy.jdkman.entity;

import java.util.Objects;

public class JDKInfoMatcher {
    public final String implementor;
    public final int majorVersion;
    public final Integer minorVersion;
    public final Integer patchVersion;
    public final String buildVersion;
    public final String fullVersion;

    public JDKInfoMatcher(String implementor, int majorVersion, Integer minorVersion, Integer patchVersion, String buildVersion, String fullVersion) {
        this.implementor = implementor;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
        if (patchVersion != null) {
            if (minorVersion == null)
                throw new NullPointerException(STR."patch version (\{patchVersion}) is specified but missing minor version");
        }
        this.buildVersion = buildVersion;
        this.fullVersion = Objects.requireNonNull(fullVersion);
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean match(JDKInfo that, MatchOptions opts) {
        if (opts.matchImplementor) {
            if (implementor != null
                && !implementor.equals(that.getImplementor())) {
                return false;
            }
        }
        if (opts.matchMajorVersion) {
            if (majorVersion != that.getMajorVersion())
                return false;
            if (majorVersion == 1) {
                // 1.7.x, 1.8.x, the minor version is critical
                if (minorVersion != null
                    && minorVersion != that.getMinorVersion())
                    return false;
            }
        } else {
            if (majorVersion > that.getMajorVersion())
                return false;
            if (majorVersion == 1 && that.getMajorVersion() == 1) {
                if (minorVersion != null
                    && minorVersion > that.getMinorVersion())
                    return false;
            }
        }
        if (majorVersion != 1) {
            // 11.x.x, 21.x.x, the minor version is not critical
            if (minorVersion != null
                && minorVersion > that.getMinorVersion())
                return false;
        }
        if (patchVersion != null
            && patchVersion > that.getPatchVersion()) {
            assert minorVersion != null;
            if (minorVersion == that.getMinorVersion()) {
                return false;
            }
        }
        if (opts.matchBuildVersion) {
            if (buildVersion != null
                && !buildVersion.equals(that.getBuildVersion())) {
                return false;
            }
        }
        return true;
    }
}
