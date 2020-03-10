/*
 * Copyright (C) 2020 Paranoid Android
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
import android.content.res.Resources;
import android.provider.SearchIndexableResource;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class AdaptivePlaybackSettings extends DashboardFragment
        implements AdaptivePlaybackPreferenceController.OnChangeListener {

    private static final String TAG = "AdaptivePlaybackSettings";

    private static AdaptivePlaybackSwitchPreferenceController sSwitchController;
    private static final List<AdaptivePlaybackPreferenceController> sTimeoutControllers =
            new ArrayList<>();

    @Override
    public void onCheckedChanged(Preference preference) {
        for (AdaptivePlaybackPreferenceController controller : sTimeoutControllers) {
            controller.updateState(preference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext())) {
            if (controller instanceof AdaptivePlaybackPreferenceController) {
                ((AdaptivePlaybackPreferenceController) controller).setOnChangeListener(this);
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext())) {
            if (controller instanceof AdaptivePlaybackPreferenceController) {
                ((AdaptivePlaybackPreferenceController) controller).setOnChangeListener(null);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.adaptive_playback_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        if (sTimeoutControllers.size() == 0) {
            Resources resources = context.getResources();

            String[] timeoutKeys = resources.getStringArray(
                    R.array.adaptive_playback_timeout_keys);

            for (int i=0; i < timeoutKeys.length; i++) {
                sTimeoutControllers.add(new AdaptivePlaybackPreferenceController(
                        context, timeoutKeys[i]));
            }
        }
        List<AbstractPreferenceController> preferenceControllers =
                new ArrayList<>(sTimeoutControllers);
        if (sSwitchController == null) {
            sSwitchController = new AdaptivePlaybackSwitchPreferenceController(context);
        }
        preferenceControllers.add(sSwitchController);
        return preferenceControllers;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.adaptive_playback_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context);
                }
            };
}
