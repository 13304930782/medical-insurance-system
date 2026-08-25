package com.medical.insurance.model;

public record PasswordResetMailRequested(String address, String code) {
}
