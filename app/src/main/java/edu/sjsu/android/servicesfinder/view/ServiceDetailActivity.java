package edu.sjsu.android.servicesfinder.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.controller.FirestoreStringTranslator;
import edu.sjsu.android.servicesfinder.controller.HomeController;
import edu.sjsu.android.servicesfinder.controller.ReviewAdapter;
import edu.sjsu.android.servicesfinder.database.ReviewDatabase;
import edu.sjsu.android.servicesfinder.databinding.ActivityServiceDetailBinding;
import edu.sjsu.android.servicesfinder.model.Review;

//******************************************************************************************
// * ServiceDetailActivity - Shows complete service details with contact options
//******************************************************************************************
public class ServiceDetailActivity extends AppCompatActivity {

    // Service info
    private String serviceTitle;
    private String serviceDescription;
    private String servicePricing;
    private String serviceCategory;
    private String serviceArea;
    private String serviceAvailability;
    private String serviceContactPreference;
    private String serviceImageUrl;

    // Provider info
    private String providerName;
    private String providerPhone;
    private String providerEmail;
    private String providerAddress;

    // === VIEW BINDING ===
    private ActivityServiceDetailBinding binding;
    // REVIEW
    private ReviewDatabase reviewDatabase;
    private String providerId;
    private ReviewAdapter reviewAdapter;


