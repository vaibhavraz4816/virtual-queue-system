package com.queueease.exception;

public class InvalidQueueStateException extends RuntimeException {
    public InvalidQueueStateException(String message) {
        super(message);
    }
}
