package com.android.settings.wifi.tether;

import android.net.wifi.WifiClient;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerExecutor;

import java.util.List;

/**
 * Wrapper for {@link android.net.wifi.WifiManager.SoftApCallback} to pass the robo test
 */
public class WifiTetherSoftApManager {

    private WifiManager mWifiManager;
    private WifiTetherSoftApCallback mWifiTetherSoftApCallback;

    private WifiManager.SoftApCallback mSoftApCallback = new WifiManager.SoftApCallback() {
        @Override
        public void onStateChanged(int state, int failureReason) {
            mWifiTetherSoftApCallback.onStateChanged(state, failureReason);
        }

        @Override
        public void onConnectedClientsChanged(List<WifiClient> clients) {
            // Do nothing - we don't care about changing anything here.
        }

        @Override
        public void onStaConnected(String Macaddr, int numClients) {
            // TODO(b/144386510): onNumClientsChanged is now onConnectedClientsChanged, and expects
            // a list of changed clients.
            //mWifiTetherSoftApCallback.onNumClientsChanged(numClients);
        }

        @Override
        public void onStaDisconnected(String Macaddr, int numClients) {
            // TODO(b/144386510): onNumClientsChanged is now onConnectedClientsChanged, and expects
            // a list of changed clients.
            //mWifiTetherSoftApCallback.onNumClientsChanged(numClients);
        }
    };
    private Handler mHandler;

    WifiTetherSoftApManager(WifiManager wifiManager,
            WifiTetherSoftApCallback wifiTetherSoftApCallback) {
        mWifiManager = wifiManager;
        mWifiTetherSoftApCallback = wifiTetherSoftApCallback;
        mHandler = new Handler();
    }

    public void registerSoftApCallback() {
        mWifiManager.registerSoftApCallback(mSoftApCallback, new HandlerExecutor(mHandler));
    }

    public void unRegisterSoftApCallback() {
        mWifiManager.unregisterSoftApCallback(mSoftApCallback);
    }

    public interface WifiTetherSoftApCallback {
        void onStateChanged(int state, int failureReason);

        /**
         * Called when the connected clients to soft AP changes.
         *
         * @param clients the currently connected clients
         */
        void onConnectedClientsChanged(List<WifiClient> clients);
    }
}
