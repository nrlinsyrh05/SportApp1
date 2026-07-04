package com.uitm.smartsportvenuefinder;

public class Venue {
    public String venueId, venueName, sportType, address, price, imageUrl;
    public double latitude, longitude;

    public Venue() {}

    public Venue(String venueId, String venueName, String sportType, String address, double latitude, double longitude, String price, String imageUrl) {
        this.venueId = venueId;
        this.venueName = venueName;
        this.sportType = sportType;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.price = price;
        this.imageUrl = imageUrl;
    }
}