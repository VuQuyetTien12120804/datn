package com.example.frontend_bookingcare.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;

import java.util.List;

public class HomeBannerAdapter extends RecyclerView.Adapter<HomeBannerAdapter.BannerHolder> {

    public static final class Banner {
        @DrawableRes
        public final int imageRes;

        public Banner(@DrawableRes int imageRes) {
            this.imageRes = imageRes;
        }
    }

    private final List<Banner> banners;

    public HomeBannerAdapter(List<Banner> banners) {
        this.banners = banners;
    }

    @NonNull
    @Override
    public BannerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_banner, parent, false);
        return new BannerHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerHolder holder, int position) {
        holder.image.setImageResource(banners.get(position).imageRes);
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class BannerHolder extends RecyclerView.ViewHolder {
        final ImageView image;

        BannerHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.banner_image);
        }
    }
}
