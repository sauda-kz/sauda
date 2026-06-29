package com.sauda.exception;

public class SaudaException extends RuntimeException {

    public SaudaException(String message) {
        super(message);
    }

    public SaudaException(String message, Throwable cause) {
        super(message, cause);
    }
}
