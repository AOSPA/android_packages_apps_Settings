/*
 * Copyright (c) 2020-2021, The Linux Foundation. All rights reserved
 * Not a contribution

 * Copyright (C) 2019 The Android Open Source Project

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

package com.android.settings.development;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.Context;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import com.android.settings.R;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.network.SubscriptionUtil;
import com.android.settingslib.Utils;

import java.util.List;

public class PreferVonrController extends DeveloperOptionsPreferenceController implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient, LifecycleObserver{
    private static final String TAG = "PreferVonrCtlr";

    private static final String KEY = "prefer_vonr_mode";


    private UserManager mUserManager;
    private SubscriptionsChangeListener mChangeListener;
    private Preference mPreference;

    public PreferVonrController(Context context, Lifecycle lifecycle) {
        super(context);
        mUserManager = context.getSystemService(UserManager.class);
        mChangeListener = new SubscriptionsChangeListener(context, this);
        if (lifecycle != null) {
          lifecycle.addObserver(this);
        }
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mChangeListener.start();
        update();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mChangeListener.stop();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    private void update() {
        if (mPreference == null) {
            return;
        }
        mPreference.setEnabled(!mChangeListener.isAirplaneModeOn());

        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(
                mContext);

        if (subs.isEmpty()) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isWifiOnly(mContext) && mUserManager.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        update();
    }

    @Override
    public void onSubscriptionsChanged() {
        update();
    }
}
