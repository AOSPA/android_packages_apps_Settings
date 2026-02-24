/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.display;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.google.android.material.appbar.AppBarLayout;

/** Accessibility settings for color contrast. */
// LINT.IfChange
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ColorContrastFragment extends DashboardFragment {

    private static final String TAG = "ColorContrastFragment";
    private static final String KEY_APP_BAR_COLLAPSED = "app_bar_collapsed";

    private AppBarLayout mAppBarLayout;
    private boolean mAppBarCollapsed;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAppBarLayout = getActivity().findViewById(R.id.app_bar);
        if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_APP_BAR_COLLAPSED)) {
            if (mAppBarLayout != null) {
                mAppBarLayout.setExpanded(false, /* animate= */ false);
                mAppBarCollapsed = true;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAppBarLayout != null) {
            mAppBarLayout.addOnOffsetChangedListener(this::onOffsetChanged);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAppBarLayout != null) {
            mAppBarLayout.removeOnOffsetChangedListener(this::onOffsetChanged);
        }
    }

    private void onOffsetChanged(AppBarLayout appBar, int verticalOffset) {
        mAppBarCollapsed = Math.abs(verticalOffset) >= appBar.getTotalScrollRange();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_APP_BAR_COLLAPSED, mAppBarCollapsed);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_color_contrast;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_COLOR_CONTRAST;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_color_contrast);
}
// LINT.ThenChange(ColorContrastApiScreen.java)
