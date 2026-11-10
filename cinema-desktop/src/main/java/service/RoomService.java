package service;

import entity.Cinema;
import entity.Room;
import repository.RoomRepository;

import java.util.ArrayList;
import java.util.List;

public class RoomService {

    private final RoomRepository repository;
    private final List<Room> rooms;
    private long nextId;

    public RoomService(List<Cinema> cinemas) {
        repository = new RoomRepository();
        rooms = repository.findAll(cinemas);
        nextId = getNextId();
    }

    public void addRoom(Room room) {
        room.setId(nextId);
        nextId++;
        rooms.add(room);
        repository.save(rooms);
    }

    public List<Room> getAllRooms() {
        return rooms;
    }

    public Room findById(long id) {
        for (Room room : rooms) {
            if (room.getId() == id) {
                return room;
            }
        }
        return null;
    }

    public List<Room> findByName(String name) {
        List<Room> result = new ArrayList<>();

        for (Room room : rooms) {
            if (room.getRoomName().toLowerCase().contains(name.toLowerCase())) {
                result.add(room);
            }
        }

        return result;
    }

    public List<Room> findByCinema(long cinemaId) {
        List<Room> result = new ArrayList<>();

        for (Room room : rooms) {
            if (room.getCinema() != null && room.getCinema().getId() == cinemaId) {
                result.add(room);
            }
        }

        return result;
    }

    public void updateRoom(Room updatedRoom) {
        Room room = findById(updatedRoom.getId());

        if (room != null) {
            room.setRoomName(updatedRoom.getRoomName());
            room.setCapacity(updatedRoom.getCapacity());
            room.setCinema(updatedRoom.getCinema());
            repository.save(rooms);
        }
    }

    public void deleteRoom(long id) {
        Room room = findById(id);

        if (room != null) {
            rooms.remove(room);
            repository.save(rooms);
        }
    }

    private long getNextId() {
        long maxId = 0;

        for (Room room : rooms) {
            if (room.getId() > maxId) {
                maxId = room.getId();
            }
        }

        return maxId + 1;
    }
}
