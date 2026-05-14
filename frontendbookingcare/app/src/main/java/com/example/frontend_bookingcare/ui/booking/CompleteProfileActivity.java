package com.example.frontend_bookingcare.ui.booking;

import android.content.Context;
import android.content.Intent;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.UpdatePatientProfileRequest;
import com.example.frontend_bookingcare.data.PatientProfileRepository;
import com.example.frontend_bookingcare.session.ProfileExtras;
import com.example.frontend_bookingcare.session.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class CompleteProfileActivity extends AppCompatActivity {

    public static Intent newIntent(Context ctx) {
        return new Intent(ctx, CompleteProfileActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_complete_profile);

        MaterialToolbar tb = findViewById(R.id.complete_profile_toolbar);
        applyTopInsetToToolbar(tb);
        tb.setNavigationOnClickListener(v -> finish());

        SessionManager sm = new SessionManager(this);
        ProfileExtras ex = sm.getProfileExtras();

        TextInputEditText phone = findViewById(R.id.cp_phone);
        TextInputEditText dob = findViewById(R.id.cp_dob);
        MaterialAutoCompleteTextView gender = findViewById(R.id.cp_gender);
        TextInputEditText address = findViewById(R.id.cp_address);
        phone.setText(ex.phone);
        dob.setText(ex.dob);
        gender.setText(ex.gender);
        address.setText(ex.address);

        // Gender dropdown
        String[] genders = new String[]{"male", "female", "other"};
        gender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genders));
        gender.setOnClickListener(v -> gender.showDropDown());

        // DOB picker
        dob.setOnClickListener(v -> openDobPicker(dob));

        MaterialButton save = findViewById(R.id.cp_save);
        save.setOnClickListener(v -> {
            String p = text(phone);
            String d = text(dob);
            String g = textAny(gender);
            String a = text(address);
            if (TextUtils.isEmpty(p) || TextUtils.isEmpty(d) || TextUtils.isEmpty(g) || TextUtils.isEmpty(a)) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!sm.isLoggedIn()) {
                sm.saveProfileExtras(new ProfileExtras(p, d, g, a));
                setResult(RESULT_OK);
                finish();
                return;
            }

            save.setEnabled(false);
            String bearer = "Bearer " + sm.getSession().accessToken;
            new PatientProfileRepository().updateMe(
                    bearer,
                    new UpdatePatientProfileRequest(p, d, g, a),
                    new PatientProfileRepository.RepoCallback() {
                        @Override
                        public void onSuccess(com.example.frontend_bookingcare.api.ApiEnvelope env) {
                            runOnUiThread(() -> {
                                sm.saveProfileExtras(new ProfileExtras(p, d, g, a));
                                Toast.makeText(CompleteProfileActivity.this, "Đã lưu hồ sơ", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                save.setEnabled(true);
                                Toast.makeText(CompleteProfileActivity.this, "Lưu thất bại: " + message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
            );
        });
    }

    private static String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private static String textAny(View v) {
        if (v instanceof TextInputEditText) return text((TextInputEditText) v);
        if (v instanceof android.widget.TextView) {
            CharSequence c = ((android.widget.TextView) v).getText();
            return c != null ? c.toString().trim() : "";
        }
        return "";
    }

    private void openDobPicker(TextInputEditText dob) {
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String dd = String.format("%02d", dayOfMonth);
                    String mm = String.format("%02d", month + 1);
                    dob.setText(dd + "/" + mm + "/" + year);
                },
                y, m, d
        );
        dlg.show();
    }

    private void applyTopInsetToToolbar(MaterialToolbar toolbar) {
        final int basePadTop = toolbar.getPaddingTop();
        final int basePadBottom = toolbar.getPaddingBottom();
        final int basePadStart = toolbar.getPaddingStart();
        final int basePadEnd = toolbar.getPaddingEnd();
        final int extra = dp(6); // xuống dưới camera 1 chút

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPaddingRelative(basePadStart, basePadTop + topInset + extra, basePadEnd, basePadBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}

