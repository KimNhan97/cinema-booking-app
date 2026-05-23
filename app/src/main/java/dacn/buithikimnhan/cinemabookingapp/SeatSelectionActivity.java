package dacn.buithikimnhan.cinemabookingapp;

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

    // Thông tin nhận từ Intent (Gán mặc định phòng hờ)
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
    // ================= HIỂN THỊ HỘP THOẠI XÁC NHẬN ĐỘ TUỔI =================
    private void showAgeConfirmationDialog() {
        // Tạo trình dựng hộp thoại giao diện bo góc tiêu chuẩn hệ thống
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);

        // Thiết lập tiêu đề và nội dung văn bản giống ảnh mẫu 100%
        builder.setTitle("Xác nhận");
        builder.setMessage("Phim dành cho mọi độ tuổi. Tôi xác nhận rạp phim không được phép phục vụ khách hàng dưới 13 tuổi cho các suất chiếu kết thúc từ 22:00 và dưới 16 tuổi cho các suất chiếu kết thúc từ 23:00. Tôi đồng ý cung cấp giấy tờ tùy thân để xác thực độ tuổi người xem. Rạp sẽ không hoàn tiền nếu người xem không đáp ứng đủ điều kiện.");

        // Cấu hình nút hành động "Xác nhận" (Bấm vào mới tiến hành lưu lên Firebase)
        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            // Gọi hàm đẩy dữ liệu cập nhật trạng thái ghế lên Cloud Firestore
            navigateToPaymentInfo();
        });

        // Cấu hình nút hành động "Hủy" (Bấm vào chỉ đóng hộp thoại, giữ nguyên màn hình chọn ghế)
        builder.setNegativeButton("Hủy", (dialog, which) -> {
            dialog.dismiss();
        });

        // Khởi tạo thực thể và tùy biến màu sắc chữ của các nút bấm cho đồng bộ giao diện
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Thay đổi màu sắc của nút để làm nổi bật hành động (Màu hồng/đỏ chủ đạo)
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#D81B60"));
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"));
    }
    private void setupRecyclerView() {
        seatAdapter = new SeatAdapter(seatList);
        rvSeatMap.setLayoutManager(new GridLayoutManager(this, 9)); // Luôn cố định lưới 9 cột
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

    // ================= THUẬT TOÁN ĐỔ SƠ ĐỒ GHẾ CHUẨN TỪ A -> J =================

    private void loadSeatsFromFirestore() {
        db.collection("showtimes")
                .document(showtimeId)
                .collection("seats")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    System.out.println("DEBUG_LOG: Kết nối thành công! Lấy được " + queryDocumentSnapshots.size() + " ghế từ document: " + showtimeId);

                    seatList.clear();

                    // Dùng HashMap để gom ghế từ Firebase theo từ khóa Tên Ghế (VD: "A1" -> Đối tượng Seat)
                    Map<String, Seat> firebaseSeatsMap = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Seat seat = doc.toObject(Seat.class);
                            if (seat != null) {
                                seat.setSeatName(doc.getId()); // ID Document trên Firestore là "A1", "A2"...
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
                    System.out.println("DEBUG_LOG: Thất bại khi kết nối Firebase! Lỗi: " + e.getMessage());
                    Toast.makeText(this, "Lỗi kết nối sơ đồ ghế: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ================= CẬP NHẬT TRẠNG THÁI ĐẶT GHẾ LÊN CLOUD =================
    // Đổi tên hàm thành chuyển trang để đúng với bản chất logic mới
    private void navigateToPaymentInfo() {
        // Gom tên các ghế đã chọn thành chuỗi dạng: "J06, J05"
        StringBuilder seatsBuilder = new StringBuilder();
        for (int i = 0; i < selectedSeatsList.size(); i++) {
            seatsBuilder.append(selectedSeatsList.get(i).getSeatName());
            if (i < selectedSeatsList.size() - 1) {
                seatsBuilder.append(", ");
            }
        }

        // Tạo chuỗi danh sách để truyền danh sách đối tượng sang nếu cần xử lý vòng lặp lẻ sau này
        Intent intent = new Intent(SeatSelectionActivity.this, PaymentInfoActivity.class);
        intent.putExtra("SHOWTIME_ID", showtimeId); // Rất quan trọng để trang sau biết lưu vào đâu
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
                    holder.tvSeat.setTextColor(Color.TRANSPARENT); // Ẩn chữ đi cho đẹp
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

            // Xử lý sự kiện click tương tác chọn/hủy chọn ghế
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