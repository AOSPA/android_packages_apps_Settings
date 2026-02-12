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

package com.android.settings.msds

import android.content.Context
import android.os.VibratorManager
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.msdl.data.model.MSDLToken
import com.google.android.msdl.domain.MSDLPlayer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MSDLPlayerWrapperTest {

    private lateinit var context: Context
    private var internalPlayer: MSDLPlayer? = null

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val vibratorManager = context.getSystemService(VibratorManager::class.java)
        internalPlayer = MSDLPlayer.createPlayer(vibratorManager.defaultVibrator)

        MSDLPlayerWrapper.setPlayer(internalPlayer)
    }

    @Test
    fun playToken_withValidPlayer_playsToken() {
        val token = MSDLToken.UNLOCK
        MSDLPlayerWrapper.playToken(token)

        val history = MSDLPlayerWrapper.getHistory()
        assertEquals(1, history.size)
        assertEquals(token.name, history[0].tokenName)
    }

    @Test
    fun playToken_withoutValidPlayer_doesNotPlay() {
        MSDLPlayerWrapper.setPlayer(null)

        val token = MSDLToken.UNLOCK
        MSDLPlayerWrapper.playToken(token)

        val history = MSDLPlayerWrapper.getHistory()
        assertEquals(0, history.size)
    }
}
