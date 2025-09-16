package ca.sheridancollege.dobariyz.services;

import java.util.List;

import ca.sheridancollege.dobariyz.domain.Movie;


public interface MovieService {

	public List<Movie> findAll();
	public Movie findById(Long id);
	public Movie findByName(String title);
	public Movie save(Movie movie);
}
