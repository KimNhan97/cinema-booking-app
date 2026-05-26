package dacn.buithikimnhan.cinemabookingapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import dacn.buithikimnhan.cinemabookingapp.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        // 1. ánh xạ lớp view
        View mainContent = findViewById(R.id.main_content);
        ImageView imgLogo = findViewById(R.id.img_logo);

        // 2. hiệu ứng Entrance
        if (mainContent != null) {
            Animation fadeInScaleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale);
            mainContent.startAnimation(fadeInScaleAnim);
        }

        // 3. Nạp hiệu ứng Mạch đập tỏa sáng liên tục (Pulse Glow)
        if (imgLogo != null) {
            Animation pulseGlowAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_glow);
            imgLogo.startAnimation(pulseGlowAnim);
        }

        // 4. hđ 2 giây để chuyển sang LoginActivity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);

                // Hiệu ứng chuyển cảnh mượt
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                finish();
            }
        }, 2000); // 2000 = 2 giây
    }
}