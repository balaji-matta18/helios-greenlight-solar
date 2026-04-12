package com.productapp.backend.exception;

public class SurveyorNotFoundException extends RuntimeException {
    public SurveyorNotFoundException(String identifier) {
        super("Surveyor not found: " + identifier);
    }
}