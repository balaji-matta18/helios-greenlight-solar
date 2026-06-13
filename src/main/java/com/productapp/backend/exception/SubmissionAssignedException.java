package com.productapp.backend.exception;

public class SubmissionAssignedException extends RuntimeException {
    public SubmissionAssignedException(String serviceNumber) {
        super("Service number '" + serviceNumber + "' is already assigned to another surveyor.");
    }
}
