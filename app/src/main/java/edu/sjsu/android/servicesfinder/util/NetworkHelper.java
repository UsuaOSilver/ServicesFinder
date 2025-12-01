package edu.sjsu.android.servicesfinder.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * NetworkHelper - Utility class for network-related operations
 */
public class NetworkHelper {

    /**
     * Check if device has internet connectivity
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Get user-friendly network error message
     */
    public static String getNetworkErrorMessage(Context context) {
        if (!isNetworkAvailable(context)) {
            return "No internet connection. Please check your network settings.";
        }
        return "Network error. Please try again.";
    }
}
