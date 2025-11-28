package tn.esprit.presentation.appointment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import tn.esprit.R;

public class DoctorAppointmentDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PATIENT_USER_ID = "arg_patient_user_id";
    private static final String ARG_PATIENT_NAME = "arg_patient_name";
    private static final String ARG_START_ISO = "arg_start_iso";
    private static final String ARG_END_ISO = "arg_end_iso";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_REASON = "arg_reason";
    private static final String ARG_TELE = "arg_tele";

    public static void show(@NonNull FragmentManager fm,
                            long patientUserId,
                            @NonNull String patientName,
                            @Nullable String startIso,
                            @Nullable String endIso,
                            @Nullable String status,
                            @Nullable String reason,
                            @Nullable Boolean tele) {

        DoctorAppointmentDetailBottomSheet sheet = new DoctorAppointmentDetailBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PATIENT_USER_ID, patientUserId);
        args.putString(ARG_PATIENT_NAME, patientName);
        args.putString(ARG_START_ISO, startIso);
        args.putString(ARG_END_ISO, endIso);
        args.putString(ARG_STATUS, status);
        args.putString(ARG_REASON, reason);
        if (tele != null) {
            args.putBoolean(ARG_TELE, tele);
        }
        sheet.setArguments(args);
        sheet.show(fm, "DoctorAppointmentDetailBottomSheet");
    }

    private long patientUserId = -1L;
    private String patientName;
    private String startIso;
    private String endIso;
    private String statusRaw;
    private String reason;
    private boolean tele = false;

    // UI
    private TextView textTitle;
    private TextView textPatientName;
    private TextView textDateTime;
    private TextView textStatusChip;
    private TextView textTele;
    private TextView textReason;
    private TextView textViewProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_doctor_appointment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            patientUserId = args.getLong(ARG_PATIENT_USER_ID, -1L);
            patientName = args.getString(ARG_PATIENT_NAME);
            startIso = args.getString(ARG_START_ISO);
            endIso = args.getString(ARG_END_ISO);
            statusRaw = args.getString(ARG_STATUS);
            reason = args.getString(ARG_REASON);
            tele = args.getBoolean(ARG_TELE, false);
        }

        textTitle = view.findViewById(R.id.text_detail_title);
        textPatientName = view.findViewById(R.id.text_patient_name);
        textDateTime = view.findViewById(R.id.text_date_time);
        textStatusChip = view.findViewById(R.id.text_status_chip);
        textTele = view.findViewById(R.id.text_teleconsultation);
        textReason = view.findViewById(R.id.text_reason);
        textViewProfile = view.findViewById(R.id.text_view_profile);

        bindContent();
        setupClicks();
    }

    private void bindContent() {
        if (textTitle != null) {
            textTitle.setText(R.string.doctor_appointment_detail_title);
        }

        if (!TextUtils.isEmpty(patientName)) {
            textPatientName.setText(patientName);
        }

        // Date/time
        String dateTime = AppointmentUiHelper.buildDateTimeDisplay(
                startIso,
                endIso,
                requireContext()
        );
        textDateTime.setText(dateTime);

        // Status chip (reuse same label + color mapping)
        bindStatus(statusRaw);

        // Teleconsultation
        if (tele) {
            textTele.setVisibility(View.VISIBLE);
            textTele.setText(R.string.appointment_teleconsultation_label);
        } else {
            textTele.setVisibility(View.GONE);
        }

        // Reason
        if (reason != null && !reason.trim().isEmpty()) {
            textReason.setVisibility(View.VISIBLE);
            textReason.setText(reason.trim());
        } else {
            textReason.setVisibility(View.GONE);
        }
    }

    private void setupClicks() {
        if (textViewProfile != null) {
            textViewProfile.setOnClickListener(v -> openPatientProfile());
        }
    }

    private void openPatientProfile() {
        if (!isAdded()) return;

        if (patientUserId <= 0L) {
            // Silently ignore; we don't know the patient user id
            Toast.makeText(
                    requireContext(),
                    R.string.doctor_appointments_error_generic,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Bundle args = new Bundle();
        args.putLong("patientId", patientUserId);

        try {
            Fragment parent = getParentFragment();
            if (parent != null) {
                NavController navController = NavHostFragment.findNavController(parent);
                navController.navigate(R.id.patientPublicProfileFragment, args);
            }
        } catch (Exception ignored) {
            // If navigation fails for any reason, we just keep the sheet open
        }

        dismissAllowingStateLoss();
    }

    private void bindStatus(@Nullable String statusRaw) {
        String label;
        String s = "";
        if (statusRaw == null) {
            label = requireContext().getString(R.string.appointment_status_unknown);
        } else {
            s = statusRaw.toUpperCase(java.util.Locale.getDefault());
            switch (s) {
                case "PENDING":
                    label = requireContext().getString(R.string.appointment_status_pending);
                    break;
                case "ACCEPTED":
                    label = requireContext().getString(R.string.appointment_status_accepted);
                    break;
                case "REJECTED":
                    label = requireContext().getString(R.string.appointment_status_rejected);
                    break;
                case "COMPLETED":
                    label = requireContext().getString(R.string.appointment_status_completed);
                    break;
                default:
                    label = requireContext().getString(R.string.appointment_status_unknown);
                    break;
            }
        }

        textStatusChip.setText(label);

        // Use same colors as adapter
        int bgColor;
        int textColor = android.graphics.Color.WHITE;

        switch (s) {
            case "PENDING":
                bgColor = android.graphics.Color.parseColor("#F9A825");
                break;
            case "ACCEPTED":
                bgColor = android.graphics.Color.parseColor("#2E7D32");
                break;
            case "REJECTED":
                bgColor = android.graphics.Color.parseColor("#C62828");
                break;
            case "COMPLETED":
                bgColor = android.graphics.Color.parseColor("#546E7A");
                break;
            default:
                bgColor = android.graphics.Color.parseColor("#757575");
                break;
        }

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(999));
        bg.setColor(bgColor);

        textStatusChip.setBackground(bg);
        textStatusChip.setTextColor(textColor);
    }

    private float dpToPx(float dp) {
        return android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                dp,
                requireContext().getResources().getDisplayMetrics()
        );
    }
}
