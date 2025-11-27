package edu.sjsu.android.servicesfinder.controller;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.sjsu.android.servicesfinder.R;
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
        this.providerDatabase = new ProviderDatabase(context);
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
            String msg = context.getString(R.string.provider_id_is_required);
            if (listener != null) listener.onError(msg);
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
                        assert auth.getCurrentUser() != null;
                        String uid = auth.getCurrentUser().getUid();
                        createAndSaveProvider(uid, fullName, email, phone, address, password);
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) {
                            String signupErMsg = context.getString(R.string.sign_up_failed);
                            String errorMsg = signupErMsg + e.getMessage();
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                errorMsg = context.getString(R.string.email_already_in_use);
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
                        callback.onError(context.getString(R.string.error_null_user));
                })
                .addOnFailureListener(e -> callback.onError(context.getString(R.string.error_email_signin_failed, e.getMessage())));
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
                    callback.onError(context.getString(R.string.error_invalid_password));

            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(context.getString(R.string.error_no_account_with_phone));
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
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        provider.setPhone(digitsOnly);
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
                String onErMsg = context.getString(R.string.error_saving_provider);
                if (listener != null) listener.onError(onErMsg + errorMessage);
            }
        });
    }

    /* ***************************************************************************************
    // LISTENER
    ******************************************************************************************/
    public interface ProviderControllerListener {
        void onProviderLoaded(Provider provider);
        void onSignUpSuccess(String providerId); // also used for PasswordUpdateSuccess
        void onError(String errorMessage);
    }

    /* ***************************************************************************************
    //  Updates password in Firebase password.
    //  Validates input and triggers success/error callback.
    ******************************************************************************************/
    public void updatePassword(String newPassword, ProviderControllerListener callback) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            callback.onError(context.getString(R.string.error_password_empty));
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(context.getString(R.string.error_no_authenticated_user));
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
                            callback.onSignUpSuccess(context.getString(R.string.success_password_updated));
                        }

                        @Override
                        public void onError(String errorMessage) {
                            callback.onError(context.getString(R.string.error_password_updated_firestore_failed, errorMessage));
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError(context.getString(R.string.error_password_update_failed, e.getMessage())));

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
            callback.onError(context.getString(R.string.error_no_authenticated_user));
            return;
        }

        providerDatabase.updateProviderFields(user.getUid(), updates, new ProviderDatabase.OnProviderOperationListener() {
            @Override
            public void onSuccess(String message) {
                callback.onSignUpSuccess(context.getString(R.string.success_profile_updated));
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(context.getString(R.string.error_profile_update_failed, errorMessage));
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
                            callback.onSuccess(context.getString(R.string.success_account_deleted));
                        })
                        .addOnFailureListener(e -> {
                            // If re-authentication is required, try to get it from user
                            if (Objects.requireNonNull(e.getMessage()).contains("recent") || e.getMessage().contains("reauthenticate")) {
                                // Try force delete by getting fresh token
                                user.getIdToken(true)
                                        .addOnSuccessListener(result -> {
                                            user.delete()
                                                    .addOnSuccessListener(unused -> {
                                                        callback.onSuccess(context.getString(R.string.success_account_deleted));
                                                    })
                                                    .addOnFailureListener(ex -> {
                                                        callback.onError(context.getString(R.string.error_sign_out_then_delete));

                                                    });
                                        })
                                        .addOnFailureListener(ex -> {
                                            callback.onError(context.getString(R.string.error_sign_out_then_delete));

                                        });
                            } else {
                                callback.onError(context.getString(R.string.error_account_delete_failed, e.getMessage()));
                            }
                        });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(context.getString(R.string.error_provider_data_delete_failed, errorMessage));
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
                                callback.onError(context.getString(R.string.error_user_creation_failed));
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
                                                                               // callback.onSignUpSuccess("Migration complete: new account created, services cloned, old account deleted");
                                                                                callback.onSignUpSuccess(context.getString(R.string.success_password_updated));
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                callback.onSignUpSuccess(context.getString(R.string.error_old_account_delete_failed));
                                                                            });
                                                                } else {
                                                                    callback.onSignUpSuccess(context.getString(R.string.warning_old_firebase_user_not_found));
                                                                }
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                callback.onSignUpSuccess(e.getMessage());
                                                            });
                                                }

                                                @Override
                                                public void onError(String errorMessage) {
                                                    callback.onSignUpSuccess(errorMessage);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            callback.onError(errorMessage);
                                        }
                                    });
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    callback.onError(context.getString(R.string.error_save_new_provider_failed, errorMessage));
                                }
                            });
                        })
                        .addOnFailureListener(e -> callback.onError(context.getString(R.string.error_new_account_creation_failed, e.getMessage())));
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(context.getString(R.string.error_old_provider_load_failed, errorMessage));
            }
        });
    }
}