package edu.sjsu.android.servicesfinder.controller;

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

import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.model.Provider;
import edu.sjsu.android.servicesfinder.model.ProviderService;

/**
 * Adapter for displaying services with embedded provider info
 * Each card shows: Service title, price, provider name, rating, location, availability
 */
public class ServiceCardAdapter extends RecyclerView.Adapter<ServiceCardAdapter.ServiceCardViewHolder> {

    private final Context context;
    private List<ServiceItem> serviceItems;
    private OnServiceClickListener listener;

    public ServiceCardAdapter(Context context) {
        this.context = context;
        this.serviceItems = new ArrayList<>();
    }

    public void setOnServiceClickListener(OnServiceClickListener listener) {
        this.listener = listener;
    }

    /**
     * Update data from provider-service map
     */
    public void setData(Map<Provider, List<ProviderService>> providerServiceMap) {
        serviceItems.clear();

        for (Map.Entry<Provider, List<ProviderService>> entry : providerServiceMap.entrySet()) {
            Provider provider = entry.getKey();
            List<ProviderService> services = entry.getValue();

            for (ProviderService service : services) {
                serviceItems.add(new ServiceItem(provider, service));
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Update with pre-built service items (for sorted lists)
     */
    public void setServiceItems(List<ServiceItem> items) {
        this.serviceItems = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServiceCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_service_card, parent, false);
        return new ServiceCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceCardViewHolder holder, int position) {
        ServiceItem item = serviceItems.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return serviceItems.size();
    }

    public List<ServiceItem> getServiceItems() {
        return serviceItems;
    }

    // =========================================================
    // VIEW HOLDER
    // =========================================================

    static class ServiceCardViewHolder extends RecyclerView.ViewHolder {

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

        public void bind(ServiceItem item, OnServiceClickListener listener) {
            Provider provider = item.provider;
            ProviderService service = item.service;

            // Service title
            serviceTitle.setText(service.getServiceTitle());

            // Pricing
            if (service.getPricing() != null && !service.getPricing().isEmpty()) {
                servicePricing.setText(service.getPricing());
                servicePricing.setVisibility(View.VISIBLE);
            } else {
                servicePricing.setVisibility(View.GONE);
            }

            // Provider name with "Provider:" prefix
            providerName.setText("Provider: " + provider.getFullName());

            // Rating (placeholder for now - you can implement real ratings later)
            // For now, hide if rating is 0
            if (service.getServiceTitle() != null) {
                // Show placeholder rating - you can replace this with actual rating logic
                providerRating.setText("⭐ New");
                providerRating.setVisibility(View.VISIBLE);
            } else {
                providerRating.setVisibility(View.GONE);
            }

            // Location - extract city from service area or provider address
            String location = service.getServiceArea();
            if (location == null || location.isEmpty()) {
                location = extractCity(provider.getAddress());
            }
            serviceLocation.setText(location);

            // Availability - show days (e.g., "Tue/Wed")
            if (service.getAvailability() != null && !service.getAvailability().isEmpty()) {
                serviceAvailability.setText(formatAvailability(service.getAvailability()));
                serviceAvailability.setVisibility(View.VISIBLE);
            } else {
                serviceAvailability.setVisibility(View.GONE);
            }

            // Category badge - show first category
            if (service.getCategory() != null && !service.getCategory().isEmpty()) {
                String firstCategory = extractFirstCategory(service.getCategory());
                categoryBadge.setText(firstCategory);
                categoryBadge.setVisibility(View.VISIBLE);
            } else {
                categoryBadge.setVisibility(View.GONE);
            }

            // Verified badge - show only if verified (currently all false)
            verifiedBadge.setVisibility(View.GONE); // Will show when provider is verified

            // Service image
            if (service.getImageUrl() != null && !service.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(service.getImageUrl())
                        .placeholder(R.drawable.ic_service_placeholder)
                        .error(R.drawable.ic_service_placeholder)
                        .centerCrop()
                        .into(serviceImage);
            } else {
                serviceImage.setImageResource(R.drawable.ic_service_placeholder);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onServiceClick(item);
                }
            });
        }

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

        private String formatAvailability(String availability) {
            // Convert "Mon, Tue, Wed" to "Mon/Tue/Wed"
            return availability.replace(", ", "/");
        }

        private String extractFirstCategory(String category) {
            // Category format: "Home: Plumbing, Electrical | Automotive: Tire Change"
            // Extract just the first category name
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
    // SERVICE ITEM MODEL
    // =========================================================

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