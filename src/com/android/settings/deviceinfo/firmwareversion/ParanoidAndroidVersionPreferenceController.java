/*
 * Copyright 2016-2019 The ParanoidAndroid Project
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

	private static final String PA_VERSION_PROP = "ro.pa.version";

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
        return SystemProperties.get(PA_VERSION_PROP, 
		        mContext.getResources().getString(R.string.device_info_default));
    }
}
