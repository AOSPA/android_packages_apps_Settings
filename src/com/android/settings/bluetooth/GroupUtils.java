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

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.widget.GearPreference.OnGearClickListener;
import com.android.settings.widget.GroupPreferenceCategory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.DeviceGroupClientProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settings.R;

import com.android.settings.core.SubSettingLauncher;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.DeviceGroup;
import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.os.Bundle;

/**
 * GroupUtils is a helper class that contains various Group UI specific
 * methods for add preference , remove preference, and group actions
 */
public class GroupUtils {

    public static final String TAG_GROUP = "Group";
    private static final String TAG = "GroupUtilss";
    private static final boolean D = ConnectedDeviceDashboardFragment.DBG_GROUP;
    private static final boolean V = Log.isLoggable(TAG, Log.VERBOSE);
    private Context mCtx;
    private CachedBluetoothDeviceManager mCacheDeviceNamanger;
    private DeviceGroupClientProfile mGroupClientProfile;
    private DeviceGroup mDeviceGroup;
    private static final int INVALID_SIZE = 0;
    private static final int INVALID_GROUPID = -1;
    public static final int GROUP_START_VAL = 1;
    private static final String PROPERTY_GROUP = "persist.vendor.service.bt.adv_audio_mask";
    private boolean mIsGroupEnabled = false;

    protected LocalBluetoothProfileManager mProfileManager;
    private LocalBluetoothManager mLocalBluetoothManager;
    private  LocalBluetoothProfile mBCProfile = null;
    private static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String KEY_GROUP_OP = "group_op";
    /*
     * Returns whether if the device is group device.
     */
    boolean isGroupDevice(CachedBluetoothDevice cachedDevice) {
        if (!mIsGroupEnabled) {
            if (D) {
                loge(" GroupProfile not enabled");
            }
            return false;
        }
        if (!cachedDevice.isGroupDevice()) {
            if (cachedDevice.isTypeUnKnown()) {
                int type = cachedDevice.getDevice().getDeviceType();
                updateGroupStatus(cachedDevice, type);
            }
        }
        if (D) {
            Log.d(TAG, "isGroupDevice " + cachedDevice.isGroupDevice() + cachedDevice
                    + " name " + cachedDevice.getName() + " type "  +  cachedDevice.getmType());
        }
        return cachedDevice.isGroupDevice();
    }

    String getGroupTitle(int groupId) {
        return " " + (groupId + GROUP_START_VAL);
    }

    /*
     * Get group id associated with this device
     */
    int getGroupId(CachedBluetoothDevice device) {
        int groupId = device.getGroupId();
        if (groupId == -1) {
            loge(" groupId is -1");
        }
        if (D) {
            Log.d(TAG, "getgroupId " + groupId + " device " + device);
        }
        return groupId;
    }

    private void updateGroupStatus(CachedBluetoothDevice device, int groupId) {
            device.setDeviceType(groupId);
        CachedBluetoothDevice  cacheDevice = mCacheDeviceNamanger.findDevice(device.getDevice());
        if (cacheDevice != null) {
            cacheDevice.setDeviceType(groupId);
            if (D) {
                Log.d(TAG, "updateGroupStatus updated " + device + " " + groupId);
            }
        } else {
            loge("updateGroupStatus failed  " + device + " groupId " + groupId);
        }
    }

    public GroupUtils(Context ctx) {
        mCtx = ctx;
        mCacheDeviceNamanger = Utils.getLocalBtManager(mCtx).getCachedDeviceManager();
        isGroupEnabled();
        mLocalBluetoothManager = Utils.getLocalBtManager(mCtx);
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mProfileManager = mLocalBluetoothManager.getProfileManager();
        mBCProfile = mProfileManager.getBCProfile();
    }

    private int getGroupId(Preference preference) {
        CachedBluetoothDevice dev = null;
        int groupId = -1;
        if (preference instanceof BluetoothDevicePreference) {
            dev = ((BluetoothDevicePreference) preference).getBluetoothDevice();
            groupId = getGroupId(dev);
        }
        if (groupId == -1) {
            loge("group id not found " + dev.getAddress());
        }
        return groupId;
    }

