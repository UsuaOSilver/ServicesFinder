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

/* ******************************************************************************
 * Database class for READ-ONLY access to hardcoded catalogues
 * Handles all Firestore read operations for catalogues
 ********************************************************************************/
public class CatalogueDatabase {

    private static final String TAG = "CatalogueDatabase";
    private static final String COLLECTION_CATALOGUES = "catalogues";

    private final FirebaseFirestore db;

    public CatalogueDatabase() {
        this.db = FirebaseFirestore.getInstance();
    }


    /* *****************************************************************************
     * Get catalogue map with embedded services (for dropdown)
     * Reads services array from inside catalogue documents
     ************************************************************************************/
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
                        @SuppressWarnings("unchecked")
                        List<String> services = (List<String>) doc.get("services");

                        // Only add if services exist and are not empty
                        if (services != null && !services.isEmpty()) {
                            catalogueMap.put(title, services);
                        }
                    }

                    listener.onSuccess(catalogueMap);
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                });
    }
    public interface OnCatalogueMapLoadedListener {
        void onSuccess(Map<String, List<String>> catalogueMap);
        void onError(String errorMessage);
    }
}