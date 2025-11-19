package edu.sjsu.android.servicesfinder.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.util.*;
import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.controller.FormHelper;
import edu.sjsu.android.servicesfinder.controller.ProviderController;
import edu.sjsu.android.servicesfinder.controller.SessionManager;
import edu.sjsu.android.servicesfinder.controller.CatalogueController;
import edu.sjsu.android.servicesfinder.controller.ProviderServiceController;
import edu.sjsu.android.servicesfinder.controller.UIHelper;
import edu.sjsu.android.servicesfinder.database.ProviderServiceDatabase;
import edu.sjsu.android.servicesfinder.database.StorageHelper;
import edu.sjsu.android.servicesfinder.databinding.ActivityProviderDashboardBinding;
import edu.sjsu.android.servicesfinder.model.Catalogue;
import edu.sjsu.android.servicesfinder.model.Provider;
import edu.sjsu.android.servicesfinder.model.ProviderService;
import edu.sjsu.android.servicesfinder.model.Service;

/* ******************************************************************************************************
 * ProviderDashboardActivity
 *
 * This screen is displayed after a provider signs in or log in.
 *
 * Responsibilities:
 *  - Load previous “draft” automatically when signIn
 *  - Allow the provider to create and save a service.
 *  - Upload a service image to Firebase Storage
 *  - Save service details to Firebase Firestore.
 *
 * Firebase components used:
 *      Firestore → stores structured service data
 *      Storage   → stores uploaded image files
 **********************************************************************************************************/
