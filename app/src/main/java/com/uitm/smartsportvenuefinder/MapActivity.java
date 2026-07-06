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
        LatLng uitm = new LatLng(3.067411043253576, 101.49726815809952);
        mMap.addMarker(new MarkerOptions()
                .position(uitm)
                .title("UiTM Shah Alam Sports Complex")
                .snippet("Football")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Kompleks Sukan Panasonic
        LatLng panasonic = new LatLng(3.0568666090523804, 101.5479922622229);
        mMap.addMarker(new MarkerOptions()
                .position(panasonic)
                .title("Kompleks Sukan Panasonic")
                .snippet("Multi Sports")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        // Kompleks Sukan Shah Alam
        LatLng shahAlam = new LatLng(3.086741331783849, 101.51462971657962);
        mMap.addMarker(new MarkerOptions()
                .position(shahAlam)
                .title("Kompleks Sukan Shah Alam")
                .snippet("Football")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Setia Alam Badminton Arena
        LatLng setia = new LatLng(3.104788661771676, 101.47807245342857);
        mMap.addMarker(new MarkerOptions()
                .position(setia)
                .title("Setia Alam Badminton Arena")
                .snippet("Badminton")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

        // Frenzy Sports Arena
        LatLng frenzy = new LatLng(3.0610784570454928, 101.4999115288357);
        mMap.addMarker(new MarkerOptions()
                .position(frenzy)
                .title("Frenzy Sports Arena")
                .snippet("Indoor Sports")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // DO Arena Space U8
        LatLng doArena = new LatLng(3.113122919396162, 101.55394981055719);
        mMap.addMarker(new MarkerOptions()
                .position(doArena)
                .title("DO Arena Space U8")
                .snippet("Football / Futsal")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        // Wembley Futsal Arena
        LatLng wembley = new LatLng(3.047656504936154, 101.50392375342858);
        mMap.addMarker(new MarkerOptions()
                .position(wembley)
                .title("Wembley Futsal Arena")
                .snippet("Futsal")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        // Shah Alam Badminton Centre
        LatLng badminton = new LatLng(3.065130664192046, 101.52661082459288);
        mMap.addMarker(new MarkerOptions()
                .position(badminton)
                .title("Shah Alam Badminton Centre")
                .snippet("Badminton")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Section 6 Sports Complex
        LatLng section6 = new LatLng(3.0834116060104346, 101.51041681665004);
        mMap.addMarker(new MarkerOptions()
                .position(section6)
                .title("Section 6 Sports Complex")
                .snippet("Multi Sports")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));

        // =======================================
// Digital Sports Arena i-City
// =======================================
        LatLng digital = new LatLng(3.065370429256171, 101.48219293691248);

        mMap.addMarker(new MarkerOptions()
                .position(digital)
                .title("Digital Sports Arena i-City")
                .snippet("Indoor Sports")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));


// =======================================
// Empire Sport Arena
// =======================================
        LatLng empire = new LatLng(3.0051846408202754, 101.50364056760135);

        mMap.addMarker(new MarkerOptions()
                .position(empire)
                .title("Empire Sport Arena")
                .snippet("Multi Sports")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));


// =======================================
// Forum SBA
// =======================================
        LatLng forum = new LatLng(3.1141258819078765, 101.45248507368193);

        mMap.addMarker(new MarkerOptions()
                .position(forum)
                .title("Forum SBA")
                .snippet("Badminton")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));


// =======================================
// Radia Arena
// =======================================
        LatLng radia = new LatLng(3.101715336153319, 101.53638013372917);

        mMap.addMarker(new MarkerOptions()
                .position(radia)
                .title("Radia Arena")
                .snippet("Football / Futsal")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));


// =======================================
// FBSA by Nuova Sport Arena
// =======================================
        LatLng fbsa = new LatLng(3.0552554532685994, 101.48234900992914);

        mMap.addMarker(new MarkerOptions()
                .position(fbsa)
                .title("FBSA by Nuova Sport Arena")
                .snippet("Badminton")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));


// =======================================
// Yosin Kampung Subang Court
// =======================================
        LatLng yosin = new LatLng(3.14303389047075, 101.53435762527357);

        mMap.addMarker(new MarkerOptions()
                .position(yosin)
                .title("Yosin Kampung Subang Court")
                .snippet("Badminton")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));


// =======================================
// Arena Futsal Padang Jawa
// =======================================
        LatLng padangJawa = new LatLng(3.0517720996792552, 101.50009253401522);

        mMap.addMarker(new MarkerOptions()
                .position(padangJawa)
                .title("Arena Futsal Padang Jawa")
                .snippet("Futsal")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));


// =======================================
// Bowl America i-City
// =======================================
        LatLng bowlAmerica = new LatLng(3.0653490022864562, 101.48226803876524);

        mMap.addMarker(new MarkerOptions()
                .position(bowlAmerica)
                .title("Bowl America i-City")
                .snippet("Bowling")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));


// =======================================
// Alam Lanes AEON Shah Alam
// =======================================
        LatLng alamLanes = new LatLng(3.0776783405200088, 101.54935913691247);

        mMap.addMarker(new MarkerOptions()
                .position(alamLanes)
                .title("Alam Lanes AEON Shah Alam")
                .snippet("Bowling")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));


// =======================================
// Wangsa Bowl Setia City Mall
// =======================================
        LatLng wangsa = new LatLng(3.110306434584387, 101.46076664855136);

        mMap.addMarker(new MarkerOptions()
                .position(wangsa)
                .title("Wangsa Bowl Setia City Mall")
                .snippet("Bowling")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));


// =======================================
// Ole Ole Super Bowl
// =======================================
        LatLng oleole = new LatLng(3.0438448740119717, 101.51777790199579);

        mMap.addMarker(new MarkerOptions()
                .position(oleole)
                .title("Ole Ole Super Bowl")
                .snippet("Bowling")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uitm, 11.5f));
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