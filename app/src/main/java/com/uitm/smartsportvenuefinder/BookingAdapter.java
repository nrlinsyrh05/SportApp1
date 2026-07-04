package com.uitm.smartsportvenuefinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends BaseAdapter {

    private Context context;
    private List<Booking> bookingList;
    private DatabaseReference mDatabase;

    public BookingAdapter(Context context, List<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public int getCount() {
        return bookingList != null ? bookingList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return bookingList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
            holder = new ViewHolder();
            holder.tvVenueName = convertView.findViewById(R.id.tvVenueName);
            holder.tvDate = convertView.findViewById(R.id.tvDate);
            holder.tvTime = convertView.findViewById(R.id.tvTime);
            holder.tvPax = convertView.findViewById(R.id.tvPax);
            holder.tvStatus = convertView.findViewById(R.id.tvStatus);
            holder.btnCancel = convertView.findViewById(R.id.btnCancel);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Booking booking = bookingList.get(position);

        // Set venue information
        holder.tvVenueName.setText(booking.venueName != null ? booking.venueName : "Unknown Venue");
        holder.tvDate.setText("Date: " + (booking.bookingDate != null ? booking.bookingDate : "N/A"));
        holder.tvTime.setText("Time: " + (booking.bookingTime != null ? booking.bookingTime : "N/A"));

        // Handle pax
        int pax = booking.getPax();
        holder.tvPax.setText("👤 " + pax + " people");

        // Set status with appropriate color
        String status = booking.status != null ? booking.status : "Pending";
        holder.tvStatus.setText("Status: " + status);

        int statusColor;
        switch (status.toLowerCase()) {
            case "confirmed":
                statusColor = context.getResources().getColor(android.R.color.holo_green_dark);
                break;
            case "cancelled":
                statusColor = context.getResources().getColor(android.R.color.holo_red_dark);
                break;
            case "completed":
                statusColor = context.getResources().getColor(android.R.color.holo_blue_dark);
                break;
            case "pending":
            default:
                statusColor = context.getResources().getColor(android.R.color.holo_orange_dark);
                break;
        }
        holder.tvStatus.setTextColor(statusColor);

        // Show/hide cancel button based on status
        if (status.equalsIgnoreCase("pending")) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> {
                cancelBooking(booking, position);
            });
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }

        return convertView;
    }

    private void cancelBooking(Booking booking, int position) {
        if (booking.bookingId == null) {
            Toast.makeText(context, "Error: Booking ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("bookings").child(booking.bookingId)
                .child("status")
                .setValue("Cancelled")
                .addOnSuccessListener(aVoid -> {
                    booking.status = "Cancelled";
                    notifyDataSetChanged();
                    Toast.makeText(context, "Booking cancelled", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error cancelling: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    static class ViewHolder {
        TextView tvVenueName;
        TextView tvDate;
        TextView tvTime;
        TextView tvPax;
        TextView tvStatus;
        Button btnCancel;
    }
}