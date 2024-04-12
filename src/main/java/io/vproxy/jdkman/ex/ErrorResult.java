package io.vproxy.jdkman.ex;

public class ErrorResult extends Exception {
    public ErrorResult(String message) {
        super(message);
    }

    public ErrorResult(String message, Throwable cause) {
        super(message, cause);
    }
}
