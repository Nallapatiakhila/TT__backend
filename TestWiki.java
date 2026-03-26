import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

public class TestWiki {
    public static void main(String[] args) {
        try {
            String placeName = "hyderabad Charminar";
            String encoded = java.net.URLEncoder.encode(placeName, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Encoded: " + encoded);
            String url = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=" 
                         + encoded + "&gsrlimit=1&prop=pageimages&format=json&pithumbsize=400";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SmartPlanApp/1.0");
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            System.out.println("Response: " + response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
