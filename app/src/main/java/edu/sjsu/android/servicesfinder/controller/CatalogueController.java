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
    private final ServiceDatabase serviceDatabase;
    private CatalogueControllerListener listener;

    public CatalogueController() {
        this.catalogueDatabase = new CatalogueDatabase();
        this.serviceDatabase = new ServiceDatabase();
    }

    public void setListener(CatalogueControllerListener listener) {
        this.listener = listener;
    }

    /**
     * Load all catalogues
     */
    public void loadAllCatalogues() {
        catalogueDatabase.getAllCatalogues(new CatalogueDatabase.OnCataloguesLoadedListener() {
            @Override
            public void onSuccess(List<Catalogue> catalogues) {
                if (listener != null) {
                    listener.onCataloguesLoaded(catalogues);
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
     * Load catalogue with its services
     */
    public void loadCatalogueWithServices(int catalogueId) {
        catalogueDatabase.getCatalogueById(catalogueId, new CatalogueDatabase.OnCatalogueLoadedListener() {
            @Override
            public void onSuccess(Catalogue catalogue) {
                // Now load services for this catalogue
                serviceDatabase.getServicesByCatalogue(catalogueId, new ServiceDatabase.OnServicesLoadedListener() {
                    @Override
                    public void onSuccess(List<Service> services) {
                        if (listener != null) {
                            listener.onCatalogueWithServicesLoaded(catalogue, services);
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

            @Override
            public void onError(String errorMessage) {
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
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
     * Build catalogue map for dropdown
     * Groups services by their catalogue title
     */
    private Map<String, List<String>> buildCatalogueMap(List<Catalogue> catalogues, List<Service> services) {
        Map<String, List<String>> map = new HashMap<>();

        for (Catalogue catalogue : catalogues) {
            java.util.List<String> serviceNames = new java.util.ArrayList<>();

            // Find all services that belong to this catalogue
            for (Service service : services) {
                if (service.getCatalogueId() == catalogue.getId()) {
                    serviceNames.add(service.getName());
                }
            }

            // Only add if there are services
            if (!serviceNames.isEmpty()) {
                map.put(catalogue.getTitle(), serviceNames);
            }
        }

        return map;
    }

    /**
     * Get Firestore query for catalogues (for RecyclerView)
     */
    public Query getCataloguesQuery() {
        return catalogueDatabase.getCataloguesQuery();
    }

    /**
     * Validate catalogue data
     */
    private boolean validateCatalogue(Catalogue catalogue) {
        return catalogue != null
                && catalogue.getTitle() != null
                && !catalogue.getTitle().trim().isEmpty();
    }

    /**
     * Format catalogue summary for display
     */
    public String formatCatalogueSummary(List<Catalogue> catalogues) {
        if (catalogues == null || catalogues.isEmpty()) {
            return "No catalogues available";
        }

        if (catalogues.size() == 1) {
            return catalogues.get(0).getTitle();
        }

        return catalogues.size() + " catalogues available";
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