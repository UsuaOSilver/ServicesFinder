package edu.sjsu.android.servicesfinder.database;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.sjsu.android.servicesfinder.model.Service;

/**
 * Database class for READ-ONLY access to hardcoded services
 * Handles all Firestore read operations for services
 */
public class ServiceDatabase {

    private static final String TAG = "ServiceDatabase";
    private static final String COLLECTION_SERVICES = "services";

    private final FirebaseFirestore db;

    public ServiceDatabase() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Get all services from Firestore
     */
    public void getAllServices(OnServicesLoadedListener listener) {
        db.collection(COLLECTION_SERVICES)
                .orderBy("name")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Service> services = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Service service = new Service();
                        service.setServiceId(doc.getLong("serviceId").intValue());
                        service.setName(doc.getString("name"));
                        service.setDescription(doc.getString("description"));
                        service.setPrice(doc.getString("price"));
                        service.setCatalogueId(doc.getLong("catalogueId").intValue());
                        services.add(service);
                    }
                    listener.onSuccess(services);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching services", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Get specific service by ID
     */
    public void getServiceById(int serviceId, OnServiceLoadedListener listener) {
        db.collection(COLLECTION_SERVICES)
                .whereEqualTo("serviceId", serviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                        Service service = new Service();
                        service.setServiceId(doc.getLong("serviceId").intValue());
                        service.setName(doc.getString("name"));
                        service.setDescription(doc.getString("description"));
                        service.setPrice(doc.getString("price"));
                        service.setCatalogueId(doc.getLong("catalogueId").intValue());
                        listener.onSuccess(service);
                    } else {
                        listener.onError("Service not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching service", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Get all services under a specific catalogue
     */
    public void getServicesByCatalogue(int catalogueId, OnServicesLoadedListener listener) {
        db.collection(COLLECTION_SERVICES)
                .whereEqualTo("catalogueId", catalogueId)
                .orderBy("name")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Service> services = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Service service = new Service();
                        service.setServiceId(doc.getLong("serviceId").intValue());
                        service.setName(doc.getString("name"));
                        service.setDescription(doc.getString("description"));
                        service.setPrice(doc.getString("price"));
                        service.setCatalogueId(doc.getLong("catalogueId").intValue());
                        services.add(service);
                    }
                    listener.onSuccess(services);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching services by catalogue", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Search services by name
     */
    public void searchServicesByName(String searchQuery, OnServicesLoadedListener listener) {
        db.collection(COLLECTION_SERVICES)
                .orderBy("name")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Service> services = new ArrayList<>();
                    String lowerQuery = searchQuery.toLowerCase();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        if (name != null && name.toLowerCase().contains(lowerQuery)) {
                            Service service = new Service();
                            service.setServiceId(doc.getLong("serviceId").intValue());
                            service.setName(name);
                            service.setDescription(doc.getString("description"));
                            service.setPrice(doc.getString("price"));
                            service.setCatalogueId(doc.getLong("catalogueId").intValue());
                            services.add(service);
                        }
                    }
                    listener.onSuccess(services);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching services", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Get Firestore query for services (for RecyclerView adapter)
     */
    public Query getServicesQuery() {
        return db.collection(COLLECTION_SERVICES)
                .orderBy("name");
    }

    /**
     * Get Firestore query for services by catalogue (for RecyclerView adapter)
     */
    public Query getServicesByCatalogueQuery(int catalogueId) {
        return db.collection(COLLECTION_SERVICES)
                .whereEqualTo("catalogueId", catalogueId)
                .orderBy("name");
    }

    // Callback interfaces
    public interface OnServicesLoadedListener {
        void onSuccess(List<Service> services);
        void onError(String errorMessage);
    }

    public interface OnServiceLoadedListener {
        void onSuccess(Service service);
        void onError(String errorMessage);
    }
}