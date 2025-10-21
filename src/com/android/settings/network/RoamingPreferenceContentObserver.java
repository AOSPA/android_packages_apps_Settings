/**
 * Copyright (c) 2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import android.util.Log;

/**
 * {@link ContentObserver} to listen to update of Roaming UI state change
 */
public class RoamingPreferenceContentObserver extends ContentObserver {
    private static final String TAG = "RoamingPreferenceContentObserver";
    private OnRoamingPreferenceChangedListener mListener;

    public RoamingPreferenceContentObserver(Handler handler) {
        super(handler);
        Log.d(TAG, "RoamingPreferenceContentObserver : Constructor");
    }

    /**
     * Return a URI of roaming state(ON vs OFF)
     */
    public static Uri getObservableUri(Context context, int subId) {
        Uri uri = Settings.Global.getUriFor(Settings.Global.DATA_ROAMING);
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        if (telephonyManager.getActiveModemCount() != 1) {
            uri = Settings.Global.getUriFor(Settings.Global.DATA_ROAMING + subId);
        }
        Log.d(TAG, "RoamingPreferenceContentObserver : getObservableUri : uri = " + uri);
        return uri;
    }

    public void setOnRoamingPreferenceChangedListener(OnRoamingPreferenceChangedListener lsn) {
        mListener = lsn;
        Log.d(TAG, "RoamingPreferenceContentObserver : setOnRoamingPreferenceChangedListener");
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        if (mListener != null) {
            Log.d(TAG, "RoamingPreferenceContentObserver : onChange");
            mListener.onRoamingPreferenceChanged();
        }
    }

    public void register(Context context, int subId) {
        final Uri uri = getObservableUri(context, subId);
        Log.d(TAG, "RoamingPreferenceContentObserver : register : uri = " + uri);
        context.getContentResolver().registerContentObserver(uri, false, this);

    }

    public void unRegister(Context context) {
        context.getContentResolver().unregisterContentObserver(this);
    }

    /**
     * Listener for update of Roaming state(ON vs OFF)
     */
    public interface OnRoamingPreferenceChangedListener {
        void onRoamingPreferenceChanged();
    }
}
