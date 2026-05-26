package dacn.buithikimnhan.cinemabookingapp.user.movie;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.user.booking.SeatSelectionActivity;
import dacn.buithikimnhan.cinemabookingapp.data.Movie;
import dacn.buithikimnhan.cinemabookingapp.data.Review;
import dacn.buithikimnhan.cinemabookingapp.data.Showtime;

public class MovieDetailActivity extends AppCompatActivity {

    private static final String TAG = "MovieDetailActivity";

    private TextView tvTitle, tvGenre, tvDuration, tvReleaseDate, tvDescription;
    private TextView tvDetailRelease, tvDetailDuration, tvDetailLanguage;
    private ImageView imgPoster, btnBack;
    private Button btnBookNow;

    private MaterialButton btnFavorite;
    private LinearLayout layoutDateContainer, layoutEmptyShowtime;
    private RecyclerView rvTimeSlots;

    private MaterialButton btnWriteReview;
    private TextView tvAverageRating, tvTotalRatingCount;
    private RatingBar movieRatingBarIndicator;
    private RecyclerView rvReviews;
    private LinearLayout layoutEmptyReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList = new ArrayList<>();

    Movie currentMovie;
    List<String> distinctDates = new ArrayList<>();

    private String selectedDate = "";
    private Showtime selectedShowtime = null;
    private View lastSelectedDateView = null;

    private boolean isFavorite = false;
    private FirebaseFirestore db;
    private String currentUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        initViews();
        setupClickListeners();

        // Kiểm tra đa phương thức nhận Intent (Object từ danh sách dưới hoặc ID từ Banner)
        currentMovie = (Movie) getIntent().getSerializableExtra("CHOSEN_MOVIE");
        String movieIdFromIntent = getIntent().getStringExtra("movieId");

