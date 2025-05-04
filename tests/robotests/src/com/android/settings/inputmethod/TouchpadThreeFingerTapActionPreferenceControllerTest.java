/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.input.KeyGestureEvent;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowSystemSettings;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link TouchpadThreeFingerTapActionPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSystemSettings.class,
})
public class TouchpadThreeFingerTapActionPreferenceControllerTest {


    private static final String PREF_KEY = "go_home";
    private static final int PREF_GESTURE = KeyGestureEvent.KEY_GESTURE_TYPE_HOME;
    private static final int OTHER_GESTURE = KeyGestureEvent.KEY_GESTURE_TYPE_BACK;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private PreferenceScreen mMockScreen;

    @Mock
    private SelectorWithWidgetPreference mMockPref;
    @Mock
    private ContentObserver mMockContentObserver;
    @Mock
    private PackageManager mMockPackageManager;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private TouchpadThreeFingerTapActionPreferenceController mController;

    @Before
    public void setup() {
        mContentResolver = mContext.getContentResolver();
        mController = new TouchpadThreeFingerTapActionPreferenceController(
                mContext, PREF_KEY, mMockContentObserver, mMockPackageManager);
        when(mMockScreen.findPreference(mController.getPreferenceKey())).thenReturn(mMockPref);
        when(mMockPref.getKey()).thenReturn(PREF_KEY);
        mController.displayPreference(mMockScreen);
    }

    @Test
    public void updateState_whenPreferenceIsNotCurrentGesture_preferenceNotChecked() {
        TouchpadThreeFingerTapUtils.setGestureType(mContentResolver, OTHER_GESTURE);

        mController.updateState(mMockPref);

        verify(mMockPref).setChecked(false);
    }


    @Test
    public void updateState_whenPreferenceMatchesCurrentGesture_preferenceChecked() {
        TouchpadThreeFingerTapUtils.setGestureType(mContentResolver, PREF_GESTURE);

        mController.updateState(mMockPref);

        verify(mMockPref).setChecked(true);
    }


    @Test
    public void onRadioButtonClick_gestureTypeUpdated() {
        mController.onRadioButtonClicked(mMockPref);

        int value = TouchpadThreeFingerTapUtils.getCurrentGestureType(mContentResolver);

        assertThat(value).isEqualTo(PREF_GESTURE);
    }
}
