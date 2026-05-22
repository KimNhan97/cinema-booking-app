package dacn.buithikimnhan.cinemabookingapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvHomeMain;
    private HomeMainAdapter mainAdapter;

    private final List<Movie> bannerList = new ArrayList<>();
    private final List<Movie> nowShowingList = new ArrayList<>();
    private FirebaseFirestore db;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvHomeMain = view.findViewById(R.id.rvHomeMain);
        db = FirebaseFirestore.getInstance();

        // 1. Khởi tạo adapter gộp chung duy nhất
        mainAdapter = new HomeMainAdapter(requireContext(), bannerList, nowShowingList);

        // 2. Cấu hình lưới 2 cột thông minh bằng GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Nếu là vị trí 0 (Header) -> cho chiếm trọn cả 2 cột chiều rộng
                // Nếu là các vị trí sau (Phim) -> cho chiếm 1 cột (tự động chia đôi màn hình thành lưới 2 cột)
                return position == 0 ? 2 : 1;
            }
        });

        rvHomeMain.setLayoutManager(layoutManager);
        rvHomeMain.setAdapter(mainAdapter);

        // 3. Tải dữ liệu từ Firestore
        loadMoviesData();

        return view;
    }

    private void loadMoviesData() {
        db.collection("movies")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        bannerList.clear();
                        nowShowingList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Movie movie = document.toObject(Movie.class);
                            if (movie != null) {
                                String status = movie.getStatus();

                                // Phim đẩy lên Banner tự động chạy
                                if ("now_showing".equals(status) || "soon_showing".equals(status)) {
                                    bannerList.add(movie);
                                }
                                // Phim đổ vào lưới danh sách phía dưới
                                if ("now_showing".equals(status)) {
                                    nowShowingList.add(movie);
                                }
                            }
                        }

                        // Thông báo cho adapter tổng làm mới lại toàn bộ giao diện màn hình Home
                        mainAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mainAdapter != null) {
            mainAdapter.stopSlider(); // Tắt luồng chạy ngầm slide ảnh để tránh rò rỉ bộ nhớ
        }
    }
}