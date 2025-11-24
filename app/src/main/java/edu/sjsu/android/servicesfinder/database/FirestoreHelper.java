package edu.sjsu.android.servicesfinder.database;

import android.content.Context;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.Date;

import edu.sjsu.android.servicesfinder.R;

//******************************************************************************************
// * Helper class for common Firestore utilities and constants
// * Provides shared Firestore instance and utility methods
//******************************************************************************************
public class FirestoreHelper {
    public static final String COLLECTION_PROVIDERS = "providers";
    private static FirebaseFirestore instance;

    //******************************************************************************************
    // Get singleton Firestore instance
    //******************************************************************************************
    public static FirebaseFirestore getInstance() {
        if (instance == null) {
            instance = FirebaseFirestore.getInstance();
            configureFirestore(instance);
        }
        return instance;
    }

    //******************************************************************************************
    // Configure Firestore settings
    //******************************************************************************************
    private static void configureFirestore(FirebaseFirestore db) {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  // Enable offline persistence
                .build();
        db.setFirestoreSettings(settings);
    }

    //******************************************************************************************
    // Handle common Firestore errors
    //******************************************************************************************
    public static String handleFirestoreError(Context context, Exception exception) {
        if (exception == null) {
            return context.getString(R.string.error_unknown);        }

        String errorMessage = exception.getMessage();
        if (errorMessage == null) {
            return context.getString(R.string.error_unknown);
        }
        // Parse common Firestore error codes
        if (errorMessage.contains("PERMISSION_DENIED")) {
            return context.getString(R.string.error_permission_denied);
        } else if (errorMessage.contains("UNAVAILABLE")) {
            return context.getString(R.string.error_network_unavailable);
        } else if (errorMessage.contains("NOT_FOUND")) {
            return context.getString(R.string.error_not_found);
        } else if (errorMessage.contains("ALREADY_EXISTS")) {
            return context.getString(R.string.error_already_exists);
        } else if (errorMessage.contains("DEADLINE_EXCEEDED")) {
            return context.getString(R.string.error_deadline_exceeded);
        } else if (errorMessage.contains("UNAUTHENTICATED")) {
            return context.getString(R.string.error_unauthenticated);
        } else {
            return errorMessage;
        }
    }

     //******************************************************************************************
     // Check if string is valid (not null and not empty)
    //******************************************************************************************
    public static boolean isValidString(String value) {
        return value != null && !value.trim().isEmpty();
    }

    //******************************************************************************************
    // Validate email format
    //******************************************************************************************
    public static boolean isValidEmail(String email) {
        if (!isValidString(email)) {
            return false;
        }
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    //******************************************************************************************
    // Validate phone format (basic check)
    //******************************************************************************************
    public static boolean isValidPhone(String phone) {
        if (!isValidString(phone)) {
            return false;
        }
        // Remove non-digit characters
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        // Check if it has 10 digits (US format)
        return digitsOnly.length() == 10;
    }

    //******************************************************************************************
    // Sanitize string input (trim and remove extra spaces)
    //******************************************************************************************
    public static String sanitizeString(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

}