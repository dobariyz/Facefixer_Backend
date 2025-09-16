package ca.sheridancollege.dobariyz.services;

import java.util.List;

import org.springframework.stereotype.Service;

import ca.sheridancollege.dobariyz.domain.Movie;
import ca.sheridancollege.dobariyz.repositories.MovieRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MovieServiceImpl implements MovieService {

	MovieRepository mRepo;
	
	@Override
	public List<Movie> findAll() {
		return mRepo.findAll();
	}

	@Override
	public Movie findById(Long id) {

		if(mRepo.findById(id).isPresent())
			return mRepo.findById(id).get();
			else
				return null;
	}

	@Override
	public Movie findByName(String title) {
		if(mRepo.findByTitle(title).isPresent())
			return mRepo.findByTitle(title).get();
			else
				return null;
	}

	@Override
	public Movie save(Movie movie) {
		return mRepo.save(movie);
	}

}
