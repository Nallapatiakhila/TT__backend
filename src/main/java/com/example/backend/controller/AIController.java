package com.example.backend.controller;

import com.example.backend.service.DestinationAIService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AIController {

    @Autowired
    private DestinationAIService destinationAIService;

    @PostMapping("/destination")
    public Map<String,String> suggestTrip(@RequestBody Map<String,String> body){

        String mood = body.getOrDefault("mood", "").trim();
        if (mood.isEmpty()) {
            mood = "happy";
        }

        String suggestion = null;
        try {
            suggestion = destinationAIService.suggestDestination(mood);
        } catch (Exception ignored) {
        }

        if (suggestion == null || suggestion.isEmpty()) {
            suggestion = getFallbackSuggestion(mood);
        }

        Map<String,String> res = new HashMap<>();
        res.put("suggestion", suggestion);

        return res;
    }

    private String getFallbackSuggestion(String mood) {
        // Provide a simple mood-based fallback if AI is unavailable.
        String destination;
        if (mood.toLowerCase().contains("relax") || mood.toLowerCase().contains("calm") || mood.toLowerCase().contains("peace")) {
            destination = "Kerala";
        } else if (mood.toLowerCase().contains("party") || mood.toLowerCase().contains("fun") || mood.toLowerCase().contains("excited")) {
            destination = "Goa";
        } else if (mood.toLowerCase().contains("adventure") || mood.toLowerCase().contains("thrill")) {
            destination = "Manali";
        } else if (mood.toLowerCase().contains("romantic") || mood.toLowerCase().contains("love")) {
            destination = "Udaipur";
        } else {
            destination = "Goa";
        }

        return "Suggested Trip for Mood: " + mood + "\n\n" +
                "Destination: " + destination + "\n\n" +
                "Day 1: Explore the top sights and local cuisine.\n" +
                "Day 2: Visit iconic landmarks and enjoy scenic views.\n" +
                "Day 3: Relax and soak in the local culture.";
    }

}