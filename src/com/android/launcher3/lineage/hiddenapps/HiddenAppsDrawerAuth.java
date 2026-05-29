/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.lineage.hiddenapps;

import android.app.Activity;

import com.android.launcher3.Launcher;

/** Handles auth result before opening the hidden-apps drawer in launcher. */
public final class HiddenAppsDrawerAuth {

    public static final int REQUEST_HIDDEN_DRAWER = 0x4A50;

    private HiddenAppsDrawerAuth() {
    }

    public static boolean handleActivityResult(Launcher launcher, int requestCode, int resultCode) {
        if (requestCode != REQUEST_HIDDEN_DRAWER) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK) {
            HiddenAppsDrawer.open(launcher);
        }
        return true;
    }
}
