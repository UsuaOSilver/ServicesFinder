package edu.sjsu.android.servicesfinder.controller;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.sjsu.android.servicesfinder.database.ProviderServiceDatabase;
import edu.sjsu.android.servicesfinder.model.ProviderService;

public class ProviderServiceController {

    private final ProviderServiceDatabase database;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public ProviderServiceController() {
        this.database = new ProviderServiceDatabase();
    }

    // ------------------ CREATE / UPDATE ------------------
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
            database.updateService(providerId, service.getId(), service, listener);
        } else {
            database.saveService(providerId, service, listener);
        }
    }

    // ------------------ LOAD LAST DRAFT ------------------
    public void loadLastServiceDraft(String providerId, OnDraftLoadedListener listener) {
        if (providerId == null || providerId.trim().isEmpty()) {
            listener.onError("Missing providerId");
            return;
        }

        firestore.collection("providers")
                .document(providerId)
                .collection("services")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query == null || query.isEmpty()) {
                        listener.onNoDraftFound();
                        return;
                    }

                    DocumentSnapshot doc = query.getDocuments().get(0);

                    ServiceDraft draft = new ServiceDraft();
                    draft.setId(doc.getId());
                    draft.setServiceTitle(doc.getString("serviceTitle"));
                    draft.setDescription(doc.getString("description"));
                    draft.setPricing(doc.getString("pricing"));
                    draft.setServiceArea(doc.getString("serviceArea"));
                    draft.setAvailability(doc.getString("availability"));
                    draft.setContactPreference(doc.getString("contactPreference"));
                    draft.setCategory(doc.getString("category"));
                    draft.setImageUrl(doc.getString("imageUrl"));

                    listener.onDraftLoaded(draft);
                })
                .addOnFailureListener(e -> listener.onError("Failed to load draft: " + e.getMessage()));
    }
    /* ****************************************************************************************
    LOAD SERVICE AREAS
    Loads all service areas from Firestore -> returns a sorted list of area names
    ****************************************************************************************/
    public void loadServiceAreas(ServiceAreaListener listener) {
        firestore.collection("service_areas")
                .get()
                .addOnSuccessListener(query -> {
                    List<String> areas = new ArrayList<>();
                    if (query != null) {
                        for (DocumentSnapshot d : query.getDocuments()) {
                            areas.add(d.getId());
                        }
                    }
                    Collections.sort(areas);
                    listener.onLoaded(areas);
                })
                .addOnFailureListener(e -> listener.onError("Failed to load areas: " + e.getMessage()));
    }

    // ================== DATA CLASS + CALLBACKS ==================

    public static class ServiceDraft {
        private String id;
        private String serviceTitle;
        private String description;
        private String pricing;
        private String serviceArea;
        private String availability;
        private String contactPreference;
        private String category;
        private String imageUrl;

        public String getId() { return id; }
        public String getServiceTitle() { return serviceTitle; }
        public String getDescription() { return description; }
        public String getPricing() { return pricing; }
        public String getServiceArea() { return serviceArea; }
        public String getAvailability() { return availability; }
        public String getContactPreference() { return contactPreference; }
        public String getCategory() { return category; }
        public String getImageUrl() { return imageUrl; }

        public void setId(String id) { this.id = id; }
        public void setServiceTitle(String serviceTitle) { this.serviceTitle = serviceTitle; }
        public void setDescription(String description) { this.description = description; }
        public void setPricing(String pricing) { this.pricing = pricing; }
        public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
        public void setAvailability(String availability) { this.availability = availability; }
        public void setContactPreference(String contactPreference) { this.contactPreference = contactPreference; }
        public void setCategory(String category) { this.category = category; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    public interface OnDraftLoadedListener {
        void onDraftLoaded(ServiceDraft draft);
        void onNoDraftFound();
        void onError(String error);
    }

    /** Callback for service areas list */
    public interface ServiceAreaListener {
        void onLoaded(List<String> areas);
        void onError(String message);
    }
}
