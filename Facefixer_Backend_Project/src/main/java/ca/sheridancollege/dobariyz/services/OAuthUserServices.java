


package ca.sheridancollege.dobariyz.services;

import java.util.List;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.UserRepository;

@Service
public class OAuthUserServices extends OidcUserService {

    private final UserRepository userRepository;

    public OAuthUserServices(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

//    @Override
//    public OidcUser loadUser(OidcUserRequest userRequest) {
//    	
//    	// Calls the default implementation to load the OIDC user information from the identity provider (IdP)
//        OidcUser oidcUser = super.loadUser(userRequest);
//
//        // Extracts user attributes from the OIDC token
//        String email = oidcUser.getAttribute("email");
//        String firstName = oidcUser.getAttribute("given_name");
//        String lastName = oidcUser.getAttribute("family_name");
//        String profilePicture = oidcUser.getAttribute("picture");
//
//        // Searches for an existing user in the database by email
//       List<User> existingUser = userRepository.findByEmail(email);
//
//        // If the user does not exist in the database, create a new user entry
//        if (existingUser.isEmpty()) {
//            User newUser = new User();
//            newUser.setEmail(email);
//            newUser.setFirstName(firstName);
//            newUser.setLastName(lastName);
//            newUser.setPassword(null); // no password for Google users
//            newUser.setProfilePicture(profilePicture);
//            userRepository.save(newUser);
//     }
//        
//        return oidcUser;
//    }
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        String firstName = oidcUser.getAttribute("given_name");
        String lastName = oidcUser.getAttribute("family_name");
        String profilePicture = oidcUser.getAttribute("picture");

        List<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstName(firstName != null ? firstName : "");
            newUser.setLastName(lastName != null ? lastName : "");
            newUser.setProfilePicture(profilePicture);
            newUser.setPassword(null); // no password for Google users
            newUser.setRole("USER");
            userRepository.save(newUser);
        } else {
            // update picture/name if changed
            User user = existingUser.get(0);
            user.setFirstName(firstName != null ? firstName : user.getFirstName());
            user.setLastName(lastName != null ? lastName : user.getLastName());
            user.setProfilePicture(profilePicture);
            userRepository.save(user);
        }

        return oidcUser;
    }
}

