package com.medical.insurance.exception;

public final class AuthValidationException extends RuntimeException {
    public AuthValidationException(String message) { super(message); }
}
