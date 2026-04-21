package com.example.frontend_bookingcare.ui.doctor_panel;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.frontend_bookingcare.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DoctorMainActivity extends AppCompatActivity {

    private static final String TAG_APPT = "d_appt";
    private static final String TAG_REQ = "d_req";
    private static final String TAG_EXAM = "d_exam";
    private static final String TAG_PROFILE = "d_profile";

    private BottomNavigationView bottomNav;
    private Fragment current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_main);
        bottomNav = findViewById(R.id.doctor_bottom_nav);
        FragmentManager fm = getSupportFragmentManager();
        bottomNav.setOnItemSelectedListener(this::onItemSelected);

        if (savedInstanceState == null) {
            DoctorAppointmentsFragment initial = new DoctorAppointmentsFragment();
            // commitNow để fragment đầu tiên attach ngay, tránh crash khi tab chuyển quá sớm
            // sau khi activity vừa được mở từ flow đăng nhập.
            fm.beginTransaction().replace(R.id.doctor_fragment_container, initial, TAG_APPT).commitNow();
            current = initial;
            bottomNav.setSelectedItemId(R.id.doctor_nav_appointments);
        } else {
            Fragment existing = fm.findFragmentById(R.id.doctor_fragment_container);
            if (existing == null) {
                existing = fm.findFragmentByTag(TAG_APPT);
            }
            if (existing == null) {
                existing = new DoctorAppointmentsFragment();
                fm.beginTransaction().replace(R.id.doctor_fragment_container, existing, TAG_APPT).commitNow();
            }
            current = existing;
        }
    }

    private boolean onItemSelected(@NonNull MenuItem item) {
        String tag = tagFor(item.getItemId());
        FragmentManager fm = getSupportFragmentManager();
        Fragment next = fm.findFragmentByTag(tag);

        if (current != null && current == next) {
            return true;
        }

        // Nếu fragment hiện tại chưa thực sự attach (state restore / launch timing),
        // dùng replace an toàn thay vì hide/show để tránh IllegalStateException.
        if (current == null || !current.isAdded()) {
            if (next == null) {
                next = createFor(item.getItemId());
            }
            fm.beginTransaction()
                    .replace(R.id.doctor_fragment_container, next, tag)
                    .commitNow();
            current = next;
            return true;
        }

        if (next == null) {
            next = createFor(item.getItemId());
            fm.beginTransaction()
                    .add(R.id.doctor_fragment_container, next, tag)
                    .hide(current)
                    .show(next)
                    .commit();
        } else {
            fm.beginTransaction().hide(current).show(next).commit();
        }
        current = next;
        return true;
    }

    private String tagFor(int id) {
        if (id == R.id.doctor_nav_requests) return TAG_REQ;
        if (id == R.id.doctor_nav_examining) return TAG_EXAM;
        if (id == R.id.doctor_nav_account) return TAG_PROFILE;
        return TAG_APPT;
    }

    private Fragment createFor(int id) {
        if (id == R.id.doctor_nav_requests) return new DoctorRequestsFragment();
        if (id == R.id.doctor_nav_examining) return new DoctorExaminingFragment();
        if (id == R.id.doctor_nav_account) return new DoctorProfileFragment();
        return new DoctorAppointmentsFragment();
    }
}
