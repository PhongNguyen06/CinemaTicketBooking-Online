package entity;

public class Movie {
    private long id;
    private String title;
    private int duration;
    private String genre;
    private String description;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Movie(long id, String title, int duration, String genre, String description) {
        this.id = id;
        this.title = title;
        this.duration = duration;
        this.genre = genre;
        this.description = description;
    }

    public Movie() {
    }
}
