package edu.sjsu.android.servicesfinder.util;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import edu.sjsu.android.servicesfinder.R;

/**
 * RetryDialog - Utility for showing retry dialogs on errors
 */
public class RetryDialog {

    /**
     * Show a retry dialog when an operation fails
     *
     * @param context Context
     * @param message Error message to display
     * @param onRetry Callback to execute when user taps Retry
     */
    public static void show(Context context, String message, Runnable onRetry) {
        new AlertDialog.Builder(context)
                .setTitle("Operation Failed")
                .setMessage(message + "\n\nWould you like to try again?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    if (onRetry != null) {
                        onRetry.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setCancelable(true)
                .show();
    }

    /**
     * Show a network error dialog with retry option
     */
    public static void showNetworkError(Context context, Runnable onRetry) {
        String message = NetworkHelper.getNetworkErrorMessage(context);
        show(context, message, onRetry);
    }

    /**
     * Show a generic error dialog without retry option
     */
    public static void showError(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
