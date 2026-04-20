package com.faceid.config;

/**
 * Thrown when no face is detected in the submitted image.
 */
public class NoFaceDetectedException extends RuntimeException {
    public NoFaceDetectedException(String message) {
        super(message);
    }
}
