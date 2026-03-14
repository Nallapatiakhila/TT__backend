package com.example.backend.service;

import com.example.backend.dto.RegisterRequest;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    // Register user
    public String register(RegisterRequest request) {

        User existingUser = userRepository.findByEmail(request.getEmail());

        if (existingUser != null) {
            return "User already exists";
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());

        userRepository.save(user);

        return "User Registered Successfully";
    }

    // Login validation
    public String login(String email, String password) {

        System.out.println("LOGIN ATTEMPT");
        System.out.println("Email: " + email);
        System.out.println("Password: " + password);

        User user = userRepository.findByEmail(email);

        if (user == null) {
            System.out.println("User not found");
            return "User not found";
        }

        System.out.println("DB Password: " + user.getPassword());

        if (!user.getPassword().trim().equals(password.trim())) {
            System.out.println("Password mismatch");
            return "Invalid password";
        }

        return "VALID";
    }

    // Get user phone for OTP
    public String getUserPhone(String email) {

        User user = userRepository.findByEmail(email);

        if (user == null) {
            return null;
        }

        return user.getPhone();
    }
}