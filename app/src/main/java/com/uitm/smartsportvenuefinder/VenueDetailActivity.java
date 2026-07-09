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

        // Get data from intent
        String venueName = getIntent().getStringExtra("venueName");
        String sport = getIntent().getStringExtra("sport");
        String address = getIntent().getStringExtra("address");

        if (venueName != null) {
            txtVenueName.setText(venueName);
            txtSport.setText("Sport: " + (sport != null ? sport : "Multi Sports"));
            txtAddress.setText("Address: " + (address != null ? address : "Shah Alam, Selangor"));
            loadVenueDetails(venueName);
        } else {
            txtVenueName.setText("No venue selected");
            txtSport.setText("Sport: N/A");
            txtAddress.setText("Address: N/A");
        }

        // Book Now button
        btnBookNow.setOnClickListener(v -> {
            Intent intent = new Intent(VenueDetailActivity.this, BookingActivity.class);
            intent.putExtra("venueName", venueName);
            intent.putExtra("venueAddress", address);
            startActivity(intent);
        });

        // Open Map button
        btnOpenMap.setOnClickListener(v -> {
            if (venueName != null) {
                String uri = "geo:0,0?q=" + venueName + ", Shah Alam";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No venue selected", Toast.LENGTH_SHORT).show();
            }
        });

        // Call button
        btnCall.setOnClickListener(v -> {
            String phone = getPhoneNumber(venueName);
            if (phone != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        // WhatsApp button
        btnWhatsApp.setOnClickListener(v -> {
            String phone = getPhoneNumber(venueName);
            if (phone != null) {
                String url = "https://wa.me/" + phone.replaceAll("[^0-9]", "");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Email button
        btnEmail.setOnClickListener(v -> {
            String email = getEmail(venueName);
            if (email != null) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + email));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about " + venueName);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Email not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadVenueDetails(String venueName) {
        String phone = "03-5510 1234";
        String website = "https://mbsa.gov.my";
        String hours = "8:00 AM - 10:00 PM";

        if (venueName != null) {
            switch (venueName) {
                case "UiTM Shah Alam Sports Complex":
                    phone = "03-5544 2000";
                    website = "https://uitm.edu.my";
                    hours = "7:00 AM - 10:00 PM";
                    break;
                case "Kompleks Sukan Panasonic":
                    phone = "03-5523 4567";
                    website = "https://panasonicsports.com";
                    hours = "7:00 AM - 11:00 PM";
                    break;
                case "Kompleks Sukan Shah Alam":
                    phone = "03-5521 7890";
                    website = "https://shahalamcomplex.com";
                    hours = "6:00 AM - 10:00 PM";
                    break;
                case "Setia Alam Badminton Arena":
                    phone = "03-3345 6789";
                    website = "https://setiabadminton.com";
                    hours = "8:00 AM - 11:00 PM";
                    break;
                case "Frenzy Sports Arena":
                    phone = "03-5567 8901";
                    website = "https://frenzysports.com";
                    hours = "9:00 AM - 12:00 AM";
                    break;
                case "Section 6 Sports Complex":
                    phone = "03-5510 1234";
                    website = "https://mbsa.gov.my";
                    hours = "8:00 AM - 10:00 PM";
                    break;
                case "Shah Alam Badminton Centre":
                    phone = "03-5522 6789";
                    website = "https://sabadminton.com";
                    hours = "7:00 AM - 11:00 PM";
                    break;
                default:
                    phone = "03-5510 1234";
                    website = "https://mbsa.gov.my";
                    hours = "8:00 AM - 10:00 PM";
                    break;
            }
        }

        txtPhone.setText("Phone: " + phone);
        txtWebsite.setText("Website: " + website);
        txtOperatingHour.setText("Operating Hours: " + hours);
    }

    private String getPhoneNumber(String venueName) {
        if (venueName == null) return null;
        switch (venueName) {
            case "UiTM Shah Alam Sports Complex": return "03-5544 2000";
            case "Kompleks Sukan Panasonic": return "03-5523 4567";
            case "Kompleks Sukan Shah Alam": return "03-5521 7890";
            case "Setia Alam Badminton Arena": return "03-3345 6789";
            case "Frenzy Sports Arena": return "03-5567 8901";
            case "Section 6 Sports Complex": return "03-5510 1234";
            case "Shah Alam Badminton Centre": return "03-5522 6789";
            default: return "03-5510 1234";
        }
    }

    private String getEmail(String venueName) {
        if (venueName == null) return null;
        switch (venueName) {
            case "UiTM Shah Alam Sports Complex": return "sports@uitm.edu.my";
            case "Kompleks Sukan Panasonic": return "info@panasonicsports.com";
            case "Setia Alam Badminton Arena": return "info@setiabadminton.com";
            default: return "info@shahalam.gov.my";
        }
    }
}