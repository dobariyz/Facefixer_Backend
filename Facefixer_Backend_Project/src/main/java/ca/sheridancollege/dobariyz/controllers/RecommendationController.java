package ca.sheridancollege.dobariyz.controllers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.sheridancollege.dobariyz.beans.DetectionResult;
import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.UserRepository;
import ca.sheridancollege.dobariyz.services.DetectionResultService;
import ca.sheridancollege.dobariyz.services.SephoraService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class RecommendationController {

    private final DetectionResultService detectionResultService;
    private final SephoraService sephoraService;
    private final UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public ResponseEntity<?> getRecommendations(
            @RequestParam(required = false) Long imageId,
            Authentication authentication) {

        try {
            // 1️⃣ Get logged-in user
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

           String email = null;
            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                email = oauth2User.getAttribute("email");
            } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
                email = userDetails.getUsername();
            }

            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Email not found in authentication."));
            }

            User user = userRepository.findFirstByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found "));

         // Fetch the DetectionResult for this user
            DetectionResult detectionResult;
            if (imageId != null) {
                Optional<DetectionResult> optResult = detectionResultService.findById(imageId);
                if (optResult.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Image not found for id: " + imageId));
                }
                detectionResult = optResult.get();
            } else {
                // If no imageId passed, get the latest image uploaded by user
                Optional<DetectionResult> latest = detectionResultService.findLatestByUserEmail(email);
                if (latest.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "No images found for user: " + email));
                }
                detectionResult = latest.get();
            }

            // 3️ Parse detections JSON
            Map<String, Object> detectionsJson = objectMapper.readValue(
                    detectionResult.getDetections(), Map.class);

            Map<String, Map<String, Object>> summary = (Map<String, Map<String, Object>>) detectionsJson.get("summary");
            if (summary == null || summary.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "No keywords detected for this image"));
            }

            // 4️ Fetch top 5 products for each keyword
            Map<String, List<Map<String, Object>>> recommendations = new HashMap<>();
            for (String keyword : summary.keySet()) {
                List<Map<String, Object>> products = sephoraService.getProducts(keyword);
                recommendations.put(keyword, products);
            }

            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
