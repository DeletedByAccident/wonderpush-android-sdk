package com.wonderpush.sdk.segmentation;

public class GeoCircle {

    public final GeoLocation center;
    public final double radiusMeters;

    public GeoCircle(GeoLocation center, double radiusMeters) {
        this.center = center;
        this.radiusMeters = radiusMeters;
    }

}
