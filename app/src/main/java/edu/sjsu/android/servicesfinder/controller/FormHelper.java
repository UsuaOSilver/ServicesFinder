package edu.sjsu.android.servicesfinder.controller;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.sjsu.android.servicesfinder.R;

/* ******************************************************************************************
 FOCUSED ON EXTRACTING DATA FROM UI COMPONENTS AND FORMATTING IT FOR STORAGE
 - COLLECTING DATA FROM UI COMPONENTS
 - FORMATTING DATA FOR STORAGE
 - CONVERTING UI SELECTIONS TO STRINGS
*******************************************************************************************/
public class FormHelper {
    private final Context context;

    public FormHelper(Context context) {
        this.context = context;
    }
    /* *************************************************************************************
     * GET SELECTED AVAILABILITY DAYS FROM CHECKBOXES
     * RETURN COMMA-SEPARATED STRING OF DAYS (E.G., "MON, TUE, FRI")
     ****************************************************************************************/
    public String getSelectedAvailability(CheckBox mon, CheckBox tue, CheckBox wed,
                                                 CheckBox thu, CheckBox fri, CheckBox sat, CheckBox sun) {

        List<String> days = new ArrayList<>();
        /*
        if (mon.isChecked()) days.add(context.getString(R.string.mon));
        if (tue.isChecked()) days.add(context.getString(R.string.tue));
        if (wed.isChecked()) days.add(context.getString(R.string.wed));
        if (thu.isChecked()) days.add(context.getString(R.string.thu));
        if (fri.isChecked()) days.add(context.getString(R.string.fri));
        if (sat.isChecked()) days.add(context.getString(R.string.sat));
        if (sun.isChecked()) days.add(context.getString(R.string.sun));
        */
        // not save translated
        if (mon.isChecked()) days.add("Mon");
        if (tue.isChecked()) days.add("Tue");
        if (wed.isChecked()) days.add("Wed");
        if (thu.isChecked()) days.add("Thu");
        if (fri.isChecked()) days.add("Fri");
        if (sat.isChecked()) days.add("Sat");
        if (sun.isChecked()) days.add("Sun");

        return String.join(", ", days);
    }

    /* *************************************************************************************
     * FORMAT CATEGORY SELECTION MAP INTO READABLE STRING
     * GET SELECTEDITEMS MAP OF CATEGORY -> SERVICES
     * RETURN FORMATTED STRING (E.G., "PLUMBING: PIPES, TOILETS | ELECTRICAL: WIRING")
     ****************************************************************************************/
    public static String formatCategoryFromSelection(Map<String, Set<String>> selectedItems) {
        List<String> summary = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : selectedItems.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                summary.add(entry.getKey() + ": " + String.join(", ", entry.getValue()));
            }
        }
        return String.join(" | ", summary);
    }

    /* ***************************************************************
     * Get selected contact preference from RadioGroup
     ****************************************************************/
    public static String getSelectedContactPreference(RadioGroup radioGroup, android.app.Activity activity) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return "";
        RadioButton radioButton = activity.findViewById(selectedId);
        return radioButton != null ? radioButton.getText().toString() : "";
    }
    /* ***************************************************************
     * Get text from TextInputEditText
     ****************************************************************/
    public static String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
    /* ***************************************************************
     * Get selected item from MaterialAutoCompleteTextView
     ****************************************************************/
    public static String getSelectedItem(MaterialAutoCompleteTextView autoCompleteTextView) {
        return autoCompleteTextView.getText() != null ? autoCompleteTextView.getText().toString().trim() : "";
    }
}