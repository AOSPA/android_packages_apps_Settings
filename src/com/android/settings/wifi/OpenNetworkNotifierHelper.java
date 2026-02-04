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

package com.android.settings.wifi;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.concurrent.Executor;

public class OpenNetworkNotifierHelper {
    @Nullable
    private static volatile OpenNetworkNotifierHelper sInstance;
    private final Context mContext;
    private volatile boolean mEnabled = false;

    private OpenNetworkNotifierHelper(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Returns the singleton instance of the {@link OpenNetworkNotifierHelper}.
     *
     * @param context The context used to initialize the instance if created.
     * @return The singleton instance.
     */
    public static synchronized OpenNetworkNotifierHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new OpenNetworkNotifierHelper(context);
        }
        return sInstance;
    }

    /**
     * Returns whether the open network notifier is currently enabled.
     *
     * <p>If multi-user Wi-Fi is enabled, this returns the cached value. Otherwise, it reads
     * directly from {@link Settings.Global}.
     *
     * @return {@code true} if enabled, {@code false} otherwise.
     */
    public boolean isEnabled() {
        if (WifiUtils.isWifiMultiuserEnabled()) {
            return mEnabled;
        } else {
            return Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0) == 1;
        }
    }

    /**
     * Asynchronously loads the notifier setting value from {@link WifiManager}.
     *
     * <p>This method should only be used when {@link WifiUtils#isWifiMultiuserEnabled()} is true.
     * The {@code onDone} callback will be executed on the provided UI Executor.
     *
     * @param wifiManager The {@link WifiManager} instance.
     * @param bgExecutor  The {@link Executor} to use for background tasks.
     * @param ui          The {@link Executor} to use for UI callbacks.
     * @param onDone      The {@link Runnable} to execute when the value is loaded.
     */
    public void loadValue(WifiManager wifiManager, Executor bgExecutor, Executor ui,
            Runnable onDone) {
        bgExecutor.execute(() -> wifiManager.isOpenNetworkNotifierEnabled(ui, (enabledValue) -> {
            mEnabled = Objects.requireNonNullElse(enabledValue, false);
            if (onDone != null) {
                onDone.run();
            }
        }));
    }

    /**
     * Sets the enabled state of the open network notifier.
     *
     * <p>If multi-user Wi-Fi is enabled, the value is persisted via {@link WifiManager} on a
     * background thread. Otherwise, it is written to {@link Settings.Global}.
     *
     * @param wifiManager The {@link WifiManager} instance.
     * @param bgExecutor  The {@link Executor} to use for background tasks.
     * @param enabled     The new enabled state.
     */
    public void setEnabled(WifiManager wifiManager, Executor bgExecutor, boolean enabled) {
        if (WifiUtils.isWifiMultiuserEnabled()) {
            bgExecutor.execute(() -> wifiManager.setOpenNetworkNotifierEnabled(enabled));
        } else {
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, enabled ? 1 : 0);
        }
    }
}
