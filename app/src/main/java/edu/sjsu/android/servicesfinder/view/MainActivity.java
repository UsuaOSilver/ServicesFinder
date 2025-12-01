    package edu.sjsu.android.servicesfinder.view;

    import android.content.Intent;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Looper;
    import android.text.Editable;
    import android.text.TextWatcher;
    import android.util.Log;
    import android.view.View;
    import android.widget.Toast;

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
    import edu.sjsu.android.servicesfinder.controller.SessionManager;
    import edu.sjsu.android.servicesfinder.database.ReviewDatabase;
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
        private SortOption currentSortOption = SortOption.MOST_RECENT;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Apply saved language before setting content view
            applySavedLanguage();

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
            setupCustomerButton();
            setupSortButton();
            setupLanguageButton();
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
            categoryMap.put("Nail Services", getString(R.string.cat_nail_services));
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


                        Log.e("CAT_UI", "User selected chip: " + englishCategory);
                        Log.e("CAT_UI", "Category key used for filtering: " + currentCategoryFilter);

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
        // CUSTOMER SIGN IN BUTTON
        // ============================================================
        private void setupCustomerButton() {
            updateCustomerButtonState();

            binding.customerSignInBtn.setOnClickListener(v -> {
                if (SessionManager.isCustomerLoggedIn(this)) {
                    // Open customer profile
                    startActivity(new Intent(this, CustomerProfileActivity.class));
                } else {
                    // Open customer authentication activity
                    startActivity(new Intent(this, CustomerAuthActivity.class));
                }
            });
        }

        private void updateCustomerButtonState() {
            if (SessionManager.isCustomerLoggedIn(this)) {
                String customerName = SessionManager.getCustomerName(this);
                String firstName = customerName.split(" ")[0];
                binding.customerSignInBtn.setText(firstName);
            } else {
                binding.customerSignInBtn.setText(R.string.customer_sign_in);
            }
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

            // Apply current sort option
            if (currentSortOption == SortOption.RATING) {
                // Special handling for rating sort - needs to fetch ratings from ReviewDatabase
                sortItemsByRating(items);
            } else {
                sortItems(items);
                serviceAdapter.setServiceItems(items);
                updateResultCount(items.size());
            }
        }

        // ============================================================
        // SORTING LOGIC
        // ============================================================
        private void sortItems(List<ServiceCardAdapter.ServiceItem> items) {
            switch (currentSortOption) {
                case MOST_RECENT:
                    // Sort by timestamp descending (newest first)
                    items.sort((a, b) -> Long.compare(b.service.getTimestamp(), a.service.getTimestamp()));
                    break;

                case PRICE_LOW_TO_HIGH:
                    items.sort((a, b) -> {
                        double priceA = extractPrice(a.service.getPricing());
                        double priceB = extractPrice(b.service.getPricing());
                        return Double.compare(priceA, priceB);
                    });
                    break;

                case PRICE_HIGH_TO_LOW:
                    items.sort((a, b) -> {
                        double priceA = extractPrice(a.service.getPricing());
                        double priceB = extractPrice(b.service.getPricing());
                        return Double.compare(priceB, priceA);
                    });
                    break;

                case RATING:
                    // Sort by provider rating (if available)
                    items.sort((a, b) -> Double.compare(b.service.getRating(), a.service.getRating()));
                    break;

                case POPULAR:
                    // Sort by service popularity (timestamp as proxy for now)
                    items.sort((a, b) -> Long.compare(b.service.getTimestamp(), a.service.getTimestamp()));
                    break;
            }
        }

        // Extract numeric price from pricing string (e.g., "$50/hour" -> 50.0)
        private double extractPrice(String pricing) {
            if (pricing == null || pricing.isEmpty()) {
                return Double.MAX_VALUE; // Put items without price at the end
            }

            try {
                // Remove currency symbols and extract first number
                String numberStr = pricing.replaceAll("[^0-9.]", "");
                if (!numberStr.isEmpty()) {
                    return Double.parseDouble(numberStr);
                }
            } catch (NumberFormatException e) {
                Log.e("SORT", "Failed to parse price: " + pricing);
            }

            return Double.MAX_VALUE;
        }

        // Sort items by rating - requires fetching ratings from ReviewDatabase
        private void sortItemsByRating(List<ServiceCardAdapter.ServiceItem> items) {
            ReviewDatabase reviewDb = new ReviewDatabase();

            // Create a map to store provider ratings
            Map<String, Float> providerRatings = new HashMap<>();
            int[] pendingCallbacks = {items.size()};

            // Fetch ratings for all providers
            for (ServiceCardAdapter.ServiceItem item : items) {
                String providerId = item.provider.getId();

                // Skip if already fetched
                if (providerRatings.containsKey(providerId)) {
                    pendingCallbacks[0]--;
                    continue;
                }

                reviewDb.getAverageRating(providerId, new ReviewDatabase.OnRatingCalculatedListener() {
                    @Override
                    public void onRatingCalculated(float averageRating, int totalReviews) {
                        providerRatings.put(providerId, averageRating);
                        pendingCallbacks[0]--;

                        // When all ratings are fetched, sort and display
                        if (pendingCallbacks[0] == 0) {
                            items.sort((a, b) -> {
                                float ratingA = providerRatings.getOrDefault(a.provider.getId(), 0f);
                                float ratingB = providerRatings.getOrDefault(b.provider.getId(), 0f);
                                return Float.compare(ratingB, ratingA); // Descending order
                            });

                            runOnUiThread(() -> {
                                serviceAdapter.setServiceItems(items);
                                updateResultCount(items.size());
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        providerRatings.put(providerId, 0f); // Default to 0 on error
                        pendingCallbacks[0]--;

                        if (pendingCallbacks[0] == 0) {
                            items.sort((a, b) -> {
                                float ratingA = providerRatings.getOrDefault(a.provider.getId(), 0f);
                                float ratingB = providerRatings.getOrDefault(b.provider.getId(), 0f);
                                return Float.compare(ratingB, ratingA);
                            });

                            runOnUiThread(() -> {
                                serviceAdapter.setServiceItems(items);
                                updateResultCount(items.size());
                            });
                        }
                    }
                });
            }

            // Handle case where all providers already cached
            if (pendingCallbacks[0] == 0) {
                serviceAdapter.setServiceItems(items);
                updateResultCount(items.size());
            }
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
            updateCustomerButtonState();
            showLoading();
            homeController.loadAllProvidersWithServices();
        }

        // ============================================================
        // SORT BUTTON SETUP
        // ============================================================
        private void setupSortButton() {
            binding.sortButton.setOnClickListener(v -> showSortDialog());
        }

        private void showSortDialog() {
            String[] sortOptions = {
                    getString(R.string.sort_most_recent),
                    getString(R.string.sort_price_low_high),
                    getString(R.string.sort_price_high_low),
                    getString(R.string.sort_rating),
                    getString(R.string.sort_popular)
            };

            int currentSelection = 0;
            switch (currentSortOption) {
                case MOST_RECENT: currentSelection = 0; break;
                case PRICE_LOW_TO_HIGH: currentSelection = 1; break;
                case PRICE_HIGH_TO_LOW: currentSelection = 2; break;
                case RATING: currentSelection = 3; break;
                case POPULAR: currentSelection = 4; break;
            }

            new android.app.AlertDialog.Builder(this)
                    .setTitle(R.string.sort_by)
                    .setSingleChoiceItems(sortOptions, currentSelection, (dialog, which) -> {
                        switch (which) {
                            case 0: currentSortOption = SortOption.MOST_RECENT; break;
                            case 1: currentSortOption = SortOption.PRICE_LOW_TO_HIGH; break;
                            case 2: currentSortOption = SortOption.PRICE_HIGH_TO_LOW; break;
                            case 3: currentSortOption = SortOption.RATING; break;
                            case 4: currentSortOption = SortOption.POPULAR; break;
                        }
                        dialog.dismiss();
                        applyFilters(); // Reapply current filters with new sort
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        // ============================================================
        // LANGUAGE BUTTON SETUP
        // ============================================================
        private void setupLanguageButton() {
            updateLanguageButtonText();
            binding.languageButton.setOnClickListener(v -> showLanguageDialog());
        }

        private void applySavedLanguage() {
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String savedLang = prefs.getString("app_language", "en");

            java.util.Locale locale = new java.util.Locale(savedLang);
            java.util.Locale.setDefault(locale);

            android.content.res.Configuration config = new android.content.res.Configuration();
            config.setLocale(locale);
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        }

        private void updateLanguageButtonText() {
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String savedLang = prefs.getString("app_language", "en");

            switch (savedLang) {
                case "es": binding.languageButton.setText("ES"); break;
                case "vi": binding.languageButton.setText("VI"); break;
                case "zh": binding.languageButton.setText("ZH"); break;
                default: binding.languageButton.setText("EN"); break;
            }
        }

        private void showLanguageDialog() {
            String[] languages = {
                    getString(R.string.lang_english),
                    getString(R.string.lang_spanish),
                    getString(R.string.lang_vietnamese),
                    getString(R.string.lang_chinese)
            };

            new android.app.AlertDialog.Builder(this)
                    .setTitle(R.string.select_language)
                    .setItems(languages, (dialog, which) -> {
                        String langCode;
                        switch (which) {
                            case 0: langCode = "en"; break;
                            case 1: langCode = "es"; break;
                            case 2: langCode = "vi"; break;
                            case 3: langCode = "zh"; break;
                            default: langCode = "en"; break;
                        }
                        changeLanguage(langCode);
                    })
                    .show();
        }

        private void changeLanguage(String languageCode) {
            // Save language preference
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putString("app_language", languageCode).apply();

            java.util.Locale locale = new java.util.Locale(languageCode);
            java.util.Locale.setDefault(locale);

            android.content.res.Configuration config = new android.content.res.Configuration();
            config.setLocale(locale);
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());

            // Restart activity to apply language change
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }

        // ============================================================
        private enum SortOption {
            RATING, PRICE_LOW_TO_HIGH, PRICE_HIGH_TO_LOW, MOST_RECENT, POPULAR
        }
    }
