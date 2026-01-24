/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.applications.credentials;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.metrics.CredmanMetricsLogger;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class DefaultCombinedPreferenceControllerTest {

    private static final String PACKAGE_NAME = "com.example.provider";
    private static final String SETTINGS_ACTIVITY = "com.example.provider.SettingsActivity";

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private CredmanMetricsLogger mCredmanMetricsLogger;
    @Mock
    private PrimaryProviderPreference mPreference;
    @Captor
    private ArgumentCaptor<PrimaryProviderPreference.Delegate> mDelegateCaptor;

    private Context mContext;
    private PrimaryProviderPreference.Delegate mDelegate;
    private AttributeSet mAttributes;
    private DefaultCombinedPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        doNothing().when(mContext).startActivity(any(Intent.class));
        doNothing().when(mContext).startActivityAsUser(any(Intent.class), any(UserHandle.class));

        if (Looper.myLooper() == null) {
            Looper.prepare(); // needed to create the preference screen
        }
        mDelegate =
                new PrimaryProviderPreference.Delegate() {
                    public void onOpenButtonClicked() {}

                    public void onChangeButtonClicked() {}
                };
        mController = new DefaultCombinedPreferenceController(mContext, false, false);
        mController.setCredmanMetricsLogger(mCredmanMetricsLogger);
    }

    @Test
    public void ensureSettingsActivityIntentCreatedSuccessfully() {
        // Ensure that the settings activity is only created if we haved the right combination
        // of package and class name.
        assertThat(CombinedProviderInfo.createSettingsActivityIntent(null, null)).isNull();
        assertThat(CombinedProviderInfo.createSettingsActivityIntent("", null)).isNull();
        assertThat(CombinedProviderInfo.createSettingsActivityIntent("", "")).isNull();
        assertThat(CombinedProviderInfo.createSettingsActivityIntent("com.test", "")).isNull();
        assertThat(CombinedProviderInfo.createSettingsActivityIntent("com.test", "ClassName"))
                .isNotNull();
    }

    @Test
    public void ensureUpdatePreferenceForProviderPopulatesInfo() {
        if (PrimaryProviderPreference.shouldUseNewSettingsUi()) {
            return;
        }

        PrimaryProviderPreference ppp = createTestPreference();
        Drawable appIcon = mContext.getResources().getDrawable(R.drawable.ic_settings_delete);

        // Update the preference to use the provider and make sure the view
        // was updated.
        mController.updatePreferenceForProvider(ppp, "App Name", "Subtitle", appIcon, null, null);
        assertThat(ppp.getTitle().toString()).isEqualTo("App Name");
        assertThat(ppp.getSummary().toString()).isEqualTo("Subtitle");
        assertThat(ppp.getIcon()).isEqualTo(appIcon);

        // Set the preference back to none and make sure the view was updated.
        mController.updatePreferenceForProvider(ppp, null, null, null, null, null);
        assertThat(ppp.getTitle().toString()).isEqualTo("None selected");
        assertThat(ppp.getSummary()).isNull();
        assertThat(ppp.getIcon()).isNull();
    }

    @Test
    public void updatePreferenceForProvider_onOpenButtonClick_logsMetrics() {
        // Update the preference, which sets the delegate.
        mController.updatePreferenceForProvider(
                mPreference, "AppName", "Subtitle", null, PACKAGE_NAME, SETTINGS_ACTIVITY);

        // Capture and trigger the delegate's open button click.
        verify(mPreference).setDelegate(mDelegateCaptor.capture());
        mDelegateCaptor.getValue().onOpenButtonClicked();

        // Verification: Ensure the outbound intent launch is logged.
        verify(mCredmanMetricsLogger).recordPreferredServiceOutboundLaunch(PACKAGE_NAME);
    }

    @Test
    public void updatePreferenceForProvider_onOpenButtonClick_launchFails_doesNotLog() {
        doThrow(new ActivityNotFoundException())
                .when(mContext)
                .startActivityAsUser(any(Intent.class), any(UserHandle.class));

        // Update the preference, which sets the delegate.
        mController.updatePreferenceForProvider(
                mPreference, "AppName", "Subtitle", null, PACKAGE_NAME, SETTINGS_ACTIVITY);

        // Capture and trigger the delegate's open button click.
        verify(mPreference).setDelegate(mDelegateCaptor.capture());
        mDelegateCaptor.getValue().onOpenButtonClicked();

        // Verification: Ensure the metric is not logged when the launch fails.
        verify(mCredmanMetricsLogger, never()).recordPreferredServiceOutboundLaunch(anyString());
    }

    @Test
    public void updatePreferenceForProvider_onChangeButtonClick_logsMetrics() {
        // Update the preference, which sets the delegate.
        mController.updatePreferenceForProvider(
                mPreference, "AppName", "Subtitle", null, PACKAGE_NAME, "SettingsActivity");

        // Capture and trigger the delegate's change button click.
        verify(mPreference).setDelegate(mDelegateCaptor.capture());
        mDelegateCaptor.getValue().onChangeButtonClicked();

        // Verification: Ensure the edit event is logged.
        verify(mCredmanMetricsLogger).logEditPreferredServiceEvent(PACKAGE_NAME);
    }

    private PrimaryProviderPreference createTestPreference() {
        int layoutId =
                ResourcesUtils.getResourcesId(
                        mContext, "layout", "preference_credential_manager_with_buttons");
        PreferenceViewHolder holder =
                PreferenceViewHolder.createInstanceForTests(
                        LayoutInflater.from(mContext).inflate(layoutId, null));
        PreferenceViewHolder holderForTest = spy(holder);
        View gearView = new View(mContext, null);
        int gearId = ResourcesUtils.getResourcesId(mContext, "id", "settings_button");
        when(holderForTest.findViewById(gearId)).thenReturn(gearView);

        PrimaryProviderPreference ppp = new PrimaryProviderPreference(mContext, mAttributes);
        ppp.setDelegate(mDelegate);
        ppp.onBindViewHolder(holderForTest);
        return ppp;
    }
}
