package edu.sjsu.android.servicesfinder.controller;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    /**
     * Load specific provider with services
     */
    public void loadProviderWithServices(String providerId) {
        database.getProviderWithServices(providerId, new ProviderServiceDatabase.OnProviderWithServicesLoadedListener() {
            @Override
            public void onSuccess(Provider provider, List<ProviderService> services) {
                if (listener != null) {
                    listener.onProviderDetailsLoaded(provider, services);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading provider details: " + errorMessage);
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    /**
     * Sort providers by name
     */
    public List<Map.Entry<Provider, List<ProviderService>>> sortProvidersByName(
            Map<Provider, List<ProviderService>> data, boolean ascending) {

        List<Map.Entry<Provider, List<ProviderService>>> list = new ArrayList<>(data.entrySet());

        list.sort(new Comparator<Map.Entry<Provider, List<ProviderService>>>() {
            @Override
            public int compare(Map.Entry<Provider, List<ProviderService>> e1,
                               Map.Entry<Provider, List<ProviderService>> e2) {
                String name1 = e1.getKey().getFullName() != null ? e1.getKey().getFullName() : "";
                String name2 = e2.getKey().getFullName() != null ? e2.getKey().getFullName() : "";
                return ascending ? name1.compareTo(name2) : name2.compareTo(name1);
            }
        });

        return list;
    }

    /**
     * Sort providers by service count
     */
    public List<Map.Entry<Provider, List<ProviderService>>> sortProvidersByServiceCount(
            Map<Provider, List<ProviderService>> data, boolean ascending) {

        List<Map.Entry<Provider, List<ProviderService>>> list = new ArrayList<>(data.entrySet());

        list.sort(new Comparator<Map.Entry<Provider, List<ProviderService>>>() {
            @Override
            public int compare(Map.Entry<Provider, List<ProviderService>> e1,
                               Map.Entry<Provider, List<ProviderService>> e2) {
                int count1 = e1.getValue().size();
                int count2 = e2.getValue().size();
                return ascending ? Integer.compare(count1, count2) : Integer.compare(count2, count1);
            }
        });

        return list;
    }

    /**
     * Format provider summary
     */
    public String formatProviderSummary(Provider provider, List<ProviderService> services) {
        StringBuilder summary = new StringBuilder();

        summary.append(provider.getFullName());

        if (services != null && !services.isEmpty()) {
            summary.append(" • ").append(services.size());
            summary.append(services.size() == 1 ? " service" : " services");
        }

        if (provider.getAddress() != null && !provider.getAddress().isEmpty()) {
            // Extract city from address (assuming format: "Street, City, State")
            String[] parts = provider.getAddress().split(",");
            if (parts.length >= 2) {
                summary.append(" • ").append(parts[1].trim());
            }
        }

        return summary.toString();
    }

    /**
     * Format service summary
     */
    public String formatServiceSummary(ProviderService service) {
        StringBuilder summary = new StringBuilder();

        summary.append(service.getServiceTitle());

        if (service.getPricing() != null && !service.getPricing().isEmpty()) {
            summary.append(" • ").append(service.getPricing());
        }

        if (service.getServiceArea() != null && !service.getServiceArea().isEmpty()) {
            summary.append(" • ").append(service.getServiceArea());
        }

        return summary.toString();
    }

    /**
     * Get total service count
     */
    public int getTotalServiceCount(Map<Provider, List<ProviderService>> data) {
        int count = 0;
        for (List<ProviderService> services : data.values()) {
            count += services.size();
        }
        return count;
    }

    /**
     * Get last search query
     */
    public String getLastSearchQuery() {
        return lastSearchQuery;
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        cachedData = null;
        lastSearchQuery = "";
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