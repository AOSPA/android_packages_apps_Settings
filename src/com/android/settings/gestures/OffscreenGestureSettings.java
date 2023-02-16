/*
 * Copyright (C) 2021 Paranoid Android
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

package com.android.settings.gestures;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.ListPreference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.MainSwitchBarController;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@SearchIndexable
public class OffscreenGestureSettings extends SettingsPreferenceFragment implements
        OffscreenGestureEnabler.OnGestureSwitchChangeListener {

    private static final HashMap<String, Integer> sGesturesKeyCodes = OffscreenGestureConstants.sGesturesKeyCodes;
    private static final HashMap<String, Integer> sGesturesDefaults = OffscreenGestureConstants.sGesturesDefaults;
    private static final HashMap<String, String> sGesturesSettings = OffscreenGestureConstants.sGesturesSettings;

    private OffscreenGestureEnabler mGestureEnabler;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        final boolean enabled = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.GESTURES_ENABLED, 1, UserHandle.USER_CURRENT) != 0;
        for (String gestureKey : sGesturesKeyCodes.keySet()) {
            if (getResources().getInteger(sGesturesKeyCodes.get(gestureKey)) > 0) {
                ListPreference pref = findPreference(gestureKey);
                int gestureDefault = getResources().getInteger(
                        sGesturesDefaults.get(gestureKey));
                int gestureBehaviour = Settings.System.getInt(getContentResolver(),
                        sGesturesSettings.get(gestureKey), gestureDefault);

                pref.setValue(String.valueOf(gestureBehaviour));
                pref.setEnabled(enabled);
                pref.setOnPreferenceChangeListener(
                        (preference, o) -> Settings.System.putIntForUser(getContentResolver(),
                                sGesturesSettings.get(preference.getKey()),
                                Integer.parseInt((String) o), UserHandle.USER_CURRENT)
                );
            } else {
                removePreference(gestureKey);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGestureEnabler.teardownSwitchController();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.offscreen_gesture_settings;
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SettingsMainSwitchBar switchBar = activity.getSwitchBar();
        mGestureEnabler = new OffscreenGestureEnabler(getContext(),
                new MainSwitchBarController(switchBar), this, getSettingsLifecycle());
    }

    @Override
    public void onChanged(boolean enabled) {
        for (String gestureKey : sGesturesKeyCodes.keySet()) {
            if (getResources().getInteger(sGesturesKeyCodes.get(gestureKey)) > 0) {
                ListPreference gesturePref = findPreference(gestureKey);
                gesturePref.setEnabled(enabled);
            }
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.offscreen_gesture_settings;
            return Collections.singletonList(sir);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = new ArrayList<>();
            for (String gestureKey : sGesturesKeyCodes.keySet()) {
                if (context.getResources().getInteger(sGesturesKeyCodes.get(gestureKey)) == 0) {
                    keys.add(gestureKey);
                }
            }
            return keys;
        }
    };
}
