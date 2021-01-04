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
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

/**
 * Maintain and update saved Group devices(bonded but not connected)
 */
public class GroupSavedBluetoothDeviceUpdater extends GroupBluetoothDeviceUpdater
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "GroupSavedBluetoothDeviceUpdater";
    private static final String PREF_KEY = "saved_group_bt";
    private BluetoothAdapter mBluetoothAdapter;

    public GroupSavedBluetoothDeviceUpdater(Context context, DashboardFragment fragment,
        DevicePreferenceCallback devicePreferenceCallback) {
        super(context, fragment, devicePreferenceCallback);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void forceUpdate() {
        if (mBluetoothAdapter.isEnabled()) {
            final CachedBluetoothDeviceManager cachedManager =
            mLocalManager.getCachedDeviceManager();
            final List<BluetoothDevice> bluetoothDevices =
            mBluetoothAdapter.getMostRecentlyConnectedDevices();
            removePreferenceIfNecessary(bluetoothDevices, cachedManager);
            for(BluetoothDevice device : bluetoothDevices) {
                final CachedBluetoothDevice cachedDevice = cachedManager.findDevice(device);
                if (cachedDevice != null) {
                    update(cachedDevice);
                }
            }
        } else {
            removeAllDevicesFromPreference();
        }
    }

    private void removePreferenceIfNecessary(List<BluetoothDevice> bluetoothDevices,
        CachedBluetoothDeviceManager cachedManager) {
        for (BluetoothDevice device : new ArrayList<>(mPreferenceMap.keySet())) {
            if (!bluetoothDevices.contains(device)) {
                final CachedBluetoothDevice cachedDevice = cachedManager.findDevice(device);
                if (cachedDevice != null) {
                    removePreference(cachedDevice);
                } else if(cachedDevice == null) {
                    try {
                        BluetoothDevicePreference pref = (BluetoothDevicePreference)
                                mPreferenceMap.get(device);
                        final CachedBluetoothDevice cacDev = pref.getBluetoothDevice();
                        if (cacDev != null) {
                            removePreference(cacDev);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "removePreferenceIfNecessary " + e);
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void update(CachedBluetoothDevice cachedDevice) {
        if (isFilterMatched(cachedDevice)) {
            addPreference(cachedDevice, BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        } else {
            removePreference(cachedDevice);
        }
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final BluetoothDevice device = cachedDevice.getDevice();
        if (DBG) {
            Log.d(TAG, " cachedDevice : " + cachedDevice + ", isConnected " + device.isConnected()
                +" isBonded  " + (device.getBondState() == BluetoothDevice.BOND_BONDED));
        }
        return  (device.getBondState() == BluetoothDevice.BOND_BONDED)
                && !device.isConnected() && isGroupDevice(cachedDevice);
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
