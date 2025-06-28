package com.example.it3c_grp10_navarrosa;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LogEntryAdapter extends RecyclerView.Adapter<LogEntryAdapter.LogEntryViewHolder> {

    private List<CatchRecord> records;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditClick(int position);
        void onDeleteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public LogEntryAdapter(List<CatchRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public LogEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_entry_item, parent, false);
        return new LogEntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogEntryViewHolder holder, int position) {
        CatchRecord record = records.get(position);
        holder.textDate.setText(record.date);
        holder.textType.setText(record.type.toString());
        holder.textTitle.setText(record.title);
        holder.textDescription.setText(record.description);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void updateList(List<CatchRecord> newList) {
        this.records = newList;
        notifyDataSetChanged();
    }

    class LogEntryViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textType, textTitle, textDescription;
        ImageButton buttonDelete;
        public LogEntryViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_date);
            textType = itemView.findViewById(R.id.text_type);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(position);
                    }
                }
            });
        }
    }
} 