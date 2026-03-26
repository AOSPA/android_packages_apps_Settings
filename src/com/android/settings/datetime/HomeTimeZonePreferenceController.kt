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

package com.android.settings.datetime

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.timezone.flags.Flags
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.datetime.ZoneGetter
import java.util.Calendar
import java.util.TimeZone

class HomeTimeZonePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    override fun getSummary(): CharSequence {
        val now = Calendar.getInstance()
        val homeTimeZone =
            Settings.Global.getString(mContext.contentResolver, Settings.Global.HOME_TIME_ZONE_ID)

        return if (!TextUtils.isEmpty(homeTimeZone)) {
            ZoneGetter.getTimeZoneOffsetAndName(
                mContext,
                TimeZone.getTimeZone(homeTimeZone),
                now.time,
            )
        } else {
            mContext.getString(R.string.home_time_zone_summary)
        }
    }

    override fun getAvailabilityStatus(): Int {
        return if (Flags.enableHomeTimeZoneApi()) AVAILABLE else UNSUPPORTED_ON_DEVICE
    }
}
