package com.example.backend.controller;

import com.example.backend.entity.SavedTrip;
import com.example.backend.repository.SavedTripRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trip")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class SavedTripController {

    @Autowired
    private SavedTripRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/save")
    public ResponseEntity<Map<String,String>> saveTrip(@RequestBody Map<String,Object> body){
        try {
            SavedTrip trip = new SavedTrip();

            trip.setDestination((String) body.get("destination"));
            trip.setFromDate((String) body.get("from"));
            trip.setToDate((String) body.get("to"));

            // Persist the full trip payload so we can restore flight details and plan in one go
            String planJson = mapper.writeValueAsString(body);
            trip.setPlanJson(planJson);

            repository.save(trip);

            return ResponseEntity.ok(Map.of("message", "Trip Saved"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to save trip"));
        }
    }

    @GetMapping("/saved")
    public ResponseEntity<?> getSavedTrips() {
        var trips = repository.findAll();
        return ResponseEntity.ok(
                trips.stream().map(trip -> {
                    Map<String, Object> t = new HashMap<>();
                    t.put("id", trip.getId());
                    t.put("destination", trip.getDestination());
                    t.put("fromDate", trip.getFromDate());
                    t.put("toDate", trip.getToDate());
                    try {
                        Map<String, Object> parsed = mapper.readValue(
                                trip.getPlanJson(),
                                new TypeReference<Map<String, Object>>() {}
                        );

                        // Preserve compatibility with previous saved data
                        Object planObj = parsed.get("plan");
                        t.put("plan", planObj instanceof List ? planObj : Collections.emptyList());
                        t.put("flightCost", parsed.getOrDefault("flightCost", 0));
                        t.put("flightDetails", parsed.getOrDefault("flightDetails", ""));
                        t.put("aiExplanation", parsed.getOrDefault("aiExplanation", ""));
                    } catch (Exception ignored) {
                        t.put("plan", Collections.emptyList());
                        t.put("flightCost", 0);
                        t.put("flightDetails", "");
                        t.put("aiExplanation", "");
                    }
                    return t;
                }).toList()
        );
    }
}

