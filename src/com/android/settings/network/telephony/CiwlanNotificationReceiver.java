/*
 * Copyright (C) 2014 The Android Open Source Project
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

/* Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2022-2023 Qualcomm Innovation Center, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted (subject to the limitations in the
 * disclaimer below) provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *
 *   * Neither the name of Qualcomm Innovation Center, Inc. nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE
 * GRANTED BY THIS LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.settings.network.telephony;

import static android.provider.Settings.ACTION_NETWORK_OPERATOR_SETTINGS;
import static android.provider.Settings.EXTRA_SUB_ID;

import android.R.drawable;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;

/**
 * Displays a notification that allows users to disable C_IWLAN
 */
public class CiwlanNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "CiwlanNotificationReceiver";

    private static final String CIWLAN_EXIT_NOTIFICATION_STATUS = "CIWLAN_EXIT_NOTIFICATION_STATUS";
    private static final String CIWLAN_EXIT_NOTIFICATION_PHONE_ID =
            "CIWLAN_EXIT_NOTIFICATION_PHONE_ID";
    private static final String CIWLAN_EXIT_NOTIFICATION_CHANNEL_ID =
            "CIWLAN_EXIT_NOTIFICATION_CHANNEL_ID";

    private static final String ACTION_DISABLE_CIWLAN_NOTIFICATION =
            "com.qti.phone.action.ACTION_DISABLE_CIWLAN_NOTIFICATION";
    private static final String ACTION_RADIO_POWER_STATE_CHANGED =
            "org.codeaurora.intent.action.RADIO_POWER_STATE";
    private static final String EXTRA_STATE = "state";

    private static final int MAX_NUM_PHONES = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case ACTION_DISABLE_CIWLAN_NOTIFICATION:
                boolean show = intent.getBooleanExtra(CIWLAN_EXIT_NOTIFICATION_STATUS, false);
                int phoneId = intent.getIntExtra(CIWLAN_EXIT_NOTIFICATION_PHONE_ID,
                        SubscriptionManager.INVALID_PHONE_INDEX);
                if (show && SubscriptionManager.getSubscriptionId(phoneId) !=
                        SubscriptionManager.getDefaultDataSubscriptionId()) {
                    Log.d(TAG, "Notification not supported for nDDS, ignoring...");
                    return;
                }
                Log.d(TAG, "ACTION_DISABLE_CIWLAN_NOTIFICATION: show = " + show + ", phoneId = " +
                        phoneId);
                toggleNotification(show, phoneId, context.getApplicationContext());
                break;
            case ACTION_RADIO_POWER_STATE_CHANGED:
                int radioStateExtra = intent.getIntExtra(EXTRA_STATE,
                        TelephonyManager.RADIO_POWER_UNAVAILABLE);
                handleRadioPowerStateChanged(context.getApplicationContext(), radioStateExtra);
                break;
            default:
                Log.e(TAG, "Unsupported action");
        }
    }

    private void handleRadioPowerStateChanged(Context context, int radioState) {
        // Hide the notification if radio becomes unavailable
        if (radioState == TelephonyManager.RADIO_POWER_UNAVAILABLE) {
            Log.d(TAG, "Cancelling all notifications");
            for (int phoneId = 0; phoneId < MAX_NUM_PHONES; phoneId++) {
                dismissNotification(context, phoneId);
            }
        }
    }

    private void toggleNotification(boolean show, int phoneId, Context context) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            Log.e(TAG, "Invalid phoneId");
            return;
        }
        if (show) {
            showNotification(context, phoneId);
        } else {
            dismissNotification(context, phoneId);
        }
    }

    private void showNotification(Context context, int phoneId) {
        Log.d(TAG, "showNotification phoneId: " + phoneId);
        NotificationManager notificationMgr = context.getSystemService(NotificationManager.class);
        createNotificationChannel(notificationMgr, context);
        // Build the positive button that launches the UI to disable C_IWLAN
        int subId = SubscriptionManager.getSubscriptionId(phoneId);
        Intent ciwlanIntent = new Intent(ACTION_NETWORK_OPERATOR_SETTINGS).putExtra(
                EXTRA_SUB_ID, subId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, phoneId, ciwlanIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action ciwlanSettingAction = new NotificationCompat.Action.Builder(0,
                context.getString(R.string.ciwlan_exit_notification_positive_button),
                pendingIntent).build();

        // Build the negative button that dismisses the notification
        Intent dismissIntent = new Intent(ACTION_DISABLE_CIWLAN_NOTIFICATION);
        dismissIntent.putExtra(CIWLAN_EXIT_NOTIFICATION_STATUS, false);
        dismissIntent.putExtra(CIWLAN_EXIT_NOTIFICATION_PHONE_ID, phoneId);
        dismissIntent.setComponent(new ComponentName(context.getPackageName(),
                CiwlanNotificationReceiver.class.getName()));
        // For the 2nd parameter, the requestCode, we are passing in the phoneId to differentiate
        // between the pending intents for the different subs. If we pass the same number for this
        // parameter, the extras for the latest pending intent will override the previous one.
        pendingIntent = PendingIntent.getBroadcast(context, phoneId, dismissIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action dismissAction = new NotificationCompat.Action.Builder(0,
                context.getString(R.string.ciwlan_exit_notification_negative_button),
                pendingIntent).build();
        String title = context.getString(R.string.backup_calling_settings_title) +
                context.getString(R.string.ciwlan_exit_notification_title,
                        SubscriptionUtil.getUniqueSubscriptionDisplayName(subId, context));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                CIWLAN_EXIT_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(context.getString(
                        R.string.ciwlan_exit_notification_description))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setOngoing(true)
                .addAction(ciwlanSettingAction)
                .addAction(dismissAction);

        // The 2nd argument to notify, the notification id, will be used to differentiate between
        // the notifications for different subs in the multi-sim case
        notificationMgr.notify(TAG + phoneId, phoneId, builder.build());
    }

    static void dismissNotification(Context context, int phoneId) {
        Log.d(TAG, "dismissNotification phoneId: " + phoneId);
        NotificationManager notificationMgr = context.getSystemService(NotificationManager.class);
        notificationMgr.cancel(TAG + phoneId, phoneId);
    }

    private void createNotificationChannel(NotificationManager notificationMgr, Context context) {
        CharSequence name = context.getString(R.string.ciwlan_channel_name);
        NotificationChannel channel = new NotificationChannel(CIWLAN_EXIT_NOTIFICATION_CHANNEL_ID,
                name, NotificationManager.IMPORTANCE_HIGH);
        notificationMgr.createNotificationChannel(channel);
    }
}