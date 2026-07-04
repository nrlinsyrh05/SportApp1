package com.uitm.smartsportvenuefinder;

import android.Manifest;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    // UI Components
    private EditText etDate, etTime, etPax;
    private TextView tvVenueName, tvVenueAddress, tvLocation;
    private Button btnSearchLocation, btnGetGps, btnConfirmBooking, btnCancel;
    private View progressOverlay;

    // Firebase
    private DatabaseReference mDatabase;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private double latitude = 0, longitude = 0;
    private String venueName = "", venueAddress = "", placeId = "";

    // Constants
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int PLACE_AUTOCOMPLETE_REQUEST = 1002;
    private static final String API_KEY = "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY);
        }

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        initViews();
        setupListeners();

        // Set current date as default
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(dateFormat.format(new Date()));

        // Set default time
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        etTime.setText(timeFormat.format(new Date()));
    }

    private void initViews() {
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etPax = findViewById(R.id.etPax);
        tvVenueName = findViewById(R.id.tvVenueName);
        tvVenueAddress = findViewById(R.id.tvVenueAddress);
        tvLocation = findViewById(R.id.tvLocation);
        btnSearchLocation = findViewById(R.id.btnSearchLocation);
        btnGetGps = findViewById(R.id.btnGetGps);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnCancel = findViewById(R.id.btnCancel);
        progressOverlay = findViewById(R.id.progressOverlay);

        // Set initial text
        tvVenueName.setText("No venue selected");
        tvVenueAddress.setText("");
        tvLocation.setText("📍 Tap 'Search' or 'GPS' to select location");
    }

    private void setupListeners() {
        btnSearchLocation.setOnClickListener(v -> openPlaceAutocomplete());
        btnGetGps.setOnClickListener(v -> getCurrentLocation());
        btnConfirmBooking.setOnClickListener(v -> createBooking());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void openPlaceAutocomplete() {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        );

        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        progressOverlay.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        progressOverlay.setVisibility(View.GONE);

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
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                if (place != null) {
                    venueName = place.getName() != null ? place.getName() : "Unknown Venue";
                    venueAddress = place.getAddress() != null ? place.getAddress() : "";
                    placeId = place.getId() != null ? place.getId() : "";

                    if (place.getLatLng() != null) {
                        latitude = place.getLatLng().latitude;
                        longitude = place.getLatLng().longitude;
                    }

                    tvVenueName.setText(venueName);
                    tvVenueAddress.setText(venueAddress);
                    tvLocation.setText("📍 " + venueAddress);

                    Toast.makeText(this, "✅ " + venueName + " selected", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Toast.makeText(this, "❌ Place selection failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createBooking() {
        // Get input values
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String paxStr = etPax.getText().toString().trim();

        // Validate
        if (venueName.isEmpty() || venueName.equals("No venue selected")) {
            Toast.makeText(this, "⚠️ Please select a venue first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty()) {
            etDate.setError("Required");
            return;
        }
        if (time.isEmpty()) {
            etTime.setError("Required");
            return;
        }
        if (paxStr.isEmpty()) {
            etPax.setError("Required");
            return;
        }

        int pax;
        try {
            pax = Integer.parseInt(paxStr);
            if (pax <= 0) {
                etPax.setError("Must be at least 1");
                return;
            }
        } catch (NumberFormatException e) {
            etPax.setError("Invalid number");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show loading
        progressOverlay.setVisibility(View.VISIBLE);
        btnConfirmBooking.setEnabled(false);

        // Create booking
        String userId = currentUser.getUid();
        String bookingId = mDatabase.child("bookings").push().getKey();

        Booking booking = new Booking(bookingId, userId, placeId, venueName,
                date, time, "Pending", pax);
        booking.setVenueAddress(venueAddress);
        booking.setLatitude(latitude);
        booking.setLongitude(longitude);
        booking.setUserName(currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "User");
        booking.setUserEmail(currentUser.getEmail() != null ?
                currentUser.getEmail() : "");

        // Save to Firebase
        mDatabase.child("bookings").child(bookingId).setValue(booking)
                .addOnCompleteListener(task -> {
                    progressOverlay.setVisibility(View.GONE);
                    btnConfirmBooking.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(BookingActivity.this,
                                "✅ Booking Confirmed!\n📍 " + venueName + "\n👤 " + pax + " people",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(BookingActivity.this,
                                "❌ Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
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