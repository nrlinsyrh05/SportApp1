package com.uitm.smartsportvenuefinder;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

public class BookingAdapter extends BaseAdapter {

    private Context context;
    private List<Booking> bookingList;
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onEdit(Booking booking);
        void onDelete(Booking booking);
    }

    public BookingAdapter(Context context, List<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    public void setOnBookingActionListener(OnBookingActionListener listener) {
        this.listener = listener;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_booking_history, parent, false);
            holder = new ViewHolder();
            holder.tvVenueName = convertView.findViewById(R.id.tvVenueName);
            holder.tvVenueAddress = convertView.findViewById(R.id.tvVenueAddress);
            holder.tvDate = convertView.findViewById(R.id.tvDate);
            holder.tvTime = convertView.findViewById(R.id.tvTime);
            holder.tvPax = convertView.findViewById(R.id.tvPax);
            holder.tvStatus = convertView.findViewById(R.id.tvStatus);
            holder.btnEdit = convertView.findViewById(R.id.btnEdit);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Booking booking = bookingList.get(position);

        // Set data
        holder.tvVenueName.setText(booking.venueName != null ? booking.venueName : "Unknown Venue");
        holder.tvVenueAddress.setText(booking.venueAddress != null ? booking.venueAddress : "");
        holder.tvDate.setText("📅 " + booking.bookingDate);
        holder.tvTime.setText("🕐 " + booking.bookingTime);
        holder.tvPax.setText("👤 " + booking.pax + " people");

        // Set status color
        String status = booking.status != null ? booking.status : "Pending";
        holder.tvStatus.setText("Status: " + status);

        int color;
        switch (status.toLowerCase()) {
            case "confirmed":
                color = context.getResources().getColor(android.R.color.holo_green_dark);
                break;
            case "cancelled":
                color = context.getResources().getColor(android.R.color.holo_red_dark);
                break;
            default:
                color = context.getResources().getColor(android.R.color.holo_orange_dark);
                break;
        }
        holder.tvStatus.setTextColor(color);

        // Show edit/delete only for pending bookings
        if (status.equalsIgnoreCase("Pending")) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            // Edit button - Open EditBookingActivity
            holder.btnEdit.setOnClickListener(v -> {
                // Open EditBookingActivity with booking data
                Intent intent = new Intent(context, EditBookingActivity.class);
                intent.putExtra("bookingId", booking.bookingId);
                intent.putExtra("venueName", booking.venueName);
                intent.putExtra("venueAddress", booking.venueAddress);
                intent.putExtra("bookingDate", booking.bookingDate);
                intent.putExtra("bookingTime", booking.bookingTime);
                intent.putExtra("pax", booking.pax);
                intent.putExtra("latitude", booking.latitude);
                intent.putExtra("longitude", booking.longitude);
                intent.putExtra("placeId", booking.venueId);
                context.startActivity(intent);
            });

            // Delete button
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(booking);
                }
            });
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView tvVenueName, tvVenueAddress, tvDate, tvTime, tvPax, tvStatus;
        Button btnEdit, btnDelete;
    }
}