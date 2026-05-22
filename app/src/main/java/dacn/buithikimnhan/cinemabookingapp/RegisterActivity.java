package dacn.buithikimnhan.cinemabookingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    // Khai báo View

     EditText edtFullName;
     EditText edtEmail;
     EditText edtPassword;
     EditText edtConfirmPassword;
     Button btnCreateAccount;

     TextView tvSignIn;

     CheckBox cbTerms;

     FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1.Ánh xạ View

        edtFullName = findViewById(R.id.edtFullName);

        edtEmail = findViewById(R.id.edtEmail);

        edtPassword = findViewById(R.id.edtPassword);

        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        tvSignIn = findViewById(R.id.tvSignIn);

        cbTerms = findViewById(R.id.cbTerms);

        // 2.Khởi tạo Firebase Auth

        mAuth = FirebaseAuth.getInstance();

        // 3.Sự kiện nút Đăng ký

        btnCreateAccount.setOnClickListener(v -> registerUser());

        // 4.Chuyển sang màn hình Đăng nhập

        tvSignIn.setOnClickListener(v -> {

            Intent intent = new Intent(
                    RegisterActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);

            finish();
        });
    }

    //5. đăng ký tài khoản

    private void registerUser() {

        // 6.Lấy dữ liệu người dùng nhập

        String fullName =
                edtFullName.getText().toString().trim();

        String email =
                edtEmail.getText().toString().trim();

        String password =
                edtPassword.getText().toString().trim();

        String confirmPassword =
                edtConfirmPassword.getText().toString().trim();

        // 7.Kiểm tra họ tên

        if (TextUtils.isEmpty(fullName)) {

            edtFullName.setError("Vui lòng nhập họ tên");

            edtFullName.requestFocus();

            return;
        }

        // 8.Kiểm tra email

        if (TextUtils.isEmpty(email)) {

            edtEmail.setError("Vui lòng nhập email");

            edtEmail.requestFocus();

            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            edtEmail.setError("Email không hợp lệ");

            edtEmail.requestFocus();

            return;
        }

        // 9.Kiểm tra mật khẩu

        if (TextUtils.isEmpty(password)) {

            edtPassword.setError("Vui lòng nhập mật khẩu");

            edtPassword.requestFocus();

            return;
        }

        if (password.length() < 6) {

            edtPassword.setError(
                    "Mật khẩu phải có ít nhất 6 ký tự"
            );

            edtPassword.requestFocus();

            return;
        }

        // 10.Kiểm tra xác nhận mật khẩu

        if (TextUtils.isEmpty(confirmPassword)) {

            edtConfirmPassword.setError(
                    "Vui lòng xác nhận mật khẩu"
            );

            edtConfirmPassword.requestFocus();

            return;
        }

        if (!password.equals(confirmPassword)) {

            edtConfirmPassword.setError(
                    "Mật khẩu không trùng khớp!"
            );

            edtConfirmPassword.requestFocus();

            return;
        }

        // 11.Kiểm tra điều khoản

        if (!cbTerms.isChecked()) {

            Toast.makeText(
                    this,
                    "Vui lòng đồng ý điều khoản sử dụng",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // 12.Đăng ký Firebase

        mAuth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(task -> {

                    // Đăng ký thành công

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                RegisterActivity.this,
                                "Đăng ký tài khoản thành công! \uD83C\uDF89",
                                Toast.LENGTH_SHORT
                        ).show();

                        // Chuyển sang LoginActivity

                        Intent intent = new Intent(
                                RegisterActivity.this,
                                LoginActivity.class
                        );

                        startActivity(intent);

                        finish();

                    }

                    // Đăng ký thất bại

                    else {

                        Toast.makeText(
                                RegisterActivity.this,
                                "Đăng ký thất bại: "
                                        + task.getException()
                                        .getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}