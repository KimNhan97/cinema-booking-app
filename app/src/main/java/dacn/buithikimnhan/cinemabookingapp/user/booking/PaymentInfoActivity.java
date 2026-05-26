package dacn.buithikimnhan.cinemabookingapp.user.booking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.HashMap;
import java.util.Map;

import dacn.buithikimnhan.cinemabookingapp.user.MainActivity;
import dacn.buithikimnhan.cinemabookingapp.R;

public class PaymentInfoActivity extends AppCompatActivity {

     TextView tvMovieTitle, tvShowTime, tvMovieFormat, tvRoomName, tvSeatsSelected, tvTotalPrice;
     ImageView btnBack;
     Button btnSubmitPayment;

    private FirebaseFirestore db;
    private String showtimeId = "";
    private String seatsListString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_info);

        db = FirebaseFirestore.getInstance();

        initViews();
        getDataFromIntent();
    }

    private void initViews() {
        tvMovieTitle = findViewById(R.id.tvMovieTitle);
        tvShowTime = findViewById(R.id.tvShowTime);
        tvMovieFormat = findViewById(R.id.tvMovieFormat);
        tvRoomName = findViewById(R.id.tvRoomName);
        tvSeatsSelected = findViewById(R.id.tvSeatsSelected);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnBack = findViewById(R.id.btnBack);
        btnSubmitPayment = findViewById(R.id.btnSubmitPayment);

        btnBack.setOnClickListener(v -> finish());
        btnSubmitPayment.setOnClickListener(v -> showMockPaymentDialog());
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            showtimeId = intent.getStringExtra("SHOWTIME_ID");
            seatsListString = intent.getStringExtra("SEATS_LIST");

            tvMovieTitle.setText(intent.getStringExtra("MOVIE_TITLE"));
            tvShowTime.setText(intent.getStringExtra("SHOWTIME_INFO"));
            tvTotalPrice.setText(intent.getStringExtra("TOTAL_PRICE"));
            tvSeatsSelected.setText(seatsListString);

            if (intent.hasExtra("ROOM_NAME")) {
                tvRoomName.setText(intent.getStringExtra("ROOM_NAME"));
            }
        }
    }

    private void showMockPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thanh toán giả lập");
        builder.setMessage("Bạn có chắc chắn muốn tiến hành thanh toán số tiền " + tvTotalPrice.getText().toString() + " không?");

        builder.setPositiveButton("Chấp nhận", (dialog, which) -> {
            saveBookedSeatsToFirebase();
        });

        builder.setNegativeButton("Hủy bỏ", (dialog, which) -> {
            Toast.makeText(this, "Giao dịch đã bị hủy bỏ.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        builder.create().show();
    }

    private void saveBookedSeatsToFirebase() {
        // 1. Kiểm tra tài khoản người dùng đăng nhập thực tế
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện thanh toán!", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = currentUser.getUid(); // Lấy UID thật từ Firebase Auth

        if (showtimeId == null || showtimeId.isEmpty() || seatsListString == null || seatsListString.isEmpty()) {
            Toast.makeText(this, "Dữ liệu hóa đơn không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        String newBookingId = "booking_" + System.currentTimeMillis();
        WriteBatch batch = db.batch();

        // 2. Cập nhật trạng thái ghế trong Showtimes sang "booked"
        String[] seatsArray = seatsListString.split(", ");
        for (String seatName : seatsArray) {
            String seatNameClean = seatName.trim();
            if (seatNameClean.isEmpty()) continue;

            Map<String, Object> seatData = new HashMap<>();
            seatData.put("status", "booked");
            seatData.put("price", 60000);

            batch.set(db.collection("showtimes")
                            .document(showtimeId)
                            .collection("seats")
                            .document(seatNameClean),
                    seatData, com.google.firebase.firestore.SetOptions.merge());
        }

        // 3. Đóng gói bản ghi hóa đơn thật lưu vào collection "bookings" kèm UID thật
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", newBookingId);
        bookingData.put("movieTitle", tvMovieTitle.getText().toString());
        bookingData.put("room", tvRoomName.getText().toString());
        bookingData.put("status", "booked");

        // === ĐOẠN ĐÃ SỬA: Xử lý làm sạch chuỗi tiền tệ trước khi đưa lên Firebase ===
        String rawPrice = tvTotalPrice.getText().toString();
        // Loại bỏ chữ "đ", dấu phẩy ",", dấu chấm "." và khoảng trắng thừa
        String cleanPrice = rawPrice.replace("đ", "")
                .replaceAll("[.,]", "")
                .trim();
        bookingData.put("totalPrice", cleanPrice); // Bây giờ sẽ lưu là "120000" thay vì "120,000"
        // =========================================================================

        bookingData.put("userId", currentUserId);

        // Tách chuỗi suất chiếu (Ví dụ: "18:36 - 2026-05-25") để đưa vào Firestore dạng trường độc lập
        String fullShowTimeText = tvShowTime.getText().toString();
        if (fullShowTimeText.contains("-")) {
            String[] timeParts = fullShowTimeText.split("-");
            bookingData.put("startTime", timeParts[0].trim());
            bookingData.put("date", timeParts[1].trim());
        } else {
            bookingData.put("startTime", "18:36");
            bookingData.put("date", "2026-05-25");
        }

        java.util.List<String> seatsList = java.util.Arrays.asList(seatsArray);
        bookingData.put("seats", seatsList);

        batch.set(db.collection("bookings").document(newBookingId), bookingData);

        // 4. Thực thi Batch đẩy đồng thời lên Cloud
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đặt vé thành công!", Toast.LENGTH_SHORT).show();

                    // Điều hướng quay về màn hình chính MainActivity (Mặc định mở HomeFragment)
                    Intent intent = new Intent(PaymentInfoActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish(); // Đóng màn hình thanh toán
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi hệ thống: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}