package service;

import entity.Cinema;
import repository.CinemaRepository;

import java.util.ArrayList;
import java.util.List;

public class CinemaService {

    private final CinemaRepository repository;
    private final List<Cinema> cinemas;

    public CinemaService() {
        repository = new CinemaRepository();
        cinemas = repository.findAll();
    }

    public void addCinema(Cinema cinema) {
        cinema.setId(getNextId());
        cinemas.add(cinema);
        repository.save(cinemas);
    }

    public List<Cinema> getAllCinemas() {
        return cinemas;
    }

    public Cinema findById(long id) {
        for (Cinema cinema : cinemas) {
            if (cinema.getId() == id) {
                return cinema;
            }
        }
        return null;
    }

    public List<Cinema> findByName(String name) {
        List<Cinema> result = new ArrayList<>();

        for (Cinema cinema : cinemas) {
            if (cinema.getName().toLowerCase().contains(name.toLowerCase())) {
                result.add(cinema);
            }
        }

        return result;
    }

    public List<Cinema> findByAddress(String address) {
        List<Cinema> result = new ArrayList<>();

        for (Cinema cinema : cinemas) {
            if (cinema.getAddress().toLowerCase().contains(address.toLowerCase())) {
                result.add(cinema);
            }
        }

        return result;
    }

    public void updateCinema(Cinema updatedCinema) {
        Cinema cinema = findById(updatedCinema.getId());

        if (cinema != null) {
            cinema.setName(updatedCinema.getName());
            cinema.setAddress(updatedCinema.getAddress());
            repository.save(cinemas);
        }
    }

    public void deleteCinema(long id) {
        Cinema cinema = findById(id);

        if (cinema != null) {
            cinemas.remove(cinema);
            repository.save(cinemas);
        }
    }

    private long getNextId() {
        long maxId = 0;

        for (Cinema cinema : cinemas) {
            if (cinema.getId() > maxId) {
                maxId = cinema.getId();
            }
        }

        return maxId + 1;
    }
}
