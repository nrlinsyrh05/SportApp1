package com.uitm.smartsportvenuefinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VenueSearchActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // UI Components
    private EditText etSearchVenue;
    private Button btnSearch, btnUseCurrentLocation, btnBookSelected;
    private RecyclerView rvSearchResults;
    private ProgressBar progressBar;
    private TextView tvNoResults, tvSelectedVenue;
    private ImageView ivClearSearch;

    // Google Maps
    private GoogleMap googleMap;
    private SupportMapFragment mapFragment;

    // Google Places
    private PlacesClient placesClient;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLatLng;

    // Data
    private List<Place> searchResults;
    private VenueSearchAdapter adapter;
    private Place selectedPlace;
    private Marker selectedMarker;

    // Constants
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_search);

        // Initialize Google Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_GOOGLE_API_KEY_HERE");
        }
        placesClient = Places.createClient(this);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        initViews();

        // Setup listeners
        setupListeners();

        // Initialize map
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_search);
        mapFragment.getMapAsync(this);

        // Get current location
        getCurrentLocation();
    }

    private void initViews() {
        etSearchVenue = findViewById(R.id.etSearchVenue);
        btnSearch = findViewById(R.id.btnSearch);
        btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);
        btnBookSelected = findViewById(R.id.btnBookSelected);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        progressBar = findViewById(R.id.progressBar);
        tvNoResults = findViewById(R.id.tvNoResults);
        tvSelectedVenue = findViewById(R.id.tvSelectedVenue);
        ivClearSearch = findViewById(R.id.ivClearSearch);

        searchResults = new ArrayList<>();
        adapter = new VenueSearchAdapter(this, searchResults, venue -> {
            selectedPlace = venue;
            showSelectedVenue(venue);
            moveMapToVenue(venue);
        });

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);
        rvSearchResults.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> searchVenues());

        btnUseCurrentLocation.setOnClickListener(v -> {
            getCurrentLocation();
            searchNearbyVenues();
        });

        btnBookSelected.setOnClickListener(v -> {
            if (selectedPlace != null) {
                proceedToBooking(selectedPlace);
            } else {
                Toast.makeText(this, "Please select a venue first", Toast.LENGTH_SHORT).show();
            }
        });

        ivClearSearch.setOnClickListener(v -> {
            etSearchVenue.setText("");
            searchResults.clear();
            adapter.notifyDataSetChanged();
            rvSearchResults.setVisibility(View.GONE);
            tvNoResults.setVisibility(View.GONE);
        });

        etSearchVenue.setOnEditorActionListener((v, actionId, event) -> {
            searchVenues();
            return true;
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Location location = task.getResult();
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        if (googleMap != null) {
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                        }

                        Toast.makeText(this, "📍 Location updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void searchVenues() {
        String query = etSearchVenue.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a venue name", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        tvNoResults.setVisibility(View.GONE);

        // Use Autocomplete for search
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query + " sports venue")
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setSessionToken(token)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    progressBar.setVisibility(View.GONE);

                    if (response.getAutocompletePredictions() != null &&
                            !response.getAutocompletePredictions().isEmpty()) {

                        // Fetch details for each prediction
                        searchResults.clear();
                        for (com.google.android.libraries.places.api.model.AutocompletePrediction prediction :
                                response.getAutocompletePredictions()) {
                            fetchPlaceDetails(prediction.getPlaceId());
                        }
                    } else {
                        rvSearchResults.setVisibility(View.GONE);
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("No venues found. Try a different search.");
                    }
                })
                .addOnFailureListener(exception -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Search failed: " + exception.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchPlaceDetails(String placeId) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES
        );

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, fields).build();

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    if (place != null && place.getLatLng() != null) {
                        // Check if it's a sports venue
                        boolean isSportsVenue = false;
                        if (place.getTypes() != null) {
                            for (com.google.android.libraries.places.api.model.Place.Type type : place.getTypes()) {
                                if (type.toString().contains("SPORTS") ||
                                        type.toString().contains("STADIUM") ||
                                        type.toString().contains("GYM")) {
                                    isSportsVenue = true;
                                    break;
                                }
                            }
                        }

                        // If not clearly a sports venue, still add it if it has "sports" in name
                        if (!isSportsVenue && place.getName() != null) {
                            String name = place.getName().toLowerCase();
                            if (name.contains("sport") || name.contains("stadium") ||
                                    name.contains("court") || name.contains("field") ||
                                    name.contains("gym") || name.contains("arena")) {
                                isSportsVenue = true;
                            }
                        }

                        if (isSportsVenue) {
                            searchResults.add(place);
                            adapter.notifyDataSetChanged();
                            showVenuesOnMap(searchResults);
                            rvSearchResults.setVisibility(View.VISIBLE);
                            tvNoResults.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(exception -> {
                    // Silently fail for individual place fetch
                });
    }

    private void searchNearbyVenues() {
        if (currentLatLng == null) {
            Toast.makeText(this, "Please get your location first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        tvNoResults.setVisibility(View.GONE);

        // Use Autocomplete for nearby search
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery("sports venue stadium court")
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setSessionToken(token)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    progressBar.setVisibility(View.GONE);

                    if (response.getAutocompletePredictions() != null &&
                            !response.getAutocompletePredictions().isEmpty()) {

                        searchResults.clear();
                        for (com.google.android.libraries.places.api.model.AutocompletePrediction prediction :
                                response.getAutocompletePredictions()) {
                            fetchPlaceDetails(prediction.getPlaceId());
                        }

                        // If no results after fetching
                        if (searchResults.isEmpty()) {
                            rvSearchResults.setVisibility(View.GONE);
                            tvNoResults.setVisibility(View.VISIBLE);
                            tvNoResults.setText("No nearby sports venues found");
                        }
                    } else {
                        rvSearchResults.setVisibility(View.GONE);
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("No nearby sports venues found");
                    }
                })
                .addOnFailureListener(exception -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Search failed: " + exception.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showVenuesOnMap(List<Place> places) {
        if (googleMap == null) return;

        googleMap.clear();

        for (Place place : places) {
            if (place.getLatLng() != null) {
                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(place.getLatLng())
                        .title(place.getName())
                        .snippet(place.getAddress()));
                marker.setTag(place);
            }
        }

        if (!places.isEmpty() && places.get(0).getLatLng() != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    places.get(0).getLatLng(), 14f));
        }
    }

    private void showSelectedVenue(Place place) {
        String name = place.getName() != null ? place.getName() : "Unknown Venue";
        String address = place.getAddress() != null ? place.getAddress() : "";

        tvSelectedVenue.setText("📍 Selected: " + name);
        tvSelectedVenue.setVisibility(View.VISIBLE);
        btnBookSelected.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
    }

    private void moveMapToVenue(Place place) {
        if (googleMap != null && place.getLatLng() != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 17f));
        }
    }

    private void proceedToBooking(Place place) {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("venueName", place.getName() != null ? place.getName() : "Venue");
        intent.putExtra("venueAddress", place.getAddress() != null ? place.getAddress() : "");
        intent.putExtra("venueId", place.getId());

        if (place.getLatLng() != null) {
            intent.putExtra("latitude", place.getLatLng().latitude);
            intent.putExtra("longitude", place.getLatLng().longitude);
        }

        startActivity(intent);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setOnMarkerClickListener(this);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        if (currentLatLng != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f));
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(3.1390, 101.6869), 12f));
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTag() instanceof Place) {
            Place place = (Place) marker.getTag();
            selectedPlace = place;
            showSelectedVenue(place);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}