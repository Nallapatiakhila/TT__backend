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

    // Fallback naming pools for unmatched days
    private static final List<String> fallbackPlaceTypes = List.of("Beach", "Park", "Fort", "Garden", "Temple", "Museum", "Waterfall", "Market", "Lake", "Hill Station", "Historic Site", "Viewpoint");
    private static final List<String> fallbackRestaurantTypes = List.of("Cafe", "Diner", "Bistro", "Seafood", "BBQ", "Veg Restaurant", "Street Food", "Family Restaurant", "Fine Dining", "Food Court");
    private static final List<String> fallbackHotelTypes = List.of("Inn", "Resort", "Suite", "Lodge", "Boutique", "Guest House", "Executive Hotel", "Luxury Hotel", "Budget Hotel", "Homestay");

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
            Map<String, List<String>> placeNames = new HashMap<>();
            placeNames.put("goa", List.of("Baga Beach", "Fort Aguada", "Anjuna Market", "Old Goa Basilica", "Calangute Beach"));
            placeNames.put("kerala", List.of("Munnar Tea Gardens", "Alleppey Backwaters", "Kovalam Beach", "Thekkady Wildlife Sanctuary", "Fort Kochi"));
            placeNames.put("manali", List.of("Rohtang Pass", "Hidimba Devi Temple", "Solang Valley", "Mall Road", "Vashisht Hot Springs"));
            placeNames.put("ooty", List.of("Ooty Lake", "Doddabetta Peak", "Botanical Gardens", "Rose Garden", "Nilgiri Mountain Railway"));
            placeNames.put("coorg", List.of("Abbey Falls", "Raja's Seat", "Dubare Elephant Camp", "Talakaveri", "Nisargadhama"));
            placeNames.put("hyderabad", List.of("Charminar", "Golconda Fort", "Hussain Sagar", "Ramoji Film City", "Birla Mandir"));
            placeNames.put("telangana", List.of("Charminar", "Warangal Fort", "Ramoji Film City", "Chowmahalla Palace", "Kuntala Waterfall"));
            placeNames.put("andhra pradesh", List.of("Tirumala Temple", "Borra Caves", "Araku Valley", "Rishikonda Beach", "Undavalli Caves"));
            placeNames.put("maharashtra", List.of("Gateway of India", "Ajanta Caves", "Ellora Caves", "Elephanta Caves", "Marine Drive"));
            placeNames.put("mumbai", List.of("Gateway of India", "Marine Drive", "Juhu Beach", "Siddhivinayak Temple", "Colaba Causeway"));
            placeNames.put("delhi", List.of("India Gate", "Red Fort", "Qutub Minar", "Humayun's Tomb", "Lotus Temple"));
            placeNames.put("rajasthan", List.of("Amber Fort", "City Palace", "Hawa Mahal", "Jaisalmer Fort", "Mehrangarh Fort"));
            placeNames.put("jaipur", List.of("Amber Fort", "City Palace", "Hawa Mahal", "Jantar Mantar", "Albert Hall Museum"));
            placeNames.put("karnataka", List.of("Mysore Palace", "Hampi", "Coorg", "Lalbagh Botanical Garden", "Jog Falls"));
            placeNames.put("bangalore", List.of("Lalbagh Botanical Garden", "Cubbon Park", "Bangalore Palace", "Vidhana Soudha", "Nandi Hills"));
            placeNames.put("tamil nadu", List.of("Meenakshi Temple", "Marina Beach", "Brihadeeswara Temple", "Ooty", "Kanyakumari"));
            placeNames.put("chennai", List.of("Marina Beach", "Kapaleeshwarar Temple", "Santhome Cathedral", "Fort St. George", "Elliot's Beach"));
            placeNames.put("gujarat", List.of("Statue of Unity", "Gir National Park", "Rann of Kutch", "Somnath Temple", "Sabarmati Ashram"));
            placeNames.put("ahmedabad", List.of("Sabarmati Ashram", "Kankaria Lake", "Adalaj Stepwell", "Akshardham Temple", "Jama Masjid"));
            placeNames.put("west bengal", List.of("Victoria Memorial", "Howrah Bridge", "Darjeeling", "Sundarbans", "Dakshineswar Kali Temple"));
            placeNames.put("kolkata", List.of("Victoria Memorial", "Howrah Bridge", "Dakshineswar Kali Temple", "Indian Museum", "Eden Gardens"));
            placeNames.put("uttar pradesh", List.of("Taj Mahal", "Varanasi Ghats", "Agra Fort", "Fatehpur Sikri", "Bara Imambara"));
            placeNames.put("uttarakhand", List.of("Valley of Flowers", "Nainital Lake", "Kedarnath Temple", "Rishikesh", "Mussoorie"));
            placeNames.put("punjab", List.of("Golden Temple", "Jallianwala Bagh", "Wagah Border", "Sheesh Mahal", "Virasat-e-Khalsa"));
            placeNames.put("haryana", List.of("Sultanpur National Park", "Brahma Sarovar", "Pinjore Gardens", "Kingdom of Dreams", "Surajkund"));
            placeNames.put("bihar", List.of("Mahabodhi Temple", "Nalanda University", "Golghar", "Vishnupad Temple", "Vikramshila"));
            placeNames.put("madhya pradesh", List.of("Khajuraho Temples", "Gwalior Fort", "Bhimbetka Caves", "Sanchi Stupa", "Kanha National Park"));
            placeNames.put("odisha", List.of("Jagannath Temple", "Konark Sun Temple", "Chilika Lake", "Lingaraja Temple", "Udayagiri Caves"));
            placeNames.put("jammu and kashmir", List.of("Dal Lake", "Gulmarg", "Pahalgam", "Shankaracharya Temple", "Sonamarg"));
            placeNames.put("assam", List.of("Kaziranga National Park", "Kamakhya Temple", "Majuli Island", "Umananda Island", "Manas National Park"));
            placeNames.put("sikkim", List.of("Nathu La Pass", "Tsomgo Lake", "Rumtek Monastery", "Pelling", "Yumthang Valley"));
            placeNames.put("himachal pradesh", List.of("Shimla", "Rohtang Pass", "Spiti Valley", "Dharamshala", "Dalhousie"));
            placeNames.put("kerala", List.of("Munnar Tea Gardens", "Alleppey Backwaters", "Kovalam Beach", "Thekkady Wildlife", "Fort Kochi"));
            placeNames.put("chhattisgarh", List.of("Chitrakote Falls", "Tirathgarh Falls", "Kanger Valley", "Bastar Palace", "Danteshwari Temple"));
            placeNames.put("jharkhand", List.of("Dassam Falls", "Hundru Falls", "Betla National Park", "Jagannath Temple Ranchi", "Baidyanath Jyotirlinga"));

            Map<String, List<String>> restaurantNames = new HashMap<>();
            restaurantNames.put("goa", List.of("Fisherman's Wharf", "Thalassa", "Gunpowder", "Bomra's", "Vinayak Family Restaurant"));
            restaurantNames.put("kerala", List.of("Kashi Art Cafe", "Thaff", "Dhe Puttu", "Kuttanad Seafood", "Fort House Restaurant"));
            restaurantNames.put("manali", List.of("Johnson's Cafe", "The Lazy Dog", "Cafe 1947", "Il Forno", "Tibetan Kitchen"));
            restaurantNames.put("ooty", List.of("1847", "Shinkows", "Hyderabad Biryani House", "Sri Krishna Inn", "Hotel Lakeview"));
            restaurantNames.put("coorg", List.of("Coorg Cuisine", "Raintree", "The Falls", "Madikeri Club", "Barrista"));
            restaurantNames.put("hyderabad", List.of("Paradise Biryani", "Chutney's", "Ohri's", "Bawarchi", "Sahib Sindh Sultan"));
            restaurantNames.put("telangana", List.of("Paradise Biryani", "Chutney's", "Ohri's", "Bawarchi", "Karachi Bakery"));
            restaurantNames.put("andhra pradesh", List.of("Sea Inn", "The Eatery", "Dolphin Restaurant", "Gismat", "Zeeshan"));
            restaurantNames.put("maharashtra", List.of("Britannia & Co", "Leopold Cafe", "Trishna", "Bademiya", "Gajalee"));
            restaurantNames.put("mumbai", List.of("Britannia & Co", "Leopold Cafe", "Trishna", "Bademiya", "Gajalee"));
            restaurantNames.put("delhi", List.of("Karim's", "Bukhara", "Indian Accent", "Paranthe Wali Gali", "Saravana Bhavan"));
            restaurantNames.put("rajasthan", List.of("Chokhi Dhani", "1135 AD", "Suvarna Mahal", "LMB", "Spice Court"));
            restaurantNames.put("jaipur", List.of("Chokhi Dhani", "1135 AD", "Suvarna Mahal", "Lajawab", "Spice Court"));
            restaurantNames.put("karnataka", List.of("Vidyarthi Bhavan", "MTR", "Karavalli", "Truffles", "Toit"));
            restaurantNames.put("bangalore", List.of("Vidyarthi Bhavan", "MTR", "Karavalli", "Truffles", "Toit"));
            restaurantNames.put("tamil nadu", List.of("Murugan Idli Shop", "Amma Chettinad", "Saravana Bhavan", "Annalakshmi", "Dindigul Thalappakatti"));
            restaurantNames.put("chennai", List.of("Murugan Idli", "Amma Chettinad", "Saravana Bhavan", "Annalakshmi", "Dindigul Thalappakatti"));
            restaurantNames.put("gujarat", List.of("Agashiye", "Swati Snacks", "Gopi Dining Hall", "Gordhan Thal", "Vishalla"));
            restaurantNames.put("ahmedabad", List.of("Agashiye", "Swati Snacks", "Gopi Dining Hall", "Gordhan Thal", "Vishalla"));
            restaurantNames.put("west bengal", List.of("Peter Cat", "Arsalan", "Oh! Calcutta", "Mocambo", "Flurys"));
            restaurantNames.put("kolkata", List.of("Peter Cat", "Arsalan", "Oh! Calcutta", "Mocambo", "Flurys"));
            restaurantNames.put("uttar pradesh", List.of("Pinch of Spice", "Tunday Kababi", "Dastarkhwan", "Dasaprakash", "Bati Chokha"));
            restaurantNames.put("uttarakhand", List.of("Kalsang", "Chotiwala", "Little Llama Cafe", "Machan", "Sakley's"));
            restaurantNames.put("punjab", List.of("Kesar Da Dhaba", "Bharawan Da Dhaba", "Brother's Dhaba", "Pal Dhaba", "Makhan Fish"));
            restaurantNames.put("haryana", List.of("Gulshan Dhaba", "Amrik Sukhdev", "Garam Dharam", "Pehlwan Dhaba", "Haveli"));
            restaurantNames.put("bihar", List.of("Pind Balluchi", "Bansi Vihar", "Kapil Dev's Elevens", "Ghar Aangan", "Mainland China"));
            restaurantNames.put("madhya pradesh", List.of("Bapu Ki Kutia", "Ranjit's Lakeview", "Peshawri", "Za-aiqa", "Manohar Dairy"));
            restaurantNames.put("odisha", List.of("Dalma", "Bichitrananda", "Chung Wah", "Mayfair", "The Zaika"));
            restaurantNames.put("jammu and kashmir", List.of("Ahdoos", "Mughal Darbar", "Stream", "Nedou's", "Nathu's Sweets"));
            restaurantNames.put("assam", List.of("Paradise", "Khorikaa", "Maihang", "Piping Hot", "Delicacy"));
            restaurantNames.put("sikkim", List.of("Taste of Tibet", "Roll House", "Thakali", "Nimtho", "The Square"));
            restaurantNames.put("himachal pradesh", List.of("Johnson's Cafe", "Renaissance", "Cafe 1947", "Himachali Rasoi", "Chopsticks"));
            restaurantNames.put("chhattisgarh", List.of("Madhulika", "Girnar Restro", "Mocha", "Sankalp", "Barbeque Nation"));
            restaurantNames.put("jharkhand", List.of("Kaveri", "Yellow Sapphire", "Seventh Heaven", "The Great Kabab Factory", "Prana Lounge"));

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

            // Always prefer live Google Places results where available, with fallback to presets and deterministic names.
            placeNames = new HashMap<>(placeNames);
            restaurantNames = new HashMap<>(restaurantNames);

            List<String> placesFromGoogle = getGoogleTouristPlaceNames(destination, days);
            if (!placesFromGoogle.isEmpty()) {
                placeNames.put(destKey, placesFromGoogle);
            } else if (!placeNames.containsKey(destKey)) {
                placeNames.put(destKey, generateFallbackNames(destination, "Place", days));
            }

            List<String> restaurantsFromGoogle = getGoogleRestaurantNames(destination, days);
            if (!restaurantsFromGoogle.isEmpty()) {
                restaurantNames.put(destKey, restaurantsFromGoogle);
            } else if (!restaurantNames.containsKey(destKey)) {
                restaurantNames.put(destKey, generateFallbackNames(destination, "Restaurant", days));
            }

            List<String> destPlaces = new ArrayList<>(placeNames.getOrDefault(destKey, Collections.emptyList()));
            List<String> googlePlaceNames = !placesFromGoogle.isEmpty() ? new ArrayList<>(placesFromGoogle) : Collections.emptyList();

            List<String> destRests = new ArrayList<>(restaurantNames.getOrDefault(destKey, Collections.emptyList()));
            List<String> googleRestNames = !restaurantsFromGoogle.isEmpty() ? new ArrayList<>(restaurantsFromGoogle) : Collections.emptyList();

            // Flight estimate (one-way + return) based on budget and distance (deterministic)
            int flightCost = calculateFlightCost(fromLocation, destination, budget, seed);
            String flightDetails = generateFlightDetails(fromLocation, destination, seed);

            for(int i=0;i<days;i++){

                Map<String,Object> day = new HashMap<>();

                // Make costs deterministic based on seed and day index
                int placeCost = placeBase + Math.abs(Objects.hash(seed, i, "place") % 150);
                int foodCost = foodBase + Math.abs(Objects.hash(seed, i, "food") % 150);
                int hotelCost = hotelBase + Math.abs(Objects.hash(seed, i, "hotel") % 2000);

                String placeName;
                if (i < googlePlaceNames.size()) {
                    placeName = googlePlaceNames.get(i);
                } else if (i < destPlaces.size()) {
                    placeName = destPlaces.get(i);
                } else {
                    // Provide a non-numeric, descriptive fallback place name
                    String type = fallbackPlaceTypes.get(i % fallbackPlaceTypes.size());
                    placeName = destinationLabel + " " + type + " " + (i + 1);
                }

                // Ensure place name reflects the destination label as well
                placeName = ensureDestinationInName(placeName, destinationLabel);

                String restaurantName;
                if (i < destRests.size()) {
                    restaurantName = destRests.get(i);
                } else {
                    String type = fallbackRestaurantTypes.get(i % fallbackRestaurantTypes.size());
                    restaurantName = destinationLabel + " " + type;
                }

                // Ensure restaurant name reflects the destination label as well
                restaurantName = ensureDestinationInName(restaurantName, destinationLabel);

                String hotelType = fallbackHotelTypes.get(i % fallbackHotelTypes.size());
                String hotelName = destinationLabel + " " + hotelType;

                // Ensure hotel name reflects the destination label as well
                hotelName = ensureDestinationInName(hotelName, destinationLabel);

                // Attempt to use Google Places API results for better names + images.
                Map<String, Object> selectedGooglePlace = null;
                String photoUrl = null;
                if (!googlePlaces.isEmpty()) {
                    if (i < googlePlaces.size()) {
                        selectedGooglePlace = googlePlaces.get(i);
                    } else {
                        selectedGooglePlace = googlePlaces.get(i % googlePlaces.size());
                    }
                    if (selectedGooglePlace != null) {
                        placeName = (String) selectedGooglePlace.getOrDefault("name", placeName);
                        photoUrl = extractPhotoUrl(selectedGooglePlace);
                    }
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

                // Try a new place-specific Google Places backup if no photo yet
                if (photoUrl == null) {
                    photoUrl = getGooglePhotoForPlace(placeName);
                }

                // Fetch a free image from Wikipedia API as a fallback
                if (photoUrl == null) {
                    photoUrl = getWikipediaPhotoForPlace(placeName);
                }

                // Final fallback to deterministic Picsum image per place/day
                if (photoUrl == null) {
                    String seedTerm = (placeName != null && !placeName.isBlank() ? placeName : destination) + " " + i;
                    String encodedSeed = java.net.URLEncoder.encode(seedTerm, java.nio.charset.StandardCharsets.UTF_8);
                    photoUrl = "https://picsum.photos/seed/" + encodedSeed + "/400/250";
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

    private List<String> getGoogleTouristPlaceNames(String destination, int maxResults) {
        try {
            List<Map<String, Object>> results = googlePlacesService.getTouristPlaces(destination);
            if (results == null || results.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<>();
            for (Map<String, Object> place : results) {
                if (place.get("name") instanceof String) {
                    names.add((String) place.get("name"));
                }
                if (names.size() >= maxResults) break;
            }
            return names;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private List<String> getGoogleRestaurantNames(String destination, int maxResults) {
        try {
            List<Map<String, Object>> results = googlePlacesService.getRestaurants(destination);
            if (results == null || results.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<>();
            for (Map<String, Object> place : results) {
                if (place.get("name") instanceof String) {
                    names.add((String) place.get("name"));
                }
                if (names.size() >= maxResults) break;
            }
            return names;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
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

    private String getGooglePhotoForPlace(String placeName) {
        if (placeName == null || placeName.isBlank()) {
            return null;
        }

        // 1) Try primary search by exact place name through Google Places API.
        try {
            List<Map<String, Object>> places = googlePlacesService.getTouristPlaces(placeName);
            if (places != null && !places.isEmpty()) {
                for (Map<String, Object> p : places) {
                    String url = extractPhotoUrl(p);
                    if (url != null && !url.isBlank()) {
                        return url;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 2) If no photo obtained, try to resolve actual coordinates and fetch nearby place photo.
        try {
            Map<String, Object> geo = googlePlacesService.getPlaceLocation(placeName);
            if (geo != null && geo.get("lat") instanceof Number && geo.get("lng") instanceof Number) {
                double lat = ((Number) geo.get("lat")).doubleValue();
                double lng = ((Number) geo.get("lng")).doubleValue();

                List<Map<String, Object>> nearbyPlaces = googlePlacesService.getTouristPlaces(placeName + " travel");
                if (nearbyPlaces != null && !nearbyPlaces.isEmpty()) {
                    for (Map<String, Object> p : nearbyPlaces) {
                        String url = extractPhotoUrl(p);
                        if (url != null && !url.isBlank()) {
                            return url;
                        }
                    }
                }

                List<Map<String, Object>> restaurants = googlePlacesService.getRestaurantsNearLocation(lat, lng, 1500, "Low Budget");
                if (restaurants != null && !restaurants.isEmpty()) {
                    for (Map<String, Object> r : restaurants) {
                        String url = extractPhotoUrl(r);
                        if (url != null && !url.isBlank()) {
                            return url;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String getWikipediaPhotoForPlace(String placeName) {
        if (placeName == null || placeName.isBlank()) {
            return null;
        }
        try {
            // Improve search matching by removing the destination name if it's prepended
            String searchName = placeName;
            if (searchName.toLowerCase().contains("hyderabad")) {
                searchName = searchName.replaceAll("(?i)hyderabad", "").trim();
            }
            if (searchName.isEmpty()) searchName = placeName;

            String encoded = java.net.URLEncoder.encode(searchName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
            String urlStr = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=" 
                         + encoded + "&gsrlimit=1&prop=pageimages&format=json&pithumbsize=400";
            java.net.URI uri = new java.net.URI(urlStr);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "SmartPlanApp/1.0");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("", headers); // Empty string instead of "parameters"
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<java.util.Map> response = 
                restTemplate.exchange(uri, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
            
            java.util.Map body = response.getBody();
            System.out.println("Wiki API response for " + searchName + " -> " + body);
            if (body != null && body.containsKey("query")) {
                java.util.Map query = (java.util.Map) body.get("query");
                if (query.containsKey("pages")) {
                    java.util.Map pages = (java.util.Map) query.get("pages");
                    if (!pages.isEmpty()) {
                        Object firstKey = pages.keySet().iterator().next();
                        java.util.Map page = (java.util.Map) pages.get(firstKey);
                        if (page.containsKey("thumbnail")) {
                            java.util.Map thumbnail = (java.util.Map) page.get("thumbnail");
                            System.out.println("Found wiki photo: " + thumbnail.get("source"));
                            return (String) thumbnail.get("source");
                        }
                    }
                }
            }
            System.out.println("Wiki API did not contain thumbnail for " + placeName);
        } catch (Exception ignored) {
            System.err.println("Wikipedia Image API Error for " + placeName + ": " + ignored.getMessage());
        }
        return null;
    }
}
