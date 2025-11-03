package edu.sjsu.android.servicesfinder.database;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.sjsu.android.servicesfinder.model.Provider;
import edu.sjsu.android.servicesfinder.model.ProviderService;

/**
 * Database class for fetching providers with their services
 * Used for home screen display
 */
public class ProviderServiceDatabase {

    private static final String TAG = "ProviderServiceDB";
    private final FirebaseFirestore db;

    public ProviderServiceDatabase() {
        this.db = FirestoreHelper.getInstance();
    }

    /**
     * Load all providers with their services
     * Returns a map of Provider -> List of ProviderService
     */
    public void getAllProvidersWithServices(OnProvidersWithServicesLoadedListener listener) {
        db.collection(FirestoreHelper.COLLECTION_PROVIDERS)
                .get()
                .addOnSuccessListener(providerSnapshot -> {
                    if (providerSnapshot.isEmpty()) {
                        listener.onSuccess(new HashMap<>());
                        return;
                    }

                    Map<Provider, List<ProviderService>> providerServiceMap = new HashMap<>();
                    int[] pendingProviders = {providerSnapshot.size()};

                    for (QueryDocumentSnapshot providerDoc : providerSnapshot) {
                        Provider provider = documentToProvider(providerDoc);

                        // Load services for this provider
                        providerDoc.getReference()
                                .collection("services")
                                .whereEqualTo("status", "Active")
                                .get()
                                .addOnSuccessListener(servicesSnapshot -> {
                                    List<ProviderService> services = new ArrayList<>();

                                    for (QueryDocumentSnapshot serviceDoc : servicesSnapshot) {
                                        ProviderService service = documentToProviderService(serviceDoc);
                                        services.add(service);
                                    }

                                    // Only add providers that have services
                                    if (!services.isEmpty()) {
                                        providerServiceMap.put(provider, services);
                                    }

                                    // Check if all providers are loaded
                                    pendingProviders[0]--;
                                    if (pendingProviders[0] == 0) {
                                        listener.onSuccess(providerServiceMap);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading services for provider: " + provider.getId(), e);
                                    pendingProviders[0]--;
                                    if (pendingProviders[0] == 0) {
                                        listener.onSuccess(providerServiceMap);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading providers", e);
                    listener.onError(FirestoreHelper.handleFirestoreError(e));
                });
    }

    /**
     * Search providers and services by keyword
     * Searches in: provider name, service title, service description, category, service area
     */
    public void searchProvidersAndServices(String query, OnProvidersWithServicesLoadedListener listener) {
        if (query == null || query.trim().isEmpty()) {
            getAllProvidersWithServices(listener);
            return;
        }

        String lowerQuery = query.toLowerCase().trim();

        db.collection(FirestoreHelper.COLLECTION_PROVIDERS)
                .get()
                .addOnSuccessListener(providerSnapshot -> {
                    Map<Provider, List<ProviderService>> filteredMap = new HashMap<>();
                    int[] pendingProviders = {providerSnapshot.size()};

                    if (providerSnapshot.isEmpty()) {
                        listener.onSuccess(filteredMap);
                        return;
                    }

                    for (QueryDocumentSnapshot providerDoc : providerSnapshot) {
                        Provider provider = documentToProvider(providerDoc);

                        providerDoc.getReference()
                                .collection("services")
                                .whereEqualTo("status", "Active")
                                .get()
                                .addOnSuccessListener(servicesSnapshot -> {
                                    List<ProviderService> matchingServices = new ArrayList<>();

                                    for (QueryDocumentSnapshot serviceDoc : servicesSnapshot) {
                                        ProviderService service = documentToProviderService(serviceDoc);

                                        // Check if service matches search query
                                        if (serviceMatchesQuery(service, provider, lowerQuery)) {
                                            matchingServices.add(service);
                                        }
                                    }

                                    // Add provider if they have matching services OR their name matches
                                    if (!matchingServices.isEmpty() ||
                                            (provider.getFullName() != null &&
                                                    provider.getFullName().toLowerCase().contains(lowerQuery))) {

                                        // If provider name matches but no services match, show all services
                                        if (matchingServices.isEmpty()) {
                                            for (QueryDocumentSnapshot serviceDoc : servicesSnapshot) {
                                                matchingServices.add(documentToProviderService(serviceDoc));
                                            }
                                        }

                                        filteredMap.put(provider, matchingServices);
                                    }

                                    pendingProviders[0]--;
                                    if (pendingProviders[0] == 0) {
                                        listener.onSuccess(filteredMap);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading services", e);
                                    pendingProviders[0]--;
                                    if (pendingProviders[0] == 0) {
                                        listener.onSuccess(filteredMap);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching", e);
                    listener.onError(FirestoreHelper.handleFirestoreError(e));
                });
    }

    /**
     * Load services by category
     */
    public void getProvidersByCategory(String category, OnProvidersWithServicesLoadedListener listener) {
        db.collection(FirestoreHelper.COLLECTION_PROVIDERS)
                .get()
                .addOnSuccessListener(providerSnapshot -> {
                    Map<Provider, List<ProviderService>> categoryMap = new HashMap<>();
                    int[] pendingProviders = {providerSnapshot.size()};

                    if (providerSnapshot.isEmpty()) {
                        listener.onSuccess(categoryMap);
                        return;
                    }

                    for (QueryDocumentSnapshot providerDoc : providerSnapshot) {
                        Provider provider = documentToProvider(providerDoc);

                        providerDoc.getReference()
                                .collection("services")
                                .whereEqualTo("status", "Active")
                                .get()
                                .addOnSuccessListener(servicesSnapshot -> {
                                    List<ProviderService> matchingServices = new ArrayList<>();

                                    for (QueryDocumentSnapshot serviceDoc : servicesSnapshot) {
                                        ProviderService service = documentToProviderService(serviceDoc);

                                        if (service.getCategory() != null &&
                                                service.getCategory().contains(category)) {
                                            matchingServices.add(service);
                                        }
                                    }

                                    if (!matchingServices.isEmpty()) {
                                        categoryMap.put(provider, matchingServices);
                                    }

                                    pendingProviders[0]--;
                                    if (pendingProviders[0] == 0) {
                                        listener.onSuccess(categoryMap);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading services by category", e);
                                    pendingProviders[0]--;
                                    if (pendingProviders[0] == 0) {
                                        listener.onSuccess(categoryMap);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading providers by category", e);
                    listener.onError(FirestoreHelper.handleFirestoreError(e));
                });
    }
    // =========================================================
    // HELPER METHODS
    // =========================================================

    private boolean serviceMatchesQuery(ProviderService service, Provider provider, String query) {
        if (service.getServiceTitle() != null &&
                service.getServiceTitle().toLowerCase().contains(query)) {
            return true;
        }

        if (service.getDescription() != null &&
                service.getDescription().toLowerCase().contains(query)) {
            return true;
        }

        if (service.getCategory() != null &&
                service.getCategory().toLowerCase().contains(query)) {
            return true;
        }

        if (service.getServiceArea() != null &&
                service.getServiceArea().toLowerCase().contains(query)) {
            return true;
        }

        return false;
    }

    private Provider documentToProvider(DocumentSnapshot doc) {
        Provider provider = new Provider();
        provider.setId(doc.getId());
        provider.setFullName(doc.getString("fullName"));
        provider.setEmail(doc.getString("email"));
        provider.setPhone(doc.getString("phone"));
        provider.setAddress(doc.getString("address"));
        return provider;
    }

    private ProviderService documentToProviderService(QueryDocumentSnapshot doc) {
        ProviderService service = new ProviderService();
        service.setId(doc.getId());
        service.setProviderId(doc.getString("providerId"));
        service.setServiceTitle(doc.getString("serviceTitle"));
        service.setDescription(doc.getString("description"));
        service.setPricing(doc.getString("pricing"));
        service.setCategory(doc.getString("category"));
        service.setServiceArea(doc.getString("serviceArea"));
        service.setAvailability(doc.getString("availability"));
        service.setContactPreference(doc.getString("contactPreference"));
        service.setImageUrl(doc.getString("imageUrl"));

        Long timestamp = doc.getLong("timestamp");
        if (timestamp != null) {
            service.setTimestamp(timestamp);
        }

        return service;
    }

    // =========================================================
    // CALLBACK INTERFACES
    // =========================================================

    public interface OnProvidersWithServicesLoadedListener {
        void onSuccess(Map<Provider, List<ProviderService>> providerServiceMap);
        void onError(String errorMessage);
    }

}