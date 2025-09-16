package ca.sheridancollege.dobariyz.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import ca.sheridancollege.dobariyz.domain.Movie;
import ca.sheridancollege.dobariyz.services.MovieService;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class BootstrapData implements CommandLineRunner {

	private MovieService movieService;
	
	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub

		 	movieService.save(Movie.builder().title("Inception").runTime(148).genre("Sci-Fi").build());
	        movieService.save(Movie.builder().title("The Dark Knight").runTime(152).genre("Action").build());
	        movieService.save(Movie.builder().title("Interstellar").runTime(169).genre("Sci-Fi").build());
	        movieService.save(Movie.builder().title("Titanic").runTime(195).genre("Romance").build());
	        movieService.save(Movie.builder().title("The Godfather").runTime(175).genre("Crime").build());
	    
	}

}
