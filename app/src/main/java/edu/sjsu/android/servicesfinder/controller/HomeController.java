package edu.sjsu.android.servicesfinder.controller;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import edu.sjsu.android.servicesfinder.database.ProviderServiceDatabase;
import edu.sjsu.android.servicesfinder.model.Provider;
import edu.sjsu.android.servicesfinder.model.ProviderService;

//* ******************************************************************************************
//* HOMECONTROLLER - BUSINESS LOGIC FOR HOME SCREEN
//* HANDLES LOADING, SEARCHING, AND FILTERING PROVIDERS WITH SERVICES
//*******************************************************************************************
public class HomeController {

    private static final String TAG = "HomeController";
    private final ProviderServiceDatabase database;
    private HomeControllerListener listener;

    // Cache for search optimization
    private Map<Provider, List<ProviderService>> cachedData;
    private String lastSearchQuery = "";

    public void setListener(HomeControllerListener listener) {
        this.listener = listener;
    }

    private final Context context;

    // Constructor
    public HomeController(Context context) {
        this.context = context;
        this.database = new ProviderServiceDatabase();
    }

    //* ****************************************************************
    //* Load all providers with their services
    // *****************************************************************
    public void loadAllProvidersWithServices1() {
        database.getAllProvidersWithServices(context, new ProviderServiceDatabase.OnProvidersWithServicesLoadedListener() {

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
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    public void loadAllProvidersWithServices() {
        Log.e("TRACE", "════════════════════════════════════════════════════════");
        Log.e("TRACE", "START: loadAllProvidersWithServices() called");
        Log.e("TRACE", "════════════════════════════════════════════════════════");

        database.getAllProvidersWithServices(context, new ProviderServiceDatabase.OnProvidersWithServicesLoadedListener() {

            @Override
            public void onSuccess(Map<Provider, List<ProviderService>> providerServiceMap) {
                cachedData = providerServiceMap;

                Log.e("TRACE", "");
                Log.e("TRACE", "────────────────────────────────────────────────────────");
                Log.e("TRACE", "DATABASE SUCCESS: Received " + providerServiceMap.size() + " providers");
                Log.e("TRACE", "────────────────────────────────────────────────────────");

                FirestoreStringTranslator translator = FirestoreStringTranslator.get(context);

                Log.e("TRACE", "TRANSLATION PIPELINE: Starting...");
                Log.e("TRACE", "");

                int providerIndex = 0;
                for (Map.Entry<Provider, List<ProviderService>> entry : providerServiceMap.entrySet()) {
                    Provider provider = entry.getKey();
                    List<ProviderService> services = entry.getValue();

                    providerIndex++;
                    Log.e("TRACE", "┌─ Provider #" + providerIndex + " ─────────────────────────");
                    Log.e("TRACE", "│  ID: " + provider.getId());
                    Log.e("TRACE", "│  Name: " + provider.getFullName());
                    Log.e("TRACE", "│  Services count: " + services.size());
                    Log.e("TRACE", "└─────────────────────────────────────────────────");

                    int serviceIndex = 0;
                    for (ProviderService service : services) {
                        serviceIndex++;

                        String originalTitle = service.getServiceTitle();
                        String originalCategory = service.getCategory();

                        Log.e("TRACE", "");
                        Log.e("TRACE", "  ▼ Service #" + serviceIndex + " BEFORE translation:");
                        Log.e("TRACE", "    • Title: \"" + originalTitle + "\" (custom name - not translated)");
                        Log.e("TRACE", "    • Category: \"" + originalCategory + "\"");

                        // DON'T translate service title - it's a custom name entered by the provider
                        // if (originalTitle != null) {
                        //     String localizedTitle = translator.translateServiceNameToLocal(originalTitle);
                        //     service.setServiceTitle(localizedTitle);
                        // }

                        if (originalCategory != null) {
                            // Use translateCategory() instead of translateCategoryName()
                            // because it handles legacy format: "Category1 | Category2: Service1, Service2"
                            String localizedCategory = translator.translateCategory(originalCategory);
                            service.setCategory(localizedCategory);

                            boolean catChanged = !originalCategory.equals(localizedCategory);
                            Log.e("TRACE", "    → Translated Category: \"" + localizedCategory + "\" " +
                                    (catChanged ? "✓ CHANGED" : "✗ UNCHANGED"));
                        }

                        Log.e("TRACE", "");
                    }
                }

                Log.e("TRACE", "");
                Log.e("TRACE", "════════════════════════════════════════════════════════");
                Log.e("TRACE", "TRANSLATION COMPLETE - Forwarding to UI listener");
                Log.e("TRACE", "════════════════════════════════════════════════════════");
                Log.e("TRACE", "");

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
                Log.e("TRACE", "");
                Log.e("TRACE", "════════════════════════════════════════════════════════");
                Log.e("TRACE", "ERROR loading providers: " + errorMessage);
                Log.e("TRACE", "════════════════════════════════════════════════════════");

                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    ///* ****************************************************************
    //* Search providers and services
    //*****************************************************************
    public void searchProvidersAndServices(String query) {
        lastSearchQuery = query;

        if (query == null || query.trim().isEmpty()) {
            loadAllProvidersWithServices();
            return;
        }

        database.searchProvidersAndServices(context, query, new ProviderServiceDatabase.OnProvidersWithServicesLoadedListener() {
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
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    //* ****************************************************************
    //* Filter by category
    //*****************************************************************
    public void filterByCategory(String category) {
        database.getProvidersByCategory(context, category, new ProviderServiceDatabase.OnProvidersWithServicesLoadedListener() {
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
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    //**********************************************************************************************
    // * Extract the provider category/services from a translated all-strings
    //**********************************************************************************************
    public String extractProviderCategoryWithServices(String categoryString) {
        if (categoryString == null || categoryString.isEmpty()) {
            return "";
        }

        // Split by "|" to get all category segments
        String[] segments = categoryString.split("\\|");

        List<String> categoriesWithServices = new ArrayList<>();

        // Find ALL segments that contain ":" (meaning they have services)
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.contains(":")) {
                // This category has services - add it to the list
                categoriesWithServices.add(trimmed);
            }
        }

        // Join all categories with services using " | "
        if (!categoriesWithServices.isEmpty()) {
            return String.join(" | ", categoriesWithServices);
        }

        // If no category has services, return the first one
        return segments.length > 0 ? segments[0].trim() : categoryString;
    }

    // =========================================================
    // LISTENER INTERFACE
    // =========================================================

    public interface HomeControllerListener {

        // Called when providers with services are loaded
        void onProvidersWithServicesLoaded(Map<Provider, List<ProviderService>> providerServiceMap);

        // Called when search results are loaded
        void onSearchResultsLoaded(Map<Provider, List<ProviderService>> providerServiceMap, String query);

        // Called when search returns no results
        void onSearchResultsEmpty(String query);

        // Called when provider details are loaded
        void onProviderDetailsLoaded(Provider provider, List<ProviderService> services);

        // Called when no data is available
        void onNoDataAvailable();

        // Called when an error occurs
        void onError(String errorMessage);
    }
}