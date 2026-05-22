package dacn.buithikimnhan.cinemabookingapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView; // Đã thêm import TextView

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private final Context context;
    private final List<Movie> movieList;

    public BannerAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Movie movie = movieList.get(position);

        // Load ảnh banner bằng Glide
        Glide.with(context)
                .load(movie.getBannerUrl())
                .placeholder(R.drawable.movie1)
                .error(R.drawable.movie1)
                .into(holder.imgBanner);

        // Tiêu đề phim
        holder.txtTitle.setText(movie.getTitle());

        // Subtitle (Thể loại • Thời lượng)
        holder.txtSubtitle.setText(movie.getGenre() + " • " + movie.getDuration() + " phút");

        // Click vào toàn bộ Item Banner
        holder.itemView.setOnClickListener(v -> navigateToDetail(movie));

        // Click vào nút Đặt vé ngay
        holder.btnBookNow.setOnClickListener(v -> navigateToDetail(movie));
    }

    private void navigateToDetail(Movie movie) {
        Intent intent = new Intent(context, MovieDetailActivity.class);
        intent.putExtra("movieId", movie.getMovieId());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return movieList != null ? movieList.size() : 0;
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBanner;
        TextView txtTitle, txtSubtitle;
        TextView btnBookNow;
        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBanner = itemView.findViewById(R.id.imgBanner);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtSubtitle = itemView.findViewById(R.id.txtSubtitle);
            btnBookNow = itemView.findViewById(R.id.btnBookNow);
        }
    }
}