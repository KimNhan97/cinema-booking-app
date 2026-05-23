package dacn.buithikimnhan.cinemabookingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class HomeMainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MOVIE = 1;

    private final Context context;
    private final List<Movie> bannerMovies;
    private final List<Movie> gridMovies;

    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;

    public HomeMainAdapter(Context context,
                           List<Movie> bannerMovies,
                           List<Movie> gridMovies) {
        this.context = context;
        this.bannerMovies = bannerMovies;
        this.gridMovies = gridMovies;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_MOVIE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_home_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_movie_grid, parent, false);
            return new MovieViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        // ================= HEADER =================
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

            // ================= USER INFO =================
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                headerHolder.tvUserEmail.setText(user.getEmail());
                if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                    headerHolder.tvUserName.setText("Hello, " + user.getDisplayName() + " 👋");
                } else if (user.getEmail() != null) {
                    headerHolder.tvUserName.setText("Hello, " + user.getEmail().split("@")[0] + " 👋");
                }
            }

            // Tìm kiếm
            headerHolder.layoutSearch.setOnClickListener(v -> {
                Intent intent = new Intent(context, SearchActivity.class);
                context.startActivity(intent);
            });

            // Banner
            if (headerHolder.bannerViewPager.getAdapter() == null) {
                BannerAdapter bannerAdapter = new BannerAdapter(context, bannerMovies);
                headerHolder.bannerViewPager.setAdapter(bannerAdapter);

                // Chuyển động slider tự động
                sliderRunnable = () -> {
                    if (bannerMovies.isEmpty()) return;
                    int nextItem = headerHolder.bannerViewPager.getCurrentItem() + 1;
                    if (nextItem >= bannerMovies.size()) {
                        nextItem = 0;
                    }
                    headerHolder.bannerViewPager.setCurrentItem(nextItem, true);
                    sliderHandler.postDelayed(sliderRunnable, 3000);
                };

                if (bannerMovies.size() > 1) {
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, 3000);
                }
            } else {
                headerHolder.bannerViewPager.getAdapter().notifyDataSetChanged();
            }
        }

        // ================= MOVIE GRID (DANH SÁCH PHIM) =================
        else if (holder instanceof MovieViewHolder) {
            // Lấy đúng vị trí phim trong danh sách (trừ đi 1 phần tử Header ở vị trí 0)
            Movie movie = gridMovies.get(position - 1);
            MovieViewHolder movieHolder = (MovieViewHolder) holder;

            movieHolder.txtMovieTitle.setText(movie.getTitle());
            movieHolder.txtMovieSubtitle.setText(movie.getGenre() + " • " + movie.getDuration() + " phút");

            // Tải hình ảnh Poster/Banner phim bằng thư viện Glide
            Glide.with(context)
                    .load(movie.getBannerUrl())
                    .placeholder(R.drawable.movie1)
                    .error(R.drawable.movie1)
                    .into(movieHolder.imgMoviePoster);

            // ================= BẮT SỰ KIỆN CLICK VÀO PHIM =================
            movieHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MovieDetailActivity.class);

                // Gửi nguyên Object Movie sang màn hình chi tiết (Yêu cầu lớp Movie phải "implements Serializable")
                intent.putExtra("CHOSEN_MOVIE", movie);

                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return 1 + (gridMovies != null ? gridMovies.size() : 0);
    }

    public void stopSlider() {
        if (sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }
    }

    // ================= HEADER VIEW HOLDER =================
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail;
        ViewPager2 bannerViewPager;
        EditText layoutSearch;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            bannerViewPager = itemView.findViewById(R.id.bannerViewPager);
            layoutSearch = itemView.findViewById(R.id.layoutSearch);
        }
    }

    // ================= MOVIE VIEW HOLDER =================
    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMoviePoster;
        TextView txtMovieTitle, txtMovieSubtitle;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMoviePoster = itemView.findViewById(R.id.imgMoviePoster);
            txtMovieTitle = itemView.findViewById(R.id.txtMovieTitle);
            txtMovieSubtitle = itemView.findViewById(R.id.txtMovieSubtitle);
        }
    }
}