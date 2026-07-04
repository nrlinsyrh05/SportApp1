package com.uitm.smartsportvenuefinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class VenueDetailActivity extends AppCompatActivity {

    TextView tvName, tvSport, tvAddress, tvPrice;
    Button btnBook, btnMap, btnReview;
    String venueId, venueName, sportType, address, price;
    double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_detail);

        tvName = findViewById(R.id.tvName);
        tvSport = findViewById(R.id.tvSport);
        tvAddress = findViewById(R.id.tvAddress);
        tvPrice = findViewById(R.id.tvPrice);
        btnBook = findViewById(R.id.btnBook);
        btnMap = findViewById(R.id.btnMap);
        btnReview = findViewById(R.id.btnReview);

        venueId = getIntent().getStringExtra("venueId");
        venueName = getIntent().getStringExtra("venueName");
        sportType = getIntent().getStringExtra("sportType");
        address = getIntent().getStringExtra("address");
        price = getIntent().getStringExtra("price");
        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);

        tvName.setText(venueName);
        tvSport.setText("Sport: " + sportType);
        tvAddress.setText("Address: " + address);
        tvPrice.setText("Price: " + price);

        btnBook.setOnClickListener(v -> {
            Intent intent = new Intent(VenueDetailActivity.this, BookingActivity.class);
            intent.putExtra("venueId", venueId);
            intent.putExtra("venueName", venueName);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            startActivity(intent);
        });

        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(VenueDetailActivity.this, MapActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("venueName", venueName);
            startActivity(intent);
        });

        btnReview.setOnClickListener(v -> {
            Intent intent = new Intent(VenueDetailActivity.this, ReviewActivity.class);
            intent.putExtra("venueId", venueId);
            intent.putExtra("venueName", venueName);
            startActivity(intent);
        });
    }
}