package com.example.frontend_bookingcare.ui.doctor_panel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.frontend_bookingcare.R;

public class DoctorExaminingFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_examining, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DoctorUiHelper.applyTopInsetPadding(view, 12);

        ((TextView) view.findViewById(R.id.doctor_exam_patient)).setText("Trần Thị Bình");
        ((TextView) view.findViewById(R.id.doctor_exam_age_gender))
                .setText(getString(R.string.doctor_age_gender_fmt, 45, "Nữ"));
        ((TextView) view.findViewById(R.id.doctor_exam_time)).setText("09:30 AM");
        ((TextView) view.findViewById(R.id.doctor_exam_phone)).setText("0912 345 678");
        ((TextView) view.findViewById(R.id.doctor_exam_address)).setText("123 Nguyễn Huệ, Q.1, TP.HCM");

        EditText notes = view.findViewById(R.id.doctor_exam_notes);
        TextView startBtn = view.findViewById(R.id.doctor_exam_start_btn);
        TextView statusChip = view.findViewById(R.id.doctor_exam_status);

        startBtn.setOnClickListener(v -> {
            statusChip.setText(R.string.doctor_status_progress);
            statusChip.setBackgroundResource(R.drawable.bg_doctor_status_progress);
            statusChip.setTextColor(getResources().getColor(R.color.doctor_status_progress_fg, null));
            notes.setEnabled(true);
            notes.setHint("");
            notes.requestFocus();
            Toast.makeText(requireContext(), R.string.doctor_exam_start, Toast.LENGTH_SHORT).show();
        });
    }
}
