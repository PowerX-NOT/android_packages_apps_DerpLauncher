/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.lineage.hiddenapps;

import android.content.Context;
import android.content.Intent;
import android.view.MotionEvent;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;

/**
 * Detects a two-finger outward spread on the workspace to open the hidden apps drawer.
 */
public class HiddenAppsPinchGestureListener {

    private static final float SPREAD_TRIGGER_RATIO = 1.25f;

    private final Launcher mLauncher;
    private float mInitialSpan = -1f;
    private boolean mTracking;

    public HiddenAppsPinchGestureListener(Launcher launcher) {
        mLauncher = launcher;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!mLauncher.isInState(LauncherState.NORMAL)) {
            reset();
            return false;
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (ev.getPointerCount() == 2) {
                    mInitialSpan = span(ev);
                    mTracking = mInitialSpan > 0;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTracking && ev.getPointerCount() >= 2) {
                    float span = span(ev);
                    if (mInitialSpan > 0 && span >= mInitialSpan * SPREAD_TRIGGER_RATIO) {
                        mTracking = false;
                        openHiddenDrawer();
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
                reset();
                break;
            default:
                break;
        }
        return false;
    }

    private void openHiddenDrawer() {
        Context context = mLauncher;
        Intent auth = new Intent("com.android.applock.action.AUTHENTICATE");
        auth.setPackage("com.android.applock");
        auth.putExtra("package_name", context.getPackageName());
        auth.putExtra("app_label", context.getString(
                com.android.launcher3.R.string.hidden_apps_drawer_title));
        auth.putExtra("hidden_drawer", true);
        mLauncher.startActivityForResult(auth, HiddenAppsDrawerAuth.REQUEST_HIDDEN_DRAWER);
    }

    private static float span(MotionEvent ev) {
        if (ev.getPointerCount() < 2) {
            return -1f;
        }
        float x = ev.getX(0) - ev.getX(1);
        float y = ev.getY(0) - ev.getY(1);
        return (float) Math.hypot(x, y);
    }

    private void reset() {
        mInitialSpan = -1f;
        mTracking = false;
    }
}
