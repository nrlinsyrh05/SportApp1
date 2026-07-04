package com.uitm.smartsportvenuefinder;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Add sample venues if they don't exist
        addSampleVenues();

        new Handler().postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 2000);
    }

    private void addSampleVenues() {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        // Check if venues already exist
        mDatabase.child("venues").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().exists()) {
                // Add sample venues
                addVenue(mDatabase, "venue1", "Stadium A", "Football",
                        "123 Main Street, Kuala Lumpur", "RM 20.00", 3.1390, 101.6869);
                addVenue(mDatabase, "venue2", "Badminton Court", "Badminton",
                        "456 Park Avenue, Kuala Lumpur", "RM 15.00", 3.1480, 101.6970);
                addVenue(mDatabase, "venue3", "Swimming Pool", "Swimming",
                        "789 Sports Complex, Kuala Lumpur", "RM 25.00", 3.1570, 101.7070);
                addVenue(mDatabase, "venue4", "Tennis Court", "Tennis",
                        "321 Tennis Lane, Kuala Lumpur", "RM 30.00", 3.1660, 101.7170);
                addVenue(mDatabase, "venue5", "Basketball Court", "Basketball",
                        "654 Basketball Street, Kuala Lumpur", "RM 18.00", 3.1750, 101.7270);
            }
        });
    }

    private void addVenue(DatabaseReference mDatabase, String id, String name,
                          String sport, String address, String price, double lat, double lng) {
        Venue venue = new Venue(id, name, sport, address, lat, lng, price, "");
        mDatabase.child("venues").child(id).setValue(venue);
    }
}