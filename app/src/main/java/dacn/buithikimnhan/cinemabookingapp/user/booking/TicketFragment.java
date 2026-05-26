package dacn.buithikimnhan.cinemabookingapp.user.booking;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import dacn.buithikimnhan.cinemabookingapp.user.MainActivity;
import dacn.buithikimnhan.cinemabookingapp.R;

public class TicketFragment extends Fragment {

    private ScrollView ticketScrollView;
    private LinearLayout ticketContainer;
    private LinearLayout layoutEmptyState;
    private Button btnHome, btnBookNow;

    private FirebaseFirestore db;
    private LayoutInflater mInflater;
    private static final String TAG = "TicketFragmentLog";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ticket, container, false);

        this.mInflater = inflater;
        db = FirebaseFirestore.getInstance();

        // Ánh xạ các thành phần giao diện điều khiển ẩn/hiện
        ticketScrollView = view.findViewById(R.id.ticketScrollView);
        ticketContainer = view.findViewById(R.id.ticketContainer);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        btnHome = view.findViewById(R.id.btnHome);
        btnBookNow = view.findViewById(R.id.btnBookNow);

        View.OnClickListener goHomeListener = v -> {
            Intent homeIntent = new Intent(getActivity(), MainActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
        };

        btnHome.setOnClickListener(goHomeListener);
        btnBookNow.setOnClickListener(goHomeListener);

        // Nạp toàn bộ dữ liệu giao dịch vé
        loadAllUserTicketsFromFirestore();

        return view;
    }

    private void loadAllUserTicketsFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showEmptyState();
            return;
        }
        String currentUserId = currentUser.getUid();

        // Làm sạch vùng chứa vé trước khi nạp dữ liệu tránh lặp lặp View
        ticketContainer.removeAllViews();

        // THAY ĐỔI QUAN TRỌNG: Truy vấn phẳng đơn giản để tránh lỗi Index và lỗi lọc thời gian
        db.collection("bookings")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Tìm thấy số lượng hóa đơn: " + queryDocumentSnapshots.size());

                        // Hiển thị khung danh sách cuộn, ẩn màn hình trống
                        ticketScrollView.setVisibility(View.VISIBLE);
                        layoutEmptyState.setVisibility(View.GONE);

                        // Duyệt qua toàn bộ danh sách vé trả về từ Cloud Firestore
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                            // Tạo phôi layout cho từng cuống vé đơn lẻ
                            View ticketView = mInflater.inflate(R.layout.item_ticket_single, ticketContainer, false);

                            TextView ticketMovieTitle = ticketView.findViewById(R.id.ticketMovieTitle);
                            TextView ticketShowTime = ticketView.findViewById(R.id.ticketShowTime);
                            TextView ticketRoom = ticketView.findViewById(R.id.ticketRoom);
                            TextView ticketSeats = ticketView.findViewById(R.id.ticketSeats);
                            TextView ticketPrice = ticketView.findViewById(R.id.ticketPrice);
                            TextView ticketBookingId = ticketView.findViewById(R.id.ticketBookingId);

                            // Đổ dữ liệu text từ Database vào
                            if (documentSnapshot.contains("movieTitle")) {
                                ticketMovieTitle.setText(documentSnapshot.getString("movieTitle"));
                            }
                            if (documentSnapshot.contains("room")) {
                                ticketRoom.setText(documentSnapshot.getString("room"));
                            }

                            String documentId = documentSnapshot.getId();
                            ticketBookingId.setText("Mã HD: " + documentId);

                            // Xử lý ghép hiển thị chuỗi ngày giờ linh hoạt
                            String date = documentSnapshot.getString("date");
                            String startTime = documentSnapshot.getString("startTime");
                            if (startTime != null && date != null) {
                                if (startTime.contains(date)) {
                                    ticketShowTime.setText(startTime);
                                } else {
                                    ticketShowTime.setText(startTime + " (Ngày: " + date + ")");
                                }
                            }

                            Object priceObj = documentSnapshot.get("totalPrice");
                            if (priceObj != null) {
                                ticketPrice.setText(priceObj.toString() + "đ");
                            }

                            // Đọc và định dạng hiển thị danh sách mảng chuỗi ghế đặt
                            java.util.List<String> seatsList = (java.util.List<String>) documentSnapshot.get("seats");
                            if (seatsList != null && !seatsList.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < seatsList.size(); i++) {
                                    sb.append(seatsList.get(i));
                                    if (i < seatsList.size() - 1) sb.append(", ");
                                }
                                ticketSeats.setText(sb.toString());
                            } else {
                                ticketSeats.setText("Trống");
                            }

                            // Thêm view cuống vé này vào khay chứa danh sách hiển thị
                            ticketContainer.addView(ticketView);
                        }

                    } else {
                        Log.d(TAG, "Không có dữ liệu hóa đơn nào khớp.");
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi truy vấn Firebase Firestore: ", e);
                    if (isAdded()) {
                        showEmptyState();
                        Toast.makeText(getContext(), "Lỗi hệ thống Cloud: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showEmptyState() {
        ticketScrollView.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE); // Bật cấu trúc màn hình rỗng
    }
}