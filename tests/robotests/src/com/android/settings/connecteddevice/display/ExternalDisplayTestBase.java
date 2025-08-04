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

import static android.view.Display.DEFAULT_DISPLAY;

import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY;
import static com.android.settings.flags.Flags.FLAG_DISPLAY_SIZE_CONNECTED_DISPLAY_SETTING;
import static com.android.settings.flags.Flags.FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST;
import static com.android.settings.flags.Flags.FLAG_ENABLE_DEFAULT_DISPLAY_IN_TOPOLOGY_SWITCH_BUGFIX;
import static com.android.settings.flags.Flags.FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING;
import static com.android.settings.flags.Flags.FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING_BUGFIX;
import static com.android.settings.flags.Flags.FLAG_ROTATION_CONNECTED_DISPLAY_SETTING;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayTopology;
import android.os.RemoteException;
import android.view.Display.Mode;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.settings.flags.FakeFeatureFlagsImpl;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class ExternalDisplayTestBase {
    static final int EXTERNAL_DISPLAY_ID = 1;
    static final int OVERLAY_DISPLAY_ID = 2;

    @Mock
    ConnectedDisplayInjector mMockedInjector;
    @Mock
    Resources mResources;
    FakeFeatureFlagsImpl mFlags = new FakeFeatureFlagsImpl();
    DesktopExperienceFlags mInjectedFlags = new DesktopExperienceFlags(mFlags);
    Context mContext;
    DisplayListener mListener;
    TestHandler mHandler;
    PreferenceManager mPreferenceManager;
    PreferenceScreen mPreferenceScreen;
    List<DisplayDevice> mDisplays;
    DisplayTopology mDisplayTopology;

    /**
     * Setup.
     */
    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        doReturn(mResources).when(mContext).getResources();
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, false);
        mFlags.setFlag(FLAG_ROTATION_CONNECTED_DISPLAY_SETTING, true);
        mFlags.setFlag(FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING, true);
        mFlags.setFlag(FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING_BUGFIX, true);
        mFlags.setFlag(FLAG_DISPLAY_SIZE_CONNECTED_DISPLAY_SETTING, true);
        mFlags.setFlag(FLAG_ENABLE_DEFAULT_DISPLAY_IN_TOPOLOGY_SWITCH_BUGFIX, true);
        updateDisplaysAndTopology(List.of(createExternalDisplay(DisplayIsEnabled.YES),
                createOverlayDisplay(DisplayIsEnabled.YES)));
        doReturn(mInjectedFlags).when(mMockedInjector).getFlags();
        mHandler = new TestHandler(mContext.getMainThreadHandler());
        doReturn(mHandler).when(mMockedInjector).getHandler();
        doReturn("").when(mMockedInjector).getSystemProperty(
                VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY);
        doReturn(true).when(mMockedInjector).isDefaultDisplayInTopologyFlagEnabled();
        doReturn(true).when(mMockedInjector).isProjectedModeEnabled();
        doAnswer((arg) -> {
            mListener = arg.getArgument(0);
            return null;
        }).when(mMockedInjector).registerDisplayListener(any());
        doReturn(0).when(mMockedInjector).getDisplayUserRotation(anyInt());
        doReturn(mContext).when(mMockedInjector).getContext();
    }

    DisplayDevice includeBuiltinDisplay() {
        List<DisplayDevice> displays = new ArrayList<>(mDisplays);
        Mode mode = new Mode(720, 1280, 60f);
        DisplayDevice builtinDisplay = new DisplayDevice(
                DEFAULT_DISPLAY, "local:1111111111", "Built-in display", mode, List.of(mode),
                DisplayIsEnabled.YES, /* isConnectedDisplay= */ false);
        displays.addFirst(builtinDisplay);
        mDisplays = displays;
        doReturn(builtinDisplay).when(mMockedInjector).getDisplay(DEFAULT_DISPLAY);
        doReturn(mDisplays).when(mMockedInjector).getDisplays();
        updateDisplayTopology();
        return builtinDisplay;
    }

    DisplayDevice createExternalDisplay(DisplayIsEnabled isEnabled) {
        var supportedModes = List.of(new Mode(0, 1920, 1080, 60, 60, new float[0], new int[0]),
                new Mode(1, 800, 600, 60, 60, new float[0], new int[0]),
                new Mode(2, 320, 240, 70, 70, new float[0], new int[0]),
                new Mode(3, 640, 480, 60, 60, new float[0], new int[0]),
                new Mode(4, 640, 480, 50, 60, new float[0], new int[0]),
                new Mode(5, 2048, 1024, 60, 60, new float[0], new int[0]),
                new Mode(6, 720, 480, 60, 60, new float[0], new int[0]));
        return new DisplayDevice(EXTERNAL_DISPLAY_ID, "local:0987654321", "HDMI",
                supportedModes.get(0), supportedModes, isEnabled, /* isConnectedDisplay= */ true);
    }

    DisplayDevice createOverlayDisplay(DisplayIsEnabled isEnabled) {
        var supportedModes = List.of(new Mode(0, 1240, 780, 60, 60, new float[0], new int[0]));
        return new DisplayDevice(OVERLAY_DISPLAY_ID, "local:1357902468", "Overlay #1",
                supportedModes.get(0), supportedModes, isEnabled, /* isConnectedDisplay= */ true);
    }

    void updateDisplaysAndTopology(List<DisplayDevice> displays) {
        mDisplays = displays;
        doReturn(mDisplays).when(mMockedInjector).getDisplays();
        for (var display : mDisplays) {
            doReturn(display).when(mMockedInjector).getDisplay(display.getId());
        }

        updateDisplayTopology();
    }

    private void updateDisplayTopology() {
        mDisplayTopology = new DisplayTopology();
        mDisplays.forEach(
                display -> mDisplayTopology.addDisplay(display.getId(), /* logicalWidth= */
                        123, /* logicalHeight= */ 456, /* logicalDensity= */ 789));
        doReturn(mDisplayTopology).when(mMockedInjector).getDisplayTopology();
    }
}
