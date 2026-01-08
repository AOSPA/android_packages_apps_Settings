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
import android.safetycenter.SafetyCenterEntry
import android.safetycenter.SafetyCenterManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.dashboard.DashboardFeatureProvider
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settings.safetycenter.SafetyCenterTestUtils.EMPTY_SC_DATA
import com.android.settings.safetycenter.SafetyCenterTestUtils.createEntry
import com.android.settings.safetycenter.SafetyCenterTestUtils.createScData
import com.android.settings.safetycenter.ui.AccountSecuritySubpageScreenApi
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settingslib.drawer.CategoryKey
import com.android.settingslib.drawer.DashboardCategory
import com.android.settingslib.drawer.Tile
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSafetyCenterManager

@RunWith(AndroidJUnit4::class)
class AccountSecuritySubpageScreenApiTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock private lateinit var mockFeatureFactory: FeatureFactory
    @Mock private lateinit var mockDashboardFeatureProvider: DashboardFeatureProvider
    @Mock private lateinit var mockTile: Tile

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(AccountSecuritySubpageScreenApi())
    private lateinit var shadowSafetyCenterManager: ShadowSafetyCenterManager

    private val accountSourceId =
        context.getString(R.string.config_safety_center_account_security_source_id)
    private val passwordSourceId =
        context.getString(R.string.config_safety_center_password_security_source_id)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val safetyCenterManager = context.getSystemService(SafetyCenterManager::class.java)!!
        shadowSafetyCenterManager = Shadow.extract(safetyCenterManager)
        shadowSafetyCenterManager.setSafetyCenterEnabled(true)

        // Mock FeatureFactory for SafetyCenterSubpageRegistry.hasInjectedTiles
        FeatureFactory.setFactory(context, mockFeatureFactory)
        `when`(mockFeatureFactory.dashboardFeatureProvider).thenReturn(mockDashboardFeatureProvider)
        `when`(mockDashboardFeatureProvider.getTilesForCategory(any())).thenReturn(null)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getScreen_allFlagsEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    @EnableFlags(Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getScreen_catalystFlagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    @DisableFlags(Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getScreen_safetyCenterFlagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    private fun setupSafetyCenterData(hasAccountEntry: Boolean, hasPasswordEntry: Boolean) {
        val entries = mutableListOf<SafetyCenterEntry>()
        if (hasAccountEntry) {
            entries.add(
                createEntry(
                    id = "account",
                    title = "account security source",
                    sourceId = accountSourceId,
                )
            )
        }
        if (hasPasswordEntry) {
            entries.add(
                createEntry(
                    id = "password",
                    title = "password security source",
                    sourceId = passwordSourceId,
                )
            )
        }
        shadowSafetyCenterManager.setSafetyCenterData(createScData(entries = entries))
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getLaunchIntent_emptySafetyCenterData_throwsFailedPreconditionException() {
        shadowSafetyCenterManager.setSafetyCenterData(EMPTY_SC_DATA)

        assertFailsWith<FailedPreconditionException> { tester.getLaunchIntent() }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getLaunchIntent_noRelevantEntries_throwsFailedPreconditionException() {
        // Data with an irrelevant source ID
        shadowSafetyCenterManager.setSafetyCenterData(
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

        assertFailsWith<FailedPreconditionException> { tester.getLaunchIntent() }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getLaunchIntent_hasAccountEntryOnly_hasIntent() {
        setupSafetyCenterData(hasAccountEntry = true, hasPasswordEntry = false)
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getLaunchIntent_hasPasswordEntryOnly_hasIntent() {
        setupSafetyCenterData(hasAccountEntry = false, hasPasswordEntry = true)
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getLaunchIntent_hasBothEntries_hasIntent() {
        setupSafetyCenterData(hasAccountEntry = true, hasPasswordEntry = true)
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2, Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    fun getLaunchIntent_hasInjectedTilesAndNoData_hasIntent() {
        shadowSafetyCenterManager.setSafetyCenterData(EMPTY_SC_DATA)

        val categoryWithTiles = DashboardCategory(CategoryKey.CATEGORY_SC_ACCOUNT_SECURITY)
        categoryWithTiles.addTile(mockTile)
        `when`(mockDashboardFeatureProvider.getTilesForCategory(any()))
            .thenReturn(categoryWithTiles)

        assertThat(tester.getLaunchIntent()).isNotNull()
    }
}