    private boolean isNewGroup(int id, ArrayList< GroupPreferenceCategory> groupList) {
        boolean isNew = true;
        for (int i = 0; i< groupList.size() - 1; i++) {
            GroupPreferenceCategory tempGroup = groupList.get(i);
            if (tempGroup == null) {
                loge("isNewGroup  tempGroup null");
                return false;
            }
            int val = tempGroup.getGroupId();
            if (D) {
                Log.d(TAG, "isNewGroup val " + val + " key " + tempGroup.getKey());
            }
            if (id == val) {
                isNew = false;
                break;
            }
          }
          if (D) {
              Log.d(TAG, "isNewGroup id  " + id + "  val " + isNew);
          }
          return isNew;
    }

    private boolean isAllFilled(int id, ArrayList< GroupPreferenceCategory> groupList) {
        boolean filled = true;
        for (int i = 0; i< groupList.size() - 1; i++) {
            GroupPreferenceCategory tempGroup = groupList.get(i);
            if (tempGroup == null) {
                loge("isAllFilled");
                return false;
            }
            int val = tempGroup.getGroupId();
            if (id == val) {
                filled = false;
                break;
            }
          }
          return filled;
    }


    private GroupPreferenceCategory getParentGroup(ArrayList< GroupPreferenceCategory> groupList,
            Preference preference) {
        GroupPreferenceCategory group = null;
        for (int i = 0; i< groupList.size() - 1; i++) {
            GroupPreferenceCategory tempGroup = groupList.get(i);
            if (tempGroup.getPreferenceCount() == 0) {
                group = tempGroup;
                break;
            }
        }
        return group;
    }

    private GroupPreferenceCategory getExistingGroup(ArrayList< GroupPreferenceCategory> mGroupList,
            Preference preference) {
        GroupPreferenceCategory group = null;
        int groupId = getGroupId(preference);
        for (GroupPreferenceCategory tempGroup : mGroupList) {
            int val = tempGroup.getGroupId();
            if (groupId == val) {
                group = tempGroup;
                break;
            }
        }
        return group;
    }

    private GroupBluetoothSettingsPreference getHedaer(int groupId,
            OnGearClickListener listener ) {
        GroupBluetoothSettingsPreference headerPreference =
                new GroupBluetoothSettingsPreference(mCtx, groupId);
        headerPreference.setOnGearClickListener(listener);
        return headerPreference;
    }

    /*
     * Add preference based on below conditions
     * a) Get group id based on Preference
     * b) Check if already group header is present or not
     * c) If new group create header
     * d) Get available GroupPreferenceCategory instance
     * e) Add Header and preference
     * f) If group already present get group based on id
     * g) Add preference to existing header
     */
    public void addPreference(ArrayList< GroupPreferenceCategory> listCategories,
            Preference preference, OnGearClickListener listener) {
        int groupId = getGroupId(preference);
        if (groupId == -1) {
            loge("addPreference groupId is not valid "+ groupId);
            return;
        }
        boolean isNewGroup = isNewGroup(groupId, listCategories);
        if (D) {
            Log.d(TAG, "addPreference  " + preference + " isNewGroup " + isNewGroup);
        }
        String key ;
        GroupPreferenceCategory group = null;
        if (isNewGroup) {
            GroupBluetoothSettingsPreference header = getHedaer(groupId, listener);
            group = getParentGroup(listCategories, preference);
            if (group == null) {
                loge("getParentGroup not found for groupId " + groupId);
                isAllGroupsFilled(listCategories, header);
                return;
            }
            group.setGroupId(groupId);
            group.addPreference(header);
            group.addPreference(preference);
            group.setVisible(true);
        } else {
            group = getExistingGroup(listCategories, preference);
            if (group == null) {
                loge("getExistingGroup not found for groupId " + groupId);
                return;
            }
            group.addPreference(preference);
        }
        if (D) {
            Log.d(TAG , "addPreference  key " + group.getKey());
        }
    }

