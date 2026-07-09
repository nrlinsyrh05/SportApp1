package com.uitm.smartsportvenuefinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class VenueDetailActivity extends AppCompatActivity {

    private TextView txtVenueName, txtSport, txtAddress, txtPhone, txtWebsite, txtOperatingHour;
    private Button btnOpenMap, btnCall, btnWhatsApp, btnEmail, btnBookNow;
    private String venueName, sport, venueAddress, venuePhone, venueWebsite, venueHours;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_detail);

        // Initialize views
        txtVenueName = findViewById(R.id.txtVenueName);
        txtSport = findViewById(R.id.txtSport);
        txtAddress = findViewById(R.id.txtAddress);
        txtPhone = findViewById(R.id.txtPhone);
        txtWebsite = findViewById(R.id.txtWebsite);
        txtOperatingHour = findViewById(R.id.txtOperatingHour);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnCall = findViewById(R.id.btnCall);
        btnWhatsApp = findViewById(R.id.btnWhatsApp);
        btnEmail = findViewById(R.id.btnEmail);
        btnBookNow = findViewById(R.id.btnBookNow);

        // Get venue data from intent
        venueName = getIntent().getStringExtra("venueName");
        sport = getIntent().getStringExtra("sport");
        String lat = getIntent().getStringExtra("latitude");
        String lng = getIntent().getStringExtra("longitude");

        // Get venue details
        loadVenueDetails(venueName);

        // Set venue name and sport
        if (venueName != null) {
            txtVenueName.setText(venueName);
            txtSport.setText("Sport: " + (sport != null ? sport : "Multi Sports"));
        }

        // ---------- OPEN MAP ----------
        btnOpenMap.setOnClickListener(v -> {
            String uri = "geo:0,0?q=" + venueName + ", Shah Alam";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
            }
        });

        // ---------- CALL ----------
        btnCall.setOnClickListener(v -> {
            String phone = getPhoneNumber(venueName);
            if (phone != null && !phone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        // ---------- WHATSAPP ----------
        btnWhatsApp.setOnClickListener(v -> {
            String phone = getPhoneNumber(venueName);
            if (phone != null && !phone.isEmpty()) {
                String url = "https://wa.me/" + phone.replaceAll("[^0-9]", "");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        // ---------- EMAIL ----------
        btnEmail.setOnClickListener(v -> {
            String email = getEmail(venueName);
            if (email != null && !email.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + email));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about " + venueName);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Email not available", Toast.LENGTH_SHORT).show();
            }
        });

        // ---------- BOOK NOW ----------
        btnBookNow.setOnClickListener(v -> {
            Intent intent = new Intent(VenueDetailActivity.this, BookingActivity.class);
            intent.putExtra("venueName", venueName);
            intent.putExtra("venueAddress", venueAddress != null ? venueAddress : "");
            intent.putExtra("venueId", venueName != null ? venueName.replaceAll("\\s+", "") : "");

            // If we have lat/lng from intent
            if (lat != null && lng != null) {
                try {
                    intent.putExtra("latitude", Double.parseDouble(lat));
                    intent.putExtra("longitude", Double.parseDouble(lng));
                } catch (NumberFormatException e) {
                    // Use default coordinates if parsing fails
                    intent.putExtra("latitude", 3.1390);
                    intent.putExtra("longitude", 101.6869);
                }
            } else {
                // Default coordinates
                intent.putExtra("latitude", 3.1390);
                intent.putExtra("longitude", 101.6869);
            }

            startActivity(intent);
        });
    }

    private void loadVenueDetails(String venueName) {
        // Default data
        String address = "Shah Alam, Selangor";
        String phone = "03-5510 1234";
        String website = "https://mbsa.gov.my";
        String hours = "8:00 AM - 10:00 PM";

        // Specific venue details
        if (venueName != null) {
            switch (venueName) {
                case "UiTM Shah Alam Sports Complex":
                    address = "UiTM Shah Alam, 40450 Shah Alam";
                    phone = "03-5544 2000";
                    website = "https://uitm.edu.my";
                    hours = "7:00 AM - 10:00 PM";
                    break;
                case "Kompleks Sukan Panasonic":
                    address = "Seksyen 14, 40000 Shah Alam";
                    phone = "03-5523 4567";
                    website = "https://panasonicsports.com";
                    hours = "7:00 AM - 11:00 PM";
                    break;
                case "Kompleks Sukan Shah Alam":
                    address = "Seksyen 13, 40000 Shah Alam";
                    phone = "03-5521 7890";
                    website = "https://shahalamcomplex.com";
                    hours = "6:00 AM - 10:00 PM";
                    break;
                case "Setia Alam Badminton Arena":
                    address = "Setia Alam, 40170 Shah Alam";
                    phone = "03-3345 6789";
                    website = "https://setiabadminton.com";
                    hours = "8:00 AM - 11:00 PM";
                    break;
                case "Frenzy Sports Arena":
                    address = "Seksyen 7, 40000 Shah Alam";
                    phone = "03-5567 8901";
                    website = "https://frenzysports.com";
                    hours = "9:00 AM - 12:00 AM";
                    break;
                case "Section 6 Sports Complex":
                    address = "Persiaran Sultan, Seksyen 6, 40000 Shah Alam";
                    phone = "03-5510 1234";
                    website = "https://mbsa.gov.my";
                    hours = "8:00 AM - 10:00 PM";
                    break;
                case "Shah Alam Badminton Centre":
                    address = "Seksyen 16, 40000 Shah Alam";
                    phone = "03-5522 6789";
                    website = "https://sabadminton.com";
                    hours = "7:00 AM - 11:00 PM";
                    break;
                case "DO Arena Space U8":
                    address = "Bukit Jelutong, 40150 Shah Alam";
                    phone = "03-5567 2345";
                    website = "https://doarena.com";
                    hours = "8:00 AM - 11:00 PM";
                    break;
                case "Wembley Futsal Arena":
                    address = "Seksyen 9, 40000 Shah Alam";
                    phone = "03-5512 3456";
                    website = "https://wembleyfutsal.com";
                    hours = "8:00 AM - 12:00 AM";
                    break;
                case "Digital Sports Arena i-City":
                    address = "i-City, 40000 Shah Alam";
                    phone = "03-5567 8901";
                    website = "https://digitalsports.com";
                    hours = "10:00 AM - 12:00 AM";
                    break;
                case "Empire Sport Arena":
                    address = "Subang Jaya, 47500 Selangor";
                    phone = "03-5632 4567";
                    website = "https://empiresports.com";
                    hours = "8:00 AM - 11:00 PM";
                    break;
                case "Ole Ole Super Bowl":
                    address = "Seksyen 7, 40000 Shah Alam";
                    phone = "03-5567 8901";
                    website = "https://oleolebowl.com";
                    hours = "10:00 AM - 12:00 AM";
                    break;
                case "Radia Arena":
                    address = "Seksyen U8, 40150 Shah Alam";
                    phone = "03-5567 1234";
                    website = "https://radiaarena.com";
                    hours = "8:00 AM - 11:00 PM";
                    break;
                default:
                    address = "Shah Alam, Selangor";
                    phone = "03-5510 1234";
                    website = "https://mbsa.gov.my";
                    hours = "8:00 AM - 10:00 PM";
                    break;
            }
        }

        venueAddress = address;
        venuePhone = phone;
        venueWebsite = website;
        venueHours = hours;

        txtAddress.setText("Address: " + address);
        txtPhone.setText("Phone: " + phone);
        txtWebsite.setText("Website: " + website);
        txtOperatingHour.setText("Operating Hours: " + hours);
    }

    private String getPhoneNumber(String venueName) {
        String phone = "03-5510 1234";
        if (venueName != null) {
            switch (venueName) {
                case "UiTM Shah Alam Sports Complex": phone = "03-5544 2000"; break;
                case "Kompleks Sukan Panasonic": phone = "03-5523 4567"; break;
                case "Kompleks Sukan Shah Alam": phone = "03-5521 7890"; break;
                case "Setia Alam Badminton Arena": phone = "03-3345 6789"; break;
                case "Frenzy Sports Arena": phone = "03-5567 8901"; break;
                case "Section 6 Sports Complex": phone = "03-5510 1234"; break;
                case "Shah Alam Badminton Centre": phone = "03-5522 6789"; break;
                case "DO Arena Space U8": phone = "03-5567 2345"; break;
                case "Wembley Futsal Arena": phone = "03-5512 3456"; break;
                case "Digital Sports Arena i-City": phone = "03-5567 8901"; break;
                case "Empire Sport Arena": phone = "03-5632 4567"; break;
                case "Ole Ole Super Bowl": phone = "03-5567 8901"; break;
                case "Radia Arena": phone = "03-5567 1234"; break;
                default: phone = "03-5510 1234"; break;
            }
        }
        return phone;
    }

    private String getEmail(String venueName) {
        String email = "info@shahalam.gov.my";
        if (venueName != null) {
            switch (venueName) {
                case "UiTM Shah Alam Sports Complex": email = "sports@uitm.edu.my"; break;
                case "Kompleks Sukan Panasonic": email = "info@panasonicsports.com"; break;
                case "Setia Alam Badminton Arena": email = "info@setiabadminton.com"; break;
                case "Section 6 Sports Complex": email = "info@mbsa.gov.my"; break;
                default: email = "info@shahalam.gov.my"; break;
            }
        }
        return email;
    }
}