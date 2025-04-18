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

import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.backup.BackupAnnotations;
import android.app.backup.BackupRestoreEventLogger;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the {@link UserAspectRatioBackupManager}.
 *
 * To run this test: atest SettingsUnitTests:UserAspectRatioBackupManagerTest
 */
@RunWith(AndroidJUnit4.class)
public class UserAspectRatioBackupManagerTest {
    private static final String DEFAULT_PACKAGE_NAME = "com.android.testapp";
    private static final String OTHER_PACKAGE_NAME = "com.android.anotherapp";

    private static final int DEFAULT_USER_ID = 0;

    private static final Map<String, Integer> DEFAULT_PACKAGE_ASPECT_RATIO_MAP = Map.of(
            DEFAULT_PACKAGE_NAME, USER_MIN_ASPECT_RATIO_FULLSCREEN);

    @Mock
    private IPackageManager mMockIPackageManager;
    @Mock
    private PackageManager mMockPackageManager;

    private UserAspectRatioBackupManager mBackupManager;

    private final BackupRestoreEventLogger mBackupLogger = new BackupRestoreEventLogger(
            BackupAnnotations.OperationType.BACKUP);
    private final BackupRestoreEventLogger mRestoreLogger = new BackupRestoreEventLogger(
            BackupAnnotations.OperationType.RESTORE);

    @Before
    public void setUp() throws Exception {
        Context context = spy(ApplicationProvider.getApplicationContext());
        mMockIPackageManager = mock(IPackageManager.class);
        mMockPackageManager = mock(PackageManager.class);
        mBackupManager = new UserAspectRatioBackupManager(context, mMockIPackageManager,
                mMockPackageManager);
    }

    @Test
    public void testBackupPayload_noAppsInstalled_returnsEmptyBlob()
            throws Exception {
        setUpInstalledPackages(List.of());

        verifyPayloadForAppAspectRatio(Map.of(), mBackupManager.getBackupPayload(mBackupLogger));
    }

    @Test
    public void testBackupPayload_noAspectRatiosSet_returnsEmptyBlob() throws Exception {
        setUpAspectRatioForPackage(DEFAULT_PACKAGE_NAME, USER_MIN_ASPECT_RATIO_UNSET);
        setUpInstalledPackages(List.of(DEFAULT_PACKAGE_NAME));

        verifyPayloadForAppAspectRatio(Map.of(), mBackupManager.getBackupPayload(mBackupLogger));
    }

    @Test
    public void testBackupPayload_aspectRatiosSet_returnsPackageSpectRatioBlob() throws Exception {
        setUpAspectRatioForPackage(DEFAULT_PACKAGE_NAME, USER_MIN_ASPECT_RATIO_FULLSCREEN);
        setUpInstalledPackages(List.of(DEFAULT_PACKAGE_NAME));

        final byte[] payload = mBackupManager.getBackupPayload(mBackupLogger);

        verifyPayloadForAppAspectRatio(DEFAULT_PACKAGE_ASPECT_RATIO_MAP, payload);
    }

    @Test
    public void testBackupPayload_exceptionInGetAspectRatioAllPackages_returnsEmptyBlob()
            throws Exception {
        setUpInstalledPackages(List.of(DEFAULT_PACKAGE_NAME));
        doThrow(new RemoteException("mock")).when(mMockIPackageManager).getUserMinAspectRatio(
                anyString(), anyInt());

        verifyPayloadForAppAspectRatio(Map.of(), mBackupManager.getBackupPayload(mBackupLogger));
    }

    @Test
    public void testBackupPayload_exceptionGetAspectRatioSomePackages_appsWithExceptionNotBackedUp()
            throws Exception {
        // Set up two apps.
        setUpInstalledPackages(List.of(DEFAULT_PACKAGE_NAME, OTHER_PACKAGE_NAME));

        setUpAspectRatioForPackage(DEFAULT_PACKAGE_NAME, USER_MIN_ASPECT_RATIO_FULLSCREEN);
        // Exception when getting locales for anotherApp.
        doThrow(new RemoteException("mock exception")).when(mMockIPackageManager)
                .getUserMinAspectRatio(
                        eq(OTHER_PACKAGE_NAME), anyInt());

        byte[] payload = mBackupManager.getBackupPayload(mBackupLogger);

        verifyPayloadForAppAspectRatio(DEFAULT_PACKAGE_ASPECT_RATIO_MAP, payload);
    }

    @Test
    public void testRestore_emptyPayload_nothingRestored() throws Exception {
        mBackupManager.stageAndApplyRestoredPayload(/* payload= */ new byte[0],
                mRestoreLogger);

        verifyNothingRestored();
    }

