package dacn.buithikimnhan.cinemabookingapp.user.movie;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.List;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Movie;
import dacn.buithikimnhan.cinemabookingapp.user.MainActivity;

public class SearchActivity extends AppCompatActivity {

    private EditText edtSearch;
    private RecyclerView rvSearchMovies;
    private CardView layoutEmptyState;
    private TextView tvCancel;

    private MovieAdapter adapter;
    private final List<Movie> movieList = new ArrayList<>();     // Lưu toàn bộ danh sách gốc từ DB
    private final List<Movie> filteredList = new ArrayList<>();  // Danh sách hiển thị sau lọc

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
        adapter = new MovieAdapter(this, filteredList);

        rvSearchMovies.setLayoutManager(new GridLayoutManager(this, 2));
        rvSearchMovies.setAdapter(adapter);

        // Tải trước dữ liệu phim về bộ nhớ đệm ẩn
        loadMovies();

        // Khởi tạo trạng thái giao diện ban đầu (Trống hoàn toàn)
        updateUiState("");

        // Sự kiện xử lý nút Hủy - Quay lại màn hình Trang Chủ (Đã sửa lỗi gọi Adapter)
        tvCancel.setOnClickListener(v -> {
            // Đã sửa đổi: Thay thế HomeMainAdapter thành MainActivity (hoặc HomeActivity tùy dự án của bạn)
            android.content.Intent intent = new android.content.Intent(SearchActivity.this, MainActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Theo dõi thanh tìm kiếm thời gian thực
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

    private void loadMovies() {
        db.collection("movies")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    movieList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Movie movie = doc.toObject(Movie.class);
                        movieList.add(movie);
                    }
                    // Lúc mới khởi động, ô tìm kiếm đang trống nên không ép dữ liệu tràn lan vào list lọc
                    filteredList.clear();
                    adapter.notifyDataSetChanged();
                });
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
        // Sử dụng một danh sách tạm thời giúp luồng tải ảnh không bị ngắt quãng giữa chừng
        List<Movie> tempList = new ArrayList<>();

        if (!keyword.isEmpty()) {
            for (Movie movie : movieList) {
                if (movie.getTitle() != null &&
                        movie.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                    tempList.add(movie);
                }
            }
        }

        // Đẩy đồng bộ cập nhật lên UI Thread bắt buộc để vẽ lại ảnh ngay lập tức
        runOnUiThread(() -> {
            filteredList.clear();
            filteredList.addAll(tempList);
            adapter.notifyDataSetChanged();
        });
    }
}