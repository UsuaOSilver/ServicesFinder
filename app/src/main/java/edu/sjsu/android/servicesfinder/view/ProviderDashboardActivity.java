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
import edu.sjsu.android.servicesfinder.model.Catalogue;
import edu.sjsu.android.servicesfinder.model.Service;

public class ProviderDashboardActivity extends AppCompatActivity
        implements CatalogueController.CatalogueControllerListener {

    private static final String TAG = "ProviderDashboard";

    private ImageView imagePreview;
    private TextInputEditText serviceTitleInput, descriptionInput, pricingInput;
    private TextView catalogueTextView;
    private Spinner serviceAreaSpinner;
    private RadioGroup contactPreferenceGroup;
    private ProgressDialog loadingDialog;

    private CheckBox mon, tue, wed, thu, fri, sat, sun;

    private CatalogueController catalogueController;
    private Uri selectedImageUri, tempImageUri;

    private FirebaseFirestore firestore;
    private StorageReference storageRef;

    private MultiSelectDropdown catalogueDropdown;
    private boolean cataloguesLoaded = false;

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(imagePreview);
                    imagePreview.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    selectedImageUri = tempImageUri;
                    Glide.with(this).load(selectedImageUri).into(imagePreview);
                    imagePreview.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_dashboard);
        setTitle("Provider Dashboard");

        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        catalogueController = new CatalogueController();
        catalogueController.setListener(this);

        initializeViews();
        catalogueTextView.setEnabled(false);
        catalogueTextView.setText("Loading catalogues...");
        catalogueDropdown = new MultiSelectDropdown(this, catalogueTextView, new HashMap<>());

        showLoadingDialog();
        loadServiceAreas();
        loadCatalogues();
        setupButtons();

        ((RadioButton) findViewById(R.id.contactCall)).setChecked(true);
    }

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

    private void handleSave() {
        String title = getText(serviceTitleInput);
        String description = getText(descriptionInput);
        String pricing = getText(pricingInput);
        String area = getSelectedItem(serviceAreaSpinner);
        String availability = getSelectedAvailability();
        String contactPreference = getSelectedContactPreference();
        Map<String, Set<String>> selectedItems = catalogueDropdown.getSelectedItems();
        String category = formatCategoryFromSelection(selectedItems);

        Log.d(TAG, "Input Debug:");
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Description: " + description);
        Log.d(TAG, "Pricing: " + pricing);
        Log.d(TAG, "Area: " + area);
        Log.d(TAG, "Availability: " + availability);
        Log.d(TAG, "Contact Preference: " + contactPreference);
        Log.d(TAG, "Category: " + category);
        Log.d(TAG, "Image URI: " + selectedImageUri);

        if (title.isEmpty() || description.isEmpty() || pricing.isEmpty() ||
                area.equals("Select Service Area") || availability.isEmpty() ||
                contactPreference.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            uploadImageToFirebase(selectedImageUri,
                    uri -> saveServiceToFirestore(title, description, pricing, category,
                            area, availability, contactPreference, uri));
        } else {
            saveServiceToFirestore(title, description, pricing, category,
                    area, availability, contactPreference, null);
        }
    }

    private void saveServiceToFirestore(String title, String description, String pricing,
                                        String category, String area, String availability,
                                        String contactPreference, String imageUrl) {

        String providerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "null";

        Log.d(TAG, "Saving service for UID: " + providerId);

        Map<String, Object> map = new HashMap<>();
        map.put("providerId", providerId);
        map.put("serviceTitle", title);
        map.put("description", description);
        map.put("pricing", pricing);
        map.put("category", category);
        map.put("serviceArea", area);
        map.put("availability", availability);
        map.put("contactPreference", contactPreference);
        map.put("imageUrl", imageUrl);
        map.put("status", "Active");
        map.put("verified", false);
        map.put("rating", 0.0);
        map.put("timestamp", System.currentTimeMillis());

        Log.d(TAG, "Service map: " + map.toString());

        firestore.collection("providers")
                .document(providerId)
                .collection("services")
                .add(map)
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "Service saved to document: " + doc.getId());
                    Toast.makeText(this, "Service added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save service", e);
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImageToFirebase(Uri imageUri, OnSuccessListener<String> callback) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading...");
        dialog.setCancelable(false);
        dialog.show();

        String fileName = "images/" + UUID.randomUUID();
        StorageReference fileRef = storageRef.child(fileName);

        Log.d(TAG, "Uploading image to: " + fileName);

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Log.d(TAG, "Image uploaded. URL: " + uri.toString());
                            dialog.dismiss();
                            callback.onSuccess(uri.toString());
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed", e);
                    dialog.dismiss();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    callback.onSuccess(null);
                });
    }

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

    private void handleCancel() {
        new AlertDialog.Builder(this)
                .setTitle("Discard changes?")
                .setMessage("Are you sure you want to cancel without saving?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    private String formatCategoryFromSelection(Map<String, Set<String>> selectedItems) {
        List<String> summary = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : selectedItems.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                summary.add(entry.getKey() + ": " + String.join(", ", entry.getValue()));
            }
        }
        return String.join(" | ", summary);
    }

    private String getSelectedContactPreference() {
        int selectedId = contactPreferenceGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return "";
        RadioButton radioButton = findViewById(selectedId);
        return radioButton != null ? radioButton.getText().toString() : "";
    }

    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Take a Photo"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) openGallery();
            else openCamera();
        });
        builder.show();
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

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getSelectedItem(Spinner spinner) {
        return spinner.getSelectedItem() != null ? spinner.getSelectedItem().toString() : "";
    }

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

    private void loadCatalogues() {
        catalogueController.loadCatalogueMapForDropdown();
    }

    @Override
    public void onCataloguesLoaded(List<Catalogue> catalogues) {}

    @Override
    public void onCatalogueWithServicesLoaded(Catalogue catalogue, List<Service> services) {}

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

        Toast.makeText(this, "Catalogues loaded successfully", Toast.LENGTH_SHORT).show();

        // load latest draft only AFTER dropdown categories are ready
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

    private void loadLastService() {

        String providerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d(TAG, "Loading last service for provider: " + providerId);

        firestore.collection("providers")
                .document(providerId)
                .collection("services")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        Log.d(TAG, "No previous service found.");
                        return;
                    }

                    var doc = query.getDocuments().get(0);

                    Log.d(TAG, "Last service loaded: " + doc.getId());

                    serviceTitleInput.setText(doc.getString("serviceTitle"));
                    descriptionInput.setText(doc.getString("description"));
                    pricingInput.setText(doc.getString("pricing"));

                    // Spinner
                    String area = doc.getString("serviceArea");
                    setSpinnerSelection(serviceAreaSpinner, area);

                    // Days
                    String availability = doc.getString("availability");
                    setAvailabilityCheckboxes(availability);

                    // RadioGroup
                    String contact = doc.getString("contactPreference");
                    setRadioSelection(contact);

                    // Category text only (Dropdown cannot restore multi-level — optional)
                    catalogueTextView.setText(doc.getString("category"));

                    // Image
                    String imageUrl = doc.getString("imageUrl");
                    if (imageUrl != null) {
                        Glide.with(this).load(imageUrl).into(imagePreview);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load last service", e);
                });
    }
    // ==============================
