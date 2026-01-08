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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowUserManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {AppExclusionActivityTest.ShadowLaunchedActivity.class})
public class AppExclusionActivityTest {
    private static final String VPN_PACKAGE = "com.test.vpn";
    private static final int USER_ID = UserHandle.myUserId();

    private ShadowUserManager mShadowUserManager;
    private ShadowPackageManager mShadowPackageManager;
    private Context mContext;
    private AppExclusionActivity mActivity;
    private ActivityController<AppExclusionActivity> mActivityController;

    @Implements(Activity.class)
    public static class ShadowLaunchedActivity extends ShadowActivity {
        private String mLaunchedFromPackage;

        public void setLaunchedFromPackage(String launchedFromPackage) {
            mLaunchedFromPackage = launchedFromPackage;
        }

        @Implementation
        public String getLaunchedFromPackage() {
            return mLaunchedFromPackage;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mShadowUserManager = Shadows.shadowOf(mContext.getSystemService(UserManager.class));
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
    }

    private void installPackage(String packageName, String appName) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo.packageName = packageName;
        packageInfo.applicationInfo.name = appName;
        mShadowPackageManager.installPackage(packageInfo);
    }

    private void createActivity(String launchedFromPackage) {
        Intent intent = new Intent(mContext, AppExclusionActivity.class);
        mActivityController = Robolectric.buildActivity(AppExclusionActivity.class, intent);
        mActivity = mActivityController.get();
        ShadowLaunchedActivity shadowActivity = Shadow.extract(mActivity);
        shadowActivity.setLaunchedFromPackage(launchedFromPackage);
        mActivityController.setup();
    }

    @Test
    public void onCreate_userRestricted_shouldFinish() {
        mShadowUserManager.setUserRestriction(
                UserHandle.of(USER_ID), UserManager.DISALLOW_CONFIG_VPN, true);

        createActivity(VPN_PACKAGE);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_noVpnPackage_shouldFinish() {
        createActivity(null);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_valid_shouldAddFragment() {
        installPackage(VPN_PACKAGE, "Test VPN");
        createActivity(VPN_PACKAGE);
        ShadowLooper.idleMainLooper();
        assertThat(mActivity.isFinishing()).isFalse();

        Fragment fragment =
                mActivity.getSupportFragmentManager().findFragmentById(R.id.content_frame);
        assertThat(fragment).isInstanceOf(AppExclusionFragment.class);

        Bundle args = fragment.getArguments();
        assertThat(args).isNotNull();
        assertThat(args.getString(AppExclusionFragment.EXTRA_VPN_PACKAGE)).isEqualTo(VPN_PACKAGE);
        assertThat(args.getInt(AppExclusionFragment.EXTRA_USER_ID)).isEqualTo(USER_ID);
    }
}
