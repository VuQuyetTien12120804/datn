package com.example.frontend_bookingcare.onboarding;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public final class OnboardingPage {

    @DrawableRes
    public final int imageRes;
    @StringRes
    public final int titleRes;
    @StringRes
    public final int descRes;

    public OnboardingPage(@DrawableRes int imageRes, @StringRes int titleRes, @StringRes int descRes) {
        this.imageRes = imageRes;
        this.titleRes = titleRes;
        this.descRes = descRes;
    }
}
