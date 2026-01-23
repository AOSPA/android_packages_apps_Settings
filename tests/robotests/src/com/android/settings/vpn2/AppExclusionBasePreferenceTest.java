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

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class AppExclusionBasePreferenceTest {
    private static final String PACKAGE_NAME = "com.example.app";
    private static final String APP_LABEL = "My App";
    private static final int USER_ID = 0;

    @Mock private AppExclusionActionCallback mActionCallback;
    private ShadowPackageManager mShadowPackageManager;

    private Context mContext;
    private PreferenceViewHolder mViewHolder;
    private ImageView mActionButton;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());

        installPackage(PACKAGE_NAME, APP_LABEL);

        // Inflate a view to create a ViewHolder
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.preference_material, null);
        final ViewGroup widgetFrame = view.findViewById(android.R.id.widget_frame);
        inflater.inflate(R.layout.vpn_app_exclusion_preference_widget, widgetFrame);

        mViewHolder = PreferenceViewHolder.createInstanceForTests(view);
        mActionButton = (ImageView) mViewHolder.findViewById(R.id.action);
    }

    private void installPackage(String packageName, String appName) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo.packageName = packageName;
        packageInfo.applicationInfo.name = appName;
        mShadowPackageManager.installPackage(packageInfo);
    }

    @Test
    public void constructor_withPackageInfo_setsTitleToAppLabel() {
        // Package installed in setUp().
        AppExclusionBasePreference pref =
                new AppExclusionCandidatePreference(
                        mContext, PACKAGE_NAME, USER_ID, mActionCallback);
        ShadowLooper.idleMainLooper();
        assertThat(pref.getTitle().toString()).isEqualTo(APP_LABEL);
    }

    @Test
    public void constructor_withoutPackageInfo_setsTitleToPackageName() {
        mShadowPackageManager.removePackage(PACKAGE_NAME);
        AppExclusionBasePreference pref =
                new AppExclusionCandidatePreference(
                        mContext, PACKAGE_NAME, USER_ID, mActionCallback);
        ShadowLooper.idleMainLooper();
        assertThat(pref.getTitle().toString()).isEqualTo(PACKAGE_NAME);
    }

    @Test
    public void onBindViewHolder_candidatePreference_setsAddAction() {
        AppExclusionBasePreference pref =
                new AppExclusionCandidatePreference(
                        mContext, PACKAGE_NAME, USER_ID, mActionCallback);
        pref.onBindViewHolder(mViewHolder);
        mViewHolder.itemView.performClick();
        verify(mActionCallback).onAppItemClick(PACKAGE_NAME, AppExclusionActionCallback.Action.ADD);
        assertThat(mActionButton.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void onBindViewHolder_selectedPreference_setsRemoveAction() {
        AppExclusionBasePreference pref =
                new AppExclusionSelectedPreference(
                        mContext, PACKAGE_NAME, USER_ID, mActionCallback);
        pref.onBindViewHolder(mViewHolder);
        mViewHolder.itemView.performClick();
        verify(mActionCallback)
                .onAppItemClick(PACKAGE_NAME, AppExclusionActionCallback.Action.REMOVE);
        assertThat(mActionButton.getVisibility()).isEqualTo(View.VISIBLE);
    }
}
