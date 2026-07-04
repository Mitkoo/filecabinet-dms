package com.filecabinet.shared.exception;

public final class ServiceExceptions {

    private ServiceExceptions() {
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicateException extends RuntimeException {
        public DuplicateException(String message) {
            super(message);
        }
    }
}