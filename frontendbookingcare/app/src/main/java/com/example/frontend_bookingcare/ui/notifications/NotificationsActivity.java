package com.example.frontend_bookingcare.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.data.PatientAppointmentsRepository;
import com.example.frontend_bookingcare.onboarding.OnboardingActivity;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.appointments.AppointmentTicketActivity;
import com.example.frontend_bookingcare.ui.common.HeaderInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final int TAB_ALL = 0;
    private static final int TAB_APPOINTMENTS = 1;
    private static final int TAB_HEALTH = 2;

    private final PatientAppointmentsRepository repository = new PatientAppointmentsRepository();
    private final List<NotificationMapper.Item> allItems = new ArrayList<>();

    private TabLayout tabs;
    private RecyclerView list;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptyHint;
    private MaterialButton emptyLoginButton;
    private SwipeRefreshLayout refresh;
    private NotificationsAdapter adapter;
    private int selectedTab = TAB_ALL;
    @Nullable private String loadError;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_notifications);

        MaterialToolbar toolbar = findViewById(R.id.notifications_toolbar);
        HeaderInsets.applyToToolbar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tabs = findViewById(R.id.notifications_tabs);
        list = findViewById(R.id.notifications_list);
        emptyState = findViewById(R.id.notifications_empty);
        emptyTitle = findViewById(R.id.notifications_empty_title);
        emptyHint = findViewById(R.id.notifications_empty_hint);
        emptyLoginButton = findViewById(R.id.notifications_empty_login);
        refresh = findViewById(R.id.notifications_refresh);

        adapter = new NotificationsAdapter(this::openTicket);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        tabs.addTab(tabs.newTab().setText(R.string.notifications_tab_all));
        tabs.addTab(tabs.newTab().setText(R.string.notifications_tab_appointments));
        tabs.addTab(tabs.newTab().setText(R.string.notifications_tab_health));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                applyFilter();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        if (emptyLoginButton != null) {
            emptyLoginButton.setOnClickListener(v -> openAccountTab());
        }

        refresh.setOnRefreshListener(this::load);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        SessionManager sm = new SessionManager(this);
        AuthSession session = sm.getSession();
        loadError = null;
        if (!sm.isLoggedIn() || session == null || session.accessToken == null || session.accessToken.isEmpty()) {
            allItems.clear();
            finishLoading();
            selectedTab = tabs != null ? tabs.getSelectedTabPosition() : TAB_ALL;
            adapter.setItems(new ArrayList<>());
            showGuestEmpty();
            return;
        }
        String bearer = "Bearer " + session.accessToken;
        refresh.setRefreshing(true);
        repository.fetch(bearer, "UPCOMING", (data, err) -> runOnUiThread(() -> {
            allItems.clear();
            loadError = err;
            if (err == null) {
                allItems.addAll(NotificationMapper.fromAppointments(this, data));
            }
            finishLoading();
            applyFilter();
        }));
    }

    private void finishLoading() {
        if (refresh != null) refresh.setRefreshing(false);
    }

    private void applyFilter() {
        if (!isLoggedIn()) {
            showGuestEmpty();
            return;
        }
        if (loadError != null) {
            showErrorEmpty(loadError);
            return;
        }

        List<NotificationMapper.Item> visible = new ArrayList<>();
        if (selectedTab == TAB_HEALTH) {
            // Chưa có nguồn tin y tế — để trống theo mockup.
        } else if (selectedTab == TAB_APPOINTMENTS) {
            for (NotificationMapper.Item item : allItems) {
                if (item.category == NotificationMapper.Category.APPOINTMENT) {
                    visible.add(item);
                }
            }
        } else {
            visible.addAll(allItems);
        }
        adapter.setItems(visible);
        updateEmptyState(visible.isEmpty());
    }

    private boolean isLoggedIn() {
        SessionManager sm = new SessionManager(this);
        AuthSession session = sm.getSession();
        return sm.isLoggedIn()
                && session != null
                && session.accessToken != null
                && !session.accessToken.isEmpty();
    }

    private void showGuestEmpty() {
        if (emptyState == null || list == null) return;
        emptyState.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        if (emptyTitle != null) emptyTitle.setText(R.string.notifications_empty_title);
        if (emptyHint != null) emptyHint.setText(R.string.notifications_empty_login_hint);
        if (emptyLoginButton != null) emptyLoginButton.setVisibility(View.VISIBLE);
    }

    private void showErrorEmpty(@NonNull String error) {
        if (emptyState == null || list == null) return;
        adapter.setItems(new ArrayList<>());
        emptyState.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        if (emptyTitle != null) emptyTitle.setText(R.string.notifications_error_title);
        if (emptyHint != null) emptyHint.setText(error);
        if (emptyLoginButton != null) emptyLoginButton.setVisibility(View.GONE);
    }

    private void updateEmptyState(boolean empty) {
        if (emptyState == null || list == null) return;
        if (empty) {
            emptyState.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
            if (emptyTitle != null) {
                emptyTitle.setText(R.string.notifications_empty_title);
            }
            if (emptyHint != null) {
                int hint = R.string.notifications_empty_all_hint;
                if (selectedTab == TAB_APPOINTMENTS) {
                    hint = R.string.notifications_empty_appointments_hint;
                } else if (selectedTab == TAB_HEALTH) {
                    hint = R.string.notifications_empty_health_hint;
                }
                emptyHint.setText(hint);
            }
            if (emptyLoginButton != null) emptyLoginButton.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            if (emptyLoginButton != null) emptyLoginButton.setVisibility(View.GONE);
        }
    }

    private void openAccountTab() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(OnboardingActivity.EXTRA_OPEN_ACCOUNT, true);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    private void openTicket(@NonNull NotificationMapper.Item item) {
        if (item.category != NotificationMapper.Category.APPOINTMENT) return;
        startActivity(AppointmentTicketActivity.newIntent(
                this,
                item.appointmentId,
                item.doctorId,
                item.doctorName != null ? item.doctorName : "",
                item.date != null ? item.date : "",
                item.start != null ? item.start : "",
                item.end != null ? item.end : "",
                item.status
        ));
    }
}
