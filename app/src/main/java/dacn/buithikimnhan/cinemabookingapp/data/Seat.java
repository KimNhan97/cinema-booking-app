package dacn.buithikimnhan.cinemabookingapp.data;

public class Seat {

    private String seatName;
    private String status;
    private long price;

    public Seat() {
    }

    public String getSeatName() {
        return seatName;
    }

    public Seat(String seatName, String status, long price) {
        this.seatName = seatName;
        this.status = status;
        this.price = price;
    }

    public void setSeatName(String seatName) {
        this.seatName = seatName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }
}