    //**********************************************************************************************************
    //  Entry point of the Activity. Initializes the layout, enables the back button in the ActionBar,
    //  sets the title, and triggers the sequence of data extraction, view initialization, data binding, and action setup.
    //**********************************************************************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === VIEW BINDING INFLATE ===
        binding = ActivityServiceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);      // Enable back button in the ActionBar
            getSupportActionBar().setTitle(getString(R.string.title_service_details));
            // Set ActionBar background color
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(ContextCompat.getColor(this, R.color.sf_primary))
            );
        }
        // REVIEW
        reviewDatabase = new ReviewDatabase();
        providerId = getIntent().getStringExtra("providerId");
        Log.e("DEBUG_REVIEW", "ServiceDetailActivity opened with providerId = " + providerId);




        getIntentExtras();  // Retrieves data passed from the main activity
        displayServiceInfo();
        setupActionButtons();

        setupReviewsSection();
    }

    // Retrieves service and provider data passed from previous activity
    private void getIntentExtras() {
        Intent intent = getIntent();

        // Service data
        serviceTitle = intent.getStringExtra("serviceTitle");
        serviceDescription = intent.getStringExtra("serviceDescription");
        servicePricing = intent.getStringExtra("servicePricing");
        serviceCategory = intent.getStringExtra("serviceCategory");
        serviceArea = intent.getStringExtra("serviceArea");
        serviceAvailability = intent.getStringExtra("serviceAvailability");
        serviceContactPreference = intent.getStringExtra("serviceContactPreference");
        serviceImageUrl = intent.getStringExtra("serviceImageUrl");

        // Provider data
        providerName = intent.getStringExtra("providerName");
        providerPhone = intent.getStringExtra("providerPhone");
        providerEmail = intent.getStringExtra("providerEmail");
        providerAddress = intent.getStringExtra("providerAddress");
    }

    // Displays all service and provider info in UI
    private void displayServiceInfo() {
        // Title
        binding.serviceDetailTitle.setText(serviceTitle);

        // Pricing
        if (servicePricing != null && !servicePricing.isEmpty()) {
            binding.serviceDetailPricing.setText(servicePricing);
            binding.serviceDetailPricing.setVisibility(View.VISIBLE);
        } else {
            binding.serviceDetailPricing.setVisibility(View.GONE);
        }

        // Description
        if (serviceDescription != null && !serviceDescription.isEmpty()) {
            binding.serviceDetailDescription.setText(serviceDescription);
        } else {
            binding.serviceDetailDescription.setText(getString(R.string.label_no_description));
        }

        // Category
        /*
        if (serviceCategory != null && !serviceCategory.isEmpty()) {
            binding.serviceDetailCategory.setText(getString(R.string.label_category_prefix, serviceCategory));
        } else {
            binding.serviceDetailCategory.setVisibility(View.GONE);
        }
        */
        // this for translator
        // Category - Extract and show only the primary category with services
        if (serviceCategory != null && !serviceCategory.isEmpty()) {
            String translatedCategory = FirestoreStringTranslator.get(this)
                    .translateCategory(serviceCategory);
            HomeController controller = new HomeController(this);
            String primaryCategory = controller.extractProviderCategoryWithServices(translatedCategory);
            binding.serviceDetailCategory.setText(getString(R.string.label_category_prefix, primaryCategory));
            binding.serviceDetailCategory.setVisibility(View.VISIBLE);
        } else {
            binding.serviceDetailCategory.setVisibility(View.GONE);
        }

        // Area
        if (serviceArea != null && !serviceArea.isEmpty()) {
            binding.serviceDetailArea.setText(getString(R.string.label_location_service_area, serviceArea));
        } else {
            binding.serviceDetailArea.setVisibility(View.GONE);
        }

        // Availability
        /*
        if (serviceAvailability != null && !serviceAvailability.isEmpty()) {
            binding.serviceDetailAvailability.setText(getString(R.string.label_calendar_availability, serviceAvailability));
        } else {
            binding.serviceDetailAvailability.setVisibility(View.GONE);
        }
        */
        // Availability
        if (serviceAvailability != null && !serviceAvailability.isEmpty()) {
            String formattedAvailability = FirestoreStringTranslator
                    .get(this)
                    .formatAvailabilityForDisplay(serviceAvailability);

            String labelWithAvailability = getString(R.string.label_calendar_availability, formattedAvailability);

            binding.serviceDetailAvailability.setText(labelWithAvailability);
            binding.serviceDetailAvailability.setVisibility(View.VISIBLE);
        } else {
            binding.serviceDetailAvailability.setVisibility(View.GONE);
        }


        // Preferred Contact
        /*
        if (serviceContactPreference != null && !serviceContactPreference.isEmpty()) {
            binding.serviceDetailPreferredContact.setText(
                    getString(R.string.label_preferred_contact, serviceContactPreference)
            );
            binding.serviceDetailPreferredContact.setVisibility(View.VISIBLE);
        } else {
            binding.serviceDetailPreferredContact.setVisibility(View.GONE);
        }
        */
        // Preferred Contact
        if (serviceContactPreference != null && !serviceContactPreference.isEmpty()) {

            String localizedContact =
                    FirestoreStringTranslator.get(this)
                            .translateContactPreference(serviceContactPreference);

            binding.serviceDetailPreferredContact.setText(
                    getString(R.string.label_preferred_contact, localizedContact)
            );

            binding.serviceDetailPreferredContact.setVisibility(View.VISIBLE);
        } else {
            binding.serviceDetailPreferredContact.setVisibility(View.GONE);
        }


        // Provider name
        binding.providerDetailName.setText(providerName);

        // Provider contact
        StringBuilder contactInfo = new StringBuilder();
        if (providerPhone != null && !providerPhone.isEmpty()) {
            contactInfo.append(getString(R.string.label_phone_prefix, formatPhone(providerPhone)));
        }
        if (providerEmail != null && !providerEmail.isEmpty()) {
            if (contactInfo.length() > 0) contactInfo.append("\n");
            contactInfo.append(getString(R.string.label_email_prefix, providerEmail));
        }
        if (providerAddress != null && !providerAddress.isEmpty()) {
            if (contactInfo.length() > 0) contactInfo.append("\n");
            contactInfo.append(getString(R.string.label_address_prefix, providerAddress));
        }
        binding.providerDetailContact.setText(contactInfo.toString());

        // Service image
        if (serviceImageUrl != null && !serviceImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(serviceImageUrl)
                    .placeholder(R.drawable.ic_service_placeholder)
                    .error(R.drawable.ic_service_placeholder)
                    .centerCrop()
                    .into(binding.serviceDetailImage);
        } else {
            binding.serviceDetailImage.setImageResource(R.drawable.ic_service_placeholder);
        }
    }

    // Sets up call, email, and map buttons
    private void setupActionButtons() {
        // Call
        binding.callButton.setOnClickListener(v -> {
            if (providerPhone != null && !providerPhone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + providerPhone));
                startActivity(intent);
            } else {
                Toast.makeText(this,
                        getString(R.string.error_phone_unavailable),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Email
        binding.emailButton.setOnClickListener(v -> {
            if (providerEmail != null && !providerEmail.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + providerEmail));
                intent.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.email_subject_inquiry, serviceTitle));
                intent.putExtra(Intent.EXTRA_TEXT,
                        getString(R.string.email_body_inquiry, providerName, serviceTitle));

                try {
                    startActivity(Intent.createChooser(intent,
                            getString(R.string.chooser_title_send_email)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this,
                            getString(R.string.error_no_email_app),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this,
                        getString(R.string.error_email_unavailable),
                        Toast.LENGTH_SHORT).show();
            }

        });

        // Location
        binding.locationButton.setOnClickListener(v -> {
            if (providerAddress != null && !providerAddress.isEmpty()) {
                Uri mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(providerAddress));
                Intent intent = new Intent(Intent.ACTION_VIEW, mapUri);
                intent.setPackage("com.google.android.apps.maps");

                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(providerAddress)));
                    startActivity(browserIntent);
                }
            } else {
                Toast.makeText(this,
                        getString(R.string.error_address_unavailable),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Hide or disable buttons when missing data
        if (providerPhone == null || providerPhone.isEmpty()) {
            binding.callButton.setVisibility(View.GONE);
        } else {
            binding.callButton.setVisibility(View.VISIBLE);
        }

        if (providerEmail == null || providerEmail.isEmpty()) {
            binding.emailButton.setVisibility(View.GONE);
        } else {
            binding.emailButton.setVisibility(View.VISIBLE);
        }

        if (providerAddress == null || providerAddress.isEmpty()) {
            binding.locationButton.setVisibility(View.GONE);
        } else {
            binding.locationButton.setVisibility(View.VISIBLE);
        }
    }

    /** Formats a 10-digit phone number as (XXX) XXX-XXXX */
    private String formatPhone(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return String.format("(%s) %s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6));
        }
        return phone;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /* =========================================================
       ON DESTROY (PREVENT MEMORY LEAK)
       ========================================================= */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Prevent memory leaks
    }

    // =========================================================
    // REVIEW SYSTEM
    // =========================================================
    private void showAddReviewDialog__() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
        Log.e("DEBUG_REVIEW", "Inflated layout has " + ((ViewGroup) dialogView).getChildCount() + " children");


        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        if (ratingBar == null) {
            Log.e("CRASH", "RATINGBAR IS NULL — LAYOUT IS WRONG OR OLD!");
            Toast.makeText(this, "FATAL: Layout is corrupted. Clean project!", Toast.LENGTH_LONG).show();
            return;
        }

        TextInputEditText commentInput = dialogView.findViewById(R.id.commentInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.submitButton).setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = commentInput.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(this, getString(R.string.error_select_rating), Toast.LENGTH_SHORT).show();
                return;
            }

            if (comment.isEmpty()) {
                commentInput.setError(getString(R.string.error_required));
                return;
            }

            submitReview(rating, comment);
            dialog.dismiss();
        });

        dialog.show();
    }
    private void showAddReviewDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_review, null);

        // DEBUG: Check if the view was inflated correctly
        if (dialogView == null) {
            Log.e("DEBUG_REVIEW", "dialogView is NULL! Inflation failed!");
            Toast.makeText(this, "ERROR: Dialog layout failed to inflate", Toast.LENGTH_LONG).show();
            return;
        }

        // DEBUG: List all view IDs that actually exist in the inflated layout
        Log.d("DEBUG_REVIEW", "=== ALL CHILD VIEWS IN DIALOG ===");
        logAllViewIds(dialogView);

        // Try to find the RatingBar
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);

        if (ratingBar == null) {
            Log.e("DEBUG_REVIEW", "RATINGBAR IS NULL! ID NOT FOUND!");
            Toast.makeText(this, "ERROR: RatingBar not found! Check XML id!", Toast.LENGTH_LONG).show();
            return;
        } else {
            Log.d("DEBUG_REVIEW", "RatingBar FOUND successfully! Rating = " + ratingBar.getRating());
        }

        TextInputEditText commentInput = dialogView.findViewById(R.id.commentInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.submitButton).setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = Objects.requireNonNull(commentInput.getText()).toString().trim();

            Log.d("DEBUG_REVIEW", "User submitted rating: " + rating + ", comment: " + comment);

            if (rating == 0) {
                Toast.makeText(this, getString(R.string.error_select_rating), Toast.LENGTH_SHORT).show();
                return;
            }

            if (comment.isEmpty()) {
                commentInput.setError(getString(R.string.error_required));
                return;
            }

            submitReview(rating, comment);
            dialog.dismiss();
        });

        dialog.show();
    }

    // Helper method to print ALL view IDs in the dialog (super useful!)
    private void logAllViewIds(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                int id = child.getId();
                if (id != View.NO_ID) {
                    String idName = getResources().getResourceEntryName(id);
                    Log.d("DEBUG_REVIEW", "Found view with ID: " + idName);
                } else {
                    Log.d("DEBUG_REVIEW", "Found view with NO ID");
                }
                if (child instanceof ViewGroup) {
                    logAllViewIds(child); // recursive
                }
            }
        }
    }
    private void submitReview(float rating, String comment) {
        Log.e("DEBUG_REVIEW", "Attempting to save review:");
        Log.e("DEBUG_REVIEW", "providerId = " + providerId);
        Log.e("DEBUG_REVIEW", "rating = " + rating);
        Log.e("DEBUG_REVIEW", "comment = " + comment);
        // Get customer info (you'll need to implement customer authentication)
        String customerId = "customer_" + System.currentTimeMillis(); // Temporary
        String customerName = "Anonymous";  // Replace with actual customer name

        Review review = new Review();
        review.setProviderId(providerId);
        review.setCustomerId(customerId);
        review.setCustomerName(customerName);
        review.setRating(rating);
        review.setComment(comment);
        review.setTimestamp(System.currentTimeMillis());
        review.setStatus("Active");

        reviewDatabase.saveReview(review, new ReviewDatabase.OnReviewSaveListener() {
            @Override
            public void onSuccess(String reviewId) {
                Toast.makeText(ServiceDetailActivity.this,
                        getString(R.string.success_review_submitted),
                        Toast.LENGTH_SHORT).show();
                loadProviderReviews(); // Reload to show new review
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ServiceDetailActivity.this,
                        getString(R.string.error_review_submit_failed),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProviderReviews() {
        if (providerId == null) {
            Log.e("ServiceDetail", "providerId is null!");
            return;
        }

        reviewDatabase.getReviewsForProvider(providerId, new ReviewDatabase.OnReviewsLoadedListener() {
            @Override
            public void onReviewsLoaded(List<Review> reviews) {
                displayReviews(reviews);
                updateAverageRating(providerId);  //  Pass providerId
            }

            @Override
            public void onError(String error) {
                Log.e("ServiceDetail", "Error loading reviews: " + error);
            }
        });
    }

    private void displayReviews(List<Review> reviews) {
        if (reviews.isEmpty()) {
            binding.reviewsRecyclerView.setVisibility(View.GONE);
            binding.noReviewsText.setVisibility(View.VISIBLE);
        } else {
            binding.reviewsRecyclerView.setVisibility(View.VISIBLE);
            binding.noReviewsText.setVisibility(View.GONE);
            reviewAdapter.setReviews(reviews);
        }
    }

    private void setupReviewsSection() {
        // Setup RecyclerView
        reviewDatabase = new ReviewDatabase();
        reviewAdapter = new ReviewAdapter(this);
        binding.reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.reviewsRecyclerView.setAdapter(reviewAdapter);
        // Setup Add Review button
        binding.addReviewButton.setOnClickListener(v -> showAddReviewDialog());

        // Load reviews
        loadProviderReviews();
    }

    private void updateAverageRating(String providerId) {
        reviewDatabase.getAverageRating(providerId, new ReviewDatabase.OnRatingCalculatedListener() {
            @Override
            public void onRatingCalculated(float averageRating, int totalReviews) {
                if (totalReviews == 0) {
                    binding.ratingSection.setVisibility(View.GONE);
                } else {
                    binding.ratingSection.setVisibility(View.VISIBLE);
                    binding.averageRatingText.setText(String.format(Locale.getDefault(),
                            "⭐ %.1f", averageRating));
                    binding.reviewCountText.setText(String.format(Locale.getDefault(),
                            "(%d %s)", totalReviews,
                            totalReviews == 1 ? "review" : "reviews"));
                }
            }

            @Override
            public void onError(String error) {
                binding.ratingSection.setVisibility(View.GONE);
            }
        });
    }

}