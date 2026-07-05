package com.uitm.smartsportvenuefinder;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ContactAdminActivity extends AppCompatActivity {

    Button btnCall, btnWhatsapp, btnEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_admin);

        btnCall = findViewById(R.id.btnCall);
        btnWhatsapp = findViewById(R.id.btnWhatsapp);
        btnEmail = findViewById(R.id.btnEmail);

        // CALL
        btnCall.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:01133602769"));
            startActivity(intent);

        });

        // WHATSAPP
        btnWhatsapp.setOnClickListener(v -> {

            String phone = "601133602769";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/" + phone));

            startActivity(intent);

        });

        // EMAIL
        btnEmail.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:admin@sportfinder.com"));

            intent.putExtra(Intent.EXTRA_SUBJECT,
                    "Sport Venue Finder Support");

            intent.putExtra(Intent.EXTRA_TEXT,
                    "Hello Admin,");

            startActivity(intent);

        });

    }
}