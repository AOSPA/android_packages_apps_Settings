/*
 *Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *Not a contribution
 */

/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothVcp;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VcpProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.bluetooth.HeadsetProfile;
import android.bluetooth.BluetoothHeadset;
import java.lang.Class;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Class for preference controller that handles BADeviceVolumePreference
 */
public class BADeviceVolumeController extends
        SliderPreferenceController implements CachedBluetoothDevice.Callback,
        LifecycleObserver, OnPause, OnResume {

    private static final String TAG = "BADeviceVolumeController";
    public static final int BROADCAST_AUDIO_MASK = 0x02;
    public static final String BLUETOOTH_ADV_AUDIO_MASK_PROP =
            "persist.vendor.service.bt.adv_audio_mask";
    public static final String BLUETOOTH_VCP_FOR_BROADCAST_PROP =
            "persist.vendor.service.bt.vcpForBroadcast";
    private static final String KEY_BA_DEVICE_VOLUME = "ba_device_volume";
    private static final String VCACHED_DEVICE_CLASS =
            "com.android.settingslib.bluetooth.VendorCachedBluetoothDevice";

    protected BADeviceVolumePreference mPreference;
    private CachedBluetoothDevice mCachedDevice;
    protected LocalBluetoothProfileManager mProfileManager;
    private LocalBluetoothManager mLocalBluetoothManager;
    private VcpProfile mVcpProfile = null;
    private boolean mIsVcpForBroadcastSupported = false;
    private HeadsetProfile mHeadsetProfile;
    private Class<?> mVCachedDeviceClass = null;
    private Object mVendorCachedDevice = null;
    @VisibleForTesting
    AudioManager mAudioManager;

    public BADeviceVolumeController(Context context) {
        super(context, KEY_BA_DEVICE_VOLUME);
        int advAudioMask = SystemProperties.getInt(BLUETOOTH_ADV_AUDIO_MASK_PROP, 0);
        mIsVcpForBroadcastSupported =
                (((advAudioMask & BROADCAST_AUDIO_MASK) == BROADCAST_AUDIO_MASK) &&
                SystemProperties.getBoolean(BLUETOOTH_VCP_FOR_BROADCAST_PROP, false));
        Log.d(TAG, "mIsVcpForBroadcastSupported: " + mIsVcpForBroadcastSupported);
    }

    @Override
    public int getAvailabilityStatus() {
        Log.d(TAG, "getAvailabilityStatus");
        if(mIsVcpForBroadcastSupported) {
            return AVAILABLE;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_BA_DEVICE_VOLUME);
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = screen.findPreference(getPreferenceKey());
            if (mAudioManager != null) {
                mPreference.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                mPreference.setMin(mAudioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC));
                refresh();
            } else {
                mPreference.setVisible(false);
            }
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        if (mCachedDevice != null) {
            mCachedDevice.unregisterCallback(this);
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        if (mCachedDevice != null) {
            mCachedDevice.registerCallback(this);
            refresh();
        }
    }

    public void init(DashboardFragment fragment, LocalBluetoothManager manager,
            CachedBluetoothDevice device) {
        Log.d(TAG, "Init");
        if (mIsVcpForBroadcastSupported) {
            mCachedDevice = device;
            mLocalBluetoothManager = manager;
            mProfileManager = mLocalBluetoothManager.getProfileManager();
            mVcpProfile = mProfileManager.getVcpProfile();
            mAudioManager = mContext.getSystemService(AudioManager.class);

            try {
                mVCachedDeviceClass = Class.forName(VCACHED_DEVICE_CLASS);
                Class[] arg = new Class[2];
                arg[0] = CachedBluetoothDevice.class;
                arg[1] = LocalBluetoothProfileManager.class;
                Method getVendorCachedBluetoothDevice = mVCachedDeviceClass.getDeclaredMethod(
                                        "getVendorCachedBluetoothDevice", arg);
                mVendorCachedDevice = (Object)getVendorCachedBluetoothDevice.invoke(
                                        null, mCachedDevice, mProfileManager);
            } catch (ClassNotFoundException | NoSuchMethodException
                     | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            mHeadsetProfile = mProfileManager.getHeadsetProfile();
        }
    }

    protected void refresh() {
        Log.d(TAG, "refresh");
        if (!mIsVcpForBroadcastSupported || mVcpProfile == null) {
            Log.d(TAG, "VCP for broadcast is not supported");
            return;
        }
        boolean showSlider = enableSlider();
        BluetoothDevice device = mCachedDevice.getDevice();
        int audioState = mHeadsetProfile.getAudioState(device);
        boolean inCall = (audioState == BluetoothHeadset.STATE_AUDIO_CONNECTING ||
                          audioState == BluetoothHeadset.STATE_AUDIO_CONNECTED);
        Log.d(TAG,"VCP refresh showSlider: " + showSlider + " inCall: " + inCall);
        if ((mVcpProfile.getConnectionStatus(device) == BluetoothProfile.STATE_CONNECTED) &&
                ((mVcpProfile.getConnectionMode(device) & BluetoothVcp.MODE_BROADCAST) != 0)) {
             Log.d(TAG, "VCP is connected for broadcast ");
             mPreference.setVisible(true);
             if (!showSlider || inCall) {
                 mPreference.setProgress(0);
                 mPreference.setEnabled(false);
                 return;
             }
             mPreference.setEnabled(true);
             int position = mVcpProfile.getAbsoluteVolume(device);

             if (position != -1) {
                 mPreference.setProgress(position);
             }
        } else {
            mPreference.setVisible(false);
        }
    }

    @Override
    public void onDeviceAttributesChanged() {
        refresh();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BA_DEVICE_VOLUME;
    }

    @Override
    public int getSliderPosition() {
        if (mPreference != null) {
            return mPreference.getProgress();
        }
        if (mVcpProfile != null) {
            return mVcpProfile.getAbsoluteVolume(mCachedDevice.getDevice());
        }
        return 0;
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (mPreference != null) {
            mPreference.setProgress(position);
        }
        if (mVcpProfile != null) {
            mVcpProfile.setAbsoluteVolume(mCachedDevice.getDevice(), position);
            return true;
        }
        return false;
    }

    @Override
    public int getMax() {
        if (mPreference != null) {
            return mPreference.getMax();
        }
        if (mAudioManager != null) {
            return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    @Override
    public int getMin() {
        if (mPreference != null) {
            return mPreference.getMin();
        }
        if (mAudioManager != null) {
            return mAudioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }
    private boolean enableSlider() {
        if (mVCachedDeviceClass == null || mVendorCachedDevice == null) {
            Log.d(TAG,"enableSlider: false");
            return false;
        }

        try {
            Method isBroadcastAudioSynced =
                    mVCachedDeviceClass.getDeclaredMethod("isBroadcastAudioSynced");
            Boolean ret = (Boolean)isBroadcastAudioSynced.invoke(mVendorCachedDevice);
            Log.d(TAG,"enableSlider: " + ret);
            return ret;
        } catch(IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.i(TAG, "Exception" + e);
        }

        Log.d(TAG,"enableSlider: false");
        return false;
    }
}

