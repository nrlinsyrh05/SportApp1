package com.uitm.smartsportvenuefinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private Button btnSearchVenue, btnMap, btnBookingHistory, btnProfile, btnLogout, btnQuickBook;
    private TextView tvWelcome;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        btnSearchVenue = findViewById(R.id.btnSearchVenue);
        btnMap = findViewById(R.id.btnMap);
        btnBookingHistory = findViewById(R.id.btnBookingHistory);
        btnProfile = findViewById(R.id.btnProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnQuickBook = findViewById(R.id.btnQuickBook);
        tvWelcome = findViewById(R.id.tvWelcome);

        // Load user info
        loadUserInfo();

        // Button click listeners
        btnSearchVenue.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, VenueSearchActivity.class));
        });

        btnMap.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MapActivity.class));
        });

        btnQuickBook.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BookingActivity.class));
        });

        btnBookingHistory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BookingHistoryActivity.class));
        });

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            // ✅ FIRST: Get name from Realtime Database (most reliable)
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);

                        if (name != null && !name.isEmpty()) {
                            tvWelcome.setText("👋 Welcome back, " + name + "!");
                            Log.d(TAG, "Name from Database: " + name);
                            return;
                        }
                    }

                    // ✅ SECOND: Try display name from Authentication
                    String displayName = currentUser.getDisplayName();
                    if (displayName != null && !displayName.isEmpty()) {
                        tvWelcome.setText("👋 Welcome back, " + displayName + "!");
                        Log.d(TAG, "Name from Auth: " + displayName);
                        return;
                    }

                    // ✅ THIRD: Fallback to email username
                    String email = currentUser.getEmail();
                    if (email != null && !email.isEmpty()) {
                        String userName = email.split("@")[0];
                        tvWelcome.setText("👋 Welcome back, " + userName + "!");
                        Log.d(TAG, "Name from Email: " + userName);
                    } else {
                        tvWelcome.setText("👋 Welcome back!");
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Database error: " + error.getMessage());
                    // Fallback to display name or email
                    String displayName = currentUser.getDisplayName();
                    if (displayName != null && !displayName.isEmpty()) {
                        tvWelcome.setText("👋 Welcome back, " + displayName + "!");
                    } else {
                        tvWelcome.setText("👋 Welcome back!");
                    }
                }
            });
        } else {
            tvWelcome.setText("👋 Welcome!");
        }
    }
}