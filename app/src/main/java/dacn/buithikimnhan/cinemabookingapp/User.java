package dacn.buithikimnhan.cinemabookingapp;

public class User {

     String fullName;
     String email;
     String phone;
     String avatar;
     String createdAt;

    public User() {
    }

    public User(String fullName,
                String email,
                String phone,
                String avatar,
                String createdAt) {

        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.avatar = avatar;
        this.createdAt = createdAt;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }
}
