/*
 * Copyright 2016-2021 Paranoid Android
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

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class ParanoidAndroidVersionPreferenceController extends BasePreferenceController {

    private static final String AOSPA_BUILD_VARIANT_PROP = "ro.aospa.build.variant";
    private static final String AOSPA_VERSION_MAJOR_PROP = "ro.aospa.version.major";
    private static final String AOSPA_VERSION_MINOR_PROP = "ro.aospa.version.minor";

    private final Context mContext;

    public ParanoidAndroidVersionPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public CharSequence getSummary() {
        String aospaVersionMajor = SystemProperties.get(AOSPA_VERSION_MAJOR_PROP,
                mContext.getResources().getString(R.string.device_info_default));
        String aospaVersionMinor = SystemProperties.get(AOSPA_VERSION_MINOR_PROP,
                mContext.getResources().getString(R.string.device_info_default));
        String aospaBuildVariant = SystemProperties.get(AOSPA_BUILD_VARIANT_PROP,
                mContext.getResources().getString(R.string.device_info_default));

        if (aospaBuildVariant.equals("Release")) {
            return aospaVersionMajor + " " + aospaVersionMinor;
        } else if (aospaBuildVariant.equals("Unofficial")) {
           return aospaVersionMajor + " " + aospaBuildVariant;
        } else {
           return aospaVersionMajor + " " + aospaBuildVariant + " " + aospaVersionMinor;
        }
    }
}
