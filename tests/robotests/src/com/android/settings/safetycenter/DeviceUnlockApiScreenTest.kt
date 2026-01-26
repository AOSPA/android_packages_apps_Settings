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
 */

package com.android.settings.safetycenter

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.safetycenter.SafetyCenterManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.dashboard.DashboardFeatureProvider
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.flags.Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI
import com.android.settings.overlay.FeatureFactory
import com.android.settings.safetycenter.SafetyCenterTestUtils.EMPTY_SC_DATA
import com.android.settings.safetycenter.SafetyCenterTestUtils.createEntry
import com.android.settings.safetycenter.SafetyCenterTestUtils.createScData
import com.android.settings.safetycenter.ui.DeviceUnlockApiScreen
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSafetyCenterManager

@RunWith(AndroidJUnit4::class)
class DeviceUnlockApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock private lateinit var mockFeatureFactory: FeatureFactory
    @Mock private lateinit var mockDashboardFeatureProvider: DashboardFeatureProvider

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(DeviceUnlockApiScreen())
    private lateinit var shadowSafetyCenterManager: ShadowSafetyCenterManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        shadowSafetyCenterManager =
            Shadow.extract(context.getSystemService(SafetyCenterManager::class.java)!!)
        shadowSafetyCenterManager.setSafetyCenterEnabled(true)

        // Mock FeatureFactory for SafetyCenterSubpageRegistry.hasInjectedTiles
        FeatureFactory.setFactory(context, mockFeatureFactory)
        whenever(mockFeatureFactory.dashboardFeatureProvider)
            .thenReturn(mockDashboardFeatureProvider)
        whenever(mockDashboardFeatureProvider.getTilesForCategory(any())).thenReturn(null)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2, FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2, FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2, FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getLaunchIntent_emptySafetyCenterData_throwsFailedPreconditionException() {
        shadowSafetyCenterManager.addEmptyData()

        assertFailsWith<FailedPreconditionException> { tester.getLaunchIntent() }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2, FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getLaunchIntent_noRelevantEntries_throwsFailedPreconditionException() {
        shadowSafetyCenterManager.addIrrelevantData()

        assertFailsWith<FailedPreconditionException> { tester.getLaunchIntent() }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2, FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getLaunchIntent_hasAccountEntryOnly_hasIntent() {
        shadowSafetyCenterManager.addDeviceUnlockEntry()

        assertThat(tester.getLaunchIntent()).isNotNull()
    }
}

private fun ShadowSafetyCenterManager.addDeviceUnlockEntry() {
    setSafetyCenterData(
        createScData(
            entries =
                listOf(
                    createEntry(
                        id = "account",
                        title = "device unlock source",
                        sourceId = "AndroidLockScreen",
                    )
                )
        )
    )
}

private fun ShadowSafetyCenterManager.addIrrelevantData() {
    setSafetyCenterData(
        createScData(
            entries =
                listOf(
                    createEntry(
                        id = "other",
                        title = "irrelevant source",
                        sourceId = "IrrelevantSource",
                    )
                )
        )
    )
}

private fun ShadowSafetyCenterManager.addEmptyData() {
    setSafetyCenterData(EMPTY_SC_DATA)
}
