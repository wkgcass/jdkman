package io.vproxy.jdkman.res;

import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;

public class Feature implements org.graalvm.nativeimage.hosted.Feature {
    @Override
    public void duringSetup(DuringSetupAccess access) {
        RuntimeResourceAccess.addResource(getClass().getModule(), "/io/vproxy/jdkman/res/jdkman_proxy-windows-x86_64.exe");
    }
}
