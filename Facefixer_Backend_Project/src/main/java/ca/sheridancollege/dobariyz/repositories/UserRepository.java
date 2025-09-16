package ca.sheridancollege.dobariyz.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.sheridancollege.dobariyz.beans.User;

public interface UserRepository extends JpaRepository<User, Long> {
	
	List<User> findByEmail(String email);

	//Optional<User> findByEmail(String email);
	Optional<User> findFirstByEmail(String email);
    Optional<User> findByRole(String role);
    Optional<User> findFirstByEmailIgnoreCase(String email);

}