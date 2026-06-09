package com.example.bringo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class Session {
    private Session() {
        // No instances
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(Keys.PREFS, Context.MODE_PRIVATE);
    }

    public static long getLoggedUserId(Context ctx) {
        // Logged-in customer/carrier userId (or -1 if none)
        return prefs(ctx).getLong(Keys.LOGGED_USER_ID, -1);
    }

    public static String getLoggedRole(Context ctx) {
        // Role string saved at login (or null if none)
        return prefs(ctx).getString(Keys.LOGGED_ROLE, null);
    }

    public static String getLoggedEmail(Context ctx) {
        // Email saved at login (or null if none)
        return prefs(ctx).getString(Keys.LOGGED_EMAIL, null);
    }

    public static long getLoggedCarrierId(Context ctx) {
        // Carrier DB id (or -1 if not a carrier / not set)
        return prefs(ctx).getLong(Keys.LOGGED_CARRIER_ID, -1);
    }

    public static void saveLogin(Context ctx, long userId, String role, String email, long carrierId) {
        // Saves the whole session after successful login/register
        prefs(ctx).edit()
                .putLong(Keys.LOGGED_USER_ID, userId)
                .putString(Keys.LOGGED_ROLE, role)
                .putString(Keys.LOGGED_EMAIL, email)
                .putLong(Keys.LOGGED_CARRIER_ID, carrierId)
                .apply();
    }

    public static void clear(Context ctx) {
        // Clears the whole saved session (userId/role/email/carrierId)
        prefs(ctx).edit().clear().apply();
    }
}