package ca.sheridancollege.dobariyz.web.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.sheridancollege.dobariyz.domain.Movie;
import ca.sheridancollege.dobariyz.services.MovieService;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/movies")
@AllArgsConstructor
public class MovieController {

	private MovieService mRepo;
	
	@GetMapping(value={"","/"})
	public List<Movie> getMovies (){
		 return mRepo.findAll();
	}
	
	@GetMapping("/{id}")
	public Movie getMovies(@PathVariable long id){
			return mRepo.findById(id);
		
	}
	
	@PostMapping(value={""}, headers = {"Content-type=application/json"})
	public Movie addStudent(@RequestBody Movie movie) { // 
		movie.setId(null);
		movie.setTitle(movie.getTitle());
		movie.setRunTime(movie.getRunTime());
		movie.setGenre(movie.getGenre());
		return mRepo.save(movie);
	}
}