        if (currentMovie != null) {
            displayMovieData();
        } else if (movieIdFromIntent != null && !movieIdFromIntent.isEmpty()) {
            fetchMovieDetailFromFirestore(movieIdFromIntent);
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

        btnWriteReview = findViewById(R.id.btnWriteReview);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalRatingCount = findViewById(R.id.tvTotalRatingCount);
        movieRatingBarIndicator = findViewById(R.id.movieRatingBarIndicator);
        layoutEmptyReviews = findViewById(R.id.layoutEmptyReviews);

        rvReviews = findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setHasFixedSize(true);
        reviewAdapter = new ReviewAdapter(reviewList);
        rvReviews.setAdapter(reviewAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnWriteReview.setOnClickListener(v -> {
            if (currentMovie != null) {
                showRatingDialog(currentMovie.getMovieId());
            }
        });

        btnFavorite.setOnClickListener(v -> {
            if (currentUserId.isEmpty()) {
                Toast.makeText(this, "Vui lòng đăng nhập để sử dụng tính năng này!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isFavorite) {
                removeMovieFromFavorites();
            } else {
                addMovieToFavorites();
            }
        });

        btnBookNow.setOnClickListener(v -> {
            if (selectedShowtime == null) {
                Toast.makeText(MovieDetailActivity.this, "Lỗi: Bạn chưa chọn khung giờ hoặc dữ liệu suất chiếu chưa sẵn sàng!", Toast.LENGTH_LONG).show();
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

    private void fetchMovieDetailFromFirestore(String movieId) {
        db.collection("movies").document(movieId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentMovie = documentSnapshot.toObject(Movie.class);
                        if (currentMovie != null) {
                            currentMovie.setMovieId(documentSnapshot.getId());
                            displayMovieData();
                        }
                    } else {
                        Toast.makeText(this, "Dữ liệu phim không tồn tại trên hệ thống!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayMovieData() {
        if (currentMovie == null) return;

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
        checkFavoriteStatus();
        loadMovieReviews(currentMovie.getMovieId());
    }

    // ================= LOGIC XỬ LÝ CHỨC NĂNG YÊU THÍCH PHIM =================

    private void checkFavoriteStatus() {
        if (currentUserId.isEmpty() || currentMovie == null) return;

        String favDocId = currentUserId + "_" + currentMovie.getMovieId();

        db.collection("favorites")
                .document(favDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isFavorite = documentSnapshot.exists();
                    setFavoriteButtonUI(isFavorite);
                });
    }

    private void addMovieToFavorites() {
        if (currentMovie == null) return;
        String favDocId = currentUserId + "_" + currentMovie.getMovieId();

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
                .addOnFailureListener(e -> Toast.makeText(MovieDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeMovieFromFavorites() {
        if (currentMovie == null) return;
        String favDocId = currentUserId + "_" + currentMovie.getMovieId();

        db.collection("favorites")
                .document(favDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    isFavorite = false;
                    setFavoriteButtonUI(false);
                    Toast.makeText(MovieDetailActivity.this, "Đã xóa khỏi danh sách yêu thích.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(MovieDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setFavoriteButtonUI(boolean favoriteStatus) {
        if (favoriteStatus) {
            btnFavorite.setText("Đã thích");
            btnFavorite.setTextColor(Color.parseColor("#D81B60"));
            btnFavorite.setIconTintResource(R.color.google_red);
            btnFavorite.setStrokeColorResource(R.color.google_red);
        } else {
            btnFavorite.setText("Thích");
            btnFavorite.setTextColor(Color.parseColor("#444444"));
            btnFavorite.setIconTintResource(android.R.color.darker_gray);
            btnFavorite.setStrokeColorResource(android.R.color.darker_gray);
        }
    }

    // ================= XỬ LÝ SỰ KIỆN LỊCH CHIẾU ĐỘNG =================

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

        db.collection("showtimes")
                .whereEqualTo("movieId", currentMovie.getMovieId())
                .whereEqualTo("date", dateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    filteredList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Showtime showtime = doc.toObject(Showtime.class);
                        if (showtime != null) {
                            showtime.setShowtimeId(doc.getId());
                            filteredList.add(showtime);
                        }
                    }

                    TimeSlotAdapter timeSlotAdapter = new TimeSlotAdapter(filteredList);
                    rvTimeSlots.setAdapter(timeSlotAdapter);

                    if (filteredList.isEmpty()) {
                        rvTimeSlots.setVisibility(View.GONE);
                        layoutEmptyShowtime.setVisibility(View.VISIBLE);
                    } else {
                        rvTimeSlots.setVisibility(View.VISIBLE);
                        layoutEmptyShowtime.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MovieDetailActivity.this, "Lỗi tải lịch chiếu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    // ================= XỬ LÝ CHỨC NĂNG ĐÁNH GIÁ (REVIEW & RATING) =================

    private void loadMovieReviews(String movieId) {
        db.collection("reviews")
                .whereEqualTo("movieId", movieId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe reviews thực tế: ", error);
                        return;
                    }

                    if (value != null) {
                        reviewList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Review review = doc.toObject(Review.class);
                            if (review != null) {
                                reviewList.add(review);
                            }
                        }
                        reviewAdapter.notifyDataSetChanged();

                        if (reviewList.isEmpty()) {
                            rvReviews.setVisibility(View.GONE);
                            layoutEmptyReviews.setVisibility(View.VISIBLE);
                        } else {
                            rvReviews.setVisibility(View.VISIBLE);
                            layoutEmptyReviews.setVisibility(View.GONE);
                        }
                    }
                });

        db.collection("movies").document(movieId)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        double totalRating = 0.0;
                        long ratingCount = 0;

                        if (snapshot.getDouble("totalRating") != null) {
                            totalRating = snapshot.getDouble("totalRating");
                        }
                        if (snapshot.getLong("ratingCount") != null) {
                            ratingCount = snapshot.getLong("ratingCount");
                        }

                        if (ratingCount > 0) {
                            // CHUẨN HỆ 5: Lấy tổng số sao chia thẳng cho tổng số lượt review
                            double avgSystem5Stars = totalRating / ratingCount;

                            // Làm tròn toán học lấy 1 chữ số thập phân gọn gàng
                            double roundedAvg = Math.round(avgSystem5Stars * 10.0) / 10.0;

                            tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", roundedAvg));
                            movieRatingBarIndicator.setRating((float) avgSystem5Stars);
                            tvTotalRatingCount.setText("Dựa trên " + ratingCount + " lượt nhận xét");
                        } else {
                            tvAverageRating.setText("0.0");
                            movieRatingBarIndicator.setRating(0.0f);
                            tvTotalRatingCount.setText("Chưa có lượt chấm điểm");
                        }
                    }
                });
    }

    private void showRatingDialog(String movieId) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        builder.setView(view);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        RatingBar ratingBar = view.findViewById(R.id.dialogRatingBar);
        EditText edtComment = view.findViewById(R.id.edtComment);
        Button btnCancel = view.findViewById(R.id.btnCancelReview);
        Button btnSubmit = view.findViewById(R.id.btnSubmitReview);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            double ratingStars = ratingBar.getRating();
            String comment = edtComment.getText().toString().trim();

            if (currentUserId.isEmpty()) {
                Toast.makeText(this, "Bạn cần đăng nhập để đánh giá!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (comment.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung nhận xét!", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmit.setEnabled(false);

            db.collection("users").document(currentUserId).get()
                    .addOnCompleteListener(task -> {
                        String userName = "Khán giả";
                        String userAvatar = "";

                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            userName = documentSnapshot.getString("fullName");
                            userAvatar = documentSnapshot.getString("avatar");
                        } else {
                            if (currentUserId.length() > 4) {
                                userName += " (" + currentUserId.substring(currentUserId.length() - 4) + ")";
                            }
                        }

                        String reviewId = db.collection("reviews").document().getId();
                        Review review = new Review(reviewId, movieId, currentUserId, userName, userAvatar, ratingStars, comment, System.currentTimeMillis());

                        db.collection("reviews").document(reviewId).set(review)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(MovieDetailActivity.this, "Cảm ơn bạn đã đánh giá phim!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    updateMovieRatingStats(movieId, ratingStars);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(MovieDetailActivity.this, "Lỗi lưu đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    btnSubmit.setEnabled(true);
                                });
                    });
        });
    }

    private void updateMovieRatingStats(String movieId, double newRatingStars) {
        DocumentReference movieRef = db.collection("movies").document(movieId);

        db.runTransaction(transaction -> {
            DocumentSnapshot movieSnapshot = transaction.get(movieRef);

            double totalRating = 0.0;
            long ratingCount = 0;

            if (movieSnapshot.exists()) {
                if (movieSnapshot.getDouble("totalRating") != null) {
                    totalRating = movieSnapshot.getDouble("totalRating");
                }
                if (movieSnapshot.getLong("ratingCount") != null) {
                    ratingCount = movieSnapshot.getLong("ratingCount");
                }
            }

            totalRating += newRatingStars;
            ratingCount += 1;

            transaction.update(movieRef, "totalRating", totalRating);
            transaction.update(movieRef, "ratingCount", ratingCount);

            return null;
        }).addOnFailureListener(e -> Log.e(TAG, "Lỗi cập nhật Transaction điểm phim: ", e));
    }

    // ================= CLASS ADAPTER CUSTOM ĐỔ DỮ LIỆU ĐÁNH GIÁ =================
    private class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
        private List<Review> list;

        public ReviewAdapter(List<Review> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
            return new ReviewViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
            Review r = list.get(position);

            if (r.getUserName() != null && !r.getUserName().isEmpty()) {
                holder.tvReviewUserName.setText(r.getUserName());
            } else {
                holder.tvReviewUserName.setText("Khán giả ẩn danh");
            }

            holder.tvReviewComment.setText(r.getComment());
            holder.reviewRatingBar.setRating((float) r.getRating());

            if (r.getTimestamp() > 0) {
                long timeInMillis = r.getTimestamp();

                if (timeInMillis < 100000000000L) {
                    timeInMillis = timeInMillis * 1000;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                holder.tvReviewTime.setText(sdf.format(new java.util.Date(timeInMillis)));
            } else {
                holder.tvReviewTime.setText("Vừa xong");
            }

            if (r.getUserAvatar() != null && !r.getUserAvatar().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(r.getUserAvatar())
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .error(android.R.drawable.sym_def_app_icon)
                        .into(holder.imgUserAvatar);
            } else {
                holder.imgUserAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        class ReviewViewHolder extends RecyclerView.ViewHolder {
            ImageView imgUserAvatar;
            TextView tvReviewUserName, tvReviewTime, tvReviewComment;
            RatingBar reviewRatingBar;

            public ReviewViewHolder(@NonNull View itemView) {
                super(itemView);
                imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
                tvReviewUserName = itemView.findViewById(R.id.tvReviewUserName);
                tvReviewTime = itemView.findViewById(R.id.tvReviewTime);
                tvReviewComment = itemView.findViewById(R.id.tvReviewComment);
                reviewRatingBar = itemView.findViewById(R.id.reviewRatingBar);
            }
        }
    }
}