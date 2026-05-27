package dacn.buithikimnhan.cinemabookingapp.admin.account;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;
import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.User;

public class ManageUserFragment extends Fragment {

    private RecyclerView rvUsers;
    private EditText edtSearch;
    private FirebaseFirestore db;
    private List<User> fullList;
    private List<User> filteredList;
    private UserAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_user, container, false);

        rvUsers = view.findViewById(R.id.rvUsers);
        edtSearch = view.findViewById(R.id.edtSearchUser);
        db = FirebaseFirestore.getInstance();

        fullList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Khởi tạo Adapter cố định ngay từ đầu để quản lý luồng dữ liệu an toàn hơn
        adapter = new UserAdapter(filteredList);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsers.setAdapter(adapter);

        listenToUsersRealtime();

        // Xử lý sự kiện tìm kiếm Realtime khi nhập text
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    // Lắng nghe sự thay đổi của collection "users" trên Firestore theo thời gian thực
    private void listenToUsersRealtime() {
        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            fullList.clear();
            for (QueryDocumentSnapshot doc : value) {
                User user = doc.toObject(User.class);
                // Nếu uid trong object bị null thì lấy tạm Id của Document
                if (user.getUid() == null || user.getUid().isEmpty()) {
                    user.setUid(doc.getId());
                }
                fullList.add(user);
            }
            filter(edtSearch.getText().toString());
        });
    }

    // Hàm lọc dữ liệu danh sách theo từ khóa tìm kiếm (Tên hoặc Email)
    private void filter(String keyword) {
        filteredList.clear();
        String cleanKeyword = keyword.toLowerCase().trim();
        for (User user : fullList) {
            String name = user.getFullName() != null ? user.getFullName().toLowerCase() : "";
            String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
            if (name.contains(cleanKeyword) || email.contains(cleanKeyword)) {
                filteredList.add(user);
            }
        }
        // Gọi phương thức cập nhật danh sách an toàn
        adapter.setData(filteredList);
    }

    // =================================================================================
    // ADAPTER RECYCLERVIEW HIỂN THỊ DANH SÁCH USER VÀ HÀM LOGIC CHỨC NĂNG
    // =================================================================================
    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<User> items;

        public UserAdapter(List<User> items) {
            this.items = new ArrayList<>(items);
        }

        // Hàm cập nhật danh sách dữ liệu mới và làm mới giao diện thủ công công khai
        public void setData(List<User> newItems) {
            this.items.clear();
            if (newItems != null) {
                this.items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
            return new UserViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = items.get(position);

            holder.txtName.setText(user.getFullName());
            holder.txtEmail.setText(user.getEmail());

            // ĐỔI MỚI: Đổ dữ liệu SĐT và Ngày tạo tài khoản lên UI
            String userPhone = user.getPhone();
            holder.txtPhone.setText("SĐT: " + (userPhone != null && !userPhone.isEmpty() ? userPhone : "Chưa cập nhật"));

            String userCreatedAt = user.getCreatedAt();
            holder.txtCreatedAt.setText("Ngày tham gia: " + (userCreatedAt != null && !userCreatedAt.isEmpty() ? userCreatedAt : "--/--/----"));

            // Avatar dạng ký tự đầu tiên viết hoa
            if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                holder.txtAvatar.setText(user.getFullName().substring(0, 1).toUpperCase());
            } else {
                holder.txtAvatar.setText("U");
            }

            // Phân biệt màu sắc Huy hiệu quyền (Role Badge)
            boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
            holder.txtRoleBadge.setText(isAdmin ? "ADMIN" : "USER");
            if (isAdmin) {
                holder.txtRoleBadge.setTextColor(Color.parseColor("#FFFFFF"));
                holder.txtRoleBadge.setBackgroundColor(Color.parseColor("#E53E3E")); // Huy hiệu đỏ cho Admin

                // BẢO VỆ TUYỆT ĐỐI: Ẩn luôn 2 nút thao tác Khóa/Xóa nếu đó là tài khoản Admin
                holder.btnLock.setVisibility(View.INVISIBLE);
                holder.btnDelete.setVisibility(View.INVISIBLE);
            } else {
                holder.txtRoleBadge.setTextColor(Color.parseColor("#2B6CB0"));
                holder.txtRoleBadge.setBackgroundColor(Color.parseColor("#EBF8FF")); // Huy hiệu xanh dương cho User
                holder.btnLock.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.VISIBLE);
            }

            // Phân biệt trạng thái Khóa / Hoạt động bằng biến boolean isBlocked
            if (user.isBlocked()) {
                holder.txtStatus.setText("ĐÃ KHÓA");
                holder.txtStatus.setTextColor(Color.parseColor("#C53030"));
                holder.txtStatus.setBackgroundColor(Color.parseColor("#FED7D7"));
                holder.btnLock.setText("Mở khóa");
                holder.btnLock.setTextColor(Color.parseColor("#48BB78")); // Màu xanh lá cây để mở lại
            } else {
                holder.txtStatus.setText("HOẠT ĐỘNG");
                holder.txtStatus.setTextColor(Color.parseColor("#2F855A"));
                holder.txtStatus.setBackgroundColor(Color.parseColor("#C6F6D5"));
                holder.btnLock.setText("Khóa TK");
                holder.btnLock.setTextColor(Color.parseColor("#ECC94B")); // Màu vàng cảnh báo khóa
            }

            // XỬ LÝ SỰ KIỆN: KHÓA / MỞ KHÓA TÀI KHOẢN
            holder.btnLock.setOnClickListener(v -> {
                if (isAdmin) {
                    Toast.makeText(getContext(), "Không thể khóa tài khoản của Admin hệ thống!", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean nextBlockState = !user.isBlocked();
                String confirmMsg = user.isBlocked() ?
                        "Bạn muốn mở khóa hoạt động cho tài khoản này?" :
                        "Bạn chắc chắn muốn khóa tài khoản này? Người dùng tương ứng sẽ bị từ chối đăng nhập!";

                new AlertDialog.Builder(getContext())
                        .setTitle("Xác nhận trạng thái")
                        .setMessage(confirmMsg)
                        .setNegativeButton("HỦY BỎ", null)
                        .setPositiveButton("ĐỒNG Ý", (dialog, which) -> {
                            db.collection("users").document(user.getUid())
                                    .update("isBlocked", nextBlockState)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Đã cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show());
                        }).show();
            });

            // XỬ LÝ SỰ KIỆN: XÓA VĨNH VIỄN TÀI KHOẢN KHỎI FIRESTORE
            holder.btnDelete.setOnClickListener(v -> {
                if (isAdmin) {
                    Toast.makeText(getContext(), "Không được phép xóa tài khoản Admin!", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(getContext())
                        .setTitle("Cảnh báo xóa vĩnh viễn")
                        .setMessage("Bạn chắc chắn muốn xóa tài khoản " + user.getEmail() + " ra khỏi hệ thống? Dữ liệu này không thể khôi phục.")
                        .setNegativeButton("HỦY BỎ", null)
                        .setPositiveButton("XÓA BỎ", (dialog, which) -> {
                            db.collection("users").document(user.getUid())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Đã xóa người dùng thành công!", Toast.LENGTH_SHORT).show());
                        }).show();
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView txtAvatar, txtName, txtEmail, txtRoleBadge, txtStatus;
            TextView txtPhone, txtCreatedAt;
            Button btnLock, btnDelete;

            public UserViewHolder(@NonNull View v) {
                super(v);
                txtAvatar = v.findViewById(R.id.txtAvatar);
                txtName = v.findViewById(R.id.txtUserName);
                txtEmail = v.findViewById(R.id.txtUserEmail);
                txtRoleBadge = v.findViewById(R.id.txtRoleBadge);
                txtStatus = v.findViewById(R.id.txtStatusBadge);
                btnLock = v.findViewById(R.id.btnLockUser);
                btnDelete = v.findViewById(R.id.btnDeleteUser);

                // Ánh xạ thành phần giao diện mới được thêm vào
                txtPhone = v.findViewById(R.id.txtUserPhone);
                txtCreatedAt = v.findViewById(R.id.txtUserCreatedAt);
            }
        }
    }
}