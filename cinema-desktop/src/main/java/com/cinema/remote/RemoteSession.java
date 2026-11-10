package com.cinema.remote;

/** The Windows client uses the hosted API by default; override for local development. */
public final class RemoteSession {
    private static final String DEFAULT_URL = "https://cinema-booking-api-8dsw.onrender.com/";
    private static final String URL = resolveUrl();
    private static final CinemaApiClient CLIENT = new CinemaApiClient(URL);

    private RemoteSession() {}

    private static String resolveUrl() {
        String override = System.getenv("CINEMA_API_URL");
        return override == null || override.isBlank() ? DEFAULT_URL : override.trim();
    }

    public static boolean enabled() { return true; }
    public static CinemaApiClient client() { return CLIENT; }
}
