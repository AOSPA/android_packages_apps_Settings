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

package com.android.settings.network.telephony;

import android.annotation.Nullable;
import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import java.util.List;

/**
 * A repository for accessing subscription information for telephony feature
 * with application context.
 */
public class SubscriptionSettingsRepository {
    private final Context mContext;
    @Nullable
    private SubscriptionManager mSubscriptionManager;
    public SubscriptionSettingsRepository(Context appContext) {
        mContext = appContext;
    }

    private SubscriptionManager getSubscriptionManager() {
        if (mSubscriptionManager == null) {
            mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        }
        return mSubscriptionManager;
    }

    /** Gts the default data subscription id. */
    public int getDefaultDataSubscriptionId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    /** Gets the list of visible active subscriptions. */
    public List<SubscriptionInfo> getVisibleActiveSubscriptionInfoList() {
        return getSubscriptionManager().getActiveSubscriptionInfoList(true);
    }
}
