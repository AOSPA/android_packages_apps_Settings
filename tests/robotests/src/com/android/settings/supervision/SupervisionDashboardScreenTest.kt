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
import android.app.Application
import android.app.role.OnRoleHoldersChangedListener
import android.app.role.RoleManager
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.text.SpannableString
import android.text.style.TtsSpan
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags.FLAG_HIDE_SUPERVISION_SETTING_IN_DEMO_MODE
import com.android.settings.supervision.SupervisionMainSwitchPreference.Companion.REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS
import com.android.settings.supervision.credentialmanagement.SupervisionPinManagementScreen
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settings.supervision.webcontentfilters.SupervisionWebContentFiltersScreen
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.ipc.MessengerService
import com.android.settingslib.ipc.MessengerServiceRule
import com.android.settingslib.ipc.PermissionChecker
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.launchFragmentScenario
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers.CALLS_REAL_METHODS
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.UseConstructor.Companion.withArguments
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowRoleManager
import org.robolectric.shadows.ShadowSupervisionManager

private val fakePreferenceDataApi = TestPreferenceDataApiImp()

class FakeSupervisionDashboardScreenMessengerService :
    MessengerService(listOf(fakePreferenceDataApi), PermissionChecker { _, _, _ -> true })

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
@ExperimentalCoroutinesApi
class SupervisionDashboardScreenTest {
    private val preferenceScreenCreator = SupervisionDashboardScreen()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockLifeCycleContext =
        mock<PreferenceLifecycleContext>(
            useConstructor = withArguments(context),
            defaultAnswer = CALLS_REAL_METHODS,
        )
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockRoleManager = mock<RoleManager>()
    private val mockResources = mock<Resources>()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @get:Rule
    val serviceRule =
        MessengerServiceRule<SupervisionMessengerClient>(
            FakeSupervisionDashboardScreenMessengerService::class.java
        )

    @Before
    fun setUp() {
        SupervisionDashboardFragment.ioDispatcher = testDispatcher
        mockLifeCycleContext.stub {
            on { preferenceScreenKey } doReturn preferenceScreenCreator.bindingKey
            on { getSystemService(SupervisionManager::class.java) } doReturn mockSupervisionManager
            on { getSystemService(RoleManager::class.java) } doReturn mockRoleManager
            on { getResources() } doReturn mockResources
        }

        ShadowRoleManager.addRoleHolder(
            RoleManager.ROLE_SYSTEM_SUPERVISION,
            "com.google.android.test.supervision",
            Process.myUserHandle(),
        )
    }

    @After
    fun tearDown() {
        SupervisionDashboardFragment.ioDispatcher = Dispatchers.IO
    }

