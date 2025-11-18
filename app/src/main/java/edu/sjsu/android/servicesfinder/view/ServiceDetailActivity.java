package edu.sjsu.android.servicesfinder.view;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.databinding.ActivityServiceDetailBinding;

/**
 * ServiceDetailActivity - Shows complete service details with contact options
 */
public class ServiceDetailActivity extends AppCompatActivity {

    // Service info
    private String serviceTitle;
    private String serviceDescription;
    private String servicePricing;
    private String serviceCategory;
    private String serviceArea;
    private String serviceAvailability;
    private String serviceImageUrl;

    // Provider info
    private String providerName;
    private String providerPhone;
    private String providerEmail;
    private String providerAddress;

    // === VIEW BINDING ===
    private ActivityServiceDetailBinding binding;

    /*
     * Entry point of the Activity. Initializes the layout, enables the back button in the ActionBar,
     * sets the title, and triggers the sequence of data extraction, view initialization, data binding, and action setup.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === VIEW BINDING INFLATE ===
        binding = ActivityServiceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);      // Enable back button in the ActionBar
            getSupportActionBar().setTitle("Service Details");
            // Set ActionBar background color
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(ContextCompat.getColor(this, R.color.sf_primary))
            );
        }

        getIntentExtras();  // Retrieves data passed from the main activity
        displayServiceInfo();
        setupActionButtons();
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
            binding.serviceDetailDescription.setText("No description available");
        }

        // Category
        if (serviceCategory != null && !serviceCategory.isEmpty()) {
            binding.serviceDetailCategory.setText(" Category: " + serviceCategory);
        } else {
            binding.serviceDetailCategory.setVisibility(View.GONE);
        }

        // Area
        if (serviceArea != null && !serviceArea.isEmpty()) {
            binding.serviceDetailArea.setText(" Location: Service Area: " + serviceArea);
        } else {
            binding.serviceDetailArea.setVisibility(View.GONE);
        }

        // Availability
        if (serviceAvailability != null && !serviceAvailability.isEmpty()) {
            binding.serviceDetailAvailability.setText(" Calendar: Available: " + serviceAvailability);
        } else {
            binding.serviceDetailAvailability.setVisibility(View.GONE);
        }

        // Provider name
        binding.providerDetailName.setText(providerName);

        // Provider contact
        StringBuilder contactInfo = new StringBuilder();
        if (providerPhone != null && !providerPhone.isEmpty()) {
            contactInfo.append(" Phone: ").append(formatPhone(providerPhone));
        }
        if (providerEmail != null && !providerEmail.isEmpty()) {
            if (contactInfo.length() > 0) contactInfo.append("\n");
            contactInfo.append(" Email: ").append(providerEmail);
        }
        if (providerAddress != null && !providerAddress.isEmpty()) {
            if (contactInfo.length() > 0) contactInfo.append("\n");
            contactInfo.append(" Address: ").append(providerAddress);
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
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Email
        binding.emailButton.setOnClickListener(v -> {
            if (providerEmail != null && !providerEmail.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + providerEmail));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about " + serviceTitle);
                intent.putExtra(Intent.EXTRA_TEXT, "Hi " + providerName + ",\n\nI'm interested in your service: " + serviceTitle + "\n\nCould you please provide more information?\n\nThank you!");

                try {
                    startActivity(Intent.createChooser(intent, "Send Email"));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Email not available", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Address not available", Toast.LENGTH_SHORT).show();
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
}