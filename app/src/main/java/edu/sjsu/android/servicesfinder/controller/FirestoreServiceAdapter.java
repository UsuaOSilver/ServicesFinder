package edu.sjsu.android.servicesfinder.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import edu.sjsu.android.servicesfinder.R;
import edu.sjsu.android.servicesfinder.model.Service;

public class FirestoreServiceAdapter extends FirestoreRecyclerAdapter<Service, FirestoreServiceAdapter.ServiceViewHolder> {

    public FirestoreServiceAdapter(@NonNull FirestoreRecyclerOptions<Service> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ServiceViewHolder holder, int position, @NonNull Service model) {
        holder.nameTextView.setText(model.getName());
        holder.descriptionTextView.setText(model.getDescription());
        holder.priceTextView.setText(model.getPrice());
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ServiceViewHolder(view);
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, descriptionTextView, priceTextView;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.serviceNameTextView);
            descriptionTextView = itemView.findViewById(R.id.serviceDescriptionTextView);
            priceTextView = itemView.findViewById(R.id.servicePriceTextView);
        }
    }
}
