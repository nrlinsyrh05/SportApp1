package com.uitm.smartsportvenuefinder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap mMap;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lng = getIntent().getDoubleExtra("longitude", 0);
        String name = getIntent().getStringExtra("venueName");

        if (lat != 0 && lng != 0) {
            LatLng venueLatLng = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions().position(venueLatLng).title(name));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(venueLatLng, 15f));
        } else {
            loadAllVenueMarkers();
        }
    }

    private void loadAllVenueMarkers() {
        mDatabase.child("venues").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Venue venue = data.getValue(Venue.class);
                    if (venue != null) {
                        LatLng latLng = new LatLng(venue.latitude, venue.longitude);
                        mMap.addMarker(new MarkerOptions().position(latLng).title(venue.venueName));
                    }
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(3.0738, 101.5183), 12f));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MapActivity.this, "Failed to load map", Toast.LENGTH_SHORT).show();
            }
        });
    }
}