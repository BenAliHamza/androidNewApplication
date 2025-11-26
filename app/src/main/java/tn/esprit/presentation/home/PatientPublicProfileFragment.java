package tn.esprit.presentation.home;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner; // kept in case of accidental XML reference, not used
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.doctor.DoctorPatientsRepository;
import tn.esprit.data.indicator.PatientIndicatorRepository;
import tn.esprit.data.medication.MedicationRepository;
import tn.esprit.data.medication.PrescriptionRepository;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.indicator.PatientIndicator;
import tn.esprit.domain.medication.Medication;
import tn.esprit.domain.medication.Prescription;
import tn.esprit.domain.medication.PrescriptionCreateRequest;
import tn.esprit.domain.medication.PrescriptionLine;
import tn.esprit.domain.medication.PrescriptionLineCreateRequest;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.presentation.indicator.PatientIndicatorAdapter;

/**
 * Doctor view: public-ish profile of a patient.
 *
 * Sections:
 *  - Patient info (name, basic meta, location, blood type, lifestyle chips).
 *  - Read-only indicators list for this patient (doctor cannot edit).
 *  - Medications/prescriptions list for this patient (doctor can create/delete their own).
 *
 * Navigation:
 *  - Expects a "patientId" argument which is the patient's User.id
 *    (as defined in nav_main.xml).
 */
public class PatientPublicProfileFragment extends Fragment {

    private ProgressBar progressBar;
    private NestedScrollView contentView;

    // Patient info views
    private ImageView imageAvatar;
    private TextView textName;
    private TextView textMeta;
    private TextView textLocation;
    private Chip chipBloodType;
    private Chip chipSmoker;
    private Chip chipAlcohol;

    // Indicators (read-only)
    private TextView textIndicatorsEmpty;
    private RecyclerView recyclerIndicators;
    private PatientIndicatorAdapter indicatorAdapter;

    // Medications (doctor can create/delete his own prescriptions)
    private TextView textMedicationsEmpty;
    private RecyclerView recyclerMedications;
    private PatientPrescriptionsAdapter prescriptionsAdapter;
    private TextView textMedicationsAdd;

    private DoctorPatientsRepository doctorPatientsRepository;
    private PatientIndicatorRepository indicatorRepository;
    private PrescriptionRepository prescriptionRepository;
    private MedicationRepository medicationRepository;
    private AuthLocalDataSource authLocalDataSource;

    // Cached medication catalog for doctor create flow
    @Nullable
    private List<Medication> medicationCatalog = null;

