package edu.sjsu.android.servicesfinder.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.controller.ServiceCardAdapter;
import edu.sjsu.android.servicesfinder.controller.SessionManager;
import edu.sjsu.android.servicesfinder.database.CustomerDatabase;
import edu.sjsu.android.servicesfinder.database.ProviderServiceDatabase;
import edu.sjsu.android.servicesfinder.databinding.ActivityCustomerProfileBinding;
import edu.sjsu.android.servicesfinder.model.Customer;
import edu.sjsu.android.servicesfinder.model.Provider;
import edu.sjsu.android.servicesfinder.model.ProviderService;
import edu.sjsu.android.servicesfinder.util.AnimationHelper;
import edu.sjsu.android.servicesfinder.util.NetworkHelper;
import edu.sjsu.android.servicesfinder.util.ProToast;
import edu.sjsu.android.servicesfinder.util.RetryDialog;

/**
 * CustomerProfileActivity - Customer profile with favorites and logout
 */
public class CustomerProfileActivity extends AppCompatActivity implements ServiceCardAdapter.OnServiceClickListener {

    private ActivityCustomerProfileBinding binding;
    private CustomerDatabase customerDatabase;
    private ProviderServiceDatabase providerServiceDatabase;
    private ServiceCardAdapter serviceAdapter;
    private String customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCustomerProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        customerDatabase = new CustomerDatabase(this);
        providerServiceDatabase = new ProviderServiceDatabase(this);

        customerId = SessionManager.getCustomerId(this);

        if (customerId == null) {
            ProToast.error(this, "Not logged in");
            finish();
            return;
        }

        setupRecyclerView();
        setupButtons();
        loadCustomerProfile();
    }

    private void setupRecyclerView() {
        serviceAdapter = new ServiceCardAdapter(this);
        serviceAdapter.setOnServiceClickListener(this);
        binding.favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.favoritesRecyclerView.setAdapter(serviceAdapter);
    }

    private void setupButtons() {
        binding.backButton.setOnClickListener(v -> finish());

        binding.logoutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        SessionManager.clearSession(this);
                        ProToast.success(this, "Logged out successfully");

                        // Go back to main activity
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadCustomerProfile() {
        // Check network connectivity first
        if (!NetworkHelper.isNetworkAvailable(this)) {
            RetryDialog.showNetworkError(this, this::loadCustomerProfile);
            return;
        }

        showLoading();

        customerDatabase.getCustomerById(customerId, new CustomerDatabase.OnCustomerLoadedListener() {
            @Override
            public void onSuccess(Customer customer) {
                displayCustomerInfo(customer);
                loadFavoriteServices(customer.getFavoriteProviders());
            }

            @Override
            public void onError(String errorMessage) {
                hideLoading();
                RetryDialog.show(CustomerProfileActivity.this,
                        "Failed to load profile: " + errorMessage,
                        CustomerProfileActivity.this::loadCustomerProfile);
            }
        });
    }

    private void displayCustomerInfo(Customer customer) {
        binding.customerNameText.setText(customer.getFullName());

        if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
            binding.customerEmailText.setText(customer.getEmail());
            binding.customerEmailText.setVisibility(View.VISIBLE);
        } else if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
            binding.customerEmailText.setText(customer.getPhone());
            binding.customerEmailText.setVisibility(View.VISIBLE);
        } else {
            binding.customerEmailText.setVisibility(View.GONE);
        }
    }

    private void loadFavoriteServices(List<String> favoriteProviderIds) {
        if (favoriteProviderIds == null || favoriteProviderIds.isEmpty()) {
            showEmptyState();
            return;
        }

        // Load all providers with services
        providerServiceDatabase.getAllProvidersWithServices(this, new ProviderServiceDatabase.OnProvidersWithServicesLoadedListener() {
            @Override
            public void onSuccess(Map<Provider, List<ProviderService>> allProvidersMap) {
                // Filter to only favorite providers
                Map<Provider, List<ProviderService>> favoriteProvidersMap = new HashMap<>();

                for (Map.Entry<Provider, List<ProviderService>> entry : allProvidersMap.entrySet()) {
                    if (favoriteProviderIds.contains(entry.getKey().getId())) {
                        favoriteProvidersMap.put(entry.getKey(), entry.getValue());
                    }
                }

                if (favoriteProvidersMap.isEmpty()) {
                    showEmptyState();
                } else {
                    displayFavorites(favoriteProvidersMap);
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideLoading();
                RetryDialog.show(CustomerProfileActivity.this,
                        "Failed to load favorites: " + errorMessage,
                        CustomerProfileActivity.this::loadCustomerProfile);
                showEmptyState();
            }
        });
    }

    private void displayFavorites(Map<Provider, List<ProviderService>> providersMap) {
        hideLoading();

        // Pass the map directly to the adapter
        serviceAdapter.setData(providersMap);

        // Animate showing favorites
        AnimationHelper.fadeOut(binding.emptyStateLayout);
        AnimationHelper.fadeIn(binding.favoritesRecyclerView);
    }

    private void showEmptyState() {
        hideLoading();

        // Animate showing empty state
        AnimationHelper.fadeIn(binding.emptyStateLayout);
        AnimationHelper.fadeOut(binding.favoritesRecyclerView);
    }

    private void showLoading() {
        AnimationHelper.fadeIn(binding.loadingProgressBar);
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.favoritesRecyclerView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        AnimationHelper.fadeOut(binding.loadingProgressBar);
    }

    @Override
    public void onServiceClick(ServiceCardAdapter.ServiceItem item) {
        // Navigate to service detail
        Intent intent = new Intent(this, ServiceDetailActivity.class);

        ProviderService service = item.service;
        Provider provider = item.provider;

        intent.putExtra("serviceTitle", service.getServiceTitle());
        intent.putExtra("serviceDescription", service.getDescription());
        intent.putExtra("servicePricing", service.getPricing());
        intent.putExtra("serviceCategory", service.getCategory());
        intent.putExtra("serviceArea", service.getServiceArea());
        intent.putExtra("serviceAvailability", service.getAvailability());
        intent.putExtra("serviceContactPreference", service.getContactPreference());
        intent.putExtra("serviceImageUrl", service.getImageUrl());

        intent.putExtra("providerName", provider.getFullName());
        intent.putExtra("providerPhone", provider.getPhone());
        intent.putExtra("providerEmail", provider.getEmail());
        intent.putExtra("providerAddress", provider.getAddress());
        intent.putExtra("providerId", provider.getId());

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload favorites when returning from service detail
        if (customerId != null) {
            loadCustomerProfile();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
