package edu.sjsu.android.servicesfinder.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import java.util.*;

public class MultiSelectDropdown {

    private final Context context;
    private final TextView anchorView;
    private Map<String, List<String>> catalogueMap;
    private final Map<String, Set<String>> selectedItems = new HashMap<>();
    private Map<String, Set<String>> backupSelection;
    private PopupWindow popupWindow;

    public MultiSelectDropdown(Context context, TextView anchorView,
                               Map<String, List<String>> catalogueMap) {
        this.context = context;
        this.anchorView = anchorView;
        this.catalogueMap = catalogueMap;

        anchorView.setClickable(true);
        anchorView.setFocusable(true);

        anchorView.setOnClickListener(v -> showDropdown());
        anchorView.post(this::updateText);
    }

    // Called when Firestore finishes loading
    public void updateCatalogueMap(Map<String, List<String>> map) {
        this.catalogueMap = map;

        // Initialize selection groups
        for (String cat : map.keySet()) {
            selectedItems.putIfAbsent(cat, new HashSet<>());
        }

        updateText();
    }

    private void showDropdown() {

        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            return;
        }

        if (catalogueMap == null || catalogueMap.isEmpty()) {
            Toast.makeText(context, "No catalogue data available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Backup previous state for cancel button
        backupSelection = deepCopy(selectedItems);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        LinearLayout listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL);

        for (var entry : catalogueMap.entrySet()) {
            String catalogue = entry.getKey();
            List<String> services = entry.getValue();

            LinearLayout serviceLayout = new LinearLayout(context);
            serviceLayout.setOrientation(LinearLayout.VERTICAL);
            serviceLayout.setPadding(60, 0, 0, 0);

            // Parent checkbox
            CheckBox catBox = new CheckBox(context);
            catBox.setText(catalogue);

            // Restore state
            catBox.setChecked(!selectedItems.getOrDefault(catalogue, new HashSet<>()).isEmpty());

            catBox.setOnCheckedChangeListener((button, checked) -> {
                if (!checked) {
                    selectedItems.get(catalogue).clear();
                }
                updateText();
                serviceLayout.setVisibility(checked ? ViewGroup.VISIBLE : ViewGroup.GONE);
            });

            listLayout.addView(catBox);

            for (String service : services) {
                CheckBox sBox = new CheckBox(context);
                sBox.setText(service);

                // restore state
                if (selectedItems.get(catalogue).contains(service))
                    sBox.setChecked(true);

                sBox.setOnCheckedChangeListener((btn, chk) -> {
                    if (chk) {
                        selectedItems.get(catalogue).add(service);
                        catBox.setChecked(true);
                        serviceLayout.setVisibility(ViewGroup.VISIBLE);
                    } else {
                        selectedItems.get(catalogue).remove(service);
                        if (selectedItems.get(catalogue).isEmpty()) {
                            catBox.setChecked(false);
                            serviceLayout.setVisibility(ViewGroup.GONE);
                        }
                    }
                    updateText();
                });

                serviceLayout.addView(sBox);
            }

            listLayout.addView(serviceLayout);

            // expand if saved selection exists
            if (!selectedItems.get(catalogue).isEmpty()) {
                serviceLayout.setVisibility(ViewGroup.VISIBLE);
            } else {
                serviceLayout.setVisibility(ViewGroup.GONE);
            }
        }

        scrollView.addView(listLayout);
        container.addView(scrollView);

        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button cancel = new Button(context);
        cancel.setText("Cancel");
        cancel.setOnClickListener(v -> {
            selectedItems.clear();
            selectedItems.putAll(deepCopy(backupSelection));
            dismiss();
            updateText();
        });

        Button done = new Button(context);
        done.setText("Done");
        done.setOnClickListener(v -> {
            updateText();
            dismiss();
        });

        LinearLayout.LayoutParams weight =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

        cancel.setLayoutParams(weight);
        done.setLayoutParams(weight);

        btnRow.addView(cancel);
        btnRow.addView(done);

        container.addView(btnRow);

        int maxHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 400,
                context.getResources().getDisplayMetrics()
        );

        popupWindow = new PopupWindow(
                container,
                ViewGroup.LayoutParams.MATCH_PARENT,
                maxHeight,
                true);

        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0xFFFFFFFF));
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        popupWindow.setElevation(12f);

        popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.START);
    }

    private void updateText() {
        List<String> summary = new ArrayList<>();

        for (var entry : selectedItems.entrySet()) {
            if (!entry.getValue().isEmpty())
                summary.add(entry.getKey() + ": " + String.join(", ", entry.getValue()));
        }

        anchorView.setText(summary.isEmpty()
                ? "Select Catalogue & Services"
                : String.join(" | ", summary));
    }

    private Map<String, Set<String>> deepCopy(Map<String, Set<String>> map) {
        Map<String, Set<String>> copy = new HashMap<>();
        for (var e : map.entrySet()) {
            copy.put(e.getKey(), new HashSet<>(e.getValue()));
        }
        return copy;
    }

    public Map<String, Set<String>> getSelectedItems() {
        return selectedItems;
    }

    public void dismiss() {
        if (popupWindow != null) popupWindow.dismiss();
    }

    /**
     * Restore selections from saved category string
     * Example input: "Home: Plumbing, Electrical | Automotive: Tire Change"
     *
     * This parses the category string and pre-selects the appropriate
     * checkboxes in the dropdown.
     *
     * @param categoryString The formatted category string from Firestore
     */
    public void setSelectedItemsFromCategory(String categoryString) {
        // Clear existing selections
        selectedItems.clear();

        // Initialize all categories with empty sets
        if (catalogueMap != null) {
            for (String cat : catalogueMap.keySet()) {
                selectedItems.put(cat, new HashSet<>());
            }
        }

        // If no category string, just update text and return
        if (categoryString == null || categoryString.trim().isEmpty()) {
            updateText();
            return;
        }

        // Parse the category string
        // Format: "Home: Plumbing, Electrical | Automotive: Tire Change"
        String[] catalogueParts = categoryString.split("\\|");

        for (String part : catalogueParts) {
            part = part.trim();

            // Each part should be "Catalogue: Service1, Service2"
            if (part.contains(":")) {
                String[] split = part.split(":", 2);
                String catalogue = split[0].trim();
                String servicesStr = split[1].trim();

                // Only add if this catalogue exists in our map
                if (selectedItems.containsKey(catalogue)) {
                    // Split services by comma
                    String[] services = servicesStr.split(",");
                    for (String service : services) {
                        selectedItems.get(catalogue).add(service.trim());
                    }
                }
            }
        }

        // Update the display text
        updateText();
    }
}
