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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;

import com.android.internal.net.VpnConfig;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.IntroPreference;

import java.util.List;
import java.util.concurrent.Future;

/** Settings screen for VPN app exclusion. */
public class AppExclusionFragment extends SettingsPreferenceFragment
        implements AppExclusionActionCallback {
    private static final String TAG = "AppExclusionFragment";

    private static final String PREF_KEY_VPN_APP = "vpn_app";
    private static final String PREF_KEY_EXCLUDED_APPS = "selected_apps";
    private static final String PREF_KEY_CANDIDATE_APPS = "candidate_apps";
    private static final String PREF_KEY_NO_SELECTED_APPS_HINT = "no_selected_apps_hint";

    public static final String EXTRA_VPN_PACKAGE = "vpn_package";
    public static final String EXTRA_USER_ID = "user_id";

    @VisibleForTesting AppExclusionViewModel mViewModel;
    @VisibleForTesting AppExclusionViewModelFactory mViewModelFactory;
    private int mUserId;
    private String mVpnPackage;

    private IntroPreference mVpnApp;
    private PreferenceCategory mSelectedAppsCategory;
    private PreferenceCategory mCandidateAppsCategory;

    @SuppressWarnings("NullAway")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Bundle arguments = getArguments();
        if (arguments == null) {
            Log.e(TAG, "Missing arguments, closing fragment");
            getActivity().finish();
            return;
        }
        if (!arguments.containsKey(EXTRA_VPN_PACKAGE)) {
            Log.e(TAG, "Missing vpn package name, closing fragment");
            getActivity().finish();
            return;
        }
        if (!arguments.containsKey(EXTRA_USER_ID)) {
            Log.e(TAG, "Missing user id, closing fragment");
            getActivity().finish();
            return;
        }
        mVpnPackage = arguments.getString(EXTRA_VPN_PACKAGE);
        mUserId = arguments.getInt(EXTRA_USER_ID);

        if (mViewModelFactory == null) {
            mViewModelFactory =
                    new AppExclusionViewModelFactory(
                            requireActivity().getApplication(), mUserId, mVpnPackage);
        }
        mViewModel =
                new ViewModelProvider(this, mViewModelFactory).get(AppExclusionViewModel.class);

        // Set ComparisonCallback so we get better animation when list changes.
        getPreferenceManager()
                .setPreferenceComparisonCallback(
                        new PreferenceManager.SimplePreferenceComparisonCallback());

        mSelectedAppsCategory = findPreference(PREF_KEY_EXCLUDED_APPS);
        mCandidateAppsCategory = findPreference(PREF_KEY_CANDIDATE_APPS);
        mVpnApp = findPreference(PREF_KEY_VPN_APP);
        setVpnAppInfo();
        observeDataFromViewModel();
        mViewModel.fillAppExclusionLists();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.VPN_APP_EXCLUSION;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.vpn_app_exclusion_settings;
    }

    @Override
    public void onAppItemClick(String packageName, Action action) {
        if (action.equals(Action.ADD)) {
            mViewModel.addAppToExclusionList(packageName);
        } else if (action.equals(Action.REMOVE)) {
            mViewModel.removeAppFromExclusionList(packageName);
        }
    }

    private void onSetAppExclusionEnabledFinished(boolean result) {
        if (!result) {
            Log.d(TAG, "onSetAppExclusionEnabledFinished: result is false.");
            return;
        }
        mViewModel.fillAppExclusionLists();
    }

    private void observeDataFromViewModel() {
        mViewModel.getAppExclusionCandidateLiveData().observe(this, this::updateAppCandidateList);
        mViewModel.getAppExclusionEnabledLiveData().observe(this, this::updateAppSelectedList);
        mViewModel
                .getSetAppExclusionEnabledLiveData()
                .observe(this, this::onSetAppExclusionEnabledFinished);
    }

    @VisibleForTesting
    void updateAppSelectedList(List<String> list) {
        if (list == null) {
            Log.w(TAG, "updateAppSelectedList: list is null, do nothing.");
            return;
        }

        mSelectedAppsCategory.removeAll();
        for (String packageName : list) {
            mSelectedAppsCategory.addPreference(
                    new AppExclusionSelectedPreference(
                            getPrefContext(), packageName, mUserId, this));
        }
        if (list.isEmpty()) {
            Preference noSelected = new Preference(getPrefContext());
            noSelected.setKey(PREF_KEY_NO_SELECTED_APPS_HINT);
            noSelected.setSummary(getContext().getString(R.string.zen_mode_bypassing_apps_none));
            noSelected.setSelectable(false);
            mSelectedAppsCategory.addPreference(noSelected);
        }
    }

    @VisibleForTesting
    void updateAppCandidateList(List<String> list) {
        if (list == null) {
            Log.w(TAG, "updateAppCandidateList: list is null, do nothing.");
            return;
        }

        mCandidateAppsCategory.removeAll();
        for (String packageName : list) {
            mCandidateAppsCategory.addPreference(
                    new AppExclusionCandidatePreference(
                            getPrefContext(), packageName, mUserId, this));
        }
    }

    private void setVpnAppInfo() {
        String label = mVpnPackage;
        final PackageInfo pkgInfo =
                AppExclusionUtils.getPackageInfo(getContext(), mUserId, mVpnPackage);
        if (pkgInfo != null) {
            try {
                label = VpnConfig.getVpnLabel(getUserContext(), mVpnPackage).toString();
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Cannot find VPN label for " + mVpnPackage, e);
            }
            Future<?> unused =
                    ThreadUtils.postOnBackgroundThread(
                            () -> {
                                // Utils.getBadgedIcon() returns the default icon if the app
                                // doesn't have one.
                                final Drawable icon =
                                        Utils.getBadgedIcon(getContext(), pkgInfo.applicationInfo);
                                getContext()
                                        .getMainThreadHandler()
                                        .post(() -> mVpnApp.setAppIcon(icon));
                            });
        }
        mVpnApp.setTitle(label);
    }

    private Context getUserContext() {
        UserHandle user = UserHandle.of(mUserId);
        return getContext().createContextAsUser(user, /* flags= */ 0);
    }
}
