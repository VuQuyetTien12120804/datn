package com.example.frontend_bookingcare.ui.account;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.BuildConfig;
import com.example.frontend_bookingcare.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class AccountHubAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onRowClick(@NonNull String rowId);
    }

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_ROW = 1;
    private static final int TYPE_FOOTER = 2;

    private final List<AccountListItem> items = new ArrayList<>();
    private final Listener listener;

    public AccountHubAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<AccountListItem> next) {
        items.clear();
        items.addAll(next);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        switch (items.get(position).kind) {
            case SECTION:
                return TYPE_SECTION;
            case ROW:
                return TYPE_ROW;
            case FOOTER:
            default:
                return TYPE_FOOTER;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SECTION) {
            return new SectionVH(inf.inflate(R.layout.item_account_section, parent, false));
        }
        if (viewType == TYPE_ROW) {
            return new RowVH(inf.inflate(R.layout.item_account_row, parent, false));
        }
        return new FooterVH(inf.inflate(R.layout.item_account_footer, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AccountListItem item = items.get(position);
        if (holder instanceof SectionVH) {
            ((TextView) holder.itemView.findViewById(R.id.section_title)).setText(item.sectionTitle);
        } else if (holder instanceof RowVH) {
            ((RowVH) holder).bind(item);
        } else if (holder instanceof FooterVH) {
            TextView ver = holder.itemView.findViewById(R.id.footer_version);
            ver.setText(holder.itemView.getContext().getString(R.string.account_version_fmt, BuildConfig.VERSION_NAME));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static final class SectionVH extends RecyclerView.ViewHolder {
        SectionVH(@NonNull View itemView) {
            super(itemView);
        }
    }

    private final class RowVH extends RecyclerView.ViewHolder {
        RowVH(@NonNull View itemView) {
            super(itemView);
        }

        void bind(AccountListItem item) {
            TextView title = itemView.findViewById(R.id.row_title);
            TextView sub = itemView.findViewById(R.id.row_subtitle);
            TextView val = itemView.findViewById(R.id.row_value);
            TextView glyph = itemView.findViewById(R.id.row_icon_glyph);
            ImageView iconImg = itemView.findViewById(R.id.row_icon_image);
            MaterialCardView iconCard = itemView.findViewById(R.id.row_icon_card);
            title.setText(item.title);
            if (item.subtitle != null && !item.subtitle.isEmpty()) {
                sub.setVisibility(View.VISIBLE);
                sub.setText(item.subtitle);
            } else {
                sub.setVisibility(View.GONE);
            }
            if (item.value != null && !item.value.isEmpty()) {
                val.setVisibility(View.VISIBLE);
                val.setText(item.value);
            } else {
                val.setVisibility(View.GONE);
            }
            if (item.iconRes != 0) {
                iconImg.setImageResource(item.iconRes);
                iconImg.setVisibility(View.VISIBLE);
                glyph.setVisibility(View.GONE);
            } else {
                iconImg.setVisibility(View.GONE);
                glyph.setVisibility(View.VISIBLE);
                glyph.setText(item.emoji != null ? item.emoji : "•");
            }
            iconCard.setCardBackgroundColor(itemView.getContext().getColor(item.iconBgRes));
            View area = itemView.findViewById(R.id.row_click_area);
            area.setOnClickListener(v -> listener.onRowClick(item.rowId));
        }
    }

    private static final class FooterVH extends RecyclerView.ViewHolder {
        FooterVH(@NonNull View itemView) {
            super(itemView);
        }
    }
}
