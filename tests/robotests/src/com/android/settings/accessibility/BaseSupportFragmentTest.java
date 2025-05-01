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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.server.accessibility.Flags;
import com.android.settings.accessibility.actionbar.FeedbackMenuController;
import com.android.settings.accessibility.actionbar.SurveyMenuController;
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

/** Tests for {@link BaseSupportFragment} */
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
@RunWith(RobolectricTestRunner.class)
public class BaseSupportFragmentTest {

    @Rule
    public final MockitoRule mMocks = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public ActivityScenarioRule<EmptyFragmentActivity> mActivityScenario =
            new ActivityScenarioRule<>(EmptyFragmentActivity.class);

    private static final String PLACEHOLDER_SURVEY_KEY = "survey_key";

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private BaseSupportFragment mHost;

    @Mock
    private FragmentActivity mActivity;
    @Mock
    private Resources mResources;
    @Mock
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mHost = spy(new BaseSupportFragment() {
            @Override
            protected int getPreferenceScreenResId() {
                return 0;
            }

            @Override
            protected String getLogTag() {
                return "";
            }

            @Override
            public int getMetricsCategory() {
                return 0;
            }

            @Override
            public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
                // do nothing
            }
        });
        when(mHost.getContext()).thenReturn(mContext);
        when(mHost.getSettingsLifecycle()).thenReturn(mLifecycle);
        when(mHost.getActivity()).thenReturn(mActivity);
        when(mActivity.getResources()).thenReturn(mResources);
    }

    @Test
    public void handleFeedbackFlow_metricsCategoryUnknown_shouldNotAttachToLifecycle() {
        mHost.onCreate(/* savedInstanceState= */ null);

        verify(mLifecycle, never()).addObserver(any(FeedbackMenuController.class));
    }

    @Test
    public void handleFeedbackFlow_metricsCategoryKnown_shouldAttachToLifecycle() {
        when(mHost.getMetricsCategory()).thenReturn(SettingsEnums.ACCESSIBILITY);

        mHost.onCreate(/* savedInstanceState= */ null);

        verify(mLifecycle).addObserver(any(FeedbackMenuController.class));
    }

    @Test
    public void handleFeedbackFlow_feedbackCategoryUnknown_shouldNotAttachToLifecycle() {
        when(mHost.getFeedbackCategory()).thenReturn(SettingsEnums.PAGE_UNKNOWN);

        mHost.onCreate(/* savedInstanceState= */ null);

        verify(mLifecycle, never()).addObserver(any(FeedbackMenuController.class));
    }

    @Test
    public void initFeedbackMenuController_feedbackCategoryKnown_shouldAttachToLifecycle() {
        when(mHost.getFeedbackCategory()).thenReturn(SettingsEnums.ACCESSIBILITY);

        mHost.onCreate(/* savedInstanceState= */ null);

        verify(mLifecycle).addObserver(any(FeedbackMenuController.class));
    }


    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void handleSurveyFlow_surveyKeyEmpty_shouldNotAttachToLifecycle() {
        when(mHost.getSurveyKey()).thenReturn(/* value= */ "");

        mHost.onCreate(/* savedInstanceState= */ null);

        verify(mLifecycle, never()).addObserver(any(SurveyMenuController.class));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void handleSurveyFlow_surveyKeyNotEmpty_shouldAttachToLifecycle() {
        when(mHost.getSurveyKey()).thenReturn(PLACEHOLDER_SURVEY_KEY);

        mHost.onCreate(/* savedInstanceState= */ null);

        verify(mLifecycle).addObserver(any(SurveyMenuController.class));
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void handleSurveyFlow_disableHaTS_surveyKeyNotEmpty_shouldNotAttachToLifecycle() {
        when(mHost.getSurveyKey()).thenReturn(PLACEHOLDER_SURVEY_KEY);

        mHost.onCreate(/* savedInstanceState= */ null);

        verify(mLifecycle, never()).addObserver(any(SurveyMenuController.class));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void handleSurveyFlow_isStartSurveyIntentTrue_shouldStartSurvey() {
        final SurveyFeatureProvider surveyFeatureProvider =
                FakeFeatureFactory.setupForTest().getSurveyFeatureProvider(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SOURCE, SOURCE_START_SURVEY);
        when(mActivity.getIntent()).thenReturn(intent);
        when(mHost.getSurveyKey()).thenReturn(PLACEHOLDER_SURVEY_KEY);

        mHost.onCreate(/* savedInstanceState= */ null);

        verify(surveyFeatureProvider).sendActivityIfAvailable(PLACEHOLDER_SURVEY_KEY);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void handleSurveyFlow_disableHaTS_isStartSurveyIntentTrue_shouldNotStartSurvey() {
        final SurveyFeatureProvider surveyFeatureProvider =
                FakeFeatureFactory.setupForTest().getSurveyFeatureProvider(mContext);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SOURCE, SOURCE_START_SURVEY);
        when(mActivity.getIntent()).thenReturn(intent);
        when(mHost.getSurveyKey()).thenReturn(PLACEHOLDER_SURVEY_KEY);

        mHost.onCreate(/* savedInstanceState= */ null);

        verify(surveyFeatureProvider, never()).sendActivityIfAvailable(PLACEHOLDER_SURVEY_KEY);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void handleSurveyFlow_isStartSurveyIntentFalse_shouldNotStartSurvey() {
        final SurveyFeatureProvider surveyFeatureProvider =
                FakeFeatureFactory.setupForTest().getSurveyFeatureProvider(mContext);
        Intent intent = new Intent();
        when(mActivity.getIntent()).thenReturn(intent);
        when(mHost.getSurveyKey()).thenReturn(PLACEHOLDER_SURVEY_KEY);

        mHost.onCreate(/* savedInstanceState= */ null);

        verify(surveyFeatureProvider, never()).sendActivityIfAvailable(PLACEHOLDER_SURVEY_KEY);
    }
}
