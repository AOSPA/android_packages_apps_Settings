/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.applications.appcompat;

import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_APP_DEFAULT;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET;

import static com.android.settings.applications.appcompat.UserAspectRatioBackupHelper.KEY_USER_ASPECT_RATIO;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.backup.BackupRestoreEventLogger;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.settings.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.InstantSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manager class for performing Backup & Restore for per-app user aspect ratio override.
 *
 * @hide
 */
public class UserAspectRatioBackupManager {
    private static final String TAG = "UserAspRatioBackupMngr";   // must be < 23 chars
    private static final boolean DEBUG = false;

    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_SERIALIZE_FAILED = "serialize_failed";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_QUERY_ASPECT_RATIO_FAILED =
            "backup_query_packages_failed";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_DESERIALIZE_FAILED = "deserialize_failed";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_TYPE_CAST_FAILED = "type_cast_failed";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_QUERY_PACKAGE_FAILED = "query_package_failed";
    @BackupRestoreEventLogger.BackupRestoreError
    private static final String ERROR_ASPECT_RATIO_FAILED = "aspect_ratio_failed";

    @NonNull
    private final IPackageManager mIPackageManager;
    @NonNull
    private final PackageManager mPackageManager;
    @NonNull
    private final Set<Integer> mAvailableUserMinAspectRatioSet;
    @NonNull
    private final UserAspectRatioRestoreStorage mStorage;

    @UserIdInt
    private final int mUserId;
    @NonNull
    private final BackupRestoreEventLogger mLogger;


