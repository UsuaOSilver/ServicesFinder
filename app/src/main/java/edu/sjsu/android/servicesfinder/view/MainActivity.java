    package edu.sjsu.android.servicesfinder.view;

    import android.content.Intent;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Looper;
    import android.text.Editable;
    import android.text.TextWatcher;
    import android.view.View;

    import androidx.annotation.Nullable;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.LinearLayoutManager;

    import com.google.android.material.chip.Chip;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    import edu.sjsu.android.servicesfinder.R;
    import edu.sjsu.android.servicesfinder.controller.HomeController;
    import edu.sjsu.android.servicesfinder.controller.ServiceCardAdapter;
    import edu.sjsu.android.servicesfinder.databinding.ActivityMainBinding;
    import edu.sjsu.android.servicesfinder.model.Provider;
    import edu.sjsu.android.servicesfinder.model.ProviderService;


    public class MainActivity extends AppCompatActivity
            implements HomeController.HomeControllerListener,
            ServiceCardAdapter.OnServiceClickListener {

        private static final int SEARCH_DELAY_MS = 300;

        private ActivityMainBinding binding;
        private HomeController homeController;
        private ServiceCardAdapter serviceAdapter;
        private Handler searchHandler;
        private Runnable searchRunnable;

        // IMPORTANT: Always English (filter keys must never translate)
        private final Map<String, String> categoryMap = new HashMap<>();

        private String currentSearchQuery = "";
        private String currentCategoryFilter = "";   // ALWAYS ENGLISH KEY
        private final SortOption currentSortOption = SortOption.MOST_RECENT;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            if (getSupportActionBar() != null) getSupportActionBar().hide();

            homeController = new HomeController(this);
            homeController.setListener(this);

            searchHandler = new Handler(Looper.getMainLooper());

            setupCategoryKeys();
            setupRecyclerView();
            setupSearchBox();
            setupFilterChips();
            setupProviderButton();

            showLoading();
            homeController.loadAllProvidersWithServices();
        }

        // ============================================================
        // CATEGORY MAP (English keys → translated display)
        // ============================================================
        private void setupCategoryKeys() {
            categoryMap.put("All", getString(R.string.cat_all));
            categoryMap.put("Automotive Services", getString(R.string.cat_automotive_services));
            categoryMap.put("Child & Elder Care", getString(R.string.cat_child_elder_care));
            categoryMap.put("Cleaning & Maintenance", getString(R.string.cat_cleaning_maintenance));
            categoryMap.put("Construction", getString(R.string.cat_construction));
            categoryMap.put("Hair Care", getString(R.string.cat_hair_care));
            categoryMap.put("Home Services", getString(R.string.cat_home_services));
            categoryMap.put("Moving & Delivery", getString(R.string.cat_moving_delivery));
            categoryMap.put("Pet Services", getString(R.string.cat_pet_services));
            categoryMap.put("Tech & Digital Services", getString(R.string.cat_tech_digital_services));
            categoryMap.put("Education Services", getString(R.string.cat_education_services));
        }

        // ============================================================
        private void setupRecyclerView() {
            binding.servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            serviceAdapter = new ServiceCardAdapter(this);
            serviceAdapter.setOnServiceClickListener(this);
            binding.servicesRecyclerView.setAdapter(serviceAdapter);
        }

        // ============================================================
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

        // ============================================================
        private void setupFilterChips() {

            for (String englishCategory : categoryMap.keySet()) {

                String translatedLabel = categoryMap.get(englishCategory);

                Chip chip = new Chip(this);
                chip.setCheckable(true);
                chip.setText(translatedLabel);

                // Important: store ENGLISH KEY for filtering
                chip.setTag(englishCategory);

                if (englishCategory.equals("All")) chip.setChecked(true);

                chip.setOnCheckedChangeListener((button, isChecked) -> {
                    if (isChecked) {
                        uncheckOtherChips(chip);

                        currentCategoryFilter = englishCategory.equals("All")
                                ? ""
                                : englishCategory;

                        if (currentCategoryFilter.isEmpty()) {
                            currentSearchQuery = "";
                            binding.searchEditText.setText("");
                            showLoading();
                            homeController.loadAllProvidersWithServices();
                        } else {
                            applyFilters();
                        }
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

        // ============================================================
        private void setupProviderButton() {
            binding.providerBtn.setOnClickListener(v ->
                    startActivity(new Intent(this, ProviderEntryActivity.class))
            );
        }

        // ============================================================
        private void performSearch(String query) {
            currentSearchQuery = query.trim();
            if (currentSearchQuery.isEmpty()) {
                homeController.loadAllProvidersWithServices();
            } else {
                showLoading();
                homeController.searchProvidersAndServices(currentSearchQuery);
            }
        }

        // ============================================================
        private void applyFilters() {
            showLoading();

            if (currentCategoryFilter.isEmpty()) {
                if (currentSearchQuery.isEmpty())
                    homeController.loadAllProvidersWithServices();
                else
                    homeController.searchProvidersAndServices(currentSearchQuery);
            } else {
                // IMPORTANT: always use ENGLISH KEY
                homeController.filterByCategory(currentCategoryFilter);
            }
        }

        // ============================================================
        private void applySorting(Map<Provider, List<ProviderService>> data) {

            List<ServiceCardAdapter.ServiceItem> items = new ArrayList<>();

            for (Map.Entry<Provider, List<ProviderService>> entry : data.entrySet()) {
                for (ProviderService service : entry.getValue()) {
                    items.add(new ServiceCardAdapter.ServiceItem(entry.getKey(), service));
                }
            }

            serviceAdapter.setServiceItems(items);
            updateResultCount(items.size());
        }

        // ============================================================
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
            if (count == 0) binding.resultCountText.setVisibility(View.GONE);
            else {
                binding.resultCountText.setVisibility(View.VISIBLE);
                binding.resultCountText.setText(
                        getResources().getQuantityString(R.plurals.services_found, count, count));
            }
        }

        // ============================================================
        @Override
        public void onProvidersWithServicesLoaded(Map<Provider, List<ProviderService>> map) {
            if (map.isEmpty()) showEmptyState(getString(R.string.empty_state_no_services));
            else {
                showContent();
                applySorting(map);
            }
        }

        @Override
        public void onSearchResultsLoaded(Map<Provider, List<ProviderService>> map, String query) {
            if (map.isEmpty()) showEmptyState(getString(R.string.empty_state_no_results, query));
            else {
                showContent();
                applySorting(map);
            }
        }

        @Override
        public void onSearchResultsEmpty(String query) {
            showEmptyState(getString(R.string.empty_state_no_results, query));
        }

        @Override public void onProviderDetailsLoaded(Provider provider, List<ProviderService> services) {}
        @Override public void onNoDataAvailable() {
            showEmptyState(getString(R.string.empty_state_no_data));
        }
        @Override public void onError(String errorMessage) {
            showEmptyState("Error: " + errorMessage);
        }

        // ============================================================
        // Click → open details
        // ============================================================
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

        // ============================================================
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

        // ============================================================
        private enum SortOption {
            RATING, PRICE_LOW_TO_HIGH, PRICE_HIGH_TO_LOW, MOST_RECENT, POPULAR
        }
    }
