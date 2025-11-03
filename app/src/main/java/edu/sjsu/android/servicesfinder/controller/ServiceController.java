package edu.sjsu.android.servicesfinder.controller;

import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import edu.sjsu.android.servicesfinder.database.ServiceDatabase;
import edu.sjsu.android.servicesfinder.model.Service;

/**
 * Controller for Service business logic
 * Handles service queries, filtering, and formatting
 */
public class ServiceController {

    private final ServiceDatabase serviceDatabase;
    private ServiceControllerListener listener;

    public ServiceController() {
        this.serviceDatabase = new ServiceDatabase();
    }

    public void setListener(ServiceControllerListener listener) {
        this.listener = listener;
    }

    /**
     * Load all services
     */
    public void loadAllServices() {
        serviceDatabase.getAllServices(new ServiceDatabase.OnServicesLoadedListener() {
            @Override
            public void onSuccess(List<Service> services) {
                if (listener != null) {
                    listener.onServicesLoaded(services);
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
     * Load services by catalogue
     */
    public void loadServicesByCatalogue(int catalogueId) {
        serviceDatabase.getServicesByCatalogue(catalogueId, new ServiceDatabase.OnServicesLoadedListener() {
            @Override
            public void onSuccess(List<Service> services) {
                if (listener != null) {
                    listener.onServicesLoaded(services);
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
     * Load specific service by ID
     */
    public void loadServiceById(int serviceId) {
        serviceDatabase.getServiceById(serviceId, new ServiceDatabase.OnServiceLoadedListener() {
            @Override
            public void onSuccess(Service service) {
                if (listener != null) {
                    listener.onServiceLoaded(service);
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
     * Search services by name
     */
    public void searchServices(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadAllServices();
            return;
        }

        serviceDatabase.searchServicesByName(query, new ServiceDatabase.OnServicesLoadedListener() {
            @Override
            public void onSuccess(List<Service> services) {
                if (listener != null) {
                    listener.onServicesLoaded(services);
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
     * Get Firestore query for all services (for RecyclerView)
     */
    public Query getServicesQuery() {
        return serviceDatabase.getServicesQuery();
    }

    /**
     * Get Firestore query for services by catalogue (for RecyclerView)
     */
    public Query getServicesByCatalogueQuery(int catalogueId) {
        return serviceDatabase.getServicesByCatalogueQuery(catalogueId);
    }

    /**
     * Filter services by price range
     */
    public List<Service> filterByPriceRange(List<Service> services, double minPrice, double maxPrice) {
        List<Service> filtered = new ArrayList<>();

        for (Service service : services) {
            double price = parsePriceFromString(service.getPrice());
            if (price >= minPrice && price <= maxPrice) {
                filtered.add(service);
            }
        }

        return filtered;
    }

    /**
     * Sort services by name
     */
    public List<Service> sortByName(List<Service> services, boolean ascending) {
        List<Service> sorted = new ArrayList<>(services);

        sorted.sort((s1, s2) -> {
            String name1 = s1.getName() != null ? s1.getName() : "";
            String name2 = s2.getName() != null ? s2.getName() : "";

            return ascending ? name1.compareTo(name2) : name2.compareTo(name1);
        });

        return sorted;
    }

    /**
     * Sort services by price
     */
    public List<Service> sortByPrice(List<Service> services, boolean ascending) {
        List<Service> sorted = new ArrayList<>(services);

        sorted.sort((s1, s2) -> {
            double price1 = parsePriceFromString(s1.getPrice());
            double price2 = parsePriceFromString(s2.getPrice());

            return ascending ? Double.compare(price1, price2) : Double.compare(price2, price1);
        });

        return sorted;
    }

    /**
     * Format service for display (summary)
     */
    public String formatServiceSummary(Service service) {
        if (service == null) {
            return "Unknown Service";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(service.getName());

        if (service.getPrice() != null && !service.getPrice().isEmpty()) {
            summary.append(" - ").append(service.getPrice());
        }

        return summary.toString();
    }

    /**
     * Format multiple services for display
     */
    public String formatServicesCount(List<Service> services) {
        if (services == null || services.isEmpty()) {
            return "No services available";
        }

        if (services.size() == 1) {
            return "1 service available";
        }

        return services.size() + " services available";
    }

    /**
     * Validate service data
     */
    public boolean validateService(Service service) {
        if (service == null) {
            return false;
        }

        return service.getName() != null && !service.getName().trim().isEmpty()
                && service.getDescription() != null && !service.getDescription().trim().isEmpty()
                && service.getPrice() != null && !service.getPrice().trim().isEmpty();
    }

    /**
     * Parse price from string (e.g., "$50/hour" -> 50.0)
     */
    private double parsePriceFromString(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            return 0.0;
        }

        try {
            // Remove currency symbols and text
            String numericPart = priceStr.replaceAll("[^0-9.]", "");
            return Double.parseDouble(numericPart);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Listener interface for callbacks to View
     */
    public interface ServiceControllerListener {
        void onServicesLoaded(List<Service> services);
        void onServiceLoaded(Service service);
        void onError(String errorMessage);
    }
}