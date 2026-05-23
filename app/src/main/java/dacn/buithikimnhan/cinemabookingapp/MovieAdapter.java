package dacn.buithikimnhan.cinemabookingapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private final Context context;
    private final List<Movie> movieList;

    public MovieAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_movie_grid, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);

        // Gán dữ liệu Text chữ
        holder.tvTitle.setText(movie.getTitle() != null ? movie.getTitle() : "Chưa có tên");
        holder.tvGenre.setText(movie.getGenre() != null ? movie.getGenre() : "Thế loại");

        // Tải ảnh mượt mà bằng Glide (Lấy context động tránh xung đột vòng đời)
        Glide.with(holder.itemView.getContext())
                .load(movie.getPosterUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Lưu cache thông minh tăng tốc độ load
                .placeholder(android.R.drawable.ic_menu_gallery) // Ảnh chờ khi đang tải
                .error(android.R.drawable.ic_delete)             // Hiện dấu X nếu link chết/lỗi mạng
                .into(holder.imgPoster);

        // Sự kiện click vào item chuyển sang chi tiết phim
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("movieId", movie.getMovieId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return movieList != null ? movieList.size() : 0;
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView tvTitle, tvGenre;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgMoviePoster);
            tvTitle = itemView.findViewById(R.id.txtMovieTitle);
            tvGenre = itemView.findViewById(R.id.txtMovieSubtitle);
        }
    }
}