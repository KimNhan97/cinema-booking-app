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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvHomeMain = view.findViewById(R.id.rvHomeMain);
        db = FirebaseFirestore.getInstance();

        mainAdapter = new HomeMainAdapter(requireContext(), bannerList, nowShowingList);

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? 2 : 1;
            }
        });

        rvHomeMain.setLayoutManager(layoutManager);
        rvHomeMain.setAdapter(mainAdapter);

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
                                        stats[0] += ratingValue;
                                        stats[1] += 1.0;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi ép kiểu Review dữ liệu", e);
                            }
                        }
                    }

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

                                        String dbMovieId = document.getString("movieId");
                                        if (dbMovieId != null && !dbMovieId.isEmpty()) {
                                            movie.setMovieId(dbMovieId);
                                        } else {
                                            movie.setMovieId(document.getId());
                                        }

                                        if (reviewStatsMap.containsKey(movie.getMovieId())) {
                                            double[] stats = reviewStatsMap.get(movie.getMovieId());
                                            if (stats != null && stats[1] > 0) {
                                                double totalRatingStars = stats[0];
                                                long ratingCount = (long) stats[1];

                                                double rawAvg = totalRatingStars / ratingCount;
                                                double roundedAvg = Math.round(rawAvg * 10.0) / 10.0;

                                                movie.setAverageRating(roundedAvg);
                                                movie.setTotalRating(totalRatingStars);
                                                movie.setRatingCount((int) ratingCount);
                                            }
                                        } else {
                                            movie.setAverageRating(0.0);
                                            movie.setTotalRating(0.0);
                                            movie.setRatingCount(0);
                                        }

                                        String status = movie.getStatus();

                                        if ("now_showing".equals(status) || "soon_showing".equals(status)) {
                                            bannerList.add(movie);
                                        }

                                        if ("now_showing".equals(status)) {
                                            nowShowingList.add(movie);
                                        }
                                    }

                                    Collections.sort(nowShowingList, (m1, m2) -> Double.compare(m2.getAverageRating(), m1.getAverageRating()));
                                    Collections.sort(bannerList, (m1, m2) -> Double.compare(m2.getAverageRating(), m1.getAverageRating()));

                                    mainAdapter.notifyDataSetChanged();
                                    mainAdapter.startSlider();
                                }
                            });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mainAdapter != null) {
            mainAdapter.stopSlider();
        }
    }
}