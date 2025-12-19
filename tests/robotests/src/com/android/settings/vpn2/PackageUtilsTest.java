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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class PackageUtilsTest {

    private static final String PACKAGE_NAME = "com.test.app";
    private static final int USER_ID = UserHandle.myUserId();

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    public void isExistingApp_appExists_returnsTrue() throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageInfoAsUser(PACKAGE_NAME, 0, USER_ID))
                .thenReturn(new PackageInfo());
        assertThat(PackageUtils.isExistingApp(mContext, USER_ID, PACKAGE_NAME)).isTrue();
    }

    @Test
    public void isExistingApp_appDoesNotExist_returnsFalse()
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageInfoAsUser(PACKAGE_NAME, 0, USER_ID))
                .thenThrow(new PackageManager.NameNotFoundException());
        assertThat(PackageUtils.isExistingApp(mContext, USER_ID, PACKAGE_NAME)).isFalse();
    }

    @Test
    public void hasLauncherIntent_hasIntent_returnsTrue() {
        List<ResolveInfo> resolveInfos = Collections.singletonList(new ResolveInfo());
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), eq(USER_ID)))
                .thenReturn(resolveInfos);
        assertThat(PackageUtils.hasLauncherIntent(mPackageManager, USER_ID, PACKAGE_NAME)).isTrue();
    }

    @Test
    public void hasLauncherIntent_noIntent_returnsFalse() {
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), eq(USER_ID)))
                .thenReturn(Collections.emptyList());
        assertThat(PackageUtils.hasLauncherIntent(mPackageManager, USER_ID, PACKAGE_NAME))
                .isFalse();
    }

    @Test
    public void hasCarIntent_hasIntent_returnsTrue() {
        List<ResolveInfo> resolveInfos = Collections.singletonList(new ResolveInfo());
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), eq(USER_ID)))
                .thenReturn(resolveInfos);
        assertThat(PackageUtils.hasCarIntent(mPackageManager, USER_ID, PACKAGE_NAME)).isTrue();
    }

    @Test
    public void hasCarIntent_noIntent_returnsFalse() {
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), eq(USER_ID)))
                .thenReturn(Collections.emptyList());
        assertThat(PackageUtils.hasCarIntent(mPackageManager, USER_ID, PACKAGE_NAME)).isFalse();
    }
}
