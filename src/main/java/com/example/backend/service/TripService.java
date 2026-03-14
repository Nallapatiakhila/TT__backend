package com.example.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class TripService {

    private final GooglePlacesService googlePlacesService;

    // Cache Google Places responses per destination (stability between refreshes)
    private static final Map<String, List<Map<String, Object>>> touristPlacesCache = new HashMap<>();
    private static final Map<String, List<Map<String, Object>>> restaurantsCache = new HashMap<>();
    private static final Map<String, List<Map<String, Object>>> hotelsCache = new HashMap<>();

    public TripService(GooglePlacesService googlePlacesService) {
        this.googlePlacesService = googlePlacesService;
    }

    public Map<String,Object> generatePlan(
            String destination,
            String fromLocation,
            String from,
            String to,
            String budget
    ){

        Map<String,Object> result = new HashMap<>();
        List<Map<String,Object>> plan = new ArrayList<>();

        try{

            if (destination == null || destination.isEmpty()) {
                destination = "India";
            }

            // Keep a stable, effectively-final copy for caching and lambda usage
            final String destinationForCache = destination;

            LocalDate start = LocalDate.parse(from);
            LocalDate end = LocalDate.parse(to);

            int days = (int) ChronoUnit.DAYS.between(start, end) + 1;

            // Make plan generation deterministic based on input values
            int seed = Objects.hash(destinationForCache.trim().toLowerCase(), fromLocation == null ? "" : fromLocation.trim().toLowerCase(), from, to, budget);
            Random random = new Random(seed);

            // Budget-based cost multiplier
            int placeBase;
            int foodBase;
            int hotelBase;
            int flightBase;
            switch (budget == null ? "" : budget.toLowerCase()) {
                case "high budget":
                    placeBase = 500;
                    foodBase = 400;
                    hotelBase = 8000;
                    flightBase = 18000;
                    break;
                case "medium budget":
                    placeBase = 350;
                    foodBase = 250;
                    hotelBase = 5000;
                    flightBase = 13000;
                    break;
                default:
                    placeBase = 200;
                    foodBase = 150;
                    hotelBase = 3000;
                    flightBase = 9000;
                    break;
            }

            // Some sample place and restaurant options for popular destinations
            Map<String, List<String>> placeNames = Map.of(
                    "goa", List.of("Baga Beach", "Fort Aguada", "Anjuna Market", "Old Goa Basilica", "Calangute Beach"),
                    "kerala", List.of("Munnar Tea Gardens", "Alleppey Backwaters", "Kovalam Beach", "Thekkady Wildlife Sanctuary", "Fort Kochi"),
                    "manali", List.of("Rohtang Pass", "Hidimba Devi Temple", "Solang Valley", "Mall Road", "Vashisht Hot Springs"),
                    "ooty", List.of("Ooty Lake", "Doddabetta Peak", "Botanical Gardens", "Rose Garden", "Nilgiri Mountain Railway"),
                    "coorg", List.of("Abbey Falls", "Raja's Seat", "Dubare Elephant Camp", "Talakaveri", "Nisargadhama"),
                    "hyderabad", List.of("Charminar", "Golconda Fort", "Hussain Sagar", "Ramoji Film City", "Birla Mandir")
            );

            Map<String, List<String>> restaurantNames = Map.of(
                    "goa", List.of("Fisherman's Wharf", "Thalassa", "Gunpowder", "Bomra's", "Vinayak Family Restaurant"),
                    "kerala", List.of("Kashi Art Cafe", "Thaff", "Dhe Puttu", "Kuttanad Seafood Restaurant", "Fort House Restaurant"),
                    "manali", List.of("Johnson's Cafe", "The Lazy Dog", "Cafe 1947", "Il Forno", "Tibetan Kitchen"),
                    "ooty", List.of("1847", "Shinkows", "Hyderabad Biryani House", "Sri Krishna Inn", "Hotel Lakeview"),
                    "coorg", List.of("Coorg Cuisine", "Raintree", "The Falls", "Madikeri Club", "Barrista"),
                    "hyderabad", List.of("Paradise Biryani", "Chutney's", "Ohri's", "Bawarchi", "Sahib Sindh Sultan")
            );

            String destKey = destination.trim().toLowerCase();

            // Preload Google Places results (if available) for more accurate images and names.
            List<Map<String, Object>> googlePlaces = Collections.emptyList();
            Map<String, List<Map<String, Object>>> googleRestaurantsByPlace = new HashMap<>();
            Map<String, Object> destinationInfo = null;
            Double destinationLat = null;
            Double destinationLng = null;
            String destinationLabel = destination;

            try {
                String cacheKey = destinationForCache.trim().toLowerCase();
                googlePlaces = touristPlacesCache.computeIfAbsent(cacheKey, k ->
                        Optional.ofNullable(googlePlacesService.getTouristPlaces(destinationForCache))
                                .orElse(Collections.emptyList())
                );
            } catch (Exception ignored) {
            }

            try {
                destinationInfo = googlePlacesService.getPlaceLocation(destination);
                if (destinationInfo != null) {
                    if (destinationInfo.get("lat") instanceof Number) {
                        destinationLat = ((Number) destinationInfo.get("lat")).doubleValue();
                    }
                    if (destinationInfo.get("lng") instanceof Number) {
                        destinationLng = ((Number) destinationInfo.get("lng")).doubleValue();
                    }
                    if (destinationInfo.get("name") instanceof String) {
                        destinationLabel = (String) destinationInfo.get("name");
                    }
                }
            } catch (Exception ignored) {
            }

            // If the destination isn't in our predefined map, create a predictable list of places/restaurants
            if (!placeNames.containsKey(destKey)) {
                placeNames = new HashMap<>(placeNames);
                placeNames.put(destKey, generateFallbackNames(destination, "Place", days));
            }
            if (!restaurantNames.containsKey(destKey)) {
                restaurantNames = new HashMap<>(restaurantNames);
                restaurantNames.put(destKey, generateFallbackNames(destination, "Restaurant", days));
            }

            List<String> destPlaces = placeNames.getOrDefault(destKey, Collections.emptyList());
            List<String> destRests = restaurantNames.getOrDefault(destKey, Collections.emptyList());

            // Flight estimate (one-way + return) based on budget and distance (deterministic)
            int flightCost = calculateFlightCost(fromLocation, destination, budget, seed);
            String flightDetails = generateFlightDetails(fromLocation, destination, seed);

            for(int i=0;i<days;i++){

                Map<String,Object> day = new HashMap<>();

                // Make costs deterministic based on seed and day index
                int placeCost = placeBase + Math.abs(Objects.hash(seed, i, "place") % 150);
                int foodCost = foodBase + Math.abs(Objects.hash(seed, i, "food") % 150);
                int hotelCost = hotelBase + Math.abs(Objects.hash(seed, i, "hotel") % 2000);

                String placeName = destPlaces.isEmpty()
                        ? destinationLabel + " Tourist Place " + (i + 1)
                        : destPlaces.get(i % destPlaces.size());

                // Ensure place name reflects the destination label as well
                placeName = ensureDestinationInName(placeName, destinationLabel);

                String restaurantName = destRests.isEmpty()
                        ? destinationLabel + " Restaurant " + (i + 1)
                        : destRests.get(i % destRests.size());

                // Ensure restaurant name reflects the destination label as well
                restaurantName = ensureDestinationInName(restaurantName, destinationLabel);

                String hotelName = destinationLabel + " Hotel " + (i + 1);

                // Ensure hotel name reflects the destination label as well
                hotelName = ensureDestinationInName(hotelName, destinationLabel);

                // Attempt to use Google Places API results for better names + images.
                Map<String, Object> selectedGooglePlace = null;
                String photoUrl = null;
                if (!googlePlaces.isEmpty()) {
                    selectedGooglePlace = googlePlaces.get(Math.floorMod(seed + i, googlePlaces.size()));
                    placeName = (String) selectedGooglePlace.getOrDefault("name", placeName);
                    photoUrl = extractPhotoUrl(selectedGooglePlace);
                }

                // Try to find restaurants near the selected place (more realistic pairing)
                List<Map<String, Object>> placeRestaurants = googleRestaurantsByPlace.get(placeName);
                if (placeRestaurants == null) {
                    try {
                        // Prefer using geographic coordinates to find nearby restaurants
                        Double lat = null;
                        Double lng = null;
                        Object geometry = selectedGooglePlace != null ? selectedGooglePlace.get("geometry") : null;
                        if (geometry instanceof Map) {
                            Object location = ((Map<?, ?>) geometry).get("location");
                            if (location instanceof Map) {
                                Object latObj = ((Map<?, ?>) location).get("lat");
                                Object lngObj = ((Map<?, ?>) location).get("lng");
                                if (latObj instanceof Number && lngObj instanceof Number) {
                                    lat = ((Number) latObj).doubleValue();
                                    lng = ((Number) lngObj).doubleValue();
                                }
                            }
                        }

                        String restKey = (destinationLabel + "|" + budget).toLowerCase();
                        List<Map<String, Object>> computedRestaurants;
                        if (lat != null && lng != null) {
                            computedRestaurants = Optional.ofNullable(
                                            googlePlacesService.getRestaurantsNearLocation(lat, lng, 1800, budget))
                                    .orElse(Collections.emptyList());
                        } else {
                            computedRestaurants = Optional.ofNullable(
                                            googlePlacesService.getRestaurantsNear(destinationLabel, budget))
                                    .orElse(Collections.emptyList());
                        }
                        placeRestaurants = restaurantsCache.computeIfAbsent(restKey, k -> computedRestaurants);
                    } catch (Exception ignored) {
                        placeRestaurants = Collections.emptyList();
                    }
                    googleRestaurantsByPlace.put(placeName, placeRestaurants);
                }

                if (!placeRestaurants.isEmpty()) {
                    Map<String, Object> googleRestaurant = placeRestaurants.get(Math.floorMod(seed + i, placeRestaurants.size()));
                    restaurantName = (String) googleRestaurant.getOrDefault("name", restaurantName);

                    // Ensure restaurant name reflects destination
                    restaurantName = ensureDestinationInName(restaurantName, destination);

                    // Only use the restaurant photo when we did not get one from the place.
                    if (photoUrl == null) {
                        photoUrl = extractPhotoUrl(googleRestaurant);
                    }
                }

                // Attempt to locate a hotel near the same place
                List<Map<String, Object>> placeHotels = Collections.emptyList();
                try {
                    Double lat = null;
                    Double lng = null;
                    Object geometry = selectedGooglePlace != null ? selectedGooglePlace.get("geometry") : null;
                    if (geometry instanceof Map) {
                        Object location = ((Map<?, ?>) geometry).get("location");
                        if (location instanceof Map) {
                            Object latObj = ((Map<?, ?>) location).get("lat");
                            Object lngObj = ((Map<?, ?>) location).get("lng");
                            if (latObj instanceof Number && lngObj instanceof Number) {
                                lat = ((Number) latObj).doubleValue();
                                lng = ((Number) lngObj).doubleValue();
                            }
                        }
                    }

                    String hotelKey = destinationLabel.toLowerCase();
                    List<Map<String, Object>> computedHotels;
                    if (lat != null && lng != null) {
                        computedHotels = Optional.ofNullable(
                                        googlePlacesService.getHotelsNearLocation(lat, lng, 1500))
                                .orElse(Collections.emptyList());
                    } else if (destinationLat != null && destinationLng != null) {
                        computedHotels = Optional.ofNullable(
                                        googlePlacesService.getHotelsNearLocation(destinationLat, destinationLng, 1500))
                                .orElse(Collections.emptyList());
                    } else {
                        computedHotels = Optional.ofNullable(
                                        googlePlacesService.getHotelsInDestination(destinationLabel))
                                .orElse(Collections.emptyList());
                    }
                    placeHotels = hotelsCache.computeIfAbsent(hotelKey, k -> computedHotels);
                } catch (Exception ignored) {
                }

                if (!placeHotels.isEmpty()) {
                    Map<String, Object> hotel = placeHotels.get(Math.floorMod(seed + i, placeHotels.size()));
                    hotelName = (String) hotel.getOrDefault("name", hotelName);

                    hotelName = ensureDestinationInName(hotelName, destination);
                }

                // Fallback to Unsplash if no Google photo is available
                if (photoUrl == null) {
                    String query = (destination + " " + placeName).replaceAll("\\s+", "+");
                    photoUrl = "https://source.unsplash.com/400x250/?" + query + "&sig=" + (seed + i);
                }

                day.put("day","Day "+(i+1));
                day.put("place", placeName);
                day.put("restaurant", restaurantName);
                day.put("hotel", hotelName);
                day.put("weather","Sunny");
                day.put("photoUrl", photoUrl);

                day.put("placeCost",placeCost);
                day.put("foodCost",foodCost);
                day.put("hotelCost",hotelCost);
                day.put("dailyCost",placeCost + foodCost + hotelCost);

                plan.add(day);
            }

            int totalCost = plan.stream()
                    .mapToInt(d -> (int)d.get("dailyCost"))
                    .sum();

            result.put("plan",plan);
            result.put("flightCost", flightCost);
            result.put("flightDetails", flightDetails);
            result.put("totalCost",totalCost + flightCost);
            result.put("aiExplanation",
                    "Day 1: Explore " + destination + " and experience the local culture.\n" +
                    "Day 2: Try the best local restaurants and street food.\n" +
                    "Day 3: See the top attractions and relax.");

        }
        catch(Exception e){

            System.out.println("ERROR OCCURRED: "+e.getMessage());

            result.put("plan",new ArrayList<>());
            result.put("totalCost",0);
            result.put("aiExplanation","Trip generation failed internally");

        }

        return result;
    }

    private List<String> generateFallbackNames(String destination, String suffix, int count) {
        String base = destination.trim().replaceAll("\\s+", " ");
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            result.add(base + " " + suffix + " " + i);
        }
        return result;
    }

    private String extractPhotoUrl(Map<String, Object> placeData) {
        if (placeData == null) {
            return null;
        }

        Object photosObj = placeData.get("photos");
        if (photosObj instanceof List) {
            List<?> photos = (List<?>) photosObj;
            if (!photos.isEmpty() && photos.get(0) instanceof Map) {
                Object photoRef = ((Map<?, ?>) photos.get(0)).get("photo_reference");
                if (photoRef instanceof String) {
                    return googlePlacesService.buildPhotoUrl((String) photoRef, 400);
                }
            }
        }
        return null;
    }

    private int calculateFlightCost(String fromLocation, String destination, String budget, int seed) {
        if (fromLocation == null || fromLocation.isEmpty()) {
            fromLocation = "Delhi"; // Default departure city
        }

        // Base distance factors (rough estimates in km)
        Map<String, Map<String, Integer>> distanceMap = Map.of(
            "delhi", Map.of("goa", 1700, "kerala", 2100, "manali", 500, "ooty", 2100, "coorg", 2400, "hyderabad", 1500),
            "mumbai", Map.of("goa", 450, "kerala", 1400, "manali", 1500, "ooty", 1200, "coorg", 1000, "hyderabad", 700),
            "bangalore", Map.of("goa", 600, "kerala", 500, "manali", 2400, "ooty", 300, "coorg", 250, "hyderabad", 570),
            "chennai", Map.of("goa", 900, "kerala", 700, "manali", 2500, "ooty", 530, "coorg", 480, "hyderabad", 630)
        );

        String fromKey = fromLocation.trim().toLowerCase();
        String destKey = destination.trim().toLowerCase();

        // Get distance or use default
        int distance = 1500; // Default distance
        if (distanceMap.containsKey(fromKey) && distanceMap.get(fromKey).containsKey(destKey)) {
            distance = distanceMap.get(fromKey).get(destKey);
        }

        // Base cost per km based on budget
        double costPerKm;
        switch (budget == null ? "" : budget.toLowerCase()) {
            case "high budget":
                costPerKm = 8.0;
                break;
            case "medium budget":
                costPerKm = 5.5;
                break;
            default:
                costPerKm = 3.5;
                break;
        }

        // Calculate round trip cost with deterministic variation based on seed
        int baseCost = (int) (distance * costPerKm * 2); // Round trip
        int variation = (int) (baseCost * 0.3); // ±30% variation
        Random random = new Random(seed);
        return baseCost + random.nextInt(variation * 2) - variation;
    }

    private String generateFlightDetails(String fromLocation, String destination, int seed) {
        if (fromLocation == null || fromLocation.isEmpty()) {
            fromLocation = "Delhi";
        }

        Random random = new Random(seed);
        String[] airlines = {"Air India", "IndiGo", "SpiceJet", "Vistara", "GoAir", "AirAsia India"};
        String airline = airlines[Math.floorMod(seed, airlines.length)];
        String flightNumber = String.format("%s %d", airline.substring(0, 2).toUpperCase(), 100 + Math.abs(seed % 900));

        return String.format("%s - %s to %s", flightNumber, fromLocation, destination);
    }

    private String ensureDestinationInName(String name, String destination) {
        if (name == null || destination == null || destination.isEmpty()) {
            return name;
        }
        String dest = destination.trim();
        if (name.toLowerCase().contains(dest.toLowerCase())) {
            return name;
        }
        return dest + " " + name;
    }
}
