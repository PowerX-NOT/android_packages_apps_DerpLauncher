/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.lineage.hiddenapps;

import android.app.Activity;
import android.app.HiddenAppsManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.List;

/** Authenticated drawer listing hidden apps the user can launch. */
public class HiddenAppsDrawerActivity extends Activity {

    private RecyclerView mRecyclerView;
    private LinearLayout mLoadingView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.hidden_apps_drawer_title);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.activity_hidden_apps);
        mRecyclerView = findViewById(R.id.hidden_apps_list);
        mLoadingView = findViewById(R.id.hidden_apps_loading);
        mProgressBar = findViewById(R.id.hidden_apps_progress_bar);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadHiddenApps();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadHiddenApps() {
        mLoadingView.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        mProgressBar.setProgress(0);

        new Thread(() -> {
            List<HiddenAppEntry> entries = collectHiddenApps();
            runOnUiThread(() -> {
                mLoadingView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
                if (entries.isEmpty()) {
                    Toast.makeText(this, R.string.hidden_apps_drawer_empty, Toast.LENGTH_SHORT)
                            .show();
                }
                mRecyclerView.setAdapter(new HiddenAppsDrawerAdapter(entries, this::launchApp));
            });
        }).start();
    }

    private List<HiddenAppEntry> collectHiddenApps() {
        List<HiddenAppEntry> result = new ArrayList<>();
        HiddenAppsManager manager = getSystemService(HiddenAppsManager.class);
        if (manager == null) {
            return result;
        }
        PackageManager pm = getPackageManager();
        for (String pkg : manager.getHiddenPackages()) {
            if (!manager.isAppHidden(pkg)) {
                continue;
            }
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                if (pm.getLaunchIntentForPackage(pkg) == null) {
                    continue;
                }
                result.add(new HiddenAppEntry(
                        pkg,
                        info.loadLabel(pm).toString(),
                        info.loadIcon(pm)));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        result.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        return result;
    }

    private void launchApp(HiddenAppEntry entry) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(entry.packageName);
        if (launch == null) {
            Toast.makeText(this, R.string.hidden_apps_drawer_launch_failed, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launch);
        finish();
    }

    static final class HiddenAppEntry {
        final String packageName;
        final String label;
        final android.graphics.drawable.Drawable icon;

        HiddenAppEntry(String packageName, String label, android.graphics.drawable.Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }
}
