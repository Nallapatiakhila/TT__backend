package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GooglePlacesService {

    @Value("${google.places.key}")
    private String apiKey;

    public List<Map<String, Object>> getTouristPlaces(String city) {

        String url =
                "https://maps.googleapis.com/maps/api/place/textsearch/json?query=tourist+places+in+"
                        + city + "&key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();

        Map response = restTemplate.getForObject(url, Map.class);

        return (List<Map<String, Object>>) response.get("results");
    }

    public List<Map<String, Object>> getRestaurants(String city) {

        String url =
                "https://maps.googleapis.com/maps/api/place/textsearch/json?query=restaurants+in+"
                        + URLEncoder.encode(city, StandardCharsets.UTF_8) + "&key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();

        Map response = restTemplate.getForObject(url, Map.class);

        return (List<Map<String, Object>>) response.get("results");
    }

    public List<Map<String, Object>> getRestaurantsNear(String location, String budget) {
        // Fallback to text search if we don't have exact location coordinates
        String priceQuery = "";
        if ("Low Budget".equals(budget)) {
            priceQuery = " cheap";
        } else if ("High Budget".equals(budget)) {
            priceQuery = " expensive";
        }

        String url =
                "https://maps.googleapis.com/maps/api/place/textsearch/json?query=restaurants+near+" 
                        + URLEncoder.encode(location + priceQuery, StandardCharsets.UTF_8) + "&key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();

        Map response = restTemplate.getForObject(url, Map.class);

        return (List<Map<String, Object>>) response.get("results");
    }

    public List<Map<String, Object>> getRestaurantsNearLocation(double lat, double lng, int radiusMeters, String budget) {
        String priceParams = "";
        if ("Low Budget".equals(budget)) {
            priceParams = "&minprice=0&maxprice=1";
        } else if ("Medium Budget".equals(budget)) {
            priceParams = "&minprice=1&maxprice=2";
        } else if ("High Budget".equals(budget)) {
            priceParams = "&minprice=2&maxprice=4";
        }

        String url =
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                        + lat + "," + lng
                        + "&radius=" + radiusMeters
                        + "&type=restaurant"
                        + priceParams
                        + "&key=" + apiKey;
        RestTemplate restTemplate = new RestTemplate();

        Map response = restTemplate.getForObject(url, Map.class);

        return (List<Map<String, Object>>) response.get("results");
    }

    public List<Map<String, Object>> getHotelsNearLocation(double lat, double lng, int radiusMeters) {
        String url =
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                        + lat + "," + lng
                        + "&radius=" + radiusMeters
                        + "&type=lodging"
                        + "&key=" + apiKey;
        RestTemplate restTemplate = new RestTemplate();

        Map response = restTemplate.getForObject(url, Map.class);

        return (List<Map<String, Object>>) response.get("results");
    }

    public List<Map<String, Object>> getHotelsInDestination(String city) {
        String url =
                "https://maps.googleapis.com/maps/api/place/textsearch/json?query=hotels+in+" 
                        + URLEncoder.encode(city, StandardCharsets.UTF_8) + "&key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();

        Map response = restTemplate.getForObject(url, Map.class);

        return (List<Map<String, Object>>) response.get("results");
    }

    public Map<String, Object> getPlaceLocation(String placeName) {
        String url =
                "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                        + URLEncoder.encode(placeName, StandardCharsets.UTF_8) + "&key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();
        Map response = restTemplate.getForObject(url, Map.class);
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

        if (results == null || results.isEmpty()) {
            return null;
        }

        Map<String, Object> first = results.get(0);

        Object geometry = first.get("geometry");
        if (!(geometry instanceof Map)) {
            return null;
        }
        Object location = ((Map<?, ?>) geometry).get("location");
        if (!(location instanceof Map)) {
            return null;
        }

        Object latObj = ((Map<?, ?>) location).get("lat");
        Object lngObj = ((Map<?, ?>) location).get("lng");
        if (!(latObj instanceof Number) || !(lngObj instanceof Number)) {
            return null;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("lat", ((Number) latObj).doubleValue());
        info.put("lng", ((Number) lngObj).doubleValue());

        if (first.get("name") instanceof String) {
            info.put("name", first.get("name"));
        } else if (first.get("formatted_address") instanceof String) {
            info.put("name", first.get("formatted_address"));
        }

        return info;
    }

    /**
     * Build a Google Places Photo API URL for the given photo reference.
     */
    public String buildPhotoUrl(String photoReference, int maxWidth) {
        if (photoReference == null || photoReference.isEmpty()) {
            return null;
        }
        return "https://maps.googleapis.com/maps/api/place/photo?maxwidth="
                + maxWidth + "&photoreference=" + photoReference + "&key=" + apiKey;
    }
}
