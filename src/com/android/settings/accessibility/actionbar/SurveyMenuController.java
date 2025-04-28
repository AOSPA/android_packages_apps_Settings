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

package com.android.settings.accessibility.actionbar;

import static com.android.settings.accessibility.notification.NotificationConstants.EXTRA_DISMISS_NOTIFICATION;
import static com.android.settings.accessibility.notification.NotificationConstants.EXTRA_PAGE_ID;

import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.accessibility.notification.SurveyNotificationService;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;
import com.android.settingslib.core.lifecycle.events.OnOptionsItemSelected;

/**
 * A controller that adds a survey menu to any Settings page.
 */
public class SurveyMenuController implements LifecycleObserver, OnCreateOptionsMenu,
        OnOptionsItemSelected {

    /**
     * The menu item ID for the send survey menu option.
     */
    public static final int MENU_SEND_SURVEY = Menu.FIRST + 20;

    @NonNull
    private final InstrumentedPreferenceFragment mHost;
    @Nullable
    private final SurveyFeatureProvider mSurveyFeatureProvider;
    @NonNull
    private final String mFeedbackKey;
    private final int mPageId;

    /**
     * Initializes the controller to add the survey menu to the given Settings fragment.
     * Uses the default survey provider.
     *
     * @param host The Settings fragment to add the menu to.
     * @param context The current context.
     * @param feedbackKey Unique identifier for the survey.
     * @return A new instance of SurveyMenuController that manages the survey menu.
     */
    @NonNull
    public static SurveyMenuController init(@NonNull InstrumentedPreferenceFragment host,
            @NonNull Context context, @NonNull String feedbackKey, int pageId) {
        return init(host,
                FeatureFactory.getFeatureFactory().getSurveyFeatureProvider(context),
                feedbackKey, pageId);
    }

    /**
     * Initializes the controller with a custom survey provider.
     *
     * @param host The Settings fragment.
     * @param surveyFeatureProvider Custom survey provider.
     * @param feedbackKey Survey identifier.
     * @return A new instance of SurveyMenuController that manages the survey menu.
     */
    @NonNull
    public static SurveyMenuController init(@NonNull InstrumentedPreferenceFragment host,
            @Nullable SurveyFeatureProvider surveyFeatureProvider, @NonNull String feedbackKey,
            int pageId) {
        final SurveyMenuController controller =
                new SurveyMenuController(host, surveyFeatureProvider, feedbackKey, pageId);
        host.getSettingsLifecycle().addObserver(controller);
        return controller;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (mSurveyFeatureProvider != null) {
            mSurveyFeatureProvider.checkSurveyAvailable(mHost, mFeedbackKey, available -> {
                if (available) {
                    menu.add(Menu.NONE, MENU_SEND_SURVEY, Menu.NONE,
                            R.string.accessibility_send_survey_title);
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == MENU_SEND_SURVEY) {
            startSurvey();
            // Prevent repeated feedback triggers.
            menuItem.setVisible(false);

            // Remove Survey Notification
            FragmentActivity activity = mHost.getActivity();
            if (activity != null) {
                final Intent dismissServiceIntent = new Intent(activity,
                        SurveyNotificationService.class);
                dismissServiceIntent.putExtra(EXTRA_DISMISS_NOTIFICATION, true);
                dismissServiceIntent.putExtra(EXTRA_PAGE_ID, mPageId);
                activity.startService(dismissServiceIntent);
            }
            return true;
        }
        return false;
    }

    /**
     * Triggers the survey sending process using the provided SurveyFeatureProvider.
     *
     * <p>If a SurveyFeatureProvider is available, it initiates the survey activity using the
     * feedback key. If the provider is null, this method does nothing.
     */
    public void startSurvey() {
        if (mSurveyFeatureProvider != null) {
            mSurveyFeatureProvider.sendActivityIfAvailable(mFeedbackKey);
        }
    }

    private SurveyMenuController(@NonNull InstrumentedPreferenceFragment host,
            @Nullable SurveyFeatureProvider surveyFeatureProvider,
            @NonNull String feedbackKey, int pageId) {
        mHost = host;
        mFeedbackKey = feedbackKey;
        mSurveyFeatureProvider = surveyFeatureProvider;
        mPageId = pageId;
    }
}
