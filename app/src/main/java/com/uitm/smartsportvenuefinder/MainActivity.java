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

    // REMOVED: btnSearchVenue
    private Button btnMap, btnBookingHistory, btnProfile, btnLogout, btnQuickBook, btnContactAdmin;
    private TextView tvWelcome;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views - REMOVED btnSearchVenue
        btnMap = findViewById(R.id.btnMap);
        btnBookingHistory = findViewById(R.id.btnBookingHistory);
        btnProfile = findViewById(R.id.btnProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnQuickBook = findViewById(R.id.btnQuickBook);
        btnContactAdmin = findViewById(R.id.btnContactAdmin);
        tvWelcome = findViewById(R.id.tvWelcome);

        // Load user info
        loadUserInfo();

        // Button click listeners - REMOVED btnSearchVenue listener
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

        btnContactAdmin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ContactAdminActivity.class));
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

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);

                        if (name != null && !name.isEmpty()) {
                            tvWelcome.setText("👋 Welcome back, " + name + "!");
                            return;
                        }
                    }

                    String displayName = currentUser.getDisplayName();
                    if (displayName != null && !displayName.isEmpty()) {
                        tvWelcome.setText("👋 Welcome back, " + displayName + "!");
                        return;
                    }

                    String email = currentUser.getEmail();
                    if (email != null && !email.isEmpty()) {
                        String userName = email.split("@")[0];
                        tvWelcome.setText("👋 Welcome back, " + userName + "!");
                    } else {
                        tvWelcome.setText("👋 Welcome back!");
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
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