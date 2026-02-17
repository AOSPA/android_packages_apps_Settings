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

package com.android.settings.accessibility.setupwizard

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.SystemProperty
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.android.setupdesign.GlifRecyclerLayout
import com.google.android.setupdesign.items.Item
import com.google.android.setupdesign.items.RecyclerItemAdapter
import com.google.common.truth.Truth.assertThat
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

/** Tests for [AccessibilitySetupWizardFragment]. */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [SettingsShadowResources::class])
class AccessibilitySetupWizardFragmentTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultAccessibilityService,
            TEST_SCREEN_READER,
        )
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultSelectToSpeakService,
            TEST_SELECT_TO_SPEAK,
        )
        a11yManager.setInstalledAccessibilityServiceList(emptyList())
    }

    @Test
    fun onViewCreated_hasCorrectTitleAndDescription() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                val layout =
                    fragment
                        .requireView()
                        .findViewById<GlifRecyclerLayout>(R.id.accessibility_suw_screen_layout)

                val headerText = layout.headerTextView.text
                assertThat(headerText)
                    .isEqualTo(appContext.getString(R.string.vision_settings_title))
                val descriptionText = layout.descriptionTextView.text
                assertThat(descriptionText)
                    .isEqualTo(appContext.getString(R.string.vision_settings_description))
            }
        }
    }

    @Test
    fun onViewCreated_servicesInstalled_showsItemsWithCorrectTitles() {
        val talkBackInfo =
            createA11yServiceInfo(
                serviceComponent = ComponentName.unflattenFromString(TEST_SCREEN_READER)!!,
                label = TEST_SCREEN_READER_LABEL,
            )
        val selectToSpeakInfo =
            createA11yServiceInfo(
                serviceComponent = ComponentName.unflattenFromString(TEST_SELECT_TO_SPEAK)!!,
                label = TEST_SELECT_TO_SPEAK_LABEL,
            )
        a11yManager.setInstalledAccessibilityServiceList(listOf(talkBackInfo, selectToSpeakInfo))

        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                val screenReaderItem = findItem(fragment, R.id.screen_reader_in_suw)
                assertThat(screenReaderItem?.isVisible).isTrue()
                assertThat(screenReaderItem?.title.toString()).isEqualTo(TEST_SCREEN_READER_LABEL)

                val selectToSpeakItem = findItem(fragment, R.id.select_to_speak_in_suw)
                assertThat(selectToSpeakItem?.isVisible).isTrue()
                assertThat(selectToSpeakItem?.title.toString())
                    .isEqualTo(TEST_SELECT_TO_SPEAK_LABEL)
            }
        }
    }

    @Test
    fun onViewCreated_noServicesInstalled_hidesItems() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                val screenReaderItem = findItem(fragment, R.id.screen_reader_in_suw)
                assertThat(screenReaderItem?.isVisible).isFalse()

                val selectToSpeakItem = findItem(fragment, R.id.select_to_speak_in_suw)
                assertThat(selectToSpeakItem?.isVisible).isFalse()
            }
        }
    }

    private fun launchFragment(): FragmentScenario<AccessibilitySetupWizardFragment> =
        launchFragmentInContainer<AccessibilitySetupWizardFragment>(themeResId = R.style.GlifTheme)

    private fun createA11yServiceInfo(
        isAlwaysOnService: Boolean = false,
        serviceComponent: ComponentName,
        label: String,
    ): AccessibilityServiceInfo {
        val info =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                serviceComponent,
                isAlwaysOnService,
            )
        info.isAccessibilityTool = true

        val originalResolveInfo = info.resolveInfo
        if (originalResolveInfo != null) {
            val spyResolveInfo = spy(originalResolveInfo)
            doReturn(label).whenever(spyResolveInfo).loadLabel(any<PackageManager>())
            doReturn(ColorDrawable(0)).whenever(spyResolveInfo).loadIcon(any<PackageManager>())
            info.resolveInfo = spyResolveInfo
        }

        return info
    }

    private fun findItem(fragment: AccessibilitySetupWizardFragment, targetId: Int): Item? {
        val layout =
            fragment
                .requireView()
                .findViewById<GlifRecyclerLayout>(R.id.accessibility_suw_screen_layout)
        val adapter = layout.adapter as RecyclerItemAdapter
        return adapter.findItemById(targetId).getItemAt(0) as? Item
    }

    companion object {
        private const val TEST_SCREEN_READER = "com.test.talkback/.TalkBackService"
        private const val TEST_SCREEN_READER_LABEL = "TalkBack"
        private const val TEST_SELECT_TO_SPEAK = "com.test.talkback/.SelectToSpeakService"
        private const val TEST_SELECT_TO_SPEAK_LABEL = "Select to Speak"

        private var systemProperty: SystemProperty? = null

        @BeforeClass
        @JvmStatic
        fun setup() {
            // Robolectric by default creates Activity contexts without an associated Display.
            // This property ensures the Activity context is attached to a simulated display,
            // preventing UnsupportedOperationException when code calls Context#getDisplay().
            systemProperty =
                SystemProperty().apply { override("robolectric.createActivityContexts", "true") }
        }

        @AfterClass
        @JvmStatic
        fun cleanUp() {
            systemProperty?.let {
                it.close()
                systemProperty = null
            }
        }
    }
}
