package com.example.frontend_bookingcare.ui.doctor_directory;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Danh bạ bác sĩ cho bệnh nhân duyệt. Khác với panel bác sĩ
 * ({@code ui.doctor_panel}) — màn này thuộc luồng người dùng bệnh nhân.
 *
 * <p>Hỗ trợ tìm kiếm real-time: lọc theo tên (có/không dấu), học vị và chuyên khoa.
 * Danh sách gốc được giữ trong {@link #allDoctors}; adapter chỉ hiển thị {@link #filterAndSort(String)}
 * để tránh tải lại dữ liệu mỗi lần gõ phím.
 */
public class DoctorListActivity extends AppCompatActivity {

    /** Optional intent extra: gõ sẵn query khi mở màn (dùng khi bấm search ở Home). */
    public static final String EXTRA_INITIAL_QUERY = "extra_initial_query";
    /** Nếu true, auto-focus ô search và bật bàn phím. */
    public static final String EXTRA_FOCUS_SEARCH = "extra_focus_search";
    /**
     * Nếu có (>0), màn hình sẽ gọi GET /api/v1/doctors/specialty/{id}
     * thay vì GET /api/v1/doctors rồi lọc client-side. Dùng khi user bấm vào
     * 1 chuyên khoa ở Home hoặc màn "Tất cả chuyên khoa".
     */
    public static final String EXTRA_SPECIALTY_ID = "extra_specialty_id";
    /** Tên chuyên khoa dùng để hiển thị trong search (chỉ UI, không ảnh hưởng query). */
    public static final String EXTRA_SPECIALTY_NAME = "extra_specialty_name";

    private final List<DoctorDetail> allDoctors = new ArrayList<>();
    /** True trong lúc đang chờ API trả về — dùng để TextWatcher không hiện empty sớm. */
    private boolean fetchInFlight = false;

    private DoctorListAdapter adapter;
    private ProgressBar progress;
    private TextView emptyView;
    private RecyclerView recycler;
    private EditText searchInput;
    private ImageButton searchClear;

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
        searchInput = findViewById(R.id.doctor_list_search_input);
        searchClear = findViewById(R.id.doctor_list_search_clear);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorListAdapter();
        recycler.setAdapter(adapter);

        wireSearch();

        handleInitialQuery();

        fetchDoctors();
    }

    private void wireSearch() {
        searchClear.setOnClickListener(v -> searchInput.setText(""));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String q = s == null ? "" : s.toString();
                searchClear.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                renderFiltered(q);
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            hideKeyboard();
            return true;
        });
    }

    private void handleInitialQuery() {
        // Khi đã có specialtyId — dùng endpoint /api/v1/doctors/specialty/{id}
        // server đã lọc sẵn → không cần đẩy query vào ô search nữa, tránh "lọc 2 lần".
        int specialtyId = getIntent().getIntExtra(EXTRA_SPECIALTY_ID, -1);
        String specialtyName = getIntent().getStringExtra(EXTRA_SPECIALTY_NAME);
        if (specialtyId > 0 && !TextUtils.isEmpty(specialtyName)) {
            searchInput.setHint(getString(R.string.doctor_list_search_hint_specialty, specialtyName));
        } else {
            String initialQuery = getIntent().getStringExtra(EXTRA_INITIAL_QUERY);
            if (!TextUtils.isEmpty(initialQuery)) {
                searchInput.setText(initialQuery);
                searchInput.setSelection(initialQuery.length());
            }
        }
        if (getIntent().getBooleanExtra(EXTRA_FOCUS_SEARCH, false)) {
            searchInput.post(() -> {
                if (searchInput.requestFocus()) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
    }

    private void fetchDoctors() {
        progress.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        fetchInFlight = true;

        int specialtyId = getIntent().getIntExtra(EXTRA_SPECIALTY_ID, -1);
        DoctorRepository repo = new DoctorRepository();
        DoctorRepository.ResultCallback<List<com.example.frontend_bookingcare.api.DoctorDto>> callback =
                (data, error) -> {
                    progress.setVisibility(View.GONE);
                    fetchInFlight = false;
                    if (error != null) {
                        allDoctors.clear();
                        adapter.submit(Collections.emptyList());
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText(getString(R.string.doctor_list_error_fmt, error));
                        return;
                    }
                    List<DoctorDetail> mapped = DoctorMapper.groupByDoctor(data);
                    allDoctors.clear();
                    allDoctors.addAll(mapped);
                    renderFiltered(searchInput.getText() == null ? "" : searchInput.getText().toString());
                };

        if (specialtyId > 0) {
            repo.fetchDoctorsBySpecialty(specialtyId, callback);
        } else {
            repo.fetchAllDoctors(callback);
        }
    }

    /**
     * Vẽ lại list theo query hiện tại. Gộp cùng 1 hàm để fetch lần đầu, TextWatcher,
     * hoặc thay đổi query khác đều chạy chung 1 pipeline.
     */
    private void renderFiltered(String rawQuery) {
        List<DoctorDetail> filtered = filterAndSort(rawQuery);
        adapter.submit(filtered);

        if (fetchInFlight) {
            // Chưa có data → progressbar đang hiện, không cần empty state.
            return;
        }

        if (filtered.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            String q = rawQuery == null ? "" : rawQuery.trim();
            if (!q.isEmpty()) {
                emptyView.setText(getString(R.string.doctor_list_search_empty_fmt, q));
            } else {
                // Nếu đang lọc theo chuyên khoa nhưng chưa có bác sĩ → message riêng.
                int specialtyId = getIntent().getIntExtra(EXTRA_SPECIALTY_ID, -1);
                String specialtyName = getIntent().getStringExtra(EXTRA_SPECIALTY_NAME);
                if (specialtyId > 0 && !TextUtils.isEmpty(specialtyName)) {
                    emptyView.setText(getString(R.string.doctor_list_specialty_empty_fmt, specialtyName));
                } else {
                    emptyView.setText(getString(R.string.doctor_list_empty));
                }
            }
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    /**
     * Match theo normalized substring trong: "title name", từng chuyên khoa.
     * Giữ thứ tự ưu tiên: khớp tên trước, khớp chuyên khoa sau.
     */
    private List<DoctorDetail> filterAndSort(String rawQuery) {
        String q = SearchNormalizer.normalize(rawQuery);
        if (q.isEmpty()) {
            return new ArrayList<>(allDoctors);
        }

        List<DoctorDetail> byName = new ArrayList<>();
        List<DoctorDetail> bySpecialty = new ArrayList<>();
        for (DoctorDetail d : allDoctors) {
            String fullName = SearchNormalizer.normalize(
                    (d.title != null ? d.title + " " : "") + (d.name == null ? "" : d.name));
            if (fullName.contains(q)) {
                byName.add(d);
                continue;
            }
            boolean matchedSpec = false;
            for (String s : d.specialties) {
                if (SearchNormalizer.normalize(s).contains(q)) {
                    matchedSpec = true;
                    break;
                }
            }
            if (matchedSpec) {
                bySpecialty.add(d);
            }
        }

        List<DoctorDetail> combined = new ArrayList<>(byName.size() + bySpecialty.size());
        combined.addAll(byName);
        combined.addAll(bySpecialty);
        return combined;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (imm != null && v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
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
