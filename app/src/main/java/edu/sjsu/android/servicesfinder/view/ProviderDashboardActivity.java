package edu.sjsu.android.servicesfinder.view;

import android.app.ProgressDialog;
import android.content.Context;
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
import edu.sjsu.android.servicesfinder.controller.FirestoreStringTranslator;
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
        setTitle(getString(R.string.title_provider_dashboard));

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize controller
        catalogueController = new CatalogueController();
        catalogueController.setListener(this);

        providerServiceController = new ProviderServiceController(this);
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
        binding.catalogueDropdown.setText(getString(R.string.progress_loading_catalogues));
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
            Toast.makeText(this, getString(R.string.error_no_provider_id), Toast.LENGTH_LONG).show();
            return;
        }

        // Collect user inputs
        String title = FormHelper.getText(binding.serviceTitleInput);
        String description = FormHelper.getText(binding.descriptionInput);
        String pricing = FormHelper.getText(binding.pricingInput);
        String area = FormHelper.getSelectedItem(binding.serviceAreaSpinner);

        FormHelper formHelper = new FormHelper(this);
        String availability = formHelper.getSelectedAvailability(
                binding.mon, binding.tue, binding.wed, binding.thu,
                binding.fri, binding.sat, binding.sun);

        //String contactPreference = FormHelper.getSelectedContactPreference(binding.contactPreferenceGroup, this);
        String localizedContactPreference =
                FormHelper.getSelectedContactPreference(binding.contactPreferenceGroup, this);

        // convert localized → English before save
        String contactPreference =
                FirestoreStringTranslator.get(this).reverseContactPreference(localizedContactPreference);


        /*
        Map<String, Set<String>> selectedItems = catalogueDropdown.getSelectedItems();
        String category = FormHelper.formatCategoryFromSelection(selectedItems);
        // SAVE ONLY ENGLISH KEYS
        String categoryToSave = FirestoreStringTranslator.get(this)
                .buildEnglishCategoryString(selectedItems);
        */
        // === KEEP YOUR EXISTING CODE (DO NOT DELETE) ===
        Map<String, Set<String>> selectedItems = catalogueDropdown.getSelectedItems();
        String category = FormHelper.formatCategoryFromSelection(selectedItems);

        Map<String, Set<String>> englishSelection = FirestoreStringTranslator.get(this)
                .reverseTranslateSelection(selectedItems);

        String categoryToSave = FirestoreStringTranslator.get(this)
                .buildEnglishCategoryString(englishSelection);




        if (title.isEmpty()) {
            binding.serviceTitleInput.setError(getString(R.string.error_required));
            Toast.makeText(this, getString(R.string.validation_enter_title), Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            binding.descriptionInput.setError(getString(R.string.error_required));
            Toast.makeText(this, getString(R.string.validation_enter_description), Toast.LENGTH_SHORT).show();
            return;
        }
        if (pricing.isEmpty()) {
            binding.pricingInput.setError(getString(R.string.error_required));
            Toast.makeText(this, getString(R.string.validation_enter_pricing), Toast.LENGTH_SHORT).show();
            return;
        }
        if (area.equals("Select Service Area") || area.isEmpty()) {
            Toast.makeText(this, getString(R.string.validation_select_area), Toast.LENGTH_SHORT).show();
            return;
        }
        if (availability.isEmpty()) {
            Toast.makeText(this, getString(R.string.validation_select_availability), Toast.LENGTH_SHORT).show();
            return;
        }
        if (contactPreference.isEmpty()) {
            Toast.makeText(this, getString(R.string.validation_select_contact), Toast.LENGTH_SHORT).show();
            return;
        }
        if (category.isEmpty()) {
            Toast.makeText(this, getString(R.string.validation_select_category), Toast.LENGTH_SHORT).show();
            return;
        }


        // 5. Handle image upload or direct save
        if (selectedImageUri != null) {
            String uriString = selectedImageUri.toString();

            if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                saveServiceToFirestore(title, description, pricing, categoryToSave, //**here***********
                        area, availability, contactPreference, providerId, uriString);
            } else {
                String finalProviderId = providerId;
                StorageHelper.uploadImageToFirebase(this, selectedImageUri, providerId, imageUrl -> {
                    saveServiceToFirestore(title, description, pricing, categoryToSave,
                            area, availability, contactPreference, finalProviderId, imageUrl);
                });
            }
        } else {
            String finalProviderId1 = providerId;
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_no_image_title))
                    .setMessage(getString(R.string.dialog_no_image_message))
                    .setPositiveButton(getString(R.string.action_continue_without_image), (dialog, which) -> {
                        saveServiceToFirestore(title, description, pricing, categoryToSave,
                                area, availability, contactPreference, finalProviderId1, null);
                    })
                    .setNegativeButton(getString(R.string.action_add_image), (dialog, which) -> showImagePickerDialog())
                    .show();

        }
    }

    private String getImageUploadErrorMessage(Context context, Exception e) {
        String message = e.getMessage();
        if (message == null) return context.getString(R.string.error_upload_unknown);

        if (message.contains("permission") || message.contains("PERMISSION_DENIED")) {
            return context.getString(R.string.error_upload_permission);
        } else if (message.contains("network") || message.contains("UNAVAILABLE")) {
            return context.getString(R.string.error_upload_network);
        } else if (message.contains("quota")) {
            return context.getString(R.string.error_upload_quota);
        } else if (message.contains("unauthorized") || message.contains("UNAUTHENTICATED")) {
            return context.getString(R.string.error_upload_unauthorized);
        } else {
            return context.getString(R.string.error_upload_failed, message);
        }
    }


    // =========================================================
    // SAVE SERVICE TO FIRESTORE
    // =========================================================
    private void saveServiceToFirestore(String title, String description, String pricing,
                                        String category, String area, String availability,
                                        String contactPreference, String providerId, String imageUrl) {
        if (providerId == null || providerId.isEmpty()) {
            String context = getString(R.string.error_context_login_session);
            String message = getString(R.string.error_no_provider_id_1, context);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
        savingDialog.setMessage(getString(R.string.progress_saving_service));
        savingDialog.setCancelable(false);
        savingDialog.show();

        ProviderServiceController controller = new ProviderServiceController(this);
        controller.saveOrUpdateService(this,providerId, service, new ProviderServiceDatabase.OnServiceSaveListener() {
            @Override
            public void onSuccess(String serviceId) {
                savingDialog.dismiss();
                Toast.makeText(ProviderDashboardActivity.this,
                        getString(R.string.success_service_saved),
                        Toast.LENGTH_SHORT).show();

                clearForm();
                finish();
            }

            @Override
            public void onError(String error) {
                savingDialog.dismiss();
                Toast.makeText(ProviderDashboardActivity.this,
                        getString(R.string.error_service_save_failed, error),
                        Toast.LENGTH_LONG).show();

            }
        });
    }

    // =========================================================
    // IMAGE PICKER
    // =========================================================
    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_select_image_title))
                .setItems(new String[]{
                        getString(R.string.dialog_option_gallery),
                        getString(R.string.dialog_option_camera)
                }, (dialog, which) -> {
                    if (which == 0) openGallery();
                    else if (which == 1) openCamera();
                })
                .setNegativeButton(getString(R.string.action_cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(Intent.createChooser(intent, getString(R.string.chooser_select_photo)));
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
                .setTitle(getString(R.string.dialog_discard_title))
                .setMessage(getString(R.string.dialog_discard_message))
                .setPositiveButton(getString(R.string.action_yes), (dialog, which) -> finish())
                .setNegativeButton(getString(R.string.action_no), null)
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
        loadingDialog.setMessage(getString(R.string.progress_loading_catalogues));
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
        providerServiceController.loadServiceAreas(this, new ProviderServiceController.ServiceAreaListener() {
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
                Toast.makeText(ProviderDashboardActivity.this,
                        getString(R.string.error_load_areas_failed),
                        Toast.LENGTH_SHORT).show();

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

    /*
    @Override
    public void onCatalogueMapLoaded(Map<String, List<String>> catalogueMap) {
        hideLoadingDialog();

        if (catalogueMap.isEmpty()) {
            binding.catalogueDropdown.setText(getString(R.string.empty_state_no_catalogues));
            binding.catalogueDropdown.setEnabled(false);
            return;
        }

        cataloguesLoaded = true;
        binding.catalogueDropdown.setEnabled(true);
        binding.catalogueDropdown.setText(getString(R.string.hint_select_catalogue_services));
        catalogueDropdown.updateCatalogueMap(catalogueMap);

        loadLastServiceDraft();
    }
    */
    @Override
    public void onCatalogueMapLoaded(Map<String, List<String>> englishCatalogueMap) {
        hideLoadingDialog();

        if (englishCatalogueMap.isEmpty()) {
            binding.catalogueDropdown.setText(getString(R.string.empty_state_no_catalogues));
            binding.catalogueDropdown.setEnabled(false);
            return;
        }

        cataloguesLoaded = true;
        binding.catalogueDropdown.setEnabled(true);
        binding.catalogueDropdown.setText(getString(R.string.hint_select_catalogue_services));

        // ONE LINE — FULLY AUTOMATIC TRANSLATION FOR ANY LANGUAGE
        Map<String, List<String>> translatedMap =
                FirestoreStringTranslator.get(this)
                        .translateCatalogueMap(englishCatalogueMap);

        catalogueDropdown.updateCatalogueMap(translatedMap);

        loadLastServiceDraft();
    }

    @Override
    public void onError(String errorMessage) {
        hideLoadingDialog();
        Toast.makeText(this,
                getString(R.string.error_loading_catalogues, errorMessage),
                Toast.LENGTH_LONG).show();

        binding.catalogueDropdown.setText(getString(R.string.fallback_catalogue_load_failed));
        binding.catalogueDropdown.setEnabled(false);
    }

    // =========================================================
    // LOAD LAST SERVICE DRAFT
    // =========================================================
    private void loadLastServiceDraft() {
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        ProviderServiceController controller = new ProviderServiceController(this);
        controller.loadLastServiceDraft(this, uid, new ProviderServiceController.OnDraftLoadedListener() {
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
                /*
                if (draft.getCategory() != null && !draft.getCategory().isEmpty()) {
                    catalogueDropdown.setSelectedItemsFromCategory(draft.getCategory());
                }
                */

                // Restore saved category selection (English saved in Firestore)
                if (draft.getCategory() != null && !draft.getCategory().isEmpty()) {
                    String saved = draft.getCategory().trim();

                    // Try to parse as clean English first (new format)
                    Map<String, Set<String>> englishMap = FirestoreStringTranslator.parseEnglishCategoryString(saved);

                    if (!englishMap.isEmpty()) {
                        // It's clean English → convert to current language
                        Map<String, Set<String>> localizedMap = FirestoreStringTranslator.getLocalizedCategoryMap(englishMap);
                        String localizedText = FirestoreStringTranslator.get(ProviderDashboardActivity.this)
                                .buildLocalizedCategoryString(localizedMap);
                        catalogueDropdown.setSelectedItemsFromCategory(localizedText);
                    } else {
                        // Old broken format (any language) → just translate directly
                        String localized = FirestoreStringTranslator.get(ProviderDashboardActivity.this).translateCategory(saved);
                        catalogueDropdown.setSelectedItemsFromCategory(localized);
                    }
                }


                if (draft.getImageUrl() != null && !draft.getImageUrl().isEmpty()) {
                    Glide.with(ProviderDashboardActivity.this).load(draft.getImageUrl()).into(binding.imagePreview);
                    binding.imagePreview.setVisibility(View.VISIBLE);
                    selectedImageUri = Uri.parse(draft.getImageUrl());
                }
                Toast.makeText(ProviderDashboardActivity.this,
                        getString(R.string.info_previous_service_loaded),
                        Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onNoDraftFound() {
               // Log.d(TAG, "No draft found - starting with empty form");
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ProviderDashboardActivity.this,
                        getString(R.string.error_failed_to_load_draft),
                        Toast.LENGTH_SHORT).show();
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
                input.setError(getString(R.string.error_password_too_short));
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
        SpannableString FirstAsk = new SpannableString(getString(R.string.dialog_stop_business));
        FirstAsk.setSpan(new StyleSpan(Typeface.BOLD),0, FirstAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        FirstAsk.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.peru)),0, FirstAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        FirstAsk.setSpan(new AbsoluteSizeSpan(22, true),0, FirstAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        new MaterialAlertDialogBuilder(this)
                .setTitle(FirstAsk)
                .setMessage(getString(R.string.dialog_stop_business_message))
                .setIcon(R.drawable.ic_delete_account)
                .setNegativeButton(getString(R.string.action_cancel), null)
                .setPositiveButton(getString(R.string.action_confirm_stop), (d, w) -> showSecondConfirmation())
                .setCancelable(false)
                .show();
    }

    private void showSecondConfirmation() {
        SpannableString SecondAsk = new SpannableString(getString(R.string.dialog_delete_account_title));
        SecondAsk.setSpan(new StyleSpan(Typeface.BOLD),0, SecondAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SecondAsk.setSpan(new ForegroundColorSpan(Color.RED),0, SecondAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SecondAsk.setSpan(new AbsoluteSizeSpan(22, true),0, SecondAsk.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        new MaterialAlertDialogBuilder(this)
                .setTitle(SecondAsk)
                .setMessage(getString(R.string.dialog_delete_account_message))
                .setIcon(R.drawable.ic_delete_account)
                .setNegativeButton(getString(R.string.action_cancel), null)
                .setPositiveButton(getString(R.string.action_delete_forever), (dialog, which) -> {
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