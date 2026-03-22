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

package com.android.settings.accessibility.a11yservice.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.accessibility.AccessibilitySettings
import com.android.settings.accessibility.a11yservice.A11yServicePreferenceFragment
import com.android.settings.accessibility.a11yservice.data.AccessibilityService
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.extensions.putComponentName
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.metadata.CatalystFlagProvider
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowPackageManager

/** Tests for [A11yServiceScreen]. */
@RunWith(AndroidJUnit4::class)
class A11yServiceScreenTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    @get:Rule val settingStoreRule = SettingsStoreRule()
    @get:Rule val platformFlags = SetFlagsRule()

    private val arguments =
        Bundle().apply {
            putComponentName(AccessibilitySettings.EXTRA_COMPONENT_NAME, A11Y_SERVICE_COMPONENT)
        }

    private val keyParameters =
        A11yServiceScreen.parametersSchema.prepare(
            AccessibilitySettings.EXTRA_COMPONENT_NAME to A11Y_SERVICE_COMPONENT.flattenToString()
        )

    private lateinit var originalProvider: CatalystFlagProvider
    private lateinit var preferenceScreenCreator: A11yServiceScreen

    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))
    private val packageManager: ShadowPackageManager = shadowOf(appContext.packageManager)

    @Before
    fun setUp() {
        originalProvider = CatalystFlagProviderFactory.getInstance()
        FakeFeatureFactory.setupForTest()
        a11yManager.setInstalledAccessibilityServiceList(listOf(createA11yServiceInfo()))

        preferenceScreenCreator =
            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                A11yServiceScreen(appContext, keyParameters)
            } else {
                A11yServiceScreen(appContext, arguments)
            }
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        CatalystFlagProviderFactory.setProvider(originalProvider)
    }

    private fun setCatalystUseKeyParameters(value: Boolean) {
        CatalystFlagProviderFactory.setProvider(
            object : CatalystFlagProvider {
                override fun catalystUseKeyParameters() = value
            }
        )
    }

    @Test
    fun getKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(A11yServiceScreen.KEY)
    }

    @Test
    fun parametersSchema_isCorrect() {
        val schema = A11yServiceScreen.parametersSchema
        val parameter = schema.getParameters()[AccessibilitySettings.EXTRA_COMPONENT_NAME]

        assertThat(parameter).isNotNull()
        assertThat(parameter!!.type).isEqualTo(AccessibilityService)
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
                    A11Y_SERVICE_COMPONENT
                )
            )
            .thenReturn(expectedPageId)
        assertThat(preferenceScreenCreator.metricsCategory).isEqualTo(expectedPageId)
    }

    @Test
    fun getTitle() {
        assertThat(preferenceScreenCreator.getTitle(appContext).toString())
            .isEqualTo(A11Y_SERVICE_COMPONENT.className)
    }

    @Test
    fun createWidget_verifyWidgetTypeAndIconSpaceReserved() {
        val widget = preferenceScreenCreator.createWidget(appContext)
        assertThat(widget).isInstanceOf(RestrictedPreference::class.java)
        assertThat(widget.isIconSpaceReserved).isTrue()
    }

    @Test
    fun bind_verifyIconNotNull() {
        val widget = preferenceScreenCreator.createAndBindWidget<RestrictedPreference>(appContext)
        assertThat(widget.icon).isNotNull()
    }

    @Test
    fun getFragmentClass_equalsA11yServicePreferenceFragment() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(A11yServicePreferenceFragment::class.java)
    }

    @Test
    fun getBindingKey() {
        assertThat(preferenceScreenCreator.bindingKey)
            .isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())
    }

    @Test
    fun getLaunchIntent_hasStringExtraForComponent() {
        val intent = preferenceScreenCreator.getLaunchIntent(appContext, null)
        assertThat(intent.action).isEqualTo(Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS)
        assertThat(intent.getStringExtra(Intent.EXTRA_COMPONENT_NAME))
            .isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun parameters_hasTwoA11yServices_returnTwoItems_bundleArguments() {
        setCatalystUseKeyParameters(false)
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        runTest {
            val serviceInfo1 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT)
            val serviceInfo2 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT2)

            a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo1, serviceInfo2))
            val collectedItems = mutableListOf<String?>()
            A11yServiceScreen.parameters(appContext).collect {
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
                        A11Y_SERVICE_COMPONENT.flattenToString(),
                        A11Y_SERVICE_COMPONENT2.flattenToString(),
                    )
                )
        }
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun parameters_hasTwoA11yServices_returnTwoItems() {
        setCatalystUseKeyParameters(true)
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        runTest {
            val serviceInfo1 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT)
            val serviceInfo2 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT2)

            a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo1, serviceInfo2))
            val collectedItems = mutableListOf<String?>()
            A11yServiceScreen.keyParameters(appContext).collect {
                collectedItems.add(it[AccessibilitySettings.EXTRA_COMPONENT_NAME])
            }
            assertThat(collectedItems).hasSize(2)
            assertThat(collectedItems)
                .containsExactlyElementsIn(
                    listOf(
                        A11Y_SERVICE_COMPONENT.flattenToString(),
                        A11Y_SERVICE_COMPONENT2.flattenToString(),
                    )
                )
        }
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun parameters_withMixedServices_onlyEmitsSystemApps() = runTest {
        setCatalystUseKeyParameters(true)
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val systemService =
            createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT, isSystemApp = true)
        val nonSystemService =
            createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT2, isSystemApp = false)
        a11yManager.setInstalledAccessibilityServiceList(listOf(systemService, nonSystemService))

        val collectedItems = mutableListOf<String?>()
        A11yServiceScreen.keyParameters(appContext).collect {
            collectedItems.add(it[AccessibilitySettings.EXTRA_COMPONENT_NAME])
        }

        assertThat(collectedItems).hasSize(1)
        assertThat(collectedItems).containsExactly(A11Y_SERVICE_COMPONENT.flattenToString())
        assertThat(collectedItems).doesNotContain(A11Y_SERVICE_COMPONENT2.flattenToString())
    }

    private fun createA11yServiceInfo(
        isAlwaysOnService: Boolean = false,
        serviceComponent: ComponentName = A11Y_SERVICE_COMPONENT,
        isSystemApp: Boolean = true,
    ): AccessibilityServiceInfo {
        return AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                serviceComponent,
                isAlwaysOnService,
            )
            .apply {
                isAccessibilityTool = true
                if (isSystemApp) {
                    resolveInfo?.serviceInfo?.applicationInfo?.let {
                        it.flags = it.flags or FLAG_SYSTEM
                    }
                }
            }
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagTrue_validString_parsedCorrectly() {
        setCatalystUseKeyParameters(false)
        val args =
            Bundle().apply {
                putString(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    A11Y_SERVICE_COMPONENT.flattenToString(),
                )
            }
        val screen = A11yServiceScreen(appContext, args)
        assertThat(screen.bindingKey).isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagTrue_validString_parsedCorrectly_usingKeyParameters() {
        setCatalystUseKeyParameters(true)
        val keyParameters =
            A11yServiceScreen.parametersSchema.prepare(
                AccessibilitySettings.EXTRA_COMPONENT_NAME to
                    A11Y_SERVICE_COMPONENT.flattenToString()
            )
        val screen = A11yServiceScreen(appContext, keyParameters)
        assertThat(screen.bindingKey).isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagTrue_invalidString_throwsException() {
        setCatalystUseKeyParameters(false)
        val args =
            Bundle().apply {
                putString(AccessibilitySettings.EXTRA_COMPONENT_NAME, "invalidComponent")
            }
        val screen = A11yServiceScreen(appContext, args)

        assertThrows(IllegalArgumentException::class.java) { screen.bindingKey }
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagTrue_invalidString_throwsException_usingKeyParameters() {
        setCatalystUseKeyParameters(true)
        val keyParameters =
            A11yServiceScreen.parametersSchema.prepare(
                AccessibilitySettings.EXTRA_COMPONENT_NAME to "invalidComponent"
            )
        val screen = A11yServiceScreen(appContext, keyParameters)

        assertThrows(IllegalArgumentException::class.java) { screen.bindingKey }
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagTrue_missingKey_throwsException() {
        setCatalystUseKeyParameters(false)
        val args = Bundle()
        val screen = A11yServiceScreen(appContext, args)

        assertThrows(IllegalArgumentException::class.java) { screen.bindingKey }
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagTrue_canRetrieveTheComponentNameFromParcelable() {
        setCatalystUseKeyParameters(false)
        val args =
            Bundle().apply {
                putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, A11Y_SERVICE_COMPONENT)
            }
        val screen = A11yServiceScreen(appContext, args)

        assertThat(screen.bindingKey).isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagFalse_validParcelable_parsedCorrectly() {
        setCatalystUseKeyParameters(false)
        val args =
            Bundle().apply {
                putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, A11Y_SERVICE_COMPONENT)
            }
        val screen = A11yServiceScreen(appContext, args)
        assertThat(screen.bindingKey).isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagFalse_missingKey_throwsException() {
        setCatalystUseKeyParameters(false)
        val args = Bundle()
        val screen = A11yServiceScreen(appContext, args)

        assertThrows(IllegalArgumentException::class.java) { screen.bindingKey }
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun featureComponentName_flagFalse_canRetrieveTheComponentNameFromString() {
        setCatalystUseKeyParameters(false)
        val args =
            Bundle().apply {
                putString(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    A11Y_SERVICE_COMPONENT.flattenToString(),
                )
            }
        val screen = A11yServiceScreen(appContext, args)

        assertThat(screen.bindingKey).isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun parameters_flagTrue_emitsKeyParametersWithString() = runTest {
        setCatalystUseKeyParameters(true)
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val serviceInfo1 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT)
        val serviceInfo2 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT2)
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo1, serviceInfo2))

        val collectedKeyParameters = mutableListOf<ValidatedKeyParameters>()
        A11yServiceScreen.keyParameters(appContext).collect { collectedKeyParameters.add(it) }

        assertThat(collectedKeyParameters).hasSize(2)
        // Check first keyParameter
        assertThat(
                collectedKeyParameters[0].getRequired(AccessibilitySettings.EXTRA_COMPONENT_NAME)
            )
            .isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())

        // Check second keyParameter
        assertThat(
                collectedKeyParameters[1].getRequired(AccessibilitySettings.EXTRA_COMPONENT_NAME)
            )
            .isEqualTo(A11Y_SERVICE_COMPONENT2.flattenToString())
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun parameters_flagTrue_emitsBundleWithString() = runTest {
        setCatalystUseKeyParameters(false)
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val serviceInfo1 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT)
        val serviceInfo2 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT2)
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo1, serviceInfo2))

        val collectedBundles = mutableListOf<Bundle>()
        A11yServiceScreen.parameters(appContext).collect { collectedBundles.add(it) }

        assertThat(collectedBundles).hasSize(2)
        // Check first bundle
        assertThat(collectedBundles[0].getString(AccessibilitySettings.EXTRA_COMPONENT_NAME))
            .isEqualTo(A11Y_SERVICE_COMPONENT.flattenToString())
        assertThat(
                collectedBundles[0].getParcelable(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    ComponentName::class.java,
                )
            )
            .isNull()
        // Check second bundle
        assertThat(collectedBundles[1].getString(AccessibilitySettings.EXTRA_COMPONENT_NAME))
            .isEqualTo(A11Y_SERVICE_COMPONENT2.flattenToString())
        assertThat(
                collectedBundles[1].getParcelable(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    ComponentName::class.java,
                )
            )
            .isNull()
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun parameters_flagFalse_emitsBundleWithParcelable() = runTest {
        setCatalystUseKeyParameters(false)
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val serviceInfo1 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT)
        val serviceInfo2 = createA11yServiceInfo(serviceComponent = A11Y_SERVICE_COMPONENT2)
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo1, serviceInfo2))

        val collectedBundles = mutableListOf<Bundle>()
        A11yServiceScreen.parameters(appContext).collect { collectedBundles.add(it) }

        assertThat(collectedBundles).hasSize(2)
        // Check first bundle
        assertThat(
                collectedBundles[0].getParcelable(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    ComponentName::class.java,
                )
            )
            .isEqualTo(A11Y_SERVICE_COMPONENT)
        assertThat(collectedBundles[0].getString(AccessibilitySettings.EXTRA_COMPONENT_NAME))
            .isNull()
        // Check second bundle
        assertThat(
                collectedBundles[1].getParcelable(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    ComponentName::class.java,
                )
            )
            .isEqualTo(A11Y_SERVICE_COMPONENT2)
        assertThat(collectedBundles[1].getString(AccessibilitySettings.EXTRA_COMPONENT_NAME))
            .isNull()
    }

    companion object {
        private const val PACKAGE_NAME = "com.foo.bar"
        private val A11Y_SERVICE_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yService")
        private val A11Y_SERVICE_COMPONENT2 = ComponentName(PACKAGE_NAME, "FakeA11yService2")
    }
}
