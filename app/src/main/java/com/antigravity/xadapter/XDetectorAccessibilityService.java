package com.antigravity.xadapter;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class XDetectorAccessibilityService extends AccessibilityService {

    private static final String TWITTER_PKG_1 = "com.twitter.android";
    private static final String TWITTER_PKG_2 = "com.twitter.android.lite";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String pkgName = event.getPackageName().toString();

        if (pkgName.equals(TWITTER_PKG_1) || pkgName.equals(TWITTER_PKG_2)) {
            // X (Twitter) opened or focused! Start the floating bubble automatically
            Intent serviceIntent = new Intent(this, FloatingBubbleService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}
