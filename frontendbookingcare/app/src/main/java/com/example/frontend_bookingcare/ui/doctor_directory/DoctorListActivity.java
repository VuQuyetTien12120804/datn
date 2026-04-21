package com.example.frontend_bookingcare.ui.doctor_directory;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.data.DoctorRepository;

import java.util.List;

/**
 * Danh bạ bác sĩ cho bệnh nhân duyệt. Khác với panel bác sĩ
 * ({@code ui.doctor_panel}) — màn này thuộc luồng người dùng bệnh nhân.
 */
public class DoctorListActivity extends AppCompatActivity {

    private DoctorListAdapter adapter;
    private ProgressBar progress;
    private TextView emptyView;
    private RecyclerView recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_list);

        View header = findViewById(R.id.doctor_list_header);
        applyTopInsetToHeader(header);

        ImageButton back = findViewById(R.id.doctor_list_back);
        back.setOnClickListener(v -> finish());

        recycler = findViewById(R.id.doctor_list_recycler);
        progress = findViewById(R.id.doctor_list_progress);
        emptyView = findViewById(R.id.doctor_list_empty);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorListAdapter();
        recycler.setAdapter(adapter);

        fetchDoctors();
    }

    private void fetchDoctors() {
        progress.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        new DoctorRepository().fetchAllDoctors((data, error) -> {
            progress.setVisibility(View.GONE);
            if (error != null) {
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText(getString(R.string.doctor_list_error_fmt, error));
                return;
            }
            List<DoctorDetail> mapped = DoctorMapper.groupByDoctor(data);
            if (mapped.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText(R.string.doctor_list_empty);
            } else {
                emptyView.setVisibility(View.GONE);
                adapter.submit(mapped);
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
