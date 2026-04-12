package com.productapp.backend.exception;

public class AdminNotFoundException extends RuntimeException {

    public AdminNotFoundException(String username) {
        super("Admin not found with username: " + username);
    }
}