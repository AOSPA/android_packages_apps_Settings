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

package com.android.settings.safetycenter

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.SensorPrivacyManager
import android.hardware.SensorPrivacyManager.Sensors
import android.hardware.SensorPrivacyManager.TOGGLE_TYPE_SOFTWARE
import android.location.LocationManager
import android.provider.DeviceConfig
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterEntry
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterManager
import android.safetycenter.SafetyCenterStatus
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settings.overlay.FeatureFactory
import com.android.settings.safetycenter.SafetyCenterTestUtils.EMPTY_SC_DATA
import com.android.settings.safetycenter.SafetyCenterTestUtils.createEntry
import com.android.settings.safetycenter.SafetyCenterTestUtils.createIssue
import com.android.settings.safetycenter.SafetyCenterTestUtils.createScData
import com.android.settings.safetycenter.ui.Action
import com.android.settings.safetycenter.ui.LogSeverityLevel
import com.android.settings.safetycenter.ui.NavigationSource
import com.android.settings.safetycenter.ui.SafetyCenterQsActivity
import com.android.settings.safetycenter.ui.SafetySourceProfileType
import com.android.settings.safetycenter.ui.ViewType
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.InstantTaskExecutorRule
import com.android.settingslib.widget.preference.statusbanner.R as StatusBannerR
import com.google.common.truth.Truth.assertThat
import java.util.HashMap
import java.util.HashSet
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLocationManager
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSafetyCenterManager

@RunWith(AndroidJUnit4::class)
@Config(
    application = Application::class,
    shadows =
        [
            SafetyCenterQsActivityTest.ShadowSensorPrivacyManagerImpl::class,
            SafetyCenterTestUtils.ShadowSettingsStatsLog::class,
        ],
)
class SafetyCenterQsActivityTest {

    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val application: Application = ApplicationProvider.getApplicationContext()
    private lateinit var shadowLocationManager: ShadowLocationManager
    private lateinit var shadowSensorPrivacyManager: ShadowSensorPrivacyManagerImpl
    private lateinit var fakeFeatureFactory: FakeFeatureFactory
    private lateinit var shadowSafetyCenterManager: ShadowSafetyCenterManager
    private lateinit var sensorPrivacyManager: SensorPrivacyManager
    private lateinit var locationManager: LocationManager

