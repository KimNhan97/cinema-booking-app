package dacn.buithikimnhan.cinemabookingapp.user.profile;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import dacn.buithikimnhan.cinemabookingapp.R;

public class TicketPagerAdapter extends RecyclerView.Adapter<TicketPagerAdapter.TicketViewHolder> {

    private final List<DocumentSnapshot> ticketList;

    public TicketPagerAdapter(List<DocumentSnapshot> ticketList) {
        this.ticketList = ticketList;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_single, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {

        DocumentSnapshot doc = ticketList.get(position);

        // Tên phim
        String movieTitle = doc.getString("movieTitle");
        holder.ticketMovieTitle.setText(
                movieTitle != null ? movieTitle : "Chưa có dữ liệu"
        );

        // Phòng chiếu
        String room = doc.getString("room");
        holder.ticketRoom.setText(
                room != null && !room.isEmpty()
                        ? room
                        : "Chưa có phòng"
        );

        // Suất chiếu
        String date = doc.getString("date");
        String startTime = doc.getString("startTime");

        if (startTime != null && date != null) {
            holder.ticketShowTime.setText(startTime + " - " + date);
        } else if (startTime != null) {
            holder.ticketShowTime.setText(startTime);
        } else {
            holder.ticketShowTime.setText("Chưa có dữ liệu");
        }

        // Ngày đặt vé
        String bookingDate = doc.getString("bookingDate");

        if (bookingDate != null && !bookingDate.isEmpty()) {
            holder.ticketBookingDate.setText("Ngày đặt: " + bookingDate);
        } else {
            holder.ticketBookingDate.setText("Ngày đặt: --");
        }

        // Mã hóa đơn
        String bookingId = doc.getString("bookingId");

        if (bookingId == null || bookingId.isEmpty()) {
            bookingId = doc.getId();
        }

        holder.ticketBookingId.setText("Mã HD: " + bookingId);
         bookingDate = doc.getString("bookingDate");

        if (bookingDate != null && !bookingDate.isEmpty()) {
            holder.ticketBookingDate.setText(bookingDate);
        } else {
            holder.ticketBookingDate.setText("Không có dữ liệu");
        }

        // Giá tiền
        try {

            Long price = doc.getLong("totalPrice");

            if (price != null) {

                NumberFormat formatter =
                        NumberFormat.getInstance(new Locale("vi", "VN"));

                holder.ticketPrice.setText(
                        formatter.format(price) + "đ"
                );

            } else {

                holder.ticketPrice.setText("0đ");
            }

        } catch (Exception e) {

            String stringPrice = doc.getString("totalPrice");

            holder.ticketPrice.setText(
                    stringPrice != null ? stringPrice : "0đ"
            );
        }

        // Ghế ngồi
        Object seatsObj = doc.get("seats");

        if (seatsObj instanceof List) {

            List<String> seatList = (List<String>) seatsObj;

            holder.ticketSeats.setText(
                    String.join(", ", seatList)
            );

        } else if (seatsObj != null) {

            holder.ticketSeats.setText(
                    seatsObj.toString()
            );

        } else {

            holder.ticketSeats.setText("-");
        }

        // Debug kiểm tra dữ liệu
        android.util.Log.d("TICKET_DEBUG",
                "Movie = " + movieTitle);

        android.util.Log.d("TICKET_DEBUG",
                "Room = " + room);

        android.util.Log.d("TICKET_DEBUG",
                "BookingDate = " + bookingDate);

        // QR Code
        Bitmap qrBitmap =
                generateQRCode(bookingId, 350, 350);

        if (qrBitmap != null) {
            holder.ticketQRCode.setImageBitmap(qrBitmap);
        }
    }

    @Override
    public int getItemCount() {
        return ticketList != null ? ticketList.size() : 0;
    }

    private Bitmap generateQRCode(String content,
                                  int width,
                                  int height) {

        MultiFormatWriter writer =
                new MultiFormatWriter();

        try {

            BitMatrix bitMatrix =
                    writer.encode(
                            content,
                            BarcodeFormat.QR_CODE,
                            width,
                            height
                    );

            BarcodeEncoder encoder =
                    new BarcodeEncoder();

            return encoder.createBitmap(bitMatrix);

        } catch (WriterException e) {

            e.printStackTrace();
            return null;
        }
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {

        TextView ticketMovieTitle;
        TextView ticketShowTime;
        TextView ticketRoom;
        TextView ticketSeats;
        TextView ticketPrice;
        TextView ticketBookingId;
        TextView ticketBookingDate; // THÊM

        ImageView ticketQRCode;

        public TicketViewHolder(@NonNull View view) {
            super(view);

            ticketMovieTitle = view.findViewById(R.id.ticketMovieTitle);
            ticketShowTime = view.findViewById(R.id.ticketShowTime);
            ticketRoom = view.findViewById(R.id.ticketRoom);
            ticketSeats = view.findViewById(R.id.ticketSeats);
            ticketPrice = view.findViewById(R.id.ticketPrice);
            ticketBookingId = view.findViewById(R.id.ticketBookingId);

            ticketBookingDate = view.findViewById(R.id.ticketBookingDate); // THÊM

            ticketQRCode = view.findViewById(R.id.ticketQRCode);
        }
    }
}