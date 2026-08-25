package com.medical.insurance.exception;

public final class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
