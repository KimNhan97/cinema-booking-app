package dacn.buithikimnhan.cinemabookingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    public HomeMainAdapter(Context context, List<Movie> bannerMovies, List<Movie> gridMovies) {
        this.context = context;
        this.bannerMovies = bannerMovies;
        this.gridMovies = gridMovies;
    }

    @Override
    public int getItemViewType(int position) {
        // chia vùng
        return position == 0 ? TYPE_HEADER : TYPE_MOVIE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_home_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_movie_grid, parent, false);
            return new MovieViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

            // Đổ dữ liệu User
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                headerHolder.tvUserEmail.setText(user.getEmail());
                if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                    headerHolder.tvUserName.setText("Hello, " + user.getDisplayName() + " 👋");
                } else if (user.getEmail() != null) {
                    headerHolder.tvUserName.setText("Hello, " + user.getEmail().split("@")[0] + " 👋");
                }
            }

            // Banner
            if (headerHolder.bannerViewPager.getAdapter() == null) {
                BannerAdapter bannerAdapter = new BannerAdapter(context, bannerMovies);
                headerHolder.bannerViewPager.setAdapter(bannerAdapter);

                // tự động chuyển slide ảnh
                sliderRunnable = () -> {
                    if (bannerMovies.isEmpty()) return;
                    int nextItem = headerHolder.bannerViewPager.getCurrentItem() + 1;
                    if (nextItem >= bannerMovies.size()) nextItem = 0;
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

        } else if (holder instanceof MovieViewHolder) {
            Movie movie = gridMovies.get(position - 1);
            MovieViewHolder movieHolder = (MovieViewHolder) holder;

            movieHolder.txtMovieTitle.setText(movie.getTitle());
            movieHolder.txtMovieSubtitle.setText(movie.getGenre() + " • " + movie.getDuration() + " phút");

            // Load ảnh poster phim bằng Glide( chưa có)
            Glide.with(context)
                    .load(movie.getBannerUrl())
                    .placeholder(R.drawable.movie1)
                    .error(R.drawable.movie1)
                    .into(movieHolder.imgMoviePoster);

            // đặt vé phim
            movieHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MovieDetailActivity.class);
                intent.putExtra("movieId", movie.getMovieId());
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        // Tổng số ô = 1 ô Header + số lượng phim đang chiếu
        return 1 + (gridMovies != null ? gridMovies.size() : 0);
    }

    public void stopSlider() {
        if (sliderRunnable != null) sliderHandler.removeCallbacks(sliderRunnable);
    }

    // view của header
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail;
        ViewPager2 bannerViewPager;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            bannerViewPager = itemView.findViewById(R.id.bannerViewPager);
        }
    }

    // view của ds movie
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