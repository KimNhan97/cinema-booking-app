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
     ImageView btnBack;
     Button btnContinue;

     List<Seat> seatList = new ArrayList<>();
    private SeatAdapter seatAdapter;
    private FirebaseFirestore db;

    private String showtimeId = "show_001";
    private String movieTitle = "";
    private String showtimeDetails = "";

     long totalPrice = 0;
     List<Seat> selectedSeatsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        db = FirebaseFirestore.getInstance();

        // 1. Đọc dữ liệu động được truyền từ màn hình trước sang
        if (getIntent().hasExtra("SHOWTIME_ID")) {
            showtimeId = getIntent().getStringExtra("SHOWTIME_ID");
        }
        if (getIntent().hasExtra("MOVIE_TITLE")) {
            movieTitle = getIntent().getStringExtra("MOVIE_TITLE");
        }
        if (getIntent().hasExtra("SHOWTIME_INFO")) {
            showtimeDetails = getIntent().getStringExtra("SHOWTIME_INFO");
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
            // 1. Kiểm tra nếu chưa chọn ghế thì báo lỗi và dừng lại
            if (selectedSeatsList.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Nếu đã chọn ghế, hiển thị hộp thoại xác nhận độ tuổi chuẩn mẫu Galaxy Cinema
            showAgeConfirmationDialog();
        });
    }
    private void showAgeConfirmationDialog() {
        // Tạo trình dựng hộp thoại giao diện bo góc tiêu chuẩn hệ thống
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Xác nhận");
        builder.setMessage("Phim dành cho mọi độ tuổi. Tôi xác nhận rạp phim không được phép phục vụ khách hàng dưới 13 tuổi cho các suất chiếu kết thúc từ 22:00 và dưới 16 tuổi cho các suất chiếu kết thúc từ 23:00. Tôi đồng ý cung cấp giấy tờ tùy thân để xác thực độ tuổi người xem. Rạp sẽ không hoàn tiền nếu người xem không đáp ứng đủ điều kiện.");

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            navigateToPaymentInfo();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> {
            dialog.dismiss();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#D81B60"));
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"));
    }
    private void setupRecyclerView() {
        seatAdapter = new SeatAdapter(seatList);
        rvSeatMap.setLayoutManager(new GridLayoutManager(this, 9));
        rvSeatMap.setAdapter(seatAdapter);
    }

    private void displayMovieInformation() {
        if (movieTitle != null && !movieTitle.isEmpty()) {
            tvMovieTitle.setText(movieTitle);
        } else {
            tvMovieTitle.setText("Chưa rõ tên phim");
        }

        if (showtimeDetails != null && !showtimeDetails.isEmpty()) {
            tvShowtimeInfo.setText(showtimeDetails);
        } else {
            tvShowtimeInfo.setText("Chưa rõ suất chiếu");
        }

        tvTotalPrice.setText("0đ");
    }

    private void loadSeatsFromFirestore() {
        db.collection("showtimes")
                .document(showtimeId)
                .collection("seats")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    System.out.println("DEBUG_LOG: Kết nối thành công! Lấy được " + queryDocumentSnapshots.size() + " ghế từ document: " + showtimeId);

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
                            System.out.println("DEBUG_LOG: Lỗi ép kiểu class Seat tại document " + doc.getId() + " - " + e.getMessage());
                        }
                    }

                    // Mảng định nghĩa các hàng chạy từ màn hình (Hàng A) ra đến xa (Hàng J)
                    String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H", "J"};

                    // Vòng lặp dựng bố cục sơ đồ rạp tuyến tính
                    for (String rowLetter : rows) {
                        for (int colNum = 1; colNum <= 9; colNum++) {
                            String currentSeatKey = rowLetter + colNum;

                            if ((rowLetter.equals("E") || rowLetter.equals("F")) && colNum <= 3) {
                                seatList.add(new Seat("", "empty", 0));
                            } else {
                                if (firebaseSeatsMap.containsKey(currentSeatKey)) {
                                    seatList.add(firebaseSeatsMap.get(currentSeatKey));
                                } else {
                                    seatList.add(new Seat(currentSeatKey, "available", 60000));
                                }
                            }
                        }
                    }

                    seatAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    System.out.println("DEBUG_LOG: Thất bại khi kết nối Firebase! Lỗi: " + e.getMessage());
                    Toast.makeText(this, "Lỗi kết nối sơ đồ ghế: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void navigateToPaymentInfo() {
        // 1. Gom tên các ghế đã chọn thành chuỗi dạng: "J06, J05"
        StringBuilder seatsBuilder = new StringBuilder();
        for (int i = 0; i < selectedSeatsList.size(); i++) {
            seatsBuilder.append(selectedSeatsList.get(i).getSeatName());
            if (i < selectedSeatsList.size() - 1) {
                seatsBuilder.append(", ");
            }
        }

        // 2. Tạo chuỗi danh sách để truyền danh sách đối tượng sang nếu cần xử lý vòng lặp lẻ sau này
        Intent intent = new Intent(SeatSelectionActivity.this, PaymentInfoActivity.class);
        intent.putExtra("SHOWTIME_ID", showtimeId);
        intent.putExtra("MOVIE_TITLE", movieTitle != null && !movieTitle.isEmpty() ? movieTitle : "Doraemon Movie 45");
        intent.putExtra("SHOWTIME_INFO", showtimeDetails);
        intent.putExtra("TOTAL_PRICE", tvTotalPrice.getText().toString());
        intent.putExtra("SEATS_LIST", seatsBuilder.toString());
        intent.putExtra("ROOM_NAME", "RAP 5");

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

            // Xử lý ẩn các ô rỗng của lối đi rạp
            if (seat.getStatus().equals("empty") || seat.getSeatName() == null || seat.getSeatName().isEmpty()) {
                holder.tvSeat.setVisibility(View.INVISIBLE);
                return;
            }

            holder.tvSeat.setVisibility(View.VISIBLE);
            holder.tvSeat.setText(seat.getSeatName());

            // Thiết lập màu sắc hiển thị động theo đúng trạng thái từ Cloud Firestore
            switch (seat.getStatus()) {
                case "booked":
                    holder.tvSeat.setBackgroundResource(R.drawable.bg_seat_booked);
                    holder.tvSeat.setTextColor(Color.TRANSPARENT); // Ẩn chữ đi cho đẹp
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

            // Xử lý sự kiện click tương tác chọn/hủy chọn ghế
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