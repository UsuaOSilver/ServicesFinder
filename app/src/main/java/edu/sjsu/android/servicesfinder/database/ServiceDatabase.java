package edu.sjsu.android.servicesfinder.database;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.sjsu.android.servicesfinder.model.Service;

/**
 * Database class for READ-ONLY access to hardcoded services
 * Handles all Firestore read operations for services
 */
public class ServiceDatabase {
    private final FirebaseFirestore db;

    public ServiceDatabase() {
        this.db = FirebaseFirestore.getInstance();
    }

}