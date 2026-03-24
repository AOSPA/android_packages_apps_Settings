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

package com.android.settings.troubleshooting

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.interfaces.troubleshooting.ITroubleshootingInfoProviderService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class TroubleshootingServiceConnectionTest {

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockResources: Resources
    @Mock private lateinit var mockPackageManager: PackageManager
    @Mock private lateinit var mockService: ITroubleshootingInfoProviderService
    @Mock private lateinit var mockBinder: IBinder
    @Mock private lateinit var mockReceiver: ResultReceiver

    private val serviceName = "com.android.test/.TroubleshootingService"
    private lateinit var connection: TroubleshootingServiceConnection

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Reset the companion object static variable
        TroubleshootingServiceConnection.isTroubleshootingServiceExists = null

        whenever(mockContext.resources).thenReturn(mockResources)
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
        whenever(mockResources.getString(R.string.config_connectivity_troubleshooting_service_name))
            .thenReturn(serviceName)

        val receivers = mapOf("test_type" to mockReceiver)
        connection = TroubleshootingServiceConnection(receivers)
    }

    @Test
    fun isTroubleshootingServiceExists_whenServiceExists_returnsTrue() {
        val componentName = ComponentName.unflattenFromString(serviceName)!!
        whenever(mockPackageManager.getServiceInfo(componentName, 0)).thenReturn(ServiceInfo())

        val result = connection.isTroubleshootingServiceExists(mockContext)

        assertTrue(result)
        assertEquals(true, TroubleshootingServiceConnection.isTroubleshootingServiceExists)
    }

    @Test
    fun isTroubleshootingServiceExists_whenServiceMissing_returnsFalse() {
        val componentName = ComponentName.unflattenFromString(serviceName)!!
        whenever(mockPackageManager.getServiceInfo(componentName, 0))
            .thenThrow(PackageManager.NameNotFoundException())

        val result = connection.isTroubleshootingServiceExists(mockContext)

        assertFalse(result)
    }

    @Test
    fun bindService_successfullyConnectsAndRegisters() {
        val testBundle = Bundle().apply { putString("key", "value") }

        whenever(mockService.getDiagnosticUiInfo("test_type")).thenReturn(testBundle)

        doAnswer { invocation ->
                val conn = invocation.arguments[3] as TroubleshootingServiceConnection
                conn.onServiceConnected(ComponentName.unflattenFromString(serviceName), mockBinder)
                true
            }
            .whenever(mockContext)
            .bindService(any(Intent::class.java), anyInt(), any(), any())

        connection.onServiceConnected(null, mockBinder)
    }

    @Test
    fun unbindService_unregistersCallbacks() {
        // Setup internal state as if connected
        val field =
            TroubleshootingServiceConnection::class
                .java
                .getDeclaredField("mITroubleshootingInfoProviderService")
        field.isAccessible = true
        field.set(connection, mockService)

        connection.unbindService(mockContext)

        verify(mockService).unregisterIssueDetectionCallback("test_type", mockReceiver)
        verify(mockContext).unbindService(connection)
    }
}
