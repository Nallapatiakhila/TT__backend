package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class DestinationAIService {

    @Value("${huggingface.token}")
    private String token;

    private static final String API_URL =
            "https://router.huggingface.co/v1/chat/completions";

    public String suggestDestination(String mood) {

        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String prompt =
                    "Suggest a travel destination in India for someone who feels "
                            + mood +
                            ". Give destination name, reason and recommended days.";

            Map<String, Object> body = new HashMap<>();

            body.put("model", "meta-llama/Llama-3.1-8B-Instruct");
            body.put("temperature", 0.4);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> user = new HashMap<>();
            user.put("role", "user");
            user.put("content", prompt);

            messages.add(user);

            body.put("messages", messages);

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(API_URL, request, Map.class);

            Map responseBody = response.getBody();

            List choices = (List) responseBody.get("choices");
            Map firstChoice = (Map) choices.get(0);
            Map message = (Map) firstChoice.get("message");

            return message.get("content").toString();

        } catch (Exception e) {

            System.out.println("Destination AI Error: " + e.getMessage());

        }

        return "AI destination unavailable.";
    }
}