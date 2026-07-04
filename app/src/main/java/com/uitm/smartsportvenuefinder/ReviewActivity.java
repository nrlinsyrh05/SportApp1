package com.uitm.smartsportvenuefinder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ReviewActivity extends AppCompatActivity {

    EditText etComment;
    RatingBar ratingBar;
    Button btnSubmitReview;
    DatabaseReference mDatabase;
    String venueId, venueName, userId, userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        venueId = getIntent().getStringExtra("venueId");
        venueName = getIntent().getStringExtra("venueName");

        etComment = (EditText) findViewById(R.id.etComment);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        btnSubmitReview = (Button) findViewById(R.id.btnSubmitReview);

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) userName = user.name;
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });

        btnSubmitReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String comment = etComment.getText().toString().trim();
                int rating = (int) ratingBar.getRating();

                if (comment.isEmpty()) {
                    Toast.makeText(ReviewActivity.this, "Please write a comment", Toast.LENGTH_SHORT).show();
                    return;
                }

                String reviewId = mDatabase.child("reviews").push().getKey();
                Review review = new Review(reviewId, venueId, userId, userName, rating, comment);
                mDatabase.child("reviews").child(reviewId).setValue(review)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(ReviewActivity.this, "Review submitted!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(ReviewActivity.this, "Failed to submit review", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}