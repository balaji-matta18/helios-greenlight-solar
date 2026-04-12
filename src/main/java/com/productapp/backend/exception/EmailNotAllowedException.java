package com.productapp.backend.exception;

public class EmailNotAllowedException extends RuntimeException {
    public EmailNotAllowedException(String email) {
        super("Email is not authorized to access this application: " + email);
    }
}