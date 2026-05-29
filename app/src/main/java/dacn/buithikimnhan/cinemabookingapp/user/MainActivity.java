package dacn.buithikimnhan.cinemabookingapp.user;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.user.booking.TicketFragment;
import dacn.buithikimnhan.cinemabookingapp.user.home.HomeFragment;
import dacn.buithikimnhan.cinemabookingapp.user.movie.FavoriteFragment;
import dacn.buithikimnhan.cinemabookingapp.user.profile.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        // 1. Hiển thị Fragment Trang chủ (HomeFragment) mặc định ngay khi mở app
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // 2. Thiết lập lắng nghe sự kiện chuyển đổi qua lại giữa các Tab của Khách hàng
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            // 3. Rẽ nhánh chuyển đổi giữa các màn hình chức năng của User
            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_favorites) {
                fragment = new FavoriteFragment();
            } else if (id == R.id.nav_tickets) {
                fragment = new TicketFragment();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            // 4. Thực hiện thay thế Fragment tương ứng lên giao diện
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }

            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        if (!isFinishing() && !isDestroyed()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }
}