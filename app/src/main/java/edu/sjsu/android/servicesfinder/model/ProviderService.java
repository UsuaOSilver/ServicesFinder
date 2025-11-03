package edu.sjsu.android.servicesfinder.model;

public class ProviderService {
    private String id;
    private String providerId;
    private String serviceTitle;
    private String description;
    private String pricing;
    private String category;
    private String serviceArea;
    private String availability;
    private String contactPreference;
    private String imageUrl;
    private long timestamp;

    public ProviderService() {}

    // getters & setters ...

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getServiceTitle() { return serviceTitle; }
    public void setServiceTitle(String serviceTitle) { this.serviceTitle = serviceTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPricing() { return pricing; }
    public void setPricing(String pricing) { this.pricing = pricing; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getContactPreference() { return contactPreference; }
    public void setContactPreference(String contactPreference) { this.contactPreference = contactPreference; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
