/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.text.format.Formatter;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.ProcStatsData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class MemoryUsagePreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private ProcStatsData mProcStatsData;
    @Mock
    private ProcStatsData.MemInfo mMemInfo;

    private Context mContext;
    private MemoryUsagePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new MemoryUsagePreferenceController(mContext));
        doReturn(mProcStatsData).when(mController).getProcStatsData();
        doNothing().when(mController).setDuration();
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mProcStatsData.getMemInfo()).thenReturn(mMemInfo);
        mController.displayPreference(mScreen);
    }

    @Test
    public void updateState_shouldUpdatePreferenceSummary() {
        mController.updateState(mPreference);

        verify(mPreference).setSummary(anyString());
    }

    @Test
    public void updateState_whenPssProfilingDisabled_shouldSetDisabledSummary() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.FORCE_ENABLE_PSS_PROFILING, 0);
        final String expectedSummary = mContext.getString(R.string.pss_profiling_disabled);

        mController.updateState(mPreference);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        final ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(mPreference).setSummary(summaryCaptor.capture());
        assertThat(summaryCaptor.getValue()).isEqualTo(expectedSummary);
    }

    @Test
    public void updateState_whenPssProfilingEnabled_shouldSetMemorySummary() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.FORCE_ENABLE_PSS_PROFILING, 1);
        mMemInfo.realUsedRam = 1024L * 1024 * 1000; // 1GB
        mMemInfo.realTotalRam = 1024L * 1024 * 2000; // 2GB
        final String usedResult = Formatter.formatShortFileSize(mContext,
                (long) mMemInfo.realUsedRam);
        final String totalResult = Formatter.formatShortFileSize(mContext,
                (long) mMemInfo.realTotalRam);
        final String expectedSummary = mContext.getString(R.string.memory_summary, usedResult,
                totalResult);

        mController.updateState(mPreference);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        final ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(mPreference).setSummary(summaryCaptor.capture());
        assertThat(summaryCaptor.getValue()).isEqualTo(expectedSummary);
    }
}
