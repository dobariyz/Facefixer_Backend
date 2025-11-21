package ca.sheridancollege.dobariyz.controllers;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;

import ca.sheridancollege.dobariyz.beans.Subscription;
import ca.sheridancollege.dobariyz.repositories.SubscriptionRepository;
import jakarta.persistence.Column;

@RestController
@RequestMapping("/api/webhook")
public class StripeWebhookController {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    /**
     * ✅ Handle Stripe webhook events
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        Event event;

        try {
            // ✅ Use signature verification if available
        	if (webhookSecret == null || webhookSecret.isEmpty()) {
        	    System.err.println("❌ WEBHOOK SECRET NOT CONFIGURED!");
        	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        	            .body("Webhook secret not configured");
        	}

        	if (sigHeader == null || sigHeader.isEmpty()) {
        	    System.err.println("⚠️ Missing Stripe-Signature header");
        	    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        	            .body("Missing signature");
        	}

        	try {
        	    event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        	} catch (SignatureVerificationException e) {
        	    System.err.println("❌ Invalid signature!");
        	    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        	            .body("Invalid signature");
        	}

        } catch (Exception e) {
            System.err.println("❌ Error parsing webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook parse error");
        }

        System.out.println("\n📥 Received Stripe webhook: " + event.getType());

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event, payload);
                break;

            case "customer.subscription.created":
            case "customer.subscription.updated":
                handleSubscriptionUpdated(event);
                break;

            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;

            case "invoice.payment_succeeded":
            case "invoice.paid":
                handlePaymentSucceeded(event);
                break;

            case "invoice.payment_failed":
                handlePaymentFailed(event);
                break;

            default:
                System.out.println("ℹ️ Unhandled event type: " + event.getType());
        }

        return ResponseEntity.ok("✅ Webhook received");
    }

    // ------------------------------------------------------
    // ✅ HANDLE CHECKOUT SESSION COMPLETED
    // ------------------------------------------------------
    private void handleCheckoutSessionCompleted(Event event, String payload) {
        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Object stripeObject = deserializer.getObject().orElse(null);

            Session session;
            if (stripeObject instanceof Session) {
                session = (Session) stripeObject;
            } else {
                System.err.println("⚠️ Deserializer failed, manually parsing session...");
                JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                JsonObject dataObject = json.getAsJsonObject("data").getAsJsonObject("object");
                if (json == null || !json.has("data")) {
                    System.err.println("❌ Invalid webhook payload structure");
                    return;
                }
                session = ApiResource.GSON.fromJson(dataObject, Session.class);
            }

            if (session == null) {
                System.err.println("❌ Could not parse checkout.session.completed event");
                return;
            }

            String subscriptionIdStr = session.getMetadata().get("subscriptionId");
            String tier = session.getMetadata().get("tier");
            String stripeSubscriptionId = session.getSubscription();
            String stripeCustomerId = session.getCustomer();

            if (subscriptionIdStr == null || tier == null) {
                System.err.println("⚠️ Missing metadata (likely a test webhook)");
                return;
            }

            Long subscriptionId = Long.parseLong(subscriptionIdStr);

            Optional<Subscription> subOpt = subscriptionRepository.findById(subscriptionId);
            if (subOpt.isEmpty()) {
                System.err.println("❌ Subscription not found: " + subscriptionId);
                return;
            }

            Subscription subscription = subOpt.get();
            subscription.upgradeTier(tier);
            subscription.setStripeCustomerId(stripeCustomerId);
            subscription.setStripeSubscriptionId(stripeSubscriptionId);
            subscription.setIsActive(true);
            subscription.setUpdatedAt(LocalDateTime.now());

            subscriptionRepository.save(subscription);

            System.out.println("✅ Subscription upgraded successfully!");
            System.out.println("   User: " + subscription.getUser().getEmail());
            System.out.println("   Tier: " + tier);

        } catch (Exception e) {
            System.err.println("❌ Error handling checkout.session.completed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------
    // ✅ HANDLE SUBSCRIPTION UPDATE
    // ------------------------------------------------------
    private void handleSubscriptionUpdated(Event event) {
        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Object obj = deserializer.getObject().orElse(null);
            if (!(obj instanceof com.stripe.model.Subscription)) return;

            com.stripe.model.Subscription stripeSub = (com.stripe.model.Subscription) obj;
            String stripeSubscriptionId = stripeSub.getId();
            String status = stripeSub.getStatus();

            Optional<Subscription> subOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
            if (subOpt.isEmpty()) {
                System.err.println("⚠️ No subscription found for Stripe ID: " + stripeSubscriptionId);
                return;
            }

            Subscription sub = subOpt.get();
            sub.setIsActive("active".equals(status));
            sub.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);

            System.out.println("✅ Subscription updated: " + sub.getUser().getEmail() + " | Status: " + status);

        } catch (Exception e) {
            System.err.println("❌ Error handling subscription update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------
    // ✅ HANDLE SUBSCRIPTION DELETED
    // ------------------------------------------------------
    private void handleSubscriptionDeleted(Event event) {
        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Object obj = deserializer.getObject().orElse(null);
            if (!(obj instanceof com.stripe.model.Subscription)) return;

            com.stripe.model.Subscription stripeSub = (com.stripe.model.Subscription) obj;
            String stripeSubscriptionId = stripeSub.getId();

            Optional<Subscription> subOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
            if (subOpt.isEmpty()) return;

            Subscription sub = subOpt.get();
            sub.resetToFree();
            sub.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);

            System.out.println("✅ Subscription cancelled: " + sub.getUser().getEmail());

        } catch (Exception e) {
            System.err.println("❌ Error handling subscription deletion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------
    // ✅ HANDLE PAYMENT SUCCEEDED
    // ------------------------------------------------------
    private void handlePaymentSucceeded(Event event) {
        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Object obj = deserializer.getObject().orElse(null);
            if (!(obj instanceof Invoice)) return;

            Invoice invoice = (Invoice) obj;
            String stripeSubscriptionId = invoice.getSubscription();
            if (stripeSubscriptionId == null) return;

            Optional<Subscription> subOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
            if (subOpt.isEmpty()) return;

            Subscription sub = subOpt.get();
            sub.setUploadsUsed(0);
            sub.setResetDate(LocalDateTime.now().plusMonths(1));
            sub.setIsActive(true);
            sub.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);

            System.out.println("✅ Payment succeeded - uploads reset for: " + sub.getUser().getEmail());

        } catch (Exception e) {
            System.err.println("❌ Error handling payment success: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------
    // ✅ HANDLE PAYMENT FAILED
    // ------------------------------------------------------
    private void handlePaymentFailed(Event event) {
        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Object obj = deserializer.getObject().orElse(null);
            if (!(obj instanceof Invoice)) return;

            Invoice invoice = (Invoice) obj;
            String stripeSubscriptionId = invoice.getSubscription();
            if (stripeSubscriptionId == null) return;

            Optional<Subscription> subOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
            if (subOpt.isEmpty()) return;

            Subscription sub = subOpt.get();
            sub.setIsActive(false);
            sub.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);

            System.out.println("⚠️ Payment failed for: " + sub.getUser().getEmail());

        } catch (Exception e) {
            System.err.println("❌ Error handling payment failure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}