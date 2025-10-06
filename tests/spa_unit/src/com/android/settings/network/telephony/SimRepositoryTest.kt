/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony

import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class SimRepositoryTest {

    private val mockUserManager = mock<UserManager>()

    private val mockPackageManager = mock<PackageManager>()

    private val mockTelephonyManager = mock<TelephonyManager>()

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { userManager } doReturn mockUserManager
            on { packageManager } doReturn mockPackageManager
            on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
            on { getSystemService(Context.TELEPHONY_SERVICE) } doReturn mockTelephonyManager
        }

    private val spyResources = spy(context.resources)

    private val repository = SimRepository(context)

    @Before
    fun setUp() {
        context.stub { on { resources } doReturn spyResources }

        // By default, available and user unrestricted
        spyResources.stub { on { getBoolean(R.bool.config_show_sim_info) } doReturn true }
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn true
        }
        mockUserManager.stub {
            on { isAdminUser } doReturn true
            on { hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS) } doReturn false
        }
    }

    @Test
    fun showMobileNetworkPageEntrance_defaults_returnTrue() {
        // use defaults from setup

        val showMobileNetworkPage = repository.showMobileNetworkPageEntrance()

        assertThat(showMobileNetworkPage).isTrue()
    }

    @Test
    fun showMobileNetworkPageEntrance_dataOnly_returnTrue() {
        mockTelephonyManager.stub { on { isDeviceVoiceCapable } doReturn false }

        val showMobileNetworkPage = repository.showMobileNetworkPageEntrance()

        assertThat(showMobileNetworkPage).isTrue()
    }

    @Test
    fun showMobileNetworkPageEntrance_voiceOnly_returnTrue() {
        mockTelephonyManager.stub { on { isDataCapable } doReturn false }

        val showMobileNetworkPage = repository.showMobileNetworkPageEntrance()

        assertThat(showMobileNetworkPage).isTrue()
    }

    @Test
    fun showMobileNetworkPageEntrance_notAdminUser_returnFalse() {
        mockUserManager.stub { on { isAdminUser } doReturn false }

        val showMobileNetworkPage = repository.showMobileNetworkPageEntrance()

        assertThat(showMobileNetworkPage).isFalse()
    }

    @Test
    fun showMobileNetworkPageEntrance_noTelephony_returnFalse() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn false
        }

        val showMobileNetworkPage = repository.showMobileNetworkPageEntrance()

        assertThat(showMobileNetworkPage).isFalse()
    }

    @Test
    fun showMobileNetworkPageEntrance_noShowSimInfo_returnFalse() {
        spyResources.stub { on { getBoolean(R.bool.config_show_sim_info) } doReturn false }

        val showMobileNetworkPage = repository.showMobileNetworkPageEntrance()

        assertThat(showMobileNetworkPage).isFalse()
    }

    @Test
    fun canEnterMobileNetworkPage_defaults_returnTrue() {
        // use defaults from setup

        val enterMobileNetworkPage = repository.canEnterMobileNetworkPage()

        assertThat(enterMobileNetworkPage).isTrue()
    }

    @Test
    fun canEnterMobileNetworkPage_disallowConfigMobileNetwork_returnFalse() {
        mockUserManager.stub {
            on { hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS) } doReturn true
        }

        val enterMobileNetworkPage = repository.canEnterMobileNetworkPage()

        assertThat(enterMobileNetworkPage).isFalse()
    }
}
