package dacn.buithikimnhan.cinemabookingapp.admin.dashboard;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Movie;

public class AdminMovieAdapter extends RecyclerView.Adapter<AdminMovieAdapter.AdminMovieViewHolder> {

    private final Context context;
    private List<Movie> displayList;
    private List<Movie> originalList;

    public AdminMovieAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.displayList = movieList;
        this.originalList = new ArrayList<>(movieList);
    }

    public void updateFullList(List<Movie> newList) {
        this.displayList = newList;
        this.originalList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminMovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_movie_list, parent, false);
        return new AdminMovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminMovieViewHolder holder, int position) {
        Movie movie = displayList.get(position);

        holder.txtTitle.setText(movie.getTitle() != null ? movie.getTitle() : "Chưa có tên");
        holder.txtSubtitle.setText((movie.getGenre() != null ? movie.getGenre() : "N/A") + " • " + movie.getDuration() + " phút");

        // Tính điểm trung bình an toàn phòng trường hợp Firestore trả về giá trị null hoặc 0
        double avgRating = movie.getAverageRating();
        if (avgRating == 0.0 && movie.getRatingCount() > 0) {
            avgRating = movie.getTotalRating() / movie.getRatingCount();
        }
        holder.txtRating.setText(String.format(Locale.getDefault(), "⭐ %.1f/5", avgRating));

        Glide.with(context)
                .load(movie.getPosterUrl())
                .placeholder(R.drawable.movie_test)
                .error(R.drawable.movie_test)
                .into(holder.imgPoster);

        holder.btnEdit.setOnClickListener(v -> showEditDialog(movie));

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn chắc chắn muốn xóa phim \"" + movie.getTitle() + "\"?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        FirebaseFirestore.getInstance().collection("movies").document(movie.getMovieId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "Đã xóa phim thành công!", Toast.LENGTH_SHORT).show();

                                    // Cập nhật nhanh danh sách hiển thị tức thời mà không cần load lại fragment
                                    int currentPosition = displayList.indexOf(movie);
                                    if (currentPosition != -1) {
                                        displayList.remove(currentPosition);
                                        originalList.remove(movie);
                                        notifyItemRemoved(currentPosition);
                                        notifyItemRangeChanged(currentPosition, displayList.size());
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(context, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void showEditDialog(Movie movie) {
        if (movie == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_movie, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.dialogTitle);
        EditText edtTitle = dialogView.findViewById(R.id.dialogEdtTitle);
        EditText edtDuration = dialogView.findViewById(R.id.dialogEdtDuration);
        EditText edtGenre = dialogView.findViewById(R.id.dialogEdtGenre);
        EditText edtReleaseDate = dialogView.findViewById(R.id.dialogEdtReleaseDate);
        EditText edtPoster = dialogView.findViewById(R.id.dialogEdtPosterUrl);
        EditText edtBanner = dialogView.findViewById(R.id.dialogEdtBannerUrl);
        EditText edtDesc = dialogView.findViewById(R.id.dialogEdtDescription);
        RadioGroup rgStatus = dialogView.findViewById(R.id.dialogRadioGroupStatus);
        RadioButton rbNow = dialogView.findViewById(R.id.radioNowShowing);
        RadioButton rbSoon = dialogView.findViewById(R.id.radioSoonShowing);

        Button btnSubmitPoster = dialogView.findViewById(R.id.dialogBtnSubmitPoster);
        Button btnSubmitBanner = dialogView.findViewById(R.id.dialogBtnSubmitBanner);
        Button btnCancel = dialogView.findViewById(R.id.dialogBtnCancel);
        Button btnSave = dialogView.findViewById(R.id.dialogBtnSave);

        tvTitle.setText("CHỈNH SỬA THÔNG TIN PHIM");

        edtTitle.setText(movie.getTitle() != null ? movie.getTitle() : "");
        edtDuration.setText(String.valueOf(movie.getDuration()));
        edtGenre.setText(movie.getGenre() != null ? movie.getGenre() : "");
        edtReleaseDate.setText(movie.getReleaseDate() != null ? movie.getReleaseDate() : "");
        edtPoster.setText(movie.getPosterUrl() != null ? movie.getPosterUrl() : "");
        edtBanner.setText(movie.getBannerUrl() != null ? movie.getBannerUrl() : "");
        edtDesc.setText(movie.getDescription() != null ? movie.getDescription() : "");

        if ("now_showing".equals(movie.getStatus())) {
            rbNow.setChecked(true);
        } else {
            rbSoon.setChecked(true);
        }

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();

        btnSubmitPoster.setOnClickListener(v -> {
            String url = edtPoster.getText().toString().trim();
            if(!url.isEmpty()) {
                Toast.makeText(context, "Đã ghi nhận đường dẫn Poster!", Toast.LENGTH_SHORT).show();
            }
        });

        btnSubmitBanner.setOnClickListener(v -> {
            String url = edtBanner.getText().toString().trim();
            if(!url.isEmpty()) {
                Toast.makeText(context, "Đã ghi nhận đường dẫn Banner!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String durationStr = edtDuration.getText().toString().trim();

            if (title.isEmpty() || durationStr.isEmpty()) {
                Toast.makeText(context, "Tên phim và Thời lượng không được bỏ trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            String status = rbNow.isChecked() ? "now_showing" : "soon_showing";

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("title", title);
            updateData.put("duration", Integer.parseInt(durationStr));
            updateData.put("genre", edtGenre.getText().toString().trim());
            updateData.put("releaseDate", edtReleaseDate.getText().toString().trim());
            updateData.put("status", status);
            updateData.put("posterUrl", edtPoster.getText().toString().trim());
            updateData.put("bannerUrl", edtBanner.getText().toString().trim());
            updateData.put("description", edtDesc.getText().toString().trim());

            FirebaseFirestore.getInstance().collection("movies").document(movie.getMovieId())
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Cập nhật dữ liệu phim thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    public void filter(String text) {
        displayList = new ArrayList<>();
        if (text.isEmpty()) {
            displayList.addAll(originalList);
        } else {
            String pattern = text.toLowerCase().trim();
            for (Movie item : originalList) {
                if ((item.getTitle() != null && item.getTitle().toLowerCase().contains(pattern)) ||
                        (item.getGenre() != null && item.getGenre().toLowerCase().contains(pattern))) {
                    displayList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return displayList != null ? displayList.size() : 0;
    }

    public static class AdminMovieViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster, btnEdit, btnDelete;
        TextView txtTitle, txtSubtitle, txtRating;

        public AdminMovieViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgAdminMoviePoster);
            txtTitle = itemView.findViewById(R.id.txtAdminMovieTitle);
            txtSubtitle = itemView.findViewById(R.id.txtAdminMovieSubtitle);
            txtRating = itemView.findViewById(R.id.txtAdminMovieRating);
            btnEdit = itemView.findViewById(R.id.btnEditMovie);
            btnDelete = itemView.findViewById(R.id.btnDeleteMovie);
        }
    }
}