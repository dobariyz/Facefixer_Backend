package ca.sheridancollege.dobariyz.services;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SephoraService {
    @Value("${rapidapi.key}")
    private String apiKey;

    @Value("${rapidapi.host}")
    private String apiHost;

    private static final String API_URL = "https://sephora.p.rapidapi.com/us/products/v2/search?q=";

    public List<Map<String, Object>> getProducts(String keyword, int limit) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", apiHost);
        headers.set("x-rapidapi-key", apiKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                API_URL + keyword,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("products")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> products = (List<Map<String, Object>>) body.get("products");
        List<Map<String, Object>> cleaned = new ArrayList<>();

        for (Map<String, Object> product : products) {
            if (cleaned.size() >= limit) break;

            Map<String, Object> currentSku = (Map<String, Object>) product.get("currentSku");
            if (currentSku == null) continue;

            double rating = 0.0;
            if (product.get("rating") != null) {
                try {
                    rating = Double.parseDouble(product.get("rating").toString());
                } catch (NumberFormatException ignored) {}
            }

            if (rating < 4.0) continue;

            String price = (currentSku.get("listPrice") != null)
                    ? currentSku.get("listPrice").toString()
                    : "N/A";

            // ✅ Build proper Sephora URL
            String targetUrl = product.get("targetUrl") != null 
                    ? product.get("targetUrl").toString() 
                    : "";
            
            String productUrl = "";
            if (!targetUrl.isEmpty()) {
                // If targetUrl already starts with http, use it as-is
                if (targetUrl.startsWith("http")) {
                    productUrl = targetUrl;
                } else {
                    // Otherwise prepend Sephora domain
                    productUrl = "https://www.sephora.com" + 
                            (targetUrl.startsWith("/") ? targetUrl : "/" + targetUrl);
                }
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", product.get("productId"));
            item.put("name", product.get("productName"));
            item.put("brand", product.get("brandName"));
            item.put("price", price);
            item.put("rating", rating);
            item.put("reviews", product.get("reviews"));
            item.put("image", product.get("heroImage"));
            item.put("url", productUrl);
            
            // ✅ Add SKU ID for better tracking
            item.put("skuId", currentSku.get("skuId"));

            cleaned.add(item);
        }

        return cleaned;
    }
    
    public List<Map<String, Object>> getSimplifiedProducts(String keyword) {
        List<Map<String, Object>> fullProducts = getProducts(keyword, 10);
        List<Map<String, Object>> simplifiedList = new ArrayList<>();

        for (Map<String, Object> product : fullProducts) {
            Map<String, Object> simplified = new HashMap<>();
            simplified.put("name", product.get("name"));
            simplified.put("brand", product.get("brand"));
            simplified.put("price", product.get("price"));
            simplified.put("image", product.get("image"));
            simplified.put("url", product.get("url"));
            simplified.put("rating", product.get("rating"));
            simplified.put("productId", product.get("id")); // ✅ Include product ID
            simplified.put("skuId", product.get("skuId")); // ✅ Include SKU ID
            simplifiedList.add(simplified);
        }

        return simplifiedList;
    }
}