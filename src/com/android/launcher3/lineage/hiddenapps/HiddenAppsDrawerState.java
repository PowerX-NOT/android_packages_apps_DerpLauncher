/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.lineage.hiddenapps;

import com.android.launcher3.model.data.AppInfo;

import java.util.Collections;
import java.util.List;

/** Temporary state while the launcher all-apps drawer shows hidden apps only. */
public final class HiddenAppsDrawerState {
    private static boolean sActive;
    private static List<AppInfo> sApps = Collections.emptyList();

    private HiddenAppsDrawerState() {
    }

    public static boolean isActive() {
        return sActive;
    }

    public static List<AppInfo> getApps() {
        return sApps;
    }

    public static void activate(List<AppInfo> apps) {
        sActive = true;
        sApps = apps != null ? apps : Collections.emptyList();
    }

    public static void deactivate() {
        sActive = false;
        sApps = Collections.emptyList();
    }
}
