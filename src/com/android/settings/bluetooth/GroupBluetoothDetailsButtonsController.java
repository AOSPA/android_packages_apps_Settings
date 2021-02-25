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
import android.bluetooth.BluetoothDeviceGroup;
import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.List;

import com.android.settings.R;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.widget.GroupOptionsPreference;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
    * This class adds Group action buttons: one to connect/disconnect from a device
    * (depending on the current connected state of all devices in this group ), and
    * one to "forget" (ie unpair) the device and one for to refresh devices.
*/
public class GroupBluetoothDetailsButtonsController extends GroupBluetoothDetailsController {

    private static final boolean DBG = ConnectedDeviceDashboardFragment.DBG_GROUP;
    private static String TAG = "GroupBluetoothDetailsButtonsController";
    private static final String KEY_GROUP_OPTIONS = "group_options";
    private boolean mConnectButtonInitialized;
    private GroupOptionsPreference mGroupOptions;
    private boolean mIsUpdate = false;
    private int mGroupId;
    private GroupUtils mGroupUtils;
    private int mGroupSize = -1;
    private int mDiscoveredSize = 0;
    private boolean isRefreshClicked = false;
    private ArrayList<CachedBluetoothDevice> mDevicesList = new ArrayList<CachedBluetoothDevice>();


    public GroupBluetoothDetailsButtonsController(Context context,
            PreferenceFragmentCompat fragment, int  groupId, Lifecycle lifecycle) {
        super(context, fragment, groupId, lifecycle);
        mGroupId = groupId;
        mGroupUtils = new GroupUtils(context);
        mGroupSize = mGroupUtils.getGroupSize(mGroupId);
    }

    @Override
    protected void init(PreferenceScreen screen) {
        if (DBG) {
            Log.d(TAG, "init ");
        }
        mGroupOptions = ((GroupOptionsPreference)
                screen.findPreference(getPreferenceKey()));
        mGroupOptions.setTextViewText(mContext.getString(R.string.group_id)
                + mGroupUtils.getGroupTitle(mGroupId));
        mGroupOptions.setForgetButtonText(R.string.forget_group);
        mGroupOptions.setForgetButtonOnClickListener((View) -> onForgetButtonPressed());
        mGroupOptions.setForgetButtonEnabled(true);
        mGroupOptions.setTexStatusText(R.string.active);
        mGroupOptions.setConnectButtonText(R.string.connect_group);
        mGroupOptions.setConnectButtonOnClickListener((View) -> onConnectButtonPressed());
        mGroupOptions.setDisconnectButtonText(R.string.disconnect_group);
        mGroupOptions.setDisconnectButtonOnClickListener((View) -> onDisConnectButtonPressed());
        mGroupOptions.setCancelRefreshButtonText(R.string.cancel_refresh_group);
        mGroupOptions.setCancelRefreshButtonOnClickListener(
                (View) -> onCacelRefreshButtonPressed());
        mGroupOptions.setCancelRefreshButtonVisible(false);
        mGroupOptions.setRefreshButtonText(R.string.refresh_group);
        mGroupOptions.setRefreshButtonOnClickListener((View) -> onRefreshButtonPressed());
        mGroupOptions.setRefreshButtonVisible(false);
        mIsUpdate = true;

        BluetoothDevice bcMemberDevice = mGroupUtils.getAnyBCConnectedDevice(mGroupId);
        boolean enableASButton = false;
        if (bcMemberDevice != null) {
            enableASButton = true;
        }
        mGroupOptions.setAddSourceGroupButtonText(R.string.add_source_group);
        mGroupOptions.setAddSourceGroupButtonEnabled(enableASButton);
        mGroupOptions.setAddSourceGroupButtonVisible(enableASButton);
        if (enableASButton) {
            mGroupOptions.setAddSourceGroupButtonOnClickListener((View) ->
            onAddSourceGroupButtonPressed());
        }
    }

