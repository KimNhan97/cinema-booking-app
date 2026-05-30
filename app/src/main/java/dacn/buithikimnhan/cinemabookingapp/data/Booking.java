package dacn.buithikimnhan.cinemabookingapp.data;

import java.io.Serializable;
import java.util.List;

public class Booking implements Serializable {

    private String bookingId;
    private String bookingDate;

    private String date;
    private String movieTitle;
    private String room;
    private List<String> seats;
    private String startTime;
    private String status;
    private long totalPrice;
    private String userId;

    public Booking() {
    }

    public Booking(String bookingId,
                   String bookingDate,
                   String date,
                   String movieTitle,
                   String room,
                   List<String> seats,
                   String startTime,
                   String status,
                   long totalPrice,
                   String userId) {

        this.bookingId = bookingId;
        this.bookingDate = bookingDate;
        this.date = date;
        this.movieTitle = movieTitle;
        this.room = room;
        this.seats = seats;
        this.startTime = startTime;
        this.status = status;
        this.totalPrice = totalPrice;
        this.userId = userId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void setSeats(List<String> seats) {
        this.seats = seats;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}