package dacn.buithikimnhan.cinemabookingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 1. Ánh xạ an toàn sử dụng lớp View (Khắc phục triệt để lỗi ClassCastException)
        View mainContent = findViewById(R.id.main_content);
        ImageView imgLogo = findViewById(R.id.img_logo);

        // 2. Nạp và kích hoạt hiệu ứng Entrance cho toàn bộ khối nội dung
        if (mainContent != null) {
            Animation fadeInScaleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale);
            mainContent.startAnimation(fadeInScaleAnim);
        }

        // 3. Nạp hiệu ứng Mạch đập tỏa sáng liên tục (Pulse Glow) áp riêng cho biểu tượng Logo
        if (imgLogo != null) {
            Animation pulseGlowAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_glow);
            imgLogo.startAnimation(pulseGlowAnim);
        }

        // 4. Thực thi logic đếm ngược delay 2 giây (2000ms) để chuyển sang LoginActivity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Khởi tạo Intent điều hướng màn hình
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);

                // Hiệu ứng chuyển cảnh mượt mà giữa 2 Activity
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                // Kết thúc SplashActivity để người dùng không quay lại được khi bấm nút Back
                finish();
            }
        }, 2000); // 2000 mili-giây tương đương 2 giây
    }
}