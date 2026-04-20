package com.faceid.config;

/**
 * Thrown when communication with the Python face recognition service fails.
 */
public class PythonServiceException extends RuntimeException {
    public PythonServiceException(String message) {
        super(message);
    }

    public PythonServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
