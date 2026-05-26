package dacn.buithikimnhan.cinemabookingapp.admin.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;

import dacn.buithikimnhan.cinemabookingapp.R;

public class ManageUserFragment extends Fragment {

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_user, container, false); // Nhớ tạo XML trống tương ứng
        db = FirebaseFirestore.getInstance();
        return view;
    }

    // Hàm gọi khi Admin muốn thực hiện Chặn một tài khoản bất kỳ
    public void blockUser(String targetUid) {
        db.collection("users").document(targetUid)
                .update("isBlocked", true) // Gán cờ chặn lên Firestore
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật giao diện thành công
                });
    }

    // Hàm gọi khi Admin muốn thăng cấp phân quyền cho User khác thành Admin phụ
    public void promoteToAdmin(String targetUid) {
        db.collection("users").document(targetUid)
                .update("role", "admin")
                .addOnSuccessListener(aVoid -> {
                    // Đã nâng cấp quyền hạn
                });
    }
}