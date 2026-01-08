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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.widget.IntroPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppExclusionFragmentTest {
    private static final String VPN_PACKAGE = "com.test.vpn";
    private static final String VPN_LABEL = "Test VPN";
    private static final int USER_ID = 0;
    private static final String APP_ONE = "com.example.app1";
    private static final String APP_TWO = "com.example.app2";
    private static final String APP_ONE_LABEL = "App One";
    private static final String APP_TWO_LABEL = "App Two";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private AppExclusionViewModel mViewModel;
    private AppExclusionViewModelFactory mViewModelFactory;
    private ShadowPackageManager mShadowPackageManager;

    private MutableLiveData<List<String>> mCandidateLiveData;
    private MutableLiveData<List<String>> mEnabledLiveData;
    private MutableLiveData<Boolean> mSetEnabledLiveData;

    private Context mContext;
    private AppExclusionFragment mFragment;
    private FragmentActivity mActivity;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());

        mCandidateLiveData = new MutableLiveData<>();
        mEnabledLiveData = new MutableLiveData<>();
        mSetEnabledLiveData = new MutableLiveData<>();
        when(mViewModel.getAppExclusionCandidateLiveData()).thenReturn(mCandidateLiveData);
        when(mViewModel.getAppExclusionEnabledLiveData()).thenReturn(mEnabledLiveData);
        when(mViewModel.getSetAppExclusionEnabledLiveData()).thenReturn(mSetEnabledLiveData);
        mViewModelFactory =
                spy(
                        new AppExclusionViewModelFactory(
                                ApplicationProvider.getApplicationContext(), USER_ID, VPN_PACKAGE));
        doReturn(mViewModel).when(mViewModelFactory).create(any());

        mActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        mFragment = new AppExclusionFragment();
        mFragment.mViewModelFactory = mViewModelFactory;

        // Install fake packages for the test using ShadowPackageManager
        installPackage(VPN_PACKAGE, VPN_LABEL);
        installPackage(APP_ONE, APP_ONE_LABEL);
        installPackage(APP_TWO, APP_TWO_LABEL);

        Bundle args = new Bundle();
        args.putString(AppExclusionFragment.EXTRA_VPN_PACKAGE, VPN_PACKAGE);
        args.putInt(AppExclusionFragment.EXTRA_USER_ID, USER_ID);
        mFragment.setArguments(args);

        mActivity
                .getSupportFragmentManager()
                .beginTransaction()
                .add(mFragment, "AppExclusionSettings")
                .commit();
        ShadowLooper.idleMainLooper();
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
    public void onAttach_initializesViewModel() {
        verify(mViewModel).fillAppExclusionLists();
    }

    @Test
    public void updateAppSelectedList_withItems_showsItemsAndHidesHint() {
        mEnabledLiveData.postValue(Arrays.asList(APP_ONE, APP_TWO));
        ShadowLooper.idleMainLooper();

        PreferenceCategory selectedApps = mFragment.findPreference("selected_apps");
        Preference noSelectedHint = mFragment.findPreference("no_selected_apps_hint");

        assertThat(selectedApps.getPreferenceCount()).isEqualTo(2);
        assertThat(selectedApps.getPreference(0).getKey()).isEqualTo(APP_ONE);
        assertThat(selectedApps.getPreference(1).getKey()).isEqualTo(APP_TWO);
        assertThat(noSelectedHint).isNull();
    }

    @Test
    public void updateAppSelectedList_empty_hidesItemsAndShowsHint() {
        mEnabledLiveData.postValue(Collections.emptyList());
        ShadowLooper.idleMainLooper();

        PreferenceCategory selectedApps = mFragment.findPreference("selected_apps");
        Preference noSelectedHint = mFragment.findPreference("no_selected_apps_hint");

        assertThat(selectedApps.getPreferenceCount()).isEqualTo(1);
        assertThat(noSelectedHint.isVisible()).isTrue();
    }

    @Test
    public void updateAppCandidateList_withItems_showsItems() {
        mCandidateLiveData.postValue(Arrays.asList(APP_ONE, APP_TWO));
        ShadowLooper.idleMainLooper();

        PreferenceCategory candidateApps = mFragment.findPreference("candidate_apps");
        assertThat(candidateApps.getPreferenceCount()).isEqualTo(2);
        assertThat(candidateApps.getPreference(0).getKey()).isEqualTo(APP_ONE);
        assertThat(candidateApps.getPreference(1).getKey()).isEqualTo(APP_TWO);
    }

    @Test
    public void onAppItemClick_add_callsViewModel() {
        mFragment.onAppItemClick(APP_ONE, AppExclusionActionCallback.Action.ADD);
        verify(mViewModel).addAppToExclusionList(APP_ONE);
    }

    @Test
    public void onAppItemClick_remove_callsViewModel() {
        mFragment.onAppItemClick(APP_ONE, AppExclusionActionCallback.Action.REMOVE);
        verify(mViewModel).removeAppFromExclusionList(APP_ONE);
    }

    @Test
    public void setVpnAppInfo_setsTitleAndIcon() {
        IntroPreference vpnAppPref = mFragment.findPreference("vpn_app");
        assertThat(vpnAppPref.getTitle().toString()).isEqualTo(VPN_LABEL);
        assertThat(vpnAppPref.getIcon()).isNotNull();
    }
}
