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

import static android.hardware.input.AppLaunchData.createLaunchDataForComponent;
import static android.hardware.input.InputManager.CUSTOM_INPUT_GESTURE_RESULT_SUCCESS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.hardware.input.AppLaunchData;
import android.hardware.input.AppLaunchData.ComponentData;
import android.hardware.input.InputGestureData;
import android.hardware.input.InputManager;
import android.hardware.input.KeyGestureEvent;
import android.os.UserHandle;

import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.testutils.shadow.ShadowSystemSettings;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link TouchpadThreeFingerTapActionPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSystemSettings.class,
})
public class TouchpadThreeFingerTapAppListControllerTest {

    private static final String PREF_KEY = "testScreen";
    private static final String TEST_TITLE_PREFIX = "testTitle";
    private static final String TEST_PACKAGE_PREFIX = "testPackage";
    private static final String TEST_CLASS_PREFIX = "testClass";
    private static final int GO_HOME_GESTURE = KeyGestureEvent.KEY_GESTURE_TYPE_HOME;
    private static final InputGestureData.Filter TOUCHPAD_FILTER = InputGestureData.Filter.TOUCHPAD;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private PreferenceScreen mMockPreferenceScreen;

    @Mock
    private ContentObserver mMockContentObserver;

    @Mock
    private LauncherApps mMockLauncherApps;
    @Mock
    private LauncherActivityInfo mMockActivityInfo1;
    @Mock
    private LauncherActivityInfo mMockActivityInfo2;
    @Mock
    private Drawable mMockDrawable;

    @Mock
    private InputManager mMockInputManager;

    private final Context mContext = RuntimeEnvironment.application;
    private ContentResolver mContentResolver;
    private TouchpadThreeFingerTapAppListController mController;
    private InputGestureData mCustomInputGesture;
    private LauncherApps.Callback mLauncherAppsCallback;

    @Before
    public void setup() {
        mContentResolver = mContext.getContentResolver();
        mController = new TouchpadThreeFingerTapAppListController(
                mContext, PREF_KEY, mMockLauncherApps, mMockInputManager, mMockContentObserver);
        setupMockLauncherApps();
        setupMockInputManager();
    }

    private void setupMockInputManager() {
        doAnswer(
                invocation -> {
                    mCustomInputGesture = null;
                    return null;
                }
        ).when(mMockInputManager).removeAllCustomInputGestures(eq(TOUCHPAD_FILTER));

        doAnswer(
                invocation -> {
                    mCustomInputGesture = invocation.getArgument(0);
                    return CUSTOM_INPUT_GESTURE_RESULT_SUCCESS;
                }
        ).when(mMockInputManager).addCustomInputGesture(any(InputGestureData.class));

        doAnswer(
                invocation -> {
                    return mCustomInputGesture;
                }
        ).when(mMockInputManager).getInputGesture(eq(TouchpadThreeFingerTapUtils.TRIGGER));
    }

    private void setupMockLauncherApps() {
        List<LauncherActivityInfo> activityInfos = new ArrayList<>();
        activityInfos.add(mMockActivityInfo1);
        activityInfos.add(mMockActivityInfo2);
        when(mMockLauncherApps.getActivityList(isNull(), any(UserHandle.class)))
                .thenReturn(activityInfos);

        for (int i = 0; i < activityInfos.size(); i++) {
            setupMockActivityInfo(activityInfos.get(i), i);
        }
    }

    private void setupMockActivityInfo(LauncherActivityInfo activityInfo, int suffix) {
        when(activityInfo.getLabel()).thenReturn(TEST_TITLE_PREFIX + suffix);
        when(activityInfo.getComponentName()).thenReturn(new ComponentName(
                TEST_PACKAGE_PREFIX + suffix, TEST_CLASS_PREFIX + suffix));
        when(activityInfo.getIcon(anyInt())).thenReturn(mMockDrawable);
    }

    @Test
    public void displayPreference_populateAllApps() {
        ArgumentCaptor<SelectorWithWidgetPreference> captor =
                ArgumentCaptor.forClass(SelectorWithWidgetPreference.class);

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockPreferenceScreen).removeAll();
        verify(mMockPreferenceScreen, times(2)).addPreference(captor.capture());

        List<SelectorWithWidgetPreference> prefs = captor.getAllValues();
        assertThat(prefs).hasSize(2);

