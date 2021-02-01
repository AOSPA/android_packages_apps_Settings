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
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;

/**
 * Controller to maintain connected devices based on group
 */
public class GroupConnectedBluetoothDeviceUpdater extends GroupBluetoothDeviceUpdater {

    private static final String TAG = "GroupConnectedBluetoothDeviceUpdater";
    private static final String PREF_KEY = "connected_group_bt";

    public GroupConnectedBluetoothDeviceUpdater(Context context, DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback) {
        super(context, fragment, devicePreferenceCallback);
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        boolean isFilterMatched = isDeviceConnected(cachedDevice) && isGroupDevice(cachedDevice);
        if (DBG) {
            Log.d(TAG, "isFilterMatched cachedDevice " + cachedDevice.getName()
                    + " addr " + cachedDevice.getAddress() + " isConnected "
                    + isDeviceConnected(cachedDevice) + " isFilterMatched " + isFilterMatched);
        }
        return isFilterMatched;
    }

    /**
     * Force to update the list of bluetooth devices
     */
    public void forceUpdate() {
        if (mLocalManager == null) {
            Log.e(TAG, "forceUpdate() Bluetooth is not supported on this device");
            return;
        }
        final Collection<CachedBluetoothDevice> cachedDevices =
                mLocalManager.getCachedDeviceManager().getCachedDevicesCopy();
        final CachedBluetoothDeviceManager cachedManager =
                mLocalManager.getCachedDeviceManager();
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            for (CachedBluetoothDevice cachedBluetoothDevice : cachedDevices) {
                update(cachedBluetoothDevice);
            }
        } else {
            removeAllDevicesFromPreference();
            removePreferenceIfNecessary(cachedDevices, cachedManager);
        }
    }

    private void removePreferenceIfNecessary(Collection<CachedBluetoothDevice> bluetoothDevices,
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
                            Log.w(TAG, " removePreferenceIfNecessary " + e);
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    @Override
    protected String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    protected void addPreference(CachedBluetoothDevice cachedDevice,
            @BluetoothDevicePreference.SortType int type) {
        final BluetoothDevice device = cachedDevice.getDevice();
        if (!mPreferenceMap.containsKey(device)) {
            final BluetoothDevicePreference btPreference =
                    new BluetoothDevicePreference(mPrefContext, cachedDevice,
                            true /* showDeviceWithoutNames */,
                            type, true /*hide summary */);
            btPreference.setOnGearClickListener(mDeviceProfilesListener);
            mPreferenceMap.put(device, btPreference);
            mDevicePreferenceCallback.onDeviceAdded(btPreference);
        }
    }
}
