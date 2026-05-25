package dacn.buithikimnhan.cinemabookingapp;

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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProfileFragment extends Fragment {

    private ImageView imgAvatar, imgProfileCover;
    private TextView tvProfileName, tvMemberRank;
     TextView tvCountWatched, tvCountTickets, tvCountPoints;
    private CardView cardTicketContainer;
    private LinearLayout layoutEmptyTicket;
    private TextView ticketMovieTitle, ticketShowTime, ticketRoom, ticketSeats, ticketPrice, ticketBookingId;

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

        // Ánh xạ trực tiếp các ô thông tin trên layout Vé đục lỗ
        cardTicketContainer = view.findViewById(R.id.cardTicketContainer);
        layoutEmptyTicket = view.findViewById(R.id.layoutEmptyTicket);

        ticketMovieTitle = view.findViewById(R.id.ticketMovieTitle);
        ticketShowTime = view.findViewById(R.id.ticketShowTime);
        ticketRoom = view.findViewById(R.id.ticketRoom);
        ticketSeats = view.findViewById(R.id.ticketSeats);
        ticketPrice = view.findViewById(R.id.ticketPrice);
        ticketBookingId = view.findViewById(R.id.ticketBookingId);
        //đăng xuất
        view.findViewById(R.id.btnLogOut).setOnClickListener(v -> {

            // 1. Thực hiện đăng xuất tài khoản khỏi Firebase Auth hệ thống
            FirebaseAuth.getInstance().signOut();

            // Thông báo cho người dùng biết
            Toast.makeText(getContext(), "Đã đăng xuất tài khoản thành công", Toast.LENGTH_SHORT).show();

            // 2. Tạo Intent để quay về màn hình Đăng nhập (LoginActivity)
            Intent intent = new Intent(getActivity(), LoginActivity.class);

            // 3. Đặt cờ xóa sạch toàn bộ các Activity trước đó (Xóa MainActivity đang chạy ngầm)
            // Giúp chặn việc người dùng nhấn nút Back trên điện thoại quay ngược lại trang cá nhân
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Kích hoạt chuyển màn hình
            startActivity(intent);

            // Tạo hiệu ứng chuyển cảnh mượt mà (Fade Out giao diện cũ và Fade In giao diện Login)
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

        // 2. Dựa vào userId lấy danh sách vé trong collection "bookings"
        db.collection("bookings")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "booked") // Chỉ lấy vé đã thanh toán/đặt thành công
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {

                        // Cập nhật tổng số lượng vé đã đặt lên màn hình cá nhân
                        int totalTickets = queryDocumentSnapshots.size();
                        tvCountTickets.setText(String.valueOf(totalTickets));

                        // Lấy ra tài liệu vé đầu tiên (Mới nhất) để điền thẳng thông tin lên UI
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);

                        // Gán các trường Text cơ bản
                        ticketMovieTitle.setText(doc.getString("movieTitle"));
                        ticketShowTime.setText(doc.getString("startTime"));
                        ticketRoom.setText(doc.getString("room"));
                        ticketBookingId.setText("Mã HD: " + doc.getString("bookingId"));

                        // --- XỬ LÝ TRƯỜNG TOTALPRICE KIỂU INT / LONG AN TOÀN ---
                        try {
                            Long price = doc.getLong("totalPrice");
                            if (price != null) {
                                // Định dạng hiển thị số phân tách hàng nghìn (Ví dụ: 60,000đ)
                                ticketPrice.setText(String.format("%,dđ", price));
                            } else {
                                ticketPrice.setText("0đ");
                            }
                        } catch (Exception e) {
                            // Trường hợp phòng hờ nếu có vé cũ trong DB vẫn lưu kiểu chuỗi String
                            String stringPrice = doc.getString("totalPrice");
                            if (stringPrice != null) {
                                ticketPrice.setText(stringPrice.contains("đ") ? stringPrice : stringPrice + "đ");
                            } else {
                                ticketPrice.setText("0đ");
                            }
                        }

                        // --- XỬ LÝ TRƯỜNG SEATS (GHẾ ĐẶT) AN TOÀN ---
                        Object seatsObj = doc.get("seats");
                        if (seatsObj instanceof List) {
                            // Nếu Firestore lưu dạng mảng: ["F4", "F5"] -> Chuyển thành chuỗi "F4, F5"
                            List<String> listSeats = (List<String>) seatsObj;
                            ticketSeats.setText(String.join(", ", listSeats));
                        } else if (seatsObj != null) {
                            // Nếu Firestore lưu dạng chuỗi văn bản: "F4"
                            ticketSeats.setText(seatsObj.toString());
                        } else {
                            ticketSeats.setText("-");
                        }

                        // Đổi trạng thái hiển thị: Hiện vé - Ẩn thông báo trống
                        cardTicketContainer.setVisibility(View.VISIBLE);
                        layoutEmptyTicket.setVisibility(View.GONE);

                    } else {
                        // Nếu tài khoản chưa từng đặt vé nào
                        cardTicketContainer.setVisibility(View.GONE);
                        layoutEmptyTicket.setVisibility(View.VISIBLE);
                        tvCountTickets.setText("0");
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý khi mất mạng hoặc lỗi kết nối Firestore
                    cardTicketContainer.setVisibility(View.GONE);
                    layoutEmptyTicket.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Không thể tải dữ liệu vé", Toast.LENGTH_SHORT).show();
                });
    }
}