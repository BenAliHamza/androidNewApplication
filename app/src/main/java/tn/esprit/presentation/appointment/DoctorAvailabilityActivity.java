package tn.esprit.presentation.appointment;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import tn.esprit.R;
import tn.esprit.data.remote.appointment.AvailabilitySessionRequest;
import tn.esprit.presentation.appointment.AppointmentViewModel;

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
        String[] types = {"NONE", "WEEKLY"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, types);
        spinnerRecurrence.setAdapter(adapter);
    }

    private void setupObservers() {
        vm.createdAvailability.observe(this, response -> {
            if (response != null) {
                Toast.makeText(this,
                        "Created. Slots = " + response.getGeneratedSlotsCount(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submit() {
        String token = getSharedPreferences("auth", MODE_PRIVATE)
                .getString("accessToken", "");

        AvailabilitySessionRequest req = new AvailabilitySessionRequest();
        req.startDate = inputStartDate.getText().toString();
        req.endDate = inputEndDate.getText().toString().isEmpty() ? null : inputEndDate.getText().toString();
        req.startTime = inputStartTime.getText().toString();
        req.endTime = inputEndTime.getText().toString();
        req.slotDurationMinutes = Integer.parseInt(inputDuration.getText().toString());
        req.recurrenceType = spinnerRecurrence.getSelectedItem().toString();

        vm.createAvailability(token, req);
    }
}
