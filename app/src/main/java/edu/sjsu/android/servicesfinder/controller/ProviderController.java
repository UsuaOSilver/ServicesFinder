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
            debugFlow.onError(validationError); // ✅ Use passed listener
            return;
        }

        provider.setFullName(FirestoreHelper.sanitizeString(provider.getFullName()));
        provider.setEmail(FirestoreHelper.sanitizeString(provider.getEmail()));
        provider.setPhone(FirestoreHelper.sanitizeString(provider.getPhone()));
        provider.setAddress(FirestoreHelper.sanitizeString(provider.getAddress()));

        providerDatabase.addProvider(provider, new ProviderDatabase.OnProviderOperationListener() {
            @Override
            public void onSuccess(String message) {
                debugFlow.onSuccess(message); // ✅ Use passed listener
            }

            @Override
            public void onError(String errorMessage) {
                debugFlow.onError(errorMessage); // ✅ Use passed listener
            }
        });
    }


    // =========================================================
    // PROVIDER LOGIN (Email/Password validation)
    // =========================================================

    /**
     * Login provider by validating email and password
     * NOTE: With Firebase Auth, this is handled by ProviderEntryActivity
     * This method is for fallback/testing only
     */
    public void loginProvider(String email, String password) {
        // Validate email format
        if (!FirestoreHelper.isValidEmail(email)) {
            if (listener != null) {
                listener.onError("Invalid email format");
            }
            return;
        }

        // Query provider by email
        providerDatabase.getProviderByEmail(email, new ProviderDatabase.OnProviderLoadedListener() {
            @Override
            public void onSuccess(Provider provider) {
                // Verify password
                if (provider.getPassword() != null && provider.getPassword().equals(password)) {
                    if (listener != null) {
                        listener.onLoginSuccess(provider);
                    }
                } else {
                    if (listener != null) {
                        listener.onError("Incorrect password");
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (listener != null) {
                    listener.onError("Email not found");
                }
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
    // UPDATE PROVIDER
    // =========================================================

    /**
     * Update provider profile
     */
    public void updateProvider(String providerId, Provider provider) {
        String validationError = validateProvider(provider);
        if (validationError != null) {
            if (listener != null) {
                listener.onError(validationError);
            }
            return;
        }

        providerDatabase.updateProvider(providerId, provider, new ProviderDatabase.OnProviderOperationListener() {
            @Override
            public void onSuccess(String message) {
                if (listener != null) {
                    listener.onOperationSuccess(message);
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
    // DELETE PROVIDER
    // =========================================================

    /**
     * Delete provider account
     */
    public void deleteProvider(String providerId) {
        providerDatabase.deleteProvider(providerId, new ProviderDatabase.OnProviderOperationListener() {
            @Override
            public void onSuccess(String message) {
                if (listener != null) {
                    listener.onOperationSuccess(message);
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
    // PROVIDER SERVICES MANAGEMENT
    // =========================================================

    /**
     * Load all services for a provider
     */
    public void loadProviderServices(String providerId) {
        providerDatabase.loadProviderServices(providerId, new ProviderDatabase.OnProviderServicesLoadedListener() {
            @Override
            public void onSuccess(List<?> services) {
                // Convert raw documents to ProviderService objects
                List<ProviderService> providerServices = new ArrayList<>();
                for (Object obj : services) {
                    if (obj instanceof QueryDocumentSnapshot) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) obj;
                        ProviderService ps = documentToProviderService(doc);
                        providerServices.add(ps);
                    }
                }

                if (listener != null) {
                    listener.onProviderServicesLoaded(providerServices);
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

    /**
     * Convert Firestore document to ProviderService object
     */
    private ProviderService documentToProviderService(QueryDocumentSnapshot doc) {
        ProviderService ps = new ProviderService();
        ps.setId(doc.getId());
        ps.setProviderId(doc.getString("providerId"));
        ps.setServiceTitle(doc.getString("serviceTitle"));
        ps.setDescription(doc.getString("description"));
        ps.setPricing(doc.getString("pricing"));
        ps.setCategory(doc.getString("category"));
        ps.setServiceArea(doc.getString("serviceArea"));
        ps.setAvailability(doc.getString("availability"));
        ps.setContactPreference(doc.getString("contactPreference"));
        ps.setImageUrl(doc.getString("imageUrl"));

        Long timestamp = doc.getLong("timestamp");
        if (timestamp != null) {
            ps.setTimestamp(timestamp);
        }

        return ps;
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
    // FORMATTING & DISPLAY HELPERS
    // =========================================================

    /**
     * Format provider for display (summary)
     */
    public String formatProviderSummary(Provider provider) {
        if (provider == null) {
            return "Unknown Provider";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(provider.getFullName());

        if (provider.getEmail() != null && !provider.getEmail().isEmpty()) {
            summary.append(" (").append(provider.getEmail()).append(")");
        }

        return summary.toString();
    }

    /**
     * Format provider contact info
     */
    public String formatProviderContact(Provider provider) {
        if (provider == null) {
            return "No contact info";
        }

        List<String> parts = new ArrayList<>();

        if (provider.getPhone() != null) {
            parts.add(FirestoreHelper.formatPhoneNumber(provider.getPhone()));
        }

        if (provider.getEmail() != null) {
            parts.add(provider.getEmail());
        }

        if (provider.getAddress() != null) {
            parts.add(provider.getAddress());
        }

        return String.join(" | ", parts);
    }

    /**
     * Format provider service count
     */
    public String formatServiceCount(List<ProviderService> services) {
        if (services == null || services.isEmpty()) {
            return "No services offered";
        }

        if (services.size() == 1) {
            return "1 service offered";
        }

        return services.size() + " services offered";
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
        void onLoginSuccess(Provider provider);

        /**
         * Called when provider services are loaded
         */
        void onProviderServicesLoaded(List<ProviderService> providerServices);

        /**
         * Called when an operation succeeds (register, update, delete)
         */
        void onOperationSuccess(String message);

        /**
         * Called when an error occurs
         */
        void onError(String errorMessage);
    }
}