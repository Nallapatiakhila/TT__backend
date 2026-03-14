package com.example.backend.service;

import org.springframework.stereotype.Service;

@Service
public class SMSService {

    public void sendSMS(String phone, String message) {

        System.out.println("================================");
        System.out.println("SMS SERVICE (TEST MODE)");
        System.out.println("Phone: " + phone);
        System.out.println("Message: " + message);
        System.out.println("================================");

    }

}