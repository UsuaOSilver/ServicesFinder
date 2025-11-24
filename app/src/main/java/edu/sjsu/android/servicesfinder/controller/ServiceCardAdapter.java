package edu.sjsu.android.servicesfinder.controller;

import static android.provider.Settings.System.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.model.Provider;
import edu.sjsu.android.servicesfinder.model.ProviderService;


/* ****************************************************************************
 * RecyclerView Adapter, displays services on the Home screen.
 * Each card shows:
 *   - service title
 *   - price
 *   - provider full name
 *   - location (city or service area)
 *   - availability (days)
 *   - category (example: Home, Automotive)
 *   - service image from Firebase Storage
 *********************************************************************************/
public class ServiceCardAdapter extends RecyclerView.Adapter<ServiceCardAdapter.ServiceCardViewHolder> {

    private final Context context;

    /**
     * serviceItems = final list we feed to RecyclerView
     * Each entry combines provider + service
     */
    private List<ServiceItem> serviceItems;


    // Callback to notify your Activity when user taps a card

    private OnServiceClickListener listener;

    public ServiceCardAdapter(Context context) {
        this.context = context;
        this.serviceItems = new ArrayList<>();
    }

    /* ****************************************************************
     * Set listener for Activity/Fragment to receive click events
     ******************************************************************/
    public void setOnServiceClickListener(OnServiceClickListener listener) {
        this.listener = listener;
    }

    /* ****************************************************************
     * Convert incoming map to our flat list
     * Input shape:
     *     Provider -> [Service1, Service2]
     * After flattening:
     *     ServiceItem(Provider, Service1)
     *     ServiceItem(Provider, Service2)
     * Then RecyclerView can display each as a card.
     ****************************************************************/
    @SuppressLint("NotifyDataSetChanged")
    public void setData(Map<Provider, List<ProviderService>> providerServiceMap) {
        serviceItems.clear(); // remove previous content

        // Loop through providers
        for (Map.Entry<Provider, List<ProviderService>> entry : providerServiceMap.entrySet()) {
            Provider provider = entry.getKey();
            List<ProviderService> services = entry.getValue();

            // Loop each service under that provider
            for (ProviderService service : services) {
                serviceItems.add(new ServiceItem(provider, service));
            }
        }

        notifyDataSetChanged(); // tell RecyclerView data changed
    }

     // Accepts already-prepared items. Useful when sorted externally.
     @SuppressLint("NotifyDataSetChanged")
    public void setServiceItems(List<ServiceItem> items) {
        this.serviceItems = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServiceCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

         // Inflate the XML layout (item_service_card.xml)
         // LayoutInflater converts XML -> actual UI View

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_service_card, parent, false);

