/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.vpn2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.VpnManager;
import android.os.UserHandle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppExclusionUtilsTest {
    private static final String PACKAGE_NAME_1 = "com.test.app1";
    private static final String PACKAGE_NAME_2 = "com.test.app2";
    private static final String VPN_PACKAGE = "com.test.vpn";
    private static final int USER_ID = UserHandle.myUserId();

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;
    @Mock private VpnManager mVpnManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(VpnManager.class)).thenReturn(mVpnManager);
    }

    @Test
    public void fetchNameListFromInfoList_shouldReturnPackageNames() {
        ApplicationInfo appInfo1 = new ApplicationInfo();
        appInfo1.packageName = PACKAGE_NAME_1;
        ApplicationInfo appInfo2 = new ApplicationInfo();
        appInfo2.packageName = PACKAGE_NAME_2;
        List<ApplicationInfo> appInfoList = new ArrayList<>();
        appInfoList.add(appInfo1);
        appInfoList.add(appInfo2);

        List<String> packageNames = AppExclusionUtils.fetchNameListFromInfoList(appInfoList);

        assertThat(packageNames).containsExactly(PACKAGE_NAME_1, PACKAGE_NAME_2);
    }

    @Test
    public void getApplicationLabel_shouldReturnLabel()
            throws PackageManager.NameNotFoundException {
        final String appLabel = "Test App";
        final ApplicationInfo appInfo = new ApplicationInfo();
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.applicationInfo = appInfo;
        appInfo.nonLocalizedLabel = appLabel;

        when(mPackageManager.getPackageInfoAsUser(PACKAGE_NAME_1, 0, USER_ID))
                .thenReturn(packageInfo);

        final String result =
                AppExclusionUtils.getApplicationLabel(mContext, USER_ID, PACKAGE_NAME_1);

        assertThat(result).isEqualTo(appLabel);
    }

    @Test
    public void getApplicationLabel_nameNotFound_shouldReturnEmptyString()
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageInfoAsUser(PACKAGE_NAME_1, 0, USER_ID))
                .thenThrow(new PackageManager.NameNotFoundException());

        final String result =
                AppExclusionUtils.getApplicationLabel(mContext, USER_ID, PACKAGE_NAME_1);

        assertThat(result).isEmpty();
    }

    @Test
    public void removeUninstalledApp_noExclusionList_shouldDoNothing() {
        when(mVpnManager.getAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE)).thenReturn(null);
        AppExclusionUtils.removeUninstalledApp(mContext, USER_ID, VPN_PACKAGE);
        verify(mVpnManager, never()).setAppExclusionList(any(), anyString(), any());
    }

    @Test
    public void removeUninstalledApp_oneAppUninstalled_shouldUpdateList()
            throws PackageManager.NameNotFoundException {
        List<String> exclusionList = Arrays.asList(PACKAGE_NAME_1, PACKAGE_NAME_2);
        when(mVpnManager.getAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE))
                .thenReturn(exclusionList);

        when(mPackageManager.getPackageInfoAsUser(PACKAGE_NAME_1, 0, USER_ID))
                .thenReturn(new PackageInfo());
        when(mPackageManager.getPackageInfoAsUser(PACKAGE_NAME_2, 0, USER_ID))
                .thenThrow(new PackageManager.NameNotFoundException());

        AppExclusionUtils.removeUninstalledApp(mContext, USER_ID, VPN_PACKAGE);

        List<String> expectedList = Arrays.asList(PACKAGE_NAME_1);
        verify(mVpnManager).setAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE, expectedList);
    }

    @Test
    public void removeUninstalledApp_noAppUninstalled_shouldNotUpdateList()
            throws PackageManager.NameNotFoundException {
        final List<String> exclusionList = Arrays.asList(PACKAGE_NAME_1, PACKAGE_NAME_2);
        when(mVpnManager.getAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE))
                .thenReturn(exclusionList);

        when(mPackageManager.getPackageInfoAsUser(PACKAGE_NAME_1, 0, USER_ID))
                .thenReturn(new PackageInfo());
        when(mPackageManager.getPackageInfoAsUser(PACKAGE_NAME_2, 0, USER_ID))
                .thenReturn(new PackageInfo());

        AppExclusionUtils.removeUninstalledApp(mContext, USER_ID, VPN_PACKAGE);

        verify(mVpnManager, never()).setAppExclusionList(any(), anyString(), any());
    }

    @Test
    public void getAppExclusionList_nullList_shouldReturnEmptyList() {
        when(mVpnManager.getAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE)).thenReturn(null);

        List<String> result = AppExclusionUtils.getAppExclusionList(mContext, USER_ID, VPN_PACKAGE);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void getAppExclusionList_shouldReturnList() {
        List<String> exclusionList = Arrays.asList(PACKAGE_NAME_1, PACKAGE_NAME_2);
        when(mVpnManager.getAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE))
                .thenReturn(exclusionList);

        List<String> result = AppExclusionUtils.getAppExclusionList(mContext, USER_ID, VPN_PACKAGE);

        assertThat(result).isEqualTo(exclusionList);
    }

    @Test
    public void setAppExclusionList_shouldCallVpnManager() {
        List<String> exclusionList = Arrays.asList(PACKAGE_NAME_1, PACKAGE_NAME_2);
        AppExclusionUtils.setAppExclusionList(mContext, USER_ID, VPN_PACKAGE, exclusionList);

        verify(mVpnManager).setAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE, exclusionList);
    }

    @Test
    public void getPackageInfo_shouldReturnPackageInfo()
            throws PackageManager.NameNotFoundException {
        final PackageInfo packageInfo = new PackageInfo();
        when(mPackageManager.getPackageInfoAsUser(PACKAGE_NAME_1, 0, USER_ID))
                .thenReturn(packageInfo);

        final PackageInfo result =
                AppExclusionUtils.getPackageInfo(mContext, USER_ID, PACKAGE_NAME_1);

        assertThat(result).isSameInstanceAs(packageInfo);
    }

    @Test
    public void getPackageInfo_nameNotFound_shouldReturnNull()
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageInfoAsUser(PACKAGE_NAME_1, 0, USER_ID))
                .thenThrow(new PackageManager.NameNotFoundException());

        final PackageInfo result =
                AppExclusionUtils.getPackageInfo(mContext, USER_ID, PACKAGE_NAME_1);

        assertThat(result).isNull();
    }

    @Test
    public void fetchNameListFromInfoList_emptyList_shouldReturnEmptyList() {
        final List<String> packageNames =
                AppExclusionUtils.fetchNameListFromInfoList(new ArrayList<>());
        assertThat(packageNames).isNotNull();
        assertThat(packageNames).isEmpty();
    }
}
