package edu.sjsu.android.servicesfinder.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.*;

import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.controller.CatalogueController;
import edu.sjsu.android.servicesfinder.database.StorageHelper;
import edu.sjsu.android.servicesfinder.model.Catalogue;
import edu.sjsu.android.servicesfinder.model.Service;

/**
 * ProviderDashboardActivity
 *
 * This screen is displayed after a provider signs in.
 *
 * Responsibilities:
 *  - Allow the provider to create and save a service offering.
 *  - Upload a service image to Firebase Storage
 *  - Save service details to Firebase Firestore.
 *  - Let the user pick categories and sub-services using a MultiSelectDropdown popup.
 *  - Allow selecting availability (Mon–Sun).
 *  - Allow choosing preferred contact  (Call/Text/Email).
 *  - Load previous “draft” automatically when signIn
 *
 * Firebase components used:
 *      Firestore → stores structured service data
 *      Storage   → stores uploaded image files
 */
public class ProviderDashboardActivity extends AppCompatActivity
        implements CatalogueController.CatalogueControllerListener {

    private static final String TAG = "ProviderDashboard";

    // UI Components
    private ImageView imagePreview;
    private TextInputEditText serviceTitleInput, descriptionInput, pricingInput;
    private TextView catalogueTextView;
    private Spinner serviceAreaSpinner;
    private RadioGroup contactPreferenceGroup;
    private ProgressDialog loadingDialog;
    private CheckBox mon, tue, wed, thu, fri, sat, sun;

    // Controllers and Firebase
    private CatalogueController catalogueController;
    private FirebaseFirestore firestore;
    private StorageReference storageRef;

    // State
    private Uri selectedImageUri, tempImageUri;
    private MultiSelectDropdown catalogueDropdown;
    private boolean cataloguesLoaded = false;
    private String editingServiceId = null;

    // Image selection launchers
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Log.d(TAG, "Image selected from gallery: " + selectedImageUri);
                    Glide.with(this).load(selectedImageUri).into(imagePreview);
                    imagePreview.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    selectedImageUri = tempImageUri;
                    Log.d(TAG, "Photo captured: " + selectedImageUri);
                    Glide.with(this).load(selectedImageUri).into(imagePreview);
                    imagePreview.setVisibility(View.VISIBLE);
                }
            });

    // =========================================================
    // LIFECYCLE METHODS
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_dashboard);
        setTitle("Provider Dashboard");

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize controller
        catalogueController = new CatalogueController();
        catalogueController.setListener(this);

        // Setup UI
        initializeViews();
        catalogueTextView.setEnabled(false);
        catalogueTextView.setText("Loading catalogues...");
        catalogueDropdown = new MultiSelectDropdown(this, catalogueTextView, new HashMap<>());

        // Load initial data
        showLoadingDialog();
        loadServiceAreas();
        loadCatalogues();
        setupButtons();

        // Set default contact preference
        ((RadioButton) findViewById(R.id.contactCall)).setChecked(true);
    }

    /**
     * initializeViews()
     *
     * This method binds each UI widget (defined in XML) to a Java variable.
     * We must call findViewById() AFTER setContentView(), otherwise they will be null.
     *
     * These references allow us to read user input, show errors, and update UI state.
     */
    private void initializeViews() {
        serviceTitleInput = findViewById(R.id.serviceTitleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        pricingInput = findViewById(R.id.pricingInput);
        catalogueTextView = findViewById(R.id.catalogueDropdown);
        imagePreview = findViewById(R.id.imagePreview);
        serviceAreaSpinner = findViewById(R.id.serviceAreaSpinner);
        contactPreferenceGroup = findViewById(R.id.contactPreferenceGroup);

        mon = findViewById(R.id.mon);
        tue = findViewById(R.id.tue);
        wed = findViewById(R.id.wed);
        thu = findViewById(R.id.thu);
        fri = findViewById(R.id.fri);
        sat = findViewById(R.id.sat);
        sun = findViewById(R.id.sun);
    }

    private void setupButtons() {
        Button uploadImageBtn = findViewById(R.id.uploadImageBtn);
        Button saveBtn = findViewById(R.id.saveBtn);
        Button cancelBtn = findViewById(R.id.cancelBtn);

        uploadImageBtn.setOnClickListener(v -> showImagePickerDialog());
        saveBtn.setOnClickListener(v -> handleSave());
        cancelBtn.setOnClickListener(v -> handleCancel());
    }

    // =========================================================
    // SAVE SERVICE
    // =========================================================
    /*
     * Called when user taps the "Save" button.
     * Steps:
     *   1. Collect all user inputs.
     *   2. Validate required fields.
     *   3. Format selected dropdown items into a category summary string.
     *   4. If an image was selected → upload to Firebase Storage.
     *   5. Save metadata to Firestore under:
     *        providers/{providerID}/services/{autoGeneratedDocID}
     *
     * The save pipeline requires asynchronous logic because:
     *   - Uploading image takes time
     *   - Saving data to Firestore is also async
     */
    private void handleSave() {
        // Get all input values
        String title = getText(serviceTitleInput);
        String description = getText(descriptionInput);
        String pricing = getText(pricingInput);
        String area = getSelectedItem(serviceAreaSpinner);
        String availability = getSelectedAvailability();
        String contactPreference = getSelectedContactPreference();
        Map<String, Set<String>> selectedItems = catalogueDropdown.getSelectedItems();
        String category = formatCategoryFromSelection(selectedItems);

        // Debug logging
        Log.d(TAG, "=== SAVE SERVICE DEBUG ===");
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Description: " + description);
        Log.d(TAG, "Pricing: " + pricing);
        Log.d(TAG, "Area: " + area);
        Log.d(TAG, "Availability: " + availability);
        Log.d(TAG, "Contact Preference: " + contactPreference);
        Log.d(TAG, "Category: " + category);
        Log.d(TAG, "Image URI: " + selectedImageUri);
        Log.d(TAG, "========================");

        // Validate inputs
        if (title.isEmpty()) {
            serviceTitleInput.setError("Required");
            Toast.makeText(this, "Please enter service title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            descriptionInput.setError("Required");
            Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pricing.isEmpty()) {
            pricingInput.setError("Required");
            Toast.makeText(this, "Please enter pricing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (area.equals("Select Service Area")) {
            Toast.makeText(this, "Please select service area", Toast.LENGTH_SHORT).show();
            return;
        }

        if (availability.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show();
            return;
        }

        if (contactPreference.isEmpty()) {
            Toast.makeText(this, "Please select contact preference", Toast.LENGTH_SHORT).show();
            return;
        }

        if (category.isEmpty()) {
            Toast.makeText(this, "Please select catalogue & services", Toast.LENGTH_SHORT).show();
            return;
        }

        // Handle image upload or save directly
        if (selectedImageUri != null) {
            Log.d(TAG, "Image selected, uploading to Firebase Storage...");
            StorageHelper.uploadImageToFirebase(this, selectedImageUri, imageUrl -> {
                Log.d(TAG, "Image upload callback received, URL: " + imageUrl);
                saveServiceToFirestore(title, description, pricing, category,
                        area, availability, contactPreference, imageUrl);
            });
        } else {
            Log.d(TAG, "No image selected, prompting user...");
            // Ask user if they want to continue without image
            new AlertDialog.Builder(this)
                    .setTitle("No Image")
                    .setMessage("Do you want to add a service without an image?")
                    .setPositiveButton("Yes, Continue", (dialog, which) -> {
                        saveServiceToFirestore(title, description, pricing, category,
                                area, availability, contactPreference, null);
                    })
                    .setNegativeButton("Add Image", (dialog, which) -> {
                        showImagePickerDialog();
                    })
                    .show();
        }

    }

        // =========================================================
        // IMAGE UPLOAD TO FIREBASE STORAGE
        // =========================================================
        /**
         * This method uploads the selected image file to Firebase Storage.
         *
         * IMPORTANT BEGINNER NOTES:
         *  • Images are stored separately from Firestore because Firestore is
         *    optimized for structured text/objects, NOT large binary data.
         *  • We only save a *download URL* (string) inside Firestore.
         *  • Upload is asynchronous — the app continues running while upload happens.
         *  • When upload finishes, we receive a callback with the final file URL.
         *
         * @param imageUri The location of the image on the device
         * @param callback Called when upload succeeds (returns URL string)
         */
        private void uploadImageToFirebase(Uri imageUri, OnSuccessListener<String> callback) {
            // Validate URI
            if (imageUri == null) {
                Log.e(TAG, "Image URI is null!");
                Toast.makeText(this, "Error: No image selected", Toast.LENGTH_SHORT).show();
                callback.onSuccess(null);
                return;
            }

            // Show upload progress
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("Uploading image...");
            dialog.setCancelable(false);
            dialog.show();

            // Create unique filename with user ID
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String fileName = "service_images/" + userId + "/" + System.currentTimeMillis() + ".jpg";
            StorageReference fileRef = storageRef.child(fileName);

            // Upload file
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Image uploaded successfully, getting download URL...");

                        // Get download URL
                        fileRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    String downloadUrl = downloadUri.toString();
                                    Log.d(TAG, "Download URL obtained: " + downloadUrl);
                                    dialog.dismiss();
                                    Toast.makeText(this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                                    callback.onSuccess(downloadUrl);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to get download URL", e);
                                    dialog.dismiss();
                                    Toast.makeText(this, "Failed to get image URL: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    callback.onSuccess(null);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Image upload failed", e);
                        dialog.dismiss();

                        // Show specific error message
                        String errorMsg = getImageUploadErrorMessage(e);
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();

                        // Ask if user wants to continue without image
                        new AlertDialog.Builder(this)
                                .setTitle("Image Upload Failed")
                                .setMessage(errorMsg + "\n\nDo you want to save the service without an image?")
                                .setPositiveButton("Yes, Continue", (d, w) -> callback.onSuccess(null))
                                .setNegativeButton("Cancel", null)
                                .show();
                    })
                    .addOnProgressListener(snapshot -> {
                        // Show upload progress
                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        dialog.setMessage("Uploading image... " + (int) progress + "%");
                    });
        }
        // =========================================================
        // GET ERROR MSG WHEN UPLOAD IMAGE
        // =========================================================
        /**
         * Firebase exceptions are technical and unfriendly. This helper
         * attempts to translate them into human-friendly messages.     *
         * Case:
         *  • permission denied → check Storage rules
         *  • network unavailable → check WiFi/mobile data
         */
        private String getImageUploadErrorMessage(Exception e) {
            String message = e.getMessage();

            if (message == null) {
                return "Unknown upload error";
            }

            if (message.contains("permission") || message.contains("PERMISSION_DENIED")) {
                return "Permission denied. Please check Firebase Storage rules.";
            } else if (message.contains("network") || message.contains("UNAVAILABLE")) {
                return "Network error. Check your internet connection.";
            } else if (message.contains("quota")) {
                return "Storage quota exceeded.";
            } else if (message.contains("unauthorized") || message.contains("UNAUTHENTICATED")) {
                return "Not authorized. Please sign in again.";
            } else {
                return "Upload failed: " + message;
            }
        }

    // =========================================================
    // SAVE SERVICE TO FIRESTORE
    // =========================================================
    /**
     * Saves structured service data (title, description, pricing, etc.)
     * into Firestore under this path:
     *
     *   providers/{providerId}/services/{autoGeneratedDocId}
     *
     * Steps:
     *  Create map of fields
     *  Attach metadata (timestamps, status, rating, etc.)
     *  Add to Firestore collection
     *  On success → clear form + exit Activity
     *  On failure → show Toast error
     */
    private void saveServiceToFirestore(String title, String description, String pricing,
                                        String category, String area, String availability,
                                        String contactPreference, String imageUrl) {

        String providerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (providerId == null) {
            Toast.makeText(this, "Error: Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "=== SAVING TO FIRESTORE ===");
        Log.d(TAG, "Provider ID: " + providerId);
        Log.d(TAG, "Image URL: " + (imageUrl != null ? imageUrl : "null (no image)"));

        ProgressDialog savingDialog = new ProgressDialog(this);
        savingDialog.setMessage("Saving service...");
        savingDialog.setCancelable(false);
        savingDialog.show();

        // Prepare the Firestore data
        Map<String, Object> serviceData = new HashMap<>();
        serviceData.put("providerId", providerId);
        serviceData.put("serviceTitle", title);
        serviceData.put("description", description);
        serviceData.put("pricing", pricing);
        serviceData.put("category", category);
        serviceData.put("serviceArea", area);
        serviceData.put("availability", availability);
        serviceData.put("contactPreference", contactPreference);
        serviceData.put("imageUrl", imageUrl != null ? imageUrl : "");
        serviceData.put("status", "Active");
        serviceData.put("verified", false);
        serviceData.put("rating", 0.0);
        serviceData.put("timestamp", System.currentTimeMillis());

        var servicesRef = firestore.collection("providers")
                .document(providerId)
                .collection("services");

        // UPDATE MODE — overwrite doc
        if (editingServiceId != null) {
            servicesRef.document(editingServiceId)
                    .set(serviceData)
                    .addOnSuccessListener(ref -> {
                        savingDialog.dismiss();
                        Toast.makeText(this, "Service updated!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        savingDialog.dismiss();
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
        // CREATE MODE — new document
        else {
            servicesRef.add(serviceData)
                    .addOnSuccessListener(ref -> {
                        savingDialog.dismiss();
                        Toast.makeText(this, "Service added successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        savingDialog.dismiss();
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }
    // =========================================================
    // IMAGE PICKER
    // =========================================================
    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Take a Photo"};

        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openGallery();
                    } else {
                        openCamera();
                    }
                })
                .show();
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(Intent.createChooser(intent, "Select a photo"));
    }

    private void openCamera() {
        tempImageUri = createTempImageUri();
        cameraLauncher.launch(tempImageUri);
    }

    private Uri createTempImageUri() {
        File file = new File(getExternalCacheDir(), System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================
    /**
     * Builds a list of days the provider selected using checkboxes.
     * We return a comma-separated string (e.g., "Mon, Tue, Fri")
     * because it is:
     *   • human-readable
     *   • easy to restore later
     */
    private String getSelectedAvailability() {
        List<String> days = new ArrayList<>();
        if (mon.isChecked()) days.add("Mon");
        if (tue.isChecked()) days.add("Tue");
        if (wed.isChecked()) days.add("Wed");
        if (thu.isChecked()) days.add("Thu");
        if (fri.isChecked()) days.add("Fri");
        if (sat.isChecked()) days.add("Sat");
        if (sun.isChecked()) days.add("Sun");
        return String.join(", ", days);
    }
    /**
     * handleCancel()
     * Shows a confirmation dialog when user taps Cancel.
     * Prevents accidental loss of input.
     */
    private void handleCancel() {
        new AlertDialog.Builder(this)
                .setTitle("Discard changes?")
                .setMessage("Are you sure you want to cancel without saving?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }
    /**
     * CATALOGUES/SERVICES DISPLAY
     * Converts a nested map structure:    Map<Category, Set<Service>>
     * into a readable text summary:   Plumbing: Pipes, Toilets | Electrical: Wiring, Outlets
     * This makes it easier to store in Firestore.
     */
    private String formatCategoryFromSelection(Map<String, Set<String>> selectedItems) {
        List<String> summary = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : selectedItems.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                summary.add(entry.getKey() + ": " + String.join(", ", entry.getValue()));
            }
        }
        return String.join(" | ", summary);
    }
    /**
     * PREFERRED CONTACT
     * RadioGroup ensures only one button is selected.
     * We return the label text of the chosen button.
     */
    private String getSelectedContactPreference() {
        int selectedId = contactPreferenceGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return "";
        RadioButton radioButton = findViewById(selectedId);
        return radioButton != null ? radioButton.getText().toString() : "";
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getSelectedItem(Spinner spinner) {
        return spinner.getSelectedItem() != null ? spinner.getSelectedItem().toString() : "";
    }
    // =========================================================
    // Resets all input fields to default state
    // =========================================================
    private void clearForm() {
        serviceTitleInput.setText("");
        descriptionInput.setText("");
        pricingInput.setText("");
        serviceAreaSpinner.setSelection(0);
        mon.setChecked(false);
        tue.setChecked(false);
        wed.setChecked(false);
        thu.setChecked(false);
        fri.setChecked(false);
        sat.setChecked(false);
        sun.setChecked(false);
        contactPreferenceGroup.clearCheck();
        ((RadioButton) findViewById(R.id.contactCall)).setChecked(true);
        selectedImageUri = null;
        imagePreview.setVisibility(View.GONE);
        catalogueTextView.setText("Select Catalogue & Services");
    }

    // =========================================================
    // LOADING DIALOGS
    // =========================================================

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setCancelable(false);
        }
        loadingDialog.setMessage("Loading catalogues...");
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    // =========================================================
    // LOAD SERVICE AREAS FROM FIREBASE
    // =========================================================

    private void loadServiceAreas() {
        firestore.collection("service_areas")
                .get()
                .addOnSuccessListener(query -> {
                    List<String> areas = new ArrayList<>();
                    for (var doc : query) {
                        areas.add(doc.getId());
                    }
                    Collections.sort(areas);
                    areas.add(0, "Select Service Area");

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, areas);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    serviceAreaSpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load service areas", e);
                    Toast.makeText(this, "Failed to load areas", Toast.LENGTH_SHORT).show();
                });
    }

    // =========================================================
    // LOAD CATALOGUES
    // =========================================================

    private void loadCatalogues() {
        catalogueController.loadCatalogueMapForDropdown();
    }

    @Override
    public void onCataloguesLoaded(List<Catalogue> catalogues) {
        // Not used - we use onCatalogueMapLoaded instead
    }

    @Override
    public void onCatalogueWithServicesLoaded(Catalogue catalogue, List<Service> services) {
        // Not used - we use onCatalogueMapLoaded instead
    }

    @Override
    public void onCatalogueMapLoaded(Map<String, List<String>> catalogueMap) {
        hideLoadingDialog();

        if (catalogueMap.isEmpty()) {
            Toast.makeText(this, "No catalogues available", Toast.LENGTH_LONG).show();
            catalogueTextView.setText("No catalogues available");
            catalogueTextView.setEnabled(false);
            return;
        }

        cataloguesLoaded = true;
        catalogueTextView.setEnabled(true);
        catalogueTextView.setText("Select Catalogue & Services");
        catalogueDropdown.updateCatalogueMap(catalogueMap);

        Toast.makeText(this, "Catalogues loaded", Toast.LENGTH_SHORT).show();

        // Load last service draft after catalogues are ready
        loadLastServiceDraft();
    }

    @Override
    public void onError(String errorMessage) {
        hideLoadingDialog();
        Log.e(TAG, "Controller error: " + errorMessage);
        Toast.makeText(this, "Error loading catalogues: " + errorMessage, Toast.LENGTH_LONG).show();
        catalogueTextView.setText("Failed to load catalogues");
        catalogueTextView.setEnabled(false);
    }

    // =========================================================
    // LOAD LAST SERVICE DRAFT
    // =========================================================

    private void loadLastServiceDraft() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d(TAG, "Loading last service draft for provider: " + uid);

        firestore.collection("providers")
                .document(uid)
                .collection("services")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Log.d(TAG, "No draft found - starting with empty form");
                        return;
                    }

                    var doc = query.getDocuments().get(0);

                    // ✅ Store the document ID so we can UPDATE it later
                    editingServiceId = doc.getId();
                    Log.d(TAG, "Draft loaded - Document ID: " + editingServiceId);

                    // === Fill text fields ===
                    serviceTitleInput.setText(doc.getString("serviceTitle"));
                    descriptionInput.setText(doc.getString("description"));
                    pricingInput.setText(doc.getString("pricing"));

                    // === Spinner ===
                    String area = doc.getString("serviceArea");
                    if (area != null) {
                        setSpinnerSelection(serviceAreaSpinner, area);
                    }

                    // === Availability checkboxes ===
                    String availability = doc.getString("availability");
                    if (availability != null) {
                        setAvailabilityCheckboxes(availability);
                    }

                    // === Contact preference radio buttons ===
                    String contact = doc.getString("contactPreference");
                    if (contact != null) {
                        setRadioSelection(contact);
                    }

                    // === Restore catalogue dropdown selections ===
                    String category = doc.getString("category");
                    if (category != null && !category.isEmpty()) {
                        catalogueDropdown.setSelectedItemsFromCategory(category);
                    }

                    // === Restore image ===
                    String imageUrl = doc.getString("imageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this).load(imageUrl).into(imagePreview);
                        imagePreview.setVisibility(View.VISIBLE);
                        selectedImageUri = Uri.parse(imageUrl);
                    }

                    Toast.makeText(this, "Previous service loaded", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load draft", e);
                });
    }


    // =========================================================
    // RESTORE UI STATE HELPERS
    // =========================================================
    // used when retrieving saved data
    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }
    // used when retrieving saved data
    private void setAvailabilityCheckboxes(String availability) {
        if (availability == null) return;
        mon.setChecked(availability.contains("Mon"));
        tue.setChecked(availability.contains("Tue"));
        wed.setChecked(availability.contains("Wed"));
        thu.setChecked(availability.contains("Thu"));
        fri.setChecked(availability.contains("Fri"));
        sat.setChecked(availability.contains("Sat"));
        sun.setChecked(availability.contains("Sun"));
    }

    // used when retrieving saved data
    private void setRadioSelection(String text) {
        for (int i = 0; i < contactPreferenceGroup.getChildCount(); i++) {
            View child = contactPreferenceGroup.getChildAt(i);
            if (child instanceof RadioButton) {
                RadioButton rb = (RadioButton) child;
                if (rb.getText().toString().equals(text)) {
                    rb.setChecked(true);
                    break;
                }
            }
        }
    }
}