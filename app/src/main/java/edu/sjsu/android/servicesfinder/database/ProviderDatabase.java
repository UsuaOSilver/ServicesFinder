package edu.sjsu.android.servicesfinder.database;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.model.Provider;

/* ***********************************************************************************************
 * ProviderDatabase - Data Access Layer for Provider operations
 *
 * Handles all Firestore operations for providers:
 * - Read provider by UID or phone
 * - Create provider (using Firebase UID as document ID)
 * - Update provider
 * - Delete provider
 * - Manage provider services subcollection
 *
 * MVC ROLE: Database/Model layer
 *************************************************************************************************/
public class ProviderDatabase {
    private static final String COLLECTION_PROVIDERS = "providers";
    private final FirebaseFirestore db;
    private final Context context;
    public ProviderDatabase(Context context) {
        this.context = context;
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
                        listener.onError(context.getString(R.string.error_provider_not_found));
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                });
    }

    // =========================================================
    // READ PROVIDER BY PHONE (for Phone-based sign-in)
    // =========================================================
    public void getProviderByPhone(String phone, OnProviderLoadedListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        Provider provider = query.getDocuments()
                                .get(0)
                                .toObject(Provider.class);
                        listener.onSuccess(provider);
                    } else {
                        listener.onError(context.getString(R.string.error_provider_not_found_with_phone));
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onError(context.getString(R.string.error_fetching_provider, e.getMessage()));
                });
    }

    // =========================================================
    // CREATE NEW PROVIDER USING FIREBASE UID AS DOCUMENT ID
    // =========================================================
    public void addProvider(Provider provider, OnProviderOperationListener listener) {
        // Log the document path and payload before writing
        db.collection(COLLECTION_PROVIDERS)
                .document(provider.getId())  // Use Firebase UID as document ID
                .set(providerToMap(provider))
                .addOnSuccessListener(aVoid -> {
                    listener.onSuccess(context.getString(R.string.success_provider_saved));
                })
                .addOnFailureListener(e -> {
                    // Log.e(TAG, "Error adding provider", e);
                    listener.onError(e.getMessage());
                });
    }

    // =========================================================
    // Convert DocumentSnapshot(Firebase data) to Provider
    // =========================================================
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

    //**********************************************************************************
    // Converts a Provider object into a Map<String, Object>, so it can be saved to Firestore
    //************************************************************************************
    private Map<String, Object> providerToMap(Provider provider) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", provider.getId());
        map.put("fullName", provider.getFullName());
        map.put("email", provider.getEmail());
        map.put("address", provider.getAddress());
        map.put("phone", provider.getPhone());

        // Use only for testing/demo purposes.
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
    // =========================================================
    // UPDATE PROVIDER FIELDS (in setting)
    // =========================================================
    public void updateProviderFields(String providerId, Map<String, Object> updates, OnProviderOperationListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .document(providerId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    listener.onSuccess(context.getString(R.string.success_provider_updated));
                })
                .addOnFailureListener(e -> {
                    listener.onError(context.getString(R.string.error_update_failed, e.getMessage()));
                });
    }
    // =========================================================
    // Deletes a provider document from Firestore by UID.
    // =========================================================

    public void deleteProvider(String providerId, OnProviderOperationListener listener) {
        // Step 1: Delete all services in subcollection
        db.collection(COLLECTION_PROVIDERS)
                .document(providerId)
                .collection("services")
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        doc.getReference().delete();
                    }

                    // Step 2: Delete the provider document itself
                    db.collection(COLLECTION_PROVIDERS)
                            .document(providerId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                listener.onSuccess(context.getString(R.string.success_provider_and_services_deleted));
                            })
                            .addOnFailureListener(e -> {
                                listener.onError(context.getString(R.string.error_delete_provider_failed, e.getMessage()));
                            });
                })
                .addOnFailureListener(e -> {
                    listener.onError(context.getString(R.string.error_delete_services_failed, e.getMessage()));
                });
    }
    /// ///////////////////////////////////////////////////////////////////////////////////////////
    public void cloneServices(String fromProviderId, String toProviderId, OnProviderOperationListener listener) {
        db.collection(COLLECTION_PROVIDERS)
                .document(fromProviderId)
                .collection("services")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        listener.onSuccess("No services to clone");
                        return;
                    }

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Map<String, Object> serviceData = doc.getData();
                        if (serviceData != null) {
                            db.collection(COLLECTION_PROVIDERS)
                                    .document(toProviderId)
                                    .collection("services")
                                    .document(doc.getId())
                                    .set(serviceData);
                        }
                    }
                    listener.onSuccess("Services cloned");
                })
                .addOnFailureListener(e -> {
                    listener.onError("Failed to clone services: " + e.getMessage());
                });
    }
}