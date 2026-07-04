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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EditBookingActivity extends AppCompatActivity {

    // UI Components
    private EditText etDate, etTime, etPax;
    private TextView tvVenueName, tvVenueAddress, tvLocation;
    private Button btnSearchLocation, btnGetGps, btnUpdateBooking, btnCancel;
    private View progressOverlay;

    // Firebase
    private DatabaseReference mDatabase;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private double latitude = 0, longitude = 0;
    private String venueName = "", venueAddress = "", placeId = "";

    // Booking to edit
    private Booking currentBooking;
    private String bookingId;

    // Constants
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int PLACE_AUTOCOMPLETE_REQUEST = 1002;
    private static final String API_KEY = "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_booking);

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

        // Get booking data from intent
        getBookingData();
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
        btnUpdateBooking = findViewById(R.id.btnUpdateBooking);
        btnCancel = findViewById(R.id.btnCancel);
        progressOverlay = findViewById(R.id.progressOverlay);

        // Change button text to "Update"
        btnUpdateBooking.setText("📤 Update Booking");
    }

    private void setupListeners() {
        btnSearchLocation.setOnClickListener(v -> openPlaceAutocomplete());
        btnGetGps.setOnClickListener(v -> getCurrentLocation());
        btnUpdateBooking.setOnClickListener(v -> updateBooking());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void getBookingData() {
        // Get booking data from intent
        bookingId = getIntent().getStringExtra("bookingId");
        String venueNameExtra = getIntent().getStringExtra("venueName");
        String venueAddressExtra = getIntent().getStringExtra("venueAddress");
        String dateExtra = getIntent().getStringExtra("bookingDate");
        String timeExtra = getIntent().getStringExtra("bookingTime");
        int paxExtra = getIntent().getIntExtra("pax", 1);
        double latExtra = getIntent().getDoubleExtra("latitude", 0);
        double lngExtra = getIntent().getDoubleExtra("longitude", 0);
        String placeIdExtra = getIntent().getStringExtra("placeId");

        // Pre-fill the form with existing data
        venueName = venueNameExtra != null ? venueNameExtra : "";
        venueAddress = venueAddressExtra != null ? venueAddressExtra : "";
        latitude = latExtra;
        longitude = lngExtra;
        placeId = placeIdExtra != null ? placeIdExtra : "";

        tvVenueName.setText(venueName);
        tvVenueAddress.setText(venueAddress);

        if (latitude != 0 && longitude != 0) {
            tvLocation.setText("📍 " + String.format("%.6f", latitude) + ", " + String.format("%.6f", longitude));
        } else {
            tvLocation.setText("📍 No location set");
        }

        etDate.setText(dateExtra != null ? dateExtra : "");
        etTime.setText(timeExtra != null ? timeExtra : "");
        etPax.setText(String.valueOf(paxExtra));

        // Create booking object for reference
        currentBooking = new Booking();
        currentBooking.setBookingId(bookingId);
        currentBooking.setVenueName(venueName);
        currentBooking.setVenueAddress(venueAddress);
        currentBooking.setBookingDate(dateExtra);
        currentBooking.setBookingTime(timeExtra);
        currentBooking.setPax(paxExtra);
        currentBooking.setLatitude(latExtra);
        currentBooking.setLongitude(lngExtra);
        currentBooking.setVenueId(placeId);
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

                            Toast.makeText(EditBookingActivity.this, "✅ GPS location updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(EditBookingActivity.this, "❌ Unable to get location", Toast.LENGTH_SHORT).show();
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

    private void updateBooking() {
        // Get input values
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String paxStr = etPax.getText().toString().trim();

        // Validate
        if (venueName.isEmpty() || venueName.equals("No venue selected")) {
            Toast.makeText(this, "⚠️ Please select a venue", Toast.LENGTH_SHORT).show();
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
        bookingRef.child("latitude").setValue(latitude);
        bookingRef.child("longitude").setValue(longitude);
        bookingRef.child("venueId").setValue(placeId);

        // Add completion listener to the last operation
        bookingRef.child("pax").setValue(pax)
                .addOnCompleteListener(task -> {
                    progressOverlay.setVisibility(View.GONE);
                    btnUpdateBooking.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(EditBookingActivity.this,
                                "✅ Booking Updated!\n📍 " + venueName + "\n👤 " + pax + " people",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(EditBookingActivity.this,
                                "❌ Update failed: " + task.getException().getMessage(),
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