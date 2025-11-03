package edu.sjsu.android.servicesfinder.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "services")
public class Service {
    @PrimaryKey(autoGenerate = true)
    private int serviceId;
    private String name;
    private String description;
    private String price;
    private int catalogueId;

    // No-arg constructor for Room (required)
    public Service() {}

    // Optional constructor for easier instantiation in code
    @Ignore
    public Service(int serviceId, String name, String description, String price, int catalogueId) {
        this.serviceId = serviceId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.catalogueId = catalogueId;
    }

    // Getters and setters
    public int getServiceId() {return serviceId; }
    public void setServiceId(int serviceId) {this.serviceId = serviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public int getCatalogueId() { return catalogueId; }
    public void setCatalogueId(int catalogueId) { this.catalogueId = catalogueId; }
}
