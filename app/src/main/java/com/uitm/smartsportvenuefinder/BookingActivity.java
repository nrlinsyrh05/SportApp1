package com.uitm.smartsportvenuefinder;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    // UI Components
    private EditText etPax;
    private TextView tvVenueName, tvVenueAddress, tvLocation, tvDate, tvTime;
    private Button btnSearchVenue, btnGetGps, btnConfirmBooking, btnCancel, btnPickDate, btnPickTime;

    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private double latitude = 0, longitude = 0;
    private String venueName = "", venueAddress = "", placeId = "";

    // Date & Time
    private Calendar selectedDate = Calendar.getInstance();
    private Calendar selectedTime = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    // Constants
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int VENUE_SEARCH_REQUEST = 1002;
    private static final String TAG = "BookingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Initialize Firebase - FIXED: Only initialize once
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference();
        mAuth = FirebaseAuth.getInstance();

        // Log Firebase URL for debugging
        Log.d(TAG, "Firebase Database URL: " + mDatabase.toString());

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        initViews();
        setupListeners();

        // Set current date and time as default
        updateDateDisplay();
        updateTimeDisplay();

        // Check if coming from VenueSearch with data (AUTO-FILL)
        handleIncomingVenueData();

        // Set default PAX value
        etPax.setText("1");

        // Test Firebase connection
        testFirebaseConnection();
    }

    private void initViews() {
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

        tvVenueName.setText("No venue selected");
        tvVenueAddress.setText("");
        tvLocation.setText("📍 Tap 'Search' to find a venue or 'GPS' for current location");
    }

    private void setupListeners() {
        btnSearchVenue.setOnClickListener(v -> {
            Intent intent = new Intent(BookingActivity.this, VenueSearchActivity.class);
            startActivityForResult(intent, VENUE_SEARCH_REQUEST);
        });

        btnGetGps.setOnClickListener(v -> getCurrentLocation());
        btnPickDate.setOnClickListener(v -> showDatePickerDialog());
        btnPickTime.setOnClickListener(v -> showTimePickerDialog());

        btnConfirmBooking.setOnClickListener(v -> {
            Toast.makeText(BookingActivity.this, "📌 Booking Started!", Toast.LENGTH_SHORT).show();
            createBooking();
        });

        btnCancel.setOnClickListener(v -> finish());

        tvDate.setOnClickListener(v -> showDatePickerDialog());
        tvTime.setOnClickListener(v -> showTimePickerDialog());
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
        tvDate.setText("📅 " + dateFormat.format(selectedDate.getTime()));
    }

    private void updateTimeDisplay() {
        tvTime.setText("🕐 " + timeFormat.format(selectedTime.getTime()));
    }

    private void handleIncomingVenueData() {
        if (getIntent().hasExtra("venueName")) {
            venueName = getIntent().getStringExtra("venueName");
            venueAddress = getIntent().getStringExtra("venueAddress");
            placeId = getIntent().getStringExtra("venueId");
            latitude = getIntent().getDoubleExtra("latitude", 0);
            longitude = getIntent().getDoubleExtra("longitude", 0);

            tvVenueName.setText(venueName);
            tvVenueAddress.setText(venueAddress);

            if (latitude != 0 && longitude != 0) {
                tvLocation.setText("📍 " + String.format("%.6f", latitude) + ", " + String.format("%.6f", longitude));
            } else {
                tvLocation.setText("📍 " + venueAddress);
            }

            Toast.makeText(this, "✅ Venue loaded: " + venueName, Toast.LENGTH_SHORT).show();
        }
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

            if (latitude != 0 && longitude != 0) {
                tvLocation.setText("📍 " + String.format("%.6f", latitude) + ", " + String.format("%.6f", longitude));
            } else {
                tvLocation.setText("📍 " + venueAddress);
            }

            Toast.makeText(this, "✅ Venue selected: " + venueName, Toast.LENGTH_SHORT).show();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

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
                        tvLocation.setText("📍 GPS: " + venueAddress);

                        Toast.makeText(BookingActivity.this, "✅ GPS location set", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BookingActivity.this, "❌ Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createBooking() {
        // Get all input values
        String date = tvDate.getText().toString().replace("📅 ", "").trim();
        String time = tvTime.getText().toString().replace("🕐 ", "").trim();
        String paxStr = etPax.getText().toString().trim();

        // Log for debugging
        Log.d(TAG, "=== CREATE BOOKING ===");
        Log.d(TAG, "Date: '" + date + "'");
        Log.d(TAG, "Time: '" + time + "'");
        Log.d(TAG, "PAX: '" + paxStr + "'");
        Log.d(TAG, "Venue: '" + venueName + "'");
        Log.d(TAG, "Address: '" + venueAddress + "'");

        // VALIDATION
        if (venueName.isEmpty() || venueName.equals("No venue selected")) {
            Toast.makeText(this, "⚠️ Please select a venue", Toast.LENGTH_LONG).show();
            return;
        }

        if (date.isEmpty()) {
            Toast.makeText(this, "⚠️ Please select a date", Toast.LENGTH_LONG).show();
            return;
        }

        if (time.isEmpty()) {
            Toast.makeText(this, "⚠️ Please select a time", Toast.LENGTH_LONG).show();
            return;
        }

        int pax;
        if (paxStr.isEmpty()) {
            pax = 1;
            etPax.setText("1");
            Toast.makeText(this, "ℹ️ PAX set to 1 (default)", Toast.LENGTH_SHORT).show();
        } else {
            try {
                pax = Integer.parseInt(paxStr);
                if (pax <= 0) {
                    pax = 1;
                    etPax.setText("1");
                    Toast.makeText(this, "ℹ️ PAX set to 1", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                pax = 1;
                etPax.setText("1");
                Toast.makeText(this, "ℹ️ PAX set to 1", Toast.LENGTH_SHORT).show();
            }
        }

        final int finalPax = pax;

        // Check authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "⚠️ Please login first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Log.d(TAG, "User authenticated: " + currentUser.getEmail());
        Toast.makeText(this, "📤 Saving booking...", Toast.LENGTH_SHORT).show();

        // Create booking ID
        String userId = currentUser.getUid();
        String bookingId = mDatabase.child("bookings").push().getKey();

        if (bookingId == null) {
            Toast.makeText(this, "❌ Failed to create booking ID", Toast.LENGTH_LONG).show();
            Log.e(TAG, "bookingId is null");
            return;
        }

        Log.d(TAG, "Booking ID: " + bookingId);

        // Create booking object
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setVenueId(placeId);
        booking.setVenueName(venueName);
        booking.setVenueAddress(venueAddress);
        booking.setBookingDate(date);
        booking.setBookingTime(time);
        booking.setPax(finalPax);
        booking.setStatus("Pending");
        booking.setTimestamp(System.currentTimeMillis());
        booking.setLatitude(latitude);
        booking.setLongitude(longitude);
        booking.setUserName(currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "User");
        booking.setUserEmail(currentUser.getEmail() != null ?
                currentUser.getEmail() : "");

        Log.d(TAG, "Booking object created");

        final String finalVenueName = venueName;
        final DatabaseReference bookingRef = mDatabase.child("bookings").child(bookingId);

        // Save to Firebase
        bookingRef.setValue(booking)
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "onComplete called, success: " + task.isSuccessful());
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Booking saved successfully!");
                        Toast.makeText(BookingActivity.this,
                                "✅✅✅ BOOKING CONFIRMED! ✅✅✅\n📍 " + finalVenueName + "\n👤 " + finalPax + " people",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "❌ Booking failed: " + (e != null ? e.getMessage() : "Unknown error"));
                        Toast.makeText(BookingActivity.this,
                                "❌ Failed: " + (e != null ? e.getMessage() : "Check internet connection"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void testFirebaseConnection() {
        DatabaseReference testRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        testRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    Log.d(TAG, "✅ CONNECTED to Firebase!");
                    Toast.makeText(BookingActivity.this, "✅ Firebase Connected!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "❌ NOT CONNECTED to Firebase!");
                    Toast.makeText(BookingActivity.this, "❌ Firebase NOT Connected! Check internet.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase connection error: " + error.getMessage());
                Toast.makeText(BookingActivity.this, "❌ Firebase Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, "⚠️ Location permission required", Toast.LENGTH_SHORT).show();
        }
    }
}