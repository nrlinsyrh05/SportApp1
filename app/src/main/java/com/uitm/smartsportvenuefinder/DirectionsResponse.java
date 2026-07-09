package com.uitm.smartsportvenuefinder;

import java.util.List;

public class DirectionsResponse {
    public String status;
    public List<Route> routes;

    public class Route {
        public OverviewPolyline overview_polyline;
        public List<Leg> legs;
        public String summary;

        public class OverviewPolyline {
            public String points;
        }

        public class Leg {
            public Distance distance;
            public Duration duration;
            public String start_address;
            public String end_address;
            public List<Step> steps;

            public class Distance {
                public String text;
                public int value;
            }

            public class Duration {
                public String text;
                public int value;
            }

            public class Step {
                public String travel_mode;
                public String html_instructions;
                public Distance distance;
                public Duration duration;
                public Polyline polyline;
                public StartLocation start_location;
                public EndLocation end_location;
                public String maneuver;

                public class Polyline {
                    public String points;
                }

                public class StartLocation {
                    public double lat;
                    public double lng;
                }

                public class EndLocation {
                    public double lat;
                    public double lng;
                }
            }
        }
    }

    public boolean isOk() {
        return status != null && status.equals("OK");
    }
}