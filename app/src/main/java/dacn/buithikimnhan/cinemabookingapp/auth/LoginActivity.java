package dacn.buithikimnhan.cinemabookingapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import dacn.buithikimnhan.cinemabookingapp.admin.AdminActivity;
import dacn.buithikimnhan.cinemabookingapp.user.MainActivity;
import dacn.buithikimnhan.cinemabookingapp.R;

public class LoginActivity extends AppCompatActivity {

    EditText edtEmail, edtPassword;
    AppCompatButton btnLogin;
    TextView tvRegister;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tvRegister);

        // Khởi tạo các instance của Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Click login
        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
        });
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Định dạng email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtPassword.requestFocus();
            return;
        }

        // Vô hiệu hóa nút bấm tạm thời để tránh user click liên tục trong lúc đợi mạng phản hồi
        btnLogin.setEnabled(false);

        // Firebase Login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Lấy UID tài khoản vừa đăng nhập thành công
                        String uid = mAuth.getCurrentUser().getUid();

                        // TIẾN HÀNH PHÂN QUYỀN VÀ KIỂM TRA PHONG TỎA TÀI KHOẢN TỪ FIRESTORE
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {

                                        // 1. Kiểm tra trạng thái Khóa tài khoản (isBlocked)
                                        Boolean isBlocked = documentSnapshot.getBoolean("isBlocked");
                                        if (isBlocked != null && isBlocked) {
                                            // Tài khoản bị chặn -> Đăng xuất ngay lập tức
                                            mAuth.signOut();
                                            Toast.makeText(LoginActivity.this, "Tài khoản của bạn đã bị khóa do vi phạm quy định!", Toast.LENGTH_LONG).show();
                                            btnLogin.setEnabled(true);
                                            return;
                                        }

                                        // 2. Kiểm tra quyền hạn vai trò (role) để chuyển hướng màn hình thích hợp
                                        String role = documentSnapshot.getString("role");
                                        Intent intent;

                                        if (role != null && role.equalsIgnoreCase("admin")) {
                                            // Là Quản trị viên -> Đi tới AdminActivity
                                            Toast.makeText(LoginActivity.this, "Đăng nhập hệ thống quản trị thành công", Toast.LENGTH_SHORT).show();
                                            intent = new Intent(LoginActivity.this, AdminActivity.class);
                                        } else {
                                            // Là khách hàng thông thường -> Vào trang đặt vé phim MainActivity
                                            Toast.makeText(LoginActivity.this, "Đăng Nhập Thành Công", Toast.LENGTH_SHORT).show();
                                            intent = new Intent(LoginActivity.this, MainActivity.class);
                                        }

                                        startActivity(intent);
                                        finish(); // Đóng LoginActivity để không quay lại được khi bấm phím back

                                    } else {
                                        // Trường hợp hiếm gặp: Có tài khoản Auth nhưng không tìm thấy document trong collection 'users'
                                        Toast.makeText(LoginActivity.this, "Dữ liệu người dùng không tồn tại!", Toast.LENGTH_SHORT).show();
                                        btnLogin.setEnabled(true);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LoginActivity.this, "Lỗi kiểm tra quyền hạn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    btnLogin.setEnabled(true);
                                });

                    } else {
                        // Đăng nhập thất bại (Sai email hoặc sai mật khẩu)
                        Toast.makeText(
                                LoginActivity.this,
                                "Sai tài khoản hoặc mật khẩu. Vui lòng thử lại!",
                                Toast.LENGTH_LONG
                        ).show();
                        btnLogin.setEnabled(true);
                    }
                });
    }
}