package edu.sjsu.android.servicesfinder.controller;

import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.sjsu.android.servicesfinder.database.CatalogueDatabase;
import edu.sjsu.android.servicesfinder.database.ServiceDatabase;
import edu.sjsu.android.servicesfinder.model.Catalogue;
import edu.sjsu.android.servicesfinder.model.Service;

/**
 * Controller for Catalogue business logic
 * Coordinates between View and Database layers
 */
public class CatalogueController {

    private final CatalogueDatabase catalogueDatabase;
    private CatalogueControllerListener listener;

    public CatalogueController() {
        this.catalogueDatabase = new CatalogueDatabase();
    }

    public void setListener(CatalogueControllerListener listener) {
        this.listener = listener;
    }

    /**
     * Load catalogue map for dropdown (Catalogue Title -> List of Service Names)
     * This is formatted specifically for MultiSelectDropdown
     * Uses embedded services array from catalogue documents
     */
    public void loadCatalogueMapForDropdown() {
        catalogueDatabase.getCatalogueMapWithEmbeddedServices(new CatalogueDatabase.OnCatalogueMapLoadedListener() {
            @Override
            public void onSuccess(Map<String, List<String>> catalogueMap) {
                if (listener != null) {
                    listener.onCatalogueMapLoaded(catalogueMap);
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
     * Listener interface for callbacks to View
     */
    public interface CatalogueControllerListener {
        void onCataloguesLoaded(List<Catalogue> catalogues);
        void onCatalogueWithServicesLoaded(Catalogue catalogue, List<Service> services);
        void onCatalogueMapLoaded(Map<String, List<String>> catalogueMap);
        void onError(String errorMessage);
    }
}