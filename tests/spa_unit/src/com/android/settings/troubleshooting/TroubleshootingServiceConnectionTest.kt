package com.android.settings.troubleshooting

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Resources
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
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class TroubleshootingServiceConnectionTest {

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockResources: Resources
    @Mock private lateinit var mockPackageManager: PackageManager
    @Mock private lateinit var mockService: ITroubleshootingInfoProviderService
    @Mock private lateinit var mockReceiver: ResultReceiver
    @Mock
    private lateinit var mockListener: TroubleshootingServiceConnection.ServiceConnectionListener

    private val servicePkg = "com.android.test"
    private val serviceClass = "com.android.test.TroubleshootingService"
    private val flattenedName = "$servicePkg/$serviceClass"

    private lateinit var connection: TroubleshootingServiceConnection

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Reset static cache for test isolation
        TroubleshootingServiceConnection.cachedExists = null

        whenever(mockContext.resources).thenReturn(mockResources)
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
        whenever(mockResources.getString(R.string.config_connectivity_troubleshooting_service_name))
            .thenReturn(flattenedName)

        val receivers = mapOf("test_type" to mockReceiver)
        connection = TroubleshootingServiceConnection(receivers)
        connection.serviceConnectionListener = mockListener
    }

    @Test
    fun isTroubleshootingServiceExists_whenServiceExists_returnsTrue() {
        val component = ComponentName(servicePkg, serviceClass)
        whenever(mockPackageManager.getServiceInfo(component, 0)).thenReturn(ServiceInfo())

        val result = connection.isTroubleshootingServiceExists(mockContext)

        assertTrue(result)
        assertEquals(true, TroubleshootingServiceConnection.cachedExists)
    }

    @Test
    fun isTroubleshootingServiceExists_whenServiceMissing_returnsFalse() {
        val component = ComponentName(servicePkg, serviceClass)
        whenever(mockPackageManager.getServiceInfo(component, 0))
            .thenThrow(PackageManager.NameNotFoundException())

        val result = connection.isTroubleshootingServiceExists(mockContext)

        assertFalse(result)
    }

    @Test
    fun bindService_successfulBind_callsContextBindService() {
        whenever(mockContext.bindService(any(), any(), anyInt())).thenReturn(true)

        connection.bindService(mockContext)

        verify(mockContext).bindService(any(), any(), anyInt())
    }

    @Test
    fun onServiceDisconnected_updatesStateAndNotifiesListener() {
        connection.onServiceDisconnected(ComponentName(servicePkg, serviceClass))

        verify(mockListener).onServiceConnectedState(false)
    }

    @Test
    fun unbindService_unregistersCallbacksAndCleansUp() {
        injectMockService(mockService)

        connection.unbindService(mockContext)

        verify(mockService).unregisterIssueDetectionCallback("test_type", mockReceiver)
        verify(mockContext).unbindService(connection)
    }

    private fun injectMockService(service: ITroubleshootingInfoProviderService?) {
        val field =
            TroubleshootingServiceConnection::class.java.getDeclaredField("troubleshootingService")
        field.isAccessible = true
        field.set(connection, service)
    }
}
