package com.example.frontend_bookingcare.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.SpecialtyDto;

import java.util.ArrayList;
import java.util.List;

public class SpecialtyGridAdapter extends RecyclerView.Adapter<SpecialtyGridAdapter.Holder> {

    public interface OnItemClick {
        void onClick(SpecialtyDto specialty);
    }

    private final List<SpecialtyDto> items = new ArrayList<>();
    private OnItemClick listener;

    public void submit(List<SpecialtyDto> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void setOnItemClick(OnItemClick listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_specialty, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SpecialtyDto s = items.get(position);
        holder.icon.setText(SpecialtyIcons.iconFor(s.code, s.name));
        holder.label.setText(s.name != null ? s.name : "");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView icon;
        final TextView label;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.specialty_icon);
            label = itemView.findViewById(R.id.specialty_label);
        }
    }
}
