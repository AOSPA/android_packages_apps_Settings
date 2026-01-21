/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.system;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.utils.DesktopSettingsUtils;
import com.android.settingslib.search.SearchIndexable;

import java.util.Arrays;
import java.util.List;

// LINT.IfChange
@SearchIndexable
public class SystemDashboardFragment extends DashboardFragment {

    private static final String KEY_ADVANCED = "advanced_category";

    private static final String TAG = "SystemDashboardFrag";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final PreferenceScreen screen = getPreferenceScreen();
        // We do not want to display an advanced button if only one setting is hidden
        if (getVisiblePreferenceCount(screen) == screen.getInitialExpandedChildrenCount() + 1) {
            screen.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            return;
        }

        // Move any remaining preferences that are not in a category to the Advanced category.
        // Temporary fallback until all external dependencies are propagated to AOSP
        final PreferenceCategory advancedCategory = findPreference(KEY_ADVANCED);
        if (advancedCategory != null) {
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                final Preference preference = screen.getPreference(i);
                if (!(preference instanceof PreferenceCategory)) {
                    // This is a stray preference, move it to the advanced category
                    movePreference(preference, advancedCategory);
                }
            }
        }
    }

    private void movePreference(Preference preference, PreferenceCategory targetCategory) {
        if (preference == null || targetCategory == null) {
            return; // Don't move if the preference or category doesn't exist
        }

        // Remove from the root screen and add to the target category
        getPreferenceScreen().removePreference(preference);
        targetCategory.addPreference(preference);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_SYSTEM_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return getResId(getContext());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_system_dashboard;
    }

    private int getVisiblePreferenceCount(PreferenceGroup group) {
        int visibleCount = 0;
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                visibleCount += getVisiblePreferenceCount((PreferenceGroup) preference);
            } else if (preference.isVisible()) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return SystemDashboardScreen.KEY;
    }

    private static int getResId(Context context) {
        if (DesktopSettingsUtils.shouldShowTopLevelDeviceCategory(context)) {
            return R.xml.system_dashboard_fragment_desktop;
        }

        return R.xml.system_dashboard_fragment;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = getResId(context);
                    return Arrays.asList(sir);
                }
            };
}
// LINT.ThenChange(SystemDashboardScreen.kt)
