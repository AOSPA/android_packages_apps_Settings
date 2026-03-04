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

package com.android.settings.accessibility.a11yactivity.ui

import android.accessibilityservice.AccessibilityShortcutInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.accessibility.AccessibilitySettings
import com.android.settings.accessibility.LaunchAccessibilityActivityPreferenceFragment
import com.android.settings.accessibility.a11yactivity.AccessibilityShortcut
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.extensions.putComponentName
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.metadata.CatalystFlagProvider
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.TwoTargetPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers

/** Tests for [A11yActivityScreen]. */
@RunWith(AndroidJUnit4::class)
class A11yActivityScreenTest {
    @get:Rule val settingStoreRule = SettingsStoreRule()
    @get:Rule val platformFlags = SetFlagsRule()
    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val arguments =
        Bundle().apply {
            putComponentName(AccessibilitySettings.EXTRA_COMPONENT_NAME, A11Y_ACTIVITY_COMPONENT)
        }

    private val keyParameters =
        A11yActivityScreen.parametersSchema.prepare(
            AccessibilitySettings.EXTRA_COMPONENT_NAME to A11Y_ACTIVITY_COMPONENT.flattenToString()
        )

    private lateinit var originalProvider: CatalystFlagProvider
    private lateinit var preferenceScreenCreator: A11yActivityScreen

    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))

    @Before
    fun setUp() {
        originalProvider = CatalystFlagProviderFactory.getInstance()
        FakeFeatureFactory.setupForTest()
        val mockInfo: AccessibilityShortcutInfo = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT)
        val activityInfo =
            mock<ActivityInfo>().apply {
                packageName = PACKAGE_NAME
                name = A11Y_ACTIVITY_COMPONENT.className
                applicationInfo = ApplicationInfo()
            }
        whenever(activityInfo.loadLabel(any())).thenReturn(DEFAULT_LABEL)
        whenever(mockInfo.activityInfo).thenReturn(activityInfo)
        whenever(mockInfo.loadSummary(any())).thenReturn(DEFAULT_SUMMARY)
        a11yManager.setInstalledAccessibilityShortcutListAsUser(listOf(mockInfo))

        preferenceScreenCreator = if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            A11yActivityScreen(appContext, keyParameters)
        } else {
            A11yActivityScreen(appContext, arguments)
        }
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        CatalystFlagProviderFactory.setProvider(originalProvider)
    }

    @Test
    fun getKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(A11yActivityScreen.KEY)
    }

    @Test
    fun parametersSchema_isCorrect() {
        val schema = A11yActivityScreen.parametersSchema
        val parameter = schema.getParameters()[AccessibilitySettings.EXTRA_COMPONENT_NAME]

        assertThat(parameter).isNotNull()
        assertThat(parameter!!.type).isEqualTo(AccessibilityShortcut)
        assertThat(parameter.required).isTrue()
    }

    @Test
    fun getHighlightMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun getMetricsCategory() {
        val expectedPageId = 123
        whenever(
                featureFactory.accessibilityPageIdFeatureProvider.getCategory(
                    A11Y_ACTIVITY_COMPONENT
                )
            )
            .thenReturn(expectedPageId)
        assertThat(preferenceScreenCreator.getMetricsCategory()).isEqualTo(expectedPageId)
    }

    @Test
    fun getSummary() {
        assertThat(preferenceScreenCreator.getSummary(appContext)).isEqualTo(DEFAULT_SUMMARY)
    }

    @Test
    fun getTitle() {
        assertThat(preferenceScreenCreator.getTitle(appContext)).isEqualTo(DEFAULT_LABEL)
    }

    @Test
    fun createWidget_verifyWidgetTypeAndIconSpaceReserved() {
        val widget = preferenceScreenCreator.createWidget(appContext)
        assertThat(widget).isInstanceOf(TwoTargetPreference::class.java)
        assertThat(widget.isIconSpaceReserved).isTrue()
    }

    @Test
    fun bind_verifyIcon() {
        val widget = preferenceScreenCreator.createAndBindWidget<TwoTargetPreference>(appContext)
        assertThat(widget.icon).isNotNull()
    }

    @Test
    fun getFragmentClass() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(LaunchAccessibilityActivityPreferenceFragment::class.java)
    }

    @Test
    fun getBindingKey() {
        assertThat(preferenceScreenCreator.bindingKey)
            .isEqualTo(A11Y_ACTIVITY_COMPONENT.flattenToString())
    }

    @Test
    fun getLaunchIntent_hasStringExtraForComponent() {
        val intent = preferenceScreenCreator.getLaunchIntent(appContext, null)
        assertThat(intent.getStringExtra(Intent.EXTRA_COMPONENT_NAME))
            .isEqualTo(A11Y_ACTIVITY_COMPONENT.flattenToString())
    }

    @Test
    @DisableFlags(
        com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun parameters_hasTwoA11yActivities_returnTwoItems_bundleArguments() = runTest {
        setCatalystUseKeyParameters(false)

        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val shortcutInfo1 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT)
        val shortcutInfo2 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT2)

        a11yManager.setInstalledAccessibilityShortcutListAsUser(
            listOf(shortcutInfo1, shortcutInfo2)
        )
        val collectedItems = mutableListOf<String?>()
        A11yActivityScreen.parameters(appContext).collect {
            collectedItems.add(
                it.getParcelable(
                        AccessibilitySettings.EXTRA_COMPONENT_NAME,
                        ComponentName::class.java,
                    )
                    ?.flattenToString()
            )
        }
        assertThat(collectedItems).hasSize(2)
        assertThat(collectedItems)
            .containsExactlyElementsIn(
                listOf(
                    A11Y_ACTIVITY_COMPONENT.flattenToString(),
                    A11Y_ACTIVITY_COMPONENT2.flattenToString(),
                )
            )
    }

    @Test
    @EnableFlags(
        com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun parameters_hasTwoA11yActivities_returnTwoItems_keyParameters() = runTest {
        setCatalystUseKeyParameters(true)

        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val shortcutInfo1 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT)
        val shortcutInfo2 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT2)

        a11yManager.setInstalledAccessibilityShortcutListAsUser(
            listOf(shortcutInfo1, shortcutInfo2)
        )
        val collectedItems = mutableListOf<String?>()
        A11yActivityScreen.keyParameters(appContext).collect {
            collectedItems.add(it[AccessibilitySettings.EXTRA_COMPONENT_NAME])
        }
        assertThat(collectedItems).hasSize(2)
        assertThat(collectedItems)
            .containsExactlyElementsIn(
                listOf(
                    A11Y_ACTIVITY_COMPONENT.flattenToString(),
                    A11Y_ACTIVITY_COMPONENT2.flattenToString(),
                )
            )
    }

    private fun createMockShortcutInfo(componentName: ComponentName): AccessibilityShortcutInfo {
        val mockInfo: AccessibilityShortcutInfo = mock()
        whenever(mockInfo.componentName).thenReturn(componentName)
        return mockInfo
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagTrue_validString_parsedCorrectly() {
        setCatalystUseKeyParameters(false)

        val args =
            Bundle().apply {
                putString(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    A11Y_ACTIVITY_COMPONENT.flattenToString(),
                )
            }
        val screen = A11yActivityScreen(appContext, args)

        assertThat(screen.getFeatureComponentName()).isEqualTo(A11Y_ACTIVITY_COMPONENT)
    }

    @Test
    @EnableFlags(
        com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun featureComponentName_flagTrue_validString_parsedCorrectly_usingKeyParameters() {
        setCatalystUseKeyParameters(true)

        val keyParameters =
            A11yActivityScreen.parametersSchema.prepare(
                AccessibilitySettings.EXTRA_COMPONENT_NAME to
                    A11Y_ACTIVITY_COMPONENT.flattenToString()
            )

        val screen = A11yActivityScreen(appContext, keyParameters)

        assertThat(screen.getFeatureComponentName()).isEqualTo(A11Y_ACTIVITY_COMPONENT)
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagTrue_invalidString_throwsException() {
        setCatalystUseKeyParameters(false)

        val args =
            Bundle().apply {
                putString(AccessibilitySettings.EXTRA_COMPONENT_NAME, "invalidComponent")
            }

        assertThrows(IllegalArgumentException::class.java) {
            val screen = A11yActivityScreen(appContext, args)
            screen.getFeatureComponentName()
        }
    }

    @Test
    @EnableFlags(
        com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun featureComponentName_flagTrue_invalidString_throwsException_usingKeyParameters() {
        setCatalystUseKeyParameters(true)

        val keyParameters =
            A11yActivityScreen.parametersSchema.prepare(
                AccessibilitySettings.EXTRA_COMPONENT_NAME to "invalidComponent"
            )

        assertThrows(IllegalArgumentException::class.java) {
            val screen = A11yActivityScreen(appContext, keyParameters)
            screen.getFeatureComponentName()
        }
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagTrue_missingKey_throwsException() {
        setCatalystUseKeyParameters(false)

        val args = Bundle()

        assertThrows(IllegalArgumentException::class.java) {
            val screen = A11yActivityScreen(appContext, args)
            screen.getFeatureComponentName()
        }
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagTrue_canRetrieveTheComponentNameFromParcelable() {
        setCatalystUseKeyParameters(false)

        val args =
            Bundle().apply {
                putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, A11Y_ACTIVITY_COMPONENT)
            }

        val screen = A11yActivityScreen(appContext, args)

        assertThat(screen.getFeatureComponentName()).isEqualTo(A11Y_ACTIVITY_COMPONENT)
    }

    @Test
    @DisableFlags(
        com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun featureComponentName_flagFalse_validParcelable_parsedCorrectly() {
        setCatalystUseKeyParameters(false)

        val args =
            Bundle().apply {
                putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, A11Y_ACTIVITY_COMPONENT)
            }
        val screen = A11yActivityScreen(appContext, args)

        assertThat(screen.getFeatureComponentName()).isEqualTo(A11Y_ACTIVITY_COMPONENT)
    }

    @Test
    @DisableFlags(
        com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun featureComponentName_flagFalse_missingKey_throwsException() {
        setCatalystUseKeyParameters(false)

        val args = Bundle()

        assertThrows(IllegalArgumentException::class.java) {
            val screen = A11yActivityScreen(appContext, args)
            screen.getFeatureComponentName()
        }
    }

    @Test
    @DisableFlags(
        com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun featureComponentName_flagFalse_canRetrieveTheComponentNameFromString() {
        setCatalystUseKeyParameters(false)

        val args =
            Bundle().apply {
                putString(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    A11Y_ACTIVITY_COMPONENT.flattenToString(),
                )
            }

        val screen = A11yActivityScreen(appContext, args)

        assertThat(screen.getFeatureComponentName()).isEqualTo(A11Y_ACTIVITY_COMPONENT)
    }

    @Test
    @EnableFlags(
        com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun parameters_flagTrue_emitsKeyParametersWithString() = runTest {
        setCatalystUseKeyParameters(true)

        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val shortcutInfo1 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT)
        val shortcutInfo2 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT2)
        a11yManager.setInstalledAccessibilityShortcutListAsUser(
            listOf(shortcutInfo1, shortcutInfo2)
        )

        val collectedKeyParameters = mutableListOf<ValidatedKeyParameters>()
        A11yActivityScreen.keyParameters(appContext).collect { collectedKeyParameters.add(it) }

        assertThat(collectedKeyParameters).hasSize(2)
        // Check first keyParameter
        assertThat(
                collectedKeyParameters[0].getRequired(AccessibilitySettings.EXTRA_COMPONENT_NAME)
            )
            .isEqualTo(A11Y_ACTIVITY_COMPONENT.flattenToString())

        // Check second keyParameter
        assertThat(
                collectedKeyParameters[1].getRequired(AccessibilitySettings.EXTRA_COMPONENT_NAME)
            )
            .isEqualTo(A11Y_ACTIVITY_COMPONENT2.flattenToString())
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun parameters_flagTrue_emitsBundleWithString() = runTest {
        setCatalystUseKeyParameters(false)

        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val shortcutInfo1 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT)
        val shortcutInfo2 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT2)
        a11yManager.setInstalledAccessibilityShortcutListAsUser(
            listOf(shortcutInfo1, shortcutInfo2)
        )

        val collectedBundles = mutableListOf<Bundle>()
        A11yActivityScreen.parameters(appContext).collect { collectedBundles.add(it) }

        assertThat(collectedBundles).hasSize(2)
        // Check first bundle
        assertThat(collectedBundles[0].getString(AccessibilitySettings.EXTRA_COMPONENT_NAME))
            .isEqualTo(A11Y_ACTIVITY_COMPONENT.flattenToString())
        assertThat(
                collectedBundles[0].getParcelable(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    ComponentName::class.java,
                )
            )
            .isNull()
        // Check second bundle
        assertThat(collectedBundles[1].getString(AccessibilitySettings.EXTRA_COMPONENT_NAME))
            .isEqualTo(A11Y_ACTIVITY_COMPONENT2.flattenToString())
        assertThat(
                collectedBundles[1].getParcelable(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    ComponentName::class.java,
                )
            )
            .isNull()
    }

    @Test
    @DisableFlags(
        com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE
    )
    fun parameters_flagFalse_emitsBundleWithParcelable() = runTest {
        setCatalystUseKeyParameters(false)

        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val shortcutInfo1 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT)
        val shortcutInfo2 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT2)
        a11yManager.setInstalledAccessibilityShortcutListAsUser(
            listOf(shortcutInfo1, shortcutInfo2)
        )

        val collectedBundles = mutableListOf<Bundle>()
        A11yActivityScreen.parameters(appContext).collect { collectedBundles.add(it) }

        assertThat(collectedBundles).hasSize(2)
        // Check first bundle
        assertThat(
                collectedBundles[0].getParcelable(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    ComponentName::class.java,
                )
            )
            .isEqualTo(A11Y_ACTIVITY_COMPONENT)
        assertThat(collectedBundles[0].getString(AccessibilitySettings.EXTRA_COMPONENT_NAME))
            .isNull()
        // Check second bundle
        assertThat(
                collectedBundles[1].getParcelable(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    ComponentName::class.java,
                )
            )
            .isEqualTo(A11Y_ACTIVITY_COMPONENT2)
        assertThat(collectedBundles[1].getString(AccessibilitySettings.EXTRA_COMPONENT_NAME))
            .isNull()
    }

    private fun setCatalystUseKeyParameters(value: Boolean) {
        CatalystFlagProviderFactory.setProvider(object : CatalystFlagProvider {
            override fun catalystUseKeyParameters() = value
        })
    }

    companion object {
        private const val PACKAGE_NAME = "com.foo.bar"
        private val A11Y_ACTIVITY_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yActivity")
        private val A11Y_ACTIVITY_COMPONENT2 = ComponentName(PACKAGE_NAME, "FakeA11yActivity2")

        private const val DEFAULT_LABEL = "default label"
        private const val DEFAULT_SUMMARY = "default summary"
    }
}

private fun A11yActivityScreen.getFeatureComponentName(): ComponentName {
    return ReflectionHelpers.getField(this, "featureComponentName")
}
