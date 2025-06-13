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

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;
import static com.android.internal.accessibility.common.NotificationConstants.ACTION_SURVEY_NOTIFICATION_DISMISSED;
import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.util.Consumer;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
    private static final String TEST_SURVEY_TRIGGER_KEY = "surveyTriggerKey";
    private static final int TEST_PAGE_ID = 10;

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
        when(mMenu.add(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mMenuItem);
        when(mMenuItem.getItemId()).thenReturn(MenusUtils.MenuId.SEND_SURVEY.getValue());
        mSurveyFeatureProvider =
                FakeFeatureFactory.getFeatureFactory().getSurveyFeatureProvider(mActivity);
    }

    @Test
    public void init_shouldAttachToLifecycle() {
        when(mHost.getSettingsLifecycle()).thenReturn(mLifecycle);

        SurveyMenuController.init(mHost, mActivity, TEST_SURVEY_TRIGGER_KEY, TEST_PAGE_ID);

        verify(mLifecycle).addObserver(any(SurveyMenuController.class));
    }

    @Test
    public void init_withSurveyFeatureProvider_shouldAttachToLifecycle() {
        when(mHost.getSettingsLifecycle()).thenReturn(mLifecycle);

        SurveyMenuController.init(mHost, mSurveyFeatureProvider, TEST_SURVEY_TRIGGER_KEY,
                TEST_PAGE_ID);

        verify(mLifecycle).addObserver(any(SurveyMenuController.class));
    }

    @Test
    public void onCreateOptionsMenu_surveyAvailable_shouldAddSurveyMenu() {
        setupSurveyAvailability(/* available= */ true);
        SurveyMenuController.init(mHost, mSurveyFeatureProvider, TEST_SURVEY_TRIGGER_KEY,
                TEST_PAGE_ID);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, /* inflater= */ null);

        verify(mMenu).add(anyInt(), eq(MenusUtils.MenuId.SEND_SURVEY.getValue()), anyInt(),
                anyInt());
        verify(mMenuItem).setIcon(R.drawable.ic_rate_review);
    }

    @Test
    public void onCreateOptionsMenu_surveyUnavailable_shouldNotAddSurveyMenu() {
        setupSurveyAvailability(/* available= */ false);
        SurveyMenuController.init(mHost, mSurveyFeatureProvider, TEST_SURVEY_TRIGGER_KEY,
                TEST_PAGE_ID);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, /* inflater= */ null);

        verify(mMenu, never()).add(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void onOptionsItemSelected_surveyMenuSelected_shouldStartAndHideSurveyEntry() {
        FragmentActivity mockActivity = mock(FragmentActivity.class);
        when(mockActivity.getPackageName()).thenReturn(SETTINGS_PACKAGE_NAME);
        when(mHost.getActivity()).thenReturn(mockActivity);
        when(mMenuItem.getItemId()).thenReturn(MenusUtils.MenuId.SEND_SURVEY.getValue());
        SurveyMenuController.init(mHost, mSurveyFeatureProvider, TEST_SURVEY_TRIGGER_KEY,
                TEST_PAGE_ID);

        mHost.getSettingsLifecycle().onOptionsItemSelected(mMenuItem);

        // Verify it start to send survey
        verify(mSurveyFeatureProvider).sendActivityIfAvailable(TEST_SURVEY_TRIGGER_KEY);
        // Verify notification dismissal broadcast was sent
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockActivity).sendBroadcastAsUser(intentCaptor.capture(),
                any(), eq(android.Manifest.permission.MANAGE_ACCESSIBILITY));
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(
                ACTION_SURVEY_NOTIFICATION_DISMISSED);
        assertThat(intentCaptor.getValue().getPackage()).isEqualTo(SETTINGS_PACKAGE_NAME);
        assertThat(intentCaptor.getValue().getIntExtra(EXTRA_PAGE_ID, -1)).isEqualTo(TEST_PAGE_ID);
        // Verify that menu is updated and the item is not added
        verify(mockActivity).invalidateOptionsMenu();
        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, /* inflater= */ null);
        verify(mMenu, never()).add(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void onOptionsItemSelected_otherMenuItemSelected_shouldNotStartSurvey() {
        SurveyMenuController.init(mHost, mSurveyFeatureProvider, TEST_SURVEY_TRIGGER_KEY,
                TEST_PAGE_ID);
        when(mMenuItem.getItemId()).thenReturn(Menu.FIRST);

        mHost.getSettingsLifecycle().onOptionsItemSelected(mMenuItem);

        verify(mSurveyFeatureProvider, never()).sendActivityIfAvailable(TEST_SURVEY_TRIGGER_KEY);
    }

    @Test
    public void startSurvey_withSurveyFeatureProvider_shouldStartSurvey() {
        final SurveyMenuController controller =
                SurveyMenuController.init(mHost, mSurveyFeatureProvider, TEST_SURVEY_TRIGGER_KEY,
                        TEST_PAGE_ID);

        controller.startSurvey();

        verify(mSurveyFeatureProvider).sendActivityIfAvailable(TEST_SURVEY_TRIGGER_KEY);
    }

    @Test
    public void startSurvey_nullSurveyFeatureProvider_shouldNotStartSurvey() {
        final SurveyMenuController controller =
                SurveyMenuController.init(mHost, (SurveyFeatureProvider) null,
                        TEST_SURVEY_TRIGGER_KEY, TEST_PAGE_ID);

        controller.startSurvey();

        verify(mSurveyFeatureProvider, never()).sendActivityIfAvailable(TEST_SURVEY_TRIGGER_KEY);
    }

    private void setupSurveyAvailability(boolean available) {
        doAnswer(invocation -> {
            Consumer<Boolean> consumer = invocation.getArgument(2);
            consumer.accept(available);
            return null;
        }).when(mSurveyFeatureProvider).checkSurveyAvailable(any(), anyString(), any());
    }
}
