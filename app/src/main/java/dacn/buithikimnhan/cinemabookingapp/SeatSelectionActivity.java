package dacn.buithikimnhan.cinemabookingapp;

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
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeatSelectionActivity extends AppCompatActivity {

    private RecyclerView rvSeatMap;
    private TextView tvTotalPrice, tvMovieTitle, tvShowtimeInfo;
    private ImageView btnBack;
    private Button btnContinue;

    private List<Seat> seatList = new ArrayList<>();
    private SeatAdapter seatAdapter;

    private FirebaseFirestore db;

    // Lưu thông tin nhận từ Intent
    private String showtimeId = "show_001";
    private String movieTitle = "";
    private String showtimeDetails = "";

    private long totalPrice = 0;
    private List<Seat> selectedSeatsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        db = FirebaseFirestore.getInstance();

        // 1. Đọc dữ liệu động được truyền từ màn hình MovieDetailActivity sang
        if (getIntent().hasExtra("SHOWTIME_ID")) {
            showtimeId = getIntent().getStringExtra("SHOWTIME_ID");
        }
        // Nhận tiêu đề phim động
        if (getIntent().hasExtra("MOVIE_TITLE")) {
            movieTitle = getIntent().getStringExtra("MOVIE_TITLE");
        }
        // Nhận chuỗi ngày giờ chiếu chi tiết động (VD: "20:15~22:35 | Thứ 7, 23/05/2026")
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
            if (selectedSeatsList.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế!", Toast.LENGTH_SHORT).show();
                return;
            }
            updateSeatsToFirebase();
        });
    }

    private void setupRecyclerView() {
        seatAdapter = new SeatAdapter(seatList);
        rvSeatMap.setLayoutManager(new GridLayoutManager(this, 9)); // Luôn cố định lưới 9 cột
        rvSeatMap.setAdapter(seatAdapter);
    }

    // Gán dữ liệu phim động lên giao diện góc dưới rạp
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

    // ================= THUẬT TOÁN ĐỔ SƠ ĐỒ GHẾ CHUẨN TỪ A -> J =================
    private void loadSeatsFromFirestore() {
        db.collection("showtimes")
                .document(showtimeId)
                .collection("seats")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    seatList.clear();

                    // Dùng HashMap để gom ghế từ Firebase theo từ khóa Tên Ghế (VD: "A1" -> Đối tượng Seat)
                    Map<String, Seat> firebaseSeatsMap = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Seat seat = doc.toObject(Seat.class);
                        if (seat != null) {
                            seat.setSeatName(doc.getId()); // ID Document trên Firestore là "A1", "A2"...
                            firebaseSeatsMap.put(doc.getId(), seat);
                        }
                    }

                    // Mảng định nghĩa các hàng chạy từ màn hình (Hàng A) ra đến xa (Hàng J)
                    String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H", "J"};

                    // Vòng lặp dựng bố cục sơ đồ rạp tuyến tính
                    for (String rowLetter : rows) {
                        for (int colNum = 1; colNum <= 9; colNum++) {
                            String currentSeatKey = rowLetter + colNum; // Tạo chuỗi khóa để tra cứu (VD: "A1", "B5")

                            // Kiểm tra xem hàng E, F có phải là khoảng trống lối đi bên góc trái (Cột 1, 2, 3) hay không
                            if ((rowLetter.equals("E") || rowLetter.equals("F")) && colNum <= 3) {
                                seatList.add(new Seat("", "empty", 0));
                            } else {
                                // Kiểm tra xem trên Firestore có dữ liệu của ghế này không
                                if (firebaseSeatsMap.containsKey(currentSeatKey)) {
                                    seatList.add(firebaseSeatsMap.get(currentSeatKey));
                                } else {
                                    // Trường hợp Firebase thiếu document phòng hờ, tự sinh ghế trống mang tên đó để tránh rỗng màn hình
                                    seatList.add(new Seat(currentSeatKey, "available", 60000));
                                }
                            }
                        }
                    }

                    // Làm mới giao diện lưới ghế sau khi sắp xếp xong
                    seatAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi kết nối sơ đồ ghế: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        db.collection("showtimes")
                .document(showtimeId)
                .collection("seats")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // CHÈN DÒNG NÀY VÀO ĐỂ KIỂM TRA:
                    System.out.println("DEBUG_LOG: Đã lấy thành công " + queryDocumentSnapshots.size() + " ghế từ Firebase!");

                    seatList.clear();
                    // ... (giữ nguyên code cũ bên dưới) ...

                    seatAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // CHÈN DÒNG NÀY VÀO ĐỂ BẮT LỖI:
                    System.out.println("DEBUG_LOG: Thất bại khi kết nối Firebase! Lỗi: " + e.getMessage());
                    Toast.makeText(this, "Lỗi kết nối sơ đồ ghế: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ================= CẬP NHẬT TRẠNG THÁI ĐẶT GHẾ LÊN CLOUD =================
    private void updateSeatsToFirebase() {
        WriteBatch batch = db.batch();

        for (Seat seat : selectedSeatsList) {
            batch.update(db.collection("showtimes")
                            .document(showtimeId)
                            .collection("seats")
                            .document(seat.getSeatName()),
                    "status", "booked");
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đặt vé và chọn ghế thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi đồng bộ dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    // ================= ADAPTER CON HIỂN THỊ HÌNH ẢNH GHẾ =================
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
                case "booked": // Ghế đã có người khác đặt trước đó (Màu xám)
                    holder.tvSeat.setBackgroundResource(R.drawable.bg_seat_booked);
                    holder.tvSeat.setTextColor(Color.TRANSPARENT); // Ẩn tên chữ đi giống ảnh mẫu của bạn
                    break;

                case "selected": // Ghế người dùng đang kích chọn (Màu hồng sen chủ đạo)
                    holder.tvSeat.setBackgroundResource(R.drawable.bg_seat_selected);
                    holder.tvSeat.setTextColor(Color.WHITE);
                    break;

                case "available": // Ghế trống còn hạn mua (Màu tím nhạt viền khung)
                default:
                    holder.tvSeat.setBackgroundResource(R.drawable.bg_seat_available);
                    holder.tvSeat.setTextColor(Color.parseColor("#7A53D5"));
                    break;
            }

            // Xử lý sự kiện click tương tác
            holder.itemView.setOnClickListener(v -> {
                if (seat.getStatus().equals("booked")) return; // Khóa tương tác nếu ghế đã bán

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