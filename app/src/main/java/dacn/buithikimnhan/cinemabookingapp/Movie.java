package dacn.buithikimnhan.cinemabookingapp;

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

    public Movie() {
    }

    public Movie(String movieId,
                 String title,
                 String description,
                 String genre,
                 int duration,
                 String posterUrl,
                 String bannerUrl,
                 String releaseDate,
                 String status) {

        this.movieId = movieId;
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.duration = duration;
        this.posterUrl = posterUrl;
        this.bannerUrl = bannerUrl;
        this.releaseDate = releaseDate;
        this.status = status;
    }

    public String getMovieId() {
        return movieId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getGenre() {
        return genre;
    }

    public int getDuration() {
        return duration;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getStatus() {
        return status;
    }
    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}