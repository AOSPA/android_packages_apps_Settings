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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.settings.accessibility.actionbar.FeedbackMenuController;
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

/** Tests for {@link BaseRestrictedSupportFragment} */
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
@RunWith(RobolectricTestRunner.class)
public class BaseRestrictedSupportFragmentTest {
    @Rule
    public final MockitoRule mMocks = MockitoJUnit.rule();
    @Rule
    public ActivityScenarioRule<EmptyFragmentActivity> mActivityScenario =
            new ActivityScenarioRule<>(EmptyFragmentActivity.class);

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private FragmentActivity mActivity;
    private BaseRestrictedSupportFragment mHost;
    @Mock
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mActivityScenario.getScenario().onActivity(activity -> mActivity = activity);
        mHost = spy(new BaseRestrictedSupportFragment(/* restrictionKey= */ null) {
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
        when(mHost.getActivity()).thenReturn(mActivity);
        when(mHost.getContext()).thenReturn(mContext);
        when(mHost.getSettingsLifecycle()).thenReturn(mLifecycle);
    }

    @Test
    public void initFeedbackMenuController_metricsCategoryUnknown_shouldNotAttachToLifecycle() {
        mHost.onCreate(/* savedInstanceState= */ null);

        verify(mLifecycle, never()).addObserver(any(FeedbackMenuController.class));
    }

    @Test
    public void initFeedbackMenuController_metricsCategoryKnown_shouldAttachToLifecycle() {
        when(mHost.getMetricsCategory()).thenReturn(SettingsEnums.ACCESSIBILITY);
        mHost.onCreate(/* savedInstanceState= */ null);

        verify(mLifecycle).addObserver(any(FeedbackMenuController.class));
    }

    @Test
    public void initFeedbackMenuController_feedbackCategoryUnknown_shouldNotAttachToLifecycle() {
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
}
