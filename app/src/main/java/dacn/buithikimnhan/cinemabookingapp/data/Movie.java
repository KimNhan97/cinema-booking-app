package dacn.buithikimnhan.cinemabookingapp.data;

import java.io.Serializable;

public class Movie implements Serializable {

    String movieId;
    String title;
    String description;
    String genre;
    int duration;
    String posterUrl;
    String bannerUrl;
    String releaseDate;
    String status;
    private double averageRating = 0.0;
    private int ratingCount = 0;
    private double totalRating = 0.0;

    public Movie() {
    }

    public Movie(String movieId, String title, String description, String genre, int duration, String posterUrl, String bannerUrl, String releaseDate, String status, double averageRating, int ratingCount, double totalRating) {
        this.movieId = movieId;
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.duration = duration;
        this.posterUrl = posterUrl;
        this.bannerUrl = bannerUrl;
        this.releaseDate = releaseDate;
        this.status = status;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
        this.totalRating = totalRating;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    // SỬA ĐỔI: Getter trả về kiểu double
    public double getTotalRating() {
        return totalRating;
    }

    // SỬA ĐỔI: Setter nhận tham số truyền vào kiểu double
    public void setTotalRating(double totalRating) {
        this.totalRating = totalRating;
    }
}