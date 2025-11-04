package edu.sjsu.android.servicesfinder.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.controller.ProviderController;
import edu.sjsu.android.servicesfinder.database.ProviderDatabase;
import edu.sjsu.android.servicesfinder.model.Provider;

/* =========================================================================
   ProviderEntryActivity – DEBUG VERSION
   ========================================================================= */
public class ProviderEntryActivity extends AppCompatActivity
        implements ProviderController.ProviderControllerListener {

    /* -------------------------- UI -------------------------- */
    private TabLayout tabLayout;
    private View signInLayout, signUpLayout;

    private TextInputEditText signInEmail, signInPassword;
    private Button signInButton, signInCancelButton;

    private TextInputEditText signUpFullName, signUpEmail, signUpPhone;
    private TextInputEditText signUpAddress, signUpPassword, signUpConfirmPassword;
    private Button signUpButton, signUpCancelButton;

    /* ----------------------- Firebase ----------------------- */
    private ProviderController providerController;
    private FirebaseAuth auth;

    private ProgressDialog loadingDialog;

    /* ===================================================================== */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_entry);
        setTitle("Provider Authentication");

        auth = FirebaseAuth.getInstance();
        providerController = new ProviderController();
        providerController.setListener(this);

        initializeViews();
        setupTabs();
        setupButtons();
        showSignIn();
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.authTabLayout);
        signInLayout = findViewById(R.id.signInLayout);
        signUpLayout = findViewById(R.id.signUpLayout);

        signInEmail = findViewById(R.id.signInEmail);
        signInPassword = findViewById(R.id.signInPassword);
        signInButton = findViewById(R.id.signInButton);
        signInCancelButton = findViewById(R.id.signInCancelButton);

        signUpFullName = findViewById(R.id.signUpFullName);
        signUpEmail = findViewById(R.id.signUpEmail);
        signUpPhone = findViewById(R.id.signUpPhone);
        signUpAddress = findViewById(R.id.signUpAddress);
        signUpPassword = findViewById(R.id.signUpPassword);
        signUpConfirmPassword = findViewById(R.id.signUpConfirmPassword);
        signUpButton = findViewById(R.id.signUpButton);
        signUpCancelButton = findViewById(R.id.signUpCancelButton);

    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Sign In"));
        tabLayout.addTab(tabLayout.newTab().setText("Sign Up"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) showSignIn(); else showSignUp();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupButtons() {
        signInButton.setOnClickListener(v -> handleSignIn());
        signUpButton.setOnClickListener(v -> handleSignUp());
        signInCancelButton.setOnClickListener(v -> finish());
        signUpCancelButton.setOnClickListener(v -> finish());

    }

    private void showSignIn() {
        signInLayout.setVisibility(View.VISIBLE);
        signUpLayout.setVisibility(View.GONE);
    }

    private void showSignUp() {
        signInLayout.setVisibility(View.GONE);
        signUpLayout.setVisibility(View.VISIBLE);
    }

    /* --------------------------- SIGN-IN --------------------------- */
    private void handleSignIn() {
        String email = getText(signInEmail);
        String password = getText(signInPassword);

        if (BuildConfig.DEBUG) {
            Log.d("DEBUG_SIGNIN", "Attempting sign-in with:");
            Log.d("DEBUG_SIGNIN", "Email    : '" + email + "'");
            Log.d("DEBUG_SIGNIN", "Password : '" + password + "' (len=" + password.length() + ")");
            Log.d("DEBUG_SIGNIN", "FirebaseAuth instance: " + auth);
        }

        if (!validateSignInInputs(email, password)) return;

        showLoading("Signing in...");
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (BuildConfig.DEBUG && user != null) {
                        Log.d("DEBUG_SIGNIN", "Sign-in SUCCESS – UID: " + user.getUid());
                    }
                    providerController.loadProviderById(user.getUid());
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Log.e("DEBUG_SIGNIN", "Sign-in FAILED", e);
                    if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
                        FirebaseAuthException fae = (FirebaseAuthException) e;
                        Log.e("DEBUG_SIGNIN", "Error code: " + fae.getErrorCode());
                        Log.e("DEBUG_SIGNIN", "Error message: " + fae.getMessage());
                    }
                    onError("Authentication failed: " + e.getMessage());
                });
    }

    private void handleSignUp() {
        String fullName = getText(signUpFullName);
        String email    = getText(signUpEmail);
        String phone    = getText(signUpPhone);
        String address  = getText(signUpAddress);
        String password = getText(signUpPassword);
        String confirm  = getText(signUpConfirmPassword);

        if (BuildConfig.DEBUG) {
            Log.d("DEBUG_INPUT", "=== SIGN-UP INPUT DUMP ===");
            Log.d("DEBUG_INPUT", "FullName : '" + fullName + "' (len=" + fullName.length() + ")");
            Log.d("DEBUG_INPUT", "Email    : '" + email + "' (len=" + email.length() + ")");
            Log.d("DEBUG_INPUT", "Phone    : '" + phone + "' (len=" + phone.length() + ")");
            Log.d("DEBUG_INPUT", "Address  : '" + address + "' (len=" + address.length() + ")");
            Log.d("DEBUG_INPUT", "Password : '" + password + "' (len=" + password.length() + ")");
            Log.d("DEBUG_INPUT", "Confirm  : '" + confirm + "' (len=" + confirm.length() + ")");
            Log.d("DEBUG_INPUT", "FirebaseAuth instance: " + auth);
            Log.d("DEBUG_INPUT", "==================================");
        }

        if (!validateSignUpInputs(fullName, email, phone, address, password, confirm))
            return;

        showLoading("Creating account...");

        if (TextUtils.isEmpty(email)) {
            if (BuildConfig.DEBUG) Log.d("DEBUG_AUTH", "Starting ANONYMOUS sign-up");
            auth.signInAnonymously()
                    .addOnSuccessListener(result -> {
                        FirebaseUser user = auth.getCurrentUser();
                        assert user != null;
                        if (BuildConfig.DEBUG) Log.d("DEBUG_AUTH", "ANONYMOUS SUCCESS – UID: " + user.getUid());
                        saveProvider(user.getUid(), fullName, "", phone, address, password); // ✅ FIXED: added password
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        if (BuildConfig.DEBUG) Log.e("DEBUG_AUTH", "ANONYMOUS FAILED", e);
                        resetSignUpFormWithError("Anonymous sign-up failed: " + e.getMessage());
                    });
        } else {
            if (BuildConfig.DEBUG) Log.d("DEBUG_AUTH", "Starting EMAIL sign-up – email: " + email);
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        FirebaseUser user = auth.getCurrentUser();
                        assert user != null;
                        if (BuildConfig.DEBUG) Log.d("DEBUG_AUTH", "EMAIL SUCCESS – UID: " + user.getUid());
                        saveProvider(user.getUid(), fullName, email, phone, address, password); //  FIXED: added password
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        if (BuildConfig.DEBUG) Log.e("DEBUG_AUTH", "EMAIL FAILED", e);
                        if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            signUpEmail.setText("");
                            signUpEmail.requestFocus();
                            resetSignUpFormWithError("Email already in use – try another or leave blank.");
                        } else {
                            if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
                                FirebaseAuthException fae = (FirebaseAuthException) e;
                                Log.e("DEBUG_AUTH", "Error code: " + fae.getErrorCode());
                                Log.e("DEBUG_AUTH", "Error message: " + fae.getMessage());
                            }
                            resetSignUpFormWithError("Sign-up failed: " + e.getMessage());
                        }
                    });
        }
    }



    private void saveProvider(String uid, String fullName, String email,
                              String phone, String address, String password) {
        Provider provider = new Provider();
        provider.setId(uid);
        provider.setFullName(fullName);
        provider.setEmail(email);
        provider.setPhone(phone);
        provider.setAddress(address);
        provider.setPassword(password);

        providerController.registerProvider(provider, new ProviderDatabase.OnProviderOperationListener() {
            @Override
            public void onSuccess(String message) {
                if (BuildConfig.DEBUG) Log.d("DEBUG_FLOW", "Provider saved: " + message);
                launchProviderDashboard(); //  Launch dashboard here
            }

            @Override
            public void onError(String errorMessage) {
                hideLoading();
                resetSignUpFormWithError("Error saving provider: " + errorMessage);
            }
        });
    }


    /* --------------------------- VALIDATION --------------------------- */
    private boolean validateSignInInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) { signInEmail.setError("Required"); return false; }
        if (TextUtils.isEmpty(password)) { signInPassword.setError("Required"); return false; }
        return true;
    }

    private boolean validateSignUpInputs(String fullName, String email, String phone,
                                         String address, String password, String confirm) {
        if (TextUtils.isEmpty(fullName)) { signUpFullName.setError("Required"); return false; }
        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signUpEmail.setError("Invalid email"); return false;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() != 10) { signUpPhone.setError("10 digits"); return false; }
        if (TextUtils.isEmpty(address)) { signUpAddress.setError("Required"); return false; }
        if (password.length() < 6) { signUpPassword.setError("Min 6 chars"); return false; }
        if (!password.equals(confirm)) { signUpConfirmPassword.setError("Mismatch"); return false; }
        return true;
    }

    /* --------------------------- HELPERS --------------------------- */
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

    private void resetSignUpFormWithError(String errorMessage) {
        hideLoading();
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        signUpPassword.setText("");
        signUpConfirmPassword.setText("");
        if (TextUtils.isEmpty(getText(signUpEmail))) {
            signUpEmail.requestFocus();
        } else {
            signUpPassword.requestFocus();
        }
    }

    private void navigateToDashboard(Provider provider) {
        Intent i = new Intent(this, ProviderDashboardActivity.class);
        i.putExtra("providerId", provider.getId());
        i.putExtra("providerName", provider.getFullName());
        //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        //finish();
    }

    private String getText(TextInputEditText edit) {
        Editable e = edit.getText();
        return e != null ? e.toString().trim() : "";
    }

    /* ------------------- ProviderControllerListener ------------------- */
    @Override public void onProviderLoaded(Provider provider) {
        hideLoading();
        navigateToDashboard(provider);
    }



    @Override public void onError(String errorMessage) {
        hideLoading();
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void clearSignUpForm() {
        signUpFullName.setText("");
        signUpEmail.setText("");
        signUpPhone.setText("");
        signUpAddress.setText("");
        signUpPassword.setText("");
        signUpConfirmPassword.setText("");
    }


    private void launchProviderDashboard() {
        Intent intent = new Intent(this, ProviderDashboardActivity.class);
        startActivity(intent);
        finish(); // Optional: close sign-up screen
    }

}