package com.productapp.backend.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email) {
        super("A surveyor is already registered with email: " + email);
    }
}