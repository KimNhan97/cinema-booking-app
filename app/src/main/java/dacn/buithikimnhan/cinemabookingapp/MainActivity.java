package dacn.buithikimnhan.cinemabookingapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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

            // Rẽ nhánh chuyển đổi giữa các màn hình chức năng của User
            if (id == R.id.nav_home) {
                fragment = new HomeFragment();         // Màn hình chính xem danh sách phim
            } else if (id == R.id.nav_favorites) {
                fragment = new FavoriteFragment();     // Danh sách phim yêu thích
            } else if (id == R.id.nav_tickets) {
                fragment = new TicketFragment();       // Lịch sử các đơn vé đã mua
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();      // Trang cá nhân & thông tin thành viên
            }

            // Thực hiện thay thế Fragment tương ứng lên giao diện
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }

            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        // Kiểm tra an toàn trạng thái Lifecycle để tránh văng ứng dụng khi thao tác nhanh
        if (!isFinishing() && !isDestroyed()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }
}