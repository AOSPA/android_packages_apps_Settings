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

package com.android.settings.connecteddevice.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

/** Unit tests for {@link ExternalDisplayUpdater}. */
@RunWith(AndroidJUnit4.class)
public class ExternalDisplayUpdaterTest extends ExternalDisplayTestBase {

    private TestableExternalDisplayUpdater mUpdater;
    @Mock private DevicePreferenceCallback mMockedCallback;
    @Mock private Drawable mMockedDrawable;
    @Mock private MetricsFeatureProvider mMetricsFeatureProvider;
    private RestrictedPreference mPreferenceAdded;
    private RestrictedPreference mPreferenceRemoved;

    @Before
    public void setUp() throws RemoteException {
        super.setUp();
        mUpdater = new TestableExternalDisplayUpdater(mMockedCallback, /* metricsCategory= */ 0);
        ReflectionHelpers.setField(mUpdater, "metricsFeatureProvider", mMetricsFeatureProvider);
    }

    @Test
    public void testPreferenceAdded_displayEnabled() {
        doAnswer(
                        (v) -> {
                            mPreferenceAdded = v.getArgument(0);
                            return null;
                        })
                .when(mMockedCallback)
                .onDeviceAdded(any());

        updateDisplaysAndTopology(List.of(createExternalDisplay(DisplayIsEnabled.YES)));

        mUpdater.initPreference(mContext, mMockedInjector);
        mUpdater.refreshPreference();
        mUpdater.registerCallback();
        mHandler.flush();

        assertThat(mPreferenceAdded).isNotNull();
    }

    @Test
    public void testPreferenceRemoved_displayDisabled() {
        doAnswer(
                        (v) -> {
                            mPreferenceRemoved = v.getArgument(0);
                            return null;
                        })
                .when(mMockedCallback)
                .onDeviceRemoved(any());

        updateDisplaysAndTopology(List.of(createExternalDisplay(DisplayIsEnabled.NO)));

        mUpdater.initPreference(mContext, mMockedInjector);
        mUpdater.refreshPreference();
        mUpdater.registerCallback();
        mHandler.flush();

        assertThat(mPreferenceRemoved).isNotNull();
    }

    @Test
    public void testPreferenceRemoved() {
        doAnswer(
                        (v) -> {
                            mPreferenceAdded = v.getArgument(0);
                            return null;
                        })
                .when(mMockedCallback)
                .onDeviceAdded(any());
        doAnswer(
                        (v) -> {
                            mPreferenceRemoved = v.getArgument(0);
                            return null;
                        })
                .when(mMockedCallback)
                .onDeviceRemoved(any());
        mUpdater.initPreference(mContext, mMockedInjector);
        mUpdater.refreshPreference();
        mUpdater.registerCallback();
        mHandler.flush();
        assertThat(mPreferenceAdded).isNotNull();
        assertThat(mPreferenceRemoved).isNull();
        // Remove display
        doReturn(List.of()).when(mMockedInjector).getDisplays();
        mListener.onDisplayRemoved(1);
        mHandler.flush();
        assertThat(mPreferenceRemoved).isEqualTo(mPreferenceAdded);
    }

    @Test
    public void testPreferenceClick_logsMetric() {
        doAnswer(
                        (v) -> {
                            mPreferenceAdded = v.getArgument(0);
                            return null;
                        })
                .when(mMockedCallback)
                .onDeviceAdded(any());

        updateDisplaysAndTopology(List.of(createExternalDisplay(DisplayIsEnabled.YES)));
        mUpdater.initPreference(mContext, mMockedInjector);
        mUpdater.refreshPreference();
        mHandler.flush();

        assertThat(mPreferenceAdded).isNotNull();
        mPreferenceAdded.performClick();

        // Verify metric logging
        verify(mMetricsFeatureProvider).logClickedPreference(any(), anyInt());
    }

    @Test
    public void testInitPreference_propertiesSet() {
        doAnswer(
                        (v) -> {
                            mPreferenceAdded = v.getArgument(0);
                            return null;
                        })
                .when(mMockedCallback)
                .onDeviceAdded(any());

        updateDisplaysAndTopology(List.of(createExternalDisplay(DisplayIsEnabled.YES)));
        mUpdater.initPreference(mContext, mMockedInjector);
        mUpdater.refreshPreference();
        mHandler.flush();

        assertThat(mPreferenceAdded.getTitle().toString())
                .isEqualTo(mContext.getString(R.string.external_display_settings_title));
        assertThat(mPreferenceAdded.getKey()).isEqualTo(ExternalDisplayUpdater.PREF_KEY);
        assertThat(mPreferenceAdded.getIcon()).isEqualTo(mMockedDrawable);
    }

    @Test
    public void testPreferenceClick_launchesSettings() {
        doAnswer(
                        (v) -> {
                            mPreferenceAdded = v.getArgument(0);
                            return null;
                        })
                .when(mMockedCallback)
                .onDeviceAdded(any());

        updateDisplaysAndTopology(List.of(createExternalDisplay(DisplayIsEnabled.YES)));
        mUpdater.initPreference(mContext, mMockedInjector);
        mUpdater.refreshPreference();
        mHandler.flush();

        mPreferenceAdded.performClick();

        assertThat(mUpdater.mLaunchSettingsCalled).isTrue();
    }

    class TestableExternalDisplayUpdater extends ExternalDisplayUpdater {

        boolean mLaunchSettingsCalled = false;

        TestableExternalDisplayUpdater(DevicePreferenceCallback callback, int metricsCategory) {
            super(callback, metricsCategory);
        }

        @Override
        @Nullable
        protected Drawable getDrawable(Context context) {
            return mMockedDrawable;
        }

        @Override
        protected void launchSettings(Context context, @Nullable android.os.Bundle args) {
            mLaunchSettingsCalled = true;
        }
    }
}
