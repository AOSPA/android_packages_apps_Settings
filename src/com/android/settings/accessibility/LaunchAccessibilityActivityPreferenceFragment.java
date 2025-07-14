/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityShortcutInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider;
import com.android.settings.accessibility.detail.a11yactivity.AccessibilityActivityFooterPreferenceController;
import com.android.settings.accessibility.detail.a11yactivity.AccessibilityActivityHtmlFooterPreferenceController;
import com.android.settings.accessibility.detail.a11yactivity.AccessibilityActivityIllustrationPreferenceController;
import com.android.settings.accessibility.detail.a11yactivity.LaunchAccessibilityActivityPreferenceController;
import com.android.settings.accessibility.detail.a11yactivity.SettingsPreferenceController;
import com.android.settings.accessibility.detail.a11yactivity.ShortcutPreferenceController;
import com.android.settings.accessibility.detail.a11yactivity.TopIntroPreferenceController;
import com.android.settings.accessibility.shared.LaunchAppInfoPreferenceController;
import com.android.settings.overlay.FeatureFactory;

/** Fragment for providing open activity button. */
public class LaunchAccessibilityActivityPreferenceFragment extends BaseSupportFragment {

    private static final String TAG = "LaunchAccessibilityActivityPreferenceFragment";
    @Nullable
    private AccessibilityShortcutInfo mAccessibilityShortcutInfo;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ComponentName componentName = getFeatureComponentName();
        mAccessibilityShortcutInfo = AccessibilityRepositoryProvider.get(
                context).getAccessibilityShortcutInfo(componentName);
        if (mAccessibilityShortcutInfo != null) {
            initializePreferenceControllers(mAccessibilityShortcutInfo);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(getFeatureName());
    }

    private void initializePreferenceControllers(
            @NonNull AccessibilityShortcutInfo accessibilityShortcutInfo) {
        use(TopIntroPreferenceController.class).initialize(accessibilityShortcutInfo);
        use(AccessibilityActivityIllustrationPreferenceController.class).initialize(
                accessibilityShortcutInfo);
        use(LaunchAccessibilityActivityPreferenceController.class).initialize(
                accessibilityShortcutInfo);

        ShortcutPreferenceController shortcutPreferenceController =
                use(ShortcutPreferenceController.class);
        if (shortcutPreferenceController != null) {
            shortcutPreferenceController.initialize(
                    accessibilityShortcutInfo,
                    getChildFragmentManager(),
                    getFeatureName(),
                    getMetricsCategory()
            );
        }
        use(SettingsPreferenceController.class).initialize(accessibilityShortcutInfo);
        use(LaunchAppInfoPreferenceController.class).initialize(
                accessibilityShortcutInfo.getComponentName());
        use(AccessibilityActivityHtmlFooterPreferenceController.class).initialize(
                accessibilityShortcutInfo);
        use(AccessibilityActivityFooterPreferenceController.class).initialize(
                accessibilityShortcutInfo);
    }

    @Override
    public int getMetricsCategory() {
        return FeatureFactory.getFeatureFactory()
                .getAccessibilityPageIdFeatureProvider().getCategory(getFeatureComponentName());
    }

    @Override
    public int getFeedbackCategory() {
        return getMetricsCategory();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_activity_detail_screen;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @NonNull
    private CharSequence getFeatureName() {
        if (mAccessibilityShortcutInfo == null
                || mAccessibilityShortcutInfo.getActivityInfo() == null) {
            return "";
        }

        return mAccessibilityShortcutInfo.getActivityInfo().loadLabel(getPackageManager());
    }

    @NonNull
    private ComponentName getFeatureComponentName() {
        return getArguments().getParcelable(
                AccessibilitySettings.EXTRA_COMPONENT_NAME, ComponentName.class);
    }
}