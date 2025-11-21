


package ca.sheridancollege.dobariyz.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import ca.sheridancollege.dobariyz.beans.Subscription;
import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.SubscriptionRepository;
import ca.sheridancollege.dobariyz.repositories.UserRepository;

@Service
public class OAuthUserServices extends OidcUserService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public OAuthUserServices(UserRepository userRepository, SubscriptionRepository subscriptionRepository) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        String firstName = oidcUser.getAttribute("given_name");
        String lastName = oidcUser.getAttribute("family_name");
        String profilePicture = oidcUser.getAttribute("picture");

        User user;
        List<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isEmpty()) {
            // ✅ Create new user
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName != null ? firstName : "");
            user.setLastName(lastName != null ? lastName : "");
            user.setProfilePicture(profilePicture);
            user.setPassword(null); // no password for Google users
            user.setRole("USER");

            user = userRepository.save(user);
            System.out.println("✅ New Google user created: " + user.getEmail());
        } else {
            // ✅ Update details if changed
            user = existingUser.get(0);
            user.setFirstName(firstName != null ? firstName : user.getFirstName());
            user.setLastName(lastName != null ? lastName : user.getLastName());
            user.setProfilePicture(profilePicture);
            user = userRepository.save(user);
        }

        // ✅ Ensure every Google user has a subscription
        if (user.getSubscription() == null) {
            Subscription subscription = new Subscription(user); // uses your default tier constructor
            subscriptionRepository.save(subscription);
            System.out.println("✅ Free subscription created for Google user: " + user.getEmail());
        }

        return oidcUser;
    }
}

