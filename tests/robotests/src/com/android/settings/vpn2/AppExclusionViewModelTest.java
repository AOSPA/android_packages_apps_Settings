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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.VpnManager;
import android.os.Looper;
import android.os.UserHandle;

import androidx.lifecycle.Observer;

import com.android.settings.testutils.InstantTaskExecutorRule;

import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppExclusionViewModelTest {
    private static final String PACKAGE_NAME_1 = "com.test.app1";
    private static final String PACKAGE_NAME_2 = "com.test.app2";
    private static final String PACKAGE_NAME_3 = "com.test.app3";
    private static final String VPN_PACKAGE = "com.test.vpn";
    private static final int USER_ID = UserHandle.myUserId();

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private Application mApplication;
    @Mock private PackageManager mPackageManager;
    @Mock private VpnManager mVpnManager;
    @Mock private Observer<List<String>> mEnabledListObserver;
    @Mock private Observer<List<String>> mCandidateListObserver;
    @Mock private Observer<Boolean> mSetListObserver;

    private AppExclusionViewModel mViewModel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mApplication.getPackageManager()).thenReturn(mPackageManager);
        when(mApplication.getSystemService(VpnManager.class)).thenReturn(mVpnManager);
        // Mock the call for getApplicationLabel used in the comparator
        when(mPackageManager.getApplicationInfo(
                        anyString(), any(PackageManager.ApplicationInfoFlags.class)))
                .thenAnswer(
                        invocation -> {
                            ApplicationInfo info = new ApplicationInfo();
                            info.packageName = invocation.getArgument(0);
                            return info;
                        });
        when(mPackageManager.getApplicationLabel(any(ApplicationInfo.class)))
                .thenAnswer(
                        invocation -> {
                            ApplicationInfo info = invocation.getArgument(0);
                            // Return package name as label for predictable sorting
                            return info.packageName;
                        });

        mViewModel =
                new AppExclusionViewModel(
                        mApplication,
                        USER_ID,
                        VPN_PACKAGE,
                        MoreExecutors.newDirectExecutorService());
        mViewModel.getAppExclusionEnabledLiveData().observeForever(mEnabledListObserver);
        mViewModel.getAppExclusionCandidateLiveData().observeForever(mCandidateListObserver);
        mViewModel.getSetAppExclusionEnabledLiveData().observeForever(mSetListObserver);
    }

    private ApplicationInfo createApplicationInfo(String packageName) {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.packageName = packageName;
        appInfo.enabled = true;
        return appInfo;
    }

    private void mockIntentResolution(String packageName, boolean resolves) {
        List<ResolveInfo> resolveInfoList =
                resolves ? Collections.singletonList(new ResolveInfo()) : Collections.emptyList();
        doReturn(resolveInfoList)
                .when(mPackageManager)
                .queryIntentActivitiesAsUser(
                        argThat(
                                intent ->
                                        intent.getPackage().equals(packageName)
                                                && intent.getAction().equals(Intent.ACTION_MAIN)),
                        anyInt(),
                        eq(USER_ID));
    }

    @Test
    public void fillAppExclusionLists_shouldUpdateLiveData() {
        // Arrange
        List<String> exclusionList = Collections.singletonList(PACKAGE_NAME_1);
        List<ApplicationInfo> installedApps =
                Arrays.asList(
                        createApplicationInfo(PACKAGE_NAME_1),
                        createApplicationInfo(PACKAGE_NAME_2),
                        createApplicationInfo(PACKAGE_NAME_3));

        when(mVpnManager.getAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE))
                .thenReturn(new ArrayList<>(exclusionList));
        when(mPackageManager.getInstalledApplicationsAsUser(
                        any(PackageManager.ApplicationInfoFlags.class), eq(USER_ID)))
                .thenReturn(installedApps);

        // Mock which apps have launcher intents
        mockIntentResolution(PACKAGE_NAME_1, true); // Excluded, has intent
        mockIntentResolution(PACKAGE_NAME_2, true); // Candidate, has intent
        mockIntentResolution(PACKAGE_NAME_3, false); // Not a candidate, no intent

        // Act
        mViewModel.fillAppExclusionLists();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        ArgumentCaptor<List<String>> enabledListCaptor = ArgumentCaptor.forClass(List.class);
        verify(mEnabledListObserver).onChanged(enabledListCaptor.capture());
        assertThat(enabledListCaptor.getValue()).containsExactly(PACKAGE_NAME_1);

        ArgumentCaptor<List<String>> candidateListCaptor = ArgumentCaptor.forClass(List.class);
        verify(mCandidateListObserver).onChanged(candidateListCaptor.capture());
        // Only PACKAGE_NAME_2 should be a candidate. 1 is excluded, 3 has no launcher.
        assertThat(candidateListCaptor.getValue()).containsExactly(PACKAGE_NAME_2);
    }

    @Test
    public void addAppToExclusionList_shouldUpdateList() {
        // Arrange
        when(mVpnManager.getAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE))
                .thenReturn(new ArrayList<>());
        when(mVpnManager.setAppExclusionList(any(), anyString(), any())).thenReturn(true);

        // Act
        mViewModel.addAppToExclusionList(PACKAGE_NAME_1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(mVpnManager)
                .setAppExclusionList(
                        eq(UserHandle.of(USER_ID)), eq(VPN_PACKAGE), listCaptor.capture());
        assertThat(listCaptor.getValue()).containsExactly(PACKAGE_NAME_1);

        verify(mSetListObserver).onChanged(true);
    }

    @Test
    public void removeAppFromExclusionList_shouldUpdateList() {
        // Arrange
        List<String> initialList = new ArrayList<>(Arrays.asList(PACKAGE_NAME_1, PACKAGE_NAME_2));
        when(mVpnManager.getAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE))
                .thenReturn(initialList);
        when(mVpnManager.setAppExclusionList(any(), anyString(), any())).thenReturn(true);

        // Mock that the pre-existing packages are launchable so they aren't filtered out
        mockIntentResolution(PACKAGE_NAME_1, true);
        mockIntentResolution(PACKAGE_NAME_2, true);

        // Act
        mViewModel.removeAppFromExclusionList(PACKAGE_NAME_1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        ArgumentCaptor<List<String>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(mVpnManager)
                .setAppExclusionList(
                        eq(UserHandle.of(USER_ID)), eq(VPN_PACKAGE), listCaptor.capture());
        assertThat(listCaptor.getValue()).containsExactly(PACKAGE_NAME_2);

        verify(mSetListObserver).onChanged(true);
    }

    @Test
    public void addAppToExclusionList_alreadyExcluded_doesNotAddAgain() {
        // Arrange
        final List<String> initialList = new ArrayList<>(Arrays.asList(PACKAGE_NAME_1));
        when(mVpnManager.getAppExclusionList(UserHandle.of(USER_ID), VPN_PACKAGE))
                .thenReturn(initialList);
        mockIntentResolution(PACKAGE_NAME_1, true);

        // Act
        mViewModel.addAppToExclusionList(PACKAGE_NAME_1);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        verify(mVpnManager, never()).setAppExclusionList(any(), anyString(), any());
        verify(mSetListObserver, never()).onChanged(anyBoolean());
    }
}
