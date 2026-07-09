package com.uitm.smartsportvenuefinder;

public class Venue {
    public String venueId;
    public String venueName;
    public String sportType;
    public String address;
    public String phone;
    public String website;
    public String operatingHour;
    public String price;
    public String imageUrl;
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