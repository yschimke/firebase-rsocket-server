package com.github.mostroverkhov.firebase_rsocket.codec;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class ResponseMappingException extends RuntimeException {
    public ResponseMappingException() {
    }

    public ResponseMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
