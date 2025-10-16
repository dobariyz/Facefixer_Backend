package ca.sheridancollege.dobariyz.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    
    // Save new detection result
    @PostMapping
    public DetectionResult saveResult(@RequestBody DetectionResult result) {
        return service.saveResult(result);
    }

    // Get all results for a specific user
    @GetMapping("/{userId}")

public ResponseEntity<List<DetectionResult>> getUserResults(@PathVariable Long userId) {
    return userRepository.findById(userId)
        .map(user -> ResponseEntity.ok(service.getResultsByUser(user)))
        .orElse(ResponseEntity.status(404).build()); // User not found
}

    
    @GetMapping("/history")
    public ResponseEntity<List<DetectionResult>> getHistory(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        String email = null;

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        }

        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<User> userOpt = userRepository.findFirstByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).build(); // User not found
        }

        User user = userOpt.get();
        List<DetectionResult> results = detectionResultRepository.findByUserId(user.getId());
        return ResponseEntity.ok(results);
    }
    
 // Delete a specific detection history by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDetection(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = null;
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        }

        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid user session"));
        }

        boolean deleted = service.deleteDetectionByIdAndEmail(id, email);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Detection history deleted successfully"));
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "Detection not found or unauthorized"));
        }
    }

}

//Add below line of code if we want to delete the path of the image from S3 also. (This is specifically for history image deletion)
//s3Service.deleteFile(detectionOpt.get().getImagePath());
//s3Service.deleteFile(detectionOpt.get().getResultPath());

// Only uncomment this if you ready to use AWS, otherwise don't change anything out of it!

//@RestController
//@RequestMapping("/api/detections")
//@RequiredArgsConstructor
//public class DetectionResultController {
//
//    private final DetectionResultService service;
//    private final UserRepository userRepository;
//    private final DetectionResultRepository detectionResultRepository;
//    private final S3Service s3Service; // ✅ Add S3 service
//
//    // Save new detection result
//    @PostMapping
//    public DetectionResult saveResult(@RequestBody DetectionResult result) {
//        return service.saveResult(result);
//    }
//
//    // Get all results for a specific user (by userId)
//    @GetMapping("/{userId}")
//    public ResponseEntity<List<DetectionResult>> getUserResults(@PathVariable Long userId) {
//        return userRepository.findById(userId)
//            .map(user -> {
//                List<DetectionResult> results = service.getResultsByUser(user)
//                        .stream()
//                        .map(this::mapS3KeysToUrls)
//                        .collect(Collectors.toList());
//                return ResponseEntity.ok(results);
//            })
//            .orElse(ResponseEntity.status(404).build());
//    }
//
//    // Get history for currently authenticated user
//    @GetMapping("/history")
//    public ResponseEntity<List<DetectionResult>> getHistory(Authentication authentication) {
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return ResponseEntity.status(401).build();
//        }
//
//        String email = null;
//        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
//            email = oauth2User.getAttribute("email");
//        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
//            email = userDetails.getUsername();
//        }
//
//        if (email == null) {
//            return ResponseEntity.status(401).build();
//        }
//
//        Optional<User> userOpt = userRepository.findFirstByEmail(email);
//        if (userOpt.isEmpty()) {
//            return ResponseEntity.status(404).build();
//        }
//
//        User user = userOpt.get();
//        List<DetectionResult> results = detectionResultRepository.findByUserId(user.getId())
//                .stream()
//                .map(this::mapS3KeysToUrls)
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(results);
//    }
//
//    // ✅ Helper method: convert S3 keys to backend-accessible URLs
//    private DetectionResult mapS3KeysToUrls(DetectionResult result) {
//        DetectionResult copy = new DetectionResult();
//        copy.setId(result.getId());
//        copy.setUser(result.getUser());
//        copy.setCreatedAt(result.getCreatedAt());
//        copy.setDetections(result.getDetections());
//
//        // Convert S3 keys to URLs through your backend endpoint
//        copy.setImagePath(result.getImagePath().replace("uploads/", ""));
//        copy.setResultPath(result.getResultPath().replace("outputs/", ""));
//
//        return copy;
//    }
//}