/*
 * Copyright (C) 2020 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.utils.CandidateInfoExtra;
import com.android.settings.widget.RadioButtonPickerFragmentWithExtra;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.FooterPreferenceMixinCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@SearchIndexable
public class RefreshRatePreferenceFragment extends RadioButtonPickerFragmentWithExtra {

    private static final String TAG = "RefreshRatePreferenceFragment";

    @VisibleForTesting
    static final String KEY_REFRESH_RATE_ADAPTIVE = "refresh_rate_adaptive";
    @VisibleForTesting
    static final String KEY_REFRESH_RATE_NORMAL = "refresh_rate_normal";
    @VisibleForTesting
    static final String KEY_REFRESH_RATE_HIGH = "refresh_rate_high";
    @VisibleForTesting
    static final String KEY_REFRESH_RATE_SUPER = "refresh_rate_super";

    private static final int REFRESH_RATE_ADAPTIVE = 0;
    private static final int REFRESH_RATE_NORMAL = 1;
    private static final int REFRESH_RATE_HIGH = 2;
    private static final int REFRESH_RATE_SUPER = 3;

    protected final FooterPreferenceMixinCompat mFooterPreferenceMixin =
            new FooterPreferenceMixinCompat(this, getSettingsLifecycle());

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        final FooterPreference footerPreference =
                mFooterPreferenceMixin.createFooterPreference();
        footerPreference.setIcon(R.drawable.ic_privacy_shield_24dp);
        footerPreference.setTitle(R.string.refresh_rate_description);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.refresh_rate_settings;
    }

    @Override
    protected List<? extends CandidateInfoExtra> getCandidates() {
        final Context c = getContext();
        final int[] availableRefreshRates = c.getResources().getIntArray(R.array.config_availableRefreshRates);

        List<CandidateInfoExtra> candidates = new ArrayList<>();
        if (availableRefreshRates != null) {
            for (int refreshRate : availableRefreshRates) {
                if (refreshRate == REFRESH_RATE_ADAPTIVE) {
                    candidates.add(new CandidateInfoExtra(
                                c.getText(R.string.refresh_rate_adaptive_title),
                                c.getText(R.string.refresh_rate_adaptive_summary),
                                KEY_REFRESH_RATE_ADAPTIVE, true /* enabled */));
                } else if (refreshRate == REFRESH_RATE_NORMAL) {
                    candidates.add(new CandidateInfoExtra(
                                c.getText(R.string.refresh_rate_normal_title),
                                c.getText(R.string.refresh_rate_normal_summary),
                                KEY_REFRESH_RATE_NORMAL, true /* enabled */));
                } else if (refreshRate == REFRESH_RATE_HIGH) {
                    candidates.add(new CandidateInfoExtra(
                                c.getText(R.string.refresh_rate_high_title),
                                c.getText(R.string.refresh_rate_high_summary),
                                KEY_REFRESH_RATE_HIGH, true /* enabled */));
                } else if (refreshRate == REFRESH_RATE_SUPER) {
                    candidates.add(new CandidateInfoExtra(
                                c.getText(R.string.refresh_rate_super_title),
                                c.getText(R.string.refresh_rate_super_summary),
                                KEY_REFRESH_RATE_SUPER, true /* enabled */));
                }
            }
        }
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        final int refreshRate = getRefreshRate();
        if (refreshRate == REFRESH_RATE_ADAPTIVE) {
            return KEY_REFRESH_RATE_ADAPTIVE;
        } else if (refreshRate == REFRESH_RATE_HIGH) {
            return KEY_REFRESH_RATE_HIGH;
        } else if (refreshRate == REFRESH_RATE_SUPER) {
            return KEY_REFRESH_RATE_SUPER;
        }
        return KEY_REFRESH_RATE_NORMAL;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        switch (key) {
            case KEY_REFRESH_RATE_ADAPTIVE:
                setRefreshRate(90, 60);
                break;
            case KEY_REFRESH_RATE_NORMAL:
                setRefreshRate(60, 60);
                break;
            case KEY_REFRESH_RATE_HIGH:
                setRefreshRate(90, 90);
                break;
            case KEY_REFRESH_RATE_SUPER:
                setRefreshRate(120, 120);
                break;
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    private int getRefreshRate() {
        final int defaultPeakValue = getContext().getResources().getInteger(com.android.internal.R.integer.config_defaultPeakRefreshRate);
        int peakRefreshRate = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, defaultPeakValue);
        int minRefreshRate = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, 0);
        if (peakRefreshRate == 90 && minRefreshRate <= 60) {
            return REFRESH_RATE_ADAPTIVE;
        } else if (peakRefreshRate == 60 && minRefreshRate == 60) {
            return REFRESH_RATE_NORMAL;
        } else if (peakRefreshRate == 90 && minRefreshRate == 90) {
            return REFRESH_RATE_HIGH;
        } else if (peakRefreshRate == 120 && minRefreshRate == 120) {
            return REFRESH_RATE_SUPER;
        }
        return REFRESH_RATE_NORMAL;
    }

    private void setRefreshRate(int peakValue, int minValue) {
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, peakValue);
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, minValue);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.refresh_rate_settings;
                    return Arrays.asList(sir);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    final int[] availableRefreshRates = context.getResources().getIntArray(
                            R.array.config_availableRefreshRates);
                    return availableRefreshRates != null && availableRefreshRates.length >= 2;
                }
            };
}
