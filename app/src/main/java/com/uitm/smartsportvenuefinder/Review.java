package com.uitm.smartsportvenuefinder;

public class Review {
    public String reviewId, venueId, userId, userName, comment;
    public int rating;

    public Review() {}

    public Review(String reviewId, String venueId, String userId, String userName, int rating, String comment) {
        this.reviewId = reviewId;
        this.venueId = venueId;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
    }
}