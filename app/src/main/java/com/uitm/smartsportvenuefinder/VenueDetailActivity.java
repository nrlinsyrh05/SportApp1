package com.uitm.smartsportvenuefinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class VenueDetailActivity extends AppCompatActivity {
    private TextView txtVenueName;
    private TextView txtSport;
    private TextView txtAddress;
    private TextView txtPhone;
    private TextView txtWebsite;
    private TextView txtOperatingHour;

    private Button btnOpenMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_venue_detail);
        txtVenueName = findViewById(R.id.txtVenueName);
        txtSport = findViewById(R.id.txtSport);
        txtAddress = findViewById(R.id.txtAddress);
        txtPhone = findViewById(R.id.txtPhone);
        txtWebsite = findViewById(R.id.txtWebsite);
        txtOperatingHour = findViewById(R.id.txtOperatingHour);

        btnOpenMap = findViewById(R.id.btnOpenMap);

        String venueName = getIntent().getStringExtra("venueName");
        String sport = getIntent().getStringExtra("sport");

        txtVenueName.setText(venueName);
        txtSport.setText("Sport : " + sport);
        loadVenueDetails(venueName);
        btnOpenMap.setOnClickListener(v -> {

            String uri = "geo:0,0?q=" + venueName + ", Shah Alam";

            android.net.Uri gmmIntentUri = android.net.Uri.parse(uri);

            android.content.Intent mapIntent =
                    new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            gmmIntentUri);

            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {

                startActivity(mapIntent);

            }

        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        }
        private void loadVenueDetails(String venueName){
            if (venueName.equals("Section 6 Sports Complex")) {

                txtAddress.setText("Address : Persiaran Sultan, Seksyen 6, 40000 Shah Alam");

                txtPhone.setText("Phone : 03-5510 1234");

                txtWebsite.setText("Website : https://mbsa.gov.my");

                txtOperatingHour.setText("Operating Hours : 8.00 AM - 10.00 PM");

            }

        }
}
