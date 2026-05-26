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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

public class HomeMainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MOVIE = 1;

    private final Context context;
    private final List<Movie> bannerMovies;
    private final List<Movie> gridMovies;

    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private ViewPager2 activeViewPager; // Lưu lại ViewPager để stop/start chính xác

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
            activeViewPager = headerHolder.bannerViewPager;

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

            // Banner Slider Setup
            if (headerHolder.bannerViewPager.getAdapter() == null) {
                BannerAdapter bannerAdapter = new BannerAdapter(context, bannerMovies);
                headerHolder.bannerViewPager.setAdapter(bannerAdapter);
            } else {
                headerHolder.bannerViewPager.getAdapter().notifyDataSetChanged();
            }

            // Gọi hàm kích hoạt tự động lướt mỗi khi bind dữ liệu Header
            startSliderInner();
        }

        // ================= MOVIE GRID (DANH SÁCH PHIM) =================
        else if (holder instanceof MovieViewHolder) {
            Movie movie = gridMovies.get(position - 1);
            MovieViewHolder movieHolder = (MovieViewHolder) holder;

            movieHolder.txtMovieTitle.setText(movie.getTitle());
            movieHolder.txtMovieSubtitle.setText(movie.getGenre() + " • " + movie.getDuration() + " phút");

            Glide.with(context)
                    .load(movie.getBannerUrl())
                    .placeholder(R.drawable.movie1)
                    .error(R.drawable.movie1)
                    .into(movieHolder.imgMoviePoster);

            // Bổ sung hiển thị số sao
            double avgRating = movie.getAverageRating();
            if (avgRating > 0) {
                movieHolder.txtRating.setText(String.format(Locale.getDefault(), "⭐ %.1f/5", avgRating));
            } else {
                movieHolder.txtRating.setText("⭐ 0.0/5");
            }
            movieHolder.txtRating.setVisibility(View.VISIBLE);

            movieHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MovieDetailActivity.class);
                intent.putExtra("CHOSEN_MOVIE", movie);
                context.startActivity(intent);
            });
        }
    }

    // Hàm khởi chạy hiệu ứng lướt tự động sang trái (vòng lặp vô tận)
    private void startSliderInner() {
        stopSlider(); // Xóa lịch trình cũ trước khi tạo vòng lặp mới nhằm tránh xung đột trùng lặp luồng

        if (bannerMovies == null || bannerMovies.size() <= 1 || activeViewPager == null) {
            return;
        }

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (activeViewPager == null || bannerMovies.isEmpty()) return;

                int nextItem = activeViewPager.getCurrentItem() + 1;
                if (nextItem >= bannerMovies.size()) {
                    nextItem = 0; // Quay về banner đầu tiên nếu lướt hết danh sách
                }

                activeViewPager.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(this, 3000); // Lặp lại sau mỗi 3 giây
            }
        };

        sliderHandler.postDelayed(sliderRunnable, 3000);
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

    // Thêm hàm startSlider ra bên ngoài để HomeFragment có thể chủ động kích hoạt lại khi dữ liệu Firestore tải xong
    public void startSlider() {
        startSliderInner();
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
        TextView txtMovieTitle, txtMovieSubtitle, txtRating;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMoviePoster = itemView.findViewById(R.id.imgMoviePoster);
            txtMovieTitle = itemView.findViewById(R.id.txtMovieTitle);
            txtMovieSubtitle = itemView.findViewById(R.id.txtMovieSubtitle);
            txtRating = itemView.findViewById(R.id.txtRating);
        }
    }
}