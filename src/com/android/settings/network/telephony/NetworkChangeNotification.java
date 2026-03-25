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

package com.android.settings.network.telephony;

import static android.telephony.TelephonyManager.EXTRA_SUBSCRIPTION_ID;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.settings.R;

public class NetworkChangeNotification extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeNotification";
    public static final int NETWORK_PROTECTION_2G_NOTIFICATION_ID = 987;
    public static final String NETWORK_PROTECTION_2G_NOTIFICATION_ID_KEY =
            "network_protection_2g_notification_id_key";
    public static final String NETWORK_PROTECTION_2G_NOTIFICATION_CHANNEL =
            "network_protection_2g_notification_channel";
    private static final long BITMASK_2G = TelephonyManager.NETWORK_TYPE_BITMASK_GSM
            | TelephonyManager.NETWORK_TYPE_BITMASK_GPRS
            | TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
            | TelephonyManager.NETWORK_TYPE_BITMASK_CDMA
            | TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT;

    @Override
    public void onReceive(Context context, Intent intent) {
        UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager != null && !userManager.isMainUser()) {
            Log.d(TAG, "The userId is not the main user");
            return;
        }
        String action = intent.getAction();

        if (action == null) {
            Log.w(TAG, "Received unexpected intent with null action.");
            return;
        }

        switch (action) {
            case TelephonyManager.ACTION_2G_DISABLED_BY_CARRIER -> on2gDisabledByCarrierRequest(
                    context, intent);
            default -> Log.w(TAG, "Received unexpected intent " + intent.getAction());
        }
    }

    private void on2gDisabledByCarrierRequest(Context context, Intent intent) {
        // Getting subId from extra.
        int subId = intent.getIntExtra(EXTRA_SUBSCRIPTION_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        if (subId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
            Log.w(TAG, "Did not received or received invalid subscritption id.");
            return;
        }
        SubscriptionManager subscriptionManager =
                context.getSystemService(SubscriptionManager.class);
        if (!subscriptionManager.isActiveSubscriptionId(subId)) {
            Log.w(TAG, "on2gDisabledByCarrierRequest invalid sub ID " + subId);
            return;
        }

        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        if ((telephonyManager.getSupportedRadioAccessFamily() & BITMASK_2G) == 0) {
            Log.w(TAG, "2G unsupported");
            return;
        }
        if (!telephonyManager.isRadioInterfaceCapabilitySupported(
                telephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK)) {
            Log.w(TAG, "Allowed network types bitmask unsupported");
            return;
        }

        CharSequence notificationTitle = context.getResources()
                .getText(R.string.network_protection_2g_on_notification_title);
        CharSequence notificationSummary = context.getResources()
                .getString(R.string.network_protection_2g_on_notification_summary);
        create2gDisabledByCarrierNotification(context, notificationTitle, notificationSummary,
                subId);
    }

    private void create2gDisabledByCarrierNotification(Context context,
                                                       CharSequence notificationTitle,
                                                       CharSequence notificationSummary,
                                                       int subId) {
        final Resources resources = context.getResources();
        int notificationId = NETWORK_PROTECTION_2G_NOTIFICATION_ID + subId;
        NotificationChannel notificationChannel =
                new NotificationChannel(NETWORK_PROTECTION_2G_NOTIFICATION_CHANNEL,
                        resources.getText(
                                R.string.network_protection_2g_notification_channel_title),
                        NotificationManager.IMPORTANCE_HIGH);
        Intent resultIntent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        resultIntent.setPackage(SETTINGS_PACKAGE_NAME);
        resultIntent.putExtra(Settings.EXTRA_SUB_ID, subId);
        resultIntent.putExtra(NETWORK_PROTECTION_2G_NOTIFICATION_ID_KEY, notificationId);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(context,
                NETWORK_PROTECTION_2G_NOTIFICATION_CHANNEL).setSmallIcon(R.drawable.ic_sim_card)
                .setColor(context.getColor(R.color.sim_noitification))
                .setContentTitle(notificationTitle).setContentText(notificationSummary)
                .setStyle(new Notification.BigTextStyle().bigText(notificationSummary)).addAction(
                        new Notification.Action.Builder(R.drawable.ic_sim_card, context.getString(
                                R.string.network_protection_2g_notification_settings_btn),
                                resultPendingIntent).setAuthenticationRequired(true).build())
                .setAutoCancel(true);

        builder.setContentIntent(resultPendingIntent);

        // Notify the notification.
        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.notify(notificationId, builder.build());
        Log.i(TAG, "Notification sended sussusfully with subId: " + subId + "title: "
                + notificationTitle + " description: " + notificationSummary);
    }
}
