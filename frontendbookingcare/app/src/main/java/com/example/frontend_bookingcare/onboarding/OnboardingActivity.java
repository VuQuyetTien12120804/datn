package com.example.frontend_bookingcare.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.frontend_bookingcare.MainActivity;
import com.example.frontend_bookingcare.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_ACCOUNT = "open_account_tab";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (OnboardingStore.isDone(this)) {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        ViewPager2 pager = findViewById(R.id.onboarding_pager);
        TabLayout indicator = findViewById(R.id.onboarding_indicator);
        MaterialButton start = findViewById(R.id.btn_onboarding_start);
        TextView auth = findViewById(R.id.tv_onboarding_auth);
        TextView skip = findViewById(R.id.tv_onboarding_skip);

        List<OnboardingPage> pages = Arrays.asList(
                new OnboardingPage(R.drawable.onboarding_1, R.string.onboarding_1_title, R.string.onboarding_1_desc),
                new OnboardingPage(R.drawable.onboarding_2, R.string.onboarding_2_title, R.string.onboarding_2_desc),
                new OnboardingPage(R.drawable.onboarding_3, R.string.onboarding_3_title, R.string.onboarding_3_desc),
                new OnboardingPage(R.drawable.onboarding_4, R.string.onboarding_4_title, R.string.onboarding_4_desc),
                new OnboardingPage(R.drawable.onboarding_5, R.string.onboarding_5_title, R.string.onboarding_5_desc)
        );
        pager.setAdapter(new OnboardingAdapter(pages));

        new TabLayoutMediator(indicator, pager, (tab, position) -> {
        }).attach();

        start.setOnClickListener(v -> finishAndOpenMain(false));
        skip.setOnClickListener(v -> finishAndOpenMain(false));
        auth.setOnClickListener(v -> finishAndOpenMain(true));
    }

    private void finishAndOpenMain(boolean openAccountTab) {
        OnboardingStore.markDone(this);
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (openAccountTab) {
            i.putExtra(EXTRA_OPEN_ACCOUNT, true);
        }
        startActivity(i);
        finish();
    }
}
