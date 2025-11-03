package edu.sjsu.android.servicesfinder.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import edu.sjsu.android.servicesfinder.R;

/**
 * ServiceDetailActivity - Shows complete service details with contact options
 */
public class ServiceDetailActivity extends AppCompatActivity {

    private static final String TAG = "ServiceDetail";

    // Service info
    private String serviceId;
    private String serviceTitle;
    private String serviceDescription;
    private String servicePricing;
    private String serviceCategory;
    private String serviceArea;
    private String serviceAvailability;
    private String serviceContactPreference;
    private String serviceImageUrl;

    // Provider info
    private String providerId;
    private String providerName;
    private String providerPhone;
    private String providerEmail;
    private String providerAddress;

    // UI Components
    private ImageView serviceImageView;
    private TextView serviceTitleText;
    private TextView servicePricingText;
    private TextView serviceDescriptionText;
    private TextView serviceCategoryText;
    private TextView serviceAreaText;
    private TextView serviceAvailabilityText;
    private TextView providerNameText;
    private TextView providerContactText;
    private Button callButton;
    private Button emailButton;
    private Button locationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Service Details");
        }

        getIntentExtras();
        initializeViews();
        displayServiceInfo();
        setupActionButtons();
    }

    private void getIntentExtras() {
        Intent intent = getIntent();

        // Service data
        serviceId = intent.getStringExtra("serviceId");
        serviceTitle = intent.getStringExtra("serviceTitle");
        serviceDescription = intent.getStringExtra("serviceDescription");
        servicePricing = intent.getStringExtra("servicePricing");
        serviceCategory = intent.getStringExtra("serviceCategory");
        serviceArea = intent.getStringExtra("serviceArea");
        serviceAvailability = intent.getStringExtra("serviceAvailability");
        serviceContactPreference = intent.getStringExtra("serviceContactPreference");
        serviceImageUrl = intent.getStringExtra("serviceImageUrl");

        // Provider data
        providerId = intent.getStringExtra("providerId");
        providerName = intent.getStringExtra("providerName");
        providerPhone = intent.getStringExtra("providerPhone");
        providerEmail = intent.getStringExtra("providerEmail");
        providerAddress = intent.getStringExtra("providerAddress");
    }

    private void initializeViews() {
        serviceImageView = findViewById(R.id.serviceDetailImage);
        serviceTitleText = findViewById(R.id.serviceDetailTitle);
        servicePricingText = findViewById(R.id.serviceDetailPricing);
        serviceDescriptionText = findViewById(R.id.serviceDetailDescription);
        serviceCategoryText = findViewById(R.id.serviceDetailCategory);
        serviceAreaText = findViewById(R.id.serviceDetailArea);
        serviceAvailabilityText = findViewById(R.id.serviceDetailAvailability);
        providerNameText = findViewById(R.id.providerDetailName);
        providerContactText = findViewById(R.id.providerDetailContact);
        callButton = findViewById(R.id.callButton);
        emailButton = findViewById(R.id.emailButton);
        locationButton = findViewById(R.id.locationButton);
    }

    private void displayServiceInfo() {
        // Service title
        serviceTitleText.setText(serviceTitle);

        // Pricing
        if (servicePricing != null && !servicePricing.isEmpty()) {
            servicePricingText.setText(servicePricing);
            servicePricingText.setVisibility(View.VISIBLE);
        } else {
            servicePricingText.setVisibility(View.GONE);
        }

        // Description
        if (serviceDescription != null && !serviceDescription.isEmpty()) {
            serviceDescriptionText.setText(serviceDescription);
            serviceDescriptionText.setVisibility(View.VISIBLE);
        } else {
            serviceDescriptionText.setText("No description available");
        }

        // Category
        if (serviceCategory != null && !serviceCategory.isEmpty()) {
            serviceCategoryText.setText("📂 " + serviceCategory);
            serviceCategoryText.setVisibility(View.VISIBLE);
        } else {
            serviceCategoryText.setVisibility(View.GONE);
        }

        // Service area
        if (serviceArea != null && !serviceArea.isEmpty()) {
            serviceAreaText.setText("📍 Service Area: " + serviceArea);
            serviceAreaText.setVisibility(View.VISIBLE);
        } else {
            serviceAreaText.setVisibility(View.GONE);
        }

        // Availability
        if (serviceAvailability != null && !serviceAvailability.isEmpty()) {
            serviceAvailabilityText.setText("📅 Available: " + serviceAvailability);
            serviceAvailabilityText.setVisibility(View.VISIBLE);
        } else {
            serviceAvailabilityText.setVisibility(View.GONE);
        }

        // Provider name
        providerNameText.setText("Provider: " + providerName);

        // Provider contact info
        StringBuilder contactInfo = new StringBuilder();
        if (providerPhone != null && !providerPhone.isEmpty()) {
            contactInfo.append("📞 ").append(formatPhone(providerPhone));
        }
        if (providerEmail != null && !providerEmail.isEmpty()) {
            if (contactInfo.length() > 0) contactInfo.append("\n");
            contactInfo.append("✉️ ").append(providerEmail);
        }
        if (providerAddress != null && !providerAddress.isEmpty()) {
            if (contactInfo.length() > 0) contactInfo.append("\n");
            contactInfo.append("📍 ").append(providerAddress);
        }
        providerContactText.setText(contactInfo.toString());

        // Service image
        if (serviceImageUrl != null && !serviceImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(serviceImageUrl)
                    .placeholder(R.drawable.ic_service_placeholder)
                    .error(R.drawable.ic_service_placeholder)
                    .centerCrop()
                    .into(serviceImageView);
        } else {
            serviceImageView.setImageResource(R.drawable.ic_service_placeholder);
        }
    }

    private void setupActionButtons() {
        // Call button
        callButton.setOnClickListener(v -> {
            if (providerPhone != null && !providerPhone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + providerPhone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Email button
        emailButton.setOnClickListener(v -> {
            if (providerEmail != null && !providerEmail.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + providerEmail));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about " + serviceTitle);
                intent.putExtra(Intent.EXTRA_TEXT,
                        "Hi " + providerName + ",\n\n" +
                                "I'm interested in your service: " + serviceTitle + "\n\n" +
                                "Could you please provide more information?\n\n" +
                                "Thank you!");

                try {
                    startActivity(Intent.createChooser(intent, "Send Email"));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Email not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Location button
        locationButton.setOnClickListener(v -> {
            if (providerAddress != null && !providerAddress.isEmpty()) {
                // Open in Google Maps
                Uri mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(providerAddress));
                Intent intent = new Intent(Intent.ACTION_VIEW, mapUri);
                intent.setPackage("com.google.android.apps.maps");

                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    // If Google Maps not installed, open in browser
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=" +
                                    Uri.encode(providerAddress)));
                    startActivity(browserIntent);
                }
            } else {
                Toast.makeText(this, "Address not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Disable buttons if contact info not available
        if (providerPhone == null || providerPhone.isEmpty()) {
            callButton.setEnabled(false);
            callButton.setAlpha(0.5f);
        }

        if (providerEmail == null || providerEmail.isEmpty()) {
            emailButton.setEnabled(false);
            emailButton.setAlpha(0.5f);
        }

        if (providerAddress == null || providerAddress.isEmpty()) {
            locationButton.setEnabled(false);
            locationButton.setAlpha(0.5f);
        }
    }

    private String formatPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "";
        }

        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.length() == 10) {
            return String.format("(%s) %s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6, 10));
        }

        return phone;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}