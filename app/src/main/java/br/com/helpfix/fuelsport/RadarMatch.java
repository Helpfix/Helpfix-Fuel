package br.com.helpfix.fuelsport;

final class RadarMatch {
    record Point(long id, double latitude, double longitude, int limit) {}

    static double distance(double latitude, double longitude, Point point) {
        double y = Math.toRadians(point.latitude - latitude);
        double x = Math.toRadians(point.longitude - longitude)
                * Math.cos(Math.toRadians((latitude + point.latitude) / 2));
        return 6_371_000 * Math.hypot(x, y);
    }

    static boolean ahead(double latitude, double longitude, double bearing, Point point) {
        double y = point.latitude - latitude;
        double x = (point.longitude - longitude)
                * Math.cos(Math.toRadians((latitude + point.latitude) / 2));
        double target = Math.toDegrees(Math.atan2(x, y));
        double difference = Math.toRadians(angleDifference(bearing, target));
        return Math.cos(difference) > 0.5
                && Math.abs(distance(latitude, longitude, point) * Math.sin(difference)) <= 45;
    }

    private static double angleDifference(double first, double second) {
        return Math.abs(Math.IEEEremainder(first - second, 360));
    }

    private RadarMatch() {}
}
