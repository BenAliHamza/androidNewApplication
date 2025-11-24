// app/src/main/java/tn/esprit/presentation/appointment/DoctorAvailabilityActivity.java
package tn.esprit.presentation.appointment;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import tn.esprit.R;
import tn.esprit.data.remote.appointment.AvailabilitySessionRequest;

public class DoctorAvailabilityActivity extends AppCompatActivity {

    private AppointmentViewModel vm;
    private EditText inputStartDate, inputEndDate, inputStartTime, inputEndTime, inputDuration;
    private Spinner spinnerRecurrence;
    private Button btnCreate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.availability_form);

        vm = new ViewModelProvider(this).get(AppointmentViewModel.class);

        inputStartDate = findViewById(R.id.inputStartDate);
        inputEndDate = findViewById(R.id.inputEndDate);
        inputStartTime = findViewById(R.id.inputStartTime);
        inputEndTime = findViewById(R.id.inputEndTime);
        inputDuration = findViewById(R.id.inputDuration);
        spinnerRecurrence = findViewById(R.id.spinnerRecurrence);
        btnCreate = findViewById(R.id.btnCreateAvailability);

        setupRecurrenceSpinner();
        setupObservers();

        btnCreate.setOnClickListener(v -> submit());
    }

    private void setupRecurrenceSpinner() {
        String[] types = {"ONE_TIME", "WEEKLY"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                types
        );
        spinnerRecurrence.setAdapter(adapter);
    }

    private void setupObservers() {
        vm.getCreatedAvailability().observe(this, response -> {
            if (response != null) {
                Toast.makeText(
                        this,
                        "Created. Slots = " + response.getGeneratedSlotsCount(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void submit() {
        String rawToken = getSharedPreferences("auth", MODE_PRIVATE)
                .getString("accessToken", "");
        if (rawToken == null || rawToken.isEmpty()) {
            Toast.makeText(this, "Missing token", Toast.LENGTH_SHORT).show();
            return;
        }
        String bearer = "Bearer " + rawToken;

        AvailabilitySessionRequest req = new AvailabilitySessionRequest();
        req.setStartDate(inputStartDate.getText().toString());
        String endDateStr = inputEndDate.getText().toString();
        req.setEndDate(endDateStr.isEmpty() ? null : endDateStr);
        req.setStartTime(inputStartTime.getText().toString());
        req.setEndTime(inputEndTime.getText().toString());
        req.setSlotDurationMinutes(Integer.parseInt(inputDuration.getText().toString()));
        req.setRecurrenceType(spinnerRecurrence.getSelectedItem().toString());
        req.setDaysOfWeek(null);

        vm.createAvailability(bearer, req);
    }
}
