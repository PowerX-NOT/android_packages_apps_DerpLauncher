/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.lineage.hiddenapps;

import android.app.Activity;
import android.content.Intent;

import com.android.launcher3.Launcher;

/** Handles auth result before opening {@link HiddenAppsDrawerActivity}. */
public final class HiddenAppsDrawerAuth {

    public static final String EXTRA_HIDDEN_DRAWER = "hidden_drawer";
    public static final int REQUEST_HIDDEN_DRAWER = 0x4A50;

    private HiddenAppsDrawerAuth() {
    }

    public static boolean handleActivityResult(Launcher launcher, int requestCode, int resultCode) {
        if (requestCode != REQUEST_HIDDEN_DRAWER) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK) {
            launcher.startActivity(new Intent(launcher, HiddenAppsDrawerActivity.class));
        }
        return true;
    }
}
