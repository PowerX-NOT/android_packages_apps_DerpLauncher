package com.android.quickstep.util;

import static android.app.ActivityManager.RECENT_IGNORE_UNAVAILABLE;

import android.app.AppLockManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.app.IAppLockStateListener;
import static com.android.quickstep.LauncherLockedStateController.TASK_LOCK_LIST_KEY_WITH_USERID;
import static com.android.quickstep.LauncherLockedStateController.TASK_LOCK_STATE;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.Launcher;
import com.android.quickstep.TaskUtilLockState;
import com.android.quickstep.views.RecentsView;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.ActivityManagerWrapper;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/** App Lock + recents utilities for Launcher overview. */
public class RecentHelper {

    private static final String TAG = RecentHelper.class.getSimpleName();
    private static RecentHelper sInstance = null;

    private AppLockManager mAppLockManager;
    private SharedPreferences mLegacyLockPrefs;
    private final Set<String> mLockedPackagesCache = new HashSet<>();
    private boolean mListenerRegistered;
    private boolean mLegacyLockListenerRegistered;

    public static RecentHelper getInstance() {
        if (sInstance == null) {
            sInstance = new RecentHelper();
        }
        return sInstance;
    }

    private AppLockManager getAppLockManager(Context context) {
        if (mAppLockManager == null) {
            mAppLockManager = context.getApplicationContext()
                    .getSystemService(AppLockManager.class);
        }
        return mAppLockManager;
    }

    private void refreshLockedPackagesCache(Context context) {
        mLockedPackagesCache.clear();
        AppLockManager mgr = getAppLockManager(context);
        if (mgr == null) {
            return;
        }
        try {
            if (!mgr.isEnabled()) {
                return;
            }
            List<String> locked = mgr.getLockedPackages();
            if (locked != null) {
                mLockedPackagesCache.addAll(locked);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to read App Lock package list", e);
        }
    }

    /**
     * True when the package is in the App Lock protected list (always mask in recents,
     * regardless of session unlock / relock policy).
     */
    public boolean isAppLockProtected(String packageName, Context context) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        refreshLockedPackagesCache(context);
        return mLockedPackagesCache.contains(packageName);
    }

    /** Legacy Launcher recents lock (overview menu), not App Lock masking. */
    public boolean isLegacyRecentsLocked(String packageName, Context context) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        if (mLegacyLockPrefs == null) {
            mLegacyLockPrefs = context.getSharedPreferences(TASK_LOCK_STATE, Context.MODE_PRIVATE);
        }
        Set<String> lockedApps = mLegacyLockPrefs.getStringSet(TASK_LOCK_LIST_KEY_WITH_USERID, null);
        if (lockedApps == null || lockedApps.isEmpty()) {
            return false;
        }
        for (String lockedPackage : lockedApps) {
            if (lockedPackage.contains(packageName)) {
                return true;
            }
        }
        return false;
    }

    /** App Lock protected and/or legacy recents lock list. */
    public boolean isAppLocked(String packageName, Context context) {
        return isAppLockProtected(packageName, context)
                || isLegacyRecentsLocked(packageName, context);
    }

    /** True when a recents card should show the privacy mask overlay. */
    public boolean shouldMaskInRecents(String packageName, Context context) {
        return isAppLocked(packageName, context);
    }

    public void registerLegacyLockListener(Context context, Runnable onChange) {
        if (mLegacyLockListenerRegistered) {
            return;
        }
        if (mLegacyLockPrefs == null) {
            mLegacyLockPrefs = context.getSharedPreferences(TASK_LOCK_STATE, Context.MODE_PRIVATE);
        }
        mLegacyLockPrefs.registerOnSharedPreferenceChangeListener((prefs, key) -> {
            if (TASK_LOCK_LIST_KEY_WITH_USERID.equals(key)) {
                onChange.run();
            }
        });
        mLegacyLockListenerRegistered = true;
    }

    public void registerAppLockListener(Context context, Runnable onChange) {
        if (mListenerRegistered) {
            return;
        }
        AppLockManager mgr = getAppLockManager(context);
        if (mgr == null) {
            return;
        }
        try {
            mgr.registerAppLockStateListener(new IAppLockStateListener.Stub() {
                @Override
                public void onAppLockStateChanged(String packageName, boolean locked) {
                    refreshLockedPackagesCache(context);
                    onChange.run();
                }
            });
            mListenerRegistered = true;
            refreshLockedPackagesCache(context);
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to register App Lock listener", e);
        }
    }

    public void clearAllTaskStacks(Context context) {
        try {
            Launcher launcher = Launcher.getLauncher(context);
            RecentsView recentsView = launcher.getOverviewPanel();
            int taskViewCount = recentsView.getTaskViewCount();
            int currentUserId = Process.myUserHandle().getIdentifier();
            refreshLockedPackagesCache(context);
            for (int i = 0; i <= taskViewCount; i++) {
                try {
                    List<ActivityManager.RecentTaskInfo> rawTasks = ActivityTaskManager.getInstance()
                            .getRecentTasks(i, RECENT_IGNORE_UNAVAILABLE, currentUserId);
                    for (ActivityManager.RecentTaskInfo recentTaskInfo : rawTasks) {
                        if (recentTaskInfo.baseIntent.getComponent() == null) {
                            continue;
                        }
                        String packageName = recentTaskInfo.baseIntent.getComponent()
                                .getPackageName();
                        Task.TaskKey taskKey = new Task.TaskKey(recentTaskInfo);
                        if (taskKey != null) {
                            int taskId = taskKey.id;
                            packageName = packageName.replace("unknown", "");
                            if (!packageName.isEmpty()) {
                                boolean taskLockState = TaskUtilLockState.getTaskLockState(
                                        context, taskKey.baseIntent.getComponent(), taskKey);
                                if (!isLegacyRecentsLocked(packageName, context)
                                        && !packageName.contains(BuildConfig.APPLICATION_ID)
                                        && !taskLockState) {
                                    ActivityManagerWrapper.getInstance().removeTask(taskId);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, "clearAllTaskStacks: ", exception);
            ActivityManagerWrapper.getInstance().removeAllRecentTasks();
        }
    }
}
