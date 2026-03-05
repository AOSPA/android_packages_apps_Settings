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
package com.android.settings.msds

import android.content.Context
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settingslib.utils.ThreadUtils
import com.google.android.msdl.data.model.MSDLToken
import com.google.android.msdl.domain.InteractionProperties
import com.google.android.msdl.domain.MSDLPlayer
import com.google.android.msdl.logging.MSDLEvent
import com.google.common.util.concurrent.ListenableFuture

/**
 * A wrapper to ensure that a single instance of the [MSDLPlayer] is provided all across Settings
 */
object MSDLPlayerWrapper {

    private const val TAG = "MSDLPlayerWrapper"

    private var internalPlayer: MSDLPlayer? = null

    /**
     * Create the singleton of the [MSDLPlayer].
     *
     * This function is only meant to be called once during initialization of the client app
     *
     * @param context The context in which the player is created. Used to access system services.
     */
    fun createPlayer(context: Context) {
        if (internalPlayer != null) return

        val unused: ListenableFuture<*>? =
            ThreadUtils.postOnBackgroundThread {
                val vibratorManager = context.getSystemService(VibratorManager::class.java)
                internalPlayer = MSDLPlayer.createPlayer(vibratorManager.defaultVibrator)
            }
    }

    /**
     * Define the internal MSDL player explicitly. It should only be used for testing purposes
     *
     * @param explicitPlayer An explicit player to use.
     */
    @VisibleForTesting
    fun setPlayer(explicitPlayer: MSDLPlayer?) {
        internalPlayer = explicitPlayer
    }

    @JvmOverloads
    fun playToken(token: MSDLToken, properties: InteractionProperties? = null) {
        val player = internalPlayer
        if (player != null) {
            player.playToken(token, properties)
        } else {
            Log.e(TAG, "Cannot play $token at this time because the internal player is null")
        }
    }

    fun getHistory(): List<MSDLEvent> {
        val history = internalPlayer?.getHistory()
        if (history != null) {
            return history
        } else {
            Log.e(TAG, "The internal player is null. Returning an empty list of events")
            return listOf()
        }
    }
}
