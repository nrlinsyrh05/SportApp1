package com.uitm.smartsportvenuefinder;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class BookingHistoryActivity extends AppCompatActivity {

    ListView lvBookings;
    ProgressBar progressBar;
    TextView tvNoBookings;
    DatabaseReference mDatabase;
    ArrayList<Booking> bookingList;
    BookingAdapter bookingAdapter;
    ValueEventListener bookingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views - These IDs must match your XML
        lvBookings = findViewById(R.id.lvBookings);
        progressBar = findViewById(R.id.progressBar);
        tvNoBookings = findViewById(R.id.tvNoBookings);

        bookingList = new ArrayList<>();
        bookingAdapter = new BookingAdapter(this, bookingList);
        lvBookings.setAdapter(bookingAdapter);

        loadBookings();
    }

    private void loadBookings() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            tvNoBookings.setVisibility(View.VISIBLE);
            tvNoBookings.setText("Please login to view bookings");
            progressBar.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvNoBookings.setVisibility(View.GONE);

        bookingListener = mDatabase.child("bookings")
                .orderByChild("userId")
                .equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        bookingList.clear();

                        for (DataSnapshot data : snapshot.getChildren()) {
                            Booking booking = data.getValue(Booking.class);
                            if (booking != null) {
                                booking.bookingId = data.getKey();
                                bookingList.add(booking);
                            }
                        }

                        bookingAdapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);

                        if (bookingList.isEmpty()) {
                            tvNoBookings.setVisibility(View.VISIBLE);
                            tvNoBookings.setText("No bookings found");
                        } else {
                            tvNoBookings.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(BookingHistoryActivity.this,
                                "Error loading bookings: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingListener != null && mDatabase != null) {
            mDatabase.child("bookings").removeEventListener(bookingListener);
        }
    }
}