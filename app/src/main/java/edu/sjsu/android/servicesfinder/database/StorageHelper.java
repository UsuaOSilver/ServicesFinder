package edu.sjsu.android.servicesfinder.database;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import edu.sjsu.android.servicesfinder.R;

/* *****************************************************************************************************
 * StorageHelper
 *
 * Handles:
 *  - Uploading image files to Firebase Storage
 *  - Image compression to reduce file size and bandwidth
 *  - Returning a download URL when finished
 *  - Translating errors into user-friendly messages
 **********************************************************************************************************/
public class StorageHelper {

    private static final String TAG = "StorageHelper";

    // Image compression settings
    private static final int MAX_IMAGE_WIDTH = 1920;
    private static final int MAX_IMAGE_HEIGHT = 1080;
    private static final int JPEG_QUALITY = 85; // 0-100, higher = better quality but larger size

    // Root reference to Firebase Storage bucket
    private static final StorageReference storageRef =
            FirebaseStorage.getInstance().getReference();

    // =======================================================================================
    // Uploads an image file to Firebase Storage with compression.
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

        // Compress image in background thread
        new Thread(() -> {
            try {
                byte[] compressedData = compressImage(context, imageUri);

                // Upload compressed image on main thread
                context.getMainLooper().getQueue().addIdleHandler(() -> {
                    uploadCompressedImage(context, compressedData, providerId, callback, dialog);
                    return false;
                });
            } catch (Exception e) {
                context.getMainLooper().getQueue().addIdleHandler(() -> {
                    dialog.dismiss();
                    String errorMsg = "Failed to compress image: " + e.getMessage();
                    new AlertDialog.Builder(context)
                            .setTitle(context.getString(R.string.dialog_title_upload_failed))
                            .setMessage(context.getString(R.string.dialog_message_continue_without_image, errorMsg))
                            .setPositiveButton(context.getString(R.string.action_continue), (d, w) -> callback.onSuccess(null))
                            .setNegativeButton(context.getString(R.string.action_cancel), null)
                            .show();
                    return false;
                });
            }
        }).start();
    }

    // =======================================================================================
    // Compresses image to reduce file size before upload
    // =======================================================================================
    private static byte[] compressImage(Context context, Uri imageUri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
        if (inputStream != null) inputStream.close();

        if (originalBitmap == null) {
            throw new Exception("Failed to decode image");
        }

        // Calculate new dimensions while maintaining aspect ratio
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        float scale = Math.min(
                (float) MAX_IMAGE_WIDTH / width,
                (float) MAX_IMAGE_HEIGHT / height
        );

        // Only scale down if image is larger than max dimensions
        if (scale < 1.0f) {
            width = Math.round(width * scale);
            height = Math.round(height * scale);
        }

        // Resize bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true);
        originalBitmap.recycle(); // Free memory

        // Compress to JPEG with quality setting
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
        resizedBitmap.recycle(); // Free memory

        return outputStream.toByteArray();
    }

    // =======================================================================================
    // Uploads compressed image bytes to Firebase Storage
    // =======================================================================================
    private static void uploadCompressedImage(Context context, byte[] imageData,
                                              String providerId, OnSuccessListener<String> callback,
                                              ProgressDialog dialog) {
        // File path: /service_images/{providerId}/{timestamp}.jpg
        String fileName = "service_images/" + providerId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference fileRef = storageRef.child(fileName);

        // Upload compressed byte array
        UploadTask uploadTask = fileRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
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