public class ProviderDashboardActivity extends AppCompatActivity
        implements CatalogueController.CatalogueControllerListener {

    private static final String TAG = "ProviderDashboard";

    // === VIEW BINDING ===
    private ActivityProviderDashboardBinding binding;

    // UI Components (now accessed via binding)
    // Removed all private view fields — binding handles them

    // Controllers and Firebase
    private CatalogueController catalogueController;
    private FirebaseFirestore firestore;
    private StorageReference storageRef;

    // State
    private Uri selectedImageUri, tempImageUri;
    private MultiSelectDropdown catalogueDropdown;
    private boolean cataloguesLoaded = false;
    private String editingServiceId = null;

    private ProviderServiceController providerServiceController;
    private ProviderController controller;

    // Image selection launchers
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(binding.imagePreview);
                    binding.imagePreview.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    selectedImageUri = tempImageUri;
                    Glide.with(this).load(selectedImageUri).into(binding.imagePreview);
                    binding.imagePreview.setVisibility(View.VISIBLE);
                }
            });

    // =========================================================
    // LIFECYCLE METHODS
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === VIEW BINDING INFLATE ===
        binding = ActivityProviderDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("Provider Dashboard");

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize controller
        catalogueController = new CatalogueController();
        catalogueController.setListener(this);

        providerServiceController = new ProviderServiceController();
        controller = new ProviderController(this);
        controller.setListener(new ProviderController.ProviderControllerListener() {
            @Override
            public void onProviderLoaded(Provider provider) {}
            @Override
            public void onSignUpSuccess(String msg) {
                Toast.makeText(ProviderDashboardActivity.this, msg, Toast.LENGTH_LONG).show();
            }
            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProviderDashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        // Setup UI
        binding.catalogueDropdown.setEnabled(false);
        binding.catalogueDropdown.setText("Loading catalogues...");
        catalogueDropdown = new MultiSelectDropdown(this, binding.catalogueDropdown, new HashMap<>());

        // Load initial data
        showLoadingDialog();
        loadServiceAreas();
        loadCatalogues();
        setupButtons();

        // Set default contact preference
        binding.contactCall.setChecked(true);

        // For Setting
        initializeSettingsIcons();
    }

    private void setupButtons() {
        binding.uploadImageBtn.setOnClickListener(v -> showImagePickerDialog());
        binding.saveBtn.setOnClickListener(v -> handleSave());
        binding.cancelBtn.setOnClickListener(v -> handleCancel());
    }

    // =========================================================
    // SAVE SERVICE
    // =========================================================
    private void handleSave() {
        // Safely get providerId
        String providerId = SessionManager.getProviderId(this);

        if (providerId == null) {
            providerId = getIntent().getStringExtra("providerId");
            if (providerId != null) {
                SessionManager.saveProvider(this, providerId, getIntent().getStringExtra("providerName"));
            }
        }

        if (providerId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            providerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            SessionManager.saveProvider(this, providerId, SessionManager.getProviderName(this));
        }

        if (providerId == null) {
            Toast.makeText(this, "No provider ID found. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        // Collect user inputs
        String title = FormHelper.getText(binding.serviceTitleInput);
        String description = FormHelper.getText(binding.descriptionInput);
        String pricing = FormHelper.getText(binding.pricingInput);
        String area = FormHelper.getSelectedItem(binding.serviceAreaSpinner);
        String availability = FormHelper.getSelectedAvailability(
                binding.mon, binding.tue, binding.wed, binding.thu,
                binding.fri, binding.sat, binding.sun);
        String contactPreference = FormHelper.getSelectedContactPreference(binding.contactPreferenceGroup, this);
        Map<String, Set<String>> selectedItems = catalogueDropdown.getSelectedItems();
        String category = FormHelper.formatCategoryFromSelection(selectedItems);

        // Validate inputs
        if (title.isEmpty()) {
            binding.serviceTitleInput.setError("Required");
            Toast.makeText(this, "Please enter service title", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            binding.descriptionInput.setError("Required");
            Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pricing.isEmpty()) {
            binding.pricingInput.setError("Required");
            Toast.makeText(this, "Please enter pricing details", Toast.LENGTH_SHORT).show();
            return;
        }
        if (area.equals("Select Service Area") || area.isEmpty()) {
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

        // 5. Handle image upload or direct save
        if (selectedImageUri != null) {
            String uriString = selectedImageUri.toString();

            if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                saveServiceToFirestore(title, description, pricing, category,
                        area, availability, contactPreference, providerId, uriString);
            } else {
                String finalProviderId = providerId;
                StorageHelper.uploadImageToFirebase(this, selectedImageUri, providerId, imageUrl -> {
                    saveServiceToFirestore(title, description, pricing, category,
                            area, availability, contactPreference, finalProviderId, imageUrl);
                });
            }
        } else {
            String finalProviderId1 = providerId;
            new AlertDialog.Builder(this)
                    .setTitle("No Image")
                    .setMessage("Do you want to add a service without an image?")
                    .setPositiveButton("Yes, Continue", (dialog, which) -> {
                        saveServiceToFirestore(title, description, pricing, category,
                                area, availability, contactPreference, finalProviderId1, null);
                    })
                    .setNegativeButton("Add Image", (dialog, which) -> showImagePickerDialog())
                    .show();
        }
    }

    private String getImageUploadErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) return "Unknown upload error";
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
    private void saveServiceToFirestore(String title, String description, String pricing,
                                        String category, String area, String availability,
                                        String contactPreference, String providerId, String imageUrl) {
        if (providerId == null || providerId.isEmpty()) {
            Toast.makeText(this, "Error: No provider ID found (login/session issue).", Toast.LENGTH_LONG).show();
            return;
        }

        ProviderService service = new ProviderService();
        service.setProviderId(providerId);
        service.setServiceTitle(title);
        service.setDescription(description);
        service.setPricing(pricing);
        service.setCategory(category);
        service.setServiceArea(area);
        service.setAvailability(availability);
        service.setContactPreference(contactPreference);
        service.setImageUrl(imageUrl != null ? imageUrl : "");
        service.setStatus("Active");
        service.setRating(0.0);
        service.setTimestamp(System.currentTimeMillis());
        if (editingServiceId != null) service.setId(editingServiceId);

        ProgressDialog savingDialog = new ProgressDialog(this);
        savingDialog.setMessage("Saving service...");
        savingDialog.setCancelable(false);
        savingDialog.show();

        ProviderServiceController controller = new ProviderServiceController();
        controller.saveOrUpdateService(providerId, service, new ProviderServiceDatabase.OnServiceSaveListener() {
            @Override
            public void onSuccess(String serviceId) {
                savingDialog.dismiss();
                Toast.makeText(ProviderDashboardActivity.this, "Service saved successfully!", Toast.LENGTH_SHORT).show();
                clearForm();
                finish();
            }

            @Override
            public void onError(String error) {
                savingDialog.dismiss();
                Toast.makeText(ProviderDashboardActivity.this, "Save failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // =========================================================
    // IMAGE PICKER
    // =========================================================
    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(new String[]{"Choose from Gallery", "Take a Photo"}, (dialog, which) -> {
                    if (which == 0) openGallery();
                    else if (which == 1) openCamera();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
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

    private void handleCancel() {
        new AlertDialog.Builder(this)
                .setTitle("Discard changes?")
                .setMessage("Are you sure you want to cancel without saving?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    // =========================================================
    // LOADING DIALOGS
    // =========================================================
    private ProgressDialog loadingDialog;

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
        providerServiceController.loadServiceAreas(new ProviderServiceController.ServiceAreaListener() {
            @Override
            public void onLoaded(List<String> areas) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        ProviderDashboardActivity.this,
                        android.R.layout.simple_spinner_item,
                        areas
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.serviceAreaSpinner.setAdapter(adapter);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, message);
                Toast.makeText(ProviderDashboardActivity.this, "Failed to load areas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================
    // LOAD CATALOGUES
    // =========================================================
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
            binding.catalogueDropdown.setText("No catalogues available");
            binding.catalogueDropdown.setEnabled(false);
            return;
        }

        cataloguesLoaded = true;
        binding.catalogueDropdown.setEnabled(true);
        binding.catalogueDropdown.setText("Select Catalogue & Services");
        catalogueDropdown.updateCatalogueMap(catalogueMap);

        Toast.makeText(this, "Catalogues loaded", Toast.LENGTH_SHORT).show();
        loadLastServiceDraft();
    }

    @Override
    public void onError(String errorMessage) {
        hideLoadingDialog();
        Toast.makeText(this, "Error loading catalogues: " + errorMessage, Toast.LENGTH_LONG).show();
        binding.catalogueDropdown.setText("Failed to load catalogues");
        binding.catalogueDropdown.setEnabled(false);
    }

    // =========================================================
    // LOAD LAST SERVICE DRAFT
    // =========================================================
    private void loadLastServiceDraft() {
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        Log.d(TAG, "Loading last service draft for provider: " + uid);

        ProviderServiceController controller = new ProviderServiceController();
        controller.loadLastServiceDraft(uid, new ProviderServiceController.OnDraftLoadedListener() {
            @Override
            public void onDraftLoaded(ProviderServiceController.ServiceDraft draft) {
                editingServiceId = draft.getId();
                binding.serviceTitleInput.setText(draft.getServiceTitle());
                binding.descriptionInput.setText(draft.getDescription());
                binding.pricingInput.setText(draft.getPricing());

                if (draft.getServiceArea() != null) {
                    UIHelper.setSpinnerSelection(binding.serviceAreaSpinner, draft.getServiceArea());
                }

                if (draft.getAvailability() != null) {
                    UIHelper.setAvailabilityCheckboxes(draft.getAvailability(),
                            binding.mon, binding.tue, binding.wed, binding.thu,
                            binding.fri, binding.sat, binding.sun);
                }

                if (draft.getContactPreference() != null) {
                    UIHelper.setRadioSelection(binding.contactPreferenceGroup, draft.getContactPreference(), ProviderDashboardActivity.this);
                }

                if (draft.getCategory() != null && !draft.getCategory().isEmpty()) {
                    catalogueDropdown.setSelectedItemsFromCategory(draft.getCategory());
                }

                if (draft.getImageUrl() != null && !draft.getImageUrl().isEmpty()) {
                    Glide.with(ProviderDashboardActivity.this).load(draft.getImageUrl()).into(binding.imagePreview);
                    binding.imagePreview.setVisibility(View.VISIBLE);
                    selectedImageUri = Uri.parse(draft.getImageUrl());
                }

                Toast.makeText(ProviderDashboardActivity.this, "Previous service loaded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoDraftFound() {
                Log.d(TAG, "No draft found - starting with empty form");
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ProviderDashboardActivity.this, "Failed to load draft", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================
    // CLEAR FORM
    // =========================================================
    private void clearForm() {
        binding.serviceTitleInput.setText("");
        binding.descriptionInput.setText("");
        binding.pricingInput.setText("");
        binding.serviceAreaSpinner.setText("", false);

        binding.mon.setChecked(false);
        binding.tue.setChecked(false);
        binding.wed.setChecked(false);
        binding.thu.setChecked(false);
        binding.fri.setChecked(false);
        binding.sat.setChecked(false);
        binding.sun.setChecked(false);

        binding.contactPreferenceGroup.clearCheck();
        binding.contactCall.setChecked(true);

        selectedImageUri = null;
        binding.imagePreview.setVisibility(View.GONE);
        binding.catalogueDropdown.setText("Select Catalogue & Services");
        editingServiceId = null;
    }

    // =========================================================
    // SETTINGS
    // =========================================================
    private void initializeSettingsIcons() {
        binding.settingsButton.setOnClickListener(v -> showSettingsDialog());
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.account_settings, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        ImageButton upButton = dialogView.findViewById(R.id.upButton);
        upButton.setOnClickListener(v -> dialog.dismiss());

        ImageButton changePasswordBtn = dialogView.findViewById(R.id.iconChangePassword);
        ImageButton editProfileBtn = dialogView.findViewById(R.id.iconEditProfile);
        ImageButton deleteAccountBtn = dialogView.findViewById(R.id.iconDeleteAccount);

        changePasswordBtn.setOnClickListener(v -> {
            dialog.dismiss();
            openChangePasswordDialog();
        });

        editProfileBtn.setOnClickListener(v -> {
            dialog.dismiss();
            openChangeProfileDialog();
        });

        deleteAccountBtn.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteAccount();
        });
    }
    /*
    private void openChangePasswordDialog() {
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        TextView title = new TextView(this);
        title.setText("Change Password");
        title.setTextSize(20);
        title.setTextColor(ContextCompat.getColor(this, R.color.white));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(padding, padding, padding, padding);

        LinearLayout titleContainer = new LinearLayout(this);
        titleContainer.setBackgroundColor(ContextCompat.getColor(this, R.color.design_default_color_primary));
        titleContainer.addView(title);

        final EditText input = new EditText(this);
        input.setHint("Enter new password");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setPadding(padding, padding, padding, padding);
        input.setBackgroundColor(ContextCompat.getColor(this, R.color.white));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(titleContainer)
                .setView(input)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button updateBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button cancelBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            updateBtn.setTextColor(ContextCompat.getColor(this, R.color.peru));
            updateBtn.setTextSize(16);
            updateBtn.setTypeface(Typeface.DEFAULT_BOLD);
            cancelBtn.setTextColor(ContextCompat.getColor(this, R.color.peru));
            cancelBtn.setTextSize(16);
            cancelBtn.setTypeface(Typeface.DEFAULT_BOLD);

            updateBtn.setOnClickListener(v -> {
                String newPassword = input.getText().toString().trim();
                if (newPassword.length() < 6) {
                    input.setError("Password must be at least 6 characters");
                    return;
                }
                controller.updatePassword(newPassword, controller.getListener());
                dialog.dismiss();
            });

            cancelBtn.setOnClickListener(v -> {
                dialog.dismiss();
                //showSettingsDialog();
            });
        });

        dialog.show();
        input.requestFocus();
        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

     */
    // Change Password Dialog =============================================================================
    private void openChangePasswordDialog() {

        // Inflate XML layout
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);

        EditText input = view.findViewById(R.id.passwordInput);
        MaterialButton updateBtn = view.findViewById(R.id.UpdatePasswordButton);
        MaterialButton cancelBtn = view.findViewById(R.id.CancelButton);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        updateBtn.setOnClickListener(v -> {
            String newPassword = input.getText().toString().trim();

            if (newPassword.length() < 6) {
                input.setError("Password must be at least 6 characters");
                return;
            }
            controller.updatePassword(newPassword, controller.getListener());
            dialog.dismiss();
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        input.requestFocus();
        Objects.requireNonNull(dialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }


    // Change ProfileDialog =============================================================================
    private void openChangeProfileDialog() {
        Intent intent = new Intent(ProviderDashboardActivity.this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void confirmDeleteAccount() {
        SpannableString FirstAsk = new SpannableString("Stop Your Business Here?");
        FirstAsk.setSpan(new StyleSpan(Typeface.BOLD),0, FirstAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        FirstAsk.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.peru)),0, FirstAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        FirstAsk.setSpan(new AbsoluteSizeSpan(22, true),0, FirstAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        new MaterialAlertDialogBuilder(this)
                .setTitle(FirstAsk)
                .setMessage("Do you really want to stop your business here?")
                .setIcon(R.drawable.ic_delete_account)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Yes, stop", (d, w) -> showSecondConfirmation())
                .setCancelable(false)
                .show();
    }

    private void showSecondConfirmation() {
        SpannableString SecondAsk = new SpannableString("Delete Account Forever?");
        SecondAsk.setSpan(new StyleSpan(Typeface.BOLD),0, SecondAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SecondAsk.setSpan(new ForegroundColorSpan(Color.RED),0, SecondAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SecondAsk.setSpan(new AbsoluteSizeSpan(22, true),0, SecondAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        new MaterialAlertDialogBuilder(this)
                .setTitle(SecondAsk)
                .setMessage("This action is irreversible. Your account, services, and all data will be permanently deleted.")
                .setIcon(R.drawable.ic_delete_account)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete Forever", (dialog, which) -> {
                    controller.deleteAccount(new ProviderController.DeleteAccountCallback() {
                        @Override public void onSuccess(String message) {
                            Toast.makeText(ProviderDashboardActivity.this, message, Toast.LENGTH_LONG).show();
                            FirebaseAuth.getInstance().signOut();
                            SessionManager.clearSession(ProviderDashboardActivity.this);
                            startActivity(new Intent(ProviderDashboardActivity.this, MainActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                            finishAffinity();
                        }
                        @Override public void onError(String error) {
                            Toast.makeText(ProviderDashboardActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setCancelable(false)
                .show()
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
    }

    // =========================================================
    // ON DESTROY
    // =========================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Prevent memory leaks
    }
}