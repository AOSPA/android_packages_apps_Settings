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

import static android.view.accessibility.Flags.FLAG_FORCE_INVERT_COLOR;

import static com.android.internal.accessibility.common.NotificationConstants.ACTION_CANCEL_SURVEY_NOTIFICATION;
import static com.android.internal.accessibility.common.NotificationConstants.ACTION_SCHEDULE_SURVEY_NOTIFICATION;
import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID;
import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.core.util.Consumer;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link ForceInvertPreferenceController}. */
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
@RunWith(RobolectricTestRunner.class)
public class ForceInvertPreferenceControllerTest {

    private static final String TEST_SURVEY_TRIGGER_KEY = "surveyTriggerKey";
    private static final int TEST_PAGE_ID = 10;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public ActivityScenarioRule<EmptyFragmentActivity> mActivityScenario =
            new ActivityScenarioRule<>(EmptyFragmentActivity.class);
    @Mock
    private FragmentActivity mActivity;
    @Mock
    private InstrumentedPreferenceFragment mHost;
    @Mock
    private Resources mResources;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mPreferenceCategory;

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private SurveyFeatureProvider mSurveyFeatureProvider;
    private ForceInvertPreferenceController mController;
    private SelectorWithWidgetPreference mStandardDarkThemePreference;
    private SelectorWithWidgetPreference mExpandedDarkThemePreference;

    @Before
    public void setUp() {
        FakeFeatureFactory.setupForTest();
        mSurveyFeatureProvider =
                FakeFeatureFactory.getFeatureFactory().getSurveyFeatureProvider(mActivity);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getConfiguration()).thenReturn(new Configuration());

        mController = new ForceInvertPreferenceController(mContext, "dark_theme_group");
        mController.initializeForSurvey(mHost, mSurveyFeatureProvider, TEST_SURVEY_TRIGGER_KEY,
                TEST_PAGE_ID);

