package edu.sjsu.android.servicesfinder.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.*;
import edu.sjsu.android.servicesfinder.R;

/**
 * ========================================================================
 * MultiSelectDropdown - Custom Multi-Level Selection Dropdown
 * ========================================================================
 /**
 * Custom dropdown for multi-selecting services under catalogues.
 * Shows expandable sections with checkboxes, Done/Cancel buttons.
 * Output: "Cat: Svc1, Svc2 | Cat2: Svc3"
 */
public class MultiSelectDropdown {

    // =========================================================
    // INSTANCE VARIABLES
    // =========================================================

    // Application context for creating views */
    private final Context context;

    // TextView that triggers and displays the dropdown */
    private final TextView anchorView;

    // Catalogue data: Catalogue name → List of service names */
    private Map<String, List<String>> catalogueMap;

    // Current selections: Catalogue name → Set of selected services */
    private final Map<String, Set<String>> selectedItems = new HashMap<>();

    // Backup of selections (for Cancel button functionality) */
    private Map<String, Set<String>> backupSelection;

    // The popup window that displays the dropdown */
    private PopupWindow popupWindow;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    public MultiSelectDropdown(Context context, TextView anchorView,
                               Map<String, List<String>> catalogueMap) {
        this.context = context;
        this.anchorView = anchorView;
        this.catalogueMap = catalogueMap;

        // Make the TextView interactive
        anchorView.setClickable(true);
        anchorView.setFocusable(true);

        // Show dropdown when TextView is clicked
        anchorView.setOnClickListener(v -> showDropdown());

        // Initialize the display text (runs after view is laid out)
        anchorView.post(this::updateText);
    }

    // =========================================================
    // PUBLIC METHODS
    // =========================================================

    public void updateCatalogueMap(Map<String, List<String>> map) {
        this.catalogueMap = map;

        // Initialize an empty Set for each catalogue
        // This ensures we can add selections later without null checks
        for (String catalogue : map.keySet()) {
            selectedItems.putIfAbsent(catalogue, new HashSet<>());
        }

        // Update the display text to show current selections (if any)
        updateText();
    }

        // Get current selections
        public Map<String, Set<String>> getSelectedItems() {
            return selectedItems;
        }


         // Dismisses the popup if it's showing.
        public void dismiss() {
            if (popupWindow != null) {
                popupWindow.dismiss();
            }
        }

        // Restore from saved string: "Cat: Svc1, Svc2 | ..."
        public void setSelectedItemsFromCategory(String categoryString) {
            // Clear all existing selections
            selectedItems.clear();

            // Initialize empty sets for all catalogues
            if (catalogueMap != null) {
                for (String cat : catalogueMap.keySet()) {
                    selectedItems.put(cat, new HashSet<>());
                }
            }

            // Handle empty/null input
            if (categoryString == null || categoryString.trim().isEmpty()) {
                updateText();
                return;
            }

            // Parse the category string
            // Format: "Catalogue1: Service1, Service2 | Catalogue2: Service3"
            String[] catalogueParts = categoryString.split("\\|");

            for (String part : catalogueParts) {
                part = part.trim();

                // Each part should be "Catalogue: Service1, Service2"
                if (part.contains(":")) {
                    String[] split = part.split(":", 2);
                    String catalogue = split[0].trim();
                    String servicesStr = split[1].trim();

                    // Only process if this catalogue exists in our data
                    if (selectedItems.containsKey(catalogue)) {
                        // Split services by comma
                        String[] services = servicesStr.split(",");
                        for (String service : services) {
                            // Add each service to the selection set
                            selectedItems.get(catalogue).add(service.trim());
                        }
                    }
                }
            }

            // Update the display text to show restored selections
            updateText();
        }

        // =========================================================
        // PRIVATE METHODS - DROPDOWN UI
        // =========================================================
         /*
         * UI STRUCTURE:
         * ┌─────────────────────────────────┐
         * │ LinearLayout (container)        │
         * │  ├─ ScrollView (scrollable)     │
         * │  │   └─ LinearLayout (list)     │
         * │  │       ├─ CheckBox (Cat 1)    │
         * │  │       ├─ LinearLayout         │
         * │  │       │   ├─ CheckBox (Svc 1)│
         * │  │       │   └─ CheckBox (Svc 2)│
         * │  │       └─ CheckBox (Cat 2)    │
         * │  └─ LinearLayout (buttons)      │
         * │      ├─ Button (Cancel)         │
         * │      └─ Button (Done)           │
         * └─────────────────────────────────┘
         */
        private void showDropdown() {
            // ==================== TOGGLE IF ALREADY OPEN ====================
            // If dropdown is already showing, close it
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
                return;
            }

