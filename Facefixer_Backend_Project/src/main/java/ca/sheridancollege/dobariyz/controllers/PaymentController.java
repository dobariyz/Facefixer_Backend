package ca.sheridancollege.dobariyz.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import ca.sheridancollege.dobariyz.beans.Subscription;
import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.SubscriptionRepository;
import ca.sheridancollege.dobariyz.repositories.UserRepository;
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Value("${stripe.price.premium}")
    private String premiumPriceId;

    @Value("${stripe.price.pro}")
    private String proPriceId;

    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeApiKey;
        System.out.println("✅ Stripe initialized");
    }

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    /**
     * ✅ POST /api/payment/create-checkout
     * Creates a Stripe Checkout session for subscription upgrade
     */
    @PostMapping("/create-checkout")
    public ResponseEntity<?> createCheckoutSession(
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
            Subscription subscription = subOpt.get();

            // 5. Get requested tier (default to 'premium')
            String requestedTier = request.getOrDefault("tier", "premium").toLowerCase();

            // Validate tier
            if (!requestedTier.equals("premium") && !requestedTier.equals("pro")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid tier. Must be 'premium' or 'pro'"));
            }

            // Check if already on this tier
            if (subscription.getTier().equals(requestedTier)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Already subscribed to " + requestedTier));
            }

            // 6. Select correct Stripe Price ID
            String priceId = requestedTier.equals("premium") ? premiumPriceId : proPriceId;

            System.out.println("🛒 Creating checkout session for: " + email);
            System.out.println("📦 Tier: " + requestedTier + " | Price ID: " + priceId);

            // 7. Create Stripe Checkout Session
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomerEmail(email)
                    .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/payment/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    // Add metadata to track the subscription
                    .putMetadata("userId", user.getId().toString())
                    .putMetadata("userEmail", email)
                    .putMetadata("tier", requestedTier)
                    .putMetadata("subscriptionId", subscription.getId().toString())
                    .build();

            Session session = Session.create(params);

            System.out.println("✅ Stripe checkout session created: " + session.getId());

            // 8. Return checkout URL to frontend
            Map<String, String> response = new HashMap<>();
            response.put("checkoutUrl", session.getUrl());
            response.put("sessionId", session.getId());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            System.err.println("❌ Stripe error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create checkout session: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error creating checkout session: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating checkout session"));
        }
    }

    /**
     * ✅ GET /api/payment/verify-session
     * Verify payment session after redirect from Stripe
     */
    @GetMapping("/verify-session")
    public ResponseEntity<?> verifySession(
            @RequestParam("session_id") String sessionId,
            Authentication authentication) {
        try {
            // Retrieve session from Stripe
            Session session = Session.retrieve(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", session.getStatus());
            response.put("paymentStatus", session.getPaymentStatus());
            response.put("customerEmail", session.getCustomerEmail());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            System.err.println("❌ Error verifying session: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to verify session"));
        }
    }

    /**
     * ✅ POST /api/payment/complete-upgrade
     * Manually complete upgrade after successful payment (for testing/fallback)
     * This is useful when webhook doesn't fire or for frontend-initiated completion
     */
