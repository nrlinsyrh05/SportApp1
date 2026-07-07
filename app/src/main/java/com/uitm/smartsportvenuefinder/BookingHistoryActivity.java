package com.uitm.smartsportvenuefinder;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class BookingHistoryActivity extends AppCompatActivity implements BookingAdapter.OnBookingActionListener {

    private ListView lvBookings;
    private ProgressBar progressBar;
    private TextView tvNoBookings;
    private Button btnRefresh, btnClearSearch;
    private EditText etSearch;
    private DatabaseReference mDatabase;
    private ArrayList<Booking> bookingList;
    private ArrayList<Booking> filteredList;
    private BookingAdapter adapter;
    private ValueEventListener bookingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Bookings");
        }

        // Initialize views
        lvBookings = findViewById(R.id.lvBookings);
        progressBar = findViewById(R.id.progressBar);
        tvNoBookings = findViewById(R.id.tvNoBookings);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        etSearch = findViewById(R.id.etSearch);

        bookingList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new BookingAdapter(this, filteredList);
        adapter.setOnBookingActionListener(this);
        lvBookings.setAdapter(adapter);

        // Setup search
        setupSearch();

        btnRefresh.setOnClickListener(v -> loadBookings());

        loadBookings();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBookings(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            btnClearSearch.setVisibility(View.GONE);
            filterBookings("");
        });
    }

    private void filterBookings(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(bookingList);
            btnClearSearch.setVisibility(View.GONE);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Booking booking : bookingList) {
                if (booking.venueName != null && booking.venueName.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(booking);
                } else if (booking.venueAddress != null && booking.venueAddress.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(booking);
                } else if (booking.bookingDate != null && booking.bookingDate.contains(query)) {
                    filteredList.add(booking);
                } else if (booking.status != null && booking.status.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(booking);
                }
            }
            btnClearSearch.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();

        // Update no bookings message
        if (filteredList.isEmpty()) {
            if (bookingList.isEmpty()) {
                tvNoBookings.setText("No bookings found");
            } else {
                tvNoBookings.setText("No matching bookings found");
            }
            tvNoBookings.setVisibility(View.VISIBLE);
        } else {
            tvNoBookings.setVisibility(View.GONE);
        }
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

        if (bookingListener != null) {
            mDatabase.child("bookings").removeEventListener(bookingListener);
        }

        bookingListener = mDatabase.child("bookings")
                .orderByChild("userId")
                .equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        bookingList.clear();
                        filteredList.clear();

                        for (DataSnapshot data : snapshot.getChildren()) {
                            Booking booking = data.getValue(Booking.class);
                            if (booking != null) {
                                booking.bookingId = data.getKey();
                                bookingList.add(booking);
                            }
                        }

                        // Copy all to filtered list
                        filteredList.addAll(bookingList);
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);

                        if (filteredList.isEmpty()) {
                            tvNoBookings.setVisibility(View.VISIBLE);
                            tvNoBookings.setText("No bookings found");
                        } else {
                            tvNoBookings.setVisibility(View.GONE);
                        }

                        // Clear search if no query
                        if (etSearch.getText().toString().isEmpty()) {
                            btnClearSearch.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(BookingHistoryActivity.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Delete only - Edit now opens a new activity
    @Override
    public void onEdit(Booking booking) {
        // This is now handled in the adapter (opens EditBookingActivity)
        // Keep this method but it won't be called
    }

    @Override
    public void onDelete(Booking booking) {
        confirmDeleteBooking(booking);
    }

    private void confirmDeleteBooking(Booking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage("Are you sure you want to delete this booking for " + booking.venueName + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBooking(booking))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBooking(Booking booking) {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("bookings").child(booking.bookingId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "✅ Booking deleted!", Toast.LENGTH_SHORT).show();
                        loadBookings();
                    } else {
                        Toast.makeText(this, "❌ Delete failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingListener != null && mDatabase != null) {
            mDatabase.child("bookings").removeEventListener(bookingListener);
        }
    }
}