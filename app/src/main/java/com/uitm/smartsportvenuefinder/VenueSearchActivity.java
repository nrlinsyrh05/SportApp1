package com.uitm.smartsportvenuefinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
    private boolean isLocationPermissionGranted = false;

    // Data
    private List<Place> searchResults;
    private VenueSearchAdapter adapter;
    private Place selectedPlace;

    // Constants
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String API_KEY = "AIzaSyDr64tr-Y3YopYDi7PmbUou96Q0o3wSYlI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_search);

        // Initialize Google Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY);
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

        // Check location permission and get location
        checkLocationPermission();
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
            if (venue.getLatLng() != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(venue.getLatLng(), 17f));
            }
        });

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);
        rvSearchResults.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> {
            hideKeyboard();
            searchVenues();
        });

        etSearchVenue.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                searchVenues();
                return true;
            }
            return false;
        });

        btnUseCurrentLocation.setOnClickListener(v -> {
            if (currentLatLng != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                searchNearbyVenues();
            } else {
                getCurrentLocation();
            }
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
            ivClearSearch.setVisibility(View.GONE);
            if (googleMap != null) {
                googleMap.clear();
                if (currentLatLng != null) {
                    googleMap.addMarker(new MarkerOptions()
                            .position(currentLatLng)
                            .title("📍 Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                }
            }
            hideKeyboard();
        });

        etSearchVenue.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            isLocationPermissionGranted = true;
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    private void getCurrentLocation() {
        if (!isLocationPermissionGranted) {
            checkLocationPermission();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        Location location = task.getResult();
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        if (googleMap != null) {
                            googleMap.addMarker(new MarkerOptions()
                                    .position(currentLatLng)
                                    .title("📍 Your Location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                            searchNearbyVenues();
                        }

                        Toast.makeText(this, "📍 Location found", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "❌ Unable to get location. Try GPS button.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void searchVenues() {
        String query = etSearchVenue.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a venue name", Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard();
        progressBar.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        tvNoResults.setVisibility(View.GONE);
        searchResults.clear();

        // Use FindAutocompletePredictions for search
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setSessionToken(token)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    progressBar.setVisibility(View.GONE);

                    if (response.getAutocompletePredictions() != null &&
                            !response.getAutocompletePredictions().isEmpty()) {

                        // Fetch details for each prediction
                        for (com.google.android.libraries.places.api.model.AutocompletePrediction prediction :
                                response.getAutocompletePredictions()) {
                            fetchPlaceDetails(prediction.getPlaceId());
                        }
                    } else {
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("No venues found. Try a different search.");
                        if (googleMap != null) {
                            googleMap.clear();
                            if (currentLatLng != null) {
                                googleMap.addMarker(new MarkerOptions()
                                        .position(currentLatLng)
                                        .title("📍 Your Location")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            }
                        }
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
                Place.Field.TYPES,
                Place.Field.RATING
        );

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, fields).build();

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    if (place != null && place.getLatLng() != null) {
                        // Check if it's a sports venue or relevant
                        boolean isRelevant = true;
                        // You can add filtering logic here if needed

                        if (isRelevant) {
                            searchResults.add(place);
                            adapter.notifyDataSetChanged();
                            showVenuesOnMap(searchResults);
                            rvSearchResults.setVisibility(View.VISIBLE);
                            tvNoResults.setVisibility(View.GONE);
                            zoomToFitMarkers(searchResults);
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
            getCurrentLocation();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        tvNoResults.setVisibility(View.GONE);
        searchResults.clear();

        // Search for nearby venues using Autocomplete
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery("sports venue stadium court gym fitness")
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setSessionToken(token)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    progressBar.setVisibility(View.GONE);

                    if (response.getAutocompletePredictions() != null &&
                            !response.getAutocompletePredictions().isEmpty()) {

                        for (com.google.android.libraries.places.api.model.AutocompletePrediction prediction :
                                response.getAutocompletePredictions()) {
                            fetchPlaceDetails(prediction.getPlaceId());
                        }

                        if (searchResults.isEmpty()) {
                            tvNoResults.setVisibility(View.VISIBLE);
                            tvNoResults.setText("No nearby sports venues found");
                            if (googleMap != null) {
                                googleMap.clear();
                                if (currentLatLng != null) {
                                    googleMap.addMarker(new MarkerOptions()
                                            .position(currentLatLng)
                                            .title("📍 Your Location")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                }
                            }
                        }
                    } else {
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("No nearby sports venues found");
                        if (googleMap != null) {
                            googleMap.clear();
                            if (currentLatLng != null) {
                                googleMap.addMarker(new MarkerOptions()
                                        .position(currentLatLng)
                                        .title("📍 Your Location")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            }
                        }
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

        // Add current location marker
        if (currentLatLng != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("📍 Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }

        // Add venue markers
        if (places != null && !places.isEmpty()) {
            for (Place place : places) {
                if (place.getLatLng() != null) {
                    String title = place.getName() != null ? place.getName() : "Venue";
                    String snippet = place.getAddress() != null ? place.getAddress() : "";

                    if (place.getRating() != null) {
                        snippet += " ⭐ " + place.getRating();
                    }

                    Marker marker = googleMap.addMarker(new MarkerOptions()
                            .position(place.getLatLng())
                            .title(title)
                            .snippet(snippet)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    marker.setTag(place);
                }
            }
        }
    }

    private void zoomToFitMarkers(List<Place> places) {
        if (places == null || places.isEmpty() || googleMap == null) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (Place place : places) {
            if (place.getLatLng() != null) {
                builder.include(place.getLatLng());
            }
        }

        if (currentLatLng != null) {
            builder.include(currentLatLng);
        }

        LatLngBounds bounds = builder.build();
        int padding = 100;
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    private void showSelectedVenue(Place place) {
        String name = place.getName() != null ? place.getName() : "Unknown Venue";
        String address = place.getAddress() != null ? place.getAddress() : "";

        tvSelectedVenue.setText("📍 Selected: " + name + " (" + address + ")");
        tvSelectedVenue.setVisibility(View.VISIBLE);
        btnBookSelected.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
    }

    private void moveMapToVenue(Place place) {
        if (googleMap != null && place.getLatLng() != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 17f));
        }
    }

    // In the proceedToBooking method, change from startActivity to startActivityForResult:

    private void proceedToBooking(Place place) {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("venueName", place.getName() != null ? place.getName() : "Venue");
        intent.putExtra("venueAddress", place.getAddress() != null ? place.getAddress() : "");
        intent.putExtra("venueId", place.getId());

        if (place.getLatLng() != null) {
            intent.putExtra("latitude", place.getLatLng().latitude);
            intent.putExtra("longitude", place.getLatLng().longitude);
        }

        // Use startActivityForResult to return to booking
        startActivity(intent);
        finish();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setOnMarkerClickListener(this);
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        this.googleMap.getUiSettings().setCompassEnabled(true);
        this.googleMap.getUiSettings().setMapToolbarEnabled(true);

        if (isLocationPermissionGranted) {
            try {
                this.googleMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        if (currentLatLng != null) {
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f));
            this.googleMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("📍 Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            LatLng defaultLocation = new LatLng(3.1390, 101.6869);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTag() instanceof Place) {
            Place place = (Place) marker.getTag();
            selectedPlace = place;
            showSelectedVenue(place);
            marker.showInfoWindow();
            return true;
        } else if (marker.getTitle() != null && marker.getTitle().contains("Your Location")) {
            Toast.makeText(this, "📍 Your current location", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isLocationPermissionGranted = true;
                getCurrentLocation();

                if (googleMap != null) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(this, "⚠️ Location permission denied. You can still search manually.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}