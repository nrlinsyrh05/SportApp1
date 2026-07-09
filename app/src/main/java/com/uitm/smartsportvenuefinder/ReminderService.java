package com.uitm.smartsportvenuefinder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
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

    // ✅ Constructor required for Worker
    public ReminderService(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Check for upcoming bookings
        checkAllBookingsForReminders();
        return Result.success();
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
                            Booking booking = data.getValue(Booking.class);
                            if (booking != null && !booking.isReminderSent()) {
                                if (shouldSendReminder(booking)) {
                                    sendReminderNotification(booking);
                                    // Mark as sent
                                    data.getRef().child("reminderSent").setValue(true);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private boolean shouldSendReminder(Booking booking) {
        try {
            // Parse booking date and time
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            Date bookingDateTime = sdf.parse(booking.getBookingDate() + " " + booking.getBookingTime());

            if (bookingDateTime == null) return false;

            long timeDiff = bookingDateTime.getTime() - System.currentTimeMillis();

            // Send reminder if within 1 hour before booking (3600000 ms)
            return timeDiff > 0 && timeDiff <= 3600000;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendReminderNotification(Booking booking) {
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

        String title = "Upcoming Booking";
        String message = "Don't forget your booking at " + booking.getVenueName();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Venue: " + booking.getVenueName() +
                                "\nDate: " + booking.getBookingDate() +
                                "\nTime: " + booking.getBookingTime() +
                                "\n\nTap to view details"))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{1000, 1000, 1000, 1000})
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1001, builder.build());
    }
}