    private long patientUserId = -1L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_public_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.patient_public_progress);
        contentView = view.findViewById(R.id.patient_public_content);

        imageAvatar = view.findViewById(R.id.image_patient_avatar);
        textName = view.findViewById(R.id.text_patient_name);
        textMeta = view.findViewById(R.id.text_patient_meta);
        textLocation = view.findViewById(R.id.text_patient_location);
        chipBloodType = view.findViewById(R.id.chip_patient_blood_type);
        chipSmoker = view.findViewById(R.id.chip_patient_smoker);
        chipAlcohol = view.findViewById(R.id.chip_patient_alcohol);

        textIndicatorsEmpty = view.findViewById(R.id.text_patient_indicators_empty_for_doctor);
        recyclerIndicators = view.findViewById(R.id.recycler_patient_indicators_for_doctor);

        textMedicationsEmpty = view.findViewById(R.id.text_patient_medications_empty_for_doctor);
        recyclerMedications = view.findViewById(R.id.recycler_patient_medications_for_doctor);
        textMedicationsAdd = view.findViewById(R.id.text_patient_medications_add);

        indicatorAdapter = new PatientIndicatorAdapter();
        recyclerIndicators.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerIndicators.setAdapter(indicatorAdapter);

        prescriptionsAdapter = new PatientPrescriptionsAdapter(
                prescription -> showDeletePrescriptionConfirmation(prescription)
        );
        recyclerMedications.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerMedications.setAdapter(prescriptionsAdapter);

        doctorPatientsRepository = new DoctorPatientsRepository(requireContext());
        indicatorRepository = new PatientIndicatorRepository(requireContext());
        prescriptionRepository = new PrescriptionRepository(requireContext());
        medicationRepository = new MedicationRepository(requireContext());
        authLocalDataSource = new AuthLocalDataSource(requireContext().getApplicationContext());

        if (textMedicationsAdd != null) {
            textMedicationsAdd.setOnClickListener(v -> onAddMedicationClicked());
        }

        patientUserId = extractPatientUserIdFromArgs();

        if (patientUserId <= 0L) {
            showMissingIdError();
            return;
        }

        showLoading(true);
        loadMedicationCatalog();
        loadPatientProfile(patientUserId);
    }

    // ---------------------------------------------------------------------
    // Arg handling
    // ---------------------------------------------------------------------

    private long extractPatientUserIdFromArgs() {
        Bundle args = getArguments();
        if (args == null) return -1L;
        // nav_main.xml: argument name is "patientId"
        return args.getLong("patientId", -1L);
    }

    private void showMissingIdError() {
        showLoading(false);
        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.patient_public_error_missing_id),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // ---------------------------------------------------------------------
    // Patient profile loading
    // ---------------------------------------------------------------------

    private void loadPatientProfile(long patientUserId) {
        doctorPatientsRepository.getMyPatient(patientUserId,
                new DoctorPatientsRepository.LoadPatientCallback() {
                    @Override
                    public void onSuccess(PatientProfile patient) {
                        if (!isAdded()) return;

                        bindPatientInfo(patient);
                        showLoading(false);

                        // Once basic info is visible, load indicators + medications
                        loadIndicatorsForPatient(patientUserId);
                        loadMedicationsForPatient(patientUserId);
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        if (!isAdded()) return;

                        showLoading(false);
                        Toast.makeText(
                                requireContext(),
                                getString(R.string.patient_public_error_generic),
                                Toast.LENGTH_LONG
                        ).show();

                        // Still show empty UI with placeholder info
                        bindPatientInfo(null);
                    }
                });
    }

    private void bindPatientInfo(@Nullable PatientProfile patient) {
        // Avatar: for now we just use the logo placeholder from XML.

        String name;
        String meta;
        String location;

        if (patient == null) {
            name = getString(R.string.patient_public_title);
            meta = getString(R.string.patient_public_info_placeholder);
            location = "";
        } else {
            String fullName = patient.getFullName();
            name = !TextUtils.isEmpty(fullName)
                    ? fullName
                    : getString(R.string.patient_public_title);

            // Basic meta: combine gender and DOB (kept simple)
            String gender = patient.getGender() != null ? patient.getGender().trim() : "";
            String dateOfBirth = patient.getDateOfBirth() != null ? patient.getDateOfBirth().trim() : "";

            if (!gender.isEmpty() && !dateOfBirth.isEmpty()) {
                meta = dateOfBirth + " · " + gender;
            } else if (!gender.isEmpty()) {
                meta = gender;
            } else if (!dateOfBirth.isEmpty()) {
                meta = dateOfBirth;
            } else {
                meta = "";
            }

            String cityCountry = patient.getCityCountryLabel();
            location = cityCountry != null ? cityCountry : "";
        }

        textName.setText(name);
        textMeta.setText(meta);
        textLocation.setText(location);

        bindPatientChips(patient);
    }

    private void bindPatientChips(@Nullable PatientProfile patient) {
        if (patient == null) {
            chipBloodType.setVisibility(View.GONE);
            chipSmoker.setVisibility(View.GONE);
            chipAlcohol.setVisibility(View.GONE);
            return;
        }

        // Blood type
        String bloodType = patient.getBloodType();
        if (!TextUtils.isEmpty(bloodType)) {
            String label = getString(R.string.profile_patient_blood_type_format, bloodType);
            chipBloodType.setText(label);
            chipBloodType.setVisibility(View.VISIBLE);
        } else {
            chipBloodType.setVisibility(View.GONE);
        }

        // Smoker
        Boolean smoker = patient.getSmoker();
        if (smoker != null) {
            chipSmoker.setVisibility(View.VISIBLE);
            if (Boolean.TRUE.equals(smoker)) {
                chipSmoker.setText(R.string.profile_patient_smoker_yes);
            } else {
                chipSmoker.setText(R.string.profile_patient_smoker_no);
            }
        } else {
            chipSmoker.setVisibility(View.GONE);
        }

        // Alcohol
        Boolean alcohol = patient.getAlcoholUse();
        if (alcohol != null) {
            chipAlcohol.setVisibility(View.VISIBLE);
            if (Boolean.TRUE.equals(alcohol)) {
                chipAlcohol.setText(R.string.profile_patient_alcohol_yes);
            } else {
                chipAlcohol.setText(R.string.profile_patient_alcohol_no);
            }
        } else {
            chipAlcohol.setVisibility(View.GONE);
        }
    }

    // ---------------------------------------------------------------------
    // Indicators (doctor read-only view)
    // ---------------------------------------------------------------------

    private void loadIndicatorsForPatient(long patientUserId) {
        String authHeader = buildAuthHeaderIfAvailable();

        // Doctor read-only endpoint: GET /indicators/patient/{patientUserId}
        indicatorRepository.getIndicatorsForPatientAsDoctor(
                authHeader,
                patientUserId,
                null,   // no type filter
                null,   // no from
                null,   // no to
                new PatientIndicatorRepository.IndicatorsCallback() {
                    @Override
                    public void onSuccess(List<PatientIndicator> indicators) {
                        if (!isAdded()) return;

                        indicatorAdapter.submitList(indicators);
                        if (indicators == null || indicators.isEmpty()) {
                            textIndicatorsEmpty.setVisibility(View.VISIBLE);
                        } else {
                            textIndicatorsEmpty.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        if (!isAdded()) return;

                        // Soft error; doctor cannot edit anyway.
                        textIndicatorsEmpty.setText(R.string.indicators_error_generic);
                        textIndicatorsEmpty.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    // ---------------------------------------------------------------------
    // Medications (doctor view; list + delete)
    // ---------------------------------------------------------------------

    private void loadMedicationsForPatient(long patientUserId) {
        // Doctor endpoint: GET /api/doctors/me/patients/{patientUserId}/prescriptions?activeOnly=true
        prescriptionRepository.getPrescriptionsForPatientAsDoctor(
                patientUserId,
                true,
                new PrescriptionRepository.PrescriptionsCallback() {
                    @Override
                    public void onSuccess(List<Prescription> prescriptions) {
                        if (!isAdded()) return;

                        prescriptionsAdapter.submitList(prescriptions);
                        if (prescriptions == null || prescriptions.isEmpty()) {
                            textMedicationsEmpty.setVisibility(View.VISIBLE);
                            textMedicationsEmpty.setText(
                                    R.string.patient_public_medications_empty
                            );
                        } else {
                            textMedicationsEmpty.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        if (!isAdded()) return;

                        textMedicationsEmpty.setVisibility(View.VISIBLE);
                        textMedicationsEmpty.setText(
                                R.string.patient_public_medications_empty
                        );
                    }
                }
        );
    }

    private void showDeletePrescriptionConfirmation(@NonNull Prescription prescription) {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.patient_prescription_delete_title)
                .setMessage(R.string.patient_prescription_delete_message)
                .setNegativeButton(R.string.medications_cancel_action, null)
                .setPositiveButton(R.string.medications_delete_action,
                        (dialog, which) -> performDeletePrescription(prescription))
                .show();
    }

    private void performDeletePrescription(@NonNull Prescription prescription) {
        Long id = prescription.getId();
        if (id == null) return;

        prescriptionRepository.deletePrescriptionForDoctor(
                id,
                new PrescriptionRepository.DeletePrescriptionCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.patient_prescription_delete_success),
                                Toast.LENGTH_SHORT
                        ).show();
                        // Reload list after delete
                        loadMedicationsForPatient(patientUserId);
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        if (!isAdded()) return;

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.patient_prescription_delete_error),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // ---------------------------------------------------------------------
    // Medications (doctor view; create prescription)
    // ---------------------------------------------------------------------

    private void loadMedicationCatalog() {
        medicationRepository.getMedications(
                null,
                new MedicationRepository.MedicationsCallback() {
                    @Override
                    public void onSuccess(List<Medication> medications) {
                        medicationCatalog = medications;
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        medicationCatalog = null;
                    }
                }
        );
    }

    private void onAddMedicationClicked() {
        if (!isAdded()) return;

        if (medicationCatalog == null || medicationCatalog.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.patient_medication_add_error_catalog),
                    Toast.LENGTH_LONG
            ).show();
            // Try to reload catalog in background for next time
            loadMedicationCatalog();
            return;
        }

        showAddPrescriptionDialog();
    }

    private void showAddPrescriptionDialog() {
        if (!isAdded()) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_prescription, null, false);

        AutoCompleteTextView inputMedication = dialogView.findViewById(R.id.input_medication);
        EditText inputStartDate = dialogView.findViewById(R.id.input_prescription_start_date);
        EditText inputEndDate = dialogView.findViewById(R.id.input_prescription_end_date);
        EditText inputNote = dialogView.findViewById(R.id.input_prescription_note);
        EditText inputDosage = dialogView.findViewById(R.id.input_medication_dosage);
        EditText inputTimesPerDay = dialogView.findViewById(R.id.input_medication_times_per_day);
        EditText inputInstructions = dialogView.findViewById(R.id.input_medication_instructions);
        ProgressBar dialogProgress = dialogView.findViewById(R.id.dialog_prescription_progress);

        List<Medication> meds = medicationCatalog != null
                ? medicationCatalog
                : Collections.emptyList();

        List<String> medNames = new ArrayList<>();
        for (Medication med : meds) {
            if (med == null) continue;
            medNames.add(med.getDisplayName());
        }

        // Searchable dropdown adapter
        android.widget.ArrayAdapter<String> medAdapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                medNames
        );
        inputMedication.setAdapter(medAdapter);
        inputMedication.setThreshold(1); // start suggesting from first character

        // Attach date pickers to date fields
        attachDatePicker(inputStartDate);
        attachDatePicker(inputEndDate);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_add_prescription_title)
                .setView(dialogView)
                .setNegativeButton(R.string.medications_cancel_action, null)
                .setPositiveButton(R.string.dialog_add_prescription_save, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            Button buttonSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (buttonSave == null) return;

            buttonSave.setOnClickListener(v -> {
                // Validation
                if (meds.isEmpty()) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.patient_medication_add_error_catalog),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                String selectedName = trimToEmpty(
                        inputMedication.getText() != null
                                ? inputMedication.getText().toString()
                                : ""
                );

                if (selectedName.isEmpty()) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.dialog_add_prescription_validation_medication),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                // Find the medication by name (case-insensitive)
                int position = -1;
                for (int i = 0; i < medNames.size(); i++) {
                    String name = medNames.get(i);
                    if (name != null && name.equalsIgnoreCase(selectedName)) {
                        position = i;
                        break;
                    }
                }

                if (position < 0 || position >= meds.size()) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.dialog_add_prescription_validation_medication),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Medication selectedMedication = meds.get(position);
                if (selectedMedication == null || selectedMedication.getId() == null) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.dialog_add_prescription_validation_medication),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                String startDate = trimToEmpty(inputStartDate.getText() != null
                        ? inputStartDate.getText().toString()
                        : "");
                String endDate = trimToEmpty(inputEndDate.getText() != null
                        ? inputEndDate.getText().toString()
                        : "");
                String note = trimToEmpty(inputNote.getText() != null
                        ? inputNote.getText().toString()
                        : "");
                String dosage = trimToEmpty(inputDosage.getText() != null
                        ? inputDosage.getText().toString()
                        : "");
                String timesStr = trimToEmpty(inputTimesPerDay.getText() != null
                        ? inputTimesPerDay.getText().toString()
                        : "");
                String instructions = trimToEmpty(inputInstructions.getText() != null
                        ? inputInstructions.getText().toString()
                        : "");

                if (startDate.isEmpty()) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.dialog_add_prescription_validation_start_date),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                if (dosage.isEmpty()) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.dialog_add_prescription_validation_dosage),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                if (timesStr.isEmpty()) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.dialog_add_prescription_validation_times),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Integer timesPerDay;
                try {
                    timesPerDay = Integer.valueOf(timesStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.dialog_add_prescription_validation_times),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                // Build request
                PrescriptionLineCreateRequest lineReq =
                        new PrescriptionLineCreateRequest(
                                selectedMedication.getId(),
                                dosage,
                                timesPerDay,
                                instructions
                        );
                List<PrescriptionLineCreateRequest> lines = new ArrayList<>();
                lines.add(lineReq);

                PrescriptionCreateRequest createRequest =
                        new PrescriptionCreateRequest();
                createRequest.setStartDate(startDate);
                if (!endDate.isEmpty()) {
                    createRequest.setEndDate(endDate);
                }
                createRequest.setNote(note);
                createRequest.setLines(lines);

                dialogProgress.setVisibility(View.VISIBLE);
                buttonSave.setEnabled(false);

                prescriptionRepository.createPrescriptionForPatient(
                        patientUserId,
                        createRequest,
                        new PrescriptionRepository.CreatePrescriptionCallback() {
                            @Override
                            public void onSuccess(Prescription prescription) {
                                if (!isAdded()) return;

                                dialog.dismiss();
                                Toast.makeText(
                                        requireContext(),
                                        getString(R.string.dialog_add_prescription_create_success),
                                        Toast.LENGTH_SHORT
                                ).show();
                                loadMedicationsForPatient(patientUserId);
                            }

                            @Override
                            public void onError(@Nullable Throwable throwable,
                                                @Nullable Integer httpCode,
                                                @Nullable String errorBody) {
                                if (!isAdded()) return;

                                dialogProgress.setVisibility(View.GONE);
                                buttonSave.setEnabled(true);

                                Toast.makeText(
                                        requireContext(),
                                        getString(R.string.dialog_add_prescription_create_error),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
            });
        });

        dialog.show();
    }

    // Attach a date picker to an EditText (non-editable, clickable)
    private void attachDatePicker(@NonNull EditText input) {
        input.setFocusable(false);
        input.setClickable(true);
        input.setOnClickListener(v -> showDatePickerDialog(input));
    }

    private void showDatePickerDialog(@NonNull EditText target) {
        if (!isAdded()) return;

        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH); // 0-based
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(
                requireContext(),
                (view, y, m, d) -> {
                    String formatted = formatDate(y, m, d);
                    target.setText(formatted);
                },
                year,
                month,
                day
        );
        picker.show();
    }

    private String formatDate(int year, int monthZeroBased, int day) {
        int monthOneBased = monthZeroBased + 1;
        // yyyy-MM-dd
        return String.format("%04d-%02d-%02d", year, monthOneBased, day);
    }

    private String trimToEmpty(@Nullable String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }

    // ---------------------------------------------------------------------
    // Loading state
    // ---------------------------------------------------------------------

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (contentView != null) {
            contentView.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
    }

    // ---------------------------------------------------------------------
    // Auth header helper
    // ---------------------------------------------------------------------

    @Nullable
    private String buildAuthHeaderIfAvailable() {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            return null;
        }
        return "Bearer " + tokens.getAccessToken();
    }
}
