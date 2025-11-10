package edu.sjsu.android.servicesfinder.controller;

import edu.sjsu.android.servicesfinder.database.ProviderServiceDatabase;
import edu.sjsu.android.servicesfinder.model.ProviderService;

/**
 * ProviderServiceController
 *
 * Acts as the middle layer between the View (ProviderDashboardActivity)
 * and the Database (ProviderServiceDatabase).
 *
 * Responsibilities:
 *  - Validate service data
 *  - Decide whether to create or update
 *  - Delegate saving/updating to the database layer
 */
public class ProviderServiceController {

    private final ProviderServiceDatabase database;

    public ProviderServiceController() {
        this.database = new ProviderServiceDatabase();
    }

    public void saveOrUpdateService(String providerId,
                                    ProviderService service,
                                    ProviderServiceDatabase.OnServiceSaveListener listener) {

        if (providerId == null || providerId.isEmpty()) {
            listener.onError("Provider not signed in");
            return;
        }

        if (service.getServiceTitle() == null || service.getServiceTitle().isEmpty()) {
            listener.onError("Service title required");
            return;
        }

        if (service.getId() != null && !service.getId().isEmpty()) {
            // Update existing service
            database.updateService(providerId, service.getId(), service, listener);
        } else {
            // Create new service
            database.saveService(providerId, service, listener);
        }
    }
}
