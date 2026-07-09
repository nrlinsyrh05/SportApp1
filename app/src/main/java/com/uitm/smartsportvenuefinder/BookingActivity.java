package com.uitm.smartsportvenuefinder;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private EditText etPax;
    private TextView tvVenueName, tvVenueAddress, tvLocation, tvDate, tvTime;
    private Button btnSearchVenue, btnGetGps, btnConfirmBooking, btnCancel, btnPickDate, btnPickTime;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    private double latitude = 0, longitude = 0;
    private String venueName = "", venueAddress = "", placeId = "";

    private Calendar selectedDate = Calendar.getInstance();
    private Calendar selectedTime = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int VENUE_SEARCH_REQUEST = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Initialize location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        tvVenueName = findViewById(R.id.tvVenueName);
        tvVenueAddress = findViewById(R.id.tvVenueAddress);
        tvLocation = findViewById(R.id.tvLocation);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        etPax = findViewById(R.id.etPax);
        btnSearchVenue = findViewById(R.id.btnSearchVenue);
        btnGetGps = findViewById(R.id.btnGetGps);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnCancel = findViewById(R.id.btnCancel);

        // Set default values - removed emojis
        tvVenueName.setText("No venue selected");
        tvVenueAddress.setText("");
        tvLocation.setText("Tap 'Search' to find a venue");
        etPax.setText("1");
        updateDateDisplay();
        updateTimeDisplay();

        // Setup listeners
        btnSearchVenue.setOnClickListener(v -> {
            Intent intent = new Intent(BookingActivity.this, VenueSearchActivity.class);
            startActivityForResult(intent, VENUE_SEARCH_REQUEST);
        });

        btnGetGps.setOnClickListener(v -> getCurrentLocation());
        btnPickDate.setOnClickListener(v -> showDatePickerDialog());
        btnPickTime.setOnClickListener(v -> showTimePickerDialog());
        btnConfirmBooking.setOnClickListener(v -> createBooking());
        btnCancel.setOnClickListener(v -> finish());

        tvDate.setOnClickListener(v -> showDatePickerDialog());
        tvTime.setOnClickListener(v -> showTimePickerDialog());

        // Check for incoming venue data
        if (getIntent().hasExtra("venueName")) {
            venueName = getIntent().getStringExtra("venueName");
            venueAddress = getIntent().getStringExtra("venueAddress");
            placeId = getIntent().getStringExtra("venueId");
            latitude = getIntent().getDoubleExtra("latitude", 0);
            longitude = getIntent().getDoubleExtra("longitude", 0);

            tvVenueName.setText(venueName);
            tvVenueAddress.setText(venueAddress);
            tvLocation.setText(venueAddress);
            Toast.makeText(this, "Venue loaded: " + venueName, Toast.LENGTH_SHORT).show();
        }
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

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        Toast.makeText(this, "Getting location...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Location location = task.getResult();
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        venueName = "Current Location";
                        venueAddress = String.format("%.6f, %.6f", latitude, longitude);
                        tvVenueName.setText(venueName);
                        tvVenueAddress.setText(venueAddress);
                        tvLocation.setText("GPS: " + venueAddress);

                        Toast.makeText(BookingActivity.this, "GPS location set", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BookingActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VENUE_SEARCH_REQUEST && resultCode == RESULT_OK && data != null) {
            venueName = data.getStringExtra("venueName");
            venueAddress = data.getStringExtra("venueAddress");
            placeId = data.getStringExtra("venueId");
            latitude = data.getDoubleExtra("latitude", 0);
            longitude = data.getDoubleExtra("longitude", 0);

            tvVenueName.setText(venueName);
            tvVenueAddress.setText(venueAddress);
            tvLocation.setText(venueAddress);

            Toast.makeText(this, "Venue selected: " + venueName, Toast.LENGTH_SHORT).show();
        }
    }

    private void createBooking() {
        // Validate venue
        if (venueName.isEmpty() || venueName.equals("No venue selected")) {
            Toast.makeText(this, "Please select a venue", Toast.LENGTH_LONG).show();
            return;
        }

        // Get date and time
        String date = tvDate.getText().toString().trim();
        String time = tvTime.getText().toString().trim();
        String paxStr = etPax.getText().toString().trim();

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

        // Check user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Create booking ID
        String userId = currentUser.getUid();
        String bookingId = mDatabase.child("bookings").push().getKey();

        if (bookingId == null) {
            Toast.makeText(this, "Failed to create booking", Toast.LENGTH_LONG).show();
            return;
        }

        // Create booking object
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setVenueId(placeId);
        booking.setVenueName(venueName);
        booking.setVenueAddress(venueAddress);
        booking.setBookingDate(date);
        booking.setBookingTime(time);
        booking.setPax(pax);
        booking.setStatus("Pending");
        booking.setTimestamp(System.currentTimeMillis());
        booking.setLatitude(latitude);
        booking.setLongitude(longitude);
        booking.setUserName(currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "User");
        booking.setUserEmail(currentUser.getEmail() != null ?
                currentUser.getEmail() : "");

        final String finalVenueName = venueName;
        final int finalPax = pax;

        Toast.makeText(this, "Saving booking...", Toast.LENGTH_SHORT).show();

        // Save to Firebase - USING TOAST instead of Notification
        mDatabase.child("bookings").child(bookingId).setValue(booking)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Toast for internal action (booking confirmed)
                        Toast.makeText(BookingActivity.this,
                                "Booking Confirmed! " + finalVenueName + " - " + finalPax + " people",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(BookingActivity.this,
                                "Booking Failed: " + error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }
}