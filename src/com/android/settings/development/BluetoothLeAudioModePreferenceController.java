/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.development;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.SystemProperties;
import android.sysprop.BluetoothProperties;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Preference controller to control Bluetooth LE audio mode
 */
public class BluetoothLeAudioModePreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bluetooth_leaudio_mode";
    private static final String BROADCAST_MODE_VALUE = "broadcast";

    static final String LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY =
            "persist.bluetooth.leaudio_dynamic_switcher.mode";

    @Nullable private final Fragment mFragment;

    @VisibleForTesting
    final String[] mListValues;
    private final String[] mListSummaries;
    @VisibleForTesting
    @Nullable String mNewMode;
    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;

    boolean mChanged = false;

    public BluetoothLeAudioModePreferenceController(@NonNull Context context,
            @Nullable Fragment fragment) {
        super(context);
        mFragment = fragment;
        mBluetoothAdapter = context.getSystemService(BluetoothManager.class).getAdapter();

        List<String> listValues = new ArrayList<>(Arrays.asList(
                mContext.getResources().getStringArray(R.array.bluetooth_leaudio_mode_values)));
        List<String> listSummaries = new ArrayList<>(Arrays.asList(
                mContext.getResources().getStringArray(R.array.bluetooth_leaudio_mode)));

        if (!BluetoothProperties.isProfileBapBroadcastSourceEnabled().orElse(false)) {
            int broadcastIndex = listValues.indexOf(BROADCAST_MODE_VALUE);
            if (broadcastIndex >= 0) {
                listValues.remove(broadcastIndex);
                listSummaries.remove(broadcastIndex);
            }
        }

        mListValues = listValues.toArray(new String[0]);
        mListSummaries = listSummaries.toArray(new String[0]);
    }

    @Override
    @NonNull public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean isAvailable() {
        return BluetoothProperties.isProfileBapUnicastClientEnabled().orElse(false);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (mFragment == null) {
            return false;
        }

        BluetoothRebootDialog.show(mFragment);
        mChanged = true;
        mNewMode = newValue.toString();
        return false;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        if (mBluetoothAdapter == null) {
            return;
        }

        String currentValue;
        if (mBluetoothAdapter.isLeAudioBroadcastSourceSupported()
                == BluetoothStatusCodes.FEATURE_SUPPORTED) {
            currentValue = BROADCAST_MODE_VALUE;
        } else if (mBluetoothAdapter.isLeAudioSupported()
                == BluetoothStatusCodes.FEATURE_SUPPORTED) {
            currentValue = "unicast";
        } else {
            currentValue = "disabled";
        }

        int index = 0;
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }

        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setEntries(mListSummaries);
        listPreference.setEntryValues(mListValues);
        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListSummaries[index]);

        if (!mBluetoothAdapter.isEnabled()) {
            listPreference.setEnabled(false);
            return;
        }
    }

    /**
     * Called when the RebootDialog confirm is clicked.
     */
    public void onRebootDialogConfirmed() {
        if (!mChanged) {
            return;
        }
        SystemProperties.set(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mNewMode);
    }

    /**
     * Called when the RebootDialog cancel is clicked.
     */
    public void onRebootDialogCanceled() {
        mChanged = false;
    }
}
