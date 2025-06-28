package com.example.it3c_grp10_navarrosa;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FeedingScheduleAdapter extends RecyclerView.Adapter<FeedingScheduleAdapter.FeedingViewHolder> {
    private List<FeedingSchedule> feedingList;
    private OnFeedingActionListener listener;

    public interface OnFeedingActionListener {
        void onEdit(int position);
        void onDelete(int position);
    }

    public void setOnFeedingActionListener(OnFeedingActionListener listener) {
        this.listener = listener;
    }

    public FeedingScheduleAdapter(List<FeedingSchedule> feedingList) {
        this.feedingList = feedingList;
    }

    @NonNull
    @Override
    public FeedingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feeding_schedule, parent, false);
        return new FeedingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedingViewHolder holder, int position) {
        FeedingSchedule schedule = feedingList.get(position);
        holder.textDate.setText(schedule.date);
        holder.textTime.setText(schedule.time);
        holder.textNotes.setText(schedule.notes);
    }

    @Override
    public int getItemCount() {
        return feedingList.size();
    }

    class FeedingViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textTime, textNotes;
        ImageButton buttonEdit, buttonDelete;
        public FeedingViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_feeding_date);
            textTime = itemView.findViewById(R.id.text_feeding_time);
            textNotes = itemView.findViewById(R.id.text_feeding_notes);
            buttonEdit = itemView.findViewById(R.id.button_edit_feeding);
            buttonDelete = itemView.findViewById(R.id.button_delete_feeding);

            buttonEdit.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onEdit(pos);
                    }
                }
            });
            buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onDelete(pos);
                    }
                }
            });
        }
    }
} 