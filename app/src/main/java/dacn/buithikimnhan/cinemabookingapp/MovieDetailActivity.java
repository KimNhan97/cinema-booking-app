package dacn.buithikimnhan.cinemabookingapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MovieDetailActivity extends AppCompatActivity {

    // Ánh xạ các thành phần UI theo đúng file XML
    private TextView tvTitle, tvGenre, tvDuration, tvReleaseDate, tvDescription;
    private TextView tvDetailRelease, tvDetailDuration, tvDetailLanguage;
    private ImageView imgPoster, btnBack;
    private Button btnBookNow;

    // Danh sách động thay thế cho dữ liệu tĩnh trong XML cũ
    private LinearLayout layoutDateContainer;
    private RecyclerView rvTimeSlots;

    Movie currentMovie;

    // Quản lý danh sách ngày chiếu tự sinh
    List<String> distinctDates = new ArrayList<>();

    private String selectedDate = "";
    private Showtime selectedShowtime = null;
    private View lastSelectedDateView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        initViews();
        setupClickListeners();

        // 1. Nhận Object Movie được truyền từ màn hình danh sách (HomeMainAdapter)
        currentMovie = (Movie) getIntent().getSerializableExtra("CHOSEN_MOVIE");

        if (currentMovie != null) {
            // 2. Gán dữ liệu cơ bản của phim lên giao diện
            tvTitle.setText(currentMovie.getTitle());
            tvGenre.setText(currentMovie.getGenre());
            tvDuration.setText(currentMovie.getDuration() + " phút");
            tvReleaseDate.setText(currentMovie.getReleaseDate());
            tvDescription.setText(currentMovie.getDescription());

            // Gán dữ liệu cho 3 cột thông số giữa màn hình
            tvDetailRelease.setText(currentMovie.getReleaseDate());
            tvDetailDuration.setText(currentMovie.getDuration() + " phút");
            tvDetailLanguage.setText("Phụ đề tiếng Việt");

            // Tải ảnh poster mượt mà qua Glide
            Glide.with(this)
                    .load(currentMovie.getBannerUrl())
                    .placeholder(R.drawable.movie1)
                    .error(R.drawable.movie1)
                    .into(imgPoster);

            // 3. Tự động tính toán và sinh lịch chiếu động (Không dùng Firestore cho showtimes nữa)
            generateCurrentDates();
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin phim!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        imgPoster = findViewById(R.id.imgMoviePoster);
        tvTitle = findViewById(R.id.tvTitle);
        tvGenre = findViewById(R.id.tvGenre);
        tvDuration = findViewById(R.id.tvDuration);
        tvReleaseDate = findViewById(R.id.tvReleaseDate);
        tvDescription = findViewById(R.id.tvDescription);

        tvDetailRelease = findViewById(R.id.tvDetailRelease);
        tvDetailDuration = findViewById(R.id.tvDetailDuration);
        tvDetailLanguage = findViewById(R.id.tvDetailLanguage);

        btnBack = findViewById(R.id.btnBack);
        btnBookNow = findViewById(R.id.btnBookNow);

        layoutDateContainer = findViewById(R.id.layoutDateContainer);
        rvTimeSlots = findViewById(R.id.rvTimeSlots);

        // Cấu hình hiển thị Khung giờ chiếu dạng lưới 2 cột
        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 2));
        rvTimeSlots.setHasFixedSize(true);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnBookNow.setOnClickListener(v -> {
            if (selectedShowtime == null) {
                // Thay đổi thông báo chi tiết hơn để gỡ lỗi (Debug)
                Toast.makeText(MovieDetailActivity.this, "Lỗi: Bạn chưa chọn khung giờ hoặc dữ liệu suất chiếu từ Firebase chưa tải xong!", Toast.LENGTH_LONG).show();
                return;
            }

            // Nếu vượt qua điều kiện null, lệnh này bắt buộc phải chạy
            Intent seatIntent = new Intent(MovieDetailActivity.this, SeatSelectionActivity.class);
            seatIntent.putExtra("SHOWTIME_ID", selectedShowtime.getShowtimeId());
            seatIntent.putExtra("MOVIE_TITLE", currentMovie.getTitle());

            String detailInfo = selectedShowtime.getStartTime() + "~" + selectedShowtime.getEndTime()
                    + " | " + selectedShowtime.getDate() + " | 2D Phụ đề";
            seatIntent.putExtra("SHOWTIME_INFO", detailInfo);

            startActivity(seatIntent);
        });
    }

    // ================= XỬ LÝ SỰ KIỆN TỰ SINH LỊCH CHIẾU ĐỘNG =================

    // Tự sinh danh sách 7 ngày liên tiếp tính từ thời gian thực lúc mở app
    private void generateCurrentDates() {
        distinctDates.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        // Vòng lặp sinh ra 7 ngày kế tiếp
        for (int i = 0; i < 7; i++) {
            distinctDates.add(sdf.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Đổ danh sách ngày vừa sinh lên thanh cuộn ngang
        renderDateTabs(distinctDates);
    }

    // Sinh các ô chọn Thứ/Ngày động trực tiếp vào LinearLayout cuộn ngang
    private void renderDateTabs(List<String> dates) {
        layoutDateContainer.removeAllViews();
        lastSelectedDateView = null;
        selectedDate = "";

        if (dates.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Hiện tại phim chưa có lịch chiếu cụ thể.");
            tvEmpty.setTextColor(Color.GRAY);
            tvEmpty.setPadding(32, 16, 32, 16);
            layoutDateContainer.addView(tvEmpty);
            return;
        }

        for (String rawDate : dates) {
            View dateView = LayoutInflater.from(this).inflate(R.layout.item_date_tab, layoutDateContainer, false);
            TextView tvDayLabel = dateView.findViewById(R.id.tvDayLabel);
            TextView tvDateLabel = dateView.findViewById(R.id.tvDateLabel);

            try {
                SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdfInput.parse(rawDate);

                SimpleDateFormat sdfDay = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));
                SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM", Locale.getDefault());

                String dayOfWeek = sdfDay.format(date);
                if (dayOfWeek.equalsIgnoreCase("Chủ Nhật")) dayOfWeek = "C.Nhật";

                // Kiểm tra xem có trùng ngày hôm nay thực tế không
                SimpleDateFormat sdfTodayCheck = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                if (sdfTodayCheck.format(new Date()).equals(rawDate)) {
                    dayOfWeek = "H.nay";
                }

                tvDayLabel.setText(dayOfWeek);
                tvDateLabel.setText(sdfDate.format(date));

            } catch (ParseException e) {
                tvDayLabel.setText("Thứ");
                tvDateLabel.setText(rawDate);
            }

            dateView.setTag(rawDate);

            // Xử lý sự kiện click đổi ngày
            dateView.setOnClickListener(v -> {
                // Khôi phục background cũ cho ô vừa chọn trước đó
                if (lastSelectedDateView != null) {
                    lastSelectedDateView.setBackgroundResource(R.drawable.bg_date_unselected);
                    TextView oldDay = lastSelectedDateView.findViewById(R.id.tvDayLabel);
                    TextView oldDate = lastSelectedDateView.findViewById(R.id.tvDateLabel);
                    if (oldDay != null) oldDay.setTextColor(Color.parseColor("#333333"));
                    if (oldDate != null) oldDate.setTextColor(Color.parseColor("#666666"));
                }

                // Cập nhật background nổi bật cho ô đang bấm
                v.setBackgroundResource(R.drawable.bg_date_selected);
                TextView currentDay = v.findViewById(R.id.tvDayLabel);
                TextView currentDate = v.findViewById(R.id.tvDateLabel);
                if (currentDay != null) currentDay.setTextColor(Color.WHITE);
                if (currentDate != null) currentDate.setTextColor(Color.WHITE);

                lastSelectedDateView = v;
                selectedDate = (String) v.getTag();

                // Lọc và sinh danh sách khung giờ chiếu tương ứng theo ngày vừa chọn
                filterTimeSlotsByDate(selectedDate);
            });

            layoutDateContainer.addView(dateView);
        }

        // Thực hiện click tự động vào ô đầu tiên (Ngày hôm nay) sau khi đã add vào Layout
        if (layoutDateContainer.getChildCount() > 0) {
            layoutDateContainer.getChildAt(0).performClick();
        }
    }

    // Hàm tự động chia khung giờ theo thời lượng riêng của từng phim
    private void filterTimeSlotsByDate(String dateStr) {
        List<Showtime> filteredList = new ArrayList<>();
        selectedShowtime = null;

        if (currentMovie == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Truy vấn Firestore: Tìm trong bảng showtimes các suất chiếu thỏa mãn cả 2 điều kiện
        db.collection("showtimes")
                .whereEqualTo("movieId", currentMovie.getMovieId()) // Đúng phim này
                .whereEqualTo("date", dateStr)                       // Đúng ngày này
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    filteredList.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Showtime showtime = doc.toObject(Showtime.class);
                        if (showtime != null) {
                            // Lấy ID document làm ShowtimeId (VD: "show_001")
                            showtime.setShowtimeId(doc.getId());
                            filteredList.add(showtime);
                        }
                    }

                    // Đổ dữ liệu thật từ Firebase vào Adapter để hiển thị lên màn hình
                    TimeSlotAdapter timeSlotAdapter = new TimeSlotAdapter(filteredList);
                    rvTimeSlots.setAdapter(timeSlotAdapter);

                    if (filteredList.isEmpty()) {
                        Toast.makeText(MovieDetailActivity.this, "Ngày này hiện tại chưa có suất chiếu!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MovieDetailActivity.this, "Lỗi tải lịch chiếu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    // ================= ADAPTER CON HIỂN THỊ KHUNG GIỜ CHIẾU ĐỘNG =================

    private class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeViewHolder> {

        private List<Showtime> showtimeList;
        private int selectedPosition = -1;

        public TimeSlotAdapter(List<Showtime> showtimeList) {
            this.showtimeList = showtimeList;
        }

        @NonNull
        @Override
        public TimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
            return new TimeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TimeViewHolder holder, int position) {
            Showtime showtime = showtimeList.get(position);
            holder.tvTimeRange.setText(String.format("%s - %s", showtime.getStartTime(), showtime.getEndTime()));

            // Kiểm tra trạng thái vị trí để hoán đổi màu nền và màu chữ
            if (selectedPosition == position) {
                // Khi được Click: Đổi sang background hồng cánh sen và chữ trắng nổi bật giống nút Mua Vé
                holder.tvTimeRange.setBackgroundResource(R.drawable.bg_time_slot_selected);
                holder.tvTimeRange.setTextColor(Color.BLACK);
                holder.tvTimeRange.setTypeface(null, Typeface.BOLD);
            } else {
                // Trạng thái mặc định khi chưa chọn: Viền xám, nền trắng, chữ đen
                holder.tvTimeRange.setBackgroundResource(R.drawable.bg_time_slot);
                holder.tvTimeRange.setTextColor(Color.parseColor("#333333"));
                holder.tvTimeRange.setTypeface(null, Typeface.NORMAL);
            }

            holder.itemView.setOnClickListener(v -> {
                int previousSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                // Lưu lại đối tượng lịch chiếu toàn cục để chuyển màn hình chọn ghế
                selectedShowtime = showtime;

                // Cập nhật lại giao diện cho ô vừa bỏ chọn và ô mới được chọn
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
            });
        }

        @Override
        public int getItemCount() {
            return showtimeList != null ? showtimeList.size() : 0;
        }

        class TimeViewHolder extends RecyclerView.ViewHolder {
            TextView tvTimeRange;

            public TimeViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTimeRange = itemView.findViewById(R.id.tvTimeRange);
            }
        }
    }
}