package com.productapp.backend.exception;

public class OtpExpiredException extends RuntimeException {

    public OtpExpiredException() {
        super("OTP has expired");
    }
}