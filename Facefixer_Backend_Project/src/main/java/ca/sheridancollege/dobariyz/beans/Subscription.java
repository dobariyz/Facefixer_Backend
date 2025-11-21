package ca.sheridancollege.dobariyz.beans;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "tier", nullable = false)
    private String tier = "free"; // 'free', 'premium', 'pro'

    @Column(name = "uploads_used", nullable = false)
    private Integer uploadsUsed = 0;

    @Column(name = "uploads_limit", nullable = false)
    private Integer uploadsLimit = 5;

    @Column(name = "reset_date")
    private LocalDateTime resetDate;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "last_webhook_event_id")
    private String lastWebhookEventId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Constructor for new users (creates default free subscription)
    public Subscription(User user) {
        this.user = user;
        this.tier = "free";
        this.uploadsUsed = 0;
        this.uploadsLimit = 5;
        this.resetDate = LocalDateTime.now().plusMonths(1);
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * ✅ UPDATED: Check if user can upload
     * Takes into account cancelled subscriptions that are still active until end date
     */
    public boolean canUpload() {
        // Check if subscription has expired (even if cancelled)
        if (subscriptionEndDate != null && LocalDateTime.now().isAfter(subscriptionEndDate)) {
            // Subscription expired, reset to free
            this.resetToFree();
            return this.uploadsUsed < this.uploadsLimit;
        }
        
        // Check if reset date has passed (monthly reset)
        if (resetDate != null && LocalDateTime.now().isAfter(resetDate)) {
            this.uploadsUsed = 0;
            this.resetDate = LocalDateTime.now().plusMonths(1);
            this.updatedAt = LocalDateTime.now();
        }
        
        return uploadsUsed < uploadsLimit;
    }

    // Helper method to increment upload count
    public void incrementUploads() {
        this.uploadsUsed++;
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method to get remaining uploads
    public Integer getRemainingUploads() {
        return uploadsLimit - uploadsUsed;
    }

    // Helper method to upgrade tier
    public void upgradeTier(String newTier) {
        this.tier = newTier;
        this.subscriptionStartDate = LocalDateTime.now();
        this.subscriptionEndDate = LocalDateTime.now().plusMonths(1);
        this.updatedAt = LocalDateTime.now();
        
        // Set limits based on tier
        switch(newTier.toLowerCase()) {
            case "premium":
                this.uploadsLimit = 50;
                break;
            case "pro":
                this.uploadsLimit = 999999; // Unlimited
                break;
            default:
                this.uploadsLimit = 5;
        }
    }

    /**
     * ✅ UPDATED: Check if subscription is expired
     * Returns true only if paid subscription has passed its end date
     */
    public boolean isExpired() {
        if (tier.equals("free")) return false; // Free never expires
        
        // If cancelled (isActive = false) but still within billing period, NOT expired
        if (subscriptionEndDate != null && LocalDateTime.now().isAfter(subscriptionEndDate)) {
            return true; // Past end date = expired
        }
        
        return false; // Still within billing period
    }

    /**
     * ✅ UPDATED: Reset to free tier
     * Called when subscription actually expires (not when cancelled!)
     */
    public void resetToFree() {
        this.tier = "free";
        this.uploadsLimit = 5;
        this.uploadsUsed = 0;
        this.isActive = true;
        this.stripeSubscriptionId = null;
        this.subscriptionStartDate = null;
        this.subscriptionEndDate = null;
        this.updatedAt = LocalDateTime.now();
        System.out.println("📉 Subscription reset to free tier");
    }
}