//    @PostMapping("/complete-upgrade")
//    public ResponseEntity<?> completeUpgrade(
//            @RequestParam("session_id") String sessionId,
//            Authentication authentication) {
//        try {
//            // 1. Check authentication
//            if (authentication == null || !authentication.isAuthenticated()) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(Map.of("error", "Unauthorized"));
//            }
//
//            // 2. Extract email
//            String email = extractEmailFromAuthentication(authentication);
//            if (email == null) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(Map.of("error", "Email not found"));
//            }
//
//            // 3. Find user
//            Optional<User> userOpt = userRepository.findFirstByEmail(email);
//            if (userOpt.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body(Map.of("error", "User not found"));
//            }
//            User user = userOpt.get();
//
//            // 4. Retrieve Stripe session
//            Session session = Session.retrieve(sessionId);
//
//            // 5. Verify payment was successful
//            if (!"complete".equals(session.getStatus()) || 
//                !"paid".equals(session.getPaymentStatus())) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(Map.of("error", "Payment not completed"));
//            }
//
//            // 6. Verify this is the right user
//            if (!email.equals(session.getCustomerEmail())) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(Map.of("error", "Session does not belong to this user"));
//            }
//
//            // 7. Get subscription info from metadata
//            String tier = session.getMetadata().get("tier");
//            String subscriptionIdStr = session.getMetadata().get("subscriptionId");
//            
//            if (tier == null || subscriptionIdStr == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(Map.of("error", "Invalid session metadata"));
//            }
//
//            Long subscriptionId = Long.parseLong(subscriptionIdStr);
//
//            // 8. Find and update subscription
//            Optional<Subscription> subOpt = subscriptionRepository.findById(subscriptionId);
//            if (subOpt.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body(Map.of("error", "Subscription not found"));
//            }
//
//            Subscription subscription = subOpt.get();
//
//            // 9. Check if already upgraded
//            if (subscription.getTier().equals(tier)) {
//                return ResponseEntity.ok(Map.of(
//                    "message", "Already upgraded",
//                    "tier", tier,
//                    "uploadsLimit", subscription.getUploadsLimit()
//                ));
//            }
//
//            // 10. Upgrade the subscription
//            subscription.upgradeTier(tier);
//            subscription.setStripeCustomerId(session.getCustomer());
//            subscription.setStripeSubscriptionId(session.getSubscription());
//            subscription.setIsActive(true);
//            subscription.setUpdatedAt(java.time.LocalDateTime.now());
//
//            subscriptionRepository.save(subscription);
//
//            System.out.println("✅ Subscription manually upgraded:");
//            System.out.println("   User: " + email);
//            System.out.println("   Tier: " + tier);
//            System.out.println("   Uploads: " + subscription.getUploadsLimit());
//
//            // 11. Return success response
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("message", "Subscription upgraded successfully");
//            response.put("tier", subscription.getTier());
//            response.put("uploadsLimit", subscription.getUploadsLimit());
//            response.put("uploadsUsed", subscription.getUploadsUsed());
//            response.put("uploadsRemaining", subscription.getRemainingUploads());
//
//            return ResponseEntity.ok(response);
//
//        } catch (StripeException e) {
//            System.err.println("❌ Stripe error: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Failed to verify payment: " + e.getMessage()));
//        } catch (Exception e) {
//            System.err.println("❌ Error completing upgrade: " + e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Error completing upgrade"));
//        }
//    }

    /**
     * ✅ POST /api/payment/complete-upgrade
     * Manually complete upgrade after successful payment (for testing/fallback)
     */
    @PostMapping("/complete-upgrade")
    public ResponseEntity<?> completeUpgrade(
            @RequestParam("session_id") String sessionId,
            Authentication authentication) {
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

            // 4. Retrieve Stripe session
            Session session = Session.retrieve(sessionId);

            // 5. Verify payment was successful
            if (!"complete".equals(session.getStatus()) || 
                !"paid".equals(session.getPaymentStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Payment not completed"));
            }

            // 6. Verify this is the right user
            if (!email.equals(session.getCustomerEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Session does not belong to this user"));
            }

            // 7. Get subscription info from metadata
            String tier = session.getMetadata().get("tier");
            String subscriptionIdStr = session.getMetadata().get("subscriptionId");
            
            if (tier == null || subscriptionIdStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid session metadata"));
            }

            Long subscriptionId = Long.parseLong(subscriptionIdStr);

            // 8. Find and update subscription
            Optional<Subscription> subOpt = subscriptionRepository.findById(subscriptionId);
            if (subOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Subscription not found"));
            }

            Subscription subscription = subOpt.get();

            // 9. Check if already upgraded
            if (subscription.getTier().equals(tier)) {
                return ResponseEntity.ok(Map.of(
                    "message", "Already upgraded",
                    "tier", tier,
                    "uploadsLimit", subscription.getUploadsLimit()
                ));
            }

            // 10. Upgrade the subscription
            subscription.upgradeTier(tier);
            subscription.setStripeCustomerId(session.getCustomer());
            subscription.setStripeSubscriptionId(session.getSubscription());
            subscription.setIsActive(true);
            subscription.setUpdatedAt(java.time.LocalDateTime.now());

            subscriptionRepository.save(subscription);

            System.out.println("✅ Subscription manually upgraded:");
            System.out.println("   User: " + email);
            System.out.println("   Tier: " + tier);
            System.out.println("   Uploads: " + subscription.getUploadsLimit());

            // 11. Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Subscription upgraded successfully");
            response.put("tier", subscription.getTier());
            response.put("uploadsLimit", subscription.getUploadsLimit());
            response.put("uploadsUsed", subscription.getUploadsUsed());
            response.put("uploadsRemaining", subscription.getRemainingUploads());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            System.err.println("❌ Stripe error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to verify payment: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error completing upgrade: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error completing upgrade"));
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