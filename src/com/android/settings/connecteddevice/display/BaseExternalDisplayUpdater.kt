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

package com.android.settings.connecteddevice.display

import android.app.admin.DevicePolicyIdentifiers
import android.app.admin.DevicePolicyManager
import android.app.admin.EnforcingAdmin
import android.content.Context
import android.os.Bundle
import android.os.UserHandle
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.connecteddevice.DevicePreferenceCallback
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener
import com.android.settings.core.SubSettingLauncher
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider

/** Base updater for external display preferences. */
abstract class BaseExternalDisplayUpdater(
    @JvmField protected val devicePreferenceCallback: DevicePreferenceCallback,
    @JvmField protected val metricsCategory: Int
) {

    @JvmField
    protected val metricsFeatureProvider: MetricsFeatureProvider =
        FeatureFactory.featureFactory.metricsFeatureProvider

    @JvmField
    protected val updateRunnable = Runnable { update() }

    @JvmField
    protected var injector: ConnectedDisplayInjector? = null

    @JvmField
    protected var context: Context? = null

    private val listener =
        object : DisplayListener() {
            override fun update(displayId: Int) {
                refreshPreference()
            }
        }

    /** Set the context to generate the Preference, so it could get the correct theme. */
    fun initPreference(context: Context) {
        initPreference(context, ConnectedDisplayInjector(context))
    }

    @VisibleForTesting
    open fun initPreference(context: Context, injector: ConnectedDisplayInjector) {
        this.context = context
        this.injector = injector
    }

    /** Register the display listener. */
    @VisibleForTesting
    open fun registerCallback() {
        injector?.registerDisplayListener(listener)
    }

    /** Unregister the display listener. */
    @VisibleForTesting
    open fun unregisterCallback() {
        injector?.unregisterDisplayListener(listener)
    }

    /** Updates preference, possibly removing it entirely. */
    @VisibleForTesting
    open fun refreshPreference() {
        injector?.let {
            unscheduleUpdate()
            it.handler.post(updateRunnable)
        }
    }

    @VisibleForTesting
    protected open fun launchSettings(context: Context, args: Bundle?) {
        SubSettingLauncher(context)
            .setDestination(TabbedDisplayPreferenceFragment::class.java.name)
            .setTitleRes(R.string.external_display_settings_title)
            .setSourceMetricsCategory(metricsCategory)
            .setArguments(args)
            .launch()
    }

    @VisibleForTesting
    protected abstract fun update()

    protected fun applyUsbDataSignalingPolicy(preference: RestrictedPreference, context: Context) {
        if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
            preference.setDisabledByAdmin(getEnforcingAdminForUsbDataSignaling(context))
        } else {
            preference.setDisabledByAdmin(checkIfUsbDataSignalingIsDisabled(context))
        }
    }

    private fun unscheduleUpdate() {
        injector?.handler?.removeCallbacks(updateRunnable)
    }

    private fun checkIfUsbDataSignalingIsDisabled(
        context: Context
    ): RestrictedLockUtils.EnforcedAdmin? {
        return RestrictedLockUtilsInternal.checkIfUsbDataSignalingIsDisabled(
            context,
            UserHandle.myUserId()
        )
    }

    private fun getEnforcingAdminForUsbDataSignaling(context: Context): EnforcingAdmin? {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return null
        return dpm.getEnforcingAdminsForPolicy(
                DevicePolicyIdentifiers.USB_DATA_SIGNALING_POLICY,
                UserHandle.myUserId()
            )
            ?.mostImportantEnforcingAdmin
    }
}
