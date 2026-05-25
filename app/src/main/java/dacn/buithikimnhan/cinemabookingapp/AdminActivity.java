package dacn.buithikimnhan.cinemabookingapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        BottomNavigationView bottomNav = findViewById(R.id.adminBottomNavigation);

        // Hiển thị Fragment mặc định lúc mở màn hình là Dashboard
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.adminFragmentContainer, new DashboardFragment()).commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) selectedFragment = new DashboardFragment();
            else if (id == R.id.nav_movies) selectedFragment = new ManageMovieFragment();
//            else if (id == R.id.nav_showtimes) selectedFragment = new ManageShowtimeFragment();
//            else if (id == R.id.nav_bookings) selectedFragment = new ManageBookingFragment();
            else if (id == R.id.nav_users) selectedFragment = new ManageUserFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.adminFragmentContainer, selectedFragment).commit();
            }
            return true;
        });
    }
}