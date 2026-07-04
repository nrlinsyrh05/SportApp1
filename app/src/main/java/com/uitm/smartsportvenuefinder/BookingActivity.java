package com.uitm.smartsportvenuefinder;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
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
import androidx.core.app.NotificationCompat;
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

    // Notification
    private static final String CHANNEL_ID = "booking_channel";
    private static final String CHANNEL_NAME = "Booking Notifications";
    private static final int NOTIFICATION_ID = 1001;

    // Constants
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int VENUE_SEARCH_REQUEST = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Create notification channel
        createNotificationChannel();

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
        btnConfirmBooking.setOnClickListener(v -> createBooking());
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

    // ============ NOTIFICATION METHODS ============

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Booking confirmation notifications");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void sendBookingNotification(String venueName, int pax, String date, String time) {
        try {
            // Intent to open BookingHistory when notification is tapped
            Intent intent = new Intent(this, BookingHistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("🎉 Booking Confirmed!")
                    .setContentText(venueName + " - " + pax + " people on " + date)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("📍 " + venueName + "\n👤 " + pax + " people\n📅 " + date + " at " + time))
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setVibrate(new long[]{1000, 1000, 1000, 1000})
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            // Silent fail - notification is optional
            e.printStackTrace();
        }
    }

    // ============ BOOKING METHOD ============

    private void createBooking() {
        // Get all input values
        String date = tvDate.getText().toString().replace("📅 ", "").trim();
        String time = tvTime.getText().toString().replace("🕐 ", "").trim();
        String paxStr = etPax.getText().toString().trim();

        // Validation
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
        } else {
            try {
                pax = Integer.parseInt(paxStr);
                if (pax <= 0) {
                    pax = 1;
                    etPax.setText("1");
                }
            } catch (NumberFormatException e) {
                pax = 1;
                etPax.setText("1");
            }
        }

        final int finalPax = pax;
        final String finalDate = date;
        final String finalTime = time;

        // Check authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "⚠️ Please login first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Toast.makeText(this, "📤 Saving booking...", Toast.LENGTH_SHORT).show();

        // Create booking ID
        String userId = currentUser.getUid();
        String bookingId = mDatabase.child("bookings").push().getKey();

        if (bookingId == null) {
            Toast.makeText(this, "❌ Failed to create booking", Toast.LENGTH_LONG).show();
            return;
        }

        // Create booking object
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setVenueId(placeId);
        booking.setVenueName(venueName);
        booking.setVenueAddress(venueAddress);
        booking.setBookingDate(finalDate);
        booking.setBookingTime(finalTime);
        booking.setPax(finalPax);
        booking.setStatus("Pending");
        booking.setTimestamp(System.currentTimeMillis());
        booking.setLatitude(latitude);
        booking.setLongitude(longitude);
        booking.setUserName(currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "User");
        booking.setUserEmail(currentUser.getEmail() != null ?
                currentUser.getEmail() : "");

        final String finalVenueName = venueName;

        // Save to Firebase
        mDatabase.child("bookings").child(bookingId).setValue(booking)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // ✅ Show success message
                        Toast.makeText(BookingActivity.this,
                                "✅ Booking Confirmed! ",
                                Toast.LENGTH_LONG).show();

                        // 📱 Send push notification
                        sendBookingNotification(finalVenueName, finalPax, finalDate, finalTime);

                        // Close activity after delay
                        new android.os.Handler().postDelayed(() -> finish(), 1500);
                    } else {
                        Exception e = task.getException();
                        String errorMsg = e != null ? e.getMessage() : "Unknown error";
                        Toast.makeText(BookingActivity.this,
                                "❌ Booking Failed: " + errorMsg,
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
            Toast.makeText(this, "⚠️ Location permission required", Toast.LENGTH_SHORT).show();
        }
    }
}