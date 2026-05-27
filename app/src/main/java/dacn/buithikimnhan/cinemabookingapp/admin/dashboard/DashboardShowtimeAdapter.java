package dacn.buithikimnhan.cinemabookingapp.admin.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.data.Showtime;

public class DashboardShowtimeAdapter extends RecyclerView.Adapter<DashboardShowtimeAdapter.ViewHolder> {

    private final List<Showtime> list;

    public DashboardShowtimeAdapter(List<Showtime> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_showtime, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Showtime s = list.get(position);

        // ===== TÊN PHIM =====
        holder.tvMovie.setText(
                s.getMovieName() != null
                        ? s.getMovieName()
                        : "Đang tải tên phim..."
        );

        // ===== PHÒNG =====
        holder.tvRoomName.setText("Phòng: " + s.getRoom());

        // ===== NGÀY CHIẾU =====
        holder.tvShowDate.setText("Ngày chiếu: " + s.getDate());

        // ===== GIỜ CHIẾU =====
        holder.tvShowTimeRange.setText(
                s.getStartTime() + " - " + s.getEndTime()
        );

        // ===== TRẠNG THÁI =====
        if (s.getStatus() != null) {
            holder.tvStatusBadge.setText(s.getStatus().toUpperCase());
        } else {
            holder.tvStatusBadge.setText("UNKNOWN");
        }

        // ===== GHẾ =====
        int availableSeats = s.getAvailableSeats();
        int totalSeats = s.getTotalSeats();

        holder.tvSeatRatio.setText(
                availableSeats + " / " + totalSeats
        );

        // Tính phần trăm progress
        int progress = 0;

        if (totalSeats > 0) {
            progress = (availableSeats * 100) / totalSeats;
        }

        holder.progressSeats.setProgress(progress);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // =========================================================
    // VIEW HOLDER
    // =========================================================

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvMovie;
        TextView tvRoomName;
        TextView tvShowDate;
        TextView tvShowTimeRange;
        TextView tvStatusBadge;
        TextView tvSeatRatio;

        ProgressBar progressSeats;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvMovie = itemView.findViewById(R.id.txtMovieName);

            tvRoomName = itemView.findViewById(R.id.txtRoomName);

            tvShowDate = itemView.findViewById(R.id.txtShowDate);

            tvShowTimeRange = itemView.findViewById(R.id.txtShowTimeRange);

            tvStatusBadge = itemView.findViewById(R.id.txtStatusBadge);

            tvSeatRatio = itemView.findViewById(R.id.txtSeatRatio);

            progressSeats = itemView.findViewById(R.id.progressSeats);
        }
    }
}