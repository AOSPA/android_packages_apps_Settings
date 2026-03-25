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

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.telephony.TelephonyManager.EXTRA_SUBSCRIPTION_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.UserManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class NetworkChangeNotificationTest {
    private static final int SUB_ID = 2;
    private final CharSequence mFakeNotificationChannelTitle = "fake_notification_channel_title";
    private final CharSequence mFakeNotificationTitle = "fake_notification_title";
    private final String mFakeNotificationSummary = "fake_notification_Summary";
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private UserManager mUserManager;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Spy
    private Resources mResources = mContext.getResources();
    private NetworkChangeNotification mNetworkChangeNotification = new NetworkChangeNotification();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.isMainUser()).thenReturn(true);
        when(mContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(
                mNotificationManager);
        when(mContext.getSystemService(NotificationManager.class)).thenReturn(mNotificationManager);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)).thenReturn(
                mSubscriptionManager);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getText(R.string.network_protection_2g_on_notification_title)).thenReturn(
                mFakeNotificationTitle);
        when(mResources.getText(
                R.string.network_protection_2g_notification_channel_title)).thenReturn(
                mFakeNotificationChannelTitle);
        when(mResources.getString(
                R.string.network_protection_2g_on_notification_summary)).thenReturn(
                mFakeNotificationSummary);
    }

    @Test
    public void onReceive_withRandomAction() {
        Intent intent = mock(Intent.class);
        when(mUserManager.isMainUser()).thenReturn(false);

        mNetworkChangeNotification.onReceive(mContext, intent);

        verify(intent, never()).getAction();
    }

    @Test
    public void onReceive_with2gDisabledByCarrierAction_invalidSubId() {
        Intent intent = new Intent(TelephonyManager.ACTION_2G_DISABLED_BY_CARRIER);
        intent.putExtra(EXTRA_SUBSCRIPTION_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mNetworkChangeNotification.onReceive(mContext, intent);

        verify(mSubscriptionManager, never()).isActiveSubscriptionId(SUB_ID);
    }

    @Test
    public void onReceive_with2gDisabledByCarrierAction_inActiveSubscriptionId() {
        Intent intent = new Intent(TelephonyManager.ACTION_2G_DISABLED_BY_CARRIER);
        intent.putExtra(EXTRA_SUBSCRIPTION_ID, SUB_ID);

        mNetworkChangeNotification.onReceive(mContext, intent);

        verify(mContext, never()).getSystemService(TelephonyManager.class);
    }

    @Test
    public void onReceive_with2gDisabledByCarrierAction_not2gSupported() {
        Intent intent = new Intent(TelephonyManager.ACTION_2G_DISABLED_BY_CARRIER);
        intent.putExtra(EXTRA_SUBSCRIPTION_ID, SUB_ID);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getSupportedRadioAccessFamily()).thenReturn(
                TelephonyManager.NETWORK_TYPE_BITMASK_LTE);

        mNetworkChangeNotification.onReceive(mContext, intent);

        verify(mTelephonyManager, never()).isRadioInterfaceCapabilitySupported(any());
    }

    @Test
    public void onReceive_with2gDisabledByCarrierAction_notRadioInterfaceCapabilitySupported() {
        Intent intent = new Intent(TelephonyManager.ACTION_2G_DISABLED_BY_CARRIER);
        intent.putExtra(EXTRA_SUBSCRIPTION_ID, SUB_ID);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getSupportedRadioAccessFamily()).thenReturn(
                TelephonyManager.NETWORK_TYPE_BITMASK_GPRS);
        when(mTelephonyManager.isRadioInterfaceCapabilitySupported(any())).thenReturn(false);

        mNetworkChangeNotification.onReceive(mContext, intent);

        verify(mContext, never()).getResources();
    }

    @Test
    public void onReceive_with2gDisabledByCarrierAction_notificationShouldSend() {
        Intent intent = new Intent(TelephonyManager.ACTION_2G_DISABLED_BY_CARRIER);
        intent.putExtra(EXTRA_SUBSCRIPTION_ID, SUB_ID);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getSupportedRadioAccessFamily()).thenReturn(
                TelephonyManager.NETWORK_TYPE_BITMASK_GPRS);
        when(mTelephonyManager.isRadioInterfaceCapabilitySupported(any())).thenReturn(true);
        when(mSubscriptionManager.isActiveSubscriptionId(SUB_ID)).thenReturn(true);

        mNetworkChangeNotification.onReceive(mContext, intent);

        // Capture the notification channel created and verify its fields.
        ArgumentCaptor<NotificationChannel> nc = ArgumentCaptor.forClass(NotificationChannel.class);
        verify(mNotificationManager).createNotificationChannel(nc.capture());

        assertThat(nc.getValue().getId()).isEqualTo(
                mNetworkChangeNotification.NETWORK_PROTECTION_2G_NOTIFICATION_CHANNEL);
        assertThat(nc.getValue().getName().toString()).isEqualTo(
                mResources.getText(R.string.network_protection_2g_notification_channel_title)
                          .toString());
        assertThat(nc.getValue().getImportance()).isEqualTo(IMPORTANCE_HIGH);

        // Capture the notification it notifies and verify its fields.
        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(mNotificationManager).notify(
                eq(mNetworkChangeNotification.NETWORK_PROTECTION_2G_NOTIFICATION_ID + SUB_ID),
                notification.capture());
        assertThat(notification.getValue().extras.getCharSequence(Notification.EXTRA_TITLE)
                                          .toString()).isEqualTo(mFakeNotificationTitle.toString());
        assertThat(notification.getValue().extras.getCharSequence(
                Notification.EXTRA_BIG_TEXT).toString()).isEqualTo(mFakeNotificationSummary);
        assertThat(notification.getValue().contentIntent).isNotNull();
    }
}
