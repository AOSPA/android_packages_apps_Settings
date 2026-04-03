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

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.icu.text.CollationKey
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spaprivileged.model.app.AppEntry
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ForceDarkAppExceptionsListModelTest {
    @get:Rule val mockitoRule = MockitoJUnit.rule()
    @get:Rule val composeTestRule = createComposeRule()

    @Mock lateinit var repository: ForceDarkAppExceptionsRepository
    @Mock lateinit var mockPackageManager: PackageManager
    @Mock lateinit var usageStatsManager: UsageStatsManager
    @Mock lateinit var context: Context
    @Mock lateinit var usageStatsA: UsageStats
    @Mock lateinit var usageStatsB: UsageStats
    @Mock lateinit var usageStatsC: UsageStats
    private lateinit var listModel: ForceDarkAppExceptionsListModel

    @Before
    fun setup() {
        whenever(context.packageName).thenReturn("package.test")
        whenever(context.createContextAsUser(any(), anyInt())).thenReturn(context)
        whenever(mockPackageManager.resolveActivity(any(), anyInt())).thenReturn(null)
        whenever(context.packageManager).thenReturn(mockPackageManager)
        whenever(usageStatsManager.queryAndAggregateUsageStats(anyLong(), anyLong()))
            .thenReturn(
                mapOf<String, UsageStats>(
                    PACKAGE_NAME_A to usageStatsA,
                    PACKAGE_NAME_B to usageStatsB,
                    PACKAGE_NAME_C to usageStatsC,
                )
            )
        whenever(context.getSystemService(Context.USAGE_STATS_SERVICE))
            .thenReturn(usageStatsManager)
        listModel =
            ForceDarkAppExceptionsListModel(context).apply {
                repositoryFactory = { _, _ -> repository }
            }
    }

    @Test
    fun transform() = runTest {
        whenever(repository.isAppForceDarkAlwaysDisable(APP_A)).thenReturn(true)
        val recordListFlow =
            listModel.transform(userIdFlow = flowOf(USER_ID), appListFlow = flowOf(listOf(APP_A)))

        val recordList = recordListFlow.first()
        assertThat(recordList).hasSize(1)
        val record = recordList[0]
        assertThat(record.app).isSameInstanceAs(APP_A)
    }

    @Test
    fun getComparator_isOrderedCorrectly() = runTest {
        whenever(repository.isAppForceDarkAlwaysDisable(APP_C)).thenReturn(true)
        whenever(repository.isAppForceDarkAlwaysDisable(APP_A)).thenReturn(false)
        whenever(repository.isAppForceDarkAlwaysDisable(APP_B)).thenReturn(false)
        whenever(repository.isAppForceDarkAlwaysDisable(APP_D)).thenReturn(false)
        whenever(repository.isAppForceDarkAlwaysDisable(APP_E)).thenReturn(false)
        whenever(usageStatsA.lastTimeUsed).thenReturn(100L)
        whenever(usageStatsB.lastTimeUsed).thenReturn(300L)
        whenever(usageStatsC.lastTimeUsed).thenReturn(200L)

        val sourceFlow =
            listModel.transform(
                userIdFlow = flowOf(USER_ID),
                appListFlow = flowOf(listOf(APP_A, APP_B, APP_C, APP_D, APP_E)),
            )
        val source =
            sourceFlow.first().mapIndexed { index, appRecord ->
                AppEntry(
                    appRecord,
                    appRecord.app.packageName,
                    CollationKey(index.toString(), byteArrayOf(0)),
                )
            }

        val orderedPackageNames: List<String> =
            source.sortedWith(listModel.getComparator(0)).map { it.record.app.packageName }

        assertThat(orderedPackageNames)
            .containsExactly(
                // APP_C first because it is the only app with the toggle disabled
                PACKAGE_NAME_C,
                // APP_B(300L), APP_A(100L) next because they are accessed recently
                PACKAGE_NAME_B,
                PACKAGE_NAME_A,
                // APP_D, APP_E are in alphabetical order since their UsageStats are empty
                PACKAGE_NAME_D,
                PACKAGE_NAME_E,
            )
            .inOrder()
    }

    @Test
    fun appIsException_isNotChecked() {
        whenever(repository.isAppForceDarkAlwaysDisable(APP_A)).thenReturn(true)

        composeTestRule.setContent {
            with(ForceDarkAppExceptionsListModel(context)) {
                AppListItemModel(
                        record =
                            ForceDarkAppExceptionRecord(
                                app = APP_A,
                                controller = ForceDarkAppExceptionsController(APP_A, repository),
                            ),
                        label = LABEL,
                        summary = { SUMMARY },
                    )
                    .AppItem()
            }
        }

        composeTestRule.onNodeWithText(LABEL).assertIsOff()
    }

    @Test
    fun appIsNotException_isChecked() {
        whenever(repository.isAppForceDarkAlwaysDisable(APP_A)).thenReturn(false)

        composeTestRule.setContent {
            with(ForceDarkAppExceptionsListModel(context)) {
                AppListItemModel(
                        record =
                            ForceDarkAppExceptionRecord(
                                app = APP_A,
                                controller = ForceDarkAppExceptionsController(APP_A, repository),
                            ),
                        label = LABEL,
                        summary = { SUMMARY },
                    )
                    .AppItem()
            }
        }

        composeTestRule.onNodeWithText(LABEL).assertIsOn()
    }

    private companion object {
        const val USER_ID = 0
        const val LABEL = "Label"
        const val SUMMARY = "Summary"
        const val PACKAGE_NAME_A = "package.name.a"
        const val PACKAGE_NAME_B = "package.name.b"
        const val PACKAGE_NAME_C = "package.name.c"
        const val PACKAGE_NAME_D = "package.name.d"
        const val PACKAGE_NAME_E = "package.name.e"
        val APP_A = ApplicationInfo().apply { packageName = PACKAGE_NAME_A }
        val APP_B = ApplicationInfo().apply { packageName = PACKAGE_NAME_B }
        val APP_C = ApplicationInfo().apply { packageName = PACKAGE_NAME_C }
        val APP_D = ApplicationInfo().apply { packageName = PACKAGE_NAME_D }
        val APP_E = ApplicationInfo().apply { packageName = PACKAGE_NAME_E }
    }
}
