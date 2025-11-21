package ca.sheridancollege.dobariyz.controllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.sheridancollege.dobariyz.beans.DetectionResult;
import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.DetectionResultRepository;
import ca.sheridancollege.dobariyz.repositories.UserRepository;
import ca.sheridancollege.dobariyz.services.DetectionResultService;
import ca.sheridancollege.dobariyz.services.S3Service;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/detections")
@RequiredArgsConstructor
public class DetectionResultController {

    private final DetectionResultService service;
    private final UserRepository userRepository;
    private final DetectionResultRepository detectionResultRepository;
    private final S3Service s3Service; // ✅ AWS S3 Service

    // ✅ Save new detection result
    @PostMapping
    public DetectionResult saveResult(@RequestBody DetectionResult result) {
        return service.saveResult(result);
    }

    // ✅ Get all results for a specific user (by userId)
    @GetMapping("/{userId}")
    public ResponseEntity<List<DetectionResult>> getUserResults(@PathVariable Long userId) {
        return userRepository.findById(userId)
            .map(user -> {
                List<DetectionResult> results = service.getResultsByUser(user)
                        .stream()
                        .map(this::convertS3KeysToBackendUrls)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(results);
            })
            .orElse(ResponseEntity.status(404).build()); // User not found
    }

    // ✅ Get detection history for currently authenticated user
    @GetMapping("/history")
    public ResponseEntity<List<DetectionResult>> getHistory(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = extractEmailFromAuthentication(authentication);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<User> userOpt = userRepository.findFirstByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).build(); // User not found
        }

        User user = userOpt.get();
        List<DetectionResult> results = detectionResultRepository.findByUserId(user.getId())
                .stream()
                .map(this::convertS3KeysToBackendUrls)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    // ✅ Delete a specific detection history by ID (with S3 cleanup)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDetection(@PathVariable Long id) {
        try {
            DetectionResult detection = detectionResultRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Detection not found"));

            String imageKey = detection.getImagePath();
            String resultKey = detection.getResultPath();

            // ✅ Only delete if path exists
            if (imageKey != null && !imageKey.isBlank()) {
                if (!imageKey.startsWith("uploads/") && !imageKey.startsWith("outputs/")) {
                    imageKey = "uploads/" + imageKey;
                }

                try {
                    s3Service.deleteFile(imageKey);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete image file: " + imageKey + " → " + ex.getMessage());
                }
            }

            if (resultKey != null && !resultKey.isBlank()) {
                if (!resultKey.startsWith("outputs/") && !resultKey.startsWith("uploads/")) {
                    resultKey = "outputs/" + resultKey;
                }

                try {
                    s3Service.deleteFile(resultKey);
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete result file: " + resultKey + " → " + ex.getMessage());
                }
            }

            detectionResultRepository.delete(detection);
            return ResponseEntity.ok("Deleted successfully");

        } catch (Exception e) {
            System.err.println("❌ Error deleting detection: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting detection: " + e.getMessage());
        }
    }


    // ✅ Helper: Extract email from authentication object
    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("email");
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }

    // ✅ Helper: Convert S3 keys to backend-accessible URLs
    private DetectionResult convertS3KeysToBackendUrls(DetectionResult result) {
        DetectionResult copy = new DetectionResult();
        copy.setId(result.getId());
        copy.setUser(result.getUser());
        copy.setCreatedAt(result.getCreatedAt());
        copy.setDetections(result.getDetections());
        copy.setImagePath(result.getImagePath());   // ✅ Keep full key
        copy.setResultPath(result.getResultPath()); // ✅ Keep full key

        // Convert S3 keys to backend URLs
        // Example: "uploads/image.jpg" → "image.jpg" (for /api/image?file=image.jpg)
        // Example: "outputs/detected_image.jpg" → "detected_image.jpg"
        
        String imagePath = result.getImagePath();
        String resultPath = result.getResultPath();

        if (imagePath != null && imagePath.startsWith("uploads/")) {
            copy.setImagePath(imagePath.replace("uploads/", ""));
        } else {
            copy.setImagePath(imagePath);
        }

        if (resultPath != null && resultPath.startsWith("outputs/")) {
            copy.setResultPath(resultPath.replace("outputs/", ""));
        } else {
            copy.setResultPath(resultPath);
        }

        return copy;
    }
}
