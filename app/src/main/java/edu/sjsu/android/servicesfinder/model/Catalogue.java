package edu.sjsu.android.servicesfinder.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "catalogues")
public class Catalogue {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;

    public Catalogue() {}

    @Ignore
    public Catalogue(String title) {
        this.title = title;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
