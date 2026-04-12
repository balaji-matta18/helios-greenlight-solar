package com.productapp.backend.exception;

public class DuplicateServiceNumberException extends RuntimeException {
    public DuplicateServiceNumberException(String serviceNumber) {
        super("A submission already exists for service number: " + serviceNumber);
    }
}