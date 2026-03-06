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

package com.android.settings.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.KeyguardManager;
import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSimProtectionControllerTest {
    @Mock
    protected TelephonyManager mTelephonyManager;
    @Mock
    protected SubscriptionManager mSubscriptionManager;
    @Mock
    protected KeyguardManager mKeyguardManager;
    protected SubscriptionInfo mSubscriptionInfo;
    protected Context mContext;
    protected final int mSubscriptionId = 1234;
    protected List<SubscriptionInfo> mSubscriptionInfoList;

    /**
     * Must be called by the subclass's {@code @Before} method.
     */
    protected void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());

        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(KeyguardManager.class)).thenReturn(mKeyguardManager);

        SubscriptionInfo.Builder builder = new SubscriptionInfo.Builder();
        builder.setDisplayName("Test");
        builder.setCarrierName("fake carrier name");
        builder.setId(mSubscriptionId);
        builder.setSimSlotIndex(1);
        builder.setEmbedded(false);
        mSubscriptionInfo = builder.build();
        mSubscriptionInfoList = new ArrayList<>();
        mSubscriptionInfoList.add(mSubscriptionInfo);
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                mSubscriptionInfoList);
        when(mSubscriptionManager.getActiveSubscriptionInfo(anyInt())).thenReturn(
                mSubscriptionInfo);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                mSubscriptionInfoList);

        when(mSubscriptionManager.setDisplayName(any(), anyInt(), anyInt())).thenReturn(0);
        when(mSubscriptionManager.getEnabledSubscriptionId(eq(1))).thenReturn(mSubscriptionId);
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(
                mTelephonyManager);
        when(mKeyguardManager.isDeviceSecure()).thenReturn(true);
    }
}
