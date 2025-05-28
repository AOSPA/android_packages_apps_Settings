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

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.preference.Preference
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE
import com.android.settings.supervision.TopLevelSupervisionPreferenceController.Companion.SETTINGS_REDIRECT_ACTION
import com.android.settings.supervision.ipc.SupervisionMessengerClient.Companion.SUPERVISION_MESSENGER_SERVICE_BIND_ACTION
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopLevelSupervisionPreferenceControllerTest {
    private val mockRoleManager = mock<RoleManager>()
    private val mockPackageManager = mock<PackageManager>()
    private val context =
        spy(Robolectric.buildActivity(Activity::class.java).get()) {
            on { getSystemService(Context.ROLE_SERVICE) }.thenReturn(mockRoleManager)
            on { packageManager }.thenReturn(mockPackageManager)
        }

    private val preference = Preference(context)

    @Before
    fun setUp() {
        preference.key = PREFERENCE_KEY
    }

    @Test
    fun supervisionPackageNameIsNull_returnUnsupported() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) }.thenReturn(listOf<String>())
        }

        val preferenceController = TopLevelSupervisionPreferenceController(context, PREFERENCE_KEY)
        verify(mockRoleManager).getRoleHolders(any())
        assertThat(preferenceController.availabilityStatus).isEqualTo(UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun noNecessaryComponent_noAppStoreLink_preferenceDisabled() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) }
                .thenReturn(listOf(SUPERVISION_PACKAGE_NAME))
        }

        mockPackageManager.stub {
            on {
                    queryIntentServices(
                        actionIntentMatcher(SUPERVISION_MESSENGER_SERVICE_BIND_ACTION),
                        any<Int>(),
                    )
                }
                .thenReturn(listOf())
        }
        mockPackageManager.stub {
            on { getInstallSourceInfo(any()) }.thenReturn(InstallSourceInfo(null, null, null, null))
        }

        val preferenceController = TopLevelSupervisionPreferenceController(context, PREFERENCE_KEY)

        assertThat(preferenceController.availabilityStatus).isEqualTo(AVAILABLE)
        preferenceController.handlePreferenceTreeClick(preference)
        preferenceController.updateState(preference)

        assertThat(preference.isEnabled).isFalse()
        verify(context)
            .startActivity(componentIntentMatcher(SupervisionDashboardActivity::class.java))
    }

    @Test
    fun hasNecessaryComponent_isFullySupervised_launchFullSupervision() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) }
                .thenReturn(listOf(SUPERVISION_PACKAGE_NAME))
        }

        mockPackageManager.stub {
            on {
                    queryIntentServices(
                        actionIntentMatcher(SUPERVISION_MESSENGER_SERVICE_BIND_ACTION),
                        any<Int>(),
                    )
                }
                .thenReturn(listOf(ResolveInfo()))
        }
        mockPackageManager.stub {
            on {
                    queryIntentActivitiesAsUser(
                        actionIntentMatcher(SETTINGS_REDIRECT_ACTION),
                        any<Int>(),
                        any<Int>(),
                    )
                }
                .thenReturn(listOf(ResolveInfo()))
        }

        val preferenceController = TopLevelSupervisionPreferenceController(context, PREFERENCE_KEY)

        assertThat(preferenceController.availabilityStatus).isEqualTo(AVAILABLE)
        preferenceController.handlePreferenceTreeClick(preference)

        verify(context).startActivity(actionIntentMatcher(SETTINGS_REDIRECT_ACTION))
    }

    @Test
    fun hasNecessaryComponent_isNotFullySupervised_returnAvailable() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) }
                .thenReturn(listOf(SUPERVISION_PACKAGE_NAME))
        }

        mockPackageManager.stub {
            on {
                    queryIntentServices(
                        actionIntentMatcher(SUPERVISION_MESSENGER_SERVICE_BIND_ACTION),
                        any<Int>(),
                    )
                }
                .thenReturn(listOf(ResolveInfo()))
        }
        mockPackageManager.stub {
            on {
                    queryIntentActivitiesAsUser(
                        actionIntentMatcher(SETTINGS_REDIRECT_ACTION),
                        any<Int>(),
                        any<Int>(),
                    )
                }
                .thenReturn(listOf())
        }

        val preferenceController = TopLevelSupervisionPreferenceController(context, PREFERENCE_KEY)

        assertThat(preferenceController.availabilityStatus).isEqualTo(AVAILABLE)

        preferenceController.handlePreferenceTreeClick(preference)
        verify(context)
            .startActivity(componentIntentMatcher(SupervisionDashboardActivity::class.java))
    }

    private fun actionIntentMatcher(action: String) = argThat<Intent> { this.action == action }

    private fun componentIntentMatcher(cls: Class<*>) =
        argThat<Intent> { this.component?.className == cls.name }

    private companion object {
        const val SUPERVISION_PACKAGE_NAME = "com.android.supervision"
        const val PREFERENCE_KEY = "test_key"
    }
}
