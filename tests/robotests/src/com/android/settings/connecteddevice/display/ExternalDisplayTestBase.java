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

import static android.hardware.display.DisplayManager.HDR_PREFERENCE_HDR_ALLOWED;
import static android.view.Display.DEFAULT_DISPLAY;

import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyResourcesManager;
import android.app.admin.DpcAuthority;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayTopology;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.view.Display.Mode;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsActivity;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.wm.shell.shared.desktopmode.FakeDesktopState;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

import java.util.ArrayList;
import java.util.List;

public class ExternalDisplayTestBase {
    static final int EXTERNAL_DISPLAY_ID = 1;
    static final int OVERLAY_DISPLAY_ID = 2;

    @Mock ConnectedDisplayInjector mMockedInjector;
    @Mock Resources mResources;
    Context mContext;
    DisplayListener mListener;
    TestHandler mHandler;
    PreferenceManager mPreferenceManager;
    PreferenceScreen mPreferenceScreen;
    List<DisplayDevice> mDisplays;
    DisplayTopology mDisplayTopology;
    @Mock ActivityManager mActivityManager;
    @Mock ActivityTaskManager mActivityTaskManager;
    @Mock DevicePolicyManager mDevicePolicyManager;
    FakeDesktopState mFakeDesktopState = new FakeDesktopState();

    /** Setup. */
    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(Robolectric.buildActivity(SettingsActivity.class).get());
        mResources = spy(mContext.getResources());
        doReturn(mResources).when(mContext).getResources();
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        updateDisplaysAndTopology(
                List.of(
                        createExternalDisplay(DisplayIsEnabled.YES),
                        createOverlayDisplay(DisplayIsEnabled.YES)));
        doReturn(mFakeDesktopState).when(mMockedInjector).getDesktopState();
        doReturn(HDR_PREFERENCE_HDR_ALLOWED).when(mMockedInjector).getUserHdrPreference(anyInt());
        SystemProperties.set(ConnectedDisplayInjector.SYSPROP_ENABLE_HDR_MODE_SPLITTING, "true");
        mHandler = new TestHandler(mContext.getMainThreadHandler());
        doReturn(mHandler).when(mMockedInjector).getHandler();
        doReturn("")
                .when(mMockedInjector)
                .getSystemProperty(VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY);
        doReturn(true).when(mMockedInjector).isProjectedModeEnabled();
        doAnswer(
                        (arg) -> {
                            mListener = arg.getArgument(0);
                            return null;
                        })
                .when(mMockedInjector)
                .registerDisplayListener(any(DisplayManager.DisplayListener.class), anyBoolean());
        doReturn(mContext).when(mMockedInjector).getContext();
        doReturn(new kotlin.Pair<>(40.0f, 40.0f)).when(mMockedInjector).getPhysicalDpi(anyInt());