            // ==================== VALIDATE DATA ====================
            // Don't show dropdown if no data is available
            if (catalogueMap == null || catalogueMap.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.error_no_catalogue_data), Toast.LENGTH_SHORT).show();
                return;
            }

            // ==================== BACKUP CURRENT STATE ====================
            // Save current selections so we can restore them if user clicks Cancel
            backupSelection = deepCopy(selectedItems);

            // ==================== CREATE MAIN CONTAINER ====================
            // This will hold the scrollable list and the button row
            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);

            // ==================== CREATE SCROLLVIEW ====================
            // Makes the catalogue/service list scrollable if it's too long
            ScrollView scrollView = new ScrollView(context);
            scrollView.setFillViewport(true); // Fill remaining space

            // Make ScrollView take all remaining vertical space
            // Layout weight 1f means it will expand to fill available space
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,  // Full width
                    0,                                     // Height 0 (will use weight)
                    1f                                     // Weight 1 (takes remaining space)
            );
            scrollView.setLayoutParams(scrollParams);

            // ==================== CREATE LIST CONTAINER ====================
            // This holds all the catalogue and service checkboxes
            LinearLayout listLayout = new LinearLayout(context);
            listLayout.setOrientation(LinearLayout.VERTICAL);

            // ==================== BUILD CATALOGUE ITEMS ====================
            // Loop through each catalogue and create UI for it
            for (var entry : catalogueMap.entrySet()) {
                String catalogue = entry.getKey();          // e.g., "Home Services"
                List<String> services = entry.getValue();   // e.g., ["Plumbing", "Electrical"]

                // -------------------- SERVICE CONTAINER --------------------
                // This LinearLayout holds all service checkboxes for this catalogue
                // It can be shown/hidden when parent catalogue is checked/unchecked
                LinearLayout serviceLayout = new LinearLayout(context);
                serviceLayout.setOrientation(LinearLayout.VERTICAL);
                serviceLayout.setPadding(60, 0, 0, 0); // Indent services to show hierarchy

                // -------------------- CATALOGUE CHECKBOX --------------------
                // Parent checkbox - represents the entire catalogue
                CheckBox catBox = new CheckBox(context);
                catBox.setText(catalogue);

                // Restore previous state if user had selections in this catalogue
                Set<String> selectedInCat = selectedItems.getOrDefault(catalogue, new HashSet<>());
                catBox.setChecked(!selectedInCat.isEmpty());

                // When catalogue checkbox is clicked:
                catBox.setOnCheckedChangeListener((button, checked) -> {
                    if (!checked) {
                        // If unchecked, clear all service selections in this catalogue
                        Objects.requireNonNull(selectedItems.get(catalogue)).clear();
                    }
                    // Update the display text
                    updateText();
                    // Show/hide the service list
                    serviceLayout.setVisibility(checked ? ViewGroup.VISIBLE : ViewGroup.GONE);
                });

                // Add catalogue checkbox to main list
                listLayout.addView(catBox);

                // -------------------- SERVICE CHECKBOXES --------------------
                // Create a checkbox for each service in this catalogue
                for (String service : services) {
                    CheckBox sBox = new CheckBox(context);
                    sBox.setText(service);

                    // Restore previous state if this service was selected
                    if (Objects.requireNonNull(selectedItems.get(catalogue)).contains(service)) {
                        sBox.setChecked(true);
                    }

                    // When service checkbox is clicked:
                    sBox.setOnCheckedChangeListener((btn, chk) -> {
                        if (chk) {
                            // Service was checked:
                            // 1. Add service to selection set
                            Objects.requireNonNull(selectedItems.get(catalogue)).add(service);
                            // 2. Check parent catalogue checkbox
                            catBox.setChecked(true);
                            // 3. Make sure service list is visible
                            serviceLayout.setVisibility(ViewGroup.VISIBLE);
                        } else {
                            // Service was unchecked:
                            // 1. Remove service from selection set
                            Objects.requireNonNull(selectedItems.get(catalogue)).remove(service);
                            // 2. If no services selected, uncheck parent catalogue
                            if (Objects.requireNonNull(selectedItems.get(catalogue)).isEmpty()) {
                                catBox.setChecked(false);
                                serviceLayout.setVisibility(ViewGroup.GONE);
                            }
                        }
                        // Update the display text
                        updateText();
                    });

                    // Add service checkbox to service container
                    serviceLayout.addView(sBox);
                }

                // Add service container to main list
                listLayout.addView(serviceLayout);

                // -------------------- INITIAL VISIBILITY --------------------
                // Show service list if there are saved selections, otherwise hide
                if (!Objects.requireNonNull(selectedItems.get(catalogue)).isEmpty()) {
                    serviceLayout.setVisibility(ViewGroup.VISIBLE);
                } else {
                    serviceLayout.setVisibility(ViewGroup.GONE);
                }
            }

            // ==================== ADD LIST TO SCROLLVIEW ====================
            scrollView.addView(listLayout);
            container.addView(scrollView);

            // ==================== CREATE BUTTON ROW ====================
            // Container for Cancel and Done buttons at the bottom
            LinearLayout btnRow = new LinearLayout(context);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);     // Horizontal layout
            btnRow.setGravity(Gravity.CENTER);                  // Center buttons horizontally
            btnRow.setPadding(16, 12, 16, 12);   // Add padding around buttons

            // ==================== DONE BUTTON ====================
            MaterialButton save = new MaterialButton(context);
            save.setText(context.getString(R.string.save));

            // Apply background tint (solid color)
            save.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.sf_primary)));

            // Apply stroke color and width
            save.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sf_primary)));
            save.setStrokeWidth(2);

            // Apply rounded corners (16dp converted to px)
            save.setCornerRadius((int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics()
            ));

            // text color
            save.setTextColor(ContextCompat.getColor(context, R.color.sf_on_primary));

            //Remove elevation animation if desired
            save.setStateListAnimator(null);

            // Layout parameters: WRAP_CONTENT with margin
            LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            doneParams.setMargins(0, 10, 120, 0); // right margin: 120dp
            save.setLayoutParams(doneParams);

            // minimum width for consistency
            save.setMinWidth((int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 120, context.getResources().getDisplayMetrics()
            ));

            // Done button behavior
            save.setOnClickListener(v -> {
                updateText();
                dismiss();
            });

            // ==================== CANCEL BUTTON ====================
            MaterialButton cancel = new MaterialButton(context);
            cancel.setText(context.getString(R.string.cancel));

            // Apply background tint (solid color)
            cancel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FBF0E9")));

            // Apply stroke color and width
            cancel.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sf_primary)));
            cancel.setStrokeWidth(2);

            // Apply rounded corners (16dp converted to px)
            cancel.setCornerRadius((int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics()
            ));

            // text color
            cancel.setTextColor(ContextCompat.getColor(context, R.color.sf_primary));

            //Remove elevation animation if desired
            cancel.setStateListAnimator(null);

            // Layout parameters: WRAP_CONTENT with margin
            LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cancelParams.setMargins(120, 10, 0, 0); // Left margin: 120dp
            cancel.setLayoutParams(cancelParams);

            // minimum width for consistency
            cancel.setMinWidth((int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 120, context.getResources().getDisplayMetrics()
            ));

            // Cancel button behavior
            cancel.setOnClickListener(v -> {
                selectedItems.clear();
                selectedItems.putAll(deepCopy(backupSelection));
                dismiss();
                updateText();
            });

        // ==================== ADD BUTTONS TO ROW ====================
        btnRow.addView(save);
        btnRow.addView(cancel);

        // ==================== ADD BUTTON ROW TO CONTAINER ====================
        container.addView(btnRow);

        // ==================== CREATE POPUP WINDOW ====================
        // Calculate maximum height (400dp converted to pixels)
        int maxHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,            // Unit: density-independent pixels
                400,                                     // Value: 400dp
                context.getResources().getDisplayMetrics()
        );

        // Get anchor view width to match popup width to it
        int anchorWidth = anchorView.getWidth();

        // If anchor width is not measured yet, use MATCH_PARENT as fallback
        int popupWidth = anchorWidth > 0 ? anchorWidth : ViewGroup.LayoutParams.MATCH_PARENT;

        // Create the popup window
        popupWindow = new PopupWindow(
                container,                               // Content view
                popupWidth,                              // Width: match anchor view width
                maxHeight,                               // Height: 400dp maximum
                true                                     // Focusable (allows dismissal)
        );

        // Configure popup behavior
        popupWindow.setFocusable(true);              // Can receive focus
        popupWindow.setOutsideTouchable(true);       // Dismiss when clicking outside
        popupWindow.setBackgroundDrawable(new ColorDrawable(0xFFFFFFFF)); // White background
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        popupWindow.setElevation(12f);               // Shadow elevation

        // ==================== SHOW POPUP ====================
        // Display popup below the anchor TextView
        popupWindow.showAsDropDown(
                anchorView,     // Anchor view
                0,              // X offset
                0,              // Y offset
                Gravity.START   // Alignment
        );
    }

    // =========================================================
    // PRIVATE METHODS - UTILITY
    // =========================================================
    private void updateText() {
        List<String> summary = new ArrayList<>();

        // Build summary for each catalogue that has selections
        for (var entry : selectedItems.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                // Format: "Catalogue: Service1, Service2"
                String catalogueSection = entry.getKey() + ": " + String.join(", ", entry.getValue());
                summary.add(catalogueSection);
            }
        }

        // Update TextView text
        if (summary.isEmpty()) {
            SpannableString hint = new SpannableString(context.getString(R.string.hint_select_catalogue_services));
            hint.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.gray)), 0, hint.length(), 0);
            anchorView.setText(hint);
        } else {
            anchorView.setText(TextUtils.join(" | ", summary));
        }

    }

    // Creates a copy of the selection map for backup

    private Map<String, Set<String>> deepCopy(Map<String, Set<String>> map) {
        Map<String, Set<String>> copy = new HashMap<>();

        // Copy each entry
        for (var entry : map.entrySet()) {
            // Create new HashSet with same contents
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        return copy;
    }
}