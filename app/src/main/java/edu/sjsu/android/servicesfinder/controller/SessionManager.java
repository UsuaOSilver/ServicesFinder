package edu.sjsu.android.servicesfinder.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public final class SessionManager {
    private static final String PREF = "services_finder_prefs";
    private static final String KEY_PROVIDER_ID = "provider_id";
    private static final String KEY_PROVIDER_NAME = "provider_name";

    private SessionManager() {}

    // =========================================================
    // SAVE SESSION
    // =========================================================
    public static void saveProvider(Context ctx, String providerId, String providerName) {
        if (ctx == null || providerId == null || providerId.trim().isEmpty()) {
            return;
        }
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_PROVIDER_ID, providerId)
                .putString(KEY_PROVIDER_NAME, providerName == null ? "" : providerName)
                .apply();
    }

    // =========================================================
    // GETTERS
    // =========================================================
    public static String getProviderId(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sp.getString(KEY_PROVIDER_ID, null);
    }

    public static String getProviderName(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sp.getString(KEY_PROVIDER_NAME, "");
    }

    // =========================================================
    // CLEAR SESSION
    // =========================================================
    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }

    // Alias method for readability (used by UI / Controller)
    public static void clearSession(Context ctx) {
        clear(ctx);
    }

    // Optional: quick check to see if user is logged in
    public static boolean isLoggedIn(Context ctx) {
        return getProviderId(ctx) != null;
    }
}
