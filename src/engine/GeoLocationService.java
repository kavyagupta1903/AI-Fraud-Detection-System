package engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Geolocation & Travel Feasibility Service.
 * Implements the spherical Haversine formula to detect physical Impossible Travel Speed
 * (e.g., transaction in New York followed 15 minutes later by a transaction in London/Lagos).
 */
public class GeoLocationService {

    public static class Coordinates {
        public final double latitude;
        public final double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static final Map<String, Coordinates> CITY_COORDINATES = new HashMap<>();
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double MAX_COMMERCIAL_FLIGHT_SPEED_KMH = 900.0; // Commercial jet cruise speed

    static {
        CITY_COORDINATES.put("NEW YORK, US", new Coordinates(40.7128, -74.0060));
        CITY_COORDINATES.put("NEW YORK", new Coordinates(40.7128, -74.0060));
        CITY_COORDINATES.put("CHICAGO, US", new Coordinates(41.8781, -87.6298));
        CITY_COORDINATES.put("CHICAGO", new Coordinates(41.8781, -87.6298));
        CITY_COORDINATES.put("SAN FRANCISCO, US", new Coordinates(37.7749, -122.4194));
        CITY_COORDINATES.put("SAN FRANCISCO", new Coordinates(37.7749, -122.4194));
        CITY_COORDINATES.put("MIAMI, US", new Coordinates(25.7617, -80.1918));
        CITY_COORDINATES.put("MIAMI", new Coordinates(25.7617, -80.1918));
        CITY_COORDINATES.put("LONDON, UK", new Coordinates(51.5074, -0.1278));
        CITY_COORDINATES.put("LONDON", new Coordinates(51.5074, -0.1278));
        CITY_COORDINATES.put("PARIS, FR", new Coordinates(48.8566, 2.3522));
        CITY_COORDINATES.put("PARIS", new Coordinates(48.8566, 2.3522));
        CITY_COORDINATES.put("FRANKFURT, DE", new Coordinates(50.1109, 8.6821));
        CITY_COORDINATES.put("FRANKFURT", new Coordinates(50.1109, 8.6821));
        CITY_COORDINATES.put("TOKYO, JP", new Coordinates(35.6762, 139.6503));
        CITY_COORDINATES.put("TOKYO", new Coordinates(35.6762, 139.6503));
        CITY_COORDINATES.put("SINGAPORE, SG", new Coordinates(1.3521, 103.8198));
        CITY_COORDINATES.put("SINGAPORE", new Coordinates(1.3521, 103.8198));
        CITY_COORDINATES.put("SYDNEY, AU", new Coordinates(-33.8688, 151.2093));
        CITY_COORDINATES.put("SYDNEY", new Coordinates(-33.8688, 151.2093));
        CITY_COORDINATES.put("DUBAI, AE", new Coordinates(25.2048, 55.2708));
        CITY_COORDINATES.put("DUBAI", new Coordinates(25.2048, 55.2708));
        CITY_COORDINATES.put("TORONTO, CA", new Coordinates(43.6532, -79.3832));
        CITY_COORDINATES.put("TORONTO", new Coordinates(43.6532, -79.3832));
        CITY_COORDINATES.put("LAGOS, NG", new Coordinates(6.5244, 3.3792));
        CITY_COORDINATES.put("LAGOS", new Coordinates(6.5244, 3.3792));
        CITY_COORDINATES.put("MOSCOW, RUSSIA", new Coordinates(55.7558, 37.6173));
        CITY_COORDINATES.put("MOSCOW, RU", new Coordinates(55.7558, 37.6173));
        CITY_COORDINATES.put("MOSCOW", new Coordinates(55.7558, 37.6173));
    }

    /**
     * Resolves approximate coordinates for location string.
     */
    public static Coordinates resolveCoordinates(String locationStr) {
        if (locationStr == null || locationStr.isBlank()) {
            return null;
        }
        String normalized = locationStr.toUpperCase().trim();
        for (Map.Entry<String, Coordinates> entry : CITY_COORDINATES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Calculates great-circle distance between two points using the Haversine formula.
     */
    public static double calculateHaversineDistanceKm(Coordinates p1, Coordinates p2) {
        if (p1 == null || p2 == null) return 0.0;

        double dLat = Math.toRadians(p2.latitude - p1.latitude);
        double dLon = Math.toRadians(p2.longitude - p1.longitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Evaluates travel velocity feasibility between two consecutive transactions.
     *
     * @param prevLocation location of prior transaction
     * @param prevTimeMillis timestamp millis of prior transaction
     * @param currLocation location of incoming transaction
     * @param currTimeMillis timestamp millis of incoming transaction
     * @return TravelFeasibility object containing distance, elapsed hours, speed km/h, and impossible flag
     */
    public static TravelFeasibility evaluateTravelVelocity(String prevLocation, long prevTimeMillis,
                                                           String currLocation, long currTimeMillis) {
        if (prevLocation == null || currLocation == null || prevLocation.equalsIgnoreCase(currLocation)) {
            return new TravelFeasibility(0.0, 0.0, 0.0, false);
        }

        Coordinates p1 = resolveCoordinates(prevLocation);
        Coordinates p2 = resolveCoordinates(currLocation);
        if (p1 == null || p2 == null) {
            return new TravelFeasibility(0.0, 0.0, 0.0, false);
        }

        double distanceKm = calculateHaversineDistanceKm(p1, p2);
        long elapsedMillis = Math.max(1000L, Math.abs(currTimeMillis - prevTimeMillis));
        double elapsedHours = elapsedMillis / (1000.0 * 3600.0);

        double speedKmh = distanceKm / elapsedHours;
        // If distance > 300km and implied speed > 900 km/h (commercial jet speed), travel is physically impossible
        boolean isImpossible = distanceKm > 300.0 && speedKmh > MAX_COMMERCIAL_FLIGHT_SPEED_KMH;

        return new TravelFeasibility(distanceKm, elapsedHours, speedKmh, isImpossible);
    }

    public static class TravelFeasibility {
        public final double distanceKm;
        public final double elapsedHours;
        public final double speedKmh;
        public final boolean isImpossible;

        public TravelFeasibility(double distanceKm, double elapsedHours, double speedKmh, boolean isImpossible) {
            this.distanceKm = Math.round(distanceKm * 10.0) / 10.0;
            this.elapsedHours = Math.round(elapsedHours * 100.0) / 100.0;
            this.speedKmh = Math.round(speedKmh * 10.0) / 10.0;
            this.isImpossible = isImpossible;
        }

        @Override
        public String toString() {
            return String.format("Distance: %.1f km | Time: %.2f hrs | Implied Speed: %.1f km/h | Impossible: %s",
                    distanceKm, elapsedHours, speedKmh, isImpossible ? "YES" : "NO");
        }
    }

    public static double calculateDistanceKm(String loc1, String loc2) {
        Coordinates p1 = resolveCoordinates(loc1);
        Coordinates p2 = resolveCoordinates(loc2);
        return calculateHaversineDistanceKm(p1, p2);
    }

    public static double calculateSpeedKmH(String loc1, String loc2, double hours) {
        double dist = calculateDistanceKm(loc1, loc2);
        return hours > 0.0 ? (dist / hours) : 0.0;
    }

    public static boolean isSpeedImpossible(String loc1, String loc2, double hours) {
        double dist = calculateDistanceKm(loc1, loc2);
        double speed = calculateSpeedKmH(loc1, loc2, hours);
        return dist > 300.0 && speed > MAX_COMMERCIAL_FLIGHT_SPEED_KMH;
    }
}
