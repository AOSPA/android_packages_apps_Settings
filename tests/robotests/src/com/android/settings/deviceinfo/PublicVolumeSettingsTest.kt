/*
 * Copyright (C) 2026 The Android Open Source Project
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
 *
 */
package com.android.settings.deviceinfo

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.storage.DiskInfo
import android.os.storage.StorageManager
import android.os.storage.VolumeInfo
import android.os.storage.VolumeRecord
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.settings.R
import com.android.settings.flags.Flags.FLAG_ENABLE_RENAME_IN_PUBLIC_VOLUME_SETTINGS
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class PublicVolumeSettingsTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mStorageManager: StorageManager
    @Mock private lateinit var mVolume: VolumeInfo
    @Mock private lateinit var mDisk: DiskInfo
    @Mock private lateinit var mFile: File
    @Mock private lateinit var mVolumeRecord: VolumeRecord

    private lateinit var mContext: Context

    private val mBundle = Bundle().apply { putString(VolumeInfo.EXTRA_VOLUME_ID, "public_volume") }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()

        // Inject the mock StorageManager
        val shadowApplication = Shadows.shadowOf(mContext.applicationContext as Application)
        shadowApplication.setSystemService(Context.STORAGE_SERVICE, mStorageManager)

        `when`(mVolume.getId()).thenReturn("public_volume")
        `when`(mVolume.getDiskId()).thenReturn("disk_id")
        `when`(mVolume.getType()).thenReturn(VolumeInfo.TYPE_PUBLIC)
        `when`(mVolume.isMountedReadable()).thenReturn(true)
        `when`(mVolume.getPath()).thenReturn(mFile)
        `when`(mVolume.getFsUuid()).thenReturn("uuid_test")
        `when`(mFile.totalSpace).thenReturn(1000000L)
        `when`(mFile.freeSpace).thenReturn(100000L)
        `when`(mStorageManager.findVolumeById("public_volume")).thenReturn(mVolume)
        `when`(mStorageManager.findDiskById("disk_id")).thenReturn(mDisk)
        `when`(mStorageManager.findRecordByUuid("uuid_test")).thenReturn(mVolumeRecord)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_RENAME_IN_PUBLIC_VOLUME_SETTINGS)
    fun test_update_addsRenamePreference() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                val renameText = mContext.getString(R.string.storage_menu_rename)

                // Manually update fragment and allow to settle before using Espresso
                fragment.update()
                ShadowLooper.idleMainLooper()
                onView(withText(renameText)).check(matches(isDisplayed()))
            }
        }
    }

    @Test
    @DisableFlags(FLAG_ENABLE_RENAME_IN_PUBLIC_VOLUME_SETTINGS)
    fun test_update_renamePreferenceNotAdded() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                val renameText = mContext.getString(R.string.storage_menu_rename)

                // Manually update fragment and allow to settle before using Espresso
                fragment.update()
                ShadowLooper.idleMainLooper()
                onView(withText(renameText)).check(doesNotExist())
            }
        }
    }

    @Test
    @EnableFlags(FLAG_ENABLE_RENAME_IN_PUBLIC_VOLUME_SETTINGS)
    fun test_onPreferenceTreeClick_renamePreference_doesNotCrash() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                val renameText = mContext.getString(R.string.storage_menu_rename)
                val renameTitle = mContext.getString(R.string.storage_rename_title)

                // Manually update fragment and allow to settle before using Espresso
                fragment.update()
                ShadowLooper.idleMainLooper()
                onView(withText(renameText)).perform(click())

                // Confirm that rename dialog appears prompting for input
                ShadowLooper.idleMainLooper()
                onView(withText(renameTitle)).inRoot(isDialog()).check(matches(isDisplayed()))
            }
        }
    }

    // Launch fragment with a valid theme and arguments
    private fun launchFragment() =
        launchFragmentInContainer<PublicVolumeSettings>(
            fragmentArgs = mBundle,
            themeResId = R.style.Theme_SubSettings,
        )
}
