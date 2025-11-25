package edu.sjsu.android.servicesfinder.controller;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
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
        database.getAllProvidersWithServices(context, new ProviderServiceDatabase.OnProvidersWithServicesLoadedListener() {

            @Override
            public void onSuccess(Map<Provider, List<ProviderService>> providerServiceMap) {
                cachedData = providerServiceMap;

                FirestoreStringTranslator translator = FirestoreStringTranslator.get(context);
                int providerIndex = 0;
                for (Map.Entry<Provider, List<ProviderService>> entry : providerServiceMap.entrySet()) {
                    Provider provider = entry.getKey();
                    List<ProviderService> services = entry.getValue();

                    providerIndex++;

                    int serviceIndex = 0;
                    for (ProviderService service : services) {
                        serviceIndex++;

                        String originalTitle = service.getServiceTitle();
                        String originalCategory = service.getCategory();

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

                        }
                    }
                }
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
    public void filterByCategory1(String category) {
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




    public void filterByCategory(String category) {

        Log.e("CAT_FILTER", "Filtering for category: " + category);

        database.getAllProvidersWithServices(context, new ProviderServiceDatabase.OnProvidersWithServicesLoadedListener() {
            @Override
            public void onSuccess(Map<Provider, List<ProviderService>> providerServiceMap) {

                Map<Provider, List<ProviderService>> finalFilteredMap = new HashMap<>();

                for (Map.Entry<Provider, List<ProviderService>> entry : providerServiceMap.entrySet()) {
                    Provider provider = entry.getKey();
                    List<ProviderService> services = entry.getValue();

                    List<ProviderService> matched = new ArrayList<>();

                    for (ProviderService service : services) {

                        String rawCat = service.getCategory();
                        if (rawCat == null) continue;

                        String[] segments = rawCat.split("\\|");
                        for (String seg : segments) {
                            seg = seg.trim();

                            // check if segment starts with category and has services after :
                            if (seg.startsWith(category)) {
                                int idx = seg.indexOf(category) + category.length();

                                if (idx < seg.length() && seg.charAt(idx) == ':') {
                                    // CATEGORY HAS SERVICES — keep this service
                                    matched.add(service);
                                    break;
                                }
                            }
                        }
                    }

                    if (!matched.isEmpty()) {
                        finalFilteredMap.put(provider, matched);
                    }
                }

                Log.e("CAT_FILTER", "FINAL filtered providers = " + finalFilteredMap.size());

                if (listener != null) {
                    if (finalFilteredMap.isEmpty()) listener.onNoDataAvailable();
                    else listener.onProvidersWithServicesLoaded(finalFilteredMap);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (listener != null) listener.onError(errorMessage);
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