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

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.pm.ApplicationInfo
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SatelliteAppListItemTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun enabledState_isClickable() {
        val appLabel = "Test App"
        val record = TestAppRecord(ApplicationInfo().apply { packageName = "com.example.app" })
        var clicked = false

        composeTestRule.setContent {
            AppListItemModel(record = record, label = appLabel, summary = { "Summary" })
                .SatelliteAppListItem(enabled = true, onClick = { clicked = true })
        }

        // Verify the item is enabled, has a click action, and can be clicked.
        composeTestRule
            .onNodeWithText(appLabel)
            .assertIsEnabled()
            .assertHasClickAction()
            .performClick()
        assert(clicked)
    }

    @Test
    fun disabledState_hasNoClickAction() {
        val appLabel = "Test App"
        val record = TestAppRecord(ApplicationInfo().apply { packageName = "com.example.app" })
        var clicked = false

        composeTestRule.setContent {
            AppListItemModel(record = record, label = appLabel, summary = { "Summary" })
                .SatelliteAppListItem(enabled = false, onClick = { clicked = true })
        }

        // Verify that the item text is displayed.
        composeTestRule.onNodeWithText(appLabel).assertExists()
        // Verify that the item (via its test tag wrapper) is semantically disabled
        // and has no click action.
        composeTestRule
            .onNodeWithTag("SatelliteAppListItem")
            .assertIsNotEnabled()
            .assertHasNoClickAction()
            .performClick()
        assert(!clicked)
    }

    private data class TestAppRecord(override val app: ApplicationInfo) : AppRecord
}
