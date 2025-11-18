package edu.sjsu.android.servicesfinder.view;

import static android.content.ContentValues.TAG;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.tabs.TabLayout;
import edu.sjsu.android.servicesfinder.controller.SessionManager;
import edu.sjsu.android.servicesfinder.controller.ProviderController;
import edu.sjsu.android.servicesfinder.databinding.ActivityProviderEntryBinding;
import edu.sjsu.android.servicesfinder.model.Provider;

/* ************************************************************************************************
 * ProviderEntryActivity
 * ------------------------------------------------------------
 * Handles both Sign-In and Sign-Up for Providers.
 * Supports:
 *  - Signup with Email optional
 *  - Sign in with Email or Phone
 *
 *  Call: ProviderController for logic & database actions
 *  Use: activity_provider_entry.xml
 *
 *************************************************************************************************/
public class ProviderEntryActivity extends AppCompatActivity
        implements ProviderController.ProviderControllerListener {

    /* -------------------------- VIEW BINDING -------------------------- */
    private ActivityProviderEntryBinding binding;

    /* -------------------------- CONTROLLER ----------------------------- */
    private ProviderController providerController;
    private ProgressDialog loadingDialog;

    /* -------------------------- LIFECYCLE ------------------------------ */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === VIEW BINDING INFLATE ===
        binding = ActivityProviderEntryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("Provider Authentication");

        // Initialize controller
        providerController = new ProviderController(this);
        providerController.setListener(this);

        // Prepare UI
        setupTabs();
        setupButtons();

        // Apply phone formatters
        setupPhoneFormatters();

        // Pre-fill for testing/demo
        binding.signInEmailOrPhone.setText("vananh@gmail.com");
        binding.signInPassword.setText("123456");

        // Default tab = Sign-In
        showSignIn();
    }

    /* =========================================================
       INITIALIZATION METHODS
    ========================================================= */
    private void setupPhoneFormatters() {
        // Apply automatic US phone formatting (###-###-####)
        // for signUpPhone
        binding.signUpPhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher("US"));

        // for signInPhone, complicated because it relate to email
        binding.signInEmailOrPhone.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                String input = s.toString();

                // If ANY letter exists → treat as email → DO NOTHING
                if (input.matches(".*[a-zA-Z].*")) {
                    return;
                }

                // Keep only digits
                String digits = input.replaceAll("[^0-9]", "");

                if (digits.isEmpty()) {
                    return;
                }

                if (digits.length() > 10) {
                    digits = digits.substring(0, 10);
                }

                // Format ###-###-####
                StringBuilder sb = new StringBuilder();
                int len = digits.length();

                for (int i = 0; i < len; i++) {
                    if (i == 3 || i == 6) {
                        sb.append('-');
                    }
                    sb.append(digits.charAt(i));
                }

                String formatted = sb.toString();

                // Prevent infinite loop
                isFormatting = true;
                s.replace(0, s.length(), formatted);
                isFormatting = false;
            }
        });
    }

    private void setupTabs() {
        // Toggle between Sign-In and Sign-Up layouts
        binding.authTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) showSignIn(); else showSignUp();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupButtons() {
        binding.signInButton.setOnClickListener(v -> handleSignIn());
        binding.signUpButton.setOnClickListener(v -> handleSignUp());
        binding.signInCancelButton.setOnClickListener(v -> finish());
        binding.signUpCancelButton.setOnClickListener(v -> finish());
    }

    /* =========================================================
       TAB SWITCHING
       ========================================================= */
    private void showSignIn() {
        binding.signInLayout.setVisibility(View.VISIBLE);
        binding.signUpLayout.setVisibility(View.GONE);
    }

    private void showSignUp() {
        binding.signInLayout.setVisibility(View.GONE);
        binding.signUpLayout.setVisibility(View.VISIBLE);
    }

    /* =========================================================
       SIGN-IN LOGIC
       ========================================================= */
    private void handleSignIn() {
        String emailOrPhone = getText(binding.signInEmailOrPhone);
        String password = getText(binding.signInPassword);

        // Validation
        if (TextUtils.isEmpty(emailOrPhone)) {
            binding.signInEmailOrPhone.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.signInPassword.setError("Required");
            return;
        }

        Log.d(TAG, "handleSignIn(): emailOrPhone=" + emailOrPhone);

        showLoading("Signing in...");

        // EMAIL LOGIN
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()) {
            Log.d(TAG, "handleSignIn(): detected EMAIL login");
            providerController.signInWithEmail(emailOrPhone, password,
                    new ProviderController.AuthCallback() {
                        @Override
                        public void onSuccess(String providerId) {
                            Log.d(TAG, "signInWithEmail SUCCESS, providerId=" + providerId);
                            hideLoading();
                            providerController.loadProviderById(providerId);
                        }

                        @Override
                        public void onError(String msg) {
                            Log.e(TAG, "signInWithEmail ERROR: " + msg, new Exception("signInWithEmail"));
                            hideLoading();
                            ProviderEntryActivity.this.onError(msg);
                        }
                    });
        } else {
            // PHONE LOGIN
            Log.d(TAG, "handleSignIn(): detected PHONE login");
            providerController.signInWithPhone(emailOrPhone, password,
                    new ProviderController.AuthCallback() {
                        @Override
                        public void onSuccess(String providerId) {
                            Log.d(TAG, "signInWithPhone SUCCESS, providerId=" + providerId);
                            hideLoading();
                            providerController.loadProviderById(providerId);
                        }

                        @Override
                        public void onError(String msg) {
                            Log.e(TAG, "signInWithPhone ERROR: " + msg, new Exception("signInWithPhone"));
                            hideLoading();
                            ProviderEntryActivity.this.onError(msg);
                        }
                    });
        }
    }

    /* =========================================================
       SIGN-UP LOGIC (EMAIL OPTIONAL)
       ========================================================= */
    private void handleSignUp() {
        String fullName = getText(binding.signUpFullName);
        String email    = getText(binding.signUpEmail);
        String phone    = getText(binding.signUpPhone);
        String address  = getText(binding.signUpAddress);
        String password = getText(binding.signUpPassword);
        String confirm  = getText(binding.signUpConfirmPassword);

        // Validate all inputs
        if (!validateSignUpInputs(fullName, email, phone, address, password, confirm))
            return;

        showLoading("Creating account...");

        // Sign up
        providerController.signUp(fullName, email, phone, address, password);
    }

    /* =========================================================
       INPUT VALIDATION
       ========================================================= */
    private boolean validateSignUpInputs(String fullName, String email, String phone,
                                         String address, String password, String confirm) {
        if (TextUtils.isEmpty(fullName)) { binding.signUpFullName.setError("Required"); return false; }
        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.signUpEmail.setError("Invalid email"); return false;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() != 10) { binding.signUpPhone.setError("10 digits"); return false; }
        if (TextUtils.isEmpty(address)) { binding.signUpAddress.setError("Required"); return false; }
        if (password.length() < 6) { binding.signUpPassword.setError("Min 6 chars"); return false; }
        if (!password.equals(confirm)) { binding.signUpConfirmPassword.setError("Mismatch"); return false; }
        return true;
    }

    /* =========================================================
       UTILITIES & HELPERS
       ========================================================= */
    private String getText(TextInputEditText edit) {
        Editable e = edit.getText();
        return e != null ? e.toString().trim() : "";
    }

    private void showLoading(String msg) {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setCancelable(false);
        }
        loadingDialog.setMessage(msg);
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    /* =========================================================
       CONTROLLER CALLBACK IMPLEMENTATION
       ========================================================= */
    @Override
    public void onProviderLoaded(Provider provider) {
        hideLoading();
        Log.d("ProviderEntryActivity", "Provider loaded: " + provider.getId());

        // Save provider session
        SessionManager.saveProvider(this, provider.getId(), provider.getFullName());

        // Wrap navigation in try-catch to isolate crash
        try {
            navigateToDashboard(provider);
        } catch (Exception e) {
            Log.e("NavigationError", "Failed to navigate to dashboard", e);
            Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSignUpSuccess(String providerId) {
        hideLoading();
        clearSignUpForm();
        launchProviderDashboard();
    }

    @Override
    public void onError(String msg) {
        Log.d("onError", "Entered onError()");
        hideLoading();

        try {
            String safeMsg = msg != null ? msg : "Unknown error";
            Toast.makeText(ProviderEntryActivity.this, "EEEEEEError: " + safeMsg, Toast.LENGTH_LONG).show();
            Log.d("onError", "Toast shown");
        } catch (Exception e) {
            Log.e("onError", "Toast crashed", e);
        }
    }

    /* =========================================================
       NAVIGATION HELPERS
       ========================================================= */
    private void navigateToDashboard(Provider provider) {
        Log.d("ProviderEntryActivity", "Navigating to dashboard for: " + provider.getFullName());
        Intent i = new Intent(this, ProviderDashboardActivity.class);
        i.putExtra("providerId", provider.getId());
        i.putExtra("providerName", provider.getFullName());
        startActivity(i);
    }

    private void clearSignUpForm() {
        binding.signUpFullName.setText("");
        binding.signUpEmail.setText("");
        binding.signUpPhone.setText("");
        binding.signUpAddress.setText("");
        binding.signUpPassword.setText("");
        binding.signUpConfirmPassword.setText("");
    }

    private void launchProviderDashboard() {
        Intent intent = new Intent(this, ProviderDashboardActivity.class);
        startActivity(intent);
        finish();
    }

    /* =========================================================
       ON DESTROY (PREVENT MEMORY LEAK)
       ========================================================= */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Prevent memory leaks
    }
}