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

    private String movieName;

    public Showtime() {
    }

    public Showtime(String showtimeId, String movieId, String date, String startTime, String endTime, String room, int availableSeats, int totalSeats, String status, String movieName) {
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.availableSeats = availableSeats;
        this.totalSeats = totalSeats;
        this.status = status;
        this.movieName = movieName;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(String showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }
}