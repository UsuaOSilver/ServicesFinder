package edu.sjsu.android.servicesfinder.database;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.sjsu.android.servicesfinder.model.Provider;

/**
 * ProviderDatabase - Data Access Layer for Provider operations
 *
 * Handles all Firestore operations for providers:
 * - Read provider by UID or email
 * - Create provider (using Firebase UID as document ID)
 * - Update provider
 * - Delete provider
 * - Manage provider services subcollection
 *
 * MVC ROLE: Database/Model layer
 */
public class ProviderDatabase {

    private static final String TAG = "ProviderDatabase";
    private static final String COLLECTION_PROVIDERS = "providers";

    private final FirebaseFirestore db;

    public ProviderDatabase() {
        this.db = FirebaseFirestore.getInstance();
    }

    // =========================================================
    // READ PROVIDER BY FIREBASE UID
    // =========================================================

    /**
     * Get provider by Firebase UID (document ID)
     */
    public void getProviderById(String providerId, OnProviderLoadedListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .document(providerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Provider provider = documentSnapshotToProvider(doc);
                        listener.onSuccess(provider);
                    } else {
                        listener.onError("Provider not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching provider by ID", e);
                    listener.onError(e.getMessage());
                });
    }

    // =========================================================
    // READ PROVIDER BY EMAIL
    // =========================================================

    /**
     * Get provider by email address
     */
    public void getProviderByEmail(String email, OnProviderLoadedListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) query.getDocuments().get(0);
                        Provider provider = queryDocumentSnapshotToProvider(doc);
                        listener.onSuccess(provider);
                    } else {
                        listener.onError("Email not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching provider by email", e);
                    listener.onError(e.getMessage());
                });
    }

    // =========================================================
    // CREATE PROVIDER USING FIREBASE UID AS DOCUMENT ID
    // =========================================================

    /**
     * Add new provider (document ID = Firebase UID)
     */
    public void addProvider(Provider provider, OnProviderOperationListener listener) {
        // Log the document path and payload before writing
        Log.d(TAG, "Saving to /providers/" + provider.getId());
        Log.d(TAG, "Payload: " + providerToMap(provider).toString());

        db.collection(COLLECTION_PROVIDERS)
                .document(provider.getId())  // Use Firebase UID as document ID
                .set(providerToMap(provider))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Provider saved: " + provider.getId());
                    listener.onSuccess("Provider saved");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding provider", e);
                    listener.onError(e.getMessage());
                });
    }


    // =========================================================
    // UPDATE PROVIDER
    // =========================================================

    /**
     * Update existing provider
     */
    public void updateProvider(String providerId, Provider provider, OnProviderOperationListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .document(providerId)
                .update(providerToMap(provider))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Provider updated: " + providerId);
                    listener.onSuccess("Provider updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating provider", e);
                    listener.onError(e.getMessage());
                });
    }

    // =========================================================
    // DELETE PROVIDER
    // =========================================================

    /**
     * Delete provider
     */
    public void deleteProvider(String providerId, OnProviderOperationListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .document(providerId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Provider deleted: " + providerId);
                    listener.onSuccess("Provider deleted");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting provider", e);
                    listener.onError(e.getMessage());
                });
    }

    // =========================================================
    // PROVIDER SERVICES SUBCOLLECTION
    // =========================================================

    /**
     * Load all services for a provider from subcollection
     */
    public void loadProviderServices(String providerId, OnProviderServicesLoadedListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .document(providerId)
                .collection("services")
                .get()
                .addOnSuccessListener(query -> {
                    Log.d(TAG, "Loaded " + query.size() + " services for provider: " + providerId);
                    listener.onSuccess(query.getDocuments());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading provider services", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Add a service to provider's subcollection
     */
    public void addService(String providerId, Map<String, Object> serviceData, OnProviderOperationListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .document(providerId)
                .collection("services")
                .add(serviceData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Service added: " + docRef.getId());
                    listener.onSuccess("Service added");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding service", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Update a service in provider's subcollection
     */
    public void updateService(String providerId, String serviceId, Map<String, Object> serviceData, OnProviderOperationListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .document(providerId)
                .collection("services")
                .document(serviceId)
                .update(serviceData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Service updated: " + serviceId);
                    listener.onSuccess("Service updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating service", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Delete a service from provider's subcollection
     */
    public void deleteService(String providerId, String serviceId, OnProviderOperationListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .document(providerId)
                .collection("services")
                .document(serviceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Service deleted: " + serviceId);
                    listener.onSuccess("Service deleted");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting service", e);
                    listener.onError(e.getMessage());
                });
    }

    // =========================================================
    // HELPER METHODS - DOCUMENT CONVERSION
    // =========================================================

    /**
     * Convert DocumentSnapshot to Provider (for getById)
     */
    private Provider documentSnapshotToProvider(DocumentSnapshot doc) {
        Provider provider = new Provider();
        provider.setId(doc.getString("id"));
        provider.setFullName(doc.getString("fullName"));
        provider.setEmail(doc.getString("email"));
        provider.setAddress(doc.getString("address"));
        provider.setPhone(doc.getString("phone"));
        provider.setPassword(doc.getString("password"));
        return provider;
    }

    /**
     * Convert QueryDocumentSnapshot to Provider (for queries)
     */
    private Provider queryDocumentSnapshotToProvider(QueryDocumentSnapshot doc) {
        Provider provider = new Provider();
        provider.setId(doc.getString("id"));
        provider.setFullName(doc.getString("fullName"));
        provider.setEmail(doc.getString("email"));
        provider.setAddress(doc.getString("address"));
        provider.setPhone(doc.getString("phone"));
        provider.setPassword(doc.getString("password"));
        return provider;
    }

    /**
     * Convert Provider to Map for Firestore
     */
    private Map<String, Object> providerToMap(Provider provider) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", provider.getId());
        map.put("fullName", provider.getFullName());
        map.put("email", provider.getEmail());
        map.put("address", provider.getAddress());
        map.put("phone", provider.getPhone());

        // ⚠️ WARNING: Storing passwords is NOT recommended in production!
        // Firebase Auth handles passwords - this is for demo/testing only
        map.put("password", provider.getPassword());

        return map;
    }

    // =========================================================
    // CALLBACK INTERFACES
    // =========================================================

    public interface OnProviderLoadedListener {
        void onSuccess(Provider provider);
        void onError(String errorMessage);
    }

    public interface OnProviderOperationListener {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface OnProviderServicesLoadedListener {
        void onSuccess(List<?> services);
        void onError(String errorMessage);
    }
}