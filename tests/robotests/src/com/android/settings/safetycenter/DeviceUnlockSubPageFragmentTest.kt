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

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.UserInfo
import android.os.UserManager
import android.permission.flags.Flags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterManager
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.safetycenter.SafetyCenterTestUtils.EMPTY_SC_DATA
import com.android.settings.safetycenter.SafetyCenterTestUtils.TEST_ACTION
import com.android.settings.safetycenter.SafetyCenterTestUtils.USER_PERSONAL
import com.android.settings.safetycenter.SafetyCenterTestUtils.USER_WORK_PROFILE
import com.android.settings.safetycenter.SafetyCenterTestUtils.createEntry
import com.android.settings.safetycenter.SafetyCenterTestUtils.createScData
import com.android.settings.safetycenter.ui.DeviceUnlockSubPageFragment
import com.android.settingslib.safetycenter.SafetySourcePreference
import com.android.settingslib.widget.IllustrationPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSafetyCenterManager
import org.robolectric.shadows.ShadowUserManager

// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs.
@SuppressLint("MissingPermission")
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
class DeviceUnlockSubPageFragmentTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private lateinit var application: Application
    private lateinit var shadowSafetyCenterManager: ShadowSafetyCenterManager

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        val userManager = application.getSystemService(UserManager::class.java)!!
        val shadowUserManager = Shadow.extract(userManager) as ShadowUserManager
        val safetyCenterManager = application.getSystemService(SafetyCenterManager::class.java)!!
        shadowSafetyCenterManager = Shadow.extract(safetyCenterManager)

        val shadowContextImpl = Shadow.extract<ShadowContextImpl>(application.baseContext)
        shadowContextImpl.setSystemService(Context.USER_SERVICE, userManager)

        shadowUserManager.addUser(USER_PERSONAL.identifier, "Personal", 0)
        shadowUserManager.addProfile(
            USER_PERSONAL.identifier,
            USER_WORK_PROFILE.identifier,
            "Work Profile",
            UserInfo.FLAG_MANAGED_PROFILE,
        )
        shadowSafetyCenterManager.setSafetyCenterEnabled(true)
    }

    private fun runTest(data: SafetyCenterData, testBlock: (DeviceUnlockSubPageFragment) -> Unit) {
        shadowSafetyCenterManager.setSafetyCenterData(data)
        val scenario =
            launchFragmentInContainer<DeviceUnlockSubPageFragment>(
                themeResId = R.style.Theme_SubSettings
            )
        scenario.onFragment { fragment ->
            ShadowLooper.idleMainLooper()
            testBlock(fragment)
        }
        scenario.close()
    }

    @Test
    fun illustrationPreference_always_isVisible() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val illustration =
                fragment.findPreference<IllustrationPreference>(DEVICE_UNLOCK_ILLUSTRATION_KEY)
            assertThat(illustration?.isVisible).isTrue()
        }
    }

    @Test
    fun lockScreenPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "lockScreenEntry",
                title = "Screen Lock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                summary = "set screen lock",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(ANDROID_LOCK_SCREEN_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun lockScreenPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(ANDROID_LOCK_SCREEN_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun lockScreenPref_whenDataChanges_uiIsUpdated() {
        // Initial state: No lock screen entry
        shadowSafetyCenterManager.setSafetyCenterData(EMPTY_SC_DATA)
        val scenario =
            launchFragmentInContainer<DeviceUnlockSubPageFragment>(
                themeResId = R.style.Theme_SubSettings
            )

        scenario.onFragment { fragment ->
            ShadowLooper.idleMainLooper()
            val preference =
                fragment.findPreference<SafetySourcePreference>(ANDROID_LOCK_SCREEN_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()

            // Data change: Add lock screen entry
            val newEntry =
                createEntry(
                    id = "lockScreenEntry",
                    title = "Screen Lock",
                    userHandle = USER_PERSONAL,
                    sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                    summary = "set screen lock",
                )
            shadowSafetyCenterManager.setSafetyCenterData(createScData(listOf(newEntry)))
            ShadowLooper.idleMainLooper()

            // UI should update to reflect the new state
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(newEntry.title)
            assertThat(preference?.summary.toString()).isEqualTo(newEntry.summary)
        }
        scenario.close()
    }

    @Test
    fun fingerprintPersonalPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "fingerprint_personal",
                title = "Fingerprint",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID,
                summary = "2 fingerprints added",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    FINGERPRINT_UNLOCK_PERSONAL_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun fingerprintPersonalPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    FINGERPRINT_UNLOCK_PERSONAL_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun fingerprintWorkPref_whenWorkProfileAndEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "fingerprint_work",
                title = "Fingerprint For work",
                userHandle = USER_WORK_PROFILE,
                sourceId = ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID,
                summary = "Work fingerprint",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    FINGERPRINT_UNLOCK_WORK_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun fingerprintWorkPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    FINGERPRINT_UNLOCK_WORK_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun fingerprintWorkPref_whenNoWorkProfileEntry_isHidden() {
        val entry =
            createEntry(
                id = "fingerprint_personal",
                title = "Fingerprint",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID,
                summary = "2 fingerprints added",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    FINGERPRINT_UNLOCK_WORK_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun faceUnlockPersonalPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "face_personal",
                title = "Face Unlock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                summary = "face added",
            )
        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(FACE_UNLOCK_PERSONAL_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun faceUnlockPersonalPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(FACE_UNLOCK_PERSONAL_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun faceUnlockWorkPref_whenWorkProfileAndEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "face_work",
                title = "Face Unlock for Work",
                userHandle = USER_WORK_PROFILE,
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                summary = "face added",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(FACE_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun faceUnlockWorkPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(FACE_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun faceUnlockWorkPref_whenNoWorkProfileEntry_isHidden() {
        val entry =
            createEntry(
                id = "face_personal",
                title = "Face Unlock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                summary = "face added",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(FACE_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun wearUnlockPersonalPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "wear_personal",
                title = "Wear Unlock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_WEAR_UNLOCK_SOURCE_ID,
                summary = "android wear summary",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(WEAR_UNLOCK_PERSONAL_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun wearUnlockPersonalPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(WEAR_UNLOCK_PERSONAL_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun wearUnlockWorkPref_whenWorkProfileAndEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "wear_work",
                title = "Wear Unlock for Work",
                userHandle = USER_WORK_PROFILE,
                sourceId = ANDROID_WEAR_UNLOCK_SOURCE_ID,
                summary = "android wear summary",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(WEAR_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun wearUnlockWorkPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(WEAR_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun wearUnlockWorkPref_whenNoWorkProfileEntry_isHidden() {
        val entry =
            createEntry(
                id = "wear_personal",
                title = "Wear Unlock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_WEAR_UNLOCK_SOURCE_ID,
                summary = "android wear summary",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(WEAR_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun onPreferenceClick_nullIntent_doesNothing() {
        val entry =
            createEntry(
                id = "lockScreenEntry",
                title = "Screen Lock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                summary = "set screen lock",
                pendingIntent = null,
            )
        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(ANDROID_LOCK_SCREEN_PREFERENCE_KEY)

            preference?.performClick()
            ShadowLooper.idleMainLooper()

            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNull()
        }
    }

    companion object {
        private const val DEVICE_UNLOCK_ILLUSTRATION_KEY = "device_unlock_illustration"
        private const val ANDROID_LOCK_SCREEN_PREFERENCE_KEY = "android_lock_screen"
        private const val FINGERPRINT_UNLOCK_PERSONAL_PREFERENCE_KEY =
            "android_fingerprint_unlock_personal"
        private const val FINGERPRINT_UNLOCK_WORK_PREFERENCE_KEY = "android_fingerprint_unlock_work"
        private const val FACE_UNLOCK_PERSONAL_PREFERENCE_KEY = "android_face_unlock_personal"
        private const val FACE_UNLOCK_WORK_PREFERENCE_KEY = "android_face_unlock_work"
        private const val WEAR_UNLOCK_PERSONAL_PREFERENCE_KEY = "android_wear_unlock_personal"
        private const val WEAR_UNLOCK_WORK_PREFERENCE_KEY = "android_wear_unlock_work"

        private const val ANDROID_LOCK_SCREEN_SOURCE_ID = "AndroidLockScreen"
        private const val ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID = "AndroidFingerprintUnlock"
        private const val ANDROID_FACE_UNLOCK_SOURCE_ID = "AndroidFaceUnlock"
        private const val ANDROID_WEAR_UNLOCK_SOURCE_ID = "AndroidWearUnlock"
    }
}
