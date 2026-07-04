package com.uitm.smartsportvenuefinder;

public class Booking {
    public String bookingId;
    public String userId;
    public String venueId;
    public String venueName;
    public String venueDescription;
    public String bookingDate;
    public String bookingTime;
    public String status;
    public long timestamp;
    public Integer pax;
    public String userName;
    public String userEmail;
    public double latitude;
    public double longitude;
    public String venueAddress;
    public String severity;
    public String imageUrl;

    public Booking() {
        // Default constructor required for Firebase
    }

    public Booking(String bookingId, String userId, String venueId, String venueName,
                   String bookingDate, String bookingTime, String status, Integer pax) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.venueId = venueId;
        this.venueName = venueName;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.status = status;
        this.pax = pax != null ? pax : 1;
        this.timestamp = System.currentTimeMillis();
        this.severity = "Casual";
    }

    // Getters and Setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    public String getVenueDescription() { return venueDescription; }
    public void setVenueDescription(String venueDescription) { this.venueDescription = venueDescription; }
    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }
    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public Integer getPax() { return pax != null ? pax : 1; }
    public void setPax(Integer pax) { this.pax = pax; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getVenueAddress() { return venueAddress; }
    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}