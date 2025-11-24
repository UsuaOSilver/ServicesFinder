package edu.sjsu.android.servicesfinder.controller;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.sjsu.android.servicesfinder.R;
/* Method in this class
A. For cat/services
    String process:
    1. parseEnglishCategoryString(String savedCategory): String -> structured map.
        "Cleaning & Maintenance: Deep Cleaning, Air Filter Repair | Electronics & IT: Wi-Fi/network repair, Computer repair | Plumbing | Moving Service: Small furniture moving"
        ->
        {
          "Cleaning & Maintenance" = ["Deep Cleaning", "Air Filter Repair"],
          "Electronics & IT" = ["Wi-Fi/network repair", "Computer repair"],
          "Plumbing" = [],
          "Moving Service" = ["Small furniture moving"]
        }
    2. buildLocalizedCategoryString(Map<String, Set<String>> localizedMap): structured map -> string.

    In loading:
    - translateServiceNameToLocal(String englishKey): Converts a single service name from English to localized.
    - translateCategoryName(String english) Converts a category from English to localized language (e.g., EN → CN, EN → VN).
    - translateCategory(String categoryField): Translates a single cat/services in EN to localized string.
    - translateCatalogueMap(Map<String, List<String>> englishMap): Converts entire catalogue (category + services) EN → localized.
    - getLocalizedCategoryMap(Map<String, Set<String>> englishMap): convert category map from EN → localized.

    In saving:
    1. buildEnglishCategoryString(Map<String, Set<String>> englishSelectionMap): bBuilds the Firestore-saved English service string when saving selections.
    2. reverseTranslateSelection(Map<String, Set<String>> localizedMap): Converts localized category/service back into English before saving.
    3. reverseCategoryName(String localizedName): Turns localized category name back to English.

B. For Availability
    formatAvailabilityForDisplay(String englishAvailability): Formats availability day strings (EN → local).

C. Contact
    1. reverseContactPreference(String localized): Converts localized contact type from local to English (Call/Text/Email -> local).
    2 translateContactPreference(String pref): Converts contact preference (call/text/email) to localized.
 */

public class FirestoreStringTranslator {
    // ----------------------------------------------------------------------
    // INSTANCE
    // ----------------------------------------------------------------------
    private static FirestoreStringTranslator INSTANCE;
    private final Context context;

    private FirestoreStringTranslator(Context context) {
        this.context = context.getApplicationContext();

        // Log current device locale at startup
        Resources res = context.getResources();
        String currentLocale = res.getConfiguration().locale.toString();
    }

