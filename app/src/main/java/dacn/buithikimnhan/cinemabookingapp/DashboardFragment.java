package dacn.buithikimnhan.cinemabookingapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardFragment extends Fragment {

    private TextView tvRevenue, tvTotalTickets, tvTotalMovies, tvTotalUsers;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        db = FirebaseFirestore.getInstance();
        tvRevenue = view.findViewById(R.id.tvRevenue);
        tvTotalTickets = view.findViewById(R.id.tvTotalTickets);
        tvTotalMovies = view.findViewById(R.id.tvTotalMovies);
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);

        calculateStatistics();
        return view;
    }

    private void calculateStatistics() {
        // 1. Tính tổng số tiền thu về từ root collection "bookings"
        db.collection("bookings").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots != null) {
                long totalRevenue = 0;
                int ticketCount = queryDocumentSnapshots.size();

                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Long price = doc.getLong("totalPrice"); // Giá trị số chuẩn int/long vừa đổi
                    if (price != null) {
                        totalRevenue += price;
                    }
                }
                tvRevenue.setText(String.format("%,dđ", totalRevenue));
                tvTotalTickets.setText(String.valueOf(ticketCount));
            }
        });

        // 2. Đếm tổng số phim
        db.collection("movies").get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot != null) tvTotalMovies.setText(String.valueOf(querySnapshot.size()));
        });

        // 3. Đếm tổng số người dùng hệ thống
        db.collection("users").get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot != null) tvTotalUsers.setText(String.valueOf(querySnapshot.size()));
        });
    }
}