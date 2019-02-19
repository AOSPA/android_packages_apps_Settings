/*
 * Copyright (C) 2019 ParanoidAndroid Project
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

package com.android.settings.buttons;

import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.dashboard.DashboardFragment;

public class ButtonsSettings extends DashboardFragment {
    private static final String TAG = "SystemSettings";

    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String EMPTY_STRING = "";

    private Handler mHandler;
    private SwitchPreference mNavigationBar;



    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.buttons_settings;
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(NavigationBarPreferenceController.class).setConfig(getConfig(context));
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
    new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.buttons_settings;
            return Arrays.asList(sir);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> keys = super.getNonIndexableKeys(context);
            // Add whatever we won´t want to show.
            // keys.add(KEY_NAVIGATION_BAR);

            return keys;
        }
    };
}
