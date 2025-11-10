package edu.sjsu.android.servicesfinder.view;

import android.content.Context;
import android.graphics.Color;
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

import java.util.*;

import edu.sjsu.android.servicesfinder.R;

/**
 * ========================================================================
 * MultiSelectDropdown - Custom Multi-Level Selection Dropdown
 * ========================================================================
 *
 * PURPOSE:
 * Provides a popup dropdown that allows users to select multiple services
 * from multiple catalogues in a hierarchical structure.
 *
 * FEATURES:
 * - Two-level hierarchy: Catalogues → Services
 * - Expandable/collapsible sections
 * - Multi-selection with checkboxes
 * - Save/Cancel functionality
 * - Persistent state across sessions
 * - Automatically formats selection as string
 *
 * USAGE:
 * 1. Create instance: new MultiSelectDropdown(context, textView, catalogueMap)
 * 2. User clicks textView → dropdown appears
 * 3. User selects catalogues and services
 * 4. User clicks "Done" → selections saved
 * 5. Get selections: dropdown.getSelectedItems()
 *
 * DATA STRUCTURE:
 * Input:  Map<String, List<String>>
 *         e.g., {"Home Services": ["Plumbing", "Electrical"]}
 * Output: Map<String, Set<String>>
 *         e.g., {"Home Services": {"Plumbing"}}
 *
 * @author Your Name
 * @version 1.0
 */
public class MultiSelectDropdown {

    // =========================================================
    // INSTANCE VARIABLES
    // =========================================================

    /** Application context for creating views */
    private final Context context;

    /** TextView that triggers and displays the dropdown */
    private final TextView anchorView;

    /** Catalogue data: Catalogue name → List of service names */
    private Map<String, List<String>> catalogueMap;

    /** Current selections: Catalogue name → Set of selected services */
    private final Map<String, Set<String>> selectedItems = new HashMap<>();

    /** Backup of selections (for Cancel button functionality) */
    private Map<String, Set<String>> backupSelection;

    /** The popup window that displays the dropdown */
    private PopupWindow popupWindow;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * Creates a new MultiSelectDropdown instance.
     *
     * @param context Application context (required for creating views)
     * @param anchorView TextView that will trigger the dropdown when clicked
     * @param catalogueMap Initial catalogue data (can be empty, updated later)
     *
     * INITIALIZATION FLOW:
     * 1. Store parameters
     * 2. Make anchorView clickable
     * 3. Set click listener to show dropdown
     * 4. Initialize display text
     */
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

    /**
     * Updates the catalogue data (called when Firebase finishes loading).
     *
     * This method is called AFTER the constructor when catalogue data
     * is loaded from Firebase Firestore.
     *
     * @param map Catalogue data from Firebase
     *
     * PROCESS:
     * 1. Store new catalogue map
     * 2. Initialize empty selection sets for each catalogue
     * 3. Update the display text
     *
     * EXAMPLE:
     * Map<String, List<String>> data = new HashMap<>();
     * data.put("Home Services", Arrays.asList("Plumbing", "Electrical"));
     * dropdown.updateCatalogueMap(data);
     */
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

    /**
     * Returns the current selections.
     *
     * @return Map where keys are catalogue names and values are Sets of selected services
     *
     * EXAMPLE OUTPUT:
     * {
     *   "Home Services": {"Plumbing", "Electrical"},
     *   "Automotive": {"Oil Change"}
     * }
     *
     * USAGE:
     * Map<String, Set<String>> selections = dropdown.getSelectedItems();
     * String formatted = formatCategoryFromSelection(selections);
     * // Result: "Home Services: Plumbing, Electrical | Automotive: Oil Change"
     */
    public Map<String, Set<String>> getSelectedItems() {
        return selectedItems;
    }

    /**
     * Dismisses the popup if it's showing.
     *
     * This is called internally by Done/Cancel buttons, but can also
     * be called externally if needed.
     */
    public void dismiss() {
        if (popupWindow != null) {
            popupWindow.dismiss();
        }
    }

    /**
     * Restores selections from a saved category string.
     *
     * This method is used when loading a previously saved service from Firebase.
     * It parses the category string and pre-selects the appropriate items.
     *
     * @param categoryString Formatted string from Firestore
     *
     * INPUT FORMAT:
     * "Home Services: Plumbing, Electrical | Automotive: Oil Change"
     *
     * PARSING PROCESS:
     * 1. Split by " | " to get each catalogue section
     * 2. For each section, split by ": " to separate catalogue from services
     * 3. Split services by ", " to get individual service names
     * 4. Add each service to the appropriate catalogue's selection set
     *
     * EXAMPLE:
     * String savedCategory = "Home Services: Plumbing, Electrical";
     * dropdown.setSelectedItemsFromCategory(savedCategory);
     * // Now "Plumbing" and "Electrical" are pre-selected in the dropdown
     */
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

