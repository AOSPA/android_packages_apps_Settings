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

import static com.android.internal.accessibility.common.NotificationConstants.ACTION_SURVEY_NOTIFICATION_DISMISSED;
import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID;

import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;
import com.android.settingslib.core.lifecycle.events.OnOptionsItemSelected;

import java.util.Optional;

/**
 * A controller that adds a survey menu to any Settings page.
 */
public class SurveyMenuController implements LifecycleObserver, OnCreateOptionsMenu,
        OnOptionsItemSelected {

    @NonNull
    private final InstrumentedPreferenceFragment mHost;
    @Nullable
    private final SurveyFeatureProvider mSurveyFeatureProvider;
    @NonNull
    private final String mFeedbackKey;
    private final int mPageId;
    @NonNull
    private Optional<Boolean> mIsSurveyConfirmedAvailable = Optional.empty();

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
        boolean available = mIsSurveyConfirmedAvailable.orElse(false);
        if (!available) {
            return;
        }

        final MenuItem item = menu.add(Menu.NONE, MenusUtils.MenuId.SEND_SURVEY.getValue(),
                Menu.NONE, R.string.accessibility_send_survey_title);
        item.setIcon(R.drawable.ic_rate_review);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == MenusUtils.MenuId.SEND_SURVEY.getValue()) {
            startSurvey();
            updateSurveyAvailability(false);

            // Remove Survey Notification
            FragmentActivity activity = mHost.getActivity();
            if (activity != null) {
                final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_DISMISSED)
                        .setPackage(activity.getPackageName())
                        .putExtra(EXTRA_PAGE_ID, mPageId);
                activity.sendBroadcastAsUser(intent, activity.getUser(),
                        android.Manifest.permission.MANAGE_ACCESSIBILITY);
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

    private void updateSurveyAvailability(boolean available) {
        final Optional<Boolean> newAvailabilityState = Optional.of(available);
        if (mIsSurveyConfirmedAvailable.equals(newAvailabilityState)) {
            return;
        }
        mIsSurveyConfirmedAvailable = newAvailabilityState;

        final FragmentActivity activity = mHost.getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private SurveyMenuController(@NonNull InstrumentedPreferenceFragment host,
            @Nullable SurveyFeatureProvider surveyFeatureProvider,
            @NonNull String feedbackKey, int pageId) {
        mHost = host;
        mFeedbackKey = feedbackKey;
        mPageId = pageId;
        mSurveyFeatureProvider = surveyFeatureProvider;
        if (mSurveyFeatureProvider != null) {
            mSurveyFeatureProvider.checkSurveyAvailable(mHost, mFeedbackKey,
                    this::updateSurveyAvailability);
        }
    }
}
