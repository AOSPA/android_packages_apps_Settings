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
package com.android.settings.spa.accessibility

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ForceDarkAppExceptionsListModelTest {
    @get:Rule val mockitoRule = MockitoJUnit.rule()
    @get:Rule val composeTestRule = createComposeRule()

    @Mock lateinit var repository: ForceDarkAppExceptionsRepository
    private lateinit var listModel: ForceDarkAppExceptionsListModel
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        listModel = ForceDarkAppExceptionsListModel(context, repository)
    }

    @Test
    fun transform() = runTest {
        whenever(repository.isAppForceDarkAlwaysDisable(APP)).thenReturn(true)
        val recordListFlow =
            listModel.transform(userIdFlow = flowOf(USER_ID), appListFlow = flowOf(listOf(APP)))

        val recordList = recordListFlow.first()
        assertThat(recordList).hasSize(1)
        val record = recordList[0]
        assertThat(record.app).isSameInstanceAs(APP)
    }

    @Test
    fun appIsException_isChecked() {
        whenever(repository.isAppForceDarkAlwaysDisable(APP)).thenReturn(true)

        composeTestRule.setContent {
            with(ForceDarkAppExceptionsListModel(context, repository)) {
                AppListItemModel(
                        record =
                            ForceDarkAppExceptionRecord(
                                app = APP,
                                controller = ForceDarkAppExceptionsController(APP, repository),
                            ),
                        label = LABEL,
                        summary = { SUMMARY },
                    )
                    .AppItem()
            }
        }

        composeTestRule.onNodeWithText(LABEL).assertIsOn()
    }

    @Test
    fun appIsNotException_isNotChecked() {
        whenever(repository.isAppForceDarkAlwaysDisable(APP)).thenReturn(false)

        composeTestRule.setContent {
            with(ForceDarkAppExceptionsListModel(context, repository)) {
                AppListItemModel(
                        record =
                            ForceDarkAppExceptionRecord(
                                app = APP,
                                controller = ForceDarkAppExceptionsController(APP, repository),
                            ),
                        label = LABEL,
                        summary = { SUMMARY },
                    )
                    .AppItem()
            }
        }

        composeTestRule.onNodeWithText(LABEL).assertIsOff()
    }

    private companion object {
        const val USER_ID = 0
        const val LABEL = "Label"
        const val SUMMARY = "Summary"
        const val PACKAGE_NAME = "package.name"
        val APP = ApplicationInfo().apply { packageName = PACKAGE_NAME }
    }
}
