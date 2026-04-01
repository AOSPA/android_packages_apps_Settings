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

package com.android.settings.network.telephony.satellite;

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_UNKNOWN;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ESOS_SUPPORTED_BOOL;

import android.annotation.Nullable;
import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import com.android.settings.overlay.FeatureFactory;

/** A repository class for interacting with the SatelliteManager API. */
public class SatelliteSettingsRepository {
    private static final String TAG = SatelliteSettingsRepository.class.getSimpleName();

    private final Context mContext;
    @Nullable
    private SatelliteManager mSatelliteManager;

    @Nullable
    private SatelliteManager getSatelliteManager() {
        if (mSatelliteManager == null) {
            mSatelliteManager = mContext.getSystemService(SatelliteManager.class);
        }
        return mSatelliteManager;
    }

    public SatelliteSettingsRepository(Context appContext) {
        mContext = appContext;
    }

    /** Refers to {@link SatelliteManager#isSatelliteAttachSupported} */
    public boolean isSatelliteAttachSupported(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.d(TAG, "isSatelliteAttachSupported: isValidSubscriptionId");
            return false;
        }
        SatelliteManager sm = getSatelliteManager();
        if (sm != null) {
            try {
                return sm.isSatelliteAttachSupported(subId);
            } catch (IllegalStateException e) {
                Log.e(TAG, "isSatelliteAttachSupported: IllegalStateException", e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "isSatelliteAttachSupported: IllegalArgumentException", e);
            } catch (SecurityException e) {
                Log.e(TAG, "isSatelliteAttachSupported: SecurityException", e);
            }
        }
        return false;
    }

    /** Refers to {@link SatelliteManager#getSatelliteNtnConnectType} */
    public int getSatelliteNtnConnectType(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.d(TAG, "getSatelliteNtnConnectType: isValidSubscriptionId");
            return CARRIER_ROAMING_NTN_CONNECT_UNKNOWN;
        }
        SatelliteManager sm = getSatelliteManager();
        if (sm != null) {
            try {
                return sm.getSatelliteNtnConnectType(subId);
            } catch (IllegalStateException e) {
                Log.e(TAG, "getSatelliteNtnConnectType: IllegalStateException", e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "getSatelliteNtnConnectType: IllegalArgumentException", e);
            } catch (SecurityException e) {
                Log.e(TAG, "getSatelliteNtnConnectType: SecurityException", e);
            }
        }
        return CARRIER_ROAMING_NTN_CONNECT_UNKNOWN;
    }

    /** Refers to {@link SatelliteManager#isSatelliteEntitlementSupported} */
    public boolean isSatelliteEntitlementSupported(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.d(TAG, "isSatelliteEntitlementSupported: isValidSubscriptionId");
            return false;
        }
        SatelliteManager sm = getSatelliteManager();
        if (sm != null) {
            try {
                return sm.isSatelliteEntitlementSupported(subId);
            } catch (IllegalStateException e) {
                Log.e(TAG, "isSatelliteEntitlementSupported: IllegalStateException", e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "isSatelliteEntitlementSupported: IllegalArgumentException", e);
            } catch (SecurityException e) {
                Log.e(TAG, "isSatelliteEntitlementSupported: SecurityException", e);
            }
        }
        return false;
    }

    /** Returns the value of {@link CarrierConfigManager#KEY_SATELLITE_ESOS_SUPPORTED_BOOL} */
    public boolean isSatelliteSosSupported(int subId) {
        return FeatureFactory.getFeatureFactory().getTelephonyFeatureProvider()
                .getCarrierConfigRepository()
                .getBoolean(
                        subId,
                        KEY_SATELLITE_ESOS_SUPPORTED_BOOL
                );
    }
}
