package org.example;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class ConsoleLineBuffer extends Handler {

    private final Logger logger;
    private final AtomicReference<String> lastLine = new AtomicReference<>();

    public ConsoleLineBuffer(Logger logger) {
        this.logger = logger;
        setLevel(Level.ALL);
    }

    public void attach() {
        logger.addHandler(this);
    }

    public void detach() {
        logger.removeHandler(this);
    }

    public String getLastLine() {
        return lastLine.get();
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null || record.getMessage() == null) {
            return;
        }
        lastLine.set(record.getMessage());
    }

    @Override
    public void flush() {
        // Nothing buffered in this handler.
    }

    @Override
    public void close() {
        detach();
    }
}

