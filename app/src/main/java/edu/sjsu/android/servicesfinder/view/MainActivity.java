package edu.sjsu.android.servicesfinder.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.sjsu.android.servicesfinder.controller.HomeController;
import edu.sjsu.android.servicesfinder.controller.ServiceCardAdapter;
import edu.sjsu.android.servicesfinder.databinding.ActivityMainBinding;
import edu.sjsu.android.servicesfinder.model.Provider;
import edu.sjsu.android.servicesfinder.model.ProviderService;

/* *******************************************************************************************
 * MainActivity - Home screen showing service list, search bar, filters, sorting.
 * Uses ViewBinding instead of findViewById.
 ******************************************************************************************* */
public class MainActivity extends AppCompatActivity
        implements HomeController.HomeControllerListener,
        ServiceCardAdapter.OnServiceClickListener {

    private static final String TAG = "MainActivity";
    private static final int SEARCH_DELAY_MS = 300;

    private ActivityMainBinding binding;   // ViewBinding reference
    private HomeController homeController;
    private ServiceCardAdapter serviceAdapter;
    private Handler searchHandler;
    private Runnable searchRunnable;

    private String currentSearchQuery = "";
    private String currentCategoryFilter = "";
    private SortOption currentSortOption = SortOption.MOST_RECENT;

    /* *******************************************************************************************
     * onCreate
     ******************************************************************************************* */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate using ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Setup core components
        homeController = new HomeController();
        homeController.setListener(this);
        searchHandler = new Handler(Looper.getMainLooper());

        initializeUI();
        setupSearchBox();
        setupFilterChips();
        setupProviderButton();
        setupRecyclerView();

        showLoading();
        homeController.loadAllProvidersWithServices();
    }

    /* *******************************************************************************************
     * Initialize UI via binding
     ******************************************************************************************* */
    private void initializeUI() {
        // Binding already exposes all views; no findViewById needed
    }

    /* *******************************************************************************************
     * Setup RecyclerView for Services
     ******************************************************************************************* */
    private void setupRecyclerView() {
        binding.servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        serviceAdapter = new ServiceCardAdapter(this);
        serviceAdapter.setOnServiceClickListener(this);
        binding.servicesRecyclerView.setAdapter(serviceAdapter);
    }

    /* *******************************************************************************************
     * Search box with debounce
     ******************************************************************************************* */
    private void setupSearchWithDebounce(TextWatcher watcher) {
        binding.searchEditText.addTextChangedListener(watcher);
    }

    private void setupSearchBox() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (searchRunnable != null)
                    searchHandler.removeCallbacks(searchRunnable);

                searchRunnable = () -> performSearch(s.toString());
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
        });
    }

    /* *******************************************************************************************
     * Filter chips
     ******************************************************************************************* */
    private void setupFilterChips() {
        String[] categories = {
                "All", "Home Services", "Automotive",
                "Health & Wellness", "Hair Care",
                "Pet Services", "Education"
        };

        for (String category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);

            if (category.equals("All")) chip.setChecked(true);

            chip.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) {
                    uncheckOtherChips(chip);

                    if (category.equals("All")) {
                        currentCategoryFilter = "";
                        currentSearchQuery = "";
                        binding.searchEditText.setText("");
                        showLoading();
                        homeController.loadAllProvidersWithServices();
                        return;
                    }

                    currentCategoryFilter = category;
                    applyFilters();
                }
            });

            binding.filterChipGroup.addView(chip);
        }
    }

    private void uncheckOtherChips(Chip selected) {
        for (int i = 0; i < binding.filterChipGroup.getChildCount(); i++) {
            Chip other = (Chip) binding.filterChipGroup.getChildAt(i);
            if (other != selected) other.setChecked(false);
        }
    }

    /* *******************************************************************************************
     * Provider Button
     ******************************************************************************************* */
    private void setupProviderButton() {
        binding.providerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ProviderEntryActivity.class))
        );
    }

    /* *******************************************************************************************
     * Perform Search
     ******************************************************************************************* */
    private void performSearch(String query) {
        currentSearchQuery = query.trim();
        Log.d(TAG, "Searching: " + currentSearchQuery);

        if (currentSearchQuery.isEmpty()) {
            homeController.loadAllProvidersWithServices();
        } else {
            showLoading();
            homeController.searchProvidersAndServices(currentSearchQuery);
        }
    }

    /* *******************************************************************************************
     * Apply Filters
     ******************************************************************************************* */
    private void applyFilters() {
        showLoading();

        if (currentCategoryFilter.isEmpty()) {
            if (currentSearchQuery.isEmpty())
                homeController.loadAllProvidersWithServices();
            else
                homeController.searchProvidersAndServices(currentSearchQuery);
        } else {
            homeController.filterByCategory(currentCategoryFilter);
        }
    }

    /* *******************************************************************************************
     * Convert & Sort Provider-Service Data
     ******************************************************************************************* */
    private void applySorting(Map<Provider, List<ProviderService>> data) {
        List<ServiceCardAdapter.ServiceItem> items = new ArrayList<>();

        // Flatten provider → services
        for (Map.Entry<Provider, List<ProviderService>> entry : data.entrySet()) {
            for (ProviderService service : entry.getValue()) {
                items.add(new ServiceCardAdapter.ServiceItem(entry.getKey(), service));
            }
        }

        switch (currentSortOption) {
            case PRICE_LOW_TO_HIGH:
                Collections.sort(items, (a, b) ->
                        Double.compare(extractPrice(a.service.getPricing()),
                                extractPrice(b.service.getPricing())));
                break;

            case PRICE_HIGH_TO_LOW:
                Collections.sort(items, (a, b) ->
                        Double.compare(extractPrice(b.service.getPricing()),
                                extractPrice(a.service.getPricing())));
                break;

            case MOST_RECENT:
                Collections.sort(items, (a, b) ->
                        Long.compare(b.service.getTimestamp(),
                                a.service.getTimestamp()));
                break;

            case RATING:
            case POPULAR:
                break;
        }

        serviceAdapter.setServiceItems(items);
        updateResultCount(items.size());
    }

    private double extractPrice(String pricing) {
        if (pricing == null || pricing.isEmpty()) return 0;

        try {
            return Double.parseDouble(pricing.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /* *******************************************************************************************
     * UI Helpers
     ******************************************************************************************* */
    private void showLoading() {
        binding.loadingProgressBar.setVisibility(View.VISIBLE);
        binding.servicesRecyclerView.setVisibility(View.GONE);
        binding.emptyStateView.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.loadingProgressBar.setVisibility(View.GONE);
        binding.servicesRecyclerView.setVisibility(View.VISIBLE);
        binding.emptyStateView.setVisibility(View.GONE);
    }

    private void showEmptyState(String msg) {
        binding.loadingProgressBar.setVisibility(View.GONE);
        binding.servicesRecyclerView.setVisibility(View.GONE);
        binding.emptyStateView.setVisibility(View.VISIBLE);
        binding.emptyStateText.setText(msg);
    }

    private void updateResultCount(int count) {
        if (count == 0) {
            binding.resultCountText.setVisibility(View.GONE);
        } else {
            binding.resultCountText.setVisibility(View.VISIBLE);
            binding.resultCountText.setText(
                    count + (count == 1 ? " service found" : " services found")
            );
        }
    }

    /* *******************************************************************************************
     * HomeController Callbacks
     ******************************************************************************************* */
    @Override
    public void onProvidersWithServicesLoaded(Map<Provider, List<ProviderService>> map) {
        if (map.isEmpty()) showEmptyState("No services available yet.");
        else {
            showContent();
            applySorting(map);
        }
    }

    @Override
    public void onSearchResultsLoaded(Map<Provider, List<ProviderService>> map, String query) {
        if (map.isEmpty()) showEmptyState("No results for \"" + query + "\"");
        else {
            showContent();
            applySorting(map);
        }
    }

    @Override
    public void onSearchResultsEmpty(String query) {
        showEmptyState("No results for \"" + query + "\"");
    }

    @Override
    public void onProviderDetailsLoaded(Provider provider, List<ProviderService> services) {}

    @Override
    public void onNoDataAvailable() {
        showEmptyState("No data available.");
    }

    @Override
    public void onError(String errorMessage) {
        showEmptyState("Error: " + errorMessage);
    }

    /* *******************************************************************************************
     * RecyclerView click → open detail page
     ******************************************************************************************* */
    @Override
    public void onServiceClick(ServiceCardAdapter.ServiceItem item) {
        Intent i = new Intent(this, ServiceDetailActivity.class);
        i.putExtra("providerId", item.provider.getId());
        i.putExtra("providerName", item.provider.getFullName());
        i.putExtra("providerPhone", item.provider.getPhone());
        i.putExtra("providerEmail", item.provider.getEmail());
        i.putExtra("providerAddress", item.provider.getAddress());
        i.putExtra("serviceId", item.service.getId());
        i.putExtra("serviceTitle", item.service.getServiceTitle());
        i.putExtra("serviceDescription", item.service.getDescription());
        i.putExtra("servicePricing", item.service.getPricing());
        i.putExtra("serviceCategory", item.service.getCategory());
        i.putExtra("serviceArea", item.service.getServiceArea());
        i.putExtra("serviceAvailability", item.service.getAvailability());
        i.putExtra("serviceContactPreference", item.service.getContactPreference());
        i.putExtra("serviceImageUrl", item.service.getImageUrl());
        startActivity(i);
    }

    /* *******************************************************************************************
     * Lifecycle
     ******************************************************************************************* */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null)
            searchHandler.removeCallbacks(searchRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showLoading();
        homeController.loadAllProvidersWithServices();
    }

    /* *******************************************************************************************
     * Sort Options
     ******************************************************************************************* */
    private enum SortOption {
        RATING,
        PRICE_LOW_TO_HIGH,
        PRICE_HIGH_TO_LOW,
        MOST_RECENT,
        POPULAR
    }
}
