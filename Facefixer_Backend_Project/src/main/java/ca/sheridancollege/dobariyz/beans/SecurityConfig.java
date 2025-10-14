package ca.sheridancollege.dobariyz.beans;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import ca.sheridancollege.dobariyz.repositories.UserRepository;
import ca.sheridancollege.dobariyz.services.JWTFilter;
import ca.sheridancollege.dobariyz.services.OAuthUserServices;
import ca.sheridancollege.dobariyz.util.JwtUtil;
import lombok.AllArgsConstructor;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {
	
	private JwtUtil jwtService;
	private OAuthUserServices oAuthUserService;
	private JWTFilter jwtFilter;
	
	@SuppressWarnings("removal")
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
		        .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
		        .csrf(csrf -> csrf.disable())
		        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		        .authorizeHttpRequests(auth -> auth
		        		.requestMatchers("/", "/auth/signupUser", "/auth/loginUser", "/auth/google",
		                        "/auth/validate-token", "/auth/generate-token", "/api/image","/api/recommendations").permitAll()
		        		.requestMatchers("/api/detect").authenticated()

		        		.anyRequest().authenticated()  // Everything else is protected
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // Ensure JWT filter is before authentication
                .oauth2Login(oauth -> oauth
                	    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oAuthUserService))
                	    .successHandler((request, response, authentication) -> {
                	        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                	        
                	        //  Get email properly
                	        String email = oauth2User.getAttribute("email");
                	        
                	        // Optional: if email is still null, fallback to "sub"
                	        if (email == null) {
                	            email = oauth2User.getAttribute("sub"); 
                	        }

                	        String jwtToken = jwtService.generateToken(email);
                	        response.setHeader("Authorization", "Bearer " + jwtToken);
                	        response.sendRedirect("http://localhost:5173/dashboard?token=" + jwtToken);
                	    })
                	)

                .logout(logout -> logout
                                .logoutSuccessUrl("/")
                                .invalidateHttpSession(true)
                                .deleteCookies("JSESSIONID")
                );

	    return httpSecurity.build();
	}
	
	@Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // Allow frontend origin
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
  }
