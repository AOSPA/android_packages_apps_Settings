/*
Copyright (c) 2017, The Linux Foundation. All rights reserved.

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
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.settings.R;
import org.codeaurora.internal.IExtTelephony;

import java.util.List;

public class ManagerPasspointReceiver extends BroadcastReceiver {

    private static final String TAG = "ManagerPasspointReceiver";
    private static final String SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private final String IS_USER_DISABLE_HS2_REL1 = "is_user_disable_hs2_rel1";
    private Context mContext;
    private WifiManager mWifiManager;
    IExtTelephony mExtTelephony;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(!context.getResources().getBoolean(R.bool.config_wifi_hotspot2_enabled_Rel1)) {
            return;
        }
        mContext = context;
        initValue();

        if (!mWifiManager.isWifiEnabled()) {
            return;
        }

        new Thread() {
            public void run() {
                updateWifiConfing();
            }
        }.start();
    }

    private void updateWifiConfing() {
        List<WifiConfiguration> configList = mWifiManager.getConfiguredNetworks();
        WifiConfiguration config = null;

        if (configList != null) {
            for (WifiConfiguration c : configList) {
                if (c.isPasspoint() && c.FQDN.equals(
                        mContext.getResources().getString(R.string.passpoint_fqdn))) {
                    config = c;
                    break;
                }
            }
        }

        if (Settings.Global.getInt(
                mContext.getContentResolver(), IS_USER_DISABLE_HS2_REL1, 1) == 0) {
            // user disable passpoint
            forgetNetwork(config);
            return;
        }

        TelephonyManager telManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        int slotId = -1;
        try {
            if (mExtTelephony == null) {
                return;
            }
            slotId = mExtTelephony.getPrimaryCarrierSlotId();
        } catch (RemoteException e) {
            Log.e(TAG, "getPrimaryCarrierSlotId() error");
        }

        int[] subIds = null;
        String imsiNum = null;
        boolean isLoaded = false;
        if (slotId != -1) {
            isLoaded = (TelephonyManager.SIM_STATE_READY == telManager.getSimState(slotId));
            subIds = SubscriptionManager.getSubId(slotId);
            if (subIds != null) {
                imsiNum = telManager.getSubscriberId(subIds[0]);
            }
        }

        if (slotId == -1 || !isLoaded || subIds == null || imsiNum == null) {
            // Disable switch here....
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.WIFI_HOTSPOT2_REL1_ENABLED, 0);
            forgetNetwork(config);
            return;
        }

        if (config == null) {
            // Add a new passpoint network
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.providerFriendlyName = mContext.getResources().getString(
                    R.string.passpoint_provider_friendly_name);
            wifiConfig.FQDN = mContext.getResources().getString(R.string.passpoint_fqdn);
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);

            String eapMethod = mContext.getResources().getString(R.string.passpoint_eap_method);
            if (eapMethod.equals("SIM"))
                wifiConfig.enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.SIM);
            else if (eapMethod.equals("AKA"))
                wifiConfig.enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.AKA);
            else if (eapMethod.equals("AKA_PRIME"))
                wifiConfig.enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.AKA_PRIME);

            wifiConfig.enterpriseConfig.setPlmn(imsiNum);
            wifiConfig.SIMNum = slotId + 1;

            int netId = mWifiManager.addNetwork(wifiConfig);
            if (netId != WifiConfiguration.INVALID_NETWORK_ID) {
                mWifiManager.enableNetwork(netId, false);
                mWifiManager.saveConfiguration();
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.WIFI_HOTSPOT2_REL1_ENABLED, 1);
            } else {
                Log.i(TAG, "netId is -1");
            }
        } else {
            // if passpoint config already present, update it.
            config.enterpriseConfig.setPlmn(imsiNum);
            config.SSID = null;
            config.SIMNum = slotId + 1;
            mWifiManager.updateNetwork(config);
            mWifiManager.enableNetwork(config.networkId, false);
            mWifiManager.saveConfiguration();
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.WIFI_HOTSPOT2_REL1_ENABLED, 1);
        }
    }

    private void forgetNetwork(WifiConfiguration config) {
        if (config != null) {
            mWifiManager.forget(config.networkId, null);
        }
    }

    private void initValue() {
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mExtTelephony =
                IExtTelephony.Stub.asInterface(ServiceManager.getService("extphone"));
    }

}