        return new ServiceCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceCardViewHolder holder, int position) {

         // Called when a view comes on screen. We grab the correct ServiceItem and bind data.

        ServiceItem item = serviceItems.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        // Total cards displayed
        return serviceItems.size();
    }

    public List<ServiceItem> getServiceItems() {
        return serviceItems;
    }

    // =========================================================
    // VIEW HOLDER
    // =========================================================
     // reusable object holding references to UI elements
     // Avoids expensive findViewById() calls repeatedly.

    public static class ServiceCardViewHolder extends RecyclerView.ViewHolder {

        // UI views found inside item_service_card.xml
        private final ImageView serviceImage;
        private final TextView serviceTitle;
        private final TextView servicePricing;
        private final TextView providerName;
        private final TextView providerRating;
        private final TextView serviceLocation;
        private final TextView serviceAvailability;
        private final TextView categoryBadge;
        private final View verifiedBadge;

        public ServiceCardViewHolder(@NonNull View itemView) {
            super(itemView);

            // Connect UI ID --> variable
            serviceImage = itemView.findViewById(R.id.serviceImage);
            serviceTitle = itemView.findViewById(R.id.serviceTitle);
            servicePricing = itemView.findViewById(R.id.servicePricing);
            providerName = itemView.findViewById(R.id.providerName);
            providerRating = itemView.findViewById(R.id.providerRating);
            serviceLocation = itemView.findViewById(R.id.serviceLocation);
            serviceAvailability = itemView.findViewById(R.id.serviceAvailability);
            categoryBadge = itemView.findViewById(R.id.categoryBadge);
            verifiedBadge = itemView.findViewById(R.id.verifiedBadge);
        }


         // Bind data from ServiceItem -> UI views

        public void bind(ServiceItem item, OnServiceClickListener listener) {

            Provider provider = item.provider;
            ProviderService service = item.service;

            // Title
            serviceTitle.setText(service.getServiceTitle());


            // Pricing

            if (service.getPricing() != null && !service.getPricing().isEmpty()) {
                servicePricing.setText(service.getPricing());
                servicePricing.setVisibility(View.VISIBLE);
            } else {
                servicePricing.setVisibility(View.GONE); // hide empty row
            }


            // Provider

            //providerName.setText("Provider: " + provider.getFullName());
            providerName.setText(itemView.getContext().getString(R.string.provider_prefix, provider.getFullName()));


            // Rating placeholder  (You can replace with average rating logic later)

            //providerRating.setText("⭐ New");
            providerRating.setText(itemView.getContext().getString(R.string.new_service));
            providerRating.setVisibility(View.VISIBLE);

            // Location derived from serviceArea OR provider address
            String location = service.getServiceArea();
            if (location == null || location.isEmpty()) {
                location = extractCity(provider.getAddress());
            }
            serviceLocation.setText(location);

            // Availability (Mon,Tue -> Mon/Tue)
            /*
            if (service.getAvailability() != null && !service.getAvailability().isEmpty()) {
                serviceAvailability.setText(formatAvailability(service.getAvailability()));
                serviceAvailability.setVisibility(View.VISIBLE);
            } else {
                serviceAvailability.setVisibility(View.GONE);
            }
            */
            // Availability (Mon,Tue → T2 · T3 · T6 in Vietnamese)
            if (service.getAvailability() != null && !service.getAvailability().isEmpty()) {
                serviceAvailability.setText(
                        FirestoreStringTranslator.get(itemView.getContext())
                                .formatAvailabilityForDisplay(service.getAvailability())
                );
                serviceAvailability.setVisibility(View.VISIBLE);
            } else {
                serviceAvailability.setVisibility(View.GONE);
            }


            // Category badge (take only first category segment)
            /*
            if (service.getCategory() != null && !service.getCategory().isEmpty()) {
                String firstCategory = extractFirstCategory(service.getCategory());
                categoryBadge.setText(firstCategory);
                categoryBadge.setVisibility(View.VISIBLE);
            } else {
                categoryBadge.setVisibility(View.GONE);
            }
            */
            if (service.getCategory() != null && !service.getCategory().isEmpty()) {
                // ADDED: Parse → Translate → Rebuild → Extract primary
                HomeController controller = new HomeController(itemView.getContext());
                String displayText = controller.extractProviderCategoryWithServices(service.getCategory());
                categoryBadge.setText(displayText);
                categoryBadge.setVisibility(View.VISIBLE);
            } else {
                categoryBadge.setVisibility(View.GONE);
            }

             // Verified badge hidden for now  (can enable when Firestore has "verified: true")

            verifiedBadge.setVisibility(View.GONE);

            // Load image using Glide (async, cached)
            if (service.getImageUrl() != null && !service.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(service.getImageUrl())
                        .placeholder(R.drawable.ic_service_placeholder) // shown while loading
                        .error(R.drawable.ic_service_placeholder)       // shown if download fails
                        .centerCrop()
                        .into(serviceImage);
            } else {
                serviceImage.setImageResource(R.drawable.ic_service_placeholder);
            }

            // User tapped the card
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onServiceClick(item);
                }
            });
        }

         // Try to extract city part from address "Street, City, State"

        private String extractCity(String address) {
            if (address == null || address.isEmpty()) {
                return "Location TBD";
            }

            String[] parts = address.split(",");
            if (parts.length >= 2) {
                return parts[1].trim();
            }
            return address;
        }

         // Convert "Mon, Tue, Wed" => "Mon/Tue/Wed"

        private String formatAvailability(String availability) {
            return availability.replace(", ", "/");
        }

        /* *******************************************************************************
         * Service category may look like:
         *   "Home: Plumbing, Electrical | Automotive: Tire Change"       "
         **************************************************************************************/
        private String extractFirstCategory(String category) {
            if (category.contains(":")) {
                return category.split(":")[0].trim();
            }
            if (category.contains("|")) {
                return category.split("\\|")[0].trim();
            }
            return category;
        }
    }

    // =========================================================
    // Another CLASS: SERVICE ITEM MODEL
    // =========================================================
    // Wrapper for combining Provider + ProviderService  so RecyclerView can work with a single list.

    public static class ServiceItem {
        public final Provider provider;
        public final ProviderService service;

        public ServiceItem(Provider provider, ProviderService service) {
            this.provider = provider;
            this.service = service;
        }
    }

    // =========================================================
    // CLICK LISTENER INTERFACE
    // =========================================================
    public interface OnServiceClickListener {
        void onServiceClick(ServiceItem item);
    }


}
