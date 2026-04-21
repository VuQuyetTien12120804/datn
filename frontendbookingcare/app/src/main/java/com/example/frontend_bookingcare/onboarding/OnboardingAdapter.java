package com.example.frontend_bookingcare.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageHolder> {

    private final List<OnboardingPage> pages;

    public OnboardingAdapter(List<OnboardingPage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding, parent, false);
        return new PageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        OnboardingPage p = pages.get(position);
        holder.image.setImageResource(p.imageRes);
        holder.title.setText(p.titleRes);
        holder.desc.setText(p.descRes);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;
        final TextView desc;

        PageHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.onboarding_image);
            title = itemView.findViewById(R.id.onboarding_title);
            desc = itemView.findViewById(R.id.onboarding_desc);
        }
    }
}