    /*
     * Remove preference based on below conditions
     * a) Get existing Preference Category
     * b) Remove preference from category
     * c) If preference count is one remove header
     */
    public void removePreference(ArrayList< GroupPreferenceCategory> listCategories,
            Preference preference) {
        GroupPreferenceCategory group = getExistingGroup(listCategories, preference);
        if (group == null) {
            loge("removePreference group null ");
            removePreference(listCategories.get(listCategories.size()-1), preference);
            return;
        }
        group.removePreference(preference);
        if (group.getPreferenceCount() == 1) {
            group.setGroupId(INVALID_GROUPID);
            group.removeAll();
            group.setVisible(false);
        }
    }

    /*
     * Remove Header if child count is 0
     */
    private void removePreference(GroupPreferenceCategory groupCategory,
            Preference preference) {
        int size = groupCategory.getPreferenceCount();
        if (size == 0) {
            loge("removePreference Header invalid");
            return;
        }
        int groupId = getGroupId(preference);
        if (groupId == INVALID_GROUPID) {
            loge("removePreference Header groupId is invalid");
            return;
        }
        for (int i=0;i<size; i++) {
            GroupBluetoothSettingsPreference headerPreference =
                    (GroupBluetoothSettingsPreference) groupCategory.getPreference(i);
            if (D) {
                Log.d(TAG, "removePreference Header headerPreference "
                    + headerPreference + " header id "
                    + headerPreference.getGroupId() + " groupId " + groupId );
            }
            if (headerPreference.getGroupId() == groupId) {
                int chCount = headerPreference.decrementChildCount();
                if (D) {
                    Log.d(TAG,"removePreference Header group id  chCount " + chCount );
                }
                if (chCount <= 0) {
                    groupCategory.removePreference(headerPreference);
                }
            }
        }
    }

    /*
	* If more than nine groups add headers only
	*/
    private void isAllGroupsFilled(ArrayList<GroupPreferenceCategory> listCategories,
            GroupBluetoothSettingsPreference preference) {
        boolean isFilled = isAllFilled(-1, listCategories);
        GroupPreferenceCategory group = listCategories.get(listCategories.size() - 1);
        if (isFilled) {
            if (group == null) {
                loge("isAllGroupsFilled received invalid group");
                return;
            }
            if (!group.getKey().contains("remaining")) {
                loge("isAllGroupsFilled not last group");
                return;
            }
            int size = group.getPreferenceCount();
            if (D) {
                Log.d(TAG, "isAllGroupsFilled size " + size);
            }
            boolean found = false;
            for (int i = 0; i < size; i++) {
                if (group.getPreference(i) instanceof GroupBluetoothSettingsPreference) {
                    GroupBluetoothSettingsPreference pref =
                            (GroupBluetoothSettingsPreference )group.getPreference(i);
                    if (preference.getGroupId() == pref.getGroupId()) {
                        found = true;
                        int chCount = pref.incrementChildCound();
                        if(D) {
                            Log.d(TAG, "isAllGroupsFilled updated chCount " + chCount);
                        }
                        break;
                    }
                }
            }
            if (!found) {
                int chCount = preference.incrementChildCound();
                group.addPreference(preference);
                if (D) {
                    Log.d(TAG, "isAllGroupsFilled added chCount " + chCount);
                }
            }
        }
    }

    ArrayList<CachedBluetoothDevice> getCahcedDevice(int groupId) {
        Collection<CachedBluetoothDevice> cachedDevices =
                mCacheDeviceNamanger.getCachedDevicesCopy();
        ArrayList<CachedBluetoothDevice> list = new ArrayList<CachedBluetoothDevice>();
        for (CachedBluetoothDevice cachedDevice : cachedDevices) {
            if (cachedDevice != null && isGroupDeviceBonded(cachedDevice)
                    && getGroupId(cachedDevice) == groupId) {
                list.add(cachedDevice);
            }
        }
        if (D) {
            Log.d(TAG, "getCahcedDevice " + groupId + " list " + list + " " + list.size());
        }
        return list;
    }

