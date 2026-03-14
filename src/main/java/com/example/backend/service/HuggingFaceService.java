package com.example.backend.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HuggingFaceService {

    private static final String API_URL =
            "https://api-inference.huggingface.co/models/google/flan-t5-large";

    private static final String API_KEY = "REMOVED";

    public String getAIResponse(String prompt){

        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(API_KEY);

            String body =
                    "{ \"inputs\": \"" + prompt + "\" }";

            HttpEntity<String> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            API_URL,
                            HttpMethod.POST,
                            request,
                            String.class
                    );

            return response.getBody();

        } catch(Exception e){

            e.printStackTrace();
            return "AI explanation could not be generated.";

        }
    }
}