package com.example.backend.controller;

import com.example.backend.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trip")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class TripController {

    @Autowired
    private TripService tripService;

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