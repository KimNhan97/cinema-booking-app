package dacn.buithikimnhan.cinemabookingapp.admin.showtime;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Showtime;

public class AdminShowTimeFragment extends Fragment {

    private RecyclerView rvShowtimes;
    EditText edtSearch;
    private Spinner spinnerFilter;
    private FirebaseFirestore db;
    private List<Showtime> fullShowtimeList;
    private List<Showtime> filteredList;
    private ShowtimeAdapter adapter;
    private String currentFilterStatus = "TẤT CẢ";
    private String currentSearchKeyword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_show_time, container, false);

        rvShowtimes = view.findViewById(R.id.rvShowtimes);
        edtSearch = view.findViewById(R.id.edtSearchShowtime);
        spinnerFilter = view.findViewById(R.id.spinnerStatusFilter);
        db = FirebaseFirestore.getInstance();

        fullShowtimeList = new ArrayList<>();
        filteredList = new ArrayList<>();
        rvShowtimes.setLayoutManager(new LinearLayoutManager(getContext()));

        setupSpinnerFilter();
        listenToShowtimesRealtime();

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchKeyword = s.toString().toLowerCase().trim();
                applyFilterAndSearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        view.findViewById(R.id.fabAddShowtime).setOnClickListener(v -> showAddEditDialog(null));

        return view;
    }

    private void setupSpinnerFilter() {
        String[] statuses = {"TẤT CẢ", "open", "close"};
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, statuses);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(spinAdapter);
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilterStatus = statuses[position];
                applyFilterAndSearch();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void listenToShowtimesRealtime() {
        db.collection("showtimes").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            fullShowtimeList.clear();
            for (QueryDocumentSnapshot doc : value) {
                Showtime showtime = doc.toObject(Showtime.class);
                showtime.setShowtimeId(doc.getId());
                showtime.setMovieName("Đang tải..."); // Trạng thái chờ ban đầu

                fetchMovieNameAndPopulate(showtime);
                fullShowtimeList.add(showtime);
            }
            applyFilterAndSearch();
        });
    }

    private void fetchMovieNameAndPopulate(Showtime showtime) {
        if (showtime.getMovieId() == null || showtime.getMovieId().isEmpty()) {
            showtime.setMovieName("Không rõ phim");
            applyFilterAndSearch();
            return;
        }
        db.collection("movies").document(showtime.getMovieId()).get().addOnSuccessListener(docSnap -> {
            if (docSnap.exists()) {
                String name = docSnap.getString("name");
                if (name == null) name = docSnap.getString("title");
                showtime.setMovieName(name != null ? name : showtime.getMovieId());
            } else {
                showtime.setMovieName("ID: " + showtime.getMovieId());
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void applyFilterAndSearch() {
        filteredList.clear();
        for (Showtime item : fullShowtimeList) {
            boolean matchesStatus = currentFilterStatus.equals("TẤT CẢ") ||
                    (item.getStatus() != null && item.getStatus().equalsIgnoreCase(currentFilterStatus));

            String movieText = item.getMovieName() != null ? item.getMovieName().toLowerCase() : "";
            String roomText = item.getRoom() != null ? item.getRoom().toLowerCase() : "";
            String timeText = item.getStartTime() != null ? item.getStartTime().toLowerCase() : "";

            boolean matchesSearch = movieText.contains(currentSearchKeyword)
                    || roomText.contains(currentSearchKeyword)
                    || timeText.contains(currentSearchKeyword);

            if (matchesStatus && matchesSearch) {
                filteredList.add(item);
            }
        }
        if (adapter == null) {
            adapter = new ShowtimeAdapter(filteredList);
            rvShowtimes.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    // --- HÀM TRỢ GIÚP CHUYỂN ĐỔI ĐỊNH DẠNG "HH:mm" SANG SỐ PHÚT ĐỂ SO SÁNH TOÁN HỌC ---
    private int convertTimeToMinutes(String timeStr) {
        try {
            String[] parts = timeStr.trim().split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours * 60 + minutes;
        } catch (Exception e) {
            return -1; // Định dạng thời gian không hợp lệ
        }
    }

    private void showAddEditDialog(@Nullable Showtime showtime) {
        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_edit_showtime, null);
        dialog.setContentView(view);

        TextView title = view.findViewById(R.id.txtDialogTitle);
        Spinner spinnerMovie = view.findViewById(R.id.dialogSpinnerMovie);
        EditText edtRoom = view.findViewById(R.id.dialogRoom);
        EditText edtDate = view.findViewById(R.id.dialogDate);
        EditText edtStart = view.findViewById(R.id.dialogStartTime);
        EditText edtEnd = view.findViewById(R.id.dialogEndTime);
        EditText edtAvail = view.findViewById(R.id.dialogAvailableSeats);
        EditText edtTotal = view.findViewById(R.id.dialogTotalSeats);
        RadioButton radOpen = view.findViewById(R.id.radOpen);
        RadioButton radClose = view.findViewById(R.id.radClose);

        List<MovieSpinnerItem> movieSpinnerList = new ArrayList<>();
        ArrayAdapter<MovieSpinnerItem> movieSpinnerAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, movieSpinnerList);
        movieSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMovie.setAdapter(movieSpinnerAdapter);

        // Đổ dữ liệu text trước
        if (showtime != null) {
            title.setText("CHỈNH SỬA THÔNG TIN SUẤT CHIẾU");
            edtRoom.setText(showtime.getRoom());
            edtDate.setText(showtime.getDate());
            edtStart.setText(showtime.getStartTime());
            edtEnd.setText(showtime.getEndTime());
            edtAvail.setText(String.valueOf(showtime.getAvailableSeats()));
            edtTotal.setText(String.valueOf(showtime.getTotalSeats()));
            if ("open".equals(showtime.getStatus())) radOpen.setChecked(true);
            else radClose.setChecked(true);
        } else {
            radOpen.setChecked(true);
        }

        // Tải danh sách phim và gán chính xác vị trí Spinner kể cả khi đang sửa
        db.collection("movies").get().addOnSuccessListener(queryDocumentSnapshots -> {
            movieSpinnerList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String mName = doc.getString("name");
                if (mName == null) mName = doc.getString("title");
                if (mName == null) mName = doc.getId();
                movieSpinnerList.add(new MovieSpinnerItem(doc.getId(), mName));
            }
            movieSpinnerAdapter.notifyDataSetChanged();

            if (showtime != null && showtime.getMovieId() != null) {
                for (int i = 0; i < movieSpinnerList.size(); i++) {
                    if (movieSpinnerList.get(i).getId().equals(showtime.getMovieId())) {
                        spinnerMovie.setSelection(i);
                        break;
                    }
                }
            }
        });

        view.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
            if (spinnerMovie.getSelectedItem() == null) {
                Toast.makeText(getContext(), "Đang tải dữ liệu phim...", Toast.LENGTH_SHORT).show();
                return;
            }

            String mid = ((MovieSpinnerItem) spinnerMovie.getSelectedItem()).getId();
            String room = edtRoom.getText().toString().trim();
            String date = edtDate.getText().toString().trim();
            String start = edtStart.getText().toString().trim();
            String end = edtEnd.getText().toString().trim();
            String availStr = edtAvail.getText().toString().trim();
            String totalStr = edtTotal.getText().toString().trim();
            String status = radOpen.isChecked() ? "open" : "close";

            if(room.isEmpty() || date.isEmpty() || start.isEmpty() || end.isEmpty() || availStr.isEmpty() || totalStr.isEmpty()){
                Toast.makeText(getContext(), "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Chuyển đổi mốc thời gian nhập vào sang số phút
            int newStartMin = convertTimeToMinutes(start);
            int newEndMin = convertTimeToMinutes(end);

            if (newStartMin == -1 || newEndMin == -1) {
                Toast.makeText(getContext(), "Thời gian không đúng định dạng HH:mm (VD: 14:30)!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newStartMin >= newEndMin) {
                Toast.makeText(getContext(), "Thời gian bắt đầu phải nhỏ hơn thời gian kết thúc!", Toast.LENGTH_SHORT).show();
                return;
            }

            // ================= KHU VỰC THUẬT TOÁN: KIỂM TRA TRÙNG PHÒNG + NGÀY + GIỜ CHIẾU CHUẨN HÓA =================
            boolean isOverlapped = false;
            String conflictMovie = "";
            String conflictTimeRange = "";

            // 1. Chuẩn hóa chuỗi dữ liệu đầu vào người dùng vừa điền
            String cleanInputRoom = room.trim().replace(" ", "").toLowerCase();
            String cleanInputDate = date.trim().replace("/", "-"); // Ép hết dấu gạch chéo thành gạch ngang

            for (Showtime existing : fullShowtimeList) {
                // Nếu đang chỉnh sửa, bỏ qua việc tự so sánh trùng với chính bản thân nó
                if (showtime != null && existing.getShowtimeId().equals(showtime.getShowtimeId())) {
                    continue;
                }

                // 2. Chuẩn hóa dữ liệu có sẵn lấy lên từ Firebase để đối sánh chính xác
                String cleanExistRoom = existing.getRoom() != null ? existing.getRoom().trim().replace(" ", "").toLowerCase() : "";
                String cleanExistDate = existing.getDate() != null ? existing.getDate().trim().replace("/", "-") : "";

                // 3. TIẾN HÀNH KIỂM TRA: Chỉ xét khi TRÙNG TÊN PHÒNG và TRÙNG NGÀY CHIẾU sau khi đã clean sạch chuỗi
                if (cleanExistRoom.equals(cleanInputRoom) && cleanExistDate.equals(cleanInputDate)) {

                    int existStartMin = convertTimeToMinutes(existing.getStartTime());
                    int existEndMin = convertTimeToMinutes(existing.getEndTime());

                    if (existStartMin != -1 && existEndMin != -1) {
                        // Công thức kiểm tra khoảng thời gian chồng lấn:
                        // (Bắt đầu mới < Kết thúc cũ) VÀ (Kết thúc mới > Bắt đầu cũ)
                        if (newStartMin < existEndMin && newEndMin > existStartMin) {
                            isOverlapped = true;
                            conflictMovie = existing.getMovieName();
                            conflictTimeRange = existing.getStartTime() + " ~ " + existing.getEndTime();
                            break;
                        }
                    }
                }
            }

            if (isOverlapped) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Trùng lịch phòng chiếu! ⚠️")
                        .setMessage("Không thể lưu! Phòng '" + room + "' vào ngày " + date + " đã được xếp lịch cho phim:\n\n" +
                                "🎬 Phim: " + conflictMovie + "\n" +
                                "⏰ Thời gian: " + conflictTimeRange + "\n\n" +
                                "Vui lòng chọn tên phòng khác hoặc thay đổi khung giờ chiếu.")
                        .setPositiveButton("ĐÃ HIỂU", null)
                        .show();
                return; // Kết thúc hàm tại đây, chặn không cho ghi dữ liệu đè lên Firestore
            }
            // ==============================================================================================

            DocumentReference docRef = (showtime == null) ?
                    db.collection("showtimes").document() : db.collection("showtimes").document(showtime.getShowtimeId());

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("movieId", mid);
            dataMap.put("room", room);
            dataMap.put("date", date);
            dataMap.put("startTime", start);
            dataMap.put("endTime", end);
            dataMap.put("availableSeats", Integer.parseInt(availStr));
            dataMap.put("totalSeats", Integer.parseInt(totalStr));
            dataMap.put("status", status);

            docRef.set(dataMap).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showSeatsStatusDialog(Showtime showtime) {
        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_admin_view_seats, null);
        dialog.setContentView(dialogView);

        TextView txtTitle = dialogView.findViewById(R.id.txtSeatDialogTitle);
        TextView txtAvailableList = dialogView.findViewById(R.id.txtAvailableSeatsList);
        TextView txtBookedList = dialogView.findViewById(R.id.txtBookedSeatsList);
        ProgressBar loading = dialogView.findViewById(R.id.pbSeatsLoading);

        txtTitle.setText("Trạng Thái Ghế - Phòng: " + showtime.getRoom());
        txtAvailableList.setText("Đang tải...");
        txtBookedList.setText("Đang tải...");

        db.collection("showtimes").document(showtime.getShowtimeId()).collection("seats")
                .get().addOnSuccessListener(querySnapshots -> {
                    loading.setVisibility(View.GONE);
                    List<String> availableSeats = new ArrayList<>();
                    List<String> bookedSeats = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        String seatName = doc.getId();
                        String seatStatus = doc.getString("status");

                        if (seatStatus != null) {
                            String cleanStatus = seatStatus.toLowerCase().trim();
                            if (cleanStatus.equals("booked")) {
                                bookedSeats.add(seatName);
                            } else {
                                availableSeats.add(seatName);
                            }
                        } else {
                            availableSeats.add(seatName);
                        }
                    }

                    if (availableSeats.isEmpty()) {
                        txtAvailableList.setText("(Hết ghế trống)");
                    } else {
                        txtAvailableList.setText(android.text.TextUtils.join(", ", availableSeats));
                    }

                    if (bookedSeats.isEmpty()) {
                        txtBookedList.setText("(Chưa có ghế đặt)");
                    } else {
                        txtBookedList.setText(android.text.TextUtils.join(", ", bookedSeats));
                    }

                }).addOnFailureListener(e -> {
                    loading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi khi nạp danh sách cấu trúc ghế!", Toast.LENGTH_SHORT).show();
                });

        dialogView.findViewById(R.id.btnFormatDialogClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void confirmDeleteShowtime(Showtime showtime) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn chắc chắn muốn xóa dữ liệu suất chiếu này?")
                .setNegativeButton("HỦY", null)
                .setPositiveButton("XÓA BỎ", (dialog, which) -> {
                    db.collection("showtimes").document(showtime.getShowtimeId()).delete().addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(), "Đã xóa suất chiếu thành công!", Toast.LENGTH_SHORT).show());
                }).show();
    }

    private class ShowtimeAdapter extends RecyclerView.Adapter<ShowtimeAdapter.ShowtimeViewHolder> {
        private List<Showtime> items;
        public ShowtimeAdapter(List<Showtime> items) { this.items = items; }

        @NonNull
        @Override
        public ShowtimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_showtime, parent, false);
            return new ShowtimeViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ShowtimeViewHolder holder, int position) {
            Showtime showtime = items.get(position);

            holder.txtMovieName.setText(showtime.getMovieName());
            holder.txtRoom.setText("Phòng: " + showtime.getRoom());
            holder.txtDate.setText("Ngày: " + showtime.getDate());
            holder.txtTime.setText("Thời gian: " + showtime.getStartTime() + " ~ " + showtime.getEndTime());
            holder.txtRatio.setText(showtime.getAvailableSeats() + " / " + showtime.getTotalSeats());

            String statusText = showtime.getStatus() != null ? showtime.getStatus().toUpperCase() : "CLOSE";
            holder.txtStatus.setText(statusText.equals("OPEN") ? "ĐANG MỞ" : "ĐÓNG CỬA");

            if ("OPEN".equals(statusText)) {
                holder.txtStatus.setTextColor(Color.parseColor("#2F855A"));
                holder.txtStatus.setBackgroundColor(Color.parseColor("#C6F6D5"));
            } else {
                holder.txtStatus.setTextColor(Color.parseColor("#C53030"));
                holder.txtStatus.setBackgroundColor(Color.parseColor("#FED7D7"));
            }

            holder.progress.setMax(showtime.getTotalSeats());
            holder.progress.setProgress(showtime.getAvailableSeats());

            holder.btnEdit.setOnClickListener(v -> showAddEditDialog(showtime));
            holder.btnDelete.setOnClickListener(v -> confirmDeleteShowtime(showtime));
            holder.btnSeats.setOnClickListener(v -> showSeatsStatusDialog(showtime));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ShowtimeViewHolder extends RecyclerView.ViewHolder {
            TextView txtMovieName, txtRoom, txtDate, txtTime, txtRatio, txtStatus;
            ProgressBar progress;
            Button btnEdit, btnDelete, btnSeats;

            public ShowtimeViewHolder(@NonNull View v) {
                super(v);
                txtMovieName = v.findViewById(R.id.txtMovieName);
                txtRoom = v.findViewById(R.id.txtRoomName);
                txtDate = v.findViewById(R.id.txtShowDate);
                txtTime = v.findViewById(R.id.txtShowTimeRange);
                txtRatio = v.findViewById(R.id.txtSeatRatio);
                txtStatus = v.findViewById(R.id.txtStatusBadge);
                progress = v.findViewById(R.id.progressSeats);
                btnEdit = v.findViewById(R.id.btnEditShowtime);
                btnDelete = v.findViewById(R.id.btnDeleteShowtime);
                btnSeats = v.findViewById(R.id.btnViewSeats);
            }
        }
    }
}