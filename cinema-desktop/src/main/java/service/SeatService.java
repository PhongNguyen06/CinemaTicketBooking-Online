package service;

import entity.Room;
import entity.Seat;
import repository.SeatRepository;

import java.util.ArrayList;
import java.util.List;

public class SeatService {

    private final SeatRepository repository;
    private final List<Seat> seats;

    public SeatService(List<Room> rooms) {
        repository = new SeatRepository();
        seats = repository.findAll(rooms);
    }

    public void addSeat(Seat seat) {
        seat.setId(getNextId());
        seats.add(seat);
        repository.save(seats);
    }

    public List<Seat> getAllSeats() {
        return seats;
    }

    public Seat findById(long id) {
        for (Seat seat : seats) {
            if (seat.getId() == id) {
                return seat;
            }
        }
        return null;
    }

    public List<Seat> findByRoom(long roomId) {
        List<Seat> result = new ArrayList<>();

        for (Seat seat : seats) {
            if (seat.getRoom() != null
                    && seat.getRoom().getId() == roomId) {
                result.add(seat);
            }
        }

        return result;
    }

    public void updateSeat(Seat updatedSeat) {
        Seat seat = findById(updatedSeat.getId());

        if (seat != null) {
            seat.setRow(updatedSeat.getRow());
            seat.setNumber(updatedSeat.getNumber());
            seat.setType(updatedSeat.getType());
            seat.setRoom(updatedSeat.getRoom());
            repository.save(seats);
        }
    }

    public void deleteSeat(long id) {
        Seat seat = findById(id);

        if (seat != null) {
            seats.remove(seat);
            repository.save(seats);
        }
    }

    private long getNextId() {
        long maxId = 0;

        for (Seat seat : seats) {
            if (seat.getId() > maxId) {
                maxId = seat.getId();
            }
        }

        return maxId + 1;
    }
}
