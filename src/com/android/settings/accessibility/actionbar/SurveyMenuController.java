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

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.accessibility.SurveyManager;
import com.android.settings.core.InstrumentedPreferenceFragment;
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
    @NonNull
    private final SurveyManager mSurveyManager;
    @NonNull
    private Optional<Boolean> mIsSurveyConfirmedAvailable = Optional.empty();

    /**
     * Initializes the controller to add the survey menu to the given Settings fragment.
     * Uses the default survey provider.
     *
     * @param host The Settings fragment to add the menu to.
     * @param surveyManager The {@link SurveyManager} instance responsible for handling surveys.
     * @return A new instance of SurveyMenuController that manages the survey menu.
     */
    @NonNull
    public static SurveyMenuController init(@NonNull InstrumentedPreferenceFragment host,
            @NonNull SurveyManager surveyManager) {
        final SurveyMenuController controller = new SurveyMenuController(host, surveyManager);
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
            mSurveyManager.startSurvey();
            mSurveyManager.cancelSurveyNotification();
            updateSurveyAvailability(false);
            return true;
        }
        return false;
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
            @NonNull SurveyManager surveyManager) {
        mHost = host;
        mSurveyManager = surveyManager;
        surveyManager.checkSurveyAvailable(this::updateSurveyAvailability);
    }
}
