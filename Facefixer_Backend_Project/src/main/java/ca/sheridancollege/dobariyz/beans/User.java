package ca.sheridancollege.dobariyz.beans;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "users")
public class User {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@Column(name = "first_name", nullable = false)
	private String firstName;
	
	@Column(name = "last_name", nullable = false)
    private String lastName;
    
	@Column(unique = true, nullable = false)
	private String email;
    
	private String profilePicture;
	
	@Column(nullable = true)
	private String password;
	
	@Column(name = "terms_accepted_at")
	private LocalDateTime termsAcceptedAt;	
	
	private String role;
	
	// One-to-One relationship with Subscription (optional, for easier access)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Subscription subscription;
}
