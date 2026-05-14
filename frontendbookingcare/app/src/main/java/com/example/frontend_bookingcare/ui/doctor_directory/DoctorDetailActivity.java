package com.example.frontend_bookingcare.ui.doctor_directory;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.ui.booking.BookingStep1Activity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * Màn chi tiết 1 bác sĩ (patient-facing) — layout mô phỏng YouMed:
 *  - Header xanh + thông tin bác sĩ + ô lưu ý
 *  - Lịch khám (ngày + slot giờ) — demo hard-code vì chưa có API slot cho FE
 *  - Giới thiệu / Chuyên khám / Nơi công tác / Đào tạo / Kinh nghiệm
 *  - Bottom action bar: Chat / Gọi video / Đặt khám
 *
 * Nhận dữ liệu qua Intent extras (gửi từ {@link DoctorListAdapter}).
 * Sau này có API chi tiết riêng thì thay {@link #bindStaticDoctor()} bằng fetch theo doctorId.
 */
public class DoctorDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DOCTOR_ID = "extra_doctor_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_SPECIALTIES = "extra_specialties";
    public static final String EXTRA_BIO = "extra_bio";
    public static final String EXTRA_AVATAR_BG = "extra_avatar_bg";

    private TextView selectedSlotView;

    /**
     * Helper cho caller khỏi cần biết key extra. Dùng chung cho mọi nơi muốn mở
     * màn chi tiết (danh sách bác sĩ, home, search...).
     */
    public static Intent newIntent(Context ctx, DoctorDetail d) {
        Intent i = new Intent(ctx, DoctorDetailActivity.class);
        if (d.doctorId != null) i.putExtra(EXTRA_DOCTOR_ID, d.doctorId.intValue());
        i.putExtra(EXTRA_TITLE, d.title);
        i.putExtra(EXTRA_NAME, d.name);
        i.putStringArrayListExtra(EXTRA_SPECIALTIES, new ArrayList<>(d.specialties));
        i.putExtra(EXTRA_BIO, d.bio);
        i.putExtra(EXTRA_AVATAR_BG, d.avatarBg);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_detail);

        View header = findViewById(R.id.detail_header);
        applyTopInsetToHeader(header);

        ImageButton back = findViewById(R.id.detail_back);
        back.setOnClickListener(v -> finish());

        bindStaticDoctor();
        bindScheduleCta();
        wireBottomBar();
    }

    /**
     * Tránh gây hiểu nhầm: lịch ở màn detail chỉ là UI minh hoạ.
     * Màn chọn slot thật nằm ở BookingStep1 (gọi API /booking/working-dates + /booking/slots).
     */
    private void bindScheduleCta() {
        TextView monthLabel = findViewById(R.id.detail_month_label);
        LinearLayout dayRow = findViewById(R.id.detail_day_row);
        LinearLayout timeRow = findViewById(R.id.detail_time_row);
        TextView hint = findViewById(R.id.detail_schedule_hint);
        View card = findViewById(R.id.detail_schedule_card);

        // Keep month chip looking nice, but no demo data.
        LocalDate today = LocalDate.now();
        String monthText = today.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        monthLabel.setText(getString(R.string.doctor_detail_month_fmt, monthText) + "  ▾");

        dayRow.removeAllViews();
        timeRow.removeAllViews();
        hint.setText(R.string.doctor_detail_schedule_cta_hint);

        View.OnClickListener open = v -> openBookingFlow();
        card.setOnClickListener(open);
        monthLabel.setOnClickListener(open);
        dayRow.setOnClickListener(open);
        timeRow.setOnClickListener(open);
    }

    private void bindStaticDoctor() {
        Intent intent = getIntent();
        String title = intent.getStringExtra(EXTRA_TITLE);
        String name = intent.getStringExtra(EXTRA_NAME);
        ArrayList<String> specialties = intent.getStringArrayListExtra(EXTRA_SPECIALTIES);
        String bio = intent.getStringExtra(EXTRA_BIO);
        int avatarBg = intent.getIntExtra(EXTRA_AVATAR_BG, R.drawable.bg_tile_blue);

        FrameLayout avatar = findViewById(R.id.detail_avatar_bg);
        TextView avatarLetter = findViewById(R.id.detail_avatar_letter);
        TextView titleView = findViewById(R.id.detail_title);
        TextView nameView = findViewById(R.id.detail_name);
        TextView headerSmall = findViewById(R.id.detail_header_title_small);
        TextView headerName = findViewById(R.id.detail_header_name);
        TextView yearsView = findViewById(R.id.detail_years);
        TextView bioView = findViewById(R.id.detail_bio);
        LinearLayout specialtyRow = findViewById(R.id.detail_specialty_row);

        avatar.setBackgroundResource(avatarBg);
        avatarLetter.setText(lastNameInitial(name));

        if (title != null && !title.isEmpty()) {
            titleView.setVisibility(View.VISIBLE);
            titleView.setText(title);
            headerSmall.setVisibility(View.VISIBLE);
            headerSmall.setText(title);
        } else {
            titleView.setVisibility(View.GONE);
            headerSmall.setVisibility(View.GONE);
        }

        String safeName = name != null ? name : "";
        nameView.setText(safeName);
        headerName.setText(safeName);

        // Seed data hiện chưa có years, đặt 1 giá trị mặc định hợp lý để match mock-up.
        yearsView.setText(getString(R.string.doctor_detail_years_default));

        // Bio: nếu backend có bio thì dùng; không thì dùng fallback.
        bioView.setText(bio != null && !bio.trim().isEmpty()
                ? bio.trim()
                : getString(R.string.doctor_detail_bio_default));

        specialtyRow.removeAllViews();
        if (specialties != null && !specialties.isEmpty()) {
            for (String s : specialties) {
                specialtyRow.addView(buildSpecialtyChip(s));
            }
        }

        // Dựng danh sách "Chuyên khám" dưới dạng bullet từ specialties
        LinearLayout treats = findViewById(R.id.detail_treats_list);
        treats.removeAllViews();
        if (specialties != null && !specialties.isEmpty()) {
            for (String s : specialties) {
                treats.addView(buildBulletRow(s));
            }
        } else {
            treats.addView(buildBulletRow(getString(R.string.doctor_detail_bio_default)));
        }
    }

    private TextView buildSpecialtyChip(String text) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextColor(getResources().getColor(R.color.brand_primary, getTheme()));
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setBackgroundResource(R.drawable.bg_specialty_chip);
        int padH = dp(10);
        int padV = dp(5);
        chip.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(6);
        chip.setLayoutParams(lp);
        return chip;
    }

    private View buildBulletRow(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        View dot = new View(this);
        int size = dp(6);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(size, size);
        dotLp.rightMargin = dp(10);
        dot.setLayoutParams(dotLp);
        dot.setBackgroundResource(R.drawable.bg_detail_bullet);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        row.addView(dot);
        row.addView(tv);
        return row;
    }

    /**
     * Dựng tạm 1 tuần lịch và 1 buổi chiều với 8 khung giờ — khớp layout mock-up.
     * Khi có API slot thật cho FE, thay block này bằng fetch từ backend và map
     * {@code appointment_slots} theo doctorId + ngày.
     */
    private void buildScheduleDemo() {
        TextView monthLabel = findViewById(R.id.detail_month_label);
        LinearLayout dayRow = findViewById(R.id.detail_day_row);
        LinearLayout timeRow = findViewById(R.id.detail_time_row);

        LocalDate today = LocalDate.now();
        String monthText = today.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        monthLabel.setText(getString(R.string.doctor_detail_month_fmt, monthText) + "  ▾");

        dayRow.removeAllViews();
        // Sinh 6 ngày kể từ hôm nay
        for (int offset = 0; offset < 6; offset++) {
            LocalDate d = today.plusDays(offset);
            boolean selected = offset == 0;
            int slotCount = 5 + (offset % 4) * 3;
            dayRow.addView(buildDayPill(d, selected, slotCount));
        }

        timeRow.removeAllViews();
        String[] times = new String[]{
                "17:45-18:00", "18:00-18:15", "18:15-18:30",
                "18:30-18:45", "18:45-19:00", "19:00-19:15"
        };
        for (String t : times) {
            timeRow.addView(buildTimePill(t));
        }
    }

    private View buildDayPill(LocalDate date, boolean selected, int slotCount) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        col.setLayoutParams(lp);

        String dowLabel = "T" + ((date.getDayOfWeek().getValue() % 7) + 1); // T2..T8 (CN)
        // Chuẩn VN: T2=Thứ Hai; DayOfWeek.MONDAY=1 → T2; SUNDAY=7 → T8 → fallback "CN"
        if (date.getDayOfWeek().getValue() == 7) dowLabel = "CN";

        TextView dow = new TextView(this);
        dow.setText(dowLabel);
        dow.setTextColor(getResources().getColor(R.color.text_secondary_dim, getTheme()));
        dow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        col.addView(dow);

        TextView day = new TextView(this);
        day.setText(String.valueOf(date.getDayOfMonth()));
        day.setGravity(android.view.Gravity.CENTER);
        int s = dp(40);
        LinearLayout.LayoutParams dayLp = new LinearLayout.LayoutParams(s, s);
        dayLp.topMargin = dp(4);
        day.setLayoutParams(dayLp);
        if (selected) {
            day.setBackgroundResource(R.drawable.bg_detail_day_active);
            day.setTextColor(getResources().getColor(R.color.white, getTheme()));
        } else {
            day.setBackgroundResource(R.drawable.bg_detail_day_inactive);
            day.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        }
        day.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        day.setTypeface(null, android.graphics.Typeface.BOLD);
        col.addView(day);

        TextView badge = new TextView(this);
        badge.setText(getString(R.string.doctor_detail_slot_count_fmt, slotCount));
        badge.setTextColor(getResources().getColor(R.color.doctor_status_done_fg, getTheme()));
        badge.setBackgroundResource(R.drawable.bg_detail_slot_badge);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        int padH = dp(6);
        int padV = dp(2);
        badge.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeLp.topMargin = dp(4);
        badge.setLayoutParams(badgeLp);
        col.addView(badge);

        return col;
    }

    private View buildTimePill(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setBackgroundResource(R.drawable.bg_detail_time_pill);
        int padH = dp(12);
        int padV = dp(10);
        tv.setPadding(padH, padV, padH, padV);

        // toggle chọn
        tv.setOnClickListener(v -> {
            if (selectedSlotView != null) {
                selectedSlotView.setSelected(false);
                selectedSlotView.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            }
            if (selectedSlotView == v) {
                selectedSlotView = null;
            } else {
                v.setSelected(true);
                ((TextView) v).setTextColor(getResources().getColor(R.color.white, getTheme()));
                selectedSlotView = (TextView) v;
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void wireBottomBar() {
        findViewById(R.id.detail_chat_btn).setOnClickListener(v ->
                toast(R.string.doctor_detail_feature_coming));
        findViewById(R.id.detail_video_btn).setOnClickListener(v ->
                toast(R.string.doctor_detail_feature_coming));
        findViewById(R.id.detail_save).setOnClickListener(v ->
                toast(R.string.doctor_detail_feature_coming));
        findViewById(R.id.detail_help).setOnClickListener(v ->
                toast(R.string.doctor_detail_feature_coming));
        findViewById(R.id.detail_map_btn).setOnClickListener(v ->
                toast(R.string.doctor_detail_feature_coming));

        findViewById(R.id.detail_book_btn).setOnClickListener(v -> openBookingFlow());
    }

    /**
     * Mở luồng 3 bước đặt lịch khám. Không cần bắt buộc user chọn slot ở màn detail
     * (schedule demo ở đây chỉ là UI marketing) — slot thật sẽ được fetch lại ở
     * BookingStep1 dựa trên API /booking/working-dates + /booking/slots.
     */
    private void openBookingFlow() {
        Intent intent = getIntent();
        int doctorId = intent.getIntExtra(EXTRA_DOCTOR_ID, -1);
        if (doctorId <= 0) {
            toast(R.string.doctor_detail_missing_id);
            return;
        }
        String title = intent.getStringExtra(EXTRA_TITLE);
        String name = intent.getStringExtra(EXTRA_NAME);
        ArrayList<String> specialties = intent.getStringArrayListExtra(EXTRA_SPECIALTIES);
        int avatarBg = intent.getIntExtra(EXTRA_AVATAR_BG, R.drawable.bg_tile_blue);
        String specialty = (specialties != null && !specialties.isEmpty()) ? specialties.get(0) : null;

        startActivity(BookingStep1Activity.newIntent(this, doctorId, title, name, specialty, avatarBg));
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    private static String lastNameInitial(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? "?" : last.substring(0, 1).toUpperCase();
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