    @Before
    fun setUp() {
        fakeFeatureFactory = FakeFeatureFactory()
        FeatureFactory.setFactory(application, fakeFeatureFactory)

        val safetyCenterManager = application.getSystemService(SafetyCenterManager::class.java)!!
        shadowSafetyCenterManager = Shadow.extract(safetyCenterManager)
        shadowSafetyCenterManager.setSafetyCenterEnabled(true)

        locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowLocationManager = shadowOf(locationManager)

        sensorPrivacyManager =
            application.getSystemService(Context.SENSOR_PRIVACY_SERVICE) as SensorPrivacyManager
        shadowSensorPrivacyManager = Shadow.extract(sensorPrivacyManager)
        shadowSensorPrivacyManager.reset()

        // Set all sensors to be supported, and "sensor privacy" disabled (sensor not blocked).
        shadowSensorPrivacyManager.setSensorSupported(
            SensorPrivacyManager.Sensors.CAMERA,
            /* supported= */ true,
        )
        shadowSensorPrivacyManager.setSensorSupported(
            SensorPrivacyManager.Sensors.MICROPHONE,
            /* supported= */ true,
        )
        sensorPrivacyManager.setSensorPrivacy(
            SensorPrivacyManager.Sensors.CAMERA,
            /* enable= */ false,
        )
        sensorPrivacyManager.setSensorPrivacy(
            SensorPrivacyManager.Sensors.MICROPHONE,
            /* enable= */ false,
        )

        shadowLocationManager.setLocationEnabled(true)

        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_PRIVACY,
            "mic_toggle_enabled",
            /* value= */ "true",
            /* makeDefault= */ false,
        )
        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_PRIVACY,
            "camera_toggle_enabled",
            /* value= */ "true",
            /* makeDefault= */ false,
        )

        SafetyCenterTestUtils.ShadowSettingsStatsLog.reset()
    }

    private fun launchActivity(
        data: SafetyCenterData,
        testBlock: (ActivityScenario<SafetyCenterQsActivity>) -> Unit,
    ) {
        val intent =
            Intent(application, SafetyCenterQsActivity::class.java)
                .setAction(Intent.ACTION_VIEW_SAFETY_CENTER_QS)
        shadowSafetyCenterManager.setSafetyCenterData(data)
        ActivityScenario.launch<SafetyCenterQsActivity>(intent).use { scenario ->
            scenario.onActivity { ShadowLooper.idleMainLooper() }
            testBlock(scenario)
        }
    }

    @Test
    fun launchActivity_hasCorrectContentDescriptions() {
        launchActivity(EMPTY_SC_DATA) {
            onView(isRoot()).perform(swipeUp())

            onView(withId(R.id.nested_scroll_view))
                .check(matches(withContentDescription("Security and privacy quick settings")))

            onView(withText("Your privacy controls")).check(matches(isDisplayed()))
            onView(withId(R.id.close_button)).check(matches(withContentDescription("Close")))

            onView(withContentDescription("Switch. Camera access. Available"))
                .check(matches(isDisplayed()))
            onView(withContentDescription("Switch. Mic access. Available"))
                .check(matches(isDisplayed()))
            onView(withContentDescription("Switch. Location access. On"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun launchActivity_toggleCamera_togglesCorrectly() {
        launchActivity(EMPTY_SC_DATA) {
            onView(isRoot()).perform(swipeUp())
            check(
                !sensorPrivacyManager.isSensorPrivacyEnabled(TOGGLE_TYPE_SOFTWARE, Sensors.CAMERA)
            )

            onView(withContentDescription("Switch. Camera access. Available"))
                .perform(scrollTo())
                .perform(click())
            ShadowLooper.idleMainLooper()

            onView(withContentDescription("Switch. Camera access. Blocked"))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            assertThat(
                    sensorPrivacyManager.isSensorPrivacyEnabled(
                        TOGGLE_TYPE_SOFTWARE,
                        Sensors.CAMERA,
                    )
                )
                .isTrue()
        }
    }

    @Test
    fun launchActivity_toggleMic_togglesCorrectly() {
        launchActivity(EMPTY_SC_DATA) {
            onView(isRoot()).perform(swipeUp())
            check(
                !sensorPrivacyManager.isSensorPrivacyEnabled(
                    TOGGLE_TYPE_SOFTWARE,
                    Sensors.MICROPHONE,
                )
            )

            onView(withContentDescription("Switch. Mic access. Available"))
                .perform(scrollTo())
                .perform(click())
            ShadowLooper.idleMainLooper()

            onView(withContentDescription("Switch. Mic access. Blocked"))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            assertThat(
                    sensorPrivacyManager.isSensorPrivacyEnabled(
                        TOGGLE_TYPE_SOFTWARE,
                        Sensors.MICROPHONE,
                    )
                )
                .isTrue()
        }
    }

    @Test
    fun launchActivity_toggleLocation_togglesCorrectly() {
        launchActivity(EMPTY_SC_DATA) {
            onView(isRoot()).perform(swipeUp())
            check(locationManager.isLocationEnabled)

            onView(withContentDescription("Switch. Location access. On"))
                .perform(scrollTo())
                .perform(click())
            ShadowLooper.idleMainLooper()

            onView(withContentDescription("Switch. Location access. Off"))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            assertThat(locationManager.isLocationEnabled).isFalse()
        }
    }

    @Test
    fun launchActivity_clickSettingsToggle_firesSafetyCenterIntent() {
        launchActivity(EMPTY_SC_DATA) {
            onView(isRoot()).perform(swipeUp())

            onView(withText("Settings")).perform(scrollTo()).perform(click())
            ShadowLooper.idleMainLooper()

            val nextStartedActivity = shadowOf(application).nextStartedActivity
            assertThat(nextStartedActivity).isNotNull()
            assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_SAFETY_CENTER)
        }
    }

    @Test
    fun launchActivity_withOkStatus_showsStatusBanner() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()
        launchActivity(createScData(status = status)) {
            onView(withId(StatusBannerR.id.banner_container))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(withText("Title OK")).check(matches(isDisplayed()))
            onView(withText("Summary OK")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun launchActivity_withWarningIssue_showsIssueBanner() {
        val activeIssue =
            createIssue(
                id = "warningIssue",
                title = "Warning Issue Title",
                summary = "Warning Issue Summary",
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
                sourceIds = setOf("any"),
            )
        launchActivity(createScData(activeIssues = listOf(activeIssue))) {
            onView(withText("Warning Issue Title")).check(matches(isDisplayed()))
            onView(withText("Warning Issue Summary")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun statusBanner_withActiveIssue_noButton() {
        val status =
            SafetyCenterStatus.Builder("Title", "Summary")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING)
                .build()
        val activeIssue =
            createIssue(
                id = "warningIssue",
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
                sourceIds = setOf("any"),
            )

        launchActivity(createScData(status = status, activeIssues = listOf(activeIssue))) {
            onView(withText(R.string.safety_center_review_settings)).check(doesNotExist())
            onView(withText(R.string.safety_center_rescan_button)).check(doesNotExist())
        }
    }

    @Test
    fun statusBanner_noIssues_entryUnknownSeverity_showsReviewSettingsButton() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Title",
                sourceId = "AndroidLockScreen",
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN,
            )

        launchActivity(createScData(status = status, entries = listOf(entry))) {
            onView(withText(R.string.safety_center_review_settings)).check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_review_settings)).check(matches(isEnabled()))
            onView(withText(R.string.safety_center_rescan_button)).check(doesNotExist())
        }
    }

    @Test
    fun statusBanner_noIssues_entrySeverityExceedsOverall_showsReviewSettingsButton() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Title",
                sourceId = "AndroidLockScreen",
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_RECOMMENDATION,
            )

        launchActivity(createScData(status = status, entries = listOf(entry))) {
            onView(withText(R.string.safety_center_review_settings)).check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_review_settings)).check(matches(isEnabled()))
            onView(withText(R.string.safety_center_rescan_button)).check(doesNotExist())
        }
    }

    @Test
    fun statusBanner_noIssues_refreshing_scanButtonShown() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .setRefreshStatus(SafetyCenterStatus.REFRESH_STATUS_FULL_RESCAN_IN_PROGRESS)
                .build()
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Title",
                sourceId = "AndroidLockScreen",
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN,
            )

        launchActivity(createScData(status = status, entries = listOf(entry))) {
            onView(withText(R.string.safety_center_review_settings)).check(doesNotExist())
            onView(withText(R.string.safety_center_rescan_button)).check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_rescan_button)).check(matches(isNotEnabled()))
        }
    }

    @Test
    fun statusBanner_noIssues_allEntriesOk_scanButtonShown() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Title",
                sourceId = "AndroidLockScreen",
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
            )

        launchActivity(createScData(status = status, entries = listOf(entry))) {
            onView(withText(R.string.safety_center_review_settings)).check(doesNotExist())
            onView(withText(R.string.safety_center_rescan_button)).check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_rescan_button)).check(matches(isEnabled()))
        }
    }

    @Test
    fun statusBanner_clickReviewSettings_firesSafetyCenterIntentWithNavigationSource() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Title",
                sourceId = "AndroidLockScreen",
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN,
            )

        launchActivity(createScData(status = status, entries = listOf(entry))) {
            onView(withText(R.string.safety_center_review_settings)).perform(click())
            ShadowLooper.idleMainLooper()

            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(Intent.ACTION_SAFETY_CENTER)
            assertThat(NavigationSource.fromIntentOrArguments(startedIntent, /* args= */ null))
                .isEqualTo(NavigationSource.QUICK_SETTINGS_TILE)
        }
    }

    @Test
    fun launchActivity_logsSafetyCenterViewed() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()

        launchActivity(createScData(status = status)) {
            ShadowLooper.idleMainLooper()
            val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
            assertThat(events).hasSize(1)
            val event = events[0]

            assertThat(event.atomId).isEqualTo(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED)
            assertThat(event.action).isEqualTo(Action.SAFETY_CENTER_VIEWED.statsLogValue)
            assertThat(event.viewType).isEqualTo(ViewType.QUICK_SETTINGS.statsLogValue)
            assertThat(event.navigationSource)
                .isEqualTo(NavigationSource.QUICK_SETTINGS_TILE.statsLogValue)
            assertThat(event.severityLevel).isEqualTo(LogSeverityLevel.UNKNOWN.statsLogValue)
            assertThat(event.sourceId).isEqualTo(0)
            assertThat(event.sourceProfileType)
                .isEqualTo(SafetySourceProfileType.UNKNOWN.statsLogValue)
            assertThat(event.issueTypeId).isEqualTo(0)
            assertThat(event.subpageId).isEqualTo(0)
            assertThat(event.issueState)
                .isEqualTo(
                    SettingsStatsLog
                        .SAFETY_CENTER_INTERACTION_REPORTED__ISSUE_STATE__ISSUE_STATE_UNKNOWN
                )
        }
    }

    // NOTE: "Sensor privacy" being enabled means the sensor is blocked.
    @Implements(SensorPrivacyManager::class)
    class ShadowSensorPrivacyManagerImpl {
        private val sensorPrivacyState = HashMap<Int, Boolean>()
        private val listeners = HashSet<SensorPrivacyManager.OnSensorPrivacyChangedListener>()
        private val supportedSensors = HashSet<Int>()

        fun setSensorSupported(sensor: Int, supported: Boolean) {
            if (supported) {
                supportedSensors.add(sensor)
            } else {
                supportedSensors.remove(sensor)
            }
        }

        @Implementation
        fun supportsSensorToggle(sensor: Int): Boolean {
            return supportedSensors.contains(sensor)
        }

        @Implementation
        fun isSensorPrivacyEnabled(toggleType: Int, sensor: Int): Boolean {
            return sensorPrivacyState.getOrDefault(sensor, false)
        }

        @Implementation
        fun setSensorPrivacy(sensor: Int, enabled: Boolean) {
            val changed = sensorPrivacyState[sensor] != enabled
            sensorPrivacyState[sensor] = enabled
            if (changed) {
                notifyListeners(sensor, enabled)
            }
        }

        @Implementation
        fun addSensorPrivacyListener(
            sensor: Int,
            listener: SensorPrivacyManager.OnSensorPrivacyChangedListener,
        ) {
            listeners.add(listener)
        }

        @Implementation
        fun removeSensorPrivacyListener(
            sensor: Int,
            listener: SensorPrivacyManager.OnSensorPrivacyChangedListener,
        ) {
            listeners.remove(listener)
        }

        private fun notifyListeners(sensor: Int, enabled: Boolean) {
            listeners.forEach { it.onSensorPrivacyChanged(sensor, enabled) }
        }

        fun reset() {
            sensorPrivacyState.clear()
            listeners.clear()
            supportedSensors.clear()
        }
    }
}
