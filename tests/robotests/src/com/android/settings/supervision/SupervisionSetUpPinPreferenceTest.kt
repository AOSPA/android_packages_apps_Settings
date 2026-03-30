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
import android.app.KeyguardManager
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_SET_UP_PIN_ENTRY
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import android.content.pm.UserInfo
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.preference.Preference
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.supervision.credentialmanagement.SupervisionPinManagementScreen
import com.android.settings.testutils.MetricsRule
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.kotlin.UseConstructor.Companion.withArguments
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl

@RunWith(AndroidJUnit4::class)
class SupervisionSetUpPinPreferenceTest {
    private val mockKeyguardManager = mock<KeyguardManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()

    @get:Rule(order = 0) val setFlagsRule = SetFlagsRule()
    @get:Rule(order = 1) val metricsRule = MetricsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockLifeCycleContext =
        mock<PreferenceLifecycleContext>(
            useConstructor = withArguments(context),
            defaultAnswer = Answers.CALLS_REAL_METHODS,
        )
    private val mockConfirmCredentialsLauncher = mock<ActivityResultLauncher<Intent>>()

    private val supervisionSetUpPinPreference = SupervisionSetUpPinPreference()

    @Before
    fun setUp() {
        Shadow.extract<ShadowContextImpl>((context as Application).baseContext).apply {
            setSystemService(Context.KEYGUARD_SERVICE, mockKeyguardManager)
            setSystemService(Context.USER_SERVICE, mockUserManager)
            setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)
        }
        whenever(
                mockLifeCycleContext.registerForActivityResult(
                    any<ActivityResultContracts.StartActivityForResult>(),
                    any(),
                )
            )
            .thenReturn(mockConfirmCredentialsLauncher)
        supervisionSetUpPinPreference.onCreate(mockLifeCycleContext)
    }

    @Test
    fun key() {
        assertThat(supervisionSetUpPinPreference.key).isEqualTo(SupervisionSetUpPinPreference.KEY)
    }

    @Test
    fun isAvailable_hasSupervisingCredential_returnsFalse() {
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(true)

        assertThat(supervisionSetUpPinPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noSupervisingUser_returnsTrue() {
        whenever(mockUserManager.users).thenReturn(emptyList())
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(true)

        assertThat(supervisionSetUpPinPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noSupervisingCredential_returnsTrue() {
        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(false)

        assertThat(supervisionSetUpPinPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun getTitle() {
        assertThat(supervisionSetUpPinPreference.title)
            .isEqualTo(R.string.supervision_set_up_pin_preference_title)
    }

    @Test
    fun dependencies_returnsCorrectKey() {
        // Verifies that the preference depends on the PIN management screen.
        val dependencies = supervisionSetUpPinPreference.dependencies(context)

        // Assert that the correct dependency key is returned.
        assertThat(dependencies).hasLength(1)
        assertThat(dependencies[0]).isEqualTo(SupervisionPinManagementScreen.KEY)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_PARENT_APPROVAL_FOR_PIN_SETUP)
    fun onPreferenceClick_launchesPinSetupFlowIntent() {
        ActivityScenario.launch(EmptyFragmentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val widget: Preference = supervisionSetUpPinPreference.createAndBindWidget(activity)

                val result = supervisionSetUpPinPreference.onPreferenceClick(widget)

                assertThat(result).isTrue()
                val intent = shadowOf(activity).nextStartedActivity
                assertThat(intent.component?.className)
                    .isEqualTo(SetupSupervisionActivity::class.java.name)

                verify(mockConfirmCredentialsLauncher, never()).launch(any())
            }
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PARENT_APPROVAL_FOR_PIN_SETUP)
    fun onPreferenceClick_hasOtherApprovalMethods_launchesCredentialIsConfirmed() {
        val expectedIntent = mock<Intent>()
        whenever(mockSupervisionManager.createConfirmSupervisionCredentialsIntent())
            .thenReturn(expectedIntent)
        val widget: Preference = supervisionSetUpPinPreference.createAndBindWidget(context)

        val result = supervisionSetUpPinPreference.onPreferenceClick(widget)

        assertThat(result).isTrue()

        val intentCaptor = argumentCaptor<Intent>()
        verify(mockConfirmCredentialsLauncher).launch(intentCaptor.capture())
        assertThat(intentCaptor.firstValue).isEqualTo(expectedIntent)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PARENT_APPROVAL_FOR_PIN_SETUP)
    fun onPreferenceClick_noOtherApprovalMethods_launchesPinSetupFlowIntent() {
        whenever(mockSupervisionManager.createConfirmSupervisionCredentialsIntent())
            .thenReturn(null)
        ActivityScenario.launch(EmptyFragmentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val widget: Preference = supervisionSetUpPinPreference.createAndBindWidget(activity)

                val result = supervisionSetUpPinPreference.onPreferenceClick(widget)

                assertThat(result).isTrue()
                verify(mockConfirmCredentialsLauncher, never()).launch(any())

                val activityShadow = shadowOf(activity)
                val pinSetupIntent = activityShadow.nextStartedActivity
                assertThat(pinSetupIntent.component?.className)
                    .isEqualTo(SetupSupervisionActivity::class.java.name)
            }
        }
    }

    @Test
    fun onPreferenceClick_logsMetrics() {
        ActivityScenario.launch(EmptyFragmentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val widget: Preference = supervisionSetUpPinPreference.createAndBindWidget(activity)

                widget.performClick()

                verify(metricsRule.metricsFeatureProvider)
                    .action(any(), eq(ACTION_SUPERVISION_SET_UP_PIN_ENTRY))
            }
        }
    }

    private companion object {
        const val SUPERVISING_USER_ID = 5
        val SUPERVISING_USER_INFO =
            UserInfo(
                SUPERVISING_USER_ID,
                /* name */ "supervising",
                /* iconPath */ "",
                /* flags */ 0,
                USER_TYPE_PROFILE_SUPERVISING,
            )
    }
}
