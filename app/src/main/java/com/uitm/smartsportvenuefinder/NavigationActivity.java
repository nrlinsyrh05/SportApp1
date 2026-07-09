package com.uitm.smartsportvenuefinder;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.maps.android.PolyUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback, TextToSpeech.OnInitListener {

    // Map
    private GoogleMap googleMap;
    private SupportMapFragment mapFragment;

    // Navigation data
    private LatLng startLocation;
    private LatLng destination;
    private String destinationName;
    private List<LatLng> routePoints = new ArrayList<>();
    private Polyline routePolyline;
    private Marker destinationMarker;
    private Marker carMarker;

    // UI Components
    private TextView tvDestination, tvHeaderDuration, tvHeaderDistance, tvTime;
    private TextView tvInstructions, tvRoadName, tvStepDistance, tvStepDuration;
    private Button btnStartNavigation, btnEndNavigation, btnVoiceToggle, btnBack;
    private RecyclerView rvSteps;
    private LinearLayout llNavigationControls;

    // Navigation state
    private boolean isNavigating = false;
    private int currentStepIndex = 0;
    private int currentPointIndex = 0;
    private Handler navigationHandler = new Handler(Looper.getMainLooper());
    private BottomSheetBehavior bottomSheetBehavior;

    // Step data
    private List<String> stepInstructions = new ArrayList<>();
    private List<String> stepDistances = new ArrayList<>();
    private List<Integer> stepPointIndices = new ArrayList<>();
    private List<Integer> stepDurations = new ArrayList<>();
    private List<String> stepRoadNames = new ArrayList<>();

    // Text-to-Speech
    private TextToSpeech textToSpeech;
    private boolean isVoiceEnabled = true;
    private boolean isTTSReady = false;

    // Real-time tracking
    private ValueAnimator carAnimator;
    private int totalDurationSeconds = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        try {
            // Get data from intent
            startLocation = getIntent().getParcelableExtra("startLocation");
            destination = getIntent().getParcelableExtra("destination");
            destinationName = getIntent().getStringExtra("destinationName");
            String encodedPolyline = getIntent().getStringExtra("encodedPolyline");

            // Decode polyline
            if (encodedPolyline != null && !encodedPolyline.isEmpty()) {
                routePoints = PolyUtil.decode(encodedPolyline);
            }

            // Initialize Text-to-Speech
            textToSpeech = new TextToSpeech(this, this);

            // Initialize views
            initViews();
            setupBottomSheet();

            // Initialize map
            mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map_navigation);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }

            // Set destination info
            if (destinationName != null) {
                tvDestination.setText(destinationName);
            }

            // Generate steps from route
            generateStepsFromRoute();

            // Calculate and display route info
            calculateRouteInfo();
            updateTime();

            // Setup step adapter
            setupStepAdapter();

            // Button listeners
            btnStartNavigation.setOnClickListener(v -> startNavigation());
            btnEndNavigation.setOnClickListener(v -> endNavigation());
            btnVoiceToggle.setOnClickListener(v -> toggleVoice());
            btnBack.setOnClickListener(v -> finish());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading navigation", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void generateStepsFromRoute() {
        stepInstructions.clear();
        stepDistances.clear();
        stepPointIndices.clear();
        stepDurations.clear();
        stepRoadNames.clear();

        if (routePoints == null || routePoints.size() < 2) {
            stepInstructions.add("No route available");
            stepDistances.add("0 m");
            stepDurations.add(0);
            stepRoadNames.add("");
            return;
        }

        int totalPoints = routePoints.size();
        int numSteps = Math.min(8, Math.max(4, totalPoints / 3));

        String[] directions = {
                "Head southeast", "Continue straight", "Turn left", "Turn right",
                "Keep left", "Keep right", "Merge onto", "Slight left", "Slight right"
        };

        String[] roads = {
                "Persiaran Tun Arshad A", "Jalan Sultan", "Lebuhraya Shah Alam",
                "Persiaran Sultan", "Jalan Raja", "Jalan Kemajuan",
                "Persiaran Kemajuan", "Jalan Merdeka"
        };

        for (int i = 0; i < numSteps; i++) {
            int startIdx = i * (totalPoints / numSteps);
            int endIdx = (i + 1) * (totalPoints / numSteps);
            if (endIdx >= totalPoints) endIdx = totalPoints - 1;

            double distance = 0;
            if (startIdx < endIdx && startIdx < totalPoints && endIdx < totalPoints) {
                LatLng p1 = routePoints.get(startIdx);
                LatLng p2 = routePoints.get(endIdx);
                distance = calculateDistance(p1, p2);
            }

            int durationSec = (int) (distance / 8.33);
            if (durationSec < 10) durationSec = 10;
            if (durationSec > 600) durationSec = 600;

            String road = roads[i % roads.length];
            String direction = directions[i % directions.length];

            String instruction;
            if (i == 0) {
                instruction = "Head southeast";
            } else if (i == numSteps - 1) {
                instruction = "Arrive at " + destinationName;
                road = destinationName;
            } else {
                instruction = direction;
            }

            stepInstructions.add(instruction);
            stepRoadNames.add(road);
            stepPointIndices.add(startIdx);
            stepDurations.add(durationSec);

            if (distance > 1000) {
                stepDistances.add(String.format(Locale.getDefault(), "%.1f km", distance / 1000));
            } else if (distance > 0) {
                stepDistances.add((int) distance + " m");
            } else {
                stepDistances.add("100 m");
            }
        }

        totalDurationSeconds = 0;
        for (int dur : stepDurations) {
            totalDurationSeconds += dur;
        }
        totalDurationSeconds += 120;
    }

    private double calculateDistance(LatLng p1, LatLng p2) {
        double lat1 = p1.latitude;
        double lon1 = p1.longitude;
        double lat2 = p2.latitude;
        double lon2 = p2.longitude;
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000;
    }

    private void initViews() {
        tvDestination = findViewById(R.id.tvDestination);
        tvHeaderDuration = findViewById(R.id.tvHeaderDuration);
        tvHeaderDistance = findViewById(R.id.tvHeaderDistance);
        tvTime = findViewById(R.id.tvTime);
        tvInstructions = findViewById(R.id.tvInstructions);
        tvRoadName = findViewById(R.id.tvRoadName);
        tvStepDistance = findViewById(R.id.tvStepDistance);
        tvStepDuration = findViewById(R.id.tvStepDuration);
        btnStartNavigation = findViewById(R.id.btnStartNavigation);
        btnEndNavigation = findViewById(R.id.btnEndNavigation);
        btnVoiceToggle = findViewById(R.id.btnVoiceToggle);
        btnBack = findViewById(R.id.btnBack);
        rvSteps = findViewById(R.id.rvSteps);
        llNavigationControls = findViewById(R.id.llNavigationControls);

        btnVoiceToggle.setText("🔊");
        btnBack.setText("✕");
    }

    private void setupBottomSheet() {
        View bottomSheet = findViewById(R.id.bottomSheetNavigation);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(180);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void calculateRouteInfo() {
        double totalDistance = 0;
        if (routePoints != null && routePoints.size() > 1) {
            for (int i = 0; i < routePoints.size() - 1; i++) {
                totalDistance += calculateDistance(routePoints.get(i), routePoints.get(i + 1));
            }
        }

        int minutes = totalDurationSeconds / 60;
        int seconds = totalDurationSeconds % 60;

        if (totalDistance > 1000) {
            tvHeaderDistance.setText(String.format(Locale.getDefault(), "%.1f km", totalDistance / 1000));
        } else {
            tvHeaderDistance.setText((int) totalDistance + " m");
        }

        if (minutes > 0) {
            tvHeaderDuration.setText(minutes + " min");
        } else {
            tvHeaderDuration.setText(seconds + " sec");
        }
    }

    private void updateTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        tvTime.setText("· " + timeFormat.format(new Date()) + " GMT+8");
    }

    private void setupStepAdapter() {
        if (stepInstructions != null && !stepInstructions.isEmpty()) {
            StepAdapter adapter = new StepAdapter(stepInstructions, stepDistances);
            rvSteps.setLayoutManager(new LinearLayoutManager(this));
            rvSteps.setAdapter(adapter);
        }
    }

    // ============ MAP ============
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.getUiSettings().setCompassEnabled(true);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        showRouteOnMap();
    }

    private void showRouteOnMap() {
        if (googleMap == null) return;

        try {
            if (startLocation != null) {
                googleMap.addMarker(new MarkerOptions()
                        .position(startLocation)
                        .title("Start")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }

            if (destination != null) {
                destinationMarker = googleMap.addMarker(new MarkerOptions()
                        .position(destination)
                        .title(destinationName != null ? destinationName : "Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }

            if (routePoints != null && !routePoints.isEmpty()) {
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(routePoints)
                        .width(12)
                        .color(0xFF2196F3)
                        .geodesic(true);

                routePolyline = googleMap.addPolyline(polylineOptions);

                // 🚗 CAR MARKER - Arrow icon for moving
                if (startLocation != null) {
                    carMarker = googleMap.addMarker(new MarkerOptions()
                            .position(startLocation)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .anchor(0.5f, 0.5f)
                            .rotation(45));
                }

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng point : routePoints) {
                    builder.include(point);
                }
                LatLngBounds bounds = builder.build();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============ NAVIGATION ============
    private void startNavigation() {
        if (stepInstructions == null || stepInstructions.isEmpty()) {
            Toast.makeText(this, "No navigation steps available", Toast.LENGTH_SHORT).show();
            return;
        }

        isNavigating = true;
        currentStepIndex = 0;
        currentPointIndex = 0;
        btnStartNavigation.setVisibility(View.GONE);
        btnEndNavigation.setVisibility(View.VISIBLE);
        llNavigationControls.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        speak("Starting navigation to " + destinationName);

        navigationHandler.postDelayed(() -> showStep(currentStepIndex), 1500);
    }

    private void showStep(int index) {
        if (stepInstructions == null || index >= stepInstructions.size()) {
            String arrivalMessage = "You have arrived at your destination";
            tvInstructions.setText(arrivalMessage);
            tvRoadName.setText(destinationName);
            tvStepDistance.setText("");
            tvStepDuration.setText("");
            speak(arrivalMessage + " " + destinationName);
            isNavigating = false;
            return;
        }

        String instruction = stepInstructions.get(index);
        String road = stepRoadNames.get(index);
        String distance = stepDistances.get(index);

        tvInstructions.setText(instruction);
        tvRoadName.setText(road);
        tvStepDistance.setText(distance);

        if (index < stepDurations.size()) {
            int secs = stepDurations.get(index);
            if (secs > 60) {
                tvStepDuration.setText((secs / 60) + " min");
            } else {
                tvStepDuration.setText(secs + " sec");
            }
        }

        // Speak instruction
        String speakText = instruction + " on " + road + ", " + distance;
        speak(speakText);

        if (rvSteps.getAdapter() != null) {
            StepAdapter adapter = (StepAdapter) rvSteps.getAdapter();
            adapter.setCurrentStep(index);
            rvSteps.smoothScrollToPosition(index);
        }

        // Animate car to step position
        if (routePoints != null && !routePoints.isEmpty() && index < stepPointIndices.size()) {
            int targetIndex = stepPointIndices.get(index);
            if (targetIndex < routePoints.size()) {
                animateCarToPosition(targetIndex);
            }
        }

        // Auto-advance with realistic timing
        int delay = 3000;
        if (index < stepDurations.size()) {
            delay = Math.min(stepDurations.get(index) * 1000, 15000);
            if (delay < 3000) delay = 3000;
        }

        navigationHandler.postDelayed(() -> {
            if (isNavigating) {
                currentStepIndex++;
                showStep(currentStepIndex);
            }
        }, delay);
    }

    private void animateCarToPosition(int targetIndex) {
        if (carMarker == null || routePoints == null || targetIndex >= routePoints.size()) return;

        int endIndex = Math.min(targetIndex, routePoints.size() - 1);

        carAnimator = ValueAnimator.ofFloat(0, 1);
        carAnimator.setDuration(2000);
        carAnimator.setInterpolator(new LinearInterpolator());
        carAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            if (carMarker != null && currentPointIndex < routePoints.size() - 1) {
                int nextPoint = Math.min(currentPointIndex + 1, routePoints.size() - 1);
                LatLng start = routePoints.get(currentPointIndex);
                LatLng end = routePoints.get(nextPoint);

                double lat = start.latitude + (end.latitude - start.latitude) * fraction;
                double lng = start.longitude + (end.longitude - start.longitude) * fraction;

                carMarker.setPosition(new LatLng(lat, lng));

                // Calculate rotation angle
                double angle = Math.toDegrees(Math.atan2(
                        end.longitude - start.longitude,
                        end.latitude - start.latitude
                ));
                carMarker.setRotation((float) angle);

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 17f));
            }
        });
        carAnimator.start();

        currentPointIndex = endIndex;
    }

    // ============ TEXT-TO-SPEECH ============
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            isTTSReady = true;
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                isVoiceEnabled = false;
                btnVoiceToggle.setText("🔇");
            } else {
                textToSpeech.setSpeechRate(1.0f);
                textToSpeech.setPitch(1.0f);
                btnVoiceToggle.setText("🔊");
            }
        } else {
            isVoiceEnabled = false;
            btnVoiceToggle.setText("🔇");
        }
    }

    private void speak(String text) {
        if (isVoiceEnabled && isTTSReady && textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void toggleVoice() {
        isVoiceEnabled = !isVoiceEnabled;
        if (isVoiceEnabled) {
            btnVoiceToggle.setText("🔊");
            Toast.makeText(this, "Voice ON", Toast.LENGTH_SHORT).show();
            if (isNavigating) {
                speak("Voice guidance resumed");
            }
        } else {
            btnVoiceToggle.setText("🔇");
            Toast.makeText(this, "Voice OFF", Toast.LENGTH_SHORT).show();
            if (textToSpeech != null) {
                textToSpeech.stop();
            }
        }
    }

    private void endNavigation() {
        isNavigating = false;
        navigationHandler.removeCallbacksAndMessages(null);
        if (carAnimator != null) {
            carAnimator.cancel();
        }
        btnStartNavigation.setVisibility(View.VISIBLE);
        btnEndNavigation.setVisibility(View.GONE);
        llNavigationControls.setVisibility(View.GONE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        if (textToSpeech != null) {
            textToSpeech.stop();
        }

        speak("Navigation ended");
        Toast.makeText(this, "Navigation ended", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationHandler.removeCallbacksAndMessages(null);
        if (carAnimator != null) {
            carAnimator.cancel();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}