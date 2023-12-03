package io.vproxy.jdkman.util;

import io.vproxy.base.util.log.LogHandler;
import io.vproxy.base.util.log.LogRecord;

public class CommentLogHandler implements LogHandler {
    private static final CommentLogHandler INST = new CommentLogHandler();

    public static CommentLogHandler get() {
        return INST;
    }

    @Override
    public void publish(LogRecord logRecord) {
        var lines = logRecord.toColoredString().split("\n");
        for (var line : lines) {
            System.out.println("## jdkman ## " + line);
        }
    }
}
