package ca.sheridancollege.dobariyz.services;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.sheridancollege.dobariyz.beans.User;
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

    @Transactional
    public String registerUser(String firstName, String lastName, String email, String password) {
    	
    	System.out.println("Received values: ");
        System.out.println("First Name: " + firstName);
        System.out.println("Last Name: " + lastName);
        System.out.println("Email: " + email);
        System.out.println("Password: " + password);

        if (firstName == null || lastName == null || email == null || password == null) {
            throw new IllegalArgumentException("All fields are required");
        }
        
    	List<User> users = userRepository.findByEmail(email);

    	if (!users.isEmpty()) {
    	    throw new IllegalArgumentException("Email already exists");
    	}

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // Hash password
        
        System.out.println("Saving user to database...");
        userRepository.save(user);
        System.out.println("User saved: " + user);

        String token = jwtUtil.generateToken(email);
        System.out.println("Generated Token: " + token);
        System.out.println("User saved successfully!");
        
        return token;

    }

    public String loginUser(String email, String password) {
    	 List<User> users = userRepository.findByEmail(email);
    	    if (users.isEmpty()) {
    	        throw new RuntimeException("User not found");  // This should return an error response instead
    	    }

    	    User user = users.get(0);
    	    if (!passwordEncoder.matches(password, user.getPassword())) {
    	        throw new RuntimeException("Invalid credentials");  // Same issue here
    	    }

    	    return jwtUtil.generateToken(email);
    }
}

