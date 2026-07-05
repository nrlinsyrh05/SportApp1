package com.uitm.smartsportvenuefinder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);

        }

        addDemoMarkers();

        mMap.setOnMarkerClickListener(marker -> {

            showVenueDialog(marker);

            return true;

        });


    }

    private void addDemoMarkers() {

        // UiTM Shah Alam Sports Complex
        LatLng uitm = new LatLng(3.0699, 101.5006);

        mMap.addMarker(new MarkerOptions()
                .position(uitm)
                .title("Kompleks Sukan UiTM Shah Alam Sports Complex")
                .snippet("Football")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Kompleks Sukan Panasonic Shah Alam
        LatLng panasonic = new LatLng(3.0824, 101.5330);

        mMap.addMarker(new MarkerOptions()
                .position(panasonic)
                .title("Kompleks Sukan Panasonic Shah Alam")
                .snippet("Multi Sports")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        // Kompleks Sukan SUK Selangor
        LatLng suk = new LatLng(3.0862, 101.5285);

        mMap.addMarker(new MarkerOptions()
                .position(suk)
                .title("Kompleks Sukan SUK Selangor")
                .snippet("Football")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Setia Alam Badminton Arena
        LatLng badminton = new LatLng(3.1093, 101.4628);

        mMap.addMarker(new MarkerOptions()
                .position(badminton)
                .title("Setia Alam Badminton Arena Shah Alam")
                .snippet("Badminton")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

        // Frenzy Sports Arena
        LatLng frenzy = new LatLng(3.0724, 101.4875);

        mMap.addMarker(new MarkerOptions()
                .position(frenzy)
                .title("Frenzy Sports Arena Shah Alam")
                .snippet("Indoor Sports")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Move Camera
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uitm, 12f));
    }
    private void showVenueDialog(com.google.android.gms.maps.model.Marker marker) {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(marker.getTitle());

        builder.setMessage(
                "Sport : " + marker.getSnippet() +
                        "\n\nDo you want to navigate to this venue?"
        );

        builder.setPositiveButton("Navigate", (dialog, which) -> {

            String venueName = marker.getTitle();

            Uri uri = Uri.parse(
                    "geo:0,0?q=" + Uri.encode(venueName + ", Shah Alam")
            );

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);

            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(getPackageManager()) != null) {

                startActivity(intent);

            } else {

                Intent browser = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                                "https://www.google.com/maps/search/?api=1&query="
                                        + Uri.encode(venueName + ", Shah Alam")
                        )
                );

                startActivity(browser);

            }

        });
        builder.setNegativeButton("Close", null);

        builder.show();

    }
}