package dacn.buithikimnhan.cinemabookingapp.user.booking;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Seat;

public class SeatSelectionActivity extends AppCompatActivity {

    private RecyclerView rvSeatMap;
    private TextView tvTotalPrice, tvMovieTitle, tvShowtimeInfo;
    private ImageView btnBack;
    private Button btnContinue;

    private List<Seat> seatList = new ArrayList<>();
    private SeatAdapter seatAdapter;
    private FirebaseFirestore db;

    private String showtimeId = "show_001";
    private String movieTitle = "";
    private String showtimeDetails = "";
    // Thêm biến roomName vào class để quản lý động tên phòng chiếu
    private String roomName = "Phòng chiếu";

    private long totalPrice = 0;
    private List<Seat> selectedSeatsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        db = FirebaseFirestore.getInstance();

        // 1. Đọc dữ liệu động được truyền từ màn hình lịch chiếu trước sang
        if (getIntent().hasExtra("SHOWTIME_ID")) {
            showtimeId = getIntent().getStringExtra("SHOWTIME_ID");
        }
        if (getIntent().hasExtra("MOVIE_TITLE")) {
            movieTitle = getIntent().getStringExtra("MOVIE_TITLE");
        }
        if (getIntent().hasExtra("SHOWTIME_INFO")) {
            showtimeDetails = getIntent().getStringExtra("SHOWTIME_INFO");
        }
        if (getIntent().hasExtra("ROOM_NAME")) {
            roomName = getIntent().getStringExtra("ROOM_NAME");
        }

        initViews();
        setupRecyclerView();

        // 2. Đổ dữ liệu phim lên Bottom Bar ngay khi vào màn hình
        displayMovieInformation();

