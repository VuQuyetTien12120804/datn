package com.example.frontend_bookingcare.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;

import java.util.List;

public class HomeBannerAdapter extends RecyclerView.Adapter<HomeBannerAdapter.BannerHolder> {

    public static final int ACTION_BOOK = 0;
    public static final int ACTION_MESSAGES = 1;
    public static final int ACTION_SPECIALTY = 2;

    public static final class Banner {
        @DrawableRes
        public final int imageRes;
        public final int action;

        public Banner(@DrawableRes int imageRes, int action) {
            this.imageRes = imageRes;
            this.action = action;
        }
    }

    public interface OnBannerClickListener {
        void onBannerClick(int action);
    }

    private final List<Banner> banners;
    @Nullable
    private OnBannerClickListener listener;

    public HomeBannerAdapter(List<Banner> banners) {
        this.banners = banners;
    }

    public void setOnBannerClickListener(@Nullable OnBannerClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BannerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_banner, parent, false);
        return new BannerHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerHolder holder, int position) {
        Banner banner = banners.get(position);
        holder.image.setImageResource(banner.imageRes);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(banner.action);
            }
        });
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
