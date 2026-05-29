package dacn.buithikimnhan.cinemabookingapp.admin.showtime;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private int selectedMovieDurationMinutes = 120;

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
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, statuses);
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
                if (showtime != null) {
                    showtime.setShowtimeId(doc.getId());
                    showtime.setMovieName("Đang tải...");
                    fetchMovieNameAndPopulate(showtime);
                    fullShowtimeList.add(showtime);
                }
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
                showtime.setMovieName(name != null ? name : "Chưa đặt tên");
            } else {
                showtime.setMovieName("ID cũ: " + showtime.getMovieId());
            }
            applyFilterAndSearch();
        });

        db.collection("showtimes").document(showtime.getShowtimeId()).collection("seats")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    int schemaTotalSeats = 81;
                    int bookedCount = 0;

                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        schemaTotalSeats = querySnapshots.size();
                        for (QueryDocumentSnapshot doc : querySnapshots) {
                            String seatStatus = doc.getString("status");
                            if (seatStatus != null && seatStatus.equalsIgnoreCase("booked")) {
                                bookedCount++;
                            }
                        }
                    }

                    int realAvailable = schemaTotalSeats - bookedCount;
                    showtime.setTotalSeats(schemaTotalSeats);
                    showtime.setAvailableSeats(Math.max(realAvailable, 0));

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
            String dateText = item.getDate() != null ? item.getDate().toLowerCase() : "";

            // Thêm bộ lọc so khớp tìm kiếm mở rộng cho cả chuỗi thông tin ngày tháng năm
            boolean matchesSearch = movieText.contains(currentSearchKeyword)
                    || roomText.contains(currentSearchKeyword)
                    || timeText.contains(currentSearchKeyword)
                    || dateText.contains(currentSearchKeyword);

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

    private int convertTimeToMinutes(String timeStr) {
        try {
            String[] parts = timeStr.trim().split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours * 60 + minutes;
        } catch (Exception e) {
            return -1;
        }
    }

    private void showAddEditDialog(@Nullable Showtime showtime) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth);
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

        edtEnd.setEnabled(false);
        edtEnd.setFocusable(false);
        edtEnd.setHint("Hệ thống tự tính toán...");

        List<MovieSpinnerItem> movieSpinnerList = new ArrayList<>();
        ArrayAdapter<MovieSpinnerItem> movieSpinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, movieSpinnerList);
        movieSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMovie.setAdapter(movieSpinnerAdapter);

        if (showtime != null) {
            title.setText("CHỈNH SỬA THÔNG TIN SUẤT CHIẾU");
            edtRoom.setText(showtime.getRoom());
            edtDate.setText(showtime.getDate());
            edtStart.setText(showtime.getStartTime());
            edtEnd.setText(showtime.getEndTime());
            edtAvail.setText(String.valueOf(showtime.getAvailableSeats()));
            edtTotal.setText(String.valueOf(showtime.getTotalSeats()));
            if ("open".equalsIgnoreCase(showtime.getStatus())) radOpen.setChecked(true);
            else radClose.setChecked(true);
        } else {
            title.setText("THÊM SUẤT CHIẾU MỚI");
            radOpen.setChecked(true);
            edtAvail.setText("81");
            edtTotal.setText("81");
        }

        db.collection("movies").get().addOnSuccessListener(queryDocumentSnapshots -> {
            movieSpinnerList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String mName = doc.getString("name");
                if (mName == null) mName = doc.getString("title");
                if (mName == null) mName = doc.getId();

                int duration = 120;
                if (doc.contains("duration")) {
                    Object durObj = doc.get("duration");
                    if (durObj instanceof Long) {
                        duration = ((Long) durObj).intValue();
                    } else if (durObj instanceof String) {
                        try {
                            duration = Integer.parseInt(((String) durObj).replaceAll("[^0-9]", ""));
                        } catch (Exception e) { duration = 120; }
                    }
                }
                movieSpinnerList.add(new MovieSpinnerItem(doc.getId(), mName, duration));
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

        spinnerMovie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MovieSpinnerItem selectedItem = movieSpinnerList.get(position);
                selectedMovieDurationMinutes = selectedItem.getDuration();
                calculateEndTime(edtStart.getText().toString(), edtEnd);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        edtStart.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateEndTime(s.toString(), edtEnd);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        view.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
            if (spinnerMovie.getSelectedItem() == null) {
                Toast.makeText(getContext(), "Vui lòng đợi dữ liệu phim!", Toast.LENGTH_SHORT).show();
                return;
            }

            String mid = ((MovieSpinnerItem) spinnerMovie.getSelectedItem()).getId();
            String room = edtRoom.getText().toString().trim();
            String rawDate = edtDate.getText().toString().trim();
            String start = edtStart.getText().toString().trim();
            String end = edtEnd.getText().toString().trim();
            String status = radOpen.isChecked() ? "open" : "close";

            if (room.isEmpty() || rawDate.isEmpty() || start.isEmpty() || end.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin mẫu!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Đồng bộ chuyển đổi ký tự phân tách ngày thành chuẩn gạch chéo
            String formattedDate = rawDate.replace("-", "/");
            Date parsedDate;
            try {
                SimpleDateFormat inputParser;
                if (formattedDate.matches("\\d{4}/\\d{2}/\\d{2}")) {
                    inputParser = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                } else {
                    inputParser = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                }
                inputParser.setLenient(false);
                parsedDate = inputParser.parse(formattedDate);

                SimpleDateFormat targetFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                formattedDate = targetFormatter.format(parsedDate);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Ngày sai định dạng! Vui lòng nhập chuẩn Ngày/Tháng/Năm", Toast.LENGTH_LONG).show();
                return;
            }
            Calendar calToday = Calendar.getInstance();
            calToday.set(Calendar.HOUR_OF_DAY, 0);
            calToday.set(Calendar.MINUTE, 0);
            calToday.set(Calendar.SECOND, 0);
            calToday.set(Calendar.MILLISECOND, 0);
            Date todayDate = calToday.getTime();

            if (parsedDate.before(todayDate)) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Ngày không hợp lệ! 📅")
                        .setMessage("Không thể xếp lịch hoặc lưu suất chiếu cho một ngày thuộc về quá khứ (" + formattedDate + ").")
                        .setPositiveButton("SỬA LẠI NGÀY", null)
                        .show();
                return;
            }

            int newStartMin = convertTimeToMinutes(start);
            int newEndMin = convertTimeToMinutes(end);

            if (newStartMin == -1 || newEndMin == -1) {
                Toast.makeText(getContext(), "Định dạng giờ (HH:mm) không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }


            boolean isOverlapped = false;
            String conflictMovie = "";
            String conflictTimeRange = "";
            String cleanInputRoom = room.trim().replace(" ", "").toLowerCase();
            int CLEANUP_BUFFER = 30; // 30 phút dọn dẹp rạp

            for (Showtime existing : fullShowtimeList) {
                if (showtime != null && existing.getShowtimeId().equals(showtime.getShowtimeId())) {
                    continue;
                }
                String cleanExistRoom = existing.getRoom() != null ? existing.getRoom().trim().replace(" ", "").toLowerCase() : "";
                String cleanExistDate = existing.getDate() != null ? existing.getDate().trim().replace("-", "/") : "";

                // So sánh nếu trùng phòng chiếu và diễn ra trong cùng một ngày
                if (cleanExistRoom.equals(cleanInputRoom) && cleanExistDate.equals(formattedDate)) {
                    int existStartMin = convertTimeToMinutes(existing.getStartTime());
                    int existEndMin = convertTimeToMinutes(existing.getEndTime());

                    if (existStartMin != -1 && existEndMin != -1) {
                        // Suất mới an toàn trước nếu: kết thúc mới + 30p <= bắt đầu cũ
                        boolean isSafeBefore = (newEndMin + CLEANUP_BUFFER) <= existStartMin;
                        // Suất mới an toàn sau nếu: bắt đầu mới >= kết thúc cũ + 30p
                        boolean isSafeAfter = newStartMin >= (existEndMin + CLEANUP_BUFFER);

                        // Nếu vi phạm khoảng trống dọn rạp 30 phút, đánh dấu lỗi trùng lịch tức thì
                        if (!isSafeBefore && !isSafeAfter) {
                            isOverlapped = true;
                            conflictMovie = existing.getMovieName() != null ? existing.getMovieName() : "Phim khác";
                            conflictTimeRange = existing.getStartTime() + " ~ " + existing.getEndTime();
                            break;
                        }
                    }
                }
            }

            if (isOverlapped) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Trùng lịch hoặc thiếu thời gian dọn rạp! ⚠️")
                        .setMessage("Không thể lưu lịch phòng chiếu!\n\n" +
                                "Phòng: '" + room + "' ngày " + formattedDate + "\n" +
                                "Đang bị vướng bởi suất của phim: \"" + conflictMovie + "\"\n" +
                                "Khung giờ chiếu: " + conflictTimeRange + "\n\n" +
                                "👉 Quy định: Suất chiếu mới tạo phải kết thúc cách giờ mở màn suất tiếp theo ít nhất 30 phút để dọn dẹp rạp!")
                        .setPositiveButton("ĐÃ HIỂU", null)
                        .show();
                return;
            }

            boolean isNewShowtime = (showtime == null);
            DocumentReference docRef = isNewShowtime ?
                    db.collection("showtimes").document() : db.collection("showtimes").document(showtime.getShowtimeId());

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("movieId", mid);
            dataMap.put("room", room);
            dataMap.put("date", formattedDate);
            dataMap.put("startTime", start);
            dataMap.put("endTime", end);
            dataMap.put("availableSeats", isNewShowtime ? 81 : Integer.parseInt(edtAvail.getText().toString().trim()));
            dataMap.put("totalSeats", isNewShowtime ? 81 : Integer.parseInt(edtTotal.getText().toString().trim()));
            dataMap.put("status", status);

            docRef.set(dataMap).addOnSuccessListener(aVoid -> {
                if (isNewShowtime) {
                    WriteBatch batch = db.batch();
                    String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H", "J"};
                    CollectionReference seatsRef = docRef.collection("seats");

                    for (String rowLetter : rows) {
                        for (int colNum = 1; colNum <= 9; colNum++) {
                            String seatName = rowLetter + colNum;
                            Map<String, Object> seatData = new HashMap<>();
                            seatData.put("status", "available");
                            seatData.put("price", 60000);
                            batch.set(seatsRef.document(seatName), seatData);
                        }
                    }
                    batch.commit().addOnSuccessListener(aVoidBatch -> {
                        Toast.makeText(getContext(), "Thêm suất chiếu thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                } else {
                    Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void calculateEndTime(String startTimeStr, EditText edtEndTarget) {
        if (startTimeStr == null || !startTimeStr.contains(":") || startTimeStr.trim().length() < 4) {
            edtEndTarget.setText("");
            return;
        }
        try {
            String[] parts = startTimeStr.trim().split(":");
            int startHours = Integer.parseInt(parts[0]);
            int startMinutes = Integer.parseInt(parts[1]);
            int totalEndMinutes = (startHours * 60) + startMinutes + selectedMovieDurationMinutes;

            int endHours = (totalEndMinutes / 60) % 24;
            int endMinutes = totalEndMinutes % 60;

            edtEndTarget.setText(String.format(Locale.getDefault(), "%02d:%02d", endHours, endMinutes));
        } catch (Exception e) {
            edtEndTarget.setText("");
        }
    }

    private void showSeatsStatusDialog(Showtime showtime) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_NoActionBar_MinWidth);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_admin_view_seats, null);
        dialog.setContentView(dialogView);

        TextView txtTitle = dialogView.findViewById(R.id.txtSeatDialogTitle);
        TextView txtAvailableList = dialogView.findViewById(R.id.txtAvailableSeatsList);
        TextView txtBookedList = dialogView.findViewById(R.id.txtBookedSeatsList);
        ProgressBar loading = dialogView.findViewById(R.id.pbSeatsLoading);
        Button btnClose = dialogView.findViewById(R.id.btnFormatDialogClose);

        txtTitle.setText("Trạng Thái Ghế - Phòng: " + showtime.getRoom());

        db.collection("showtimes").document(showtime.getShowtimeId()).collection("seats")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    loading.setVisibility(View.GONE);
                    List<String> bookedSeats = new ArrayList<>();
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : querySnapshots) {
                            if ("booked".equalsIgnoreCase(doc.getString("status"))) {
                                bookedSeats.add(doc.getId());
                            }
                        }
                    }

                    List<String> availableSeats = new ArrayList<>();
                    String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H", "J"};
                    for (String r : rows) {
                        for (int c = 1; c <= 9; c++) {
                            String seat = r + c;
                            if (!bookedSeats.contains(seat)) availableSeats.add(seat);
                        }
                    }

                    txtAvailableList.setText(availableSeats.isEmpty() ? "Hết ghế trống" : TextUtils.join(", ", availableSeats));
                    txtBookedList.setText(bookedSeats.isEmpty() ? "Chưa có ghế đặt" : TextUtils.join(", ", bookedSeats));
                    btnClose.setOnClickListener(v -> dialog.dismiss());
                });
        dialog.show();
    }

    private void confirmDeleteShowtime(Showtime showtime) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa ❌")
                .setMessage("Bạn có chắc chắn muốn xóa suất chiếu của phim '" + showtime.getMovieName() + "' không?")
                .setNegativeButton("HỦY", null)
                .setPositiveButton("XÓA BỎ", (dialog, which) -> {
                    db.collection("showtimes").document(showtime.getShowtimeId()).delete().addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(), "Đã xóa suất chiếu thành công!", Toast.LENGTH_SHORT).show());
                }).show();
    }

    private class ShowtimeAdapter extends RecyclerView.Adapter<ShowtimeAdapter.ShowtimeViewHolder> {
        private final List<Showtime> items;
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