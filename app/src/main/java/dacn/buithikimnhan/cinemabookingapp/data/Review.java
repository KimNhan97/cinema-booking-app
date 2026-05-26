package dacn.buithikimnhan.cinemabookingapp.data;

import java.io.Serializable;

public class Review implements Serializable {
    private String reviewId;
    private String movieId;
    private String userId;
    private String userName;
    private String userAvatar;
    private double rating;
    private String comment;
    private long timestamp; // Khớp chuẩn với Number (int64) trên Firestore

    // Hàm khởi tạo không đối số bắt buộc phải có để Firebase Firestore tự động ép dữ liệu (Mapping)
    public Review() {
    }

    // Hàm khởi tạo đầy đủ tham số
    public Review(String reviewId, String movieId, String userId, String userName, String userAvatar, double rating, String comment, long timestamp) {
        this.reviewId = reviewId;
        this.movieId = movieId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}