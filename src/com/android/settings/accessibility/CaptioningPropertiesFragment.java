/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.accessibility.captionpreferences.ui.CaptioningPropertiesScreen;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Settings fragment containing captioning properties. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
// LINT.IfChange
public class CaptioningPropertiesFragment extends DashboardFragment {

    private static final String TAG = "CaptioningPropertiesFragment";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_CAPTION_PROPERTIES;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return Flags.catalystCaptionPreferencesScreen() ? 0 : R.xml.captioning_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NotNull Context context) {
        return CaptioningPropertiesScreen.KEY;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_caption;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(
                    Flags.catalystCaptionPreferencesScreen() ? 0 : R.xml.captioning_settings);
}
// LINT.ThenChange(captionpreferences/ui/CaptioningPropertiesScreen.kt)
