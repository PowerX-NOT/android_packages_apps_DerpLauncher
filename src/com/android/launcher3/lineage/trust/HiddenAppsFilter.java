/*
 * Copyright (C) 2019 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.lineage.trust;

import android.app.HiddenAppsManager;
import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;

import com.android.launcher3.AppFilter;
import com.android.launcher3.dagger.ApplicationContext;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.PredictedContainerInfo;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class HiddenAppsFilter extends AppFilter {

    private final Context mContext;

    @Inject
    public HiddenAppsFilter(@ApplicationContext Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean shouldShowApp(ComponentName app) {
        if (shouldHidePackage(mContext, app.getPackageName())) {
            return false;
        }
        return super.shouldShowApp(app);
    }

    /** Returns true when the package should be hidden from launcher surfaces. */
    public static boolean shouldHidePackage(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        HiddenAppsManager manager = context.getSystemService(HiddenAppsManager.class);
        return manager != null && manager.shouldHideFromLauncher(packageName);
    }

    /** Returns true when a launcher item should be hidden from suggestions and predictions. */
    public static boolean shouldHideItem(Context context, ItemInfo item) {
        if (item == null) {
            return false;
        }
        ComponentName component = item.getTargetComponent();
        if (component != null) {
            return shouldHidePackage(context, component.getPackageName());
        }
        if (item.getIntent() != null) {
            String pkg = item.getIntent().getPackage();
            if (!TextUtils.isEmpty(pkg)) {
                return shouldHidePackage(context, pkg);
            }
        }
        return false;
    }

    /** Filters predicted/suggested items for the all-apps row and hotseat. */
    public static List<ItemInfo> filterPredictions(Context context, List<ItemInfo> items) {
        if (items == null || items.isEmpty()) {
            return items;
        }
        List<ItemInfo> filtered = new ArrayList<>(items.size());
        for (ItemInfo item : items) {
            if (!shouldHideItem(context, item)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /** Returns a copy of {@code info} with hidden packages removed from predictions. */
    public static PredictedContainerInfo filterPredictedContainer(
            Context context, PredictedContainerInfo info) {
        if (info == null) {
            return null;
        }
        return new PredictedContainerInfo(info.id, filterPredictions(context, info.getContents()));
    }
}
