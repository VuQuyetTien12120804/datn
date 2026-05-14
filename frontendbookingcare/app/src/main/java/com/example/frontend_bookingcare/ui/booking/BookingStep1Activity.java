package com.example.frontend_bookingcare.ui.booking;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CalendarView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.ProfileExtras;
import com.example.frontend_bookingcare.session.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bước 1 của luồng đặt lịch: chọn ngày + chọn slot giờ khám.
 *
 * Data flow:
 *  1) {@code onCreate}: nhận doctor info từ Intent extras → bind card + load
 *     working-dates từ API.
 *  2) Khi user bấm 1 ngày → fetch slots cho ngày đó, chia làm 2 tab sáng/chiều.
 *  3) Khi user bấm 1 slot → enable nút "Tiếp tục".
 *  4) "Tiếp tục" → đóng gói vào {@link BookingDraft} rồi mở Step2.
 */
public class BookingStep1Activity extends AppCompatActivity {

    public static final String EXTRA_DOCTOR_ID = "bs1_doctor_id";
    public static final String EXTRA_DOCTOR_TITLE = "bs1_doctor_title";
    public static final String EXTRA_DOCTOR_NAME = "bs1_doctor_name";
    public static final String EXTRA_DOCTOR_SPECIALTY = "bs1_doctor_specialty";
    public static final String EXTRA_DOCTOR_AVATAR_BG = "bs1_doctor_avatar_bg";

    private final BookingRepository repo = new BookingRepository();

    private int doctorId = -1;
    @Nullable private String doctorTitle;
    @Nullable private String doctorName;
    @Nullable private String doctorSpecialty;
    private int doctorAvatarBg = R.drawable.bg_tile_blue;

    // Selection state
    @Nullable private String selectedDate;              // "yyyy-MM-dd"
    @Nullable private SlotDto selectedSlot;
    private String activeSessionTab = "afternoon";      // "morning" | "afternoon"
    private List<SlotDto> lastFetchedSlots = new ArrayList<>();

    // Views
    private CalendarView calendarView;
    private final Set<String> workingDateSet = new HashSet<>(); // iso yyyy-MM-dd
    private LinearLayout slotsContainer;
    private TextView slotsStatus;
    private ProgressBar slotsProgress;
    private TextView tabMorning;
    private TextView tabAfternoon;
    private MaterialButton continueBtn;
    private TextView sectionTime;
    private LinearLayout timeCard;
    private MaterialButton completeProfileBtn;
    private TextView profileHint;

    private static final int REQ_COMPLETE_PROFILE = 901;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    public static Intent newIntent(Context ctx, int doctorId, @Nullable String title,
                                   @Nullable String name, @Nullable String specialty,
                                   int avatarBg) {
        Intent i = new Intent(ctx, BookingStep1Activity.class);
        i.putExtra(EXTRA_DOCTOR_ID, doctorId);
        i.putExtra(EXTRA_DOCTOR_TITLE, title);
        i.putExtra(EXTRA_DOCTOR_NAME, name);
        i.putExtra(EXTRA_DOCTOR_SPECIALTY, specialty);
        i.putExtra(EXTRA_DOCTOR_AVATAR_BG, avatarBg);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_step1);

        applyTopInsetToHeader(findViewById(R.id.booking_header));
        BookingStepperHelper.bind(findViewById(R.id.booking_stepper), 1);

        Intent in = getIntent();
        doctorId = in.getIntExtra(EXTRA_DOCTOR_ID, -1);
        doctorTitle = in.getStringExtra(EXTRA_DOCTOR_TITLE);
        doctorName = in.getStringExtra(EXTRA_DOCTOR_NAME);
        doctorSpecialty = in.getStringExtra(EXTRA_DOCTOR_SPECIALTY);
        doctorAvatarBg = in.getIntExtra(EXTRA_DOCTOR_AVATAR_BG, R.drawable.bg_tile_blue);

        wireHeader();
        bindDoctorCard();

