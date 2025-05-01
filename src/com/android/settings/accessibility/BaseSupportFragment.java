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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.server.accessibility.Flags;
import com.android.settings.accessibility.actionbar.FeedbackMenuController;
import com.android.settings.accessibility.actionbar.SurveyMenuController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SurveyFeatureProvider;

/**
 * Base fragment for dashboard style UI containing support-related items.
 *
 * <p>Child classes <strong>must</strong> configure the mapping between {@link SettingsEnums} page
 * IDs and feedback bucket IDs from {@link AccessibilityFeedbackFeatureProvider}.
 */
public abstract class BaseSupportFragment extends DashboardFragment {

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleFeedbackFlow();

        if (Flags.enableLowVisionHats()) {
            handleSurveyFlow();
        }
    }

    /**
     * Returns the category of the feedback page.
     *
     * <p>This method defaults to returning the same value as {@link #getMetricsCategory()},
     * which is typically a value from {@link SettingsEnums}.
     *
     * <p>The default behavior uses {@link #getMetricsCategory()} to determine the feedback
     * category. This minimizes code duplication by reusing the existing metrics category.
     * In most cases, the feedback page is considered a sub-category or a closely related
     * aspect of the primary metrics category.
     *
     * <p>If a specific feedback category needs to be defined independently, indicating a
     * requirement for distinct categorization, this method should be overridden in the
     * subclass.
     *
     * <p>By default, this method aims to balance code efficiency and potential future
     * flexibility. If your use case deviates from the common scenario where metrics and
     * feedback categories align, please override this method accordingly.
     *
     * <p>For example, if you wish to disable the feedback menu for a particular page, you can
     * override this method to return {@link SettingsEnums#PAGE_UNKNOWN}.
     *
     * @return The feedback category, which defaults to returned by {@link #getMetricsCategory()}.
     */
    protected int getFeedbackCategory() {
        return getMetricsCategory();
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

        // Handle direct survey triggers; no need to initialize survey menu.
        final Intent intent = getIntent();
        if (intent != null
                && intent.getStringExtra(EXTRA_SOURCE) != null
                && TextUtils.equals(intent.getStringExtra(EXTRA_SOURCE), SOURCE_START_SURVEY)) {
            final SurveyFeatureProvider surveyFeatureProvider =
                    FeatureFactory.getFeatureFactory().getSurveyFeatureProvider(context);
            if (surveyFeatureProvider != null) {
                surveyFeatureProvider.sendActivityIfAvailable(surveyKey);
            }
            return;
        }

        SurveyMenuController.init(this, context, surveyKey, getFeedbackCategory());
    }

    private void handleFeedbackFlow() {
        int feedbackCategory = getFeedbackCategory();
        if (feedbackCategory == SettingsEnums.PAGE_UNKNOWN) {
            return;
        }

        FeedbackMenuController.init(this, feedbackCategory);
    }
}
