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
package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FeedbackPreferenceControllerTest {

    @Mock
    private Fragment mFragment;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private FeedbackPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mock(DevicePolicyManager.class)).when(mContext)
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        mController = new FeedbackPreferenceController(mFragment, mContext);
        final String prefKey = mController.getPreferenceKey();
        when(mScreen.findPreference(prefKey)).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(prefKey);
    }

    @Test
    public void isAvailable_noReporterPackage_shouldReturnFalse() {
        when(mContext.getResources().getString(anyInt())).thenReturn("");
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isVisible_afterUpdateState_shouldBeSameAsIsAvailable() {
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isEqualTo(mController.isAvailable());
    }

    @Test
    public void handlePreferenceTreeClick_whenConfigIsTrue_startsActivityWithNewTaskFlag() {
        final FeedbackPreferenceController controller = spy(mController);
        doReturn(true).when(controller).isAvailable();
        when(mContext.getResources().getBoolean(
                R.bool.config_launch_feedback_reporter_in_new_task)).thenReturn(true);

        controller.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment).startActivity(captor.capture());

        final Intent intent = captor.getValue();
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK)
                .isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void handlePreferenceTreeClick_whenConfigIsFalse_startsActivityForResultWithoutNewTaskFlag() {
        final FeedbackPreferenceController controller = spy(mController);
        doReturn(true).when(controller).isAvailable();
        when(mContext.getResources().getBoolean(
                R.bool.config_launch_feedback_reporter_in_new_task)).thenReturn(false);

        controller.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment).startActivityForResult(captor.capture(), anyInt());

        final Intent intent = captor.getValue();
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(0);
    }
}
