package dacn.buithikimnhan.cinemabookingapp.admin.booking;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Booking;

public class AdminBookingFragment extends Fragment {

    private BookingAdapter adapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference bookingsRef = db.collection("bookings");
     List<Booking> originalList = new ArrayList<>();

    private String currentStatusFilter = "Tất cả";

    private EditText edtSearch;
     FloatingActionButton btnScanQR;
    private RecyclerView rvBookings;

    private TextView tvCountBooked, tvCountCheckedIn, tvCountCancelled;
    private TextView tabAll, tabBooked, tabCheckIn, tabCancelled;

    public AdminBookingFragment() {
    }

    public static AdminBookingFragment newInstance() {
        return new AdminBookingFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_booking, container, false);
        edtSearch = view.findViewById(R.id.edtSearch);
        btnScanQR = view.findViewById(R.id.btnScanQR);
        rvBookings = view.findViewById(R.id.rvBookings);

        tvCountBooked = view.findViewById(R.id.tvCountBooked);
        tvCountCheckedIn = view.findViewById(R.id.tvCountCheckedIn);
        tvCountCancelled = view.findViewById(R.id.tvCountCancelled);

        tabAll = view.findViewById(R.id.tabAll);
        tabBooked = view.findViewById(R.id.tabBooked);
        tabCheckIn = view.findViewById(R.id.tabCheckIn);
        tabCancelled = view.findViewById(R.id.tabCancelled);

        setupRecyclerView();
        setupTabFilters();
        listenToDataRealtime();

        // Lắng nghe ô tìm kiếm
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Quét QR Code soát vé
        btnScanQR.setOnClickListener(v -> {
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(AdminBookingFragment.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Di chuyển camera vào mã QR vé của khách");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        });

        return view;
    }

    private void setupRecyclerView() {
        adapter = new BookingAdapter(getContext(), new ArrayList<>());
        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBookings.setAdapter(adapter);
        rvBookings.setHasFixedSize(true);
        rvBookings.setNestedScrollingEnabled(true);
    }

    private void setupTabFilters() {
        tabAll.setOnClickListener(v -> updateTabSelection("Tất cả", tabAll));
        tabBooked.setOnClickListener(v -> updateTabSelection("booked", tabBooked));
        tabCheckIn.setOnClickListener(v -> updateTabSelection("checked_in", tabCheckIn));
        tabCancelled.setOnClickListener(v -> updateTabSelection("cancelled", tabCancelled));
    }

    private void updateTabSelection(String filterType, TextView selectedTab) {
        currentStatusFilter = filterType;

        TextView[] tabs = {tabAll, tabBooked, tabCheckIn, tabCancelled};
        for (TextView tab : tabs) {
            tab.setTextColor(android.graphics.Color.parseColor("#424242"));

            android.graphics.drawable.GradientDrawable normalShape = new android.graphics.drawable.GradientDrawable();
            normalShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            normalShape.setCornerRadius(dpToPx(8));
            normalShape.setColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            normalShape.setStroke(2, ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            tab.setBackground(normalShape);

            tab.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));
        }
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        android.graphics.drawable.GradientDrawable activeShape = new android.graphics.drawable.GradientDrawable();
        activeShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        activeShape.setCornerRadius(dpToPx(8));
        activeShape.setColor(0xFFA62B2B);

        selectedTab.setBackground(activeShape);
        selectedTab.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));

        applyFiltersAndSort();
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void listenToDataRealtime() {
        bookingsRef.addSnapshotListener((snapshots, error) -> {
            if (error != null) return;
            originalList.clear();
            int countBooked = 0;
            int countCheckIn = 0;
            int countCancelled = 0;

            if (snapshots != null) {
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                    Booking booking = doc.toObject(Booking.class);
                    originalList.add(booking);

                    String status = booking.getStatus();
                    if (status != null) {
                        switch (status.toLowerCase()) {
                            case "booked": countBooked++; break;
                            case "checked_in": countCheckIn++; break;
                            case "cancelled": countCancelled++; break;
                        }
                    }
                }
            }

            if (isAdded()) {
                tvCountBooked.setText(String.valueOf(countBooked));
                tvCountCheckedIn.setText(String.valueOf(countCheckIn));
                tvCountCancelled.setText(String.valueOf(countCancelled));
            }

            applyFiltersAndSort();
        });
    }

    private void applyFiltersAndSort() {
        List<Booking> filteredList = new ArrayList<>();
        String keyword = edtSearch.getText().toString().trim().toLowerCase();

        for (Booking item : originalList) {
            if (!"Tất cả".equals(currentStatusFilter)) {
                if (item.getStatus() == null || !item.getStatus().equalsIgnoreCase(currentStatusFilter)) {
                    continue;
                }
            }

            // Bộ lọc tìm kiếm bằng chữ
            if (!keyword.isEmpty()) {
                String title = (item.getMovieTitle() != null ? item.getMovieTitle() : "").toLowerCase();
                String bId = (item.getBookingId() != null ? item.getBookingId() : "").toLowerCase();
                String uId = (item.getUserId() != null ? item.getUserId() : "").toLowerCase();

                if (!title.contains(keyword) && !bId.contains(keyword) && !uId.contains(keyword)) {
                    continue;
                }
            }

            filteredList.add(item);
        }

        // Sắp xếp giảm dần theo ngày chiếu và giờ chiếu
        Collections.sort(filteredList, (o1, o2) -> {
            String d1 = o1.getDate();
            String d2 = o2.getDate();
            int dateCompare = (d2 != null ? d2 : "").compareTo(d1 != null ? d1 : "");
            if (dateCompare != 0) return dateCompare;

            String t1 = o1.getStartTime();
            String t2 = o2.getStartTime();
            return (t2 != null ? t2 : "").compareTo(t1 != null ? t1 : "");
        });

        adapter.updateData(filteredList);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(getContext(), "Đã hủy quét", Toast.LENGTH_SHORT).show();
            } else {
                handleQRCheckIn(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleQRCheckIn(String bookingId) {
        bookingsRef.document(bookingId).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String currentStatus = document.getString("status");

                if ("booked".equals(currentStatus)) {
                    bookingsRef.document(bookingId).update("status", "checked_in")
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "VÉ HỢP LỆ! Xác nhận check-in thành công.", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else if ("checked_in".equals(currentStatus)) {
                    Toast.makeText(getContext(), "CẢNH BÁO: Vé đã quét check-in trước đó!", Toast.LENGTH_LONG).show();
                } else if ("cancelled".equals(currentStatus)) {
                    Toast.makeText(getContext(), "CẢNH BÁO: Vé đã bị hủy trên hệ thống!", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "LỖI: Mã vé không tồn tại!", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}