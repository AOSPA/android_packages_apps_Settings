/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.gestures

import android.app.role.OnRoleHoldersChangedListener
import android.app.role.RoleManager
import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import com.android.internal.R
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore

class DoubleTapPowerStorage(private val context: Context) :
    AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {
    private val settingsStore = SettingsSecureStore.get(context)
    private val roleManager: RoleManager? = context.getSystemService(RoleManager::class.java)

    private val onRoleHoldersChangedListener = OnRoleHoldersChangedListener { roleName, user ->
        if (roleName != RoleManager.ROLE_WALLET || user.identifier != UserHandle.myUserId()) {
            return@OnRoleHoldersChangedListener
        }
        notifyChange(DataChangeReason.UPDATE)
    }

    override fun contains(key: String) =
        key == DoubleTapPowerForCameraPreference.KEY || key == DoubleTapPowerForWalletPreference.KEY

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val currentValue =
            settingsStore.getInt(KEY)
                ?: context.resources.getInteger(
                    R.integer.config_doubleTapPowerGestureMultiTargetDefaultAction
                )
        return when (key) {
            DoubleTapPowerForCameraPreference.KEY -> (currentValue == CAMERA_LAUNCH_VALUE) as T?
            DoubleTapPowerForWalletPreference.KEY -> (currentValue == WALLET_LAUNCH_VALUE) as T?
            else -> false as T?
        }
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean) return
        when (key) {
            DoubleTapPowerForCameraPreference.KEY -> settingsStore.setInt(KEY, CAMERA_LAUNCH_VALUE)
            DoubleTapPowerForWalletPreference.KEY -> settingsStore.setInt(KEY, WALLET_LAUNCH_VALUE)
            else -> return
        }
    }

    override fun onFirstObserverAdded() {
        settingsStore.addObserver(GESTURE_ENABLED, this, HandlerExecutor.main)
        settingsStore.addObserver(KEY, this, HandlerExecutor.main)
        roleManager?.addOnRoleHoldersChangedListenerAsUser(
            HandlerExecutor.main,
            onRoleHoldersChangedListener,
            UserHandle.of(UserHandle.myUserId()),
        )
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(GESTURE_ENABLED, this)
        settingsStore.removeObserver(KEY, this)
        roleManager?.removeOnRoleHoldersChangedListenerAsUser(
            onRoleHoldersChangedListener,
            UserHandle.of(UserHandle.myUserId()),
        )
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(DataChangeReason.UPDATE)
    }

    companion object {
        const val KEY = Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE
        const val GESTURE_ENABLED = Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED

        const val CAMERA_LAUNCH_VALUE = 0
        const val WALLET_LAUNCH_VALUE = 1
    }
}
