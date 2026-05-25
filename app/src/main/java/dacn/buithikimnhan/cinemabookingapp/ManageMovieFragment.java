package dacn.buithikimnhan.cinemabookingapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ManageMovieFragment extends Fragment {

    private FirebaseFirestore db;
    private FloatingActionButton fabAddMovie;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_movie, container, false);
        db = FirebaseFirestore.getInstance();
        fabAddMovie = view.findViewById(R.id.fabAddMovie);

        fabAddMovie.setOnClickListener(v -> showAddMovieDialog());

        // Bạn có thể dùng chung cấu trúc Adapter hiển thị danh sách phim hiện có của bạn ở đây
        return view;
    }

    private void showAddMovieDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Thêm Phim Mới");

        // Tạo nhanh ô nhập tên phim
        final EditText inputTitle = new EditText(getContext());
        inputTitle.setHint("Nhập tên phim chính xác");
        builder.setView(inputTitle);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String movieName = inputTitle.getText().toString().trim();
            if (!movieName.isEmpty()) {

                String generatedId = db.collection("movies").document().getId(); // Tự sinh mã phim

                Map<String, Object> newMovie = new HashMap<>();
                newMovie.put("movieId", generatedId);
                newMovie.put("movieTitle", movieName);
                newMovie.put("status", "Đang chiếu"); // Trạng thái mặc định ban đầu
                newMovie.put("totalRating", 0.0);
                newMovie.put("ratingCount", 0);

                db.collection("movies").document(generatedId).set(newMovie)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Đã thêm phim thành công!", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}