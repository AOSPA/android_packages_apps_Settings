/*
Copyright (c) 2021 The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 copyright notice, this list of conditions and the following
 disclaimer in the documentation and/or other materials provided
 with the distribution.
 * Neither the name of The Linux Foundation nor the names of its
 contributors may be used to endorse or promote products derived
 from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.settings.development;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settings.R;

import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackBase;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Token;

public class SmartDdsSwitchPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private final String TAG = "SmartDdsSwitchPreferenceController";

    private final int EVENT_SET_DEFAULT_TOGGLE_STATE_RESPONSE = 1;

    private final int SETTING_VALUE_ON = 1;
    private final int SETTING_VALUE_OFF = 0;

    private static SmartDdsSwitchPreferenceController mInstance;
    private Client mClient;
    private Context mContext;
    private String mPackageName;
    private final TelephonyManager mTelephonyManager;
    private ExtTelephonyManager mExtTelephonyManager;
    private boolean mFeatureAvailable = false;
    private boolean mServiceConnected = false;
    private boolean mSwitchEnabled = false;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateState(mPreference);
        }
    };

    private SmartDdsSwitchPreferenceController(Context context) {
        super(context);
        Log.d(TAG, "Constructor");
        mContext = context.getApplicationContext();
        mPackageName = mContext.getPackageName();
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
        IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    public static SmartDdsSwitchPreferenceController getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SmartDdsSwitchPreferenceController(context);
        }
        return mInstance;
    }

    public void cleanUp() {
        Log.d(TAG, "Disconnecting ExtTelephonyService");
        mExtTelephonyManager.disconnectService();
        mInstance = null;
    }

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "ExtTelephonyService connected");
            mServiceConnected = true;
            mClient = mExtTelephonyManager.registerCallback(mPackageName, mCallback);
            Log.d(TAG, "Client = " + mClient);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "ExtTelephonyService disconnected");
            mContext.unregisterReceiver(mBroadcastReceiver);
            mServiceConnected = false;
            mClient = null;
        }
    };

    private ExtPhoneCallbackBase mCallback = new ExtPhoneCallbackBase() {
        @Override
        public void setSmartDdsSwitchToggleResponse(Token token, boolean result) throws
                RemoteException {
            Log.d(TAG, "setSmartDdsSwitchToggleResponse: token = " + token + " result = " + result);
            if (result) {
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_DEFAULT_TOGGLE_STATE_RESPONSE));
            }
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case EVENT_SET_DEFAULT_TOGGLE_STATE_RESPONSE: {
                    Log.d(TAG, "EVENT_SET_DEFAULT_TOGGLE_STATE_RESPONSE");
                    String defaultSummary = mContext.getResources().getString(
                            R.string.smart_dds_switch_summary);
                    updateUi(defaultSummary, true);
                    putSwitchValue(mSwitchEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
                    break;
                }
                default:
                    Log.e(TAG, "Unsupported action");
            }
        }
    };

    @Override
    public String getPreferenceKey() {
        return Settings.Global.SMART_DDS_SWITCH;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mSwitchEnabled = (Boolean) newValue;
        Log.d(TAG, "onPreferenceChange: isEnabled = " + mSwitchEnabled);
        // Temporarily update the text and disable the button until the response is received
        String waitSummary = mContext.getResources().getString(
                R.string.smart_dds_switch_wait_summary);
        updateUi(waitSummary, false);
        if (mServiceConnected && mClient != null) {
            try {
                mExtTelephonyManager.setSmartDdsSwitchToggle(mSwitchEnabled, mClient);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Exception " + e);
                return false;
            }
        } else {
            Log.e(TAG, "ExtTelephonyManager service not connected");
            return false;
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (mPreference != null) {
            final int smartDdsSwitch = getSwitchValue();
            ((SwitchPreference) mPreference).setChecked(smartDdsSwitch != SETTING_VALUE_OFF);
        }
        if (mServiceConnected) {
            try {
                mFeatureAvailable = mExtTelephonyManager.isSmartDdsSwitchFeatureAvailable();
            } catch (Exception e) {
                Log.e(TAG, "Exception " + e);
            }
            Log.d(TAG, "mFeatureAvailable: " + mFeatureAvailable);
            if (mFeatureAvailable) {
                String defaultSummary = mContext.getResources().getString(
                        R.string.smart_dds_switch_summary);
                updateUi(defaultSummary, isAvailable());
            } else {
                Log.d(TAG, "Feature unavailable");
                preference.setVisible(false);
            }
        } else {
            Log.d(TAG, "Service not connected");
        }
    }

    private void updateUi(String summary, boolean enable) {
        Log.d(TAG, "updateUi enable: " + enable);
        if (mPreference != null) {
            ((SwitchPreference) mPreference).setVisible(true);
            ((SwitchPreference) mPreference).setSummary(summary);
            ((SwitchPreference) mPreference).setEnabled(enable);
        }
    }

    @Override
    public boolean isAvailable() {
        // Only show the toggle if 1) APM is off and 2) more than one subscription is active
        SubscriptionManager subscriptionManager = mContext.getSystemService(
                SubscriptionManager.class);
        int numActiveSubscriptionInfoCount = subscriptionManager.getActiveSubscriptionInfoCount();
        Log.d(TAG, "numActiveSubscriptionInfoCount: " + numActiveSubscriptionInfoCount);
        return !isAirplaneModeOn() && (numActiveSubscriptionInfoCount > 1);
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void putSwitchValue(int state) {
        Settings.Global.putInt(mContext.getContentResolver(), getPreferenceKey(), state);
    }

    private int getSwitchValue() {
        return Settings.Global.getInt(mContext.getContentResolver(), getPreferenceKey(),
                SETTING_VALUE_OFF);
    }
}