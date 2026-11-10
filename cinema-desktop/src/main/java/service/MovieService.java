package service;

import entity.Movie;
import repository.MovieRepository;

import java.util.List;

public class MovieService {

    private final MovieRepository repository;
    private final List<Movie> movies;

    public MovieService() {
        repository = new MovieRepository();
        movies = repository.findAll();
    }

    public void addMovie(Movie movie) {
        movie.setId(getNextId());
        movies.add(movie);
        repository.save(movies);
    }

    public List<Movie> getAllMovies() {
        return movies;
    }

    public Movie findById(long id) {
        for (Movie movie : movies) {
            if (movie.getId() == id) {
                return movie;
            }
        }
        return null;
    }

    public List<Movie> findByTitle(String title) {
        List<Movie> result = new java.util.ArrayList<>();
        String keyword = title.toLowerCase();

        for (Movie movie : movies) {
            if (movie.getTitle().toLowerCase().contains(keyword)) {
                result.add(movie);
            }
        }

        return result;
    }

    public List<Movie> findByGenre(String genre) {
        List<Movie> result = new java.util.ArrayList<>();
        String keyword = genre.toLowerCase();

        for (Movie movie : movies) {
            if (movie.getGenre().toLowerCase().contains(keyword)) {
                result.add(movie);
            }
        }

        return result;
    }

    public void updateMovie(Movie updatedMovie) {
        Movie movie = findById(updatedMovie.getId());

        if (movie != null) {
            movie.setTitle(updatedMovie.getTitle());
            movie.setDuration(updatedMovie.getDuration());
            movie.setGenre(updatedMovie.getGenre());
            movie.setDescription(updatedMovie.getDescription());
            repository.save(movies);
        }
    }

    public void deleteMovie(long id) {
        Movie movie = findById(id);

        if (movie != null) {
            movies.remove(movie);
            repository.save(movies);
        }
    }

    private long getNextId() {
        long maxId = 0;

        for (Movie movie : movies) {
            if (movie.getId() > maxId) {
                maxId = movie.getId();
            }
        }

        return maxId + 1;
    }
}
