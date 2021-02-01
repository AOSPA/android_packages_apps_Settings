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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.GroupSavedBluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.overlay.FeatureFactory;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import android.util.Log;

/**
 * Controller to maintain the {@link PreferenceGroup} for saved group devices.
 * It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}
 */
public class GroupPreviouslyConnectedDevicePreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop, DevicePreferenceCallback, BluetoothCallback {

    private static final String TAG = "GroupPreviouslyConnectedDevicePreferenceController";
    private PreferenceGroup mPreferenceGroup;
    private GroupSavedBluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private int mPreferenceSize;
    private static final int MAX_DEVICE_NUM = 3;
    private DockUpdater mSavedDockUpdater;
    private LocalBluetoothAdapter mLocalAdapter;
    private LocalBluetoothManager manager;


    public GroupPreviouslyConnectedDevicePreferenceController(Context context,
        String preferenceKey) {
        super(context, preferenceKey);
        mSavedDockUpdater = FeatureFactory.getFactory(context).
            getDockUpdaterFeatureProvider().getSavedDockUpdater(context, this);
        manager = Utils.getLocalBtManager(context);
        if ( manager != null) {
            mLocalAdapter = manager.getBluetoothAdapter();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        )
        ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
        mPreferenceGroup.setVisible(false);
        if (isAvailable()) {
            final Context context = screen.getContext();
            mBluetoothDeviceUpdater.setPrefContext(context);
           mSavedDockUpdater.setPreferenceContext(context);
        }
    }

    @Override
    public void onStart() {
        mBluetoothDeviceUpdater.registerCallback();
      mSavedDockUpdater.registerCallback();
    }

    @Override
    public void onStop() {
        mBluetoothDeviceUpdater.unregisterCallback();
       mSavedDockUpdater.unregisterCallback();
    }

    public void init(DashboardFragment fragment) {
        mBluetoothDeviceUpdater = new GroupSavedBluetoothDeviceUpdater(fragment.getContext(),
                fragment, GroupPreviouslyConnectedDevicePreferenceController.this);
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        mPreferenceSize++;
        if (mPreferenceSize <= MAX_DEVICE_NUM) {
            mPreferenceGroup.addPreference(preference);
        }
        updatePreferenceVisiblity();
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceSize--;
        mPreferenceGroup.removePreference(preference);
        updatePreferenceVisiblity();
    }

    void updatePreferenceVisiblity() {
        if ((mLocalAdapter != null) && (mLocalAdapter.getBluetoothState()
                == BluetoothAdapter.STATE_ON)) {
            mPreferenceGroup.setVisible(mPreferenceSize > 0);
        } else {
            mPreferenceGroup.setVisible(false);
        }
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        updatePreferenceVisiblity();
    }
}
