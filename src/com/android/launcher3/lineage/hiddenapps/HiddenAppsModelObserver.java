/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.lineage.hiddenapps;

import android.app.HiddenAppsManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.app.IHiddenAppsStateListener;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.util.Executors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reloads launcher model when hidden-app configuration changes. */
public final class HiddenAppsModelObserver extends ContentObserver {
    private static final String TAG = "HiddenApps.Model";

    private final Context mContext;
    private final LauncherModel mModel;
    private Set<String> mLastHiddenPackages = Set.of();

    private final IHiddenAppsStateListener mStateListener = new IHiddenAppsStateListener.Stub() {
        @Override
        public void onHiddenAppsChanged() {
            Log.i(TAG, "onHiddenAppsChanged from system service");
            scheduleRefresh();
        }
    };

    public HiddenAppsModelObserver(Context context, LauncherModel model, Handler handler) {
        super(handler);
        mContext = context.getApplicationContext();
        mModel = model;
    }

    public void register() {
        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(HiddenAppsManager.SETTING_CONFIG),
                false,
                this,
                UserHandle.USER_ALL);
        HiddenAppsManager manager = mContext.getSystemService(HiddenAppsManager.class);
        if (manager != null) {
            try {
                manager.registerHiddenAppsStateListener(mStateListener);
                Log.i(TAG, "registered IHiddenAppsStateListener");
            } catch (Exception e) {
                Log.w(TAG, "Failed to register hidden apps listener", e);
            }
        } else {
            Log.w(TAG, "HiddenAppsManager unavailable at register");
        }
        Log.i(TAG, "registered ContentObserver for " + HiddenAppsManager.SETTING_CONFIG);
        scheduleRefresh();
    }

    public void unregister() {
        mContext.getContentResolver().unregisterContentObserver(this);
        HiddenAppsManager manager = mContext.getSystemService(HiddenAppsManager.class);
        if (manager != null) {
            try {
                manager.unregisterHiddenAppsStateListener(mStateListener);
            } catch (Exception e) {
                Log.w(TAG, "Failed to unregister hidden apps listener", e);
            }
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.i(TAG, "hiddenapps_config changed uri=" + uri + " selfChange=" + selfChange);
        scheduleRefresh();
    }

    private void scheduleRefresh() {
        Executors.MODEL_EXECUTOR.execute(this::refreshHiddenPackages);
    }

    private void refreshHiddenPackages() {
        HiddenAppsManager manager = mContext.getSystemService(HiddenAppsManager.class);
        if (manager == null) {
            Log.w(TAG, "HiddenAppsManager unavailable, forcing model reload");
            mModel.forceReload();
            return;
        }

        List<String> hidden = manager.getHiddenPackages();
        Set<String> toRefresh = new HashSet<>(hidden);
        toRefresh.addAll(mLastHiddenPackages);
        mLastHiddenPackages = new HashSet<>(hidden);

        if (toRefresh.isEmpty()) {
            return;
        }

        Log.i(TAG, "refresh hidden count=" + hidden.size() + " refreshPkgs=" + toRefresh);
        UserHandle user = Process.myUserHandle();
        mModel.enqueueModelUpdateTask(new HiddenAppsRefreshTask(user, toRefresh));
    }
}
