package edu.sjsu.android.servicesfinder.database;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import edu.sjsu.android.servicesfinder.R;

/* *****************************************************************************************************
 * StorageHelper
 *
 * Handles:
 *  - Uploading image files to Firebase Storage
 *  - Returning a download URL when finished
 *  - Translating errors into user-friendly messages
 **********************************************************************************************************/
public class StorageHelper {

    private static final String TAG = "StorageHelper";

    // Root reference to Firebase Storage bucket
    private static final StorageReference storageRef =
            FirebaseStorage.getInstance().getReference();

    // =======================================================================================
    // Uploads an image file to Firebase Storage.
    // =======================================================================================

    public static void uploadImageToFirebase(Context context, Uri imageUri,
                                             String providerId, OnSuccessListener<String> callback) {

        if (imageUri == null) {
            Toast.makeText(context, context.getString(R.string.error_no_image_selected), Toast.LENGTH_SHORT).show();
            callback.onSuccess(null);
            return;
        }

        // Show progress dialog
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.message_uploading_image));
        dialog.setCancelable(false);
        dialog.show();

        // File path: /service_images/{providerId}/{timestamp}.jpg
        String fileName = "service_images/" + providerId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference fileRef = storageRef.child(fileName);

        // Begin upload
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        dialog.dismiss();
                        Toast.makeText(context, context.getString(R.string.success_image_uploaded), Toast.LENGTH_SHORT).show();
                        callback.onSuccess(downloadUri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    String errorMsg = getImageUploadErrorMessage(context, e);
                    new AlertDialog.Builder(context)
                            .setTitle(context.getString(R.string.dialog_title_upload_failed))
                            .setMessage(context.getString(R.string.dialog_message_continue_without_image, errorMsg))
                            .setPositiveButton(context.getString(R.string.action_continue), (d, w) -> callback.onSuccess(null))
                            .setNegativeButton(context.getString(R.string.action_cancel), null)
                            .show();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred())
                            / snapshot.getTotalByteCount();
                    dialog.setMessage(context.getString(R.string.message_uploading_image_progress, (int) progress));
                });
    }

    // =======================================================================================
    // Converts confusing Firebase exceptions into friendly sentences.
    // =======================================================================================
    private static String getImageUploadErrorMessage(Context context, Exception e) {
        String message = e.getMessage();
        if (message == null) return context.getString(R.string.error_upload_unknown);
        if (message.contains("permission") || message.contains("PERMISSION_DENIED"))
            return context.getString(R.string.error_upload_permission_denied);
        if (message.contains("network") || message.contains("UNAVAILABLE"))
            return context.getString(R.string.error_upload_network);
        if (message.contains("quota"))
            return context.getString(R.string.error_upload_quota);
        if (message.contains("unauthorized") || message.contains("UNAUTHENTICATED"))
            return context.getString(R.string.error_upload_unauthorized);
        return context.getString(R.string.error_upload_failed, message);
    }

}
