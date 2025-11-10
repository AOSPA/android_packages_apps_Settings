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

package com.android.settings.backup

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.flags.Flags.FLAG_ENABLE_ACCOUNTS_AND_BACKUP_SCREEN
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopLevelAccountsAndBackupEntryPreferenceControllerTest {
    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val controller = TopLevelAccountsAndBackupEntryPreferenceController(context, "key")

    @EnableFlags(FLAG_ENABLE_ACCOUNTS_AND_BACKUP_SCREEN)
    @Test
    fun getAvailabilityStatus_flagOn_available() {
        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @DisableFlags(FLAG_ENABLE_ACCOUNTS_AND_BACKUP_SCREEN)
    @Test
    fun getAvailabilityStatus_flagOff_unavailable() {
        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }
}