    @Test
    public void testRestore_zeroLengthPayload_nothingRestored() throws Exception {
        mBackupManager.stageAndApplyRestoredPayload(/* payload= */ writeEmptyTestPayload(),
                mRestoreLogger);

        verifyNothingRestored();
    }

    @Test
    public void testRestore_appNotInstalled_nothingIsRestored() throws Exception {
        final byte[] out = writeTestPayload(DEFAULT_PACKAGE_ASPECT_RATIO_MAP);
        // Backed up app is not installed on the restore device.
        setUpInstalledPackages(List.of());

        mBackupManager.stageAndApplyRestoredPayload(out, mRestoreLogger);

        verifyNothingRestored();
    }

    @Test
    public void testRestore_aspectRatioRestoreForInstalledApp() throws Exception {
        final byte[] out = writeTestPayload(DEFAULT_PACKAGE_ASPECT_RATIO_MAP);
        setUpInstalledPackages(List.of(DEFAULT_PACKAGE_NAME));
        setUpAspectRatioForPackage(DEFAULT_PACKAGE_NAME, USER_MIN_ASPECT_RATIO_UNSET);

        mBackupManager.stageAndApplyRestoredPayload(out, mRestoreLogger);

        // Locales were restored.
        verify(mMockIPackageManager).setUserMinAspectRatio(DEFAULT_PACKAGE_NAME,
                DEFAULT_USER_ID, USER_MIN_ASPECT_RATIO_FULLSCREEN);
    }

    @Test
    public void testRestore_aspectRatioAlreadySet_doNotRestoreAspectRatio() throws Exception {
        final byte[] out = writeTestPayload(DEFAULT_PACKAGE_ASPECT_RATIO_MAP);
        setUpInstalledPackages(List.of(DEFAULT_PACKAGE_NAME));
        setUpAspectRatioForPackage(DEFAULT_PACKAGE_NAME, USER_MIN_ASPECT_RATIO_SPLIT_SCREEN);

        mBackupManager.stageAndApplyRestoredPayload(out, mRestoreLogger);

        verifyNothingRestored();
    }


    /**
     * Verifies that nothing was restored for any package.
     *
     * <p>If {@link IPackageManager#setUserMinAspectRatio(String, int, int)} is not invoked, we can
     * conclude that nothing was restored.
     */
    private void verifyNothingRestored() throws Exception {
        verify(mMockIPackageManager, never()).setUserMinAspectRatio(anyString(), anyInt(),
                anyInt());
    }

    private void setUpInstalledPackages(@NonNull List<String> packageNameList)
            throws PackageManager.NameNotFoundException {
        // Return null for all packages, and later set proper values for installed ones.
        doReturn(null).when(mMockPackageManager).getPackageInfo(anyString(), anyInt());

        final List<ApplicationInfo> applicationInfoList = new ArrayList<>();
        for (String packageName : packageNameList) {
            doReturn(new PackageInfo()).when(mMockPackageManager).getPackageInfo(eq(packageName),
                    anyInt());
            final ApplicationInfo applicationInfo = new ApplicationInfo();
            applicationInfo.packageName = packageName;
            applicationInfoList.add(applicationInfo);
        }

        doReturn(applicationInfoList).when(mMockPackageManager).getInstalledApplications(anyInt());
    }

    private static byte[] writeEmptyTestPayload() throws IOException {
        return writeTestPayload(Map.of());
    }

    @Nullable
    private static byte[] writeTestPayload(@NonNull Map<String, Integer> pkgAspectRatioMap)
            throws IOException {
        if (pkgAspectRatioMap.isEmpty()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos =
                new ObjectOutputStream(bos)) {
            oos.writeObject(pkgAspectRatioMap);
            return bos.toByteArray();
        }
    }

    private void setUpAspectRatioForPackage(@NonNull String packageName, int aspectRatio)
            throws Exception {
        doReturn(aspectRatio).when(mMockIPackageManager).getUserMinAspectRatio(
                eq(packageName), anyInt());
    }

    private void verifyPayloadForAppAspectRatio(Map<String, Integer> expectedPkgAspectRatioMap,
            byte[] payload) throws IOException, ClassNotFoundException {

        try (ByteArrayInputStream bis = new ByteArrayInputStream(payload); ObjectInputStream ois =
                new ObjectInputStream(bis)) {
            final Map<String, Integer> backupDataMap = (Map<String, Integer>) ois.readObject();
            assertEquals(expectedPkgAspectRatioMap, backupDataMap);
        }
    }
}
