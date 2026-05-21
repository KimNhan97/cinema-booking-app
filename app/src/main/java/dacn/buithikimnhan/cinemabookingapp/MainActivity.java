package dacn.buithikimnhan.cinemabookingapp;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Gắn hiệu ứng nhún phóng to/thu nhỏ cho các Button/CardView
        View bookNowButton = findViewById(R.id.bottomNavigation); // Bạn có thể ánh xạ thêm nút khác
        setupMicroInteraction(bookNowButton);
    }

    private void setupMicroInteraction(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                }
                return false;
            }
        });
    }
}