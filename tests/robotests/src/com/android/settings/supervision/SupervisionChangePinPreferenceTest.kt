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
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.UserInfo
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.password.ChooseLockGeneric
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers.CALLS_REAL_METHODS
import org.mockito.kotlin.UseConstructor.Companion.withArguments
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
class SupervisionChangePinPreferenceTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val mockChangePinLauncher = mock<ActivityResultLauncher<Intent>>()
    private val mockUserManager = mock<UserManager>()
    private val mockKeyguardManager = mock<KeyguardManager>()
    private val preference = SupervisionChangePinPreference()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    USER_SERVICE -> mockUserManager
                    KEYGUARD_SERVICE -> mockKeyguardManager
                    else -> super.getSystemService(name)
                }
        }

    private val mockLifeCycleContext =
        mock<PreferenceLifecycleContext>(
            useConstructor = withArguments(context),
            defaultAnswer = CALLS_REAL_METHODS,
        )

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Before
    fun setUp() {
        whenever(
                mockLifeCycleContext.registerForActivityResult(
                    any<ActivityResultContracts.StartActivityForResult>(),
                    any(),
                )
            )
            .thenReturn(mockChangePinLauncher)
        preference.onCreate(mockLifeCycleContext)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(R.string.supervision_change_pin_preference_title)
    }

    @Test
    fun onPreferenceClick_whenSupervisingCredentialSet_launchesCorrectIntent() {
        val widget: Preference = preference.createAndBindWidget(context)

        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionChangePinPreference.KEY) } doReturn widget
        }

        whenever(mockUserManager.users).thenReturn(listOf(SUPERVISING_USER_INFO))
        whenever(mockKeyguardManager.isDeviceSecure(SUPERVISING_USER_ID)).thenReturn(true)
        val result = preference.onPreferenceClick(widget)
        assertThat(result).isTrue()
        verifyChangePinActivityStarted(SUPERVISING_USER_ID)
    }

    @Test
    fun onPreferenceClick_whenSupervisingCredentialNotSet_returnsFalse() {
        val widget: Preference = preference.createAndBindWidget(context)

        mockLifeCycleContext.stub {
            on { findPreference<Preference>(SupervisionChangePinPreference.KEY) } doReturn widget
        }

        whenever(mockUserManager.users).thenReturn(emptyList())
        val result = preference.onPreferenceClick(widget)
        assertThat(result).isFalse()
        verify(mockChangePinLauncher, never()).launch(any())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_SNACKBARS_TOAST_MESSAGE)
    fun onChangePinComplete_showsCorrectToastString() {
        val expectedToastMessage = context.getString(R.string.supervision_pin_changed)
        preference.onChangePinComplete(ActivityResult(Activity.RESULT_OK, null))
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expectedToastMessage)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_SNACKBARS_TOAST_MESSAGE)
    fun onChangePinComplete_flagDisabled_noToast() {
        preference.onChangePinComplete(ActivityResult(Activity.RESULT_OK, null))
        assertThat(ShadowToast.getTextOfLatestToast()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_SNACKBARS_TOAST_MESSAGE)
    fun onChangePinComplete_flowCanceled_noToast() {
        preference.onChangePinComplete(ActivityResult(Activity.RESULT_CANCELED, null))
        assertThat(ShadowToast.getTextOfLatestToast()).isNull()
    }

    private fun verifyChangePinActivityStarted(expectedUserId: Int) {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockChangePinLauncher).launch(intentCaptor.capture())
        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        val intent = intentCaptor.firstValue
        assertThat(intent.component?.className).isEqualTo(ChooseLockGeneric::class.java.name)
        assertThat(intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, 0))
            .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
        assertThat(intent.getIntExtra(Intent.EXTRA_USER_ID, 0)).isEqualTo(expectedUserId)
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
