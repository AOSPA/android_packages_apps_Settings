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

package com.android.settings.print

import android.content.Context
import android.graphics.drawable.Drawable
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.print.PrintServiceApiScreen.Companion.EXTRA_CHECKED
import com.android.settings.print.PrintServiceApiScreen.Companion.EXTRA_SERVICE_COMPONENT_NAME
import com.android.settings.print.PrintServiceApiScreen.Companion.EXTRA_TITLE
import com.android.settings.print.PrintServiceApiScreen.Companion.PRINT_SWITCH_KEY
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.Parameters
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class PrintServiceApiScreenTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockPrintRepository = mock<PrintRepository>()
    private lateinit var provider: WifiFeatureProvider
    private lateinit var screen: PrintServiceApiScreen
    private lateinit var tester: ApiTester

    private val fakeInfos =
        MutableStateFlow(
            listOf(
                PrintRepository.PrintServiceDisplayInfo(
                    title = TEST_TITLE,
                    isEnabled = true,
                    summary = TEST_SUMMARY,
                    icon = mock<Drawable>(),
                    componentName = TEST_COMPONENT_NAME,
                )
            )
        )
    private val parameters = Parameters(EXTRA_SERVICE_COMPONENT_NAME to TEST_COMPONENT_NAME)

    @Before
    fun setUp() = runTest {
        provider = FakeFeatureFactory.setupForTest().wifiFeatureProvider
        provider.stub { on { printRepository } doReturn mockPrintRepository }
        mockPrintRepository.stub { on { printServiceDisplayInfosFlow() } doReturn fakeInfos }

        screen = PrintServiceApiScreen()
        val allParameters = screen.getAllPossibleParameters(context).first()
        screen.initializeParameters(allParameters)
        tester = ApiTester(screen)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun launchScreenExtra_parameterized_returnsCorrectExtras() = runTest {
        val allParameters = screen.getAllPossibleParameters(context).first()
        screen.initializeParameters(allParameters)

        val extras = screen.launchScreenExtra
        val infos = fakeInfos.value.first()

        assertThat(extras.getString(EXTRA_SERVICE_COMPONENT_NAME)).isEqualTo(infos.componentName)
        assertThat(extras.getString(EXTRA_TITLE)).isEqualTo(infos.title)
        assertThat(extras.getBoolean(EXTRA_CHECKED, false)).isEqualTo(infos.isEnabled)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_isEnabled_returnsTrue() = runTest {
        mockPrintRepository.stub { onBlocking { isEnabled(TEST_COMPONENT_NAME) } doReturn true }

        assertThat(tester.get<Boolean>(PRINT_SWITCH_KEY, parameters)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_isNotEnabled_returnsFalse() = runTest {
        mockPrintRepository.stub { onBlocking { isEnabled(TEST_COMPONENT_NAME) } doReturn false }

        assertThat(tester.get<Boolean>(PRINT_SWITCH_KEY, parameters)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_true_enablesService() = runTest {
        tester.set(PRINT_SWITCH_KEY, true, parameters)

        verify(mockPrintRepository).setEnabled(TEST_COMPONENT_NAME, true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_false_disablesService() = runTest {
        tester.set(PRINT_SWITCH_KEY, false, parameters)

        verify(mockPrintRepository).setEnabled(TEST_COMPONENT_NAME, false)
    }

    private companion object {
        const val TEST_TITLE = "title"
        const val TEST_SUMMARY = "summary"
        const val TEST_COMPONENT_NAME = "com.android.test/.FakeService"
    }
}
