/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.lineage.hiddenapps;

import android.app.HiddenAppsManager;
import android.content.Context;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.launcher3.LauncherModel.ModelUpdateTask;
import com.android.launcher3.model.AllAppsList;
import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.model.ModelTaskController;
import com.android.launcher3.model.tasks.PackageUpdatedTask;
import com.android.launcher3.util.ItemInfoMatcher;

import java.util.HashSet;
import java.util.Set;

/**
 * Immediately hides or restores launcher surfaces for packages whose hide mode changed.
 * Does not rely on {@link android.content.pm.LauncherApps#getActivityList} returning empty.
 */
public final class HiddenAppsRefreshTask implements ModelUpdateTask {
    private static final String TAG = "HiddenApps.Refresh";

    @NonNull
    private final UserHandle mUser;
    @NonNull
    private final Set<String> mPackages;

    public HiddenAppsRefreshTask(@NonNull UserHandle user, @NonNull Set<String> packages) {
        mUser = user;
        mPackages = packages;
    }

    @Override
    public void execute(@NonNull ModelTaskController taskController, @NonNull BgDataModel dataModel,
            @NonNull AllAppsList appsList) {
        Context context = taskController.getContext();
        HiddenAppsManager manager = context.getSystemService(HiddenAppsManager.class);
        if (manager == null) {
            Log.w(TAG, "HiddenAppsManager unavailable");
            return;
        }

        Set<String> toHide = new HashSet<>();
        Set<String> toRestore = new HashSet<>();
        for (String pkg : mPackages) {
            if (manager.shouldHideFromLauncher(pkg)) {
                toHide.add(pkg);
                Log.i(TAG, "hide pkg=" + pkg + " mode=" + manager.getHiddenMode(pkg));
            } else {
                toRestore.add(pkg);
                Log.i(TAG, "restore pkg=" + pkg);
            }
        }

        if (!toHide.isEmpty()) {
            for (String pkg : toHide) {
                taskController.getIconCache().removeIconsForPkg(pkg, mUser);
                appsList.removePackage(pkg, mUser);
            }
            taskController.bindApplicationsIfNeeded();
            taskController.deleteAndBindComponentsRemoved(
                    ItemInfoMatcher.ofPackages(toHide, mUser),
                    "HiddenApps: package hidden from launcher");
            Log.i(TAG, "removed from launcher pkgs=" + toHide);
        }

        for (String pkg : toRestore) {
            new PackageUpdatedTask(PackageUpdatedTask.OP_UPDATE, mUser, pkg)
                    .execute(taskController, dataModel, appsList);
        }
    }
}
