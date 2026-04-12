package com.productapp.backend.exception;

public class OtpNotFoundException extends RuntimeException {

    public OtpNotFoundException(String mobileNumber) {
        super("No OTP found for mobile number: " + mobileNumber);
    }
}