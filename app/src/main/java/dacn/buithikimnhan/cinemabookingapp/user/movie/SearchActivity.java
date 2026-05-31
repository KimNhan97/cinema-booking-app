package dacn.buithikimnhan.cinemabookingapp.user.movie;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Movie;
import dacn.buithikimnhan.cinemabookingapp.data.Review;
import dacn.buithikimnhan.cinemabookingapp.user.MainActivity;

public class SearchActivity extends AppCompatActivity {

    private EditText edtSearch;
    private RecyclerView rvSearchMovies;
    private CardView layoutEmptyState;
    private TextView tvCancel;

    private MovieAdapter adapter;
    private final List<Movie> movieList = new ArrayList<>();
    private final List<Movie> filteredList = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Ánh xạ các View
        edtSearch = findViewById(R.id.edtSearch);
        rvSearchMovies = findViewById(R.id.rvSearchMovies);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvCancel = findViewById(R.id.tvCancel);

        db = FirebaseFirestore.getInstance();

        // Khởi tạo Adapter và thiết lập sự kiện Click chuyển đến trang chi tiết phim
        adapter = new MovieAdapter(this, filteredList);

        rvSearchMovies.setLayoutManager(new GridLayoutManager(this, 2));
        rvSearchMovies.setAdapter(adapter);

        // 1. Tải trước dữ liệu phim kèm thống kê sao đánh giá chuẩn xác
        loadMoviesAndRatings();

        // 2. Khởi tạo trạng thái giao diện ban đầu (Trống hoàn toàn)
        updateUiState("");

        // 3. Sự kiện xử lý nút Hủy - Quay lại màn hình Trang Chủ
        tvCancel.setOnClickListener(v -> {
            Intent intent = new Intent(SearchActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // 4. Theo dõi thanh tìm kiếm thời gian thực
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().trim();
                updateUiState(keyword);
                filterMovies(keyword);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadMoviesAndRatings() {
        // Đọc bảng reviews để tính toán lại điểm số sao chính xác nhất từ dữ liệu thô Firestore
        db.collection("reviews").get().addOnSuccessListener(reviewSnapshots -> {
            Map<String, double[]> reviewStatsMap = new HashMap<>();

            if (reviewSnapshots != null) {
                for (QueryDocumentSnapshot doc : reviewSnapshots) {
                    try {
                        // ĐỒNG BỘ AN TOÀN: Lấy trực tiếp chuỗi từ Document phòng trường hợp ép kiểu Class bị lỗi
                        String mId = doc.getString("movieId");

                        // Nếu trường viết thường bị null, thử kiểm tra trường viết hoa (đề phòng lệch tên thuộc tính)
                        if (mId == null) mId = doc.getString("movieID");

                        Double ratingValueObj = doc.getDouble("rating");
                        double ratingValue = (ratingValueObj != null) ? ratingValueObj : 0.0;

                        if (mId != null && !mId.trim().isEmpty()) {
                            String cleanMovieId = mId.trim();
                            if (!reviewStatsMap.containsKey(cleanMovieId)) {
                                reviewStatsMap.put(cleanMovieId, new double[]{0.0, 0.0});
                            }
                            double[] stats = reviewStatsMap.get(cleanMovieId);
                            if (stats != null) {
                                stats[0] += ratingValue;
                                stats[1] += 1.0;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("SearchActivity", "Lỗi phân tích phần tử Review từ Firestore: ", e);
                    }
                }
            }

            // Tiến hành tải danh sách phim từ Firestore về máy
            db.collection("movies").get().addOnSuccessListener(movieSnapshots -> {
                movieList.clear();
                if (movieSnapshots != null) {
                    for (QueryDocumentSnapshot doc : movieSnapshots) {
                        Movie movie = doc.toObject(Movie.class);

                        // Gán ID tài liệu gốc vào đối tượng phòng hờ dữ liệu thô trong DB thiếu trường này
                        if (movie.getMovieId() == null || movie.getMovieId().isEmpty()) {
                            movie.setMovieId(doc.getId());
                        }

                        // Đối chiếu mã ID phim với Map thống kê để gán số sao hiển thị
                        String currentMovieId = movie.getMovieId().trim();
                        if (reviewStatsMap.containsKey(currentMovieId)) {
                            double[] stats = reviewStatsMap.get(currentMovieId);
                            if (stats != null && stats[1] > 0) {
                                double rawAvg = stats[0] / stats[1];
                                double roundedAvg = Math.round(rawAvg * 10.0) / 10.0;

                                movie.setAverageRating(roundedAvg);
                                movie.setRatingCount((int) stats[1]);
                            }
                        } else {
                            movie.setAverageRating(0.0);
                            movie.setRatingCount(0);
                        }

                        movieList.add(movie);
                    }
                }

                // Cập nhật ngay lập tức bộ lọc danh sách dựa theo từ khóa hiện tại trên thanh EditText
                filterMovies(edtSearch.getText().toString().trim());
            });
        }).addOnFailureListener(e -> Log.e("SearchActivity", "Lỗi truy vấn bảng reviews: ", e));
    }

    private void updateUiState(String keyword) {
        if (keyword.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvSearchMovies.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvSearchMovies.setVisibility(View.VISIBLE);
        }
    }

    private void filterMovies(String keyword) {
        List<Movie> tempList = new ArrayList<>();

        if (!keyword.isEmpty()) {
            for (Movie movie : movieList) {
                if (movie.getTitle() != null &&
                        movie.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                    tempList.add(movie);
                }
            }
        }
        runOnUiThread(() -> {
            filteredList.clear();
            filteredList.addAll(tempList);
            adapter.notifyDataSetChanged();
        });
    }
}