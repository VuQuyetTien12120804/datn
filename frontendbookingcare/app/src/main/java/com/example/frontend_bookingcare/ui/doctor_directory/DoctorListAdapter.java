package com.example.frontend_bookingcare.ui.doctor_directory;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;

import java.util.ArrayList;
import java.util.List;

public class DoctorListAdapter extends RecyclerView.Adapter<DoctorListAdapter.Holder> {

    private final List<DoctorDetail> doctors = new ArrayList<>();

    public DoctorListAdapter() {
    }

    public DoctorListAdapter(List<DoctorDetail> initial) {
        if (initial != null) doctors.addAll(initial);
    }

    public void submit(List<DoctorDetail> data) {
        doctors.clear();
        if (data != null) doctors.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor_list, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DoctorDetail d = doctors.get(position);
        Context ctx = holder.itemView.getContext();

        View.OnClickListener openDetail = v ->
                ctx.startActivity(DoctorDetailActivity.newIntent(ctx, d));
        holder.itemView.setOnClickListener(openDetail);
        if (holder.bookBtn != null) holder.bookBtn.setOnClickListener(openDetail);

        holder.avatarBg.setBackgroundResource(d.avatarBg);
        holder.avatarLetter.setText(d.lastNameInitial());

        if (d.title != null && !d.title.isEmpty()) {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(d.title);
        } else {
            holder.title.setVisibility(View.GONE);
        }

        holder.name.setText(d.name);

        if (d.yearsExp != null && d.yearsExp > 0) {
            holder.years.setVisibility(View.VISIBLE);
            holder.years.setText(ctx.getString(R.string.doctor_years_fmt, d.yearsExp));
        } else {
            holder.years.setVisibility(View.GONE);
        }

        if (d.address != null && !d.address.isEmpty()) {
            holder.addressRow.setVisibility(View.VISIBLE);
            holder.address.setText(d.address);
        } else {
            holder.addressRow.setVisibility(View.GONE);
        }

        holder.specialtyRow.removeAllViews();
        if (d.specialties != null && !d.specialties.isEmpty()) {
            holder.specialtyRow.setVisibility(View.VISIBLE);
            for (String s : d.specialties) {
                TextView chip = new TextView(ctx);
                chip.setText(s);
                chip.setTextColor(ctx.getResources().getColor(R.color.brand_primary, ctx.getTheme()));
                chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                chip.setBackgroundResource(R.drawable.bg_specialty_chip);
                int padH = dp(ctx, 10);
                int padV = dp(ctx, 5);
                chip.setPadding(padH, padV, padH, padV);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = dp(ctx, 6);
                chip.setLayoutParams(lp);
                holder.specialtyRow.addView(chip);
            }
        } else {
            holder.specialtyRow.setVisibility(View.GONE);
        }
    }

    private static int dp(Context ctx, int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics()));
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final FrameLayout avatarBg;
        final TextView avatarLetter;
        final TextView title;
        final TextView name;
        final TextView years;
        final View addressRow;
        final TextView address;
        final LinearLayout specialtyRow;
        final TextView bookBtn;

        Holder(@NonNull View itemView) {
            super(itemView);
            avatarBg = itemView.findViewById(R.id.doctor_avatar_bg);
            avatarLetter = itemView.findViewById(R.id.doctor_avatar_letter);
            title = itemView.findViewById(R.id.doctor_title);
            name = itemView.findViewById(R.id.doctor_name);
            years = itemView.findViewById(R.id.doctor_years);
            address = itemView.findViewById(R.id.doctor_address);
            addressRow = itemView.findViewById(R.id.doctor_address_row);
            specialtyRow = itemView.findViewById(R.id.doctor_specialty_row);
            bookBtn = itemView.findViewById(R.id.doctor_book_btn);
        }
    }
}
