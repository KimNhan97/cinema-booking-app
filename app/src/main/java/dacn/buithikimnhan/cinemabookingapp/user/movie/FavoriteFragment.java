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
    List<Movie> favoriteMoviesList = new ArrayList<>();
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

                            // Đồng bộ gán cả 2 trường title phòng trường hợp db thiết kế không đồng nhất
                            String title = doc.getString("movieTitle");
                            if (title == null) title = doc.getString("title");
                            movie.setTitle(title);

                            movie.setBannerUrl(doc.getString("bannerUrl"));
                            movie.setPosterUrl(doc.getString("posterUrl"));
                            movie.setGenre(doc.getString("genre"));

                            // Đảm bảo đọc chuẩn xác dữ liệu kiểu số và chuỗi
                            if (doc.contains("description") && doc.get("description") != null) {
                                movie.setDescription(doc.getString("description"));
                            } else {
                                movie.setDescription("Nội dung đang cập nhật...");
                            }

                            if (doc.contains("duration") && doc.get("duration") != null) {
                                try {
                                    movie.setDuration(((Long) doc.get("duration")).intValue());
                                } catch (Exception e) {
                                    movie.setDuration(120);
                                }
                            } else {
                                movie.setDuration(120);
                            }

                            if (doc.contains("releaseDate") && doc.get("releaseDate") != null) {
                                movie.setReleaseDate(doc.getString("releaseDate"));
                            } else {
                                movie.setReleaseDate("2026-01-01");
                            }

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

            // Khắc phục lỗi hiển thị Poster
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

            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MovieDetailActivity.class);
                intent.putExtra("movieId", movie.getMovieId());
                // Thêm cờ chạy Intent an toàn từ Context của Adapter
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });

            return convertView;
        }
    }
}