package com.example.frontend_bookingcare.ui.appointments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.R;
import com.google.android.material.tabs.TabLayout;

public class AppointmentsFragment extends Fragment {

    private LinearLayout cards;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cards = view.findViewById(R.id.appointments_cards);
        TabLayout tabs = view.findViewById(R.id.appointments_tabs);
        tabs.addTab(tabs.newTab().setText(R.string.tab_upcoming));
        tabs.addTab(tabs.newTab().setText(R.string.tab_completed));
        tabs.addTab(tabs.newTab().setText(R.string.tab_cancelled));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                fillDemoCards(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        fillDemoCards(0);
    }

    private void fillDemoCards(int tabIndex) {
        cards.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        String status = getString(R.string.tab_upcoming);
        if (tabIndex == 1) status = getString(R.string.tab_completed);
        if (tabIndex == 2) status = getString(R.string.tab_cancelled);

        for (int i = 0; i < 2; i++) {
            View v = inf.inflate(R.layout.item_appointment_card, cards, false);
            TextView st = v.findViewById(R.id.appt_status);
            st.setVisibility(View.VISIBLE);
            st.setText(status);
            cards.addView(v);
        }
    }
}
