/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.development.bluetooth;

import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog preference controller to set the Bluetooth A2DP config of LHDC quality
 */
public class BluetoothLHDCQualityDialogPreferenceController extends
        AbstractBluetoothDialogPreferenceController {

    // In standard case, low0 is available
    private static final int index_adjust_offset = 0;
    // In case of low0 removed, shift the rest indices
    //private static final int index_adjust_offset = 1;

    private static final String KEY = "bluetooth_select_a2dp_codec_lhdc_playback_quality";
    private static final String TAG = "BtLhdcAudioQualityCtr";
    private static final int DEFAULT_MASK = 0xC000;
    private static final int DEFAULT_TAG = 0x8000;
    private static final int VALUE_MASK = 0xFF;
    //DEFAULT_MAX_INDEX: sync with BluetoothLHDCQualityDialogPreference.java::getDefaultIndex()
    private static final int DEFAULT_MAX_INDEX = (9 - index_adjust_offset);
    private static final int INDEX_HIGH1 = (8 - index_adjust_offset);

    public BluetoothLHDCQualityDialogPreferenceController(Context context, Lifecycle lifecycle,
                                                      BluetoothA2dpConfigStore store) {
        super(context, lifecycle, store);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        ((BaseBluetoothDialogPreference) mPreference).setCallback(this);
    }

    @Override
    protected void writeConfigurationValues(final int index) {
        synchronized (mBluetoothA2dpConfigStore) {
            long codecSpecific1Value = mBluetoothA2dpConfigStore.getCodecSpecific1Value();
            int codecType = mBluetoothA2dpConfigStore.getCodecType();

            if (codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV2 ||
                codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV3 ||
                codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV5) {
                codecSpecific1Value &= ~ DEFAULT_MASK;
                codecSpecific1Value &= ~ VALUE_MASK;
                codecSpecific1Value |= DEFAULT_TAG;
                if (index <= DEFAULT_MAX_INDEX) {
                    codecSpecific1Value |= (index + index_adjust_offset);
                } else {
                    codecSpecific1Value |= DEFAULT_MAX_INDEX;
                }
                mBluetoothA2dpConfigStore.setCodecSpecific1Value(codecSpecific1Value);
            }
        }
    }

    @Override
    protected int getCurrentIndexByConfig(BluetoothCodecConfig config) {
        if (config == null) {
            Log.e(TAG, "Unable to get current config index. Config is null.");
        }
        int index = 0;
        long codecSpecific1Value = config.getCodecSpecific1();
        int codecType = config.getCodecType();
        long featureTag = 0;
        if (codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV2 ||
            codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV3 ||
            codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV5) {
            featureTag = DEFAULT_TAG;
        }
        index = convertCfgToBtnIndex(featureTag, codecSpecific1Value);

        // make a sync from current to storage while get
        synchronized (mBluetoothA2dpConfigStore) {
            mBluetoothA2dpConfigStore.setCodecSpecific1Value(codecSpecific1Value);
        }
        return index;
    }

    @Override
    public List<Integer> getSelectableIndex() {
        List<Integer> selectableIndex = new ArrayList<>();
        final BluetoothCodecConfig currentConfig = getCurrentCodecConfig();
        // All items are available to set from UI but be filtered by native system bt
        for (int i = 0; i <= DEFAULT_MAX_INDEX; i++) {
            if (currentConfig != null &&
               (currentConfig.getCodecType() == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV2 ||
               currentConfig.getCodecType() == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV3) && 
               (i == INDEX_HIGH1)) {
                 // LHDCV2,V3 not support 1000kbps, skip the add
            } else {
                selectableIndex.add(i);
            }
        }
        return selectableIndex;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        // Enable this preference menu when current codec type is LHDC V2/V3/V5. For other cases, disable it.
        final BluetoothCodecConfig currentConfig = getCurrentCodecConfig();
        if (currentConfig != null &&
           (currentConfig.getCodecType() == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV2 ||
           currentConfig.getCodecType() == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV3 ||
           currentConfig.getCodecType() == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LHDCV5)) {
            preference.setEnabled(true);
        } else {
            preference.setEnabled(false);
            //preference.setSummary("");
        }
    }

    @Override
    public void onHDAudioEnabled(boolean enabled) {
        mPreference.setEnabled(false);
    }

    @VisibleForTesting
    int convertCfgToBtnIndex(long tag, long index) {
        int ret = 0;
        long tmp = index & DEFAULT_MASK;
        if (tmp == tag) {
            index &= VALUE_MASK;
        } else {
            index = getDefaultIndex();
        }
        ret = (int)(index - index_adjust_offset);
        if(ret < 0)
        {
            ret = 0;
        }
        return ret;
    }
}
