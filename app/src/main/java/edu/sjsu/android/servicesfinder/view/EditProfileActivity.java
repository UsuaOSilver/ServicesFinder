    package edu.sjsu.android.servicesfinder.view;

    import android.graphics.drawable.Drawable;
    import android.os.Bundle;
    import android.telephony.PhoneNumberFormattingTextWatcher;
    import android.widget.Toast;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.appcompat.content.res.AppCompatResources;

    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import java.util.Objects;

    import edu.sjsu.android.servicesfinder.R;
    import edu.sjsu.android.servicesfinder.controller.ProviderController;
    import edu.sjsu.android.servicesfinder.database.ProviderDatabase;
    import edu.sjsu.android.servicesfinder.databinding.EditProfileSettingBinding;
    import edu.sjsu.android.servicesfinder.model.Provider;

    /* *******************************************************************************************
     * EditProfileActivity - Allows provider to edit name, email, phone, and address.
     * Rewritten using ViewBinding for cleaner, safer UI access.
     ******************************************************************************************* */
    public class EditProfileActivity extends AppCompatActivity {

        private EditProfileSettingBinding binding;   // ViewBinding reference
        private ProviderController controller;       // Handles update logic

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Inflate UI via binding
            binding = EditProfileSettingBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Setup toolbar
            setSupportActionBar(binding.editProfileToolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            // set Up btn white color
            Drawable upArrow = AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back);
            if (upArrow != null) {
                upArrow.setTint(getColor(android.R.color.white));
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }

            // Controller
            controller = new ProviderController(this);

            /* Listener for update/migration results */
            controller.setListener(new ProviderController.ProviderControllerListener() {
                @Override
                public void onSignUpSuccess(String message) {
                    Toast.makeText(EditProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String msg) {
                    Toast.makeText(EditProfileActivity.this, "Error: " + msg, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onProviderLoaded(Provider provider) {
                    binding.editProfileFullName.setText(provider.getFullName());
                    binding.editProfileEmail.setText(provider.getEmail());
                    binding.editProfilePhone.setText(provider.getPhone());
                    binding.editProfileAddress.setText(provider.getAddress());
                }
            });

            // Auto-format phone input (###) ###-####
            binding.editProfilePhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

            // Load current user profile
            loadCurrentProvider();

            /* Save button click */
            binding.editProfileSaveButton.setOnClickListener(v -> saveProfile());

            /* Cancel button exits */
            binding.editProfileCancelButton.setOnClickListener(v -> finish());
        }

        /* *******************************************************************************************
         * Load provider data from Firestore
         ******************************************************************************************* */
        private void loadCurrentProvider() {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            ProviderDatabase providerDatabase = new ProviderDatabase();
            providerDatabase.getProviderById(user.getUid(), new ProviderDatabase.OnProviderLoadedListener() {
                @Override
                public void onSuccess(Provider provider) {
                    binding.editProfileFullName.setText(provider.getFullName());
                    binding.editProfileEmail.setText(provider.getEmail());
                    binding.editProfilePhone.setText(provider.getPhone());
                    binding.editProfileAddress.setText(provider.getAddress());
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        /* *******************************************************************************************
         * Handle Save button → update or migrate account
         ******************************************************************************************* */
        private void saveProfile() {
            String name = Objects.requireNonNull(binding.editProfileFullName.getText()).toString().trim();
            String email = Objects.requireNonNull(binding.editProfileEmail.getText()).toString().trim();
            String phone = Objects.requireNonNull(binding.editProfilePhone.getText()).toString().trim()
                    .replaceAll("[^0-9]", "");
            String address = Objects.requireNonNull(binding.editProfileAddress.getText()).toString().trim();

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "No authenticated user", Toast.LENGTH_SHORT).show();
                return;
            }

            String oldEmail = currentUser.getEmail();
            String oldUid = currentUser.getUid();

            // If email changes → clone + migrate account
            if (!email.equals(oldEmail)) {
                controller.cloneProviderWithServices(
                        oldUid,
                        email,
                        name,
                        phone,
                        address,
                        new ProviderController.ProviderControllerListener() {
                            @Override
                            public void onSignUpSuccess(String providerId) {
                                Toast.makeText(EditProfileActivity.this,
                                        "New account created with services cloned", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(EditProfileActivity.this,
                                        "Migration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onProviderLoaded(Provider provider) {}
                        }
                );
            } else {
                // Email unchanged → normal profile update
                controller.updateProfile(name, email, phone, address, null, controller.getListener());
            }
        }

        /* *******************************************************************************************
         * Toolbar back button
         ******************************************************************************************* */
        @Override
        public boolean onSupportNavigateUp() {
            finish();
            return true;
        }
    }
