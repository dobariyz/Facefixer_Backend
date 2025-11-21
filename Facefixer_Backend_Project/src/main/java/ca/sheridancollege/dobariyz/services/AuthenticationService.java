package ca.sheridancollege.dobariyz.services;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.sheridancollege.dobariyz.beans.Subscription;
import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.SubscriptionRepository;
import ca.sheridancollege.dobariyz.repositories.UserRepository;
import ca.sheridancollege.dobariyz.util.JwtUtil;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Transactional // ✅ Ensure both user and subscription are created together
    public String registerUser(String firstName, String lastName, String email, String password) {
        // 1. Check if email already exists
        Optional<User> existingUser = userRepository.findFirstByEmail(email);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // 2. Create and save new user
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER"); // Default role
        
        User savedUser = userRepository.save(user);
        System.out.println("✅ User created: " + savedUser.getEmail());

        // 3. ✅ AUTO-CREATE FREE SUBSCRIPTION (NEW!)
        Subscription subscription = new Subscription(savedUser); // Uses constructor with defaults
        subscriptionRepository.save(subscription);
        System.out.println("✅ Free subscription created for user: " + savedUser.getEmail());
        System.out.println("   Tier: " + subscription.getTier());
        System.out.println("   Uploads: " + subscription.getUploadsUsed() + "/" + subscription.getUploadsLimit());

        // 4. Generate JWT token
        String token = jwtUtil.generateToken(email);
        
        return token;
    }

    /**
     * ✅ Login user
     */
    public String loginUser(String email, String password) {
        Optional<User> userOpt = userRepository.findFirstByEmail(email);
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOpt.get();

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Generate JWT token
        return jwtUtil.generateToken(email);
    }
}

