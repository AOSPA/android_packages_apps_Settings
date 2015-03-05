/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import android.util.Log;
import android.util.SparseArray;
import com.android.settings.R;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class AppOpsState {
    private static final boolean DEBUG = false;

    private static final String TAG = "AppOpsState";

    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final CharSequence[] mOpLabels;
    private final CharSequence[] mOpSummaries;
    private final PackageManager mPm;

    private List<AppOpEntry> mApps = null;

    public AppOpsState(final Context context) {
        mContext = context;

        mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mOpLabels = context.getResources().getTextArray(R.array.app_ops_labels);
        mOpSummaries = context.getResources().getTextArray(R.array.app_ops_summaries);
        mPm = context.getPackageManager();
    }

    private static final int[] OPERATIONS = new int[] {
            AppOpsManager.OP_COARSE_LOCATION,
            AppOpsManager.OP_FINE_LOCATION,
            AppOpsManager.OP_GPS,
            AppOpsManager.OP_WIFI_SCAN,
            AppOpsManager.OP_NEIGHBORING_CELLS,
            AppOpsManager.OP_MONITOR_LOCATION,
            AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION,
            AppOpsManager.OP_READ_CONTACTS,
            AppOpsManager.OP_WRITE_CONTACTS,
            AppOpsManager.OP_READ_CALL_LOG,
            AppOpsManager.OP_WRITE_CALL_LOG,
            AppOpsManager.OP_READ_CALENDAR,
            AppOpsManager.OP_WRITE_CALENDAR,
            AppOpsManager.OP_READ_CLIPBOARD,
            AppOpsManager.OP_WRITE_CLIPBOARD,
            AppOpsManager.OP_READ_SMS,
            AppOpsManager.OP_RECEIVE_SMS,
            AppOpsManager.OP_RECEIVE_EMERGECY_SMS,
            AppOpsManager.OP_RECEIVE_MMS,
            AppOpsManager.OP_RECEIVE_WAP_PUSH,
            AppOpsManager.OP_WRITE_SMS,
            AppOpsManager.OP_SEND_SMS,
            AppOpsManager.OP_READ_ICC_SMS,
            AppOpsManager.OP_WRITE_ICC_SMS,
            AppOpsManager.OP_VIBRATE,
            AppOpsManager.OP_CAMERA,
            AppOpsManager.OP_RECORD_AUDIO,
            AppOpsManager.OP_PLAY_AUDIO,
            AppOpsManager.OP_TAKE_MEDIA_BUTTONS,
            AppOpsManager.OP_TAKE_AUDIO_FOCUS,
            AppOpsManager.OP_AUDIO_MASTER_VOLUME,
            AppOpsManager.OP_AUDIO_VOICE_VOLUME,
            AppOpsManager.OP_AUDIO_RING_VOLUME,
            AppOpsManager.OP_AUDIO_MEDIA_VOLUME,
            AppOpsManager.OP_AUDIO_ALARM_VOLUME,
            AppOpsManager.OP_AUDIO_NOTIFICATION_VOLUME,
            AppOpsManager.OP_AUDIO_BLUETOOTH_VOLUME,
            AppOpsManager.OP_MUTE_MICROPHONE,
            AppOpsManager.OP_POST_NOTIFICATION,
            AppOpsManager.OP_ACCESS_NOTIFICATIONS,
            AppOpsManager.OP_CALL_PHONE,
            AppOpsManager.OP_WRITE_SETTINGS,
            AppOpsManager.OP_SYSTEM_ALERT_WINDOW,
            AppOpsManager.OP_WAKE_LOCK,
            AppOpsManager.OP_PROJECT_MEDIA,
            AppOpsManager.OP_ACTIVATE_VPN,
    };

    private static final boolean[] SHOW_PERMS = new boolean[] {
            true, /* OP_COARSE_LOCATION */
            true, /* OP_FINE_LOCATION */
            false, /* OP_GPS */
            false, /* OP_WIFI_SCAN */
            false, /* OP_NEIGHBORING_CELLS */
            false, /* OP_MONITOR_LOCATION */
            false, /* OP_MONITOR_HIGH_POWER_LOCATION */
            true, /* OP_READ_CONTACTS */
            true, /* OP_WRITE_CONTACTS */
            true, /* OP_READ_CALL_LOG */
            true, /* OP_WRITE_CALL_LOG */
            true, /* OP_READ_CALENDAR */
            true, /* OP_WRITE_CALENDAR */
            false, /* OP_READ_CLIPBOARD */
            false, /* OP_WRITE_CLIPBOARD */
            true, /* OP_READ_SMS */
            true, /* OP_RECEIVE_SMS */
            true, /* OP_RECEIVE_EMERGECY_SMS */
            true, /* OP_RECEIVE_MMS */
            true, /* OP_RECEIVE_WAP_PUSH */
            true, /* OP_WRITE_SMS */
            true, /* OP_SEND_SMS */
            true, /* OP_READ_ICC_SMS */
            true, /* OP_WRITE_ICC_SMS */
            false, /* OP_VIBRATE */
            true, /* OP_CAMERA */
            true, /* OP_RECORD_AUDIO */
            false, /* OP_PLAY_AUDIO */
            false, /* OP_TAKE_MEDIA_BUTTONS */
            false, /* OP_TAKE_AUDIO_FOCUS */
            false, /* OP_AUDIO_MASTER_VOLUME */
            false, /* OP_AUDIO_VOICE_VOLUME */
            false, /* OP_AUDIO_RING_VOLUME */
            false, /* OP_AUDIO_MEDIA_VOLUME */
            false, /* OP_AUDIO_ALARM_VOLUME */
            false, /* OP_AUDIO_NOTIFICATION_VOLUME */
            false, /* OP_AUDIO_BLUETOOTH_VOLUME */
            false, /* OP_MUTE_MICROPHONE */
            false, /* OP_POST_NOTIFICATION */
            true, /* OP_ACCESS_NOTIFICATIONS */
            true, /* OP_CALL_PHONE */
            true, /* OP_WRITE_SETTINGS */
            true, /* OP_SYSTEM_ALERT_WINDOW */
            true, /* OP_WAKE_LOCK */
            false, /* OP_PROJECT_MEDIA */
            false, /* OP_ACTIVATE_VPN */
    };

    static {
        if (OPERATIONS.length != SHOW_PERMS.length) {
            throw new IllegalStateException("OPERATIONS length (" + OPERATIONS.length + ") " +
                    "does not match SHOW_PERMS length (" + SHOW_PERMS.length + ")");
        }
    }

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppEntry {
        private final File mApkFile;
        private final ApplicationInfo mInfo;
        private final SparseArray<AppOpsManager.OpEntry> mOps =
                new SparseArray<AppOpsManager.OpEntry>();
        private final SparseArray<AppOpEntry> mOpSwitches = new SparseArray<AppOpEntry>();
        private final AppOpsState mState;

        private Drawable mIcon = null;
        private String mLabel = null;
        private boolean mMounted = false;

        public AppEntry(final AppOpsState state, final ApplicationInfo info) {
            mState = state;
            mInfo = info;

            mApkFile = new File(info.sourceDir);
        }

        public void addOp(final AppOpEntry entry, final AppOpsManager.OpEntry op) {
            mOps.put(op.getOp(), op);
            mOpSwitches.put(AppOpsManager.opToSwitch(op.getOp()), entry);
        }

        public boolean hasOp(final int op) {
            return mOps.indexOfKey(op) >= 0;
        }

        public AppOpEntry getOpSwitch(final int op) {
            return mOpSwitches.get(AppOpsManager.opToSwitch(op));
        }

        public ApplicationInfo getApplicationInfo() {
            return mInfo;
        }

        public String getLabel() {
            return mLabel;
        }

        public Drawable getIcon() {
            if (mIcon == null || !mMounted) {
                if (mMounted = mApkFile.exists()) {
                    return mIcon = mInfo.loadIcon(mState.mPm);
                }
            }

            return mIcon == null ? mState.mContext.getResources()
                    .getDrawable(android.R.drawable.sym_def_app_icon) : mIcon;
        }

        @Override
        public String toString() {
            return mLabel;
        }

        public void loadLabel(Context context) {
            if (mLabel == null || !mMounted) {
                if (mMounted = mApkFile.exists()) {
                    // Try to get the real label instead of the package name.
                    final CharSequence label = mInfo.loadLabel(context.getPackageManager());
                    mLabel = label == null ? mInfo.packageName : label.toString();
                } else {
                    mLabel = mInfo.packageName;
                }
            }
        }
    }

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppOpEntry {
        private final AppEntry mApp;
        private final ArrayList<AppOpsManager.OpEntry> mOps =
                new ArrayList<AppOpsManager.OpEntry>();
        private final AppOpsManager.PackageOps mPkgOps;
        private final ArrayList<AppOpsManager.OpEntry> mSwitchOps =
                new ArrayList<AppOpsManager.OpEntry>();
        private final int mSwitchOrder;

        public AppOpEntry(final AppOpsManager.PackageOps pkg, final AppOpsManager.OpEntry op,
                final AppEntry app, final int switchOrder) {
            mPkgOps = pkg;
            mApp = app;
            mSwitchOrder = switchOrder;

            mApp.addOp(this, op);

            mOps.add(op);

            mSwitchOps.add(op);
        }

        private static void addOp(final ArrayList<AppOpsManager.OpEntry> list,
                final AppOpsManager.OpEntry op) {
            for (int i = 0; i < list.size(); i++) {
                final AppOpsManager.OpEntry pos = list.get(i);
                if ((pos.isRunning() != op.isRunning() && op.isRunning()) ||
                        pos.getTime() < op.getTime()) {
                    list.add(i, op);
                    return;
                }
            }

            list.add(op);
        }

        public void addOp(final AppOpsManager.OpEntry op) {
            mApp.addOp(this, op);

            addOp(mOps, op);

            if (mApp.getOpSwitch(AppOpsManager.opToSwitch(op.getOp())) == null) {
                addOp(mSwitchOps, op);
            }
        }

        public AppEntry getAppEntry() {
            return mApp;
        }

        public int getSwitchOrder() {
            return mSwitchOrder;
        }

        public AppOpsManager.PackageOps getPackageOps() {
            return mPkgOps;
        }

        public int getNumOpEntry() {
            return mOps.size();
        }

        public AppOpsManager.OpEntry getOpEntry(final int pos) {
            return mOps.get(pos);
        }

        private CharSequence getCombinedText(final ArrayList<AppOpsManager.OpEntry> ops,
                final CharSequence[] items) {
            switch (ops.size()) {
            case 0:
                return "";
            case 1:
                return items[ops.get(0).getOp()];
            default:
                final StringBuilder builder = new StringBuilder();
                for (int i = 0; i < ops.size(); i++) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(items[ops.get(i).getOp()]);
                }
                return builder.toString();
            }
        }

        public CharSequence getSummaryText(final AppOpsState state) {
            return getCombinedText(mOps, state.mOpSummaries);
        }

        public CharSequence getSwitchText(final AppOpsState state) {
            return getCombinedText(mSwitchOps.size() > 0 ? mSwitchOps : mOps, state.mOpLabels);
        }

        public CharSequence getTimeText(final Resources res, final boolean showEmptyText) {
            if (isRunning()) {
                return res.getText(R.string.app_ops_running);
            }

            if (getTime() > 0) {
                return DateUtils.getRelativeTimeSpanString(getTime(), System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
            }

            return showEmptyText ? res.getText(R.string.app_ops_never_used) : "";
        }

        public boolean isRunning() {
            return mOps.get(0).isRunning();
        }

        public long getTime() {
            return mOps.get(0).getTime();
        }

        @Override
        public String toString() {
            return mApp.getLabel();
        }
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppOpEntry> APP_OP_COMPARATOR = new Comparator<AppOpEntry>() {
        private final Collator mCollator = Collator.getInstance();

        @Override
        public int compare(final AppOpEntry lh, final AppOpEntry rh) {
            return mCollator.compare(lh.getAppEntry().getLabel(), rh.getAppEntry().getLabel());
        }
    };

    private void addOp(final List<AppOpEntry> entries, final AppOpsManager.PackageOps pkgOps,
            final AppEntry appEntry, final AppOpsManager.OpEntry opEntry, final boolean allowMerge,
            final int switchOrder) {
        if (allowMerge && entries.size() > 0) {
            for (AppOpEntry entry : entries) {
                if (entry.getAppEntry() == appEntry) {
                    if (DEBUG) Log.d(TAG, "Add op " + opEntry.getOp() + " to package " +
                            pkgOps.getPackageName() + ": append to " + entry);
                    entry.addOp(opEntry);
                    return;
                }
            }
        }

        AppOpEntry entry = appEntry.getOpSwitch(opEntry.getOp());
        if (entry != null) {
            entry.addOp(opEntry);
            return;
        }

        entry = new AppOpEntry(pkgOps, opEntry, appEntry, switchOrder);
        if (DEBUG) Log.d(TAG, "Add op " + opEntry.getOp() + " to package " +
                pkgOps.getPackageName() + ": making new " + entry);
        entries.add(entry);
    }

    public List<AppOpEntry> buildState() {
        return buildState(0, null);
    }

    private AppEntry getAppEntry(final Context context, final HashMap<String, AppEntry> appEntries,
            final String packageName, ApplicationInfo appInfo) {
        AppEntry appEntry = appEntries.get(packageName);

        if (appEntry == null) {
            if (appInfo == null) {
                try {
                    appInfo = mPm.getApplicationInfo(packageName,
                            PackageManager.GET_DISABLED_COMPONENTS |
                                    PackageManager.GET_UNINSTALLED_PACKAGES);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Unable to find info for package " + packageName, e);
                    return null;
                }
            }

            appEntry = new AppEntry(this, appInfo);
            appEntry.loadLabel(context);
            appEntries.put(packageName, appEntry);
        }

        return appEntry;
    }

    public List<AppOpEntry> buildState(final int uid, final String packageName) {
        final Context context = mContext;

        final HashMap<String, AppEntry> appEntries = new HashMap<String, AppEntry>();
        final List<AppOpEntry> entries = new ArrayList<AppOpEntry>();

        final ArrayList<String> perms = new ArrayList<String>();
        final ArrayList<Integer> permOps = new ArrayList<Integer>();
        final int[] opToOrder = new int[AppOpsManager._NUM_OP];

        for (int i = 0; i < OPERATIONS.length; i++) {
            if (SHOW_PERMS[i]) {
                final int op = OPERATIONS[i];
                final String perm = AppOpsManager.opToPermission(op);
                if (perm != null && !perms.contains(perm)) {
                    perms.add(perm);
                    permOps.add(op);
                    opToOrder[op] = i;
                }
            }
        }

        final List<AppOpsManager.PackageOps> pkgs;
        if (packageName != null) {
            pkgs = mAppOps.getOpsForPackage(uid, packageName, OPERATIONS);
        } else {
            pkgs = mAppOps.getPackagesForOps(OPERATIONS);
        }

        if (pkgs != null) {
            for (final AppOpsManager.PackageOps pkgOps : pkgs) {
                final AppEntry appEntry = getAppEntry(context, appEntries,
                        pkgOps.getPackageName(), null);

                if (appEntry == null) {
                    continue;
                }

                for (final AppOpsManager.OpEntry opEntry : pkgOps.getOps()) {
                    addOp(entries, pkgOps, appEntry, opEntry, packageName == null,
                            packageName == null ? 0 : opToOrder[opEntry.getOp()]);
                }
            }
        }

        final List<PackageInfo> apps;
        if (packageName != null) {
            apps = new ArrayList<PackageInfo>();

            try {
                apps.add(mPm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS));
            } catch (PackageManager.NameNotFoundException e) {
            }
        } else {
            final String[] permsArray = new String[perms.size()];
            perms.toArray(permsArray);
            apps = mPm.getPackagesHoldingPermissions(permsArray, 0);
        }

        for (final PackageInfo appInfo : apps) {
            final AppEntry appEntry = getAppEntry(context, appEntries, appInfo.packageName,
                    appInfo.applicationInfo);

            if (appEntry == null) {
                continue;
            }

            List<AppOpsManager.OpEntry> dummyOps = null;
            AppOpsManager.PackageOps pkgOps = null;

            if (appInfo.requestedPermissions != null) {
                for (int i = 0; i < appInfo.requestedPermissions.length; i++) {
                    if (appInfo.requestedPermissionsFlags != null) {
                        if ((appInfo.requestedPermissionsFlags[i] &
                                PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                            if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + " perm " +
                                    appInfo.requestedPermissions[i] + " not granted; skipping");
                            continue;
                        }
                    }

                    if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + ": requested perm " +
                            appInfo.requestedPermissions[i]);

                    for (int j = 0; j < perms.size(); j++) {
                        if (!perms.get(j).equals(appInfo.requestedPermissions[i])) {
                            continue;
                        }

                        if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + " perm " +
                                perms.get(j) + " has op " + permOps.get(j) + ": " +
                                appEntry.hasOp(permOps.get(j)));

                        if (appEntry.hasOp(permOps.get(j))) {
                            continue;
                        }

                        if (dummyOps == null) {
                            dummyOps = new ArrayList<AppOpsManager.OpEntry>();
                            pkgOps = new AppOpsManager.PackageOps(appInfo.packageName,
                                    appInfo.applicationInfo.uid, dummyOps);
                        }

                        final AppOpsManager.OpEntry opEntry = new AppOpsManager.OpEntry(
                                permOps.get(j), AppOpsManager.MODE_ALLOWED, 0, 0, 0);
                        dummyOps.add(opEntry);
                        addOp(entries, pkgOps, appEntry, opEntry, packageName == null,
                                packageName == null ? 0 : opToOrder[opEntry.getOp()]);
                    }
                }
            }
        }

        Collections.sort(entries, APP_OP_COMPARATOR);
        return entries;
    }
}