        for (int i = 0; i < prefs.size(); i++) {
            SelectorWithWidgetPreference pref = prefs.get(i);
            assertTrue((TEST_TITLE_PREFIX + i).contentEquals(pref.getTitle()));
            assertThat(pref.getIcon()).isEqualTo(mMockDrawable);

            ComponentName component = new ComponentName(
                    TEST_PACKAGE_PREFIX + i, TEST_CLASS_PREFIX + i);
            String key = component.flattenToString();
            assertThat(pref.getKey()).isEqualTo(key);
        }
    }

    @Test
    public void updateState_whenActionIsLaunchApp_correspondingAppChecked() {
        ArgumentCaptor<SelectorWithWidgetPreference> captor = capturePrefs();

        setupAppSelection(/* matchingIndex = */ 1);
        mController.updateState(mMockPreferenceScreen);

        List<SelectorWithWidgetPreference> prefs = captor.getAllValues();
        assertThat(prefs.get(0).isChecked()).isFalse();
        assertThat(prefs.get(1).isChecked()).isTrue();
    }

    @Test
    public void updateState_whenActionIsGoHome_nothingChecked() {
        ArgumentCaptor<SelectorWithWidgetPreference> captor = capturePrefs();

        TouchpadThreeFingerTapUtils.setGestureType(mContentResolver, GO_HOME_GESTURE);
        mController.updateState(mMockPreferenceScreen);

        List<SelectorWithWidgetPreference> prefs = captor.getAllValues();
        assertTrue(prefs.stream().noneMatch(TwoStatePreference::isChecked));
    }

    @Test
    public void onRadioButtonClick_gestureAndTargetAppUpdated() {
        ArgumentCaptor<SelectorWithWidgetPreference> captor = capturePrefs();

        int clickingIndex = 0;
        mController.onRadioButtonClicked(captor.getAllValues().get(clickingIndex));

        // Settings key is updated
        assertTrue(TouchpadThreeFingerTapUtils.isGestureTypeLaunchApp(mContentResolver));

        // InputManager gesture is updated
        assertThat(mCustomInputGesture).isNotNull();
        ComponentData componentData =
                (ComponentData) mCustomInputGesture.getAction().appLaunchData();
        assertThat(componentData).isNotNull();
        assertThat(componentData.getPackageName()).isEqualTo(TEST_PACKAGE_PREFIX + clickingIndex);
        assertThat(componentData.getClassName()).isEqualTo(TEST_CLASS_PREFIX + clickingIndex);

        // The pref list is updated accordingly
        List<SelectorWithWidgetPreference> prefs = captor.getAllValues();
        assertThat(prefs.get(0).isChecked()).isTrue();
        assertThat(prefs.get(1).isChecked()).isFalse();
    }

    @Test
    public void onPackageRemoved_isNotSelectedApp_doNothing() {
        ArgumentCaptor<LauncherApps.Callback> captor =
                ArgumentCaptor.forClass(LauncherApps.Callback.class);
        verify(mMockLauncherApps).registerCallback(captor.capture());
        mLauncherAppsCallback = captor.getValue();

        int selectedAppIndex = 0;
        setupAppSelection(/* matchingIndex = */ selectedAppIndex);
        mController.updateState(mMockPreferenceScreen);

        mLauncherAppsCallback.onPackageRemoved(
                TEST_PACKAGE_PREFIX + 1, UserHandle.CURRENT);

        assertTrue(TouchpadThreeFingerTapUtils.isGestureTypeLaunchApp(mContentResolver));
    }

    @Test
    public void onPackageRemoved_isSelectedApp_setToDefaultGesture() {
        ArgumentCaptor<LauncherApps.Callback> captor =
                ArgumentCaptor.forClass(LauncherApps.Callback.class);
        verify(mMockLauncherApps).registerCallback(captor.capture());
        mLauncherAppsCallback = captor.getValue();

        int selectedAppIndex = 0;
        setupAppSelection(/* matchingIndex = */ selectedAppIndex);
        mController.updateState(mMockPreferenceScreen);

        mLauncherAppsCallback.onPackageRemoved(
                TEST_PACKAGE_PREFIX + selectedAppIndex, UserHandle.CURRENT);

        // Settings key is updated
        int gesture = TouchpadThreeFingerTapUtils.getCurrentGestureType(mContentResolver);
        assertThat(gesture).isEqualTo(TouchpadThreeFingerTapUtils.DEFAULT_GESTURE_TYPE);
    }

    private ArgumentCaptor<SelectorWithWidgetPreference> capturePrefs() {
        ArgumentCaptor<SelectorWithWidgetPreference> captor =
                ArgumentCaptor.forClass(SelectorWithWidgetPreference.class);
        mController.displayPreference(mMockPreferenceScreen);
        verify(mMockPreferenceScreen, times(2)).addPreference(captor.capture());
        when(mMockPreferenceScreen.getPreferenceCount()).thenReturn(captor.getAllValues().size());
        when(mMockPreferenceScreen.getPreference(eq(0)))
                .thenReturn(captor.getAllValues().get(0));
        when(mMockPreferenceScreen.getPreference(eq(1)))
                .thenReturn(captor.getAllValues().get(1));
        return captor;
    }

    private void setupAppSelection(int matchingIndex) {
        AppLaunchData appLaunchData = createLaunchDataForComponent(
                TEST_PACKAGE_PREFIX + matchingIndex, TEST_CLASS_PREFIX + matchingIndex);

        mCustomInputGesture = createGestureForApp(appLaunchData);
        TouchpadThreeFingerTapUtils.setLaunchAppAsGestureType(mContentResolver);
    }

    private InputGestureData createGestureForApp(AppLaunchData appLaunchData) {
        return new InputGestureData.Builder()
                .setTrigger(TouchpadThreeFingerTapUtils.TRIGGER)
                .setAppLaunchData(appLaunchData)
                .build();
    }
}
