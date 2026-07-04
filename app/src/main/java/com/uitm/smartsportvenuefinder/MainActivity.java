package com.uitm.smartsportvenuefinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    Button btnBookVenue, btnMap, btnBookingHistory, btnProfile, btnLogout, btnQuickBook;
    TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        btnBookVenue = findViewById(R.id.btnBookVenue);
        btnMap = findViewById(R.id.btnMap);
        btnBookingHistory = findViewById(R.id.btnBookingHistory);
        btnProfile = findViewById(R.id.btnProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnQuickBook = findViewById(R.id.btnQuickBook);
        tvStatus = findViewById(R.id.tvStatus);

        // Display user info
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            tvStatus.setText("👋 Welcome, " + (email != null ? email : "User"));
        }

        // Book Venue button - Direct booking form
        btnBookVenue.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BookingActivity.class);
            startActivity(intent);
        });

        // Quick Book button - Direct booking form
        btnQuickBook.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BookingActivity.class);
            startActivity(intent);
        });

        // In MainActivity.java, add this button
        Button btnSearchVenue;

        // In onCreate
        btnSearchVenue = findViewById(R.id.btnSearchVenue);

        btnSearchVenue.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, VenueSearchActivity.class));
        });

        // Map button
        btnMap.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MapActivity.class));
        });

        // Booking history button
        btnBookingHistory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BookingHistoryActivity.class));
        });

        // Profile button
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        // Logout button
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }
}