package ca.sheridancollege.dobariyz.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.sheridancollege.dobariyz.beans.Subscription;
import ca.sheridancollege.dobariyz.beans.User;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    Optional<Subscription> findByUser(User user);
    
    Optional<Subscription> findByUserId(Long userId);
    
    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);
    
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    boolean existsByUserId(Long userId);
}