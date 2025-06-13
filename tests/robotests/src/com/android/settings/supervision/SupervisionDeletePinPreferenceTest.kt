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
import android.app.supervision.SupervisionManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.UserInfo
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_FULL_SECONDARY
import android.os.UserManager.USER_TYPE_FULL_SYSTEM
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAlertDialogCompat::class])
class SupervisionDeletePinPreferenceTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockActivityResultLauncher = mock<ActivityResultLauncher<Intent>>()
    private var startedIntent: Intent? = null
    private var notifiedKey: String? = null
    private var capturedActivityResultCallback: ActivityResultCallback<ActivityResult>? = null
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
    // This object is created explicitly instead of mocked in order to preserve access to the
    // original context in test.
    private val lifeCycleContext =
        object : PreferenceLifecycleContext(context) {
            override val lifecycleScope: LifecycleCoroutineScope
                get() = mock {} // unused

            override val fragmentManager: FragmentManager
                get() = mock {} // unused

            override val childFragmentManager: FragmentManager
                get() = mock {} // unused

            override fun <T> findPreference(key: String): T? {
                if (key == SupervisionDeletePinPreference.KEY) {
                    return widget as T?
                }
                return null
            }

            override fun <T : Any> requirePreference(key: String) = findPreference<T>(key)!!

            override fun getKeyValueStore(key: String): KeyValueStore? = null

            override fun notifyPreferenceChange(key: String) {
                notifiedKey = key
            }

            @Suppress("DEPRECATION")
            override fun startActivityForResult(
                intent: Intent,
                requestCode: Int,
                options: Bundle?,
            ) {}

            override fun <I, O> registerForActivityResult(
                contract: ActivityResultContract<I, O>,
                callback: ActivityResultCallback<O>,
            ): ActivityResultLauncher<I> {
                capturedActivityResultCallback = callback as? ActivityResultCallback<ActivityResult>
                return mockActivityResultLauncher as ActivityResultLauncher<I>
            }
        }

    @Before
    fun setUp() {
        preference.onCreate(lifeCycleContext)
        context.setTheme(R.style.Theme_AppCompat) // Needed for AlertDialog creation
        startedIntent = null
        notifiedKey = null
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(R.string.supervision_delete_pin_preference_title)
    }

    @Test
    fun getSummary() {
        assertThat(preference.summary).isEqualTo(R.string.supervision_delete_pin_preference_summary)
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
    fun areAnyUsersSupervisedExceptCurrent_currentUserSupervised_returnsFalse() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn false
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        assertThat(
                preference.areAnyUsersExceptCurrentSupervised(
                    mockSupervisionManager,
                    mockUserManager,
                )
            )
            .isFalse()
    }

    @Test
    fun areAnyUsersSupervisedExceptCurrent_secondaryUserSupervised_returnsTrue() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        assertThat(
                preference.areAnyUsersExceptCurrentSupervised(
                    mockSupervisionManager,
                    mockUserManager,
                )
            )
            .isTrue()
    }

    @Test
    fun onConfirmDeleteClick_currentUserSupervised_deletesSupervisionData() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
            on { removeUser(UserHandle(SUPERVISING_USER_ID)) } doReturn true
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn false
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        preference.onConfirmDeleteClick()
        verifyConfirmPinActivityStarted()

        val result = ActivityResult(Activity.RESULT_OK, null)
        capturedActivityResultCallback?.onActivityResult(result)
        verify(mockSupervisionManager).setSupervisionRecoveryInfo(null)
        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(mockUserManager).removeUser(eq(UserHandle(SUPERVISING_USER_ID)))
        assertThat(startedIntent).isNotNull()
        assertThat(notifiedKey).isEqualTo(SupervisionDeletePinPreference.KEY)
    }

    @Test
    fun onConfirmDeleteClick_removeUserFails_doesNotDeleteSupervisionRecoveryData() {
        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
            on { removeUser(UserHandle(SUPERVISING_USER_ID)) } doReturn false
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        preference.onConfirmDeleteClick()
        verifyConfirmPinActivityStarted()

        val result = ActivityResult(Activity.RESULT_OK, null)
        capturedActivityResultCallback?.onActivityResult(result)
        // We should disable supervision before the supervising profile is removed
        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(mockSupervisionManager, never()).setSupervisionRecoveryInfo(any())
        assertThat(startedIntent).isNull()
        assertAlertDialogHasMessage(R.string.supervision_delete_pin_error_message)
    }

    @Test
    fun onPinConfirmed_resultCanceled_doesNothing() {
        val result = ActivityResult(Activity.RESULT_CANCELED, null)
        capturedActivityResultCallback?.onActivityResult(result)

        verify(mockSupervisionManager, never()).setSupervisionEnabled(any())
        verify(mockUserManager, never()).removeUser(UserHandle(SUPERVISING_USER_ID))
        assertThat(startedIntent).isNull()
        assertThat(notifiedKey).isNull()
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
