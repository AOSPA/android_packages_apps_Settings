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

import static android.Manifest.permission.MANAGE_ACCESSIBILITY;

import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SurveyFeatureProvider;

/**
 * Abstract base class for preference controllers that support survey flow and logic.
 */
public abstract class BaseSurveyPreferenceController extends BasePreferenceController {

    @Nullable
    Fragment mFragment;
    @Nullable
    SurveyFeatureProvider mSurveyFeatureProvider;
    @NonNull
    String mSurveyKey = "";
    int mPageId = SettingsEnums.PAGE_UNKNOWN;

    /**
     * Constructs a new BaseSurveyPreferenceController.
     *
     * @param context The application context.
     * @param preferenceKey The unique key for this preference.
     */
    public BaseSurveyPreferenceController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * Initializes the controller for survey support.
     *
     * @param fragment The Settings fragment.
     * @param surveyKey The key used to identify the survey.
     * @param pageId The page ID used for feedback tracking.
     */
    public void initializeForSurvey(@NonNull Fragment fragment, @NonNull String surveyKey,
            int pageId) {
        initializeForSurvey(fragment,
                FeatureFactory.getFeatureFactory().getSurveyFeatureProvider(mContext),
                surveyKey, pageId);
    }

    /**
     * Initializes the controller for survey support.
     *
     * @param fragment The Settings fragment.
     * @param surveyFeatureProvider Custom survey provider.
     * @param surveyKey The key used to identify the survey.
     * @param pageId The page ID used for feedback tracking.
     */
    @VisibleForTesting
    public void initializeForSurvey(@NonNull Fragment fragment,
            @Nullable SurveyFeatureProvider surveyFeatureProvider,
            @NonNull String surveyKey, int pageId) {
        mFragment = fragment;
        mSurveyFeatureProvider = surveyFeatureProvider;
        mSurveyKey = surveyKey;
        mPageId = pageId;
    }

    /**
     * Sends a survey broadcast with the specified action.
     *
     * @param action The action string for the broadcast intent.
     */
    public void sendSurveyBroadcast(String action) {
        final Intent intent = new Intent(action)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, mPageId);
        mContext.sendBroadcastAsUser(intent, mContext.getUser(), MANAGE_ACCESSIBILITY);
    }
}
