package com.productapp.backend.exception;

public class SubmissionAlreadySubmittedException extends RuntimeException {
    public SubmissionAlreadySubmittedException(String serviceNumber) {
        super("Service number " + serviceNumber
                + " already has a submission. Please use the Edit option.");
    }
}