/*
 * Copyright 2025 The Android Open Source Project
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
package com.android.settings.appfunctions.intents

import com.android.settings.appfunctions.providers.StaticIntent

fun getOtherIntents(): List<StaticIntent> =
    listOf(
        StaticIntent(
            description = "Pair a new Bluetooth device",
            intentUri =
                "intent:#Intent;action=android.settings.BLUETOOTH_PAIRING_SETTINGS;package=com.android.settings;end",
        ),
        StaticIntent(
            description = "Show saved Bluetooth devices",
            intentUri =
                "intent:#Intent;action=com.android.settings.PREVIOUSLY_CONNECTED_DEVICE;package=com.android.settings;end",
        ),
        StaticIntent(
            description = "Configure Cast settings",
            intentUri =
                "intent:#Intent;action=android.settings.CAST_SETTINGS;package=com.android.settings;end",
        ),
        StaticIntent(
            description = "Configure Wallpaper & Style",
            intentUri =
                "intent:#Intent;component=com.android.settings/.wallpaper.StyleSuggestionActivity;end",
        ),
        StaticIntent(
            description = "Configure Notification Conversations",
            intentUri =
                "intent:#Intent;action=android.settings.CONVERSATION_SETTINGS;package=com.android.settings;end",
        ),
        StaticIntent(
            description = "Configure Notification Bubbles",
            intentUri =
                "intent:#Intent;action=android.settings.NOTIFICATION_BUBBLE_SETTINGS;package=com.android.settings;end",
        ),
        StaticIntent(
            description = "Configure Notification read, reply & control",
            intentUri =
                "intent:#Intent;action=android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;package=com.android.settings;end",
        ),
        StaticIntent(
            description = "Show Notification history",
            intentUri =
                "intent:#Intent;action=android.settings.NOTIFICATION_HISTORY;package=com.android.settings;end",
        ),
        StaticIntent(
            description = "Configure Ringtone sound pattern",
            intentUri =
                "intent:#Intent;action=android.intent.action.RINGTONE_PICKER;i.android.intent.extra.ringtone.TYPE=1;end",
        ),
        StaticIntent(
            description = "Configure Notification sound pattern",
            intentUri =
                "intent:#Intent;action=android.intent.action.RINGTONE_PICKER;i.android.intent.extra.ringtone.TYPE=2;end",
        ),
        StaticIntent(
            description = "Manage Unused apps",
            intentUri =
                "intent:#Intent;action=android.intent.action.MANAGE_UNUSED_APPS;package=com.android.settings;end",
        ),
        StaticIntent(
            description = "Configure Backup settings",
            intentUri =
                "intent:#Intent;action=com.android.settings.BACKUP_SETTINGS;package=com.android.settings;end",
        ),
    )
