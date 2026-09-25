package com.queueease.exception;

public class DuplicateQueueEntryException extends RuntimeException {
    public DuplicateQueueEntryException(String message) {
        super(message);
    }
}
