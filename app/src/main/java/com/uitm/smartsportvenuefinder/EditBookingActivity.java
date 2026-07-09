package com.uitm.smartsportvenuefinder;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditBookingActivity extends AppCompatActivity {

    // UI Components
    private EditText etPax;
    private TextView tvVenueName, tvVenueAddress, tvDate, tvTime;
    private Button btnSearchVenue, btnPickDate, btnPickTime, btnUpdateBooking, btnCancel;
    private View progressOverlay;

    // Firebase
    private DatabaseReference mDatabase;

    private String venueName = "", venueAddress = "", placeId = "";
    private String bookingId;
    private String currentDate = "", currentTime = "";
    private int currentPax = 1;

    private Calendar selectedDate = Calendar.getInstance();
    private Calendar selectedTime = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    private static final int VENUE_SEARCH_REQUEST = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_booking);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initViews();
        setupListeners();

        // Get booking ID from intent
        bookingId = getIntent().getStringExtra("bookingId");

        if (bookingId != null) {
            loadBookingData();
        } else {
            Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        tvVenueName = findViewById(R.id.tvVenueName);
        tvVenueAddress = findViewById(R.id.tvVenueAddress);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        etPax = findViewById(R.id.etPax);
        btnSearchVenue = findViewById(R.id.btnSearchVenue);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnUpdateBooking = findViewById(R.id.btnUpdateBooking);
        btnCancel = findViewById(R.id.btnCancel);
        progressOverlay = findViewById(R.id.progressOverlay);

        tvVenueName.setText("Loading...");
        tvVenueAddress.setText("");
        tvDate.setText("Loading...");
        tvTime.setText("Loading...");
        etPax.setText("1");
    }

    private void setupListeners() {
        btnSearchVenue.setOnClickListener(v -> {
            Intent intent = new Intent(EditBookingActivity.this, MapActivity.class);
            startActivityForResult(intent, VENUE_SEARCH_REQUEST);
        });

        btnPickDate.setOnClickListener(v -> showDatePickerDialog());
        btnPickTime.setOnClickListener(v -> showTimePickerDialog());

        btnUpdateBooking.setOnClickListener(v -> updateBooking());
        btnCancel.setOnClickListener(v -> finish());

        tvDate.setOnClickListener(v -> showDatePickerDialog());
        tvTime.setOnClickListener(v -> showTimePickerDialog());
    }

    private void loadBookingData() {
        progressOverlay.setVisibility(View.VISIBLE);

        mDatabase.child("bookings").child(bookingId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressOverlay.setVisibility(View.GONE);

                        if (snapshot.exists()) {
                            Booking booking = snapshot.getValue(Booking.class);
                            if (booking != null) {
                                // Get data
                                venueName = booking.getVenueName() != null ? booking.getVenueName() : "";
                                venueAddress = booking.getVenueAddress() != null ? booking.getVenueAddress() : "";
                                placeId = booking.getVenueId() != null ? booking.getVenueId() : "";
                                currentDate = booking.getBookingDate() != null ? booking.getBookingDate() : "";
                                currentTime = booking.getBookingTime() != null ? booking.getBookingTime() : "";
                                currentPax = booking.getPax() != null ? booking.getPax() : 1;

                                // Set data to views
                                tvVenueName.setText(venueName);
                                tvVenueAddress.setText(venueAddress);

                                // Set date and time
                                if (!currentDate.isEmpty()) {
                                    tvDate.setText(currentDate);
                                    // Parse date to Calendar
                                    try {
                                        selectedDate.setTime(dateFormat.parse(currentDate));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (!currentTime.isEmpty()) {
                                    tvTime.setText(currentTime);
                                    // Parse time to Calendar
                                    try {
                                        selectedTime.setTime(timeFormat.parse(currentTime));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                etPax.setText(String.valueOf(currentPax));

                            } else {
                                Toast.makeText(EditBookingActivity.this, "Booking data not found", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(EditBookingActivity.this, "Booking does not exist", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressOverlay.setVisibility(View.GONE);
                        Toast.makeText(EditBookingActivity.this,
                                "Failed to load: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateDisplay();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePicker.show();
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute);
                    updateTimeDisplay();
                },
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE),
                false
        );
        timePicker.show();
    }

    private void updateDateDisplay() {
        tvDate.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void updateTimeDisplay() {
        tvTime.setText(timeFormat.format(selectedTime.getTime()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VENUE_SEARCH_REQUEST && resultCode == RESULT_OK && data != null) {
            venueName = data.getStringExtra("venueName");
            venueAddress = data.getStringExtra("venueAddress");
            placeId = data.getStringExtra("venueId");

            tvVenueName.setText(venueName);
            tvVenueAddress.setText(venueAddress);

            Toast.makeText(this, "Venue selected: " + venueName, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBooking() {
        // Get input values
        String date = tvDate.getText().toString().trim();
        String time = tvTime.getText().toString().trim();
        String paxStr = etPax.getText().toString().trim();

        // Validate
        if (venueName.isEmpty() || venueName.equals("No venue selected")) {
            Toast.makeText(this, "Please select a venue", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_LONG).show();
            return;
        }

        if (time.isEmpty()) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_LONG).show();
            return;
        }

        int pax;
        try {
            pax = Integer.parseInt(paxStr);
            if (pax <= 0) pax = 1;
        } catch (NumberFormatException e) {
            pax = 1;
        }

        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show loading
        progressOverlay.setVisibility(View.VISIBLE);
        btnUpdateBooking.setEnabled(false);

        // Update all fields in Firebase
        DatabaseReference bookingRef = mDatabase.child("bookings").child(bookingId);

        bookingRef.child("venueName").setValue(venueName);
        bookingRef.child("venueAddress").setValue(venueAddress);
        bookingRef.child("bookingDate").setValue(date);
        bookingRef.child("bookingTime").setValue(time);
        bookingRef.child("pax").setValue(pax);
        bookingRef.child("venueId").setValue(placeId);

        // Add completion listener
        bookingRef.child("pax").setValue(pax)
                .addOnCompleteListener(task -> {
                    progressOverlay.setVisibility(View.GONE);
                    btnUpdateBooking.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(EditBookingActivity.this,
                                "Booking Updated!",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(EditBookingActivity.this,
                                "Update failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}