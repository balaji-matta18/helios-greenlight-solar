//package com.productapp.backend.service;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@Configuration
//public class PasswordGenerator {
//
//    @Bean
//    public CommandLineRunner generatePassword(PasswordEncoder passwordEncoder) {
//        return args -> {
//            String rawPassword = "admin123";
//            String encoded = passwordEncoder.encode(rawPassword);
//            System.out.println("Generated Hash: " + encoded);
//        };
//    }
//}