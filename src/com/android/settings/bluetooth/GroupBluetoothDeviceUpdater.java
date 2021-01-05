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
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Update the Group bluetooth devices. It gets bluetooth event from {@link LocalBluetoothManager}
 * using {@link BluetoothCallback}. It notifies the upper level whether to add/remove the
 * preference through {@link DevicePreferenceCallback}
 *
 * In {@link GroupBluetoothDeviceUpdater}, it uses {@link BluetoothDeviceFilter.Filter} to detect
 * whether the {@link CachedBluetoothDevice} is relevant.
 */
public abstract class GroupBluetoothDeviceUpdater extends BluetoothDeviceUpdater {
    private static final String TAG = "GroupBluetoothDeviceUpdater";
    protected static final boolean DBG = ConnectedDeviceDashboardFragment.DBG_GROUP;

    public GroupBluetoothDeviceUpdater(Context context, DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback) {
        super(context, fragment, devicePreferenceCallback);
    }

    @Override
    protected void addPreference(CachedBluetoothDevice cachedDevice) {
        addPreference(cachedDevice, BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    public void launchgroupOptions(Preference preference) {
        if (DBG)
            Log.d(TAG, " launchgroupOptions :" + preference);
        mMetricsFeatureProvider.logClickedPreference(preference, mFragment.getMetricsCategory());
        final GroupBluetoothSettingsPreference pref =
                (GroupBluetoothSettingsPreference) preference;
        final Bundle args = new Bundle();
        args.putInt(GroupBluetoothFragment.KEY_GROUP_ID, pref.getGroupId());
        new SubSettingLauncher(mFragment.getContext()).setDestination
                    (GroupBluetoothFragment.class.getName())
                .setArguments(args).setTitleRes(R.string.group_options)
                .setSourceMetricsCategory(mFragment.getMetricsCategory()).launch();
    }

}
