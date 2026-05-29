/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.lineage.hiddenapps;

import android.app.HiddenAppInfo;
import android.app.HiddenAppsManager;
import android.content.Intent;
import android.os.Process;
import android.widget.Toast;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.icons.BitmapInfo;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.util.Executors;

import java.util.ArrayList;
import java.util.List;

/** Opens hidden apps in the main launcher all-apps drawer. */
public final class HiddenAppsDrawer {

    private HiddenAppsDrawer() {
    }

    public static void open(Launcher launcher) {
        HiddenAppsManager manager = launcher.getSystemService(HiddenAppsManager.class);
        if (manager == null) {
            Toast.makeText(launcher, R.string.hidden_apps_drawer_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        Executors.MODEL_EXECUTOR.execute(() -> {
            List<AppInfo> apps = loadHiddenApps(manager);
            Executors.MAIN_EXECUTOR.execute(() -> {
                if (apps.isEmpty()) {
                    Toast.makeText(launcher, R.string.hidden_apps_drawer_empty,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (launcher.getAppsView().isSearching()) {
                    launcher.getAppsView().getSearchUiManager().resetSearch();
                }
                HiddenAppsDrawerState.activate(apps);
                launcher.getAppsView().getPersonalAppList().onAppsUpdated();
                launcher.getStateManager().goToState(LauncherState.ALL_APPS, true);
                launcher.setTitle(launcher.getString(R.string.hidden_apps_drawer_title));
            });
        });
    }

    public static void deactivate(Launcher launcher) {
        if (!HiddenAppsDrawerState.isActive()) {
            return;
        }
        HiddenAppsDrawerState.deactivate();
        launcher.getAppsView().getPersonalAppList().onAppsUpdated();
        launcher.setTitle(launcher.getString(R.string.all_apps_button_label));
    }

    private static List<AppInfo> loadHiddenApps(HiddenAppsManager manager) {
        List<AppInfo> result = new ArrayList<>();
        for (HiddenAppInfo info : manager.getHiddenAppsForDrawer()) {
            if (info.launchComponent == null) {
                continue;
            }
            Intent launch = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(info.launchComponent);
            AppInfo appInfo = new AppInfo(
                    info.launchComponent,
                    info.label,
                    Process.myUserHandle(),
                    launch);
            if (info.icon != null) {
                appInfo.bitmap = BitmapInfo.fromBitmap(info.icon);
            }
            result.add(appInfo);
        }
        result.sort((a, b) -> a.title.toString().compareToIgnoreCase(b.title.toString()));
        return result;
    }
}
