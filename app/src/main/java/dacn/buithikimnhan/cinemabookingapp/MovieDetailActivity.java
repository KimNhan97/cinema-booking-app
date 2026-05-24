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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MovieDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvGenre, tvDuration, tvReleaseDate, tvDescription;
    private TextView tvDetailRelease, tvDetailDuration, tvDetailLanguage;
    private ImageView imgPoster, btnBack;
    private Button btnBookNow;

    // Khai báo thêm nút Yêu thích
    private MaterialButton btnFavorite;

    private LinearLayout layoutDateContainer, layoutEmptyShowtime;
    private RecyclerView rvTimeSlots;

    Movie currentMovie;
    List<String> distinctDates = new ArrayList<>();

    private String selectedDate = "";
    private Showtime selectedShowtime = null;
    private View lastSelectedDateView = null;

    // Biến toàn cục quản lý trạng thái Yêu thích (True: Đã thích, False: Chưa thích)
    private boolean isFavorite = false;
    private FirebaseFirestore db;
    private String currentUserId = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        db = FirebaseFirestore.getInstance();

        // Lấy UID tài khoản đang đăng nhập từ Firebase Auth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        initViews();
        setupClickListeners();

        currentMovie = (Movie) getIntent().getSerializableExtra("CHOSEN_MOVIE");

        if (currentMovie != null) {
            tvTitle.setText(currentMovie.getTitle());
            tvGenre.setText(currentMovie.getGenre());
            tvDuration.setText(currentMovie.getDuration() + " phút");
            tvReleaseDate.setText(currentMovie.getReleaseDate());
            tvDescription.setText(currentMovie.getDescription());

            tvDetailRelease.setText(currentMovie.getReleaseDate());
            tvDetailDuration.setText(currentMovie.getDuration() + " phút");
            tvDetailLanguage.setText("Phụ đề tiếng Việt");

            Glide.with(this)
                    .load(currentMovie.getBannerUrl())
                    .placeholder(R.drawable.movie1)
                    .error(R.drawable.movie1)
                    .into(imgPoster);

            generateCurrentDates();

            // THẦN CHÚ: Kiểm tra xem tài khoản này đã bấm thích bộ phim này trong DB chưa
            checkFavoriteStatus();
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

        btnFavorite = findViewById(R.id.btnFavorite);

        layoutDateContainer = findViewById(R.id.layoutDateContainer);
        layoutEmptyShowtime = findViewById(R.id.layoutEmptyShowtime);
        rvTimeSlots = findViewById(R.id.rvTimeSlots);

        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 2));
        rvTimeSlots.setHasFixedSize(true);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Xử lý sự kiện khi click vào nút Thích / Hủy Thích
        btnFavorite.setOnClickListener(v -> {
            if (currentUserId.isEmpty()) {
                Toast.makeText(this, "Vui lòng đăng nhập để sử dụng tính năng này!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Đảo trạng thái và thực thi tác vụ tương ứng
            if (isFavorite) {
                removeMovieFromFavorites();
            } else {
                addMovieToFavorites();
            }
        });

        btnBookNow.setOnClickListener(v -> {
            if (selectedShowtime == null) {
                Toast.makeText(MovieDetailActivity.this, "Lỗi: Bạn chưa chọn khung giờ hoặc dữ liệu suất chiếu từ Firebase chưa tải xong!", Toast.LENGTH_LONG).show();
                return;
            }

            Intent seatIntent = new Intent(MovieDetailActivity.this, SeatSelectionActivity.class);
            seatIntent.putExtra("SHOWTIME_ID", selectedShowtime.getShowtimeId());
            seatIntent.putExtra("MOVIE_TITLE", currentMovie.getTitle());

            String detailInfo = selectedShowtime.getStartTime() + "~" + selectedShowtime.getEndTime()
                    + " | " + selectedShowtime.getDate() + " | 2D Phụ đề";
            seatIntent.putExtra("SHOWTIME_INFO", detailInfo);

            startActivity(seatIntent);
        });
    }

    // ================= LOGIC XỬ LÝ CHỨC NĂNG YÊU THÍCH PHIM =================

    private void checkFavoriteStatus() {
        if (currentUserId.isEmpty() || currentMovie == null) return;

        // Tạo chuỗi ID kết hợp duy nhất theo công thức: userId_movieId
        String favDocId = currentUserId + "_" + currentMovie.getMovieId();

        db.collection("favorites")
                .document(favDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Nếu tài liệu tồn tại -> Người dùng đã thích phim này
                        isFavorite = true;
                        setFavoriteButtonUI(true);
                    } else {
                        isFavorite = false;
                        setFavoriteButtonUI(false);
                    }
                });
    }

    private void addMovieToFavorites() {
        String favDocId = currentUserId + "_" + currentMovie.getMovieId();

        // Đóng gói thông tin phim để đẩy lên Firebase
        Map<String, Object> favData = new HashMap<>();
        favData.put("userId", currentUserId);
        favData.put("movieId", currentMovie.getMovieId());
        favData.put("movieTitle", currentMovie.getTitle());
        favData.put("bannerUrl", currentMovie.getBannerUrl());
        favData.put("posterUrl", currentMovie.getPosterUrl());
        favData.put("genre", currentMovie.getGenre());

        db.collection("favorites")
                .document(favDocId)
                .set(favData)
                .addOnSuccessListener(aVoid -> {
                    isFavorite = true;
                    setFavoriteButtonUI(true);
                    Toast.makeText(MovieDetailActivity.this, "Đã thêm vào danh sách yêu thích!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MovieDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void removeMovieFromFavorites() {
        String favDocId = currentUserId + "_" + currentMovie.getMovieId();

        db.collection("favorites")
                .document(favDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    isFavorite = false;
                    setFavoriteButtonUI(false);
                    Toast.makeText(MovieDetailActivity.this, "Đã xóa khỏi danh sách yêu thích.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MovieDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Hàm thay đổi giao diện nút Thích dựa theo trạng thái logic
    private void setFavoriteButtonUI(boolean favoriteStatus) {
        if (favoriteStatus) {
            // Trạng thái ĐÃ THÍCH: Nền hồng, Chữ đỏ đậm, Icon trái tim đầy đặn (Nếu có hệ thống icon tương ứng)
            btnFavorite.setText("Đã thích");
            btnFavorite.setTextColor(Color.parseColor("#D81B60"));
            btnFavorite.setIconTintResource(R.color.google_red); // Hoặc màu hồng của bạn
            btnFavorite.setStrokeColorResource(R.color.google_red);
            // Bạn có thể đổi icon sang hình tim đỏ đầy nếu có drawable: btnFavorite.setIconResource(R.drawable.ic_heart_filled);
        } else {
            // Trạng thái CHƯA THÍCH: Viền xám, Chữ xám mặc định ban đầu
            btnFavorite.setText("Thích");
            btnFavorite.setTextColor(Color.parseColor("#444444"));
            btnFavorite.setIconTintResource(android.R.color.darker_gray);
            btnFavorite.setStrokeColorResource(android.R.color.darker_gray);
            // Hoàn tác icon viền: btnFavorite.setIconResource(R.drawable.ic_heart_outline);
        }
    }

    // ================= XỬ LÝ SỰ KIỆN TỰ SINH LỊCH CHIẾU ĐỘNG =================

    private void generateCurrentDates() {
        distinctDates.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            distinctDates.add(sdf.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        renderDateTabs(distinctDates);
    }

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

            dateView.setOnClickListener(v -> {
                if (lastSelectedDateView != null) {
                    lastSelectedDateView.setBackgroundResource(R.drawable.bg_date_unselected);
                    TextView oldDay = lastSelectedDateView.findViewById(R.id.tvDayLabel);
                    TextView oldDate = lastSelectedDateView.findViewById(R.id.tvDateLabel);
                    if (oldDay != null) oldDay.setTextColor(Color.parseColor("#333333"));
                    if (oldDate != null) oldDate.setTextColor(Color.parseColor("#666666"));
                }

                v.setBackgroundResource(R.drawable.bg_date_selected);
                TextView currentDay = v.findViewById(R.id.tvDayLabel);
                TextView currentDate = v.findViewById(R.id.tvDateLabel);
                if (currentDay != null) currentDay.setTextColor(Color.WHITE);
                if (currentDate != null) currentDate.setTextColor(Color.WHITE);

                lastSelectedDateView = v;
                selectedDate = (String) v.getTag();

                filterTimeSlotsByDate(selectedDate);
            });

            layoutDateContainer.addView(dateView);
        }

        if (layoutDateContainer.getChildCount() > 0) {
            layoutDateContainer.getChildAt(0).performClick();
        }
    }

    private void filterTimeSlotsByDate(String dateStr) {
        List<Showtime> filteredList = new ArrayList<>();
        selectedShowtime = null;

        if (currentMovie == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("showtimes")
                .whereEqualTo("movieId", currentMovie.getMovieId())
                .whereEqualTo("date", dateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    filteredList.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Showtime showtime = doc.toObject(Showtime.class);
                        if (showtime != null) {
                            showtime.setShowtimeId(doc.getId());
                            filteredList.add(showtime);
                        }
                    }

                    TimeSlotAdapter timeSlotAdapter = new TimeSlotAdapter(filteredList);
                    rvTimeSlots.setAdapter(timeSlotAdapter);

                    // LOGIC THAY ĐỔI GIAO DIỆN ĐỘNG Ở ĐÂY:
                    if (filteredList.isEmpty()) {
                        // Nếu không có suất chiếu: Ẩn lưới chọn giờ, hiện màn hình "Tiếc quá!"
                        rvTimeSlots.setVisibility(View.GONE);
                        layoutEmptyShowtime.setVisibility(View.VISIBLE);
                    } else {
                        // Nếu có suất chiếu: Hiện lưới chọn giờ như bình thường, ẩn thông báo trống
                        rvTimeSlots.setVisibility(View.VISIBLE);
                        layoutEmptyShowtime.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MovieDetailActivity.this, "Lỗi tải lịch chiếu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeViewHolder> {

         List<Showtime> showtimeList;
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

            if (selectedPosition == position) {
                holder.tvTimeRange.setBackgroundResource(R.drawable.bg_time_slot_selected);
                holder.tvTimeRange.setTextColor(Color.BLACK);
                holder.tvTimeRange.setTypeface(null, Typeface.BOLD);
            } else {
                holder.tvTimeRange.setBackgroundResource(R.drawable.bg_time_slot);
                holder.tvTimeRange.setTextColor(Color.parseColor("#333333"));
                holder.tvTimeRange.setTypeface(null, Typeface.NORMAL);
            }

            holder.itemView.setOnClickListener(v -> {
                int previousSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                selectedShowtime = showtime;

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