    /**
     * Helper to monitor package states for the purpose of restoring user aspect ratios.
     *
     * <p>This package monitor also keeps the backed-up and restored data up-to-date when a package
     * is removed.
     */
    @VisibleForTesting
    final PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            final int aspectRatio = mStorage.getAndRemoveUserAspectRatioForPackage(packageName);
            if (aspectRatio != USER_MIN_ASPECT_RATIO_UNSET) {
                checkExistingAspectRatioAndApplyRestore(packageName, aspectRatio);
            }
        }

        @Override
        public void onPackageRemoved(@NonNull String packageName, int uid) {
            // Just in case, but the user aspect ratio should be restored and removed as soon as an
            // app is installed. Therefore there should not be anything to clean up here.
            mStorage.getAndRemoveUserAspectRatioForPackage(packageName);
        }
    };

    public UserAspectRatioBackupManager(@NonNull Context context,
            @NonNull IPackageManager iPackageManager, @NonNull PackageManager packageManager,
            @NonNull BackupRestoreEventLogger logger, @NonNull Handler handler,
            @NonNull InstantSource instantSource) {
        mIPackageManager = iPackageManager;
        mPackageManager = packageManager;
        mUserId = mPackageManager.getUserId();
        mStorage = new UserAspectRatioRestoreStorage(context, mUserId, instantSource);
        mLogger = logger;

        mPackageMonitor.register(context, UserHandle.of(UserHandle.USER_ALL), handler);

        final int[] userAspectRatioResourceValues = context.getResources().getIntArray(
                R.array.config_userAspectRatioOverrideValues);
        mAvailableUserMinAspectRatioSet = Arrays.stream(userAspectRatioResourceValues).boxed()
                .collect(Collectors.toSet());
        // App Default is not in the set above, but is always offered. Users can also specify this
        // value, for example if there is an OEM override to some other value.
        mAvailableUserMinAspectRatioSet.add(USER_MIN_ASPECT_RATIO_APP_DEFAULT);
    }

    /**
     * Returns the per-app user aspect ratio settings to be backed up as a data-blob.
     */
    @Nullable
    public byte[] getBackupPayload() {
        final Map<String, Integer> aspectRatioStates = getAllUserAspectRatios(mLogger);

        if (DEBUG) {
            Slog.d(TAG, "User aspect ratio states to backup =" + aspectRatioStates);
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos =
                new ObjectOutputStream(bos)) {
            oos.writeObject(aspectRatioStates);
            return bos.toByteArray();
        } catch (Exception e) {
            mLogger.logItemsBackupFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_SERIALIZE_FAILED);
            Slog.e(TAG, "Could not serialize payload.", e);
            return null;
        }
    }

    @NonNull
    private Map<String, Integer> getAllUserAspectRatios(@NonNull BackupRestoreEventLogger logger) {
        final List<ApplicationInfo> appList = mPackageManager.getInstalledApplications(
                PackageManager.MATCH_DISABLED_COMPONENTS
                        | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS);

        final HashMap<String, Integer> aspectRatioStates = new HashMap<>();
        for (ApplicationInfo app : appList) {
            try {
                final int aspectRatio = mIPackageManager.getUserMinAspectRatio(app.packageName,
                        mUserId);
                if (aspectRatio != USER_MIN_ASPECT_RATIO_UNSET) {
                    aspectRatioStates.put(app.packageName, aspectRatio);
                }
            } catch (RemoteException e) {
                logger.logItemsBackupFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                        ERROR_QUERY_ASPECT_RATIO_FAILED);
                Slog.e(TAG, "Could not get user aspect ratio for " + app.packageName + ".", e);
            }
        }
        return aspectRatioStates;
    }

    @Nullable
    private Map<String, Integer> readFromByteArray(@NonNull byte[] payload) {
        Object readObject = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(payload); ObjectInputStream ois =
                new ObjectInputStream(bis)) {
            readObject = ois.readObject();
            // Cannot check generic type, therefore ClassCastException will be thrown and logged if
            // type is incorrect.
            return (Map<String, Integer>) readObject;
        } catch (ClassCastException e) {
            mLogger.logItemsRestoreFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_TYPE_CAST_FAILED);
            Slog.e(TAG, "Could not cast to Map<String, Integer>: " + readObject, e);
            return null;
        } catch (IOException | ClassNotFoundException e) {
            mLogger.logItemsRestoreFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_DESERIALIZE_FAILED);
            Slog.e(TAG, "Could not read payload for backup", e);
            return null;
        }
    }

    /**
     * Restores the per-app user aspect ratio settings that were previously backed up.
     *
     * <p>This method will parse the input data blob and restore the aspect ratio settings for apps
     * which are present on the device. It will stage the aspect ratio data for the apps which are
     * not installed at the time this is called, to be referenced later when the app is installed.
     */
    public void stageAndApplyRestoredPayload(@NonNull byte[] payload) {
        final Map<String, Integer> userAspectRatioStates = readFromByteArray(payload);
        if (userAspectRatioStates == null || userAspectRatioStates.isEmpty()) {
            Slog.d(TAG, "StageAndApplyRestoredPayload: payload is empty.");
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "StageAndApplyRestoredPayload user=" + mUserId + " payload="
                    + new String(payload, StandardCharsets.UTF_8));
        }

        for (String pkgName : userAspectRatioStates.keySet()) {
            final Integer aspectRatio = userAspectRatioStates.get(pkgName);
            if (aspectRatio == null) {
                Slog.d(TAG, "StageAndApplyRestoredPayload null aspect ratio for package: "
                        + pkgName);
                continue;
            }
            if (isPackageInstalled(pkgName)) {
                Slog.d(TAG, "StageAndApplyRestoredPayload Found package: " + pkgName);
                checkExistingAspectRatioAndApplyRestore(pkgName, aspectRatio);
            } else {
                Slog.d(TAG, "StageAndApplyRestoredPayload package not installed: " + pkgName);
                mStorage.storePackageAndUserAspectRatio(pkgName, aspectRatio);
            }
        }
        mStorage.restoreCompleted();
    }

    private boolean isPackageInstalled(@NonNull String packageName) {
        try {
            return mPackageManager.getPackageInfo(packageName, /* flags= */ 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            // It is common that the package is not installed during setup. Restore will be retried
            // when the package is installed.
            if (DEBUG) {
                Slog.d(TAG, "Could not get package info for " + packageName);
            }
            return false;
        } catch (Exception e) {
            mLogger.logItemsRestoreFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_QUERY_PACKAGE_FAILED);
            if (DEBUG) {
                Slog.e(TAG, "Could not get package info for " + packageName, e);
            }
            return false;
        }
    }

    /** Applies the restore for per-app user set min aspect ratio. */
    private void checkExistingAspectRatioAndApplyRestore(@NonNull String pkgName,
            @PackageManager.UserMinAspectRatio int aspectRatio) {
        try {
            final int existingUserAspectRatio = mIPackageManager.getUserMinAspectRatio(pkgName,
                    mUserId);
            // Don't apply the restore if the aspect ratio have already been set for the app.
            if (existingUserAspectRatio != USER_MIN_ASPECT_RATIO_UNSET) {
                Slog.d(TAG, "Not restoring user aspect ratio=" + aspectRatio + " for package="
                        + pkgName + " as it is already set to " + existingUserAspectRatio + ".");
                return;
            }
            // TODO(b/407738654): Implement: when an aspect ratio option is unavailable, fallback to
            //  the closest aspect ratio option that results in bigger app bounds.
            if (mAvailableUserMinAspectRatioSet.contains(aspectRatio)) {
                mIPackageManager.setUserMinAspectRatio(pkgName, mUserId, aspectRatio);
                if (DEBUG) {
                    Slog.d(TAG, "Restored user aspect ratio=" + aspectRatio + " for package="
                            + pkgName);
                }
            }
        } catch (RemoteException | IllegalArgumentException e) {
            mLogger.logItemsRestoreFailed(KEY_USER_ASPECT_RATIO, /* count= */ 1,
                    ERROR_ASPECT_RATIO_FAILED);
            Slog.e(TAG, "Could not restore user aspect ratio for package " + pkgName, e);
        }
    }
}
