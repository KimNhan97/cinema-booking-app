package dacn.buithikimnhan.cinemabookingapp.admin.booking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Booking;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private final Context context;
    private final List<Booking> bookingList;
    private final OnBookingClickListener clickListener;

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    public BookingAdapter(Context context, List<Booking> bookingList, OnBookingClickListener clickListener) {
        this.context = context;
        this.bookingList = bookingList;
        this.clickListener = clickListener;
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

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvPrice.setText(currencyFormat.format(price));

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

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onBookingClick(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    public void updateData(List<Booking> newList) {
        this.bookingList.clear();
        this.bookingList.addAll(newList);
        notifyDataSetChanged();
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