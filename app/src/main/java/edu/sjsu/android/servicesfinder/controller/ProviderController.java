package edu.sjsu.android.servicesfinder.controller;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import edu.sjsu.android.servicesfinder.database.FirestoreHelper;
import edu.sjsu.android.servicesfinder.database.ProviderDatabase;
import edu.sjsu.android.servicesfinder.model.Provider;



/* *****************************************************************************************
 * PROVIDERCONTROLLER  *
 * Handles:
 * - Provider registration (FirebaseAuth or Firestore)
 * - Provider login validation
 * - Provider data loading
 * - Input validation (prevent invalid input) and sanitization (remove dangerous characters)
 ***********************************************************************************************/
public class ProviderController {

    private final ProviderDatabase providerDatabase;
    private final FirebaseAuth auth;
    private ProviderControllerListener listener;

    private final Context context;
    public ProviderController(Context context) {
        this.providerDatabase = new ProviderDatabase();
        this.auth = FirebaseAuth.getInstance();
        this.context = context;
    }

    public void setListener(ProviderControllerListener listener) {
        this.listener = listener;
    }
    public ProviderControllerListener getListener() {
        return listener;
    }

    /* ****************************************************************************************
    // REGISTER PROVIDER
    ****************************************************************************************/
    public void registerProvider(Provider provider, ProviderDatabase.OnProviderOperationListener debugFlow) {
        provider.setFullName(FirestoreHelper.sanitizeString(provider.getFullName()));
        provider.setEmail(FirestoreHelper.sanitizeString(provider.getEmail()));
        provider.setPhone(FirestoreHelper.sanitizeString(provider.getPhone()));
        provider.setAddress(FirestoreHelper.sanitizeString(provider.getAddress()));

        providerDatabase.addProvider(provider, new ProviderDatabase.OnProviderOperationListener() {
            @Override
            public void onSuccess(String message) {
                debugFlow.onSuccess(message);
            }

            @Override
            public void onError(String errorMessage) {
                debugFlow.onError(errorMessage);
            }
        });
    }

    /* ****************************************************************************************
    // LOAD PROVIDER
    ******************************************************************************************/
    public void loadProviderById(String providerId) {
        if (providerId == null || providerId.trim().isEmpty()) {
            if (listener != null) listener.onError("Provider ID is required");
            return;
        }

        providerDatabase.getProviderById(providerId, new ProviderDatabase.OnProviderLoadedListener() {
            @Override
            public void onSuccess(Provider provider) {
                if (listener != null) listener.onProviderLoaded(provider);
            }

            @Override
            public void onError(String errorMessage) {
                if (listener != null) listener.onError(errorMessage);
            }
        });
    }

