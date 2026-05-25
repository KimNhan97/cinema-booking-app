package dacn.buithikimnhan.cinemabookingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // Ánh xạ đúng các ID từ file XML giao diện cinematic của bạn
    private EditText edtFullName, edtEmail, edtPassword, edtConfirmPassword;
    private CheckBox cbTerms;
    private TextView tvSignIn;
    private android.view.View btnCreateAccount; // Dùng View tổng quát vì XML của bạn khai báo thẻ <Button>

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo các công cụ Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Tiến hành ánh xạ View khớp hoàn toàn với Layout XML
        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvSignIn = findViewById(R.id.tvSignIn);

        // Xử lý sự kiện khi bấm nút tạo tài khoản
        btnCreateAccount.setOnClickListener(v -> handleRegister());

        // Chuyển ngược lại màn hình Login nếu bấm vào text đã có tài khoản
        tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void handleRegister() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        // 1. Kiểm tra họ và tên
        if (TextUtils.isEmpty(fullName)) {
            edtFullName.setError("Vui lòng nhập họ và tên của bạn");
            edtFullName.requestFocus();
            return;
        }

        // 2. Kiểm tra định dạng Email
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập Email");
            edtEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Định dạng email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        // 3. Kiểm tra độ dài mật khẩu
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải chứa ít nhất 6 ký tự");
            edtPassword.requestFocus();
            return;
        }

        // 4. Kiểm tra mật khẩu nhập lại có khớp nhau hay không
        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmPassword.setError("Vui lòng xác nhận lại mật khẩu");
            edtConfirmPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không trùng khớp!");
            edtConfirmPassword.requestFocus();
            return;
        }

        // 5. Kiểm tra xem người dùng đã tích chọn đồng ý điều khoản rạp phim chưa
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Bạn phải đồng ý với Điều khoản sử dụng của ứng dụng để tiếp tục!", Toast.LENGTH_LONG).show();
            return;
        }

        // Vô hiệu hóa nút bấm tạm thời để tránh user click lặp lại nhiều lần
        btnCreateAccount.setEnabled(false);

        // TIẾN HÀNH TẠO TÀI KHOẢN TRÊN FIREBASE AUTHENTICATION
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();

                            // Lấy ngày hiện tại thực tế để lưu thông tin ngày tạo tài khoản
                            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                            // Đóng gói thông tin để lưu song song xuống Firestore database gốc
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("uid", uid);
                            userData.put("fullName", fullName);
                            userData.put("email", email);
                            userData.put("phone", ""); // Khách hàng bổ sung sau ở trang cá nhân
                            userData.put("avatar", "avatar_url"); // URL ảnh đại diện mẫu ban đầu
                            userData.put("createdAt", currentDate);
                            userData.put("role", "customer"); // LUÔN GÁN CỐ ĐỊNH QUYỀN CUSTOMER KHI TỰ ĐĂNG KÝ
                            userData.put("isBlocked", false); // Cờ chặn tài khoản mặc định là false

                            // Thực hiện lệnh ghi dữ liệu xuống Firestore
                            db.collection("users").document(uid).set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterActivity.this, "Đăng ký tài khoản thành công!", Toast.LENGTH_SHORT).show();

                                        // Sau khi đăng ký thành công, đưa user thẳng vào màn hình chính MainActivity
                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(RegisterActivity.this, "Lỗi tạo hồ sơ Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        btnCreateAccount.setEnabled(true);
                                    });
                        }
                    } else {
                        // Lỗi tạo tài khoản từ hệ thống Auth (ví dụ email này đã có người đăng ký trước đó rồi)
                        Toast.makeText(RegisterActivity.this, "Lỗi đăng ký: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        btnCreateAccount.setEnabled(true);
                    }
                });
    }
}