package com.example.frontend_bookingcare.ui.support.chat;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<ChatMessage> items = new ArrayList<>();

    public void setItems(List<ChatMessage> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void add(ChatMessage m) {
        items.add(m);
        notifyItemInserted(items.size() - 1);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bubble, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatMessage m = items.get(position);
        Context ctx = h.itemView.getContext();
        h.text.setText(m.text);
        h.time.setText(DateFormat.format("HH:mm", new Date(m.createdAtMs)));

        LinearLayout row = h.row;
        LinearLayout.LayoutParams lpText = (LinearLayout.LayoutParams) h.text.getLayoutParams();
        LinearLayout.LayoutParams lpTime = (LinearLayout.LayoutParams) h.time.getLayoutParams();

        if (m.fromMe) {
            row.setGravity(Gravity.END);
            h.text.setBackgroundResource(R.drawable.bg_bubble_me);
            lpText.gravity = Gravity.END;
            lpTime.gravity = Gravity.END;
            h.time.setTextColor(ctx.getColor(R.color.text_muted));
        } else {
            row.setGravity(Gravity.START);
            h.text.setBackgroundResource(R.drawable.bg_bubble_other);
            lpText.gravity = Gravity.START;
            lpTime.gravity = Gravity.START;
            h.time.setTextColor(ctx.getColor(R.color.text_muted));
        }
        h.text.setLayoutParams(lpText);
        h.time.setLayoutParams(lpTime);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final LinearLayout row;
        final TextView text;
        final TextView time;

        VH(@NonNull View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.bubble_row);
            text = itemView.findViewById(R.id.bubble_text);
            time = itemView.findViewById(R.id.bubble_time);
        }
    }
}

