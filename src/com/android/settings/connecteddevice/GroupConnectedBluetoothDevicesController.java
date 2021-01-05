/******************************************************************************
 *  Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/
package com.android.settings.connecteddevice;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.GroupBluetoothSettingsPreference;
import com.android.settings.bluetooth.ConnectedBluetoothDeviceUpdater;
import com.android.settings.bluetooth.GroupConnectedBluetoothDeviceUpdater;
import com.android.settings.bluetooth.GroupUtils;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.connecteddevice.usb.ConnectedUsbDeviceUpdater;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.DockUpdaterFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GearPreference;
import com.android.settings.widget.GearPreference.OnGearClickListener;
import com.android.settings.widget.GroupPreferenceCategory;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;

import com.android.settings.R;

/**
 * Controller to maintain for all connected Group devices.
 * It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}
 */
public class GroupConnectedBluetoothDevicesController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop,
        DevicePreferenceCallback, OnGearClickListener {

    private static final String TAG = "GroupConnectedBluetoothDevicesController";
    private static final String KEY_ONE = "group_one";
    private static final String KEY_TWO = "group_two";
    private static final String KEY_THREE = "group_three";
    private static final String KEY_FOUR = "group_four";
    private static final String KEY_FIVE = "group_five";
    private static final String KEY_SIX = "group_six";
    private static final String KEY_SEVEN = "group_seven";
    private static final String KEY_EIGHT = "group_eight";
    private static final String KEY_NINE = "group_nine";
    private static final String KEY_REMAINING = "group_remaining";
    private static final String KEY_GROUP = "group_connected_device_list";
    private GroupBluetoothSettingsPreference mGroupSettings;
    private PreferenceGroup mPreferenceGroup;
    private GroupConnectedBluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private Preference mPreferenceSeeAll;
    private GroupPreferenceCategory mPreferenceGroupOne, mPreferenceGroupTwo, mPreferenceGroupThree,
             mPreferenceGroupFour, mPreferenceGroupFive, mPreferenceGroupSix, mPreferenceGroupSeven,
             mPreferenceGroupEight, mPreferenceGroupNine,mPreferenceGroupRemaining;
    private ArrayList< GroupPreferenceCategory> mGroupList =
            new ArrayList<GroupPreferenceCategory>();
    private GroupUtils mGroupUtils;
    private Context mPerfCtx;

    public GroupConnectedBluetoothDevicesController(Context context) {
        super(context, KEY_ONE);
    }

    @Override
    public void onStart() {
        mBluetoothDeviceUpdater.registerCallback();
    }

    @Override
    public void onStop() {
        mBluetoothDeviceUpdater.unregisterCallback();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(KEY_GROUP);
        mPreferenceGroupOne = screen.findPreference(KEY_ONE);
        mPreferenceGroupTwo = screen.findPreference(KEY_TWO);
        mPreferenceGroupThree = screen.findPreference(KEY_THREE);
        mPreferenceGroupFour = screen.findPreference(KEY_FOUR);
        mPreferenceGroupFive = screen.findPreference(KEY_FIVE);
        mPreferenceGroupSix = screen.findPreference(KEY_SIX);
        mPreferenceGroupSeven = screen.findPreference(KEY_SEVEN);
        mPreferenceGroupEight= screen.findPreference(KEY_EIGHT);
        mPreferenceGroupNine = screen.findPreference(KEY_NINE);
        mPreferenceGroupRemaining = screen.findPreference(KEY_REMAINING);
        mGroupList.add(mPreferenceGroupOne);
        mGroupList.add(mPreferenceGroupTwo);
        mGroupList.add(mPreferenceGroupThree);
        mGroupList.add(mPreferenceGroupFour);
        mGroupList.add(mPreferenceGroupFive);
        mGroupList.add(mPreferenceGroupSix);
        mGroupList.add(mPreferenceGroupSeven);
        mGroupList.add(mPreferenceGroupEight);
        mGroupList.add(mPreferenceGroupNine);
        mGroupList.add(mPreferenceGroupRemaining);

        if (isAvailable()) {
            final Context context = screen.getContext();
            mGroupUtils = new GroupUtils(context);
            mBluetoothDeviceUpdater.setPrefContext(context);
            mBluetoothDeviceUpdater.forceUpdate();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        final PackageManager packageManager = mContext.getPackageManager();
        return (packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ONE;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        mGroupUtils.addPreference(mGroupList, preference, this);
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mGroupUtils.removePreference(mGroupList, preference);
    }

    public void init(DashboardFragment fragment) {
        mBluetoothDeviceUpdater = new GroupConnectedBluetoothDeviceUpdater(fragment.getContext(),
                fragment, GroupConnectedBluetoothDevicesController.this);
    }

    @Override
    public void onGearClick(GearPreference p) {
        GroupBluetoothSettingsPreference pre =(GroupBluetoothSettingsPreference)p;
        mBluetoothDeviceUpdater.launchgroupOptions(pre);
    }
}
