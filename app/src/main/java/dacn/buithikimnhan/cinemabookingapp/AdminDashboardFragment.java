package dacn.buithikimnhan.cinemabookingapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardFragment extends Fragment {

    private static final String TAG = "AdminDashboard";

    private View cardRevenue, cardTickets, cardUsers, cardMovies;
    private TextView tvRevenue, tvTicketsSold, tvTotalUsers, tvActiveMovies;

     RecyclerView rvHotMovies;
     RecyclerView rvSessions;
     RecyclerView rvBookings;

    private List<Movie> hotMovieList;
    private HotMovieAdapter hotMovieAdapter;

    private FirebaseFirestore db;

    public AdminDashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();


        cardRevenue = view.findViewById(R.id.cardRevenue);
        cardTickets = view.findViewById(R.id.cardTickets);
        cardUsers = view.findViewById(R.id.cardUsers);
        cardMovies = view.findViewById(R.id.cardMovies);

        tvRevenue = view.findViewById(R.id.tvRevenue);
        tvTicketsSold = view.findViewById(R.id.tvTicketsSold);
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        tvActiveMovies = view.findViewById(R.id.tvActiveMovies);

        rvHotMovies = view.findViewById(R.id.rvHotMovies);
        rvSessions = view.findViewById(R.id.rvSessions);
        rvBookings = view.findViewById(R.id.rvBookings);

        if (rvHotMovies != null) {
            rvHotMovies.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            hotMovieList = new ArrayList<>();
            hotMovieAdapter = new HotMovieAdapter(hotMovieList);
            rvHotMovies.setAdapter(hotMovieAdapter);
        }
        if (rvSessions != null) {
            rvSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        if (rvBookings != null) {
            rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        setupAnimations();
        fetchFirestoreData();


    }

    private void fetchFirestoreData() {

        // --- 1. TÍNH TOÁN DOANH THU & VÉ TỪ COLLECTION "bookings" ---
        db.collection("bookings")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi khi lấy dữ liệu bookings: ", error);
                        return;
                    }

                    if (snapshots != null) {
                        long totalRevenue = 0;
                        int totalTickets = 0;

                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Object priceObj = doc.get("totalPrice");
                                if (priceObj instanceof Number) {
                                    totalRevenue += ((Number) priceObj).longValue();
                                } else if (priceObj instanceof String) {
                                    String priceStr = (String) priceObj;
                                    priceStr = priceStr.replaceAll("[.,]", "").trim();
                                    if (!priceStr.isEmpty()) {
                                        totalRevenue += Long.parseLong(priceStr);
                                    }
                                }

                                List<?> seats = (List<?>) doc.get("seats");
                                if (seats != null) {
                                    totalTickets += seats.size();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi xử lý hóa đơn ID: " + doc.getId(), e);
                            }
                        }

                        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                        tvRevenue.setText(currencyFormat.format(totalRevenue));
                        tvTicketsSold.setText(totalTickets + " vé");
                    }
                });

        // --- 2. DỰA TRÊN ĐỐI TƯỢNG REVIEW.CLASS ĐỂ XÉT RATING PHIM HOT ---
        db.collection("reviews")
                .addSnapshotListener((reviewSnapshots, reviewError) -> {
                    if (reviewError != null) {
                        Log.e(TAG, "Lỗi khi lấy dữ liệu reviews: ", reviewError);
                        return;
                    }

                    // Map gom nhóm thống kê: movieId -> [Tổng số điểm sao, Số lượt đánh giá]
                    Map<String, double[]> reviewStatsMap = new HashMap<>();

                    if (reviewSnapshots != null) {
                        for (QueryDocumentSnapshot doc : reviewSnapshots) {
                            try {
                                // Tự động ép kiểu (Mapping) trực tiếp sang Class Review của bạn cực kỳ an toàn
                                Review review = doc.toObject(Review.class);

                                String mId = review.getMovieId();
                                double ratingValue = review.getRating();

                                if (mId != null) {
                                    if (!reviewStatsMap.containsKey(mId)) {
                                        reviewStatsMap.put(mId, new double[]{0.0, 0.0});
                                    }
                                    double[] stats = reviewStatsMap.get(mId);
                                    if (stats != null) {
                                        stats[0] += ratingValue; // Cộng dồn điểm rating từ object review
                                        stats[1] += 1.0;         // Đếm tăng số lượt review thêm 1
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi khi ép kiểu dữ liệu từ tư liệu Review document ID: " + doc.getId(), e);
                            }
                        }
                    }

                    // Sau khi tổng hợp xong Map từ bảng reviews, lấy bảng movies để kết xuất danh sách cuối cùng
                    db.collection("movies")
                            .addSnapshotListener((movieSnapshots, movieError) -> {
                                if (movieError != null) {
                                    Log.e(TAG, "Lỗi khi lấy dữ liệu movies: ", movieError);
                                    return;
                                }
                                if (movieSnapshots != null) {
                                    int movieCount = movieSnapshots.size();
                                    tvActiveMovies.setText(movieCount + " phim");

                                    hotMovieList.clear();
                                    for (QueryDocumentSnapshot doc : movieSnapshots) {
                                        Movie movie = doc.toObject(Movie.class);

                                        // Gán ID từ document Firestore làm movieId dự phòng nếu field bị trống
                                        if (movie.getMovieId() == null) {
                                            movie.setMovieId(doc.getId());
                                        }

                                        // Đối chiếu tính toán số điểm sao dựa vào Map thống kê Review phía trên
                                        if (reviewStatsMap.containsKey(movie.getMovieId())) {
                                            double[] stats = reviewStatsMap.get(movie.getMovieId());
                                            if (stats != null && stats[1] > 0) {
                                                double rawAvg = stats[0] / stats[1];
                                                // Làm tròn lấy đúng một chữ số sau dấu phẩy (Ví dụ: 4.8)
                                                double roundedAvg = Math.round(rawAvg * 10.0) / 10.0;
                                                movie.setAverageRating(roundedAvg);
                                                movie.setRatingCount((int) stats[1]);
                                            }
                                        } else {
                                            movie.setAverageRating(0.0);
                                            movie.setRatingCount(0);
                                        }

                                        hotMovieList.add(movie);
                                    }

                                    // Sắp xếp danh sách phim theo số sao trung bình giảm dần (Phim Hot lên đầu)
                                    Collections.sort(hotMovieList, (m1, m2) -> Double.compare(m2.getAverageRating(), m1.getAverageRating()));

                                    // Đẩy dữ liệu đã làm mới lên giao diện RecyclerView
                                    hotMovieAdapter.notifyDataSetChanged();
                                }
                            });
                });

        // --- 3. ĐẾM SỐ LƯỢNG TÀI KHOẢN TỪ COLLECTION "users" ---
        db.collection("users")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi khi lấy dữ liệu users: ", error);
                        return;
                    }
                    if (snapshots != null) {
                        int userCount = snapshots.size();
                        tvTotalUsers.setText(userCount + " tài khoản");
                    }
                });
    }

    private void setupAnimations() {

        setTouchAnimation(cardRevenue);
        setTouchAnimation(cardTickets);
        setTouchAnimation(cardUsers);
        setTouchAnimation(cardMovies);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchAnimation(final View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    break;
            }
            return true;
        });
    }

    /**
     * Inner Class Adapter kết xuất danh sách phim có số hiệu Rank đè lên Poster
     */
    private class HotMovieAdapter extends RecyclerView.Adapter<HotMovieAdapter.MovieViewHolder> {

        private final List<Movie> list;

        public HotMovieAdapter(List<Movie> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hot_movie, parent, false);
            return new MovieViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
            Movie movie = list.get(position);

            // Đặt số thứ tự xếp hạng (Bắt đầu hiển thị từ số 1 tăng dần)
            holder.tvRankNumber.setText(String.valueOf(position + 1));

            holder.tvMovieTitle.setText(movie.getTitle());

            // Kết xuất hiển thị: ⭐ 4.5 (12 đánh giá)
            holder.tvRating.setText(String.format(Locale.US, "⭐ %.1f (%d đánh giá)", movie.getAverageRating(), movie.getRatingCount()));

            // Gọi thư viện Glide nạp ảnh từ posterUrl
            if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(movie.getPosterUrl())
                        .placeholder(android.R.color.darker_gray)
                        .into(holder.imgPoster);
            }
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        class MovieViewHolder extends RecyclerView.ViewHolder {
            TextView tvRankNumber, tvMovieTitle, tvRating;
            ImageView imgPoster;

            public MovieViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRankNumber = itemView.findViewById(R.id.tvRankNumber);
                tvMovieTitle = itemView.findViewById(R.id.tvMovieTitle);
                tvRating = itemView.findViewById(R.id.tvRating);
                imgPoster = itemView.findViewById(R.id.imgPoster);
            }
        }
    }
}