package ca.sheridancollege.dobariyz.controllers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.UserRepository;
import ca.sheridancollege.dobariyz.services.AuthenticationService;
import ca.sheridancollege.dobariyz.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository; //

    @Autowired
    public AuthenticationController(AuthenticationService authenticationService, JwtUtil jwtUtil, UserRepository userRepository ) {
        this.authenticationService = authenticationService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/signupUser")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody User user) {
    	System.out.println("Received User Data: " + user);    	
    	
    	try {
            // Call the authentication service to register the user and generate the token
            String token = authenticationService.registerUser(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPassword()
            );

            // Return the generated token in the response
            return ResponseEntity.ok(Map.of("token", token));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage())); // Handle duplicate email properly
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Something went wrong!"));
        }
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/loginUser")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody Map<String, String> request) {
    	 try {
    	        String token = authenticationService.loginUser(
    	                request.get("email"),
    	                request.get("password")
    	        );
    	        return ResponseEntity.ok(Map.of("token", token));
    	    } catch (RuntimeException e) {
    	        return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));  // Return 401 Unauthorized
    	    }}

    @GetMapping("/logout")
    public ResponseEntity<Map<String, String>> logoutUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
         request.logout();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    @GetMapping("/generate-token")
    public String generateToken(@RequestParam String email) {
        return jwtUtil.generateToken(email);
    }
    
    @CrossOrigin(origins = "*")
	 @GetMapping("/validate-token")
	 @ResponseBody
	 public String validateToken(@RequestHeader("Authorization") String token) {
	     if (token.startsWith("Bearer ")) {
	         token = token.substring(7);  // Remove 'Bearer ' prefix
	     }
     boolean isValid = jwtUtil.validateToken(token, jwtUtil.extractEmail(token));
     return isValid ? "Valid Token" : "Invalid Token";
	 }
    
 // Add this new endpoint
    @CrossOrigin(origins = "*")
    @GetMapping("/user-profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            String email = jwtUtil.extractEmail(token);
            
            if (!jwtUtil.validateToken(token, email)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }
            
            Optional<User> userOpt = userRepository.findFirstByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            Map<String, Object> response = Map.of(
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "email", user.getEmail()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching user profile"));
        }
    }
 // NEW ENDPOINT: Accept Terms & Conditions
    @CrossOrigin(origins = "*")
    @PostMapping("/accept-terms")
    public ResponseEntity<?> acceptTerms(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String email = jwtUtil.extractEmail(token);

            if (!jwtUtil.validateToken(token, email)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }

            Optional<User> userOpt = userRepository.findFirstByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            user.setTermsAcceptedAt(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Terms accepted successfully",
                    "termsAcceptedAt", user.getTermsAcceptedAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error accepting terms"));
        }
    }

    // NEW ENDPOINT: Check if user has accepted terms
    @CrossOrigin(origins = "*")
    @GetMapping("/check-terms")
    public ResponseEntity<?> checkTerms(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String email = jwtUtil.extractEmail(token);

            if (!jwtUtil.validateToken(token, email)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }

            Optional<User> userOpt = userRepository.findFirstByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            return ResponseEntity.ok(Map.of("termsAccepted", user.getTermsAcceptedAt() != null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error checking terms status"));
        }
    }
}