package com.example.frontend_bookingcare.ui.doctor_panel;



import android.os.Bundle;

import android.view.MenuItem;



import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;

import androidx.fragment.app.Fragment;

import androidx.fragment.app.FragmentManager;



import com.example.frontend_bookingcare.R;

import com.google.android.material.badge.BadgeDrawable;

import com.google.android.material.bottomnavigation.BottomNavigationView;



public class DoctorMainActivity extends AppCompatActivity {



    private static final String TAG_APPT = "d_appt";

    private static final String TAG_REQ = "d_req";

    private static final String TAG_EXAM = "d_exam";

    private static final String TAG_MSG = "d_msg";

    private static final String TAG_PROFILE = "d_profile";



    private BottomNavigationView bottomNav;

    private Fragment current;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!DoctorSessionGuard.ensureDoctorSession(this)) {
            return;
        }
        setContentView(R.layout.activity_doctor_main);

        bottomNav = findViewById(R.id.doctor_bottom_nav);

        FragmentManager fm = getSupportFragmentManager();

        bottomNav.setOnItemSelectedListener(this::onItemSelected);



        if (savedInstanceState == null) {

            DoctorAppointmentsFragment initial = new DoctorAppointmentsFragment();

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

    @Override
    protected void onResume() {
        super.onResume();
        DoctorSessionGuard.ensureDoctorSession(this);
    }

    /** Mở tab Khám bệnh với ca cụ thể (từ Lịch khám). */

    public void openExaminingForAppointment(int appointmentId) {

        if (appointmentId <= 0) return;

        FragmentManager fm = getSupportFragmentManager();

        DoctorExaminingFragment exam = (DoctorExaminingFragment) fm.findFragmentByTag(TAG_EXAM);

        if (exam == null) {

            exam = DoctorExaminingFragment.newInstance(appointmentId);

        } else {

            exam.openAppointment(appointmentId);

        }

        if (!exam.isAdded()) {

            if (current != null && current.isAdded()) {

                fm.beginTransaction()

                        .hide(current)

                        .add(R.id.doctor_fragment_container, exam, TAG_EXAM)

                        .show(exam)

                        .commit();

            } else {

                fm.beginTransaction()

                        .replace(R.id.doctor_fragment_container, exam, TAG_EXAM)

                        .commitNow();

            }

        } else if (current != null && current != exam && current.isAdded()) {

            fm.beginTransaction().hide(current).show(exam).commit();

        }

        current = exam;

        bottomNav.setSelectedItemId(R.id.doctor_nav_examining);

    }



    private boolean onItemSelected(@NonNull MenuItem item) {

        String tag = tagFor(item.getItemId());

        FragmentManager fm = getSupportFragmentManager();

        Fragment next = fm.findFragmentByTag(tag);



        if (current != null && current == next) {

            return true;

        }



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



    public void setDoctorRequestsPendingCount(int pendingCount) {

        if (bottomNav == null) return;

        int menuId = R.id.doctor_nav_requests;

        if (pendingCount <= 0) {

            bottomNav.removeBadge(menuId);

            return;

        }

        BadgeDrawable badge = bottomNav.getOrCreateBadge(menuId);

        badge.setVisible(true);

        badge.setNumber(Math.min(pendingCount, 99));

        badge.setBackgroundColor(ContextCompat.getColor(this, R.color.doctor_priority_orange));

        badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.white));

    }



    private String tagFor(int id) {

        if (id == R.id.doctor_nav_requests) return TAG_REQ;

        if (id == R.id.doctor_nav_examining) return TAG_EXAM;

        if (id == R.id.doctor_nav_messages) return TAG_MSG;

        if (id == R.id.doctor_nav_account) return TAG_PROFILE;

        return TAG_APPT;

    }



    private Fragment createFor(int id) {

        if (id == R.id.doctor_nav_requests) return new DoctorRequestsFragment();

        if (id == R.id.doctor_nav_examining) return new DoctorExaminingFragment();

        if (id == R.id.doctor_nav_messages) return new DoctorMessagesFragment();

        if (id == R.id.doctor_nav_account) return new DoctorProfileFragment();

        return new DoctorAppointmentsFragment();

    }

}

