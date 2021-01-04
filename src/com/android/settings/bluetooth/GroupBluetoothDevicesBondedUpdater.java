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
package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintain and update saved group devices(bonded but not connected)
 */
public class GroupBluetoothDevicesBondedUpdater extends GroupBluetoothDeviceUpdater
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "GroupBluetoothDevicesBondedUpdater";
    private static final String PREF_KEY = "group_options_bonded_devices_updater";
    private int mGroupId;
    private GroupUtils mGroupUtils;

    public GroupBluetoothDevicesBondedUpdater(Context context, DashboardFragment fragment,
        DevicePreferenceCallback devicePreferenceCallback, int groupId) {
        super(context, fragment, devicePreferenceCallback);
        mGroupUtils = new GroupUtils(context);
        mGroupId = groupId;
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final BluetoothDevice device = cachedDevice.getDevice();
        int groupId = -1;
        if (DBG) {
            Log.d(TAG, "isFilterMatched " + cachedDevice + "bond state  " + device.getBondState()
                    + " mGroupId " + mGroupId);
        }
        return (device.getBondState() == BluetoothDevice.BOND_BONDED)
                && !device.isConnected() && isGroupDevice(cachedDevice)
                && mGroupId == mGroupUtils.getGroupId(cachedDevice);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        mMetricsFeatureProvider.logClickedPreference(preference, mFragment.getMetricsCategory());
        final CachedBluetoothDevice device = ((BluetoothDevicePreference) preference)
                .getBluetoothDevice();
        device.connect();
        return true;
    }

    @Override
    protected String getPreferenceKey() {
        return PREF_KEY;
    }
}