    @Test
    fun key() = runTest {
        assertThat(preferenceScreenCreator.key).isEqualTo(SupervisionDashboardScreen.KEY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_SCREEN)
    fun flagEnabled() = runTest {
        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_SCREEN)
    fun flagDisabled() = runTest {
        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun toggleMainSwitch_pinVerificationSucceeded_enablesChildPreferences() = runTest {
        preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
            val mainSwitchPreference =
                fragment.findPreference<MainSwitchPreference>(SupervisionMainSwitchPreference.KEY)!!
            val childPreference =
                fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

            assertThat(childPreference.isEnabled).isFalse()

            mainSwitchPreference.performClick()
            // Pretend the PIN verification succeeded.
            fragment.onActivityResult(
                REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS,
                Activity.RESULT_OK,
                null,
            )

            assertThat(childPreference.isEnabled).isTrue()
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun toggleMainSwitch_pinVerificationFailed_childPreferencesRemainDisabled() = runTest {
        preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
            val mainSwitchPreference =
                fragment.findPreference<MainSwitchPreference>(SupervisionMainSwitchPreference.KEY)!!
            val childPreference =
                fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

            assertThat(childPreference.isEnabled).isFalse()

            mainSwitchPreference.performClick()
            // Pretend the PIN verification failed.
            fragment.onActivityResult(
                REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS,
                Activity.RESULT_CANCELED,
                null,
            )

            assertThat(childPreference.isEnabled).isFalse()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onResume_updatesDependentPreferenceSummary() = runTest {
        val initialSummary = "Initial Summary"
        fakePreferenceDataApi.preferenceData =
            mapOf(
                SupervisionWebContentFiltersScreen.KEY to PreferenceData(summary = initialSummary)
            )

        preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
            val webContentFilterPreference =
                fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

            // Idle to ensure all tasks in onResume are completed.
            advanceUntilIdle()
            shadowOf(Looper.getMainLooper()).idle()
            // Add another idle() to ensure all asynchronous tasks triggered by onResume
            // and the first idle() are completed.
            shadowOf(Looper.getMainLooper()).idle()

            assertThat(webContentFilterPreference.summary).isEqualTo(initialSummary)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onResume_updatesDependentPreferenceSummary_withContentDescription() = runTest {
        val initialSummary = "Initial Summary"
        val initialSummaryContentDescription = "Initial Content Description"
        fakePreferenceDataApi.preferenceData =
            mapOf(
                SupervisionWebContentFiltersScreen.KEY to
                    PreferenceData(
                        summary = initialSummary,
                        summaryContentDescription = initialSummaryContentDescription,
                    )
            )

        preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
            val webContentFilterPreference =
                fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

            // Idle to ensure all tasks in onResume are completed.
            advanceUntilIdle()
            shadowOf(Looper.getMainLooper()).idle()
            // Add another idle() to ensure all asynchronous tasks triggered by onResume
            // and the first idle() are completed.
            shadowOf(Looper.getMainLooper()).idle()

            val summary = webContentFilterPreference.summary as SpannableString
            assertThat(summary.toString()).isEqualTo(initialSummary)
            val ttsSpans = summary.getSpans(0, summary.length, TtsSpan::class.java)
            assertThat(ttsSpans.size).isEqualTo(1)
            assertThat(ttsSpans[0].args.getString(TtsSpan.ARG_TEXT))
                .isEqualTo(initialSummaryContentDescription)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun refreshDashboardTiles_updatesDependentPreferenceSummary() = runTest {
        val initialSummary = "Initial Summary"
        fakePreferenceDataApi.preferenceData =
            mapOf(
                SupervisionWebContentFiltersScreen.KEY to PreferenceData(summary = initialSummary)
            )

        preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
            val dashboardFragment = fragment as SupervisionDashboardFragment
            val webContentFilterPreference =
                fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

            // Idle to ensure all tasks in onResume are completed.
            advanceUntilIdle()
            shadowOf(Looper.getMainLooper()).idle()
            // Add another idle() to ensure all asynchronous tasks triggered by onResume
            // and the first idle() are completed.
            shadowOf(Looper.getMainLooper()).idle()

            assertThat(webContentFilterPreference.summary).isEqualTo(initialSummary)

            // Update the preference summary.
            val updatedSummary = "Updated summary"
            webContentFilterPreference.summary = updatedSummary
            assertThat(webContentFilterPreference.summary).isEqualTo(updatedSummary)

            // refreshDashboardTiles should restore the value from the data map.
            dashboardFragment.refreshDashboardTiles("test")

            shadowOf(Looper.getMainLooper()).idle()

            assertThat(webContentFilterPreference.summary).isEqualTo(initialSummary)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun refreshDashboardTiles_updatesDependentPreferenceSummary_withContentDescription() = runTest {
        val initialSummary = "Initial Summary"
        val initialSummaryContentDescription = "Initial Content Description"
        fakePreferenceDataApi.preferenceData =
            mapOf(
                SupervisionWebContentFiltersScreen.KEY to
                    PreferenceData(
                        summary = initialSummary,
                        summaryContentDescription = initialSummaryContentDescription,
                    )
            )

        preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
            val dashboardFragment = fragment as SupervisionDashboardFragment
            val webContentFilterPreference =
                fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

            // Idle to ensure all tasks in onResume are completed.
            advanceUntilIdle()
            shadowOf(Looper.getMainLooper()).idle()
            // Add another idle() to ensure all asynchronous tasks triggered by onResume
            // and the first idle() are completed.
            shadowOf(Looper.getMainLooper()).idle()

            var summary = webContentFilterPreference.summary as SpannableString
            assertThat(summary.toString()).isEqualTo(initialSummary)
            var ttsSpans = summary.getSpans(0, summary.length, TtsSpan::class.java)
            assertThat(ttsSpans.size).isEqualTo(1)
            assertThat(ttsSpans[0].args.getString(TtsSpan.ARG_TEXT))
                .isEqualTo(initialSummaryContentDescription)

            // Update the preference summary.
            val updatedSummary = "Updated summary"
            webContentFilterPreference.summary = updatedSummary
            assertThat(webContentFilterPreference.summary).isEqualTo(updatedSummary)

            // refreshDashboardTiles should restore the value from the data map.
            dashboardFragment.refreshDashboardTiles("test")

            shadowOf(Looper.getMainLooper()).idle()

            summary = webContentFilterPreference.summary as SpannableString
            assertThat(summary.toString()).isEqualTo(initialSummary)
            ttsSpans = summary.getSpans(0, summary.length, TtsSpan::class.java)
            assertThat(ttsSpans.size).isEqualTo(1)
            assertThat(ttsSpans[0].args.getString(TtsSpan.ARG_TEXT))
                .isEqualTo(initialSummaryContentDescription)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun clickDashboardPreference_withSupervisionDisabled_launchesSetupSupervisionActivity() =
        runTest {
            shadowOf(context as Application)
                .setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)

            preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
                val dashboardFragment = fragment as SupervisionDashboardFragment
                val childPreference =
                    fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

                // With supervision disabled, the preference click should redirect to
                // SetupSupervisionActivity after refreshDashboardTiles is called.
                mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn false }
                dashboardFragment.refreshDashboardTiles("test")

                childPreference.performClick()

                val intent = shadowOf(fragment.activity).nextStartedActivity
                assertThat(intent.component?.className)
                    .isEqualTo(SetupSupervisionActivity::class.java.name)
            }
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun clickDashboardPreference_supervisionEnabled_noPinSet_launchesSetupSupervisionActivity() =
        runTest {
            val supervisionManager = context.getSystemService(SupervisionManager::class.java)
            val shadowSupervisionManager: ShadowSupervisionManager =
                Shadow.extract(supervisionManager)

            // Supervision enabled
            shadowSupervisionManager.overrideSupervisionEnabled(true)
            // Pin not set
            shadowSupervisionManager.overrideCredentialType(-1)

            preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
                val dashboardFragment = fragment as SupervisionDashboardFragment
                val childPreference =
                    fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

                dashboardFragment.refreshDashboardTiles("test")

                childPreference.performClick()

                val intent = shadowOf(fragment.activity).nextStartedActivity
                assertThat(intent).isNotNull()
                assertThat(intent.component?.className)
                    .isEqualTo(SetupSupervisionActivity::class.java.name)
            }
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun refreshDashboardTiles_withSupervisionEnabled_restoresOriginalListener() = runTest {
        shadowOf(context as Application)
            .setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)

        preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
            val dashboardFragment = fragment as SupervisionDashboardFragment
            val childPreference =
                fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

            // Pin is set
            mockSupervisionManager.stub {
                on { createConfirmSupervisionCredentialsIntent() } doReturn Intent("FAKE_ACTION")
            }

            // Store the original listener.
            mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn true }
            dashboardFragment.refreshDashboardTiles("enables supervision")
            val originalListener = childPreference.onPreferenceClickListener

            // With supervision disabled, the preference click listener should be updated.
            mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn false }
            dashboardFragment.refreshDashboardTiles("disables supervision")
            assertThat(childPreference.onPreferenceClickListener)
                .isNotSameInstanceAs(originalListener)

            // With supervision enabled, the preference click listener should be restored.
            mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn true }
            dashboardFragment.refreshDashboardTiles("enables supervision again")
            assertThat(childPreference.onPreferenceClickListener).isSameInstanceAs(originalListener)
        }
    }

    @Test
    fun getPreferenceScreenBindingKey_returnsScreenKey() = runTest {
        preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
            assertThat(
                    (fragment as SupervisionDashboardFragment).getPreferenceScreenBindingKey(
                        mockLifeCycleContext
                    )
                )
                .isEqualTo(SupervisionDashboardScreen.KEY)
        }
    }

