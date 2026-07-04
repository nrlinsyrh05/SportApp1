package com.uitm.smartsportvenuefinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.libraries.places.api.model.Place;
import java.util.List;

public class VenueSearchAdapter extends RecyclerView.Adapter<VenueSearchAdapter.ViewHolder> {

    private Context context;
    private List<Place> placeList;
    private OnVenueClickListener listener;

    public interface OnVenueClickListener {
        void onVenueClick(Place place);
    }

    public VenueSearchAdapter(Context context, List<Place> placeList, OnVenueClickListener listener) {
        this.context = context;
        this.placeList = placeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_venue_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Place place = placeList.get(position);

        String name = place.getName() != null ? place.getName() : "Unknown Venue";
        String address = place.getAddress() != null ? place.getAddress() : "No address";

        // FIXED: Properly handle Place.Type list
        String type = "Sports Venue";
        if (place.getTypes() != null && !place.getTypes().isEmpty()) {
            // Convert Place.Type to String
            Place.Type placeType = place.getTypes().get(0);
            type = placeType.toString();
            // Clean up the type name
            if (type.contains("_")) {
                String[] parts = type.split("_");
                StringBuilder sb = new StringBuilder();
                for (String part : parts) {
                    sb.append(part.substring(0, 1).toUpperCase())
                            .append(part.substring(1).toLowerCase())
                            .append(" ");
                }
                type = sb.toString().trim();
            }
        }

        holder.tvVenueName.setText(name);
        holder.tvVenueAddress.setText("📍 " + address);
        holder.tvVenueType.setText("🏸 " + type);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVenueClick(place);
            }
        });
    }

    @Override
    public int getItemCount() {
        return placeList != null ? placeList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVenueName, tvVenueAddress, tvVenueType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVenueName = itemView.findViewById(R.id.tvVenueName);
            tvVenueAddress = itemView.findViewById(R.id.tvVenueAddress);
            tvVenueType = itemView.findViewById(R.id.tvVenueType);
        }
    }
}