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

package com.android.settings.supervision

import android.app.Application
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settingslib.ipc.MessengerServiceRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowPackageManager

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class SupervisionDashboardActivityTest {

    private lateinit var applicationContext: Context
    private lateinit var shadowPackageManager: ShadowPackageManager
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mockSupervisionManager = mock<RoleManager>()
    private val testSupervisionPackage = "com.android.settings.test"

    @get:Rule
    val serviceRule =
        MessengerServiceRule<SupervisionMessengerClient>(
            TestSupervisionMessengerService::class.java
        )

    @Before
    fun setup() {
        applicationContext = ApplicationProvider.getApplicationContext<Context>()
        shadowPackageManager = shadowOf(applicationContext.packageManager)

        Shadow.extract<ShadowContextImpl>((context as Application).baseContext).apply {
            setSystemService(Context.ROLE_SERVICE, mockSupervisionManager)
        }
    }

    @Test
    fun hasNecessaryComponent_loadInitialFragment() = runTest {
        // Setup necessary supervision component to be present
        mockSupervisionManager.stub {
            on { getRoleHolders(eq(RoleManager.ROLE_SYSTEM_SUPERVISION)) } doReturn
                listOf(testSupervisionPackage)
        }
        val serviceComponentName =
            ComponentName(testSupervisionPackage, "FakeSupervisionMessengerService")
        val intentFilter =
            IntentFilter(SupervisionMessengerClient.SUPERVISION_MESSENGER_SERVICE_BIND_ACTION)
        shadowPackageManager.addServiceIfNotPresent(serviceComponentName)
        shadowPackageManager.addIntentFilterForService(serviceComponentName, intentFilter)

        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)

        activityScenario.onActivity { activity ->
            val fragmentName = activity.getInitialFragmentName(Intent())
            assertThat(fragmentName).isEqualTo(SupervisionDashboardFragment::class.java.name)
        }
    }

    @Test
    fun noNecessaryComponent_startLoadingActivityAndFinishSelf() = runTest {
        // No supervision component to be present
        mockSupervisionManager.stub {
            on { getRoleHolders(eq(RoleManager.ROLE_SYSTEM_SUPERVISION)) } doReturn listOf()
        }

        val activityScenario = ActivityScenario.launch(SupervisionDashboardActivity::class.java)
        val nextActivityIntent = shadowOf(applicationContext as Application).nextStartedActivity

        // Check that the loading activity is started
        assertThat(nextActivityIntent.component?.className)
            .isEqualTo(SupervisionDashboardLoadingActivity::class.java.name)

        // Check that the activity is finished
        assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }
}
