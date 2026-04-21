package com.example.frontend_bookingcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.frontend_bookingcare.onboarding.OnboardingActivity;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.RoleRouter;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.account.AccountFragment;
import com.example.frontend_bookingcare.ui.appointments.AppointmentsFragment;
import com.example.frontend_bookingcare.ui.consult.ConsultFragment;
import com.example.frontend_bookingcare.ui.home.HomeFragment;
import com.example.frontend_bookingcare.ui.messages.MessagesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_HOME = "TAG_HOME";
    private static final String TAG_APPOINTMENTS = "TAG_APPOINTMENTS";
    private static final String TAG_CONSULT = "TAG_CONSULT";
    private static final String TAG_MESSAGES = "TAG_MESSAGES";
    private static final String TAG_ACCOUNT = "TAG_ACCOUNT";
    private static final String STATE_CURRENT_TAG = "current_tab_tag";

    private BottomNavigationView bottomNav;
    private Fragment current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthSession s = new SessionManager(this).getSession();
        // Nếu role này có Activity chuyên biệt (ví dụ doctor → DoctorMainActivity),
        // chuyển sang đó ngay thay vì dựng UI mặc định dành cho bệnh nhân.
        Intent roleIntent = RoleRouter.intentFor(this, s);
        if (roleIntent != null) {
            startActivity(roleIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottom_nav);
        FragmentManager fm = getSupportFragmentManager();

        boolean openAccount = getIntent() != null && getIntent().getBooleanExtra(OnboardingActivity.EXTRA_OPEN_ACCOUNT, false);

        if (savedInstanceState == null) {
            if (openAccount) {
                AccountFragment account = new AccountFragment();
                fm.beginTransaction().add(R.id.fragment_container, account, TAG_ACCOUNT).commit();
                current = account;
                bottomNav.setSelectedItemId(R.id.nav_account);
            } else {
                HomeFragment home = new HomeFragment();
                fm.beginTransaction().add(R.id.fragment_container, home, TAG_HOME).commit();
                current = home;
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        } else {
            String tag = savedInstanceState.getString(STATE_CURRENT_TAG, TAG_HOME);
            current = fm.findFragmentByTag(tag);
            if (current == null) {
                current = new HomeFragment();
                fm.beginTransaction().add(R.id.fragment_container, current, TAG_HOME).commit();
            }
            bottomNav.setSelectedItemId(tagToMenuId(tag));
        }

        bottomNav.setOnItemSelectedListener(this::onBottomItemSelected);
    }

    private int tagToMenuId(String tag) {
        if (TAG_APPOINTMENTS.equals(tag)) return R.id.nav_appointments;
        if (TAG_CONSULT.equals(tag)) return R.id.nav_consult;
        if (TAG_MESSAGES.equals(tag)) return R.id.nav_messages;
        if (TAG_ACCOUNT.equals(tag)) return R.id.nav_account;
        return R.id.nav_home;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (current != null && current.getTag() != null) {
            outState.putString(STATE_CURRENT_TAG, current.getTag());
        }
    }

    private boolean onBottomItemSelected(@NonNull MenuItem item) {
        String tag = menuIdToTag(item.getItemId());
        FragmentManager fm = getSupportFragmentManager();
        Fragment next = fm.findFragmentByTag(tag);
        if (next == null) {
            next = createFragment(item.getItemId());
            fm.beginTransaction()
                    .add(R.id.fragment_container, next, tag)
                    .hide(current)
                    .show(next)
                    .commit();
        } else {
            fm.beginTransaction()
                    .hide(current)
                    .show(next)
                    .commit();
        }
        current = next;
        return true;
    }

    private String menuIdToTag(int id) {
        if (id == R.id.nav_appointments) return TAG_APPOINTMENTS;
        if (id == R.id.nav_consult) return TAG_CONSULT;
        if (id == R.id.nav_messages) return TAG_MESSAGES;
        if (id == R.id.nav_account) return TAG_ACCOUNT;
        return TAG_HOME;
    }

    private Fragment createFragment(int menuId) {
        if (menuId == R.id.nav_appointments) return new AppointmentsFragment();
        if (menuId == R.id.nav_consult) return new ConsultFragment();
        if (menuId == R.id.nav_messages) return new MessagesFragment();
        if (menuId == R.id.nav_account) return new AccountFragment();
        return new HomeFragment();
    }

    /** Sau đăng nhập / đăng ký: chuyển tab Trang chủ và làm mới phần chào + tên. */
    public void navigateToHomeAfterAuth() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.post(() -> {
            FragmentManager fm = getSupportFragmentManager();
            Fragment home = fm.findFragmentByTag(TAG_HOME);
            if (home instanceof HomeFragment) {
                ((HomeFragment) home).refreshUserHeader();
            }
        });
    }
}
