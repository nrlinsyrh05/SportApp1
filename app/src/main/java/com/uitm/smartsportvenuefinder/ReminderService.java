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
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

            // Initialize Firebase if needed
            FirebaseApp.initializeApp(getApplicationContext());

            // Check if user is logged in
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.d(TAG, "User not logged in, skipping reminder check");
                return Result.success();
            }

            // Check for upcoming bookings
            checkAllBookingsForReminders(currentUser.getUid());

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in doWork: " + e.getMessage());
            return Result.failure();
        }
    }

    private void checkAllBookingsForReminders(String userId) {
        try {
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
                                        if (shouldSendReminder(booking)) {
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
        } catch (Exception e) {
            Log.e(TAG, "Error checking bookings: " + e.getMessage());
        }
    }

    private boolean shouldSendReminder(Booking booking) {
        try {
            // Check if reminder already sent
            if (booking.isReminderSent()) {
                return false;
            }

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

            // Send reminder if within 1 hour before booking (3600000 ms)
            // and not already passed
            return timeDiff > 0 && timeDiff <= 3600000;

        } catch (Exception e) {
            Log.e(TAG, "Error checking reminder: " + e.getMessage());
            return false;
        }
    }

    private void sendReminderNotification(Booking booking) {
        try {
            Context context = getApplicationContext();

            // Create notification channel for Android O and above
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

            // Create intent to open BookingHistory when notification is tapped
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
            String message = "Booking in 1 Hour at " + booking.getVenueName();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Venue: " + booking.getVenueName() +
                                    "\nDate: " + booking.getBookingDate() +
                                    "\nTime: " + booking.getBookingTime() +
                                    "\n\nYour booking is in 1 hour. Tap to view details"))
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