    private void onAddSourceGroupButtonPressed() {
        mGroupUtils.launchAddSourceGroup(mGroupId);
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        if (DBG) {
            Log.d(TAG, "onConnectionStateChanged cachedDevice "+cachedDevice +" state "+state);
        }
        if (mGroupUtils.isUpdate(mGroupId, cachedDevice)) {
            refresh();
        }
    }

    @Override
    public void onProfileConnectionStateChanged(CachedBluetoothDevice cachedDevice,
            int state, int bluetoothProfile) {
        if (DBG) {
            Log.d(TAG, "onProfileConnectionStateChanged cachedDevice " + cachedDevice
                    + " state " + state);
        }
        if (mGroupUtils.isUpdate(mGroupId, cachedDevice)) {
            refresh();
        }
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        boolean isUpdated = false;
        if (bondState == BluetoothDevice.BOND_BONDED) {
            isUpdated = mGroupUtils.addDevice(mDevicesList, mGroupId, cachedDevice);
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            isUpdated = mGroupUtils.removeDevice(mDevicesList, mGroupId, cachedDevice);
        }
        if (isUpdated) {
            mDiscoveredSize = mDevicesList.size();
        }
        if (DBG) {
            Log.d(TAG, "onDeviceBondStateChanged cachedDevice " + cachedDevice
                    + " name " + cachedDevice.getName() + " bondState " + bondState
                    +" isUpdated " + isUpdated + " mDiscoveredSize " + mDiscoveredSize);
        }
        if (isUpdated) {
            updateProgressScan();
            refresh();
        }
    }
    @Override
    public void onStop() {
        if (DBG) {
            Log.d(TAG, "onStop ");
        }
        super.onStop();
        disableScanning();
    }

    @Override
    public void onGroupDiscoveryStatusChanged (int groupId, int status, int reason) {
        if (DBG) {
            Log.d(TAG, "onSetDiscoveryStatusChanged " + groupId + " status :" + status
                    + " Reason :" + reason);
        }
        if (groupId == mGroupId) {
            if (isRefreshClicked && status == BluetoothDeviceGroup.GROUP_DISCOVERY_STOPPED ) {
                isRefreshClicked = false;
            }
            updateProgressScan();
        }
    }

    @Override
    protected void loadDevices() {
        mDevicesList = mGroupUtils.getCahcedDevice(mGroupId);
        mDiscoveredSize = mDevicesList.size();
        if (DBG) {
            Log.d(TAG, "loadDevices mGroupId " + mGroupId + " mGroupSize " + mGroupSize
                    + " mDiscoveredSize " + mDiscoveredSize);
        }
        updateProgressScan();
    }

    @Override
    protected void refresh() {
        boolean isBusy = false;
        boolean showConnect = false;
        boolean showDisconnect = false;
        boolean isActive = false;
        List<CachedBluetoothDevice> devicesList = new ArrayList<>(mDevicesList);
        if (DBG) {
            Log.d(TAG, "updateFlags list " + devicesList + " size " + devicesList.size());
        }
        for (CachedBluetoothDevice cacheDevice : devicesList) {
            if (DBG) {
                Log.d(TAG, "refresh cacheDevice " + cacheDevice + " connected "
                     + cacheDevice.isConnected() +" busy " + cacheDevice.isBusy());
            }
            if (!isBusy && cacheDevice.isBusy()) {
                isBusy = true;
                mIsUpdate = true;
            }
            if (!showDisconnect && cacheDevice.isConnected()) {
                showDisconnect = true;
                mIsUpdate = true;
            } else if (!showConnect && !cacheDevice.isConnected()) {
                showConnect = true;
                mIsUpdate = true;
            } if((!isActive) && cacheDevice.isConnected()
                    && (cacheDevice.isActiveDevice(BluetoothProfile.A2DP)
                    || cacheDevice.isActiveDevice(BluetoothProfile.HEADSET)
                    || cacheDevice.isActiveDevice(BluetoothProfile.HEARING_AID))) {
                isActive = true;
            }
        }
        if (DBG) {
            Log.d(TAG, "refresh isBusy " + isBusy + " showConnect " + showConnect
                    + " showDisconnect :" + showDisconnect +" isActive " + isActive
                    + " mIsUpdate " + mIsUpdate);
        }
        if (!mIsUpdate) {
            return;
        }
        mGroupOptions.setConnectButtonEnabled(!isBusy);
        mGroupOptions.setDisconnectButtonEnabled(!isBusy);
        mGroupOptions.setRefreshButtonEnabled(!isBusy);
        mGroupOptions.setCancelRefreshButtonEnabled(!isBusy);
        mGroupOptions.setDisconnectButtonEnabled(showDisconnect);
        mGroupOptions.setDisconnectButtonVisible(showDisconnect);
        mGroupOptions.setConnectButtonEnabled(showConnect);
        mGroupOptions.setConnectButtonVisible(showConnect);
        mGroupOptions.setTvStatusVisible(isActive);
        mIsUpdate = false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_GROUP_OPTIONS;
    }

