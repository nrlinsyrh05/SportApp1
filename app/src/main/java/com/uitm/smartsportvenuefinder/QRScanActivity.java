package com.uitm.smartsportvenuefinder;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class QRScanActivity extends AppCompatActivity {

    DatabaseReference mDatabase;

    ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String bookingId = result.getContents();
                    updateBookingStatus(bookingId);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        startScan();
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan your booking QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        barcodeLauncher.launch(options);
    }

    private void updateBookingStatus(String bookingId) {
        mDatabase.child("bookings").child(bookingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    mDatabase.child("bookings").child(bookingId).child("status").setValue("Checked In");
                    Toast.makeText(QRScanActivity.this, "Check-in successful!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(QRScanActivity.this, "Booking not found!", Toast.LENGTH_SHORT).show();
                }
                finish();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(QRScanActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}