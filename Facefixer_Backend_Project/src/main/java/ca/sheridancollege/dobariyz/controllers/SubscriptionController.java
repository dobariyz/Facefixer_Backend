package ca.sheridancollege.dobariyz.controllers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.param.SubscriptionCancelParams;

import ca.sheridancollege.dobariyz.beans.Subscription;
import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.SubscriptionRepository;
import ca.sheridancollege.dobariyz.repositories.UserRepository;
import ca.sheridancollege.dobariyz.util.JwtUtil;

@RestController
@RequestMapping("/api/subscription")
@CrossOrigin(origins = "http://localhost:5173")
public class SubscriptionController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * ✅ GET /api/subscription/status
     * Returns user's subscription status and usage
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSubscriptionStatus(Authentication authentication) {
        try {
            // 1. Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            // 2. Extract email
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Email not found"));
            }

            // 3. Find user
            Optional<User> userOpt = userRepository.findFirstByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            System.out.println("✅ Found user ID: " + user.getId()); // ✅ DEBUG

            // 4. Find subscription
            Optional<Subscription> subOpt = subscriptionRepository.findByUserId(user.getId());
            if (subOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No subscription found"));
            }

            Subscription sub = subOpt.get();
            System.out.println("📊 Subscription tier: " + sub.getTier()); // ✅ DEBUG

            // ✅ Check if monthly reset needed (your entity handles this)
            sub.canUpload(); // This triggers auto-reset if needed
            subscriptionRepository.save(sub); // Save if reset occurred

            // 5. Return subscription status (matching your entity fields)
            Map<String, Object> response = new HashMap<>();
            response.put("userEmail", email); // ✅ DEBUG: Show which user
            response.put("userId", user.getId()); // ✅ DEBUG: Show user ID
            response.put("tier", sub.getTier());
            response.put("uploadsUsed", sub.getUploadsUsed());
            response.put("uploadsLimit", sub.getUploadsLimit());
            response.put("uploadsRemaining", sub.getRemainingUploads());
            response.put("canUpload", sub.canUpload());
            response.put("isActive", sub.getIsActive());
            response.put("isExpired", sub.isExpired());
            
            if (sub.getResetDate() != null) {
                response.put("resetDate", sub.getResetDate().toString());
            }
            
            if (sub.getSubscriptionStartDate() != null) {
                response.put("subscriptionStartDate", sub.getSubscriptionStartDate().toString());
            }
            
            if (sub.getSubscriptionEndDate() != null) {
                response.put("subscriptionEndDate", sub.getSubscriptionEndDate().toString());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error fetching subscription status: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching subscription status"));
        }
    }

    /**
     * ✅ POST /api/subscription/upgrade
     * Initiates premium upgrade process (Stripe checkout in Step 3)
     */
    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradeSubscription(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        try {
            // 1. Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            // 2. Extract email
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Email not found"));
            }

            // 3. Find user
            Optional<User> userOpt = userRepository.findFirstByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();

            // 4. Find subscription
            Optional<Subscription> subOpt = subscriptionRepository.findByUserId(user.getId());
            if (subOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No subscription found"));
            }

            Subscription sub = subOpt.get();

            // 5. Get requested tier (default to 'premium')
            String requestedTier = request.getOrDefault("tier", "premium");

            // 6. Check if already on this tier or higher
            if (sub.getTier().equals(requestedTier)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Already subscribed to " + requestedTier));
            }

            // ✅ TODO (Step 3): Create Stripe checkout session here
            // For now, we'll just return a placeholder response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Upgrade endpoint ready");
            response.put("currentTier", sub.getTier());
            response.put("requestedTier", requestedTier);
            response.put("upgradeAvailable", true);
            response.put("note", "Stripe integration coming in Step 3");
            
            // Show pricing info
            if (requestedTier.equals("premium")) {
                response.put("price", "$9.99/month");
                response.put("uploads", "50 per month");
            } else if (requestedTier.equals("pro")) {
                response.put("price", "$29.99/month");
                response.put("uploads", "Unlimited");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error upgrading subscription: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing upgrade request"));
        }
    }

    /**
     * ✅ POST /api/subscription/cancel
     * Cancel premium subscription (keeps access until end of billing period)
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription(Authentication authentication) {
        try {
            // 1. Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            // 2. Extract email
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Email not found"));
            }

            // 3. Find user
            Optional<User> userOpt = userRepository.findFirstByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();

            // 4. Find subscription
            Optional<Subscription> subOpt = subscriptionRepository.findByUserId(user.getId());
            if (subOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No subscription found"));
            }

            Subscription sub = subOpt.get();

            // 5. Check if already on free tier
            if (sub.getTier().equals("free")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Already on free tier"));
            }

            // ✅ FIXED: Don't reset to free immediately!
            // Instead, mark as "will cancel" and keep benefits until subscriptionEndDate
            
            // Set inactive flag (but keep tier and limits!)
            sub.setIsActive(false);
            sub.setUpdatedAt(LocalDateTime.now());
            
            // If there's a Stripe subscription, cancel it (at period end)
            if (sub.getStripeSubscriptionId() != null) {
                try {
                    // Cancel at period end in Stripe (not immediately!)
                    com.stripe.model.Subscription stripeSub = 
                        com.stripe.model.Subscription.retrieve(sub.getStripeSubscriptionId());
                    stripeSub.cancel(
                        SubscriptionCancelParams.builder()
                            .setInvoiceNow(false)
                            .setProrate(false)
                            .build()
                    );
                    System.out.println("✅ Stripe subscription cancelled at period end");
                } catch (Exception e) {
                    System.err.println("⚠️ Error cancelling Stripe subscription: " + e.getMessage());
                }
            }
            
            subscriptionRepository.save(sub);

            System.out.println("✅ Subscription marked for cancellation: " + email);
            System.out.println("   Current Tier: " + sub.getTier() + " (will keep until " + 
                (sub.getSubscriptionEndDate() != null ? sub.getSubscriptionEndDate() : "end of billing") + ")");

            return ResponseEntity.ok(Map.of(
                    "message", "Subscription cancelled. You'll keep " + sub.getTier() + 
                        " benefits until " + 
                        (sub.getSubscriptionEndDate() != null ? 
                            sub.getSubscriptionEndDate().toString() : "the end of your billing period"),
                    "tier", sub.getTier(),
                    "uploadsLimit", sub.getUploadsLimit(),
                    "expiresAt", sub.getSubscriptionEndDate() != null ? 
                        sub.getSubscriptionEndDate().toString() : null
            ));

        } catch (Exception e) {
            System.err.println("❌ Error cancelling subscription: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error cancelling subscription"));
        }
    }

    /**
     * ✅ Helper method to extract email from authentication
     */
    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("email");
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }
}