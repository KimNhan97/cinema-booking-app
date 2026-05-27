package dacn.buithikimnhan.cinemabookingapp.admin.showtime;

public class MovieSpinnerItem {
     String id;
     String name;

    public MovieSpinnerItem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}