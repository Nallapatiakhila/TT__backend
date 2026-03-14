// package com.example.backend.service;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.http.*;

// import java.util.*;

// @Service
// public class AIService {

//     @Value("${huggingface.token}")
//     private String token;

//     private static final String API_URL =
//             "https://router.huggingface.co/v1/chat/completions";

//     public String generateTripExplanation(String mood, String destination, int days) {

//         try {

//             RestTemplate restTemplate = new RestTemplate();

//             HttpHeaders headers = new HttpHeaders();
//             headers.setBearerAuth(token);
//             headers.setContentType(MediaType.APPLICATION_JSON);

//             String prompt =
//                     "Create a " + days + " day travel itinerary for a person feeling "
//                             + mood +
//                             " visiting " +
//                             destination +
//                             ". Include places to visit and food suggestions. Format day wise.";

//             Map<String, Object> body = new HashMap<>();
//             body.put("model", "meta-llama/Llama-3.1-8B-Instruct");
//             body.put("temperature", 0);

//             List<Map<String, String>> messages = new ArrayList<>();

//             Map<String, String> userMessage = new HashMap<>();
//             userMessage.put("role", "user");
//             userMessage.put("content", prompt);

//             messages.add(userMessage);

//             body.put("messages", messages);

//             HttpEntity<Map<String, Object>> request =
//                     new HttpEntity<>(body, headers);

//             ResponseEntity<Map> response =
//                     restTemplate.postForEntity(API_URL, request, Map.class);

//             Map responseBody = response.getBody();

//             List choices = (List) responseBody.get("choices");
//             Map firstChoice = (Map) choices.get(0);
//             Map message = (Map) firstChoice.get("message");

//             return message.get("content").toString().trim();

//         } catch (Exception e) {

//             System.out.println("AI ERROR: " + e.getMessage());

//         }

//         return "AI itinerary unavailable.";
//     }
// }






// package com.example.backend.service;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.http.*;

// import java.util.*;

// @Service
// public class AIService {

//     @Value("${huggingface.token}")
//     private String token;

//     private static final String API_URL =
//             "";

//     public String generateTripExplanation(String mood, String destination, int days) {

//         try {

//             RestTemplate restTemplate = new RestTemplate();

//             HttpHeaders headers = new HttpHeaders();
//             headers.set("Authorization", "Bearer " + token);
//             headers.setContentType(MediaType.APPLICATION_JSON);

//             String prompt =
//                     "Create a " + days + " day travel itinerary for a person feeling "
//                             + mood +
//                             " visiting " +
//                             destination +
//                             ". Include places to visit and food suggestions. Format day wise.";

//             Map<String, String> body = new HashMap<>();
//             body.put("inputs", prompt);

//             HttpEntity<Map<String, String>> request =
//                     new HttpEntity<>(body, headers);

//             ResponseEntity<List> response =
//                     restTemplate.exchange(API_URL, HttpMethod.POST, request, List.class);

//             List result = response.getBody();

//             if (result != null && !result.isEmpty()) {

//                 Map map = (Map) result.get(0);

//                 return map.get("generated_text").toString();
//             }

//         } catch (Exception e) {

//             System.out.println("AI ERROR: " + e.getMessage());

//         }

//         return "AI itinerary unavailable.";
//     }
// }

package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class AIService {

    @Value("${huggingface.token}")
    private String token;

    private static final String API_URL =
            "https://router.huggingface.co/v1/chat/completions";

    public String askAI(String prompt) {

        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String,Object> body = new HashMap<>();

            body.put("model","meta-llama/Llama-3.1-8B-Instruct");
            body.put("temperature",0.7);

            List<Map<String,String>> messages = new ArrayList<>();

            Map<String,String> msg = new HashMap<>();
            msg.put("role","user");
            msg.put("content",prompt);

            messages.add(msg);

            body.put("messages",messages);

            HttpEntity<Map<String,Object>> request =
                    new HttpEntity<>(body,headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(API_URL,request,Map.class);

            Map responseBody = response.getBody();

            List choices = (List) responseBody.get("choices");
            Map firstChoice = (Map) choices.get(0);
            Map message = (Map) firstChoice.get("message");

            return message.get("content").toString().trim();

        } catch(Exception e){

            return "AI failed to generate response.";

        }
    }

    public String suggestDestination(String mood){

        String prompt =
                "Suggest one travel destination based on mood: "
                + mood +
                ". Only return city name.";

        return askAI(prompt);
    }

    public String generateTripExplanation(String mood,String destination,int days){

        String prompt =
                "Create a " + days +
                " day travel itinerary for a person feeling "
                + mood +
                " visiting " + destination +
                ". Include places and food suggestions.";

        return askAI(prompt);
    }

}