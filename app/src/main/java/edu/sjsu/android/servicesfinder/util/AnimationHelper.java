package edu.sjsu.android.servicesfinder.util;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

/**
 * AnimationHelper - Utility class for common UI animations
 */
public class AnimationHelper {

    private static final int ANIMATION_DURATION = 300; // milliseconds

    /**
     * Fade in animation for a view
     */
    public static void fadeIn(View view) {
        if (view.getVisibility() == View.VISIBLE) return;

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(ANIMATION_DURATION);
        view.startAnimation(fadeIn);
        view.setVisibility(View.VISIBLE);
    }

    /**
     * Fade out animation for a view
     */
    public static void fadeOut(View view) {
        if (view.getVisibility() != View.VISIBLE) return;

        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(ANIMATION_DURATION);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(fadeOut);
    }

    /**
     * Scale animation for button clicks
     */
    public static void scaleButton(View view) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 0.95f,  // X scale: from 100% to 95%
                1.0f, 0.95f,  // Y scale: from 100% to 95%
                Animation.RELATIVE_TO_SELF, 0.5f,  // Pivot X at center
                Animation.RELATIVE_TO_SELF, 0.5f   // Pivot Y at center
        );
        scaleAnimation.setDuration(100);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        scaleAnimation.setRepeatCount(1);
        view.startAnimation(scaleAnimation);
    }

    /**
     * Pulse animation for highlighting important elements
     */
    public static void pulse(View view) {
        ScaleAnimation pulseAnimation = new ScaleAnimation(
                1.0f, 1.1f,  // X scale: from 100% to 110%
                1.0f, 1.1f,  // Y scale: from 100% to 110%
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulseAnimation.setDuration(200);
        pulseAnimation.setRepeatMode(Animation.REVERSE);
        pulseAnimation.setRepeatCount(1);
        view.startAnimation(pulseAnimation);
    }

    /**
     * Smooth show/hide for loading indicators
     */
    public static void showLoading(View loadingView, View contentView) {
        fadeIn(loadingView);
        fadeOut(contentView);
    }

    public static void hideLoading(View loadingView, View contentView) {
        fadeOut(loadingView);
        fadeIn(contentView);
    }
}
