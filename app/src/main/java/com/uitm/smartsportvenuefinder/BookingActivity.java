package com.uitm.smartsportvenuefinder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BookingActivity extends AppCompatActivity {

    EditText etVenueName, etVenueAddress, etDate, etTime, etPax;
    TextView tvLocation;
    Button btnGetLocation, btnConfirmBooking, btnCancel;
    DatabaseReference mDatabase;
    FusedLocationProviderClient fusedLocationClient;

    double latitude = 0, longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        etVenueName = findViewById(R.id.etVenueName);
        etVenueAddress = findViewById(R.id.etVenueAddress);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etPax = findViewById(R.id.etPax);
        tvLocation = findViewById(R.id.tvLocation);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnCancel = findViewById(R.id.btnCancel);

        // Set initial location text
        tvLocation.setText("📍 Location: Not set - Click 'Get Location'");

        // Check if coming from venue search with data
        if (getIntent().hasExtra("venueName")) {
            etVenueName.setText(getIntent().getStringExtra("venueName"));
            etVenueAddress.setText(getIntent().getStringExtra("venueAddress"));
            latitude = getIntent().getDoubleExtra("latitude", 0);
            longitude = getIntent().getDoubleExtra("longitude", 0);
            if (latitude != 0 && longitude != 0) {
                tvLocation.setText("📍 Location: " + String.format("%.6f", latitude) +
                        ", " + String.format("%.6f", longitude));
            }
        }

        // Button listeners
        btnGetLocation.setOnClickListener(v -> getCurrentLocation());
        btnConfirmBooking.setOnClickListener(v -> createBooking());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Location location = task.getResult();
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            tvLocation.setText("📍 Location: " + String.format("%.6f", latitude) +
                                    ", " + String.format("%.6f", longitude));
                            Toast.makeText(BookingActivity.this, "✅ Location updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(BookingActivity.this, "❌ Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void createBooking() {
        // Get all input values
        String venueName = etVenueName.getText().toString().trim();
        String venueAddress = etVenueAddress.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String paxStr = etPax.getText().toString().trim();

        // Validate inputs
        if (venueName.isEmpty()) {
            etVenueName.setError("Please enter venue name");
            return;
        }
        if (venueAddress.isEmpty()) {
            etVenueAddress.setError("Please enter venue address");
            return;
        }
        if (date.isEmpty()) {
            etDate.setError("Please enter date");
            return;
        }
        if (time.isEmpty()) {
            etTime.setError("Please enter time");
            return;
        }
        if (paxStr.isEmpty()) {
            etPax.setError("Please enter number of people");
            return;
        }

        int pax;
        try {
            pax = Integer.parseInt(paxStr);
            if (pax <= 0) {
                etPax.setError("Must be at least 1 person");
                return;
            }
        } catch (NumberFormatException e) {
            etPax.setError("Invalid number");
            return;
        }

        // Check if location is set
        if (latitude == 0 && longitude == 0) {
            Toast.makeText(this, "⚠️ Please get your current location first", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Create booking ID
        String userId = currentUser.getUid();
        String bookingId = mDatabase.child("bookings").push().getKey();

        // Create booking object
        Booking booking = new Booking(bookingId, userId, "manual_venue", venueName,
                date, time, "Pending", pax);

        // Set additional fields
        booking.setVenueAddress(venueAddress);
        booking.setLatitude(latitude);
        booking.setLongitude(longitude);
        booking.setUserName(currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "User");
        booking.setUserEmail(currentUser.getEmail() != null ?
                currentUser.getEmail() : "");

        // Save to database
        mDatabase.child("bookings").child(bookingId).setValue(booking)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save venue for future reference
                        saveVenueLocation(venueName, venueAddress);

                        Toast.makeText(BookingActivity.this,
                                "✅ Booking confirmed for " + pax + " people!\n📍 " + venueName,
                                Toast.LENGTH_LONG).show();

                        finish();
                    } else {
                        Toast.makeText(BookingActivity.this,
                                "❌ Booking failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveVenueLocation(String name, String address) {
        String venueId = mDatabase.child("venues").push().getKey();
        Venue venue = new Venue(venueId, name, "Sports", address,
                latitude, longitude, "RM 10.00", "");
        mDatabase.child("venues").child(venueId).setValue(venue);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            Toast.makeText(this, "⚠️ Location permission required", Toast.LENGTH_SHORT).show();
        }
    }
}