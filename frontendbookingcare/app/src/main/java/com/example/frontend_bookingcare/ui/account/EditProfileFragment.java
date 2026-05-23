package com.example.frontend_bookingcare.ui.account;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.R;
import com.example.frontend_bookingcare.api.UpdatePatientProfileRequest;
import com.example.frontend_bookingcare.data.PatientProfileRepository;
import com.example.frontend_bookingcare.session.AuthSession;
import com.example.frontend_bookingcare.session.ProfileExtras;
import com.example.frontend_bookingcare.session.SessionManager;
import com.example.frontend_bookingcare.ui.booking.BookingFormatters;
import com.example.frontend_bookingcare.ui.common.UnicodeInputHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class EditProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.edit_profile_toolbar);
        AccountUiHelper.bindToolbarBack(this, toolbar);

        AccountFragment parent = (AccountFragment) getParentFragment();
        if (parent == null) return;
        SessionManager sm = parent.getSessionManager();
        ProfileExtras ex = sm.getProfileExtras();

        TextInputEditText phone = view.findViewById(R.id.input_phone);
        TextInputEditText dob = view.findViewById(R.id.input_dob);
        MaterialAutoCompleteTextView gender = view.findViewById(R.id.input_gender);
        TextInputEditText address = view.findViewById(R.id.input_address);
        UnicodeInputHelper.enableMultilineText(address);
        phone.setText(ex.phone);
        dob.setText(ex.dob);
        gender.setText(BookingFormatters.displayGender(requireContext(), ex.gender));
        address.setText(ex.address);

        gender.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, BookingFormatters.genderLabels(requireContext())));
        gender.setOnClickListener(v -> gender.showDropDown());

        dob.setFocusable(false);
        dob.setClickable(true);
        dob.setOnClickListener(v -> openDobPicker(dob));

        MaterialButton save = view.findViewById(R.id.btn_save_profile);
        save.setOnClickListener(v -> {
            String phoneVal = text(phone);
            String dobVal = text(dob);
            String genderVal = textAny(gender);
            String addressVal = text(address);

            if (!BookingFormatters.isValidVnPhone(phoneVal)) {
                Toast.makeText(requireContext(), R.string.profile_phone_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!BookingFormatters.isValidDobDdMmYyyy(dobVal)) {
                Toast.makeText(requireContext(), R.string.profile_dob_invalid, Toast.LENGTH_SHORT).show();
                return;
            }

            String genderEnum = BookingFormatters.genderToEnum(genderVal);
            ProfileExtras next = new ProfileExtras(phoneVal, dobVal, genderEnum, addressVal);
            AuthSession s = sm.getSession();
            if (!sm.isLoggedIn() || s == null || TextUtils.isEmpty(s.accessToken)) {
                Toast.makeText(requireContext(), R.string.booking_need_login, Toast.LENGTH_SHORT).show();
                return;
            }
            save.setEnabled(false);
            String bearer = "Bearer " + s.accessToken;
                new PatientProfileRepository().updateMe(
                        bearer,
                        new UpdatePatientProfileRequest(next.phone, next.dob, next.gender, next.address),
                        new PatientProfileRepository.RepoCallback() {
                            @Override
                            public void onSuccess(com.example.frontend_bookingcare.api.ApiEnvelope env) {
                                requireActivity().runOnUiThread(() -> {
                                    sm.saveProfileExtras(next);
                                    Toast.makeText(requireContext(), R.string.edit_profile_saved_toast, Toast.LENGTH_SHORT).show();
                                    parent.getChildFragmentManager().popBackStack();
                                });
                            }

                            @Override
                            public void onError(String message) {
                                requireActivity().runOnUiThread(() -> {
                                    save.setEnabled(true);
                                    Toast.makeText(requireContext(),
                                            getString(R.string.save_failed_fmt, message), Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                );
        });
    }

    private void openDobPicker(TextInputEditText dobField) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            String formatted = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            dobField.setText(formatted);
        }, cal.get(Calendar.YEAR) - 25, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
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
}
