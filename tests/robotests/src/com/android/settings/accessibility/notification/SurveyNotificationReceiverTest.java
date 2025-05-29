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

package com.android.settings.accessibility.notification;

import static com.android.internal.accessibility.common.NotificationConstants.ACTION_SURVEY_NOTIFICATION_DISMISSED;
import static com.android.internal.accessibility.common.NotificationConstants.ACTION_SURVEY_NOTIFICATION_SHOWN;
import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.server.accessibility.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowNotificationManager;

/** Tests for {@link SurveyNotificationReceiver}. */
@RunWith(RobolectricTestRunner.class)
public class SurveyNotificationReceiverTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = spy(RuntimeEnvironment.getApplication());
    private SurveyNotificationReceiver mReceiver;
    private ShadowNotificationManager mShadowNotificationManager;

    @Before
    public void setUp() {
        mReceiver = new SurveyNotificationReceiver();
        NotificationManager notificationManager =
                mContext.getSystemService(NotificationManager.class);
        mShadowNotificationManager = shadowOf(notificationManager);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceiveNotificationShown_disableHaTS_notCreatesNotificationChannel() {
        final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName());

        mReceiver.onReceive(mContext, intent);

        assertThat(mShadowNotificationManager.getNotificationChannels()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceiveNotificationShown_withDarkUiSettingsPageId_createAndPostNotification() {
        final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);

        mReceiver.onReceive(mContext, intent);

        assertThat(mShadowNotificationManager.getNotificationChannels()).isNotEmpty();
        assertThat(mShadowNotificationManager.getNotificationChannels().getFirst().getId())
                .isEqualTo(NotificationHelper.NOTIFICATION_CHANNEL_ID);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceiveNotificationShown_withoutPageId_notCreateAndPostNotification() {
        final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName());

        mReceiver.onReceive(mContext, intent);

        assertThat(mShadowNotificationManager.getAllNotifications()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceiveNotificationShown_withUnknownPageId_notCreateAndPostNotification() {
        final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.PAGE_UNKNOWN);

        mReceiver.onReceive(mContext, intent);

        assertThat(mShadowNotificationManager.getAllNotifications()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceiveNotificationDismiss_withDarkUiSettingsPageId_cancelExpectedNotification() {
        final Intent showIntent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);
        mReceiver.onReceive(mContext, showIntent);

        final Intent dismissIntent = new Intent(ACTION_SURVEY_NOTIFICATION_DISMISSED)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);
        mReceiver.onReceive(mContext, dismissIntent);

        assertThat(mShadowNotificationManager.getAllNotifications()).isEmpty();
    }
}