    /**
     * Shows the dropdown popup with catalogue and service selections.
     *
     * This is the main UI method that builds and displays the entire dropdown.
     *
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
     *
     * BEHAVIOR:
     * - Clicking catalogue checkbox expands/collapses services
     * - Selecting services automatically checks parent catalogue
     * - Deselecting all services unchecks parent catalogue
     * - Cancel button restores previous state
     * - Done button saves current state
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
            Toast.makeText(context, "No catalogue data available", Toast.LENGTH_SHORT).show();
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
        btnRow.setPadding(16, 12, 16, 12);                  // Add padding around buttons

        // ==================== CANCEL BUTTON ====================
        Button cancel = new Button(context);
        cancel.setText("Cancel");

        // Create rounded background
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setCornerRadius(24);  // 24dp rounded corners
        cancelBg.setColor(ContextCompat.getColor(context, R.color.darkgoldenrod));

        // Layout parameters: WRAP_CONTENT (button sizes to text)
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,    // Width: wrap content
                ViewGroup.LayoutParams.WRAP_CONTENT     // Height: wrap content
        );
        cancelParams.setMargins(0, 10, 120, 0); // Right margin: 120dp (creates space between buttons)

        // Apply styling
        cancel.setLayoutParams(cancelParams);
        cancel.setBackground(cancelBg);
        cancel.setTextColor(ContextCompat.getColor(context, R.color.white));
        cancel.setStateListAnimator(null);  // Remove Material Design elevation animation
        //cancel.setPadding(40, 20, 40, 20);  // Internal padding (makes button bigger)
        cancel.setMinWidth(120);            // Minimum width for consistency

        // Cancel button behavior: Restore previous selections and close
        cancel.setOnClickListener(v -> {
            selectedItems.clear();                              // Clear current selections
            selectedItems.putAll(deepCopy(backupSelection));   // Restore backup
            dismiss();                                          // Close popup
            updateText();                                       // Update display
        });

        // ==================== DONE BUTTON ====================
        Button done = new Button(context);
        done.setText("Done");

        // Create rounded background
        GradientDrawable doneBg = new GradientDrawable();
        doneBg.setCornerRadius(24);  // 24dp rounded corners
        doneBg.setColor(ContextCompat.getColor(context, R.color.darkgoldenrod));

        // Layout parameters: WRAP_CONTENT (button sizes to text)
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,    // Width: wrap content
                ViewGroup.LayoutParams.WRAP_CONTENT     // Height: wrap content
        );
        doneParams.setMargins(120, 10, 0, 0); // Left margin: 120dp (creates space between buttons)

        // Apply styling
        done.setLayoutParams(doneParams);
        done.setBackground(doneBg);
        done.setTextColor(ContextCompat.getColor(context, R.color.white));
        done.setStateListAnimator(null);  // Remove Material Design elevation animation
        //done.setPadding(40, 20, 40, 20);  // Internal padding (makes button bigger)
        done.setMinWidth(120);            // Minimum width for consistency

        // Done button behavior: Save selections and close
        done.setOnClickListener(v -> {
            updateText();   // Update display with final selections
            dismiss();      // Close popup
        });

        // ==================== ADD BUTTONS TO ROW ====================
        btnRow.addView(cancel);
        btnRow.addView(done);

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

    /**
     * Updates the anchor TextView with current selections.
     *
     * This method is called:
     * - When user checks/unchecks items
     * - When selections are restored from saved data
     * - When user clicks Done
     *
     * OUTPUT FORMAT:
     * - No selections: "Select Catalogue & Services"
     * - With selections: "Home Services: Plumbing, Electrical | Automotive: Oil Change"
     *
     * ALGORITHM:
     * 1. Loop through all catalogues
     * 2. For each catalogue with selections:
     *    a. Get catalogue name
     *    b. Get comma-separated service names
     *    c. Format as "Catalogue: Service1, Service2"
     * 3. Join all catalogue sections with " | "
     */
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
            SpannableString hint = new SpannableString("Select Catalogue & Services");
            hint.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.gray)), 0, hint.length(), 0);
            anchorView.setText(hint);
        } else {
            anchorView.setText(TextUtils.join(" | ", summary));
        }

    }

    /**
     * Creates a deep copy of the selection map.
     *
     * A deep copy is necessary because:
     * - We need to backup selections for the Cancel button
     * - Simply copying the map reference would not work (both would point to same data)
     * - We need to copy both the map AND the sets inside it
     *
     * @param map Original selection map
     * @return Deep copy of the map with new Set instances
     *
     * EXAMPLE:
     * Original: {"Home": {"Plumbing"}}
     * Copy:     {"Home": {"Plumbing"}}  (different object instances)
     *
     * Modifying copy won't affect original:
     * copy.get("Home").add("Electrical");
     * // Original still has only "Plumbing"
     */
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