    private void onForgetButtonPressed() {
        if (DBG) {
            Log.d(TAG, "onForgetButtonPressed");
        }
        GroupForgetDialogFragment fragment = GroupForgetDialogFragment.newInstance(mGroupId);
        fragment.show(mFragment.getFragmentManager(), GroupForgetDialogFragment.TAG);
    }

    private void onConnectButtonPressed() {
        disableScanning();
        boolean connect = mGroupUtils.connectGroup(mGroupId);
        if (DBG) {
            Log.d(TAG, "onConnectButtonPressed connect " + connect);
        }
    }

    private void onDisConnectButtonPressed() {
        disableScanning();
        boolean disconnect =  mGroupUtils.disconnectGroup(mGroupId);
        if (DBG) {
            Log.d(TAG, "onDisConnectButtonPressed disconnect " + disconnect);
        }
    }

    private void onRefreshButtonPressed() {
        isRefreshClicked = mGroupUtils.startGroupDiscovery(mGroupId);
        if (DBG) {
            Log.d(TAG, "onRefreshButtonPressed isRefreshClicked " + isRefreshClicked);
        }

    }

    private void onCacelRefreshButtonPressed() {
        isRefreshClicked = false;
        boolean stopDiscovery = mGroupUtils.stopGroupDiscovery(mGroupId);
        if (DBG) {
            Log.d(TAG, "onCacelRefreshButtonPressed stopDiscovery " + stopDiscovery);
        }
    }

    private void updateProgressScan() {
        boolean showRefresh = false;
        if ((mGroupSize >0) && (mGroupSize > mDiscoveredSize)) {
            showRefresh = true;
        }
        boolean isRefreshing = mGroupUtils.isGroupDiscoveryInProgress(mGroupId);
        if (showRefresh) {
            if (isRefreshing) {
                mGroupOptions.setProgressScanVisible(true);
                mGroupOptions.setRefreshButtonVisible(false);
                mGroupOptions.setCancelRefreshButtonVisible(true);
            } else {
                mGroupOptions.setProgressScanVisible(false);
                mGroupOptions.setRefreshButtonVisible(true);
                mGroupOptions.setCancelRefreshButtonVisible(false);
            }
        } else {
            mGroupOptions.setProgressScanVisible(showRefresh);
            mGroupOptions.setCancelRefreshButtonVisible(showRefresh);
            mGroupOptions.setRefreshButtonVisible(showRefresh);
       }
       if (DBG) {
           Log.d(TAG, "updateProgressScan showRefresh " + showRefresh
                   + ", isRefreshing " + isRefreshing + " mDiscoveredSize " + mDiscoveredSize);
       }
    }

    private void disableScanning () {
        if (isRefreshClicked) {
            mGroupUtils.stopGroupDiscovery(mGroupId);
        }
    }
}
