/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.connecteddevice;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.sysprop.BluetoothProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.BluetoothDeviceRenamePreferenceController;
import com.android.settings.bluetooth.BluetoothSwitchPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.password.PasswordUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.MainSwitchBarController;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated screen for allowing the user to toggle bluetooth which displays relevant information to
 * the user based on related settings such as bluetooth scanning.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class BluetoothDashboardFragment extends DashboardFragment {

    private static final String TAG = "BluetoothDashboardFrag";
    private static final String KEY_BLUETOOTH_SCREEN_FOOTER = "bluetooth_screen_footer";
    private static final String BLUETOOTH_ADV_AUDIO_MASK_PROP
                                                  = "persist.vendor.service.bt.adv_audio_mask";
    private static final String BLUETOOTH_BROADCAST_UI_PROP = "persist.bluetooth.broadcast_ui";
    private static final String BLUETOOTH_BROADCAST_PTS_PROP
                                                  = "persist.vendor.service.bt.broadcast_pts";
    private static final int BROADCAST_MASK = 0x04;
    private static boolean mBroadcastEnabled = false;
    private static boolean mBroadcastPropertyChecked = false;
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    private static final String SLICE_ACTION = "com.android.settings.SEARCH_RESULT_TRAMPOLINE";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private FooterPreference mFooterPreference;
    private SettingsMainSwitchBar mSwitchBar;
    private BluetoothSwitchPreferenceController mController;

    public BluetoothDashboardFragment() {
        boolean broadcastPtsEnabled =
                SystemProperties.getBoolean(BLUETOOTH_BROADCAST_PTS_PROP, false);
        if ((BluetoothProperties.isProfileBapBroadcastSourceEnabled().orElse(false) ||
                BluetoothProperties.isProfileBapBroadcastAssistEnabled().orElse(false)) &&
                !broadcastPtsEnabled) {
            SystemProperties.set(BLUETOOTH_BROADCAST_UI_PROP, "false");
        } else {
            Log.d(TAG, "Use legacy broadcast if available");
            SystemProperties.set(BLUETOOTH_BROADCAST_UI_PROP, "true");
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_FRAGMENT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_bluetooth_screen;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_screen;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mFooterPreference = findPreference(KEY_BLUETOOTH_SCREEN_FOOTER);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(BluetoothDeviceRenamePreferenceController.class).setFragment(this);
    }

    @Override
    protected void displayResourceTilesToScreen(PreferenceScreen screen) {
        if (mBroadcastEnabled == false) {
           screen.removePreference(screen.findPreference("bluetooth_screen_broadcast_enable"));
           screen.removePreference(
               screen.findPreference("bluetooth_screen_broadcast_pin_configure"));
        }
        super.displayResourceTilesToScreen(screen);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        if (mBroadcastPropertyChecked == false) {
            int advAudioMask = SystemProperties.getInt(BLUETOOTH_ADV_AUDIO_MASK_PROP, 0);
            mBroadcastEnabled = (((advAudioMask & BROADCAST_MASK) == BROADCAST_MASK) &&
                SystemProperties.getBoolean(BLUETOOTH_BROADCAST_UI_PROP, true));
            mBroadcastPropertyChecked = true;
        }

        if (mBroadcastEnabled == false) {
            return controllers;
        }

        Log.d (TAG, "createPreferenceControllers for Broadcast");

        try {
            Class<?> classBroadcastPinController =
                Class.forName("com.android.settings.bluetooth.BluetoothBroadcastPinController");
            Class<?> classBroadcastEnableController =
                Class.forName("com.android.settings.bluetooth.BluetoothBroadcastEnableController");
            Constructor ctorPin, ctorEnable;
            ctorPin = classBroadcastPinController
                          .getDeclaredConstructor(new Class[] {Context.class, Lifecycle.class});
            ctorEnable = classBroadcastEnableController
                .getDeclaredConstructor(new Class[] {Context.class, String.class, Lifecycle.class});
            Object objBroadcastPinController = ctorPin.newInstance(context, getSettingsLifecycle());
            Object objBroadcastEnableController = ctorEnable
                .newInstance(context, new String("bluetooth_screen_broadcast_enable"), getSettingsLifecycle());
            objBroadcastPinController.getClass().getMethod("setFragment", Fragment.class)
                .invoke(objBroadcastPinController, (Fragment) this);
            controllers.add((AbstractPreferenceController) objBroadcastPinController);
            controllers.add((AbstractPreferenceController) objBroadcastEnableController);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException | InstantiationException | IllegalArgumentException |
                 ExceptionInInitializerError e) {
            mBroadcastEnabled = false;
            e.printStackTrace();
        } finally {
            return controllers;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String callingAppPackageName = PasswordUtils.getCallingAppPackageName(
                getActivity().getActivityToken());
        String action = getIntent() != null ? getIntent().getAction() : "";
        if (DEBUG) {
            Log.d(TAG, "onActivityCreated() calling package name is : " + callingAppPackageName
                    + ", action : " + action);
        }

        SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.setTitle(getContext().getString(R.string.bluetooth_main_switch_title));
        mController = new BluetoothSwitchPreferenceController(activity,
                new MainSwitchBarController(mSwitchBar), mFooterPreference);
        mController.setAlwaysDiscoverable(isAlwaysDiscoverable(callingAppPackageName, action));
        Lifecycle lifecycle = getSettingsLifecycle();
        if (lifecycle != null) {
            lifecycle.addObserver(mController);
        }
    }

    @VisibleForTesting
    boolean isAlwaysDiscoverable(String callingAppPackageName, String action) {
        return TextUtils.equals(SLICE_ACTION, action) ? false
            : TextUtils.equals(SETTINGS_PACKAGE_NAME, callingAppPackageName)
                || TextUtils.equals(SYSTEMUI_PACKAGE_NAME, callingAppPackageName);
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.bluetooth_screen) {

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = super.getNonIndexableKeys(context);
                if (mBroadcastEnabled == false) {
                    keys.add("bluetooth_screen_broadcast_enable");
                    keys.add("bluetooth_screen_broadcast_pin_configure");
                }
                return keys;
            }
        };
}
