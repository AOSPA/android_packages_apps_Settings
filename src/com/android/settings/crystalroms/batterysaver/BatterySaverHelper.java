/*
 * Copyright (C) 2014 The CrystalPA Project
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

package com.android.settings.crystalroms.batterysaver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.PhoneConstants;

import java.util.Calendar;

public class BatterySaverHelper {

    private final static String TAG = "BatterySaverHelper";

    private static final String SCHEDULE_BATTERY_SAVER =
            "org.omnirom.omnigears.batterysaver.SCHEDULE_BATTERY_SAVER";

    private static final int FULL_DAY = 1440; // 1440 minutes in a day

    // Pending intent to start/stop service
    private static PendingIntent makeServiceIntent(Context context,
            String action, int requestCode) {
        Intent intent = new Intent(context, BatterySaverReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static boolean deviceSupportsBluetooth() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }

    public static boolean deviceSupportsMobileData(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean deviceSupportsGps(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    public static boolean deviceSupportsLteCdma(Context context) {
        final TelephonyManager tm =
            (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return (tm.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE);
    }

    public static boolean deviceSupportsLteGsm(Context context) {
        final TelephonyManager tm =
            (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return (tm.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) || (tm.getLteOnGsmMode() != 0);
    }

    public static boolean deviceSupportsVibrator(Context ctx) {
        Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        return vibrator.hasVibrator();
    }

    public static boolean deviceSupportsLed(Context ctx) {
        boolean notifLed = ctx.getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveNotificationLed);
        boolean chargeLed = ctx.getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveBatteryLed);
        return (notifLed || chargeLed);
    }

    public static void setBatterySaverActive(Context context, int value) {
        final ContentResolver resolver = context.getContentResolver();
        Settings.Global.putInt(resolver,
                     Settings.Global.BATTERY_SAVER_OPTION, value);
    }

    public static void scheduleService(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        final int batterySaverActive = Settings.Global.getInt(resolver,
                Settings.Global.BATTERY_SAVER_OPTION, 0);
        final boolean batterySaverEnabled = batterySaverActive != 0;
        final boolean batterySaverStopped = batterySaverActive == 2;
        final int batterySaverStart = Settings.Global.getInt(resolver,
                Settings.Global.BATTERY_SAVER_START, 0);
        final int batterySaverEnd = Settings.Global.getInt(resolver,
                Settings.Global.BATTERY_SAVER_END, 0);
        Intent serviceTriggerIntent = (new Intent())
                   .setClassName("com.android.systemui", "com.android.systemui.batterysaver.BatterySaverService");
        PendingIntent startIntent = makeServiceIntent(context, SCHEDULE_BATTERY_SAVER, 1);
        PendingIntent stopIntent = makeServiceIntent(context, SCHEDULE_BATTERY_SAVER, 2);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        am.cancel(startIntent);
        am.cancel(stopIntent);

        if (!batterySaverEnabled) {
            context.stopService(serviceTriggerIntent);
            return;
        }

        if (batterySaverStart == batterySaverEnd) {
            // 24 hours, start without stop
            if (batterySaverStopped) {
                setBatterySaverActive(context, 1);
            }
            context.startService(serviceTriggerIntent);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        int currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        boolean inBatterySaver = false;
        // time from now on (in minutes) when the service start/stop should be scheduled
        int serviceStartMinutes = -1, serviceStopMinutes = -1;

        if (batterySaverEnd < batterySaverStart) {
            // Starts at night, ends in the morning.
            if (currentMinutes >= batterySaverStart) {
                inBatterySaver = true;
                serviceStopMinutes = FULL_DAY - currentMinutes + batterySaverEnd;
            } else if (currentMinutes <= batterySaverEnd) {
                inBatterySaver = true;
                serviceStopMinutes = batterySaverEnd - currentMinutes;
            } else {
                inBatterySaver = false;
                serviceStartMinutes = batterySaverStart - currentMinutes;
                serviceStopMinutes = FULL_DAY - currentMinutes + batterySaverEnd;
            }
        } else {
            // Starts in the morning, ends at night.
            if (currentMinutes >= batterySaverStart && currentMinutes <= batterySaverEnd) {
                inBatterySaver = true;
                serviceStopMinutes = batterySaverEnd - currentMinutes;
            } else {
                inBatterySaver = false;
                if (currentMinutes <= batterySaverStart) {
                    serviceStartMinutes = batterySaverStart - currentMinutes;
                    serviceStopMinutes = batterySaverEnd - currentMinutes;
                } else {
                    serviceStartMinutes = FULL_DAY - currentMinutes + batterySaverStart;
                    serviceStopMinutes = FULL_DAY - currentMinutes + batterySaverEnd;
                }
            }
        }

        if (inBatterySaver) {
            if (batterySaverStopped) {
                setBatterySaverActive(context, 1);
            }
            context.startService(serviceTriggerIntent);
        } else if (!batterySaverStopped) {
            context.stopService(serviceTriggerIntent);
            setBatterySaverActive(context, 2);
        }

        if (serviceStartMinutes >= 0) {
            // Start service a minute early
            serviceStartMinutes--;
            calendar.add(Calendar.MINUTE, serviceStartMinutes);
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), startIntent);
            calendar.add(Calendar.MINUTE, -serviceStartMinutes);
        }

        if (serviceStopMinutes >= 0) {
            // Stop service a minute late
            serviceStopMinutes++;
            calendar.add(Calendar.MINUTE, serviceStopMinutes);
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), stopIntent);
            calendar.add(Calendar.MINUTE, -serviceStopMinutes);
        }
    }
}
