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

package com.android.settings.network.tether

import android.content.ContextWrapper
import android.net.TetheringManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.tether.TetheringRepository.TetherType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class TetheringRepositoryTest {

    private val mockTetheringManager = mock<TetheringManager>()
    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(TetheringManager::class.java) -> mockTetheringManager
                    else -> super.getSystemService(name)
                }
        }

    private var repository = TetheringRepository(context, mockTetheringManager)

    @Test
    fun isEnabled_noWifiTethering_returnFalse() = runTest {
        mockTetheringManager.stub { on { tetheredIfaces } doReturn emptyArray<String>() }

        assertFalse(repository.isEnabled(TetherType.WIFI))
    }

    @Test
    fun isEnabled_hasWifiTethering_returnTrue() = runTest {
        mockTetheringManager.stub { on { tetheredIfaces } doReturn arrayOf("wlan0") }

        assertTrue(repository.isEnabled(TetherType.WIFI))
    }

    @Test
    fun isEnabled_noUsbTethering_returnFalse() = runTest {
        mockTetheringManager.stub { on { tetheredIfaces } doReturn emptyArray<String>() }

        assertFalse(repository.isEnabled(TetherType.USB))
    }

    @Test
    fun isEnabled_hasUsbTethering_returnTrue() = runTest {
        mockTetheringManager.stub { on { tetheredIfaces } doReturn arrayOf("usb0") }

        assertTrue(repository.isEnabled(TetherType.USB))
    }

    @Test
    fun isEnabled_noBtTethering_returnFalse() = runTest {
        mockTetheringManager.stub { on { tetheredIfaces } doReturn emptyArray<String>() }

        assertFalse(repository.isEnabled(TetherType.BLUETOOTH))
    }

    @Test
    fun isEnabled_hasBtTethering_returnTrue() = runTest {
        mockTetheringManager.stub { on { tetheredIfaces } doReturn arrayOf("bt-pan0") }

        assertTrue(repository.isEnabled(TetherType.BLUETOOTH))
    }

    @Test
    fun isEnabled_noEthTethering_returnFalse() = runTest {
        mockTetheringManager.stub { on { tetheredIfaces } doReturn emptyArray<String>() }

        assertFalse(repository.isEnabled(TetherType.ETHERNET))
    }

    @Test
    fun isEnabled_hasEthTethering_returnTrue() = runTest {
        mockTetheringManager.stub { on { tetheredIfaces } doReturn arrayOf("eth0") }

        assertTrue(repository.isEnabled(TetherType.ETHERNET))
    }

    @Test
    fun setEnabled_startWifiTethering_verifyStartTethering() = runTest {
        mockStartTethering()

        repository.setEnabled(TetherType.WIFI, true)

        verifyStartTethering()
    }

    @Test
    fun setEnabled_stopWifiTethering_verifyStopTethering() = runTest {
        repository.setEnabled(TetherType.WIFI, false)

        verify(mockTetheringManager).stopTethering(eq(TetheringManager.TETHERING_WIFI))
    }

    @Test
    fun setEnabled_startUsbTethering_verifyStartTethering() = runTest {
        mockStartTethering()

        repository.setEnabled(TetherType.USB, true)

        verifyStartTethering()
    }

    @Test
    fun setEnabled_stopUsbTethering_verifyStopTethering() = runTest {
        repository.setEnabled(TetherType.USB, false)

        verify(mockTetheringManager).stopTethering(eq(TetheringManager.TETHERING_USB))
    }

    @Test
    fun setEnabled_startBtTethering_verifyStartTethering() = runTest {
        mockStartTethering()

        repository.setEnabled(TetherType.BLUETOOTH, true)

        verifyStartTethering()
    }

    @Test
    fun setEnabled_stopBtTethering_verifyStopTethering() = runTest {
        repository.setEnabled(TetherType.BLUETOOTH, false)

        verify(mockTetheringManager).stopTethering(eq(TetheringManager.TETHERING_BLUETOOTH))
    }

    @Test
    fun setEnabled_startEthTethering_verifyStartTethering() = runTest {
        mockStartTethering()

        repository.setEnabled(TetherType.ETHERNET, true)

        verifyStartTethering()
    }

    @Test
    fun setEnabled_stopEthTethering_verifyStopTethering() = runTest {
        repository.setEnabled(TetherType.ETHERNET, false)

        verify(mockTetheringManager).stopTethering(eq(TetheringManager.TETHERING_ETHERNET))
    }

    private fun mockStartTethering() {
        whenever(
                mockTetheringManager.startTethering(
                    any<TetheringManager.TetheringRequest>(),
                    any(),
                    any(),
                )
            )
            .thenAnswer { invocation ->
                val callback = invocation.getArgument<TetheringManager.StartTetheringCallback>(2)
                callback.onTetheringStarted()
                null
            }
    }

    private fun verifyStartTethering() {
        verify(mockTetheringManager)
            .startTethering(any<TetheringManager.TetheringRequest>(), any(), any())
    }
}
