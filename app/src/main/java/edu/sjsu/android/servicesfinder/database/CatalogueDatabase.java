package edu.sjsu.android.servicesfinder.database;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.sjsu.android.servicesfinder.model.Catalogue;

/**
 * Database class for READ-ONLY access to hardcoded catalogues
 * Handles all Firestore read operations for catalogues
 */
public class CatalogueDatabase {

    private static final String TAG = "CatalogueDatabase";
    private static final String COLLECTION_CATALOGUES = "catalogues";

    private final FirebaseFirestore db;

    public CatalogueDatabase() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Get all catalogues from Firestore
     */
    public void getAllCatalogues(OnCataloguesLoadedListener listener) {
        db.collection(COLLECTION_CATALOGUES)
                .orderBy("title")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Catalogue> catalogues = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Catalogue catalogue = new Catalogue();
                        catalogue.setId(doc.getLong("id").intValue());
                        catalogue.setTitle(doc.getString("title"));
                        catalogues.add(catalogue);
                    }
                    listener.onSuccess(catalogues);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching catalogues", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Get specific catalogue by ID
     */
    public void getCatalogueById(int catalogueId, OnCatalogueLoadedListener listener) {
        db.collection(COLLECTION_CATALOGUES)
                .whereEqualTo("id", catalogueId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                        Catalogue catalogue = new Catalogue();
                        catalogue.setId(doc.getLong("id").intValue());
                        catalogue.setTitle(doc.getString("title"));
                        listener.onSuccess(catalogue);
                    } else {
                        listener.onError("Catalogue not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching catalogue", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Get Firestore query for catalogues (for RecyclerView adapter)
     */
    public Query getCataloguesQuery() {
        return db.collection(COLLECTION_CATALOGUES)
                .orderBy("title");
    }

    /**
     * Get catalogue map with embedded services (for dropdown)
     * Reads services array from inside catalogue documents
     */
    public void getCatalogueMapWithEmbeddedServices(OnCatalogueMapLoadedListener listener) {
        db.collection(COLLECTION_CATALOGUES)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, List<String>> catalogueMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Get title (or use document ID as fallback)
                        String title = doc.getString("title");
                        if (title == null || title.isEmpty()) {
                            title = doc.getId();
                        }

                        // Get embedded services array
                        List<String> services = (List<String>) doc.get("services");

                        // Only add if services exist and are not empty
                        if (services != null && !services.isEmpty()) {
                            catalogueMap.put(title, services);
                        }
                    }

                    listener.onSuccess(catalogueMap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching catalogue map", e);
                    listener.onError(e.getMessage());
                });
    }

    // Callback interfaces
    public interface OnCataloguesLoadedListener {
        void onSuccess(List<Catalogue> catalogues);
        void onError(String errorMessage);
    }

    public interface OnCatalogueLoadedListener {
        void onSuccess(Catalogue catalogue);
        void onError(String errorMessage);
    }

    public interface OnCatalogueMapLoadedListener {
        void onSuccess(Map<String, List<String>> catalogueMap);
        void onError(String errorMessage);
    }
}