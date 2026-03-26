package com.example.backend.controller;

import com.example.backend.service.GooglePlacesService;
import com.example.backend.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/trip")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://tt-frontend-3wjf.vercel.app"})
public class TripController {

    @Autowired
    private TripService tripService;

    @Autowired
    private GooglePlacesService googlePlacesService;

    @GetMapping("/photo")
    public ResponseEntity<Void> proxyPhoto(
            @RequestParam(required = false) String photoReference,
            @RequestParam(required = false) String name) {

        String url = null;

        if (photoReference != null && !photoReference.isEmpty()) {
            url = googlePlacesService.buildPhotoUrl(photoReference, 400);
        } else if (name != null && !name.isEmpty()) {
            try {
                var list = googlePlacesService.getTouristPlaces(name);
                if (list != null && !list.isEmpty()) {
                    var first = list.get(0);
                    Object photos = first.get("photos");
                    if (photos instanceof java.util.List && !((java.util.List<?>) photos).isEmpty()) {
                        Object mapObj = ((java.util.List<?>) photos).get(0);
                        if (mapObj instanceof Map) {
                            Object ref = ((Map<?, ?>) mapObj).get("photo_reference");
                            if (ref instanceof String) {
                                url = googlePlacesService.buildPhotoUrl((String) ref, 400);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (url == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @PostMapping("/generate")
    public Map<String,Object> generateTrip(@RequestBody Map<String,String> request){

        String destination = request.get("destination");
        String fromLocation = request.get("fromLocation");
        String from = request.get("from");
        String to = request.get("to");
        String budget = request.get("budget");

        return tripService.generatePlan(destination, fromLocation, from, to, budget);
    }
}