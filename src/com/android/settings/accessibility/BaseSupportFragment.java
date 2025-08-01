/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_SOURCE;
import static com.android.internal.accessibility.common.NotificationConstants.SOURCE_START_SURVEY;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.accessibility.actionbar.SurveyMenuController;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Base fragment for dashboard style UI containing support-related items.
 */
public abstract class BaseSupportFragment extends DashboardFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (com.android.server.accessibility.Flags.enableLowVisionHats()) {
            handleSurveyFlow();
        }
    }

    @NonNull
    @Override
    public RecyclerView onCreateRecyclerView(@NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView =
                super.onCreateRecyclerView(inflater, parent, savedInstanceState);
        return AccessibilityFragmentUtils.addCollectionInfoToAccessibilityDelegate(recyclerView);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (menu.size() == 1) {
            final MenuItem singleItem = menu.getItem(0);
            if (singleItem != null) {
                singleItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        } else if (menu.size() > 1) {
            for (int i = 0; i < menu.size(); i++) {
                final MenuItem item = menu.getItem(i);
                if (item != null) {
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }
            }
        }
    }

    @NonNull
    protected String getSurveyKey() {
        return "";
    }

    private void handleSurveyFlow() {
        final Context context = getActivity();
        if (context == null) {
            return;
        }

        final String surveyKey = getSurveyKey();
        if (TextUtils.isEmpty(surveyKey)) {
            return;
        }

        final SurveyManager surveyManager = new SurveyManager(this, context,
                surveyKey, getMetricsCategory());

        // Handle direct survey triggers; no need to initialize survey menu.
        final Intent intent = getIntent();
        if (intent != null
                && intent.getStringExtra(EXTRA_SOURCE) != null
                && TextUtils.equals(intent.getStringExtra(EXTRA_SOURCE), SOURCE_START_SURVEY)) {
            surveyManager.startSurvey();
            return;
        }

        SurveyMenuController.init(this, surveyManager);
    }
}
