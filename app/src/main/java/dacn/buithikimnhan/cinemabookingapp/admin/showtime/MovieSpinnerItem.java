package dacn.buithikimnhan.cinemabookingapp.admin.showtime;

public class MovieSpinnerItem {
     String id;
     String name;
    int duration;

    public MovieSpinnerItem(String id, String name, int duration) {
        this.id = id;
        this.name = name;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return name + " (" + duration + " phút)";
    }
}