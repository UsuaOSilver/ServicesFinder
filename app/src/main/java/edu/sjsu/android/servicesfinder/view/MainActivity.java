package edu.sjsu.android.servicesfinder.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.controller.HomeController;
import edu.sjsu.android.servicesfinder.controller.ServiceCardAdapter;
import edu.sjsu.android.servicesfinder.model.Provider;
import edu.sjsu.android.servicesfinder.model.ProviderService;

/**
 * MainActivity - Home screen with service listing, search, and filters
 * Shows all services with embedded provider info
 */
public class MainActivity extends AppCompatActivity
        implements HomeController.HomeControllerListener,
        ServiceCardAdapter.OnServiceClickListener {

    private static final String TAG = "MainActivity";
    private static final int SEARCH_DELAY_MS = 300; // 300ms debounce

    // UI Components
    private EditText searchEditText;
    private ChipGroup filterChipGroup;
    private RecyclerView servicesRecyclerView;
    private ProgressBar loadingProgressBar;
    private View emptyStateView;
    private TextView emptyStateText;
    private Button providerBtn;
    private TextView resultCountText;

    // Controllers and Adapters
    private HomeController homeController;
    private ServiceCardAdapter serviceAdapter;

    // Search debounce
    private Handler searchHandler;
    private Runnable searchRunnable;

    // Current state
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "";
    private SortOption currentSortOption = SortOption.MOST_RECENT;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeComponents();
        setupSearchWithDebounce();
        setupFilterChips();
        setupProviderButton();
        setupRecyclerView();

        // Load initial data
        showLoading();
        homeController.loadAllProvidersWithServices();
    }

    private void initializeComponents() {
        searchEditText = findViewById(R.id.searchEditText);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        servicesRecyclerView = findViewById(R.id.servicesRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        emptyStateView = findViewById(R.id.emptyStateView);
        emptyStateText = findViewById(R.id.emptyStateText);
        providerBtn = findViewById(R.id.providerBtn);
        resultCountText = findViewById(R.id.resultCountText);

        homeController = new HomeController();
        homeController.setListener(this);

        searchHandler = new Handler(Looper.getMainLooper());
    }

    private void setupRecyclerView() {
        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        serviceAdapter = new ServiceCardAdapter(this);
        serviceAdapter.setOnServiceClickListener(this);
        servicesRecyclerView.setAdapter(serviceAdapter);
    }

    private void setupSearchWithDebounce() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Schedule new search after 300ms
                searchRunnable = () -> performSearch(s.toString());
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterChips() {
        // Predefined categories based on Firebase structure
        String[] categories = {
                "All",
                "Home Services",
                "Automotive",
                "Health & Wellness",
                "Hair Care",
                "Pet Services",
                "Education"
        };

        for (String category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);

            if (category.equals("All")) {
                chip.setChecked(true);
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Uncheck other chips
                    for (int i = 0; i < filterChipGroup.getChildCount(); i++) {
                        Chip otherChip = (Chip) filterChipGroup.getChildAt(i);
                        if (otherChip != chip) {
                            otherChip.setChecked(false);
                        }
                    }

                    currentCategoryFilter = category.equals("All") ? "" : category;
                    applyFilters();
                }
            });

            filterChipGroup.addView(chip);
        }
    }

    private void setupProviderButton() {
        providerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProviderEntryActivity.class);
            startActivity(intent);
        });
    }

    private void performSearch(String query) {
        currentSearchQuery = query.trim();

        Log.d(TAG, "Performing search: " + currentSearchQuery);

        if (currentSearchQuery.isEmpty()) {
            homeController.loadAllProvidersWithServices();
        } else {
            showLoading();
            homeController.searchProvidersAndServices(currentSearchQuery);
        }
    }

    private void applyFilters() {
        showLoading();

        if (currentCategoryFilter.isEmpty()) {
            if (currentSearchQuery.isEmpty()) {
                homeController.loadAllProvidersWithServices();
            } else {
                homeController.searchProvidersAndServices(currentSearchQuery);
            }
        } else {
            homeController.filterByCategory(currentCategoryFilter);
        }
    }

    private void applySorting(Map<Provider, List<ProviderService>> data) {
        List<ServiceCardAdapter.ServiceItem> items = new ArrayList<>();

        // Convert map to list of ServiceItems
        for (Map.Entry<Provider, List<ProviderService>> entry : data.entrySet()) {
            for (ProviderService service : entry.getValue()) {
                items.add(new ServiceCardAdapter.ServiceItem(entry.getKey(), service));
            }
        }

        // Sort based on current option
        switch (currentSortOption) {
            case RATING:
                // Sort by rating (placeholder - all are 0 currently)
                Collections.sort(items, (a, b) ->
                        Double.compare(0, 0)); // Replace with actual rating
                break;

            case PRICE_LOW_TO_HIGH:
                Collections.sort(items, (a, b) -> {
                    double priceA = extractPrice(a.service.getPricing());
                    double priceB = extractPrice(b.service.getPricing());
                    return Double.compare(priceA, priceB);
                });
                break;

            case PRICE_HIGH_TO_LOW:
                Collections.sort(items, (a, b) -> {
                    double priceA = extractPrice(a.service.getPricing());
                    double priceB = extractPrice(b.service.getPricing());
                    return Double.compare(priceB, priceA);
                });
                break;

            case MOST_RECENT:
                Collections.sort(items, (a, b) ->
                        Long.compare(b.service.getTimestamp(), a.service.getTimestamp()));
                break;

            case POPULAR:
                // Sort by popularity (placeholder)
                // You can implement this based on view count, rating count, etc.
                break;
        }

        serviceAdapter.setServiceItems(items);
        updateResultCount(items.size());
    }

    private double extractPrice(String pricing) {
        if (pricing == null || pricing.isEmpty()) {
            return 0;
        }

        try {
            // Extract numeric part (e.g., "$50" -> 50, "$25/hour" -> 25)
            String numericPart = pricing.replaceAll("[^0-9.]", "");
            return Double.parseDouble(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showLoading() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        servicesRecyclerView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingProgressBar.setVisibility(View.GONE);
        servicesRecyclerView.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);
    }

    private void showEmptyState(String message) {
        loadingProgressBar.setVisibility(View.GONE);
        servicesRecyclerView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }

    private void updateResultCount(int count) {
        if (count == 0) {
            resultCountText.setVisibility(View.GONE);
        } else {
            resultCountText.setVisibility(View.VISIBLE);
            String text = count + (count == 1 ? " service found" : " services found");
            resultCountText.setText(text);
        }
    }

    // ==================== HomeControllerListener Callbacks ====================

    @Override
    public void onProvidersWithServicesLoaded(Map<Provider, List<ProviderService>> providerServiceMap) {
        Log.d(TAG, "Loaded " + providerServiceMap.size() + " providers");

        if (providerServiceMap.isEmpty()) {
            showEmptyState("No services available yet.\nCheck back soon!");
        } else {
            showContent();
            applySorting(providerServiceMap);
        }
    }

    @Override
    public void onSearchResultsLoaded(Map<Provider, List<ProviderService>> providerServiceMap, String query) {
        Log.d(TAG, "Search results for: " + query);

        if (providerServiceMap.isEmpty()) {
            showEmptyState("No results found for \"" + query + "\"");
        } else {
            showContent();
            applySorting(providerServiceMap);
        }
    }

    @Override
    public void onSearchResultsEmpty(String query) {
        showEmptyState("No results found for \"" + query + "\"\n\nTry different keywords or filters");
    }

    @Override
    public void onProviderDetailsLoaded(Provider provider, List<ProviderService> services) {
        // Not used in main activity
    }

    @Override
    public void onNoDataAvailable() {
        showEmptyState("No services available in your area yet.\n\nTry expanding filters or check back soon!");
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(TAG, "Error: " + errorMessage);
        Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
        showEmptyState("Unable to load services.\n\n" + errorMessage);
    }

    // ==================== ServiceCardAdapter.OnServiceClickListener ====================

    @Override
    public void onServiceClick(ServiceCardAdapter.ServiceItem item) {
        Log.d(TAG, "Service clicked: " + item.service.getServiceTitle());

        // Open service detail activity
        Intent intent = new Intent(this, ServiceDetailActivity.class);
        intent.putExtra("providerId", item.provider.getId());
        intent.putExtra("providerName", item.provider.getFullName());
        intent.putExtra("providerPhone", item.provider.getPhone());
        intent.putExtra("providerEmail", item.provider.getEmail());
        intent.putExtra("providerAddress", item.provider.getAddress());
        intent.putExtra("serviceId", item.service.getId());
        intent.putExtra("serviceTitle", item.service.getServiceTitle());
        intent.putExtra("serviceDescription", item.service.getDescription());
        intent.putExtra("servicePricing", item.service.getPricing());
        intent.putExtra("serviceCategory", item.service.getCategory());
        intent.putExtra("serviceArea", item.service.getServiceArea());
        intent.putExtra("serviceAvailability", item.service.getAvailability());
        intent.putExtra("serviceContactPreference", item.service.getContactPreference());
        intent.putExtra("serviceImageUrl", item.service.getImageUrl());
        startActivity(intent);
    }

    // ==================== Sort Options ====================

    private enum SortOption {
        RATING,
        PRICE_LOW_TO_HIGH,
        PRICE_HIGH_TO_LOW,
        MOST_RECENT,
        POPULAR
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}