    public BluetoothDevice getAnyBCConnectedDevice (int groupId) {
        BluetoothDevice bcMemberDevice = null;

        mDeviceGroup = mGroupClientProfile.getGroup(groupId);
        if (mDeviceGroup == null) {
            Log.e(TAG, "getAnyBCConnectedDevice: dGrp is null");
            return null;
        }
        if (mBCProfile == null) {
             Log.e(TAG, "getAnyBCConnectedDevice: BCProfile is null");
             return null;
        }
        List<BluetoothDevice> setMembers = mDeviceGroup.getDeviceGroupMembers();
        for (BluetoothDevice dev : setMembers) {
            if (mBCProfile.getConnectionStatus(dev) == BluetoothProfile.STATE_CONNECTED) {
               bcMemberDevice = dev;
               break;
            }
        }
        return bcMemberDevice;
     }
     void launchAddSourceGroup(int groupId) {
          Class<?> SADetail = null;
          try {
            SADetail = Class.forName("com.android.settings.bluetooth.BluetoothSADetail");
          } catch (ClassNotFoundException ex) {
            Log.e(TAG, "no SADetail exists");
            SADetail = null;
          }
          if (SADetail != null) {
              BluetoothDevice bcMemberDevice = getAnyBCConnectedDevice(groupId);
              final Bundle args = new Bundle();
              if (bcMemberDevice == null) {
                  //do nothing
                  return;
              }
              args.putString(KEY_DEVICE_ADDRESS,
                     bcMemberDevice.getAddress());
              args.putShort(KEY_GROUP_OP,
                     (short)1);

              new SubSettingLauncher(mCtx)
                    .setDestination("com.android.settings.bluetooth.BluetoothSADetail")
                    .setArguments(args)
                    .setTitleRes(R.string.bluetooth_search_broadcasters)
                    .setSourceMetricsCategory(SettingsEnums.BLUETOOTH_DEVICE_PICKER)
                     .launch();
          }
          return;
    }

    boolean connectGroup(int groupId) {
        if (isValid()) {
            return mGroupClientProfile.connectGroup(groupId);
        }
        return false;
    }

    boolean disconnectGroup(int groupId) {
        if (isValid()) {
            return mGroupClientProfile.disconnectGroup(groupId);
        }
        return false;
    }

    boolean forgetGroup(int groupId) {
        if (isValid()) {
            return mGroupClientProfile.forgetGroup(groupId);
        }
        return false;
    }

    boolean isGroupDiscoveryInProgress(int groupId) {
        if (!isValid()) {
            return false;
        }
        return mGroupClientProfile.isGroupDiscoveryInProgress(groupId);
    }

    boolean startGroupDiscovery(int groupId) {
        if (!isValid()) {
            return false;
        }
        boolean isDiscovering= mGroupClientProfile.isGroupDiscoveryInProgress(groupId);
        if (D) {
            Log.d(TAG, "startGroupDiscovery " + groupId + "isDiscovering " + isDiscovering);
        }
        if (!isDiscovering) {
            mGroupClientProfile.startGroupDiscovery(groupId);
            return true;
        }
        return false;
    }

    boolean stopGroupDiscovery(int groupId) {
        if (!isValid()) {
            return false;
        }
        boolean isDiscovering = mGroupClientProfile.isGroupDiscoveryInProgress(groupId);
        if (D) {
            Log.d(TAG, "stopGroupDiscovery " + groupId + "isDiscovering " + isDiscovering);
        }
        if (isDiscovering) {
            mGroupClientProfile.stopGroupDiscovery(groupId);
            return true;
        }
        return false;
    }

    int getGroupSize(int groupId) {
        int size = INVALID_SIZE;
        if (isValid()) {
            mDeviceGroup = mGroupClientProfile.getGroup(groupId);
            if (mDeviceGroup != null) {
                size = mDeviceGroup.getDeviceGroupSize();
            }
        }
        if (D) {
            Log.d(TAG, "getDeviceGroupSize size " + size);
        }
        return size;
    }

    public boolean isHidePCGGroups() {
        boolean isHide = true;
        List<BluetoothDevice> bluetoothDevices =
                BluetoothAdapter.getDefaultAdapter().getMostRecentlyConnectedDevices();
        if (bluetoothDevices != null && bluetoothDevices.size() > 0) {
            for (BluetoothDevice device : bluetoothDevices) {
                CachedBluetoothDevice cachedDevice = mCacheDeviceNamanger.findDevice(device);
                if (cachedDevice != null && isGroupDeviceBondedOnly(cachedDevice)) {
                    isHide = false;
                    break;
                }
            }
        }
        if (D) {
            Log.d(TAG, "isHidePCGGroups " + isHide);
        }
        return isHide;
    }

