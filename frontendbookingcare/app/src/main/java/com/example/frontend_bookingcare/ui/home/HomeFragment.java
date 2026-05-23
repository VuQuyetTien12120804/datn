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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.HorizontalScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.SpecialtyDto;
import com.example.frontend_bookingcare.data.DoctorRepository;
import com.example.frontend_bookingcare.data.SpecialtyRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.doctor_directory.DoctorDetail;
import com.example.frontend_bookingcare.ui.doctor_directory.DoctorDetailActivity;
import com.example.frontend_bookingcare.ui.doctor_directory.DoctorListActivity;
import com.example.frontend_bookingcare.ui.doctor_directory.DoctorMapper;
import com.example.frontend_bookingcare.ui.notifications.NotificationsActivity;
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
            btnSeeAllSpec.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), SpecialtyListActivity.class)));
        }

        // Ô search ở header: bấm vào → mở danh bạ bác sĩ với ô search được focus sẵn
        View searchCard = view.findViewById(R.id.home_search_card);
        if (searchCard != null) {
            searchCard.setOnClickListener(v -> {
                Intent i = new Intent(requireContext(), DoctorListActivity.class);
                i.putExtra(DoctorListActivity.EXTRA_FOCUS_SEARCH, true);
                startActivity(i);
            });
        }

        View bell = view.findViewById(R.id.home_bell);
        if (bell != null) {
            bell.setOnClickListener(v -> startActivity(
                    new Intent(requireContext(), NotificationsActivity.class)));
        }
    }

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

    /**
     * MainActivity đổi tab bằng {@code hide}/{@code show}: không gọi {@code onResume}/{@code onPause}.
     * Sau đăng xuất ở tab khác, quay lại Trang chủ phải đọc lại session — nếu không header vẫn hiện tên cũ.
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            View v = getView();
            if (v != null) {
                refreshUserHeader(v);
            }
            startBannerAutoScroll();
        } else if (hidden) {
            stopBannerAutoScroll();
        }
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
        if (sm.isLoggedIn() && s != null && s.fullName != null && !s.fullName.isEmpty()) {
            displayName = s.fullName;
        } else if (sm.isLoggedIn() && s != null && s.email != null && !s.email.isEmpty()) {
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
                new HomeBannerAdapter.Banner(R.drawable.home_banner_1, HomeBannerAdapter.ACTION_MESSAGES),
                new HomeBannerAdapter.Banner(R.drawable.home_banner_2, HomeBannerAdapter.ACTION_MESSAGES),
                new HomeBannerAdapter.Banner(R.drawable.home_banner_3, HomeBannerAdapter.ACTION_BOOK),
                new HomeBannerAdapter.Banner(R.drawable.home_banner_4, HomeBannerAdapter.ACTION_SPECIALTY),
                new HomeBannerAdapter.Banner(R.drawable.home_banner_5, HomeBannerAdapter.ACTION_BOOK)
        );
        HomeBannerAdapter adapter = new HomeBannerAdapter(banners);
        adapter.setOnBannerClickListener(this::onBannerClicked);
        bannerPager.setAdapter(adapter);

        new TabLayoutMediator(dots, bannerPager, (tab, position) -> {
        }).attach();
    }

    private void onBannerClicked(int action) {
        switch (action) {
            case HomeBannerAdapter.ACTION_BOOK:
                startActivity(new Intent(requireContext(), DoctorListActivity.class));
                break;
            case HomeBannerAdapter.ACTION_MESSAGES:
                openMessagesTab();
                break;
            case HomeBannerAdapter.ACTION_SPECIALTY:
                startActivity(new Intent(requireContext(), SpecialtyListActivity.class));
                break;
            default:
                break;
        }
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

    // ---------- Service grid (3 tiles) ----------

    private void buildServiceGrid(@NonNull View view) {
        GridLayout grid = view.findViewById(R.id.home_service_grid);
        grid.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());

        int[][] items = new int[][]{
                {R.string.home_service_book_doctor, R.drawable.bg_tile_orange, R.drawable.ic_service_stethoscope, 0},
                {R.string.home_service_chat, R.drawable.bg_tile_blue, R.drawable.ic_service_chat, 1},
                {R.string.home_service_specialty, R.drawable.bg_tile_teal, R.drawable.ic_service_microscope, 2},
        };

        for (int i = 0; i < items.length; i++) {
            View tile = inf.inflate(R.layout.item_home_service, grid, false);
            ((FrameLayout) tile.findViewById(R.id.service_icon_bg)).setBackgroundResource(items[i][1]);
            android.widget.ImageView iconView = tile.findViewById(R.id.service_icon_image);
            iconView.setImageResource(items[i][2]);
            ((TextView) tile.findViewById(R.id.service_label)).setText(items[i][0]);

            final int action = items[i][3];
            tile.setOnClickListener(v -> onServiceTileClicked(action));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(i % 3, 1, 1f);
            lp.rowSpec = GridLayout.spec(i / 3, 1);
            tile.setLayoutParams(lp);
            grid.addView(tile);
        }
    }

    /**
     * Điều hướng cho 3 ô dịch vụ trên Home:
     *  0 - Đặt khám bác sĩ      → DoctorListActivity
     *  1 - Chat với bác sĩ      → tab Tin nhắn
     *  2 - Khám theo chuyên khoa → SpecialtyListActivity
     */
    private void onServiceTileClicked(int action) {
        switch (action) {
            case 0:
                startActivity(new Intent(requireContext(), DoctorListActivity.class));
                break;
            case 1:
                openMessagesTab();
                break;
            case 2:
                startActivity(new Intent(requireContext(), SpecialtyListActivity.class));
                break;
            default:
                break;
        }
    }

    private void openMessagesTab() {
        SessionManager sm = new SessionManager(requireContext());
        if (!sm.isLoggedIn()) {
            Toast.makeText(requireContext(), R.string.messages_login_required, Toast.LENGTH_SHORT).show();
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).switchToTab(R.id.nav_account);
            }
            return;
        }
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).switchToTab(R.id.nav_messages);
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
        ProgressBar progress = view.findViewById(R.id.home_doctors_progress);
        TextView status = view.findViewById(R.id.home_doctors_status);
        if (progress != null) progress.setVisibility(View.VISIBLE);
        if (status != null) {
            status.setVisibility(View.GONE);
            status.setText("");
        }

        new DoctorRepository().fetchAllDoctors((data, error) -> {
            if (!isAdded() || getView() == null) return;
            View root = getView();
            ProgressBar p = root.findViewById(R.id.home_doctors_progress);
            TextView st = root.findViewById(R.id.home_doctors_status);
            if (p != null) p.setVisibility(View.GONE);

            if (error != null) {
                renderDoctorsEmpty(root);
                if (st != null) {
                    st.setVisibility(View.VISIBLE);
                    st.setText(getString(R.string.specialty_list_error_fmt, error));
                }
                return;
            }

            java.util.List<DoctorDetail> doctors = DoctorMapper.groupByDoctor(data);
            if (doctors.isEmpty()) {
                renderDoctorsEmpty(root);
            } else {
                if (st != null) st.setVisibility(View.GONE);
                renderDoctors(root, doctors.size() > HOME_DOCTORS_LIMIT
                        ? doctors.subList(0, HOME_DOCTORS_LIMIT) : doctors);
            }
        });
    }

    private void renderDoctorsEmpty(@NonNull View view) {
        LinearLayout row = view.findViewById(R.id.home_doctors_row);
        row.removeAllViews();
        TextView status = view.findViewById(R.id.home_doctors_status);
        if (status != null) {
            status.setVisibility(View.VISIBLE);
            status.setText(getString(R.string.home_doctors_empty));
        }
    }

    private void renderDoctors(@NonNull View view, @NonNull java.util.List<DoctorDetail> doctors) {
        TextView status = view.findViewById(R.id.home_doctors_status);
        if (status != null) {
            status.setVisibility(View.GONE);
        }
        LinearLayout row = view.findViewById(R.id.home_doctors_row);
        row.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (DoctorDetail d : doctors) {
            View card = inf.inflate(R.layout.item_home_doctor, row, false);
            String displayName = (d.title != null && !d.title.isEmpty() ? d.title + " " : "") + d.name;
            ((TextView) card.findViewById(R.id.doctor_name)).setText(displayName);
            ((FrameLayout) card.findViewById(R.id.doctor_avatar_bg)).setBackgroundResource(d.avatarBg);
            ((TextView) card.findViewById(R.id.doctor_avatar_letter)).setText(d.lastNameInitial());
            card.setOnClickListener(v ->
                    startActivity(DoctorDetailActivity.newIntent(requireContext(), d)));
            row.addView(card);
        }
        fitHomeDoctorsToThreePerViewport(view);
    }

    private void openDoctorList() {
        startActivity(new Intent(requireContext(), DoctorListActivity.class));
    }

    /**
     * Make the home doctor row show exactly 3 full items per viewport.
     * Still horizontally scrollable for more doctors.
     */
    private void fitHomeDoctorsToThreePerViewport(@NonNull View root) {
        View rowV = root.findViewById(R.id.home_doctors_row);
        if (!(rowV instanceof LinearLayout)) return;
        LinearLayout row = (LinearLayout) rowV;
        View parent = (View) row.getParent();
        if (!(parent instanceof HorizontalScrollView)) return;
        HorizontalScrollView hsv = (HorizontalScrollView) parent;

        hsv.post(() -> {
            if (!isAdded()) return;
            int viewport = hsv.getWidth();
            if (viewport <= 0) return;

            int rowPad = row.getPaddingStart() + row.getPaddingEnd();
            int hsvPad = hsv.getPaddingStart() + hsv.getPaddingEnd();
            int available = viewport - rowPad - hsvPad;
            if (available <= 0) return;

            int gap = dp(8);
            int width = (available - gap * 2) / 3; // 3 items => 2 gaps inside
            if (width <= 0) return;

            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                LinearLayout.LayoutParams lp;
                if (child.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                } else {
                    lp = new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT);
                }
                lp.width = width;
                lp.leftMargin = (i == 0) ? gap : gap / 2;
                lp.rightMargin = (i == row.getChildCount() - 1) ? gap : gap / 2;
                child.setLayoutParams(lp);
            }
        });
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    // ---------- Specialty grid: chỉ dữ liệu từ API / DB (không fallback giả) ----------

    private static final int HOME_SPECIALTIES_LIMIT = 8;

    private void buildSpecialtyGrid(@NonNull View view) {
        loadSpecialtiesFromApi(view);
    }

    private void loadSpecialtiesFromApi(@NonNull View view) {
        ProgressBar progress = view.findViewById(R.id.home_specialty_progress);
        TextView status = view.findViewById(R.id.home_specialty_status);
        if (progress != null) {
            progress.setVisibility(View.VISIBLE);
        }
        if (status != null) {
            status.setVisibility(View.GONE);
            status.setText("");
        }

        new SpecialtyRepository().fetchAllSpecialties((data, error) -> {
            if (!isAdded() || getView() == null) return;
            View root = getView();
            ProgressBar p = root.findViewById(R.id.home_specialty_progress);
            TextView st = root.findViewById(R.id.home_specialty_status);
            if (p != null) {
                p.setVisibility(View.GONE);
            }

            if (error != null || data == null || data.isEmpty()) {
                GridLayout grid = root.findViewById(R.id.home_specialty_grid);
                grid.removeAllViews();
                if (st != null) {
                    st.setVisibility(View.VISIBLE);
                    st.setText(error != null
                            ? getString(R.string.specialty_list_error_fmt, error)
                            : getString(R.string.specialty_list_empty));
                }
                return;
            }

            if (st != null) {
                st.setVisibility(View.GONE);
            }
            List<SpecialtyDto> visible = data.size() > HOME_SPECIALTIES_LIMIT
                    ? data.subList(0, HOME_SPECIALTIES_LIMIT)
                    : data;
            renderSpecialties(root, visible);
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
            tile.setOnClickListener(v -> openDoctorsBySpecialty(s));
            grid.addView(tile);
        }
    }

    /**
     * Mở danh sách bác sĩ lọc theo specialtyId → DoctorListActivity sẽ gọi đúng
     * endpoint GET /api/v1/doctors/specialty/{id}. Nếu DTO không có id (bất thường),
     * fallback sang search theo tên.
     */
    private void openDoctorsBySpecialty(@NonNull SpecialtyDto s) {
        Intent intent = new Intent(requireContext(), DoctorListActivity.class);
        if (s.specialtyId != null && s.specialtyId > 0) {
            intent.putExtra(DoctorListActivity.EXTRA_SPECIALTY_ID, s.specialtyId.intValue());
            if (s.name != null) {
                intent.putExtra(DoctorListActivity.EXTRA_SPECIALTY_NAME, s.name);
            }
        } else if (s.name != null && !s.name.isEmpty()) {
            intent.putExtra(DoctorListActivity.EXTRA_INITIAL_QUERY, s.name);
        }
        startActivity(intent);
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
