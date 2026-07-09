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
            holder.btnEdit = convertView.findViewById(R.id.btnEdit);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get booking at position
        Booking booking = bookingList.get(position);

        // Set data
        holder.tvVenueName.setText(booking.venueName != null ? booking.venueName : "Unknown Venue");
        holder.tvVenueAddress.setText(booking.venueAddress != null ? booking.venueAddress : "");
        holder.tvDate.setText("📅 " + booking.bookingDate);
        holder.tvTime.setText("🕐 " + booking.bookingTime);
        holder.tvPax.setText("👤 " + booking.pax + " people");

        // Create final copies for lambda expressions
        final Booking currentBooking = booking;

        // Edit button
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditBookingActivity.class);
            intent.putExtra("bookingId", currentBooking.bookingId);
            intent.putExtra("venueName", currentBooking.venueName);
            intent.putExtra("venueAddress", currentBooking.venueAddress);
            intent.putExtra("bookingDate", currentBooking.bookingDate);
            intent.putExtra("bookingTime", currentBooking.bookingTime);
            intent.putExtra("pax", currentBooking.pax);
            intent.putExtra("latitude", currentBooking.latitude);
            intent.putExtra("longitude", currentBooking.longitude);
            intent.putExtra("placeId", currentBooking.venueId);
            context.startActivity(intent);
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(currentBooking);
            }
        });

        return convertView;
    }

    static class ViewHolder {
        TextView tvVenueName, tvVenueAddress, tvDate, tvTime, tvPax;
        Button btnEdit, btnDelete;
    }
}