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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
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
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // UI Components
    private EditText etSearchVenue;
    private Button btnSearch, btnDetailsSelected, btnNavigateSelected;
    private RecyclerView rvSearchResults;
    private ProgressBar progressBar;
    private TextView tvNoResults, tvSelectedVenue;
    private ImageView ivClearSearch;
    private LinearLayout llActionButtons;

    // Google Maps
    private GoogleMap googleMap;
    private SupportMapFragment mapFragment;

    // Google Places
    private PlacesClient placesClient;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLatLng;
    private boolean isLocationPermissionGranted = false;
    private boolean isMapReady = false;

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
        setContentView(R.layout.activity_map);

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
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Check location permission and get location
        checkLocationPermission();
    }

    private void initViews() {
        etSearchVenue = findViewById(R.id.etSearchVenue);
        btnSearch = findViewById(R.id.btnSearch);
        btnDetailsSelected = findViewById(R.id.btnDetailsSelected);
        btnNavigateSelected = findViewById(R.id.btnNavigateSelected);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        progressBar = findViewById(R.id.progressBar);
        tvNoResults = findViewById(R.id.tvNoResults);
        tvSelectedVenue = findViewById(R.id.tvSelectedVenue);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        llActionButtons = findViewById(R.id.llActionButtons);

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
        llActionButtons.setVisibility(View.GONE);
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

        // DETAILS button - opens VenueDetailActivity
        btnDetailsSelected.setOnClickListener(v -> {
            if (selectedPlace != null) {
                showVenueDetails(selectedPlace);
            } else {
                Toast.makeText(this, "Please select a venue first", Toast.LENGTH_SHORT).show();
            }
        });

        // NAVIGATE button - launches built-in navigation
        btnNavigateSelected.setOnClickListener(v -> {
            if (selectedPlace != null) {
                launchBuiltInNavigation(selectedPlace);
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
            tvSelectedVenue.setVisibility(View.GONE);
            llActionButtons.setVisibility(View.GONE);
            ivClearSearch.setVisibility(View.GONE);
            selectedPlace = null;
            if (googleMap != null) {
                googleMap.clear();
                if (currentLatLng != null) {
                    googleMap.addMarker(new MarkerOptions()
                            .position(currentLatLng)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                }
                addDemoMarkers();
                if (currentLatLng != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
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

                        if (googleMap != null && isMapReady) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                            googleMap.addMarker(new MarkerOptions()
                                    .position(currentLatLng)
                                    .title("Your Location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            searchNearbyVenues();
                        }

                        Toast.makeText(this, "Location found", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Unable to get location.", Toast.LENGTH_SHORT).show();
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
                                        .title("Your Location")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            }
                            addDemoMarkers();
                            if (currentLatLng != null) {
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
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
                        boolean isRelevant = true;

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
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        tvNoResults.setVisibility(View.GONE);
        searchResults.clear();

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
                            tvNoResults.setText("Nearby sports venues");
                            if (googleMap != null) {
                                googleMap.clear();
                                if (currentLatLng != null) {
                                    googleMap.addMarker(new MarkerOptions()
                                            .position(currentLatLng)
                                            .title("Your Location")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                }
                                addDemoMarkers();
                                if (currentLatLng != null) {
                                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                                }
                            }
                        }
                    } else {
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("Nearby sports venues");
                        if (googleMap != null) {
                            googleMap.clear();
                            if (currentLatLng != null) {
                                googleMap.addMarker(new MarkerOptions()
                                        .position(currentLatLng)
                                        .title("Your Location")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            }
                            addDemoMarkers();
                            if (currentLatLng != null) {
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
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

        if (currentLatLng != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }

        addDemoMarkers();

        if (places != null && !places.isEmpty()) {
            for (Place place : places) {
                if (place.getLatLng() != null) {
                    String title = place.getName() != null ? place.getName() : "Venue";
                    String snippet = place.getAddress() != null ? place.getAddress() : "";

                    if (place.getRating() != null) {
                        snippet += " Rating: " + place.getRating();
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

        if (currentLatLng != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
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

        tvSelectedVenue.setText("Selected: " + name);
        tvSelectedVenue.setVisibility(View.VISIBLE);
        llActionButtons.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
    }

    private void moveMapToVenue(Place place) {
        if (googleMap != null && place.getLatLng() != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 17f));
        }
    }

    // ============ VIEW DETAILS ============
    private void showVenueDetails(Place place) {
        Intent intent = new Intent(MapActivity.this, VenueDetailActivity.class);
        intent.putExtra("venueName", place.getName() != null ? place.getName() : "Venue");
        intent.putExtra("venueAddress", place.getAddress() != null ? place.getAddress() : "");
        intent.putExtra("sport", "Sports Venue");

        if (place.getLatLng() != null) {
            intent.putExtra("latitude", String.valueOf(place.getLatLng().latitude));
            intent.putExtra("longitude", String.valueOf(place.getLatLng().longitude));
        }

        startActivity(intent);
    }

    // ============ LAUNCH NAVIGATION ============
    private void launchBuiltInNavigation(Place place) {
        if (currentLatLng == null) {
            Toast.makeText(this, "Your location not available. Please enable GPS.", Toast.LENGTH_LONG).show();
            return;
        }

        if (place.getLatLng() == null) {
            Toast.makeText(this, "Venue location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        String originStr = currentLatLng.latitude + "," + currentLatLng.longitude;
        String destStr = place.getLatLng().latitude + "," + place.getLatLng().longitude;
        String venueName = place.getName() != null ? place.getName() : "Venue";

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + originStr +
                "&destination=" + destStr +
                "&key=" + API_KEY +
                "&mode=driving" +
                "&language=en";

        new Thread(() -> {
            try {
                java.net.URL requestUrl = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) requestUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();

                java.io.InputStream inputStream = connection.getInputStream();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String finalResponse = response.toString();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    parseDirectionsResponse(finalResponse, currentLatLng, place.getLatLng(), venueName);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MapActivity.this, "Failed to get directions: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void parseDirectionsResponse(String response, LatLng origin, LatLng dest, String venueName) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            String status = jsonObject.getString("status");

            if (status.equals("OK")) {
                JSONArray routes = jsonObject.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);

                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String points = overviewPolyline.getString("points");

                    Intent intent = new Intent(MapActivity.this, NavigationActivity.class);
                    intent.putExtra("startLocation", origin);
                    intent.putExtra("destination", dest);
                    intent.putExtra("destinationName", venueName);
                    intent.putExtra("encodedPolyline", points);
                    startActivity(intent);

                } else {
                    Toast.makeText(this, "No route found", Toast.LENGTH_SHORT).show();
                }
            } else {
                String errorMessage = getDirectionsErrorMessage(status);
                Toast.makeText(this, "Directions error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing directions: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getDirectionsErrorMessage(String status) {
        switch (status) {
            case "NOT_FOUND": return "Origin or destination not found";
            case "ZERO_RESULTS": return "No route found between locations";
            case "MAX_WAYPOINTS_EXCEEDED": return "Too many waypoints";
            case "INVALID_REQUEST": return "Invalid request";
            case "OVER_QUERY_LIMIT": return "Query limit exceeded";
            case "REQUEST_DENIED": return "Request denied - check API key";
            case "UNKNOWN_ERROR": return "Unknown error occurred";
            default: return status;
        }
    }

    // ============ DIALOG FOR DEMO MARKERS ============
    private void showVenueDialog(Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(marker.getTitle());
        builder.setMessage(
                "Sport: " + marker.getSnippet() +
                        "\n\nWhat would you like to do?"
        );

        // VIEW DETAILS button
        builder.setPositiveButton("Book", (dialog, which) -> {
            Intent intent = new Intent(MapActivity.this, VenueDetailActivity.class);
            intent.putExtra("venueName", marker.getTitle());
            intent.putExtra("sport", marker.getSnippet());
            intent.putExtra("address", marker.getSnippet());
            startActivity(intent);
        });

        // NAVIGATE button
        builder.setNeutralButton("Navigate", (dialog, which) -> {
            LatLng position = marker.getPosition();
            if (position != null && currentLatLng != null) {
                Place place = Place.builder()
                        .setName(marker.getTitle())
                        .setAddress(marker.getSnippet())
                        .setLatLng(position)
                        .build();
                launchBuiltInNavigation(place);
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void addDemoMarkers() {
        LatLng uitm = new LatLng(3.067411043253576, 101.49726815809952);
        googleMap.addMarker(new MarkerOptions()
                .position(uitm)
                .title("UiTM Shah Alam Sports Complex")
                .snippet("Football")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        LatLng panasonic = new LatLng(3.0568666090523804, 101.5479922622229);
        googleMap.addMarker(new MarkerOptions()
                .position(panasonic)
                .title("Kompleks Sukan Panasonic")
                .snippet("Multi Sports")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        LatLng shahAlam = new LatLng(3.086741331783849, 101.51462971657962);
        googleMap.addMarker(new MarkerOptions()
                .position(shahAlam)
                .title("Kompleks Sukan Shah Alam")
                .snippet("Football")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        LatLng setia = new LatLng(3.104788661771676, 101.47807245342857);
        googleMap.addMarker(new MarkerOptions()
                .position(setia)
                .title("Setia Alam Badminton Arena")
                .snippet("Badminton")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        LatLng frenzy = new LatLng(3.0610784570454928, 101.4999115288357);
        googleMap.addMarker(new MarkerOptions()
                .position(frenzy)
                .title("Frenzy Sports Arena")
                .snippet("Indoor Sports")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        LatLng doArena = new LatLng(3.113122919396162, 101.55394981055719);
        googleMap.addMarker(new MarkerOptions()
                .position(doArena)
                .title("DO Arena Space U8")
                .snippet("Football / Futsal")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        LatLng wembley = new LatLng(3.047656504936154, 101.50392375342858);
        googleMap.addMarker(new MarkerOptions()
                .position(wembley)
                .title("Wembley Futsal Arena")
                .snippet("Futsal")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        LatLng badminton = new LatLng(3.065130664192046, 101.52661082459288);
        googleMap.addMarker(new MarkerOptions()
                .position(badminton)
                .title("Shah Alam Badminton Centre")
                .snippet("Badminton")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        LatLng section6 = new LatLng(3.0834116060104346, 101.51041681665004);
        googleMap.addMarker(new MarkerOptions()
                .position(section6)
                .title("Section 6 Sports Complex")
                .snippet("Multi Sports")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uitm, 11.5f));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.isMapReady = true;
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

        addDemoMarkers();

        if (currentLatLng != null) {
            this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
            this.googleMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("Your Location")
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
            Toast.makeText(this, "Your current location", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            showVenueDialog(marker);
            return true;
        }
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
                Toast.makeText(this, "Location permission denied. You can still search manually.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}