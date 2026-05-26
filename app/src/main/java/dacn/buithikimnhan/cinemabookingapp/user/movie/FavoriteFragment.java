package dacn.buithikimnhan.cinemabookingapp.user.movie;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Movie;

public class FavoriteFragment extends Fragment {

    private ListView lvFavorites;
    private LinearLayout layoutEmptyFavorite;

    private FirebaseFirestore db;
    private List<Movie> favoriteMoviesList = new ArrayList<>();
    private FavoriteListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);

        db = FirebaseFirestore.getInstance();
        lvFavorites = view.findViewById(R.id.lvFavorites);
        layoutEmptyFavorite = view.findViewById(R.id.layout_empty_favorite);

        // Khởi tạo Adapter và gán cho ListView
        adapter = new FavoriteListAdapter(getContext(), favoriteMoviesList);
        lvFavorites.setAdapter(adapter);

        // SỰ KIỆN CLICK DÒNG: Khi ấn vào bất kỳ bộ phim nào trong danh sách yêu thích, mở thẳng trang chi tiết phim đó
        lvFavorites.setOnItemClickListener((parent, view1, position, id) -> {
            Movie chosenMovie = favoriteMoviesList.get(position);
            Intent intent = new Intent(getActivity(), MovieDetailActivity.class);
            intent.putExtra("CHOSEN_MOVIE", chosenMovie);
            startActivity(intent);
        });

        // Tải dữ liệu phim yêu thích từ mạng về
        loadFavoriteMoviesFromFirestore();

        return view;
    }

    private void loadFavoriteMoviesFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showEmptyState();
            return;
        }
        String currentUserId = currentUser.getUid();

        favoriteMoviesList.clear();

        // Tiến hành lọc trong bảng "favorites" có trường userId trùng với người dùng này
        db.collection("favorites")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {

                        // Chuyển giao diện sang chế độ có danh sách hiển thị
                        lvFavorites.setVisibility(View.VISIBLE);
                        layoutEmptyFavorite.setVisibility(View.GONE);

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            // Chuyển đổi dữ liệu ngược về Object Movie để tái sử dụng
                            Movie movie = new Movie();
                            movie.setMovieId(doc.getString("movieId"));
                            movie.setTitle(doc.getString("movieTitle"));
                            movie.setBannerUrl(doc.getString("bannerUrl"));
                            movie.setPosterUrl(doc.getString("posterUrl"));
                            movie.setGenre(doc.getString("genre"));

                            // Các trường thông tin phụ đề phòng nếu trang chi tiết cần đọc
                            movie.setDescription(doc.contains("description") ? doc.getString("description") : "Nội dung đang cập nhật...");
                            movie.setDuration(doc.contains("duration") ? doc.getLong("duration").intValue() : 120);
                            movie.setReleaseDate(doc.contains("releaseDate") ? doc.getString("releaseDate") : "2026-01-01");

                            favoriteMoviesList.add(movie);
                        }

                        // Cập nhật làm mới lại ListView
                        adapter.notifyDataSetChanged();

                    } else {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        showEmptyState();
                        Toast.makeText(getContext(), "Lỗi tải danh sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEmptyState() {
        lvFavorites.setVisibility(View.GONE);
        layoutEmptyFavorite.setVisibility(View.VISIBLE);
    }

    // ================= ADAPTER TÙY BIẾN CHO LISTVIEW YÊU THÍCH =================
    private static class FavoriteListAdapter extends BaseAdapter {

        private Context context;
        private List<Movie> movies;

        public FavoriteListAdapter(Context context, List<Movie> movies) {
            this.context = context;
            this.movies = movies;
        }

        @Override
        public int getCount() {
            return movies.size();
        }

        @Override
        public Object getItem(int position) {
            return movies.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_favorite_movie, parent, false);
            }

            ImageView imgPoster = convertView.findViewById(R.id.imgFavPoster);
            TextView tvTitle = convertView.findViewById(R.id.tvFavTitle);
            TextView tvGenre = convertView.findViewById(R.id.tvFavGenre);

            Movie movie = movies.get(position);

            tvTitle.setText(movie.getTitle());
            tvGenre.setText(movie.getGenre());

            // KHẮC PHỤC LỖI HIỂN THỊ POSTER:
            // Ưu tiên lấy ảnh poster đứng (posterUrl), nếu trống thì mới lấy ảnh banner ngang làm dự phòng
            String imageToShow = movie.getPosterUrl();
            if (imageToShow == null || imageToShow.isEmpty()) {
                imageToShow = movie.getBannerUrl();
            }

            // Tải ảnh mượt mà bằng thư viện Glide
            Glide.with(context.getApplicationContext())
                    .load(imageToShow)
                    .placeholder(R.drawable.movie_test)
                    .error(R.drawable.movie_test)
                    .into(imgPoster);

            return convertView;
        }
    }
}