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

import static com.android.settings.accessibility.actionbar.SurveyMenuController.MENU_SEND_SURVEY;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import android.view.Menu;
import android.view.MenuItem;

import androidx.core.util.Consumer;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link SurveyMenuController} */
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
@RunWith(RobolectricTestRunner.class)
public class SurveyMenuControllerTest {
    private static final String SURVEY_TRIGGER_KEY = "surveyTriggerKey";

    @Rule
    public final MockitoRule mMocks = MockitoJUnit.rule();
    @Rule
    public ActivityScenarioRule<EmptyFragmentActivity> mActivityScenario =
            new ActivityScenarioRule<>(EmptyFragmentActivity.class);

    private FragmentActivity mActivity;
    private InstrumentedPreferenceFragment mHost;
    private SurveyFeatureProvider mSurveyFeatureProvider;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private Menu mMenu;
    @Mock
    private MenuItem mMenuItem;

    @Before
    public void setUp() {
        FakeFeatureFactory.setupForTest();
        mActivityScenario.getScenario().onActivity(activity -> mActivity = activity);
        mHost = spy(new InstrumentedPreferenceFragment() {
            @Override
            public int getMetricsCategory() {
                return 0;
            }
        });
        when(mHost.getActivity()).thenReturn(mActivity);
        mSurveyFeatureProvider =
                FakeFeatureFactory.getFeatureFactory().getSurveyFeatureProvider(mActivity);
    }

    @Test
    public void init_shouldAttachToLifecycle() {
        when(mHost.getSettingsLifecycle()).thenReturn(mLifecycle);

        SurveyMenuController.init(mHost, mActivity, SURVEY_TRIGGER_KEY);

        verify(mLifecycle).addObserver(any(SurveyMenuController.class));
    }

    @Test
    public void init_withSurveyFeatureProvider_shouldAttachToLifecycle() {
        when(mHost.getSettingsLifecycle()).thenReturn(mLifecycle);

        SurveyMenuController.init(mHost, mSurveyFeatureProvider, SURVEY_TRIGGER_KEY);

        verify(mLifecycle).addObserver(any(SurveyMenuController.class));
    }

    @Test
    public void onCreateOptionsMenu_surveyAvailable_shouldAddSurveyMenu() {
        setupSurveyAvailability(/* available= */ true);
        SurveyMenuController.init(mHost, mSurveyFeatureProvider, SURVEY_TRIGGER_KEY);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, /* inflater= */ null);

        verify(mMenu).add(anyInt(), eq(MENU_SEND_SURVEY), anyInt(), anyInt());
    }

    @Test
    public void onCreateOptionsMenu_surveyUnavailable_shouldNotAddSurveyMenu() {
        setupSurveyAvailability(/* available= */ false);
        SurveyMenuController.init(mHost, mSurveyFeatureProvider, SURVEY_TRIGGER_KEY);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, /* inflater= */ null);

        verify(mMenu, never()).add(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void onOptionsItemSelected_surveyMenuAdded_shouldStartSurvey() {
        when(mMenuItem.getItemId()).thenReturn(MENU_SEND_SURVEY);
        SurveyMenuController.init(mHost, mSurveyFeatureProvider, SURVEY_TRIGGER_KEY);

        mHost.getSettingsLifecycle().onOptionsItemSelected(mMenuItem);

        verify(mSurveyFeatureProvider).sendActivityIfAvailable(SURVEY_TRIGGER_KEY);
    }

    @Test
    public void onOptionsItemSelected_surveyMenuNotAdded_shouldNotStartSurvey() {
        SurveyMenuController.init(mHost, mSurveyFeatureProvider, SURVEY_TRIGGER_KEY);

        mHost.getSettingsLifecycle().onOptionsItemSelected(mMenuItem);

        verify(mSurveyFeatureProvider, never()).sendActivityIfAvailable(SURVEY_TRIGGER_KEY);
    }

    private void setupSurveyAvailability(boolean available) {
        doAnswer(invocation -> {
            Consumer<Boolean> consumer = invocation.getArgument(2);
            consumer.accept(available);
            return null;
        }).when(mSurveyFeatureProvider).checkSurveyAvailable(any(), anyString(), any());
    }
}
