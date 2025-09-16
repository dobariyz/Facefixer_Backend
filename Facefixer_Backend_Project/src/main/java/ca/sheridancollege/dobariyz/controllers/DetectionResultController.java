package ca.sheridancollege.dobariyz.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
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


}
