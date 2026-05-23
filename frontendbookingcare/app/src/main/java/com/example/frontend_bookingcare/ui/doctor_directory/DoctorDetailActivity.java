package com.example.frontend_bookingcare.ui.doctor_directory;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.SlotDto;
import com.example.frontend_bookingcare.data.BookingRepository;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.booking.BookingFormatters;
import com.example.frontend_bookingcare.ui.booking.BookingPolicy;
import com.example.frontend_bookingcare.ui.booking.BookingStep1Activity;
import com.example.frontend_bookingcare.ui.support.chat.CustomerCareChatActivity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn chi tiết 1 bác sĩ (patient-facing) — layout mô phỏng YouMed:
 *  - Header xanh + thông tin bác sĩ + ô lưu ý
 *  - Lịch khám thật từ API /booking/working-dates + /booking/slots
 *  - Giới thiệu / Chuyên khám / Nơi công tác / Đào tạo / Kinh nghiệm
 *  - Bottom action bar: Chat / Đặt khám
 */
public class DoctorDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DOCTOR_ID = "extra_doctor_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_SPECIALTIES = "extra_specialties";
    public static final String EXTRA_BIO = "extra_bio";
    public static final String EXTRA_ADDRESS = "extra_address";
    public static final String EXTRA_AVATAR_BG = "extra_avatar_bg";

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM/yyyy");

    private final BookingRepository bookingRepo = new BookingRepository();

    private int doctorId = -1;
    private final List<String> workingDates = new ArrayList<>();
    @Nullable private String selectedDate;
    @Nullable private SlotDto selectedSlot;
    @Nullable private TextView selectedSlotView;
    private String activeSession = "afternoon";
    private List<SlotDto> lastFetchedSlots = new ArrayList<>();
    private final Map<String, Integer> availableCountByDate = new HashMap<>();

    private TextView monthLabel;
    private LinearLayout dayRow;
    private LinearLayout timeRow;
    private TextView scheduleHint;
    private TextView sessionLabel;
    private View sessionRow;

    public static Intent newIntent(Context ctx, DoctorDetail d) {
        Intent i = new Intent(ctx, DoctorDetailActivity.class);
        if (d.doctorId != null) i.putExtra(EXTRA_DOCTOR_ID, d.doctorId.intValue());
        i.putExtra(EXTRA_TITLE, d.title);
        i.putExtra(EXTRA_NAME, d.name);
        i.putStringArrayListExtra(EXTRA_SPECIALTIES, new ArrayList<>(d.specialties));
        i.putExtra(EXTRA_BIO, d.bio);
        i.putExtra(EXTRA_ADDRESS, d.address);
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

        doctorId = getIntent().getIntExtra(EXTRA_DOCTOR_ID, -1);

        bindStaticDoctor();
        bindScheduleViews();
        wireBottomBar();
        loadWorkingDates();
    }

    private void bindScheduleViews() {
        monthLabel = findViewById(R.id.detail_month_label);
        dayRow = findViewById(R.id.detail_day_row);
        timeRow = findViewById(R.id.detail_time_row);
        scheduleHint = findViewById(R.id.detail_schedule_hint);
        sessionLabel = findViewById(R.id.detail_session_label);
        sessionRow = findViewById(R.id.detail_session_row);

        scheduleHint.setText(R.string.doctor_detail_schedule_loading);
        sessionRow.setOnClickListener(v -> toggleSessionTab());
    }

    private void loadWorkingDates() {
        dayRow.removeAllViews();
        timeRow.removeAllViews();
        workingDates.clear();
        availableCountByDate.clear();
        selectedDate = null;
        selectedSlot = null;
        selectedSlotView = null;
        scheduleHint.setText(R.string.doctor_detail_schedule_loading);

        if (doctorId <= 0) {
            scheduleHint.setText(R.string.booking_dates_error_doctor);
            return;
        }

        bookingRepo.fetchWorkingDates(doctorId, (data, error) -> {
            if (error != null) {
                scheduleHint.setText(getString(R.string.booking_dates_error_fmt, error));
                return;
            }
            if (data == null || data.isEmpty()) {
                scheduleHint.setText(R.string.booking_dates_empty);
                return;
            }

            workingDates.clear();
            LocalDate first = null;
            for (String iso : data) {
                LocalDate d = BookingFormatters.parseIsoDate(iso);
                if (d == null) continue;
                workingDates.add(iso);
                if (first == null || d.isBefore(first)) first = d;
            }

            if (workingDates.isEmpty()) {
                scheduleHint.setText(R.string.booking_dates_empty);
                return;
            }

            renderDayRow();
            if (first != null) {
                selectDate(first.format(BookingFormatters.ISO_DATE));
            }
        });
    }

    private void renderDayRow() {
        dayRow.removeAllViews();
        for (String iso : workingDates) {
            LocalDate d = BookingFormatters.parseIsoDate(iso);
            if (d == null) continue;
            dayRow.addView(buildDayPill(iso, d));
        }
    }

    private View buildDayPill(String iso, LocalDate date) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wrapLp.setMargins(dp(4), 0, dp(4), 0);
        wrap.setLayoutParams(wrapLp);
        wrap.setTag(iso);

        FrameLayout circle = new FrameLayout(this);
        int size = dp(44);
        LinearLayout.LayoutParams circleLp = new LinearLayout.LayoutParams(size, size);
        circle.setLayoutParams(circleLp);

        TextView dayNum = new TextView(this);
        dayNum.setText(String.valueOf(date.getDayOfMonth()));
        dayNum.setGravity(Gravity.CENTER);
        dayNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        dayNum.setTypeface(null, Typeface.BOLD);
        FrameLayout.LayoutParams numLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        dayNum.setLayoutParams(numLp);

        circle.addView(dayNum);
        wrap.addView(circle);

        TextView sub = new TextView(this);
        sub.setTag("sub");
        sub.setGravity(Gravity.CENTER);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dim));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(4);
        sub.setLayoutParams(subLp);
        Integer count = availableCountByDate.get(iso);
        sub.setText(count != null
                ? getString(R.string.doctor_detail_slot_count_fmt, count)
                : BookingFormatters.dowLabel(this, date));
        wrap.addView(sub);

        applyDayPillVisual(wrap, circle, dayNum, iso.equals(selectedDate));
        wrap.setOnClickListener(v -> selectDate(iso));
        return wrap;
    }

    private void applyDayPillVisual(LinearLayout wrap, FrameLayout circle, TextView dayNum, boolean selected) {
        circle.setBackgroundResource(selected
                ? R.drawable.bg_detail_day_active
                : R.drawable.bg_detail_day_inactive);
        dayNum.setTextColor(ContextCompat.getColor(this,
                selected ? R.color.white : R.color.text_primary));
    }

    private void refreshDayRowSelection() {
        for (int i = 0; i < dayRow.getChildCount(); i++) {
            View child = dayRow.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout wrap = (LinearLayout) child;
            Object tag = wrap.getTag();
            if (!(tag instanceof String)) continue;
            String iso = (String) tag;
            FrameLayout circle = (FrameLayout) wrap.getChildAt(0);
            TextView dayNum = (TextView) circle.getChildAt(0);
            applyDayPillVisual(wrap, circle, dayNum, iso.equals(selectedDate));

            TextView sub = wrap.findViewWithTag("sub");
            if (sub != null) {
                Integer count = availableCountByDate.get(iso);
                if (count != null) {
                    sub.setText(getString(R.string.doctor_detail_slot_count_fmt, count));
                }
            }
        }
    }

    private void selectDate(String iso) {
        if (iso.equals(selectedDate)) return;
        selectedDate = iso;
        selectedSlot = null;
        selectedSlotView = null;
        refreshDayRowSelection();

        LocalDate d = BookingFormatters.parseIsoDate(iso);
        if (d != null) {
            monthLabel.setText(getString(R.string.doctor_detail_month_fmt, d.format(MONTH_FMT)) + "  ▾");
        }

        loadSlotsForDate(iso);
    }

    private void loadSlotsForDate(String iso) {
        timeRow.removeAllViews();
        lastFetchedSlots = new ArrayList<>();
        scheduleHint.setText(R.string.doctor_detail_schedule_loading);

        bookingRepo.fetchSlots(doctorId, iso, (data, error) -> {
            if (!iso.equals(selectedDate)) return;
            if (error != null) {
                scheduleHint.setText(getString(R.string.booking_slots_error_fmt, error));
                return;
            }
            lastFetchedSlots = data != null ? data : new ArrayList<>();
            int available = countAvailableSlots(lastFetchedSlots);
            availableCountByDate.put(iso, available);
            refreshDayRowSelection();

            if (lastFetchedSlots.isEmpty()) {
                scheduleHint.setText(R.string.booking_slots_empty);
                sessionLabel.setText(R.string.doctor_detail_afternoon);
                return;
            }

            boolean anyMorning = hasAvailableInSession(iso, "morning");
            boolean anyAfternoon = hasAvailableInSession(iso, "afternoon");
            activeSession = anyMorning ? "morning" : "afternoon";
            if (!hasAvailableInSession(iso, activeSession)) {
                activeSession = anyAfternoon ? "afternoon" : "morning";
            }

            updateSessionLabel();
            renderTimeSlots();
            updateScheduleHint();
        });
    }

    private boolean hasAvailableInSession(String iso, String session) {
        for (SlotDto s : lastFetchedSlots) {
            if (!session.equals(BookingFormatters.sessionOf(s.startTime))) continue;
            if (Boolean.TRUE.equals(s.isAvailable)
                    && !BookingPolicy.violatesMinLead(iso, s.startTime)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSlotsInSession(String session) {
        return selectedDate != null && hasAvailableInSession(selectedDate, session);
    }

    private int countAvailableSlots(List<SlotDto> slots) {
        if (selectedDate == null) return 0;
        int n = 0;
        for (SlotDto s : slots) {
            if (Boolean.TRUE.equals(s.isAvailable)
                    && !BookingPolicy.violatesMinLead(selectedDate, s.startTime)) {
                n++;
            }
        }
        return n;
    }

    private void toggleSessionTab() {
        if (lastFetchedSlots.isEmpty() || selectedDate == null) return;
        String next = "morning".equals(activeSession) ? "afternoon" : "morning";
        if (!hasAvailableInSession(selectedDate, next)) {
            Toast.makeText(this, R.string.booking_slots_tab_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        activeSession = next;
        selectedSlot = null;
        selectedSlotView = null;
        updateSessionLabel();
        renderTimeSlots();
        updateScheduleHint();
    }

    private void updateSessionLabel() {
        sessionLabel.setText("morning".equals(activeSession)
                ? R.string.doctor_detail_morning
                : R.string.doctor_detail_afternoon);
    }

    private void renderTimeSlots() {
        timeRow.removeAllViews();
        if (selectedDate == null) return;

        List<SlotDto> inSession = new ArrayList<>();
        for (SlotDto s : lastFetchedSlots) {
            if (activeSession.equals(BookingFormatters.sessionOf(s.startTime))) {
                inSession.add(s);
            }
        }

        if (inSession.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.booking_slots_tab_empty);
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dim));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            empty.setPadding(dp(4), dp(8), dp(4), dp(8));
            timeRow.addView(empty);
            return;
        }

        for (SlotDto slot : inSession) {
            timeRow.addView(buildTimePill(slot));
        }
    }

    private TextView buildTimePill(SlotDto slot) {
        boolean available = Boolean.TRUE.equals(slot.isAvailable);
        boolean tooSoon = selectedDate != null
                && BookingPolicy.violatesMinLead(selectedDate, slot.startTime);

        TextView tv = new TextView(this);
        tv.setText(BookingFormatters.timeRange(slot.startTime, slot.endTime));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setBackgroundResource(R.drawable.bg_booking_time_pill);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), 0, dp(4), 0);
        tv.setLayoutParams(lp);

        if (!available || tooSoon) {
            tv.setAlpha(0.4f);
            return tv;
        }

        tv.setOnClickListener(v -> selectSlot(slot, tv));
        if (selectedSlot != null && slot.slotId != null && slot.slotId.equals(selectedSlot.slotId)) {
            tv.setSelected(true);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        return tv;
    }

    private void selectSlot(SlotDto slot, TextView clicked) {
        selectedSlot = slot;
        if (selectedSlotView != null) {
            selectedSlotView.setSelected(false);
            selectedSlotView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
        selectedSlotView = clicked;
        clicked.setSelected(true);
        clicked.setTextColor(ContextCompat.getColor(this, R.color.white));
        updateScheduleHint();
    }

    private void updateScheduleHint() {
        if (selectedSlot != null) {
            scheduleHint.setText(getString(
                    R.string.doctor_detail_selected_slot_fmt,
                    BookingFormatters.prettyDate(this, selectedDate),
                    BookingFormatters.timeRange(selectedSlot.startTime, selectedSlot.endTime)));
            return;
        }
        int available = countAvailableSlots(lastFetchedSlots);
        if (available <= 0) {
            scheduleHint.setText(R.string.booking_slots_empty);
        } else {
            scheduleHint.setText(R.string.doctor_detail_pick_slot_hint);
        }
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
        TextView addressView = findViewById(R.id.detail_address);

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

        yearsView.setVisibility(View.GONE);

        bioView.setText(bio != null && !bio.trim().isEmpty()
                ? bio.trim()
                : getString(R.string.doctor_detail_bio_default));

        String address = intent.getStringExtra(EXTRA_ADDRESS);
        if (address != null && !address.trim().isEmpty()) {
            addressView.setText(address.trim());
        } else {
            addressView.setText(getString(R.string.clinic_default_address));
        }

        specialtyRow.removeAllViews();
        if (specialties != null && !specialties.isEmpty()) {
            for (String s : specialties) {
                specialtyRow.addView(buildSpecialtyChip(s));
            }
        }

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
        row.setGravity(Gravity.CENTER_VERTICAL);

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

    private void wireBottomBar() {
        findViewById(R.id.detail_chat_btn).setOnClickListener(v -> openDoctorChat());
        findViewById(R.id.detail_map_btn).setOnClickListener(v -> openClinicMap());
        findViewById(R.id.detail_book_btn).setOnClickListener(v -> openBookingFlow());
    }

    private void openClinicMap() {
        String address = getIntent().getStringExtra(EXTRA_ADDRESS);
        if (address == null || address.trim().isEmpty()) {
            address = getString(R.string.clinic_default_address);
        }
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
        Intent map = new Intent(Intent.ACTION_VIEW, uri);
        map.setPackage("com.google.android.apps.maps");
        if (map.resolveActivity(getPackageManager()) != null) {
            startActivity(map);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(address))));
        }
    }

    private void openDoctorChat() {
        SessionManager sm = new SessionManager(this);
        if (!sm.isLoggedIn()) {
            Toast.makeText(this, R.string.doctor_detail_chat_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = getIntent();
        int id = intent.getIntExtra(EXTRA_DOCTOR_ID, -1);
        if (id <= 0) {
            toast(R.string.doctor_detail_missing_id);
            return;
        }
        String name = intent.getStringExtra(EXTRA_NAME);
        String displayName = name != null && !name.isEmpty() ? name : getString(R.string.dash_placeholder);
        startActivity(CustomerCareChatActivity.newIntent(
                this,
                "doctor:" + id,
                getString(R.string.chat_doctor_title_fmt, displayName),
                getString(R.string.chat_doctor_subtitle_fmt, displayName),
                false
        ));
    }

    private void openBookingFlow() {
        Intent intent = getIntent();
        int id = intent.getIntExtra(EXTRA_DOCTOR_ID, -1);
        if (id <= 0) {
            toast(R.string.doctor_detail_missing_id);
            return;
        }

        String title = intent.getStringExtra(EXTRA_TITLE);
        String name = intent.getStringExtra(EXTRA_NAME);
        ArrayList<String> specialties = intent.getStringArrayListExtra(EXTRA_SPECIALTIES);
        int avatarBg = intent.getIntExtra(EXTRA_AVATAR_BG, R.drawable.bg_tile_blue);
        String specialty = (specialties != null && !specialties.isEmpty()) ? specialties.get(0) : null;

        if (selectedSlot == null || selectedDate == null || selectedSlot.slotId == null) {
            startActivity(BookingStep1Activity.newIntent(
                    this, id, title, name, specialty, avatarBg));
            return;
        }

        if (BookingPolicy.violatesMinLead(selectedDate, selectedSlot.startTime)) {
            Toast.makeText(this,
                    getString(R.string.booking_min_lead_time_fmt, BookingPolicy.MIN_LEAD_MINUTES),
                    Toast.LENGTH_LONG).show();
            return;
        }

        startActivity(BookingStep1Activity.newIntent(
                this, id, title, name, specialty, avatarBg, selectedDate, selectedSlot.slotId));
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
        final int extra = dp(6);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPaddingRelative(startPad, basePad + topInset + extra, endPad, bottomPad);
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }
}
