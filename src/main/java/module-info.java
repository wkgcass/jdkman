module io.vproxy.jdkman {
    requires kotlin.stdlib;
    requires vjson;
    requires io.vproxy.base;
    requires org.graalvm.nativeimage;
    opens io.vproxy.jdkman.res;
}
