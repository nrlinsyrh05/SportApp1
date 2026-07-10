package com.uitm.smartsportvenuefinder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderService extends Worker {

    private static final String CHANNEL_ID = "reminder_channel";
    private static final String CHANNEL_NAME = "Booking Reminders";
    private static final String TAG = "ReminderService";

    public ReminderService(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "ReminderService doWork started");

            // Check if user is logged in
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Log.d(TAG, "User not logged in, skipping reminder check");
                return Result.success();
            }

            // Check for upcoming bookings
            checkAllBookingsForReminders();

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in doWork: " + e.getMessage());
            return Result.failure();
        }
    }

    private void checkAllBookingsForReminders() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) return;

        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        bookingsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            try {
                                Booking booking = data.getValue(Booking.class);
                                if (booking != null) {
                                    // Check if reminder should be sent
                                    // Handle case where reminderSent might not exist
                                    boolean reminderSent = false;
                                    if (data.hasChild("reminderSent")) {
                                        Boolean sent = data.child("reminderSent").getValue(Boolean.class);
                                        reminderSent = sent != null && sent;
                                    }

                                    if (!reminderSent && shouldSendReminder(booking)) {
                                        sendReminderNotification(booking);
                                        // Mark reminder as sent
                                        data.getRef().child("reminderSent").setValue(true);
                                        Log.d(TAG, "Reminder sent for booking: " + booking.getVenueName());
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing booking: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: " + error.getMessage());
                    }
                });
    }

    private boolean shouldSendReminder(Booking booking) {
        try {
            // Check if booking is still pending
            if (!"Pending".equals(booking.getStatus())) {
                return false;
            }

            // Parse booking date and time
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            String dateTimeStr = booking.getBookingDate() + " " + booking.getBookingTime();
            Date bookingDateTime = sdf.parse(dateTimeStr);

            if (bookingDateTime == null) return false;

            long timeDiff = bookingDateTime.getTime() - System.currentTimeMillis();

            // Send reminder if within 1 hour before booking
            return timeDiff > 0 && timeDiff <= 3600000;

        } catch (Exception e) {
            Log.e(TAG, "Error checking reminder: " + e.getMessage());
            return false;
        }
    }

    private void sendReminderNotification(Booking booking) {
        try {
            Context context = getApplicationContext();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Booking reminder notifications");
                channel.enableVibration(true);
                channel.setShowBadge(true);

                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.createNotificationChannel(channel);
            }

            Intent intent = new Intent(context, BookingHistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            String title = "Upcoming Booking Reminder";
            String message = "Don't forget your booking at " + booking.getVenueName();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Venue: " + booking.getVenueName() +
                                    "\nDate: " + booking.getBookingDate() +
                                    "\nTime: " + booking.getBookingTime() +
                                    "\n\nYour booking is in 1 hour! Tap to view details. "))
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setVibrate(new long[]{1000, 1000, 1000, 1000})
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify((int) System.currentTimeMillis(), builder.build());

        } catch (Exception e) {
            Log.e(TAG, "Error sending notification: " + e.getMessage());
        }
    }
}