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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import java.util.ArrayList;
import java.util.List;

import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.lifecycle.Lifecycle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;

/*
 * Fragment to view Group related components
*/
public class GroupBluetoothFragment extends RestrictedDashboardFragment
        implements BluetoothCallback {

	private static final boolean DBG = ConnectedDeviceDashboardFragment.DBG_GROUP;
    private static final String TAG = "GroupBluetoothFragment";
    static final String KEY_GROUP_ID = "group_id";
    private int mGroupId = -1;
    private Context mCtx;
    private GroupUtils mGroupUtils;
    protected LocalBluetoothManager mLocalManager;

    public GroupBluetoothFragment() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    public static GroupBluetoothFragment newInstance(int groupId) {
        Bundle args = new Bundle(1);
        args.putInt(KEY_GROUP_ID, groupId);
        GroupBluetoothFragment fragment = new GroupBluetoothFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        mGroupId = getArguments().getInt(KEY_GROUP_ID);
        mCtx = context;
        mGroupUtils = new GroupUtils(mCtx);
        mLocalManager = Utils.getLocalBtManager(mCtx);
        mLocalManager.getEventManager().registerCallback(this);
        super.onAttach(context);
        if (mGroupId <= -1) {
            // Close this page if groupId is not valid
            Log.w(TAG, "onAttach mGroupId not valid " + mGroupId);
            finish();
            return;
        }
        use(GroupBluetoothDetailsButtonsController.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        finishFragmentIfNecessary();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLocalManager.getEventManager().unregisterCallback(this);
    }

    void finishFragmentIfNecessary() {
        if (mGroupId < 0 || mGroupUtils.isHideGroupOptions(mGroupId)) {
            Log.w(TAG, "finishFragment");
            finish();
            return;
        }
    }

    @Override
    public int getMetricsCategory() {
        return Instrumentable.METRICS_CATEGORY_UNKNOWN;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_group_details_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        if (DBG) {
            Log.d(TAG, "createPreferenceControllers mGroupId " + mGroupId);
        }
        ArrayList<AbstractPreferenceController> controllers = new ArrayList<>();
        Lifecycle lifecycle = getSettingsLifecycle();
        controllers.add(new GroupBluetoothDetailsButtonsController(context,
                this, mGroupId, lifecycle));
        controllers.add(new GroupBluetoothDevicesAvailableMediaController(
                context, this, lifecycle, mGroupId));
        controllers.add(new GroupBluetoothDevicesConnectedController(
                context, this, lifecycle, mGroupId));
        controllers.add(new GroupBluetoothDevicesBondedController(
                context, this, lifecycle, mGroupId));
        return controllers;
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        if (DBG) {
            Log.d(TAG, "onBluetoothStateChanged bluetoothState " + bluetoothState);
        }
        if (BluetoothAdapter.STATE_TURNING_OFF == bluetoothState) {
            finish();
        }
    }
}
