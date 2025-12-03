/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.datetime;

import static android.app.time.Capabilities.CAPABILITY_NOT_ALLOWED;
import static android.app.time.Capabilities.CAPABILITY_NOT_APPLICABLE;
import static android.app.time.Capabilities.CAPABILITY_NOT_SUPPORTED;
import static android.app.time.Capabilities.CAPABILITY_POSSESSED;

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.content.Context;
import android.content.res.Resources;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.time.Instant;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executor;

public class TimeZoneOffsetInfoPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop, TimeManager.TimeZoneDetectorListener {

    private final DateFormat mDateFormat;
    private static final String TAG = "TimeZoneOffsetInfoSettings";
    private final TimeManager mTimeManager;
    private @Nullable TimeZoneCapabilitiesAndConfig mTimeZoneCapabilitiesAndConfig;

    public TimeZoneOffsetInfoPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
        mDateFormat = DateFormat.getDateInstance(SimpleDateFormat.LONG);
        mDateFormat.setContext(DisplayContext.CAPITALIZATION_NONE);
        mTimeManager = context.getSystemService(TimeManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!android.timezone.flags.Flags.enableTimeZoneOffsetChangeBroadcast()
                && !android.timezone.flags.Flags.timeZoneOffsetChangeNotifications()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        TimeZoneCapabilities timeZoneCapabilities =
                getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ false).getCapabilities();
        int capability =
                timeZoneCapabilities
                        .getConfigureTimeZoneOffsetChangeNotificationsEnabledCapability();

        // The preference can be present and enabled, present and disabled or not present.
        if (capability == CAPABILITY_NOT_SUPPORTED || capability == CAPABILITY_NOT_ALLOWED) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (capability == CAPABILITY_NOT_APPLICABLE || capability == CAPABILITY_POSSESSED) {
            return DISABLED_DEPENDENT_SETTING;
        } else {
            Log.e(TAG, "Unknown capability=" + capability);
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public CharSequence getSummary() {
        TimeZone timeZone = TimeZone.getDefault();
        ZoneOffsetTransition nextDstTransition = findNextDstTransition(timeZone);

        if (nextDstTransition == null) {
            return mContext.getString(R.string.footer_time_zone_offset_change_no_next_change);
        }

        final Calendar transitionTime = Calendar.getInstance();
        transitionTime.setTimeInMillis(nextDstTransition.getInstant().toEpochMilli());

        return mDateFormat.format(transitionTime);
    }


    private ZoneOffsetTransition findNextDstTransition(TimeZone timeZone) {
        ZoneRules zoneRules = timeZone.toZoneId().getRules();

        ZoneOffsetTransition transition = zoneRules.nextTransition(Instant.now());

        return transition;
    }

    @Override
    public void onStart() {
        // Register for updates to the user's time zone capabilities or configuration which could
        // require UI changes.
        Executor mainExecutor = mContext.getMainExecutor();
        mTimeManager.addTimeZoneDetectorListener(mainExecutor, this);
        // Setup the initial state.
        refreshUi();
    }

    @Override
    public void onStop() {
        mTimeManager.removeTimeZoneDetectorListener(this);
    }

    /**
     * Implementation of {@link TimeManager.TimeZoneDetectorListener#onChange()}. Called by the
     * system server after a change that affects {@link TimeZoneCapabilitiesAndConfig}.
     */
    @Override
    public void onChange() {
        refreshUi();
    }

    private void refreshUi() {
        // Force a refresh of cached user capabilities and config.
        getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ true);
    }

    /**
     * Returns the current user capabilities and configuration. {@code forceRefresh} can be {@code
     * true} to discard any cached copy.
     */
    private TimeZoneCapabilitiesAndConfig getTimeZoneCapabilitiesAndConfig(boolean forceRefresh) {
        if (forceRefresh || mTimeZoneCapabilitiesAndConfig == null) {
            mTimeZoneCapabilitiesAndConfig = mTimeManager.getTimeZoneCapabilitiesAndConfig();
        }
        return mTimeZoneCapabilitiesAndConfig;
    }
}
