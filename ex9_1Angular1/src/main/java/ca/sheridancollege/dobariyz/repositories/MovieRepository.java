package ca.sheridancollege.dobariyz.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.sheridancollege.dobariyz.domain.Movie;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

	Optional<Movie> findByTitle(String title);
}
