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
import com.google.firebase.firestore.DocumentSnapshot; // Thêm import này
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import dacn.buithikimnhan.cinemabookingapp.user.MainActivity;
import dacn.buithikimnhan.cinemabookingapp.R;

public class PaymentInfoActivity extends AppCompatActivity {

    // Thêm tvUserName và tvUserContact vào đây
    TextView tvMovieTitle, tvShowTime, tvMovieFormat, tvRoomName, tvSeatsSelected, tvTotalPrice, tvUserName, tvUserContact;
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
        loadUserProfile(); // Gọi hàm tải thông tin người dùng ở đây
    }

    private void initViews() {
        tvMovieTitle = findViewById(R.id.tvMovieTitle);
        tvShowTime = findViewById(R.id.tvShowTime);
        tvMovieFormat = findViewById(R.id.tvMovieFormat);
        tvRoomName = findViewById(R.id.tvRoomName);
        tvSeatsSelected = findViewById(R.id.tvSeatsSelected);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);

        // Ánh xạ View thông tin người nhận từ XML
        tvUserName = findViewById(R.id.tvUserName);
        tvUserContact = findViewById(R.id.tvUserContact);

        btnBack = findViewById(R.id.btnBack);
        btnSubmitPayment = findViewById(R.id.btnSubmitPayment);

        btnBack.setOnClickListener(v -> finish());
        btnSubmitPayment.setOnClickListener(v -> showMockPaymentDialog());
    }

    // Hàm lấy dữ liệu người dùng đang đăng nhập từ Firestore
    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            tvUserName.setText("Chưa đăng nhập");
            tvUserContact.setText("-");
            return;
        }

        String uid = currentUser.getUid();

        // Truy vấn vào bảng "users", document có ID trùng với UID tài khoản
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Lấy các trường dữ liệu dựa theo key đã lưu trên Firestore
                        String fullName = documentSnapshot.getString("fullName");
                        String phone = documentSnapshot.getString("phone");
                        String email = documentSnapshot.getString("email");

                        // Hiển thị lên giao diện (Nếu null thì dùng giá trị mặc định)
                        tvUserName.setText(fullName != null ? fullName : "Người dùng");

                        String contactText = (phone != null ? phone : "") + " - " + (email != null ? email : "");
                        tvUserContact.setText(contactText);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không thể tải thông tin người nhận!", Toast.LENGTH_SHORT).show();
                });
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            String roomName = intent.getStringExtra("ROOM_NAME");

            Toast.makeText(this, "ROOM = " + roomName, Toast.LENGTH_LONG).show();
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện thanh toán!", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = currentUser.getUid();

        if (showtimeId == null || showtimeId.isEmpty() || seatsListString == null || seatsListString.isEmpty()) {
            Toast.makeText(this, "Dữ liệu hóa đơn không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        String newBookingId = "booking_" + System.currentTimeMillis();
        WriteBatch batch = db.batch();

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

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", newBookingId);
        bookingData.put("movieTitle", tvMovieTitle.getText().toString());
        bookingData.put("room", tvRoomName.getText().toString());
        bookingData.put("status", "booked");

        String rawPrice = tvTotalPrice.getText().toString();
        String cleanPrice = rawPrice.replace("đ", "")
                .replaceAll("[.,]", "")
                .trim();

        int totalPriceInt = 0;
        try {
            totalPriceInt = Integer.parseInt(cleanPrice);
        } catch (NumberFormatException e) {
            // Không xử lý lỗi định dạng
        }
        bookingData.put("totalPrice", totalPriceInt);
        bookingData.put("userId", currentUserId);

        String fullShowTimeText = tvShowTime.getText().toString();
        String extractedStartTime = "18:36";
        String extractedDate = "2026-05-25";

        try {
            if (fullShowTimeText.contains("|")) {
                String[] parts = fullShowTimeText.split("\\|");
                String timePart = parts[0].trim();
                extractedDate = parts[1].trim().replace("/", "-");

                if (timePart.contains("~")) {
                    extractedStartTime = timePart.split("~")[0].trim();
                } else if (timePart.contains("-")) {
                    extractedStartTime = timePart.split("-")[0].trim();
                } else {
                    extractedStartTime = timePart;
                }
            } else if (fullShowTimeText.contains("-")) {
                String[] timeParts = fullShowTimeText.split("-");
                extractedStartTime = timeParts[0].trim();
                extractedDate = timeParts[1].trim();
            }
        } catch (Exception e) {
            // Đề phòng lỗi phân tách chuỗi ngoài ý muốn, giữ nguyên giá trị mặc định ban đầu
        }

        bookingData.put("startTime", extractedStartTime);
        bookingData.put("date", extractedDate);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String currentBookingDate = sdf.format(new Date());
        bookingData.put("bookingDate", currentBookingDate);

        java.util.List<String> seatsList = java.util.Arrays.asList(seatsArray);
        bookingData.put("seats", seatsList);

        batch.set(db.collection("bookings").document(newBookingId), bookingData);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đặt vé thành công!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(PaymentInfoActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi hệ thống: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}