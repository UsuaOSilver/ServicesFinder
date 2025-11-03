package edu.sjsu.android.servicesfinder.controller;

import android.util.Log;
import java.util.List;
import java.util.Map;
import edu.sjsu.android.servicesfinder.database.ProviderServiceDatabase;
import edu.sjsu.android.servicesfinder.model.Provider;
import edu.sjsu.android.servicesfinder.model.ProviderService;

/**
 * HomeController - Business logic for home screen
 * Handles loading, searching, and filtering providers with services
 */
public class HomeController {

    private static final String TAG = "HomeController";

    private final ProviderServiceDatabase database;
    private HomeControllerListener listener;

    // Cache for search optimization
    private Map<Provider, List<ProviderService>> cachedData;
    private String lastSearchQuery = "";

    public HomeController() {
        this.database = new ProviderServiceDatabase();
    }

    public void setListener(HomeControllerListener listener) {
        this.listener = listener;
    }

    /**
     * Load all providers with their services
     */
    public void loadAllProvidersWithServices() {
        database.getAllProvidersWithServices(new ProviderServiceDatabase.OnProvidersWithServicesLoadedListener() {
            @Override
            public void onSuccess(Map<Provider, List<ProviderService>> providerServiceMap) {
                cachedData = providerServiceMap;

                if (listener != null) {
                    if (providerServiceMap.isEmpty()) {
                        listener.onNoDataAvailable();
                    } else {
                        listener.onProvidersWithServicesLoaded(providerServiceMap);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading providers: " + errorMessage);
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    /**
     * Search providers and services
     */
    public void searchProvidersAndServices(String query) {
        lastSearchQuery = query;

        if (query == null || query.trim().isEmpty()) {
            loadAllProvidersWithServices();
            return;
        }

        database.searchProvidersAndServices(query, new ProviderServiceDatabase.OnProvidersWithServicesLoadedListener() {
            @Override
            public void onSuccess(Map<Provider, List<ProviderService>> providerServiceMap) {
                if (listener != null) {
                    if (providerServiceMap.isEmpty()) {
                        listener.onSearchResultsEmpty(query);
                    } else {
                        listener.onSearchResultsLoaded(providerServiceMap, query);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Search error: " + errorMessage);
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    /**
     * Filter by category
     */
    public void filterByCategory(String category) {
        database.getProvidersByCategory(category, new ProviderServiceDatabase.OnProvidersWithServicesLoadedListener() {
            @Override
            public void onSuccess(Map<Provider, List<ProviderService>> providerServiceMap) {
                if (listener != null) {
                    if (providerServiceMap.isEmpty()) {
                        listener.onNoDataAvailable();
                    } else {
                        listener.onProvidersWithServicesLoaded(providerServiceMap);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Filter error: " + errorMessage);
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    // =========================================================
    // LISTENER INTERFACE
    // =========================================================

    public interface HomeControllerListener {
        /**
         * Called when providers with services are loaded
         */
        void onProvidersWithServicesLoaded(Map<Provider, List<ProviderService>> providerServiceMap);

        /**
         * Called when search results are loaded
         */
        void onSearchResultsLoaded(Map<Provider, List<ProviderService>> providerServiceMap, String query);

        /**
         * Called when search returns no results
         */
        void onSearchResultsEmpty(String query);

        /**
         * Called when provider details are loaded
         */
        void onProviderDetailsLoaded(Provider provider, List<ProviderService> services);

        /**
         * Called when no data is available
         */
        void onNoDataAvailable();

        /**
         * Called when an error occurs
         */
        void onError(String errorMessage);
    }
}