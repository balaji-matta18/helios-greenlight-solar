package com.productapp.backend.exception;

public class UserNotFoundException extends RuntimeException {

    // For ID-based lookup
    public UserNotFoundException(Long userId) {
        super("User not found with id: " + userId);
    }

    // For mobile-based lookup
    public UserNotFoundException(String mobileNumber) {
        super("User not found with mobile number: " + mobileNumber);
    }
}