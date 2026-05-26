package dacn.buithikimnhan.cinemabookingapp.user.home;

import android.os.Bundle;
import android.util.Log;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Movie;
import dacn.buithikimnhan.cinemabookingapp.data.Review;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    RecyclerView rvHomeMain;
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
                // Nếu là vị trí 0 (Header/Banner Slider) -> cho chiếm trọn cả 2 cột chiều rộng
                // Nếu là các vị trí sau (Phim) -> cho chiếm 1 cột (tự động chia đôi màn hình)
                return position == 0 ? 2 : 1;
            }
        });

        rvHomeMain.setLayoutManager(layoutManager);
        rvHomeMain.setAdapter(mainAdapter);

        // 3. Tải và xử lý sắp xếp dữ liệu phim theo Review Rating thực tế hệ 5/5
        loadMoviesWithRatingOrder();

        return view;
    }

    private void loadMoviesWithRatingOrder() {
        db.collection("reviews")
                .addSnapshotListener((reviewSnapshots, reviewError) -> {
                    if (reviewError != null) {
                        Log.e(TAG, "Lỗi khi lấy dữ liệu đánh giá: ", reviewError);
                        return;
                    }

                    // Map lưu trữ thống kê: movieId -> [Tổng số điểm sao tích lũy, Số lượt review]
                    Map<String, double[]> reviewStatsMap = new HashMap<>();

                    if (reviewSnapshots != null) {
                        for (QueryDocumentSnapshot doc : reviewSnapshots) {
                            try {
                                Review review = doc.toObject(Review.class);
                                String mId = review.getMovieId();
                                double ratingValue = review.getRating();

                                if (mId != null) {
                                    if (!reviewStatsMap.containsKey(mId)) {
                                        reviewStatsMap.put(mId, new double[]{0.0, 0.0});
                                    }
                                    double[] stats = reviewStatsMap.get(mId);
                                    if (stats != null) {
                                        stats[0] += ratingValue; // Cộng dồn tổng số điểm sao
                                        stats[1] += 1.0;         // Tăng số lượng lượt nhận xét
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi ép kiểu Review dữ liệu", e);
                            }
                        }
                    }

                    // Lấy dữ liệu danh sách phim từ Firestore
                    db.collection("movies")
                            .addSnapshotListener((movieSnapshots, movieError) -> {
                                if (movieError != null) {
                                    Log.e(TAG, "Lỗi khi lấy dữ liệu phim: ", movieError);
                                    return;
                                }

                                if (movieSnapshots != null) {
                                    bannerList.clear();
                                    nowShowingList.clear();

                                    for (QueryDocumentSnapshot document : movieSnapshots) {
                                        Movie movie = document.toObject(Movie.class);

                                        // Gán ID của Document từ Firestore vào thuộc tính movieId ngay lập tức!
                                        movie.setMovieId(document.getId());

                                        // Đối chiếu và tính toán số điểm sao trung bình thực tế từ bảng Reviews
                                        if (reviewStatsMap.containsKey(movie.getMovieId())) {
                                            double[] stats = reviewStatsMap.get(movie.getMovieId());
                                            if (stats != null && stats[1] > 0) {
                                                double totalRatingStars = stats[0];
                                                long ratingCount = (long) stats[1];

                                                double rawAvg = totalRatingStars / ratingCount;
                                                // Làm tròn toán học lấy 1 chữ số thập phân chuẩn hệ 5/5
                                                double roundedAvg = Math.round(rawAvg * 10.0) / 10.0;

                                                movie.setAverageRating(roundedAvg);
                                                movie.setTotalRating(totalRatingStars);
                                                movie.setRatingCount((int) ratingCount);
                                            }
                                        } else {
                                            // Trường hợp chưa có lượt đánh giá nào
                                            movie.setAverageRating(0.0);
                                            movie.setTotalRating(0.0);
                                            movie.setRatingCount(0);
                                        }

                                        String status = movie.getStatus();

                                        // Phim thỏa mãn điều kiện làm Banner (được sao chép đối tượng nguyên vẹn kèm ID)
                                        if ("now_showing".equals(status) || "soon_showing".equals(status)) {
                                            bannerList.add(movie);
                                        }

                                        // Phim hiển thị ở danh sách lưới phía dưới
                                        if ("now_showing".equals(status)) {
                                            nowShowingList.add(movie);
                                        }
                                    }

                                    // Thuật toán sắp xếp giảm dần theo điểm đánh giá trung bình hệ 5/5 chuẩn xác
                                    Collections.sort(nowShowingList, (m1, m2) -> Double.compare(m2.getAverageRating(), m1.getAverageRating()));
                                    Collections.sort(bannerList, (m1, m2) -> Double.compare(m2.getAverageRating(), m1.getAverageRating()));

                                    // Làm mới dữ liệu lên Adapter chính để cập nhật cả lưới phim và banner lướt tự động
                                    mainAdapter.notifyDataSetChanged();
                                    // Làm mới dữ liệu lên Adapter chính để cập nhật cả lưới phim và banner lướt tự động
                                    mainAdapter.notifyDataSetChanged();

                                    // KÍCH HOẠT LẠI SLIDER TỰ ĐỘNG SAU KHI DỮ LIỆU ĐÃ TẢI XONG TỪ FIREBASE
                                    mainAdapter.startSlider();
                                }
                            });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mainAdapter != null) {
            mainAdapter.stopSlider(); // Giải phóng luồng chạy ngầm của ViewPager Banner khi chuyển màn hình
        }
    }
}