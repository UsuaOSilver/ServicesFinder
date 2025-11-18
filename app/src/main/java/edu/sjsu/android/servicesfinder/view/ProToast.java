package edu.sjsu.android.servicesfinder.view;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.Toast;

import edu.sjsu.android.servicesfinder.databinding.ProToastLayoutBinding;

public class ProToast {
    // ===============================
    //  HOW TO USE THIS CLASS
    // ===============================
    /*
        Example usage:
        // Success toast
        ProToast.show(this, "Profile updated!", ProToast.SUCCESS);
        // Error toast
        ProToast.show(this, "Invalid password!", ProToast.ERROR);
        // Info toast
        ProToast.show(this, "Loading…", ProToast.INFO);
        In Adapter:
        ProToast.show(holder.itemView.getContext(), "Clicked!", ProToast.INFO);
        In Fragment:
        ProToast.show(requireContext(), "Saved", ProToast.SUCCESS);
    */
    // ===============================

    public static final int SUCCESS = 1;
    public static final int ERROR = 2;
    public static final int INFO = 3;

    public static void show(Context context, String message, int type) {

        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate using ViewBinding → no more null root warning
        ProToastLayoutBinding binding =
                ProToastLayoutBinding.inflate(inflater, null, false);

        // Set message
        binding.toastText.setText(message);

        // Change background color by type
        switch (type) {
            case SUCCESS:
                binding.toastContainer.setBackgroundColor(0xFF2E7D32); // green
                break;
            case ERROR:
                binding.toastContainer.setBackgroundColor(0xFFC62828); // red
                break;
            default:
                binding.toastContainer.setBackgroundColor(0xFF1565C0); // blue
        }

        // Build toast
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(binding.getRoot());
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 180);
        toast.show();
    }
}
