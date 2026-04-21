package com.example.frontend_bookingcare.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.SpecialtyDto;
import com.example.frontend_bookingcare.data.DoctorRepository;
import com.example.frontend_bookingcare.data.SpecialtyRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.doctor_directory.DoctorDetail;
import com.example.frontend_bookingcare.ui.doctor_directory.DoctorListActivity;
import com.example.frontend_bookingcare.ui.doctor_directory.DoctorMapper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final long BANNER_AUTO_MS = 4500L;

    private ViewPager2 bannerPager;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private Runnable bannerTick;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyTopInsetToHeader(view);
        refreshUserHeader(view);
        setupBanners(view);
        buildServiceGrid(view);
        buildDoctorsRow(view);
        buildSpecialtyGrid(view);

        View btnSeeAllSpec = view.findViewById(R.id.btn_see_all_specialties);
        if (btnSeeAllSpec != null) {
            btnSeeAllSpec.setOnClickListener(v -> startActivity(
                    new Intent(requireContext(), SpecialtyListActivity.class)));
        }
    }

    /**
     * Đẩy header xuống dưới status bar / camera cutout (WindowInsets) — giống cách
     * AccountUiHelper áp dụng cho các màn hình trong Tài khoản.
     */
    private void applyTopInsetToHeader(@NonNull View view) {
        View header = view.findViewById(R.id.home_header);
        if (header == null) return;
        final int basePadTop = header.getPaddingTop();
        final int startPad = header.getPaddingStart();
        final int endPad = header.getPaddingEnd();
        final int bottomPad = header.getPaddingBottom();
        final int extra = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());

        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPaddingRelative(startPad, basePadTop + topInset + extra, endPad, bottomPad);
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) {
            refreshUserHeader(v);
        }
        startBannerAutoScroll();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopBannerAutoScroll();
    }

    public void refreshUserHeader() {
        View v = getView();
        if (v != null) {
            refreshUserHeader(v);
        }
    }

    private void refreshUserHeader(@NonNull View view) {
        TextView greeting = view.findViewById(R.id.home_greeting);
        TextView name = view.findViewById(R.id.home_user_name);
        TextView avatarLetter = view.findViewById(R.id.home_avatar_letter);
        SessionManager sm = new SessionManager(requireContext());
        AuthSession s = sm.getSession();
        greeting.setText(pickGreeting());

        String displayName;
        if (s != null && s.fullName != null && !s.fullName.isEmpty()) {
            displayName = s.fullName;
        } else if (s != null && s.email != null && !s.email.isEmpty()) {
            displayName = s.email;
        } else {
            displayName = getString(R.string.home_guest_name);
        }
        name.setText(displayName);
        avatarLetter.setText(firstLetter(displayName));
    }

    private String pickGreeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 12) return getString(R.string.home_greeting_morning);
        if (h >= 12 && h < 18) return getString(R.string.home_greeting_afternoon);
        if (h >= 18 && h < 22) return getString(R.string.home_greeting_evening);
        return getString(R.string.home_greeting_night);
    }

    private String firstLetter(String s) {
        if (s == null || s.isEmpty()) return "?";
        String trimmed = s.trim();
        return trimmed.isEmpty() ? "?" : trimmed.substring(0, 1).toUpperCase();
    }

    // ---------- Banner ----------

    private void setupBanners(@NonNull View view) {
        bannerPager = view.findViewById(R.id.home_banner_pager);
        TabLayout dots = view.findViewById(R.id.home_banner_dots);

        List<HomeBannerAdapter.Banner> banners = Arrays.asList(
                new HomeBannerAdapter.Banner(R.drawable.home_banner_1),
                new HomeBannerAdapter.Banner(R.drawable.home_banner_2),
                new HomeBannerAdapter.Banner(R.drawable.home_banner_3),
                new HomeBannerAdapter.Banner(R.drawable.home_banner_4),
                new HomeBannerAdapter.Banner(R.drawable.home_banner_5)
        );
        bannerPager.setAdapter(new HomeBannerAdapter(banners));

        new TabLayoutMediator(dots, bannerPager, (tab, position) -> {
        }).attach();
    }

    private void startBannerAutoScroll() {
        stopBannerAutoScroll();
        if (bannerPager == null || bannerPager.getAdapter() == null) return;
        final int count = bannerPager.getAdapter().getItemCount();
        if (count <= 1) return;
        bannerTick = () -> {
            if (bannerPager == null) return;
            int next = (bannerPager.getCurrentItem() + 1) % count;
            bannerPager.setCurrentItem(next, true);
            bannerHandler.postDelayed(bannerTick, BANNER_AUTO_MS);
        };
        bannerHandler.postDelayed(bannerTick, BANNER_AUTO_MS);
    }

    private void stopBannerAutoScroll() {
        if (bannerTick != null) {
            bannerHandler.removeCallbacks(bannerTick);
            bannerTick = null;
        }
    }

    // ---------- Service grid (5 tiles, 3 columns) ----------

    private void buildServiceGrid(@NonNull View view) {
        GridLayout grid = view.findViewById(R.id.home_service_grid);
        grid.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());

        int[][] items = new int[][]{
                {R.string.home_service_book_doctor, R.drawable.bg_tile_orange, 0},
                {R.string.home_service_chat, R.drawable.bg_tile_blue, 1},
                {R.string.home_service_video, R.drawable.bg_tile_purple, 2},
                {R.string.home_service_records, R.drawable.bg_tile_green, 3},
                {R.string.home_service_vaccine, R.drawable.bg_tile_pink, 4},
                {R.string.home_service_specialty, R.drawable.bg_tile_teal, 5},
        };
        String[] glyphs = new String[]{"🩺", "💬", "📹", "💚", "💉", "🔬"};

        for (int i = 0; i < items.length; i++) {
            View tile = inf.inflate(R.layout.item_home_service, grid, false);
            ((FrameLayout) tile.findViewById(R.id.service_icon_bg)).setBackgroundResource(items[i][1]);
            ((TextView) tile.findViewById(R.id.service_icon)).setText(glyphs[i]);
            ((TextView) tile.findViewById(R.id.service_label)).setText(items[i][0]);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(i % 3, 1, 1f);
            lp.rowSpec = GridLayout.spec(i / 3, 1);
            tile.setLayoutParams(lp);
            grid.addView(tile);
        }
    }

    // ---------- Doctors row ----------

    private static final int HOME_DOCTORS_LIMIT = 8;

    private void buildDoctorsRow(@NonNull View view) {
        View seeAll = view.findViewById(R.id.home_doctors_see_all);
        if (seeAll != null) {
            seeAll.setOnClickListener(v -> openDoctorList());
        }
        loadDoctorsFromApi(view);
    }

    private void loadDoctorsFromApi(@NonNull View view) {
        new DoctorRepository().fetchAllDoctors((data, error) -> {
            if (!isAdded() || getView() == null) return;
            java.util.List<DoctorDetail> doctors = error != null
                    ? java.util.Collections.<DoctorDetail>emptyList()
                    : DoctorMapper.groupByDoctor(data);
            if (doctors.isEmpty()) {
                renderFallbackDoctors(view);
            } else {
                renderDoctors(view, doctors.size() > HOME_DOCTORS_LIMIT ? doctors.subList(0, HOME_DOCTORS_LIMIT) : doctors);
            }
        });
    }

    private void renderDoctors(@NonNull View view, @NonNull java.util.List<DoctorDetail> doctors) {
        LinearLayout row = view.findViewById(R.id.home_doctors_row);
        row.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (DoctorDetail d : doctors) {
            View card = inf.inflate(R.layout.item_home_doctor, row, false);
            String displayName = (d.title != null && !d.title.isEmpty() ? d.title + " " : "") + d.name;
            ((TextView) card.findViewById(R.id.doctor_name)).setText(displayName);
            ((FrameLayout) card.findViewById(R.id.doctor_avatar_bg)).setBackgroundResource(d.avatarBg);
            ((TextView) card.findViewById(R.id.doctor_avatar_letter)).setText(d.lastNameInitial());
            card.setOnClickListener(v -> openDoctorList());
            row.addView(card);
        }
    }

    private void renderFallbackDoctors(@NonNull View view) {
        LinearLayout row = view.findViewById(R.id.home_doctors_row);
        row.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        int[][] docs = new int[][]{
                {R.string.home_demo_doctor_1, R.drawable.bg_tile_pink},
                {R.string.home_demo_doctor_2, R.drawable.bg_tile_blue},
                {R.string.home_demo_doctor_3, R.drawable.bg_tile_teal},
                {R.string.home_demo_doctor_4, R.drawable.bg_tile_amber},
        };
        for (int[] d : docs) {
            View card = inf.inflate(R.layout.item_home_doctor, row, false);
            String name = getString(d[0]);
            ((TextView) card.findViewById(R.id.doctor_name)).setText(name);
            ((FrameLayout) card.findViewById(R.id.doctor_avatar_bg)).setBackgroundResource(d[1]);
            ((TextView) card.findViewById(R.id.doctor_avatar_letter)).setText(avatarLetter(name));
            card.setOnClickListener(v -> openDoctorList());
            row.addView(card);
        }
    }

    private void openDoctorList() {
        startActivity(new Intent(requireContext(), DoctorListActivity.class));
    }

    private String avatarLetter(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? "?" : last.substring(0, 1).toUpperCase();
    }

    // ---------- Specialty grid (lấy từ API, fallback demo nếu lỗi) ----------

    private static final int HOME_SPECIALTIES_LIMIT = 8;

    private void buildSpecialtyGrid(@NonNull View view) {
        loadSpecialtiesFromApi(view);
    }

    private void loadSpecialtiesFromApi(@NonNull View view) {
        new SpecialtyRepository().fetchAllSpecialties((data, error) -> {
            if (!isAdded() || getView() == null) return;
            if (error != null || data == null || data.isEmpty()) {
                renderFallbackSpecialties(view);
            } else {
                java.util.List<SpecialtyDto> visible = data.size() > HOME_SPECIALTIES_LIMIT
                        ? data.subList(0, HOME_SPECIALTIES_LIMIT)
                        : data;
                renderSpecialties(view, visible);
            }
        });
    }

    private void renderSpecialties(@NonNull View view, @NonNull java.util.List<SpecialtyDto> specialties) {
        GridLayout grid = view.findViewById(R.id.home_specialty_grid);
        grid.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (int i = 0; i < specialties.size(); i++) {
            SpecialtyDto s = specialties.get(i);
            View tile = inf.inflate(R.layout.item_home_specialty, grid, false);
            ((TextView) tile.findViewById(R.id.specialty_icon)).setText(SpecialtyIcons.iconFor(s.code, s.name));
            ((TextView) tile.findViewById(R.id.specialty_label)).setText(s.name != null ? s.name : "");
            tile.setLayoutParams(gridCell(i, 4));
            grid.addView(tile);
        }
    }

    private void renderFallbackSpecialties(@NonNull View view) {
        GridLayout grid = view.findViewById(R.id.home_specialty_grid);
        grid.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());

        int[] labels = new int[]{
                R.string.home_spec_allergy,
                R.string.home_spec_traditional,
                R.string.home_spec_pulmo,
                R.string.home_spec_sport,
                R.string.home_spec_ortho,
                R.string.home_spec_ob,
                R.string.home_spec_eye,
                R.string.home_spec_uro,
        };
        String[] glyphs = new String[]{"🛡️", "☯️", "🫁", "🏃", "🦴", "🤰", "👁️", "♂️"};

        for (int i = 0; i < labels.length; i++) {
            View tile = inf.inflate(R.layout.item_home_specialty, grid, false);
            ((TextView) tile.findViewById(R.id.specialty_icon)).setText(glyphs[i]);
            ((TextView) tile.findViewById(R.id.specialty_label)).setText(labels[i]);
            tile.setLayoutParams(gridCell(i, 4));
            grid.addView(tile);
        }
    }

    private static GridLayout.LayoutParams gridCell(int index, int columns) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(index % columns, 1, 1f);
        lp.rowSpec = GridLayout.spec(index / columns, 1);
        return lp;
    }
}
