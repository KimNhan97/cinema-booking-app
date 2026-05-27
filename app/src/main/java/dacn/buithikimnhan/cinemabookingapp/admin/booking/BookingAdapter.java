package dacn.buithikimnhan.cinemabookingapp.admin.booking;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Booking;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private final Context context;
    private List<Booking> bookingList;

    public BookingAdapter(Context context, List<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        String id = booking.getBookingId();
        String title = booking.getMovieTitle();
        long price = booking.getTotalPrice();
        String status = booking.getStatus();
        String room = booking.getRoom();
        String startTime = booking.getStartTime();
        List<String> seats = booking.getSeats();

        holder.tvMovieTitle.setText(title != null ? title : "");
        holder.tvBookingId.setText("Mã vé: " + (id != null ? id : ""));
        holder.tvPrice.setText(price + "đ");

        if (room != null && startTime != null) {
            holder.tvTime.setText(room + " - " + startTime);
        } else {
            holder.tvTime.setText("");
        }

        if (seats != null && !seats.isEmpty()) {
            holder.tvSeats.setText(String.join(", ", seats));
        } else {
            holder.tvSeats.setText("");
        }

        // ĐỒNG BỘ MÀU SẮC ĐỒNG ĐỀU (Thanh Indicator bên trái trùng khít màu nhãn Tag trạng thái)
        if (status != null) {
            switch (status.toLowerCase()) {
                case "booked":
                    holder.tvStatus.setText("BOOKED");
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#B57C1E"));
                    holder.viewStatusIndicator.setBackgroundColor(android.graphics.Color.parseColor("#FFB300"));
                    holder.tvStatus.setBackgroundResource(android.R.drawable.toast_frame);
                    holder.tvStatus.getBackground().setTint(android.graphics.Color.parseColor("#FFF8E1"));
                    break;

                case "checked_in":
                    holder.tvStatus.setText("CHECKED IN");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                    holder.viewStatusIndicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                    holder.tvStatus.setBackgroundResource(android.R.drawable.toast_frame);
                    holder.tvStatus.getBackground().setTint(android.graphics.Color.parseColor("#E8F5E9"));
                    break;

                case "cancelled":
                    holder.tvStatus.setText("CANCELLED");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                    holder.viewStatusIndicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                    holder.tvStatus.setBackgroundResource(android.R.drawable.toast_frame);
                    holder.tvStatus.getBackground().setTint(android.graphics.Color.parseColor("#FFEBEE"));
                    break;

                default:
                    holder.tvStatus.setText(status.toUpperCase());
                    holder.viewStatusIndicator.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                    break;
            }
        }

        holder.itemView.setOnClickListener(v -> showTicketDetailDialog(booking));
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    // Làm sạch Adapter trước khi thêm tập dữ liệu mới để đồng bộ chuẩn xác UI
    public void updateData(List<Booking> newList) {
        this.bookingList.clear();
        this.bookingList.addAll(newList);
        notifyDataSetChanged();
    }

    private void showTicketDetailDialog(Booking bookingData) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_ticket_detail);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvBookingId = dialog.findViewById(R.id.tvBookingId);
        TextView tvMovieTitle = dialog.findViewById(R.id.tvMovieTitle);
        TextView tvRoomAndSeats = dialog.findViewById(R.id.tvRoomAndSeats);
        TextView tvPriceAndStatus = dialog.findViewById(R.id.tvPriceAndStatus);
        Button btnCheckIn = dialog.findViewById(R.id.btnCheckIn);
        Button btnCancelTicket = dialog.findViewById(R.id.btnCancelTicket);

        String id = bookingData.getBookingId();
        String title = bookingData.getMovieTitle();
        String room = bookingData.getRoom();
        String startTime = bookingData.getStartTime();
        String status = bookingData.getStatus();
        long price = bookingData.getTotalPrice();
        List<String> seats = bookingData.getSeats();

        String seatsString = (seats != null && !seats.isEmpty()) ? String.join(", ", seats) : "";

        tvBookingId.setText("Mã đặt vé: " + id);
        tvMovieTitle.setText(title);
        tvRoomAndSeats.setText("Phòng: " + room + " | Giờ: " + startTime + " | Ghế: " + seatsString);
        tvPriceAndStatus.setText("Tổng tiền: " + price + "đ | Trạng thái: " + status);

        if ("checked_in".equals(status) || "cancelled".equals(status)) {
            btnCheckIn.setEnabled(false);
            btnCancelTicket.setEnabled(false);
        }

        FirebaseFirestore firestoreDb = FirebaseFirestore.getInstance();

        btnCheckIn.setOnClickListener(v -> {
            if (id != null) {
                firestoreDb.collection("bookings").document(id).update("status", "checked_in")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Đã soát vé thành công!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
            }
        });

        btnCancelTicket.setOnClickListener(v -> {
            if (id != null) {
                firestoreDb.collection("bookings").document(id).update("status", "cancelled")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Đã hủy vé thành công!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
            }
        });

        dialog.show();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvMovieTitle, tvBookingId, tvPrice, tvStatus;
        TextView tvTime, tvSeats;
        View viewStatusIndicator;

        public BookingViewHolder(@NonNull View view) {
            super(view);
            tvMovieTitle = view.findViewById(R.id.itemMovieTitle);
            tvStatus = view.findViewById(R.id.itemStatus);
            tvBookingId = view.findViewById(R.id.itemBookingId);
            tvPrice = view.findViewById(R.id.itemPrice);
            tvSeats = view.findViewById(R.id.itemSeats);
            tvTime = view.findViewById(R.id.itemTime);
            viewStatusIndicator = view.findViewById(R.id.viewStatusIndicator);
        }
    }
}