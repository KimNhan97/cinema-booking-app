package dacn.buithikimnhan.cinemabookingapp.user.booking;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

import java.util.List;

import dacn.buithikimnhan.cinemabookingapp.user.MainActivity;
import dacn.buithikimnhan.cinemabookingapp.R;

public class TicketFragment extends Fragment {

    private ScrollView ticketScrollView;
    private LinearLayout ticketContainer;
    private LinearLayout layoutEmptyState;
    Button btnHome, btnBookNow;
    private FirebaseFirestore db;
    private LayoutInflater mInflater;
    private static final String TAG = "TicketFragmentLog";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ticket, container, false);

        this.mInflater = inflater;
        db = FirebaseFirestore.getInstance();

        ticketScrollView = view.findViewById(R.id.ticketScrollView);
        ticketContainer = view.findViewById(R.id.ticketContainer);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        btnHome = view.findViewById(R.id.btnHome);
        btnBookNow = view.findViewById(R.id.btnBookNow);

        View.OnClickListener goHomeListener = v -> {
            if (getActivity() != null) {
                Intent homeIntent = new Intent(getActivity(), MainActivity.class);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);
            }
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

        // Làm sạch vùng chứa vé trước khi nạp dữ liệu tránh lặp lặp View cũ rác
        ticketContainer.removeAllViews();

        db.collection("bookings")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("status", "checked_in")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {

                        int validTicketCount = 0;

                        // Duyệt qua toàn bộ danh sách vé trả về từ Cloud Firestore
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                            String status = documentSnapshot.getString("status");
                            if (status != null && !status.equalsIgnoreCase("checked_in")) {
                                continue;
                            }

                            validTicketCount++;

                            // Tạo phôi layout cho từng cuống vé đơn lẻ
                            View ticketView = mInflater.inflate(R.layout.item_ticket_single, ticketContainer, false);

                            TextView ticketMovieTitle = ticketView.findViewById(R.id.ticketMovieTitle);
                            TextView ticketShowTime = ticketView.findViewById(R.id.ticketShowTime);
                            TextView ticketRoom = ticketView.findViewById(R.id.ticketRoom);
                            TextView ticketSeats = ticketView.findViewById(R.id.ticketSeats);
                            TextView ticketPrice = ticketView.findViewById(R.id.ticketPrice);
                            TextView ticketBookingId = ticketView.findViewById(R.id.ticketBookingId);
                            ImageView ticketQRCode = ticketView.findViewById(R.id.ticketQRCode);
                            TextView ticketBookingDate = ticketView.findViewById(R.id.ticketBookingDate);

                            // Đổ dữ liệu text tiêu đề phim
                            if (documentSnapshot.contains("movieTitle")) {
                                ticketMovieTitle.setText(documentSnapshot.getString("movieTitle"));
                            }
                            // Đổ dữ liệu phòng rạp
                            if (documentSnapshot.contains("room")) {
                                ticketRoom.setText(documentSnapshot.getString("room"));
                            }
                            Object seatsObj = documentSnapshot.get("seats");
                            if (seatsObj != null) {
                                if (seatsObj instanceof List) {
                                    List<?> rawSeatsList = (List<?>) seatsObj;
                                    if (!rawSeatsList.isEmpty()) {
                                        StringBuilder sbSeats = new StringBuilder();
                                        for (int i = 0; i < rawSeatsList.size(); i++) {
                                            sbSeats.append(rawSeatsList.get(i).toString());
                                            if (i < rawSeatsList.size() - 1) {
                                                sbSeats.append(", ");
                                            }
                                        }
                                        ticketSeats.setText(sbSeats.toString());
                                    } else {
                                        ticketSeats.setText("Không có ghế");
                                    }
                                } else {
                                    ticketSeats.setText(seatsObj.toString());
                                }
                            } else {
                                ticketSeats.setText("---");
                            }

                            // Đọc mã hóa đơn an toàn (Lấy trường bookingId từ tài liệu)
                            String bookingId = documentSnapshot.getString("bookingId");
                            if (bookingId == null || bookingId.isEmpty()) {
                                bookingId = documentSnapshot.getId(); // Dự phòng lấy luôn DocumentId nếu null
                            }
                            ticketBookingId.setText("Mã HD: " + bookingId);

                            // TỰ ĐỘNG XỬ LÝ SINH MÃ QR TẠI ĐÂY
                            Bitmap qrBitmap = QRCodeHelper.generateQRCode(bookingId, 350, 350);
                            if (qrBitmap != null) {
                                ticketQRCode.setImageBitmap(qrBitmap);
                            }

                            // Xử lý ghép hiển thị chuỗi ngày giờ linh hoạt
                            String date = documentSnapshot.getString("date");
                            String startTime = documentSnapshot.getString("startTime");
                            if (startTime != null && date != null) {
                                if (startTime.contains(date)) {
                                    ticketShowTime.setText(startTime);
                                } else {
                                    ticketShowTime.setText(startTime + " | " + date);
                                }
                            }

                            // 🟢 THÊM: Đọc trường dữ liệu ngày đặt từ dữ liệu Cloud Firestore
                            if (ticketBookingDate != null) {
                                String bookedDateStr = documentSnapshot.getString("bookingDate");
                                if (bookedDateStr != null && !bookedDateStr.isEmpty()) {
                                    ticketBookingDate.setText(bookedDateStr);
                                } else {
                                    ticketBookingDate.setText("---");
                                }
                            }

                            // Đọc giá tiền đa năng và định dạng tiền tệ
                            Object priceObj = documentSnapshot.get("totalPrice");
                            if (priceObj != null) {
                                long priceValue = 0;
                                if (priceObj instanceof Number) {
                                    priceValue = ((Number) priceObj).longValue();
                                } else {
                                    try {
                                        priceValue = Long.parseLong(priceObj.toString().trim());
                                    } catch (NumberFormatException e) {
                                        priceValue = 0;
                                    }
                                }
                                // Định dạng hiển thị tiền tệ
                                String formattedPrice = String.format("%,d", priceValue) + "đ";
                                ticketPrice.setText(formattedPrice);
                            } else {
                                ticketPrice.setText("0đ");
                            }

                            // Thêm view cuống vé này vào khay chứa danh sách hiển thị
                            ticketContainer.addView(ticketView);
                        }

                        // Kiểm tra lại số lượng vé hợp lệ sau khi duyệt vòng lặp để hiển thị Layout thích hợp
                        if (validTicketCount > 0) {
                            Log.d(TAG, "Tìm thấy số lượng hóa đơn hợp lệ đã checked_in: " + validTicketCount);
                            ticketScrollView.setVisibility(View.VISIBLE);
                            layoutEmptyState.setVisibility(View.GONE);
                        } else {
                            Log.d(TAG, "Không có vé nào ở trạng thái 'checked_in'.");
                            showEmptyState();
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