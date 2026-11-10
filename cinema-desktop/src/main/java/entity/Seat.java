package entity;

import enums.SeatType;

public class Seat {
    private long id;
    private String row;
    private int number;
    private SeatType type;
    private Room room;

    public Seat() {
    }

    public Seat(long id, String row, int number, SeatType type, Room room) {
        this.id = id;
        this.row = row;
        this.number = number;
        this.type = type;
        this.room = room;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public SeatType getType() {
        return type;
    }

    public void setType(SeatType type) {
        this.type = type;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}
