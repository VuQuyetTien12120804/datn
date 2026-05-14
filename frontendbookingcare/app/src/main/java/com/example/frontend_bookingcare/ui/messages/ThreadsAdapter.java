package com.example.frontend_bookingcare.ui.messages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;

import java.util.ArrayList;
import java.util.List;

public class ThreadsAdapter extends RecyclerView.Adapter<ThreadsAdapter.VH> {

    public interface OnThreadClick {
        void onClick(MessageThread t);
    }

    private final List<MessageThread> items = new ArrayList<>();
    private final OnThreadClick onClick;

    public ThreadsAdapter(OnThreadClick onClick) {
        this.onClick = onClick;
    }

    public void setItems(List<MessageThread> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_thread, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MessageThread t = items.get(position);
        h.title.setText(t.title);
        h.sub.setText(t.patientName);
        h.preview.setText(t.preview);
        h.time.setText(t.timeLabel);
        h.lock.setVisibility(t.locked ? View.VISIBLE : View.GONE);
        h.avatarLetter.setText(firstLetter(t.title));
        if (t.unreadCount > 0) {
            h.unread.setVisibility(View.VISIBLE);
            h.unread.setText(t.unreadCount > 9 ? "9+" : String.valueOf(t.unreadCount));
        } else {
            h.unread.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(t);
        });
    }

    private static String firstLetter(String s) {
        if (s == null) return "?";
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return "?";
        // take last word initial (usually last name)
        String[] parts = trimmed.split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? "?" : last.substring(0, 1).toUpperCase();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView sub;
        final TextView preview;
        final TextView time;
        final ImageView lock;
        final TextView avatarLetter;
        final TextView unread;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.thread_title);
            sub = itemView.findViewById(R.id.thread_sub);
            preview = itemView.findViewById(R.id.thread_preview);
            time = itemView.findViewById(R.id.thread_time);
            lock = itemView.findViewById(R.id.thread_lock);
            avatarLetter = itemView.findViewById(R.id.thread_avatar_letter);
            unread = itemView.findViewById(R.id.thread_unread_badge);
        }
    }
}