    boolean isHideGroupOptions(int groupId) {
        boolean isHide = true;
        Collection<CachedBluetoothDevice> cachedDevices =
                mCacheDeviceNamanger.getCachedDevicesCopy();
        if (cachedDevices != null && cachedDevices.size() > 0) {
            for (CachedBluetoothDevice cachedDevice : cachedDevices) {
                if (cachedDevice != null && isGroupDeviceBonded(cachedDevice)
                    && isGroupIdMatch(cachedDevice, groupId)) {
                    isHide = false;
                    break;
                }
            }
        }
        if (D) {
            Log.d(TAG, "isHideGroupOptions " + isHide);
        }
        return isHide;
    }

    private boolean isGroupDeviceBondedOnly(CachedBluetoothDevice cachedDevice) {
        if (cachedDevice.getBondState() == BluetoothDevice.BOND_BONDED
            && !cachedDevice.isConnected() && isGroupDevice(cachedDevice)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isGroupIdMatch(CachedBluetoothDevice cachedDevice, int groupId) {
        return groupId == getGroupId(cachedDevice);
    }

    private boolean isGroupDeviceBonded(CachedBluetoothDevice cachedDevice) {
        if (cachedDevice.getBondState() == BluetoothDevice.BOND_BONDED
            && isGroupDevice(cachedDevice)) {
            return true;
        } else {
            return false;
        }
    }

    private void loge(String msg) {
        Log.e(TAG, msg);
    }

    private boolean isValid() {
        if (mGroupClientProfile == null) {
            LocalBluetoothProfileManager profilemanager =
                    Utils.getLocalBtManager(mCtx).getProfileManager();
            mGroupClientProfile = profilemanager.getDeviceGroupClientProfile();
        }
        return (mGroupClientProfile == null) ? false : true;
    }

    private void isGroupEnabled() {
        try {
            int advAudioFeatureMask = SystemProperties.getInt(PROPERTY_GROUP, 0);
            if (D) {
                Log.d(TAG,"isGroupEnabled advAudioFeatureMask " + advAudioFeatureMask);
            }
            if (advAudioFeatureMask != 0) {
                mIsGroupEnabled = true;
            }
        } catch (Exception e) {
            mIsGroupEnabled = false;
            Log.e(TAG, "isGroupEnabled " + e);
        }
    }

    boolean isUpdate(int groupId, CachedBluetoothDevice cachedDevice) {
        return (isGroupDevice(cachedDevice) && groupId == getGroupId(cachedDevice));
    }

    boolean addDevice(ArrayList<CachedBluetoothDevice> deviceList, int groupId,
            CachedBluetoothDevice cachedDevice) {
        boolean add = true;
        boolean isAdded = false;
        if (isUpdate(groupId, cachedDevice)) {
            for (CachedBluetoothDevice device : deviceList) {
                if (device.getAddress().equals(cachedDevice.getAddress())) {
                    add = false;
                    break;
                }
            }
            if (add) {
                isAdded = deviceList.add(cachedDevice);
            }
        }
        if (D) {
            Log.d(TAG, "addDevice cachedDevice " + cachedDevice + " name " +
                cachedDevice.getName() + " is added " + isAdded);
        }
        return add;
    }

    boolean removeDevice(ArrayList<CachedBluetoothDevice> deviceList, int groupId,
            CachedBluetoothDevice cachedDevice) {
        boolean remove = false;
        boolean isremoved = false;
        CachedBluetoothDevice removedDevice = null ;
        if (isUpdate(groupId, cachedDevice)) {
            for (CachedBluetoothDevice device : deviceList) {
                if (device.getAddress().equals(cachedDevice.getAddress())) {
                    remove = true;
                    removedDevice = device;
                    break;
                }
            }
            if (remove) {
                isremoved = deviceList.remove(removedDevice);
            }
        }
        if (D) {
            Log.d(TAG, "removeDevice cachedDevice " + cachedDevice + " name " +
                    cachedDevice.getName() + " isremoved " + isremoved);
        }
        return remove;
    }
}
