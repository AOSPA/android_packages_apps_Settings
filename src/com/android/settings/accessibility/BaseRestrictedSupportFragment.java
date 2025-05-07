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

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.android.settings.accessibility.actionbar.FeedbackMenuController;
import com.android.settings.dashboard.RestrictedDashboardFragment;

/**
 * Base fragment for dashboard within a limited-access context, containing support-related items.
 *
 * <p>Child classes <strong>must</strong> configure the mapping between {@link SettingsEnums} page
 * IDs and feedback bucket IDs from {@link AccessibilityFeedbackFeatureProvider}.
 */
public abstract class BaseRestrictedSupportFragment extends RestrictedDashboardFragment {

    public BaseRestrictedSupportFragment(@Nullable String restrictionKey) {
        super(restrictionKey);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int feedbackCategory = getFeedbackCategory();
        if (feedbackCategory != SettingsEnums.PAGE_UNKNOWN) {
            FeedbackMenuController.init(this, getFeedbackCategory());
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
}
