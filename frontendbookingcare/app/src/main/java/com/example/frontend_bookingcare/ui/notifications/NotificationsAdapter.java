package com.example.frontend_bookingcare.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.VH> {

    public interface OnNotificationClick {
        void onOpen(@NonNull NotificationMapper.Item item);
    }

    private final List<NotificationMapper.Item> items = new ArrayList<>();
    private final OnNotificationClick onClick;

    public NotificationsAdapter(OnNotificationClick onClick) {
        this.onClick = onClick;
    }

    public void setItems(List<NotificationMapper.Item> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationMapper.Item item = items.get(position);
        h.title.setText(item.title);
        h.body.setText(item.body);
        h.time.setText(item.timeLabel);
        h.action.setVisibility(
                item.category == NotificationMapper.Category.APPOINTMENT ? View.VISIBLE : View.GONE);
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onOpen(item);
        });
        h.action.setOnClickListener(v -> {
            if (onClick != null) onClick.onOpen(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView body;
        final TextView time;
        final TextView action;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            body = itemView.findViewById(R.id.notification_body);
            time = itemView.findViewById(R.id.notification_time);
            action = itemView.findViewById(R.id.notification_action);
        }
    }
}
