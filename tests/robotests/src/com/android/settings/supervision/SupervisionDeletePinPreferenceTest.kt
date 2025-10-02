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
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_DELETE_PIN
import android.app.settings.SettingsEnums.SUPERVISION_MANAGE_PIN_SCREEN
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.UserInfo
import android.net.Uri
import android.os.UserManager
import android.os.UserManager.USER_TYPE_FULL_SECONDARY
import android.os.UserManager.USER_TYPE_FULL_SYSTEM
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings.Global
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.MetricsRule
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.android.settingslib.metadata.PreferenceLifecycleContext
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAlertDialogCompat::class])
class SupervisionDeletePinPreferenceTest {
    @get:Rule val metricsRule = MetricsRule()
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockActivityResultLauncher = mock<ActivityResultLauncher<Intent>>()
    private var startedIntent: Intent? = null
    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    getSystemServiceName(SupervisionManager::class.java) -> mockSupervisionManager
                    getSystemServiceName(UserManager::class.java) -> mockUserManager
                    else -> super.getSystemService(name)
                }

            override fun startActivity(intent: Intent) {
                startedIntent = intent
            }
        }
    private val preference = SupervisionDeletePinPreference()
    private val widget = preference.createWidget(context)
    private val mockLifeCycleContext =
        mock<PreferenceLifecycleContext>(
            useConstructor = withArguments(context),
            defaultAnswer = CALLS_REAL_METHODS,
        )
    private lateinit var activity: ComponentActivity
    private var backPressedCalled = false

    @Before
    fun setUp() {
        backPressedCalled = false
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().resume().get()
        activity.setTheme(R.style.Theme_AppCompat)
        activity.onBackPressedDispatcher.addCallback(activity) { backPressedCalled = true }

        mockLifeCycleContext.stub {
            on { findPreference<Any>(SupervisionDeletePinPreference.KEY) } doReturn widget
            on { registerForActivityResult<Intent, ActivityResult>(any(), any()) } doReturn
                mockActivityResultLauncher
            on { baseContext } doReturn activity
        }
        preference.onCreate(mockLifeCycleContext)
        context.setTheme(R.style.Theme_AppCompat) // Needed for AlertDialog creation
        startedIntent = null
        ShadowToast.reset()
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(R.string.supervision_delete_pin_preference_title)
    }

    @Test
    fun showDeletionDialog_currentUserSupervised_showsConfirmation() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn false
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        preference.showDeletionDialog(context)
        assertAlertDialogHasMessage(R.string.supervision_delete_pin_confirm_message)
    }

    @Test
    fun showDeletionDialog_secondaryUserSupervised_showsSupervisionEnabledWarning() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        preference.showDeletionDialog(context)
        assertAlertDialogHasMessage(R.string.supervision_delete_pin_supervision_enabled_message)
    }

    @Test
    fun showDeletionDialog_secondaryUserSupervised_showsSupervisionEnabledWarning_clicksLearnMore() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }
        val learnMoreLink = context.getString(R.string.supervision_pin_learn_more_link)
        Global.putInt(context.contentResolver, Global.DEVICE_PROVISIONED, 1)
        shadowOf(context.packageManager).apply {
            val componentName = ComponentName(context, "browser")
            val intentFilter =
                IntentFilter(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addDataScheme(Uri.parse(learnMoreLink).scheme)
                }
            addActivityIfNotPresent(componentName)
            addIntentFilterForActivity(componentName, intentFilter)
        }

        preference.showDeletionDialog(context)
        preference.onLearnMore()

        assertThat(startedIntent?.dataString).isEqualTo(learnMoreLink)
        assertThat(startedIntent?.action).isEqualTo(Intent.ACTION_VIEW)
    }

    @Test
    fun onConfirmDeleteClick_startsConfirmPinActivity() {
        preference.onConfirmDeleteClick()
        verifyConfirmPinActivityStarted()
    }

    @Test
    fun onPinConfirmed_removeUserFails_doesNotDeleteSupervisionRecoveryData() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
            on { removeUserEvenWhenDisallowed(SUPERVISING_USER_ID) } doReturn false
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        onActivityResult(ActivityResult(Activity.RESULT_OK, null))
        // Don't disable supervision if we can't delete data, even though we could.
        verify(mockSupervisionManager, never()).isSupervisionEnabled = any()
        verify(mockSupervisionManager, never()).supervisionRecoveryInfo = any()
        assertThat(startedIntent).isNull()
        assertAlertDialogHasMessage(R.string.supervision_delete_pin_error_message)
    }

    @Test
    fun onPinConfirmed_resultCanceled_doesNothing() {
        onActivityResult(ActivityResult(Activity.RESULT_CANCELED, null))

        verify(mockSupervisionManager, never()).isSupervisionEnabled = any()
        verify(mockUserManager, never()).removeUserEvenWhenDisallowed(SUPERVISING_USER_ID)
        verify(mockLifeCycleContext, never()).notifyPreferenceChange(any())
        assertThat(startedIntent).isNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_SNACKBARS_TOAST_MESSAGE)
    fun onPinConfirmed_currentUserSupervised_deletesSupervisionData_flagDisabled_noToast() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
            on { removeUserEvenWhenDisallowed(SUPERVISING_USER_ID) } doReturn true
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn false
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        onActivityResult(ActivityResult(Activity.RESULT_OK, null))

        verify(mockSupervisionManager).supervisionRecoveryInfo = null
        verify(mockSupervisionManager).isSupervisionEnabled = false
        verify(mockUserManager).removeUserEvenWhenDisallowed(eq(SUPERVISING_USER_ID))

        assertThat(backPressedCalled).isTrue()
        assertThat(startedIntent).isNull()
        assertThat(ShadowToast.getTextOfLatestToast()).isNull()

        verify(metricsRule.metricsFeatureProvider)
            .action(mockLifeCycleContext, ACTION_SUPERVISION_DELETE_PIN)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_PIN_SNACKBARS_TOAST_MESSAGE)
    fun onPinConfirmed_currentUserSupervised_deletesSupervisionData_flagEnabled_correctToast() {
        val expectedToastMessage = context.getString(R.string.supervision_pin_deleted)
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
            on { removeUserEvenWhenDisallowed(SUPERVISING_USER_ID) } doReturn true
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn false
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        onActivityResult(ActivityResult(Activity.RESULT_OK, null))

        verify(mockSupervisionManager).supervisionRecoveryInfo = null
        verify(mockSupervisionManager).isSupervisionEnabled = false
        verify(mockUserManager).removeUserEvenWhenDisallowed(eq(SUPERVISING_USER_ID))

        assertThat(backPressedCalled).isTrue()
        assertThat(startedIntent).isNull()
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expectedToastMessage)

        verify(metricsRule.metricsFeatureProvider)
            .action(mockLifeCycleContext, ACTION_SUPERVISION_DELETE_PIN)
    }

    @Test
    fun onPreferenceClick_logsClick() {
        preference.onPreferenceClick(widget)
        verify(metricsRule.metricsFeatureProvider)
            .clicked(SUPERVISION_MANAGE_PIN_SCREEN, SupervisionDeletePinPreference.KEY)
        assertThat(startedIntent).isNull()
    }

    private fun onActivityResult(result: ActivityResult) {
        val captor = argumentCaptor<ActivityResultCallback<ActivityResult>>()
        verify(mockLifeCycleContext)
            .registerForActivityResult<Intent, ActivityResult>(any(), captor.capture())
        captor.allValues.single().onActivityResult(result)
    }

    private fun assertAlertDialogHasMessage(resId: Int) {
        val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
        val shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog)
        assertThat(shadowDialog.message).isEqualTo(appContext.getString(resId))
    }

    private fun verifyConfirmPinActivityStarted() {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockActivityResultLauncher).launch(intentCaptor.capture())
        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        val intent = intentCaptor.firstValue
        assertThat(intent.component?.className)
            .isEqualTo(ConfirmSupervisionCredentialsActivity::class.java.name)
        val extras = intent.extras
        assertThat(extras).isNotNull()
        assertThat(
                extras!!.getBoolean(ConfirmSupervisionCredentialsActivity.EXTRA_FORCE_CONFIRMATION)
            )
            .isTrue()
    }

    companion object {
        private const val MAIN_USER_ID = 0
        private const val SECONDARY_USER_ID = 1
        private const val SUPERVISING_USER_ID = 10
        private val MAIN_USER = UserInfo(MAIN_USER_ID, "Main", null, 0, USER_TYPE_FULL_SYSTEM)
        private val SECONDARY_USER =
            UserInfo(SECONDARY_USER_ID, "Secondary", null, 0, USER_TYPE_FULL_SECONDARY)
        private val SUPERVISING_PROFILE =
            UserInfo(SUPERVISING_USER_ID, "Supervising", null, 0, USER_TYPE_PROFILE_SUPERVISING)
    }
}
