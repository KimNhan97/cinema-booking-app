package dacn.buithikimnhan.cinemabookingapp.admin.movie;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.admin.dashboard.AdminMovieAdapter;
import dacn.buithikimnhan.cinemabookingapp.data.Movie;

public class ManageMovieFragment extends Fragment {

    private FirebaseFirestore db;
     RecyclerView rvAdminMovies;
     EditText edtSearchMovie;
     FloatingActionButton fabAddMovie;

    private AdminMovieAdapter adapter;
    private final List<Movie> movieList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_movie, container, false);

        db = FirebaseFirestore.getInstance();
        rvAdminMovies = view.findViewById(R.id.rvAdminMovies);
        edtSearchMovie = view.findViewById(R.id.edtSearchMovie);
        fabAddMovie = view.findViewById(R.id.fabAddMovie);

        rvAdminMovies.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminMovieAdapter(getContext(), movieList);
        rvAdminMovies.setAdapter(adapter);

        // 1. Đọc danh sách phim thời gian thực
        loadMoviesFromFirestore();

        // 2. Xử lý bộ lọc Tìm kiếm khi gõ chữ
        edtSearchMovie.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 3. Sự kiện nút Cộng (+) -> Thêm phim mới
        fabAddMovie.setOnClickListener(v -> showAddMovieDialog());

        return view;
    }

    private void loadMoviesFromFirestore() {
        db.collection("movies").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            movieList.clear();
            for (QueryDocumentSnapshot doc : value) {
                Movie movie = doc.toObject(Movie.class);
                movie.setMovieId(doc.getId());
                movieList.add(movie);
            }
            adapter.updateFullList(movieList);
        });
    }

    private void showAddMovieDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_movie, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.dialogTitle);
        tvTitle.setText("THÊM BỘ PHIM MỚI");

        EditText edtTitle = dialogView.findViewById(R.id.dialogEdtTitle);
        EditText edtDuration = dialogView.findViewById(R.id.dialogEdtDuration);
        EditText edtGenre = dialogView.findViewById(R.id.dialogEdtGenre);
        EditText edtReleaseDate = dialogView.findViewById(R.id.dialogEdtReleaseDate);
        EditText edtPoster = dialogView.findViewById(R.id.dialogEdtPosterUrl);
        EditText edtBanner = dialogView.findViewById(R.id.dialogEdtBannerUrl);
        EditText edtDesc = dialogView.findViewById(R.id.dialogEdtDescription);
        RadioGroup rgStatus = dialogView.findViewById(R.id.dialogRadioGroupStatus);
        RadioButton rbNow = dialogView.findViewById(R.id.radioNowShowing);
        Button btnCancel = dialogView.findViewById(R.id.dialogBtnCancel);
        Button btnSave = dialogView.findViewById(R.id.dialogBtnSave);

        rbNow.setChecked(true);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String durationStr = edtDuration.getText().toString().trim();

            if (title.isEmpty() || durationStr.isEmpty()) {
                Toast.makeText(getContext(), "Không được để trống Tên và Thời lượng!", Toast.LENGTH_SHORT).show();
                return;
            }

            String status = rbNow.isChecked() ? "now_showing" : "soon_showing";
            String generatedId = db.collection("movies").document().getId();

            Map<String, Object> newMovie = new HashMap<>();
            newMovie.put("movieId", generatedId);
            newMovie.put("title", title);
            newMovie.put("duration", Integer.parseInt(durationStr));
            newMovie.put("genre", edtGenre.getText().toString().trim());
            newMovie.put("releaseDate", edtReleaseDate.getText().toString().trim());
            newMovie.put("status", status);
            newMovie.put("posterUrl", edtPoster.getText().toString().trim());
            newMovie.put("bannerUrl", edtBanner.getText().toString().trim());
            newMovie.put("description", edtDesc.getText().toString().trim());


            newMovie.put("totalRating", 0.0);
            newMovie.put("ratingCount", 0);

            db.collection("movies").document(generatedId).set(newMovie)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Đã thêm phim mới thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });
    }
}