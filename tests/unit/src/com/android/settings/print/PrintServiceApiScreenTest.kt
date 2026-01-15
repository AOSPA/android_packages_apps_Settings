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
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class PrintServiceApiScreenTest {
    private val tester = ApiTester(PrintServiceApiScreen())

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fakePrintRepository = FakePrintRepository(context)
    private lateinit var provider: WifiFeatureProvider

    private val fakeInfos =
        MutableStateFlow(
            listOf(
                PrintRepository.PrintServiceDisplayInfo(
                    title = "title",
                    isEnabled = true,
                    summary = "summary",
                    icon = mock<Drawable>(),
                    componentName = "componentName",
                )
            )
        )

    // Fake implementation of PrintRepository
    inner class FakePrintRepository(context: Context) : PrintRepository(context) {
        override fun printServiceDisplayInfosFlow():
            Flow<List<PrintRepository.PrintServiceDisplayInfo>> {
            return fakeInfos
        }
    }

    @Before
    fun setUp() {
        provider = FakeFeatureFactory.setupForTest().wifiFeatureProvider
        provider.stub { on { printRepository } doReturn fakePrintRepository }
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
    fun constants_haveCorrectValues() {
        assertThat(PrintServiceApiScreen.KEY).isEqualTo("print_service_settings")
        assertThat(PrintServiceApiScreen.EXTRA_SERVICE_COMPONENT_NAME)
            .isEqualTo("EXTRA_SERVICE_COMPONENT_NAME")
        assertThat(PrintServiceApiScreen.EXTRA_TITLE).isEqualTo("EXTRA_TITLE")
        assertThat(PrintServiceApiScreen.EXTRA_CHECKED).isEqualTo("EXTRA_CHECKED")
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun launchScreenExtra_parameterized_returnsCorrectExtras() = runBlocking {
        val apiScreen = PrintServiceApiScreen()
        val allParameters = apiScreen.getAllPossibleParameters(context).first()
        apiScreen.initializeParameters(allParameters)

        val extras = apiScreen.launchScreenExtra

        assertThat(extras.getString(PrintServiceApiScreen.EXTRA_SERVICE_COMPONENT_NAME))
            .isEqualTo(fakeInfos.value.first().componentName)
        assertThat(extras.getString(PrintServiceApiScreen.EXTRA_TITLE))
            .isEqualTo(fakeInfos.value.first().title)
        assertThat(extras.getBoolean(PrintServiceApiScreen.EXTRA_CHECKED, false))
            .isEqualTo(fakeInfos.value.first().isEnabled)
    }
}