        // Lấy tham chiếu view TRƯỚC khi bindTimeTabs() — vì bindTimeTabs() gọi
        // setActiveSessionTab() → renderSlots() → đọc slotsContainer; nếu gọi
        // bindTimeTabs trước khi khởi tạo các field này sẽ NPE.
        calendarView = findViewById(R.id.booking_calendar);
        slotsContainer = findViewById(R.id.booking_slots_container);
        slotsStatus = findViewById(R.id.booking_slots_status);
        slotsProgress = findViewById(R.id.booking_slots_progress);
        sectionTime = findViewById(R.id.booking_section_time);
        timeCard = findViewById(R.id.booking_time_card);
        continueBtn = findViewById(R.id.booking_continue);
        completeProfileBtn = findViewById(R.id.booking_complete_profile_btn);
        profileHint = findViewById(R.id.booking_profile_hint);

        // patient card cần completeProfileBtn/profileHint nên phải bind sau khi findViewById
        bindPatientCard();

        bindTimeTabs();

        continueBtn.setEnabled(false);
        continueBtn.setAlpha(0.55f);
        continueBtn.setOnClickListener(v -> goToStep2());

        completeProfileBtn.setOnClickListener(v -> openCompleteProfile());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // month is 0-based
            LocalDate d = LocalDate.of(year, month + 1, dayOfMonth);
            String iso = d.format(ISO);
            if (!workingDateSet.contains(iso)) {
                Toast.makeText(this, R.string.booking_day_no_slot, Toast.LENGTH_SHORT).show();
                // CalendarView vẫn highlight ngày vừa chạm dù ta không cập nhật state — gây lệch:
                // UI như đã chọn 17/5 nhưng selectedDate/slot vẫn là ngày cũ → đặt nhầm ngày.
                snapCalendarToSelectedWorkingDate();
                return;
            }
            selectDate(iso);
        });

        loadWorkingDates();
    }

    private void wireHeader() {
        ImageButton back = findViewById(R.id.booking_back);
        back.setOnClickListener(v -> finish());
    }

    private void bindDoctorCard() {
        FrameLayout avatarBg = findViewById(R.id.booking_doctor_avatar_bg);
        TextView avatarLetter = findViewById(R.id.booking_doctor_avatar_letter);
        TextView titleView = findViewById(R.id.booking_doctor_title);
        TextView nameView = findViewById(R.id.booking_doctor_name);
        TextView specialtyView = findViewById(R.id.booking_doctor_specialty);

        avatarBg.setBackgroundResource(doctorAvatarBg);
        avatarLetter.setText(firstLetter(doctorName));

        if (!TextUtils.isEmpty(doctorTitle)) {
            titleView.setVisibility(View.VISIBLE);
            titleView.setText(doctorTitle);
        } else {
            titleView.setVisibility(View.GONE);
        }
        nameView.setText(doctorName != null ? doctorName : "");

        if (!TextUtils.isEmpty(doctorSpecialty)) {
            specialtyView.setVisibility(View.VISIBLE);
            specialtyView.setText(getString(R.string.booking_specialty_fmt, doctorSpecialty));
        } else {
            specialtyView.setVisibility(View.GONE);
        }
    }

    private void bindPatientCard() {
        SessionManager sm = new SessionManager(this);
        AuthSession s = sm.getSession();
        ProfileExtras ex = sm.getProfileExtras();

        TextView name = findViewById(R.id.booking_patient_name);
        TextView gender = findViewById(R.id.booking_patient_gender);
        TextView dob = findViewById(R.id.booking_patient_dob);
        TextView phone = findViewById(R.id.booking_patient_phone);

        name.setText(s != null && !TextUtils.isEmpty(s.fullName)
                ? s.fullName : getString(R.string.booking_not_available));
        gender.setText(TextUtils.isEmpty(ex.gender)
                ? getString(R.string.booking_not_updated)
                : BookingFormatters.displayGender(ex.gender));
        dob.setText(TextUtils.isEmpty(ex.dob) ? getString(R.string.booking_not_updated) : ex.dob);
        phone.setText(TextUtils.isEmpty(ex.phone) ? getString(R.string.booking_not_updated) : ex.phone);

        boolean ok = hasEnoughProfile(s, ex);
        completeProfileBtn.setVisibility(ok ? View.GONE : View.VISIBLE);
        profileHint.setVisibility(ok ? View.GONE : View.VISIBLE);
    }

    private static boolean hasEnoughProfile(@Nullable AuthSession s, @NonNull ProfileExtras ex) {
        if (s == null) return false;
        if (TextUtils.isEmpty(s.accessToken)) return false;
        if (TextUtils.isEmpty(s.email)) return false;
        if (TextUtils.isEmpty(s.fullName)) return false;
        if (TextUtils.isEmpty(ex.phone)) return false;
        if (TextUtils.isEmpty(ex.dob)) return false;
        if (TextUtils.isEmpty(ex.gender)) return false;
        if (TextUtils.isEmpty(ex.address)) return false;
        return true;
    }

    private void openCompleteProfile() {
        startActivityForResult(CompleteProfileActivity.newIntent(this), REQ_COMPLETE_PROFILE);
    }

    private void bindTimeTabs() {
        tabMorning = findViewById(R.id.booking_tab_morning);
        tabAfternoon = findViewById(R.id.booking_tab_afternoon);
        tabMorning.setOnClickListener(v -> setActiveSessionTab("morning"));
        tabAfternoon.setOnClickListener(v -> setActiveSessionTab("afternoon"));
        setActiveSessionTab("afternoon");
    }

    private void setActiveSessionTab(String session) {
        activeSessionTab = session;
        boolean morning = "morning".equals(session);
        tabMorning.setSelected(morning);
        tabAfternoon.setSelected(!morning);
        tabMorning.setTypeface(null, morning ? Typeface.BOLD : Typeface.NORMAL);
        tabAfternoon.setTypeface(null, morning ? Typeface.NORMAL : Typeface.BOLD);
        renderSlots(lastFetchedSlots);
    }

    // ---------- Load working dates ----------

    private void loadWorkingDates() {
        ProgressBar progress = findViewById(R.id.booking_dates_progress);
        TextView status = findViewById(R.id.booking_dates_status);
        progress.setVisibility(View.VISIBLE);
        status.setVisibility(View.GONE);
        workingDateSet.clear();

        if (doctorId <= 0) {
            progress.setVisibility(View.GONE);
            status.setVisibility(View.VISIBLE);
            status.setText(R.string.booking_dates_error_doctor);
            return;
        }

        repo.fetchWorkingDates(doctorId, (data, error) -> {
            progress.setVisibility(View.GONE);
            if (error != null) {
                status.setVisibility(View.VISIBLE);
                status.setText(getString(R.string.booking_dates_error_fmt, error));
                return;
            }
            if (data == null || data.isEmpty()) {
                status.setVisibility(View.VISIBLE);
                status.setText(R.string.booking_dates_empty);
                return;
            }
            status.setVisibility(View.GONE);
            renderDatesToCalendar(data);
        });
    }

    private void renderDatesToCalendar(List<String> isoDates) {
        LocalDate first = null;
        for (String iso : isoDates) {
            LocalDate d = BookingFormatters.parseIsoDate(iso);
            if (d == null) continue;
            workingDateSet.add(iso);
            if (first == null || d.isBefore(first)) first = d;
        }
        if (first != null) {
            long millis = first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            calendarView.setMinDate(millis);
            calendarView.setDate(millis, false, true);
            selectDate(first.format(ISO));
        }
    }

    private void selectDate(String iso) {
        if (iso.equals(selectedDate)) return;
        selectedDate = iso;
        selectedSlot = null;
        continueBtn.setEnabled(false);
        continueBtn.setAlpha(0.55f);
        loadSlotsForDate(iso);
    }

    /** Đưa CalendarView khớp lại với {@link #selectedDate} (ngày làm việc đang dùng cho slot). */
    private void snapCalendarToSelectedWorkingDate() {
        if (selectedDate == null) return;
        LocalDate d = BookingFormatters.parseIsoDate(selectedDate);
        if (d == null) return;
        long millis = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        calendarView.setDate(millis, false, true);
    }

    // ---------- Load slots ----------

    private void loadSlotsForDate(String iso) {
        sectionTime.setVisibility(View.VISIBLE);
        timeCard.setVisibility(View.VISIBLE);
        slotsProgress.setVisibility(View.VISIBLE);
        slotsStatus.setVisibility(View.GONE);
        slotsContainer.removeAllViews();

        repo.fetchSlots(doctorId, iso, (data, error) -> {
            slotsProgress.setVisibility(View.GONE);
            if (error != null) {
                lastFetchedSlots = new ArrayList<>();
                slotsStatus.setVisibility(View.VISIBLE);
                slotsStatus.setText(getString(R.string.booking_slots_error_fmt, error));
                return;
            }
            lastFetchedSlots = data != null ? data : new ArrayList<>();
            if (lastFetchedSlots.isEmpty()) {
                slotsStatus.setVisibility(View.VISIBLE);
                slotsStatus.setText(R.string.booking_slots_empty);
                return;
            }
            slotsStatus.setVisibility(View.GONE);

            if (selectedSlot != null && BookingPolicy.violatesMinLead(iso, selectedSlot.startTime)) {
                selectedSlot = null;
                continueBtn.setEnabled(false);
                continueBtn.setAlpha(0.55f);
            }

            // Auto-chọn tab có slot (ưu tiên sáng nếu có slot sáng, ngược lại chiều).
            boolean anyMorning = false;
            for (SlotDto s : lastFetchedSlots) {
                if ("morning".equals(BookingFormatters.sessionOf(s.startTime))) {
                    anyMorning = true;
                    break;
                }
            }
            setActiveSessionTab(anyMorning ? "morning" : "afternoon");
        });
    }

    private void renderSlots(List<SlotDto> slots) {
        if (slotsContainer == null) return;
        if (selectedSlot != null && selectedDate != null
                && BookingPolicy.violatesMinLead(selectedDate, selectedSlot.startTime)) {
            selectedSlot = null;
            continueBtn.setEnabled(false);
            continueBtn.setAlpha(0.55f);
        }
        slotsContainer.removeAllViews();
        List<SlotDto> inTab = new ArrayList<>();
        for (SlotDto s : slots) {
            if (activeSessionTab.equals(BookingFormatters.sessionOf(s.startTime))) {
                inTab.add(s);
            }
        }

        if (inTab.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText(R.string.booking_slots_tab_empty);
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_dim));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tv.setPadding(0, dp(12), 0, dp(12));
            tv.setGravity(Gravity.CENTER);
            slotsContainer.addView(tv);
            return;
        }

        // Bố cục 3 cột: build từng hàng khi đủ 3 pill.
        LinearLayout row = newRow();
        for (int i = 0; i < inTab.size(); i++) {
            SlotDto s = inTab.get(i);
            row.addView(buildSlotPill(s));
            if ((i + 1) % 3 == 0) {
                slotsContainer.addView(row);
                row = newRow();
            }
        }
        if (row.getChildCount() > 0) {
            // Fill placeholder để 3 pill cuối không bị dãn.
            while (row.getChildCount() < 3) row.addView(spacer());
            slotsContainer.addView(row);
        }
    }

    private LinearLayout newRow() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        r.setLayoutParams(lp);
        return r;
    }

    private View spacer() {
        View sp = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        sp.setLayoutParams(lp);
        return sp;
    }

    private TextView buildSlotPill(SlotDto slot) {
        boolean available = Boolean.TRUE.equals(slot.isAvailable);
        boolean tooSoon = selectedDate != null && BookingPolicy.violatesMinLead(selectedDate, slot.startTime);
        TextView tv = new TextView(this);
        tv.setText(BookingFormatters.timeRange(slot.startTime, slot.endTime));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setBackgroundResource(R.drawable.bg_booking_time_pill);
        tv.setPadding(dp(10), dp(10), dp(10), dp(10));
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        tv.setLayoutParams(lp);

        if (!available || tooSoon) {
            tv.setAlpha(0.4f);
            tv.setEnabled(false);
            return tv;
        }
        tv.setOnClickListener(v -> selectSlot(slot, tv));
        // Re-apply selected visual state if this is the already-chosen slot.
        if (selectedSlot != null && slot.slotId != null && slot.slotId.equals(selectedSlot.slotId)) {
            tv.setSelected(true);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        return tv;
    }

    private void selectSlot(SlotDto slot, TextView clicked) {
        selectedSlot = slot;
        // Repaint pills in current rows.
        for (int r = 0; r < slotsContainer.getChildCount(); r++) {
            View row = slotsContainer.getChildAt(r);
            if (!(row instanceof LinearLayout)) continue;
            LinearLayout ll = (LinearLayout) row;
            for (int c = 0; c < ll.getChildCount(); c++) {
                View child = ll.getChildAt(c);
                if (child instanceof TextView) {
                    child.setSelected(child == clicked);
                    ((TextView) child).setTextColor(ContextCompat.getColor(
                            this,
                            child == clicked ? R.color.white : R.color.text_primary));
                }
            }
        }
        continueBtn.setEnabled(true);
        continueBtn.setAlpha(1f);
    }

    // ---------- Continue ----------

    private void goToStep2() {
        if (selectedSlot == null || selectedDate == null) {
            Toast.makeText(this, R.string.booking_need_pick_slot, Toast.LENGTH_SHORT).show();
            return;
        }
        SessionManager sm = new SessionManager(this);
        AuthSession s = sm.getSession();
        if (s == null || TextUtils.isEmpty(s.accessToken)) {
            Toast.makeText(this, R.string.booking_need_login, Toast.LENGTH_SHORT).show();
            return;
        }
        ProfileExtras ex = sm.getProfileExtras();
        if (!hasEnoughProfile(s, ex)) {
            Toast.makeText(this, R.string.booking_need_complete_profile, Toast.LENGTH_LONG).show();
            openCompleteProfile();
            return;
        }

        if (BookingPolicy.violatesMinLead(selectedDate, selectedSlot.startTime)) {
            Toast.makeText(this, getString(R.string.booking_min_lead_time_fmt, BookingPolicy.MIN_LEAD_MINUTES),
                    Toast.LENGTH_LONG).show();
            continueBtn.setEnabled(false);
            continueBtn.setAlpha(0.55f);
            renderSlots(lastFetchedSlots);
            return;
        }

        BookingDraft d = new BookingDraft();
        d.doctorId = doctorId;
        d.doctorTitle = doctorTitle;
        d.doctorName = doctorName;
        d.doctorSpecialty = doctorSpecialty;
        d.doctorAvatarBg = doctorAvatarBg;
        d.slotId = selectedSlot.slotId != null ? selectedSlot.slotId : -1;
        d.slotDate = selectedDate;
        d.startTime = selectedSlot.startTime;
        d.endTime = selectedSlot.endTime;
        d.session = BookingFormatters.sessionOf(selectedSlot.startTime);
        d.notes = textOf(R.id.booking_notes);

        d.fullName = s.fullName;
        d.email = s.email;
        d.phone = ex.phone;
        d.dob = ex.dob;
        d.gender = ex.gender;
        d.address = ex.address;

        Intent i = new Intent(this, BookingStep2Activity.class);
        d.writeTo(i);
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_COMPLETE_PROFILE) {
            bindPatientCard();
        }
    }

    // ---------- Utils ----------

    private String textOf(int id) {
        View v = findViewById(id);
        if (v instanceof TextView) {
            CharSequence c = ((TextView) v).getText();
            return c == null ? "" : c.toString().trim();
        }
        return "";
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }

    private static String firstLetter(@Nullable String s) {
        if (s == null || s.isEmpty()) return "?";
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return "?";
        String[] parts = trimmed.split("\\s+");
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