    /* *****************************************************************************************
    // SIGN-UP
    ******************************************************************************************/
    public void signUp(String fullName, String email, String phone,
                       String address, String password) {

        if (email != null && !email.trim().isEmpty()) {
            // Normal Firebase email sign-up
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        String uid = auth.getCurrentUser().getUid();
                        createAndSaveProvider(uid, fullName, email, phone, address, password);
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) {
                            String errorMsg = "Sign-up failed: " + e.getMessage();
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                errorMsg = "Email already in use";
                            }
                            listener.onError(errorMsg);
                        }
                    });

        } else {
            // Phone-only sign-up (Firestore only, no FirebaseAuth)
            String fakeUid = "P_" + System.currentTimeMillis();
            createAndSaveProvider(fakeUid, fullName, "", phone, address, password);
        }
    }

    /* *****************************************************************************************
    // SIGN-IN
    ******************************************************************************************/
    public interface AuthCallback {
        void onSuccess(String providerId);
        void onError(String message);
    }
    /* *****************************************************************************************
    // Email-based sign-in via FirebaseAuth
    /* *****************************************************************************************/
    public void signInWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() != null)
                        callback.onSuccess(result.getUser().getUid());
                    else
                        callback.onError("Null user returned");
                })
                .addOnFailureListener(e -> callback.onError("Email sign-in failed: " + e.getMessage()));
    }
    /* *****************************************************************************************
    // Phone-based sign-in via Firestore
    ******************************************************************************************/
    public void signInWithPhone(String phone, String password, AuthCallback callback) {
        phone = phone.replaceAll("[^0-9]", "");
        providerDatabase.getProviderByPhone(phone, new ProviderDatabase.OnProviderLoadedListener() {
            @Override
            public void onSuccess(Provider provider) {
                if (provider.getPassword().equals(password))
                    callback.onSuccess(provider.getId());
                else
                    callback.onError("Invalid password");
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("No account found with this phone number");
            }
        });
    }

    /* **************************************************************************************
    // SAVE PROVIDER
    **************************************************************************************/
    private void createAndSaveProvider(String uid, String fullName, String email,
                                       String phone, String address, String password) {
        Provider provider = new Provider();
        provider.setId(uid);
        provider.setFullName(fullName);
        provider.setEmail(email);
        provider.setPhone(phone);
        provider.setAddress(address);
        provider.setPassword(password);

        registerProvider(provider, new ProviderDatabase.OnProviderOperationListener() {
            @Override
            public void onSuccess(String message) {
                // Save session locally for both email + phone signup
                if (listener instanceof Context) {
                    Context ctx = (Context) listener;
                    SessionManager.saveProvider(ctx, uid, fullName);
                }

                if (listener != null) {
                    listener.onSignUpSuccess(uid);
                } else {
                    Log.e("ProviderController", "Listener is null during onSignUpSuccess");
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (listener != null) listener.onError("Error saving provider: " + errorMessage);
            }
        });
    }

    /* ***************************************************************************************
    // LISTENER
    ******************************************************************************************/
    public interface ProviderControllerListener {
        void onProviderLoaded(Provider provider);
        void onSignUpSuccess(String providerId); // alse used for PasswordUpdateSuccess
        void onError(String errorMessage);
    }

    /* ***************************************************************************************
    //  Updates password in Firebase password.
    //  Validates input and triggers success/error callback.
    ******************************************************************************************/
    public void updatePassword(String newPassword, ProviderControllerListener callback) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            callback.onError("Password cannot be empty");
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No authenticated user");
            return;
        }

        user.updatePassword(newPassword)
                .addOnSuccessListener(aVoid -> {
                    // Optional Firestore sync
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("password", newPassword);

                    providerDatabase.updateProviderFields(user.getUid(), updates, new ProviderDatabase.OnProviderOperationListener() {
                        @Override
                        public void onSuccess(String message) {
                            callback.onSignUpSuccess("Password updated successfully");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            callback.onError("Password updated in Firebase, but failed in Firestore: " + errorMessage);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError("Failed to update password: " + e.getMessage()));
    }

    /* ***************************************************************************************
     * Updates the current user's profile fields in Firestore.
     * Accepts full name and email, sanitizes input, and updates Firestore document.
     ****************************************************************************************/
    public void updateProfile(String fullName, String email, String phone, String address, String password, ProviderControllerListener callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("address", address);

        // Optional: only update password if it's non-empty
        if (password != null && !password.trim().isEmpty()) {
            updates.put("password", password);
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No authenticated user");
            return;
        }

        providerDatabase.updateProviderFields(user.getUid(), updates, new ProviderDatabase.OnProviderOperationListener() {
            @Override
            public void onSuccess(String message) {
                callback.onSignUpSuccess("Profile updated successfully");
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("Failed to update profile: " + errorMessage);
            }
        });

        // Optional: update FirebaseAuth password
        if (password != null && !password.trim().isEmpty()) {
            user.updatePassword(password)
                    .addOnSuccessListener(aVoid -> Log.d("ProviderController", "Firebase password updated"))
                    .addOnFailureListener(e -> Log.e("ProviderController", "Failed to update Firebase password", e));
        }
    }

    /* ************************************************
    // REPLACE YOUR deleteAccount METHOD WITH THIS
    ***************************************************/

    public void deleteAccount(DeleteAccountCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No authenticated user");
            return;
        }

        String uid = user.getUid();

        // Delete Firestore data first
        providerDatabase.deleteProvider(uid, new ProviderDatabase.OnProviderOperationListener() {
            @Override
            public void onSuccess(String message) {
                // Delete Authentication user (THIS DELETES THE EMAIL)
                user.delete()
                        .addOnSuccessListener(aVoid -> {
                            callback.onSuccess("Account deleted successfully");
                        })
                        .addOnFailureListener(e -> {
                            // If re-authentication is required, try to get it from user
                            if (Objects.requireNonNull(e.getMessage()).contains("recent") || e.getMessage().contains("reauthenticate")) {
                                // Try force delete by getting fresh token
                                user.getIdToken(true)
                                        .addOnSuccessListener(result -> {
                                            user.delete()
                                                    .addOnSuccessListener(unused -> {
                                                        callback.onSuccess("Account deleted successfully");
                                                    })
                                                    .addOnFailureListener(ex -> {
                                                        callback.onError("Please sign out and sign in again, then try deleting your account");
                                                    });
                                        })
                                        .addOnFailureListener(ex -> {
                                            callback.onError("Please sign out and sign in again, then try deleting your account");
                                        });
                            } else {
                                callback.onError("Failed to delete account: " + e.getMessage());
                            }
                        });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("Failed to delete provider data: " + errorMessage);
            }
        });
    }

    /* ******************************************************
    // Callback for ProviderDashboardActivity
    ********************************************************/
    public interface DeleteAccountCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /* ***************************************************************************************************
     We can not to verify email with free Firebase, so e use "create new and delete old" to change email
    *******************************************************************************************************/
    public void cloneProviderWithServices(String oldUid, String newEmail, String fullName, String phone, String address, ProviderControllerListener callback) {
        providerDatabase.getProviderById(oldUid, new ProviderDatabase.OnProviderLoadedListener() {
            @Override
            public void onSuccess(Provider oldProvider) {
                String password = oldProvider.getPassword(); // reuse old password

                auth.createUserWithEmailAndPassword(newEmail, password)
                        .addOnSuccessListener(result -> {
                            FirebaseUser newUser = result.getUser();
                            if (newUser == null) {
                                callback.onError("New user creation failed");
                                return;
                            }

                            Provider newProvider = new Provider();
                            newProvider.setId(newUser.getUid());
                            newProvider.setFullName(fullName);
                            newProvider.setEmail(newEmail);
                            newProvider.setPhone(phone);
                            newProvider.setAddress(address);
                            newProvider.setPassword(password);

                            providerDatabase.addProvider(newProvider, new ProviderDatabase.OnProviderOperationListener() {
                                @Override
                                public void onSuccess(String message) {
                                    providerDatabase.cloneServices(oldUid, newUser.getUid(), new ProviderDatabase.OnProviderOperationListener() {
                                        @Override
                                        public void onSuccess(String msg) {
                                            // Delete old Firestore provider document
                                            providerDatabase.deleteProvider(oldUid, new ProviderDatabase.OnProviderOperationListener() {
                                                @Override
                                                public void onSuccess(String deletionMsg) {
                                                    // Now delete old FirebaseAuth account
                                                    auth.signInWithEmailAndPassword(oldProvider.getEmail(), oldProvider.getPassword())
                                                            .addOnSuccessListener(authResult -> {
                                                                FirebaseUser oldUser = authResult.getUser();
                                                                if (oldUser != null) {
                                                                    oldUser.delete()
                                                                            .addOnSuccessListener(aVoid -> {
                                                                                Log.d("ProviderController", "Old FirebaseAuth account deleted");
                                                                                callback.onSignUpSuccess("Migration complete: new account created, services cloned, old account deleted");
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                Log.e("ProviderController", "Failed to delete old FirebaseAuth account", e);
                                                                                callback.onSignUpSuccess("New provider cloned, old Firestore deleted, but failed to delete old FirebaseAuth account");
                                                                            });
                                                                } else {
                                                                    callback.onSignUpSuccess("New provider cloned, but old FirebaseAuth user not found");
                                                                }
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Log.e("ProviderController", "Failed to re-authenticate old account", e);
                                                                callback.onSignUpSuccess("New provider cloned, but failed to re-authenticate old account: " + e.getMessage());
                                                            });
                                                }

                                                @Override
                                                public void onError(String errorMessage) {
                                                    callback.onSignUpSuccess("New provider cloned, but failed to delete old Firestore provider: " + errorMessage);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            callback.onError("Provider cloned, but failed to copy services: " + errorMessage);
                                        }
                                    });
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    callback.onError("Failed to save new provider: " + errorMessage);
                                }
                            });
                        })
                        .addOnFailureListener(e -> callback.onError("Failed to create new account: " + e.getMessage()));
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("Failed to load old provider: " + errorMessage);
            }
        });
    }
}