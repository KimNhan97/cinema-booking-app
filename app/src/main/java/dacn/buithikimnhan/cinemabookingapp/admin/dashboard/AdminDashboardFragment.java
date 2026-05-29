package dacn.buithikimnhan.cinemabookingapp.admin.dashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.auth.LoginActivity;
import dacn.buithikimnhan.cinemabookingapp.data.Movie;
import dacn.buithikimnhan.cinemabookingapp.data.Review;
import dacn.buithikimnhan.cinemabookingapp.data.Showtime;

public class AdminDashboardFragment extends Fragment {

    private static final String TAG = "AdminDashboard";

    private View cardRevenue, cardTickets, cardUsers, cardMovies;
    private TextView tvRevenue, tvTicketsSold, tvTotalUsers, tvActiveMovies;
     AppCompatButton btnAdminLogOut;

    RecyclerView rvHotMovies;
    RecyclerView rvSessions;
    RecyclerView rvBookings;

    private List<Movie> hotMovieList;
    private HotMovieAdapter hotMovieAdapter;
    private List<Showtime> showtimeList;
    private DashboardShowtimeAdapter showtimeAdapter;

    private FirebaseFirestore db;

    public AdminDashboardFragment() {
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
        btnAdminLogOut = view.findViewById(R.id.btnAdminLogOut);

        rvHotMovies = view.findViewById(R.id.rvHotMovies);
        rvSessions = view.findViewById(R.id.rvSessions);
        rvBookings = view.findViewById(R.id.rvBookings);

        if (btnAdminLogOut != null) {
            btnAdminLogOut.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(getContext(), "Admin đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        }

        // Cấu hình RecyclerView Phim hot nhất
        if (rvHotMovies != null) {
            rvHotMovies.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            hotMovieList = new ArrayList<>();
            hotMovieAdapter = new HotMovieAdapter(hotMovieList);
            rvHotMovies.setAdapter(hotMovieAdapter);
        }

        // Cấu hình RecyclerView Suất chiếu hôm nay
        if (rvSessions != null) {
            rvSessions.setLayoutManager(new LinearLayoutManager(getContext()));
            showtimeList = new ArrayList<>();
            showtimeAdapter = new DashboardShowtimeAdapter(showtimeList);
            rvSessions.setAdapter(showtimeAdapter);
        }

        if (rvBookings != null) {
            rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        setupAnimations();
        fetchFirestoreData();
    }

    private void fetchFirestoreData() {
        // 1. Lấy dữ liệu Doanh thu & Vé bán ra từ Bookings
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

        // 2. Lấy đánh giá và danh sách Phim Hot Nhất
        db.collection("reviews")
                .addSnapshotListener((reviewSnapshots, reviewError) -> {
                    if (reviewError != null) {
                        Log.e(TAG, "Lỗi khi lấy dữ liệu reviews: ", reviewError);
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
                                Log.e(TAG, "Lỗi khi ép kiểu Review ID: " + doc.getId(), e);
                            }
                        }
                    }

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

                                        if (movie.getMovieId() == null) {
                                            movie.setMovieId(doc.getId());
                                        }

                                        if (reviewStatsMap.containsKey(movie.getMovieId())) {
                                            double[] stats = reviewStatsMap.get(movie.getMovieId());
                                            if (stats != null && stats[1] > 0) {
                                                double rawAvg = stats[0] / stats[1];
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

                                    Collections.sort(hotMovieList, (m1, m2) -> Double.compare(m2.getAverageRating(), m1.getAverageRating()));
                                    hotMovieAdapter.notifyDataSetChanged();
                                }
                            });
                });

        // 3. Lấy dữ liệu Tổng số tài khoản người dùng
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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());

        Log.d("AdminDashboard", "Ngày hôm nay hệ thống quét: " + todayStr);

        db.collection("showtimes")
                .whereEqualTo("date", todayStr)
                .addSnapshotListener((showtimeSnapshots, showtimeError) -> {
                    if (showtimeError != null) {
                        Log.e(TAG, "Lỗi khi lấy dữ liệu showtimes: ", showtimeError);
                        return;
                    }

                    if (showtimeSnapshots != null) {
                        showtimeList.clear();

                        // Biến đếm để theo dõi tiến trình load tên phim từ collection phụ
                        final int totalShowtimes = showtimeSnapshots.size();
                        if (totalShowtimes == 0) {
                            showtimeAdapter.notifyDataSetChanged();
                            return;
                        }

                        for (QueryDocumentSnapshot doc : showtimeSnapshots) {
                            try {
                                Showtime showtime = doc.toObject(Showtime.class);
                                if (showtime.getShowtimeId() == null) {
                                    showtime.setShowtimeId(doc.getId());
                                }

                                // Tiến hành quét tìm tên phim dựa trên mã movieId của suất chiếu
                                String mId = doc.getString("movieId");
                                if (mId != null && !mId.isEmpty()) {
                                    db.collection("movies").document(mId).get()
                                            .addOnSuccessListener(movieDoc -> {
                                                if (movieDoc.exists()) {
                                                    String actualTitle = movieDoc.getString("title");
                                                    // Set trực tiếp tên phim thật vào thuộc tính ảo để cung cấp cho Adapter đọc
                                                    showtime.setMovieName(actualTitle);
                                                } else {
                                                    showtime.setMovieName("Phim không tồn tại hoặc đã bị xóa");
                                                }

                                                // Thêm vào danh sách và sắp xếp cập nhật lại UI đồng bộ
                                                if (!showtimeList.contains(showtime)) {
                                                    showtimeList.add(showtime);
                                                }

                                                // Thực hiện sắp xếp tăng dần theo giờ chiếu khi một item nạp xong tên phim
                                                Collections.sort(showtimeList, (s1, s2) -> {
                                                    if (s1.getStartTime() == null || s2.getStartTime() == null) return 0;
                                                    return s1.getStartTime().compareTo(s2.getStartTime());
                                                });
                                                showtimeAdapter.notifyDataSetChanged();
                                            })
                                            .addOnFailureListener(e -> {
                                                showtime.setMovieName("Lỗi tải tên phim");
                                                if (!showtimeList.contains(showtime)) {
                                                    showtimeList.add(showtime);
                                                }
                                                showtimeAdapter.notifyDataSetChanged();
                                            });
                                } else {
                                    showtime.setMovieName("Mã phim trống");
                                    showtimeList.add(showtime);
                                    showtimeAdapter.notifyDataSetChanged();
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi xử lý gán dữ liệu Showtime: " + doc.getId(), e);
                            }
                        }
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

            holder.tvRankNumber.setText(String.valueOf(position + 1));
            holder.tvMovieTitle.setText(movie.getTitle());
            holder.tvRating.setText(String.format(Locale.US, "⭐ %.1f (%d đánh giá)", movie.getAverageRating(), movie.getRatingCount()));

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