    public static FirestoreStringTranslator get(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new FirestoreStringTranslator(context);
        }
        return INSTANCE;
    }

    // Helper: Get fresh Resources with current device locale
    private Resources getLocalResources() {
        Resources localRes = context.getResources();
        return localRes;
    }

    // Helper: Get fresh Resources with English locale
    private Resources getEnglishResources() {
        Configuration enConfig = new Configuration(context.getResources().getConfiguration());
        enConfig.setLocale(Locale.ENGLISH);

        // Use createConfigurationContext to get isolated Resources
        Context enContext = context.createConfigurationContext(enConfig);
        Resources enRes = enContext.getResources();

        // Test by loading a known resource
        try {
            String testCat = enRes.getString(R.string.cat_cleaning_maintenance);
        } catch (Exception e) {
            Log.e("TRACE_TRANSLATOR", "    [DEBUG] Failed to load test resource!", e);
        }

        return enRes;
    }

    // ----------------------------------------------------------------------
    // FORWARD TRANSLATION (EN → localized)
    // ----------------------------------------------------------------------

    public String translateCategory(String categoryField) {
        if (categoryField == null || categoryField.trim().isEmpty()) return "";

        if (categoryField.contains(":")) {
            return translateLegacyFormat(categoryField);
        }
        return translateCategoryName(categoryField);
    }

    private String translateLegacyFormat(String legacy) {
        String[] parts = legacy.split("\\|");
        List<String> translatedParts = new ArrayList<>();

        for (String part : parts) {
            translatedParts.add(translateLegacyPart(part.trim()));
        }
        return String.join(" | ", translatedParts);
    }

    private String translateLegacyPart(String part) {
        if (!part.contains(":")) {
            return translateCategoryName(part);
        }

        String[] split = part.split(":", 2);
        String cat = translateCategoryName(split[0].trim());
        String[] services = split[1].split(",");

        List<String> localizedServices = new ArrayList<>();
        for (String svc : services) {
            localizedServices.add(translateServiceNameToLocal(svc.trim()));
        }

        return cat + ": " + String.join(", ", localizedServices);
    }

    // ----------------------------------------------------------------------
    // CATEGORY TRANSLATION (EN → localized)
    // ----------------------------------------------------------------------
    public String translateCategoryName(String english) {
        if (english == null || english.trim().isEmpty()) return "";
        try {
            Resources localRes = getLocalResources();
            Resources enRes = getEnglishResources();

            Field[] fields = R.string.class.getDeclaredFields();
            int matchAttempts = 0;

            for (Field field : fields) {
                int resId = field.getInt(null);

                String resourceName = localRes.getResourceEntryName(resId);
                if (!resourceName.startsWith("cat_")) continue;

                String englishValue = enRes.getString(resId);

                matchAttempts++;
                if (matchAttempts <= 3) {
                    Log.e("TRACE_TRANSLATOR", "    ↳ Comparing with resource: \"" + englishValue + "\" [" + resourceName + "]");
                }

                if (englishValue.equalsIgnoreCase(english)) {
                    String localized = localRes.getString(resId);
                    return localized;
                }
            }
        } catch (Exception e) {
            Log.e("Translator", "translateCategoryName error", e);
        }

        String fallback = capitalize(english);
        return fallback;
    }

    // ----------------------------------------------------------------------
    // SERVICE TRANSLATION (EN → localized)
    // ----------------------------------------------------------------------
    public String translateServiceNameToLocal(String englishKey) {
        if (englishKey == null || englishKey.trim().isEmpty()) return "";
       try {
            Resources localRes = getLocalResources();
            Resources enRes = getEnglishResources();

            Field[] fields = R.string.class.getDeclaredFields();
            int matchAttempts = 0;

            for (Field field : fields) {
                int resId = field.getInt(null);

                String enValue = enRes.getString(resId);
                String resourceName = localRes.getResourceEntryName(resId);

                matchAttempts++;
                if (matchAttempts <= 3 && resourceName.startsWith("svc_")) {
                    Log.e("TRACE_TRANSLATOR", "    ↳ Comparing with resource: \"" + enValue + "\" [" + resourceName + "]");
                }

                if (enValue.equalsIgnoreCase(englishKey)) {
                    String localized = localRes.getString(resId);
                    return localized;
                }
            }

            Log.e("TRACE_TRANSLATOR", "    ✗ NO MATCH after checking " + matchAttempts + " resources");
        } catch (Exception e) {
            Log.e("Translator", "Reflection error", e);
        }

        String fallback = capitalize(englishKey);
        return fallback;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ----------------------------------------------------------------------
    // AVAILABILITY TRANSLATION
    // ----------------------------------------------------------------------
    public String formatAvailabilityForDisplay(String englishAvailability) {
        if (englishAvailability == null || englishAvailability.trim().isEmpty()) return "";

        String[] days = englishAvailability.split(",\\s*");
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < days.length; i++) {
            if (i > 0) out.append(i == days.length - 1 ? " & " : " · ");
            out.append(translateDay(days[i].trim()));
        }

        return out.toString();
    }

    private String translateDay(String englishDay) {
        Resources localRes = getLocalResources();

        switch (englishDay) {
            case "Mon": return localRes.getString(R.string.mon);
            case "Tue": return localRes.getString(R.string.tue);
            case "Wed": return localRes.getString(R.string.wed);
            case "Thu": return localRes.getString(R.string.thu);
            case "Fri": return localRes.getString(R.string.fri);
            case "Sat": return localRes.getString(R.string.sat);
            case "Sun": return localRes.getString(R.string.sun);
            default: return englishDay;
        }
    }

    // ----------------------------------------------------------------------
    // SAVE (generate English string → Firestore)
    // ----------------------------------------------------------------------
    public String buildEnglishCategoryString(Map<String, Set<String>> englishSelectionMap) {
        if (englishSelectionMap == null || englishSelectionMap.isEmpty()) return "";

        List<String> parts = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : englishSelectionMap.entrySet()) {
            String category = entry.getKey();
            Set<String> services = entry.getValue();

            if (services.isEmpty()) {
                parts.add(category);
            } else {
                parts.add(category + ": " + String.join(", ", services));
            }
        }

        return String.join(" | ", parts);
    }

    // ----------------------------------------------------------------------
    // REVERSE TRANSLATION (localized → English)
    // ----------------------------------------------------------------------
    public Map<String, Set<String>> reverseTranslateSelection(Map<String, Set<String>> localizedMap) {
        Map<String, Set<String>> englishMap = new LinkedHashMap<>();

        if (localizedMap == null || localizedMap.isEmpty()) return englishMap;

        for (Map.Entry<String, Set<String>> entry : localizedMap.entrySet()) {
            String localizedCategory = entry.getKey();
            String englishCategory = reverseCategoryName(localizedCategory);

            Set<String> englishServices = new HashSet<>();
            for (String svc : entry.getValue()) {
                englishServices.add(reverseServiceName(svc));
            }

            englishMap.put(englishCategory, englishServices);
        }

        return englishMap;
    }

    public String reverseCategoryName(String localizedName) {
        if (localizedName == null || localizedName.trim().isEmpty()) return localizedName;

        try {
            Resources localRes = getLocalResources();
            Resources enRes = getEnglishResources();

            Field[] fields = R.string.class.getDeclaredFields();
            for (Field field : fields) {
                int resId = field.getInt(null);

                String resourceName = localRes.getResourceEntryName(resId);
                if (!resourceName.startsWith("cat_")) continue;

                String localizedValue = localRes.getString(resId);
                if (localizedValue.equalsIgnoreCase(localizedName)) {
                    return enRes.getString(resId);
                }
            }
        } catch (Exception e) {
            Log.e("Translator", "reverseCategoryName error", e);
        }

        return localizedName;
    }

    //********************************************************************************************
    // * Reverse translate service name: Localized → English
    // * Algorithm:
    // * 1. Find localized string in localized resources → get resource ID
    // * 2. Use that resource ID to get English string from English resources
    // *******************************************************************************************
    private String reverseServiceName(String localizedName) {
        if (localizedName == null || localizedName.trim().isEmpty()) {
            return localizedName;
        }

        try {
            Resources localRes = getLocalResources();
            Resources enRes = getEnglishResources();

            Field[] fields = R.string.class.getDeclaredFields();

            for (Field field : fields) {
                int resId = field.getInt(null);

                String resourceName = localRes.getResourceEntryName(resId);
                // Only check service strings (svc_*)
                if (!resourceName.startsWith("svc_")) continue;

                // Step 1: Find matching localized string
                String localizedValue = localRes.getString(resId);

                if (localizedValue.equalsIgnoreCase(localizedName)) {
                    // Step 2: Use the same resource ID to get English value
                    String englishValue = enRes.getString(resId);
                    return englishValue;
                }
            }
        } catch (Exception e) {
            Log.e("Translator", "reverseServiceName error", e);
        }

        // If no match found, return original
        return localizedName;
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;

        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    private String englishServiceFromKey(String key) {
        switch (key) {
            case "wifi_network_repair": return "Wi-Fi/network repair";
            case "computer_repair": return "Computer repair";
            case "smartphone_repair": return "Smartphone repair";
            case "website_design": return "Website design";
            case "graphic_design": return "Graphic design";
            default:
                return capitalize(key.replace("_", " "));
        }
    }

    // ----------------------------------------------------------------------
    // PARSER FOR FIRESTORE STRING (English)
    // ----------------------------------------------------------------------
    public static Map<String, Set<String>> parseEnglishCategoryString(String savedCategory) {
        Map<String, Set<String>> result = new LinkedHashMap<>();

        if (savedCategory == null || savedCategory.trim().isEmpty()) return result;

        String[] parts = savedCategory.split("\\|");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            if (!part.contains(":")) {
                result.put(part, new HashSet<>());
                continue;
            }

            String[] split = part.split(":", 2);
            String cat = split[0].trim();
            String[] svcList = split[1].split(",");

            Set<String> services = new HashSet<>();
            for (String s : svcList) {
                services.add(s.trim());
            }

            result.put(cat, services);
        }

        return result;
    }

    public String buildLocalizedCategoryString(Map<String, Set<String>> localizedMap) {
        if (localizedMap == null || localizedMap.isEmpty()) return "";

        List<String> parts = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : localizedMap.entrySet()) {
            String localizedCategory = entry.getKey();
            Set<String> localizedServices = entry.getValue();

            if (localizedServices == null || localizedServices.isEmpty()) {
                parts.add(localizedCategory);
            } else {
                parts.add(localizedCategory + ": " + String.join(", ", localizedServices));
            }
        }

        return String.join(" | ", parts);
    }

    public Map<String, List<String>> translateCatalogueMap(Map<String, List<String>> englishMap) {
        Map<String, List<String>> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : englishMap.entrySet()) {
            String englishCategory = entry.getKey();
            List<String> englishServices = entry.getValue();

            String localizedCategory = translateCategoryName(englishCategory);

            List<String> localizedServices = new ArrayList<>();
            for (String svc : englishServices) {
                localizedServices.add(translateServiceNameToLocal(svc));
            }

            result.put(localizedCategory, localizedServices);
        }

        return result;
    }

    public static Map<String, Set<String>> getLocalizedCategoryMap(Map<String, Set<String>> englishMap) {
        if (englishMap == null || englishMap.isEmpty()) return new LinkedHashMap<>();

        Map<String, Set<String>> result = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : englishMap.entrySet()) {
            String englishCategory = entry.getKey();
            Set<String> englishServices = entry.getValue();

            String localizedCat = INSTANCE.translateCategoryName(englishCategory);

            Set<String> localizedServices = new HashSet<>();
            for (String svc : englishServices) {
                localizedServices.add(INSTANCE.translateServiceNameToLocal(svc));
            }

            result.put(localizedCat, localizedServices);
        }

        return result;
    }

    public String reverseContactPreference(String localized) {
        if (localized == null || localized.trim().isEmpty()) return localized;

        localized = localized.trim();
        Resources localRes = getLocalResources();
        String pkg = localRes.getResourcePackageName(R.string.app_name);

        Map<String, String> fixed = new LinkedHashMap<>();
        fixed.put("call", "Call");
        fixed.put("text", "Text");
        fixed.put("email", "Email");

        for (String key : fixed.keySet()) {
            int resId = localRes.getIdentifier("contact_" + key, "string", pkg);

            if (resId != 0) {
                String localizedValue = localRes.getString(resId).trim();

                if (localizedValue.equalsIgnoreCase(localized)) {
                    return fixed.get(key);
                }
            }
        }

        return fixed.getOrDefault(localized.toLowerCase(), localized);
    }

    public String translateContactPreference(String pref) {
        if (pref == null) return "";

        Resources localRes = getLocalResources();

        switch (pref.toLowerCase()) {
            case "call": return localRes.getString(R.string.contact_call);
            case "text": return localRes.getString(R.string.contact_text);
            case "email": return localRes.getString(R.string.contact_email);
            default: return pref;
        }
    }
}