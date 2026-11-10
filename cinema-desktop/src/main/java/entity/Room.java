package entity;

public class Room {
    private long id;
    private String roomName;
    private int capacity;
    private Cinema cinema;

    public Room() {
    }

    public Room(long id, String roomName, int capacity, Cinema cinema) {
        this.id = id;
        this.roomName = roomName;
        this.capacity = capacity;
        this.cinema = cinema;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
    }
}
