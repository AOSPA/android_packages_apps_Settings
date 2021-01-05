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

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

/**
 * Controller to maintain connected ( not active)  Group devices
*/

public class GroupBluetoothDevicesConnectedUpdater extends GroupBluetoothGroupDeviceUpdater {

    private static final String TAG = "GroupBluetoothDevicesConnectedUpdater";
    private static final String PREF_KEY = "group_devices_connected";
    private int mGroupId;
    private final AudioManager mAudioManager;
    private GroupUtils mGroupUtils;

    public GroupBluetoothDevicesConnectedUpdater(Context context, DashboardFragment fragment,
        DevicePreferenceCallback devicePreferenceCallback, int groupId) {
        super(context, fragment, devicePreferenceCallback);
        mGroupId = groupId;
        mAudioManager = context.getSystemService(AudioManager.class);
        mGroupUtils = new GroupUtils(context);
    }

    @Override
    public void onAudioModeChanged() {
        if (DBG) {
            Log.d(TAG, "onAudioModeChanged ");
        }
        forceUpdate();
    }

    @Override
    public boolean isFilterMatched(CachedBluetoothDevice cachedDevice) {
        final int audioMode = mAudioManager.getMode();
        final int currentAudioProfile;
        if (audioMode == AudioManager.MODE_RINGTONE || audioMode == AudioManager.MODE_IN_CALL
            || audioMode == AudioManager.MODE_IN_COMMUNICATION) {
            // in phone call
            currentAudioProfile = BluetoothProfile.HEADSET;
            } else {
            // without phone call
            currentAudioProfile = BluetoothProfile.A2DP;
        }
        boolean isFilterMatched = false;
        if (isDeviceConnected(cachedDevice)) {
            if (DBG) {
                Log.d(TAG, "isFilterMatched() current audio profile : " + currentAudioProfile);
            }
            // If device is Hearing Aid, it is compatible with HFP and A2DP.
            // It would not show in Connected Devices group.
            if (cachedDevice.isConnectedHearingAidDevice()) {
                return false;
            }
            // According to the current audio profile type,
            // this page will show the bluetooth device that doesn't have corresponding
            // profile.
            // For example:
            // If current audio profile is a2dp,
            // show the bluetooth device that doesn't have a2dp profile.
            // If current audio profile is headset,
            // show the bluetooth device that doesn't have headset profile.
            switch (currentAudioProfile) {
                case BluetoothProfile.A2DP:
                isFilterMatched = !cachedDevice.isConnectedA2dpDevice();
                break;
                case BluetoothProfile.HEADSET:
                isFilterMatched = !cachedDevice.isConnectedHfpDevice();
                break;
            }
        }
        if (DBG) {
            Log.d(TAG, "isFilterMatche cachedDevice : " + cachedDevice +" name "
                + cachedDevice.getName() + ", isFilterMatched : " + isFilterMatched );
        }

        return isFilterMatched &&  isGroupDevice(cachedDevice)
                && mGroupId == mGroupUtils.getGroupId(cachedDevice);
    }

    @Override
    protected String getPreferenceKey() {
        return PREF_KEY;
    }
}
