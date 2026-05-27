package dacn.buithikimnhan.cinemabookingapp.user.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.auth.LoginActivity;

public class ProfileFragment extends Fragment {

    private ImageView imgAvatar, imgProfileCover;
    private TextView tvProfileName, tvMemberRank;
    TextView tvCountWatched, tvCountTickets, tvCountPoints;

    // Đã thay đổi: Sử dụng ViewPager2 để quản lý việc lướt ngang các cuống vé
    private ViewPager2 vpTickets;
    private LinearLayout layoutEmptyTicket;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        loadUserDataAndTickets();

        return view;
    }

    private void initViews(View view) {
        // Ánh xạ phần thông tin tài khoản
        imgAvatar = view.findViewById(R.id.imgAvatar);
        imgProfileCover = view.findViewById(R.id.imgProfileCover);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvMemberRank = view.findViewById(R.id.tvMemberRank);
        tvCountWatched = view.findViewById(R.id.tvCountWatched);
        tvCountTickets = view.findViewById(R.id.tvCountTickets);
        tvCountPoints = view.findViewById(R.id.tvCountPoints);

        // Ánh xạ thành phần lướt vé và thông báo trống
        vpTickets = view.findViewById(R.id.vpTickets);
        layoutEmptyTicket = view.findViewById(R.id.layoutEmptyTicket);

        // Sự kiện Đăng xuất
        view.findViewById(R.id.btnLogOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getContext(), "Đã đăng xuất tài khoản thành công", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void loadUserDataAndTickets() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        // 1. Lấy thông tin cá nhân của User từ collection "users"
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String avatarUrl = documentSnapshot.getString("avatarUrl");
                        Long points = documentSnapshot.getLong("points");
                        String rank = documentSnapshot.getString("rank");

                        tvProfileName.setText(name != null ? name : currentUser.getDisplayName());
                        tvCountPoints.setText(String.valueOf(points != null ? points : 0));
                        if (rank != null) tvMemberRank.setText("THÀNH VIÊN " + rank.toUpperCase());

                        if (avatarUrl != null && !avatarUrl.isEmpty() && isAdded()) {
                            Glide.with(this).load(avatarUrl).into(imgAvatar);
                            Glide.with(this).load(avatarUrl).into(imgProfileCover);
                        }
                    }
                });

        // 2. Lấy toàn bộ danh sách vé có trạng thái "booked" của User để truyền vào ViewPager2 lướt ngang
        db.collection("bookings")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "booked")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {

                        // Cập nhật số lượng vé đặt lên thông số "Số Vé Mua"
                        int totalTickets = queryDocumentSnapshots.size();
                        tvCountTickets.setText(String.valueOf(totalTickets));

                        // Khởi tạo Adapter liên kết mảng dữ liệu vé với ViewPager2
                        TicketPagerAdapter pagerAdapter = new TicketPagerAdapter(queryDocumentSnapshots.getDocuments());
                        vpTickets.setAdapter(pagerAdapter);

                        // Cấu hình lướt mượt: Cho phép load đệm sang 2 bên
                        vpTickets.setOffscreenPageLimit(1);

                        // Hiển thị khung lướt vé, ẩn layout trống
                        vpTickets.setVisibility(View.VISIBLE);
                        layoutEmptyTicket.setVisibility(View.GONE);

                    } else {
                        // Nếu không có vé nào
                        vpTickets.setVisibility(View.GONE);
                        layoutEmptyTicket.setVisibility(View.VISIBLE);
                        tvCountTickets.setText("0");
                    }
                })
                .addOnFailureListener(e -> {
                    vpTickets.setVisibility(View.GONE);
                    layoutEmptyTicket.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Không thể tải dữ liệu vé", Toast.LENGTH_SHORT).show();
                });
    }
}