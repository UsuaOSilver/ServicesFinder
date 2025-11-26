package edu.sjsu.android.servicesfinder.util;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import edu.sjsu.android.servicesfinder.R;

public class ProToast {
    // Usage:
    // ProToast.success(this, "success");
    // ProToast.warning(this, "warning");
    // ProToast.error(this, "error");

    // SUCCESS Toast ===============================================
    public static void success(Context context, String message) {
        show(context, message, R.drawable.check, R.drawable.bg_toast_success);
    }

    // ERROR Toast ===============================================
    public static void error(Context context, String message) {
        show(context, message, R.drawable.error, R.drawable.bg_toast_error);
    }

    // WARNING Toast ===============================================
    public static void warning(Context context, String message) {
        show(context, message, R.drawable.warning, R.drawable.bg_toast_warning);
    }

    // CORE FUNCTION ===============================================
    private static void show(Context context, String message, int iconRes, int backgroundRes) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.pro_toast, null);

        ImageView icon = layout.findViewById(R.id.toastIcon);
        TextView text = layout.findViewById(R.id.toastText);

        layout.setBackgroundResource(backgroundRes);

        icon.setImageResource(iconRes);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120); // 120dp above bottom
        toast.show();
    }
}