    @Test
    fun getTitle() = runTest {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.supervision_settings_title)
    }

    @Test
    fun getKeywords() = runTest {
        assertThat(preferenceScreenCreator.keywords)
            .isEqualTo(R.string.keywords_supervision_settings)
    }

    @Test
    @DisableFlags(FLAG_HIDE_SUPERVISION_SETTING_IN_DEMO_MODE)
    fun isAvailable() = runTest {
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(FLAG_HIDE_SUPERVISION_SETTING_IN_DEMO_MODE)
    fun isAvailable_inDemoMode_configHidingTrue_isFalse() = runTest {
        SettingsGlobalStore.get(mockLifeCycleContext).setInt(Settings.Global.DEVICE_DEMO_MODE, 1)

        mockResources.stub {
            on { getBoolean(R.bool.config_hide_supervision_setting_in_demo_mode) }.thenReturn(true)
        }

        assertThat(preferenceScreenCreator.isAvailable(mockLifeCycleContext)).isFalse()

        // Ensure reset to non-demo-mode at the end of test.
        SettingsGlobalStore.get(context).setInt(Settings.Global.DEVICE_DEMO_MODE, 0)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreate_registersListener() = runTest {
        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(any())
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onDestroy_unregistersListener() = runTest {
        val listenerCaptor = argumentCaptor<SupervisionManager.SupervisionListener>()

        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(listenerCaptor.capture())

        preferenceScreenCreator.onDestroy(mockLifeCycleContext)
        verify(mockSupervisionManager).unregisterSupervisionListener(listenerCaptor.firstValue)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onResume_addsOnRoleHoldersChangedListener() = runTest {
        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        preferenceScreenCreator.onResume(mockLifeCycleContext)
        verify(mockRoleManager)
            .addOnRoleHoldersChangedListenerAsUser(
                any<Executor>(),
                any<OnRoleHoldersChangedListener>(),
                any<UserHandle>(),
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onStart_registerSupervisionListener() = runTest {
        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        preferenceScreenCreator.onStart(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(any())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onPause_removeOnRoleHoldersChangedListener() = runTest {
        val roleListenerCaptor = argumentCaptor<OnRoleHoldersChangedListener>()

        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        preferenceScreenCreator.onResume(mockLifeCycleContext)
        verify(mockRoleManager)
            .addOnRoleHoldersChangedListenerAsUser(
                any<Executor>(),
                roleListenerCaptor.capture(),
                any<UserHandle>(),
            )

        preferenceScreenCreator.onPause(mockLifeCycleContext)
        verify(mockRoleManager)
            .removeOnRoleHoldersChangedListenerAsUser(roleListenerCaptor.firstValue, UserHandle.ALL)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onStop_unregisterSupervisionListener() = runTest {
        val supervisionListenerCaptor = argumentCaptor<SupervisionManager.SupervisionListener>()

        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        preferenceScreenCreator.onStart(mockLifeCycleContext)
        verify(mockSupervisionManager)
            .registerSupervisionListener(supervisionListenerCaptor.capture())

        preferenceScreenCreator.onStop(mockLifeCycleContext)
        verify(mockSupervisionManager)
            .unregisterSupervisionListener(supervisionListenerCaptor.firstValue)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun listener_withoutUiUpdates_onSupervisionDisabled_refreshesPreferences() = runTest {
        val listenerCaptor = argumentCaptor<SupervisionManager.SupervisionListener>()

        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(listenerCaptor.capture())

        listenerCaptor.firstValue.onSupervisionDisabled(0 /* userId */)

        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionDashboardScreen.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionMainSwitchPreference.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun listener_withoutUiUpdates_onSupervisionEnabled_refreshesPreferences() = runTest {
        val listenerCaptor = argumentCaptor<SupervisionManager.SupervisionListener>()

        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(listenerCaptor.capture())

        listenerCaptor.firstValue.onSupervisionEnabled(0 /* userId */)

        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionDashboardScreen.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionMainSwitchPreference.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun listener_onSupervisionDisabled_refreshesPreferences() = runTest {
        val listenerCaptor = argumentCaptor<SupervisionManager.SupervisionListener>()

        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        preferenceScreenCreator.onStart(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(listenerCaptor.capture())

        listenerCaptor.firstValue.onSupervisionDisabled(0 /* userId */)

        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionDashboardScreen.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionSetUpPinPreference.KEY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun listener_onSupervisionEnabled_refreshesPreferences() = runTest {
        val listenerCaptor = argumentCaptor<SupervisionManager.SupervisionListener>()

        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        preferenceScreenCreator.onStart(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(listenerCaptor.capture())

        listenerCaptor.firstValue.onSupervisionEnabled(0 /* userId */)

        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionDashboardScreen.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionSetUpPinPreference.KEY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onResume_supervisionSettingsUiUpdatesEnabled_nonEmptySupervisionAppsList_supervisionAppsGroupVisible() =
        runTest {
            val testPackageName = "com.android.settings.test"
            val testLabel = "testLabel"
            val testActivity = "com.android.settings.test.TestActivity"
            val testIcon = ColorDrawable(Color.RED)

            mockRoleManager.stub { on { getRoleHolders(any()) } doReturn listOf(testPackageName) }

            val testActivityInfo =
                mock<ActivityInfo>() {
                    on { loadIcon(any()) } doReturn testIcon
                    on { loadLabel(any()) } doReturn testLabel
                }
            testActivityInfo.packageName = testPackageName
            testActivityInfo.name = testActivity
            val resolveInfoList = listOf(ResolveInfo().apply { activityInfo = testActivityInfo })
            val mockPackageManager =
                mock<PackageManager> {
                    on { queryIntentActivities(any<Intent>(), anyInt()) } doReturn resolveInfoList
                }
            mockLifeCycleContext.stub { on { packageManager } doReturn mockPackageManager }
            val supervisionAppsGroup = PreferenceCategory(context)
            PreferenceManager(context)
                .createPreferenceScreen(context)
                .addPreference(supervisionAppsGroup)
            mockLifeCycleContext.stub {
                on {
                    findPreference<PreferenceGroup>(
                        SupervisionDashboardScreen.ACTIVE_SUPERVISION_APPS_GROUP
                    )
                } doReturn supervisionAppsGroup
            }

            preferenceScreenCreator.onCreate(mockLifeCycleContext)
            preferenceScreenCreator.onResume(mockLifeCycleContext)

            assertThat(supervisionAppsGroup.isVisible).isTrue()
            assertThat(supervisionAppsGroup.preferenceCount).isEqualTo(1)
            val preference = supervisionAppsGroup.getPreference(0)
            assertThat(preference.intent?.action)
                .isEqualTo(Settings.MANAGE_SUPERVISION_APP_SETTINGS)
            assertThat(preference.widgetLayoutResource)
                .isEqualTo(R.layout.preference_external_action_icon)
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onResume_supervisionSettingsUiUpdatesDisabled_emptySupervisionAppsList_supervisionAppsGroupIsNotVisible() =
        runTest {
            mockRoleManager.stub { on { getRoleHolders(any()) } doReturn emptyList() }
            val supervisionAppsGroup = PreferenceCategory(context, null)
            mockLifeCycleContext.stub {
                on {
                    findPreference<PreferenceGroup>(
                        SupervisionDashboardScreen.ACTIVE_SUPERVISION_APPS_GROUP
                    )
                } doReturn supervisionAppsGroup
            }

            preferenceScreenCreator.onCreate(mockLifeCycleContext)
            preferenceScreenCreator.onResume(mockLifeCycleContext)

            assertThat(supervisionAppsGroup.isVisible).isFalse()
            assertThat(supervisionAppsGroup.preferenceCount).isEqualTo(0)
        }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onResume_supervisionSettingsUiUpdatesDisabled_doNotAttemptToLoadSupervisionAppsList() =
        runTest {
            preferenceScreenCreator.onResume(mockLifeCycleContext)

            verify(mockLifeCycleContext, never())
                .findPreference<PreferenceGroup>(
                    SupervisionDashboardScreen.ACTIVE_SUPERVISION_APPS_GROUP
                )
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onResume_supervisionSettingsUiUpdatesEnabled_refreshPinManagementPreference() = runTest {
        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        preferenceScreenCreator.onResume(mockLifeCycleContext)

        verify(mockLifeCycleContext, times(1))
            .notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
    }
}
