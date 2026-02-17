/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.media.AudioAttributes;
import android.provider.Settings;

import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingConstants;

/**
 * The controller of the hearing device ringtone and alarm routing switch preference.
 */
public class HearingDeviceRingtoneAlarmRoutingSwitchPreferenceController extends
        HearingDeviceAudioRoutingBaseSwitchPreferenceController {

    public HearingDeviceRingtoneAlarmRoutingSwitchPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected @AudioAttributes.AttributeUsage int[] getAudioAttributeUsages() {
        return HearingAidAudioRoutingConstants.RINGTONE_ROUTING_ATTRIBUTES;
    }
    @Override
    protected void setRoutingValue(Context context, int routingValue) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.HEARING_AID_RINGTONE_ROUTING, routingValue);
    }

    @Override
    protected int getRoutingValue(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.HEARING_AID_RINGTONE_ROUTING,
                HearingAidAudioRoutingConstants.RoutingValue.AUTO);
    }

    @Override
    protected void updatePreferenceSummary(SwitchPreferenceCompat preference,
            boolean isRouteToHearing) {
        if (isRouteToHearing) {
            preference.setSummary(
                    R.string.accessibility_hearing_device_routing_duplicate_both_summary);
        } else {
            preference.setSummary(
                    R.string.accessibility_hearing_device_routing_device_speaker_summary);
        }
    }
}