        // 3. Tải sơ đồ ghế từ Firestore
        loadSeatsFromFirestore();
    }

    private void initViews() {
        rvSeatMap = findViewById(R.id.rvSeatMap);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvMovieTitle = findViewById(R.id.tvMovieTitle);
        tvShowtimeInfo = findViewById(R.id.tvShowtimeInfo);
        btnBack = findViewById(R.id.btnBack);
        btnContinue = findViewById(R.id.btnContinue);

        btnBack.setOnClickListener(v -> finish());

        btnContinue.setOnClickListener(v -> {
            // Kiểm tra nếu chưa chọn ghế thì báo lỗi và dừng lại
            if (selectedSeatsList.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Nếu đã chọn ghế, hiển thị hộp thoại xác nhận độ tuổi chuẩn Galaxy Cinema
            showAgeConfirmationDialog();
        });
    }

    private void showAgeConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Xác nhận độ tuổi");
        builder.setMessage("Phim dành cho mọi độ tuổi. Tôi xác nhận rạp phim không được phép phục vụ khách hàng dưới 13 tuổi cho các suất chiếu kết thúc từ 22:00 và dưới 16 tuổi cho các suất chiếu kết thúc từ 23:00. Tôi đồng ý cung cấp giấy tờ tùy thân để xác thực độ tuổi người xem. Rạp sẽ không hoàn tiền nếu người xem không đáp ứng đủ điều kiện.");

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            navigateToPaymentInfo();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#D81B60"));
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"));
    }

    private void setupRecyclerView() {
        seatAdapter = new SeatAdapter(seatList);
        rvSeatMap.setLayoutManager(new GridLayoutManager(this, 9)); // Rạp gồm 9 cột ghế
        rvSeatMap.setAdapter(seatAdapter);
    }

    private void displayMovieInformation() {
        tvMovieTitle.setText((movieTitle != null && !movieTitle.isEmpty()) ? movieTitle : "Chưa rõ tên phim");
        tvShowtimeInfo.setText((showtimeDetails != null && !showtimeDetails.isEmpty()) ? showtimeDetails : "Chưa rõ suất chiếu");
        tvTotalPrice.setText("0đ");
    }

    private void loadSeatsFromFirestore() {
        db.collection("showtimes")
                .document(showtimeId)
                .collection("seats")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    seatList.clear();
                    Map<String, Seat> firebaseSeatsMap = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Seat seat = doc.toObject(Seat.class);
                            if (seat != null) {
                                seat.setSeatName(doc.getId());
                                firebaseSeatsMap.put(doc.getId(), seat);
                            }
                        } catch (Exception e) {
                            System.out.println("DEBUG_LOG: Lỗi ép kiểu dữ liệu ghế: " + e.getMessage());
                        }
                    }

                    // Mảng tạo cấu trúc hàng ghế từ A -> J
                    String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H", "J"};

                    for (String rowLetter : rows) {
                        for (int colNum = 1; colNum <= 9; colNum++) {
                            String currentSeatKey = rowLetter + colNum;

                            // Tạo lối đi trống ảo ở hàng E và F (Cột 1,2,3 ẩn đi)
                            if ((rowLetter.equals("E") || rowLetter.equals("F")) && colNum <= 3) {
                                seatList.add(new Seat("", "empty", 0));
                            } else {
                                if (firebaseSeatsMap.containsKey(currentSeatKey)) {
                                    seatList.add(firebaseSeatsMap.get(currentSeatKey));
                                } else {
                                    // Tạo ghế mặc định nếu trên Firestore chưa khởi tạo document ghế này
                                    seatList.add(new Seat(currentSeatKey, "available", 60000));
                                }
                            }
                        }
                    }
                    seatAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi kết nối sơ đồ ghế: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToPaymentInfo() {
        StringBuilder seatsBuilder = new StringBuilder();
        for (int i = 0; i < selectedSeatsList.size(); i++) {
            seatsBuilder.append(selectedSeatsList.get(i).getSeatName());
            if (i < selectedSeatsList.size() - 1) {
                seatsBuilder.append(", ");
            }
        }

        Intent intent = new Intent(SeatSelectionActivity.this, PaymentInfoActivity.class);
        intent.putExtra("SHOWTIME_ID", showtimeId);
        intent.putExtra("MOVIE_TITLE", (movieTitle != null && !movieTitle.isEmpty()) ? movieTitle : "Phim chưa đặt tên");
        intent.putExtra("SHOWTIME_INFO", showtimeDetails);
        intent.putExtra("TOTAL_PRICE", tvTotalPrice.getText().toString());
        intent.putExtra("SEATS_LIST", seatsBuilder.toString());

        // Truyền biến toàn cục roomName thay vì hardcode "RAP 5" như cũ
        intent.putExtra("ROOM_NAME", roomName);

        startActivity(intent);
    }

    private void calculateTotalPrice() {
        totalPrice = 0;
        for (Seat s : selectedSeatsList) {
            totalPrice += s.getPrice();
        }
        if (totalPrice == 0) {
            tvTotalPrice.setText("0đ");
        } else {
            tvTotalPrice.setText(String.format("%,dđ", totalPrice));
        }
    }

    // --- SEAT ADAPTER ---
    private class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.SeatViewHolder> {
        private List<Seat> list;

        public SeatAdapter(List<Seat> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seat, parent, false);
            return new SeatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
            Seat seat = list.get(position);

            if (seat.getStatus().equals("empty") || seat.getSeatName() == null || seat.getSeatName().isEmpty()) {
                holder.tvSeat.setVisibility(View.INVISIBLE);
                return;
            }

            holder.tvSeat.setVisibility(View.VISIBLE);
            holder.tvSeat.setText(seat.getSeatName());

            switch (seat.getStatus()) {
                case "booked":
                    holder.tvSeat.setBackgroundResource(R.drawable.bg_seat_booked);
                    holder.tvSeat.setTextColor(Color.TRANSPARENT);
                    break;
                case "selected":
                    holder.tvSeat.setBackgroundResource(R.drawable.bg_seat_selected);
                    holder.tvSeat.setTextColor(Color.WHITE);
                    break;
                case "available":
                default:
                    holder.tvSeat.setBackgroundResource(R.drawable.bg_seat_available);
                    holder.tvSeat.setTextColor(Color.parseColor("#7A53D5"));
                    break;
            }

            holder.itemView.setOnClickListener(v -> {
                if (seat.getStatus().equals("booked")) return;

                if (seat.getStatus().equals("available")) {
                    seat.setStatus("selected");
                    selectedSeatsList.add(seat);
                } else if (seat.getStatus().equals("selected")) {
                    seat.setStatus("available");
                    selectedSeatsList.remove(seat);
                }

                notifyItemChanged(position);
                calculateTotalPrice();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class SeatViewHolder extends RecyclerView.ViewHolder {
            TextView tvSeat;
            public SeatViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSeat = itemView.findViewById(R.id.tvSeatNameItem);
            }
        }
    }
}