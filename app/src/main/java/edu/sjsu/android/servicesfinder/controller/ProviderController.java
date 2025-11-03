package edu.sjsu.android.servicesfinder.controller;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.sjsu.android.servicesfinder.database.FirestoreHelper;
import edu.sjsu.android.servicesfinder.database.ProviderDatabase;
import edu.sjsu.android.servicesfinder.model.Provider;
import edu.sjsu.android.servicesfinder.model.ProviderService;

/**
 * ProviderController - Business Logic Layer
 *
 * Handles:
 * - Provider registration (with Firebase Auth)
 * - Provider login validation
 * - Provider data loading
 * - Provider service management
 * - Input validation and sanitization
 * - Data formatting for UI
 *
 * MVC ROLE: Controller
 * - Coordinates between View and Database
 * - Applies business logic
 * - Validates data
 * - Formats data for display
 */
public class ProviderController {

    private final ProviderDatabase providerDatabase;
    private ProviderControllerListener listener;

    public ProviderController() {
        this.providerDatabase = new ProviderDatabase();
    }

    public void setListener(ProviderControllerListener listener) {
        this.listener = listener;
    }

    // =========================================================
    // PROVIDER REGISTRATION
    // =========================================================

    /**
     * Register a new provider (called AFTER Firebase Auth creates user)
     *
     * @param provider  Provider object with Firebase UID set as id
     * @param debugFlow
     */
    public void registerProvider(Provider provider, ProviderDatabase.OnProviderOperationListener debugFlow) {
        String validationError = validateProvider(provider);
        if (validationError != null) {
            debugFlow.onError(validationError); //  Use passed listener
            return;
        }

        provider.setFullName(FirestoreHelper.sanitizeString(provider.getFullName()));
        provider.setEmail(FirestoreHelper.sanitizeString(provider.getEmail()));
        provider.setPhone(FirestoreHelper.sanitizeString(provider.getPhone()));
        provider.setAddress(FirestoreHelper.sanitizeString(provider.getAddress()));

        providerDatabase.addProvider(provider, new ProviderDatabase.OnProviderOperationListener() {
            @Override
            public void onSuccess(String message) {
                debugFlow.onSuccess(message); //  Use passed listener
            }

            @Override
            public void onError(String errorMessage) {
                debugFlow.onError(errorMessage); //  Use passed listener
            }
        });
    }

    // =========================================================
    // LOAD PROVIDER BY FIREBASE UID
    // =========================================================

    /**
     * Load provider profile by Firebase UID
     * @param providerId Firebase UID
     */
    public void loadProviderById(String providerId) {
        if (providerId == null || providerId.trim().isEmpty()) {
            if (listener != null) {
                listener.onError("Provider ID is required");
            }
            return;
        }

        providerDatabase.getProviderById(providerId, new ProviderDatabase.OnProviderLoadedListener() {
            @Override
            public void onSuccess(Provider provider) {
                if (listener != null) {
                    listener.onProviderLoaded(provider);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    /**
     * Validate provider data
     * @return Error message if invalid, null if valid
     */
    private String validateProvider(Provider provider) {
        if (provider == null) {
            return "Provider data is null";
        }

        if (!FirestoreHelper.isValidString(provider.getId())) {
            return "Provider ID is required";
        }

        if (!FirestoreHelper.isValidString(provider.getFullName())) {
            return "Full name is required";
        }

        if (!FirestoreHelper.isValidEmail(provider.getEmail())) {
            return "Valid email is required";
        }

        if (!FirestoreHelper.isValidPhone(provider.getPhone())) {
            return "Valid phone number is required (10 digits)";
        }

        if (!FirestoreHelper.isValidString(provider.getAddress())) {
            return "Address is required";
        }

        // Password validation only for registration
        if (!FirestoreHelper.isValidString(provider.getPassword())) {
            return "Password is required";
        }

        if (provider.getPassword().length() < 6) {
            return "Password must be at least 6 characters";
        }

        return null; // Valid
    }


    // =========================================================
    // LISTENER INTERFACE
    // =========================================================

    /**
     * Listener interface for callbacks to View
     */
    public interface ProviderControllerListener {
        /**
         * Called when provider data is loaded
         */
        void onProviderLoaded(Provider provider);

        /**
         * Called when login is successful
         */
        //void onLoginSuccess(Provider provider);

        /**
         * Called when provider services are loaded
         */
        //void onProviderServicesLoaded(List<ProviderService> providerServices);

        /**
         * Called when an operation succeeds (register, update, delete)
         */
        //void onOperationSuccess(String message);

        /**
         * Called when an error occurs
         */
        void onError(String errorMessage);
    }
}