        setupMockDpm();
    }

    DisplayDevice includeBuiltinDisplay() {
        List<DisplayDevice> displays = new ArrayList<>(mDisplays);
        Mode mode = new Mode(720, 1280, 60f);
        DisplayDevice builtinDisplay =
                new DisplayDevice(
                        DEFAULT_DISPLAY,
                        "local:1111111111",
                        "Built-in display",
                        mode,
                        List.of(mode),
                        DisplayIsEnabled.YES,
                        /* isConnectedDisplay= */ false,
                        /* rotation= */ 0,
                        /* isHdrSupported= */ true);
        displays.addFirst(builtinDisplay);
        doReturn(builtinDisplay).when(mMockedInjector).getDisplay(DEFAULT_DISPLAY);
        updateDisplaysAndTopology(displays);
        return builtinDisplay;
    }

    DisplayDevice createExternalDisplay(DisplayIsEnabled isEnabled) {
        var supportedModes =
                List.of(
                        new Mode(0, 1920, 1080, 60, 60, new float[0], new int[0]),
                        new Mode(1, 800, 600, 60, 60, new float[0], new int[0]),
                        new Mode(2, 320, 240, 70, 70, new float[0], new int[0]),
                        new Mode(3, 640, 480, 60, 60, new float[0], new int[0]),
                        new Mode(4, 640, 480, 50, 60, new float[0], new int[0]),
                        new Mode(5, 2048, 1024, 60, 60, new float[0], new int[0]),
                        new Mode(6, 720, 480, 60, 60, new float[0], new int[0]),
                        new Mode(
                                7,
                                1,
                                -1,
                                Mode.FLAG_ANISOTROPY_CORRECTION,
                                760,
                                600,
                                60,
                                60,
                                new float[0],
                                new int[0]),
                        new Mode(
                                8,
                                3,
                                -1,
                                Mode.FLAG_ANISOTROPY_CORRECTION,
                                720,
                                480,
                                60,
                                60,
                                new float[0],
                                new int[0]));
        return new DisplayDevice(
                EXTERNAL_DISPLAY_ID,
                "local:0987654321",
                "HDMI",
                supportedModes.get(0),
                supportedModes,
                isEnabled,
                /* isConnectedDisplay= */ true,
                /* rotation= */ 0,
                /* isHdrSupported= */ true);
    }

    DisplayDevice createOverlayDisplay(DisplayIsEnabled isEnabled) {
        var supportedModes = List.of(new Mode(0, 1240, 780, 60, 60, new float[0], new int[0]));
        return new DisplayDevice(
                OVERLAY_DISPLAY_ID,
                "local:1357902468",
                "Overlay #1",
                supportedModes.get(0),
                supportedModes,
                isEnabled,
                /* isConnectedDisplay= */ true,
                /* rotation= */ 0,
                /* isHdrSupported= */ true);
    }

    void updateDisplaysAndTopology(List<DisplayDevice> displays) {
        mDisplays = displays;
        doReturn(mDisplays).when(mMockedInjector).getDisplays();
        List<DisplayDeviceAdditionalInfo> displayAdditionalInfoList =
                mDisplays.stream()
                        .map(
                                display ->
                                        new DisplayDeviceAdditionalInfo(
                                                display.getId(),
                                                display.getUniqueId(),
                                                display.getName(),
                                                display.getMode(),
                                                display.getSupportedModes(),
                                                display.isEnabled(),
                                                display.isConnectedDisplay(),
                                                /* rotation= */ 0,
                                                /* isHdrSupported= */ true,
                                                /* connectionPreference= */ 0,
                                                HDR_PREFERENCE_HDR_ALLOWED))
                        .toList();
        doReturn(displayAdditionalInfoList)
                .when(mMockedInjector)
                .getDisplaysWithAdditionalInfo(null);
        for (var display : mDisplays) {
            doReturn(display).when(mMockedInjector).getDisplay(display.getId());
        }

        updateDisplayTopology();
    }

    private void updateDisplayTopology() {
        mDisplayTopology = new DisplayTopology();
        mDisplays.forEach(
                display ->
                        mDisplayTopology.addDisplay(
                                display.getId(),
                                /* logicalWidth= */ 123,
                                /* logicalHeight= */ 456,
                                /* logicalDensity= */ 789));
        doReturn(mDisplayTopology).when(mMockedInjector).getDisplayTopology();
    }

    private void setupMockDpm() {
        final EnforcingAdmin enforcingAdmin =
                new EnforcingAdmin(
                        "pkg.test",
                        DpcAuthority.DPC_AUTHORITY,
                        UserHandle.of(UserHandle.myUserId()),
                        new ComponentName("admin", "adminclass"));

        PolicyEnforcementInfo mockPolicyInfo = mock(PolicyEnforcementInfo.class);
        DevicePolicyResourcesManager mockPolicyResourcesManager =
                mock(DevicePolicyResourcesManager.class);

        doReturn(enforcingAdmin).when(mockPolicyInfo).getMostImportantEnforcingAdmin();
        doReturn(mockPolicyInfo)
                .when(mDevicePolicyManager)
                .getEnforcingAdminsForPolicy(
                        eq(DevicePolicyIdentifiers.LOCK_TASK_POLICY), anyInt());
        doReturn("").when(mockPolicyResourcesManager).getString(any(), any());
        doReturn(mockPolicyResourcesManager).when(mDevicePolicyManager).getResources();
    }
}
