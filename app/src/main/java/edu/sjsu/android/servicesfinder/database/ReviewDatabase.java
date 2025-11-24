package edu.sjsu.android.servicesfinder.database;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.sjsu.android.servicesfinder.model.Review;

public class ReviewDatabase {
    private static final String TAG = "ReviewDatabase";
    private final FirebaseFirestore db;

    public ReviewDatabase() {
        this.db = FirestoreHelper.getInstance();
    }

    // =========================================================
    // SAVE REVIEW
    // =========================================================
    public void saveReview(Review review, OnReviewSaveListener listener) {
        db.collection("reviews")
                .add(review)
                .addOnSuccessListener(docRef -> {
                    review.setId(docRef.getId());
                    listener.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving review", e);
                    listener.onError(e.getMessage());
                });
    }

    // =========================================================
    // LOAD REVIEWS FOR A PROVIDER
    // =========================================================
    public void getReviewsForProvider(String providerId, OnReviewsLoadedListener listener) {
        Log.e("DEBUG_REVIEW", "Querying reviews for providerId = " + providerId);
        db.collection("reviews")
                .whereEqualTo("providerId", providerId)
                //.whereEqualTo("status", "Active")
                //.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Log.e("DEBUG_REVIEW", "Retrieved doc: " + doc.getData());
                        Review review = doc.toObject(Review.class);
                        review.setId(doc.getId());
                        reviews.add(review);
                    }
                    Log.e("DEBUG_REVIEW", "Fetched " + reviews.size() + " reviews from DB");
                    listener.onReviewsLoaded(reviews);
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG_REVIEW", "Error loading reviews", e);
                    listener.onError(e.getMessage());
                });
    }

    // =========================================================
    // CALCULATE AVERAGE RATING
    // =========================================================
    public void getAverageRating(String providerId, OnRatingCalculatedListener listener) {
        db.collection("reviews")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("status", "Active")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        listener.onRatingCalculated(0.0f, 0);
                        return;
                    }

                    float totalRating = 0;
                    int count = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Review review = doc.toObject(Review.class);
                        totalRating += review.getRating();
                        count++;
                    }

                    float average = totalRating / count;
                    listener.onRatingCalculated(average, count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error calculating rating", e);
                    listener.onError(e.getMessage());
                });
    }

    // =========================================================
    // CALLBACK INTERFACES
    // =========================================================
    public interface OnReviewSaveListener {
        void onSuccess(String reviewId);
        void onError(String error);
    }

    public interface OnReviewsLoadedListener {
        void onReviewsLoaded(List<Review> reviews);
        void onError(String error);
    }

    public interface OnRatingCalculatedListener {
        void onRatingCalculated(float averageRating, int totalReviews);
        void onError(String error);
    }
}