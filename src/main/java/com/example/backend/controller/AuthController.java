package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.OtpService;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AuthController {

    private final UserRepository userRepository;
    private final OtpService otpService;

    public AuthController(UserRepository userRepository, OtpService otpService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
    }

    // REGISTER USER
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {

        String name = body.get("name");
        String email = body.get("email");
        String phone = body.get("phone");
        String password = body.get("password");

        Map<String, Object> response = new HashMap<>();

        // check if email already exists
        User existingUser = userRepository.findByEmail(email);
        if (existingUser != null) {
            response.put("message", "Email already registered");
            return response;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(password);

        userRepository.save(user);

        otpService.sendOtp(email);

        response.put("message", "OTP sent successfully");

        return response;
    }

    // LOGIN USER
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {

        String email = body.get("email");
        String password = body.get("password");

        Map<String, Object> response = new HashMap<>();

        User user = userRepository.findByEmail(email);

        if (user == null) {
            response.put("message", "User not found");
            return response;
        }

        if (!user.getPassword().equals(password)) {
            response.put("message", "Invalid password");
            return response;
        }

        otpService.sendOtp(email);

        response.put("message", "OTP sent successfully");

        return response;
    }
    @PostMapping("/verify-otp")
    public Map<String,Object> verifyOtp(@RequestBody Map<String,String> body){

    String email = body.get("email");
    String otp = body.get("otp");

    Map<String,Object> response = new HashMap<>();

    boolean isValid = otpService.verifyOtp(email, otp);

    if(isValid){
        response.put("message","OTP verified successfully");
    } else {
        response.put("message","OTP verification failed");
    }

    return response;
}
}