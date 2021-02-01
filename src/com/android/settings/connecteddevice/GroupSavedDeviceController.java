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

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.GroupBluetoothSettingsPreference;
import com.android.settings.bluetooth.GroupUtils;
import com.android.settings.bluetooth.GroupSavedBluetoothDeviceUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.DockUpdaterFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GearPreference;
import com.android.settings.widget.GearPreference.OnGearClickListener;
import com.android.settings.widget.GroupPreferenceCategory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import android.util.Log;

import java.util.ArrayList;

import com.android.settings.R;

/**
 * Controller to maintain the {@link PreferenceGroup} for all
 * saved group devices. It uses {@link DevicePreferenceCallback}
 * to add/remove {@link Preference}
 */
public class GroupSavedDeviceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop,
        DevicePreferenceCallback , OnGearClickListener {

    private static final String KEY = "group_saved_device_list";
    private static final String KEY_PCG_ONE = "pcg_one";
    private static final String KEY_PCG_TWO = "pcg_two";
    private static final String KEY_PCG_THREE = "pcg_three";
    private static final String KEY_PCG_FOUR = "pcg_four";
    private static final String KEY_PCG_FIVE = "pcg_five";
    private static final String KEY_PCG_SIX = "pcg_six";
    private static final String KEY_PCG_SEVEN = "pcg_seven";
    private static final String KEY_PCG_EIGHT = "pcg_eight";
    private static final String KEY_PCG_NINE = "pcg_nine";
    private static final String KEY_PCG_REMAINING = "pcg_remaining";
    private static final String TAG = "GroupSavedDeviceController";
    private PreferenceGroup mPreferenceGroup;
    private GroupSavedBluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private GroupBluetoothSettingsPreference mGroupPreference;
    private GroupUtils mGroupUtils;
    private ArrayList<GroupPreferenceCategory> mListCategories =
            new ArrayList<GroupPreferenceCategory>();
    GroupPreferenceCategory mPreferenceGroupRemaining;

    public GroupSavedDeviceController(Context context) {
        super(context, KEY);
        DockUpdaterFeatureProvider dockUpdaterFeatureProvider =
        FeatureFactory.getFactory(context).getDockUpdaterFeatureProvider();
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
        mPreferenceGroup = screen.findPreference(KEY);
        mPreferenceGroup.setVisible(false);
        mListCategories.add(screen.findPreference(KEY_PCG_ONE));
        mListCategories.add(screen.findPreference(KEY_PCG_TWO));
        mListCategories.add(screen.findPreference(KEY_PCG_THREE));
        mListCategories.add(screen.findPreference(KEY_PCG_FOUR));
        mListCategories.add(screen.findPreference(KEY_PCG_FIVE));
        mListCategories.add(screen.findPreference(KEY_PCG_SIX));
        mListCategories.add(screen.findPreference(KEY_PCG_SEVEN));
        mListCategories.add(screen.findPreference(KEY_PCG_EIGHT));
        mListCategories.add(screen.findPreference(KEY_PCG_NINE));
        mPreferenceGroupRemaining = screen.findPreference(KEY_PCG_REMAINING);
        mPreferenceGroupRemaining.setGroupId(-99);
        mListCategories.add(mPreferenceGroupRemaining);

        if (isAvailable()) {
            final Context context = screen.getContext();
            mGroupUtils = new GroupUtils(context);
            mBluetoothDeviceUpdater.setPrefContext(context);
            mBluetoothDeviceUpdater.forceUpdate();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
        ? AVAILABLE
        : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        mGroupUtils.addPreference(mListCategories, preference, this);
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mGroupUtils.removePreference(mListCategories, preference);
    }

    public void init(DashboardFragment fragment) {
        mBluetoothDeviceUpdater = new GroupSavedBluetoothDeviceUpdater(fragment.getContext(),
        fragment, GroupSavedDeviceController.this);
    }

    @Override
    public void onGearClick(GearPreference p) {
        GroupBluetoothSettingsPreference pre = (GroupBluetoothSettingsPreference) p;
        mBluetoothDeviceUpdater.launchgroupOptions(pre);
    }
}
