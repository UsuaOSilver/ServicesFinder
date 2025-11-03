package edu.sjsu.android.servicesfinder.database;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * StorageHelper
 *
 * This helper class is responsible ONLY for:
 *  - Uploading image files to Firebase Storage
 *  - Returning a download URL when finished
 *  - Handling storage-related error messages
 *
 * Beginners should note:
 *  - Firebase Storage is for binary files (photos, videos, PDFs)
 *  - Firestore is for structured object data
 *  - We store ONLY the URL of a photo in Firestore, NOT the photo itself
 */
public class StorageHelper {

    private static final String TAG = "StorageHelper";

    // Root reference to Firebase Storage bucket
    private static final StorageReference storageRef =
            FirebaseStorage.getInstance().getReference();

    /**
     * Uploads an image file to Firebase Storage.
     *
     * @param context      Activity context for UI dialogs
     * @param imageUri     The file path on device (camera/gallery)
     * @param callback     Returns download URL (String) OR null on failure
     */
    public static void uploadImageToFirebase(Context context, Uri imageUri,
                                             OnSuccessListener<String> callback) {

        if (imageUri == null) {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show();
            callback.onSuccess(null);
            return;
        }

        // Show progress dialog
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage("Uploading image...");
        dialog.setCancelable(false);
        dialog.show();

        // File path: /service_images/userid/timestamp.jpg
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String fileName = "service_images/" + userId + "/" + System.currentTimeMillis() + ".jpg";

        // Build reference inside Firebase Storage
        StorageReference fileRef = storageRef.child(fileName);

        // Begin upload
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {

                    // Now retrieve the final download URL
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        dialog.dismiss();
                        Toast.makeText(context, "Image uploaded!", Toast.LENGTH_SHORT).show();
                        callback.onSuccess(downloadUri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();

                    // Translate technical message to friendly text
                    String errorMsg = getImageUploadErrorMessage(e);

                    new AlertDialog.Builder(context)
                            .setTitle("Upload Failed")
                            .setMessage(errorMsg + "\n\nContinue without image?")
                            .setPositiveButton("Continue", (d, w) -> callback.onSuccess(null))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred())
                            / snapshot.getTotalByteCount();
                    dialog.setMessage("Uploading image... " + (int) progress + "%");
                });
    }

    /**
     * Converts confusing Firebase exceptions into friendly sentences.
     */
    private static String getImageUploadErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) return "Unknown upload error";

        if (message.contains("permission") || message.contains("PERMISSION_DENIED"))
            return "Permission denied. Please check Firebase Storage rules.";

        if (message.contains("network") || message.contains("UNAVAILABLE"))
            return "Network error. Check internet connection.";

        if (message.contains("quota"))
            return "Storage quota exceeded.";

        if (message.contains("unauthorized") || message.contains("UNAUTHENTICATED"))
            return "Not authorized. Please sign in again.";

        return "Upload failed: " + message;
    }
}
