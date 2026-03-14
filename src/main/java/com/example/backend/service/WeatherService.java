package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class WeatherService {

    @Value("${weather.api.key}")
    private String apiKey;

    public List<String> getForecast(String city) {

        String url =
                "https://api.openweathermap.org/data/2.5/forecast?q="
                        + city
                        + "&appid="
                        + apiKey
                        + "&units=metric";

        RestTemplate restTemplate = new RestTemplate();

        Map response = restTemplate.getForObject(url, Map.class);

        List<Map<String, Object>> list =
                (List<Map<String, Object>>) response.get("list");

        List<String> weatherList = new ArrayList<>();

        for (int i = 0; i < 8 && i < list.size(); i++) {

            Map weather =
                    (Map)((List)list.get(i).get("weather")).get(0);

            weatherList.add(weather.get("main").toString());
        }

        return weatherList;
    }
}