// Helpers for restoring UI state
// ==============================
    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

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

    private void loadLastServiceDraft() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d(TAG, "Loading last service draft for provider: " + uid);

        FirebaseFirestore.getInstance()
                .collection("providers")
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
                    Log.d(TAG, "Draft loaded - Document ID: " + doc.getId());

                    // ✅ Fill text fields
                    serviceTitleInput.setText(doc.getString("serviceTitle"));
                    descriptionInput.setText(doc.getString("description"));
                    pricingInput.setText(doc.getString("pricing"));

                    //  Set service area spinner
                    String area = doc.getString("serviceArea");
                    if (area != null) {
                        setSpinnerSelection(serviceAreaSpinner, area);
                        Log.d(TAG, "Restored service area: " + area);
                    }

                    //  Set availability checkboxes
                    String availability = doc.getString("availability");
                    if (availability != null) {
                        setAvailabilityCheckboxes(availability);
                        Log.d(TAG, "Restored availability: " + availability);
                    }

                    //  Set contact preference radio button
                    String contact = doc.getString("contactPreference");
                    if (contact != null) {
                        setRadioSelection(contact);
                        Log.d(TAG, "Restored contact preference: " + contact);
                    }

                    //  RESTORE DROPDOWN SELECTIONS - THIS IS THE KEY FIX!
                    String category = doc.getString("category");
                    if (category != null && !category.isEmpty()) {
                        catalogueDropdown.setSelectedItemsFromCategory(category);
                        Log.d(TAG, "Restored category selections: " + category);
                    } else {
                        Log.d(TAG, "No category data to restore");
                    }

                    // Load image if exists
                    String imageUrl = doc.getString("imageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this).load(imageUrl).into(imagePreview);
                        imagePreview.setVisibility(View.VISIBLE);
                        selectedImageUri = Uri.parse(imageUrl); // Store the URI
                        Log.d(TAG, "Restored image: " + imageUrl);
                    }

                    Toast.makeText(this, "Previous service draft loaded", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load draft", e);
                    Toast.makeText(this, "Could not load previous draft", Toast.LENGTH_SHORT).show();
                });
    }



}
