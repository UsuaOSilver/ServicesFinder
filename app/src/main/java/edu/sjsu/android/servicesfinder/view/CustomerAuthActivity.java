package edu.sjsu.android.servicesfinder.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.controller.CustomerController;
import edu.sjsu.android.servicesfinder.controller.SessionManager;
import edu.sjsu.android.servicesfinder.databinding.ActivityCustomerAuthBinding;
import edu.sjsu.android.servicesfinder.model.Customer;
import edu.sjsu.android.servicesfinder.util.ProToast;

/**
 * CustomerAuthActivity - Customer Sign In and Sign Up
 * Allows customers to create accounts and sign in to leave reviews and manage favorites
 */
public class CustomerAuthActivity extends AppCompatActivity implements CustomerController.CustomerControllerListener {

    private ActivityCustomerAuthBinding binding;
    private CustomerController customerController;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCustomerAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        customerController = new CustomerController(this);
        customerController.setListener(this);

        setupTabLayout();
        setupSignInForm();
        setupSignUpForm();
        setupPhoneFormatting();
        setupBackButton();
        setupRoleSwitcher();
    }

    private void setupBackButton() {
        binding.backButton.setOnClickListener(v -> finish());
    }

    private void setupRoleSwitcher() {
        binding.switchToProviderButton.setOnClickListener(v -> {
            // Navigate to Provider Entry Activity
            Intent intent = new Intent(this, ProviderEntryActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupTabLayout() {
        binding.authTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Sign In tab
                    binding.signInLayout.setVisibility(View.VISIBLE);
                    binding.signUpLayout.setVisibility(View.GONE);
                } else {
                    // Sign Up tab
                    binding.signInLayout.setVisibility(View.GONE);
                    binding.signUpLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSignInForm() {
        binding.signInButton.setOnClickListener(v -> handleSignIn());
    }

    private void setupSignUpForm() {
        binding.signUpButton.setOnClickListener(v -> handleSignUp());
    }

    private void setupPhoneFormatting() {
        // Auto-format phone number as user types (US format)
        binding.signUpPhone.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            private int cursorPosition = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isFormatting) {
                    cursorPosition = binding.signUpPhone.getSelectionStart();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;
                String phone = s.toString().replaceAll("[^0-9]", "");

                if (phone.length() > 10) {
                    phone = phone.substring(0, 10);
                }

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < phone.length(); i++) {
                    if (i == 0) formatted.append("(");
                    formatted.append(phone.charAt(i));
                    if (i == 2) formatted.append(") ");
                    if (i == 5) formatted.append("-");
                }

                s.replace(0, s.length(), formatted.toString());

                // Restore cursor position
                int newCursor = Math.min(cursorPosition + (formatted.length() - s.length()), formatted.length());
                binding.signUpPhone.setSelection(Math.max(0, Math.min(newCursor, formatted.length())));

                isFormatting = false;
            }
        });
    }

    // =========================================================
    // SIGN IN LOGIC
    // =========================================================
    private void handleSignIn() {
        String emailOrPhone = binding.signInEmailOrPhone.getText().toString().trim();
        String password = binding.signInPassword.getText().toString().trim();

        // Validation
        if (emailOrPhone.isEmpty()) {
            ProToast.warning(this, "Please enter email or phone");
            return;
        }

        if (password.isEmpty()) {
            ProToast.warning(this, getString(R.string.error_password_empty));
            return;
        }

        showProgress("Signing in...");

        // Determine if input is email or phone
        if (emailOrPhone.contains("@")) {
            // Email sign-in
            customerController.signInWithEmail(emailOrPhone, password, new CustomerController.AuthCallback() {
                @Override
                public void onSuccess(String customerId) {
                    loadCustomerAndFinish(customerId);
                }

                @Override
                public void onError(String message) {
                    hideProgress();
                    ProToast.error(CustomerAuthActivity.this, message);
                }
            });
        } else {
            // Phone sign-in (no password for phone-only accounts)
            String phone = emailOrPhone.replaceAll("[^0-9]", "");
            customerController.signInWithPhone(phone, new CustomerController.AuthCallback() {
                @Override
                public void onSuccess(String customerId) {
                    loadCustomerAndFinish(customerId);
                }

                @Override
                public void onError(String message) {
                    hideProgress();
                    ProToast.error(CustomerAuthActivity.this, message);
                }
            });
        }
    }

    // =========================================================
    // SIGN UP LOGIC
    // =========================================================
    private void handleSignUp() {
        String fullName = binding.signUpFullName.getText().toString().trim();
        String email = binding.signUpEmail.getText().toString().trim();
        String phone = binding.signUpPhone.getText().toString().trim();
        String password = binding.signUpPassword.getText().toString().trim();
        String confirmPassword = binding.signUpConfirmPassword.getText().toString().trim();

        // Validation
        if (fullName.isEmpty()) {
            ProToast.warning(this, "Please enter your full name");
            return;
        }

        if (email.isEmpty() && phone.isEmpty()) {
            ProToast.warning(this, "Please enter either email or phone");
            return;
        }

        if (!email.isEmpty() && !email.contains("@")) {
            ProToast.warning(this, "Please enter a valid email");
            return;
        }

        if (!email.isEmpty()) {
            if (password.isEmpty()) {
                ProToast.warning(this, "Please enter a password");
                return;
            }

            if (password.length() < 6) {
                ProToast.warning(this, "Password must be at least 6 characters");
                return;
            }

            if (!password.equals(confirmPassword)) {
                ProToast.warning(this, "Passwords do not match");
                return;
            }
        }

        showProgress("Creating account...");

        customerController.signUp(fullName, email, phone, password);
    }

    // =========================================================
    // CONTROLLER CALLBACKS
    // =========================================================
    @Override
    public void onCustomerLoaded(Customer customer) {
        hideProgress();
        SessionManager.saveCustomer(this, customer.getId(), customer.getFullName());
        ProToast.success(this, "Welcome, " + customer.getFullName() + "!");

        // Return to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onCustomerRegistered(String customerId) {
        loadCustomerAndFinish(customerId);
    }

    @Override
    public void onError(String errorMessage) {
        hideProgress();
        ProToast.error(this, errorMessage);
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================
    private void loadCustomerAndFinish(String customerId) {
        customerController.loadCustomerById(customerId);
    }

    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgress();
        binding = null;
    }
}
