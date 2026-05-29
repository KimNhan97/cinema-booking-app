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
import java.util.List;
import dacn.buithikimnhan.cinemabookingapp.R;

public class TicketPagerAdapter extends RecyclerView.Adapter<TicketPagerAdapter.TicketViewHolder> {

    private final List<DocumentSnapshot> ticketList;

    public TicketPagerAdapter(List<DocumentSnapshot> ticketList) {
        this.ticketList = ticketList;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket_single, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        DocumentSnapshot doc = ticketList.get(position);

        // 1. Đổ thông tin text
        holder.ticketMovieTitle.setText(doc.getString("movieTitle"));
        holder.ticketRoom.setText(doc.getString("room"));

        String date = doc.getString("date");
        String startTime = doc.getString("startTime");
        if (startTime != null && date != null) {
            holder.ticketShowTime.setText(startTime + " - " + date);
        } else {
            holder.ticketShowTime.setText(startTime != null ? startTime : "");
        }

        String bookingId = doc.getString("bookingId");
        if (bookingId == null || bookingId.isEmpty()) {
            bookingId = doc.getId();
        }
        holder.ticketBookingId.setText("Mã HD: " + bookingId);

        // 2. Hiển thị giá tiền
        try {
            Long price = doc.getLong("totalPrice");
            if (price != null) {
                holder.ticketPrice.setText(String.format("%,dđ", price));
            } else {
                holder.ticketPrice.setText("0đ");
            }
        } catch (Exception e) {
            String stringPrice = doc.getString("totalPrice");
            holder.ticketPrice.setText(stringPrice != null ? stringPrice : "0đ");
        }

        // 2. Định dạng danh sách ghế
        Object seatsObj = doc.get("seats");
        if (seatsObj instanceof List) {
            List<String> listSeats = (List<String>) seatsObj;
            holder.ticketSeats.setText(String.join(", ", listSeats));
        } else if (seatsObj != null) {
            holder.ticketSeats.setText(seatsObj.toString());
        } else {
            holder.ticketSeats.setText("-");
        }

        // 3. Tạo mã QR Code độc lập cho từng vé
        Bitmap qrBitmap = generateQRCode(bookingId, 350, 350);
        if (qrBitmap != null) {
            holder.ticketQRCode.setImageBitmap(qrBitmap);
        }
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    private Bitmap generateQRCode(String content, int width, int height) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView ticketMovieTitle, ticketShowTime, ticketRoom, ticketSeats, ticketPrice, ticketBookingId;
        ImageView ticketQRCode;

        public TicketViewHolder(@NonNull View view) {
            super(view);
            ticketMovieTitle = view.findViewById(R.id.ticketMovieTitle);
            ticketShowTime = view.findViewById(R.id.ticketShowTime);
            ticketRoom = view.findViewById(R.id.ticketRoom);
            ticketSeats = view.findViewById(R.id.ticketSeats);
            ticketPrice = view.findViewById(R.id.ticketPrice);
            ticketBookingId = view.findViewById(R.id.ticketBookingId);
            ticketQRCode = view.findViewById(R.id.ticketQRCode);
        }
    }
}