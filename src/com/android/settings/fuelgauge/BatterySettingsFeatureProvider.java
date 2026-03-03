/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;

import java.util.List;

/** Feature provider for battery settings usage. */
public interface BatterySettingsFeatureProvider {

    /** Returns {@code true} if manufacture date should be shown */
    boolean isManufactureDateAvailable(Context context, long manufactureDateMs);

    /** Returns {@code true} if first use date should be shown */
    boolean isFirstUseDateAvailable(Context context, long firstUseDateMs);

    /** Checks whether the battery information page is enabled in the About phone page */
    boolean isBatteryInfoEnabled(Context context);

    /** A way to add more battery tip detectors. */
    void addBatteryTipDetector(
            Context context,
            List<BatteryTip> batteryTips,
            BatteryInfo batteryInfo,
            BatteryTipPolicy batteryTipPolicy);

    /** Returns a label for the bottom summary during wireless charging. */
    @Nullable
    CharSequence getWirelessChargingLabel(@NonNull Context context, @NonNull BatteryInfo info);

    /** Returns a content description for the bottom summary during wireless charging. */
    @Nullable
    CharSequence getWirelessChargingContentDescription(
            @NonNull Context context, @NonNull BatteryInfo info);

    /** Returns a charging remaining time label for wireless charging. */
    @Nullable
    CharSequence getWirelessChargingRemainingLabel(
            @NonNull Context context, long remainingTimeMs, long currentTimeMs);

    /** Returns {@code true} if it's in the charging optimization mode. */
    boolean isChargingOptimizationMode(@NonNull Context context, boolean isLongLife);

    /** Returns a charging remaining time label for charging optimization mode. */
    @Nullable
    CharSequence getChargingOptimizationRemainingLabel(
            @NonNull Context context,
            int batteryLevel,
            int pluggedStatus,
            long chargeRemainingTimeMs,
            long currentTimeMs);

    /** Returns a charge label for charging optimization mode. */
    @Nullable
    CharSequence getChargingOptimizationChargeLabel(
            @NonNull Context context,
            int batteryLevel,
            String batteryPercentageString,
            long chargeRemainingTimeMs,
            long currentTimeMs);

    /**
     * Returns {@code true} if the device is currently forcing a full charge, ignoring any
     * optimization or limits that might usually stop charging before 100%.
     */
    boolean isForceFullCharge(@NonNull Context context);

    /** Returns a label for forcing a full charge. */
    @Nullable
    CharSequence getForceFullChargeLabel(@NonNull Context context);

    /** Returns the URI to monitor for battery status changes. */
    @Nullable
    Uri getBatteryStatusUri();

    /** Returns the battery status label; return null if built-in status should be used. */
    @Nullable
    CharSequence getBatteryStatusLabel(@NonNull Context context, @NonNull BatteryInfo info);

    /** Returns the battery status short label; return null if built-in status should be used. */
    @Nullable
    CharSequence getBatteryStatusShortLabel(@NonNull Context context, @NonNull BatteryInfo info);
}
