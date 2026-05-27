package dacn.buithikimnhan.cinemabookingapp.admin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.admin.booking.AdminBookingFragment;
import dacn.buithikimnhan.cinemabookingapp.admin.dashboard.AdminDashboardFragment;
import dacn.buithikimnhan.cinemabookingapp.admin.movie.ManageMovieFragment;
import dacn.buithikimnhan.cinemabookingapp.admin.account.ManageUserFragment;
import dacn.buithikimnhan.cinemabookingapp.admin.showtime.AdminShowTimeFragment;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        BottomNavigationView bottomNav = findViewById(R.id.adminBottomNavigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.adminFragmentContainer, new AdminDashboardFragment()).commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) selectedFragment = new AdminDashboardFragment();
            else if (id == R.id.nav_movies) selectedFragment = new ManageMovieFragment();
            else if (id == R.id.nav_bookings) selectedFragment = new AdminBookingFragment();
            else if (id == R.id.nav_showtime) selectedFragment = new AdminShowTimeFragment();
            else if (id == R.id.nav_users) selectedFragment = new ManageUserFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.adminFragmentContainer, selectedFragment).commit();
            }
            return true;
        });
    }
}