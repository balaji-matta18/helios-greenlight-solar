package com.productapp.backend.exception;

public class TooManyOtpRequestsException extends RuntimeException {

    public TooManyOtpRequestsException(int cooldownSeconds) {
        super("Please wait " + cooldownSeconds + " seconds before requesting a new OTP");
    }
}