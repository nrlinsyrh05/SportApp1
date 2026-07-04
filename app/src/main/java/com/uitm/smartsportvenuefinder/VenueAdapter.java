package com.uitm.smartsportvenuefinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class VenueAdapter extends BaseAdapter {

    Context context;
    ArrayList<Venue> venueList;

    public VenueAdapter(Context context, ArrayList<Venue> venueList) {
        this.context = context;
        this.venueList = venueList;
    }

    @Override
    public int getCount() { return venueList.size(); }

    @Override
    public Object getItem(int position) { return venueList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_venue, parent, false);
        }
        Venue venue = venueList.get(position);

        TextView tvVenueName = (TextView) convertView.findViewById(R.id.tvVenueName);
        TextView tvSportType = (TextView) convertView.findViewById(R.id.tvSportType);
        TextView tvAddress = (TextView) convertView.findViewById(R.id.tvAddress);
        TextView tvPrice = (TextView) convertView.findViewById(R.id.tvPrice);

        tvVenueName.setText(venue.venueName);
        tvSportType.setText(venue.sportType);
        tvAddress.setText(venue.address);
        tvPrice.setText(venue.price);

        return convertView;
    }
}