        mStandardDarkThemePreference = new SelectorWithWidgetPreference(mContext);
        mStandardDarkThemePreference
                .setKey(ForceInvertPreferenceController.STANDARD_DARK_THEME_KEY);
        mExpandedDarkThemePreference = new SelectorWithWidgetPreference(mContext);
        mExpandedDarkThemePreference
                .setKey(ForceInvertPreferenceController.EXPANDED_DARK_THEME_KEY);
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreferenceCategory);
        when(mPreferenceCategory
                .findPreference(ForceInvertPreferenceController.STANDARD_DARK_THEME_KEY))
                .thenReturn(mStandardDarkThemePreference);
        when(mPreferenceCategory
                .findPreference(ForceInvertPreferenceController.EXPANDED_DARK_THEME_KEY))
                .thenReturn(mExpandedDarkThemePreference);
    }

    @Test
    @DisableFlags(FLAG_FORCE_INVERT_COLOR)
    public void getAvailabilityStatus_flagOff_shouldReturnUnsupported() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags(FLAG_FORCE_INVERT_COLOR)
    public void getAvailabilityStatus_flagOn_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void displayPreference_forceInvertOff_reflectsCorrectValue() {
        setForceInvertEnabled(false);

        mController.displayPreference(mScreen);

        assertThat(mStandardDarkThemePreference.isChecked()).isTrue();
        assertThat(mExpandedDarkThemePreference.isChecked()).isFalse();
    }

    @Test
    public void displayPreference_forceInvertOn_reflectsCorrectValue() {
        setForceInvertEnabled(true);

        mController.displayPreference(mScreen);

        assertThat(mStandardDarkThemePreference.isChecked()).isFalse();
        assertThat(mExpandedDarkThemePreference.isChecked()).isTrue();
    }

    @Test
    public void clickStandardPreference_settingChanges() {
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mStandardDarkThemePreference);

        assertThat(mStandardDarkThemePreference.isChecked()).isTrue();
        assertThat(mExpandedDarkThemePreference.isChecked()).isFalse();
        boolean isForceInvertEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, /* def= */ -1) == ON;
        assertThat(isForceInvertEnabled).isFalse();
    }

    @Test
    public void clickExpandedPreference_settingChanges() {
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mExpandedDarkThemePreference);

        assertThat(mStandardDarkThemePreference.isChecked()).isFalse();
        assertThat(mExpandedDarkThemePreference.isChecked()).isTrue();
        boolean isForceInvertEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, /* def= */ -1) == ON;
        assertThat(isForceInvertEnabled).isTrue();
    }

    @Test
    public void clickExpandedPreference_samePreference_noChange() {
        setForceInvertEnabled(true);
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mExpandedDarkThemePreference);

        boolean isForceInvertEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, /* def= */ -1) == ON;
        assertThat(isForceInvertEnabled).isTrue();
    }

    @Test
    public void clickExpandedPreference_darkThemeAndSurveyAvailable_sendScheduleBroadcast() {
        setForceInvertEnabled(false);
        setDarkTheme();
        setupSurveyAvailability(true);
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mExpandedDarkThemePreference);

        verify(mSurveyFeatureProvider).checkSurveyAvailable(
                any(InstrumentedPreferenceFragment.class), any(), any());
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).sendBroadcastAsUser(intentCaptor.capture(),
                any(), eq(android.Manifest.permission.MANAGE_ACCESSIBILITY));
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(
                ACTION_SCHEDULE_SURVEY_NOTIFICATION);
        assertThat(intentCaptor.getValue().getPackage()).isEqualTo(SETTINGS_PACKAGE_NAME);
        assertThat(intentCaptor.getValue().getIntExtra(EXTRA_PAGE_ID, /* def= */ -1))
                .isEqualTo(TEST_PAGE_ID);
    }

    @Test
    public void clickExpandedPreference_darkThemeAndSurveyNotAvailable_noBroadcast() {
        setForceInvertEnabled(false);
        setDarkTheme();
        setupSurveyAvailability(false);
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mExpandedDarkThemePreference);

        verify(mSurveyFeatureProvider).checkSurveyAvailable(
                any(InstrumentedPreferenceFragment.class), any(), any());
        verify(mContext, never()).sendBroadcastAsUser(any(), any(), any());
    }

    @Test
    public void clickExpandedPreference_lightTheme_noSurveyCheck() {
        setForceInvertEnabled(false);
        setLightTheme();
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mExpandedDarkThemePreference);

        verify(mSurveyFeatureProvider, never()).checkSurveyAvailable(any(), any(), any());
    }

    @Test
    public void clickStandardPreference_sendCancelBroadcast() {
        setForceInvertEnabled(true);
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mStandardDarkThemePreference);

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).sendBroadcastAsUser(intentCaptor.capture(),
                any(), eq(android.Manifest.permission.MANAGE_ACCESSIBILITY));
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(
                ACTION_CANCEL_SURVEY_NOTIFICATION);
        assertThat(intentCaptor.getValue().getPackage()).isEqualTo(SETTINGS_PACKAGE_NAME);
        assertThat(intentCaptor.getValue().getIntExtra(EXTRA_PAGE_ID, /* def= */ -1))
                .isEqualTo(TEST_PAGE_ID);
    }

    private void setForceInvertEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, enabled ? ON : OFF);
    }

    private void setLightTheme() {
        Configuration config = new Configuration();
        config.uiMode = Configuration.UI_MODE_NIGHT_NO;
        when(mResources.getConfiguration()).thenReturn(config);
    }

    private void setDarkTheme() {
        Configuration config = new Configuration();
        config.uiMode = Configuration.UI_MODE_NIGHT_YES;
        when(mResources.getConfiguration()).thenReturn(config);
    }

    private void setupSurveyAvailability(boolean available) {
        doAnswer(invocation -> {
            Consumer<Boolean> consumer = invocation.getArgument(2);
            consumer.accept(available);
            return null;
        }).when(mSurveyFeatureProvider).checkSurveyAvailable(any(), anyString(), any());
    }
}
