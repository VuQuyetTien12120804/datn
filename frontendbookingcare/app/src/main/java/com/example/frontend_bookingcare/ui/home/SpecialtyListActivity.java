package com.example.frontend_bookingcare.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.data.SpecialtyRepository;
import com.example.frontend_bookingcare.ui.doctor_directory.DoctorListActivity;

public class SpecialtyListActivity extends AppCompatActivity {

    private SpecialtyGridAdapter adapter;
    private ProgressBar progress;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specialty_list);

        View header = findViewById(R.id.specialty_list_header);
        applyTopInsetToHeader(header);

        ImageButton close = findViewById(R.id.specialty_list_close);
        close.setOnClickListener(v -> finish());

        progress = findViewById(R.id.specialty_list_progress);
        emptyView = findViewById(R.id.specialty_list_empty);

        RecyclerView recycler = findViewById(R.id.specialty_list_recycler);
        recycler.setLayoutManager(new GridLayoutManager(this, 4));
        adapter = new SpecialtyGridAdapter();
        adapter.setOnItemClick(specialty -> {
            if (specialty == null) return;
            Intent i = new Intent(this, DoctorListActivity.class);
            // Ưu tiên dùng specialtyId → gọi đúng endpoint /api/v1/doctors/specialty/{id}.
            if (specialty.specialtyId != null && specialty.specialtyId > 0) {
                i.putExtra(DoctorListActivity.EXTRA_SPECIALTY_ID, specialty.specialtyId.intValue());
                if (specialty.name != null) {
                    i.putExtra(DoctorListActivity.EXTRA_SPECIALTY_NAME, specialty.name);
                }
            } else if (specialty.name != null && !specialty.name.isEmpty()) {
                // Fallback: không có id (lỗi parse) → dùng search.
                i.putExtra(DoctorListActivity.EXTRA_INITIAL_QUERY, specialty.name);
            } else {
                return;
            }
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        fetch();
    }

    private void fetch() {
        progress.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        new SpecialtyRepository().fetchAllSpecialties((data, error) -> {
            progress.setVisibility(View.GONE);
            if (error != null) {
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText(getString(R.string.specialty_list_error_fmt, error));
                return;
            }
            if (data == null || data.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText(R.string.specialty_list_empty);
                adapter.submit(java.util.Collections.emptyList());
            } else {
                emptyView.setVisibility(View.GONE);
                adapter.submit(data);
            }
        });
    }

    private void applyTopInsetToHeader(@NonNull View header) {
        final int basePad = header.getPaddingTop();
        final int startPad = header.getPaddingStart();
        final int endPad = header.getPaddingEnd();
        final int bottomPad = header.getPaddingBottom();
        final int extra = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPaddingRelative(startPad, basePad + topInset + extra, endPad, bottomPad);
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }
}
