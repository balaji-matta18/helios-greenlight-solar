package com.productapp.backend.exception;

public class SubmissionNotFoundException extends RuntimeException {
    public SubmissionNotFoundException(String identifier) {
        super("Submission not found: " + identifier);
    }
}