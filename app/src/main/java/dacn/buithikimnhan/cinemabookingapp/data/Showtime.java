package dacn.buithikimnhan.cinemabookingapp.data;

public class Showtime {
    private String showtimeId;
    private String movieId;
    private String date;
    private String startTime;

    private String endTime;
     String room;
     int availableSeats;
     int totalSeats;
     String status;

    public Showtime() {}

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Getter và Setter
    public String getShowtimeId() { return showtimeId; }
    public void setShowtimeId(String showtimeId) { this.showtimeId = showtimeId; }
    public String getMovieId() { return movieId; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() {
        return endTime;
    }
}