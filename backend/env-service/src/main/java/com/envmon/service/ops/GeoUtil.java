package com.envmon.service.ops;

/** 站房坐标与打卡点距离（Haversine），BR-03 阈值 300 米 */
public final class GeoUtil {

    private static final double EARTH_RADIUS_M = 6371000.0;

    private GeoUtil() {
    }

    public static int distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (int) Math.round(2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a)));
    }

    public static boolean inFence(double lng1, double lat1, double lng2, double lat2, int limitMeters) {
        return distanceMeters(lng1, lat1, lng2, lat2) <= limitMeters;
    }
}
