package edu.sjsu.android.servicesfinder.database;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.Date;

/**
 * Helper class for common Firestore utilities and constants
 * Provides shared Firestore instance and utility methods
 */
public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";

    // Collection names as constants
    public static final String COLLECTION_CATALOGUES = "catalogues";
    public static final String COLLECTION_SERVICES = "services";
    public static final String COLLECTION_PROVIDERS = "providers";
    public static final String COLLECTION_PROVIDER_SERVICES = "provider_services";

    // Field names as constants
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_SERVICE_ID = "serviceId";
    public static final String FIELD_CATALOGUE_ID = "catalogueId";
    public static final String FIELD_FULL_NAME = "fullName";
    public static final String FIELD_SERVICE_NAME = "serviceName";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_ADDRESS = "address";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_PROVIDER_ID = "providerId";

    private static FirebaseFirestore instance;

    /**
     * Get singleton Firestore instance
     */
    public static FirebaseFirestore getInstance() {
        if (instance == null) {
            instance = FirebaseFirestore.getInstance();
            configureFirestore(instance);
        }
        return instance;
    }

    /**
     * Configure Firestore settings
     */
    private static void configureFirestore(FirebaseFirestore db) {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  // Enable offline persistence
                .build();
        db.setFirestoreSettings(settings);
        Log.d(TAG, "Firestore configured with persistence enabled");
    }

    /**
     * Create current timestamp
     */
    public static Timestamp createTimestamp() {
        return Timestamp.now();
    }

    /**
     * Create timestamp from date
     */
    public static Timestamp createTimestamp(Date date) {
        return new Timestamp(date);
    }

    /**
     * Convert timestamp to date
     */
    public static Date timestampToDate(Timestamp timestamp) {
        return timestamp != null ? timestamp.toDate() : null;
    }

    /**
     * Handle common Firestore errors
     */
    public static String handleFirestoreError(Exception exception) {
        if (exception == null) {
            return "Unknown error occurred";
        }

        String errorMessage = exception.getMessage();
        if (errorMessage == null) {
            return "Unknown error occurred";
        }

        // Parse common Firestore error codes
        if (errorMessage.contains("PERMISSION_DENIED")) {
            return "You don't have permission to access this data";
        } else if (errorMessage.contains("UNAVAILABLE")) {
            return "Network unavailable. Please check your connection";
        } else if (errorMessage.contains("NOT_FOUND")) {
            return "Requested data not found";
        } else if (errorMessage.contains("ALREADY_EXISTS")) {
            return "This data already exists";
        } else if (errorMessage.contains("DEADLINE_EXCEEDED")) {
            return "Request timed out. Please try again";
        } else if (errorMessage.contains("UNAUTHENTICATED")) {
            return "Please sign in to continue";
        } else {
            return errorMessage;
        }
    }

    /**
     * Log Firestore error
     */
    public static void logError(String tag, String operation, Exception exception) {
        Log.e(tag, "Error during " + operation + ": " + exception.getMessage(), exception);
    }

    /**
     * Check if string is valid (not null and not empty)
     */
    public static boolean isValidString(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Validate required fields
     */
    public static boolean validateRequiredFields(String... fields) {
        for (String field : fields) {
            if (!isValidString(field)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (!isValidString(email)) {
            return false;
        }
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validate phone format (basic check)
     */
    public static boolean isValidPhone(String phone) {
        if (!isValidString(phone)) {
            return false;
        }
        // Remove non-digit characters
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        // Check if it has 10 digits (US format)
        return digitsOnly.length() == 10;
    }

    /**
     * Generate error message for missing fields
     */
    public static String getMissingFieldsError(String... fieldNames) {
        if (fieldNames.length == 0) {
            return "Required fields are missing";
        }
        return "Missing required field(s): " + String.join(", ", fieldNames);
    }

    /**
     * Sanitize string input (trim and remove extra spaces)
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Format phone number to (XXX) XXX-XXXX
     */
    public static String formatPhoneNumber(String phone) {
        if (phone == null) {
            return "";
        }
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 10) {
            return String.format("(%s) %s-%s",
                    digitsOnly.substring(0, 3),
                    digitsOnly.substring(3, 6),
                    digitsOnly.substring(6, 10));
        }
        return phone;
    }

    /**
     * Check if Firestore is available (network check)
     */
    public static void checkFirestoreAvailability(OnAvailabilityCheckListener listener) {
        getInstance()
                .collection(COLLECTION_CATALOGUES)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listener.onAvailable(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore unavailable", e);
                    listener.onAvailable(false);
                });
    }

    /**
     * Callback interface for availability check
     */
    public interface OnAvailabilityCheckListener {
        void onAvailable(boolean isAvailable);
    }
}