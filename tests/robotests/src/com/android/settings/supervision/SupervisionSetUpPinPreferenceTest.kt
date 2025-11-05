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

import android.app.KeyguardManager
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_SET_UP_PIN_ENTRY
import android.app.supervision.SupervisionManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.UserInfo
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.flag.junit.SetFlagsRule
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.preference.Preference
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.MetricsRule
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SupervisionSetUpPinPreferenceTest {
    private val mockKeyguardManager = mock<KeyguardManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockSupervisionManager = mock<SupervisionManager>()

    @get:Rule val metricsRule = MetricsRule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    Context.KEYGUARD_SERVICE -> mockKeyguardManager
                    Context.USER_SERVICE -> mockUserManager
                    Context.SUPERVISION_SERVICE -> mockSupervisionManager
                    else -> super.getSystemService(name)
                }
        }

    private val supervisionSetUpPinPreference = SupervisionSetUpPinPreference()

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
    fun onPreferenceClick_launchesCorrectIntent() {
        ActivityScenario.launch(EmptyFragmentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val widget: Preference = supervisionSetUpPinPreference.createAndBindWidget(activity)

                val result = supervisionSetUpPinPreference.onPreferenceClick(widget)

                assertThat(result).isTrue()
                val intent = shadowOf(activity).nextStartedActivity
                assertThat(intent.component?.className)
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
